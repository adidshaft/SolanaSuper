package com.solanasuper.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solanasuper.security.KeyManager
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HealthDatabaseTest {

    private lateinit var database: HealthDatabase
    private lateinit var dao: HealthDao
    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 0. Initialize SQLCipher libraries
        net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
        
        // 1. Initialize KeyManager to get the passphrase
        keyManager = KeyManager(context)
        val passphrase = keyManager.getOrGeneratePassphrase()
        assertNotNull("Passphrase should not be null", passphrase)
        assertTrue("Passphrase should satisfy 256-bit requirement", passphrase.size >= 32)
        
        // 2. Initialize SQLCipher-backed Room Database
        val factory = SupportFactory(passphrase)
        
        database = Room.inMemoryDatabaseBuilder(context, HealthDatabase::class.java)
            .openHelperFactory(factory) // Enforce encryption
            .build()
            
        dao = database.healthDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveEncryptedHealthRecord() {
        // Arrange
        val record = HealthEntity(
            id = "rec_001",
            timestamp = 1700000000000L,
            dataType = "HEART_RATE",
            encryptedPayload = "encrypted_blob_placeholder" // In real app, payload effectively encrypted by DB
        )

        // Act
        dao.insertHealthRecord(record)
        val retrieved = dao.getHealthRecord("rec_001")

        // Assert
        assertNotNull(retrieved)
        assertEquals("HEART_RATE", retrieved?.dataType)
        assertEquals(1700000000000L, retrieved?.timestamp)
    }
}
