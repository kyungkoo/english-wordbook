# English Wordbook

`영단어장` is an Android vocabulary study app for creating wordbooks from worksheet photos and practicing them with simple study modes.

## Features

- Scan a worksheet from the camera or gallery.
- Extract English words and Korean meanings with ML Kit OCR.
- Review and edit scanned words before saving.
- Manage multiple vocabulary books.
- Study with flip cards, including pronunciation playback.
- Skip to the next flip card without marking it.
- Practice spelling with a letter game.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- ML Kit Text Recognition
- Gradle

## Project Structure

```text
app/src/main/java/com/example/vocabai/   App source
app/src/main/res/                        Android resources and launcher icons
app/src/test/java/com/example/vocabai/   Unit tests
```

## Requirements

- Android Studio or Android SDK installed
- JDK compatible with the Android Gradle Plugin
- A connected Android device or emulator for installation

This project includes the Gradle wrapper, so use `.\gradlew` from the repository root on Windows.

## Build And Test

Run unit tests:

```powershell
.\gradlew testDebugUnitTest
```

Build a debug APK:

```powershell
.\gradlew assembleDebug
```

Install on a connected device:

```powershell
.\gradlew installDebug
```

## Notes

- `local.properties` is intentionally ignored because it contains local Android SDK paths.
- Keep the project path ASCII-only on Windows for better Android/Gradle compatibility.
- The app icon and splash artwork are stored under `app/src/main/res/`.
