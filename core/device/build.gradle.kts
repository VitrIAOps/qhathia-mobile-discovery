plugins {
    alias(libs.plugins.android.library)
}

// Device control with no knowledge of the host app: plain constructors (no Hilt), no dependency on
// other project modules, and no manifest entries of its own.
android {
    namespace = "com.qhathia.mobile.discovery.core.device"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit5.api)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
}
