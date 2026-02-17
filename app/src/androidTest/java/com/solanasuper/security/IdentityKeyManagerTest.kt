package com.solanasuper.security

import android.security.keystore.UserNotAuthenticatedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdentityKeyManagerTest {

    @Test
    fun ensureIdentity_createsKey_safely() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val keyManager = IdentityKeyManager(context)
        
        // This should not throw
        keyManager.ensureIdentity()
        
        // Verify key exists
        val pubKey = keyManager.getSolanaPublicKey()
        if (pubKey == null) {
            fail("Public key should be generated")
        }
    }
}
