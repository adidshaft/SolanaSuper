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

    suspend fun receiveFunds(amount: Long, source: String = "UBI_CLAIM") {
        val newTransaction = OfflineTransaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            timestamp = System.currentTimeMillis(),
            recipientId = "SELF",
            status = TransactionStatus.AVAILABLE
            // Note: We might need a type field or use recipientId to distinguish
        )
        transactionDao.insert(newTransaction)
        android.util.Log.d("SovereignLifeOS", "WalletDao Transaction: UBI/Funds Successfully Added to Local DB. Amount: $amount, Source: $source")
    }

    suspend fun sendFunds(amount: Long, recipient: String): Boolean {
        val currentBalance = transactionDao.getAvailableBalance() ?: 0L
        if (currentBalance < amount) {
            return false
        }

        val newTransaction = OfflineTransaction(
            id = UUID.randomUUID().toString(),
            amount = -amount, // Negative for sending
            timestamp = System.currentTimeMillis(),
            recipientId = recipient,
            status = TransactionStatus.AVAILABLE
        )
        transactionDao.insert(newTransaction)
        return true
    }
}
