## 1. Domain layer (`core:domain`)

- [x] 1.1 Add `CartItem` domain model (`productId`, `name`, `priceUnit: Double`, `imageUrl`, `availability`, `quantity: Int`, `maxQuantity: Int`) with a `lineSubtotal` derived value, under `core/domain/.../model/`
- [x] 1.2 Add `Cart` domain model (`items: List<CartItem>`) with derived `totalItemCount` and `totalAmount`, plus an `EMPTY` constant, under `core/domain/.../model/`
- [x] 1.3 Add `CartConstants` (`INACTIVITY_TIMEOUT_SECONDS = 60`, stock-cap mapping placeholders `DEFAULT_MAX_QUANTITY` / `LOW_STOCK_MAX_QUANTITY` / `CONSULT_MAX_QUANTITY`, `PHONE_REGEX`, name rule) — comment the stock caps with `// TODO(inventory-stock)` until the API exposes a count
- [x] 1.4 Add `CartRepository` port (`fun observe(): Flow<Cart>`, `suspend fun addProduct(item: CartItem)`, `suspend fun setQuantity(productId, quantity)`, `suspend fun removeProduct(productId)`, `suspend fun clear()`) under `core/domain/.../repository/`
- [x] 1.5 Add `ObserveCartUseCase`, `AddToCartUseCase` (merges same product, bounds by `maxQuantity`), `UpdateCartItemQuantityUseCase` (1..maxQuantity; below 1 removes), `RemoveCartItemUseCase`, `ClearCartUseCase` under `core/domain/.../usecase/`
- [x] 1.6 Add `ValidateConsumerNameUseCase` (non-blank + ≥1 referential name token) and `ValidateConsumerPhoneUseCase` (matches `CartConstants.PHONE_REGEX`) returning a small valid/error result, under `core/domain/.../usecase/`
- [x] 1.7 Unit-test the cart use cases and validations (add/merge/cap, set/remove, clear, totals, name/phone valid+invalid) against a fake repository

## 2. Data layer (`core:data`)

- [x] 2.1 Add `CartSessionLocalDataSource` as a `@Singleton` in-memory holder exposing `MutableStateFlow<Cart>` with add/setQuantity/remove/clear mutations (mirrors `StoreSessionLocalDataSource` shape, but in memory — no DataStore), under `core/data/.../cart/`
- [x] 2.2 Add `CartRepositoryImpl` delegating to `CartSessionLocalDataSource`, under `core/data/.../cart/`
- [x] 2.3 Add a Hilt module binding `CartRepository` → `CartRepositoryImpl` (`@Singleton`; include `ksp(libs.kotlin.metadata.jvm)` in the module), under `core/data/.../di/`
- [x] 2.4 Unit-test `CartRepositoryImpl` (observe emits updates; add merges; setQuantity bounds; remove; clear resets to `Cart.EMPTY`)

## 3. Shared testing (`core:testing`)

- [x] 3.1 Add `FakeCartRepository` (in-memory `MutableStateFlow<Cart>`) for use-case/ViewModel tests
- [x] 3.2 Add `CartFixtures` (a `cartItem(...)` factory and a sample multi-line `cart(...)`)

## 4. Shared UI (`core:ui`)

- [x] 4.1 Add a `CartButton(itemCount: Int, onClick, contentDescription)` composable with a count badge to `core/ui/.../component/`, with light + dark `@Preview`s (zero-count and non-zero)
- [x] 4.2 Add a reusable `QuantityStepper(quantity, onIncrease, onDecrease, canIncrease, canDecrease)` composable to `core/ui/.../component/` with `@Preview`s (the detail screen and the cart dialog both consume it)
- [x] 4.3 Move `ic_whatsapp` from `features/product-detail/.../res/drawable/` to `core/ui/.../res/drawable/` so the cart can reuse it (update the product-detail reference to `core:ui` R)

## 5. Create the feature module (`features:cart`)

- [x] 5.1 Create the module directory `features/cart/src/main/java/com/qhathia/mobile/discovery/feature/cart/`
- [x] 5.2 Add `features/cart/build.gradle.kts` from the Feature module template (compose + hilt + ksp; depend on `core:domain`, `core:ui`, `core:common`; `ksp(libs.kotlin.metadata.jvm)`)
- [x] 5.3 Register `include(":features:cart")` in `settings.gradle.kts`
- [x] 5.4 Add the module `AndroidManifest.xml` (`<manifest />`)

## 6. Feature presentation — MVI (`features:cart`)

- [x] 6.1 Add `CartUiState` (items, totals, consumer name/phone + their validation errors, `isOrderEnabled`) under `model/`
- [x] 6.2 Add `CartUiEvent` (`IncreaseLine(productId)`, `DecreaseLine(productId)`, `RemoveLine(productId)`, `OpenLineDetail(productId)`, `NameChanged`, `PhoneChanged`, `PlaceOrder`, `ShareWhatsApp`, `KeepShopping`, `Close`) under `model/`
- [x] 6.3 Add `CartUiEffect` (`NavigateToDetail(productId)`, `Dismiss`) under `model/`
- [x] 6.4 Add `CartViewModel` (`@HiltViewModel`): observe cart via `ObserveCartUseCase`; map to state with totals; run the validations on input; handle stepper/remove/line-tap (emit `NavigateToDetail`), `KeepShopping`/`Close` (emit `Dismiss`); `PlaceOrder` and `ShareWhatsApp` are no-ops marked `// TODO` (no API)
- [x] 6.5 Add cart copy strings (`strings.xml`): "Carrito", item-count subtitle, "Realizar pedido", "Seguir buscando", "Compartir por WhatsApp", form labels/placeholders (name, celular), validation messages, totals/receipt labels, content descriptions

## 7. Feature UI (`features:cart`)

- [x] 7.1 Add `CartDialogContent` (stateless): header ("Carrito" + count subtitle + close "X"), scrollable item list (image, name, price, `QuantityStepper`, line subtotal, tap → `OpenLineDetail`), receipt-styled totals section, consumer-data form (name + celular with inline errors), and the "Realizar pedido" / "Seguir buscando" / "Compartir por WhatsApp" buttons per Figma `758-25521`
- [x] 7.2 Add an empty-cart presentation in `CartDialogContent`
- [x] 7.3 Add `CartDialog` (stateful host): bind `CartViewModel`, `collectAsStateWithLifecycle`, render inside a Compose `Dialog`, forward `NavigateToDetail` / `Dismiss` effects via hoisted callbacks (`onNavigateToDetail`, `onDismiss`)
- [x] 7.4 Add light + dark `@Preview`s for the cart dialog (with items + valid form, with invalid form, and empty)
- [x] 7.5 Unit-test `CartViewModel` (totals/subtitle from observed cart, stepper bounds + remove-on-zero, validations gate `isOrderEnabled`, line tap emits `NavigateToDetail`, close/keep-shopping emits `Dismiss`, place-order/share are no-ops)

## 8. Wire `features:product-search`

- [x] 8.1 Replace the private `CartButton` usage with the shared `core:ui` `CartButton`, passing the live item count (observe via `ObserveCartUseCase` in `ProductSearchViewModel`, expose in `ProductSearchUiState`)
- [x] 8.2 Change `ProductSearchUiEvent.CartTapped(productId)` handling to add that product to the cart (build a `CartItem` from the result `Product`) via `AddToCartUseCase`; keep the idle-timer reset
- [x] 8.3 Add an `OpenCart` event + `ProductSearchUiEffect.OpenCart`; emit it from the top-bar Ir al carrito control; surface it through a new `onOpenCart` callback on `ProductSearchScreen`
- [x] 8.4 Update product-search unit tests for the cart-add and open-cart behaviors

## 9. Wire `features:product-detail`

- [x] 9.1 Make the detail `QuantityStepper` functional (use the shared `core:ui` stepper); track the chosen quantity in `ProductDetailUiState`, bounded by the product's `maxQuantity`
- [x] 9.2 Change `ProductDetailUiEvent.AddToCart` handling to add the loaded `ProductDetail` with the stepper quantity via `AddToCartUseCase`
- [x] 9.3 Replace the private `CartButton` with the shared `core:ui` `CartButton` showing the live item count (observe via `ObserveCartUseCase`); add an `OpenCart` event + `ProductDetailUiEffect.OpenCart`, emit from the top-bar cart control, surface via a new `onOpenCart` callback on `ProductDetailScreen`
- [x] 9.4 Update product-detail unit tests for the functional stepper, cart-add, and open-cart behaviors

## 10. App integration (`:app`)

- [x] 10.1 Add `:features:cart` to `:app` dependencies in `app/build.gradle.kts`
- [x] 10.2 In `MainActivity`, hold app-level `showCart` state; render `CartDialog` as an overlay above the `NavHost` when `showCart` is true; wire its `onDismiss` → hide, `onNavigateToDetail` → hide + `navController.navigate(productDetailRoute(id))`
- [x] 10.3 Route the search and detail `onOpenCart` callbacks to set `showCart = true`
- [x] 10.4 Implement the 1-minute off-Home inactivity clear: wrap the `NavHost` in a root `Modifier.pointerInput` that resets the timer on pointer interaction (observe-only `awaitPointerEvent`, no consume); run the timer only while the current destination is not `HOME_ROUTE`; on expiry call `ClearCartUseCase` (clears the cart per the literal requirement)

## 11. Definition of Done

- [x] 11.1 Confirm no new manifest permissions are needed (WhatsApp share deferred; `INTERNET` / `ACCESS_NETWORK_STATE` already declared); manifest audit complete — cart makes no network/intent calls, no manifest change
- [x] 11.2 `./gradlew test` passes (domain, data, cart, search, detail unit tests)
- [x] 11.3 `./gradlew lint` passes (cart, core:ui, product-search, product-detail, app)
- [x] 11.4 Light + dark previews verified for the cart dialog and the `CartButton`/`QuantityStepper`; all interactive controls expose content descriptions and ≥48dp touch targets
- [x] 11.5 Verify the render-only "Realizar pedido" and "Compartir por WhatsApp" paths are clearly marked `TODO` and make no network/intent calls
