package com.solanasuper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [WalletBalance::class, OfflineTransaction::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    // Note: We originally called it WalletDao, but IncomeViewModel expects TransactionDao.
    // Let's alias it or check what we implemented.
    // Step 1027 says WalletDao. 
    // Step 1244 says TransactionDao.
    // I likely have TWO Daos or a naming confusion.
    // Let's check if WalletDao.kt exists.
    // Step 1249 shows TransactionDao.kt.
    // Step 1027 shows WalletDao.kt.
    // I should unify them. TransactionDao is better for Generic Transactions.
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: WalletDatabase? = null

        fun getDatabase(context: Context): WalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "wallet_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
