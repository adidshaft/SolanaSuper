package com.solanasuper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WalletDao {

    @Query("SELECT availableBalance FROM wallet_balance WHERE id = 1")
    suspend fun getAvailableBalance(): Long?

    @Query("UPDATE wallet_balance SET availableBalance = :newBalance WHERE id = 1")
    suspend fun updateBalance(newBalance: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun initBalance(balance: WalletBalance)

    suspend fun initializeBalance(amount: Long) {
        initBalance(WalletBalance(availableBalance = amount))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: OfflineTransaction)

    @Query("SELECT * FROM offline_transactions WHERE id = :txId")
    suspend fun getTransaction(txId: String): OfflineTransaction?

    @Transaction // Critical: Atomic Lock
    suspend fun lockFunds(txId: String, amount: Long): Boolean {
        val currentBalance = getAvailableBalance() ?: 0L
        
        if (currentBalance >= amount) {
            val newBalance = currentBalance - amount
            updateBalance(newBalance)
            
            val tx = OfflineTransaction(
                id = txId,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                recipientId = null, // Can be updated later
                status = TransactionStatus.LOCKED_PENDING_P2P
            )
            insertTransaction(tx)
            return true
        } else {
            // Insufficient funds
            // Optionally log a failed attempt
            return false
        }
    }
}
