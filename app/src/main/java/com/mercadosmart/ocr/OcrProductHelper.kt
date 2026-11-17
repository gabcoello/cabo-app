package com.mercadosmart.ocr

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

data class OcrScanResult(
    val name: String = "",
    val price: String = "",
    val isPerKg: Boolean = false
)

object OcrProductHelper {

    fun createTempImageUri(context: Context): Uri {
        val dir = File(context.cacheDir, "ocr_images").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun recognizeProductFromUri(
        context: Context,
        uri: Uri,
        onResult: (OcrScanResult) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val parsed = parseTextToProduct(visionText.text)
                    onResult(parsed)
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Falha ao ler texto da imagem")
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Erro ao carregar imagem")
        }
    }

    /**
     * Extrai nome provável e preço a partir do texto bruto reconhecido na etiqueta/produto.
     */
    internal fun parseTextToProduct(rawText: String): OcrScanResult {
        if (rawText.isBlank()) return OcrScanResult()

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val fullLower = rawText.lowercase()
        val isPerKg = fullLower.contains("/kg") ||
            fullLower.contains("o kg") ||
            fullLower.contains("por kg") ||
            Regex("""\bkg\b""").containsMatchIn(fullLower) && fullLower.contains("r$")

        // 1. Buscar preço: prioriza padrão com R$, depois qualquer valor decimal XX,XX ou XX.XX
        val currencyRegex = Regex("""R\$\s*(\d{1,4}[.,]\d{2})""", RegexOption.IGNORE_CASE)
        val decimalRegex = Regex("""(?<!\d)(\d{1,4}[.,]\d{2})(?!\d)""")

        var extractedPrice = ""
        val currencyMatch = currencyRegex.find(rawText)
        if (currencyMatch != null) {
            extractedPrice = currencyMatch.groupValues[1].replace(",", ".")
        } else {
            // Pega candidatos decimais que não parecem data ou código longo
            val candidates = decimalRegex.findAll(rawText)
                .map { it.groupValues[1].replace(",", ".") }
                .filter {
                    val v = it.toDoubleOrNull() ?: 0.0
                    v in 0.10..5000.0
                }
                .toList()
            if (candidates.isNotEmpty()) {
                extractedPrice = candidates.first()
            }
        }

        // 2. Buscar nome provável do produto
        val noiseWords = setOf(
            "r$", "oferta", "promocao", "promoção", "preco", "preço", "por", "de",
            "atacado", "varejo", "clube", "fidelidade", "unidade", "un", "kg",
            "validade", "val", "cod", "código", "codigo", "economize", "desconto"
        )

        val candidateLines = lines.filter { line ->
            val lower = line.lowercase()
            val hasLetters = line.any { it.isLetter() }
            val isMostlyPrice = currencyRegex.matches(line) || decimalRegex.matches(line)
            val isBarcode = line.count { it.isDigit() } >= 8 && !hasLetters
            val isOnlyNoise = lower.split(Regex("\\s+")).all { token ->
                val clean = token.trim('.', ',', ':', ';', '-', '/')
                clean.isEmpty() || clean in noiseWords || clean.all { it.isDigit() }
            }
            hasLetters && !isMostlyPrice && !isBarcode && !isOnlyNoise && line.length >= 3
        }

        val bestName = candidateLines
            .take(2)
            .joinToString(" ")
            .replace(Regex("""R\$\s*\d+[.,]\d{2}""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(50)

        return OcrScanResult(
            name = bestName,
            price = extractedPrice,
            isPerKg = isPerKg
        )
    }
}
