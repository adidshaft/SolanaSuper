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
            // Ideally we insert a "Deposit" transaction, but our current system is mostly for spending.
            // Let's assume we can add funds via a back-door for this prototype.
            // Or just refresh.
            loadData()
        }
    }

    fun startP2P() {
        // Trigger P2P flow (handled by UI event usually)
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
