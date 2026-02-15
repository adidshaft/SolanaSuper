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
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // In a real app, use Flow from Room. Here we poll or fetch once.
                val balance = transactionDao.getAvailableBalance() ?: 0L
                val transactions = transactionDao.getAllTransactions() // Need to add this to DAO
                
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
            // Mock UBI Claim
            _state.update { it.copy(isLoading = true) }
            delay(1000)
            
            // Insert Deposit
            transactionManager.receiveFunds(1000, "Global UBI")
            
            loadData()
        }
    }

    fun startSending() {
        // Role: Discoverer (Sender)
        if (_state.value.p2pStatus != P2PStatus.IDLE) return

        viewModelScope.launch {
            _state.update { it.copy(p2pStatus = P2PStatus.SCANNING, error = null) }
            p2pTransferManager.startDiscovery()
            
            // For prototype, we might want to listen to callbacks from P2PTransferManager to update state
            // But relying on the existing simulated logic for now, or we can wire it up fully.
            // The prompt asks to "trigger ConnectionsClient.startDiscovery(...)".
            // We should keep the simulated UI flow for now unless we fully integrate callbacks.
            // But better to at least call the real manager.
        }
    }

    fun startReceiving() {
        // Role: Advertiser (Receiver)
        if (_state.value.p2pStatus != P2PStatus.IDLE) return

        viewModelScope.launch {
            _state.update { it.copy(p2pStatus = P2PStatus.SCANNING, error = null) } // "Scanning" as generic "Waiting" state
            p2pTransferManager.startAdvertising()
        }
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
