package com.example.vocabai

import android.util.Log
import com.google.mlkit.vision.text.Text

/**
 * Converts ML Kit OCR output into vocabulary pairs.
 *
 * Pipeline:
 * 1. Read OCR lines with coordinates.
 * 2. Separate possible word and meaning lines.
 * 3. Detect table orientation and approximate word/meaning columns.
 * 4. Filter out column noise.
 * 5. Match rows without reusing meanings.
 */
object OcrVocabularyParser {
  private const val LOG_TAG = "VocabAi"

  fun parse(latinText: Text, koreanText: Text, mode: VocabMode): List<VocabRow> {
    val latinLines = latinText.toRecognizedLines()
    val koreanLines = koreanText.toRecognizedLines()

    logInfo("ML Kit latin lines=${latinLines.formatForLog()}")
    logInfo("ML Kit korean lines=${koreanLines.formatForLog()}")

    val pairs = parseRecognizedLines(latinLines, koreanLines, mode)
    logInfo("ML Kit pairs=${pairs.joinToString(" | ") { "${it.term} => ${it.meaning}" }.takeForLog()}")
    return pairs
  }

  internal fun parseLinesForTest(
    latinLines: List<RecognizedLine>,
    koreanLines: List<RecognizedLine>,
    mode: VocabMode,
  ): List<VocabRow> = parseRecognizedLines(latinLines, koreanLines, mode)

  private fun parseRecognizedLines(
    latinLines: List<RecognizedLine>,
    koreanLines: List<RecognizedLine>,
    mode: VocabMode,
  ): List<VocabRow> {
    val englishColumns = if (mode == VocabMode.EN_EN) detectEnglishColumns(latinLines) else null
    val termTokens =
      if (mode == VocabMode.EN_EN && englishColumns != null) {
        latinLines
          .flatMap { it.toVocabularyTerms(latinLines) }
          .filter { it.isNearColumn(englishColumns.termColumn, englishColumns.tolerance) }
          .sortedForReading()
      } else {
        latinLines
          .flatMap { it.toVocabularyTerms(latinLines) }
          .sortedForReading()
      }

    val rawMeaningTokens =
      if (mode == VocabMode.EN_KO) {
        koreanLines
          .mapNotNull { it.toKoreanMeaningOrNull(koreanLines) }
          .filter { it.text.any { char -> char in '가'..'힣' } }
          .sortedForReading()
      } else {
        val englishMeaningLines = latinLines.mapNotNull { it.toEnglishMeaningOrNull() }
        if (englishColumns != null) {
          englishMeaningLines
            .filter { it.isNearColumn(englishColumns.meaningColumn, englishColumns.tolerance) }
            .sortedForReading()
        } else {
          englishMeaningLines
            .filterNot { candidate -> termTokens.any { term -> term.text.equals(candidate.text, ignoreCase = true) } }
            .sortedForReading()
        }
      }

    logInfo("ML Kit term candidates=${termTokens.formatForLog()}")
    logInfo("ML Kit meaning candidates=${rawMeaningTokens.formatForLog()}")

    val layout = detectTableLayout(termTokens, rawMeaningTokens)
    logInfo("ML Kit layout=$layout")
    val meaningTokens =
      if (mode == VocabMode.EN_KO) {
        expandCompactOnlyKoreanMeanings(rawMeaningTokens, termTokens, layout)
      } else {
        rawMeaningTokens
      }
    logInfo("ML Kit expanded meaning candidates=${meaningTokens.formatForLog()}")

    val columnFilteredTerms = termTokens.filterByColumn(layout.termColumn, layout.rowAxis, layout.rowTolerance * 5)
    val columnFilteredMeanings = meaningTokens.filterByColumn(layout.meaningColumn, layout.rowAxis, layout.rowTolerance * 5)
    logInfo("ML Kit column filtered terms=${columnFilteredTerms.formatForLog()}")
    logInfo("ML Kit column filtered meanings=${columnFilteredMeanings.formatForLog()}")

    return extractRowsByTableLayout(
      termTokens = columnFilteredTerms,
      meaningTokens = columnFilteredMeanings,
      layout = layout,
      recoverKoreanGaps = mode == VocabMode.EN_KO,
    )
  }

  private fun Text.toRecognizedLines(): List<RecognizedLine> =
    textBlocks.flatMap { block ->
      block.lines.mapNotNull { line ->
        val box = line.boundingBox ?: return@mapNotNull null
        RecognizedLine(line.text, box.centerX(), box.centerY(), box.left, box.right)
      }
    }

  private fun detectTableLayout(termTokens: List<RecognizedLine>, meaningTokens: List<RecognizedLine>): OcrTableLayout {
    val rowAxis = detectRowAxis(termTokens, meaningTokens)
    val sortedTerms = termTokens.sortedBy { it.rowValue(rowAxis) }
    val gaps =
      sortedTerms
        .zipWithNext()
        .map { (a, b) -> kotlin.math.abs(b.rowValue(rowAxis) - a.rowValue(rowAxis)) }
        .filter { it > 20 }
        .sorted()
    val medianGap = gaps.getOrNull(gaps.size / 2) ?: 140
    val rowTolerance = (medianGap * 0.45).toInt().coerceIn(55, 150)
    return OcrTableLayout(
      rowAxis = rowAxis,
      rowTolerance = rowTolerance,
      termColumn = termTokens.map { it.columnValue(rowAxis) }.medianOrZero(),
      meaningColumn = meaningTokens.map { it.columnValue(rowAxis) }.medianOrZero(),
    )
  }

  private fun detectRowAxis(termTokens: List<RecognizedLine>, meaningTokens: List<RecognizedLine>): OcrRowAxis {
    if (termTokens.isEmpty() || meaningTokens.isEmpty()) return OcrRowAxis.Y
    val avgNearestX = termTokens.map { term -> meaningTokens.minOf { kotlin.math.abs(it.x - term.x) } }.average()
    val avgNearestY = termTokens.map { term -> meaningTokens.minOf { kotlin.math.abs(it.y - term.y) } }.average()
    return if (avgNearestX < avgNearestY) OcrRowAxis.X else OcrRowAxis.Y
  }

  private fun extractRowsByTableLayout(
    termTokens: List<RecognizedLine>,
    meaningTokens: List<RecognizedLine>,
    layout: OcrTableLayout,
    recoverKoreanGaps: Boolean = false,
  ): List<VocabRow> {
    val sortedTerms = termTokens.sortedWith(compareBy<RecognizedLine> { it.rowValue(layout.rowAxis) }.thenBy { it.columnValue(layout.rowAxis) })
    val sortedMeanings = meaningTokens.sortedWith(compareBy<RecognizedLine> { it.rowValue(layout.rowAxis) }.thenBy { it.columnValue(layout.rowAxis) })
    val usedMeaningIndexes = mutableSetOf<Int>()
    val matchedMeanings = mutableMapOf<Int, RecognizedLine>()

    sortedTerms.forEachIndexed { termIndex, term ->
      val closeCandidates =
        sortedMeanings
          .mapIndexed { index, meaning -> index to meaning }
          .filterNot { (index, _) -> index in usedMeaningIndexes }
          .filter { (_, meaning) -> kotlin.math.abs(meaning.rowValue(layout.rowAxis) - term.rowValue(layout.rowAxis)) <= layout.rowTolerance }

      val selected =
        closeCandidates.minByOrNull { (_, meaning) -> meaning.distanceFrom(term, layout) }

      if (selected != null) {
        usedMeaningIndexes += selected.first
        matchedMeanings[termIndex] = selected.second
      }
    }

    if (recoverKoreanGaps) {
      recoverKoreanGapMeanings(sortedTerms, sortedMeanings, matchedMeanings, usedMeaningIndexes, layout)
    }

    return sortedTerms
      .mapIndexedNotNull { index, term ->
        matchedMeanings[index]?.let { meaning -> VocabRow(term.text, meaning.text) }
      }
      .distinctBy { it.term.lowercase() }
  }

  data class RecognizedLine(val text: String, val x: Int, val y: Int, val left: Int, val right: Int)

  private data class OcrTableLayout(
    val rowAxis: OcrRowAxis,
    val rowTolerance: Int,
    val termColumn: Int,
    val meaningColumn: Int,
  )

  private enum class OcrRowAxis {
    X,
    Y,
  }

  private data class EnglishColumns(val termColumn: Int, val meaningColumn: Int, val tolerance: Int)

  private fun RecognizedLine.distanceFrom(other: RecognizedLine, layout: OcrTableLayout): Int =
    kotlin.math.abs(rowValue(layout.rowAxis) - other.rowValue(layout.rowAxis)) * 20 +
      kotlin.math.abs(columnValue(layout.rowAxis) - other.columnValue(layout.rowAxis))

  private fun RecognizedLine.rowValue(rowAxis: OcrRowAxis): Int =
    if (rowAxis == OcrRowAxis.X) x else y

  private fun RecognizedLine.columnValue(rowAxis: OcrRowAxis): Int =
    if (rowAxis == OcrRowAxis.X) y else x

  private fun List<RecognizedLine>.filterByColumn(targetColumn: Int, rowAxis: OcrRowAxis, tolerance: Int): List<RecognizedLine> {
    if (isEmpty()) return emptyList()
    val filtered = filter { kotlin.math.abs(it.columnValue(rowAxis) - targetColumn) <= tolerance }
    return filtered.ifEmpty { this }
  }

  private fun detectEnglishColumns(lines: List<RecognizedLine>): EnglishColumns? {
    val wordHeader = lines.firstOrNull { it.text.normalizedHeader() in setOf("word", "words", "term", "terms") }
    val meaningHeader = lines.firstOrNull { it.text.normalizedHeader() in setOf("meaning", "meanings", "definition", "definitions") }
    if (wordHeader != null && meaningHeader != null && wordHeader.x != meaningHeader.x) {
      val gap = kotlin.math.abs(meaningHeader.x - wordHeader.x)
      return EnglishColumns(wordHeader.x, meaningHeader.x, (gap * 0.45).toInt().coerceAtLeast(70))
    }

    val contentColumns =
      lines
        .filterNot { it.text.isNoiseLabel() || it.text.isPartOfSpeechNoise() }
        .map { it.x }
        .sorted()
    if (contentColumns.size < 4) return null
    val midpoint = contentColumns[contentColumns.size / 2]
    val left = contentColumns.filter { it <= midpoint }.medianOrZero()
    val right = contentColumns.filter { it > midpoint }.medianOrZero()
    if (right - left < 90) return null
    return EnglishColumns(left, right, ((right - left) * 0.45).toInt().coerceAtLeast(70))
  }

  private fun RecognizedLine.toVocabularyTerms(allLines: List<RecognizedLine>): List<RecognizedLine> {
    val cleaned = text.trim()
    if (cleaned.isBlank()) return emptyList()
    if (isLikelyLabel(allLines)) return emptyList()
    if (cleaned.isPartOfSpeechNoise()) return emptyList()
    val match = Regex("""^[0-9]+[.)]?\s+(.+?)(?:\s+(adjective|noun|verb|adverb|preposition|conjunction))?$""", RegexOption.IGNORE_CASE).find(cleaned)
    val candidateText = match?.groupValues?.get(1)?.trim() ?: cleaned
    val parts = candidateText.splitVocabularyTermParts()
    if (parts.isEmpty()) return emptyList()
    return parts.mapIndexedNotNull { index, part ->
      if (!part.isVocabularyTermCandidate()) {
        null
      } else {
        copy(text = part, x = splitPartX(index, parts.size))
      }
    }
  }

  private fun RecognizedLine.toKoreanMeaningOrNull(allLines: List<RecognizedLine>): RecognizedLine? {
    if (isLikelyLabel(allLines)) return null
    val hangulParts = Regex("""[가-힣][가-힣\s()·,/-]*""").findAll(text)
      .map { it.value.trim() }
      .filter { it.isNotBlank() }
      .toList()
    val candidate = hangulParts.lastOrNull() ?: return null
    return copy(text = candidate)
  }

  private fun RecognizedLine.toEnglishMeaningOrNull(): RecognizedLine? {
    val cleaned = text.trim()
    val withoutNumber = cleaned.replace(Regex("""^[0-9]+\s+"""), "")
    val withoutPos = withoutNumber.replace(Regex("""\b(adjective|noun|verb|adverb|preposition|conjunction)\b""", RegexOption.IGNORE_CASE), "").trim()
    if (withoutPos.isBlank()) return null
    return if (withoutPos.isEnglishMeaningCandidate()) copy(text = withoutPos) else null
  }

  private fun expandCompactOnlyKoreanMeanings(
    meanings: List<RecognizedLine>,
    terms: List<RecognizedLine>,
    layout: OcrTableLayout,
  ): List<RecognizedLine> =
    meanings.flatMap { meaning ->
      val compact = meaning.text.compactHangulText()
      if (compact.length in 2..4 && meaning.text.hasOnlyHangulCompactToken()) {
        splitCompactMeaningNearTerms(meaning, compact, terms, layout)
      } else {
        listOf(meaning)
      }
    }.sortedForReading()

  private fun splitCompactMeaningNearTerms(
    meaning: RecognizedLine,
    compact: String,
    terms: List<RecognizedLine>,
    layout: OcrTableLayout,
  ): List<RecognizedLine> {
    val nearbyTerms = terms.filter { kotlin.math.abs(it.rowValue(layout.rowAxis) - meaning.rowValue(layout.rowAxis)) <= layout.rowTolerance }
    if (nearbyTerms.size < compact.length) return listOf(meaning)
    return compact.mapIndexed { index, char ->
      meaning.copy(text = char.toString(), rowAxis = layout.rowAxis, rowValue = nearbyTerms[index].rowValue(layout.rowAxis))
    }
  }

  private fun recoverKoreanGapMeanings(
    sortedTerms: List<RecognizedLine>,
    sortedMeanings: List<RecognizedLine>,
    matchedMeanings: MutableMap<Int, RecognizedLine>,
    usedMeaningIndexes: MutableSet<Int>,
    layout: OcrTableLayout,
  ) {
    sortedMeanings.forEachIndexed { meaningIndex, meaning ->
      if (meaningIndex in usedMeaningIndexes) return@forEachIndexed
      val compactTail = meaning.text.compactHangulTail()
      if (compactTail.length !in 2..4 || !meaning.text.contains(Regex("""\s"""))) return@forEachIndexed

      val missingTermIndexes =
        sortedTerms
          .mapIndexed { index, term -> index to term }
          .filter { (index, term) ->
            index !in matchedMeanings &&
              term.rowValue(layout.rowAxis) > meaning.rowValue(layout.rowAxis) + layout.rowTolerance
          }
          .sortedBy { (_, term) -> term.rowValue(layout.rowAxis) }
          .take(compactTail.length)

      if (missingTermIndexes.size == compactTail.length) {
        missingTermIndexes.forEachIndexed { compactIndex, (termIndex, term) ->
          matchedMeanings[termIndex] =
            meaning.copy(
              text = compactTail[compactIndex].toString(),
              rowAxis = layout.rowAxis,
              rowValue = term.rowValue(layout.rowAxis),
            )
        }
        usedMeaningIndexes += meaningIndex
      }
    }
  }

  private fun RecognizedLine.isNearColumn(column: Int, tolerance: Int): Boolean =
    kotlin.math.abs(x - column) <= tolerance

  private fun RecognizedLine.splitPartX(index: Int, count: Int): Int {
    if (count <= 1) return x
    val usableLeft = if (right > left) left else x - 20 * count
    val usableRight = if (right > left) right else x + 20 * count
    val step = (usableRight - usableLeft).toDouble() / count
    return (usableLeft + step * index + step / 2.0).toInt()
  }

  private fun RecognizedLine.copy(text: String, rowAxis: OcrRowAxis, rowValue: Int): RecognizedLine =
    if (rowAxis == OcrRowAxis.X) {
      copy(text = text, x = rowValue)
    } else {
      copy(text = text, y = rowValue)
    }

  private fun List<RecognizedLine>.sortedForReading(): List<RecognizedLine> =
    sortedWith(compareBy<RecognizedLine> { it.y }.thenBy { it.x })

  private fun String.normalizedHeader(): String =
    lowercase().replace(Regex("""[^a-z가-힣]"""), "")

  private fun String.splitVocabularyTermParts(): List<String> =
    split(Regex("""\s*[,;·]\s*"""))
      .map { it.trim() }
      .filter { it.isNotBlank() }

  private fun String.compactHangulText(): String =
    filter { it in '가'..'힣' }

  private fun String.compactHangulTail(): String =
    trim()
      .split(Regex("""\s+"""))
      .lastOrNull()
      .orEmpty()
      .compactHangulText()

  private fun String.hasOnlyHangulCompactToken(): Boolean {
    val trimmed = trim()
    return trimmed.isNotBlank() && trimmed.none { it.isWhitespace() } && trimmed.all { it in '가'..'힣' }
  }

  private fun String.isNoiseLabel(): Boolean {
    val lower = normalizedHeader()
    return lower in setOf(
      "no",
      "num",
      "number",
      "date",
      "name",
      "word",
      "words",
      "term",
      "terms",
      "meaning",
      "meanings",
      "definition",
      "definitions",
      "unit",
      "book",
      "의미",
      "뜻",
      "이름",
      "날짜",
      "번호",
    )
  }

  private fun RecognizedLine.isLikelyLabel(allLines: List<RecognizedLine>): Boolean {
    if (!text.isNoiseLabel()) return false
    val topContentY = allLines.filterNot { it.text.normalizedHeader().all(Char::isDigit) }.minOfOrNull { it.y } ?: y
    return y <= topContentY + 45
  }

  private fun List<RecognizedLine>.formatForLog(): String =
    joinToString(" | ") { "${it.text}@${it.x},${it.y}" }.takeForLog()

  private fun List<Int>.medianOrZero(): Int {
    if (isEmpty()) return 0
    val sorted = sorted()
    return sorted[sorted.size / 2]
  }

  private fun String.takeForLog(): String =
    replace("\n", "\\n").take(3500)

  private fun logInfo(message: String) {
    try {
      Log.i(LOG_TAG, message)
    } catch (_: RuntimeException) {
      // JVM unit tests do not provide android.util.Log.
    }
  }

  private fun String.isVocabularyTermCandidate(): Boolean {
    val normalized = trim()
    if (normalized.length < 2) return false
    if (normalized.isPartOfSpeechNoise()) return false
    if (normalized.length <= 2 && normalized.all { it.isUpperCase() }) return false
    if (normalized.all { it.isDigit() }) return false
    val lower = normalized.lowercase()
    if (normalized.count { it.isWhitespace() } >= 3) return false
    return normalized.any { it in 'a'..'z' || it in 'A'..'Z' } &&
      normalized.all { it.isLetterOrDigit() || it.isWhitespace() || it in setOf('(', ')', '\'', '-', '/') }
  }

  private fun String.isPartOfSpeechNoise(): Boolean {
    val lower = trim().lowercase()
    val normalized = lower.replace(Regex("""[^a-z]"""), "")
    if (normalized in setOf("adjective", "noun", "verb", "adverb", "preposition", "conjunction")) return true
    return setOf("adjective", "noun", "verb", "adverb", "preposition", "conjunction")
      .any { partOfSpeech -> normalized.startsWith(partOfSpeech) && normalized.length <= partOfSpeech.length + 2 }
  }

  private fun String.isEnglishMeaningCandidate(): Boolean {
    val normalized = trim()
    if (normalized.length < 2) return false
    if (normalized.all { it.isDigit() }) return false
    val lower = normalized.lowercase()
    if (lower.isNoiseLabel()) return false
    if (lower in setOf("adjective", "noun", "verb", "adverb", "preposition", "conjunction")) return false
    return normalized.any { it in 'a'..'z' || it in 'A'..'Z' }
  }
}
