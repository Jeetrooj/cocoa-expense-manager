package com.cocoa.expenses.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExpenseCategory(val displayName: String) {
    INVESTMENTS("Investments"),
    UTILITIES_BILLS("Utilities & Bills"),
    FUEL_VEHICLE("Fuel & Vehicle"),
    POCKET_MONEY("Pocket Money"),
    SHOPPING_LIFESTYLE("Shopping & Lifestyle"),
    GYM_FITNESS("Gym & Fitness"),
    EDUCATION_CERTIFICATES("Education & Certificates"),
    EMI_CREDIT_CARDS("EMI & Credit Cards"),
    BORROWED_PEOPLE("Borrowed / People"),
    GENERAL_EXPENSES("General Expenses"),
    MISCELLANEOUS("Miscellaneous")
}

enum class TransactionStatus { PAID, PENDING }

enum class AccountTag(val label: String) {
    BOI("BOI"),
    KMB("KMB"),
    PNB("PNB"),
    SLICE("slice"),
    CASH("Cash"),
    UNSPECIFIED("Unspecified")
}

enum class SubBudgetAnchor { NONE, FUEL_ANCHOR, POCKET_MONEY_ANCHOR }

enum class LendingType { BORROWED, LENT_OR_GIVEN, SPLIT_REPAYMENT }

@Entity(tableName = "monthly_sheets")
data class MonthlySheetEntity(
    @PrimaryKey val monthId: String, // e.g., "2026-07"
    val displayName: String,         // e.g., "July 2026"
    val totalBudget: Double,
    val fuelBudgetAllocation: Double = 1000.0,
    val pocketMoneyAllocation: Double = 1000.0,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "main_ledger_transactions",
    foreignKeys = [ForeignKey(
        entity = MonthlySheetEntity::class,
        parentColumns = ["monthId"],
        childColumns = ["monthId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("monthId")]
)
data class MainLedgerTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val serialNo: Int,
    val description: String,
    val category: ExpenseCategory,
    val amount: Double,
    val status: TransactionStatus,
    val accountTag: AccountTag,
    val createdAtDate: String,
    val notes: String = "",
    val isSubBudgetAnchor: SubBudgetAnchor = SubBudgetAnchor.NONE
)

@Entity(
    tableName = "fuel_log_entries",
    foreignKeys = [ForeignKey(
        entity = MonthlySheetEntity::class,
        parentColumns = ["monthId"],
        childColumns = ["monthId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("monthId")]
)
data class FuelLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val date: String,
    val liters: Double,
    val amount: Double,
    val pricePerLiter: Double = if (liters > 0) amount / liters else 0.0,
    val vehicleNote: String = "Petrol",
    val accountTag: AccountTag = AccountTag.SLICE
)

@Entity(
    tableName = "pocket_money_micro_entries",
    foreignKeys = [ForeignKey(
        entity = MonthlySheetEntity::class,
        parentColumns = ["monthId"],
        childColumns = ["monthId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("monthId")]
)
data class PocketMoneyMicroEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val date: String,
    val itemDescription: String,
    val amount: Double,
    val categoryTag: String = "Daily Micro-Spend"
)

@Entity(
    tableName = "people_lending_entries",
    foreignKeys = [ForeignKey(
        entity = MonthlySheetEntity::class,
        parentColumns = ["monthId"],
        childColumns = ["monthId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("monthId")]
)
data class PeopleLendingAndRepaymentEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val date: String,
    val personOrTitle: String,
    val type: LendingType,
    val amount: Double,
    val breakdownNotes: String = "",
    val isSettled: Boolean = false
)

@Entity(tableName = "account_wallets")
data class AccountWalletEntity(
    @PrimaryKey val accountTag: AccountTag,
    val openingBalance: Double = 0.0
)
