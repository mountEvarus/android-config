# android-config

Shared build configuration for the Android apps. Sibling to
[go-config](https://github.com/mountEvarus/go-config) and
[js-config](https://github.com/mountEvarus/js-config), consumed the same way: as a git
submodule under `packages/`.

Public on purpose. Nothing here is secret, and a public submodule needs no token to clone.

```
libs.versions.toml                  the single version catalog
build-logic/                        the convention plugin
.github/workflows/android-app.yml   the reusable build/release workflow
sample/                             a minimal app that proves the plugin works
```

## Adding it to an app

```bash
git submodule add https://github.com/mountEvarus/android-config.git packages/android-config
```

`mobile/settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("../packages/android-config/build-logic")
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
        create("libs") { from(files("../packages/android-config/libs.versions.toml")) }
    }
}

rootProject.name = "<app>"
include(":app")
```

There is no `mobile/build.gradle.kts` at all. The convention plugin puts AGP and the Kotlin
plugins on the classpath itself, so there is no root build file and no
`plugins { ... apply false }` block to maintain.

`mobile/app/build.gradle.kts` carries dependencies and nothing else:

```kotlin
plugins {
    id("dev.evanhynes.android.app")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
}
```

`mobile/gradle.properties` names the app, which sets the applicationId
(`dev.evanhynes.<name>`) and the default signing key alias:

```properties
android.app.name=foo
```

Required, with no fallback, and the build fails with an explanatory message if it is
missing. Deriving it from the directory name would let a repo rename silently change the
applicationId, and to Android a different applicationId is a different app: the update
would install alongside the old one rather than replacing it, stranding its data.

For an existing app the value must match the applicationId already published. It becomes a
package segment, so it must be lowercase letters and digits, starting with a letter, which
is why long-name uses `longname`.

`mobile/version.properties` holds the hand-bumped part of the version:

```properties
VERSION_MAJOR=1
VERSION_MINOR=0
```

`.github/workflows/android.yml`:

```yaml
name: Android
on:
  push:
    branches: [main]
    paths: ['mobile/**', 'packages/android-config', '.github/workflows/android.yml']
  pull_request:
    paths: ['mobile/**', 'packages/android-config', '.github/workflows/android.yml']
  workflow_dispatch:

concurrency:
  group: android-${{ github.ref }}
  cancel-in-progress: true

jobs:
  android:
    uses: mountEvarus/android-config/.github/workflows/android-app.yml@main
    with:
      app-name: foo
      release-title: Foo (Android)
    secrets: inherit
```

## What the convention plugin sets

Applied by `id("dev.evanhynes.android.app")`:

- AGP, the Compose compiler plugin and ktlint
- `namespace` and `applicationId` as `dev.evanhynes.<app-name>`
- `compileSdk`, `targetSdk`, `minSdk` from the catalog
- `versionName`/`versionCode` from `version.properties` plus the CI run number
- the release signing config, from `keystore.properties` or `RELEASE_KEYSTORE_*`
- JDK 17 source/target and jvmTarget
- Android Lint with `abortOnError` (errors fail the build, warnings are reported)
- unit tests with Android resources and default return values enabled

An app still declares anything genuinely its own: `buildConfigField`,
`manifestPlaceholders`, extra plugins (`ksp`, `hilt`, `kotlin-serialization`), and a `minSdk`
override where a dependency forces one.

Room's schema directory stays with the app, because it needs the KSP plugin and Gradle's
embedded Kotlin is too old to compile against KSP's current metadata:

```kotlin
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
```

Note the submodule path has no `/**`. A submodule bump changes the gitlink path itself
(`:160000 160000 abc def M packages/android-config`), and a `/**` pattern needs a segment
beneath it, so it would never match: the app would take a catalog update without ever
building or releasing.

## Submodules

The workflow initialises **only** `packages/android-config`, not every submodule. A consuming
repo may also carry private submodules for its other surfaces, and `submodules: recursive` would
fail trying to clone those with the default token. The Android build never reads them, so it does
not ask for them, and no PAT is needed anywhere in this pipeline.

## Environment config

The workflow writes `mobile/local.properties` before building, from repo-level settings:

| Source | Key |
|---|---|
| `vars.API_BASE_URL` | `api.base.url` |
| `secrets.DEVICE_API_KEY` | `device.api.key` |
| `secrets.GOOGLE_MAPS_API_KEY` | `google.maps.api.key` |

Only keys that are set get written, so an app that does not use one falls back to its build
default. Apps read them the usual way and expose what they need as `buildConfigField`:

```kotlin
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
// ...
buildConfigField("String", "API_BASE_URL", "\"${localProperties.getProperty("api.base.url", "http://10.0.2.2:8080/")}\"")
```

The default is the Android emulator's host loopback, so a local API works out of the box.

## Signing

Resolved in order: `keystore.properties` at the Gradle root, then the `RELEASE_KEYSTORE_*`
environment variables, then nothing, which produces an unsigned APK.

Blank counts as missing. An unset GitHub secret arrives as an empty string rather than an
absent variable, so without that check a forgotten secret yields a broken signing config
instead of a clean fallback.

Repo secrets for a release: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

## Local development

AGP 9 needs **JDK 17**. A machine whose default `java` is newer will fail before Gradle
starts, with a message about the Gradle daemon rather than about the JDK, so it is worth
checking first:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew assembleDebug
```

On macOS, `brew install openjdk@17` if `/usr/libexec/java_home -v 17` finds nothing.

## The sample

`sample/` is a minimal Compose app wired to the convention plugin exactly as a real app is.
CI builds it on every push. It exists so a change here cannot silently break four apps that
would only find out on their next submodule bump.

```bash
cd sample && ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

## Versions

On AGP 9 and Gradle 9. AGP 8.13 was the original target, since it reaches compileSdk 36
without the AGP 9 migration, but current AndroidX has moved past it: Compose BOM 2026.08.00
pulls `runtime-saveable` 1.12.0, which requires AGP 9.1+ and compileSdk 37. Pinning a stale
Compose to stay on 8.x was the worse trade.

`compileSdk` is 37 so the apps compile against the newest APIs. `targetSdk` stays 36, so
nothing opts into Android 17 runtime behaviour changes until something needs them.

## Dependency updates

Renovate is configured in `renovate.json` and scoped deliberately:

```json
"enabledManagers": ["gradle", "gradle-wrapper", "github-actions"]
```

`enabledManagers` is a whitelist, so nothing else is ever picked up. Updates arrive in
three groups rather than a stream:

| Group | Contents |
|---|---|
| android build toolchain | AGP, Kotlin, KSP, ktlint, and the Gradle distribution |
| androidx | everything under `androidx.*` |
| github actions | the SHA-pinned actions in the reusable workflow |

The toolchain is one group on purpose: AGP, Kotlin, KSP and Gradle have to stay mutually
compatible, so a half-applied bump is worse than no bump. Weekly schedule, three
concurrent PRs, dependency dashboard on.

Because every consuming app takes its versions from this catalog, one merged PR here is
the update for all of them. Each app then picks it up by bumping its submodule pointer.
