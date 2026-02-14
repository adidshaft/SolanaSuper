
package com.solanasuper.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HealthRecord::class], version = 1, exportSchema = false)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao
}
