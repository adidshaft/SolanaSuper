
package com.solanasuper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_records WHERE id = :id")
    fun getRecord(id: String): HealthRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: HealthRecord)
    
    @Query("SELECT * FROM health_records")
    fun getAllRecords(): List<HealthRecord>
}
