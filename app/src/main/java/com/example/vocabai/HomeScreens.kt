package com.example.vocabai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
  state: VocabularyUiState,
  onOpenScan: () -> Unit,
  onOpenBook: (String) -> Unit,
) {
  Scaffold(
    topBar = {
      WordTopBar(title = "WordNote", actionLabel = "스캔", onAction = onOpenScan)
    },
    containerColor = AppColors.Background,
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        TodayReviewCard(state = state, onOpenScan = onOpenScan)
      }
      if (state.books.isEmpty()) {
        item { EmptyHome(onOpenScan = onOpenScan) }
      } else {
        item { SectionHeader("내 단어장") }
        items(state.books, key = { it.id }) { book ->
          val words = state.wordsFor(book.id)
          VocabularyBookCard(book = book, words = words, onClick = { onOpenBook(book.id) })
        }
      }
    }
  }
}

@Composable
private fun TodayReviewCard(state: VocabularyUiState, onOpenScan: () -> Unit) {
  val summary = StudySummary.from(state.words)
  NotebookCard(modifier = Modifier.fillMaxWidth(), containerColor = AppColors.PrimarySoft) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.School, contentDescription = null, tint = AppColors.Primary)
        Text("오늘 복습", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.Ink)
      }
      Text(
        if (summary.total == 0) "시험지를 스캔해서 첫 단어장을 만들어 보세요." else "${summary.review}개 단어가 복습을 기다리고 있어요.",
        color = AppColors.Muted,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProgressPill("${summary.total} words")
        ProgressPill(summary.reviewLabel, accent = AppColors.Review)
      }
      PrimaryActionButton(
        text = if (summary.total == 0) "시험지 스캔하기" else "새 단어장 만들기",
        icon = Icons.Default.Add,
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenScan,
      )
    }
  }
}

@Composable
private fun EmptyHome(onOpenScan: () -> Unit) {
  NotebookCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("아직 단어장이 없습니다", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.Ink)
      Text("시험지를 스캔하면 단어와 뜻을 확인한 뒤 내 단어장으로 저장할 수 있어요.", color = AppColors.Muted)
      PrimaryActionButton(text = "시험지 스캔하기", icon = Icons.Default.Add, modifier = Modifier.fillMaxWidth(), onClick = onOpenScan)
    }
  }
}

@Composable
private fun VocabularyBookCard(book: VocabularyBook, words: List<WordEntry>, onClick: () -> Unit) {
  val summary = StudySummary.from(words)
  NotebookCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Ink)
          Text("최근 수정 ${book.updatedAt.formatDate()}", color = AppColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
        ProgressPill(summary.reviewLabel, accent = if (summary.review == 0) AppColors.Primary else AppColors.Review)
      }
      LinearProgressIndicator(
        progress = { summary.progress },
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Primary,
        trackColor = AppColors.PaperDeep,
      )
      Text("${summary.memorized}/${summary.total} 암기 완료", color = AppColors.Muted)
    }
  }
}

private fun Long.formatDate(): String =
  if (this <= 0L) "오늘" else SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(this))
