# QA report — device-unlinked-session

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run.

| | |
|---|---|
| App (before) | `profile` on `6db980f` (same app code as the QA-tested `490694e`) |
| App (after) | `profile` with this change |
| Backend | production `http://87.99.156.164` (phases 35–43) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a` ("QA Discovery — Ferretería"), never El Martillo |
| Device | AVD `Tablet_API36` paired to the QA store (`Medium_Tablet` is paired to El Martillo and must not be unlinked) |

| Case | What is checked | Before | After | Evidence | Who |
|---|---|---|---|---|---|
| X:XVI-040 | Unlink from staff → ≤ 60 s: Welcome with notice, `mLockTaskModeState=NONE`, no more heartbeats, no token | ✖ (QA 03/11: 401 `DEVICE_UNLINKED` every 60 s for 144 s, still pinned on Home) | ⏸ | | QA team |
| X:XVI-041 | Commands to the old device id → 404; the tablet no longer operates for the store | ⏸ | ⏸ | | QA team |
| XVI-001 (re-pair) | Home shows the new store name, never the old one; no old PIN; no notice on the next Welcome | ⏸ | ⏸ | | QA team |
| Smoke | Catalog, search, detail, share still work while paired; `403` does not unpair | — | ⏸ | | QA team |

## Status

- **Handed to the QA team (2026-11-06):** every emulator case above (X:XVI-040, X:XVI-041, XVI-001 re-pair, smoke, TalkBack reading of the Welcome notice). The change was verified with unit tests, lint and a `profile` build. Run it against the QA store with a tablet paired to it, never with `Medium_Tablet` (El Martillo).

## Unit-test coverage of the delta specs

| Spec scenario | Test |
|---|---|
| api-error-details — Problem body is parsed / Legacy code key / Unreadable body | `core:network` `ProblemDetailTest` |
| api-error-details — Pairing exchange keeps its error codes | `core:network` `DevicePairingApiServiceTest`, `core:data` `DevicePairingRepositoryImplTest` |
| device-unlink — Detect on any call / Only DEVICE_UNLINKED revokes | `core:network` `DeviceUnlinkedDetectionTest` |
| device-unlink — Revoke the local device session / Customer data is cleared | `core:domain` `HandleDeviceUnlinkedUseCaseTest` |
| device-unlink — Concurrent 401s / Late answer after re-pairing | `HandleDeviceUnlinkedUseCaseTest` |
| device-unlink — No retry of a rejected token | `core:data` 5 repository tests (`given_401_…_without_retry`) |
| device-unlink — Unlinked tablet returns to Welcome | `app` `MainViewModelTest.revoked_session_routes_to_onboarding_and_releases_the_kiosk` (+ X:XVI-040) |
| store-session — Revoke / Dismiss / No notice by default | `core:data` `StoreSessionRepositoryImplTest` |
| store-session — Revocation survives a restart | DataStore persistence (+ X:XVI-040 with `am force-stop`) |
| device-unlink — Nothing of the old store survives | `DeviceSettingsRepositoryImplTest.clear_…`, `StoreSessionRepositoryImplTest.revoke_…` (+ XVI-001) |
| app-onboarding — Welcome notice shown / not shown / Continue dismisses | `features:onboarding` `WelcomeViewModelTest` |
