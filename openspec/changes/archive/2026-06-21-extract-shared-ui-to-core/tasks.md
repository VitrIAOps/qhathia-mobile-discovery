## 1. Shared copy in `core:ui` (additive)

- [x] 1.1 Create `core/ui/src/main/res/values/strings.xml` (the module currently ships only drawables).
- [x] 1.2 Add shared error/action keys with the existing verbatim values: `discovery_error_network_title` ("Perdí la conexión…"), `discovery_error_network_message`, `discovery_error_generic_title` ("Algo salió mal"), `discovery_action_retry` ("Reintentar").
- [x] 1.3 Add `discovery_price_format` ("S/ %1$.2f").
- [x] 1.4 Add shared action labels + content descriptions: back, new-search, go-to-cart, voice-search mic cd, search-icon cd, share — using the current verbatim values.
- [x] 1.5 Add shared close-confirmation button labels (`discovery_dialog_confirm_close` "Sí, cerrar", `discovery_dialog_cancel` "Cancelar") and the shared search placeholder.
- [x] 1.6 Add shared voice-processing copy reused by home + product-search: `discovery_voice_listening` ("Escuchando"), `discovery_voice_organizing` ("Organizando ideas"), `discovery_voice_slow_response`.

## 2. Shared components in `core:ui`

- [x] 2.1 Create `core/ui/.../component/InlineErrorText.kt` — `bodySmall` error-colored text; light + dark `@Preview`.
- [x] 2.2 Create `core/ui/.../component/DiscoveryDivider.kt` — 1dp full-width divider Box (color param defaulting to `DiscoveryColors.Divider`); `@Preview`.
- [x] 2.3 Create `core/ui/.../component/CloseIconButton.kt` — bordered top-right "X" using `R.drawable.ic_close`; params `onClick`, `contentDescription`, `modifier`; light + dark `@Preview`.
- [x] 2.4 Create `core/ui/.../component/PrimaryActionButton.kt` — filled button with inline `CircularProgressIndicator`; params `text`, `isLoading`, `enabled`, `onClick`; light + dark `@Preview`.
- [x] 2.5 Create `core/ui/.../component/DiscoveryTextField.kt` — labeled single-line field with placeholder + error border; params `value`, `onValueChange`, `label`, `placeholder`, `isError`, `visualTransformation`, `keyboardType`; light + dark + error `@Preview`.
- [x] 2.6 Create `core/ui/.../component/ConfirmationDialog.kt` — `AlertDialog` wrapper; params `title`, `message`, `confirmLabel`, `dismissLabel`, `onConfirm`, `onDismiss`; light + dark `@Preview`.
- [x] 2.7 Build `:core:ui` (`./gradlew :core:ui:assembleDebug`) to confirm the additive layer compiles.

## 3. Migrate `features:settings`

- [x] 3.1 `WifiPasswordDialogContent`: replace the private close box with `CloseIconButton`, `PasswordField` with `DiscoveryTextField`, `SubmitButton` with `PrimaryActionButton`, and the inline error `Text` with `InlineErrorText`.
- [x] 3.2 `UnpairConfirmationDialog` / any close-confirmation: route through `ConfirmationDialog`; repoint the cancel/retry labels to the shared strings.
- [x] 3.3 Replace settings dividers (`NetworkSettingsContent`, `PairingContent`) with `DiscoveryDivider` where they match the 1dp pattern.
- [x] 3.4 Delete now-redundant `settings` strings (`pairing_retry`→shared retry, shared cancel, `network_back`/`network_settings_back_cd`→shared back) and private composables; keep feature-specific copy.
- [x] 3.5 Build `:features:settings`.

## 4. Migrate `features:cart`

- [x] 4.1 `CartDialogContent`: replace both close "X" boxes with `CloseIconButton`, the name/phone fields with `DiscoveryTextField`, the place-order button with `PrimaryActionButton`, dividers with `DiscoveryDivider`, and inline errors with `InlineErrorText`.
- [x] 4.2 Repoint price rendering to `discovery_price_format`; repoint share + quantity content descriptions to shared strings.
- [x] 4.3 Delete redundant `cart` strings (`cart_price`, share label, qty cd duplicates) and private composables.
- [x] 4.4 Build `:features:cart`.

## 5. Migrate `features:product-detail`

- [x] 5.1 Route the close-confirmation `AlertDialog` through `ConfirmationDialog`; repoint shared confirm/cancel labels.
- [x] 5.2 Repoint network/generic error title+message, retry, price, back/new-search/go-to-cart/mic/search-icon/share, prompt+placeholder, and qty content descriptions to the shared `core.ui.R` strings.
- [x] 5.3 Replace 1dp dividers with `DiscoveryDivider`.
- [x] 5.4 Delete the redundant `product-detail` strings now sourced from `core:ui`.
- [x] 5.5 Build `:features:product-detail`.

## 6. Migrate `features:product-search`

- [x] 6.1 Route the close-confirmation `AlertDialog` through `ConfirmationDialog`; repoint shared confirm/cancel labels.
- [x] 6.2 Repoint network error title+message, price, back/new-search/go-to-cart/mic/search-icon, and search placeholder to shared strings.
- [x] 6.3 Update `ProductSearchMessages.processingTextRes()` to return the shared `UiR.string` entries for "Escuchando"/"Organizando ideas"/slow-response (resolver stays in the feature).
- [x] 6.4 Delete the redundant `product-search` strings now sourced from `core:ui`.
- [x] 6.5 Build `:features:product-search`.

## 7. Migrate `features:catalog`

- [x] 7.1 Repoint network error title+message, price, back/new-search/go-to-cart/mic/search-icon, prompt+placeholder, and the shared confirm/cancel labels to shared strings (keep `catalog_close_dialog_title`/`_message` feature-specific).
- [x] 7.2 Use `ConfirmationDialog` for the close-catalog dialog, passing the feature-specific title/message.
- [x] 7.3 Delete the redundant `catalog` strings now sourced from `core:ui`.
- [x] 7.4 Build `:features:catalog`.

## 8. Migrate `features:home`

- [x] 8.1 Update `SttErrorMessages.toCopy()` to return shared `UiR.string` entries for the network-error title/message and the generic-error title (generic message stays feature-specific; resolver stays in the feature).
- [x] 8.2 Repoint shared action content descriptions (mic, search icon) and voice copy ("Escuchando", "Organizando ideas", slow) to shared strings.
- [x] 8.3 Delete the redundant `home` strings now sourced from `core:ui`.
- [x] 8.4 Build `:features:home`.

## 9. Verification

- [x] 9.1 Run `./gradlew assembleDebug` for the whole project — confirm no unresolved `R.string`/component references remain. (BUILD SUCCESSFUL.)
- [x] 9.2 Run `./gradlew test` — confirm existing ViewModel/unit tests pass unchanged. (BUILD SUCCESSFUL — no test changes needed.)
- [x] 9.3 Grep each feature `strings.xml` to confirm none of the migrated verbatim values survive as duplicates. (0 duplicates in features; each value present once in core:ui.)
- [x] 9.4 Visually compare each migrated screen's `@Preview` (light + dark) against its pre-refactor rendering to confirm no visual regression. (Verified — no visual regression.)
