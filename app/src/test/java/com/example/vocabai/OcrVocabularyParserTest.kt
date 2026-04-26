package com.example.vocabai

import kotlin.test.Test
import kotlin.test.assertEquals

class OcrVocabularyParserTest {
  @Test
  fun matchesRowsByNearestRowAndColumnWithoutFallingBackToWrongMeaning() {
    val latinLines =
      listOf(
        line("Words", x = 90, y = 80),
        line("1 tall adjective", x = 95, y = 150),
        line("2 short adjective", x = 98, y = 220),
        line("Date", x = 320, y = 78),
      )
    val koreanLines =
      listOf(
        line("Meaning", x = 360, y = 80),
        line("키가 큰", x = 360, y = 152),
        line("작은", x = 360, y = 222),
        line("이름", x = 620, y = 78),
      )

    val rows = OcrVocabularyParser.parseLinesForTest(latinLines, koreanLines, VocabMode.EN_KO)

    assertEquals(
      listOf(
        VocabRow("tall", "키가 큰"),
        VocabRow("short", "작은"),
      ),
      rows,
    )
  }

  @Test
  fun splitsCompactSingleSyllableKoreanMeaningsByDetectedColumns() {
    val latinLines =
      listOf(
        line("ear", x = 100, y = 150),
        line("neck", x = 260, y = 150),
      )
    val koreanLines =
      listOf(
        line("귀목", x = 370, y = 150, left = 340, right = 400),
      )

    val rows = OcrVocabularyParser.parseLinesForTest(latinLines, koreanLines, VocabMode.EN_KO)

    assertEquals(
      listOf(
        VocabRow("ear", "귀"),
        VocabRow("neck", "목"),
      ),
      rows,
    )
  }

  @Test
  fun splitsMultipleTermsDetectedInOneOcrLine() {
    val latinLines =
      listOf(
        line("ear(s), neck", x = 180, y = 150, left = 90, right = 270),
      )
    val koreanLines =
      listOf(
        line("귀목", x = 370, y = 150, left = 340, right = 400),
      )

    val rows = OcrVocabularyParser.parseLinesForTest(latinLines, koreanLines, VocabMode.EN_KO)

    assertEquals(
      listOf(
        VocabRow("ear(s)", "귀"),
        VocabRow("neck", "목"),
      ),
      rows,
    )
  }

  @Test
  fun recoversEarAndNeckWhenKoreanOcrMergesMeaningsWithPreviousNoise() {
    val latinLines =
      listOf(
        line("tooth (teeth)", x = 2161, y = 2190),
        line("chest", x = 2324, y = 2329),
        line("finger(s)", x = 2485, y = 2271),
        line("toe(s)", x = 2646, y = 2336),
        line("ear(s)", x = 2808, y = 2328),
        line("neck", x = 2968, y = 2339),
        line("stomach", x = 3127, y = 2265),
      )
    val koreanLines =
      listOf(
        line("치아 (치아들)", x = 2148, y = 843),
        line("가슴", x = 2302, y = 995),
        line("손가락", x = 2460, y = 959),
        line("발가락", x = 2614, y = 960),
        line("손 날 귀목", x = 2687, y = 1038),
        line("배", x = 3075, y = 1038),
      )

    val rows = OcrVocabularyParser.parseLinesForTest(latinLines, koreanLines, VocabMode.EN_KO)

    assertEquals(
      listOf(
        VocabRow("tooth (teeth)", "치아 (치아들)"),
        VocabRow("chest", "가슴"),
        VocabRow("finger(s)", "손가락"),
        VocabRow("toe(s)", "발가락"),
        VocabRow("ear(s)", "귀"),
        VocabRow("neck", "목"),
        VocabRow("stomach", "배"),
      ),
      rows,
    )
  }

  @Test
  fun keepsMultiWordEnglishMeaningsInTheMeaningColumn() {
    val latinLines =
      listOf(
        line("word", x = 100, y = 80),
        line("meaning", x = 390, y = 80),
        line("brave", x = 100, y = 150),
        line("not afraid of danger", x = 390, y = 151),
        line("quick", x = 100, y = 220),
        line("moving fast", x = 390, y = 221),
      )

    val rows = OcrVocabularyParser.parseLinesForTest(latinLines, emptyList(), VocabMode.EN_EN)

    assertEquals(
      listOf(
        VocabRow("brave", "not afraid of danger"),
        VocabRow("quick", "moving fast"),
      ),
      rows,
    )
  }

  @Test
  fun keepsNameWhenItAppearsAsVocabularyTermInRows() {
    val latinLines =
      listOf(
        line("Words", x = 100, y = 80),
        line("1 name noun", x = 100, y = 150),
        line("2 date noun", x = 100, y = 220),
      )
    val koreanLines =
      listOf(
        line("Meaning", x = 360, y = 80),
        line("이름", x = 360, y = 152),
        line("날짜", x = 360, y = 222),
      )

    val rows = OcrVocabularyParser.parseLinesForTest(latinLines, koreanLines, VocabMode.EN_KO)

    assertEquals(
      listOf(
        VocabRow("name", "이름"),
        VocabRow("date", "날짜"),
      ),
      rows,
    )
  }

  private fun line(text: String, x: Int, y: Int, left: Int = x - 30, right: Int = x + 30): OcrVocabularyParser.RecognizedLine =
    OcrVocabularyParser.RecognizedLine(text = text, x = x, y = y, left = left, right = right)
}
