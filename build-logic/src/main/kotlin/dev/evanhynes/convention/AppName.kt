package dev.evanhynes.convention

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

object AppName {
    private const val PROPERTY = "android.app.name"

    /**
     * The app's short name, which becomes the applicationId (`dev.evanhynes.<name>`) and the
     * default signing key alias.
     *
     * Deliberately has no fallback. Deriving it from the directory name would make the
     * applicationId change silently if a repo were ever renamed, and to Android a different
     * applicationId is a different app: the update would install alongside the old one instead
     * of replacing it, stranding its data. An explicit line in gradle.properties costs nothing
     * and cannot drift.
     */
    fun of(project: Project): String {
        val name = (project.findProperty(PROPERTY) as String?)?.takeIf(String::isNotBlank)
            ?: throw InvalidUserDataException(
                """
                $PROPERTY is not set. Add it to gradle.properties:

                    $PROPERTY=<name>

                It sets the applicationId (dev.evanhynes.<name>) and the default signing key
                alias. For an existing app it must match the applicationId already published,
                or the update will install as a second app rather than upgrading the first.
                """.trimIndent(),
            )

        require(name.matches(Regex("[a-z][a-z0-9]*"))) {
            "$PROPERTY is '$name'. It becomes a package segment, so it must be lowercase " +
                "letters and digits only, starting with a letter. 'film-roll' would be invalid; " +
                "'filmroll' is what that app uses."
        }
        return name
    }
}
