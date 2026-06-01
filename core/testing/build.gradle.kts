import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure-Kotlin module: its fakes/fixtures/dispatcher utilities are framework-free, so it stays JVM so
// the pure-Kotlin :core:domain can consume it (Android modules consume the resulting jar just fine).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // Test utilities live in the main source set so consumers pick them up via
    // testImplementation(project(":core:testing")). Exposed as api(...) so the test libs
    // are transitively available to those consumers.
    api(project(":core:domain"))
    api(libs.junit5.api)
    api(libs.mockk)
    api(libs.turbine)
    api(libs.coroutines.test)
}
