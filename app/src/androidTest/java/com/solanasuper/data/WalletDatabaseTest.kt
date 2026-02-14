package com.solanasuper.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletDatabaseTest {

    private lateinit var db: WalletDatabase
    private lateinit var dao: WalletDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java).build()
        dao = db.walletDao()

        // Seed initial balance: 100 SOL
        runBlocking {
            dao.initializeBalance(100L)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun lockFunds_shouldSucceed_whenBalanceIsSufficient() = runBlocking {
        // Arrange
        val amountToLock = 50L
        val txId = "tx_001"

        // Act
        val success = dao.lockFunds(txId, amountToLock)

        // Assert
        if (!success) fail("Failed to lock funds despite sufficient balance")
        
        val record = dao.getTransaction(txId)
        assertNotNull(record)
        assertEquals(TransactionStatus.LOCKED_PENDING_P2P, record?.status)
        
        // Verify remaining balance (Available)
        // 100 - 50 = 50 Available
        val available = dao.getAvailableBalance()
        assertEquals(50L, available)
    }

    @Test
    fun lockFunds_shouldFail_whenBalanceIsInsufficient() = runBlocking {
        // Arrange
        val amountToLock = 150L // More than 100
        val txId = "tx_fail"

        // Act
        try {
            val success = dao.lockFunds(txId, amountToLock)
            if (success) fail("Locked funds despite insufficient balance!")
        } catch (e: Exception) {
            // Success if it throws (or returns false, depending on implementation preference)
            // For this test, we accept false as failure too.
            // But if it returns true, we fail.
        }
        
        // Assert
        val record = dao.getTransaction(txId)
        // Should use explicit null check or status check
        if (record != null && record.status != TransactionStatus.FAILED) {
             fail("Transaction should not exist or be marked FAILED")
        }
    }

    @Test
    fun doubleSpend_shouldFail() = runBlocking {
        // Arrange
        dao.initializeBalance(10L)
        
        // Act 1: Lock 10 (Succeeds)
        val success1 = dao.lockFunds("tx_1", 10L)
        if (!success1) fail("First lock failed")

        // Act 2: Lock 5 (Should Fail - 0 available)
        val success2 = dao.lockFunds("tx_2", 5L)
        
        // Assert
        if (success2) fail("Double spend allowed! Second lock succeeded.")
    }
}
