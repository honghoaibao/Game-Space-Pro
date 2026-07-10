// Top-level build file. Plugins are declared here with `apply false` so
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

// ktlint and detekt are APPLIED individually in each module's own
// `plugins { }` block (`alias(libs.plugins.ktlint)` /
// `alias(libs.plugins.detekt)`), NOT here via a blanket
// `subprojects { apply(plugin = ...) }` call like an earlier version of
// this file did. Root cause of a real, repeatable CI failure: applying
// ktlint's plugin from inside `subprojects { }` interacts badly with when
// `org.jetbrains.kotlin.jvm` gets applied to the same project — ktlint's
// plugin registers a `pluginManager.withPlugin("...kotlin...")` reactive
// hook internally, and cross-project `subprojects { }` timing doesn't
// guarantee the same ordering a plugin normally expects from its own
// module's `plugins { }` block ("Could not find any convention object of
// type KotlinSourceSet" applying kotlin.jvm — persisted across two
// different Kotlin versions, which ruled out a version-compatibility
// explanation and pointed at plugin-application ordering instead).
//
// This block only *configures* the ktlint/detekt extensions after each
// module has already applied them itself — guarded with `plugins.withId`
// so it only runs once that plugin is actually present, which is safe:
// pure configuration, not plugin application, so there's no equivalent
// ordering hazard.
subprojects {
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            android.set(true)
            ignoreFailures.set(false)
        }
    }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            parallel = true
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        }

        dependencies {
            add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:$detektFormattingVersion")
        }
    }
}
