# Android Mobile Frontend — Development Standards & Playbook

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
  - [Core Technologies](#core-technologies)
  - [UI Framework](#ui-framework)
  - [Architecture & DI](#architecture--di)
  - [Network & Async](#network--async)
  - [Testing Framework](#testing-framework)
- [Project Structure & Modularization](#project-structure--modularization)
  - [Module Taxonomy](#module-taxonomy)
  - [Dependency Graph Rules](#dependency-graph-rules)
- [Architecture Standards](#architecture-standards)
  - [Clean Architecture Layers](#clean-architecture-layers)
  - [Unidirectional Data Flow (UDF)](#unidirectional-data-flow-udf)
  - [MVI Contract](#mvi-contract)
- [Coding Standards & Naming Conventions](#coding-standards--naming-conventions)
  - [Kotlin Language Guidelines](#kotlin-language-guidelines)
  - [Naming Convention Reference Table](#naming-convention-reference-table)
  - [Package & Module Naming](#package--module-naming)
- [Network & Asynchrony Standards](#network--asynchrony-standards)
  - [Ktor Client Configuration](#ktor-client-configuration)
  - [Kotlin Flows Contract](#kotlin-flows-contract)
  - [Error Handling Strategy](#error-handling-strategy)
- [Dependency Injection with Hilt](#dependency-injection-with-hilt)
- [Testing Standards](#testing-standards)
  - [Testing Pyramid](#testing-pyramid)
  - [ViewModel Testing](#viewmodel-testing)
  - [Composable UI Testing](#composable-ui-testing)
- [Performance Best Practices](#performance-best-practices)
- [Development Workflow](#development-workflow)
- [Vertical Feature Example — Product Discovery](#vertical-feature-example--product-discovery)

---

## Overview

This document defines the **mandatory standards, conventions, and architectural guidelines** for every Android engineer contributing to this project. It acts as the single source of truth — a technical playbook — to ensure code consistency, maintainability, testability, and a high-quality user experience.

All code must follow Modern Android Development (MAD) principles as defined by Google, with Jetpack Compose as the only UI toolkit. Deviation from these standards requires explicit architectural justification documented as an ADR (Architecture Decision Record).

---

## Technology Stack

### Core Technologies

| Concern | Technology | Stable Version |
|---|---|---|
| Language | Kotlin | **2.3.20** |
| Min SDK | Android 8.0 | API 26 |
| Target SDK | Android 16 | API 36 |
| Compile SDK | Android 16 | API 36 |
| Build System | Gradle (KTS) + Version Catalog | **9.x** |
| JDK | Eclipse Temurin / any distribution | **21+** |
| Coroutines | kotlinx-coroutines | **1.11.0** |

> **Note — KSP1 End of Life:** Kotlin 2.3.x drops support for KSP1 (kapt-based annotation processing). All projects **must** use **KSP2** (`com.google.devtools.ksp` ≥ `2.3.x`). Hilt, Room, and Compose Destinations must be configured with the KSP Gradle plugin, not kapt.


| Concern | Technology |
|---|---|
| UI Toolkit | Jetpack Compose (BOM managed) |
| Design System | Material Design 3 (`material3`) |
| Navigation | Compose Destinations or Navigation Compose |
| Image Loading | Coil 3 (Compose extension) |
| Animations | Compose Animation APIs |

### Architecture & DI

| Concern | Technology |
|---|---|
| Architecture | Clean Architecture + MVI (or MVVM) |
| ViewModel | `androidx.lifecycle:lifecycle-viewmodel-compose` |
| Dependency Injection | Hilt (Dagger Hilt) |
| Async State | Kotlin StateFlow / SharedFlow |

### Network & Async

| Concern | Technology | Stable Version |
|---|---|---|
| HTTP Client | Ktor Client (engine: CIO) | **3.4.3** |
| Serialization | `kotlinx-serialization-json` | **1.10.0** |
| Reactive Streams | Kotlin Flow (no RxJava, no LiveData) | — |

### Testing Framework

| Scope | Technology |
|---|---|
| Unit Tests | JUnit5 + MockK + Turbine |
| UI Tests | Compose Testing APIs (`compose-ui-test`) |
| E2E | UI Automator / Maestro |
| Coverage | Kover |

### Version Catalog Reference

The canonical version catalog (`gradle/libs.versions.toml`) must declare the following verified-stable set. All versions are pinned together — **upgrading one may require upgrading others** (especially the Kotlin ↔ KSP2 pair).

```toml
# gradle/libs.versions.toml  — verified stable as of May 2026
[versions]
kotlin            = "2.3.20"
# Since KSP 2.3.0 the version is standalone — no longer tied to Kotlin compiler version.
# Latest stable: 2.3.6 (Feb 2026). Do NOT use the old <kotlin>-<patch> format.
ksp               = "2.3.6"
agp               = "9.2.0"             # Android Gradle Plugin (April 2026)
gradle            = "9.4"               # Gradle wrapper (requires JDK 21+)

# Jetpack
compose-bom       = "2026.05.00"         # BOM resolves all androidx.compose.* versions
lifecycle         = "2.9.0"
navigation        = "2.9.0"
hilt              = "2.57.1"           # stay on 2.57.1; 2.58 has AGP 9 issues, 2.59 has a runtime artifact bug.
                                       # On AGP 9 needs gradle.properties flags + a kotlin-metadata-jvm override
                                       # on the ksp classpath (see notes below + android-development-guide.md).
hilt-androidx     = "1.2.0"             # androidx.hilt extensions (ViewModel, Navigation)
room              = "2.7.1"

# Network & Async
ktor              = "3.4.3"
coroutines        = "1.11.0"
serialization     = "1.10.0"

# UI & Utils
coil              = "3.1.0"
immutable-collections = "0.3.8"
# material-icons-extended is FROZEN at 1.7.8 and decoupled from the Compose BOM, so it needs
# an explicit version. Heavy (~9 MB) — prefer core icons or text toggles where you can.
material-icons-extended = "1.7.8"

# Storage / Security
datastore         = "1.1.2"
# security-crypto: the MasterKey / MasterKey.Builder + EncryptedSharedPreferences.create() API
# only exists from 1.1.0-alpha onward (1.0.0 has just the deprecated MasterKeys string-alias API).
# alpha06 is the last published build; the library is unmaintained — for new work prefer
# DataStore + a Keystore-backed cipher over EncryptedSharedPreferences.
security-crypto   = "1.1.0-alpha06"

# Testing
junit5            = "5.11.4"
mockk             = "1.13.14"
turbine           = "1.2.0"
kover             = "0.9.1"

[plugins]
android-app               = { id = "com.android.application",                    version.ref = "agp"    }
android-library           = { id = "com.android.library",                        version.ref = "agp"    }
# kotlin-android is NOT listed here — AGP 9.x applies Kotlin automatically (built-in Kotlin).
# Do NOT add org.jetbrains.kotlin.android to any module.
kotlin-compose            = { id = "org.jetbrains.kotlin.plugin.compose",        version.ref = "kotlin" }
kotlin-serialization      = { id = "org.jetbrains.kotlin.plugin.serialization",  version.ref = "kotlin" }
ksp                       = { id = "com.google.devtools.ksp",                    version.ref = "ksp"    }
hilt                      = { id = "com.google.dagger.hilt.android",             version.ref = "hilt"   }
kover                     = { id = "org.jetbrains.kotlinx.kover",                version.ref = "kover"  }

[libraries]
# Compose (resolved via BOM — no explicit version needed)
compose-bom               = { group = "androidx.compose",           name = "compose-bom",               version.ref = "compose-bom" }
compose-ui                = { group = "androidx.compose.ui",         name = "ui"                        }
compose-ui-tooling        = { group = "androidx.compose.ui",         name = "ui-tooling"                }
# ui-tooling-preview carries the @Preview annotation API — any module with @Preview composables
# needs it as implementation(...) (ui-tooling above is debugImplementation(...), for rendering only).
compose-ui-tooling-preview = { group = "androidx.compose.ui",        name = "ui-tooling-preview"        }
compose-ui-test           = { group = "androidx.compose.ui",         name = "ui-test-junit4"            }
compose-material3         = { group = "androidx.compose.material3",  name = "material3"                 }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended", version.ref = "material-icons-extended" }

# Lifecycle & ViewModel
lifecycle-viewmodel       = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime         = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose",   version.ref = "lifecycle" }

# Navigation
navigation-compose        = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Hilt
hilt-android              = { group = "com.google.dagger", name = "hilt-android",           version.ref = "hilt"          }
hilt-compiler             = { group = "com.google.dagger", name = "hilt-compiler",           version.ref = "hilt"          }
hilt-navigation-compose   = { group = "androidx.hilt",     name = "hilt-navigation-compose", version.ref = "hilt-androidx" }
# Overrides the kotlin-metadata-jvm bundled with Hilt 2.57.x (which caps at metadata 2.2.0) so it
# can read Kotlin 2.3 metadata. Add as ksp(...) on EVERY Hilt module; version.ref = "kotlin" keeps
# it aligned with the compiler automatically.
kotlin-metadata-jvm       = { group = "org.jetbrains.kotlin", name = "kotlin-metadata-jvm",   version.ref = "kotlin"       }

# Ktor
ktor-client-core          = { group = "io.ktor", name = "ktor-client-core",              version.ref = "ktor" }
ktor-client-cio           = { group = "io.ktor", name = "ktor-client-cio",               version.ref = "ktor" }
ktor-client-content-neg   = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-client-logging       = { group = "io.ktor", name = "ktor-client-logging",           version.ref = "ktor" }
ktor-serialization-json   = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }

# Serialization & Coroutines
kotlinx-serialization     = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines        = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android",  version.ref = "coroutines"    }
kotlinx-immutable         = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "immutable-collections" }

# Coil
coil-compose              = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }

# Storage / Security
datastore-preferences     = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
security-crypto           = { group = "androidx.security",   name = "security-crypto",      version.ref = "security-crypto" }

# Testing
junit5-api                = { group = "org.junit.jupiter",  name = "junit-jupiter-api",    version.ref = "junit5" }
junit5-engine             = { group = "org.junit.jupiter",  name = "junit-jupiter-engine", version.ref = "junit5" }
# junit-platform-launcher is required on the test runtime classpath in Gradle 9.x.
# Without it, Gradle fails with "Failed to load JUnit Platform" even with useJUnitPlatform() set.
junit5-launcher           = { group = "org.junit.platform", name = "junit-platform-launcher" }
mockk                     = { group = "io.mockk",           name = "mockk",                version.ref = "mockk"  }
turbine                   = { group = "app.cash.turbine",   name = "turbine",              version.ref = "turbine" }
coroutines-test           = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
```

> **KSP2 versioning (changed in KSP 2.3.0):** Since KSP 2.3.0, the version number is **standalone** and no longer tied to the Kotlin compiler version. The old `<kotlin>-<patch>` format (e.g. `2.0.0-1.0.21`) is deprecated and incompatible with KSP 2.3+. Use a plain version like `2.3.6`. You can upgrade `kotlin` and `ksp` independently, though it's still good practice to update both together when new releases align.

---

## Project Structure & Modularization

The project adopts **feature-based modularization** combined with shared core layers. Each Gradle module maps to a single, clearly-defined responsibility boundary.

> **IMPORTANT — Gradle modules, not packages:** Every entry in the tree below is a **separate Gradle subproject**, not a Java/Kotlin package directory. Each module lives at the repository root as its own directory, has its own `build.gradle.kts`, and is registered in the root `settings.gradle.kts` via an `include()` statement. Do **not** create these as subdirectories or packages inside `:app`.

> **Current project state:** The project is in a **single-module transitional state** — `settings.gradle.kts` currently only declares `include(":app")` and all code lives inside `:app` organized by packages. When implementing any new feature or core layer, you **must** create it as a proper Gradle module following the checklist in the [Creating a New Module](#creating-a-new-module) section. Do not add new layers or features as packages inside `:app`.

```
root/
├── app/                          # Application shell — DI wiring, manifest, theme entry
├── core/
│   ├── data/                     # Repository implementations, local DB, datasources
│   ├── domain/                   # Pure business logic — UseCases, domain models, interfaces
│   ├── network/                  # Ktor client setup, interceptors, serialization
│   ├── ui/                       # Shared Compose components, design tokens, theme
│   ├── testing/                  # Shared test utilities, fakes, fixtures
│   └── common/                   # Kotlin extensions, utilities, constants
├── features/
│   ├── auth/                     # Login, registration, session management
│   ├── home/                     # Home/dashboard feed
│   ├── product-discovery/        # Product listing, search, filters
│   ├── product-detail/           # Product detail, gallery, purchase flow
│   └── profile/                  # User profile, settings
└── build-logic/                  # Convention plugins — shared Gradle config
```

### Module Taxonomy

**`app`** — The thin application module. It wires Hilt components, declares the `NavHost`, applies the global `MaterialTheme`, and holds the `AndroidManifest.xml`. It must not contain business logic.

**`core:domain`** — Pure Kotlin module with zero Android dependencies. Contains `UseCase` classes, domain model data classes, and repository interfaces (ports). This is the innermost ring of the Clean Architecture.

**`core:data`** — Implements the repository interfaces from `core:domain`. Holds Room DAOs, local data sources, and maps between network/local DTOs and domain models. Depends on `core:network` and `core:domain`.

**`core:network`** — Owns the Ktor `HttpClient` singleton, API interface declarations, request/response DTOs annotated with `@Serializable`, and network error mapping. Has no knowledge of domain models.

**`core:ui`** — The project's component library. Contains atomic and molecular Compose components (buttons, cards, inputs), `MaterialTheme` configuration, typographic scale, and color tokens. Has no ViewModel or business logic.

**`core:testing`** — Shared test infrastructure: `FakeRepository` implementations, `TestDispatcherRule`, fixture factories, and MockK helpers.

**`features/*`** — Vertical slices of the application. Each feature module contains its own `Screen` composables, `ViewModel`, `UiState`, `UiEvent`, Hilt module, and navigation graph. A feature module may depend on `core:domain`, `core:ui`, and `core:common`, but **never on another feature module**.

**`build-logic`** — Gradle convention plugins that centralize repetitive build configuration (Android library config, Compose compiler flags, Hilt plugin wiring, etc.) to keep feature `build.gradle.kts` files lean.

### Dependency Graph Rules

```
features/* → core:domain ← core:data → core:network
features/* → core:ui
features/* → core:common
app        → features/* + core:*
core:data  → core:domain  (implements interfaces)
core:*     → core:testing (testImplementation only)
```

**Hard rules:**
- Feature modules must not depend on other feature modules.
- `core:domain` must have zero Android framework dependencies.
- `core:network` must not depend on `core:domain` (map at the data layer boundary).

### Creating a New Module

Follow these steps **in order** every time a new Gradle module needs to be created. Skipping any step will produce a broken build.

**Step 1 — Create the module directory at the repository root**

Use the Gradle path separator (`:`) to derive the filesystem path: `:core:domain` → `core/domain/`, `:feature:auth` → `features/auth/`.

```
mkdir -p core/domain/src/main/java/com/qhathia/kiosk/core/domain
mkdir -p core/domain/src/test/java/com/qhathia/kiosk/core/domain
```

**Step 2 — Add a `build.gradle.kts` to the new module directory**

Choose the template that matches the module type (see templates below) and write it to `<module-dir>/build.gradle.kts`.

**Step 3 — Register the module in the root `settings.gradle.kts`**

Add an `include()` call. Nested paths use `:` as separator:

```kotlin
// settings.gradle.kts
include(":app")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:ui")
include(":core:common")
include(":core:testing")
include(":features:auth")
include(":features:home")
// … one line per module
```

**Step 4 — Wire the dependency in consuming modules**

In any `build.gradle.kts` that needs the new module, add it as an `implementation` or `testImplementation` dependency:

```kotlin
dependencies {
    implementation(project(":core:domain"))
    testImplementation(project(":core:testing"))
}
```

**Step 5 — Create the `AndroidManifest.xml` (Android Library modules only)**

Android library modules require a minimal manifest at `src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

---

### Module `build.gradle.kts` Templates

#### Pure Kotlin module — `core:domain`

No Android dependencies. Use this template for any module that must remain framework-agnostic.

```kotlin
// core/domain/build.gradle.kts
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

> **Note:** Do not apply `com.android.library` or any Android plugin here — `core:domain` must have zero Android framework dependencies.

#### Android Library module — `core:data`, `core:network`, `core:ui`, `core:common`, `core:testing`

```kotlin
// e.g. core/network/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)      // only if this module uses @Serializable
    alias(libs.plugins.ksp)                       // only if this module uses Hilt or Room
    alias(libs.plugins.hilt)                      // only if this module defines Hilt modules
}

android {
    namespace = "com.qhathia.kiosk.core.network"   // adjust per module
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
    // Declare only what this module directly needs.
    // Example for core:network:
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.neg)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization)

    // Hilt (if applicable)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)             // required on AGP 9 / Kotlin 2.3 — see metadata note

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
}
```

#### Feature module — `features:auth`, `features:home`, etc.

Feature modules are Android Library modules with Compose and Hilt enabled, plus dependencies on the `core:*` modules they need.

```kotlin
// e.g. features/auth/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.qhathia.kiosk.feature.auth"   // adjust per feature
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)   // @Preview annotation API
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)             // required on AGP 9 / Kotlin 2.3 — see metadata note

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)

    debugImplementation(libs.compose.ui.tooling)
}
```

---

## Architecture Standards

### Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│             Presentation Layer           │
│  Screen (Composable) ←→ ViewModel       │
│  UiState · UiEvent · UiEffect           │
├─────────────────────────────────────────┤
│               Domain Layer               │
│  UseCase · Repository (interface)        │
│  Domain Model · Business Rules           │
├─────────────────────────────────────────┤
│                Data Layer                │
│  RepositoryImpl · RemoteDataSource       │
│  LocalDataSource · DTO ↔ Domain Mapper   │
├─────────────────────────────────────────┤
│              Framework Layer             │
│  Ktor HttpClient · Room · Datastore      │
└─────────────────────────────────────────┘
```

Each layer communicates **only inward**. The domain layer defines interfaces; all outer layers implement them. This makes the domain fully testable without any framework dependency.

### Unidirectional Data Flow (UDF)

```
 User Action
     │
     ▼
 ViewModel.onEvent(UiEvent)
     │
     ▼
 UseCase.invoke()          ← core:domain
     │
     ▼
 Repository.fetch()        ← core:data
     │
     ▼
 Ktor / Room               ← core:network / local DB
     │
     ▼ (Flow / suspend)
 RepositoryImpl maps DTO → DomainModel
     │
     ▼
 ViewModel updates _uiState (StateFlow)
     │
     ▼
 Screen collects uiState → recompose
```

The flow is strictly one-directional. Events flow **down** (UI → ViewModel → UseCase → Data). State flows **up** (Data → ViewModel → UI). This eliminates shared mutable state and race conditions.

### MVI Contract

Every feature exposes three sealed types:

```kotlin
// UiState — the complete, immutable snapshot of what the screen should show.
// Must be a data class for structural equality checks by Compose.
data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

// UiEvent — user-driven intents (button taps, text input, pull-to-refresh).
sealed interface ProductListUiEvent {
    data object Refresh : ProductListUiEvent
    data class Search(val query: String) : ProductListUiEvent
    data class OpenDetail(val productId: String) : ProductListUiEvent
}

// UiEffect — one-shot side-effects that must not be replicated in UiState
// (navigation, snackbar, toast). Delivered via SharedFlow.
sealed interface ProductListUiEffect {
    data class NavigateToDetail(val productId: String) : ProductListUiEffect
    data class ShowError(val message: String) : ProductListUiEffect
}
```

---

## Coding Standards & Naming Conventions

### Kotlin Language Guidelines

- **All code, comments, log messages, and documentation must be in English.**
- Prefer `val` over `var`; immutability is the default.
- Use `data class` for DTOs and domain models; use `sealed class` / `sealed interface` for state and event hierarchies.
- Use Kotlin extension functions to avoid utility class proliferation.
- Avoid using `!!` (non-null assertion). Prefer `?.let {}`, `requireNotNull()`, or explicit null checks.
- Coroutine scope must always be `viewModelScope` inside ViewModels. Never launch coroutines with `GlobalScope`.
- Prefer structured concurrency: use `supervisorScope` and `coroutineScope` appropriately.
- Use `@OptIn` annotations explicitly when consuming experimental APIs; never suppress them globally.

### Naming Convention Reference Table

| Artifact | Convention | Example |
|---|---|---|
| Composable function | PascalCase noun/noun-phrase | `ProductCard`, `SearchBar`, `ProductListScreen` |
| Composable preview function | PascalCase + `Preview` suffix | `ProductCardPreview`, `ProductListScreenPreview` |
| UiState class | `<Feature>UiState` | `ProductListUiState`, `AuthUiState` |
| UiEvent sealed type | `<Feature>UiEvent` | `ProductListUiEvent` |
| UiEffect sealed type | `<Feature>UiEffect` | `ProductListUiEffect` |
| ViewModel class | `<Feature>ViewModel` | `ProductListViewModel`, `AuthViewModel` |
| UseCase class | `<Verb><Subject>UseCase` | `GetProductsUseCase`, `LoginUseCase` |
| Repository interface | `<Subject>Repository` | `ProductRepository`, `AuthRepository` |
| Repository implementation | `<Subject>RepositoryImpl` | `ProductRepositoryImpl` |
| Remote data source | `<Subject>RemoteDataSource` | `ProductRemoteDataSource` |
| Local data source | `<Subject>LocalDataSource` | `ProductLocalDataSource` |
| DTO (network) | `<Subject>Dto` | `ProductDto`, `ProductListResponseDto` |
| Domain model | Plain noun PascalCase | `Product`, `User`, `SearchResult` |
| Hilt module | `<Subject>Module` | `ProductModule`, `NetworkModule` |
| Kotlin file (non-class) | camelCase | `productMappers.kt`, `dateUtils.kt` |
| Variable | camelCase | `productList`, `isLoading`, `searchQuery` |
| Constant | UPPER_SNAKE_CASE | `DEFAULT_TIMEOUT_MS`, `BASE_URL` |
| Gradle module | kebab-case | `feature-product-discovery`, `core-network` |
| Package | lowercase dot-separated | `com.example.feature.productdiscovery` |

### Package & Module Naming

Each feature module follows this internal package layout:

```
com.example.app.feature.productdiscovery/
├── di/
│   └── ProductDiscoveryModule.kt
├── ui/
│   ├── ProductListScreen.kt       # Stateful (Smart) composable + ViewModel
│   ├── ProductListContent.kt      # Stateless (Dumb) composable
│   ├── ProductCard.kt             # Atomic composable
│   └── ProductListViewModel.kt
├── model/
│   ├── ProductListUiState.kt
│   ├── ProductListUiEvent.kt
│   └── ProductListUiEffect.kt
└── navigation/
    └── ProductDiscoveryNavGraph.kt
```

---

## Network & Asynchrony Standards

### Ktor Client Configuration

The `HttpClient` is a singleton provided by Hilt from `core:network`. It must not be instantiated anywhere else.

```kotlin
// core:network — KtorClientFactory.kt  (Ktor 3.4.3)
@Provides
@Singleton
fun provideHttpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            coerceInputValues = true
        })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000L
        connectTimeoutMillis = 15_000L
        socketTimeoutMillis = 30_000L
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Timber.tag("KtorClient").d(message)
            }
        }
        level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
    }
    defaultRequest {
        url(BuildConfig.BASE_URL)
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        header(HttpHeaders.AcceptLanguage, "en")
    }
}
```

**Rules:**
- All response DTOs must be annotated with `@Serializable`.
- Network calls are **suspend functions** returning `Result<T>` or a domain-specific sealed `NetworkResult<T>`. They must never throw uncaught exceptions to the caller.
- Timeouts, retry logic, and auth token injection (if applicable) are handled inside Ktor plugin configuration — never duplicated at the use-case or ViewModel level.

```kotlin
// core:network — ProductApiService.kt
class ProductApiService(private val client: HttpClient) {

    suspend fun getProducts(page: Int, pageSize: Int): NetworkResult<ProductListResponseDto> =
        runCatching {
            client.get("products") {
                parameter("page", page)
                parameter("pageSize", pageSize)
            }.body<ProductListResponseDto>()
        }.toNetworkResult()
}
```

### Kotlin Flows Contract

- **`StateFlow`** is the standard mechanism for exposing UI state from a ViewModel. It is always initialized with a default value and is immediately available to collectors.
- **`SharedFlow`** is used exclusively for one-shot events (`UiEffect`) that must not be replayed on configuration changes (e.g., navigation commands, Snackbar triggers).
- **`Flow` (cold)** is used in the data and domain layers for streams of data from repositories.
- **`LiveData` is forbidden.** Migrate any existing usage to `StateFlow`.
- Always collect flows with `collectAsStateWithLifecycle()` in composables to respect the UI lifecycle and avoid memory leaks.
- **Never call `StateFlow.value` inside a composable body.** Direct `.value` access is a snapshot read that does not trigger recomposition and raises a `StateFlowValueCalledInComposition` lint error. For reactive UI state, always use `collectAsStateWithLifecycle()`. Reading `.value` outside composition (e.g., in `Activity.onCreate()` before `setContent`, or in a ViewModel) is safe and correct.

```kotlin
// ViewModel — correct Flow exposure
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductListUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<ProductListUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: ProductListUiEvent) {
        when (event) {
            is ProductListUiEvent.Refresh -> loadProducts()
            is ProductListUiEvent.OpenDetail -> navigateToDetail(event.productId)
            is ProductListUiEvent.Search -> onSearch(event.query)
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getProductsUseCase()
                .onSuccess { products ->
                    _uiState.update { it.copy(products = products.toImmutableList(), isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                    _uiEffect.emit(ProductListUiEffect.ShowError(error.message ?: "Unknown error"))
                }
        }
    }

    private fun navigateToDetail(productId: String) {
        viewModelScope.launch {
            _uiEffect.emit(ProductListUiEffect.NavigateToDetail(productId))
        }
    }
}
```

### Error Handling Strategy

Define a sealed `NetworkResult` type in `core:network` to represent the three possible states of any network call, and map it to domain-level `Result<T>` at the repository boundary.

```kotlin
// core:network — NetworkResult.kt
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class HttpError(val code: Int, val message: String) : NetworkResult<Nothing>()
    data class NetworkError(val cause: Throwable) : NetworkResult<Nothing>()
}

fun <T> Result<T>.toNetworkResult(): NetworkResult<T> = fold(
    onSuccess = { NetworkResult.Success(it) },
    onFailure = { throwable ->
        when (throwable) {
            is ResponseException -> NetworkResult.HttpError(
                throwable.response.status.value,
                throwable.response.status.description,
            )
            else -> NetworkResult.NetworkError(throwable)
        }
    },
)
```

---

## Dependency Injection with Hilt

- **Hilt is the sole DI framework.** Koin and manual DI are forbidden.
- Every `ViewModel` must be annotated with `@HiltViewModel` and have its dependencies `@Inject`ed via the constructor.
- Hilt modules are declared per-module and installed in the appropriate `Component`.
- Use `@Singleton` for stateless services (HttpClient, repositories). Use `@ViewModelScoped` for objects that must live as long as the ViewModel.
- `@ApplicationContext` and `@ActivityContext` qualifiers must be used explicitly to avoid context leaks.
- **Hilt 2.56+ requires KSP2** — configure via `ksp(libs.hilt.compiler)`, never `kapt(libs.hilt.compiler)`. Mixing kapt and KSP2 in the same module causes compilation errors.

```kotlin
// build.gradle.kts (any module using Hilt) — KSP2 setup
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)                     // KSP2 — never use kapt() here
    ksp(libs.kotlin.metadata.jvm)               // AGP 9 / Kotlin 2.3: lets Hilt read 2.3 metadata
    implementation(libs.hilt.navigation.compose)
}

// core:network — NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = KtorClientFactory.create()

    @Provides
    @Singleton
    fun provideProductApiService(client: HttpClient): ProductApiService =
        ProductApiService(client)
}

// feature:product-discovery — ProductDiscoveryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class ProductDiscoveryModule {

    @Binds
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
}
```

---

## Testing Standards

### Testing Pyramid

- **70% Unit tests** — ViewModels, UseCases, Repositories (with fakes), mappers, utility functions.
- **20% Integration tests** — Repository + DataSource interaction, navigation graph correctness.
- **10% UI / E2E tests** — Critical user journeys via Compose Testing or Maestro.

> **JUnit 5 Gradle setup:** JUnit 5 requires `useJUnitPlatform()` in `testOptions` **and** `junit-platform-launcher` on the test runtime classpath. Without both, Gradle 9.x fails with "Failed to load JUnit Platform". The full test dependency block for a module using JUnit5 + MockK + Turbine:
>
> ```kotlin
> // build.gradle.kts
> android {
>     testOptions {
>         unitTests.all { it.useJUnitPlatform() }
>     }
> }
> dependencies {
>     testImplementation(libs.junit5.api)
>     testImplementation(libs.mockk)
>     testImplementation(libs.turbine)
>     testImplementation(libs.coroutines.test)
>     testRuntimeOnly(libs.junit5.engine)
>     testRuntimeOnly(libs.junit5.launcher)  // required in Gradle 9.x — not bundled automatically
> }
> ```

### ViewModel Testing

```kotlin
class ProductListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule() // replaces Dispatchers.Main with TestDispatcher

    private val fakeGetProductsUseCase = FakeGetProductsUseCase()
    private lateinit var viewModel: ProductListViewModel

    @BeforeEach
    fun setUp() {
        viewModel = ProductListViewModel(fakeGetProductsUseCase)
    }

    @Test
    fun `when Refresh event is received, loading state is emitted then products`() = runTest {
        fakeGetProductsUseCase.emit(Result.success(fakeProducts))

        viewModel.onEvent(ProductListUiEvent.Refresh)

        viewModel.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            val loaded = awaitItem()
            assertEquals(fakeProducts, loaded.products.toList())
            assertFalse(loaded.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Composable UI Testing

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun productCard_displaysNameAndPrice() {
    composeTestRule.setContent {
        AppTheme {
            ProductCard(
                product = Product(id = "1", name = "Headphones", price = "$99"),
                onCardClick = {},
            )
        }
    }

    composeTestRule.onNodeWithText("Headphones").assertIsDisplayed()
    composeTestRule.onNodeWithText("$99").assertIsDisplayed()
}
```

**Testing rules:**
- Use `FakeRepository` and `FakeUseCase` implementations from `core:testing` — never mock repositories with MockK.
- Use **Turbine** for testing Flow emissions in sequence.
- Use `TestDispatcher` (via `MainDispatcherRule`) to control coroutine scheduling in unit tests.
- Composable tests must always set the content inside `AppTheme`.

---

## Performance Best Practices

- **Defer reads**: Pass lambdas to composables instead of computed values when the value changes frequently, to defer the read to the innermost composable and skip unnecessary outer recompositions.
- **`key()` in `LazyColumn`**: Always specify a stable, unique `key` lambda in `items()` to preserve composable identity across list updates and avoid full rebinding.
- **`derivedStateOf`**: Wrap computed state that derives from other state objects to prevent redundant recompositions when the source changes but the derived value does not.
- **Avoid deep lambda capture chains** in performance-sensitive list items.
- **Baseline Profiles**: Generate a Baseline Profile using Macrobenchmark for critical startup and navigation paths. Commit the `baseline-prof.txt` to source control.
- **R8 / ProGuard**: Full R8 minification and resource shrinking must be enabled for release builds.
- **Image loading**: Always specify `size` constraints when loading images with Coil to avoid loading unnecessarily large bitmaps.

```kotlin
// Correct — key ensures identity in lazy lists
LazyColumn {
    items(products, key = { it.id }) { product ->
        ProductCard(product = product, onCardClick = onCardClick)
    }
}

// Correct — derivedStateOf avoids recomposition when only source index changes
val showScrollToTop by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 5 }
}
```

---


## Manifest Reference

Every feature that introduces a system capability or Android component must update `app/src/main/AndroidManifest.xml`. The manifest lives in `:app` — the only module that has one.

### Permissions

Declare permissions in the `<manifest>` block, before `<application>`.

| Capability | Permission |
|---|---|
| HTTP / HTTPS network calls (Ktor) | `android.permission.INTERNET` |
| Check network connectivity | `android.permission.ACCESS_NETWORK_STATE` |
| Run a ForegroundService | `android.permission.FOREGROUND_SERVICE` |
| Post notifications (API 33+) | `android.permission.POST_NOTIFICATIONS` |
| Precise location | `android.permission.ACCESS_FINE_LOCATION` |
| Approximate location | `android.permission.ACCESS_COARSE_LOCATION` |
| Camera | `android.permission.CAMERA` |
| Read media images (API 33+) | `android.permission.READ_MEDIA_IMAGES` |
| Read external storage (API ≤ 32) | `android.permission.READ_EXTERNAL_STORAGE` |
| Bluetooth scan (API 31+) | `android.permission.BLUETOOTH_SCAN` |
| Receive boot completed | `android.permission.RECEIVE_BOOT_COMPLETED` |

```xml
<!-- Example — network + foreground service -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

> **Runtime permissions:** Permissions marked as "dangerous" (camera, location, storage, etc.) must also be requested at runtime using `rememberPermissionState()` from Accompanist or the Compose permission APIs. Declaring them in the manifest alone is not sufficient on API 23+.

### Components

Declare components inside the `<application>` block.

#### Activity

```xml
<activity
    android:name=".feature.example.ExampleActivity"
    android:exported="false" />
```

Only the launcher `Activity` should have `android:exported="true"` with a `MAIN` / `LAUNCHER` intent filter.

#### Static BroadcastReceiver

Use a static receiver when the component must respond to system broadcasts even when the app is not running (e.g., `BOOT_COMPLETED`, `ACTION_POWER_CONNECTED`). For in-app events, always prefer a dynamic receiver registered in code.

```xml
<receiver
    android:name=".core.receiver.BootReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

#### Service / ForegroundService

```xml
<service
    android:name=".core.service.SyncService"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

Valid `foregroundServiceType` values: `camera`, `connectedDevice`, `dataSync`, `health`, `location`, `mediaPlayback`, `mediaProjection`, `microphone`, `phoneCall`, `remoteMessaging`, `shortService`, `specialUse`, `systemExempted`.

#### FileProvider

Required when sharing files with other apps via `FileProvider.getUriForFile()`.

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />
</provider>
```

### Dynamic vs. static receivers — decision rule

| Scenario | Registration |
|---|---|
| Must work when app is not running | Static — manifest `<receiver>` |
| Only needed while app is running | Dynamic — `context.registerReceiver()` / `LifecycleEventObserver` |
| Scoped to a ViewModel or screen | Dynamic — unregister in `onCleared()` / `DisposableEffect` |

---

## Development Workflow

- **Feature Branches**: Every feature or fix lives in its own branch with the naming pattern `feat/<feature-name>`, `fix/<issue-id>-description`, or `chore/<task>`.
- **Commit Messages**: Follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`). Messages in English only.
- **Code Review**: All PRs require at least one review and must pass CI (lint, tests, build) before merging.
- **Definition of Done**: A feature is done when it has unit tests, passes `./gradlew lint`, and the Screen composable has a working `@Preview` for both light and dark mode.

**Gradle tasks:**

```bash
./gradlew assembleDebug               # Debug build
./gradlew assembleRelease             # Release build (R8 enabled)
./gradlew test                        # All unit tests
./gradlew connectedAndroidTest        # Instrumented tests on device/emulator
./gradlew lint                        # Android Lint
./gradlew koverHtmlReport             # Code coverage report
./gradlew :app:bundleRelease          # AAB for Play Store
```

---

## Vertical Feature Example — Product Discovery

The following is a complete, minimal implementation of the **Product Discovery** feature slice, illustrating how all layers interact in practice.

### 1. Domain Layer — `core:domain`

```kotlin
// Domain model — no framework dependency
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val imageUrl: String,
)

// Repository port — interface only, no implementation here
interface ProductRepository {
    suspend fun getProducts(page: Int): Result<List<Product>>
}

// UseCase — orchestrates business rules, wraps repository call
class GetProductsUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(page: Int = 1): Result<List<Product>> =
        repository.getProducts(page)
}
```

### 2. Network Layer — `core:network`

```kotlin
// DTO — serialization concern only
@Serializable
data class ProductDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("price") val price: String,
    @SerialName("image_url") val imageUrl: String,
)

@Serializable
data class ProductListResponseDto(
    @SerialName("data") val data: List<ProductDto>,
    @SerialName("total") val total: Int,
)

// API service — wraps Ktor calls and maps every result to NetworkResult
class ProductApiService @Inject constructor(private val client: HttpClient) {

    suspend fun getProducts(page: Int, pageSize: Int = 20): NetworkResult<ProductListResponseDto> =
        runCatching {
            client.get("v1/products") {
                parameter("page", page)
                parameter("pageSize", pageSize)
            }.body<ProductListResponseDto>()
        }.toNetworkResult()
}
```

### 3. Data Layer — `core:data`

```kotlin
// DTO → Domain mapper — keeps serialization concerns out of the domain layer
fun ProductDto.toDomain(): Product = Product(
    id = id,
    name = name,
    description = description,
    price = price,
    imageUrl = imageUrl,
)

// Repository implementation — maps NetworkResult → Result<T> at the data boundary,
// so the domain and presentation layers never see Ktor or DTO types.
class ProductRepositoryImpl @Inject constructor(
    private val apiService: ProductApiService,
) : ProductRepository {

    override suspend fun getProducts(page: Int): Result<List<Product>> =
        when (val result = apiService.getProducts(page)) {
            is NetworkResult.Success ->
                Result.success(result.data.data.map { it.toDomain() })
            is NetworkResult.HttpError ->
                Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError ->
                Result.failure(result.cause)
        }
}
```

### 4. Presentation Layer — `features:product-discovery`

```kotlin
// ViewModel — exposes an immutable UiState, consumes UiEvent, owns the viewModelScope.
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init { onEvent(ProductListUiEvent.Refresh) }

    fun onEvent(event: ProductListUiEvent) {
        when (event) {
            is ProductListUiEvent.Refresh -> loadProducts()
            is ProductListUiEvent.Search -> { /* apply query filter */ }
            is ProductListUiEvent.OpenDetail -> { /* emit a navigation UiEffect */ }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getProductsUseCase()
                .onSuccess { products ->
                    _uiState.update { it.copy(products = products.toImmutableList(), isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
```

```kotlin
// Stateful (Smart) composable — binds the ViewModel and hoists state down.
@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProductListContent(uiState = uiState, onEvent = viewModel::onEvent)
}

// Stateless (Dumb) composable — a pure function of UiState. Trivially previewable and testable.
@Composable
fun ProductListContent(
    uiState: ProductListUiState,
    onEvent: (ProductListUiEvent) -> Unit,
) {
    when {
        uiState.isLoading -> CircularProgressIndicator()
        uiState.errorMessage != null -> Text(uiState.errorMessage)
        else -> LazyColumn {
            items(uiState.products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onCardClick = { onEvent(ProductListUiEvent.OpenDetail(product.id)) },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Product list — light")
@Composable
private fun ProductListContentPreview() {
    AppTheme {
        ProductListContent(
            uiState = ProductListUiState(
                products = persistentListOf(
                    Product("1", "Headphones", "Wireless over-ear", "$99", ""),
                ),
            ),
            onEvent = {},
        )
    }
}
```

This slice exercises every layer and every rule above: the domain stays framework-free, the data layer maps `NetworkResult → Result` at the boundary, and the presentation layer follows the MVI contract with a stateless `*Content` composable and a mandatory `@Preview`.