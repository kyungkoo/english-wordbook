# AGENTS.md

## Project

This repository contains `english-wordbook`, an Android app for building and studying English vocabulary books from scanned worksheets.

The user-facing app name is `영단어장`.

## Tech Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose + Material 3
- Room
- ML Kit text recognition, including Korean OCR
- Gradle wrapper is included; use `.\gradlew` from the repository root on Windows.

## Important Paths

- `app/src/main/java/com/example/vocabai/` - Kotlin app source.
- `app/src/test/java/com/example/vocabai/` - JVM unit tests.
- `app/src/main/res/` - Android resources, launcher icons, splash icon, and XML resources.
- `app/src/main/AndroidManifest.xml` - app label, launcher icon, FileProvider, and activity setup.

## Common Commands

Run unit tests:

```powershell
.\gradlew testDebugUnitTest
```

Build the debug APK:

```powershell
.\gradlew assembleDebug
```

Install on a connected device:

```powershell
.\gradlew installDebug
```

Check connected devices:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
```

## Development Notes

- Keep generated build output out of git. `.gradle/`, `.kotlin/`, `app/build/`, and `local.properties` are intentionally ignored.
- Do not commit `local.properties`; it contains machine-specific Android SDK paths.
- Keep the project path ASCII-only on Windows. Android/Gradle tooling can fail under non-ASCII project paths.
- When changing camera or OCR behavior, verify both gallery input and camera capture paths.
- Camera capture uses a `FileProvider`; keep `app/src/main/res/xml/file_paths.xml` and the manifest provider in sync.
- Launcher icons are stored in `mipmap-*`; the high-resolution source and splash image are in `drawable-nodpi`.
- After UI changes, run `.\gradlew testDebugUnitTest` and install with `.\gradlew installDebug` when device verification is requested.

## Current UX Details

- The scan result save action accounts for system navigation bars with `navigationBarsPadding()`.
- The flip card study screen supports tapping to flip, listening to pronunciation, and moving to the next card without marking memorization.
- The app icon and splash artwork currently use an ABC clicker image.
