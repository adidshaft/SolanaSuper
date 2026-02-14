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

class HealthViewModel(
    private val promptManager: BiometricPromptManager
    // private val repository: HealthRepository // Inject later for real data
) : ViewModel() {

    private val _state = MutableStateFlow(HealthState())
    val state = _state.asStateFlow()

    init {
        // Collect biometric results
        viewModelScope.launch {
            promptManager.promptResults.collectLatest { result ->
                when (result) {
                    is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                        _state.update { 
                            it.copy(
                                isLocked = false,
                                error = null,
                                records = getMockRecords() 
                            ) 
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
