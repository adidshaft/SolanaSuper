package com.solanasuper.ui.health

import com.solanasuper.data.HealthEntity

data class HealthState(
    val isLocked: Boolean = true,
    val records: List<DecryptedHealthRecord> = emptyList(),
    val error: String? = null
)
