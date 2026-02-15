
package com.solanasuper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

class MainActivity : AppCompatActivity() {

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
        
        checkAndRequestPermissions()

        val promptManager = BiometricPromptManager(this)
        val identityKeyManager = IdentityKeyManager(this)
        val arciumClient = MockArciumClient()
        
        val database = WalletDatabase.getDatabase(this)
        val transactionDao = database.transactionDao()
        val transactionManager = com.solanasuper.p2p.TransactionManager(transactionDao)
        val p2pTransferManager = com.solanasuper.network.P2PTransferManager(this)

        androidx.activity.enableEdgeToEdge()
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
                        p2pTransferManager = p2pTransferManager
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val missing = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }
}
