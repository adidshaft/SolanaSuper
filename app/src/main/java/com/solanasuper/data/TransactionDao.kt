package com.solanasuper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: OfflineTransaction): Long

    // Simplified balance check: Total Initial Mock Balance - Sum of LOCKED/PENDING/CONFIRMED transactions
    // In a real app, you'd sync an on-chain balance and subtract pending.
    // For this prototype, we'll assume a hardcoded initial balance in the Manager or a separate Balance entity.
    @androidx.room.Query("SELECT * FROM offline_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<OfflineTransaction>

    @androidx.room.Query("SELECT SUM(amount) FROM offline_transactions WHERE status = 'AVAILABLE'")
    suspend fun getAvailableBalance(): Long?
}
