pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QhathiaDiscovery"
include(":app")
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:ui")
include(":core:testing")
include(":features:home")
include(":features:catalog")
include(":features:product-search")
include(":features:product-detail")
include(":features:cart")
include(":features:settings")
