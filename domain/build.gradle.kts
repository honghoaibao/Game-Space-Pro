import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Deliberately a plain Kotlin/JVM module, not an Android library.
// The domain layer must stay free of android.* imports so business rules
// (use cases, models, repository contracts) are testable with plain JUnit
// and reusable if a non-Android surface (e.g. a CLI diagnostics tool) is
// ever added.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
