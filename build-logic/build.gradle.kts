plugins {
    `kotlin-dsl`
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

// `implementation`, not `compileOnly`: the convention plugin applies these by id, so the
// plugin jars have to reach the consuming build's script classpath through this included
// build. With `compileOnly` every app would have to re-declare them with `apply false`,
// which is exactly the local build config this repo exists to delete.
dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApp") {
            id = "dev.evanhynes.android.app"
            implementationClass = "dev.evanhynes.convention.AndroidAppConventionPlugin"
        }
    }
}
