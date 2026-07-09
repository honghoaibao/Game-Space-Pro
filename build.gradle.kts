/ / Top-level build file. Plugins are declared here with `apply false` so
// their versions are resolved once via the version catalog, then applied
// per-module in each module's own build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// Captured here, at the root script's own top level, because the
// generated `libs` version-catalog accessor does NOT reliably resolve
// inside the `subprojects { }` closure below — that lambda's receiver is
// each individual subproject, a different script-compilation context than
// where `libs` is registered as an extension. (Confirmed via a real CI
// failure: "Extension with name 'libs' does not exist" when referenced
// directly inside `subprojects { }`.) Capturing the value as a plain
// String up here and closing over it sidesteps the issue entirely.
val detektFormattingVersion = libs.versions.detekt.get()

// ktlint + detekt are applied uniformly to every module (app, core, domain,
// and every module added later) from one place, rather than repeated in
// each module's build script, so CI's `ktlintCheck` / `detekt` tasks always
// cover the whole project without needing to remember to wire up new
// modules individually.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        ignoreFailures.set(false)
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    }

    dependencies {
        add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:$detektFormattingVersion")
    }
}
