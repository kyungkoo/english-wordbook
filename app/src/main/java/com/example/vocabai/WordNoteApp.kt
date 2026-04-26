package com.example.vocabai

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface AppScreen {
  data object Home : AppScreen
  data class ScanResult(val words: List<ScannedWord>, val errorMessage: String? = null) : AppScreen
  data class BookDetail(val bookId: String) : AppScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordNoteApp(onSpeak: (String) -> Unit) {
  val context = LocalContext.current
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val repository = remember(context) { VocabularyRepository(context) }
  var state by remember { mutableStateOf(VocabularyUiState()) }
  var loaded by remember { mutableStateOf(false) }
  var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
  var showScanSheet by rememberSaveable { mutableStateOf(false) }
  var isScanning by rememberSaveable { mutableStateOf(false) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  LaunchedEffect(Unit) {
    state = withContext(Dispatchers.IO) { repository.load() }
    loaded = true
  }

  LaunchedEffect(state, loaded) {
    if (loaded) {
      withContext(Dispatchers.IO) { repository.save(state) }
    }
  }

  fun openScanResult(words: List<ScannedWord>, errorMessage: String? = null) {
    isScanning = false
    showScanSheet = false
    screen = AppScreen.ScanResult(words = words, errorMessage = errorMessage)
  }
  fun openScanSheet() {
    showScanSheet = true
  }
  val galleryLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        isScanning = true
        scope.launch {
          val result =
            runCatching {
              withContext(Dispatchers.IO) {
                scanImageUriToWords(context, uri)
              }
            }
          openScanResult(words = result.getOrDefault(emptyList()), errorMessage = result.exceptionOrNull()?.message)
        }
      }
    }
  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
      val uri = pendingCameraUri
      pendingCameraUri = null
      if (saved && uri != null) {
        isScanning = true
        scope.launch {
          val result = runCatching { withContext(Dispatchers.IO) { scanImageUriToWords(context, uri) } }
          openScanResult(words = result.getOrDefault(emptyList()), errorMessage = result.exceptionOrNull()?.message)
        }
      } else {
        isScanning = false
      }
    }

  when (val current = screen) {
    AppScreen.Home ->
      HomeScreen(
        state = state,
        onOpenScan = ::openScanSheet,
        onOpenBook = { screen = AppScreen.BookDetail(it) },
      )

    is AppScreen.ScanResult ->
      ScanResultScreen(
        words = current.words,
        errorMessage = current.errorMessage,
        onBack = { screen = AppScreen.Home },
        onRetry = ::openScanSheet,
        onSave = { title, words ->
          state = state.saveScannedBook(title, words)
          screen = AppScreen.Home
        },
      )

    is AppScreen.BookDetail ->
      VocabularyBookScreen(
        book = state.books.firstOrNull { it.id == current.bookId },
        words = state.wordsFor(current.bookId),
        onBack = { screen = AppScreen.Home },
        onRenameBook = { title -> state = state.renameBook(current.bookId, title) },
        onDeleteBook = {
          state = state.deleteBook(current.bookId)
          screen = AppScreen.Home
        },
        onAddWord = { word -> state = state.addWord(current.bookId, word) },
        onUpdateWord = { state = state.updateWord(it) },
        onDeleteWord = { state = state.deleteWord(it) },
        onMemorized = { wordId -> state = state.markMemorizedAndAdvance(current.bookId, wordId) },
        onNeedsReview = { wordId -> state = state.markNeedsReview(current.bookId, wordId) },
        onSpeak = onSpeak,
      )
  }

  if (showScanSheet) {
    ModalBottomSheet(onDismissRequest = { showScanSheet = false }) {
      ScanEntrySheet(
        isScanning = isScanning,
        onCamera = {
          val uri = createCameraImageUri(context)
          pendingCameraUri = uri
          cameraLauncher.launch(uri)
        },
        onGallery = { galleryLauncher.launch("image/*") },
      )
    }
  }
}

private fun createCameraImageUri(context: Context): Uri {
  val imageDirectory = File(context.cacheDir, "scan_images").apply { mkdirs() }
  val imageFile = File.createTempFile("vocab-scan-", ".jpg", imageDirectory)
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}
