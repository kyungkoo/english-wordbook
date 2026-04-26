package com.example.vocabai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VocabularyUiStateTest {
  @Test
  fun savesScannedWordsAsVocabularyBook() {
    val scanWords =
      listOf(
        ScannedWord("abandon", "버리다, 포기하다", "to leave something behind"),
        ScannedWord("accurate", "정확한", "correct in details"),
      )

    val state = VocabularyUiState().saveScannedBook("4월 모의고사 단어장", scanWords)

    assertEquals(1, state.books.size)
    assertEquals("4월 모의고사 단어장", state.books.single().title)
    assertEquals(2, state.wordsFor(state.books.single().id).size)
  }

  @Test
  fun updatesAndDeletesWordEntries() {
    val state =
      VocabularyUiState()
        .saveScannedBook(
          "4월 모의고사 단어장",
          listOf(ScannedWord("abandn", "버리다", "to leave")),
        )
    val word = state.wordsFor(state.books.single().id).single()

    val updated =
      state.updateWord(
        word.copy(
          english = "abandon",
          koreanMeaning = "버리다, 포기하다",
          englishMeaning = "to leave something behind",
        )
      )

    assertEquals("abandon", updated.wordsFor(state.books.single().id).single().english)
    assertTrue(updated.deleteWord(word.id).wordsFor(state.books.single().id).isEmpty())
  }

  @Test
  fun marksCardMemorizedAndAdvancesToNextWord() {
    val state =
      VocabularyUiState()
        .saveScannedBook(
          "4월 모의고사 단어장",
          listOf(
            ScannedWord("abandon", "버리다", "to leave"),
            ScannedWord("accurate", "정확한", "correct"),
          ),
        )
    val bookId = state.books.single().id
    val firstWord = state.wordsFor(bookId).first()

    val result = state.markMemorizedAndAdvance(bookId, firstWord.id)

    assertTrue(result.wordsFor(bookId).first().memorized)
    assertFalse(result.wordsFor(bookId).last().memorized)
    assertEquals(result.wordsFor(bookId).last().id, result.nextCardId(bookId, firstWord.id))
  }

  @Test
  fun renamesVocabularyBook() {
    val state =
      VocabularyUiState()
        .saveScannedBook("임시 단어장", listOf(ScannedWord("abandon", "버리다", "to leave")))
    val bookId = state.books.single().id

    val renamed = state.renameBook(bookId, "4월 모의고사")

    assertEquals("4월 모의고사", renamed.books.single().title)
  }

  @Test
  fun deletesVocabularyBookAndItsWords() {
    val state =
      VocabularyUiState()
        .saveScannedBook("삭제할 단어장", listOf(ScannedWord("abandon", "버리다", "to leave")))
    val bookId = state.books.single().id

    val deleted = state.deleteBook(bookId)

    assertTrue(deleted.books.isEmpty())
    assertTrue(deleted.wordsFor(bookId).isEmpty())
  }

  @Test
  fun addsManualWordToExistingBook() {
    val state = VocabularyUiState().saveScannedBook("내 단어장", emptyList())
    val bookId = state.books.single().id

    val updated =
      state.addWord(
        bookId,
        ScannedWord("brave", "용감한", "not afraid of danger"),
      )

    assertEquals("brave", updated.wordsFor(bookId).single().english)
    assertEquals(2, updated.nextWordIndex)
  }

  @Test
  fun filtersReviewWordsToUnmemorizedEntries() {
    val state =
      VocabularyUiState()
        .saveScannedBook(
          "복습 단어장",
          listOf(
            ScannedWord("abandon", "버리다", "to leave"),
            ScannedWord("brave", "용감한", "not afraid"),
          ),
        )
    val bookId = state.books.single().id
    val firstWordId = state.wordsFor(bookId).first().id

    val reviewed = state.markMemorizedAndAdvance(bookId, firstWordId)

    assertEquals(listOf("brave"), reviewed.reviewWordsFor(bookId).map { it.english })
  }

  @Test
  fun canMarkWordAsNeedsReviewAgain() {
    val state =
      VocabularyUiState()
        .saveScannedBook("복습 단어장", listOf(ScannedWord("abandon", "버리다", "to leave")))
    val bookId = state.books.single().id
    val wordId = state.wordsFor(bookId).single().id

    val needsReview = state.markMemorizedAndAdvance(bookId, wordId).markNeedsReview(bookId, wordId)

    assertEquals(listOf("abandon"), needsReview.reviewWordsFor(bookId).map { it.english })
  }

  @Test
  fun usesProvidedTimestampForBookCreationAndUpdates() {
    val state =
      VocabularyUiState()
        .saveScannedBook(
          title = "시간 테스트",
          scanWords = listOf(ScannedWord("abandon", "버리다", "to leave")),
          now = 1000L,
        )
    val bookId = state.books.single().id

    val renamed = state.renameBook(bookId, "새 제목", now = 2000L)

    assertEquals(1000L, renamed.books.single().createdAt)
    assertEquals(2000L, renamed.books.single().updatedAt)
  }
}
