import java.util.Properties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

// --- Configuration Loading ---

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("velora.keystore")

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

// Load local properties for sensitive signing information
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// --- Helper functions for versioning ---

/**
 * Generates a descriptive suffix based on UTC time to ensure consistency.
 * Example result: -evening-07052026184335 (UTC)
 */
fun getVersionSuffix(): String {
    val date = Date()
    val utcZone = TimeZone.getTimeZone("UTC")

    // Explicitly set UTC timezone for the hour calculation
    val hourFormat = SimpleDateFormat("HH").apply { timeZone = utcZone }
    val hour = hourFormat.format(date).toInt()

    val timeOfDay = when (hour) {
        in 5..10 -> "morning"
        in 11..14 -> "afternoon"
        in 15..18 -> "evening"
        in 19..23 -> "night"
        else -> "midnight"
    }

    // Explicitly set UTC timezone for the timestamp
    val timestamp = SimpleDateFormat("ddMMyyyyHHmmss").apply {
        timeZone = utcZone
    }.format(date)

    return "-$timeOfDay-$timestamp"
}

/**
 * Generates a unique version code based on the UTC date and hour.
 * Format: YYMMDDHH (e.g., 26050718)
 */
fun generateVersionCode(): Int {
    return SimpleDateFormat("yyMMddHH").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date()).toInt()
}

// CRITICAL: Store suffix in a variable to ensure consistency across the build process
val buildSuffix = getVersionSuffix()
val fullVersionName = "0.1.3$buildSuffix"

// ---------------------------------------

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.andrutstudio.velora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.andrutstudio.velora"
        minSdk = 26
        targetSdk = 35

        // Uses UTC-based version code
        versionCode = generateVersionCode()

        versionName = fullVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = keystoreProperties["storeFile"]?.let { file(it as String) }
            storePassword = keystoreProperties["storePassword"] as? String ?: ""
            keyAlias = keystoreProperties["keyAlias"] as? String ?: ""
            keyPassword = keystoreProperties["keyPassword"] as? String ?: ""
        }
        create("release") {
            storeFile = localProperties.getProperty("signing.storeFile")?.let { file(it) }
            storePassword = localProperties.getProperty("signing.storePassword") ?: ""
            keyAlias = localProperties.getProperty("signing.keyAlias") ?: ""
            keyPassword = localProperties.getProperty("signing.keyPassword") ?: ""
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "RPC_MAINNET", "\"https://pactus.org\"")
            buildConfigField("String", "RPC_TESTNET", "\"https://pactus.org\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "RPC_MAINNET", "\"https://pactus.org\"")
            buildConfigField("String", "RPC_TESTNET", "\"https://pactus.org\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // --- Automatic APK Renaming Implementation ---
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this
            if (output is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                // variant.versionName inherits the UTC buildSuffix from defaultConfig
                val fileName = "VeloraWallet-${variant.versionName}.apk"
                output.outputFileName = fileName
            }
        }
    }
}

dependencies {
    // UI & Core
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    // Architecture & DI
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Persistence
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Cryptography & Blockchain
    implementation(libs.security.crypto)
    implementation(libs.biometric)
    implementation(libs.trustwallet.core)

    // Utilities
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.android)
    implementation(libs.coil.compose)
    implementation(libs.browser)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.reorderable)

    // Background Tasks
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
}
