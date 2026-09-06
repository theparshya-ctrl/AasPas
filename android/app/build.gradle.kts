plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")
// DEBUG API_BASE_URL is environment-based (never hard-code in app code):
// - Emulator: omit API_BASE_URL to use http://10.0.2.2:8000/
// - Physical device: set Tailscale URL in android/local.properties (gitignored)
// - Release: production HTTPS below (not this debug value)
val apiBaseUrl: String = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:8000/")
    .let { if (it.endsWith("/")) it else "$it/" }

val betaProperties = Properties().apply {
    val file = rootProject.file("beta.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
// BETA URL is baked at build time from android/beta.properties (HTTPS only; never Tailscale/LAN).
val betaApiBaseUrl: String = betaProperties.getProperty("BETA_API_BASE_URL", "https://aaspas-beta.fly.dev/")
    .let { if (it.endsWith("/")) it else "$it/" }

android {
    namespace = "com.aaspas.customer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aaspas.customer"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "APP_ENVIRONMENT", "\"DEV\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        manifestPlaceholders["APP_LABEL"] = "AasPas"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "APP_ENVIRONMENT", "\"DEV\"")
            manifestPlaceholders["APP_LABEL"] = "AasPas DEV"
        }
        create("beta") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            buildConfigField("String", "API_BASE_URL", "\"$betaApiBaseUrl\"")
            buildConfigField("String", "APP_ENVIRONMENT", "\"BETA\"")
            manifestPlaceholders["APP_LABEL"] = "AasPas BETA"
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://api.aaspas.example/\"")
            buildConfigField("String", "APP_ENVIRONMENT", "\"PRODUCTION\"")
            manifestPlaceholders["APP_LABEL"] = "AasPas"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.5")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:6.2.1")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}
