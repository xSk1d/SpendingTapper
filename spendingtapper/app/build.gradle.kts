import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing is optional. Point it at a keystore via app/keystore.properties
// (gitignored) or the SPENDINGTAPPER_KEYSTORE_* environment variables and release builds are
// signed with your own key, so you can install updates over the top of each other.
// With no keystore configured the release build falls back to the debug key, which
// still installs but changes identity on every machine that builds it.
val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

fun signingValue(propertyKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propertyKey) ?: System.getenv(envKey)

val keystorePath = signingValue("storeFile", "SPENDINGTAPPER_KEYSTORE_FILE")
val hasReleaseKeystore = keystorePath != null && rootProject.file(keystorePath).exists()

android {
    namespace = "dev.xsk1d.spendingtapper"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.xsk1d.spendingtapper"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystorePath!!)
                storePassword = signingValue("storePassword", "SPENDINGTAPPER_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SPENDINGTAPPER_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SPENDINGTAPPER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        // targetSdk deliberately tracks the Android version the phone actually runs
        // rather than the newest SDK available, so behaviour changes can be tested.
        disable += "OldTargetApi"
        // Dependency freshness is a deliberate decision made in the version catalog,
        // not something that should start failing the build on its own schedule.
        disable += "NewerVersionAvailable"
        // Lint suggests merging mipmap-anydpi-v26 into mipmap-anydpi at this minSdk,
        // but aapt then fails to resolve the launcher icon at all.
        disable += "ObsoleteSdkInt"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
