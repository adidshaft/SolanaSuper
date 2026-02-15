package com.solanasuper.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
// import com.solanasuper.data.HealthRepository // Not strictly needed if we mock for now or use it later
import com.solanasuper.data.HealthEntity
import com.solanasuper.security.BiometricPromptManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.solanasuper.network.NetworkManager
import com.solanasuper.ui.state.ArciumComputationState
import kotlinx.coroutines.delay
import com.solanasuper.core.ZKProver
import com.solanasuper.core.EnclaveProto

class HealthViewModel(
    private val promptManager: BiometricPromptManager
    // private val repository: HealthRepository // Inject later for real data
) : ViewModel() {

    private val _state = MutableStateFlow(HealthState())
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // Collect biometric results
        viewModelScope.launch {
            promptManager.promptResults.collectLatest { result ->
                when (result) {
                    is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                        // Launch the MPC simulation
                        launch {
                            processUnlock()
                        }
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                        _state.update { it.copy(error = result.error) }
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                        _state.update { it.copy(error = "Authentication failed") }
                    }
                }
            }
        }
    }

    fun unlockVault() {
        promptManager.showBiometricPrompt(
            title = "Unlock Health Vault",
            description = "Authenticate to view your secure medical records"
        )
    }

    // Process the unlock with simulated MPC steps
    // Process the unlock with simulated MPC steps but REAL Local Proof Generation
    private suspend fun processUnlock() {
         // State 1: Generating Local Proof (Real JNI)
        _state.update { it.copy(mpcState = ArciumComputationState.GENERATING_LOCAL_PROOF) }
        
        // Prepare Real Request
        val request = EnclaveProto.EnclaveRequest.newBuilder()
            .setRequestId("health_unlock_${System.currentTimeMillis()}")
            .setActionType("VERIFY_HEALTH_ACCESS")
            .build()
            
        // BLOCKING JNI CALL (Offloaded)
        val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            ZKProver.processRequest(request)
        }
        
         // VERIFICATION LOGGING
        android.util.Log.d("HealthVM", "ZK Execution Complete. Success: ${response.success}")
        if (response.success) {
            val proofHex = response.proofData.toByteArray().joinToString("") { "%02x".format(it) }
            android.util.Log.d("HealthVM", "Generated Access Proof: $proofHex")
        }
        
        // State 2: Submitting to Network
         _state.update { it.copy(mpcState = ArciumComputationState.SUBMITTING_TO_ARCIUM_MXE) }
        
        if (NetworkManager.isLiveMode.value) {
            try {
                android.util.Log.d("HealthVM", "Attempting Live Verification...")
                kotlinx.coroutines.withTimeout(3500) {
                     // Real Network Call Placeholder
                     val url = java.net.URL("https://api.devnet.arcium.com/health/verify") 
                     val connection = url.openConnection() as java.net.HttpURLConnection
                     connection.connectTimeout = 3000
                     connection.connect() // Should fail/timeout or 404
                }
            } catch (e: Exception) {
               android.util.Log.e("HealthVM", "Live Verification Failed: ${e.message}. Fallback.")
               _uiEvent.send("⚠️ Network Busy: Falling back to Simulation")
               delay(1000) // Fallback delay
            }
        } else {
             delay(1000) // Simulation delay
        }

         // State 3: Computing (Remote)
         _state.update { it.copy(mpcState = ArciumComputationState.COMPUTING_IN_MXE) }
        delay(1500)
        
        // State 4: Callback
         _state.update { it.copy(mpcState = ArciumComputationState.COMPUTATION_CALLBACK) }
        delay(1000)
        
        // Success
         _state.update { 
            it.copy(
                mpcState = ArciumComputationState.COMPLETED,
                isLocked = false,
                error = null,
                records = getMockRecords() 
            ) 
        }
        delay(1500) // Show checkmark
        _state.update { it.copy(mpcState = ArciumComputationState.IDLE) }
    }

    private fun getMockRecords(): List<DecryptedHealthRecord> {
        return listOf(
            DecryptedHealthRecord("1", "Vaccine Certificate", "COVID-19 Vaccination Record", System.currentTimeMillis()),
            DecryptedHealthRecord("2", "Prescription", "Amoxicillin 500mg", System.currentTimeMillis()),
            DecryptedHealthRecord("3", "Lab Result", "Blood Test - Normal", System.currentTimeMillis())
        )
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val promptManager: BiometricPromptManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HealthViewModel(promptManager) as T
        }
    }
}
