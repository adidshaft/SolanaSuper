package com.solanasuper.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthRepository(private val healthDao: HealthDao) {

    suspend fun saveHealthRecord(record: HealthEntity) = withContext(Dispatchers.IO) {
        healthDao.insertHealthRecord(record)
    }

    suspend fun getHealthRecord(id: String): HealthEntity? = withContext(Dispatchers.IO) {
        healthDao.getHealthRecord(id)
    }

    suspend fun getAllRecords(): List<HealthEntity> = withContext(Dispatchers.IO) {
        healthDao.getAllHealthRecords()
    }
}
