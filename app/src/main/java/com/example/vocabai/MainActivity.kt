package com.example.vocabai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.Locale

class MainActivity : ComponentActivity() {
  private var tts: TextToSpeech? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private var wordListBatch = 0
  private var finalWordListUtteranceId: String? = null
  private var isSpeakingWordList by mutableStateOf(false)

  private companion object {
    const val WORD_LIST_PAUSE_MS = 700L
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MaterialTheme {
        DisposableEffect(Unit) {
          tts =
            TextToSpeech(this@MainActivity) { status ->
              if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
              }
            }
          tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
              override fun onStart(utteranceId: String?) = Unit

              override fun onDone(utteranceId: String?) {
                finishWordListIfNeeded(utteranceId)
              }

              @Deprecated("Deprecated in Java")
              override fun onError(utteranceId: String?) {
                finishWordListIfNeeded(utteranceId)
              }
            },
          )
          onDispose {
            tts?.stop()
            tts?.shutdown()
          }
        }

        Surface(color = Color(0xFFF6F7F2), modifier = Modifier.fillMaxSize()) {
          WordNoteApp(
            isSpeakingWordList = isSpeakingWordList,
            onSpeak = ::speakWord,
            onToggleSpeakWordList = ::toggleSpeakWordList,
          )
        }
      }
    }
  }

  private fun speakWord(word: String) {
    stopWordList()
    tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "wordnote-$word")
  }

  private fun toggleSpeakWordList(words: List<String>) {
    if (isSpeakingWordList) {
      stopWordList()
      return
    }

    val readableWords = words.map { it.trim() }.filter { it.isNotBlank() }
    val speaker = tts ?: return
    if (readableWords.isEmpty()) return

    speaker.stop()
    wordListBatch += 1
    isSpeakingWordList = true
    readableWords.forEachIndexed { index, word ->
      val utteranceId = "word-list-$wordListBatch-$index"
      if (index == readableWords.lastIndex) {
        finalWordListUtteranceId = utteranceId
      }
      speaker.speak(
        word,
        if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
        null,
        utteranceId,
      )
      if (index < readableWords.lastIndex) {
        speaker.playSilentUtterance(WORD_LIST_PAUSE_MS, TextToSpeech.QUEUE_ADD, "word-list-pause-$wordListBatch-$index")
      }
    }
  }

  private fun stopWordList() {
    tts?.stop()
    finalWordListUtteranceId = null
    isSpeakingWordList = false
  }

  private fun finishWordListIfNeeded(utteranceId: String?) {
    if (utteranceId == null || utteranceId != finalWordListUtteranceId) return
    mainHandler.post {
      if (utteranceId == finalWordListUtteranceId) {
        finalWordListUtteranceId = null
        isSpeakingWordList = false
      }
    }
  }
}
