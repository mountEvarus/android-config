package dev.evanhynes.convention

import org.gradle.api.Project
import java.util.Properties

data class ReleaseSigning(
    val storeFile: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
) {
    companion object {
        /**
         * Signing config from `keystore.properties` (local dev), else the `RELEASE_KEYSTORE_*`
         * environment variables (CI), else null so `assembleRelease` produces an unsigned APK.
         */
        fun resolve(project: Project): ReleaseSigning? {
            val propsFile = project.rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = Properties().apply { propsFile.inputStream().use(::load) }
                fun prop(name: String) = props.getProperty(name)?.takeIf(String::isNotBlank)

                val storePassword = prop("storePassword") ?: return null
                return ReleaseSigning(
                    storeFile = prop("storeFile") ?: "release.jks",
                    storePassword = storePassword,
                    keyAlias = prop("keyAlias") ?: AppName.of(project),
                    // PKCS12 keystores cannot hold a key password distinct from the store
                    // password, so in practice these are the same value.
                    keyPassword = prop("keyPassword") ?: storePassword,
                )
            }

            val storePassword = env("RELEASE_KEYSTORE_PASSWORD") ?: return null
            return ReleaseSigning(
                storeFile = env("RELEASE_KEYSTORE_FILE") ?: "release.jks",
                storePassword = storePassword,
                keyAlias = env("RELEASE_KEY_ALIAS") ?: AppName.of(project),
                keyPassword = env("RELEASE_KEY_PASSWORD") ?: storePassword,
            )
        }

        /**
         * An unset GitHub secret arrives as an empty string rather than an absent variable, so
         * blank has to count as missing. Otherwise a forgotten secret yields a broken signing
         * config instead of falling back to a clean unsigned build.
         */
        private fun env(name: String): String? = System.getenv(name)?.takeIf(String::isNotBlank)
    }
}
