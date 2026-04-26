package com.example.vocabai

object VocabularyPersistenceCodec {
  private const val VERSION = "wordnote-state-v1"
  private const val SEP = "\t"

  fun encode(state: VocabularyUiState): String =
    buildString {
      appendLine(VERSION)
      appendLine("next${SEP}${state.nextBookIndex}${SEP}${state.nextWordIndex}")
      state.books.forEach { book ->
        appendLine(
          listOf(
            "book",
            book.id,
            book.title,
            book.createdAt.toString(),
            book.updatedAt.toString(),
          ).joinToString(SEP) { it.escapeField() }
        )
      }
      state.words.forEach { word ->
        appendLine(
          listOf(
            "word",
            word.id,
            word.bookId,
            word.english,
            word.koreanMeaning,
            word.englishMeaning,
            word.memorized.toString(),
          ).joinToString(SEP) { it.escapeField() }
        )
      }
    }

  fun decode(text: String): VocabularyUiState {
    val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.firstOrNull() != VERSION) return VocabularyUiState()

    var nextBookIndex = 1
    var nextWordIndex = 1
    val books = mutableListOf<VocabularyBook>()
    val words = mutableListOf<WordEntry>()

    lines.drop(1).forEach { line ->
      val fields = line.split(SEP).map { it.unescapeField() }
      when (fields.firstOrNull()) {
        "next" -> {
          nextBookIndex = fields.getOrNull(1)?.toIntOrNull() ?: nextBookIndex
          nextWordIndex = fields.getOrNull(2)?.toIntOrNull() ?: nextWordIndex
        }
        "book" -> {
          if (fields.size >= 5) {
            books +=
              VocabularyBook(
                id = fields[1],
                title = fields[2],
                createdAt = fields[3].toLongOrNull() ?: 0L,
                updatedAt = fields[4].toLongOrNull() ?: 0L,
              )
          }
        }
        "word" -> {
          if (fields.size >= 7) {
            words +=
              WordEntry(
                id = fields[1],
                bookId = fields[2],
                english = fields[3],
                koreanMeaning = fields[4],
                englishMeaning = fields[5],
                memorized = fields[6].toBooleanStrictOrNull() ?: false,
              )
          }
        }
      }
    }

    return VocabularyUiState(
      books = books,
      words = words,
      nextBookIndex = nextBookIndex,
      nextWordIndex = nextWordIndex,
    )
  }

  private fun String.escapeField(): String =
    replace("\\", "\\\\")
      .replace("\t", "\\t")
      .replace("\n", "\\n")

  private fun String.unescapeField(): String {
    val output = StringBuilder()
    var index = 0
    while (index < length) {
      val char = this[index]
      if (char == '\\' && index + 1 < length) {
        when (this[index + 1]) {
          't' -> output.append('\t')
          'n' -> output.append('\n')
          '\\' -> output.append('\\')
          else -> output.append(this[index + 1])
        }
        index += 2
      } else {
        output.append(char)
        index += 1
      }
    }
    return output.toString()
  }
}
