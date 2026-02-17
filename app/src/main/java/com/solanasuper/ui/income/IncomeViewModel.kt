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
                _state.update { it.copy(p2pStatus = PeerStatus.TRANSFERRING) }
            }

            override fun onDataReceived(endpointId: String, data: ByteArray) {
                 viewModelScope.launch {
                    _state.update { it.copy(p2pStatus = PeerStatus.SUCCESS) }
                    delay(2500)
                    this@IncomeViewModel.loadData()
                    _state.update { it.copy(p2pStatus = PeerStatus.IDLE) }
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

    fun rejectConnection() {
        val endpointId = _state.value.p2pEndpointId ?: return
        p2pTransferManager.rejectConnection(endpointId)
        stopP2P()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(status = UiStatus.Loading) }
            try {
                if (!NetworkManager.isLiveMode.value) {
                    // Simulation Fallback
                    val balance = transactionDao.getAvailableBalance() ?: 0L
                    _state.update {
                        it.copy(
                            balance = balance / 1_000_000_000.0,
                            status = UiStatus.Success
                        )
                    }
                    return@launch
                }

                // On-Chain Truth
                val rpcUrl = NetworkManager.activeRpcUrl.value
                val solanaAddress = identityKeyManager.getSolanaPublicKey() ?: throw Exception("No Identity")

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // 1. Fetch Balance
                    val balanceSol = fetchSolanaBalance(rpcUrl, solanaAddress)
                    
                    // 2. Fetch History (Signatures)
                    val history = fetchTransactionHistory(rpcUrl, solanaAddress)

                    _state.update { 
                        it.copy(
                            balance = balanceSol,
                            transactions = history,
                            status = UiStatus.Success
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(status = UiStatus.Error(e.message ?: "Failed to load on-chain data")) }
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
        val json = """{"jsonrpc":"2.0", "id":1, "method":"getSignaturesForAddress", "params":["$address", {"limit": 10}]}"""
        val response = postRpc(rpcUrl, json)
        
        val jsonResponse = org.json.JSONObject(response)
        if (jsonResponse.has("error")) return emptyList()
        
        val resultArray = jsonResponse.getJSONArray("result")
        val list = mutableListOf<UiTransaction>()
        
        for (i in 0 until resultArray.length()) {
            val tx = resultArray.getJSONObject(i)
            val signature = tx.getString("signature")
            val blockTime = tx.optLong("blockTime", 0) * 1000 // sec to ms
            
            list.add(UiTransaction(
                id = signature,
                amount = 0.0,
                timestamp = blockTime,
                recipientId = "On-Chain",
                isReceived = true
            ))
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
        _state.update { it.copy(p2pStatus = PeerStatus.SCANNING, status = UiStatus.Idle) }
        p2pTransferManager.startDiscovery()
    }

    fun startReceiving() {
        if (_state.value.p2pStatus != PeerStatus.IDLE) return
        _state.update { it.copy(p2pStatus = PeerStatus.SCANNING, status = UiStatus.Idle) }
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
                     
                     // 4. Send to RPC
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
