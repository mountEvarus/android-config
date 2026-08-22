package dev.evanhynes.convention

import org.gradle.api.Project
import java.util.Properties

data class SemanticVersion(val name: String, val code: Int) {
    companion object {
        /**
         * MAJOR.MINOR comes from version.properties and is bumped by hand for meaningful
         * releases. PATCH is the CI run number, so every published build strictly increases
         * without anyone maintaining it. Local builds fall back to patch 0 and versionCode 1.
         */
        fun resolve(project: Project): SemanticVersion {
            val file = project.rootProject.file("version.properties")
            val props = Properties().apply {
                if (file.exists()) file.inputStream().use(::load)
            }
            val major = props.getProperty("VERSION_MAJOR", "1")
            val minor = props.getProperty("VERSION_MINOR", "0")

            return SemanticVersion(
                name = System.getenv("VERSION_NAME") ?: "$major.$minor.0",
                code = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1,
            )
        }
    }
}
