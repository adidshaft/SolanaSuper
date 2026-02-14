package com.solanasuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_balance")
data class WalletBalance(
    @PrimaryKey val id: Int = 1, // Singleton row
    val availableBalance: Long
)
