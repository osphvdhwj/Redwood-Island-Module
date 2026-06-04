plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
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

        ndk {
            abiFilters.add("arm64-v8a")
        }

        resourceConfigurations.addAll(listOf("en"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("keystore.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.accompanist.drawablepainter)

    // Image Loading
    implementation(libs.coil.compose)

    // System Components
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime-ktx)
    
    // Concurrency
    implementation(libs.coroutines.android)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.google.truth)
    testImplementation(libs.turbine)

    // UI Components
    implementation(libs.androidx.appcompat)

    // ML Kit Intelligence
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language)

    // Xposed API
    compileOnly(libs.xposed.api)
}
