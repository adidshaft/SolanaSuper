package com.solanasuper.security

import android.security.keystore.UserNotAuthenticatedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdentityKeyManagerTest {

    @Test
    fun generateKey_shouldRequireUserAuthentication() {
        val keyManager = IdentityKeyManager()
        keyManager.generateIdentityKey()
        
        val dataToSign = "IdentityProof".toByteArray()

        try {
            // This should fail with UserNotAuthenticatedException if properly configured
            keyManager.signData(dataToSign)
            
            // If we reach here, it means the key was used WITHOUT authentication -> FAIL
            fail("Key usage should require user authentication, but it succeeded without it.")
        } catch (e: UserNotAuthenticatedException) {
            // Expected
        } catch (e: Exception) {
             // If it's the specific android.security.keystore exception (which might be wrapped), check hierarchy
             if (e::class.java.name.contains("UserNotAuthenticatedException")) {
                 return // Success
             }
             // Otherwise, rethrow or fail if it's the wrong exception
             // Note: In older Android versions or depending on implementation, 
             // it might throw SignatureException caused by UserNotAuthenticatedException
             var cause = e.cause
             while (cause != null) {
                 if (cause::class.java.name.contains("UserNotAuthenticatedException")) {
                     return // Success
                 }
                 cause = cause.cause
             }
             // If we're here, it wasn't the expected exception
             // However, for the RED phase, we EXPECT this block to NOT catch the right exception 
             // because the key is NOT secure yet.
             // But wait, if signData SUCCEEDS (no exception), we fail via the `fail()` call above.
             throw e
        } catch (e: java.security.InvalidAlgorithmParameterException) {
            // SUCCESS (kind of): On an emulator without enrolled biometrics, 
            // generating a key with setUserAuthenticationRequired(true) fails with this exception.
            // This proves we DID set the requirement.
            // If we hadn't set it, generation would succeed (which would be a failure for this test).
            return
        } catch (e: java.lang.IllegalStateException) {
            // Also possible on some API levels if no secure lock screen
            return
        }
    }
}
