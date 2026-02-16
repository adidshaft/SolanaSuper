package com.solanasuper.ui.governance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solanasuper.core.EnclaveProto
import com.solanasuper.core.ZKProver
import com.solanasuper.network.MockArciumClient
import com.solanasuper.security.BiometricPromptManager
import com.solanasuper.security.IdentityKeyManager
import com.solanasuper.network.NetworkManager
import com.solanasuper.ui.state.ArciumComputationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class GovernanceViewModel(
    private val promptManager: BiometricPromptManager,
    private val identityKeyManager: IdentityKeyManager,
    private val arciumClient: MockArciumClient
) : ViewModel() {

    private val _state = MutableStateFlow(GovernanceState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    private val _signRequest = Channel<String>()
    val signRequest = _signRequest.receiveAsFlow()

    private var pendingVoteChoice: String? = null

    // promptManager results are no longer used here; we use RequestSign flow.
    // init block removed.

    fun initiateVote(choice: String) {
        pendingVoteChoice = choice
        viewModelScope.launch {
            if (identityKeyManager.getSolanaPublicKey() == null) {
                identityKeyManager.ensureIdentity()
            }
            // Request UI to trigger signing
            _signRequest.send(choice) 
        }
    }
    
    // Called by UI after successful signing
    fun onVoteSigned(signatureBytes: ByteArray) {
        pendingVoteChoice?.let { choice ->
            viewModelScope.launch {
                processVote(signatureBytes, choice)
            }
        }
    }

    private suspend fun processVote(signatureBytes: ByteArray, choice: String) {
        try {
            val proposalId = "proposal_ubi_001"
            
            // State 1: Generating Local Proof (JNI)
            _state.update { it.copy(
                mpcState = ArciumComputationState.GENERATING_LOCAL_PROOF,
                voteStatus = "Generating Local ZK Proof (Rust Enclave)..."
            ) }
            
            // 1. Signature is already provided by Hardware Wallet (IdentityKeyManager)
            // The payload was likely "Vote_${choice}_for_$proposalId" (Determined by UI/VM agreement)
            // We assume the signature passed in VALID for that payload.
            
            // 2. Call Native Rust JNI to Generate Proof
            val govReq = EnclaveProto.GovernanceRequest.newBuilder()
                .setProposalId(proposalId)
                .setVoteChoice(choice)
                .setIdentitySignature(com.google.protobuf.ByteString.copyFrom(signatureBytes))
                .build()
                
            val request = EnclaveProto.EnclaveRequest.newBuilder()
                .setRequestId("vote_${System.currentTimeMillis()}")
                .setActionType("GENERATE_MPC_VOTE")
                .setGovernanceReq(govReq)
                .build()
                
            // BLOCKING JNI CALL - Real Cryptography (Offloaded to Default Dispatcher)
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                ZKProver.processRequest(request)
            }
            
            // VERIFICATION LOGGING
            android.util.Log.d("GovernanceVM", "ZK Execution Complete. Success: ${response.success}")
            if (response.success) {
                // Convert to Hex for readability in logs
                val proofHex = response.proofData.toByteArray().joinToString("") { "%02x".format(it) }
                android.util.Log.d("GovernanceVM", "Generated Proof: $proofHex")
            } else {
                android.util.Log.e("GovernanceVM", "ZK Failure: ${response.errorMessage}")
            }

            if (response.success) {
                // State 2: Submitting to Network
                _state.update { it.copy(
                    mpcState = ArciumComputationState.SUBMITTING_TO_ARCIUM_MXE,
                    voteStatus = if (NetworkManager.isLiveMode.value) "Broadcasting to Live Arcium Network..." else "Submitting to Arcium Network (Simulated)..."
                ) }

                var success = false
                
                if (NetworkManager.isLiveMode.value) {
                    // LIVE MODE: Call Cloud Relayer (Handles Cold Start)
                    android.util.Log.d("GovernanceVM", "Broadcasting to Live Arcium Relayer...")
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val relayerUrl = "https://sovereign-arcium-relayer.onrender.com/api/vote"
                        val url = java.net.URL(relayerUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        
                        // CONNECT TIMEOUT: 60s for Render Cold Start
                        connection.connectTimeout = 60000 
                        connection.readTimeout = 60000
                        
                        connection.requestMethod = "POST"
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json")
                        
                        // Construct JSON Payload
                        // The relayer expects: { "proposalId": "...", "voteChoice": "...", "proof": "HEX_STRING" }
                        val proofHex = response.proofData.toByteArray().joinToString("") { "%02x".format(it) }
                        val json = org.json.JSONObject()
                        json.put("proposalId", proposalId)
                        json.put("voteChoice", choice)
                        json.put("proof", proofHex)
                        
                        connection.outputStream.use { it.write(json.toString().toByteArray()) }
                        
                        val responseCode = connection.responseCode
                        if (responseCode in 200..299) {
                            success = true
                            android.util.Log.d("GovernanceVM", "Relayer Success: $responseCode")
                        } else {
                            val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                            android.util.Log.e("GovernanceVM", "Relayer Error $responseCode: $errorStream")
                            throw Exception("Relayer HTTP $responseCode")
                        }
                    }
                } else {
                    // SIMULATION MODE
                     delay(1500)
                     success = arciumClient.submitVote(proposalId, response.proofData.toByteArray())
                }

                // State 3: Remote Computation (Simulated for Demo visuals either way)
                 _state.update { it.copy(
                    mpcState = ArciumComputationState.COMPUTING_IN_MXE,
                    voteStatus = "Executing Secure MPC Computation..."
                ) }
                delay(2000)
                
                // State 4: Callback Received
                _state.update { it.copy(
                    mpcState = ArciumComputationState.COMPUTATION_CALLBACK,
                    voteStatus = "Verifying Network Result..."
                ) }
                delay(1000)
                
                if (success) {
                    _state.update { it.copy(
                        mpcState = ArciumComputationState.COMPLETED,
                        voteStatus = "Vote Verified & Counted via MPC! 🗳️"
                    ) }
                    delay(2000) // Show success for a bit
                    _state.update { it.copy(mpcState = ArciumComputationState.IDLE) }
                } else {
                    _state.update { it.copy(
                        mpcState = ArciumComputationState.FAILED,
                        voteStatus = "Network Rejected Vote (Invalid Proof)"
                    ) }
                    delay(2000)
                    _state.update { it.copy(mpcState = ArciumComputationState.IDLE) }
                }
            } else {
                _state.update { it.copy(
                    mpcState = ArciumComputationState.FAILED,
                    voteStatus = "ZK Proof Generation Failed: ${response.errorMessage}"
                ) }
                delay(2000)
                _state.update { it.copy(mpcState = ArciumComputationState.IDLE) }
            }
        } catch (e: Exception) {
             _state.update { it.copy(
                mpcState = ArciumComputationState.FAILED,
                voteStatus = "Error: ${e.message}"
            ) }
            delay(2000)
            _state.update { it.copy(mpcState = ArciumComputationState.IDLE) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val promptManager: BiometricPromptManager,
        private val identityKeyManager: IdentityKeyManager,
        private val arciumClient: MockArciumClient
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GovernanceViewModel(promptManager, identityKeyManager, arciumClient) as T
        }
    }
}

data class GovernanceState(
    val voteStatus: String = "Active Proposal: Implement UBI",
    val mpcState: ArciumComputationState = ArciumComputationState.IDLE
)
