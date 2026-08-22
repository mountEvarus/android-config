package dev.evanhynes.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Everything identical across the personal-OS Android apps. An app applies this instead of
 * the android/kotlin/compose plugin stack, so its build file carries dependencies and
 * genuinely app-specific config, and nothing else.
 */
class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // AGP 9 has built-in Kotlin support, so applying org.jetbrains.kotlin.android here
        // is not just unnecessary, it is a hard error.
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun sdkLevel(alias: String) = libs.findVersion(alias).get().requiredVersion.toInt()

        val appId = "dev.evanhynes.${AppName.of(this)}"
        val appVersion = SemanticVersion.resolve(this)
        val signing = ReleaseSigning.resolve(this)

        extensions.configure<ApplicationExtension> {
            namespace = appId
            compileSdk = sdkLevel("compile-sdk")

            defaultConfig {
                applicationId = appId
                minSdk = sdkLevel("min-sdk")
                targetSdk = sdkLevel("target-sdk")
                versionCode = appVersion.code
                versionName = appVersion.name
            }

            buildFeatures {
                compose = true
            }

            if (signing != null) {
                signingConfigs.create("release") {
                    storeFile = rootProject.file(signing.storeFile)
                    storePassword = signing.storePassword
                    keyAlias = signing.keyAlias
                    keyPassword = signing.keyPassword
                }
            }

            buildTypes {
                getByName("release") {
                    // Off deliberately. These are sideloaded personal apps, so there is nothing
                    // worth obfuscating, and R8 would strip the reflection-based libraries
                    // (Hilt, Retrofit, Moshi) behind a keep-rule burden nobody wants to carry.
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                    signingConfig = signingConfigs.findByName("release")
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            lint {
                abortOnError = true
                warningsAsErrors = true
                // Dependency freshness is Renovate's job. Failing the build on it would make
                // every app red the day any library ships a release.
                disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
            }

            testOptions.unitTests {
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit").get())
        }
    }
}
