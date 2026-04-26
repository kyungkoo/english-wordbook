package com.example.vocabai

import kotlin.test.Test
import kotlin.test.assertEquals

class StudySummaryTest {
  @Test
  fun summarizesBookProgressForCardsAndHub() {
    val words =
      listOf(
        word("abandon", memorized = true),
        word("brave", memorized = false),
        word("clear", memorized = false),
      )

    val summary = StudySummary.from(words)

    assertEquals(3, summary.total)
    assertEquals(1, summary.memorized)
    assertEquals(2, summary.review)
    assertEquals(0.33333334f, summary.progress)
    assertEquals("복습 2개", summary.reviewLabel)
  }

  @Test
  fun summarizesEmptyBookWithoutDivisionByZero() {
    val summary = StudySummary.from(emptyList())

    assertEquals(0, summary.total)
    assertEquals(0f, summary.progress)
    assertEquals("단어 없음", summary.reviewLabel)
  }

  private fun word(english: String, memorized: Boolean): WordEntry =
    WordEntry(
      id = english,
      bookId = "book-1",
      english = english,
      koreanMeaning = "뜻",
      englishMeaning = "definition",
      memorized = memorized,
    )
}
