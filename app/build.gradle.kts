import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Release signing is supplied out-of-band so no key material lands in git.
// Either create keystore.properties in the project root (it is gitignored):
//     storeFile=/absolute/path/to/release.jks
//     storePassword=...
//     keyAlias=...
//     keyPassword=...
// or set MEMENTO_STORE_FILE / MEMENTO_STORE_PASSWORD / MEMENTO_KEY_ALIAS /
// MEMENTO_KEY_PASSWORD in the environment (for CI).
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(propKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propKey) ?: System.getenv(envKey)

val releaseStoreFile = signingValue("storeFile", "MEMENTO_STORE_FILE")
val hasReleaseSigning = releaseStoreFile != null && file(releaseStoreFile).exists()

android {
    namespace = "com.betteruniverse.mementolauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.betteruniverse.mementolauncher"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "MEMENTO_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "MEMENTO_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "MEMENTO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Deliberately left unsigned when no keystore is configured, rather than falling
            // back to the debug key — a debug-signed artifact must never be shippable by
            // accident. `assembleRelease` still builds and R8 still runs, so the release path
            // stays testable without key material.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            // The bundle's only native code is third-party: androidx.graphics.path (Compose)
            // and DataStore's shared-counter lib. Play Console warns that no debug symbols were
            // uploaded for them — that warning is UNAVOIDABLE here and safe to ignore: AndroidX
            // publishes those .so files pre-stripped and does not ship symbols, so
            // mergeReleaseNativeDebugMetadata runs NO-SOURCE (verified). This setting is kept so
            // that if any future dependency (or our own NDK code) ships unstripped natives,
            // their symbol tables are packaged into the bundle automatically.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
    }

    testOptions {
        unitTests {
            // Android framework stubs throw "not mocked" by default, so any production code path
            // that touches android.util.Log is untestable on the JVM. Returning defaults makes
            // logging a no-op in tests instead, which is what lets the error-handling branches
            // (unreadable stored JSON, and so on) be covered at all.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Jetpack Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.google.material)

    // WorkManager for background updates
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // JSON Serialization
    implementation(libs.kotlinx.serialization.json)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)

    // Debug tooling
    debugImplementation(libs.androidx.ui.tooling)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
}
