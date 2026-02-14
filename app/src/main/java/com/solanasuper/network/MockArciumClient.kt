package com.solanasuper.network

import kotlinx.coroutines.delay

class MockArciumClient {

    suspend fun submitVote(proposalId: String, zkProof: ByteArray): Boolean {
        // Simulate network latency
        delay(1000)
        
        // In a real MPC network, this would verify the ZK proof is valid
        // and add the blinded vote share to the computation.
        // For simulation, we just check if proof is non-empty and starts with "mpc_" (from Rust)
        val proofStr = String(zkProof)
        return proofStr.startsWith("mpc_vote_share_for_$proposalId")
    }
}
