plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        versionName = "3.0.Satellite"

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildFeatures {
        buildConfig = true
        aidl = true
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
    implementation(project(":shared"))
    
    // Xposed API (Essential for Satellites)
    compileOnly(libs.xposed.api)
    
    // Lightweight dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)
}
