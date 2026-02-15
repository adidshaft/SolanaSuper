package com.solanasuper.data

import kotlinx.coroutines.flow.Flow

class ActivityRepository(private val activityLogDao: ActivityLogDao) {
    
    val allActivities: Flow<List<ActivityLogEntity>> = activityLogDao.getAllActivities()

    suspend fun logActivity(type: ActivityType, hash: String) {
        val activity = ActivityLogEntity(
            timestamp = System.currentTimeMillis(),
            type = type,
            hashValue = hash
        )
        activityLogDao.insertActivity(activity)
    }
    
    suspend fun clearHistory() {
        activityLogDao.clearAll()
    }
}
