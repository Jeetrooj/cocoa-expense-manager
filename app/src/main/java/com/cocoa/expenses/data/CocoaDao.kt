package com.cocoa.expenses.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CocoaDao {
    @Query("SELECT * FROM monthly_sheets ORDER BY monthId ASC")
    fun observeAllMonths(): Flow<List<MonthlySheetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonth(month: MonthlySheetEntity)

    @Query("SELECT * FROM main_ledger_transactions WHERE monthId = :monthId ORDER BY serialNo ASC, id ASC")
    fun observeLedgerForMonth(monthId: String): Flow<List<MainLedgerTransaction>>

    @Query("SELECT * FROM main_ledger_transactions ORDER BY createdAtDate ASC")
    fun observeAllLedgerTransactions(): Flow<List<MainLedgerTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerTransaction(tx: MainLedgerTransaction): Long

    @Update
    suspend fun updateLedgerTransaction(tx: MainLedgerTransaction)

    @Delete
    suspend fun deleteLedgerTransaction(tx: MainLedgerTransaction)

    @Query("SELECT * FROM fuel_log_entries WHERE monthId = :monthId ORDER BY date ASC, id ASC")
    fun observeFuelLogsForMonth(monthId: String): Flow<List<FuelLogEntry>>

    @Query("SELECT * FROM fuel_log_entries ORDER BY date ASC")
    fun observeAllFuelLogs(): Flow<List<FuelLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(entry: FuelLogEntry)

    @Delete
    suspend fun deleteFuelLog(entry: FuelLogEntry)

    @Query("SELECT * FROM pocket_money_micro_entries WHERE monthId = :monthId ORDER BY date ASC, id ASC")
    fun observePocketMoneyForMonth(monthId: String): Flow<List<PocketMoneyMicroEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPocketMoneyEntry(entry: PocketMoneyMicroEntry)

    @Delete
    suspend fun deletePocketMoneyEntry(entry: PocketMoneyMicroEntry)

    @Query("SELECT * FROM people_lending_entries WHERE monthId = :monthId ORDER BY date ASC, id ASC")
    fun observePeopleEntriesForMonth(monthId: String): Flow<List<PeopleLendingAndRepaymentEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeopleEntry(entry: PeopleLendingAndRepaymentEntry)

    @Update
    suspend fun updatePeopleEntry(entry: PeopleLendingAndRepaymentEntry)

    @Delete
    suspend fun deletePeopleEntry(entry: PeopleLendingAndRepaymentEntry)

    @Query("SELECT * FROM account_wallets")
    fun observeAccountWallets(): Flow<List<AccountWalletEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccountWallet(wallet: AccountWalletEntity)

    @Query("DELETE FROM monthly_sheets")
    suspend fun clearAllMonthsAndCascades()
}
