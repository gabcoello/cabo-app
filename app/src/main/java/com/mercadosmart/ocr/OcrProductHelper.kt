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
    val isPerKg: Boolean = false,
    val weightAmount: String = "",
    val weightUnit: String = ""
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
     * Extrai nome provável, preço e peso a partir do texto bruto reconhecido na etiqueta/produto,
     * desconsiderando linhas de data, validade, fabricação, lote e tara.
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
            fullLower.contains("r$/kg") ||
            (Regex("""\bkg\b""").containsMatchIn(fullLower) && fullLower.contains("r$"))

        // 1. Buscar preço: prioriza valor por kg se indicado, ou padrão com R$, ou primeiro decimal válido
        val currencyRegex = Regex("""R\$\s*(\d{1,4}[.,]\d{2})""", RegexOption.IGNORE_CASE)
        val perKgPriceRegex = Regex("""(\d{1,4}[.,]\d{2})\s*(?:/\s*kg|por\s*kg|o\s*kg)""", RegexOption.IGNORE_CASE)
        val decimalRegex = Regex("""(?<!\d)(\d{1,4}[.,]\d{2})(?!\d)""")

        var extractedPrice = ""
        val perKgMatch = perKgPriceRegex.find(rawText)
        if (perKgMatch != null) {
            extractedPrice = perKgMatch.groupValues[1].replace(",", ".")
        } else {
            val currencyMatch = currencyRegex.find(rawText)
            if (currencyMatch != null) {
                extractedPrice = currencyMatch.groupValues[1].replace(",", ".")
            } else {
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
        }

        // 2. Buscar peso (kg ou g), ignorando linhas de tara e linhas de preço/kg
        var extractedWeight = ""
        var extractedWeightUnit = ""

        val explicitWeightRegex = Regex(
            """peso(?:\s+l[ií]q(?:uido)?)?\s*:?\s*(\d{1,4}(?:[.,]\d{1,3})?)\s*(kg|g)\b""",
            RegexOption.IGNORE_CASE
        )
        val genericWeightRegex = Regex(
            """\b(\d{1,4}(?:[.,]\d{1,3})?)\s*(kg|g)\b""",
            RegexOption.IGNORE_CASE
        )

        val nonTaraLines = lines.filter { !it.lowercase().contains("tara") }
        for (line in nonTaraLines) {
            val match = explicitWeightRegex.find(line)
            if (match != null) {
                extractedWeight = match.groupValues[1].replace(",", ".")
                extractedWeightUnit = match.groupValues[2].lowercase()
                break
            }
        }

        if (extractedWeight.isEmpty()) {
            for (line in nonTaraLines) {
                val lower = line.lowercase()
                val isPricePerKgLine = lower.contains("/kg") || lower.contains("por kg") ||
                    lower.contains("o kg") || lower.contains("r$")
                if (!isPricePerKgLine) {
                    val match = genericWeightRegex.find(line)
                    if (match != null) {
                        extractedWeight = match.groupValues[1].replace(",", ".")
                        extractedWeightUnit = match.groupValues[2].lowercase()
                        break
                    }
                }
            }
        }

        // 3. Buscar nome provável do produto, desconsiderando data, validade, fabricação, lote, tara etc.
        val ignoredLineTerms = listOf(
            "data", "validade", "val.", "val:", "fabricacao", "fabricação", "fab.", "fab:",
            "lote", "tara", "embalado", "vencimento", "venc.", "venc:", "peso liq",
            "peso líquido", "peso bruto", "cnpj", "cod.", "código", "codigo"
        )
        val dateRegex = Regex("""\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b""")

        val noiseWords = setOf(
            "r$", "oferta", "promocao", "promoção", "preco", "preço", "por", "de",
            "atacado", "varejo", "clube", "fidelidade", "unidade", "un", "kg", "g",
            "data", "validade", "val", "fabricacao", "fabricação", "fab", "lote", "tara",
            "peso", "cod", "código", "codigo", "economize", "desconto", "vencimento"
        )

        val candidateLines = lines.filter { line ->
            val lower = line.lowercase()
            val hasLetters = line.any { it.isLetter() }
            val isMostlyPrice = currencyRegex.matches(line) || decimalRegex.matches(line)
            val isBarcode = line.count { it.isDigit() } >= 8 && !hasLetters
            val hasIgnoredMetadata = ignoredLineTerms.any { term -> lower.contains(term) }
            val hasDatePattern = dateRegex.containsMatchIn(line)
            val isWeightOnlyLine = genericWeightRegex.matches(line)
            val isOnlyNoise = lower.split(Regex("\\s+")).all { token ->
                val clean = token.trim('.', ',', ':', ';', '-', '/')
                clean.isEmpty() || clean in noiseWords || clean.all { it.isDigit() }
            }
            hasLetters && !isMostlyPrice && !isBarcode && !hasIgnoredMetadata &&
                !hasDatePattern && !isWeightOnlyLine && !isOnlyNoise && line.length >= 3
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
            isPerKg = isPerKg,
            weightAmount = extractedWeight,
            weightUnit = extractedWeightUnit
        )
    }
}
