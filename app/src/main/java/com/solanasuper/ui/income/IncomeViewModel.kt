package com.solanasuper.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solanasuper.data.TransactionDao
import com.solanasuper.data.TransactionStatus
import com.solanasuper.p2p.TransactionManager
import com.solanasuper.network.NetworkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

import com.solanasuper.security.IdentityKeyManager

class IncomeViewModel(
    private val transactionManager: TransactionManager,
    private val transactionDao: TransactionDao,
    private val p2pTransferManager: com.solanasuper.network.P2PTransferManager,
    private val identityKeyManager: IdentityKeyManager
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

    data class PendingTx(val recipient: String, val amount: Double, val data: ByteArray)

    init {
        setupP2P()
        loadData()
        refresh() // Check for pending broadcasts on start
    }

    fun refresh() {
        loadData()
        retryPendingBroadcasts()
    }

    private fun retryPendingBroadcasts() {
        viewModelScope.launch {
            if (!NetworkManager.isLiveMode.value) return@launch
            
            val pendingTxs = transactionDao.getPendingBroadcasts()
            if (pendingTxs.isEmpty()) return@launch

            com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Retrying ${pendingTxs.size} pending broadcasts...")
            val rpcUrl = NetworkManager.activeRpcUrl.value
            
            pendingTxs.forEach { tx ->
                try {
                     if (tx.signedPayload != null) {
                         val json = """{"jsonrpc":"2.0", "id":1, "method":"sendTransaction", "params":["${tx.signedPayload}", {"encoding": "base64"}]}"""
                         
                         kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                             val response = postRpc(rpcUrl, json)
                             val jsonResp = org.json.JSONObject(response)
                             if (jsonResp.has("error")) {
                                 throw Exception("RPC Error: ${jsonResp.get("error")}")
                             }
                         }
                         
                         // Success! Mark as done (or delete). We'll update status.
                         // Using ID to update.
                         // For simplicity in this Dao, we might need a specific update method or just re-insert.
                         // Since we don't have updateStatus, we'll re-insert with modified fields or delete if it was a temp holder.
                         // Ideally we want to keep it as history? But we fetch history from chain.
                         // So we can just mark it as settled locally or delete. 
                         // Let's mark it as SETTLED and not pending.
                         
                         transactionDao.insert(tx.copy(status = TransactionStatus.CONFIRMED, isLiveBroadcastPending = false))
                         com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Retry Success for ${tx.id}")
                     }
                } catch (e: Exception) {
                    com.solanasuper.utils.AppLogger.e("IncomeViewModel", "Retry Failed for ${tx.id}", e)
                }
            }
            // Reload data after attempts
            delay(1000)
            loadData()
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

                 if (message.startsWith("Tx:")) {
                     val base64Tx = message.removePrefix("Tx:")
                     viewModelScope.launch {
                         _state.update { it.copy(p2pStatus = PeerStatus.TRANSFERRING) }
                         try {
                             if (!NetworkManager.isLiveMode.value) throw Exception("Cannot broadcast in Simulation Mode")

                             val rpcUrl = NetworkManager.activeRpcUrl.value
                             com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Broadcasting P2P Transaction via RPC...")
                             
                             var txSignature = ""
                             kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                 val json = """{"jsonrpc":"2.0", "id":1, "method":"sendTransaction", "params":["$base64Tx", {"encoding": "base64"}]}"""
                                 val response = postRpc(rpcUrl, json)
                                 
                                 // Check for error in response
                                 val jsonResp = org.json.JSONObject(response)
                                 if (jsonResp.has("error")) {
                                     throw Exception("RPC Broadcast Error: ${jsonResp.get("error")}")
                                 }
                                 txSignature = jsonResp.getString("result")
                             }
                             
                             com.solanasuper.utils.AppLogger.i("IncomeViewModel", "P2P Broadcast Success: $txSignature")
                             _state.update { it.copy(p2pStatus = PeerStatus.SUCCESS, status = UiStatus.Success) }
                             delay(2500)
                             loadData() // Refresh live balance
                             stopP2P()
                         } catch (e: Exception) {
                            com.solanasuper.utils.AppLogger.e("IncomeViewModel", "P2P Broadcast Failed", e)
                            // PERSISTENCE: Save for later retry
                            try {
                                val offlineTx = com.solanasuper.data.OfflineTransaction(
                                    id = "pending_${System.currentTimeMillis()}", // Temporary ID until broadcast
                                    amount = 0, // Unknown amount from just base64, unless we parse it. For now 0 is fine as placeholder.
                                    timestamp = System.currentTimeMillis(),
                                    recipientId = "Network",
                                    status = TransactionStatus.LOCKED_SYNCING,
                                    isLiveBroadcastPending = true,
                                    signedPayload = base64Tx
                                )
                                transactionDao.insert(offlineTx)
                                _state.update { it.copy(p2pStatus = PeerStatus.ERROR, status = UiStatus.Error("Broadcast Failed. Saved for retry.")) }
                            } catch (dbEx: Exception) {
                                com.solanasuper.utils.AppLogger.e("IncomeViewModel", "Failed to save pending broadcast", dbEx)
                                _state.update { it.copy(p2pStatus = PeerStatus.ERROR, status = UiStatus.Error("Critical: Broadcast & Save Failed.")) }
                            }
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
                     
                     // 1. Get Blockhash
                     val rpcUrl = NetworkManager.activeRpcUrl.value
                     val blockhashStr = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                         getLatestBlockhash(rpcUrl)
                     }
                     val blockhash = com.solanasuper.utils.Base58.decode(blockhashStr)

                     // 2. Build Message
                     val myAddress = identityKeyManager.getSolanaPublicKey() ?: throw Exception("No Identity")
                     val myPubkey = com.solanasuper.utils.Base58.decode(myAddress)
                     val toPubkey = com.solanasuper.utils.Base58.decode(recipientKey)
                     val lamports = (amount * 1_000_000_000).toLong()

                     val message = com.solanasuper.utils.SolanaUtil.createTransferMessage(
                         myPubkey, toPubkey, lamports, blockhash
                     )
                     
                     // 3. Prompt Sign (Re-use pendingTx flow logic? Or custom?)
                     // For P2P we'll do a direct sign request here to keep it self-contained or use channel
                     // NOTE: Using channel means UI must observe it. UI observes 'signRequest'.
                     
                     // We need to store who we are sending to for the callback to handle correctly?
                     // Actually, we can just use the channel and wait for the response? 
                     // No, the existing flow allows user scan -> sign.
                     // Let's use a specialized P2P pending state or just re-use the channel.
                     
                     // Helper: We need to know this signing request is for P2P, not broadcast.
                     // But broadcastTransaction() handles the "after sign" logic.
                     // We should Refactor broadcastTransaction to support P2P routing OR handle signing inline here if possible?
                     // Inline signing requires UI callback. The 'signRequest' channel is observed by UI to show BiometricPrompt.
                     // The Result comes back to 'broadcastTransaction'.
                     
                     // HACK for now: We will set 'pendingTransaction' with a special recipient "P2P:<EndpointID>"
                     // And in 'broadcastTransaction', we check if recipient starts with "P2P:" then we send Data instead of RPC.
                     
                     pendingTransaction = PendingTx("P2P:$endpointId", amount, message)
                     _signRequest.send(message)
                     
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
                        val history = fetchTransactionHistory(rpcUrl, solanaAddress)
                        com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Live History: ${history.size} items")

                        _state.update { 
                            it.copy(
                                balance = balanceSol,
                                transactions = history,
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
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
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
        com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Preparing transaction: $amount SOL to $recipient")
        viewModelScope.launch {
            try {
                // 1. Validate
                if (amount <= 0) throw Exception("Invalid amount")
                if (amount > (_state.value.balance ?: 0.0)) throw Exception("Insufficient funds")

                if (NetworkManager.isLiveMode.value) {
                     // 1. Get Blockhash
                     val rpcUrl = NetworkManager.activeRpcUrl.value
                     val blockhashStr = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                         getLatestBlockhash(rpcUrl)
                     }
                     val blockhash = com.solanasuper.utils.Base58.decode(blockhashStr)

                     // 2. Build Message
                     val myAddress = identityKeyManager.getSolanaPublicKey() ?: throw Exception("No Identity")
                     val myPubkey = com.solanasuper.utils.Base58.decode(myAddress)
                     val toPubkey = com.solanasuper.utils.Base58.decode(recipient)
                     val lamports = (amount * 1_000_000_000).toLong()

                     val message = com.solanasuper.utils.SolanaUtil.createTransferMessage(
                         myPubkey, toPubkey, lamports, blockhash
                     )
                     
                     pendingTransaction = PendingTx(recipient, amount, message)
                     _signRequest.send(message) // Prompt User to Sign
                } else {
                     // Simulation Fallback
                     val instruction = "Transfer ${amount} SOL to $recipient".toByteArray()
                     pendingTransaction = PendingTx(recipient, amount, instruction)
                     _signRequest.send(instruction)
                }
            } catch (e: Exception) {
                com.solanasuper.utils.AppLogger.e("IncomeViewModel", "Transaction preparation failed", e)
                _state.update { it.copy(status = UiStatus.Error(e.message ?: "Invalid Transaction")) }
            }
        }
    }

    fun broadcastTransaction(signature: ByteArray) {
        val tx = pendingTransaction ?: return
        com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Broadcasting signed transaction...")
        viewModelScope.launch {
            _state.update { it.copy(status = UiStatus.Loading) }
            try {
                 val rpcUrl = NetworkManager.activeRpcUrl.value
                 
                 if (NetworkManager.isLiveMode.value) {
                     // 3. Encode Final Transaction
                     val finalTxBytes = com.solanasuper.utils.SolanaUtil.encodeTransaction(signature, tx.data)
                     val finalTxBase64 = android.util.Base64.encodeToString(finalTxBytes, android.util.Base64.NO_WRAP)
                     
                     if (tx.recipient.startsWith("P2P:")) {
                         // P2P Route: Send Payload to Peer
                         val endpointId = tx.recipient.removePrefix("P2P:")
                         com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Routing P2P Tx to $endpointId")
                         
                         val payload = "Tx:$finalTxBase64".toByteArray()
                         p2pTransferManager.sendData(endpointId, payload)
                         
                         _state.update { it.copy(p2pStatus = PeerStatus.SUCCESS) }
                         delay(1000) // Brief delay
                     } else {
                         // Normal Route: RPC Broadcast
                         kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                             val json = """
                                 {"jsonrpc":"2.0", "id":1, "method":"sendTransaction", "params":["$finalTxBase64", {"encoding": "base64"}]}
                             """.trimIndent()
                             
                             val response = postRpc(rpcUrl, json)
                             val jsonObject = org.json.JSONObject(response)
                             if (jsonObject.has("error")) {
                                 throw Exception("RPC Error: ${jsonObject.get("error")}")
                             }
                             val txId = jsonObject.getString("result")
                             com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Transaction Sent: $txId")
                         }
                     }
                 } else {
                     com.solanasuper.utils.AppLogger.d("IncomeViewModel", "Simulation Mode: Locking funds locally")
                     delay(1000)
                     val success = transactionManager.lockFunds((tx.amount * 1_000_000_000).toLong())
                     if (!success) throw Exception("Insufficient Funds (Simulated)")
                 }

                 com.solanasuper.utils.AppLogger.i("IncomeViewModel", "Transaction successful!")
                 loadData()
                 _state.update { it.copy(status = UiStatus.Success) }
                 pendingTransaction = null
            } catch (e: Exception) {
                com.solanasuper.utils.AppLogger.e("IncomeViewModel", "Broadcast failed", e)
                _state.update { it.copy(status = UiStatus.Error("Broadcast Failed: ${e.message}")) }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val transactionManager: TransactionManager,
        private val transactionDao: TransactionDao,
        private val p2pTransferManager: com.solanasuper.network.P2PTransferManager,
        val identityKeyManager: IdentityKeyManager // Allow access in UI for signing helper
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IncomeViewModel(transactionManager, transactionDao, p2pTransferManager, identityKeyManager) as T
        }
    }
}
