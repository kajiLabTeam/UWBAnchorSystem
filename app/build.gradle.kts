plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("com.google.gms.google-services")
}

android {
    namespace = "net.harutiro.uwbanchorsystem"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.harutiro.uwbanchorsystem"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Minio API Configuration
        buildConfigField("String", "MINIO_API_BASE_URL", "\"${project.property("MINIO_API_BASE_URL")}\"")
        buildConfigField("String", "MINIO_API_USERNAME", "\"${project.property("MINIO_API_USERNAME")}\"")
        buildConfigField("String", "MINIO_API_PASSWORD", "\"${project.property("MINIO_API_PASSWORD")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //  USBSerial
    implementation(libs.usb.serial.for1.android)

    // JetpackCompose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    testImplementation(kotlin("test"))

    // http
    implementation(libs.retrofit)
    implementation(libs.logging.interceptor)
    implementation(libs.converter.gson)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // nearby
    implementation(libs.play.services.nearby)
}
