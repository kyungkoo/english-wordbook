plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
}

android {
  namespace = "com.example.vocabai"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.vocabai"
    minSdk = 31
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
  }
}

dependencies {
  implementation("androidx.core:core-ktx:1.15.0")
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation(platform("androidx.compose:compose-bom:2026.03.01"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.room:room-runtime:2.8.4")
  implementation("androidx.room:room-ktx:2.8.4")
  implementation("com.google.ai.edge.litertlm:litertlm-android:0.10.0")
  implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")
  implementation("com.google.mlkit:text-recognition:16.0.1")
  implementation("com.google.mlkit:text-recognition-korean:16.0.1")
  ksp("androidx.room:room-compiler:2.8.4")
  testImplementation(kotlin("test"))
  debugImplementation("androidx.compose.ui:ui-tooling")
}
