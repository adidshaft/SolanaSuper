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

import com.solanasuper.security.IdentityKeyManager
// import com.solana.networking.RpcClient // Uncomment if needed for real implementation
// import com.solana.networking.Networking // ...

class IncomeViewModel(
    private val transactionManager: TransactionManager,
    private val transactionDao: TransactionDao,
    private val p2pTransferManager: com.solanasuper.network.P2PTransferManager,
    private val identityKeyManager: IdentityKeyManager
) : ViewModel() {

    private val _state = MutableStateFlow(IncomeState())
    val state = _state.asStateFlow()

    init {
        setupP2P()
        loadData()
    }

    private fun setupP2P() {
        p2pTransferManager.callback = object : com.solanasuper.network.P2PTransferManager.P2PCallback {
            override fun onPeerFound(endpointId: String) {
                _state.update { it.copy(p2pStatus = P2PStatus.FOUND_PEER) }
            }

            override fun onConnectionInitiated(endpointId: String, info: com.google.android.gms.nearby.connection.ConnectionInfo) {
                _state.update { 
                    it.copy(
                        p2pStatus = P2PStatus.VERIFYING,
                        p2pPeerName = info.endpointName,
                        p2pAuthToken = info.authenticationToken,
                        p2pEndpointId = endpointId
                    ) 
                }
            }

            override fun onConnected(endpointId: String) {
                _state.update { it.copy(p2pStatus = P2PStatus.TRANSFERRING) } // Connected -> Transferring for simplicity
            }

            override fun onDataReceived(endpointId: String, data: ByteArray) {
                 viewModelScope.launch {
                    _state.update { it.copy(p2pStatus = P2PStatus.SUCCESS) }
                    delay(2500)
                    this@IncomeViewModel.loadData()
                    _state.update { it.copy(p2pStatus = P2PStatus.IDLE) }
                 }
            }

            override fun onError(message: String) {
                _state.update { it.copy(p2pStatus = P2PStatus.ERROR, error = message) }
            }

            override fun onDisconnected(endpointId: String) {
                _state.update { it.copy(p2pStatus = P2PStatus.IDLE, error = "Disconnected") }
            }
        }
    }

    fun confirmConnection() {
        val endpointId = _state.value.p2pEndpointId ?: return
        p2pTransferManager.acceptConnection(endpointId)
        _state.update { it.copy(p2pStatus = P2PStatus.CONNECTING) }
    }

    fun rejectConnection() {
        val endpointId = _state.value.p2pEndpointId ?: return
        p2pTransferManager.rejectConnection(endpointId)
        stopP2P()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val balance = transactionDao.getAvailableBalance() ?: 0L
                val transactions = transactionDao.getAllTransactions()
                
                _state.update { 
                    it.copy(
                        balance = balance.toDouble(),
                        transactions = transactions,
                        isLoading = false
                    ) 
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun claimUbi() {
        if (_state.value.isClaiming) return

        viewModelScope.launch {
            _state.update { it.copy(isClaiming = true, isLoading = true, error = null) }
            try {
                // 1. Get Solana Address
                val solanaAddress = identityKeyManager.getSolanaPublicKey()
                
                if (solanaAddress.isNullOrEmpty()) {
                    throw Exception("No identity found. Restart app.")
                }
                
                android.util.Log.d("SolanaSuper", "Requesting Airdrop for: $solanaAddress")

                 // 2. Request Airdrop (Devnet)
                kotlinx.coroutines.Dispatchers.IO.let { dispatcher ->
                    kotlinx.coroutines.withContext(dispatcher) {
                         if (NetworkManager.isLiveMode.value) {
                             // REAL SOLANA DEVNET POST
                             try {
                                 val url = java.net.URL("https://api.devnet.solana.com")
                                 val connection = url.openConnection() as java.net.HttpURLConnection
                                 connection.requestMethod = "POST"
                                 connection.doOutput = true
                                 connection.connectTimeout = 5000
                                 connection.readTimeout = 5000
                                 connection.setRequestProperty("Content-Type", "application/json")
                                 
                                 // Request 1 SOL (1,000,000,000 Lamports)
                                 val jsonBody = """
                                     {"jsonrpc":"2.0", "id":1, "method":"requestAirdrop", "params":["$solanaAddress", 1000000000]}
                                 """.trimIndent()
                                 
                                 connection.outputStream.use { it.write(jsonBody.toByteArray()) }
                                 
                                 val responseCode = connection.responseCode
                                 android.util.Log.d("SolanaSuper", "Airdrop HTTP $responseCode")
                                 
                                 if (responseCode == 200) {
                                     // Success!
                                     val response = connection.inputStream.bufferedReader().use { it.readText() }
                                     android.util.Log.d("SolanaSuper", "Airdrop Response: $response")
                                     // We could parse the signature here, but looking at balance is enough proof.
                                     
                                     // Wait a moment for confirmation
                                     delay(2000)
                                     
                                     // Update local balance to reflect "success" even if we don't query RPC balance yet
                                     // (We want immediate feedback)
                                     transactionManager.receiveFunds(1000L, "Devnet Airdrop")
                                     
                                 } else if (responseCode == 429) {
                                     throw Exception("Rate Limited (429). Try again later.")
                                 } else {
                                     throw Exception("Devnet Error (HTTP $responseCode)")
                                 }
                             } catch (e: Exception) {
                                  android.util.Log.e("SolanaSuper", "Airdrop Error", e)
                                  throw e
                             }
                         } else {
                             // SIMULATION
                             delay(1000)
                             transactionManager.receiveFunds(1000L, "Simulated Airdrop")
                         }
                    }
                }
                
                loadData()
            } catch (e: Exception) {
                val msg = if (e.message?.contains("429") == true) "Faucet Rate Limited. Try later." else e.message
                _state.update { it.copy(error = msg) }
            } finally {
                _state.update { it.copy(isClaiming = false, isLoading = false) }
            }
        }
    }

    fun startSending() {
        if (_state.value.p2pStatus != P2PStatus.IDLE) return
        _state.update { it.copy(p2pStatus = P2PStatus.SCANNING, error = null) }
        p2pTransferManager.startDiscovery()
    }

    fun startReceiving() {
        if (_state.value.p2pStatus != P2PStatus.IDLE) return
        _state.update { it.copy(p2pStatus = P2PStatus.SCANNING, error = null) }
        p2pTransferManager.startAdvertising()
    }

    fun stopP2P() {
        p2pTransferManager.stop()
        _state.update { it.copy(p2pStatus = P2PStatus.IDLE, error = null, p2pPeerName = null) }
    }

    override fun onCleared() {
        super.onCleared()
        p2pTransferManager.stop()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val transactionManager: TransactionManager,
        private val transactionDao: TransactionDao,
        private val p2pTransferManager: com.solanasuper.network.P2PTransferManager,
        private val identityKeyManager: IdentityKeyManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IncomeViewModel(transactionManager, transactionDao, p2pTransferManager, identityKeyManager) as T
        }
    }
}
