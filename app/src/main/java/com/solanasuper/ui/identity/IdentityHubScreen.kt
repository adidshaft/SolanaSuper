package com.solanasuper.ui.identity

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.security.BiometricPromptManager
import com.solanasuper.security.IdentityKeyManager
import java.security.Signature

@Composable
fun IdentityHubScreen(
    promptManager: BiometricPromptManager,
    identityKeyManager: IdentityKeyManager
) {
    var authStatus by remember { mutableStateOf("Idle") }

    LaunchedEffect(promptManager) {
        promptManager.promptResults.collect { result ->
            when (result) {
                is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                    authStatus = "Error: ${result.error}"
                }
                BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                    authStatus = "Authentication Failed"
                }
                is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                    // Successful authentication!
                    val cryptoObject = result.result.cryptoObject
                    if (cryptoObject != null) {
                        try {
                            val signature = cryptoObject.signature
                            if (signature != null) {
                                // 1. SIGN THE PAYLOAD (Proof of Ownership of Key)
                                val timestamp = System.currentTimeMillis()
                                val payload = "IdentityProof_$timestamp".toByteArray()
                                signature.update(payload)
                                val signedBytes = signature.sign()
                                
                                // 2. Send to Rust ZK Core for Attestation
                                val identityReq = com.solanasuper.core.EnclaveProto.IdentityRequest.newBuilder()
                                    .setAttributeId("biometric_auth")
                                    .setEncryptedIdentitySeed(com.google.protobuf.ByteString.copyFrom(signedBytes)) 
                                    .build()
                                    
                                val request = com.solanasuper.core.EnclaveProto.EnclaveRequest.newBuilder()
                                    .setRequestId("req_$timestamp")
                                    .setActionType("GENERATE_IDENTITY_PROOF")
                                    .setIdentityReq(identityReq)
                                    .build()
                                    
                                val response = com.solanasuper.core.ZKProver.processRequest(request)
                                
                                if (response.success) {
                                    authStatus = "Success! Proof len: ${response.proofData.size()}"
                                } else {
                                    authStatus = "ZK Error: ${response.errorMessage}"
                                }
                            }
                        } catch (e: Exception) {
                           authStatus = "Signing/ZK Failed: ${e.message}"
                        }
                    } else {
                        authStatus = "Success (No CryptoObject)"
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Identity Hub",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Status: $authStatus",
            style = MaterialTheme.typography.bodyMedium,
            color = if(authStatus.contains("Success")) Color(0xFF14F195) else Color.White.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = {
                try {
                    // Ensure key exists
                    if (identityKeyManager.getPublicKey() == null) {
                        identityKeyManager.generateIdentityKey()
                    }

                    // Initialize Signature
                    val signature = identityKeyManager.initSignature()
                    
                    if (signature != null) {
                         promptManager.showBiometricPrompt(
                            title = "Authenticate Identity",
                            description = "Unlock your secure identity key",
                            cryptoObject = BiometricPrompt.CryptoObject(signature)
                        )
                    } else {
                        authStatus = "Error: Could not init signature"
                    }
                } catch (e: Exception) {
                    authStatus = "Error: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Text("Generate Attestation", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        }
    }
}
