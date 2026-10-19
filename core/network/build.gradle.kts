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

        // Ktor request/response logging toggle, decoupled from BuildConfig.DEBUG so a non-debuggable
        // build (e.g. the `profile` build type) can still surface the API call logs. Off by default;
        // debug builds also force it on via BuildConfig.DEBUG at the call site.
        buildConfigField("boolean", "ENABLE_NETWORK_LOGGING", "false")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        // Non-debuggable QA/profiling build: keep the API logs on while the app stays profileable.
        create("profile") {
            initWith(getByName("release"))
            buildConfigField("boolean", "ENABLE_NETWORK_LOGGING", "true")
        }
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
    implementation(libs.ktor.client.websockets)
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
    testImplementation(libs.ktor.client.mock)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
}
