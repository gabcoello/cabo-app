package com.mercadosmart.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mercadosmart.data.model.PurchaseHistory
import com.mercadosmart.data.model.PurchaseItem
import com.mercadosmart.ui.theme.*
import com.mercadosmart.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateToCompare: (String) -> Unit
) {
    val context = LocalContext.current
    val historyList by viewModel.historyList.collectAsState()
    var search by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    val gson = remember { Gson() }

    val filtered = historyList.filter {
        it.place.contains(search, ignoreCase = true) || it.itemsJson.contains(search, ignoreCase = true)
    }

    val dateFormat = remember { SimpleDateFormat("dd 'de' MMM, yyyy - HH:mm", Locale("pt", "BR")) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Barra de busca
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Buscar por local ou produto...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (search.isNotEmpty()) {
                    IconButton(onClick = { search = "" }) {
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
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp)
        )

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nenhum histórico encontrado", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Conforme você concluir suas compras na aba Lista, elas ficarão guardadas aqui com data, local e valores.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    val isExpanded = expandedId == item.id
                    val purchaseItems: List<PurchaseItem> = remember(item.itemsJson) {
                        try {
                            val type = object : TypeToken<List<PurchaseItem>>() {}.type
                            gson.fromJson(item.itemsJson, type) ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header clicável
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedId = if (isExpanded) null else item.id }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.place, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    Text(
                                        text = dateFormat.format(Date(item.date)),
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    if (item.paymentMethod.isNotBlank()) {
                                        Text(
                                            text = "Pago com: ${item.paymentMethod}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Accent
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "R$ ${String.format("%.2f", item.total)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = PrimaryDark
                                    )
                                    Icon(
                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Conteúdo Expandido
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    HorizontalDivider(color = Border)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        "PRODUTOS COMPRADOS (${purchaseItems.size})",
                                        style = MaterialTheme.typography.labelSmall
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    purchaseItems.forEach { prod ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onNavigateToCompare(prod.name) }
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(prod.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                                                    Text(" 🔍", fontSize = 11.sp)
                                                }
                                                Text(
                                                    text = prod.formattedMeasure(),
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            Text(
                                                text = "R$ ${String.format("%.2f", prod.quantity * prod.price)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(PrimaryLight)
                                                    .clickable {
                                                        viewModel.addHistoryItemToList(prod)
                                                        Toast.makeText(
                                                            context,
                                                            "${prod.name} adicionado à lista!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.AddShoppingCart,
                                                        contentDescription = "Adicionar à lista",
                                                        tint = PrimaryDark,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        "+ Lista",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryDark
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (item.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(TagBg)
                                                .padding(8.dp)
                                        ) {
                                            Text(item.notes, fontSize = 12.sp, color = TextSecondary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Ações do card
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { viewModel.repeatPurchase(item.itemsJson) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copiar para Lista", color = PrimaryDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        IconButton(onClick = { viewModel.deleteHistory(item) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Danger, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
