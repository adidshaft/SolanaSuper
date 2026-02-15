package com.solanasuper.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NetworkManager {
    private val _isLiveMode = MutableStateFlow(false) // Default to Simulation
    val isLiveMode = _isLiveMode.asStateFlow()

    fun toggleMode() {
        _isLiveMode.update { !it }
    }
    
    fun setLiveMode(isLive: Boolean) {
        _isLiveMode.value = isLive
    }
}
