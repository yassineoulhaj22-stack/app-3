plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zonerush.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zonerush.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Nearby Connections: multijoueur Bluetooth / Wi-Fi direct, SANS internet
    implementation("com.google.android.gms:play-services-nearby:19.1.0")
}
