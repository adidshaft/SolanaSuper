package com.solanasuper.ui.state

enum class ArciumComputationState(val message: String) {
    IDLE("Idle"),
    GENERATING_LOCAL_PROOF("Generating Zero-Knowledge Proof (Local Rust Enclave)..."),
    SUBMITTING_TO_ARCIUM_MXE("Broadcasting to Arcium Network (MXE)..."),
    COMPUTING_IN_MXE("Executing Secure MPC Computation..."),
    COMPUTATION_CALLBACK("Decrypting Network Result..."),
    COMPLETED("Computation Verified"),
    FAILED("Computation Failed")
}
