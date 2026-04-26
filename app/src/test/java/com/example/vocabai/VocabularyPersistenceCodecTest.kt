package com.example.vocabai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VocabularyPersistenceCodecTest {
  @Test
  fun restoresBooksWordsAndMemorizedState() {
    val original =
      VocabularyUiState()
        .saveScannedBook(
          "4월 모의고사 단어장",
          listOf(
            ScannedWord("abandon", "버리다, 포기하다", "to leave something behind"),
            ScannedWord("accurate", "정확한", "correct in details"),
          ),
        )
    val bookId = original.books.single().id
    val wordId = original.wordsFor(bookId).first().id
    val memorized = original.markMemorizedAndAdvance(bookId, wordId)

    val restored = VocabularyPersistenceCodec.decode(VocabularyPersistenceCodec.encode(memorized))

    assertEquals(memorized.books, restored.books)
    assertEquals(memorized.words, restored.words)
    assertTrue(restored.wordsFor(bookId).first().memorized)
  }

  @Test
  fun preservesNewlinesTabsAndSeparatorsInsideText() {
    val original =
      VocabularyUiState()
        .saveScannedBook(
          "시험\t단어장",
          listOf(
            ScannedWord("look-up", "찾다\n확인하다", "to search | check"),
          ),
        )

    val restored = VocabularyPersistenceCodec.decode(VocabularyPersistenceCodec.encode(original))

    assertEquals(original.books.single().title, restored.books.single().title)
    assertEquals(original.words.single().koreanMeaning, restored.words.single().koreanMeaning)
    assertEquals(original.words.single().englishMeaning, restored.words.single().englishMeaning)
  }
}
