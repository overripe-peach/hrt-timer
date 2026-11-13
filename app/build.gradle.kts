plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gayoddities.hrttimer"
    compileSdk = 37

    signingConfigs {
        create("release") {
            // Point to your keystore file relative to the app module directory
            storeFile = file("/Users/riley/AndroidStudioProjects/APKSignKey.jks")
            storePassword = "x!=Yxcr%@E<RLYz:H;49_N>iTYUmkbH^N%VC7SHW>+2EDX%+XDg=w:+A+tX^"
            keyAlias = "key69"
            keyPassword = "&&Pw+Q>f&DdLmhd%tprMN+k3FCp>A!243mx^x3.:_Q;fPkSxEp&g&&b.T#EQ"
        }
    }

    defaultConfig {
        applicationId = "com.gayoddities.hrttimer"
        minSdk = 24
        targetSdk = 37
        versionCode = 7
        versionName = "0.73.5"

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

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview) // <-- Required for preview annotations
    implementation(libs.androidx.compose.material3)
    testImplementation(platform(libs.androidx.compose.bom)) //

    // Glance & WorkManager
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)

    // Debug & Testing tools
    debugImplementation(libs.androidx.compose.ui.tooling) // <-- Required for tooling resolution
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(platform(libs.androidx.compose.bom))
}
