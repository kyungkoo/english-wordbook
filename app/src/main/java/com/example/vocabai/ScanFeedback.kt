package com.example.vocabai

data class ScanFeedback(
  val title: String,
  val message: String,
  val canSave: Boolean,
) {
  companion object {
    fun forResult(words: List<ScannedWord>, errorMessage: String?): ScanFeedback =
      when {
        words.isNotEmpty() ->
          ScanFeedback(
            title = "${words.size}개 단어 발견",
            message = "저장하기 전에 오탈자를 확인해 주세요.",
            canSave = true,
          )
        !errorMessage.isNullOrBlank() ->
          ScanFeedback(
            title = "스캔에 실패했습니다",
            message = "이미지를 읽는 중 문제가 생겼습니다. 더 선명한 사진으로 다시 시도해 주세요.",
            canSave = false,
          )
        else ->
          ScanFeedback(
            title = "인식된 단어가 없습니다",
            message = "단어와 뜻이 잘 보이도록 더 선명한 사진으로 다시 시도해 주세요.",
            canSave = false,
          )
      }
  }
}
