package com.solanasuper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface InvestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: InvestPosition): Long

    @Update
    suspend fun update(position: InvestPosition)

    @Query("SELECT * FROM invest_positions ORDER BY openTimestamp DESC")
    suspend fun getAllPositions(): List<InvestPosition>

    @Query("SELECT * FROM invest_positions WHERE status = 'OPEN' ORDER BY openTimestamp DESC")
    suspend fun getOpenPositions(): List<InvestPosition>

    @Query("SELECT * FROM invest_positions WHERE type = 'STAKE' AND status = 'OPEN'")
    suspend fun getActiveStakes(): List<InvestPosition>

    @Query("SELECT * FROM invest_positions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InvestPosition?

    @Query("SELECT SUM(collatLamports) FROM invest_positions WHERE status = 'OPEN'")
    suspend fun getTotalLockedLamports(): Long?

    @Query("SELECT SUM(realizedPnlUsd) FROM invest_positions WHERE status = 'CLOSED'")
    suspend fun getTotalRealizedPnl(): Double?
}
