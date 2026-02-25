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
import com.solanasuper.data.ActivityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class Proposal(
    val id: String,
    val label: String,        // e.g. "Proposal #1"
    val title: String,
    val description: String,
    val yesVotes: Int = 0,
    val noVotes: Int = 0,
    val hasVoted: Boolean = false,
    val userVote: String? = null // "YES" or "NO"
)

data class GovernanceState(
    val proposals: List<Proposal> = listOf(
        Proposal(
            id = "proposal_ubi_001",
            label = "PROPOSAL #1",
            title = "Universal Basic Income",
            description = "Should the network distribute 10 SOL monthly to every verified human sovereign?",
            yesVotes = 1842, noVotes = 317
        ),
        Proposal(
            id = "proposal_depin_002",
            label = "PROPOSAL #2",
            title = "DePIN Node Incentives",
            description = "Allocate 5% of protocol fees to reward operators running offline mesh relay nodes.",
            yesVotes = 2103, noVotes = 489
        ),
        Proposal(
            id = "proposal_zkkyc_003",
            label = "PROPOSAL #3",
            title = "ZK-KYC for Seeker",
            description = "Mandate zero-knowledge biometric attestation for all Solana Seeker hardware wallet activations.",
            yesVotes = 987, noVotes = 1124
        )
    ),
    val activeProposalIndex: Int = 0,
    val mpcState: ArciumComputationState = ArciumComputationState.IDLE,
    val voteStatus: String = ""
)

class GovernanceViewModel(
    private val promptManager: BiometricPromptManager,
    private val identityKeyManager: IdentityKeyManager,
    private val arciumClient: MockArciumClient,
    private val repository: ActivityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GovernanceState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // Emits Pair<choice, payloadBytes> — single source of truth for sign payload
    private val _signRequest = Channel<Pair<String, ByteArray>>()
    val signRequest = _signRequest.receiveAsFlow()

    private var pendingVoteChoice: String? = null
    private var pendingProposalId: String? = null

    fun selectProposal(index: Int) {
        _state.update { it.copy(activeProposalIndex = index) }
    }

    fun initiateVote(choice: String) {
        val proposal = _state.value.proposals[_state.value.activeProposalIndex]
        if (proposal.hasVoted) {
            viewModelScope.launch { _uiEvent.send("You already voted on: ${proposal.title}") }
            return
        }
        pendingVoteChoice = choice
        pendingProposalId = proposal.id
        viewModelScope.launch {
            if (identityKeyManager.getSolanaPublicKey() == null) {
                identityKeyManager.ensureIdentity()
            }
            // Build payload — single source of truth
            val payload = "Vote_${choice}_for_${proposal.id}".toByteArray()
            _signRequest.send(Pair(choice, payload))
        }
    }

    fun onVoteSigned(signatureBytes: ByteArray) {
        val choice = pendingVoteChoice ?: return
        val proposalId = pendingProposalId ?: return
        viewModelScope.launch {
            processVote(signatureBytes, choice, proposalId)
        }
    }

    private suspend fun processVote(signatureBytes: ByteArray, choice: String, proposalId: String) {
        try {
            _state.update { it.copy(
                mpcState = ArciumComputationState.GENERATING_LOCAL_PROOF,
                voteStatus = "Generating ZK Vote Proof..."
            ) }

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

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                ZKProver.processRequest(request)
            }

            android.util.Log.d("GovernanceVM", "ZK Complete. Success=${response.success}")

            if (response.success) {
                _state.update { it.copy(
                    mpcState = ArciumComputationState.SUBMITTING_TO_ARCIUM_MXE,
                    voteStatus = if (NetworkManager.isLiveMode.value)
                        "Broadcasting to Live Arcium Network..." else "Submitting to Arcium (Simulated)..."
                ) }

                var success = false

                if (NetworkManager.isLiveMode.value) {
                    android.util.Log.d("GovernanceVM", "Broadcasting to Live Arcium Relayer...")
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val relayerUrl = "https://sovereign-arcium-relayer.onrender.com/api/vote"
                        val url = java.net.URL(relayerUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 60000
                        connection.readTimeout = 60000
                        connection.requestMethod = "POST"
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json")
                        val proofHex = response.proofData.toByteArray().joinToString("") { "%02x".format(it) }
                        val json = org.json.JSONObject()
                        json.put("proposalId", proposalId)
                        json.put("voteChoice", choice)
                        json.put("proof", proofHex)
                        connection.outputStream.use { it.write(json.toString().toByteArray()) }
                        val responseCode = connection.responseCode
                        if (responseCode in 200..299) {
                            success = true
                        } else {
                            val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                            throw Exception("Relayer HTTP $responseCode: $errorStream")
                        }
                    }
                } else {
                    delay(1500)
                    success = arciumClient.submitVote(proposalId, response.proofData.toByteArray())
                }

                _state.update { it.copy(mpcState = ArciumComputationState.COMPUTING_IN_MXE, voteStatus = "Executing Secure MPC Computation...") }
                delay(2000)
                _state.update { it.copy(mpcState = ArciumComputationState.COMPUTATION_CALLBACK, voteStatus = "Verifying Network Result...") }
                delay(1000)

                if (success) {
                    // Update tally locally
                    _state.update { s ->
                        val proposals = s.proposals.toMutableList()
                        val idx = proposals.indexOfFirst { it.id == proposalId }
                        if (idx >= 0) {
                            val p = proposals[idx]
                            proposals[idx] = p.copy(
                                yesVotes = if (choice == "YES") p.yesVotes + 1 else p.yesVotes,
                                noVotes = if (choice == "NO") p.noVotes + 1 else p.noVotes,
                                hasVoted = true,
                                userVote = choice
                            )
                        }
                        s.copy(
                            proposals = proposals,
                            mpcState = ArciumComputationState.COMPLETED,
                            voteStatus = "Vote Verified & Counted via MPC! 🗳️"
                        )
                    }
                    if (NetworkManager.isLiveMode.value) {
                        repository.logActivity(com.solanasuper.data.ActivityType.ARCIUM_PROOF, "Vote Cast (Live) | $proposalId | Choice: $choice")
                    }
                    delay(2500)
                    _state.update { it.copy(mpcState = ArciumComputationState.IDLE, voteStatus = "") }
                } else {
                    _state.update { it.copy(mpcState = ArciumComputationState.FAILED, voteStatus = "Network Rejected Vote") }
                    delay(2000)
                    _state.update { it.copy(mpcState = ArciumComputationState.IDLE, voteStatus = "") }
                }
            } else {
                _state.update { it.copy(mpcState = ArciumComputationState.FAILED, voteStatus = "ZK Proof Failed: ${response.errorMessage}") }
                delay(2000)
                _state.update { it.copy(mpcState = ArciumComputationState.IDLE, voteStatus = "") }
            }
        } catch (e: Exception) {
            _state.update { it.copy(mpcState = ArciumComputationState.FAILED, voteStatus = "Error: ${e.message}") }
            delay(2000)
            _state.update { it.copy(mpcState = ArciumComputationState.IDLE, voteStatus = "") }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val promptManager: BiometricPromptManager,
        private val identityKeyManager: IdentityKeyManager,
        private val arciumClient: MockArciumClient,
        private val repository: ActivityRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GovernanceViewModel(promptManager, identityKeyManager, arciumClient, repository) as T
        }
    }
}
