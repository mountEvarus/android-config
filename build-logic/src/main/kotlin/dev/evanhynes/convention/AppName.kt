package dev.evanhynes.convention

import org.gradle.api.Project

object AppName {
    /**
     * The app's short name, used for the applicationId, the default signing key alias and the
     * release asset. Taken from `android.app.name` in gradle.properties, falling back to the
     * repo directory name so a single-app repo needs no configuration at all.
     */
    fun of(project: Project): String =
        (project.findProperty("android.app.name") as String?)?.takeIf(String::isNotBlank)
            ?: project.rootProject.projectDir.let { dir ->
                // `mobile/` is a layout convention, not an identity, so step past it.
                if (dir.name == "mobile") dir.parentFile.name else dir.name
            }
}
