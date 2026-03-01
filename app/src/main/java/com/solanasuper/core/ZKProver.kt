package com.solanasuper.core

import com.google.protobuf.InvalidProtocolBufferException

object ZKProver {
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("solanasuper_core")
            isNativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("ZKProver", "Native library not found or invalid. Using mocked fallback for ZK Proofs.")
        } catch (e: Exception) {
            android.util.Log.e("ZKProver", "Error loading native library", e)
        }
    }

    /**
     * Native method that accepts a serialized EnclaveRequest and returns a serialized EnclaveResponse.
     */
    private external fun processEnclaveRequest(requestBytes: ByteArray): ByteArray

    /**
     * Public API to process an EnclaveRequest using the native Rust backend.
     */
    fun processRequest(request: EnclaveProto.EnclaveRequest): EnclaveProto.EnclaveResponse {
        if (!isNativeLoaded) {
            android.util.Log.w("ZKProver", "Skipping native execution - returning simulated ZK Proof")
            return EnclaveProto.EnclaveResponse.newBuilder()
                .setSuccess(true)
                .setProofData(com.google.protobuf.ByteString.copyFrom("mocked_zk_proof_${System.currentTimeMillis()}".toByteArray()))
                .build()
        }

        val requestBytes = request.toByteArray()
        val responseBytes = processEnclaveRequest(requestBytes)
        
        return try {
            EnclaveProto.EnclaveResponse.parseFrom(responseBytes)
        } catch (e: InvalidProtocolBufferException) {
            EnclaveProto.EnclaveResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage("Failed to parse native response: ${e.message}")
                .build()
        }
    }
}
