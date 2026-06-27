plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "de.nichu42.boxviewer"
  compileSdk = 37

  defaultConfig {
    applicationId = "de.nichu42.boxviewer"
    minSdk = 24
    targetSdk = 37
    versionCode = 8
    versionName = "0.40"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "$rootDir/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      versionNameSuffix = "-debug"
    }
  }


  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

configurations {
  val sharedTestImplementation = configurations.create("sharedTestImplementation") {
    extendsFrom(configurations.implementation.get())
  }
  configurations.testImplementation.get().extendsFrom(sharedTestImplementation)
  configurations.androidTestImplementation.get().extendsFrom(sharedTestImplementation)
}

// Ensure at least one secrets-related properties file exists so that the Secrets Gradle Plugin does not fail configuration.
val localPropsExist = project.rootProject.file("local.properties").exists()
val envFileExist = project.rootProject.file(".env").exists()
val envExampleFileExist = project.rootProject.file(".env.example").exists()

if (!localPropsExist && !envFileExist && !envExampleFileExist) {
  try {
    project.rootProject.file("local.properties").createNewFile()
  } catch (_: Exception) {
    // Ignore any exceptions
  }
}

// Configure the Secrets Gradle Plugin to use the .env and .env.example files
// if they exist, otherwise fallback automatically to default properties.
secrets {
  val envFile = project.rootProject.file(".env")
  val envExampleFile = project.rootProject.file(".env.example")
  if (envFile.exists()) {
    propertiesFileName = ".env"
  }
  if (envExampleFile.exists()) {
    defaultPropertiesFileName = ".env.example"
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.coil.svg)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.zxing.core)
  testImplementation(libs.androidx.core)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.runner)
  "sharedTestImplementation"(libs.androidx.compose.ui.test.junit4)
  "sharedTestImplementation"(libs.androidx.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}


