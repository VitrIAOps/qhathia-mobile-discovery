## 1. QA baseline

- [x] 1.1 Reproduce X:XVI-040 on the current `profile` APK (`6db980f`, same code as the QA-tested `490694e`) against the QA store `6b89ea64-…`:
  - pair the API 26 AVD;
  - unlink it from staff (or with `POST /devices/{id}/unlink`);
  - record logcat (`401 DEVICE_UNLINKED` every 60 s), `mLockTaskModeState`, and the catalog error.
  
  Save the evidence in `qa-report.md` as "before" (user-assisted if the staff app is needed).
  - Handed to the QA team (2026-11-06): verified with unit tests only; the emulator run is QA's (regression `/qa R01`, X:XVI-040 / XVI-041 / XVI-001).
- [x] 1.2 Create `qa-report.md`, with one row per case (X:XVI-040, X:XVI-041, XVI-001 re-pair) and columns: before, after, evidence, who ran it.

## 2. Problem detail in `core:network` (D1)

- [x] 2.1 Add `dto/ProblemDetailDto.kt`: `ProblemDetailDto` (all fields optional: `errorCode`, `code`, `detail`, `productId`, `productName`, `reason`, `available`, `requested`, `changes`, `fieldErrors`; computed `resolvedErrorCode = errorCode ?: code`) and `PriceChangeDto`.
- [x] 2.2 Add `ProblemDetailParser.kt`: `suspend fun ResponseException.readProblemDetail(): ProblemDetailDto?`, using its own lenient `Json` inside `runCatching`. Empty or invalid body → `null`.
- [x] 2.3 `NetworkResult.HttpError` gains `problem: ProblemDetailDto? = null`; `toNetworkResult()` becomes `suspend` and fills `problem` for a `ResponseException`. Fix any non-suspend caller the compiler reports.
- [x] 2.4 `DevicePairingApiService.exchangeToken` uses `readProblemDetail()?.resolvedErrorCode`; delete the private `readErrorCode` and its `Json`.
- [x] 2.5 Tests (`MockEngine`, `core:network`):
  - a `409 PRICE_CHANGED` body parses `errorCode`, `detail` and `changes`;
  - `code` without `errorCode` resolves;
  - an empty, HTML or malformed body gives `problem = null` with the status kept;
  - the existing pairing exchange tests still pass.

## 3. Unlink detection in the HTTP client (D2)

- [x] 3.1 Add `DeviceUnlinkedSignal` (`@Singleton`, `@Inject constructor`): `events: SharedFlow<String?>` (buffer 8, `DROP_OLDEST`) and `notifyUnlinked(bearerToken: String?)` with `tryEmit`.
- [x] 3.2 `KtorClientFactory.configure(enableLogging, onDeviceUnlinked: (String?) -> Unit = {})`: install `HttpResponseValidator { handleResponseExceptionWithRequest { … } }`. For a `ClientRequestException` with `401` whose `readProblemDetail()?.resolvedErrorCode == "DEVICE_UNLINKED"`, call `onDeviceUnlinked(bearer)`, where `bearer` is the request's `Authorization` header minus `Bearer `. Never rethrow differently or swallow; never log the token. Thread the parameter through both `create(...)` overloads.
- [x] 3.3 `NetworkModule.provideHttpClient(signal: DeviceUnlinkedSignal)` passes `signal::notifyUnlinked`.
- [x] 3.4 Tests (`MockEngine` over `KtorClientFactory.create(engine, onDeviceUnlinked = …)`):
  - `401 DEVICE_UNLINKED` → callback once, with the request's bearer token, **and** the caller's `toNetworkResult()` still yields `HttpError(401, problem.errorCode = DEVICE_UNLINKED)` (the body can be read twice);
  - `403 FORBIDDEN` → no callback;
  - `401` without a body → no callback;
  - `401` with another `errorCode` → no callback;
  - `200` → no callback.

## 4. Domain: port, session revocation, use cases (D3, D4)

- [x] 4.1 `StoreSession` gains `unlinkedNotice: Boolean = false`. `StoreSessionRepository` gains `revokeDeviceSession()` and `dismissUnlinkedNotice()`.
- [x] 4.2 `DeviceSettingsRepository` gains `suspend fun clear()`.
- [x] 4.3 New port `DeviceUnlinkEvents { fun observe(): Flow<String?> }` (`core:domain/repository`).
- [x] 4.4 New `HandleDeviceUnlinkedUseCase`. It collects events sequentially; for each event it reads the stored session and skips when it is not paired or when the event token is non-null and differs from the stored token. Otherwise it runs, in order:
  1. `EndCustomerSessionUseCase()`;
  2. `deviceSettingsRepository.clear()`;
  3. `heldPairingCodeRepository.set(null)`;
  4. `kioskSessionRepository.setExitedFromSettings(false)`;
  5. `storeSessionRepository.revokeDeviceSession()`.
  
  A failure in one step must not stop the next. The session revoke runs last and always.
- [x] 4.5 New `DismissUnlinkedNoticeUseCase`.
- [x] 4.6 Tests (JUnit 5, fakes):
  - an event while paired clears the cart, settings, held code and kiosk flag, then revokes;
  - an event while unpaired does nothing;
  - two events → one revocation;
  - an event with a different token does nothing;
  - an event with a `null` token revokes;
  - a failing settings clear still revokes.

## 5. Data: implementations and bindings

- [x] 5.1 `StoreSessionLocalDataSource`:
  - `revokeDeviceSession()`: one `edit` that removes the token, device name, pairing code and store name, and sets `unlinked_notice = true` (new `booleanPreferencesKey`);
  - `dismissUnlinkedNotice()`;
  - map `unlinkedNotice` in `toStoreSession()`.
  
  Wire both methods in `StoreSessionRepositoryImpl`.
- [x] 5.2 `DeviceSettingsLocalDataSource.clear()` removes every `settings_*` key it owns; wire it in `DeviceSettingsRepositoryImpl.clear()`.
- [x] 5.3 `DeviceUnlinkEventsImpl` over `DeviceUnlinkedSignal.events`, bound in a new `di/DeviceUnlinkDataModule` (`@Binds`, `SingletonComponent`).
- [x] 5.4 Remove the 401 retry in `ProductCatalogRepositoryImpl`, `ProductDetailRepositoryImpl` (product and suggestions), `ProductSearchRepositoryImpl`, `OrderRepositoryImpl` and `CartShareRepositoryImpl`:
  - a `401` maps like any other `HttpError`, with the same category each repository uses today for a non-retried HTTP error;
  - delete the `HTTP_UNAUTHORIZED` constants that become unused;
  - delete `SessionTokenProvider.clear()` and `forceRefresh`, and update its KDoc.
- [x] 5.5 Update the fakes in `core:testing`:
  - `FakeStoreSessionRepository` (`revokeDeviceSession`, `dismissUnlinkedNotice`, `unlinkedNotice`);
  - `FakeDeviceSettingsRepository.clear()`;
  - new `FakeDeviceUnlinkEvents`.
- [x] 5.6 Tests:
  - `StoreSessionLocalDataSource` (revoke clears the four keys and sets the notice; dismiss clears only the notice; default is no notice);
  - `DeviceSettingsLocalDataSource.clear()`;
  - each of the 5 repositories: a `401` sends **one** request and returns its failure category (update the existing tests that expected a retry).

## 6. App wiring (D5)

- [x] 6.1 `DiscoveryApp`: `@Inject lateinit var handleDeviceUnlinked: Lazy<HandleDeviceUnlinkedUseCase>` and `applicationScope.launch { handleDeviceUnlinked.get()() }`, in the main process only, next to the heartbeat. Do not touch `RestartActivity` or its manifest entry.
- [x] 6.2 Confirm that nothing else needs to change for the reaction: `MainViewModel.startDestination` → Welcome, `ObserveKioskLockUseCase` → `stopLockTask()`, heartbeat stop. Add a `MainViewModelTest` case: the session goes from paired to unpaired → `startDestination` is Onboarding.

## 7. Welcome notice (D6) in `features:onboarding`

- [x] 7.1 `features/onboarding/build.gradle.kts`, mirroring `features/home`:
  - add the `ksp` and `hilt` plugins;
  - add `core:domain`, `hilt.android`, `hilt.navigation.compose`, `lifecycle.viewmodel`, `ksp(hilt.compiler)` and `ksp(kotlin.metadata.jvm)`;
  - add the JUnit 5 / mockk / turbine / coroutines-test / `core:testing` test dependencies.
- [x] 7.2 `model/` contract: `WelcomeUiState(showUnlinkedNotice)`, `WelcomeUiEvent.Continue`, `WelcomeUiEffect.NavigateToInitialConfiguration`. `WelcomeViewModel` (`@HiltViewModel`) observes `ObserveStoreSessionUseCase`; on `Continue`, it calls `DismissUnlinkedNoticeUseCase` and then emits the effect.
- [x] 7.3 `WelcomeScreen(onContinue)` uses `hiltViewModel()`, collects the state with `collectAsStateWithLifecycle()`, and forwards the effect to `onContinue`. `WelcomeContent(showUnlinkedNotice, onContinue)` draws the notice banner above the title: tonal warning surface from `core:ui` tokens, `liveRegion = Polite`, centered, max width 480 dp like the column. `OnboardingNavigation` keeps the same signature.
- [x] 7.4 String `onboarding_unlinked_notice` = "Esta tablet se desvinculó de la tienda. Para volver a usarla, vincúlala de nuevo desde la app del comercio." in `features/onboarding/src/main/res/values/strings.xml`.
- [x] 7.5 Previews: light and dark × with and without the notice.
- [x] 7.6 `WelcomeViewModelTest`:
  - a pending notice → `showUnlinkedNotice = true`;
  - no notice → `false`;
  - `Continue` dismisses, then navigates, in that order.

## 8. Build and verify

- [x] 8.1 Run `testDebugUnitTest` for `:core:network`, `:core:domain`, `:core:data`, `:features:onboarding` and `:app`, one module at a time, with JDK 21 (Android Studio JBR).
- [x] 8.2 Build the `profile` APK and install it on the API 26 AVD (paired).
- [x] 8.3 X:XVI-040: unlink from staff. Within ≤ 60 s:
  - Welcome is shown with the notice;
  - `dumpsys activity activities | grep mLockTaskModeState` → `NONE`;
  - no more `POST /devices/heartbeat` in logcat;
  - logcat shows no token;
  - "Continuar" generates a new code.
- [x] 8.4 X:XVI-041: commands to the old device id still give 404 (backend), and the tablet does not operate for the store.
- [x] 8.5 Re-pair (XVI-001): Home shows the new store's name, never the previous one; Settings does not ask for the old PIN before the new settings arrive; Welcome shows no notice on the next visit.
- [x] 8.6 Regression smoke: catalog, search, detail, order and share still work while paired; a `403` path (if reachable) does not unpair.
- [x] 8.7 Fill in `qa-report.md` "after" with the evidence paths, and check every scenario in the four delta specs against a test or a QA case.

  - Handed to the QA team (2026-11-06): verified with unit tests only; the emulator run is QA's (regression `/qa R01`, X:XVI-040 / XVI-041 / XVI-001).