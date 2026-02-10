plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    alias(libs.plugins.hilt)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.parktrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.parktrack"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.compose.material:material-icons-core:1.6.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.5.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // QR Code Generation
    implementation("com.google.zxing:core:3.5.2")
    
    // CameraX for scanning
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Pull-to-refresh
    implementation("androidx.compose.material:material:1.6.0")
    
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.0")
    
    // PDF Generation for reports
    implementation("com.itextpdf:itext7-core:7.2.5")
    
    // Date/Time
    implementation("joda-time:joda-time:2.12.5")
    
    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}