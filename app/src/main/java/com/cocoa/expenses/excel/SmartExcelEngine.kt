package com.cocoa.expenses.excel

import com.cocoa.expenses.data.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

data class ParsedSheetBundle(
    val monthEntity: MonthlySheetEntity,
    val ledgerItems: List<MainLedgerTransaction>,
    val fuelLogs: List<FuelLogEntry>,
    val pocketLogs: List<PocketMoneyMicroEntry>,
    val peopleLogs: List<PeopleLendingAndRepaymentEntry>
)

data class ExcelImportPreview(
    val sheets: List<ParsedSheetBundle>,
    val totalLedgerRecords: Int,
    val totalFuelLogs: Int,
    val totalFuelLiters: Double,
    val totalPocketSpends: Int,
    val totalRepaymentRecords: Int
)

object SmartExcelEngine {

    fun parseWorkbook(inputStream: InputStream, defaultYear: Int = 2026): ExcelImportPreview {
        val workbook = WorkbookFactory.create(inputStream)
        val parsedSheets = mutableListOf<ParsedSheetBundle>()

        for (sIdx in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sIdx)
            val tabName = sheet.sheetName.trim()
            val (monthId, displayName) = resolveMonthFromTabName(tabName, defaultYear, sIdx)

            // 1. Dynamic Anchor Scan: Scan first 10 rows & 10 cols for "Sn no."
            var snRow = -1
            var snCol = -1
            for (r in 0..10) {
                val row = sheet.getRow(r) ?: continue
                for (c in 0..10) {
                    val text = getCellString(row.getCell(c)).lowercase(Locale.ROOT)
                    if (text.contains("sn no") || text == "sn" || text == "s.no") {
                        snRow = r
                        snCol = c
                        break
                    }
                }
                if (snRow != -1) break
            }
            if (snRow == -1) continue

            // 2. Scan for Final Data / Budget cap
            var budgetAmount = 15000.0
            for (r in 0..15) {
                val row = sheet.getRow(r) ?: continue
                for (c in 0..25) {
                    val cellStr = getCellString(row.getCell(c)).lowercase(Locale.ROOT)
                    if (cellStr == "budget" || cellStr == "monthly budget") {
                        val nextVal = getCellNumeric(row.getCell(c + 1))
                        if (nextVal > 0) budgetAmount = nextVal
                    }
                }
            }

            // 3. Parse Main Ledger rows starting below snRow
            val ledgerList = mutableListOf<MainLedgerTransaction>()
            var fuelAllocation = 1000.0
            var pocketAllocation = 1000.0
            var rIdx = snRow + 1
            while (rIdx <= sheet.lastRowNum) {
                val row = sheet.getRow(rIdx)
                if (row == null) { rIdx++; continue }
                val snVal = getCellNumeric(row.getCell(snCol)).toInt()
                val desc = getCellString(row.getCell(snCol + 1)).trim()
                if (desc.isEmpty() || desc.lowercase(Locale.ROOT).startsWith("total")) {
                    if (snVal == 0) break
                }
                val rawCat = getCellString(row.getCell(snCol + 2))
                val amt = getCellNumeric(row.getCell(snCol + 3))
                val rawStatus = getCellString(row.getCell(snCol + 4))
                val rawAcc = getCellString(row.getCell(snCol + 5))
                val rawDate = getCellString(row.getCell(snCol + 6)).ifEmpty { "$monthId-01" }
                val notes = getCellString(row.getCell(snCol + 7))

                val anchor = when {
                    desc.equals("fuel", ignoreCase = true) -> {
                        fuelAllocation = amt
                        SubBudgetAnchor.FUEL_ANCHOR
                    }
                    desc.contains("pocket money", ignoreCase = true) -> {
                        pocketAllocation = amt
                        SubBudgetAnchor.POCKET_MONEY_ANCHOR
                    }
                    else -> SubBudgetAnchor.NONE
                }

                ledgerList.add(
                    MainLedgerTransaction(
                        monthId = monthId,
                        serialNo = if (snVal > 0) snVal else ledgerList.size + 1,
                        description = desc,
                        category = normalizeCategory(rawCat, desc),
                        amount = amt,
                        status = normalizeStatus(rawStatus),
                        accountTag = normalizeAccount(rawAcc),
                        createdAtDate = rawDate,
                        notes = notes,
                        isSubBudgetAnchor = anchor
                    )
                )
                rIdx++
            }

            // 4. Scan right-hand columns for LITERS (Fuel Sub-Ledger)
            val fuelLogs = mutableListOf<FuelLogEntry>()
            var litersRow = -1
            var litersCol = -1
            for (r in 0..12) {
                val row = sheet.getRow(r) ?: continue
                for (c in (snCol + 6)..30) {
                    val txt = getCellString(row.getCell(c)).lowercase(Locale.ROOT)
                    if (txt == "liters" || txt == "ltrs") {
                        litersRow = r
                        litersCol = c
                        break
                    }
                }
                if (litersRow != -1) break
            }
            if (litersRow != -1) {
                var fr = litersRow + 1
                while (fr <= sheet.lastRowNum) {
                    val row = sheet.getRow(fr) ?: break
                    val dateCell = getCellString(row.getCell(litersCol - 1))
                    if (dateCell.lowercase(Locale.ROOT).contains("total")) break
                    val ltrs = getCellNumeric(row.getCell(litersCol))
                    val amt = getCellNumeric(row.getCell(litersCol + 1))
                    if (ltrs <= 0 && amt <= 0) break
                    val note = getCellString(row.getCell(litersCol + 2)).ifEmpty { "Petrol" }
                    fuelLogs.add(
                        FuelLogEntry(
                            monthId = monthId,
                            date = dateCell.ifEmpty { "$monthId-05" },
                            liters = ltrs,
                            amount = amt,
                            pricePerLiter = if (ltrs > 0) amt / ltrs else 0.0,
                            vehicleNote = note
                        )
                    )
                    fr++
                }
            }

            // 5. Scan for secondary TRANSACTIONS + budget left (Pocket Money Micro-Expenses)
            val pocketLogs = mutableListOf<PocketMoneyMicroEntry>()
            var pmRow = -1
            var pmCol = -1
            for (r in 0..12) {
                val row = sheet.getRow(r) ?: continue
                for (c in (snCol + 10)..35) {
                    val txt = getCellString(row.getCell(c)).lowercase(Locale.ROOT)
                    if (txt == "transactions" || txt == "pocket transactions") {
                        pmRow = r
                        pmCol = c
                        break
                    }
                }
                if (pmRow != -1) break
            }
            if (pmRow != -1) {
                var pr = pmRow + 1
                while (pr <= sheet.lastRowNum) {
                    val row = sheet.getRow(pr) ?: break
                    val dt = getCellString(row.getCell(pmCol - 1))
                    val itemDesc = getCellString(row.getCell(pmCol))
                    val amt = getCellNumeric(row.getCell(pmCol + 1))
                    if (itemDesc.isEmpty() || amt <= 0) break
                    pocketLogs.add(
                        PocketMoneyMicroEntry(
                            monthId = monthId,
                            date = dt.ifEmpty { "$monthId-10" },
                            itemDescription = itemDesc,
                            amount = amt
                        )
                    )
                    pr++
                }
            }

            // 6. Scan for People / Lending / Repayments block (NAME or pay ment of)
            val peopleLogs = mutableListOf<PeopleLendingAndRepaymentEntry>()
            for (r in 8..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue
                for (c in 0..30) {
                    val txt = getCellString(row.getCell(c)).lowercase(Locale.ROOT)
                    if (txt == "name" || txt == "pay ment of") {
                        val headerCol = if (txt == "name") c - 1 else c - 4
                        var rr = r + 1
                        while (rr <= sheet.lastRowNum) {
                            val pRow = sheet.getRow(rr) ?: break
                            val dt = getCellString(pRow.getCell(headerCol))
                            val person = getCellString(pRow.getCell(headerCol + 1))
                            val amt = getCellNumeric(pRow.getCell(headerCol + 2))
                            val rawType = getCellString(pRow.getCell(headerCol + 3))
                            val breakdown = getCellString(pRow.getCell(headerCol + 4))
                            if (person.isEmpty() || amt <= 0) break
                            val lType = when {
                                breakdown.contains("/") || rawType.contains("SPLIT", true) -> LendingType.SPLIT_REPAYMENT
                                rawType.contains("BORROW", true) -> LendingType.BORROWED
                                else -> LendingType.LENT_OR_GIVEN
                            }
                            peopleLogs.add(
                                PeopleLendingAndRepaymentEntry(
                                    monthId = monthId,
                                    date = dt.ifEmpty { "$monthId-15" },
                                    personOrTitle = person,
                                    type = lType,
                                    amount = amt,
                                    breakdownNotes = breakdown
                                )
                            )
                            rr++
                        }
                    }
                }
            }

            parsedSheets.add(
                ParsedSheetBundle(
                    monthEntity = MonthlySheetEntity(
                        monthId = monthId,
                        displayName = displayName,
                        totalBudget = budgetAmount,
                        fuelBudgetAllocation = fuelAllocation,
                        pocketMoneyAllocation = pocketAllocation,
                        sortOrder = sIdx
                    ),
                    ledgerItems = ledgerList,
                    fuelLogs = fuelLogs,
                    pocketLogs = pocketLogs,
                    peopleLogs = peopleLogs
                )
            )
        }

        workbook.close()
        return ExcelImportPreview(
            sheets = parsedSheets,
            totalLedgerRecords = parsedSheets.sumOf { it.ledgerItems.size },
            totalFuelLogs = parsedSheets.sumOf { it.fuelLogs.size },
            totalFuelLiters = parsedSheets.sumOf { s -> s.fuelLogs.sumOf { it.liters } },
            totalPocketSpends = parsedSheets.sumOf { it.pocketLogs.size },
            totalRepaymentRecords = parsedSheets.sumOf { it.peopleLogs.size }
        )
    }

    fun normalizeCategory(raw: String, desc: String): ExpenseCategory {
        val c = raw.trim().lowercase(Locale.ROOT)
        val d = desc.trim().lowercase(Locale.ROOT)
        return when {
            c.contains("invest") || d.contains("sip") || d.contains("hsbc") -> ExpenseCategory.INVESTMENTS
            c.contains("utlit") || c.contains("utilit") || c.contains("bill") || d.contains("wifi") || d.contains("recharge") -> ExpenseCategory.UTILITIES_BILLS
            c.contains("fuel") || c.contains("vehicle") || d.contains("petrol") || d.contains("2t oil") || d.contains("bike") -> ExpenseCategory.FUEL_VEHICLE
            d.contains("pocket money") -> ExpenseCategory.POCKET_MONEY
            c.contains("shop") || c.contains("lifestyle") || d.contains("puja") || d.contains("gift") -> ExpenseCategory.SHOPPING_LIFESTYLE
            c.contains("gym") || d.contains("gym") -> ExpenseCategory.GYM_FITNESS
            c.contains("educat") || c.contains("cert") -> ExpenseCategory.EDUCATION_CERTIFICATES
            c.contains("loan") || c.contains("instalment") || c.contains("emi") -> ExpenseCategory.EMI_CREDIT_CARDS
            c.contains("borrow") || c.contains("people") || d.contains("repayment") -> ExpenseCategory.BORROWED_PEOPLE
            c.contains("miscl") || c.contains("misc") -> ExpenseCategory.MISCELLANEOUS
            else -> ExpenseCategory.GENERAL_EXPENSES
        }
    }

    fun normalizeStatus(raw: String): TransactionStatus {
        val s = raw.trim().lowercase(Locale.ROOT)
        return if (s == "paid" || s == "done") TransactionStatus.PAID else TransactionStatus.PENDING
    }

    fun normalizeAccount(raw: String): AccountTag {
        val a = raw.trim().lowercase(Locale.ROOT)
        return when {
            a.contains("pnb") -> AccountTag.PNB
            a.contains("kmb") -> AccountTag.KMB
            a.contains("slice") -> AccountTag.SLICE
            a.contains("boi") -> AccountTag.BOI
            a.contains("cash") -> AccountTag.CASH
            else -> AccountTag.UNSPECIFIED
        }
    }

    private fun resolveMonthFromTabName(tabName: String, year: Int, fallbackIdx: Int): Pair<String, String> {
        val lower = tabName.lowercase(Locale.ROOT)
        val months = listOf(
            "january" to ("01" to "January"),
            "february" to ("02" to "February"),
            "march" to ("03" to "March"),
            "april" to ("04" to "April"),
            "may" to ("05" to "May"),
            "june" to ("06" to "June"),
            "july" to ("07" to "July"),
            "august" to ("08" to "August"),
            "september" to ("09" to "September"),
            "october" to ("10" to "October"),
            "november" to ("11" to "November"),
            "december" to ("12" to "December")
        )
        for ((key, pair) in months) {
            if (lower.contains(key)) {
                return "$year-${pair.first}" to "${pair.second} $year"
            }
        }
        val num = String.format(Locale.ROOT, "%02d", (fallbackIdx + 1).coerceIn(1, 12))
        return "$year-$num" to "$tabName $year"
    }

    private fun getCellString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                val v = cell.numericCellValue
                if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            }
            CellType.FORMULA -> try { cell.stringCellValue } catch (_: Exception) { cell.numericCellValue.toString() }
            else -> ""
        }
    }

    private fun getCellNumeric(cell: Cell?): Double {
        if (cell == null) return 0.0
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.replace(Regex("[^0-9.-]"), "").toDoubleOrNull() ?: 0.0
            CellType.FORMULA -> try { cell.numericCellValue } catch (_: Exception) { 0.0 }
            else -> 0.0
        }
    }

    fun exportToWorkbook(bundles: List<ParsedSheetBundle>, outputStream: OutputStream) {
        val wb = XSSFWorkbook()
        for (bundle in bundles) {
            val sheet = wb.createSheet(bundle.monthEntity.displayName)
            val headerRow = sheet.createRow(0)
            val cols = listOf("Sn no.", "Description", "Category", "Amount", "Status", "ON ACC", "Date", "Notes")
            cols.forEachIndexed { idx, title -> headerRow.createCell(idx).setCellValue(title) }
            bundle.ledgerItems.forEachIndexed { idx, tx ->
                val r = sheet.createRow(idx + 1)
                r.createCell(0).setCellValue(tx.serialNo.toDouble())
                r.createCell(1).setCellValue(tx.description)
                r.createCell(2).setCellValue(tx.category.displayName)
                r.createCell(3).setCellValue(tx.amount)
                r.createCell(4).setCellValue(tx.status.name)
                r.createCell(5).setCellValue(tx.accountTag.label)
                r.createCell(6).setCellValue(tx.createdAtDate)
                r.createCell(7).setCellValue(tx.notes)
            }
        }
        wb.write(outputStream)
        wb.close()
    }
}
