package com.solanasuper.ui.income

data class UiTransaction(
    val id: String,
    val amount: Double,
    val timestamp: Long,
    val recipientId: String?,
    val isReceived: Boolean
)
