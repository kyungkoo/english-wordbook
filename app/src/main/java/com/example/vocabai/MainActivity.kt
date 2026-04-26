package com.example.vocabai

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.Locale

class MainActivity : ComponentActivity() {
  private var tts: TextToSpeech? = null

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
          onDispose {
            tts?.stop()
            tts?.shutdown()
          }
        }

        Surface(color = Color(0xFFF6F7F2), modifier = Modifier.fillMaxSize()) {
          WordNoteApp(onSpeak = { word ->
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "wordnote-$word")
          })
        }
      }
    }
  }
}
