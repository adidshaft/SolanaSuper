package com.solanasuper.ui.income



// import com.solanasuper.data.OfflineTransaction

enum class PeerStatus {
    IDLE,
    SCANNING,
    FOUND_PEER,
    CONNECTING,
    CONNECTED,
    VERIFYING,
    CONNECTED_WAITING_INPUT,
    CONNECTED_WAITING_FUNDS,
    TRANSFERRING,
    SUCCESS,
    ERROR
}

sealed interface UiStatus {
    object Idle : UiStatus
    object Loading : UiStatus
    object Success : UiStatus
    data class Error(val message: String) : UiStatus
}

data class IncomeUiState(
    val status: UiStatus = UiStatus.Idle,
    val balance: Double = 0.0,
    val transactions: List<UiTransaction> = emptyList(),
    val p2pStatus: PeerStatus = PeerStatus.IDLE,
    val p2pPeerName: String? = null,
    val peerPublicKey: String? = null,
    val p2pAuthToken: String? = null,
    val p2pEndpointId: String? = null,
    val isP2PSender: Boolean = false
) {
    val isLoading: Boolean get() = status is UiStatus.Loading
    val error: String? get() = (status as? UiStatus.Error)?.message
}
