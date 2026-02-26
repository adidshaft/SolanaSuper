package com.solanasuper.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solanasuper.R
import com.solanasuper.data.TransactionStatus
import com.solanasuper.data.WalletDatabase
import com.solanasuper.network.NetworkManager
import com.solanasuper.utils.AppLogger
import com.solanasuper.utils.SolanaUtil
import com.solanasuper.security.IdentityKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = WalletDatabase.getDatabase(applicationContext)
        val transactionDao = database.transactionDao()
        val identityManager = IdentityKeyManager(applicationContext)

        AppLogger.i("NetworkSyncWorker", "Checking for pending sync transactions...")

        // check network simplistic check (or rely on constraint)
        // We assume we are online if this runs with constraints.

        val pendingSync = transactionDao.getPendingSyncTransactions()
        if (pendingSync.isEmpty()) {
            return Result.success()
        }

        // We have pending commitments.
        // In a real app with "Biometric", we cannot sign here.
        // We must notify the user to open the app.
        
        notifyUserToSync(pendingSync.size)
        
        return Result.success()
    }

    private fun notifyUserToSync(count: Int) {
        val channelId = "sync_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sync", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Offline Payments Pending")
            .setContentText("You have $count pending P2P payments. Tap to sync.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
