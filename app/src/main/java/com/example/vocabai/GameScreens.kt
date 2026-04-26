package com.example.vocabai

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class FlipCardFace {
  Front,
  Back,
}

fun flipCardFaceForRotation(rotationY: Float): FlipCardFace =
  if (rotationY < 90f) FlipCardFace.Front else FlipCardFace.Back

fun nextFlipCardIndex(currentIndex: Int, total: Int): Int =
  if (total <= 0) 0 else (currentIndex + 1) % total

@Composable
fun FlipCardScreen(
  words: List<WordEntry>,
  modifier: Modifier = Modifier,
  onMemorized: (String) -> Unit,
  onNeedsReview: (String) -> Unit,
  onSpeak: (String) -> Unit,
) {
  var reviewOnly by rememberSaveable { mutableStateOf(false) }
  val studyWords = if (reviewOnly) words.filterNot { it.memorized } else words
  var currentIndex by rememberSaveable(studyWords.size, reviewOnly) { mutableIntStateOf(0) }
  var flipped by rememberSaveable(currentIndex) { mutableStateOf(false) }

  if (studyWords.isEmpty()) {
    EmptyGameMessage(if (reviewOnly) "복습할 미암기 단어가 없습니다" else "암기할 단어가 없습니다")
    return
  }

  if (currentIndex >= studyWords.size) currentIndex = 0
  val word = studyWords[currentIndex]
  val density = LocalDensity.current.density
  val rotationY by animateFloatAsState(
    targetValue = if (flipped) 180f else 0f,
    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
    label = "flipCardRotation",
  )
  val visibleFace = flipCardFaceForRotation(rotationY)
  fun advanceWithoutMarking() {
    currentIndex = nextFlipCardIndex(currentIndex, studyWords.size)
    flipped = false
  }

  Column(
    modifier = modifier.fillMaxSize().padding(22.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    StudyModeHeader(
      current = currentIndex + 1,
      total = studyWords.size,
      reviewOnly = reviewOnly,
      fullLabel = "전체 단어로 학습",
      reviewLabel = "미암기만 학습",
      onToggle = { reviewOnly = !reviewOnly },
    )
    NotebookCard(
      modifier =
        Modifier
          .fillMaxWidth(0.88f)
          .widthIn(max = 420.dp)
          .height(280.dp)
          .graphicsLayer {
            this.rotationY = rotationY
            cameraDistance = 12f * density
          }
          .clickable { flipped = !flipped },
      containerColor = AppColors.Surface,
    ) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (visibleFace == FlipCardFace.Back) {
          Column(
            modifier = Modifier.graphicsLayer { this.rotationY = 180f },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Text(word.koreanMeaning, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.Ink)
            if (word.englishMeaning.isNotBlank()) {
              Text(word.englishMeaning, style = MaterialTheme.typography.bodyLarge, color = AppColors.Muted)
            }
          }
        } else {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(word.english, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppColors.Ink)
            PrimaryActionButton(text = "듣기", icon = Icons.AutoMirrored.Filled.VolumeUp, onClick = { onSpeak(word.english) })
          }
        }
      }
    }
    Text(if (flipped) "뜻을 확인했으면 상태를 골라 주세요." else "카드를 탭하면 뜻을 볼 수 있어요.", color = AppColors.Muted)
    if (flipped) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = {
          onNeedsReview(word.id)
          advanceWithoutMarking()
        }, shape = ButtonShape) {
          Text("다시 보기")
        }
        Button(onClick = {
          onMemorized(word.id)
          advanceWithoutMarking()
        }, shape = ButtonShape) {
          Text("알고 있음")
        }
      }
    } else {
      OutlinedButton(onClick = ::advanceWithoutMarking, shape = ButtonShape) {
        Text("다음 단어")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterGameScreen(
  words: List<WordEntry>,
  modifier: Modifier = Modifier,
  onMemorized: (String) -> Unit,
) {
  var reviewOnly by rememberSaveable { mutableStateOf(false) }
  val studyWords = if (reviewOnly) words.filterNot { it.memorized } else words
  var currentIndex by rememberSaveable(studyWords.size, reviewOnly) { mutableIntStateOf(0) }

  if (studyWords.isEmpty()) {
    EmptyGameMessage(if (reviewOnly) "복습할 미암기 단어가 없습니다" else "맞출 단어가 없습니다")
    return
  }

  if (currentIndex >= studyWords.size) currentIndex = 0
  val word = studyWords[currentIndex]
  var game by remember(word.id) { mutableStateOf(LetterGameState.forWord(word)) }

  Column(
    modifier = modifier.fillMaxSize().padding(22.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    StudyModeHeader(
      current = currentIndex + 1,
      total = studyWords.size,
      reviewOnly = reviewOnly,
      fullLabel = "전체 단어로 풀기",
      reviewLabel = "미암기만 풀기",
      onToggle = { reviewOnly = !reviewOnly },
    )
    NotebookCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("뜻", color = AppColors.Muted, fontWeight = FontWeight.SemiBold)
        Text(word.koreanMeaning, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.Ink)
        if (word.englishMeaning.isNotBlank()) {
          Text(word.englishMeaning, color = AppColors.Muted)
        }
      }
    }

    Text("정답", fontWeight = FontWeight.Bold, color = AppColors.Ink)
    NotebookCard(modifier = Modifier.fillMaxWidth(), containerColor = AppColors.PaperDeep) {
      Text(
        game.selectedTiles.joinToString(" ") { it.char.toString() }.ifBlank { List(game.target.length) { "_" }.joinToString(" ") },
        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = AppColors.Ink,
      )
    }

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      game.availableTiles.forEach { tile ->
        FilledTonalButton(
          onClick = { game = game.selectTile(tile.id) },
          modifier = Modifier.size(width = 52.dp, height = 46.dp),
          shape = ButtonShape,
        ) {
          Text(tile.char.toString())
        }
      }
    }

    if (game.isCorrect) {
      ProgressPill("정답입니다!", accent = AppColors.Primary)
      PrimaryActionButton(text = "다음 단어", modifier = Modifier.fillMaxWidth(), onClick = {
        onMemorized(word.id)
        currentIndex = (currentIndex + 1) % studyWords.size
      })
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { game = game.undo() }, enabled = game.selectedTiles.isNotEmpty(), shape = ButtonShape) {
          Text("지우기")
        }
        OutlinedButton(onClick = { game = game.reset() }, enabled = game.selectedTiles.isNotEmpty(), shape = ButtonShape) {
          Text("초기화")
        }
      }
    }
  }
}

@Composable
private fun StudyModeHeader(
  current: Int,
  total: Int,
  reviewOnly: Boolean,
  fullLabel: String,
  reviewLabel: String,
  onToggle: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      ProgressPill("$current / $total")
      ProgressPill(if (reviewOnly) "미암기 모드" else "전체 모드", accent = if (reviewOnly) AppColors.Review else AppColors.Primary)
    }
    IconTextButton(
      text = if (reviewOnly) fullLabel else reviewLabel,
      icon = Icons.Default.Replay,
      modifier = Modifier.fillMaxWidth(),
      onClick = onToggle,
    )
  }
}

@Composable
private fun EmptyGameMessage(message: String) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(message, color = AppColors.Muted)
  }
}
