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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.mercadosmart.data.model.PaymentBudget
import com.mercadosmart.data.model.PaymentTypes
import com.mercadosmart.data.model.ShoppingItem
import com.mercadosmart.ui.theme.*

@Composable
fun FinishPurchaseDialog(
    items: List<ShoppingItem>,
    totalAmount: Double,
    paymentBudgets: List<PaymentBudget>,
    onDismiss: () -> Unit,
    onConfirm: (place: String, notes: String, paymentBudgetId: Long?, fallbackPaymentType: String?, fallbackPaymentLabel: String) -> Unit
) {
    var place by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedBudgetId by remember(paymentBudgets) {
        mutableStateOf(paymentBudgets.firstOrNull()?.id)
    }
    var selectedFallbackType by remember(paymentBudgets) {
        mutableStateOf<String?>(if (paymentBudgets.isEmpty()) PaymentTypes.DEBIT else null)
    }
    var fallbackPaymentLabel by remember { mutableStateOf("") }

    val recentPlaces = listOf("Atacadão", "Assaí", "Carrefour", "Pão de Açúcar", "Feira Livre", "Mercado Local")
    val checkedCount = items.count { it.isChecked }
    val selectedBudget = paymentBudgets.firstOrNull { it.id == selectedBudgetId }

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
                        Text("Finalizar Compra", style = MaterialTheme.typography.titleLarge)
                        Text("Salvar histórico de preços", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resumo do Total
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
                            Text("Total da Compra", color = PrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("$checkedCount de ${items.size} itens no carrinho", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text(
                            text = "R$ ${String.format("%.2f", totalAmount)}",
                            color = PrimaryDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Local
                Text("ONDE VOCÊ COMPROU? *", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    placeholder = { Text("Ex: Assaí Atacadista, Feira...", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sugestões rápidas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    recentPlaces.forEach { p ->
                        val isSelected = place == p
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Primary else TagBg)
                                .border(1.dp, if (isSelected) Primary else Border, RoundedCornerShape(16.dp))
                                .clickable { place = p }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = p,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campo PAGO COM
                Text("PAGO COM", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))

                if (paymentBudgets.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        paymentBudgets.forEach { budget ->
                            val isSelected = selectedBudgetId == budget.id
                            val isZeroed = budget.amount <= 0.0
                            val remaining = (budget.amount - totalAmount).coerceAtLeast(0.0)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryLight else Background)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Primary else Border,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            selectedBudgetId = null
                                        } else {
                                            selectedBudgetId = budget.id
                                            selectedFallbackType = null
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    iconForPaymentType(budget.type),
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryDark else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = budget.displayName(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PrimaryDark else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = when {
                                            isZeroed -> "Saldo zerado • será adicionado aos gastos"
                                            isSelected -> "R$ ${String.format("%.2f", budget.amount)} → R$ ${String.format("%.2f", remaining)}"
                                            else -> "Disponível: R$ ${String.format("%.2f", budget.amount)}"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isSelected) PrimaryDark else TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selecionado",
                                        tint = PrimaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedBudget != null && selectedBudget.amount > 0.0 && totalAmount > selectedBudget.amount) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "O total supera o saldo de ${selectedBudget.displayName()}. O saldo será zerado e o valor completo irá para os gastos.",
                            fontSize = 11.sp,
                            color = Warning
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("OU ESCOLHA OUTRO MEIO (SEM ORÇAMENTO)", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                } else {
                    Text(
                        text = "Selecione o meio de pagamento (será registrado na área de gastos):",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Meios de pagamento padrão (para quando orçamento estiver zerado ou não cadastrado)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentTypes.all.forEach { type ->
                        val isSelected = selectedBudgetId == null && selectedFallbackType == type
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Accent else TagBg)
                                .border(1.dp, if (isSelected) Accent else Border, RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedBudgetId = null
                                    selectedFallbackType = if (isSelected) null else type
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                iconForPaymentType(type),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = PaymentTypes.shortLabel(type),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                if (selectedBudgetId == null && selectedFallbackType != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("APELIDO DO MEIO NÃO CADASTRADO (OPCIONAL)", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = fallbackPaymentLabel,
                        onValueChange = { fallbackPaymentLabel = it },
                        placeholder = { Text("Ex: Cartão extra, Dinheiro, Nubank...", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Observações
                Text("OBSERVAÇÕES (OPCIONAL)", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Ex: Desconto de R$ 5,00...", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (place.isNotBlank()) {
                            onConfirm(
                                place.trim(),
                                notes.trim(),
                                selectedBudgetId,
                                selectedFallbackType,
                                fallbackPaymentLabel.trim()
                            )
                            onDismiss()
                        }
                    },
                    enabled = place.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Salvar no Histórico e Concluir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
