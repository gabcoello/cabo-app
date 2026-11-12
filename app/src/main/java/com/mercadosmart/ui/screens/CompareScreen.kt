package com.mercadosmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercadosmart.ui.theme.*
import com.mercadosmart.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompareScreen(
    viewModel: MainViewModel,
    initialSearch: String = ""
) {
    val comparisons by viewModel.priceComparisons.collectAsState()
    var search by remember { mutableStateOf(initialSearch) }

    val filtered = comparisons.filter {
        it.name.contains(search, ignoreCase = true)
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Barra de Busca
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Pesquisar produto para ver histórico de preços...", color = TextMuted, fontSize = 13.sp) },
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

        // Banner informativo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentLight)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Insights, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Compare onde cada item foi mais barato ao longo do tempo para economizar em listas futuras!",
                color = Accent,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sem dados suficientes", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Quando você salvar compras com preço e local, este comparador exibirá onde cada item foi mais barato.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.name }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top: Nome do produto e Badge de Melhor Preço
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    Text("${item.totalRecords} registros de compra", fontSize = 12.sp, color = TextSecondary)
                                }

                                if (item.minPrice > 0) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (item.allPricesEqual) SurfaceVariant else PrimaryLight)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (item.allPricesEqual) "MESMO VALOR" else "MELHOR PREÇO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.allPricesEqual) TextSecondary else PrimaryDark
                                        )
                                        Text(
                                            "R$ ${String.format("%.2f", item.minPrice)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = if (item.allPricesEqual) TextPrimary else PrimaryDark
                                        )
                                        Text(
                                            text = if (item.allPricesEqual) "Os valores são os mesmos" else "em ${item.bestPlace}",
                                            fontSize = 11.sp,
                                            color = if (item.allPricesEqual) TextSecondary else PrimaryDark
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Lista de ocorrências
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Background)
                                    .padding(8.dp)
                            ) {
                                Text("HISTÓRICO DE LOCAIS E VALORES", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))

                                item.records.forEach { record ->
                                    val isLowest = !item.allPricesEqual && record.price == item.minPrice && item.minPrice > 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isLowest) PrimaryLight else Color.Transparent)
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = if (isLowest) PrimaryDark else TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = record.place,
                                                fontSize = 13.sp,
                                                fontWeight = if (isLowest) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isLowest) PrimaryDark else TextPrimary
                                            )
                                        }

                                        Text(
                                            text = dateFormat.format(Date(record.date)),
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )

                                        Text(
                                            text = "R$ ${String.format("%.2f", record.price)}/${record.unit}",
                                            fontSize = 13.sp,
                                            fontWeight = if (isLowest) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isLowest) PrimaryDark else TextPrimary
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
}
