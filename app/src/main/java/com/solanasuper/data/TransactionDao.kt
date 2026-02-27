package com.solanasuper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: OfflineTransaction): Long

    @androidx.room.Query("SELECT * FROM offline_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<OfflineTransaction>

    @androidx.room.Query("SELECT SUM(amount) FROM offline_transactions WHERE status = 'AVAILABLE'")
    suspend fun getAvailableBalance(): Long?

    @androidx.room.Query("SELECT * FROM offline_transactions WHERE isLiveBroadcastPending = 1")
    suspend fun getPendingBroadcasts(): List<OfflineTransaction>

    @androidx.room.Query("SELECT * FROM offline_transactions WHERE status IN ('PENDING_SYNC', 'SIGNED_OFFLINE')")
    suspend fun getPendingSyncTransactions(): List<OfflineTransaction>

    /** True offline signed transactions with a durable nonce — ready to broadcast. */
    @androidx.room.Query("SELECT * FROM offline_transactions WHERE status = 'SIGNED_OFFLINE'")
    suspend fun getSignedOfflineTxs(): List<OfflineTransaction>

    @androidx.room.Query("UPDATE offline_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: TransactionStatus)
}

