package com.solanasuper.p2p

import com.solanasuper.data.OfflineTransaction
import com.solanasuper.data.TransactionDao
import com.solanasuper.data.TransactionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class TransactionManagerTest {

    @Mock
    lateinit var transactionDao: TransactionDao

    private lateinit var transactionManager: TransactionManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        transactionManager = TransactionManager(transactionDao)
    }

    @Test
    fun `lockFunds should create locked transaction and deduct balance`() {
        runBlocking {
            // Arrange
            val amount = 500L
            val currentBalance = 1000L
            `when`(transactionDao.getAvailableBalance()).thenReturn(currentBalance)

            // Act
            val result = transactionManager.lockFunds(amount)

            // Assert
            assertEquals(true, result)
            verify(transactionDao).insert(
                org.mockito.kotlin.argThat { 
                    status == TransactionStatus.LOCKED_PENDING_P2P && this.amount == amount 
                }
            )
        }
    }

    @Test
    fun `lockFunds should return false if insufficient funds`() {
        runBlocking {
            // Arrange
            val amount = 1500L
            val currentBalance = 1000L
            `when`(transactionDao.getAvailableBalance()).thenReturn(currentBalance)

            // Act
            val result = transactionManager.lockFunds(amount)

            // Assert
            assertEquals(false, result)
        }
    }
}
