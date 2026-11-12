package com.mercadosmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mercadosmart.data.model.ShoppingItem
import com.mercadosmart.ui.theme.*
import com.mercadosmart.viewmodel.ProductPriceRecord

@Composable
fun ItemDialog(
    itemToEdit: ShoppingItem? = null,
    latestPriceTip: ProductPriceRecord? = null,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var quantity by remember { mutableStateOf(itemToEdit?.quantity?.toString()?.replace(".0", "") ?: "1") }
    var unit by remember { mutableStateOf(itemToEdit?.unit ?: "un") }
    var price by remember { mutableStateOf(if ((itemToEdit?.price ?: 0.0) > 0) itemToEdit?.price.toString() else "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "Outros") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }

    val units = listOf("un", "kg", "g", "L", "ml", "pct", "cx")
    val categories = listOf(
        "Hortifruti", "Açougue & Frios", "Laticínios & Ovos",
        "Padaria & Cereais", "Bebidas", "Higiene & Beleza", "Limpeza", "Outros"
    )

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
                    Text(
                        text = if (itemToEdit == null) "Novo Item" else "Editar Item",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nome do Produto
                Text("PRODUTO *", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ex: Arroz 5kg, Leite, Frutas...", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Dica de último preço pago se houver
                if (latestPriceTip != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentLight)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Último: R$ ${String.format("%.2f", latestPriceTip.price)} em ${latestPriceTip.place}",
                            fontSize = 12.sp,
                            color = Accent,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { price = latestPriceTip.price.toString() }) {
                            Text("Usar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Accent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quantidade e Unidade
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("QUANTIDADE", style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text("UNIDADE", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            units.forEach { u ->
                                val selected = unit == u
                                Box(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Primary else TagBg)
                                        .clickable { unit = u }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = u,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Preço Unitário
                Text("PREÇO ESTIMADO (R$)", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    placeholder = { Text("0.00", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Categorias
                Text("CATEGORIA", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    categories.forEach { cat ->
                        val selected = category == cat
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) PrimaryLight else TagBg)
                                .border(1.dp, if (selected) Primary else Border, RoundedCornerShape(16.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) PrimaryDark else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botão Salvar
                Button(
                    onClick = {
                        val parsedQty = quantity.replace(",", ".").toDoubleOrNull() ?: 1.0
                        val parsedPrice = price.replace(",", ".").toDoubleOrNull() ?: 0.0

                        onSave(
                            itemToEdit?.copy(
                                name = name.trim(),
                                quantity = parsedQty,
                                unit = unit,
                                price = parsedPrice,
                                category = category,
                                notes = notes.trim()
                            ) ?: ShoppingItem(
                                name = name.trim(),
                                quantity = parsedQty,
                                unit = unit,
                                price = parsedPrice,
                                category = category,
                                notes = notes.trim()
                            )
                        )
                        onDismiss()
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Salvar Item", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
