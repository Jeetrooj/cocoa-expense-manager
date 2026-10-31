package com.cocoa.expenses.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * CRITICAL DATA RULE:
 * Starts with a 100% clean, empty database. No historical July–November user transactions
 * are pre-populated or hardcoded into the default database state.
 */
@Database(
    entities = [
        MonthlySheetEntity::class,
        MainLedgerTransaction::class,
        FuelLogEntry::class,
        PocketMoneyMicroEntry::class,
        PeopleLendingAndRepaymentEntry::class,
        AccountWalletEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CocoaDatabase : RoomDatabase() {
    abstract fun cocoaDao(): CocoaDao

    companion object {
        @Volatile
        private var INSTANCE: CocoaDatabase? = null

        fun getInstance(context: Context): CocoaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CocoaDatabase::class.java,
                    "cocoa_offline_vault.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
