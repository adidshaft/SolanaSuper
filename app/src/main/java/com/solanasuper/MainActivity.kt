
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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val promptManager = BiometricPromptManager(this)
        val identityKeyManager = IdentityKeyManager(this)
        val arciumClient = MockArciumClient()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        promptManager = promptManager,
                        identityKeyManager = identityKeyManager,
                        arciumClient = arciumClient
                    )
                }
            }
        }
    }
}
