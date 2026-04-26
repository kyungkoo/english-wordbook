package com.example.vocabai

import kotlin.test.Test
import kotlin.test.assertEquals

class FlipCardAnimationTest {
  @Test
  fun showsFrontUntilHalfwayThroughRotation() {
    assertEquals(FlipCardFace.Front, flipCardFaceForRotation(0f))
    assertEquals(FlipCardFace.Front, flipCardFaceForRotation(89.9f))
  }

  @Test
  fun showsBackAfterHalfwayThroughRotation() {
    assertEquals(FlipCardFace.Back, flipCardFaceForRotation(90f))
    assertEquals(FlipCardFace.Back, flipCardFaceForRotation(180f))
  }

  @Test
  fun advancesToNextCardWithoutChangingStudyModeState() {
    assertEquals(1, nextFlipCardIndex(currentIndex = 0, total = 3))
    assertEquals(0, nextFlipCardIndex(currentIndex = 2, total = 3))
  }
}
