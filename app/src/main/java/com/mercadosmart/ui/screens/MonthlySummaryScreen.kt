package com.mercadosmart.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mercadosmart.data.model.MethodOverBudgetDetail
import com.mercadosmart.data.model.PurchasePeriodActivity
import com.mercadosmart.ui.components.formatMonthShort
import com.mercadosmart.ui.theme.*
import com.mercadosmart.viewmodel.MainViewModel

@Composable
fun MonthlySummaryScreen(viewModel: MainViewModel) {
    val summaries by viewModel.monthlySummaries.collectAsState()
    val currentMonthSummary by viewModel.currentMonthSummary.collectAsState()
    val gson = remember { Gson() }

    var selectedMonthKey by remember(summaries) {
        mutableStateOf(currentMonthSummary?.monthKey ?: summaries.firstOrNull()?.monthKey)
    }
    val selectedSummary = summaries.firstOrNull { it.monthKey == selectedMonthKey } ?: currentMonthSummary ?: summaries.firstOrNull()

    var isDetailsExpanded by remember(selectedSummary?.monthKey) { mutableStateOf(false) }

    val methodDetails: List<MethodOverBudgetDetail> = remember(selectedSummary?.methodDetailsJson) {
        try {
            val type = object : TypeToken<List<MethodOverBudgetDetail>>() {}.type
            selectedSummary?.methodDetailsJson?.let { gson.fromJson(it, type) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val peakPeriods: List<PurchasePeriodActivity> = remember(selectedSummary?.peakPeriodsJson) {
        try {
            val type = object : TypeToken<List<PurchasePeriodActivity>>() {}.type
            selectedSummary?.peakPeriodsJson?.let { gson.fromJson(it, type) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Seletor de Meses (Mês Atual + Meses Anteriores Fechados)
        if (summaries.isNotEmpty()) {
            Text("MESES ARMAZENADOS", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                summaries.forEach { summary ->
                    val isSelected = summary.monthKey == selectedSummary?.monthKey
                    val label = "${formatMonthShort(summary.month, summary.year)} ${if (summary.isClosed) "(Fechado)" else "(Atual)"}"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Primary else Surface)
                            .border(1.dp, if (isSelected) Primary else Border, RoundedCornerShape(16.dp))
                            .clickable { selectedMonthKey = summary.monthKey }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (selectedSummary == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sem dados mensais ainda", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Cadastre seu orçamento e conclua compras para acompanhar os valores excedidos por mês.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            val isOver = selectedSummary.overBudgetAmount > 0.0
            val usagePercent = if (selectedSummary.totalBudget > 0.0) {
                (selectedSummary.totalSpent / selectedSummary.totalBudget) * 100.0
            } else {
                0.0
            }
            val barFraction = if (isOver) {
                (selectedSummary.overBudgetPercent / 100.0).toFloat().coerceIn(0f, 1f)
            } else {
                (usagePercent / 100.0).toFloat().coerceIn(0f, 1f)
            }

            // Card Simplificado (Total gasto no mês + Barra verde até zerar o orçamento, depois reinicia em vermelho)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOver) Danger.copy(alpha = 0.35f) else Border
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDetailsExpanded = !isDetailsExpanded }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Gasto • ${formatMonthShort(selectedSummary.month, selectedSummary.year)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "R$ ${String.format("%.2f", selectedSummary.totalSpent)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOver) Danger else PrimaryDark
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isOver) DangerLight else PrimaryLight)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isOver) {
                                        "+${String.format("%.1f", selectedSummary.overBudgetPercent)}% além"
                                    } else {
                                        "${String.format("%.1f", usagePercent)}% usado"
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isOver) Danger else PrimaryDark
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isDetailsExpanded) "Ocultar detalhes" else "Toque para detalhar",
                                    fontSize = 11.sp,
                                    color = Accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    if (isDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { barFraction },
                        color = if (isOver) Danger else Primary,
                        trackColor = if (isOver) DangerLight else PrimaryLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Orçamento: R$ ${String.format("%.2f", selectedSummary.totalBudget)}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (isOver) {
                                "Excedido: R$ ${String.format("%.2f", selectedSummary.overBudgetAmount)}"
                            } else {
                                "Restante: R$ ${String.format("%.2f", (selectedSummary.totalBudget - selectedSummary.totalSpent).coerceAtLeast(0.0))}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOver) Danger else PrimaryDark
                        )
                    }
                }
            }

            // Detalhes Expandidos ao clicar no Card
            AnimatedVisibility(visible = isDetailsExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("DETALHAMENTO POR MEIO DE PAGAMENTO", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (methodDetails.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nenhum gasto ou orçamento registrado neste mês.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            methodDetails.forEach { detail ->
                                val methodOver = detail.overAmount > 0.0
                                val methodUsagePercent = if (detail.budgetAmount > 0.0) {
                                    (detail.spentAmount / detail.budgetAmount) * 100.0
                                } else {
                                    0.0
                                }
                                val methodFraction = if (methodOver) {
                                    (detail.overPercent / 100.0).toFloat().coerceIn(0f, 1f)
                                } else {
                                    (methodUsagePercent / 100.0).toFloat().coerceIn(0f, 1f)
                                }
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = detail.methodName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (methodOver) {
                                                    "+${String.format("%.1f", detail.overPercent)}% além (R$ ${String.format("%.2f", detail.overAmount)})"
                                                } else {
                                                    "${String.format("%.1f", methodUsagePercent)}% usado"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (methodOver) Danger else PrimaryDark
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        LinearProgressIndicator(
                                            progress = { methodFraction },
                                            color = if (methodOver) Danger else Primary,
                                            trackColor = if (methodOver) DangerLight else PrimaryLight,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Orçado: R$ ${String.format("%.2f", detail.budgetAmount)}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "Gasto: R$ ${String.format("%.2f", detail.spentAmount)}",
                                                fontSize = 11.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Períodos de Maior Atividade de Compra no Mês
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("PERÍODOS DE MAIOR ATIVIDADE DE COMPRA", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (peakPeriods.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nenhuma compra registrada neste mês ainda.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            peakPeriods.forEach { period ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (period.isPeak) WarningLight else Surface)
                                        .border(
                                            1.dp,
                                            if (period.isPeak) Warning else Border,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            if (period.isPeak) Icons.Default.LocalFireDepartment else Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = if (period.isPeak) Warning else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = period.periodLabel,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                                if (period.isPeak) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Pico",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Warning)
                                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${period.purchaseCount} ${if (period.purchaseCount == 1) "compra" else "compras"}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "R$ ${String.format("%.2f", period.totalSpent)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
