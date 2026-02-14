package com.solanasuper.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZKProverTest {

    @Test
    fun testProcessEnclaveRequest() {
        val request = EnclaveProto.EnclaveRequest.newBuilder()
            .setRequestId("req_123")
            .setActionType("TEST_ACTION")
            .setPayload(ByteString.copyFromUtf8("TestPayload"))
            .build()
            
        val response = ZKProver.processRequest(request)
        
        // Assertions
        assertNotNull(response)
        assertEquals("req_123", response.requestId)
        assertTrue(response.success)
        assertTrue(response.proofData.size() > 0)
    }

    @Test
    fun testIdentityRequest() {
        val identityReq = EnclaveProto.IdentityRequest.newBuilder()
            .setAttributeId("age_over_18")
            .setEncryptedIdentitySeed(ByteString.copyFromUtf8("signed_seed_bytes"))
            .build()
            
        val request = EnclaveProto.EnclaveRequest.newBuilder()
            .setRequestId("id_req_001")
            .setActionType("GENERATE_IDENTITY_PROOF")
            .setIdentityReq(identityReq)
            .build()
            
        val response = ZKProver.processRequest(request)
        
        assertNotNull(response)
        assertTrue(response.success)
        val proofStr = response.proofData.toStringUtf8()
        assertEquals("identity_proof_for_age_over_18", proofStr)
    }
}
