
package com.solanasuper.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SolanaSuperHealthKey"
        private const val ENCRYPTED_PASS_KEY = "encrypted_pass"
        private const val IV_KEY = "iv"
    }

    fun getOrGeneratePassphrase(): ByteArray {
        val encryptedPass = prefs.getString(ENCRYPTED_PASS_KEY, null)
        val iv = prefs.getString(IV_KEY, null)

        return if (encryptedPass != null && iv != null) {
            decryptPassphrase(encryptedPass, iv)
        } else {
            generateAndStorePassphrase()
        }
    }

    private fun generateAndStorePassphrase(): ByteArray {
        // 1. Generate 256-bit random passphrase
        val passphrase = ByteArray(32)
        SecureRandom().nextBytes(passphrase)

        // 2. Encrypt with KeyStore
        val cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeyStoreKey())
        
        val encryptedBytes = cipher.doFinal(passphrase)
        val iv = cipher.iv

        // 3. Store
        prefs.edit()
            .putString(ENCRYPTED_PASS_KEY, Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
            .putString(IV_KEY, Base64.encodeToString(iv, Base64.DEFAULT))
            .apply()

        return passphrase
    }

    private fun decryptPassphrase(encryptedBase64: String, ivBase64: String): ByteArray {
        val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
        val iv = Base64.decode(ivBase64, Base64.DEFAULT)

        val cipher = getCipher()
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeyStoreKey(), spec)

        return cipher.doFinal(encryptedBytes)
    }

    private fun getOrCreateKeyStoreKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            return entry?.secretKey ?: generateKeyStoreKey()
        }
        return generateKeyStoreKey()
    }

    private fun generateKeyStoreKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(false) // We verify IV manually for GCM
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance("AES/GCM/NoPadding")
    }
}
