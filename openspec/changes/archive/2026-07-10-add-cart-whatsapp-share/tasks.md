> **Module note:** No new Gradle module is required — every module this change touches (`:core:domain`, `:core:network`, `:core:data`, `:features:cart`) already exists and is registered in `settings.gradle.kts`. The share slice layers onto these mirroring the existing `cart-order-submission` vertical slice. Do **not** add any of it as packages inside `:app`.

## 1. Domain layer (`core:domain`)

- [x] 1.1 Add the `CartShare` domain model (framework-free): `status: String`, `pdfUrl: String`, `messageId: String?` — capturing the `share-pdf` response for the domain.
- [x] 1.2 Add the `CartShareRepository` port: `suspend fun shareCart(name: String, phone: String, items: List<CartItem>): Result<CartShare>`.
- [x] 1.3 Add `CartShareErrorCategory` enum + `CartShareException` (mirroring `OrderErrorCategory`/`OrderException`); a single generic category is sufficient given the simplified message.
- [x] 1.4 Add `ShareCartUseCase` delegating to `CartShareRepository.shareCart`.

## 2. Network layer (`core:network`)

- [x] 2.1 Add `CartShareDtos.kt` — `@Serializable` `ShareCartRequestDto` (customerName, customerPhone, items), `ShareCartItemRequestDto` (productId, quantity), `ShareCartResponseDto` (status, messageId, pdfUrl, error) — matching `POST /api/v1/carts/share-pdf` in api-contract §9.5.
- [x] 2.2 Add `CartShareApiService.shareCart(accessToken, request)` — `client.post("${QHATHIA_BASE_URL}/api/v1/carts/share-pdf")` with `Authorization: Bearer`, JSON body, a per-request timeout, returning `NetworkResult<ShareCartResponseDto>` via `toNetworkResult()`.

## 3. Data layer (`core:data`)

- [x] 3.1 Add `CartShareMappers.kt` — build `ShareCartRequestDto` from name/phone/`List<CartItem>`, reusing `String.toApiMobileNumber()` and the item-mapping from `OrderMappers` (identical body); map `ShareCartResponseDto` → domain `CartShare`.
- [x] 3.2 Add `CartShareErrorMapper` folding `NetworkResult.HttpError`/`NetworkError`/token failure into `CartShareErrorCategory`.
- [x] 3.3 Add `CartShareRepositoryImpl` — resolve the DEVICE JWT via `SessionTokenProvider`, submit via `CartShareApiService`, and on `401` do the `clear()` + `forceRefresh` retry-once (as in `OrderRepositoryImpl`); treat any `201` as `Result.success(CartShare)`, other outcomes as failure.
- [x] 3.4 Add a Hilt `@Binds` for `CartShareRepository → CartShareRepositoryImpl` (extend `OrderDataModule` or add a `CartShareDataModule`).

## 4. Cart feature — wire the share button (`features:cart`)

- [x] 4.1 Inject `ShareCartUseCase` into `CartViewModel`.
- [x] 4.2 Replace the `ShareWhatsApp` no-op with a `shareCart()` handler mirroring `placeOrder()`: guard `isOrderEnabled` and `submission == Idle`, snapshot the `OrderedLine` summary, set `submission = Submitting`, call `shareCart(name, phone, items)`, set `Success(summary)` on success / `Error` on failure. Reuse the existing `OrderSubmission` phase — do not add a parallel phase or new screens.
- [x] 4.3 In `finishAndReturnHome()`, also reset `submission = OrderSubmission.Idle` so a reopened cart never shows a stale success/error screen ("el carrito se queda con mensaje de éxito tras envío").
- [x] 4.4 Update the `CartViewModel` KDoc so it no longer describes "Compartir por WhatsApp" as an intentional no-op.

## 5. Tests & Definition of Done

- [x] 5.1 `ShareCartUseCase` / `CartShareRepositoryImpl` unit tests (success, 401-retry-once, error mapping) using fakes from `core:testing`; add a `FakeCartShareRepository` if needed.
- [x] 5.2 `CartShareMappers` unit tests — phone normalization variants (`+51 987 654 321`, `51987654321`, `987654321`) and request/response mapping.
- [x] 5.3 `CartViewModel` tests (Turbine): `ShareWhatsApp` emits `Submitting`→`Success`/`Error`; double-submit is ignored; a share while an order is submitting is blocked; `finishAndReturnHome` resets `submission` to `Idle`.
- [x] 5.4 Validate Definition of Done: `./gradlew lint` and `./gradlew test` pass, and the cart dialog previews still render in light and dark for the submitting/success/error phases (reused from the order slice — no new previews required).

## 6. Follow-up — share from product detail + order-then-share (`core:ui`, `features:cart`, `features:product-detail`)

- [x] 6.1 Extract the submission-phase overlays into `core:ui` (`SubmissionScaffold.kt`: `SubmittingOverlay`, `SubmissionSuccessContent`, `SubmissionErrorContent`, `SubmissionLine`) — copy passed in as parameters — so cart and product-detail reuse the identical loading/success/error UX without a feature→feature dependency. Refactor `CartDialogContent` to consume them.
- [x] 6.2 `CartViewModel` "Realizar pedido" now creates the order **and**, on success, shares the quote PDF (retried up to 3×). `OrderSubmission.Success` gains `shareDelivered`; when every share retry fails the order still succeeds with the "acércate al vendedor e indícale tu nombre" fallback copy.
- [x] 6.3 `finishAndReturnHome` also clears the consumer form (name/phone/errors) so the next session starts blank.
- [x] 6.4 Product detail: "Compartir por WhatsApp" opens a self-contained `ShareProductDialog` (own name/phone form, validated with the cart's use cases) that shares the selected product (at the stepper quantity) via `ShareCartUseCase` with the same 3× retry, reusing the `core:ui` overlays. Accepting success closes and resets the dialog.
- [x] 6.5 Tests: `CartViewModel` (place-order-also-shares delivered/not-delivered, form cleared on accept) and `ProductDetailViewModel` (dialog gating, validation, share success/error-after-retries, accept resets). `./gradlew lint` + affected `testDebugUnitTest` pass.
