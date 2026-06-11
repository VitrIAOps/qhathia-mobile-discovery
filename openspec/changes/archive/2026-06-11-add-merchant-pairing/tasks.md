## 1. Build setup (Ktor WebSockets)

- [x] 1.1 Add `ktor-client-websockets` (group `io.ktor`, `version.ref = "ktor"`) to `[libraries]` in `gradle/libs.versions.toml`
- [x] 1.2 Add `implementation(libs.ktor.client.websockets)` to `core/network/build.gradle.kts`
- [x] 1.3 Install the `WebSockets` plugin on the CIO `HttpClient` in `KtorClientFactory` (no base-URL change — services keep using absolute URLs)

## 2. core:network — pairing API service, socket client & DTOs

- [x] 2.1 Add `PairingTokenResponseDto` (`pairingToken`, `sessionId`, `expiresAt`) in `core/network/.../dto/DevicePairingDtos.kt`
- [x] 2.2 Add `DeviceTokenResponseDto` (`deviceId`, `deviceAccessToken`, `storeId`, `deviceName`, `platform`, `status`) to the same DTO file
- [x] 2.3 Add `DeviceInfoResponseDto` (`deviceId`, `storeId`, `deviceName`, `platform`, `appVersion`, `status`, `pairedAt`, `lastSeenAt`, `lastSyncAt`) to the same DTO file
- [x] 2.4 Add `PairingStatusMessageDto` (`status`, `sessionId`) for the WebSocket payload
- [x] 2.5 Create `DevicePairingApiService` with `generatePairingToken()` (POST, public), `exchangeToken(sessionId)` (GET, public), and `getPairedDevice(deviceToken)` (GET, Bearer) — each `runCatching { … }.toNetworkResult()` against `BuildConfig.QHATHIA_BASE_URL`, mirroring `AuthApiService`
- [x] 2.6 Create `PairingSocketClient` exposing `observe(sessionId): Flow<PairingStatusMessageDto>` over the raw WSS endpoint, emitting each status message and completing on the terminal message/socket close

## 3. core:domain — models, port & use cases

- [x] 3.1 Add domain models `PairingCode` (code + expiresAt + remaining-time helper), `PairingStatus` (sealed/enum: Waiting, Approved, Rejected, Expired), and `PairedDevice` in `core/domain/.../model/`
- [x] 3.2 Define `DevicePairingRepository` port: `generateCode(): Result<PairingCode>` returning code + sessionId, `observeStatus(sessionId): Flow<PairingStatus>`, `exchangeForDeviceToken(sessionId): Result<PairedDevice>`, `getPairedDevice(): Result<PairedDevice>`, `unpair(): Result<Unit>`
- [x] 3.3 Add `PairingConstants` (code length 5, TTL 5 min) in `core/domain`
- [x] 3.4 Add use cases: `GeneratePairingCodeUseCase`, `ObservePairingStatusUseCase`, `ExchangeDeviceTokenUseCase`, `GetPairedDeviceUseCase`, `UnpairDeviceUseCase`

## 4. core:data — repository impl, persistence & auth seam

- [x] 4.1 Extend `StoreSession` with `deviceAccessToken: String?` and an `isPaired` derived flag; update `StoreSessionLocalDataSource` with a `device_access_token` key (+ minimal device identity) and read/write/clear methods
- [x] 4.2 Extend `StoreSessionRepository` (port) + impl with persist/clear of the device token and expose it via `observe()`
- [x] 4.3 Create `DevicePairingRepositoryImpl` mapping `NetworkResult → Result`, persisting the device token on successful exchange, and stubbing `unpair()` with a documented TODO (no API)
- [x] 4.4 Add a DTO→domain mapper file for pairing (`PairingMappers.kt`)
- [x] 4.5 Change `SessionTokenProvider.accessToken()` to return the persisted device token when present, falling back to dev-credential login only when absent
- [x] 4.6 Add a Hilt `@Module` (`DevicePairingDataModule`) binding `DevicePairingRepository` to its impl

## 5. features:settings — pairing presentation (MVI)

> UI layout, tokens and copy for both screens are captured in `figma-reference.md` (+ screenshots in `assets/`). Map to `DiscoveryColors`/`DiscoveryTheme` — do not hardcode hex.

- [x] 5.1 Add `PairingUiState` (section selection, code, remaining seconds, phase: Loading/WaitingForApproval/Rejected/Expired/Paired/Error, paired-device details with pending fields), `PairingUiEvent`, `PairingUiEffect` under `feature/settings/model`
- [x] 5.2 Add `PairingViewModel`: generate code on entering the section, run the `expiresAt`-driven countdown, collect `observeStatus`, exchange token on `APPROVED`, load paired-device details, and handle unpair confirmation; cancel work in `onCleared`
- [x] 5.3 Implement section switching in the settings side menu (Wi-Fi ↔ Vincular) with an animated selection shadow; make the existing "Vincular" item functional
- [x] 5.4 Add `PairingContent` (stateless) rendering the "Código generado" state (code + countdown) per Figma node `1055-14805`
- [x] 5.5 Add the "Comercio vinculado" paired-device view per Figma node `1080-1587` (device name, platform/model, sync time, code used, merchant name as pending)
- [x] 5.6 Add the unpair button + confirmation dialog (stateless content + dialog), surfacing the "server unpair not yet available" message on confirm
- [x] 5.7 Wire the pairing section into `SettingsNavigation` / settings screen host and add string + drawable resources
- [x] 5.8 Add `@Preview` (light + dark) for the code-generated, waiting, paired, and error states

## 6. Tests & Definition of Done

- [x] 6.1 `PairingViewModelTest` (Turbine + MainDispatcherRule): code generation on entry, countdown→expiry regenerates, APPROVED→token exchange→paired, REJECTED/EXPIRED states, unpair confirmation flow
- [x] 6.2 `DevicePairingRepositoryImplTest` with a fake API service/socket: success + HttpError + NetworkError mapping, token persistence on exchange, stubbed unpair behavior
- [x] 6.3 `SessionTokenProvider` test: device token preferred when present, dev-login fallback when absent
- [x] 6.4 Add fakes/fixtures in `core:testing` as needed (FakeDevicePairingRepository)
- [ ] 6.5 Run `./gradlew test` and `./gradlew lint`; confirm pairing screens have working light + dark `@Preview`
