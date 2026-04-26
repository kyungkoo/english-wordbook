package com.example.vocabai

import android.content.Context

class VocabularyRepository(context: Context) {
  private val appContext = context.applicationContext
  private val dao = VocabularyDatabase.get(appContext).vocabularyDao()
  private val legacyPreferences = appContext.getSharedPreferences("wordnote", Context.MODE_PRIVATE)

  suspend fun load(): VocabularyUiState {
    val books = dao.books().map { it.toModel() }
    val words = dao.words().map { it.toModel() }
    if (books.isNotEmpty() || words.isNotEmpty()) {
      return VocabularyUiState(
        books = books,
        words = words,
        nextBookIndex = books.nextBookIndex(),
        nextWordIndex = words.nextWordIndex(),
      )
    }

    val legacyState =
      legacyPreferences.getString(KEY_LEGACY_STATE, null)
        ?.let(VocabularyPersistenceCodec::decode)
        ?: VocabularyUiState()
    if (legacyState.books.isNotEmpty() || legacyState.words.isNotEmpty()) {
      save(legacyState)
    }
    return legacyState
  }

  suspend fun save(state: VocabularyUiState) {
    dao.replaceState(state)
  }

  private fun List<VocabularyBook>.nextBookIndex(): Int =
    maxOfOrNull { it.id.substringAfter("book-", "").toIntOrNull() ?: 0 }?.plus(1) ?: 1

  private fun List<WordEntry>.nextWordIndex(): Int =
    maxOfOrNull { it.id.substringAfter("word-", "").toIntOrNull() ?: 0 }?.plus(1) ?: 1

  private companion object {
    const val KEY_LEGACY_STATE = "vocabulary_state"
  }
}
