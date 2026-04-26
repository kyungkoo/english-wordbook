package com.example.vocabai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanFeedbackTest {
  @Test
  fun explainsEmptyScanWithRetryGuidance() {
    val feedback = ScanFeedback.forResult(words = emptyList(), errorMessage = null)

    assertEquals("인식된 단어가 없습니다", feedback.title)
    assertTrue(feedback.message.contains("더 선명한 사진"))
    assertTrue(feedback.canSave.not())
  }

  @Test
  fun explainsFailedScanWithRecoverableAction() {
    val feedback = ScanFeedback.forResult(words = emptyList(), errorMessage = "이미지를 열 수 없습니다.")

    assertEquals("스캔에 실패했습니다", feedback.title)
    assertTrue(feedback.message.contains("다시 시도"))
    assertFalse(feedback.canSave)
  }

  @Test
  fun allowsSavingWhenWordsExist() {
    val feedback = ScanFeedback.forResult(
      words = listOf(ScannedWord("abandon", "버리다", "to leave")),
      errorMessage = null,
    )

    assertEquals("1개 단어 발견", feedback.title)
    assertTrue(feedback.canSave)
  }
}
