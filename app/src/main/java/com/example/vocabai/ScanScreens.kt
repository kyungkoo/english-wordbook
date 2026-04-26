package com.example.vocabai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScanEntrySheet(isScanning: Boolean, onCamera: () -> Unit, onGallery: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("단어장 만들기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.Ink)
    Text("시험지 사진을 고르면 단어 후보를 추출하고, 저장 전에 직접 확인할 수 있어요.", color = AppColors.Muted)
    IconTextButton(
      text = if (isScanning) "스캔 중" else "카메라로 촬영",
      icon = Icons.Default.CameraAlt,
      modifier = Modifier.fillMaxWidth(),
      onClick = onCamera,
    )
    IconTextButton(
      text = "갤러리에서 선택",
      icon = Icons.Default.PhotoLibrary,
      modifier = Modifier.fillMaxWidth(),
      onClick = onGallery,
    )
    Spacer(Modifier.height(18.dp))
  }
}

@Composable
fun ScanResultScreen(
  words: List<ScannedWord>,
  errorMessage: String?,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onSave: (String, List<ScannedWord>) -> Unit,
) {
  var title by rememberSaveable { mutableStateOf("4월 모의고사 단어장") }
  var editableWords by rememberSaveable { mutableStateOf(words) }
  var editingWord by remember { mutableStateOf<ScannedWord?>(null) }
  val feedback = ScanFeedback.forResult(editableWords, errorMessage)

  Scaffold(
    topBar = { WordTopBar(title = "스캔 결과", navigationLabel = "←", onNavigation = onBack) },
    bottomBar = {
      PrimaryActionButton(
        text = if (feedback.canSave) "단어장 저장" else "저장할 단어가 없습니다",
        icon = Icons.Default.Edit,
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        enabled = feedback.canSave,
        onClick = { onSave(title, editableWords) },
      )
    },
    containerColor = AppColors.Background,
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          label = { Text("단어장 이름") },
          singleLine = true,
          shape = ButtonShape,
        )
      }
      item {
        NotebookCard(modifier = Modifier.fillMaxWidth(), containerColor = if (feedback.canSave) AppColors.PrimarySoft else AppColors.ReviewSoft) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(feedback.title, fontWeight = FontWeight.Bold, color = AppColors.Ink)
            Text(feedback.message, color = AppColors.Muted)
          }
        }
      }
      if (editableWords.isEmpty()) {
        item {
          NotebookCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              if (!errorMessage.isNullOrBlank()) {
                Text("오류: $errorMessage", color = AppColors.Muted)
              }
              PrimaryActionButton(text = "다시 스캔하기", icon = Icons.Default.Replay, modifier = Modifier.fillMaxWidth(), onClick = onRetry)
            }
          }
        }
      } else {
        items(editableWords, key = { it.english + it.koreanMeaning + it.englishMeaning }) { word ->
          ScannedWordCard(word = word, onEdit = { editingWord = word })
        }
      }
    }
  }

  editingWord?.let { word ->
    WordEditSheet(
      initial = word.toWordEntry(bookId = "scan-preview", id = "scan-preview"),
      onDismiss = { editingWord = null },
      onDelete = {
        editableWords = editableWords.filterNot { it == word }
        editingWord = null
      },
      onSave = { updated ->
        editableWords =
          editableWords.map {
            if (it == word) ScannedWord(updated.english, updated.koreanMeaning, updated.englishMeaning) else it
          }
        editingWord = null
      },
    )
  }
}

@Composable
private fun ScannedWordCard(word: ScannedWord, onEdit: () -> Unit) {
  NotebookCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(word.english, fontWeight = FontWeight.Bold, color = AppColors.Ink)
        IconTextButton(text = "수정", icon = Icons.Default.Edit, onClick = onEdit)
      }
      Text(word.koreanMeaning, color = AppColors.Ink)
      if (word.englishMeaning.isNotBlank()) {
        Text(word.englishMeaning, color = AppColors.Muted)
      }
    }
  }
}

private fun ScannedWord.toWordEntry(bookId: String, id: String): WordEntry =
  WordEntry(
    id = id,
    bookId = bookId,
    english = english,
    koreanMeaning = koreanMeaning,
    englishMeaning = englishMeaning,
  )
