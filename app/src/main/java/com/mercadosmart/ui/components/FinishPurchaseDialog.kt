package com.mercadosmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mercadosmart.data.model.ShoppingItem
import com.mercadosmart.ui.theme.*

@Composable
fun FinishPurchaseDialog(
    items: List<ShoppingItem>,
    totalAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (place: String, notes: String) -> Unit
) {
    var place by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val recentPlaces = listOf("Atacadão", "Assaí", "Carrefour", "Pão de Açúcar", "Feira Livre", "Mercado Local")
    val checkedCount = items.count { it.isChecked }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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

                Spacer(modifier = Modifier.height(12.dp))

                // Observações
                Text("OBSERVAÇÕES (OPCIONAL)", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Ex: Pago no Pix, desconto de R$ 5...", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (place.isNotBlank()) {
                            onConfirm(place.trim(), notes.trim())
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
