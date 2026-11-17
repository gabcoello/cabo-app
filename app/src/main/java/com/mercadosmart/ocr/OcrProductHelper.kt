package com.mercadosmart.ocr

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.math.abs
import kotlin.math.max

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
                    val visualText = buildVisuallyAlignedText(visionText)
                    val parsed = parseTextToProduct(visualText.ifBlank { visionText.text })
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
     * Reconstrói as linhas do OCR agrupando elementos que estão na mesma altura visual (boundingBox.centerY),
     * ordenados da esquerda para a direita. Isso evita que rótulos em coluna (ex: "PESO:", "R$/kg:")
     * fiquem separados dos seus valores ("0,608kg", "49,50").
     */
    private fun buildVisuallyAlignedText(visionText: Text): String {
        val allLines = visionText.textBlocks
            .flatMap { it.lines }
            .filter { it.text.isNotBlank() && it.boundingBox != null }
            .sortedBy { it.boundingBox!!.centerY() }

        if (allLines.isEmpty()) return visionText.text

        val rows = mutableListOf<MutableList<Text.Line>>()
        for (line in allLines) {
            val box = line.boundingBox!!
            val cy = box.centerY()
            val h = max(box.height(), 16)

            val targetRow = rows.firstOrNull { row ->
                val avgY = row.map { it.boundingBox!!.centerY() }.average()
                val avgH = row.map { max(it.boundingBox!!.height(), 16) }.average()
                abs(cy - avgY) <= max(h, avgH.toInt()) * 0.65
            }

            if (targetRow != null) {
                targetRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        return rows.joinToString("\n") { row ->
            row.sortedBy { it.boundingBox!!.left }
                .joinToString(" ") { it.text.trim() }
        }
    }

    private fun normalizeNumericNoise(text: String): String {
        return text
            // Corrige OCR lendo "O,608" ou "o.608" como letra O no lugar de 0
            .replace(Regex("""(?<![A-Za-zÀ-ÿ])[oO]\s*([.,]\s*\d)"""), "0$1")
            // Remove espaços acidentais ao redor de vírgula/ponto decimal (ex: "0, 608" -> "0,608")
            .replace(Regex("""(\d)\s*([.,])\s*(\d)"""), "$1$2$3")
            // Se houver peso sem zero à esquerda (ex: ",608kg" ou ".608 kg"), adiciona o 0
            .replace(Regex("""(?<!\d)([.,]\d{1,3})\s*(kg|g)\b""", RegexOption.IGNORE_CASE), "0$1$2")
    }

    /**
     * Extrai exclusivamente:
     * 1. Descrição do produto (1 ou mais palavras válidas)
     * 2. Peso (kg ou g, preservando 0 à esquerda e casas decimais)
     * 3. Preço por kg (R$/kg ou /kg)
     * 4. Valor com R$ na frente
     * Ignora todo o restante (data, validade, valido ate, embalado em, fabricacao, lote, tara, codigo de barras).
     */
    internal fun parseTextToProduct(rawText: String): OcrScanResult {
        if (rawText.isBlank()) return OcrScanResult()

        val normalizedText = normalizeNumericNoise(rawText)
        val lines = normalizedText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 1. Preço por kg: "R$/kg: 49,50", "$/kg 49,50", "/kg: 49,50", "49,50 /kg"
        val afterPerKgRegex = Regex(
            """(?:r?\$?\s*/\s*kg|por\s*kg|o\s*kg)\s*[:=\-]?\s*(\d{1,4}[.,]\d{2})""",
            RegexOption.IGNORE_CASE
        )
        val beforePerKgRegex = Regex(
            """(\d{1,4}[.,]\d{2})\s*(?:/\s*kg|por\s*kg|o\s*kg)""",
            RegexOption.IGNORE_CASE
        )
        val perKgLabelOnlyRegex = Regex("""(?:r?\$?\s*/\s*kg|por\s*kg|o\s*kg)""", RegexOption.IGNORE_CASE)
        val decimalPriceRegex = Regex("""(\d{1,4}[.,]\d{2})""")

        var extractedPricePerKg = ""
        for (i in lines.indices) {
            val line = lines[i]
            val afterMatch = afterPerKgRegex.find(line)
            if (afterMatch != null) {
                extractedPricePerKg = afterMatch.groupValues[1].replace(",", ".")
                break
            }
            val beforeMatch = beforePerKgRegex.find(line)
            if (beforeMatch != null) {
                extractedPricePerKg = beforeMatch.groupValues[1].replace(",", ".")
                break
            }
            // Se o rótulo R$/kg ficou isolado em uma linha e o valor na próxima linha
            if (perKgLabelOnlyRegex.containsMatchIn(line) && i + 1 < lines.size) {
                val nextLine = lines[i + 1]
                if (!nextLine.lowercase().contains("total") && !nextLine.lowercase().contains("r$")) {
                    val nextNum = decimalPriceRegex.find(nextLine)
                    if (nextNum != null) {
                        extractedPricePerKg = nextNum.groupValues[1].replace(",", ".")
                        break
                    }
                }
            }
        }

        if (extractedPricePerKg.isEmpty()) {
            val fullAfter = afterPerKgRegex.find(normalizedText)
            if (fullAfter != null) {
                extractedPricePerKg = fullAfter.groupValues[1].replace(",", ".")
            } else {
                val fullBefore = beforePerKgRegex.find(normalizedText)
                if (fullBefore != null) {
                    extractedPricePerKg = fullBefore.groupValues[1].replace(",", ".")
                }
            }
        }

        // 2. Valor com R$ na frente (excluindo R$/kg)
        val currencyRegex = Regex("""R\$(?!\s*/\s*kg)\s*[:=\-]?\s*(\d{1,4}[.,]\d{2})""", RegexOption.IGNORE_CASE)
        var extractedPrice = ""
        val currencyMatches = currencyRegex.findAll(normalizedText).toList()
        if (currencyMatches.isNotEmpty()) {
            extractedPrice = currencyMatches.last().groupValues[1].replace(",", ".")
        }

        // 3. Peso (kg ou g), preservando 0 à esquerda e casas decimais (ex: 0.608)
        var extractedWeight = ""
        var extractedWeightUnit = ""

        val explicitWeightRegex = Regex(
            """peso(?:\s+l[ií]q(?:uido)?)?\s*[:=\-]?\s*(\d{1,4}(?:[.,]\d{1,3})?)\s*(kg|g)\b""",
            RegexOption.IGNORE_CASE
        )
        val genericWeightRegex = Regex(
            """(\d{1,4}(?:[.,]\d{1,3})?)\s*(kg|g)\b""",
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
                    lower.contains("o kg")
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
        val decimalOnlyRegex = Regex("""(?<!\d)\d{1,4}[.,]\d{2}(?!\d)""")

        val candidateLines = lines.filter { line ->
            val lower = line.lowercase()
            val hasLetters = line.any { it.isLetter() }
            val isBarcode = line.count { it.isDigit() } >= 7
            val hasIgnoredMetadata = ignoredLineTerms.any { term -> lower.contains(term) }
            val hasDatePattern = dateRegex.containsMatchIn(line)
            val isMostlyNumberOrPrice = decimalOnlyRegex.matches(line) || genericWeightRegex.matches(line)
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
