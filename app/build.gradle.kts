plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.overload.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.overload.app"
        minSdk = 26
        targetSdk = 35
        // Version follows the web app (overloadVersion in gradle.properties) plus a build number from CI:
        // 0.17.1, 0.17.2, ... versionCode always goes up, also when the web version changes.
        val base = (project.findProperty("overloadVersion") as String? ?: "0.0").trim()
        val (major, minor) = base.split(".").map { it.toInt() } + listOf(0, 0)
        val build = (System.getenv("BUILD_NUMBER") ?: "0").toInt()
        versionCode = major * 1_000_000 + minor * 1_000 + build
        versionName = if (build > 0) "$base.$build" else base
    }
    // Fixed debug key (committed) so every CI build has the same signature and installs over the last one.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android"
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
}
