package com.mercadosmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PriceChange
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercadosmart.ui.screens.CompareScreen
import com.mercadosmart.ui.screens.HistoryScreen
import com.mercadosmart.ui.screens.RemindersScreen
import com.mercadosmart.ui.screens.ShoppingListScreen
import com.mercadosmart.ui.theme.Border
import com.mercadosmart.ui.theme.MercadoSmartTheme
import com.mercadosmart.ui.theme.Primary
import com.mercadosmart.ui.theme.PrimaryDark
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

                Scaffold(
                    topBar = {
                        // Header customizado moderno
                        Surface(
                            shadowElevation = 1.dp,
                            color = Surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text("MercadoSmart", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = TextPrimary)
                                    Text("Compras, Economia & Histórico", fontSize = 11.sp, color = TextSecondary)
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
                }
            }
        }
    }
}
