## 1. Domain layer (`:core:domain`)

- [x] 1.1 Add to `DeviceConstants`: `ACK_MAX_ATTEMPTS = 3`, `ACK_RETRY_DELAY = 2.seconds`, `ACK_DETAIL_MAX_LENGTH = 300`, plus the failure details "Comando no soportado: %s" and "No se pudo reiniciar la app".
- [x] 1.2 Add ports `DeviceCommandRepository { suspend fun ack(commandId, success, detail?): Result<Unit> }`, `CatalogRefreshSignal { fun request(); fun observe(): Flow<Unit> }` and `AppRestarter { fun canRestart(): Boolean; fun restart() }`.
- [x] 1.3 Add `ObserveCatalogRefreshUseCase`.
- [x] 1.4 Add `ProcessDeviceCommandsUseCase`: sequential `collect` of `DeviceHeartbeatRepository.observePendingCommands()`, dispatch per design D1, ack with retries (D2), restart only after the ack attempts, catch execution errors (rethrow `CancellationException`), truncate details.
- [x] 1.5 Unit tests (virtual time): refresh → signal + ack true; restart → ack before restart; restart unavailable → ack false, no restart; unknown → ack false with detail; throwing execution → ack false and next command still processed; ack retried up to 3 attempts 2 s apart; detail truncated to 300; commands processed in order.

## 2. Network layer (`:core:network`)

- [x] 2.1 Add `DeviceCommandAckDto(success, detail?)` and `DeviceCommandApiService.ack(deviceToken, commandId, body)` → `POST /api/v1/devices/me/commands/{commandId}/ack`, bearer DEVICE token, 15 s per-request timeout, `NetworkResult<Unit>`.
- [x] 2.2 `MockEngine` tests: path with the command id, bearer header, body (detail omitted when null), 204 → success, 404 → `HttpError`.

## 3. Data layer (`:core:data`)

- [x] 3.1 Add `command/DeviceCommandRepositoryImpl` (token from `StoreSessionRepository`, fail fast without a token).
- [x] 3.2 Add `command/CatalogRefreshSignalImpl` (`@Singleton`, `MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, DROP_OLDEST)`).
- [x] 3.3 Add `command/AppRestarterImpl` (`@ApplicationContext`): `canRestart()` = launch intent resolves; `restart()` = `Intent.makeRestartActivityTask(component)` + `startActivity` + `Runtime.getRuntime().exit(0)`.
- [x] 3.4 Add `di/DeviceCommandDataModule.kt` binding the three implementations.
- [x] 3.5 Tests: repository (no token → failure without request; success; HTTP error → failure); refresh signal (collector receives a request; a late collector receives nothing).

## 4. Testing support (`:core:testing`)

- [x] 4.1 Add `FakeDeviceCommandRepository` (records acks, scriptable failures), `FakeCatalogRefreshSignal`, `FakeAppRestarter`.

## 5. Catalog in-place refresh (`:features:catalog`)

- [x] 5.1 Inject `ObserveCatalogRefreshUseCase` into `CatalogViewModel`; on each emission run `refreshInPlace()` per design D4 (ignore while loading; `load()` on the error screen; silent fetch otherwise, keeping the selected tab or falling back to Todo; keep products on failure).
- [x] 5.2 Extend `CatalogViewModelTest`: refresh keeps the selected tab; missing tab → Todo; failure keeps products and shows no error; refresh on error screen reloads; no loading flag during a silent refresh.

## 6. App wiring (`:app`)

- [x] 6.1 In `DiscoveryApp.onCreate`, also launch `ProcessDeviceCommandsUseCase` in the `@ApplicationScope` scope.

## 7. Verification

- [x] 7.1 `./gradlew :app:assembleDebug` builds.
- [x] 7.2 `./gradlew testDebugUnitTest :core:domain:test` passes. (305 tests, 0 failures.)
- [x] 7.3 `./gradlew lintDebug` passes with no new warnings.
- [ ] 7.4 Manual check on a paired tablet: "Actualizar catálogo" from staff turns "Listo" within ~1 min and an open catalog shows the change; "Reiniciar" turns "Listo" and the app relaunches on Home.
