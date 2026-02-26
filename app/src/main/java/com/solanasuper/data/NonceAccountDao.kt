package com.solanasuper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NonceAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: NonceAccount)

    @Query("SELECT * FROM nonce_accounts WHERE id = 1 LIMIT 1")
    suspend fun get(): NonceAccount?

    @Query("UPDATE nonce_accounts SET currentNonce = :nonce, lastRefreshedMs = :ts WHERE id = 1")
    suspend fun updateNonce(nonce: String, ts: Long = System.currentTimeMillis())

    @Query("UPDATE nonce_accounts SET isValid = :valid WHERE id = 1")
    suspend fun setValid(valid: Boolean)

    @Query("DELETE FROM nonce_accounts WHERE id = 1")
    suspend fun delete()
}
