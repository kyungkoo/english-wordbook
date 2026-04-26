package com.example.vocabai

data class VocabRow(val term: String, val meaning: String)

enum class VocabMode(val label: String) {
  EN_KO("영어 - 한글뜻"),
  EN_EN("영어 - 영어뜻"),
}

data class ScannedWord(
  val english: String,
  val koreanMeaning: String,
  val englishMeaning: String,
)

data class VocabularyBook(
  val id: String,
  val title: String,
  val createdAt: Long,
  val updatedAt: Long,
)

data class WordEntry(
  val id: String,
  val bookId: String,
  val english: String,
  val koreanMeaning: String,
  val englishMeaning: String,
  val memorized: Boolean = false,
)

data class VocabularyUiState(
  val books: List<VocabularyBook> = emptyList(),
  val words: List<WordEntry> = emptyList(),
  val nextBookIndex: Int = 1,
  val nextWordIndex: Int = 1,
) {
  fun saveScannedBook(title: String, scanWords: List<ScannedWord>, now: Long = System.currentTimeMillis()): VocabularyUiState {
    val bookId = "book-$nextBookIndex"
    val book =
      VocabularyBook(
        id = bookId,
        title = title.ifBlank { "새 단어장" },
        createdAt = now,
        updatedAt = now,
      )
    val entries =
      scanWords.mapIndexed { index, word ->
        WordEntry(
          id = "word-${nextWordIndex + index}",
          bookId = bookId,
          english = word.english,
          koreanMeaning = word.koreanMeaning,
          englishMeaning = word.englishMeaning,
        )
      }
    return copy(
      books = books + book,
      words = words + entries,
      nextBookIndex = nextBookIndex + 1,
      nextWordIndex = nextWordIndex + scanWords.size,
    )
  }

  fun wordsFor(bookId: String): List<WordEntry> = words.filter { it.bookId == bookId }

  fun reviewWordsFor(bookId: String): List<WordEntry> = wordsFor(bookId).filterNot { it.memorized }

  fun updateWord(entry: WordEntry): VocabularyUiState =
    copy(words = words.map { if (it.id == entry.id) entry else it })

  fun addWord(bookId: String, word: ScannedWord): VocabularyUiState {
    val entry =
      WordEntry(
        id = "word-$nextWordIndex",
        bookId = bookId,
        english = word.english.trim(),
        koreanMeaning = word.koreanMeaning.trim(),
        englishMeaning = word.englishMeaning.trim(),
      )
    return copy(words = words + entry, nextWordIndex = nextWordIndex + 1)
  }

  fun deleteWord(wordId: String): VocabularyUiState =
    copy(words = words.filterNot { it.id == wordId })

  fun renameBook(bookId: String, title: String, now: Long = System.currentTimeMillis()): VocabularyUiState =
    copy(books = books.map { if (it.id == bookId) it.copy(title = title.ifBlank { "새 단어장" }, updatedAt = now) else it })

  fun deleteBook(bookId: String): VocabularyUiState =
    copy(
      books = books.filterNot { it.id == bookId },
      words = words.filterNot { it.bookId == bookId },
    )

  fun markMemorizedAndAdvance(bookId: String, wordId: String): VocabularyUiState =
    copy(words = words.map { if (it.bookId == bookId && it.id == wordId) it.copy(memorized = true) else it })

  fun markNeedsReview(bookId: String, wordId: String): VocabularyUiState =
    copy(words = words.map { if (it.bookId == bookId && it.id == wordId) it.copy(memorized = false) else it })

  fun nextCardId(bookId: String, currentWordId: String): String? {
    val bookWords = wordsFor(bookId)
    if (bookWords.isEmpty()) return null
    val currentIndex = bookWords.indexOfFirst { it.id == currentWordId }
    if (currentIndex < 0) return bookWords.first().id
    return bookWords[(currentIndex + 1) % bookWords.size].id
  }
}
