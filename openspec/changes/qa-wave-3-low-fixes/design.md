## Context

Wave 3 of the 2026-10-25 QA run: the low cases the app can fix by itself. It starts from `main` after wave 2 (`6c288e6`). The current state of the code behind each case:

| Case(s) | Root cause in the app |
|---|---|
| INI-070, CAR-080, DET-080, VIN-070 | Icon-only controls put `contentDescription` on the `Icon` inside an `IconButton` or a clickable `Box`. The clickable node itself has no label: `uiautomator` on Home shows `Button text="" content-desc=""` for the gear, with "Ajustes" on a non-clickable child. The same pattern repeats on the cards' cart buttons, close buttons, the back arrow and the stepper. |
| CAR-080 | `DiscoveryTextField` shows its hint as a placeholder composable; the `EditText` node exposes neither a hint nor a label. |
| DET-080 | `ProductImageGallery` thumbnails use `contentDescription = null`. |
| VIN-070 / EXP-005 (vinc.) | The pairing code is five separate `Text`s, read as five loose letters. |
| DET-004 | `ProductDetailViewModel.onAddToCart` keeps `quantity` after adding, so a second tap adds the same amount again. |
| DET-062, DET-063 | The six `AsyncImage` calls (cards, gallery, cart lines, success summary) have no `error` / `fallback`: a missing or broken image leaves a blank area. |
| RED-004, EXP-011 | `ProductDetailUiEvent.Retry` calls `load()` only; `loadSuggestions()` runs once in `init`. |
| CAR-014 | Both phone fields declare `KeyboardType.Phone`, but nothing filters the value: letters typed with a hardware keyboard, pasted, or from a keyboard that ignores the hint are accepted. To be confirmed in the baseline (task 1.1). |
| EXP-005 | `ProductSearchViewModel.showEntry` calls `clearSelection()` and the history entry stores no selection, so going back shows the result with every chip unselected. |
| EXP-003 | `FLAG_KEEP_SCREEN_ON` is set only by the kiosk lock, which needs a paired tablet. The onboarding pairing screen times out like any app. |
| EXP-004 | `MainActivity.onCreate` requests `RECORD_AUDIO` on every launch, so a fresh install asks over the welcome screen. |
| AJU-030 (Actividad) | "Actividad" uses `lastSyncAt`, which the server leaves `null`; `/devices/me` also returns `lastSeenAt`, which is filled. |
| AJU-032 | "Código usado" comes from `DevicePairingRepositoryImpl.lastGeneratedCode`, held in memory only: lost after any restart. |
| VIN-003, AJU-030 (Comercio) | `StoreSession.storeName` is never written (`updateStoreName` has no caller) and no endpoint returns the store name to the tablet. Backend dependency. |
| AJU-041 | `NetworkSettingsRepositoryImpl.setMobileDataEnabled` always fails: a non-system app cannot change mobile data. |
| AJU-040 | `RefreshNetworks` is a TODO (`-> Unit`). |
| AJU-043 | The data source starts one scan when collected; when the location permission is granted afterwards nothing scans again, so the list is empty the first time. |
| AJU-022 | The location permission is requested only on entering the screen; once refused there is no way to ask again. |

Constraints: Clean Architecture + MVI; no new library, Gradle module or endpoint. The app is pinned in kiosk mode, so it cannot open Android's own settings screens while locked.

## Goals / Non-Goals

**Goals:**
- `uiautomator dump` on every screen shows no clickable node without text or a description (a measurable rule for QA).
- No blank image areas, no repeated quantities, no missing related products after a retry.
- The pairing screen stays on, and a fresh install does not ask for the microphone over the welcome screen.
- Settings shows real values or an honest "No disponible", and every Wi-Fi control does something.

**Non-Goals:**
- The store name itself (backend: add `storeName` to `GET /devices/me`). The app is ready to show it.
- VIN-011, EXP-001, INI-015 / BUS-017 (backend).
- A full TalkBack audit beyond the QA findings. The labelling rule covers new code too.

## Decisions

### D1. The label lives on the touchable element (INI-070, CAR-080, DET-080, VIN-070)

Icon-only controls set the description on the clickable modifier (`Modifier.semantics { contentDescription = … }` on the `IconButton` / clickable `Box`, with the `Icon`'s own `contentDescription = null`), so the node that receives the tap is the one that is labelled. A small helper in `core:ui` (`LabeledIconButton` or a `Modifier.labeled(…)` extension) avoids repeating it. Text fields set `semantics { contentDescription = label }` from their visible label. The pairing code row uses `clearAndSetSemantics { contentDescription = "Código de vinculación: 2 Y 5 6 7" }`; thumbnails get "Foto N de <producto>".

Alternative: rely on Compose merging the icon into the button for TalkBack. It works for TalkBack, but `uiautomator` and the QA tools still report unlabelled buttons, and the gap is easy to reintroduce.

QA check: a script over `uiautomator dump` lists clickable nodes with empty `text` and `content-desc`; the target is zero on Home, Catalog, Search, Detail, Cart, Pairing and Settings.

### D2. One `ProductImage` with a placeholder (DET-062, DET-063)

`core:ui` gets `ProductImage(url, contentDescription, modifier)`: `AsyncImage` with a neutral "no image" placeholder (gray background and a photo icon) for `null`, empty and failed URLs, and the same background while loading. Cards, the gallery main image and thumbnails, cart lines and the success summary use it.

### D3. Detail: stepper reset and retry (DET-004, RED-004)

After `AddToCart` succeeds (`Added`), `quantity` goes back to 1. `Retry` runs `load()` and, when related products are not `Loaded`, `loadSuggestions()` as well.

### D4. Digits-only phone input (CAR-014)

The phone fields (cart and product share) keep `KeyboardType.Phone` and filter the value before it reaches the ViewModel: digits, spaces and one leading `+`, capped at the length the validation accepts. The filter is a pure function in `core:domain` next to `ValidateConsumerPhoneUseCase`, so both features share it and it is unit-tested.

### D5. Search history keeps the selection (EXP-005)

`HistoryEntry` stores the selected filters and suggestion of that result; `showEntry` restores them instead of clearing them. The selection only labels the chips; the result itself is already a settled snapshot, so no search runs again.

### D6. Pairing: keep the screen on; permission on Home (EXP-003, EXP-004)

- `PairingScreen` sets `LocalView.current.keepScreenOn = true` while a code is shown (`WaitingForApproval`) and clears it otherwise, in both hosts (onboarding and Settings).
- The `RECORD_AUDIO` request moves from `MainActivity.onCreate` to the first time Home is resumed in the process, using the same permission gate as the mic buttons. The mic buttons keep asking on tap. The Home request is not throttled further: Android itself stops asking after "Don't ask again".

### D7. Settings: real values, working Wi-Fi controls (AJU-022/030/032/040/041/043)

- `PairedDevice` gets `lastSeenAt`; "Actividad" shows `lastSyncAt ?: lastSeenAt`.
- The code is saved in the session DataStore when the token exchange succeeds (`StoreSession.pairingCode`) and read by `getPairedDevice`. Without it, "Código usado" shows "No disponible".
- `DevicePairingDto` accepts an optional `storeName`; when present, `getPairedDevice` saves it through `UpdateStoreNameUseCase`, and Home and Settings show it.
- `NetworkSettingsRepository.rescan()` calls `WifiManager.startScan()`; "Buscar redes" uses it, and the screen calls it when the location permission becomes granted. Android throttles scans (4 per 2 minutes on API 28+); a throttled scan still returns the latest results.
- When the location permission is missing, the list area shows a message and a "Permitir ubicación" button that requests it again. If Android no longer shows the dialog, the message says to allow it from the device settings outside kiosk mode.
- "Datos móviles" becomes a status row ("Activos" / "Inactivos", or "Sin SIM" when the tablet has no telephony / no SIM) with an "Abrir ajustes de datos" button (hidden without SIM). The button runs the existing temporary kiosk exit (`ExitKioskModeUseCase`, the same as "Salir del modo kiosko": unpin, show the bars, not persisted) and then starts Android's screen: `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` on API 29+, `Settings.ACTION_DATA_ROAMING_SETTINGS` on API 26–28, falling back to `Settings.ACTION_WIRELESS_SETTINGS` when no activity resolves. Back returns to the Network settings, which re-read the state on resume. The tablet pins again when the merchant goes back to Home (existing "Temporary exit and re-lock" rule).
- Why the exit is needed: while the task is pinned, starting another package's activity is refused by the system ("Attempt to violate Lock Task Mode", seen in wave 1, KIO-041). Task 1.0 confirms the intents and the re-pin on API 26 and API 36 before implementing.
- Alternatives: a read-only row (the merchant must exit kiosk mode by hand and find the setting), or keeping the switch (always fails).

## Risks / Trade-offs

- [Moving the mic request to Home asks the customer instead of the installer] → It happens once, on the first Home after pairing, which is usually the installer's own test. Before, it was asked on the welcome screen, which is also the installer.
- [`startScan()` is throttled] → The button still refreshes the list from the latest results; the UI does not promise a new scan.
- [`storeName` may never come] → The field is optional and the current placeholders stay.
- [The merchant leaves the tablet on Android's settings] → Same exposure as "Salir del modo kiosko" today, behind the exit PIN; the tablet pins again on the next Home, a restart, or the system gesture rule.
- [Labels duplicated between the icon and the button] → The helper sets the icon's description to `null`, so there is a single label.

## Migration Plan

App-only change, shipped with the next APK. The new `pairingCode` DataStore key starts empty; tablets already paired show "No disponible" until they pair again. Rollback is reverting the change.

## Open Questions

- Backend: add `storeName` (and optionally the store category) to `GET /devices/me`.
- Copy for the new texts ("No disponible", "Permitir ubicación", the Wi-Fi permission message) follows the existing tone; product can adjust it.
