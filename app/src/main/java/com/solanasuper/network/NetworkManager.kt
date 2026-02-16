package com.solanasuper.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NetworkManager {
    private val _isLiveMode = MutableStateFlow(false) // Default to Simulation
    val isLiveMode = _isLiveMode.asStateFlow()

    // RPC Configuration (Injected via BuildConfig)
    // Sourced from local.properties (QUICKNODE_SOLANA_RPC) or fallback to public Devnet
    private val SECURE_RPC_URL = com.solanasuper.BuildConfig.QUICKNODE_SOLANA_RPC

    private val _activeRpcUrl = MutableStateFlow(SECURE_RPC_URL)
    val activeRpcUrl = _activeRpcUrl.asStateFlow()

    fun toggleMode() {
        _isLiveMode.value = !_isLiveMode.value
        com.solanasuper.utils.AppLogger.i("NetworkManager", "Switched to ${if (_isLiveMode.value) "LIVE" else "SIMULATION"} mode")
    }
    
    fun setLiveMode(isLive: Boolean) {
        _isLiveMode.value = isLive
    }

    fun setRpcUrl(url: String) {
        _activeRpcUrl.value = url
    }
}
