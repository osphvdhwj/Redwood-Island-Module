plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.dynamicisland"
    compileSdk = 35
    
    lint {
        abortOnError = false
    }

    defaultConfig {
        applicationId = "com.example.dynamicisland"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "3.0"

        // Reduce APK size by only supporting specific ABIs
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }

        // Keep only English resources to save space
        resourceConfigurations.addAll(listOf("en"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true // Enable resource shrinking
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
}

dependencies {
    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")

    // Image Loading
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")

    // System Components
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // UI Components
    implementation("com.google.android.material:material:1.12.0")

    // ML Kit Intelligence
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.0")

    // Xposed API
    compileOnly("de.robv.android.xposed:api:82")
}
