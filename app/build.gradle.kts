import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ytdlp.forandroid"
    compileSdk = 36

    // Release signing from local.properties (git ignored). Without it the
    // release falls back to the debug key so builds never break.
    // Back up ~/keystores/ytdlp-release.keystore: losing it means the app
    // can never be updated under the same identity.
    val keystoreProps = Properties()
    rootProject.file("local.properties").takeIf { props -> props.exists() }?.let { propsFile ->
        propsFile.inputStream().use { stream -> keystoreProps.load(stream) }
    }
    val releaseStoreFile = keystoreProps.getProperty("release.store.file")
        ?.let { path -> file(path) }?.takeIf { candidate -> candidate.exists() }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = releaseStoreFile
                storePassword = keystoreProps.getProperty("release.store.password")
                keyAlias = keystoreProps.getProperty("release.key.alias")
                keyPassword = keystoreProps.getProperty("release.key.password")
            }
        }
    }

    defaultConfig {
        applicationId = "com.ytdlp.forandroid"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // youtubedl-android ships native .so per ABI; keep all four for now.
        // Use ABI splits / app bundles to slim downloads (arm64-v8a covers ~95% devices).
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
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
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true // required: youtubedl-android needs extractNativeLibs="true"
        }
        resources {
            // youtubedl-android bundles large python stdlib; ignore dupes
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.workmanager.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    // Unmodified yt-dlp engine + ffmpeg + aria2c, running locally on-device.
    // See https://github.com/yausername/youtubedl-android
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)
    implementation(libs.youtubedl.aria2c)
}
