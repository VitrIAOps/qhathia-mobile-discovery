plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.qhathia.mobile.discovery.core.testing"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
