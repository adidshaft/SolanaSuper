package com.solanasuper.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

class BiometricPromptManager(private val activity: FragmentActivity) {

    private val resultChannel = MutableSharedFlow<BiometricResult>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val promptResults = resultChannel.asSharedFlow()

    fun showBiometricPrompt(
        title: String,
        description: String,
        cryptoObject: BiometricPrompt.CryptoObject? = null
    ) {
        val manager = BiometricManager.from(activity)
        // If CryptoObject is provided, we MUST use BIOMETRIC_STRONG (no Device Credential allowed for Per-Use Keys usually)
        val authenticators = if (cryptoObject != null) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val promptInfoBuilder = PromptInfo.Builder()
            .setTitle(title)
            .setDescription(description)
            .setAllowedAuthenticators(authenticators)

        if (cryptoObject != null) {
           // When NOT using Device Credential, we can set Negative Button
           promptInfoBuilder.setNegativeButtonText("Cancel")
        }
        
        val promptInfo = promptInfoBuilder.build()

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                resultChannel.tryEmit(BiometricResult.AuthenticationError(errString.toString()))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                resultChannel.tryEmit(BiometricResult.AuthenticationSuccess(result))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                resultChannel.tryEmit(BiometricResult.AuthenticationFailed)
            }
        })

        if (cryptoObject != null) {
            prompt.authenticate(promptInfo, cryptoObject)
        } else {
            prompt.authenticate(promptInfo)
        }
    }

    sealed interface BiometricResult {
        data object AuthenticationFailed : BiometricResult
        data class AuthenticationError(val error: String) : BiometricResult
        data class AuthenticationSuccess(val result: BiometricPrompt.AuthenticationResult) : BiometricResult
    }
}
