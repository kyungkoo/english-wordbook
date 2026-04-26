package com.example.vocabai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private enum class BookPane {
  Hub,
  List,
  FlipCard,
  LetterGame,
}

@Composable
fun VocabularyBookScreen(
  book: VocabularyBook?,
  words: List<WordEntry>,
  onBack: () -> Unit,
  onRenameBook: (String) -> Unit,
  onDeleteBook: () -> Unit,
  onAddWord: (ScannedWord) -> Unit,
  onUpdateWord: (WordEntry) -> Unit,
  onDeleteWord: (String) -> Unit,
  onMemorized: (String) -> Unit,
  onNeedsReview: (String) -> Unit,
  onSpeak: (String) -> Unit,
) {
  if (book == null) {
    AlertDialog(
      onDismissRequest = onBack,
      confirmButton = { TextButton(onClick = onBack) { Text("확인") } },
      title = { Text("단어장을 찾을 수 없습니다") },
    )
    return
  }

  var pane by rememberSaveable { mutableStateOf(BookPane.Hub) }
  var showBookEdit by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  val title =
    when (pane) {
      BookPane.Hub -> book.title
      BookPane.List -> "단어 목록"
      BookPane.FlipCard -> "플립카드"
      BookPane.LetterGame -> "단어맞추기"
    }

  Scaffold(
    topBar = {
      WordTopBar(
        title = title,
        navigationLabel = "←",
        onNavigation = { if (pane == BookPane.Hub) onBack() else pane = BookPane.Hub },
        actionLabel = if (pane == BookPane.Hub) "관리" else null,
        onAction = if (pane == BookPane.Hub) ({ showBookEdit = true }) else null,
      )
    },
    containerColor = AppColors.Background,
  ) { padding ->
    when (pane) {
      BookPane.Hub ->
        BookHubScreen(
          book = book,
          words = words,
          modifier = Modifier.padding(padding),
          onOpenList = { pane = BookPane.List },
          onOpenFlipCard = { pane = BookPane.FlipCard },
          onOpenLetterGame = { pane = BookPane.LetterGame },
        )
      BookPane.List ->
        WordListScreen(
          words = words,
          modifier = Modifier.padding(padding),
          onAddWord = onAddWord,
          onUpdateWord = onUpdateWord,
          onDeleteWord = onDeleteWord,
        )
      BookPane.FlipCard ->
        FlipCardScreen(
          words = words,
          modifier = Modifier.padding(padding),
          onMemorized = onMemorized,
          onNeedsReview = onNeedsReview,
          onSpeak = onSpeak,
        )
      BookPane.LetterGame ->
        LetterGameScreen(
          words = words,
          modifier = Modifier.padding(padding),
          onMemorized = onMemorized,
        )
    }
  }

  if (showBookEdit) {
    BookEditSheet(
      title = book.title,
      onDismiss = { showBookEdit = false },
      onRename = {
        onRenameBook(it)
        showBookEdit = false
      },
      onDelete = {
        showBookEdit = false
        showDeleteConfirm = true
      },
    )
  }
  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("단어장을 삭제할까요?") },
      text = { Text("단어장과 안에 있는 단어가 모두 삭제됩니다.") },
      dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") } },
      confirmButton = { TextButton(onClick = onDeleteBook) { Text("삭제", color = AppColors.Danger) } },
    )
  }
}

@Composable
private fun BookHubScreen(
  book: VocabularyBook,
  words: List<WordEntry>,
  modifier: Modifier,
  onOpenList: () -> Unit,
  onOpenFlipCard: () -> Unit,
  onOpenLetterGame: () -> Unit,
) {
  val summary = StudySummary.from(words)
  LazyColumn(
    modifier = modifier.fillMaxSize().padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      NotebookCard(modifier = Modifier.fillMaxWidth(), containerColor = AppColors.PrimarySoft) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.Ink)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressPill("${summary.total} words")
            ProgressPill(summary.reviewLabel, accent = if (summary.review == 0) AppColors.Primary else AppColors.Review)
          }
          LinearProgressIndicator(
            progress = { summary.progress },
            modifier = Modifier.fillMaxWidth(),
            color = AppColors.Primary,
            trackColor = AppColors.PaperDeep,
          )
          Text("${summary.memorized}개를 알고 있고, ${summary.review}개를 복습하면 됩니다.", color = AppColors.Muted)
        }
      }
    }
    item { SectionHeader("학습하기") }
    item {
      StudyActionCard("단어 목록", "뜻과 오탈자를 확인하고 직접 단어를 추가해요.", Icons.Default.Edit, onOpenList)
    }
    item {
      StudyActionCard("플립카드", "영어를 보고 뜻을 떠올린 뒤 알고 있는지 표시해요.", Icons.Default.School, onOpenFlipCard)
    }
    item {
      StudyActionCard("단어맞추기", "뜻을 보고 섞인 알파벳을 눌러 단어를 완성해요.", Icons.Default.Quiz, onOpenLetterGame)
    }
  }
}

@Composable
private fun StudyActionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
  NotebookCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      ProgressPill(" ", accent = AppColors.Primary)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          androidx.compose.material3.Icon(icon, contentDescription = null, tint = AppColors.Primary)
          Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Ink)
        }
        Text(description, color = AppColors.Muted)
      }
    }
  }
}

@Composable
private fun WordListScreen(
  words: List<WordEntry>,
  modifier: Modifier,
  onAddWord: (ScannedWord) -> Unit,
  onUpdateWord: (WordEntry) -> Unit,
  onDeleteWord: (String) -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  var reviewOnly by rememberSaveable { mutableStateOf(false) }
  var editingWord by remember { mutableStateOf<WordEntry?>(null) }
  var addingWord by remember { mutableStateOf(false) }
  val filtered =
    words
      .filter { !reviewOnly || !it.memorized }
      .filter {
        query.isBlank() ||
          it.english.contains(query, ignoreCase = true) ||
          it.koreanMeaning.contains(query) ||
          it.englishMeaning.contains(query, ignoreCase = true)
      }

  LazyColumn(
    modifier = modifier.fillMaxSize().padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("검색") },
        singleLine = true,
        shape = ButtonShape,
      )
    }
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconTextButton(
          text = if (reviewOnly) "전체 보기" else "미암기만",
          icon = Icons.Default.Replay,
          modifier = Modifier.weight(1f),
          onClick = { reviewOnly = !reviewOnly },
        )
        IconTextButton(
          text = "추가",
          icon = Icons.Default.Edit,
          modifier = Modifier.weight(1f),
          onClick = { addingWord = true },
        )
      }
    }
    item {
      Text(if (reviewOnly) "미암기 ${filtered.size}개" else "${filtered.size}개 단어", color = AppColors.Muted)
    }
    items(filtered, key = { it.id }) { word ->
      WordEntryCard(word = word, onEdit = { editingWord = word })
    }
  }

  editingWord?.let { word ->
    WordEditSheet(
      initial = word,
      onDismiss = { editingWord = null },
      onDelete = {
        onDeleteWord(word.id)
        editingWord = null
      },
      onSave = {
        onUpdateWord(it)
        editingWord = null
      },
    )
  }
  if (addingWord) {
    WordEditSheet(
      initial = WordEntry(id = "new-word", bookId = "new-book", english = "", koreanMeaning = "", englishMeaning = ""),
      title = "단어 추가",
      deleteLabel = "취소",
      onDismiss = { addingWord = false },
      onDelete = { addingWord = false },
      onSave = {
        onAddWord(ScannedWord(it.english, it.koreanMeaning, it.englishMeaning))
        addingWord = false
      },
    )
  }
}

@Composable
private fun WordEntryCard(word: WordEntry, onEdit: () -> Unit) {
  NotebookCard(modifier = Modifier.fillMaxWidth()) {
    Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      CompactIconButton(
        icon = Icons.Default.Edit,
        contentDescription = "단어 수정",
        modifier = Modifier.align(Alignment.TopEnd),
        onClick = onEdit,
      )
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          word.english,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = AppColors.Ink,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          word.koreanMeaning,
          style = MaterialTheme.typography.titleMedium,
          color = AppColors.Ink,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        if (word.englishMeaning.isNotBlank()) {
          Text(
            word.englishMeaning,
            color = AppColors.Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (word.memorized) {
          ProgressPill("암기", accent = AppColors.Primary)
        } else {
          ProgressPill("복습", accent = AppColors.Review)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordEditSheet(
  initial: WordEntry,
  title: String = "단어 수정",
  deleteLabel: String = "삭제",
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
  onSave: (WordEntry) -> Unit,
) {
  var english by rememberSaveable(initial.id) { mutableStateOf(initial.english) }
  var koreanMeaning by rememberSaveable(initial.id) { mutableStateOf(initial.koreanMeaning) }
  var englishMeaning by rememberSaveable(initial.id) { mutableStateOf(initial.englishMeaning) }

  ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.Paper) {
    Column(modifier = Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.Ink)
      OutlinedTextField(value = english, onValueChange = { english = it }, modifier = Modifier.fillMaxWidth(), label = { Text("영어") }, shape = ButtonShape)
      OutlinedTextField(value = koreanMeaning, onValueChange = { koreanMeaning = it }, modifier = Modifier.fillMaxWidth(), label = { Text("한글 뜻") }, shape = ButtonShape)
      OutlinedTextField(value = englishMeaning, onValueChange = { englishMeaning = it }, modifier = Modifier.fillMaxWidth(), label = { Text("영어 뜻") }, shape = ButtonShape)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), shape = ButtonShape) {
          Text(deleteLabel, color = if (deleteLabel == "삭제") AppColors.Danger else AppColors.Ink)
        }
        Button(
          onClick = { onSave(initial.copy(english = english.trim(), koreanMeaning = koreanMeaning.trim(), englishMeaning = englishMeaning.trim())) },
          modifier = Modifier.weight(1f),
          enabled = english.isNotBlank(),
          shape = ButtonShape,
        ) {
          Text("저장")
        }
      }
      Spacer(Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookEditSheet(
  title: String,
  onDismiss: () -> Unit,
  onRename: (String) -> Unit,
  onDelete: () -> Unit,
) {
  var nextTitle by rememberSaveable(title) { mutableStateOf(title) }

  ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.Paper) {
    Column(modifier = Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("단어장 관리", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.Ink)
      OutlinedTextField(
        value = nextTitle,
        onValueChange = { nextTitle = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("단어장 이름") },
        singleLine = true,
        shape = ButtonShape,
      )
      PrimaryActionButton(text = "이름 변경", icon = Icons.Default.Edit, modifier = Modifier.fillMaxWidth(), enabled = nextTitle.isNotBlank(), onClick = { onRename(nextTitle.trim()) })
      IconTextButton(text = "단어장 삭제", icon = Icons.Default.Delete, modifier = Modifier.fillMaxWidth(), onClick = onDelete)
      Spacer(Modifier.height(12.dp))
    }
  }
}
