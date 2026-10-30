## 1. QA baseline

- [ ] 1.1 Build the `profile` APK from `fix/qa-wave-3` before any change (same code as `main` @ `6c288e6`) and reproduce on `Medium_Tablet` (API 26) and `Tablet_API36` with adb:
  - Labels: a script over `uiautomator dump` that lists clickable nodes with empty `text` and `content-desc` on Home, Catalog, Search, Detail, Cart, Pairing (`Tablet_API36`) and Settings (needs the exit PIN; skip if not available). Save the counts.
  - CAR-080: the cart `EditText`s with no text show no hint or description in the dump.
  - DET-004: stepper at 3, "Agregar", "Agregar" again → 6 units.
  - DET-062 / DET-063: a product with no photo or a broken one (search production data; if none, note it and rely on the preview / unit check).
  - RED-004: detail opened, network off, "Reintentar" with network back → related products missing.
  - CAR-014: type letters in the cart phone field with `input text`; check the keyboard shown on tap (screenshot).
  - EXP-005: select a chip, run another search, go back → chip unselected.
  - EXP-003: `Tablet_API36` pairing code on screen, wait for the screen timeout (`settings get system screen_off_timeout`, set it to 30 s for the test) → screen off.
  - EXP-004: `pm clear` on `Tablet_API36` (unpaired) → permission request over the welcome screen.
  - AJU-030 / AJU-032 / AJU-022 / AJU-040 / AJU-041 / AJU-043: only if the exit PIN is available; otherwise mark as user-assisted.
- [ ] 1.2 Create `qa-report.md` with one row per case ID: before, after, evidence, who ran it.

## 2. Labels on touchable controls (D1): INI-070, CAR-080, DET-080, VIN-070

- [ ] 2.1 `core:ui`: a helper for labelled icon controls (`LabeledIconButton` or `Modifier.labeled(description)`) that sets the description on the clickable node and none on the icon. Use it in `CloseIconButton`, the search bar mic and search icons, `QuantityStepper`, and the card cart buttons (`ProductCard`, `ProductRowCard`).
- [ ] 2.2 Features: apply it to the Home gear and mic, the back arrows (Catalog, Search, Detail, Pairing), the cart line remove and "Ver detalle", the share dialog close, the Wi-Fi dialog icons, and any other clickable icon found by the 1.1 script.
- [ ] 2.3 `DiscoveryTextField`: expose the field label as the description (also when empty).
- [ ] 2.4 Pairing code row: one node, "Código de vinculación: 2 Y 5 6 7" (`clearAndSetSemantics`). Gallery thumbnails: "Foto N de <producto>" (new `core:ui` string with placeholders).
- [ ] 2.5 Compose UI test or unit check is not set up in the project; the 1.1 script is the check (9.2).

## 3. Product images (D2): DET-062, DET-063

- [ ] 3.1 `core:ui`: `ProductImage(url, contentDescription, modifier)` with the "no image" placeholder for null, blank and failed URLs, and the neutral background while loading. Light and dark previews.
- [ ] 3.2 Replace the six `AsyncImage` calls (cards, gallery main and thumbnails, cart lines, success summary) with it.

## 4. Product detail (D3): DET-004, RED-004

- [ ] 4.1 `ProductDetailViewModel`: after an `Added` result, `quantity` back to 1. Unit test (3 then add → 1; limit refused → quantity kept).
- [ ] 4.2 `Retry` also runs `loadSuggestions()` when related products are not `Loaded`. Unit test (failed related + retry → loaded).

## 5. Phone input (D4): CAR-014

- [ ] 5.1 `core:domain`: `filterPhoneInput(raw: String): String` next to the phone validation (digits, spaces, one leading `+`, max length). Unit tests (letters dropped, `+51 987 654 321` kept, second `+` dropped, cap).
- [ ] 5.2 Cart and product share use it on every phone change (in the ViewModels, so the state never holds letters). Existing ViewModel tests still pass; add one per feature.

## 6. Search history (D5): EXP-005

- [ ] 6.1 `HistoryEntry` keeps the selected filters and suggestion; `showEntry` restores them. Unit test: select a chip, new search, Back → chip selected and no new search request.

## 7. Onboarding and pairing (D6): EXP-003, EXP-004

- [ ] 7.1 `PairingScreen`: `keepScreenOn` while the phase is `WaitingForApproval`, cleared when it changes or the screen leaves.
- [ ] 7.2 `MainActivity`: remove the `onCreate` request; request `RECORD_AUDIO` the first time Home is resumed in the process (shared permission gate from wave 2 or the activity launcher). Mic buttons unchanged.

## 8. Settings (D7): AJU-022, AJU-030, AJU-032, AJU-040, AJU-041, AJU-043, VIN-003

- [ ] 8.1 `core:network`: optional `storeName` in the `/devices/me` DTO. `core:domain`: `PairedDevice.lastSeenAt`. `core:data`: mapping; `getPairedDevice` saves a non-blank `storeName` with `UpdateStoreNameUseCase`.
- [ ] 8.2 Session DataStore: `pairingCode` saved on a successful exchange, cleared on unpair; `getPairedDevice` reads it. Unit tests in the repository.
- [ ] 8.3 Pairing details: "Actividad" uses `lastSyncAt ?: lastSeenAt`; "Código usado" shows "No disponible" when missing; previews updated.
- [ ] 8.4 `NetworkSettingsRepository.rescan()` (`WifiManager.startScan()`); `RefreshNetworks` calls it; `LocationPermissionResult(granted = true)` also calls it. Unit tests in the ViewModel with a fake repository.
- [ ] 8.5 Location permission missing: message plus "Permitir ubicación" that relaunches the request; the "no longer asked" case shows the device-settings message (`shouldShowRequestPermissionRationale` false after a refusal).
- [ ] 8.6 "Datos móviles" as a read-only status row; remove the toggle path (`ToggleMobileData`, `SetMobileDataEnabledUseCase`, `actionNotPermitted` if unused elsewhere) and its tests.

## 9. Build checks and emulator QA

- [ ] 9.1 `./gradlew testDebugUnitTest` (JDK 21 JBR) green.
- [ ] 9.2 `./gradlew lint assembleDebug assembleProfile` green; previews (light and dark) for every new or changed state.
- [ ] 9.3 Re-run every case of 1.1 on the new `profile` APK for the "after" column; the label script shows zero unlabelled clickable nodes on each screen.
- [ ] 9.4 Regression sweep: waves 1 and 2 flows still pass (navigation guard, Back = "Volver", voice stop, cart dismissal, session timeout, pinning).
- [ ] 9.5 Complete `qa-report.md` and update `docs/handoff/qa-2026-10-25-casos-observados-por-flujo.md` with an **Ola 3** status in each wave-3 row, noting what stays with the backend.
