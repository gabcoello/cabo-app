package com.mercadosmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercadosmart.ui.components.ExpensesDialog
import com.mercadosmart.ui.components.PaymentBudgetDialog
import com.mercadosmart.ui.screens.CompareScreen
import com.mercadosmart.ui.screens.HistoryScreen
import com.mercadosmart.ui.screens.RemindersScreen
import com.mercadosmart.ui.screens.ShoppingListScreen
import com.mercadosmart.ui.theme.Border
import com.mercadosmart.ui.theme.Danger
import com.mercadosmart.ui.theme.DangerLight
import com.mercadosmart.ui.theme.MercadoSmartTheme
import com.mercadosmart.ui.theme.Primary
import com.mercadosmart.ui.theme.PrimaryDark
import com.mercadosmart.ui.theme.PrimaryLight
import com.mercadosmart.ui.theme.Surface
import com.mercadosmart.ui.theme.TextMuted
import com.mercadosmart.ui.theme.TextPrimary
import com.mercadosmart.ui.theme.TextSecondary
import com.mercadosmart.viewmodel.MainViewModel

enum class Screen(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    LIST("Lista", Icons.Filled.FormatListBulleted, Icons.Outlined.FormatListBulleted),
    HISTORY("Histórico", Icons.Filled.History, Icons.Outlined.History),
    COMPARE("Comparar", Icons.Filled.PriceChange, Icons.Outlined.PriceChange),
    REMINDERS("Lembretes", Icons.Filled.Alarm, Icons.Outlined.Alarm)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            MercadoSmartTheme {
                var currentScreen by remember { mutableStateOf(Screen.LIST) }
                var compareInitialSearch by remember { mutableStateOf("") }
                var showBudgetDialog by remember { mutableStateOf(false) }
                var showExpensesDialog by remember { mutableStateOf(false) }

                val paymentBudgets by viewModel.paymentBudgets.collectAsState()
                val totalBudget by viewModel.totalBudget.collectAsState()
                val totalExpenses by viewModel.totalExpenses.collectAsState()
                val expensesByMethod by viewModel.expensesByMethod.collectAsState()
                val historyList by viewModel.historyList.collectAsState()

                Scaffold(
                    topBar = {
                        // Header customizado moderno com miniaturas de Orçamento e Gastos no canto superior direito
                        Surface(
                            shadowElevation = 1.dp,
                            color = Surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            "MercadoSmart",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Compras & Economia",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Miniaturas: Gastos e Disponível (visíveis em todas as telas)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Miniatura Gastos
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DangerLight)
                                            .border(1.dp, Danger.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                            .clickable { showExpensesDialog = true }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.ReceiptLong,
                                            contentDescription = "Gastos",
                                            tint = Danger,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Gastos",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Danger
                                            )
                                            Text(
                                                text = "R$ ${String.format("%.2f", totalExpenses)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Danger,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Miniatura Disponível
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PrimaryLight)
                                            .border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .clickable { showBudgetDialog = true }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = "Orçamento",
                                            tint = PrimaryDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Disponível",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryDark
                                            )
                                            Text(
                                                text = "R$ ${String.format("%.2f", totalBudget)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PrimaryDark,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            Screen.values().forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (screen == Screen.COMPARE) compareInitialSearch = ""
                                        currentScreen = screen
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            screen.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Primary,
                                        selectedTextColor = Primary,
                                        indicatorColor = com.mercadosmart.ui.theme.PrimaryLight,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.LIST -> ShoppingListScreen(viewModel = viewModel)
                            Screen.HISTORY -> HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToCompare = { query ->
                                    compareInitialSearch = query
                                    currentScreen = Screen.COMPARE
                                }
                            )
                            Screen.COMPARE -> CompareScreen(
                                viewModel = viewModel,
                                initialSearch = compareInitialSearch
                            )
                            Screen.REMINDERS -> RemindersScreen(viewModel = viewModel)
                        }
                    }

                    if (showBudgetDialog) {
                        PaymentBudgetDialog(
                            budgets = paymentBudgets,
                            totalBudget = totalBudget,
                            onDismiss = { showBudgetDialog = false },
                            onSaveBudget = { viewModel.savePaymentBudget(it) },
                            onDeleteBudget = { viewModel.deletePaymentBudget(it) }
                        )
                    }

                    if (showExpensesDialog) {
                        ExpensesDialog(
                            totalExpenses = totalExpenses,
                            expensesByMethod = expensesByMethod,
                            historyList = historyList,
                            onDismiss = { showExpensesDialog = false }
                        )
                    }
                }
            }
        }
    }
}
