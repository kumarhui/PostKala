plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // The new Kotlin 2.0+ way to handle the Compose Compiler
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "cvam.dignity.postkala"
    // Targeting the latest SDKs requires modern Java targets
    compileSdk = 36

    defaultConfig {
        applicationId = "cvam.dignity.postkala"
        minSdk = 26
        targetSdk = 35 // Updated to match compileSdk for better compatibility
        versionCode = 4
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // REQUIRED: Modern Compose and SDK 35+ require Java 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // REQUIRED: Must match compileOptions
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

// Configuration for the new Kotlin Compose Compiler plugin
composeCompiler {
    enableStrongSkippingMode = true
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Explicitly adding Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // --- FIX: Added Splash Screen API Library ---
    implementation("androidx.core:core-splashscreen:1.0.1")

    // --- FIX: Guava dependency for ListenableFuture ---
    implementation("com.google.guava:guava:33.0.0-android")

    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // CameraX
    val cameraVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    // ML Kit & Scanning
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.zxing:core:3.5.3")

    // Google Play In-App Updates
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}