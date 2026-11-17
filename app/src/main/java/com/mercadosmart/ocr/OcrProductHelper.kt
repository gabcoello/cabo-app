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
    val pricePerKg: String = "",
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
     * Extrai exclusivamente:
     * 1. Descrição do produto (1 ou mais palavras válidas)
     * 2. Peso (kg ou g)
     * 3. Preço por kg (R$/kg ou /kg)
     * 4. Valor com R$ na frente
     * Ignora todo o restante (data, validade, valido ate, embalado em, fabricacao, lote, tara, codigo de barras).
     */
    internal fun parseTextToProduct(rawText: String): OcrScanResult {
        if (rawText.isBlank()) return OcrScanResult()

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 1. Preço por kg: "R$/kg: 49,50", "/kg: 49,50" ou "49,50 /kg"
        val afterPerKgRegex = Regex(
            """(?:r\$\s*/\s*kg|/\s*kg|por\s*kg|o\s*kg)\s*[:=]?\s*(\d{1,4}[.,]\d{2})""",
            RegexOption.IGNORE_CASE
        )
        val beforePerKgRegex = Regex(
            """(\d{1,4}[.,]\d{2})\s*(?:/\s*kg|por\s*kg|o\s*kg)""",
            RegexOption.IGNORE_CASE
        )

        var extractedPricePerKg = ""
        val afterPerKgMatch = afterPerKgRegex.find(rawText)
        if (afterPerKgMatch != null) {
            extractedPricePerKg = afterPerKgMatch.groupValues[1].replace(",", ".")
        } else {
            val beforePerKgMatch = beforePerKgRegex.find(rawText)
            if (beforePerKgMatch != null) {
                extractedPricePerKg = beforePerKgMatch.groupValues[1].replace(",", ".")
            }
        }

        // 2. Valor com R$ na frente (excluindo R$/kg)
        val currencyRegex = Regex("""R\$(?!\s*/\s*kg)\s*[:=]?\s*(\d{1,4}[.,]\d{2})""", RegexOption.IGNORE_CASE)
        var extractedPrice = ""
        val currencyMatches = currencyRegex.findAll(rawText).toList()
        if (currencyMatches.isNotEmpty()) {
            // Se houver mais de um R$, pega o último (geralmente TOTAL R$) ou o primeiro válido
            extractedPrice = currencyMatches.last().groupValues[1].replace(",", ".")
        }

        // 3. Peso (kg ou g), ignorando tara e linhas de preço/kg
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

        // 4. Descrição do produto (1 ou mais palavras), ignorando todo o resto
        val ignoredLineTerms = listOf(
            "data", "validade", "valido", "válido", "val.", "val:", "fabricacao", "fabricação",
            "fab.", "fab:", "embalado", "lote", "tara", "vencimento", "venc.", "venc:",
            "peso", "total", "r$", "/kg", "cnpj", "cpf", "cod.", "código", "codigo",
            "oferta", "promocao", "promoção", "atacado", "varejo"
        )
        val dateRegex = Regex("""\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b""")
        val decimalRegex = Regex("""(?<!\d)\d{1,4}[.,]\d{2}(?!\d)""")

        val candidateLines = lines.filter { line ->
            val lower = line.lowercase()
            val hasLetters = line.any { it.isLetter() }
            val isBarcode = line.count { it.isDigit() } >= 7
            val hasIgnoredMetadata = ignoredLineTerms.any { term -> lower.contains(term) }
            val hasDatePattern = dateRegex.containsMatchIn(line)
            val isMostlyNumberOrPrice = decimalRegex.matches(line) || genericWeightRegex.matches(line)
            val wordCount = line.split(Regex("\\s+")).count { token -> token.any { it.isLetter() } }

            hasLetters && wordCount >= 1 && !isBarcode && !hasIgnoredMetadata &&
                !hasDatePattern && !isMostlyNumberOrPrice && line.length >= 3
        }

        val bestName = candidateLines
            .firstOrNull()
            .orEmpty()
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(50)

        return OcrScanResult(
            name = bestName,
            price = extractedPrice,
            pricePerKg = extractedPricePerKg,
            weightAmount = extractedWeight,
            weightUnit = extractedWeightUnit
        )
    }
}
