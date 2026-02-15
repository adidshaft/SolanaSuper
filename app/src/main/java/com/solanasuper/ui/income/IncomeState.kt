package com.solanasuper.ui.income

import com.solanasuper.data.OfflineTransaction

data class IncomeState(
    val balance: Double = 0.0,
    val transactions: List<OfflineTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val p2pStatus: P2PStatus = P2PStatus.IDLE,
    val p2pPeerName: String? = null,
    val p2pAuthToken: String? = null,
    val p2pEndpointId: String? = null,
    val isClaiming: Boolean = false
)
