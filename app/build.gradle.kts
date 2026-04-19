import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.licenta_test"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.licenta_test"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //reads from local.properties and then creates BuildConfig.java and writes the api key in it
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        val apiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""

        // Generates the variable for the api key
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
    }

    buildFeatures{
        buildConfig = true //to activate the buildConfig feature
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
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0") //for the ai model
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2") //for the ai model
    implementation("com.google.guava:guava:33.1.0-android") //for the Future library
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.opencsv:opencsv:5.12.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}