package com.mercadosmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mercadosmart.data.model.MethodOverBudgetDetail
import com.mercadosmart.data.model.MonthlySummary
import com.mercadosmart.data.model.PurchasePeriodActivity
import com.mercadosmart.ui.theme.*

fun formatMonthShort(month: Int, year: Int): String {
    val names = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
    val mName = names.getOrElse(month - 1) { month.toString() }
    return "$mName/$year"
}

@Composable
fun MonthlyOverBudgetDialog(
    summaries: List<MonthlySummary>,
    initialMonthKey: String? = null,
    onDismiss: () -> Unit
) {
    val gson = remember { Gson() }
    var selectedMonthKey by remember(summaries, initialMonthKey) {
        mutableStateOf(initialMonthKey ?: summaries.firstOrNull()?.monthKey)
    }
    val selectedSummary = summaries.firstOrNull { it.monthKey == selectedMonthKey } ?: summaries.firstOrNull()

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Além do Orçamento", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Acompanhamento mensal e picos de compra",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                // Seletor de Meses (Mês Atual + Meses Anteriores Fechados)
                if (summaries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("MESES ARMAZENADOS", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        summaries.forEach { summary ->
                            val isSelected = summary.monthKey == selectedSummary?.monthKey
                            val label = "${formatMonthShort(summary.month, summary.year)} ${if (summary.isClosed) "(Fechado)" else "(Atual)"}"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Primary else TagBg)
                                    .border(1.dp, if (isSelected) Primary else Border, RoundedCornerShape(16.dp))
                                    .clickable { selectedMonthKey = summary.monthKey }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                if (selectedSummary != null) {
                    Spacer(modifier = Modifier.height(14.dp))

                    val isOver = selectedSummary.overBudgetAmount > 0.0
                    val barFraction = (selectedSummary.overBudgetPercent / 100.0).toFloat().coerceIn(0f, 1f)

                    // Card Principal do Mês Selecionado
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isOver) DangerLight else PrimaryLight)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isOver) "Excedido no mês" else "Dentro do orçamento",
                                    color = if (isOver) Danger else PrimaryDark,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "R$ ${String.format("%.2f", selectedSummary.overBudgetAmount)}",
                                    color = if (isOver) Danger else PrimaryDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isOver) "+${String.format("%.1f", selectedSummary.overBudgetPercent)}%" else "0,0%",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isOver) Danger else PrimaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { barFraction },
                            color = if (isOver) Danger else Primary,
                            trackColor = Surface.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Orçamento: R$ ${String.format("%.2f", selectedSummary.totalBudget)}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Gasto: R$ ${String.format("%.2f", selectedSummary.totalSpent)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Detalhamento por Meio de Pagamento (% excedido)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("DETALHAMENTO POR MEIO DE PAGAMENTO", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (methodDetails.isEmpty()) {
                        Text(
                            text = "Nenhum gasto ou orçamento registrado neste mês.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            methodDetails.forEach { detail ->
                                val methodOver = detail.overAmount > 0.0
                                val methodFraction = (detail.overPercent / 100.0).toFloat().coerceIn(0f, 1f)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Background)
                                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
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
                                                "+${String.format("%.1f", detail.overPercent)}% (R$ ${String.format("%.2f", detail.overAmount)})"
                                            } else {
                                                "Dentro (0%)"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (methodOver) Danger else PrimaryDark
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { methodFraction },
                                        color = if (methodOver) Danger else Primary,
                                        trackColor = if (methodOver) DangerLight else PrimaryLight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
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

                    // Períodos de Maior Atividade de Compra no Mês
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PERÍODOS DE MAIOR ATIVIDADE DE COMPRA", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (peakPeriods.isEmpty()) {
                        Text(
                            text = "Nenhuma compra registrada neste mês ainda.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            peakPeriods.forEach { period ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (period.isPeak) WarningLight else Background)
                                        .border(
                                            1.dp,
                                            if (period.isPeak) Warning else Border,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = period.periodLabel,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
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
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "R$ ${String.format("%.2f", period.totalSpent)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
