package com.solanasuper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        OfflineTransaction::class,
        WalletBalance::class,
        ActivityLogEntity::class,
        InvestPosition::class,
        NonceAccount::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WalletDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun investDao(): InvestDao
    abstract fun nonceAccountDao(): NonceAccountDao

    companion object {
        @Volatile
        private var INSTANCE: WalletDatabase? = null

        fun getDatabase(context: Context): WalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "wallet_database"
                )
                .fallbackToDestructiveMigration() // For development simplicity
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
