package com.solanasuper.ui.income

import com.solanasuper.data.OfflineTransaction

data class IncomeState(
    val balance: Double = 0.0,
    val transactions: List<OfflineTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
