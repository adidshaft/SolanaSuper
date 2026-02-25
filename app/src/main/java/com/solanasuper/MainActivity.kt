
package com.solanasuper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.solanasuper.network.MockArciumClient
import com.solanasuper.security.BiometricPromptManager
import com.solanasuper.security.IdentityKeyManager
import com.solanasuper.ui.MainNavigation
import com.solanasuper.data.WalletDatabase
import java.security.Security
import net.i2p.crypto.eddsa.EdDSASecurityProvider

class MainActivity : FragmentActivity() {

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            Log.d("SovereignLifeOS", "All permissions granted")
        } else {
            Log.e("SovereignLifeOS", "Permissions denied - Nearby Connections will not work")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register EdDSA Provider for IdentityKeyManager
        Security.addProvider(EdDSASecurityProvider())
        
        checkAndRequestPermissions()

        val promptManager = BiometricPromptManager(this)
        val identityKeyManager = IdentityKeyManager(this)
        val arciumClient = MockArciumClient()
        
        val database = WalletDatabase.getDatabase(this)
        val transactionDao = database.transactionDao()
        val activityLogDao = database.activityLogDao()
        val activityRepository = com.solanasuper.data.ActivityRepository(activityLogDao)
        val investDao = database.investDao()
        val nonceAccountDao = database.nonceAccountDao()
        
        // Initialize Health Persistence
        val healthDatabase = com.solanasuper.data.HealthDatabase.getDatabase(this)
        val healthRepository = com.solanasuper.data.HealthRepository(healthDatabase.healthDao())
        
        // Ensure Identity is Generated on First Launch (Silent)
        identityKeyManager.ensureIdentity()
        
        val transactionManager = com.solanasuper.p2p.TransactionManager(transactionDao)
        val p2pTransferManager = com.solanasuper.network.P2PTransferManager(this)

        // Enqueue Network Sync Worker
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.solanasuper.worker.NetworkSyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES // Minimum interval
        )
            .setConstraints(constraints)
            .build()
            
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NetworkSyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        promptManager = promptManager,
                        identityKeyManager = identityKeyManager,
                        arciumClient = arciumClient,
                        transactionManager = transactionManager,
                        transactionDao = transactionDao,
                        p2pTransferManager = p2pTransferManager,
                        activityRepository = activityRepository,
                        healthRepository = healthRepository,
                        investDao = investDao,
                        nonceAccountDao = nonceAccountDao
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // Add Notification Permission for Android 13+
        val allPermissions = if (Build.VERSION.SDK_INT >= 33) {
            REQUIRED_PERMISSIONS + Manifest.permission.POST_NOTIFICATIONS
        } else {
            REQUIRED_PERMISSIONS
        }

        val missing = allPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            Log.d("SovereignLifeOS", "All permissions granted")
        }
    }
}
