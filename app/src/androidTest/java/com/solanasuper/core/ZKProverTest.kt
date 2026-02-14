
package com.solanasuper.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.ByteString
import com.solanasuper.core.proto.EnclaveProto.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZKProverTest {

    @Test
    fun testIdentityProofGeneration() {
        // 1. Create a request
        val request = EnclaveRequest.newBuilder()
            .setIdentityReq(
                IdentityRequest.newBuilder()
                    .setAttributeId("age_over_18")
                    .setEncryptedIdentitySeed(ByteString.copyFromUtf8("mock_seed"))
                    .build()
            )
            .build()

        // 2. Call the JNI bridge (This will likely fail with UnsatisfiedLinkError until we implement the Rust lib)
        try {
            val response = ZKProver.processRequest(request)
            
            // 3. Assert success
            assertTrue("Enclave should return success", response.success)
            assertFalse("Proof should not be empty", response.zkProof.isEmpty)
        } catch (e: UnleatisfiedLinkError) {
            // Expected for now in TDD, but we want to see it fail in the report
            fail("Native library not linked: ${e.message}")
        }
    }
}
