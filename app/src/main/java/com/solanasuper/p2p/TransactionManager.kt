package com.solanasuper.p2p

import com.solanasuper.data.OfflineTransaction
import com.solanasuper.data.TransactionDao
import com.solanasuper.data.TransactionStatus

class TransactionManager(private val transactionDao: TransactionDao) {

    suspend fun lockFunds(amount: Long) {
        val currentBalance = transactionDao.getBalance()
        
        if (currentBalance < amount) {
            throw IllegalStateException("Insufficient funds")
        }

        val newTransaction = OfflineTransaction(
            amount = amount,
            timestamp = System.currentTimeMillis(),
            recipientId = null, // Not known yet at lock time
            status = TransactionStatus.LOCKED
        )

        transactionDao.insert(newTransaction)
    }
}
