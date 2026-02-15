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

    private var pendingVoteChoice: String? = null

    init {
        // Collect biometric results for signing
        viewModelScope.launch {
            promptManager.promptResults.collectLatest { result ->
                when (result) {
                    is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                        val cryptoObject = result.result.cryptoObject
                        if (cryptoObject?.signature != null && pendingVoteChoice != null) {
                            processVote(cryptoObject.signature!!, pendingVoteChoice!!)
                        }
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                        _state.update { it.copy(voteStatus = "Auth Error: ${result.error}") }
                    }
                    BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                        _state.update { it.copy(voteStatus = "Auth Failed") }
                    }
                }
            }
        }
    }

    fun initiateVote(choice: String) {
        pendingVoteChoice = choice
        try {
            if (identityKeyManager.getPublicKey() == null) {
                identityKeyManager.generateIdentityKey()
            }
            val signature = identityKeyManager.initSignature()
            if (signature != null) {
                promptManager.showBiometricPrompt(
                    title = "Confirm Vote",
                    description = "Sign your vote with your secure identity",
                    cryptoObject = androidx.biometric.BiometricPrompt.CryptoObject(signature)
                )
            } else {
                _state.update { it.copy(voteStatus = "Could not initialize secure signature") }
            }
        } catch (e: Exception) {
            _state.update { it.copy(voteStatus = "Error: ${e.message}") }
        }
    }

    private suspend fun processVote(signature: java.security.Signature, choice: String) {
        try {
            val proposalId = "proposal_ubi_001"
            
            // State 1: Generating Local Proof (JNI)
            _state.update { it.copy(
                mpcState = ArciumComputationState.GENERATING_LOCAL_PROOF,
                voteStatus = "Generating Local ZK Proof (Rust Enclave)..."
            ) }
            
            // 1. Sign Vote Payload (Hardware Key)
            val votePayload = "Vote_${choice}_for_$proposalId".toByteArray()
            signature.update(votePayload)
            val signedBytes = signature.sign()

            // 2. Call Native Rust JNI to Generate Proof
            val govReq = EnclaveProto.GovernanceRequest.newBuilder()
                .setProposalId(proposalId)
                .setVoteChoice(choice)
                .setIdentitySignature(com.google.protobuf.ByteString.copyFrom(signedBytes))
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
                    try {
                        // LIVE MODE: Attempt Real Network Call with Timeout
                        android.util.Log.d("GovernanceVM", "Attempting Live Network Call...")
                        // We use a withTimeout block to enforce the 3.5s limit
                        kotlinx.coroutines.withTimeout(3500) { 
                             success = arciumClient.submitVoteReal(proposalId, response.proofData.toByteArray())
                        }
                        android.util.Log.d("GovernanceVM", "Live Network Call Success: $success")
                    } catch (e: Exception) {
                        // FALLBACK
                        android.util.Log.e("GovernanceVM", "Live Network Failed/Timed Out: ${e.message}. Falling back to Simulation.")
                        _state.update { it.copy(voteStatus = "Network Busy. Switching to Simulation...") }
                        _uiEvent.send("⚠️ Network Busy: Falling back to Simulation")
                        delay(1000) 
                         
                        // Fallback Simulation Logic
                        delay(1500)
                        success = arciumClient.submitVote(proposalId, response.proofData.toByteArray())
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
