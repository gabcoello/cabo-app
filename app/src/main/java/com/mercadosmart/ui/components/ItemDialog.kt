package com.mercadosmart.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.mercadosmart.data.model.ShoppingItem
import com.mercadosmart.ocr.OcrProductHelper
import com.mercadosmart.ui.theme.*
import com.mercadosmart.viewmodel.ProductPriceRecord

@Composable
fun ItemDialog(
    itemToEdit: ShoppingItem? = null,
    latestPriceTip: ProductPriceRecord? = null,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var quantity by remember {
        mutableStateOf(
            itemToEdit?.quantity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "1"
        )
    }
    var unit by remember { mutableStateOf(itemToEdit?.unit ?: "un") }
    var price by remember {
        mutableStateOf(if ((itemToEdit?.price ?: 0.0) > 0) itemToEdit?.price.toString() else "")
    }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "Outros") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }

    // Campos para kg / g
    var pricePerKgStr by remember {
        mutableStateOf(if ((itemToEdit?.pricePerKg ?: 0.0) > 0) itemToEdit?.pricePerKg.toString() else "")
    }
    var weightAmountStr by remember {
        mutableStateOf(
            if ((itemToEdit?.weightAmount ?: 0.0) > 0) {
                val w = itemToEdit!!.weightAmount
                if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
            } else ""
        )
    }

    // Campos informativos para pct / cx
    var packageSizeStr by remember {
        mutableStateOf(
            if ((itemToEdit?.packageSize ?: 0.0) > 0) {
                val s = itemToEdit!!.packageSize
                if (s % 1.0 == 0.0) s.toInt().toString() else s.toString()
            } else ""
        )
    }
    var packageSizeUnit by remember {
        mutableStateOf(itemToEdit?.packageSizeUnit?.ifBlank { null } ?: if (unit == "cx") "un" else "g")
    }

    // Estado de OCR pela câmera
    var isScanning by remember { mutableStateOf(false) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            isScanning = true
            scanFeedback = null
            OcrProductHelper.recognizeProductFromUri(
                context = context,
                uri = uri,
                onResult = { result ->
                    isScanning = false
                    if (result.name.isNotBlank()) name = result.name
                    if (result.price.isNotBlank()) {
                        if (result.isPerKg) {
                            unit = "kg"
                            pricePerKgStr = result.price
                        } else {
                            price = result.price
                        }
                    }
                    scanFeedback = if (result.name.isBlank() && result.price.isBlank()) {
                        "Não foi possível identificar nome ou preço. Tente aproximar mais da etiqueta."
                    } else {
                        "Campos preenchidos! Confira e edite se necessário."
                    }
                },
                onError = { err ->
                    isScanning = false
                    scanFeedback = "Erro ao ler etiqueta: $err"
                }
            )
        }
    }

    val launchCamera = {
        val uri = OcrProductHelper.createTempImageUri(context)
        pendingPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            scanFeedback = "Permissão de câmera necessária para escanear produtos."
        }
    }

    val units = listOf("un", "kg", "g", "L", "ml", "pct", "cx")
    val packageUnits = listOf("kg", "g", "un")
    val categories = listOf(
        "Hortifruti", "Açougue & Frios", "Laticínios & Ovos",
        "Padaria & Cereais", "Bebidas", "Higiene & Beleza", "Limpeza", "Outros"
    )

    val isWeightUnit = unit == "kg" || unit == "g"
    val isPackUnit = unit == "pct" || unit == "cx"

    val parsedPricePerKg = pricePerKgStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val parsedWeight = weightAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val calculatedWeightTotal = if (unit == "kg") {
        parsedPricePerKg * parsedWeight
    } else {
        parsedPricePerKg * (parsedWeight / 1000.0)
    }

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
                    .verticalScroll(rememberScrollState())
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botão de Câmera para escanear produto/etiqueta
                        FilledTonalButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    launchCamera()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PrimaryLight,
                                contentColor = PrimaryDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Escanear", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Escanear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                        }
                    }
                }

                if (isScanning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryContainer)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Identificando nome e preço...", fontSize = 12.sp, color = PrimaryDark, fontWeight = FontWeight.SemiBold)
                    }
                } else if (scanFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentLight)
                            .padding(10.dp)
                    ) {
                        Text(scanFeedback!!, fontSize = 12.sp, color = Accent)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nome do Produto
                Text("PRODUTO *", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ex: Arroz 5kg, Café, Frutas...", color = TextMuted) },
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
                            text = "Último: R$ ${String.format("%.2f", latestPriceTip.price)}/${latestPriceTip.unit} em ${latestPriceTip.place}",
                            fontSize = 12.sp,
                            color = Accent,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            if (isWeightUnit) {
                                pricePerKgStr = latestPriceTip.price.toString()
                            } else {
                                price = latestPriceTip.price.toString()
                            }
                        }) {
                            Text("Usar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Accent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unidade de Medida
                Text("UNIDADE DE MEDIDA", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    units.forEach { u ->
                        val selected = unit == u
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Primary else TagBg)
                                .clickable {
                                    unit = u
                                    if (u == "cx" && packageSizeUnit.isBlank()) packageSizeUnit = "un"
                                    if (u == "pct" && packageSizeUnit.isBlank()) packageSizeUnit = "g"
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
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

                Spacer(modifier = Modifier.height(10.dp))

                if (isWeightUnit) {
                    // Campos específicos para kg ou g
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VALOR POR KG (R$/kg)", style = MaterialTheme.typography.labelSmall)
                            OutlinedTextField(
                                value = pricePerKgStr,
                                onValueChange = { pricePerKgStr = it },
                                placeholder = { Text("0.00", color = TextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (unit == "kg") "PESO (kg)" else "PESO (g)",
                                style = MaterialTheme.typography.labelSmall
                            )
                            OutlinedTextField(
                                value = weightAmountStr,
                                onValueChange = { weightAmountStr = it },
                                placeholder = { Text(if (unit == "kg") "Ex: 1.5" else "Ex: 500", color = TextMuted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryContainer)
                            .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Valor gasto no produto:", fontSize = 12.sp, color = PrimaryDark, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "R$ ${String.format("%.2f", calculatedWeightTotal)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryDark
                        )
                    }
                } else {
                    // Campos padrão de Quantidade e Preço
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
                        }
                    }

                    // Campos informativos para pacote (pct) ou caixa (cx)
                    if (isPackUnit) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Background)
                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (unit == "pct") "CONTEÚDO DO PACOTE (INFORMATIVO)" else "CONTEÚDO DA CAIXA (INFORMATIVO)",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = packageSizeStr,
                                    onValueChange = { packageSizeStr = it },
                                    placeholder = { Text(if (unit == "pct") "Ex: 250, 500" else "Ex: 12, 24", color = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    packageUnits.forEach { pu ->
                                        val sel = packageSizeUnit == pu
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (sel) Primary else Surface)
                                                .border(1.dp, if (sel) Primary else Border, RoundedCornerShape(8.dp))
                                                .clickable { packageSizeUnit = pu }
                                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = pu,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sel) Color.White else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Apenas informativo para não misturar tamanhos diferentes no comparador de preços.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

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
                        val finalQty: Double
                        val finalPrice: Double
                        val finalPricePerKg: Double
                        val finalWeightAmount: Double
                        val finalPkgSize: Double
                        val finalPkgUnit: String

                        if (isWeightUnit) {
                            finalQty = 1.0
                            finalPrice = calculatedWeightTotal
                            finalPricePerKg = parsedPricePerKg
                            finalWeightAmount = parsedWeight
                            finalPkgSize = 0.0
                            finalPkgUnit = ""
                        } else {
                            finalQty = quantity.replace(",", ".").toDoubleOrNull() ?: 1.0
                            finalPrice = price.replace(",", ".").toDoubleOrNull() ?: 0.0
                            finalPricePerKg = 0.0
                            finalWeightAmount = 0.0
                            if (isPackUnit) {
                                finalPkgSize = packageSizeStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                finalPkgUnit = if (finalPkgSize > 0) packageSizeUnit else ""
                            } else {
                                finalPkgSize = 0.0
                                finalPkgUnit = ""
                            }
                        }

                        onSave(
                            itemToEdit?.copy(
                                name = name.trim(),
                                quantity = finalQty,
                                unit = unit,
                                price = finalPrice,
                                category = category,
                                notes = notes.trim(),
                                pricePerKg = finalPricePerKg,
                                weightAmount = finalWeightAmount,
                                packageSize = finalPkgSize,
                                packageSizeUnit = finalPkgUnit
                            ) ?: ShoppingItem(
                                name = name.trim(),
                                quantity = finalQty,
                                unit = unit,
                                price = finalPrice,
                                category = category,
                                notes = notes.trim(),
                                pricePerKg = finalPricePerKg,
                                weightAmount = finalWeightAmount,
                                packageSize = finalPkgSize,
                                packageSizeUnit = finalPkgUnit
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
