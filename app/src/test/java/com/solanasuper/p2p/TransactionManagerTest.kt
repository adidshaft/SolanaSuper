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
    fun `lockFunds should create locked transaction and deduct balance`() = runBlocking {
        // Arrange
        val amount = 500L
        val currentBalance = 1000L
        `when`(transactionDao.getBalance()).thenReturn(currentBalance)

        // Act
        transactionManager.lockFunds(amount)

        // Assert
        verify(transactionDao).insert(
            org.mockito.kotlin.argThat { 
                status == TransactionStatus.LOCKED && this.amount == amount 
            }
        )
    }

    @Test
    fun `lockFunds should throw exception if insufficient funds`() = runBlocking {
        // Arrange
        val amount = 1500L
        val currentBalance = 1000L
        `when`(transactionDao.getBalance()).thenReturn(currentBalance)

        // Act & Assert
        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                transactionManager.lockFunds(amount)
            }
        }
        assertEquals("Insufficient funds", exception.message)
    }
}
