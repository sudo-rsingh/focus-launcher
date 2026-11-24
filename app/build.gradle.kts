// AGP 9 compiles Kotlin itself (built-in Kotlin), so no Kotlin plugin is applied.
// There are no library dependencies: no AndroidX, no Compose, nothing extra.
plugins {
    id("com.android.application")
}

// Release signing comes from CI secrets (see .github/workflows/android.yml).
// Without them, assembleRelease produces an unsigned APK.
val releaseKeystore = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }

android {
    namespace = "app.focuslauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.focuslauncher"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storeType = "pkcs12"
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
