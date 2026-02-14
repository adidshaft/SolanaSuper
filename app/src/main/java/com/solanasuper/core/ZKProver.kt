package com.solanasuper.core

import com.google.protobuf.InvalidProtocolBufferException

object ZKProver {
    init {
        System.loadLibrary("solanasuper_core")
    }

    /**
     * Native method that accepts a serialized EnclaveRequest and returns a serialized EnclaveResponse.
     */
    private external fun processEnclaveRequest(requestBytes: ByteArray): ByteArray

    /**
     * Public API to process an EnclaveRequest using the native Rust backend.
     */
    fun processRequest(request: EnclaveProto.EnclaveRequest): EnclaveProto.EnclaveResponse {
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
