package com.solanasuper.ui.health

import com.solanasuper.ui.state.ArciumComputationState

data class HealthState(
    val isLocked: Boolean = true,
    val error: String? = null,
    val records: List<DecryptedHealthRecord> = emptyList(),
    val mpcState: ArciumComputationState = ArciumComputationState.IDLE
)
