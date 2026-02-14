package com.solanasuper.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GovernanceTest {

    @Test
    fun testSecureVoteGeneration() {
        // Arrange
        val governanceReq = EnclaveProto.GovernanceRequest.newBuilder()
            .setProposalId("proposal_ubi_001")
            .setVoteChoice("YES")
            .setIdentitySignature(ByteString.copyFromUtf8("dummy_signature")) // In real flow, from IdentityKeyManager
            .build()
            
        val request = EnclaveProto.EnclaveRequest.newBuilder()
            .setRequestId("vote_req_101")
            .setActionType("GENERATE_MPC_VOTE")
            .setGovernanceReq(governanceReq)
            .build()
            
        // Act
        val response = ZKProver.processRequest(request)
        
        // Assert: EXPECT "mpc_vote_share_for_proposal_ubi_001"
        // Current implementation defaults to "mock_zk_proof_data", so this SHOULD FAIL (Red Phase)
        assertNotNull(response)
        assertTrue(response.success)
        
        val proofStr = response.proofData.toStringUtf8()
        assertEquals("mpc_vote_share_for_proposal_ubi_001", proofStr)
    }
}
