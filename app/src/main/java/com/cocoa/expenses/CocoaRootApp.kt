package com.cocoa.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocoa.expenses.data.*
import com.cocoa.expenses.ui.theme.*
import com.cocoa.expenses.util.CocoaHaptics
import kotlinx.coroutines.launch

@Composable
fun CocoaRootApp(database: CocoaDatabase) {
    val dao = remember { database.cocoaDao() }
    val months by dao.observeAllMonths().collectAsState(initial = emptyList())
    var legalAccepted by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }
    var selectedMonthId by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(months) {
        if (selectedMonthId == null && months.isNotEmpty()) {
            selectedMonthId = months.first().monthId
        }
    }

    if (!legalAccepted) {
        OnboardingLegalScreen(onAccept = {
            CocoaHaptics.mediumConfirm(view, context)
            legalAccepted = true
        })
        return
    }

    Scaffold(
        containerColor = WarmWhiteBackground,
        bottomBar = {
            NavigationBar(containerColor = DarkEspressoChocolate) {
                val navItems = listOf(
                    "Monthly Vault" to Icons.Default.AccountBalanceWallet,
                    "Timelines" to Icons.Default.Timeline,
                    "Analytics" to Icons.Default.PieChart,
                    "Settings" to Icons.Default.Settings
                )
                navItems.forEachIndexed { idx, pair ->
                    NavigationBarItem(
                        selected = activeTab == idx,
                        onClick = {
                            CocoaHaptics.lightTick(view)
                            activeTab = idx
                        },
                        icon = { Icon(pair.second, contentDescription = pair.first) },
                        label = { Text(pair.first, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CrispWhiteText,
                            selectedTextColor = CrispWhiteText,
                            indicatorColor = CaramelGanacheAccent,
                            unselectedIconColor = Color(0xFFD5C3B8),
                            unselectedTextColor = Color(0xFFD5C3B8)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val activeMonth = months.find { it.monthId == selectedMonthId }
            when (activeTab) {
                0 -> MonthlyVaultScreen(
                    months = months,
                    activeMonth = activeMonth,
                    dao = dao,
                    onSelectMonth = {
                        CocoaHaptics.lightTick(view)
                        selectedMonthId = it
                    },
                    onCreateMonth = { mId, name, budget ->
                        scope.launch {
                            dao.upsertMonth(MonthlySheetEntity(mId, name, budget))
                            selectedMonthId = mId
                            CocoaHaptics.mediumConfirm(view, context)
                        }
                    }
                )
                1 -> TimelinesHubScreen(activeMonth = activeMonth, dao = dao)
                2 -> InvestmentsAndAnalyticsScreen(dao = dao)
                3 -> SettingsAndLegalScreen(dao = dao)
            }
        }
    }
}

@Composable
fun OnboardingLegalScreen(onAccept: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkEspressoChocolate)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Cocoa", color = CrispWhiteText, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            Text("Managing Expenses by Cocoa", color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = RichCocoaBeanAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("100% Offline-First Financial Vault", color = CrispWhiteText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Starts with a clean, empty local SQLite database.\n" +
                        "• Smart Multi-Sheet .xlsx Importer & Exporter.\n" +
                        "• Dedicated Fuel (Liters) & Pocket Money Sub-Budgets.\n" +
                        "• Zero telemetry, zero external servers, 100% private.",
                        color = Color(0xFFF7EFE6),
                        fontSize = 13.sp
                    )
                }
            }
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                Text(
                    "I accept the Offline Privacy Policy & Terms of Use of Cocoa — Managing Expenses by Cocoa.",
                    color = CrispWhiteText,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAccept,
                enabled = agreed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CaramelGanacheAccent)
            ) {
                Text("Enter Cocoa Vault", color = CrispWhiteText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MonthlyVaultScreen(
    months: List<MonthlySheetEntity>,
    activeMonth: MonthlySheetEntity?,
    dao: CocoaDao,
    onSelectMonth: (String) -> Unit,
    onCreateMonth: (String, String, Double) -> Unit
) {
    if (activeMonth == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your Cocoa Vault is Clean & Empty", color = DeepEspressoText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Import your multi-sheet .xlsx tracker or create your first Monthly Vault.", color = RoastedMochaText)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onCreateMonth("2026-10", "October 2026", 15000.0) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkEspressoChocolate)
            ) {
                Text("+ Create First Month (Oct 2026)", color = CrispWhiteText)
            }
        }
        return
    }

    val ledger by dao.observeLedgerForMonth(activeMonth.monthId).collectAsState(initial = emptyList())
    val totalSpent = ledger.sumOf { it.amount }
    val remaining = activeMonth.totalBudget - totalSpent

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkEspressoChocolate),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(activeMonth.displayName, color = Color(0xFFF59E0B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Remaining: ₹${String.format("%,.2f", remaining)}",
                        color = if (remaining < 0) StatusOverBudgetRed else CrispWhiteText,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Budget: ₹${String.format("%,.2f", activeMonth.totalBudget)} • Spent: ₹${String.format("%,.2f", totalSpent)}",
                        color = Color(0xFFE5D5C5),
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(ledger) { tx ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AlabasterMilkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tx.description, color = DeepEspressoText, fontWeight = FontWeight.Bold)
                        Text("${tx.category.displayName} • ${tx.accountTag.label}", color = RoastedMochaText, fontSize = 12.sp)
                    }
                    Text(
                        "₹${String.format("%,.2f", tx.amount)}",
                        color = DeepEspressoText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TimelinesHubScreen(activeMonth: MonthlySheetEntity?, dao: CocoaDao) {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Chronological Timelines Hub (${activeMonth?.displayName ?: "No Month Selected"})", color = DeepEspressoText, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InvestmentsAndAnalyticsScreen(dao: CocoaDao) {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Investments (SIP / HSBC), Accounts & Analytics", color = DeepEspressoText, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsAndLegalScreen(dao: CocoaDao) {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Settings, Recurring Commitments & Offline Legal Center", color = DeepEspressoText, fontWeight = FontWeight.Bold)
    }
}
