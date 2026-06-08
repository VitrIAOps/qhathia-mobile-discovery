## 1. Create the `features:settings` module

- [x] 1.1 Create the module directory `features/settings/` with `src/main/java/com/qhathia/mobile/discovery/feature/settings/` and a `src/test/java/...` mirror.
- [x] 1.2 Add `features/settings/build.gradle.kts` using the Feature module template (android-library + kotlin-compose + ksp + hilt; depend on `:core:domain`, `:core:ui`, `:core:common`; Compose BOM, lifecycle, navigation-compose, hilt + hilt-navigation-compose, `ksp(libs.hilt.compiler)` + `ksp(libs.kotlin.metadata.jvm)`, immutable collections; JUnit5/MockK/Turbine/coroutines-test for tests; `debugImplementation(libs.compose.ui.tooling)`).
- [x] 1.3 Register `include(":features:settings")` in the root `settings.gradle.kts`.
- [x] 1.4 Create `features/settings/src/main/AndroidManifest.xml` (minimal `<manifest />`).
- [x] 1.5 Confirm the module compiles in isolation (`./gradlew :features:settings:assembleDebug`).

## 2. Domain layer (`core:domain`)

- [x] 2.1 Add `model/WifiNetwork.kt` (`ssid`, `isSecured`, `signalLevel`, `isConnected`).
- [x] 2.2 Add `model/NetworkConnectivityState.kt` (`wifiEnabled`, `mobileDataEnabled`, `airplaneModeActive`, `connectedSsid`) with an `EMPTY` default.
- [x] 2.3 Add `SettingsConstants.kt` with `MOCK_SETTINGS_PASSWORD = "1234"` and the expected password length.
- [x] 2.4 Add `repository/SettingsAccessRepository.kt` (`suspend fun validatePassword(input: String): Result<Boolean>`).
- [x] 2.5 Add `repository/NetworkSettingsRepository.kt` (`observeConnectivity(): Flow<NetworkConnectivityState>`, `observeWifiNetworks(): Flow<List<WifiNetwork>>`, `suspend fun setWifiEnabled(enabled)`, `suspend fun setMobileDataEnabled(enabled)`, `suspend fun connect(ssid, password: String?): Result<Unit>`).
- [x] 2.6 Add use cases: `ValidateSettingsPasswordUseCase`, `ObserveNetworkStateUseCase`, `ObserveWifiNetworksUseCase`, `SetWifiEnabledUseCase`, `SetMobileDataEnabledUseCase`, `ConnectToWifiUseCase`.
- [x] 2.7 Unit-test `ValidateSettingsPasswordUseCase` (accepts `1234`, rejects others) and any use-case business logic.

## 3. Data layer (`core:data`)

- [x] 3.1 Add `settings/MockSettingsPasswordDataSource.kt` validating against `SettingsConstants.MOCK_SETTINGS_PASSWORD`.
- [x] 3.2 Add `settings/SettingsAccessRepositoryImpl.kt` delegating to the mock data source.
- [x] 3.3 Add `network/WifiDataSource.kt` and `network/ConnectivityDataSource.kt` injecting `@ApplicationContext` and wrapping `WifiManager` (scan results, suggestion/panel-based connect) and `ConnectivityManager` + `Settings.Global.AIRPLANE_MODE_ON` (connectivity/airplane state via `callbackFlow`).
- [x] 3.4 Add `network/NetworkSettingsRepositoryImpl.kt` mapping data sources → domain models; for OS-restricted actions (API 29+ Wi-Fi/data toggle, silent connect) use the supported API path or return an explicit `Result.failure`/state per design §4.
- [x] 3.5 Add `di/SettingsDataModule.kt` and `di/NetworkSettingsDataModule.kt` (`@Binds` the two repositories into `SingletonComponent`).
- [x] 3.6 Unit-test `SettingsAccessRepositoryImpl` and the mapping logic in `NetworkSettingsRepositoryImpl` (using fakes for the Android data sources).

## 4. Settings-access UI (`features:settings`)

- [x] 4.1 Add `model/SettingsUiState.kt`, `model/SettingsUiEvent.kt`, `model/SettingsUiEffect.kt` (PIN input, validation error; events: input changed, submit, dismiss, reset; effects: unlocked, navigate back).
- [x] 4.2 Add `ui/SettingsViewModel.kt` (`@HiltViewModel`) wiring `ValidateSettingsPasswordUseCase`; auto-validate on a complete PIN and emit `Unlocked`; expose `StateFlow` state + `SharedFlow` effects.
- [x] 4.3 Add `ui/SettingsPasswordDialog.kt` + `ui/SettingsPasswordDialogContent.kt` following the Figma "Configuración protegida" PIN pad (secure badge, four PIN dots, numeric keypad with backspace; auto-validate on completion).
- [x] 4.4 Add `ui/SettingsPasswordGate.kt` (stateful, app-hosted overlay) shown before navigating into settings; a correct PIN opens the Network settings screen, dismissal returns to Home. (No separate options/side-menu shell — the Network settings screen is the landing.)
- [x] 4.5 Add light + dark `@Preview` for `SettingsPasswordDialogContent` (empty / typing / error).

## 5. Network-settings UI (`features:settings`)

- [x] 5.1 Add `model/NetworkSettingsUiState.kt`, `model/NetworkSettingsUiEvent.kt`, `model/NetworkSettingsUiEffect.kt` (wifi/mobile-data states, airplane-mode flag, network list, selected secured network, connection progress/result).
- [x] 5.2 Add `ui/NetworkSettingsViewModel.kt` (`@HiltViewModel`) wiring the observe/toggle/connect use cases; derive the airplane-mode guard that disables selection.
- [x] 5.3 Add `ui/NetworkSettingsScreen.kt` (stateful) + `ui/NetworkSettingsContent.kt` (stateless) per Figma "Ajustes de red": mobile-data switch above the Wi-Fi toggle, then the accessible-network list (name + secured/open + connected marker).
- [x] 5.4 Add `ui/WifiPasswordDialog.kt` + `ui/WifiPasswordDialogContent.kt`: top-right close "X", ~56dp Wi-Fi icon, network name, password `TextField`, submit button (shown only for secured networks).
- [x] 5.5 Add design-system processing/loading + "airplane mode active" and "not permitted/location off" states.
- [x] 5.6 Add light + dark `@Preview` for `NetworkSettingsContent` and `WifiPasswordDialogContent`.

## 6. Navigation & app wiring (`features:settings` + `:app`)

- [x] 6.1 Add `navigation/SettingsNavigation.kt` with `NETWORK_SETTINGS_ROUTE` and a `NavGraphBuilder.networkSettingsScreen(...)` extension hoisting navigation callbacks (the PIN gate is an app-level overlay, not a destination).
- [x] 6.2 In `:app` `MainActivity`, remove the `SETTINGS_ROUTE` placeholder; wire Home's existing `onNavigateToSettings` to show the PIN gate overlay, and on a correct PIN navigate to the Network settings destination.
- [x] 6.3 Add `:features:settings` as a dependency of `:app` in `app/build.gradle.kts`.

## 7. Manifest & permissions (`:app`)

- [x] 7.1 Add `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, and `ACCESS_FINE_LOCATION` to `app/src/main/AndroidManifest.xml` (`ACCESS_NETWORK_STATE` already present).
- [x] 7.2 Request `ACCESS_FINE_LOCATION` at runtime when entering the Network settings screen (mirror `MainActivity`'s `RECORD_AUDIO` request); handle the denied / location-disabled state in the UI.

## 8. Tests & Definition of Done

- [x] 8.1 Add `SettingsViewModelTest` (correct PIN emits `Unlocked`; wrong PIN clears the entry and shows an error; non-digits ignored; reset blanks the gate) using fakes from `core:testing`.
- [x] 8.2 Add `NetworkSettingsViewModelTest` (toggle reflects state; airplane mode blocks selection; secured selection opens password dialog; connect outcome handling) with Turbine.
- [x] 8.3 Run `./gradlew test` and `./gradlew lint`; confirm both screens have working light + dark `@Preview`s. Validate against the OpenSpec Definition of Done.
