// The whole point of the convention plugin: an app declares its dependencies and nothing else.
plugins {
    id("dev.evanhynes.android.app")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
}
