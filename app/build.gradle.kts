plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// App version derived from the version catalog (single source of truth):
//   versionName = "<appVersion>-<appPrerelease>"  e.g. 1.0.0-SNAPSHOT
//   versionCode = major*10000 + minor*100 + patch e.g. 1.0.0 -> 10000  (monotonic; minor/patch < 100)
val appSemVer = libs.versions.appVersion.get()
val appVersionName = "$appSemVer-${libs.versions.appPrerelease.get()}"
val appVersionCode = appSemVer.split(".").map { it.toInt() }
    .let { (major, minor, patch) -> major * 10000 + minor * 100 + patch }

android {
    namespace = "com.qhathia.mobile.discovery"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.qhathia.mobile.discovery"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // QA / Android Studio profiling build: non-debuggable (clean profiler metrics) but
        // `profileable`, signed with the debug keystore so QA can install it, R8 left off to match
        // release behavior without keep-rule risk. Network API logs stay on via ENABLE_NETWORK_LOGGING.
        create("profile") {
            initWith(getByName("release"))
            isProfileable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Name every APK exactly `qhathia-discovery-v<versionName>.apk` (no build-type suffix). Each build
// type writes to its own outputs/apk/<buildType>/ folder, so the identical name does not collide.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName
                ?.set("qhathia-discovery-v$appVersionName.apk")
        }
    }
}

dependencies {
    // Feature + design-system modules
    implementation(project(":features:home"))
    implementation(project(":features:catalog"))
    implementation(project(":features:product-search"))
    implementation(project(":features:product-detail"))
    implementation(project(":features:cart"))
    implementation(project(":features:settings"))
    implementation(project(":features:onboarding"))
    implementation(project(":core:ui"))
    // Domain types referenced by the NavHost wiring (e.g. SearchSource on the Home → ProductSearch handoff).
    implementation(project(":core:domain"))
    // Data + network layers — included by the app shell so their Hilt bindings (StoreSessionRepository,
    // SpeechToTextRepository, AudioRecorder, HttpClient) are aggregated into the dependency graph.
    implementation(project(":core:data"))
    implementation(project(":core:network"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // lifecycle-runtime-compose — collectAsStateWithLifecycle in the app-shell NavHost gate.
    implementation(libs.lifecycle.runtime)

    // Navigation host
    implementation(libs.navigation.compose)

    // Hilt (application shell)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}