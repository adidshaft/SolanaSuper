package com.solanasuper.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [WalletBalance::class, OfflineTransaction::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
}
