package com.solanasuper.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solanasuper.data.ActivityLogEntity
import com.solanasuper.data.ActivityRepository
import com.solanasuper.security.IdentityKeyManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    private val identityKeyManager: IdentityKeyManager,
    private val repository: ActivityRepository
) : ViewModel() {

    // Expose activities
    val activities: StateFlow<List<ActivityLogEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Expose Address
    // In a real app we might want this reactive, but SharedPrefs is sync.
    // We can expose as a flow or just a property.
    val solanaAddress: String = identityKeyManager.getSolanaPublicKey() ?: "Addr_Not_Found"
    
    // For Demo: Seed some data if empty?
    // User requested "Update the DAOs... to save these hashes".
    // For now, let's trust the system generates them, or we can add a debug "simulate" button if needed.
    
    class Factory(
        private val identityKeyManager: IdentityKeyManager,
        private val repository: ActivityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(identityKeyManager, repository) as T
        }
    }
}
