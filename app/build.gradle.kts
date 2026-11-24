import java.util.Properties

// Load local.properties
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

// Extract variables safely with fallbacks or error checks
val customStorePassword = localProperties.getProperty("storePassword") ?: ""
val customKeyPassword = localProperties.getProperty("keyPassword") ?: ""
val customStoreFile = localProperties.getProperty("storeFile") ?: ""
val customKeyAlias = localProperties.getProperty("keyAlias") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gayoddities.hrttimer"
    compileSdk = 37

    signingConfigs {
        create("release") {
            storeFile = File(customStoreFile)
            storePassword = customStorePassword
            keyAlias = customKeyAlias
            keyPassword = customKeyPassword
        }
    }

    defaultConfig {
        applicationId = "com.gayoddities.hrttimer"
        minSdk = 24
        targetSdk = 37
        versionCode = 13
        versionName = "0.81.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true // Standard syntax for minification
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Link the signing configuration here:
            signingConfig = signingConfigs.getByName("release")
            ndk {
                debugSymbolLevel = "FULL"
            }
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM (Covers main and androidTest source sets)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Glance & WorkManager
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)

    // Debug & Testing tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}