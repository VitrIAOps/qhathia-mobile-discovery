plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.qhathia.mobile.discovery.core.network"
    compileSdk = 36
    defaultConfig {
        minSdk = 26

        // Endpoints. Qhathia API is plain HTTP (kiosk LAN backend) — :app enables cleartext.
        buildConfigField("String", "QHATHIA_BASE_URL", "\"http://87.99.156.164\"")
        buildConfigField("String", "DEEPGRAM_BASE_URL", "\"https://api.deepgram.com\"")

        // Interim development credentials for POST /api/v1/auth/login — used only until device
        // pairing/auth provides the real session JWT. Not a production secret.
        buildConfigField("String", "QHATHIA_DEV_EMAIL", "\"juan@tienda.com\"")
        buildConfigField("String", "QHATHIA_DEV_PASSWORD", "\"mipass123\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
}
