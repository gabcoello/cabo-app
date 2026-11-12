package com.mercadosmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mercadosmart.data.model.PaymentBudget
import com.mercadosmart.data.model.PaymentTypes
import com.mercadosmart.ui.theme.*

fun iconForPaymentType(type: String): ImageVector = when (type) {
    PaymentTypes.TICKET -> Icons.Default.Restaurant
    PaymentTypes.CREDIT -> Icons.Default.CreditCard
    PaymentTypes.DEBIT -> Icons.Default.AccountBalanceWallet
    else -> Icons.Default.Payments
}

@Composable
fun PaymentBudgetDialog(
    budgets: List<PaymentBudget>,
    totalBudget: Double,
    onDismiss: () -> Unit,
    onSaveBudget: (PaymentBudget) -> Unit,
    onDeleteBudget: (PaymentBudget) -> Unit
) {
    var editingBudget by remember { mutableStateOf<PaymentBudget?>(null) }
    var selectedType by remember { mutableStateOf(PaymentTypes.TICKET) }
    var label by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    fun startEdit(budget: PaymentBudget) {
        editingBudget = budget
        selectedType = budget.type
        label = budget.label
        amountText = String.format("%.2f", budget.amount).replace(".", ",")
    }

    fun resetForm() {
        editingBudget = null
        selectedType = PaymentTypes.TICKET
        label = ""
        amountText = ""
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
                .heightIn(max = 660.dp)
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
                        Text("Meu Orçamento", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Gerencie seus valores disponíveis",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card do Somatório Total
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryLight)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total Disponível",
                                color = PrimaryDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                "${budgets.size} ${if (budgets.size == 1) "método cadastrado" else "métodos cadastrados"}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "R$ ${String.format("%.2f", totalBudget)}",
                            color = PrimaryDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                }

                // Lista de métodos já cadastrados
                if (budgets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("MÉTODOS CADASTRADOS", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        budgets.forEach { budget ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Background)
                                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        iconForPaymentType(budget.type),
                                        contentDescription = null,
                                        tint = PrimaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = budget.displayName(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "R$ ${String.format("%.2f", budget.amount)}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = PrimaryDark
                                    )
                                }

                                IconButton(
                                    onClick = { startEdit(budget) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Accent, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (editingBudget?.id == budget.id) resetForm()
                                        onDeleteBudget(budget)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Border)
                Spacer(modifier = Modifier.height(14.dp))

                // Formulário para adicionar ou editar
                Text(
                    text = if (editingBudget == null) "ADICIONAR NOVO VALOR" else "EDITAR MÉTODO SELECIONADO",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("TIPO DE PAGAMENTO *", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentTypes.all.forEach { type ->
                        val isSelected = selectedType == type
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Primary else TagBg)
                                .border(1.dp, if (isSelected) Primary else Border, RoundedCornerShape(16.dp))
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                iconForPaymentType(type),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = type,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("VALOR (R$) *", style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            placeholder = { Text("0,00", color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("APELIDO (OPCIONAL)", style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            placeholder = { Text("Ex: Alelo, Nubank", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val parsedAmount = amountText.replace(",", ".").toDoubleOrNull()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editingBudget != null) {
                        OutlinedButton(
                            onClick = { resetForm() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Cancelar")
                        }
                    }

                    Button(
                        onClick = {
                            if (parsedAmount != null && parsedAmount >= 0.0) {
                                onSaveBudget(
                                    editingBudget?.copy(
                                        type = selectedType,
                                        label = label.trim(),
                                        amount = parsedAmount
                                    ) ?: PaymentBudget(
                                        type = selectedType,
                                        label = label.trim(),
                                        amount = parsedAmount
                                    )
                                )
                                resetForm()
                            }
                        },
                        enabled = parsedAmount != null && parsedAmount >= 0.0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = if (editingBudget == null) "Adicionar Valor" else "Atualizar Valor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
