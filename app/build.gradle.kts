plugins {
    alias(libs.plugins.android.application)

    // Google Services plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.todolistapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.todolistapp"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // -------------------------------
    // 🔥 Firebase
    // -------------------------------

    // Firebase Bill Of Materials (версии можно не указывать)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))

    // Firestore (база данных)
    implementation("com.google.firebase:firebase-firestore")

    // Auth (если понадобится авторизация)
    implementation("com.google.firebase:firebase-auth")
}
