package com.example.vocabai

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface AppRoute {
    data object Home : AppRoute

    data class ScanResult(val words: List<ScannedWord>, val errorMessage: String? = null) : AppRoute

    data class BookDetail(val bookId: String, val pane: BookPane = BookPane.Hub) : AppRoute
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordNoteApp(
    isSpeakingWordList: Boolean,
    onSpeak: (String) -> Unit,
    onToggleSpeakWordList: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember(context) { VocabularyRepository(context) }
    var state by remember { mutableStateOf(VocabularyUiState()) }
    var loaded by remember { mutableStateOf(false) }
    val backStack = remember { mutableStateListOf<Any>(AppRoute.Home) }
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

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            (context as? Activity)?.finish()
        }
    }

    fun navigateHome() {
        backStack.clear()
        backStack.add(AppRoute.Home)
    }

    fun openScanResult(words: List<ScannedWord>, errorMessage: String? = null) {
        isScanning = false
        showScanSheet = false
        if (backStack.lastOrNull() is AppRoute.ScanResult) {
            backStack.removeLastOrNull()
        }
        backStack.add(AppRoute.ScanResult(words = words, errorMessage = errorMessage))
    }
    fun openScanSheet() {
        showScanSheet = true
    }
    fun openBook(bookId: String) {
        backStack.add(AppRoute.BookDetail(bookId = bookId))
    }
    fun openBookPane(bookId: String, pane: BookPane) {
        backStack.add(AppRoute.BookDetail(bookId = bookId, pane = pane))
    }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                isScanning = true
                scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) { scanImageUriToWords(context, uri) }
                    }
                    openScanResult(
                        words = result.getOrDefault(emptyList()),
                        errorMessage = result.exceptionOrNull()?.message,
                    )
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
                    val result = runCatching {
                        withContext(Dispatchers.IO) { scanImageUriToWords(context, uri) }
                    }
                    openScanResult(
                        words = result.getOrDefault(emptyList()),
                        errorMessage = result.exceptionOrNull()?.message,
                    )
                }
            } else {
                isScanning = false
            }
        }

    NavDisplay(
        backStack = backStack,
        onBack = ::navigateBack,
        entryProvider = { key ->
            when (key) {
                is AppRoute.Home ->
                    NavEntry(key) {
                        HomeScreen(
                            state = state,
                            onOpenScan = ::openScanSheet,
                            onOpenBook = ::openBook,
                        )
                    }

                is AppRoute.ScanResult ->
                    NavEntry(key) {
                        ScanResultScreen(
                            words = key.words,
                            errorMessage = key.errorMessage,
                            onBack = ::navigateBack,
                            onRetry = ::openScanSheet,
                            onSave = { title, words ->
                                state = state.saveScannedBook(title, words)
                                navigateHome()
                            },
                        )
                    }

                is AppRoute.BookDetail ->
                    NavEntry(key) {
                        val bookId = key.bookId
                        VocabularyBookScreen(
                            book = state.books.firstOrNull { it.id == bookId },
                            words = state.wordsFor(bookId),
                            pane = key.pane,
                            onBack = ::navigateBack,
                            onOpenList = { openBookPane(bookId, BookPane.List) },
                            onOpenFlipCard = { openBookPane(bookId, BookPane.FlipCard) },
                            onOpenLetterGame = { openBookPane(bookId, BookPane.LetterGame) },
                            onRenameBook = { title -> state = state.renameBook(bookId, title) },
                            onDeleteBook = {
                                state = state.deleteBook(bookId)
                                navigateHome()
                            },
                            onAddWord = { word -> state = state.addWord(bookId, word) },
                            onUpdateWord = { state = state.updateWord(it) },
                            onDeleteWord = { state = state.deleteWord(it) },
                            onMemorized = { wordId ->
                                state = state.markMemorizedAndAdvance(bookId, wordId)
                            },
                            onNeedsReview = { wordId ->
                                state = state.markNeedsReview(bookId, wordId)
                            },
                            isSpeakingWordList = isSpeakingWordList,
                            onSpeak = onSpeak,
                            onToggleSpeakWordList = onToggleSpeakWordList,
                        )
                    }

                else -> error("Unknown route: $key")
            }
        },
    )

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
