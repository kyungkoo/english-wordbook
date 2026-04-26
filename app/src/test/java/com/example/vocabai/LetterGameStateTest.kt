package com.example.vocabai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LetterGameStateTest {
  @Test
  fun buildsAnswerBySelectingShuffledLetters() {
    val state = LetterGameState.forWord(word("abandon"))

    val selected =
      "abandon".fold(state) { current, char ->
        val tile = current.availableTiles.first { it.char == char }
        current.selectTile(tile.id)
      }

    assertEquals("abandon", selected.answer)
    assertTrue(selected.isCorrect)
    assertTrue(selected.availableTiles.isEmpty())
  }

  @Test
  fun ignoresSpacesHyphensAndParenthesesWhenBuildingAnswer() {
    val state = LetterGameState.forWord(word("tooth (teeth)"))

    assertEquals("toothteeth", state.target)
    assertEquals(10, state.availableTiles.size)
  }

  @Test
  fun canUndoAndResetSelectedLetters() {
    val state = LetterGameState.forWord(word("brave"))
    val first = state.availableTiles.first()
    val second = state.availableTiles.drop(1).first()

    val selected = state.selectTile(first.id).selectTile(second.id)
    val undone = selected.undo()
    val reset = selected.reset()

    assertEquals(1, undone.answer.length)
    assertEquals(first.char.toString(), undone.answer)
    assertEquals("", reset.answer)
    assertFalse(reset.isCorrect)
    assertEquals(5, reset.availableTiles.size)
  }

  private fun word(english: String): WordEntry =
    WordEntry(
      id = "word-$english",
      bookId = "book-1",
      english = english,
      koreanMeaning = "뜻",
      englishMeaning = "definition",
    )
}
