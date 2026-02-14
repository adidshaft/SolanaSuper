
package com.solanasuper.core

import com.solanasuper.core.proto.EnclaveProto.EnclaveRequest
import com.solanasuper.core.proto.EnclaveProto.EnclaveResponse

object ZKProver {
    init {
        System.loadLibrary("solanasuper_core")
    }

    /**
     * Native method that bridges to Rust.
     * Takes serialized Protobuf bytes, returns serialized Protobuf bytes.
     */
    private external fun processEnclaveRequest(requestBytes: ByteArray): ByteArray

    /**
     * Kotlin-friendly wrapper.
     */
    fun processRequest(request: EnclaveRequest): EnclaveResponse {
        val requestBytes = request.toByteArray()
        val responseBytes = processEnclaveRequest(requestBytes)
        return EnclaveResponse.parseFrom(responseBytes)
    }
}
