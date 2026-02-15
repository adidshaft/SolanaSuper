package com.solanasuper.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.io.File
import net.i2p.crypto.eddsa.KeyPairGenerator
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey

class IdentityKeyManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val IDENTITY_KEY_ALIAS = "SolanaSuperIdentityKey"
        private const val SOLANA_STORAGE_KEY_ALIAS = "SolanaSuperStorageKey"
        private const val SOLANA_PRIVATE_KEY_FILE = "solana_private_key.enc"
        private const val SOLANA_IV_FILE = "solana_iv.bin"
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun generateIdentityKey(): java.security.KeyPair {
        val keyPairGenerator = java.security.KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE
        )
        
        // Identity Key (Hardware, Biometric)
        val spec = KeyGenParameterSpec.Builder(
            IDENTITY_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setUserAuthenticationRequired(true)
            .build()

        keyPairGenerator.initialize(spec)
        val kp = keyPairGenerator.generateKeyPair()
        
        // Also ensure Solana Key is generated
        if (getSolanaPublicKey() == null) {
            generateAndStoreSolanaKey()
        }
        
        return kp
    }

    // --- Solana Logic ---

    fun ensureSolanaKey() {
        if (getSolanaPublicKey() == null) {
            generateAndStoreSolanaKey()
        }
    }

    private fun generateAndStoreSolanaKey() {
        try {
            // 1. Generate new Solana Keypair using EdDSA
            val kpg = KeyPairGenerator()
            val kp = kpg.generateKeyPair()
            val privateKey = kp.private as EdDSAPrivateKey
            val publicKey = kp.public as EdDSAPublicKey
            
            // 2. Encrypt Private Key with AES
            // Note: Storage Key DOES NOT require biometrics for encryption/decryption of the private key on disk.
            // This allows the app to "access" the key (e.g. for display or auto-signing if we wanted) without prompt.
            // However, for high-security actions, we should require the Identity Key (which is bio-bound).
            val secretKey = getOrCreateStorageKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(privateKey.seed)
            
            // 3. Store Encrypted Data
            File(context.filesDir, SOLANA_PRIVATE_KEY_FILE).writeBytes(encryptedBytes)
            File(context.filesDir, SOLANA_IV_FILE).writeBytes(iv)
            
            // 4. Store Public Key (Base58)
            val pubKeyBytes = publicKey.abyte
            val pubKeyString = com.solanasuper.utils.Base58.encode(pubKeyBytes)
            
            context.getSharedPreferences("solana_prefs", Context.MODE_PRIVATE)
                .edit().putString("solana_pubkey", pubKeyString).apply()

            android.util.Log.d("SolanaSuper", "Generated Identity: $pubKeyString")

        } catch (e: Exception) {
            android.util.Log.e("SolanaSuper", "Failed to generate Solana Key", e)
        }
    }
    
    fun getSolanaPublicKey(): String? {
        // Returns Base58 address
        return context.getSharedPreferences("solana_prefs", Context.MODE_PRIVATE)
            .getString("solana_pubkey", null)
    }

    fun getSolanaPrivateKeyCipher(mode: Int): Cipher? {
        val secretKey = keyStore.getKey(SOLANA_STORAGE_KEY_ALIAS, null) as? SecretKey ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        if (mode == Cipher.DECRYPT_MODE) {
            val ivFile = File(context.filesDir, SOLANA_IV_FILE)
            if (!ivFile.exists()) return null
            val iv = ivFile.readBytes()
            cipher.init(mode, secretKey, GCMParameterSpec(128, iv))
        } else {
            cipher.init(mode, secretKey)
        }
        return cipher
    }
    
    fun signSolanaTransaction(cipher: Cipher, transactionBytes: ByteArray): ByteArray {
        val encryptedBytes = File(context.filesDir, SOLANA_PRIVATE_KEY_FILE).readBytes()
        val privateKeySeed = cipher.doFinal(encryptedBytes)
        
        // Reconstruct Private Key
        val spec = net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable.getByName(net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable.ED_25519)
        val privKeySpec = net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec(privateKeySeed, spec)
        val privateKey = EdDSAPrivateKey(privKeySpec)
        
        // Sign
        val sgr = net.i2p.crypto.eddsa.EdDSAEngine(java.security.MessageDigest.getInstance(spec.hashAlgorithm))
        sgr.initSign(privateKey)
        sgr.update(transactionBytes)
        return sgr.sign()
    }

    private fun getOrCreateStorageKey(): SecretKey {
        return keyStore.getKey(SOLANA_STORAGE_KEY_ALIAS, null) as? SecretKey ?: run {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                SOLANA_STORAGE_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Removed: .setUserAuthenticationRequired(true) -> Allows silent background usage
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    // --- Identity Methods ---
    
    fun signData(data: ByteArray): ByteArray {
        val signature = initSignature() ?: return ByteArray(0)
        signature.update(data)
        return signature.sign()
    }

    fun initSignature(): Signature? {
        val entry = keyStore.getEntry(IDENTITY_KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: return null
            
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(entry.privateKey)
        return signature
    }
    
    fun getPublicKey(): ByteArray? {
         val entry = keyStore.getEntry(IDENTITY_KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
         return entry?.certificate?.publicKey?.encoded
    }
}
