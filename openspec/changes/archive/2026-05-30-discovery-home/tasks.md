## 1. Build infrastructure (version catalog & plugins)

- [x] 1.1 Extend `gradle/libs.versions.toml` with versions for `ksp`, `hilt`, `hilt-androidx`, `navigation`, `lifecycle`, `datastore`, `coroutines`, `immutable-collections`, `junit5`, `mockk`, `turbine` (aligned to the canonical catalog: Kotlin 2.3.20 / KSP 2.3.6 / standalone format)
- [x] 1.2 Add the `[libraries]` entries from the standards' canonical catalog needed here: hilt-android, hilt-compiler, hilt-navigation-compose, kotlin-metadata-jvm, navigation-compose, lifecycle-viewmodel, lifecycle-runtime, datastore-preferences, kotlinx-coroutines(-core), kotlinx-immutable, junit5-api/engine/launcher, mockk, turbine, coroutines-test, compose-ui-tooling-preview, material-icons-core, javax-inject
- [x] 1.3 Add the `[plugins]` entries `ksp` and `hilt` (+ `android-library`, `kotlin-jvm`); root `build.gradle.kts` declares all with `apply false` to resolve versions once
- [x] 1.4 Add the AGP 9 / Hilt compatibility flags to `gradle.properties` (`android.newDsl=false`, `android.disallowKotlinSourceSets=false`, `android.useAndroidX=true`)
- [x] 1.5 Confirm Gradle resolves the catalog/module graph with no errors (`gradlew projects` succeeds)

## 2. core:common module

- [x] 2.1 Create directory `core/common/src/main/java/com/qhathia/mobile/discovery/core/common` (and matching `src/test/...`)
- [x] 2.2 Write `core/common/build.gradle.kts` using the Android Library template (no Hilt)
- [x] 2.3 Register `include(":core:common")` in `settings.gradle.kts`
- [x] 2.4 Create `core/common/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 2.5 Add `DispatcherProvider` (interface + `DefaultDispatcherProvider`)

## 3. core:domain module (store-session ports)

- [x] 3.1 Create directory `core/domain/src/main/java/com/qhathia/mobile/discovery/core/domain` (and `src/test/...`)
- [x] 3.2 Write `core/domain/build.gradle.kts` using the Pure Kotlin template (`kotlin-jvm` alias, jvmTarget 11)
- [x] 3.3 Register `include(":core:domain")` in `settings.gradle.kts`
- [x] 3.4 Add `StoreSession` domain model (`storeName: String?`) with `EMPTY`
- [x] 3.5 Add `StoreSessionRepository` interface (`observe(): Flow<StoreSession>` + `updateStoreName`)
- [x] 3.6 Add `ObserveStoreSessionUseCase` and `UpdateStoreNameUseCase`
- [x] 3.7 Define the mic timeout constant (`HomeConstants.MIC_LISTENING_TIMEOUT_SECONDS = 30`)

## 4. core:data module (store-session implementation)

- [x] 4.1 Create directory `core/data/src/main/java/com/qhathia/mobile/discovery/core/data` (and `src/test/...`)
- [x] 4.2 Write `core/data/build.gradle.kts` (Android Library + Hilt: `ksp(hilt.compiler)` + `ksp(kotlin.metadata.jvm)`, datastore, coroutines) depending on `:core:domain`, `:core:common`
- [x] 4.3 Register `include(":core:data")` in `settings.gradle.kts`
- [x] 4.4 Create `core/data/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 4.5 Implement `StoreSessionLocalDataSource` backed by a Preferences DataStore (`store_session`)
- [x] 4.6 Implement `StoreSessionRepositoryImpl` and `StoreSessionDataModule` (`@Binds` repo, `@Provides` DataStore)

## 5. core:ui module (design system)

- [x] 5.1 Create directory `core/ui/src/main/java/com/qhathia/mobile/discovery/core/ui` (theme, tokens, component subpackages)
- [x] 5.2 Write `core/ui/build.gradle.kts` (Android Library + Compose; material3, material-icons-core, ui-tooling-preview; debug ui-tooling)
- [x] 5.3 Register `include(":core:ui")` in `settings.gradle.kts`
- [x] 5.4 Create `core/ui/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 5.5 Move the theme into `core:ui` as `DiscoveryTheme` (light + dark colorScheme, typography, shapes) — brand tokens structured as placeholders pending the Figma export
- [x] 5.6 Add design tokens: `Spacing` (xs 4 / sm 8 / md 16 / lg 24 / xl 32) and `DiscoveryShapes`
- [x] 5.7 Build shared composables `DiscoverySearchBar`, `MicButton`, `IconActionButton` (stateless, `modifier` param, content descriptions, ≥48dp targets) with light + dark `@Preview`s

## 6. core:testing module

- [x] 6.1 Create directory `core/testing/src/main/java/com/qhathia/mobile/discovery/core/testing`
- [x] 6.2 Write `core/testing/build.gradle.kts` (Android Library; exposes junit5, mockk, turbine, coroutines-test as `api`)
- [x] 6.3 Register `include(":core:testing")` in `settings.gradle.kts`
- [x] 6.4 Create `core/testing/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 6.5 Add `MainDispatcherExtension` (JUnit5), `FakeStoreSessionRepository`, and `StoreSessionFixtures`

## 7. features:home module

- [x] 7.1 Create directory `features/home/src/main/java/com/qhathia/mobile/discovery/feature/home` (`ui`, `model`, `navigation` subpackages) and `src/test/...`
- [x] 7.2 Write `features/home/build.gradle.kts` (Feature template; depends on `:core:domain`, `:core:ui`, `:core:common`; `ksp(kotlin.metadata.jvm)` + `ksp(hilt.compiler)`)
- [x] 7.3 Register `include(":features:home")` in `settings.gradle.kts`
- [x] 7.4 Create `features/home/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 7.5 Add MVI contract: `HomeUiState` (`storeName`, `MicState`, derived `areControlsVisible`), `HomeUiEvent`, `HomeUiEffect`
- [x] 7.6 Implement `HomeViewModel` (`@HiltViewModel`): observe store session; 30s `viewModelScope` countdown; no-audio timeout → `NoAudioRetry` rotating prompts; navigation effects
- [x] 7.7 Externalize retry-prompt strings (string-array) and content descriptions as string resources
- [x] 7.8 Implement `HomeContent` (stateless: store name, search bar, mic/catalog/settings, entrance + `AnimatedVisibility`; controls visible only when `MicState == Idle`) and `HomeScreen` (stateful)
- [x] 7.9 Add light + dark `@Preview`s for `HomeContent` (idle, listening, retry states)
- [x] 7.10 Add `homeScreen` NavGraph builder + `HOME_ROUTE`, hoisting catalog/results/settings callbacks

## 8. Wire the app shell (:app)

- [x] 8.1 Add Hilt + module dependencies to `app/build.gradle.kts` (`ksp` + `hilt` plugins, `ksp(hilt.compiler)` + `ksp(kotlin.metadata.jvm)`, `:features:home`, `:core:ui`, `:core:data`, navigation)
- [x] 8.2 Create `@HiltAndroidApp class DiscoveryApp : Application()` and reference via `android:name`
- [x] 8.3 Annotate `MainActivity` with `@AndroidEntryPoint`; replace the `Greeting` scaffold with a root `NavHost` in `DiscoveryTheme`
- [x] 8.4 Declare the Home start destination + placeholder routes for catalog / results / settings; connect Home's hoisted callbacks
- [x] 8.5 Delete the now-unused `app/.../ui/theme/*` and `Greeting`

## 9. Manifest audit

- [x] 9.1 Confirmed no new runtime permissions required (no network call yet, mic is UI-only); only the Application `android:name` was added
- [x] 9.2 `INTERNET` / `ACCESS_NETWORK_STATE` / `RECORD_AUDIO` left out until the deferred network/STT work introduces them

## 10. Tests & Definition of Done

- [x] 10.1 Unit-tested `HomeViewModel` with `MainDispatcherExtension` + Turbine + `FakeStoreSessionRepository`: store name surfaces; mic hides controls; 30s timeout → retry prompt; consecutive timeouts vary the prompt; cancel restores; navigation effects emitted
- [x] 10.2 Unit-tested `StoreSessionRepositoryImpl` (empty session, update persists, observe emits current then update) against a real DataStore on a temp file
- [x] 10.3 `./gradlew lintDebug` passes (BUILD SUCCESSFUL, no errors)
- [x] 10.4 `./gradlew testDebugUnitTest` green
- [x] 10.5 Light + dark previews compile and are present for `HomeContent` and the `core:ui` components (rendered preview is a visual check in Android Studio)
- [x] 10.6 Validated against the Figma node `286-9269` via `get_design_context` + `get_variable_defs`: exact color tokens (`blue-600 #163550`, `blue-800 #0d1f38`, `neutral-50 #f8f8f8`, `neutral-500 #757575`, catalog `#0a2540`, border `#dbdbdb`, footer `#878787`), Urbanist type family (bundled), the orange-gradient mic with glow, "TIENDA" + "Ir al catálogo" top bar, settings gear, "powered by Qhathia" footer, and the idle/listening/retry copy — all rebuilt to match. (Final pixel check is a visual pass in Android Studio.)
