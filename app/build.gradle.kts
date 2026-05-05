plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.dynamicisland"
    compileSdk = 36 // ⬆️ Upgraded to Android 16 (Baklava)

    defaultConfig {
        applicationId = "com.example.dynamicisland"
        minSdk = 33
        targetSdk = 36 // ⬆️ Upgraded to Android 16 (Baklava)
        versionCode = 1
        versionName = "1.0"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
        compose = true
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" 
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    compileOnly("de.robv.android.xposed:api:82")

    // Animations & Palette
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Jetpack Compose Integration
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Wavy Slider
    implementation("ir.mahozad.multiplatform:wavy-slider:1.0.0")

    // ── BATCH 6 additions ──────────────────────────────────────────────────────

    // MLKit on-device translation (no network required after model download)
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.mlkit:language-id:17.0.6")

    // MLKit barcode scanning (for Continuity Camera)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // CameraX (for Continuity Camera viewfinder)
    val cameraXVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
}