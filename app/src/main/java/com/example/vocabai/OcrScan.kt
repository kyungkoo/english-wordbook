package com.example.vocabai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text as MlKitText
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun scanBitmapToWords(bitmap: Bitmap): List<ScannedWord> {
  val inputImage = InputImage.fromBitmap(bitmap, 0)
  return scanInputImageToWords(inputImage)
}

suspend fun scanImageUriToWords(context: Context, uri: Uri): List<ScannedWord> {
  val inputImage = InputImage.fromFilePath(context, uri)
  return scanInputImageToWords(inputImage)
}

private suspend fun scanInputImageToWords(inputImage: InputImage): List<ScannedWord> {
  val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

  val latinText =
    try {
      latinRecognizer.processImageAwait(inputImage)
    } finally {
      latinRecognizer.close()
    }
  val koreanText =
    try {
      koreanRecognizer.processImageAwait(inputImage)
    } finally {
      koreanRecognizer.close()
    }

  val koreanRows = OcrVocabularyParser.parse(latinText, koreanText, VocabMode.EN_KO)
  val englishRows = OcrVocabularyParser.parse(latinText, koreanText, VocabMode.EN_EN).associateBy { it.term.lowercase() }

  return koreanRows.map { row ->
    ScannedWord(
      english = row.term,
      koreanMeaning = row.meaning,
      englishMeaning = englishRows[row.term.lowercase()]?.meaning.orEmpty(),
    )
  }
}

private suspend fun com.google.mlkit.vision.text.TextRecognizer.processImageAwait(image: InputImage): MlKitText =
  suspendCancellableCoroutine { continuation ->
    process(image)
      .addOnSuccessListener { result -> continuation.resume(result) }
      .addOnFailureListener { error -> continuation.resumeWithException(error) }
  }
