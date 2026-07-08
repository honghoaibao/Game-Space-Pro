pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GameSpacePro"

// NOTE: only modules that actually exist are included here. The full
// architecture (data, ui, feature_*) from the product spec is added
// incrementally as each module is scaffolded — an empty `include()` for a
// module with no build.gradle.kts yet would break the sync.
include(":app")
include(":core")
include(":domain")
include(":ui")
include(":data")
include(":feature_shizuku")
