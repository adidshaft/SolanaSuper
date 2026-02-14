package com.solanasuper.core

import com.google.protobuf.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ZKProverTest {

    @Test
    fun `processRequest should call native method and return response`() {
        // Arrange
        val request = EnclaveProto.EnclaveRequest.newBuilder()
            .setRequestId("req-123")
            .setActionType("GENERATE_PROOF")
            .setPayload(ByteString.copyFromUtf8("dummy_data"))
            .build()

        // Act
        val response = ZKProver.processRequest(request)
        
        // Assert
        assertEquals("req-123", response.requestId)
        assertEquals(true, response.success)
        assertEquals("", response.errorMessage)
        assertFalse(response.proofData.isEmpty)
    }
}
