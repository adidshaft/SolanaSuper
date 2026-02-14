package com.solanasuper.ui.governance

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.solanasuper.core.EnclaveProto
import com.solanasuper.core.ZKProver
import com.solanasuper.network.MockArciumClient
import com.solanasuper.security.BiometricPromptManager
import com.solanasuper.security.IdentityKeyManager
import kotlinx.coroutines.launch

@Composable
fun GovernanceScreen(
    promptManager: BiometricPromptManager,
    identityKeyManager: IdentityKeyManager,
    arciumClient: MockArciumClient = MockArciumClient()
) {
    var voteStatus by remember { mutableStateOf("Active Proposal: Implement UBI") }
    var selectedVote by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(promptManager) {
        promptManager.promptResults.collect { result ->
            when (result) {
                is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                    voteStatus = "Auth Error: ${result.error}"
                }
                BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                    voteStatus = "Auth Failed"
                }
                is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                    val cryptoObject = result.result.cryptoObject
                    if (cryptoObject?.signature != null && selectedVote != null) {
                        try {
                            val signature = cryptoObject.signature!!
                            val proposalId = "proposal_ubi_001"
                            
                            // 1. Sign Vote Payload (Hardware Key)
                            val votePayload = "Vote_${selectedVote}_for_$proposalId".toByteArray()
                            signature.update(votePayload)
                            val signedBytes = signature.sign()
                            
                            voteStatus = "Generating Zero-Knowledge Proof..."
                            
                            // 2. Generate ZK MPC Proof (Rust Enclave)
                            scope.launch {
                                try {
                                    val govReq = EnclaveProto.GovernanceRequest.newBuilder()
                                        .setProposalId(proposalId)
                                        .setVoteChoice(selectedVote)
                                        .setIdentitySignature(com.google.protobuf.ByteString.copyFrom(signedBytes))
                                        .build()
                                        
                                    val request = EnclaveProto.EnclaveRequest.newBuilder()
                                        .setRequestId("vote_${System.currentTimeMillis()}")
                                        .setActionType("GENERATE_MPC_VOTE")
                                        .setGovernanceReq(govReq)
                                        .build()
                                        
                                    val response = ZKProver.processRequest(request)
                                    
                                    if (response.success) {
                                        voteStatus = "Submitting to Arcium Network..."
                                        // 3. Submit to Arcium (Mock)
                                        val success = arciumClient.submitVote(proposalId, response.proofData.toByteArray())
                                        if (success) {
                                            voteStatus = "Vote Verified & Counted via MPC! 🗳️"
                                        } else {
                                            voteStatus = "Network Rejected Vote (Invalid Proof)"
                                        }
                                    } else {
                                        voteStatus = "ZK Proof Generation Failed: ${response.errorMessage}"
                                    }
                                } catch (e: Exception) {
                                    voteStatus = "Error: ${e.message}"
                                }
                            }
                        } catch (e: Exception) {
                            voteStatus = "Signing Failed: ${e.message}"
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Democracy Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Proposal #1: Universal Basic Income", style = MaterialTheme.typography.titleMedium)
                Text("Should the network distribute 10 SOL monthly to verified humans?", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                             selectedVote = "YES"
                             initiateVote(identityKeyManager, promptManager) { err -> voteStatus = err }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("VOTE YES")
                    }
                    
                    Button(
                        onClick = {
                             selectedVote = "NO"
                             initiateVote(identityKeyManager, promptManager) { err -> voteStatus = err }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ) {
                        Text("VOTE NO")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(voteStatus, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun initiateVote(
    keyManager: IdentityKeyManager,
    promptManager: BiometricPromptManager,
    onError: (String) -> Unit
) {
    try {
        if (keyManager.getPublicKey() == null) {
            keyManager.generateIdentityKey()
        }
        val signature = keyManager.initSignature()
        if (signature != null) {
            promptManager.showBiometricPrompt(
                title = "Confirm Vote",
                description = "Sign your vote with your secure identity",
                cryptoObject = BiometricPrompt.CryptoObject(signature)
            )
        } else {
            onError("Could not initialize secure signature")
        }
    } catch (e: Exception) {
        onError("Error: ${e.message}")
    }
}
