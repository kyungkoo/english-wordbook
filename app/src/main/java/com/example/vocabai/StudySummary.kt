package com.example.vocabai

data class StudySummary(
  val total: Int,
  val memorized: Int,
  val review: Int,
  val progress: Float,
) {
  val reviewLabel: String =
    when {
      total == 0 -> "단어 없음"
      review == 0 -> "복습 완료"
      else -> "복습 ${review}개"
    }

  companion object {
    fun from(words: List<WordEntry>): StudySummary {
      val total = words.size
      val memorized = words.count { it.memorized }
      val review = total - memorized
      val progress = if (total == 0) 0f else memorized.toFloat() / total.toFloat()
      return StudySummary(total = total, memorized = memorized, review = review, progress = progress)
    }
  }
}
