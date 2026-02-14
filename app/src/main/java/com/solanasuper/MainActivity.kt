
package com.solanasuper

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity // Better compatibility than FragmentActivity directly
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.solanasuper.network.MockArciumClient
import com.solanasuper.security.BiometricPromptManager
import com.solanasuper.security.IdentityKeyManager
import com.solanasuper.ui.MainNavigation
import com.solanasuper.data.WalletDatabase

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val promptManager = BiometricPromptManager(this)
        val identityKeyManager = IdentityKeyManager()
        val arciumClient = MockArciumClient()
        
        val database = WalletDatabase.getDatabase(this)
        val transactionDao = database.transactionDao()
        val transactionManager = com.solanasuper.p2p.TransactionManager(transactionDao)

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
                        transactionDao = transactionDao
                    )
                }
            }
        }
    }
}
