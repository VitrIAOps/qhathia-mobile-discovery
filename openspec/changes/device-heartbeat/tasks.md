## 1. Domain layer (`:core:domain`)

- [x] 1.1 Add `DeviceConstants` with `HEARTBEAT_INTERVAL = 60.seconds` and `MODEL_MAX_LENGTH = 100`.
- [x] 1.2 Add models: `DeviceTelemetry(appVersion, batteryLevel?, networkStatus?, model?, uptimeSeconds?)`, `DeviceNetworkStatus { WIFI, MOBILE, OFFLINE }`, `DeviceCommand(id, type)`, `DeviceCommandType` (`RefreshCatalog`, `RestartApp`, `Unknown(raw)`), `HeartbeatResult(serverTime?, catalogVersion?, settingsVersion?, pendingCommands)`.
- [x] 1.3 Add ports `DeviceTelemetryProvider { suspend fun current(): DeviceTelemetry }` and `DeviceHeartbeatRepository { suspend fun sendHeartbeat(telemetry): Result<HeartbeatResult>; fun observeHeartbeats(): Flow<HeartbeatResult>; fun observePendingCommands(): Flow<DeviceCommand> }`.
- [x] 1.4 Add `SendHeartbeatUseCase` (telemetry → repository) and `ObserveHeartbeatUseCase`.
- [x] 1.5 Add `RunDeviceHeartbeatUseCase`: `StoreSessionRepository.observe().map { isPaired }.distinctUntilChanged().collectLatest { paired → send now, then every interval }`, swallowing failures and rethrowing `CancellationException` (design D2).
- [x] 1.6 Unit tests with `runTest` virtual time: immediate send on paired start; one send per 60 s; no sends while unpaired; sends start right after pairing; loop stops when the session is cleared; a failure does not stop the next tick.

## 2. Network layer (`:core:network`)

- [x] 2.1 Add `dto/DeviceHeartbeatDtos.kt`: `HeartbeatRequestDto` (`appVersion` required; `batteryLevel`, `networkStatus`, `model`, `uptimeSeconds` nullable with `null` defaults), `HeartbeatResponseDto` (`received`, `serverTime?`, `catalogVersion?`, `settingsVersion?`, `pendingCommands = emptyList()`), `PendingCommandDto(id, command)`.
- [x] 2.2 Add `DeviceHeartbeatApiService.sendHeartbeat(deviceToken, body)` → `POST /api/v1/devices/heartbeat` with `bearerAuth`, JSON body, per-request `timeout { requestTimeoutMillis = 15_000 }`, folded into `NetworkResult` (mirror `DevicePairingApiService`).
- [x] 2.3 Tests (Ktor `MockEngine`): request path, bearer header and body (null fields omitted); full response parsing; minimal response (`received` + `serverTime`) parsing; HTTP 401/500 → `NetworkResult.HttpError`. (Added test-only `ktor-client-mock`; `KtorClientFactory.create(engine)` reuses the production config over a mock engine.)

## 3. Data layer (`:core:data`)

- [x] 3.1 Add `heartbeat/DeviceHeartbeatMappers.kt`: `DeviceTelemetry → HeartbeatRequestDto` and `HeartbeatResponseDto → HeartbeatResult` (unknown command strings → `DeviceCommandType.Unknown`).
- [x] 3.2 Add `heartbeat/DeviceHeartbeatRepositoryImpl` (`@Singleton`): reads the token from `StoreSessionRepository` (fail fast when absent), calls the service, emits successes into a `MutableSharedFlow(replay = 1, DROP_OLDEST)` and each pending command into a bounded `Channel(16, DROP_OLDEST)` exposed with `receiveAsFlow()` (design D5).
- [x] 3.3 Add `heartbeat/DeviceTelemetryProviderImpl` (`@ApplicationContext`): version name via `PackageManager`, battery via `BatteryManager.BATTERY_PROPERTY_CAPACITY` (drop outside 0–100), network from `ConnectivityDataSource.observe().first()` (`WIFI`/`MOBILE`/`OFFLINE`), model from `Build.MANUFACTURER` + `Build.MODEL` (de-duplicated, capitalized, ≤100), uptime from `Process.getStartElapsedRealtime()`; each field in its own `runCatching`. (Added `ConnectivitySnapshot.wifiActive` from the transport: the SSID is `null` without the location permission, so it cannot signal "on Wi-Fi" by itself.)
- [x] 3.4 Add `di/DeviceHeartbeatDataModule.kt` binding both implementations.
- [x] 3.5 Tests: mappers (incl. unknown command, missing optionals); repository (no token → failure without a request; success publishes to the heartbeat flow; late subscriber gets the latest result; a command is received once and not replayed to a second collector; HTTP error → failure, nothing published); telemetry model formatting/truncation and network-status mapping as pure functions.

## 4. Testing support (`:core:testing`)

- [x] 4.1 Add `FakeDeviceHeartbeatRepository` (records sent telemetry, scriptable results) and `FakeDeviceTelemetryProvider`.
- [x] 4.2 Add `DeviceHeartbeatFixtures` (telemetry, response DTO / `HeartbeatResult` with and without commands).

## 5. App wiring (`:app`)

- [x] 5.1 Add `di/AppCoroutineModule.kt` providing an `@ApplicationScope` `CoroutineScope(SupervisorJob() + Dispatchers.Default)` (qualifier declared in `:app`).
- [x] 5.2 In `DiscoveryApp.onCreate`, inject the scope and `RunDeviceHeartbeatUseCase` and launch it once. No other logic in the app shell.

## 6. Verification

- [x] 6.1 `./gradlew :app:assembleDebug` builds (JDK 21 / Android Studio JBR).
- [x] 6.2 `./gradlew testDebugUnitTest` passes for `:core:domain`, `:core:network`, `:core:data` and `:app`. (283 tests across the project, 0 failures.)
- [x] 6.3 `./gradlew lintDebug` passes with no new warnings. (No lint issue in any new/changed file.)
- [ ] 6.4 Manual check on a paired tablet/emulator: the staff "Dispositivos" card switches to "Conectada" within a minute and shows battery, network, app version and model; with the network off the app keeps working with no error UI and the card recovers once the network is back.
- [ ] 6.5 Confirm the `networkStatus` vocabulary (`WIFI`/`MOBILE`/`OFFLINE`) against the staff app's mapping and adjust the mapper if needed (design Open Questions).
