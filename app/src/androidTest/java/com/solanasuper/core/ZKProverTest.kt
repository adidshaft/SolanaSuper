package com.solanasuper.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZKProverTest {

    @Test
    fun processRequest_shouldCallNativeMethodAndReturnResponse() {
        // Arrange
        val request = EnclaveProto.EnclaveRequest.newBuilder()
            .setRequestId("req-123")
            .setActionType("GENERATE_PROOF")
            .setPayload(ByteString.copyFromUtf8("dummy_data"))
            .build()

        // Act
        // This runs on the emulator/device, so it should successfully load libsolanasuper_core.so
        val response = ZKProver.processRequest(request)
        
        // Assert
        assertEquals("req-123", response.requestId)
        assertEquals(true, response.success)
        assertEquals("", response.errorMessage)
        assertFalse(response.proofData.isEmpty)
    }
}
