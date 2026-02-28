package com.solanasuper.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.solanasuper.data.NonceAccountDao
import com.solanasuper.data.OfflineTransaction
import com.solanasuper.data.TransactionDao
import com.solanasuper.data.TransactionStatus
import com.solanasuper.data.OfflineCommitment
import com.solanasuper.network.NetworkManager
import com.solanasuper.network.SolanaSyncWorker
import com.solanasuper.p2p.TransactionManager
import com.solanasuper.security.IdentityKeyManager
import com.solanasuper.utils.AppLogger
import com.solanasuper.utils.DurableNonceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class IncomeViewModel(
    private val transactionManager: TransactionManager,
    private val transactionDao: TransactionDao,
    private val p2pTransferManager: com.solanasuper.network.P2PTransferManager,
    private val identityKeyManager: IdentityKeyManager,
    private val nonceAccountDao: NonceAccountDao
) : ViewModel() {

    // Expose IdentityKeyManager for UI interactions (Signing)
    val identityManager: IdentityKeyManager get() = identityKeyManager

    private val _state = MutableStateFlow(IncomeUiState())
    val state = _state.asStateFlow()

    // New State for optional signing flow
    private val _signRequest = Channel<ByteArray>()
    val signRequest = _signRequest.receiveAsFlow()
    
    // New UI Events (e.g. Open Faucet URL)
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var pendingTransaction: PendingTx? = null

    /** Nonce setup state: the raw Ed25519 seed for the nonce account keypair's self-signature. */
    private var pendingNonceSetupSeed32: ByteArray? = null

    /** If nonce setup is deferred from a P2P send, re-trigger this transfer afterwards. */
    private var pendingTransferAfterSetup: Triple<String, Double, String>? = null // endpointId, amount, peerPubKey

    data class PendingTx(val recipient: String, val amount: Double, val data: ByteArray)

    init {
        setupP2P()
        loadData()
        refresh()
        // Provision nonce account in background when online
        viewModelScope.launch { ensureNonceSetup() }
    }

    fun refresh() {
        loadData()
        retryPendingBroadcasts()
    }

    /**
     * If the nonce account exists but is stale, refreshes its value from chain.
     * Nonce creation is handled lazily on first P2P send via biometric flow.
     */
    private suspend fun ensureNonceSetup() {
        if (!NetworkManager.isLiveMode.value) return
        try {
            val rpcUrl = NetworkManager.activeRpcUrl.value
            val existing = nonceAccountDao.get()
            if (existing != null && !existing.isValid) {
                AppLogger.i("IncomeViewModel", "Refreshing stale nonce from chain…")
                DurableNonceManager.refreshNonce(rpcUrl, nonceAccountDao)
            } else if (existing != null) {
                AppLogger.i("IncomeViewModel", "Nonce account ready: ${existing.nonceAccountPubkey}")
            }
            // If null — creation happens on first P2P send attempt via biometric
        } catch (e: Exception) {
            AppLogger.w("IncomeViewModel", "Nonce check failed (non-fatal): ${e.message}")
        }
    }

    private fun retryPendingBroadcasts() {
        if (!NetworkManager.isLiveMode.value) return
        // Delegate to SolanaSyncWorker which handles both SIGNED_OFFLINE and PENDING_SYNC rows
        scheduleSyncWorker()
    }

    /** Schedules the SolanaSyncWorker with a network constraint + exponential back-off. */
    private fun scheduleSyncWorker() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.solanasuper.network.SolanaSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15L, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            androidx.work.WorkManager.getInstance(identityKeyManager.context).enqueue(workRequest)
            com.solanasuper.utils.AppLogger.i("IncomeViewModel", "SolanaSyncWorker instantly queued for background broadcast")
        } catch (_: Exception) {
            com.solanasuper.utils.AppLogger.e("IncomeViewModel", "Failed to queue SyncWorker via WorkManager")
        }
    }



    private fun setupP2P() {
        p2pTransferManager.callback = object : com.solanasuper.network.P2PTransferManager.P2PCallback {
            override fun onPeerFound(endpointId: String) {
                _state.update { it.copy(p2pStatus = PeerStatus.FOUND_PEER) }
            }

            override fun onConnectionInitiated(endpointId: String, info: com.google.android.gms.nearby.connection.ConnectionInfo) {
                _state.update { 
                    it.copy(
                        p2pStatus = PeerStatus.VERIFYING,
                        p2pPeerName = info.endpointName,
                        p2pAuthToken = info.authenticationToken,
                        p2pEndpointId = endpointId
                    ) 
                }
            }

            override fun onConnected(endpointId: String) {
                // Handshake: Send my Public Key
                val myKey = identityKeyManager.getSolanaPublicKey()
                if (myKey != null) {
                    com.solanasuper.utils.AppLogger.d("IncomeViewModel", "P2P Connected. Sending PubKey: $myKey")
                    val payload = "PubKey:$myKey".toByteArray()
                    p2pTransferManager.sendData(endpointId, payload)
                }

                _state.update { 
                    if (it.isP2PSender) {
                        it.copy(p2pStatus = PeerStatus.CONNECTED_WAITING_INPUT) 
                    } else {
                        it.copy(p2pStatus = PeerStatus.CONNECTED_WAITING_FUNDS)
                    }
                }
            }

            override fun onDataReceived(endpointId: String, data: ByteArray) {
                 val message = String(data)
                 com.solanasuper.utils.AppLogger.d("IncomeViewModel", "P2P Data Received: $message")

                 if (message.startsWith("PubKey:")) {
                     val key = message.removePrefix("PubKey:")
                     _state.update { it.copy(peerPublicKey = key) }
                     return
                 }

                 if (message.startsWith("DurableCommitment:")) {
                     val json = message.removePrefix("DurableCommitment:")
                     viewModelScope.launch {
                         try {
                             AppLogger.i("IncomeViewModel", "Received DurableCommitment (real signed tx)")
                             val jsonObj      = org.json.JSONObject(json)
                             val sender       = jsonObj.getString("sender")
                             val amountLamports = jsonObj.getLong("amountLamports")
                             val timestamp    = jsonObj.getLong("timestamp")
                             val nonceUsed    = jsonObj.optString("nonceUsed", null)
                             val signedTxB64  = jsonObj.optString("signedTxB64", null)

                             // Store as SIGNED_OFFLINE — receiver has the real signed tx
                             // and can independently verify on-chain when online
                             val receiverRow = OfflineTransaction(
                                 id                       = "p2p_receive_$timestamp",
                                 amount                   = amountLamports,   // Positive for receiver
                                 timestamp                = timestamp,
                                 recipientId              = sender,
                                 status                   = if (!signedTxB64.isNullOrBlank())
                                                                TransactionStatus.SIGNED_OFFLINE
                                                            else
                                                                TransactionStatus.PENDING_SYNC,
                                 isLiveBroadcastPending   = false,  // Receiver doesn't broadcast, just tracks
                                 signedPayload            = signedTxB64,
                                 nonceUsed                = nonceUsed,
                                 peerSignedCommitmentJson = json
                             )
                             transactionDao.insert(receiverRow)

                             // Credit local balance immediately (optimistic)
                             transactionManager.receiveFunds(amountLamports, "P2P DurableNonce Receive")

                             _state.update { it.copy(p2pStatus = PeerStatus.SUCCESS) }
                             delay(2000)
                             loadData()
                             stopP2P()
                         } catch (e: Exception) {
                             AppLogger.e("IncomeViewModel", "Bad DurableCommitment", e)
                             _state.update { it.copy(status = UiStatus.Error("Invalid P2P Commitment")) }
                         }
                     }
                     return
                 }

                 if (message.startsWith("Commitment:")) {
                     val json = message.removePrefix("Commitment:")
                     viewModelScope.launch {
                         try {
                             com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Received Commitment: $json")
                             
                             // 1. Parse JSON
                             val jsonObj = org.json.JSONObject(json)
                             val sender = jsonObj.getString("sender")
                             val recipient = jsonObj.getString("recipient")
                             val amountLamports = jsonObj.getLong("amountLamports")
                             val timestamp = jsonObj.getLong("timestamp")
                             val signature = jsonObj.getString("signature")
                             
                             // ... (Rest of logic)
                             
                             // 3. Save to DB (PENDING_SYNC - Incoming)
                             val offlineTx = com.solanasuper.data.OfflineTransaction(
                                 id = "p2p_receive_${timestamp}",
                                 amount = amountLamports,
                                 timestamp = timestamp,
                                 recipientId = sender, // From Sender
                                 status = TransactionStatus.PENDING_SYNC,
                                 isLiveBroadcastPending = false, // Receiver doesn't need to broadcast, sender does. But we track it.
                                 signedPayload = json
                             )
                             transactionDao.insert(offlineTx)
                             
                             // 4. Update UI
                             transactionManager.receiveFunds(amountLamports, "Pending P2P")
                             _state.update { it.copy(p2pStatus = PeerStatus.SUCCESS) }
                             delay(2000)
                             loadData() 
                             
                             stopP2P()
                             
                         } catch (e: Exception) {
                             com.solanasuper.utils.AppLogger.e("IncomeViewModel", "Bad Commitment", e)
                             _state.update { it.copy(status = UiStatus.Error("Invalid P2P Commitment")) }
                         }
                     }
                     return
                 }
                 if (message.startsWith("Transfer:")) {
                     val amount = message.removePrefix("Transfer:").toDoubleOrNull()
                     if (amount != null) {
                         viewModelScope.launch {
                             transactionManager.receiveFunds((amount * 1_000_000_000).toLong(), "P2P Receive")
                             _state.update { it.copy(p2pStatus = PeerStatus.SUCCESS) }
                             delay(2500)
                             this@IncomeViewModel.loadData()
                             _state.update { it.copy(p2pStatus = PeerStatus.IDLE) }
                         }
                     }
                 }
            }

            override fun onError(message: String) {
                _state.update { it.copy(p2pStatus = PeerStatus.ERROR, status = UiStatus.Error(message)) }
            }

            override fun onDisconnected(endpointId: String) {
                _state.update { it.copy(p2pStatus = PeerStatus.IDLE) }
            }
        }
    }

    fun confirmConnection() {
        val endpointId = _state.value.p2pEndpointId ?: return
        p2pTransferManager.acceptConnection(endpointId)
        _state.update { it.copy(p2pStatus = PeerStatus.CONNECTING) }
    }

    fun sendP2P(amount: Double) {
        val endpointId = _state.value.p2pEndpointId ?: return
        viewModelScope.launch {
             if (NetworkManager.isLiveMode.value) {
                 try {
                     // Live Mode P2P
                     val recipientKey = _state.value.peerPublicKey
                     if (recipientKey == null) {
                         _state.update { it.copy(status = UiStatus.Error("Peer Identity Unknown")) }
                         return@launch
                     }

                     _state.update { it.copy(status = UiStatus.Loading) }
                     
                     // HACK for now: We will set 'pendingTransaction' with a special recipient "P2P:<EndpointID>"
                     // And in 'broadcastTransaction', we check if recipient starts with "P2P:" then we send Data instead of RPC.
                     
                     prepareTransaction("P2P:$endpointId", amount)
                     
                 } catch (e: Exception) {
                     com.solanasuper.utils.AppLogger.e("IncomeViewModel", "Live P2P Failed", e)
                     _state.update { it.copy(status = UiStatus.Error("P2P Error: ${e.message}")) }
                 }
             } else {
                 // Simulated Mode
                 // Local deduction
                 val amountLamports = (amount * 1_000_000_000).toLong()
                 com.solanasuper.utils.AppLogger.d("IncomeViewModel", "Attempting P2P Send: $amount SOL ($amountLamports lamports)")
                 
                 // Check balance first specifically for logging
                 val currentBalance = transactionDao.getAvailableBalance() ?: 0L
                 com.solanasuper.utils.AppLogger.d("IncomeViewModel", "Current Offline Balance: $currentBalance lamports")
    
                 val success = transactionManager.sendFunds(amountLamports, "P2P Send")
                 if (success) {
                     // Send Data
                     val payload = "Transfer:$amount".toByteArray()
                     p2pTransferManager.sendData(endpointId, payload)
                     
                     _state.update { it.copy(p2pStatus = PeerStatus.SUCCESS) }
                     delay(2500)
                     loadData()
                     stopP2P()
                 } else {
                     _state.update { it.copy(status = UiStatus.Error("Insufficient Funds (Check SIM Balance)")) }
                 }
             }
        }
    }

    fun rejectConnection() {
        val endpointId = _state.value.p2pEndpointId ?: return
        p2pTransferManager.rejectConnection(endpointId)
        stopP2P()
    }

    fun loadData() {
        com.solanasuper.utils.AppLogger.d("IncomeViewModel", "loadData() called")
        viewModelScope.launch {
            _state.update { it.copy(status = UiStatus.Loading) }
            try {
                if (!NetworkManager.isLiveMode.value) {
                    com.solanasuper.utils.AppLogger.d("IncomeViewModel", "Simulation Mode")
                    val balance = transactionDao.getAvailableBalance() ?: 0L
                    _state.update {
                        it.copy(
                            balance = balance / 1_000_000_000.0,
                            status = UiStatus.Success
                        )
                    }
                    return@launch
                }

                // On-Chain Truth (Live Mode)
                val rpcUrl = NetworkManager.activeRpcUrl.value
                val solanaAddress = identityKeyManager.getSolanaPublicKey() ?: throw Exception("No Identity")
                
                com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Fetching LIVE data for $solanaAddress")

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // 1. Fetch Balance
                    try {
                        val balanceSol = fetchSolanaBalance(rpcUrl, solanaAddress)
                        com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Live Balance: $balanceSol SOL")
                        
                        // 2. Fetch History
                        val onChainHistory = fetchTransactionHistory(rpcUrl, solanaAddress)
                        
                        // 3. Merge Local Pending (Offline Commitments)
                        // We filter for PENDING_SYNC or LIVE_BROADCAST_PENDING that are NOT in onChainHistory
                        val localPending = transactionDao.getPendingSyncTransactions()
                        // Convert OfflineTransaction -> UiTransaction
                        val pendingUi = localPending.map { tx ->
                             UiTransaction(
                                 id = "pending_${tx.timestamp}",
                                 amount = kotlin.math.abs(tx.amount) / 1_000_000_000.0,
                                 timestamp = tx.timestamp,
                                 recipientId = if (tx.amount < 0) "To Peer (Pending)" else "From Peer (Pending)",
                                 isReceived = tx.amount > 0,
                                 status = com.solanasuper.data.TransactionStatus.PENDING_SYNC
                             )
                        }
                        
                        // Combine: Pending First, then History
                        val combinedHistory = pendingUi + onChainHistory

                        // 4. Adjust Display Balance
                        // We must subtract pending OUTGOING (-) and add pending INCOMING (+)
                        // so both sender and receiver see the optimistic balance locally while waiting for the network
                        val pendingOutgoing = localPending.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
                        val pendingIncoming = localPending.filter { it.amount > 0 }.sumOf { it.amount }
                        val displayBalance = balanceSol - (pendingOutgoing / 1_000_000_000.0) + (pendingIncoming / 1_000_000_000.0)

                        com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Live History: ${onChainHistory.size}, Pending: ${localPending.size}")

                        _state.update { 
                            it.copy(
                                balance = displayBalance,
                                transactions = combinedHistory,
                                status = UiStatus.Success
                            ) 
                        }
                    } catch (e: Exception) {
                        // CRITICAL: DO NOT FALLBACK TO LOCAL DB IN LIVE MODE
                        com.solanasuper.utils.AppLogger.e("IncomeViewModel", "Live Data Fetch Failed", e)
                        _state.update { 
                            it.copy(
                                status = UiStatus.Error("Network Error: ${e.message}. Balance set to 0."),
                                balance = 0.0 // Explicitly 0 on error
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(status = UiStatus.Error(e.message ?: "Unknown Error")) }
            }
        }
    }

    // Helper to get Blockhash
    private fun getLatestBlockhash(rpcUrl: String): String {
        val json = """{"jsonrpc":"2.0", "id":1, "method":"getLatestBlockhash", "params":[]}"""
        val response = postRpc(rpcUrl, json)
        val jsonObject = org.json.JSONObject(response)
        if (jsonObject.has("error")) throw Exception("RPC Error: ${jsonObject.get("error")}")
        return jsonObject.getJSONObject("result").getJSONObject("value").getString("blockhash")
    }

    private fun fetchSolanaBalance(rpcUrl: String, address: String): Double {
        val json = """{"jsonrpc":"2.0", "id":1, "method":"getBalance", "params":["$address"]}"""
        val response = postRpc(rpcUrl, json)
        val jsonObject = org.json.JSONObject(response)
        if (jsonObject.has("error")) throw Exception("RPC Error: ${jsonObject.get("error")}")
        
        val lamports = jsonObject.getJSONObject("result").getLong("value")
        return lamports / 1_000_000_000.0
    }

    private fun fetchTransactionHistory(rpcUrl: String, address: String): List<UiTransaction> {
        val json = """{"jsonrpc":"2.0", "id":1, "method":"getSignaturesForAddress", "params":["$address", {"limit": 5}]}"""
        val response = postRpc(rpcUrl, json)
        
        val jsonResponse = org.json.JSONObject(response)
        if (jsonResponse.has("error")) return emptyList()
        
        val resultArray = jsonResponse.getJSONArray("result")
        val list = mutableListOf<UiTransaction>()
        
        // Parallelize fetching details would be better, but sequential is safer for now
        for (i in 0 until resultArray.length()) {
            try {
                val txObj = resultArray.getJSONObject(i)
                val signature = txObj.getString("signature")
                val blockTime = txObj.optLong("blockTime", 0) * 1000 // sec to ms
                
                // Fetch Details
                val detailsJson = """
                    {"jsonrpc":"2.0", "id":1, "method":"getTransaction", "params":["$signature", {"encoding": "json", "maxSupportedTransactionVersion": 0}]}
                """.trimIndent()
                
                val detailsResponse = postRpc(rpcUrl, detailsJson)
                val detailsObj = org.json.JSONObject(detailsResponse)
                
                if (!detailsObj.has("result") || detailsObj.isNull("result")) {
                    // Fallback if details fail
                    list.add(UiTransaction(signature, 0.0, blockTime, "Processing...", true))
                    continue
                }
                
                val result = detailsObj.getJSONObject("result")
                val meta = result.getJSONObject("meta")
                val transaction = result.getJSONObject("transaction")
                val message = transaction.getJSONObject("message")
                
                // Find my index
                val accountKeys = message.getJSONArray("accountKeys")
                var myIndex = -1
                for (k in 0 until accountKeys.length()) {
                    // accountKeys can be array of strings OR array of objects (if versioned)
                    // standard json encoding usually returns strings or objects with 'pubkey'
                    val keyItem = accountKeys.get(k)
                    val keyString = if (keyItem is String) keyItem else (keyItem as org.json.JSONObject).getString("pubkey")
                    
                    if (keyString == address) {
                        myIndex = k
                        break
                    }
                }
                
                if (myIndex != -1) {
                    val preBalances = meta.getJSONArray("preBalances")
                    val postBalances = meta.getJSONArray("postBalances")
                    
                    val pre = preBalances.getLong(myIndex)
                    val post = postBalances.getLong(myIndex)
                    
                    val diffLamports = post - pre
                    val isReceived = diffLamports >= 0
                    val amountSol = kotlin.math.abs(diffLamports) / 1_000_000_000.0
                    
                    list.add(UiTransaction(
                        id = signature,
                        amount = amountSol,
                        timestamp = blockTime,
                        recipientId = if (isReceived) "From Network" else "To Network",
                        isReceived = isReceived
                    ))
                } else {
                     list.add(UiTransaction(signature, 0.0, blockTime, "Unknown", true))
                }
            } catch (e: Exception) {
                com.solanasuper.utils.AppLogger.e("IncomeVM", "Failed to parse tx", e)
                // Add placeholder on error to avoid empty list gaps
                list.add(UiTransaction(
                     id = resultArray.getJSONObject(i).getString("signature"),
                     amount = 0.0,
                     timestamp = System.currentTimeMillis(),
                     recipientId = "Error",
                     isReceived = true
                 ))
            }
        }
        return list
    }

    private fun postRpc(url: String, body: String): String {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 60000 // 60s for Render cold-start (strict rule)
        connection.readTimeout = 60000
        
        connection.outputStream.use { it.write(body.toByteArray()) }
        
        if (connection.responseCode != 200) {
            throw Exception("RPC HTTP ${connection.responseCode}")
        }
        
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    fun claimUbi() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(status = UiStatus.Loading) }
            try {
                val solanaAddress = identityKeyManager.getSolanaPublicKey() ?: throw Exception("No Identity")
                
                // FORCE DEVNET URL for Airdrop (ignore QuickNode)
                val airdropRpcUrl = "https://api.devnet.solana.com"
                val LAMROTS_PER_SOL = 1_000_000_000L

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                     if (NetworkManager.isLiveMode.value) {
                         android.util.Log.d("SolanaSuper", "Airdrop via $airdropRpcUrl")

                         
                         val jsonBody = """
                             {"jsonrpc":"2.0", "id":1, "method":"requestAirdrop", "params":["$solanaAddress", $LAMROTS_PER_SOL]}
                         """.trimIndent()
                         
                         val response = postRpc(airdropRpcUrl, jsonBody)
                         if (response.contains("error")) {
                              // If program failed, instruct to use Faucet
                              throw Exception("Airdrop limit reached or rejected")
                         }
                         delay(4000)
                     } else {
                         delay(1000)
                         transactionManager.receiveFunds(LAMROTS_PER_SOL, "Simulated Airdrop")
                     }
                }
                loadData()
            } catch (e: Exception) {
                // FALLBACK: Offer to open web faucet
                _state.update { it.copy(status = UiStatus.Error("Auto-Airdrop Failed: Opening Web Faucet")) }
                delay(1000) // wait for user to see message
                _uiEvent.send("OPEN_URL|https://faucet.solana.com")
            }
        }
    }

    fun startSending() {
        if (_state.value.p2pStatus != PeerStatus.IDLE) return
        _state.update { it.copy(p2pStatus = PeerStatus.SCANNING, status = UiStatus.Idle, isP2PSender = true) }
        p2pTransferManager.startDiscovery()
    }

    fun startReceiving() {
        if (_state.value.p2pStatus != PeerStatus.IDLE) return
        _state.update { it.copy(p2pStatus = PeerStatus.SCANNING, status = UiStatus.Idle, isP2PSender = false) }
        p2pTransferManager.startAdvertising()
    }

    fun stopP2P() {
        p2pTransferManager.stop()
        _state.update { it.copy(p2pStatus = PeerStatus.IDLE, p2pPeerName = null) }
    }

    override fun onCleared() {
        super.onCleared()
        p2pTransferManager.stop()
    }

    // 3-Way Send Logic

    fun prepareTransaction(recipient: String, amount: Double) {
        AppLogger.i("IncomeViewModel", "Preparing transaction: $amount SOL to $recipient")
        viewModelScope.launch {
            try {
                if (amount <= 0) throw Exception("Invalid amount")
                if (amount > (_state.value.balance ?: 0.0)) throw Exception("Insufficient funds")

                val lamports = (amount * 1_000_000_000).toLong()
                val myAddress = identityKeyManager.getSolanaPublicKey() ?: throw Exception("No Identity")

                if (NetworkManager.isLiveMode.value) {
                    val rpcUrl = NetworkManager.activeRpcUrl.value

                    if (recipient.startsWith("P2P:")) {
                        // ═══════════════════════════════════════════════════
                        // OFFLINE P2P PATH — Use Durable Nonce
                        // ═══════════════════════════════════════════════════
                        val peerPublicKey = _state.value.peerPublicKey
                            ?: throw Exception("Peer identity not received. Reconnect.")

                        // Ensure nonce account exists; create if needed via biometric
                        val nonceRecord = nonceAccountDao.get()
                        if (nonceRecord == null || !nonceRecord.isValid) {
                            AppLogger.i("IncomeViewModel", "No nonce account — routing setup through biometric")
                            _state.update { it.copy(status = UiStatus.Loading) }

                            // Build the CreateAccount+InitializeNonce message (network call for blockhash)
                            val setupMsg = withContext(Dispatchers.IO) {
                                DurableNonceManager.buildNonceSetupMessage(myAddress, rpcUrl)
                            }

                            // Store nonce seed (for self-sig in broadcastTransaction)
                            pendingNonceSetupSeed32 = DurableNonceManager.derivedNonceSeed32(myAddress)
                            // Stash the original transfer intent to replay after setup
                            val endpointId = recipient.removePrefix("P2P:")
                            pendingTransferAfterSetup = Triple(endpointId, amount, peerPublicKey)

                            // Route through biometric — sig[0] = wallet authority
                            pendingTransaction = PendingTx("nonce_setup", 0.0, setupMsg)
                            _signRequest.send(setupMsg)
                            return@launch  // wait for broadcastTransaction(walletSig) callback
                        }

                        // Build offline transfer message using cached durable nonce
                        val message = withContext(Dispatchers.IO) {
                            DurableNonceManager.buildOfflineTransferMessage(
                                fromPubkeyB58 = myAddress,
                                toPubkeyB58   = peerPublicKey,
                                lamports      = lamports,
                                dao           = nonceAccountDao
                            )
                        }

                        pendingTransaction = PendingTx(recipient, amount, message)
                        _signRequest.send(message) // Biometric prompt fires

                    } else {
                        // ═══════════════════════════════════════════════════
                        // ONLINE DIRECT SEND — Regular blockhash tx
                        // ═══════════════════════════════════════════════════
                        val blockhashStr = withContext(Dispatchers.IO) { getLatestBlockhash(rpcUrl) }
                        val blockhash = com.solanasuper.utils.Base58.decode(blockhashStr)
                        val message = com.solanasuper.utils.SolanaUtil.createTransferMessage(
                            com.solanasuper.utils.Base58.decode(myAddress),
                            com.solanasuper.utils.Base58.decode(recipient),
                            lamports,
                            blockhash
                        )
                        pendingTransaction = PendingTx(recipient, amount, message)
                        _signRequest.send(message)
                    }

                } else {
                    // SIM mode
                    val instruction = "Transfer ${amount} SOL to $recipient".toByteArray()
                    pendingTransaction = PendingTx(recipient, amount, instruction)
                    _signRequest.send(instruction)
                }
            } catch (e: Exception) {
                AppLogger.e("IncomeViewModel", "Transaction preparation failed", e)
                _state.update { it.copy(status = UiStatus.Error(e.message ?: "Invalid Transaction")) }
            }
        }
    }

    fun broadcastTransaction(signature: ByteArray) {
        val tx = pendingTransaction ?: return
        AppLogger.i("IncomeViewModel", "Broadcasting signed transaction (recipient=${tx.recipient})")
        viewModelScope.launch {
            _state.update { it.copy(status = UiStatus.Loading) }
            try {
                val rpcUrl = NetworkManager.activeRpcUrl.value

                // ── Nonce Account Setup Path ────────────────────────────────
                // Biometric fired for the setup tx. Complete the 2-sig flow:
                //   sig[0] = walletSig (biometric, just received)
                //   sig[1] = nonce account self-sig (deterministic, no biometric)
                if (tx.recipient == "nonce_setup") {
                    val nonceSeed32 = pendingNonceSetupSeed32
                        ?: throw Exception("Nonce seed lost — retrying will fix this")
                    val myAddress = identityKeyManager.getSolanaPublicKey()
                        ?: throw Exception("No wallet identity")

                    // Nonce account's self-signature (internal, no biometric)
                    val nonceAccountSig = DurableNonceManager.signWithSeed(nonceSeed32, tx.data)

                    // 2-sig transaction: [authority(biometric), nonceAccount(internal)]
                    val encodedTx = com.solanasuper.utils.SolanaUtil.encodeTransactionMultiSig(
                        signatures = listOf(signature, nonceAccountSig),
                        message    = tx.data
                    )
                    val base64Tx = android.util.Base64.encodeToString(encodedTx, android.util.Base64.NO_WRAP)

                    // Broadcast
                    AppLogger.i("IncomeViewModel", "Broadcasting nonce setup tx...")
                    val response = withContext(Dispatchers.IO) {
                        postRpc(rpcUrl, """{"jsonrpc":"2.0","id":1,"method":"sendTransaction","params":["$base64Tx",{"encoding":"base64","skipPreflight":false,"preflightCommitment":"processed"}]}""")
                    }
                    val json = org.json.JSONObject(response)
                    if (json.has("error")) {
                        val errStr = json.get("error").toString()
                        if (errStr.contains("already in use", ignoreCase = true)) {
                            AppLogger.i("IncomeViewModel", "Nonce account already exists on-chain. Recovering state...")
                        } else {
                            throw Exception("Nonce setup failed: $errStr")
                        }
                    } else {
                        AppLogger.i("IncomeViewModel", "Nonce account created: ${json.optString("result")}")
                    }

                    // Wait for on-chain confirmation (if newly created), then fetch the nonce value
                    delay(4_000)
                    val noncePubkeyB58 = DurableNonceManager.derivedNoncePubkeyB58(myAddress)
                    val nonceValue = withContext(Dispatchers.IO) {
                        DurableNonceManager.fetchNonceValuePublic(rpcUrl, noncePubkeyB58)
                    }

                    // Save to Room DB
                    nonceAccountDao.upsert(
                        com.solanasuper.data.NonceAccount(
                            nonceAccountPubkey = noncePubkeyB58,
                            currentNonce       = nonceValue,
                            authorityPubkey    = myAddress
                        )
                    )
                    AppLogger.i("IncomeViewModel", "Nonce account stored. Nonce: $nonceValue")

                    // Clean up and retry the deferred P2P transfer
                    pendingNonceSetupSeed32 = null
                    pendingTransaction = null
                    val deferred = pendingTransferAfterSetup
                    pendingTransferAfterSetup = null

                    if (deferred != null) {
                        val (endpointId, deferredAmount, _) = deferred
                        AppLogger.i("IncomeViewModel", "Retrying deferred P2P transfer after nonce setup")
                        _state.update { it.copy(status = UiStatus.Idle) }
                        prepareTransaction("P2P:$endpointId", deferredAmount)
                    } else {
                        _state.update { it.copy(status = UiStatus.Success) }
                    }
                    return@launch
                }

                // ── Normal transaction paths ──────────────────────────────
                if (NetworkManager.isLiveMode.value) {
                    if (tx.recipient.startsWith("P2P:")) {
                        // ═══════════════════════════════════════════════════
                        // OFFLINE P2P BROADCAST — Store signed tx for later
                        // ═══════════════════════════════════════════════════
                        val endpointId   = tx.recipient.removePrefix("P2P:")
                        val myAddress    = identityKeyManager.getSolanaPublicKey() ?: ""
                        val peerKey      = _state.value.peerPublicKey ?: ""
                        val lamports     = (tx.amount * 1_000_000_000).toLong()
                        val ts           = System.currentTimeMillis()
                        val nonceRecord  = nonceAccountDao.get()

                        // 1. Encode the fully-signed durable-nonce transaction
                        val signedTxB64 = DurableNonceManager.encodeSignedOfflineTx(signature, tx.data)

                        // 2. Save as SIGNED_OFFLINE — SyncWorker will broadcast when online
                        val senderRow = OfflineTransaction(
                            id                       = "p2p_send_$ts",
                            amount                   = -lamports,
                            timestamp                = ts,
                            recipientId              = peerKey,
                            status                   = TransactionStatus.SIGNED_OFFLINE,
                            isLiveBroadcastPending   = true,
                            signedPayload            = signedTxB64,
                            nonceUsed                = nonceRecord?.currentNonce,
                            peerSignedCommitmentJson = null
                        )
                        transactionDao.insert(senderRow)

                        // 3. Send DurableCommitment JSON over Bluetooth
                        //    Receiver gets the signed tx bytes — can verify on-chain independently
                        val durableCommitmentJson = """
                            {
                                "type": "DurableCommitment",
                                "sender": "$myAddress",
                                "recipient": "$peerKey",
                                "amountLamports": $lamports,
                                "timestamp": $ts,
                                "nonceUsed": "${nonceRecord?.currentNonce ?: ""}",
                                "signedTxB64": "$signedTxB64"
                            }
                        """.trimIndent()

                        AppLogger.i("IncomeViewModel", "Sending DurableCommitment to peer $endpointId")
                        p2pTransferManager.sendData(endpointId, "DurableCommitment:$durableCommitmentJson".toByteArray())

                        _state.update { it.copy(p2pStatus = PeerStatus.SUCCESS, status = UiStatus.Success) }
                        delay(1000)
                        
                        // 4. Wake up the SyncWorker to broadcast immediately (if online)
                        scheduleSyncWorker()

                    } else {
                        // ═══════════════════════════════════════════════════
                        // ONLINE DIRECT SEND — Broadcast immediately
                        // ═══════════════════════════════════════════════════
                        val finalTxBytes  = com.solanasuper.utils.SolanaUtil.encodeTransaction(signature, tx.data)
                        val finalTxBase64 = android.util.Base64.encodeToString(finalTxBytes, android.util.Base64.NO_WRAP)

                        withContext(Dispatchers.IO) {
                            val json = """{"jsonrpc":"2.0","id":1,"method":"sendTransaction","params":["$finalTxBase64",{"encoding":"base64"}]}"""
                            val response = postRpc(rpcUrl, json)
                            val jsonObject = org.json.JSONObject(response)
                            if (jsonObject.has("error")) throw Exception("RPC Error: ${jsonObject.get("error")}")
                            AppLogger.i("IncomeViewModel", "TX sent: ${jsonObject.getString("result")}")
                        }
                    }
                } else {
                    // SIM mode
                    AppLogger.d("IncomeViewModel", "Simulation: locking funds locally")
                    delay(1000)
                    val success = transactionManager.lockFunds((tx.amount * 1_000_000_000).toLong())
                    if (!success) throw Exception("Insufficient Funds (Simulated)")
                }

                AppLogger.i("IncomeViewModel", "Transaction successful!")
                loadData()
                _state.update { it.copy(status = UiStatus.Success) }
                pendingTransaction = null
            } catch (e: Exception) {
                AppLogger.e("IncomeViewModel", "Broadcast failed", e)
                _state.update { it.copy(status = UiStatus.Error("Broadcast Failed: ${e.message}")) }
            }
        }
    }


    /**
     * Called when the biometric flow returns a signature.
     * For "nonce_setup" recipients, handled above.
     * For normal live P2P sends, encodes and stores SIGNED_OFFLINE.
     * For direct online sends, broadcasts immediately.
     * For SIM mode, deducts locally.
     */
    private fun handleNormalBroadcast(
        tx: PendingTx,
        signature: ByteArray,
        rpcUrl: String
    ) { /* extracted above inline — kept for readability */ }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val transactionManager: TransactionManager,
        private val transactionDao: TransactionDao,
        private val p2pTransferManager: com.solanasuper.network.P2PTransferManager,
        val identityKeyManager: IdentityKeyManager,
        private val nonceAccountDao: NonceAccountDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IncomeViewModel(
                transactionManager, transactionDao, p2pTransferManager,
                identityKeyManager, nonceAccountDao
            ) as T
        }
    }
}
