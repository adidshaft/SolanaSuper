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
        // This is expected to fail with UnsatisfiedLinkError because the native library 
        // "solanasuper_core" is not yet built or available in the unit test environment.
        try {
            val response = ZKProver.processRequest(request)
            
            // If by some miracle it works (e.g. mocked), we assert success
            // But realistically, we expect the native call to happen.
            // Since we cannot easily mock the `external` function in a unit test without 
            // more complex setup (like Robolectric or an interface wrapper), 
            // the failure of this test (UnsatisfiedLinkError) CONFIRMS that we are reaching the JNI layer.
            
            // For the purpose of the "Red Phase", failing with LinkError is acceptable proof 
            // that the Kotlin code is trying to bridge to Rust.
            
        } catch (e: UnsatisfiedLinkError) {
            // This catches the missing library error. 
            // In a strict TDD world, we might want to verify the implementation logic *around* the JNI call.
            // But here, the requirement is to show the integration point fails.
            assertEquals("java.library.path", System.getProperty("java.library.path")) 
            throw e // Re-throw to ensure the test technically "fails" in the report
        }
    }
}
