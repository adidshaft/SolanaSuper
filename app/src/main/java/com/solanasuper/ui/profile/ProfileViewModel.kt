package com.solanasuper.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solanasuper.data.ActivityLogEntity
import com.solanasuper.data.ActivityRepository
import com.solanasuper.data.ActivityType
import com.solanasuper.security.IdentityKeyManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    private val identityKeyManager: IdentityKeyManager,
    private val repository: ActivityRepository
) : ViewModel() {

    val activities: StateFlow<List<ActivityLogEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val solanaAddress: String = identityKeyManager.getSolanaPublicKey() ?: "Addr_Not_Found"

    /** Computed 0-100 Sovereignty Score based on on-chain + vault activity */
    val sovereigntyScore: Int
        get() {
            val acts = activities.value
            var score = 0
            if (solanaAddress != "Addr_Not_Found") score += 25          // Has wallet
            if (acts.any { it.type == ActivityType.SOLANA_TX }) score += 25  // Made a tx
            if (acts.any { it.type == ActivityType.ARCIUM_PROOF }) score += 25 // Voted/shared ZK proof
            if (acts.any { it.type == ActivityType.IPFS_HASH }) score += 25   // Uploaded to IPFS
            return score
        }

    /** Returns the BIP39 mnemonic word list for backup display.
     *  Returns null if no wallet has been created yet. */
    fun getMnemonic(): List<String>? {
        return try {
            val mnemonic = identityKeyManager.getMnemonic() ?: return null
            mnemonic.trim().split("\\s+".toRegex())
        } catch (e: Exception) {
            null
        }
    }

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
