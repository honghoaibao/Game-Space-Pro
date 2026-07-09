import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Release signing is driven entirely by environment variables so the same
// build.gradle.kts works both locally (unsigned release APK, the default)
// and in CI (signed, only when the CI workflow found the signing secrets —
// see .github/workflows/build.yml and the "GitHub Actions" section of the
// product spec: "If signing secrets exist: automatically sign. If not:
// unsigned release APK.").
val releaseKeystorePath: String? = System.getenv("SIGNING_STORE_FILE")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank()

// Overridable via `-PversionNameOverride=1.2.0 -PversionCodeOverride=42`,
// used by .github/workflows/release.yml to derive both from the pushed git
// tag rather than hand-editing this file per release. Falls back to these
// defaults for every local/debug/CI-build-job build.
val versionNameOverride = (findProperty("versionNameOverride") as String?) ?: "0.1.0"
val versionCodeOverride = (findProperty("versionCodeOverride") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.gamespacepro.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gamespacepro"
        // Android 9 (Pie), matching the "Android 9+" platform requirement.
        minSdk = 28
        targetSdk = 36
        versionCode = versionCodeOverride
        versionName = versionNameOverride

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":feature_shizuku"))
    implementation(project(":feature_accessibility"))
    implementation(project(":feature_dashboard"))

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // :ui exposes compose-bom/ui/ui-graphics/material3 via `api`, so they're
    // not redeclared here. Tooling artifacts stay explicit per-module since
    // they're debug/dev-only and :ui intentionally doesn't propagate those.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
