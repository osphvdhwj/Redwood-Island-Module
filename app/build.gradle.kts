plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.dynamicisland"
    compileSdk = 35 // ⬆️ Upgraded to Android 15

    defaultConfig {
        applicationId = "com.example.dynamicisland"
        minSdk = 33
        targetSdk = 35 // ⬆️ Upgraded to Android 15
        versionCode = 1
        versionName = "1.0"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false

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
        sourceCompatibility = JavaVersion.VERSION_17 // ⬆️ Upgraded for Android 15 ART
        targetCompatibility = JavaVersion.VERSION_17 // ⬆️ Upgraded for Android 15 ART
    }
    kotlinOptions {
        jvmTarget = "17" // ⬆️ Upgraded for Android 15 ART
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // ⬆️ Bumped for newer Compose
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
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00") // ⬆️ Latest Stable BOM
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Wavy Slider
    implementation("ir.mahozad.multiplatform:wavy-slider:1.0.0")
}
