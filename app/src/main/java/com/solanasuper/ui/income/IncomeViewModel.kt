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
    private val transactionDao: TransactionDao
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

    fun startP2P() {
        if (_state.value.p2pStatus != P2PStatus.IDLE) return

        viewModelScope.launch {
            _state.update { it.copy(p2pStatus = P2PStatus.SCANNING, error = null) }
            
            // Simulate scanning delay
            delay(2000)
            
            // Mock finding a peer
            _state.update { it.copy(p2pStatus = P2PStatus.FOUND_PEER, p2pPeerName = "Unknown Peer") }
            delay(1500)
            
            // Lock funds and start transfer
            val transferAmount = 10L // Hardcoded for demo
            val success = transactionManager.lockFunds(transferAmount)
            
            if (success) {
                _state.update { it.copy(p2pStatus = P2PStatus.TRANSFERRING) }
                delay(2000) // Sim ZK Proof gen
                
                _state.update { it.copy(p2pStatus = P2PStatus.SUCCESS) }
                delay(1500)
                
                // Refresh data and reset
                loadData()
                _state.update { it.copy(p2pStatus = P2PStatus.IDLE) }
            } else {
                _state.update { 
                    it.copy(
                        p2pStatus = P2PStatus.ERROR, 
                        error = "Insufficient funds for transfer"
                    ) 
                }
                delay(2000)
                _state.update { it.copy(p2pStatus = P2PStatus.IDLE) }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val transactionManager: TransactionManager,
        private val transactionDao: TransactionDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IncomeViewModel(transactionManager, transactionDao) as T
        }
    }
}
