// Mirrors what a real app's settings.gradle.kts does, with the paths adjusted: an app reaches
// the submodule at ../packages/android-config, the sample is already inside it.
pluginManagement {
    includeBuild("../build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../libs.versions.toml")) }
    }
}

rootProject.name = "sample"
include(":app")
