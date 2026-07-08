import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gamespacepro.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
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
    // `api`, not `implementation`: this is a design-system module — every
    // module that depends on :ui is going to write Composables and needs
    // Compose UI/Material3/Color on its own compile classpath too. Forcing
    // every feature module to redeclare these would just be boilerplate
    // this module exists to avoid.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
