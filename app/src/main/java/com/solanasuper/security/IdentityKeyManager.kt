package com.solanasuper.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature

class IdentityKeyManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SolanaSuperIdentityKey"
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun generateIdentityKey(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE
        )
        
        // GREEN PHASE: Enforcing user authentication
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setUserAuthenticationRequired(true)
            //.setUserAuthenticationValidityDurationSeconds(-1) // Require auth for EVERY use (default, but explicit is good) -> Actually, -1 is default for keys requiring auth for every operation? No, if duration is not set, it requires auth for every operation (biometric). If duration is set, it allows reuse. We want strict biometric for signing.
            // For Signature/Cipher, usually need CryptoObject if duration is NOT set.
            // Let's stick to default (which implies BioPrompt per use).
            .build()

        keyPairGenerator.initialize(spec)
        val kp = keyPairGenerator.generateKeyPair()
        android.util.Log.d("SovereignLifeOS", "Identity Hardware Key Accessed/Generated Successfully. Public Key: ${android.util.Base64.encodeToString(kp.public.encoded, android.util.Base64.DEFAULT)}")
        return kp
    }

    fun signData(data: ByteArray): ByteArray {
        val signature = initSignature() ?: return ByteArray(0)
        signature.update(data)
        return signature.sign()
    }

    fun initSignature(): Signature? {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: return null
            
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(entry.privateKey)
        return signature
    }
    
    fun getPublicKey(): ByteArray? {
         val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
         return entry?.certificate?.publicKey?.encoded
    }
}
