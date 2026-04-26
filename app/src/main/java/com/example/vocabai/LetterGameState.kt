package com.example.vocabai

data class LetterTile(
  val id: String,
  val char: Char,
)

data class LetterGameState(
  val target: String,
  val availableTiles: List<LetterTile>,
  val selectedTiles: List<LetterTile> = emptyList(),
) {
  val answer: String = selectedTiles.joinToString("") { it.char.toString() }
  val isCorrect: Boolean = answer == target && target.isNotBlank()

  fun selectTile(tileId: String): LetterGameState {
    val tile = availableTiles.firstOrNull { it.id == tileId } ?: return this
    return copy(
      availableTiles = availableTiles.filterNot { it.id == tileId },
      selectedTiles = selectedTiles + tile,
    )
  }

  fun undo(): LetterGameState {
    val tile = selectedTiles.lastOrNull() ?: return this
    return copy(
      availableTiles = availableTiles + tile,
      selectedTiles = selectedTiles.dropLast(1),
    )
  }

  fun reset(): LetterGameState =
    copy(
      availableTiles = selectedTiles + availableTiles,
      selectedTiles = emptyList(),
    )

  companion object {
    fun forWord(word: WordEntry): LetterGameState {
      val target = word.english.normalizedLetterTarget()
      val tiles =
        target
          .mapIndexed { index, char -> LetterTile(id = "tile-$index-$char", char = char) }
          .let { deterministicShuffle(it) }
      return LetterGameState(target = target, availableTiles = tiles)
    }

    private fun deterministicShuffle(tiles: List<LetterTile>): List<LetterTile> {
      if (tiles.size <= 2) return tiles.reversed()
      val midpoint = tiles.size / 2
      return tiles.drop(midpoint) + tiles.take(midpoint)
    }
  }
}

private fun String.normalizedLetterTarget(): String =
  lowercase().filter { it in 'a'..'z' }
