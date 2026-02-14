package com.solanasuper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HealthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHealthRecord(record: HealthEntity)

    @Query("SELECT * FROM health_records WHERE id = :id")
    fun getHealthRecord(id: String): HealthEntity?
    
    @Query("SELECT * FROM health_records")
    fun getAllHealthRecords(): List<HealthEntity>
}
