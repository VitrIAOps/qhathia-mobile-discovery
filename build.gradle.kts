// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Every plugin is declared here with `apply false` so its version resolves once on the build
// classpath; modules then apply via `alias(...)` without re-requesting a version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}