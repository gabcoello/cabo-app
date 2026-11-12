package com.mercadosmart.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercadosmart.data.model.ShoppingItem
import com.mercadosmart.ui.components.FinishPurchaseDialog
import com.mercadosmart.ui.components.ItemDialog
import com.mercadosmart.ui.theme.*
import com.mercadosmart.viewmodel.MainViewModel

@Composable
fun ShoppingListScreen(viewModel: MainViewModel) {
    val items by viewModel.shoppingItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }

    var showItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ShoppingItem?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }

    // Totais
    val totalPredicted = items.sumOf { it.price * it.quantity }
    val checkedItems = items.filter { it.isChecked }
    val totalChecked = checkedItems.sumOf { it.price * it.quantity }

    // Filtros
    val categories = listOf("Todas") + items.map { it.category }.distinct()
    val filteredItems = items.filter {
        val matchesSearch = it.name.contains(searchQuery, ignoreCase = true)
        val matchesCat = selectedCategory == "Todas" || it.category == selectedCategory
        matchesSearch && matchesCat
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Card de Totais (Previsto vs No Carrinho)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Previsto", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "R$ ${String.format("%.2f", totalPredicted)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Border)
                    )

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No Carrinho (${checkedItems.size}/${items.size})",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "R$ ${String.format("%.2f", totalChecked)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryDark
                        )
                    }
                }
            }

            // Barra de Busca e Botão Concluir
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar na lista...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )

                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showFinishDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Concluir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Filtro por Categoria se houver mais de uma
            if (categories.size > 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) PrimaryLight else Surface)
                                .border(1.dp, if (isSelected) Primary else Border, RoundedCornerShape(16.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryDark else TextSecondary
                            )
                        }
                    }
                }
            }

            // Lista de Itens
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sua lista está vazia", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Toque no botão + para adicionar os produtos que você precisa comprar.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val cardBg by animateColorAsState(if (item.isChecked) Color(0xFFF8FAFC) else Surface)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox customizado
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (item.isChecked) Primary else Color.Transparent)
                                        .border(2.dp, Primary, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.toggleItemCheck(item) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.isChecked) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Dados do produto
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            itemToEdit = item
                                            showItemDialog = true
                                        }
                                ) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (item.isChecked) TextMuted else TextPrimary,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.quantity} ${item.unit}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(TagBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.category,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(TagBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Subtotal do item
                                if (item.price > 0) {
                                    Text(
                                        text = "R$ ${String.format("%.2f", item.price * item.quantity)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (item.isChecked) TextMuted else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }

                                // Botão Deletar
                                IconButton(
                                    onClick = { viewModel.deleteItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB Adicionar
        FloatingActionButton(
            onClick = {
                itemToEdit = null
                showItemDialog = true
            },
            shape = CircleShape,
            containerColor = Primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar", modifier = Modifier.size(28.dp))
        }

        // Dialogs
        if (showItemDialog) {
            val tip = itemToEdit?.let { viewModel.getLatestPriceFor(it.name) }
            ItemDialog(
                itemToEdit = itemToEdit,
                latestPriceTip = tip,
                onDismiss = { showItemDialog = false },
                onSave = {
                    if (itemToEdit == null) viewModel.addItem(it) else viewModel.updateItem(it)
                }
            )
        }

        if (showFinishDialog) {
            val paymentBudgets by viewModel.paymentBudgets.collectAsState()
            val purchaseTotal = if (totalChecked > 0) totalChecked else totalPredicted
            FinishPurchaseDialog(
                items = items,
                totalAmount = purchaseTotal,
                paymentBudgets = paymentBudgets,
                onDismiss = { showFinishDialog = false },
                onConfirm = { place, notes, paymentBudgetId ->
                    viewModel.finishPurchase(place, notes, items, purchaseTotal, paymentBudgetId)
                }
            )
        }
    }
}
