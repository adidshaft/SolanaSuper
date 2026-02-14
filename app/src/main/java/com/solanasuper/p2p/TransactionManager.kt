package com.solanasuper.p2p

import com.solanasuper.data.OfflineTransaction
import com.solanasuper.data.TransactionDao
import com.solanasuper.data.TransactionStatus

import java.util.UUID

class TransactionManager(private val transactionDao: TransactionDao) {

    suspend fun lockFunds(amount: Long): Boolean {
        val currentBalance = transactionDao.getAvailableBalance() ?: 0L
        
        if (currentBalance < amount) {
            return false // Return false instead of throwing for cleaner UI handling
        }

        val newTransaction = OfflineTransaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            timestamp = System.currentTimeMillis(),
            recipientId = null, // Not known yet at lock time
            status = TransactionStatus.LOCKED_PENDING_P2P
        )

        transactionDao.insert(newTransaction)
        return true
    }
}
