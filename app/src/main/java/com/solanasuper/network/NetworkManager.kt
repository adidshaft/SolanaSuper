package com.solanasuper.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NetworkManager {
    private val _isLiveMode = MutableStateFlow(false) // Default to Simulation
    val isLiveMode = _isLiveMode.asStateFlow()

    // RPC Configuration (QuickNode Support)
    // In production, these should be injected or loaded from valid config/env
    private const val DEFAULT_DEVNET_RPC = "https://api.devnet.solana.com"
    private const val QUICKNODE_DEVNET_RPC = "https://example.solana-devnet.quiknode.pro/custom-token/" // Placeholder

    private val _activeRpcUrl = MutableStateFlow(DEFAULT_DEVNET_RPC)
    val activeRpcUrl = _activeRpcUrl.asStateFlow()

    fun toggleMode() {
        _isLiveMode.update { !it }
    }
    
    fun setLiveMode(isLive: Boolean) {
        _isLiveMode.value = isLive
    }

    fun setRpcUrl(url: String) {
        _activeRpcUrl.value = url
    }
}
