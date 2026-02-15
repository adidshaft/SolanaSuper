package com.solanasuper.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solanasuper.data.TransactionDao
import com.solanasuper.data.TransactionStatus
import com.solanasuper.p2p.TransactionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IncomeViewModel(
    private val transactionManager: TransactionManager,
    private val transactionDao: TransactionDao,
    private val p2pTransferManager: com.solanasuper.network.P2PTransferManager
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
                _state.update { 
                    it.copy(
                        p2pStatus = P2PStatus.FOUND_PEER,
                        p2pPeerName = "Peer: $endpointId"
                    ) 
                }
            }

            override fun onDataReceived(endpointId: String, data: ByteArray) {
                _state.update { it.copy(p2pStatus = P2PStatus.TRANSFERRING) }
                // Handle actual data transfer logic here
                viewModelScope.launch {
                    delay(2000) // Simulate processing
                    _state.update { it.copy(p2pStatus = P2PStatus.SUCCESS) }
                    loadData()
                }
            }

            override fun onConnected(endpointId: String) {
                _state.update { it.copy(p2pStatus = P2PStatus.CONNECTED, p2pPeerName = "Connected to $endpointId") }
            }

            override fun onError(message: String) {
                _state.update { it.copy(error = message, p2pStatus = P2PStatus.ERROR) }
            }
        }
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
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(1000)
            transactionManager.receiveFunds(1000, "Global UBI")
            loadData()
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
        private val p2pTransferManager: com.solanasuper.network.P2PTransferManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IncomeViewModel(transactionManager, transactionDao, p2pTransferManager) as T
        }
    }
}
