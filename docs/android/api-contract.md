# VitrIA — Referencia de API

**Base URL:** `http://87.99.156.164` (producción — IP fija del VPS, no hay dominio HTTPS)  
**Versión:** 0.1.0-SNAPSHOT  
**Formato:** JSON / UTF-8  
**Autenticación:** Bearer JWT en header `Authorization`

> **Nota para el frontend**: producción es `http://87.99.156.164` (IP directa, sin HTTPS, sin dominio). Local dev: `http://localhost` (todos los servicios via nginx en :80).

> **Si venís del lado Android**: [`docs/handoff/android-pendientes.md`](../handoff/android-pendientes.md)
> es el plan de trabajo — qué consumir, qué borrar y por qué, ordenado por impacto.
> Este documento es el contrato; aquel es la lista de tareas.

---

## Cambios recientes (Fase 1-34)

**Fase 34 — 2026-10-25:** el token de la tablet lleva rubro, centro comercial y slugs, igual que el del staff. Antes llevaba solo `storeId`, y la IA de la búsqueda no sabía de qué rubro era la tienda. Para tokens viejos, vo completa esos datos desde su proyección. Detalle en la [sección 34](#34-el-token-de-la-tablet-lleva-el-rubro-fase-34). La app no cambia.


**Fase 33 — 2026-10-25:** `GET /insights/unmet-searches` solo cuenta búsquedas en las que la IA identificó un producto. "Quiero algo negro" ya no sale como `negro`. Detalle en la [sección 33](#33-productos-no-encontrados-solo-con-producto-identificado-fase-33). Sin cambio de contrato.


**Fase 32 — 2026-10-25:** `GET /insights/unmet-searches` separa lo que falta en el rubro de lo que piden pero no es del rubro. `groups` trae solo lo primero; lo segundo va en el campo nuevo `otherRequests`. Cada grupo suma `storeFit`. Detalle en la [sección 32](#32-afinidad-con-el-rubro-en-productos-no-encontrados-fase-32). No rompe la app actual.


**Fase 31 — 2026-10-25:** "Algo menor a 150 soles" devuelve los productos de ese rango, más baratos primero. Antes caía en "no pude encontrar lo que buscas". Las sugerencias del "sin resultados" priorizan las que entran en el rango. "Menor" ya no aparece en "Productos no encontrados". Detalle en la [sección 31](#31-búsqueda-solo-de-precio-fase-31). Sin cambio de contrato.


**Fase 30 — 2026-10-24:** la IA de la búsqueda por voz conoce el rubro y los productos de la tienda. Detalle en la [sección 30](#30-la-ia-de-la-búsqueda-conoce-la-tienda-fase-30). Sin cambio de contrato.


**Fase 29 — 2026-10-24 (reporte del front del 2026-10-23):** detalle en la [sección 29](#29-búsqueda-por-voz-precio-con-descuento-y-productos-no-encontrados-fase-29).

- **`POST /conversations/search` suma `discountedPrice` y `discountEndsAt`** en cada `results[*]`: resultados normales y sugerencias de "sin resultados". Misma regla que biz. Se calcula al responder, así que un descuento vencido deja de aplicar a la hora exacta. `null` = sin descuento vigente.
- **El rango de precio de la voz mide el precio que se paga.** "Más de 50" compara contra `discountedPrice ?: price`.
- **El texto hablado dice el precio con descuento**, no el de lista.
- **`GET /insights/unmet-searches` deja de mostrar oraciones y adjetivos.** "Dame algo de más de 50 soles" ya no es un producto no encontrado; "Una mesa giratoria" sale como `mesa giratoria` y no como `giratoria`; las interpretaciones de baja confianza del LLM no entran.


**Fase 25-28 — 2026-10-18 (triage de 31 bugs reportados desde la app):** detalle por bug en [`docs/qa/TRIAGE-BUGS.md`](../qa/TRIAGE-BUGS.md), tareas del front en [`docs/handoff/android-pendientes.md`](../handoff/android-pendientes.md).

*Campos nuevos — ninguno rompe un contrato existente:*
- **`GET /products` suma `stock`**: faltaba, y la app lo estaba **inventando** desde el hash del UUID. El comerciante veía "19 stock", abría el producto y tenía 1, y el pedido se rechazaba al confirmar.
- **`discountedPrice` y `discountEndsAt`** en `GET /products`, `/products/{id}` y `/catalog/by-category`. El backend **nunca había calculado** el precio con descuento: devolvía `discounts[]` crudo y cada cliente debía hacer la aritmética. Ninguno la hacía. `null` = sin descuento vigente.
- **Push `ORDER_EXPIRING`**, 10 min antes del vencimiento (configurable). El toggle "Pedido por expirar" existía desde la Fase 20 **sin emisor**. `status` se mantiene en `PENDING`: el pedido no cambió de estado.

*Correcciones de comportamiento:*
- **`GET /products`: los filtros se cruzan.** `categoryId` y `availability` se combinaban con un `if/else if` y la categoría ganaba — "Iluminación interior" + "Stock bajo" devolvía la categoría entera.
- **`availability=LOW_STOCK` ahora significa algo**: se traduce a `0 < stock <= umbral` (default 5, el mismo de "Productos para reponer"). La sync stock↔availability nunca asigna ese valor, así que el filtro devolvía cero **siempre**.
- **La búsqueda por voz aplica piso Y techo.** "Lámpara más de 50 soles" devolvía las de **menos** de 50: `minPrice` se extraía, viajaba en el DTO y nadie filtraba por él.
- **"Clientes" deja de quedarse en 0.** Sin el header `X-Kiosk-Session` la sesión se infiere por ventana de inactividad del mismo `device_id` (15 min, configurable). El header, si viene, siempre gana.
- **Comandos a la tablet: los vencidos dejan de bloquear.** Con la tablet apagada un comando se quedaba "en vuelo" hasta la purga de las 03:30 y el botón quedaba en gris casi un día.
- **El botón de WhatsApp del PDF** concatenaba `51` siempre: `+51 987 654 321` terminaba en `wa.me/5151987654321`. Si el número no tiene forma de móvil peruano, ahora se **omite** el botón.
- **"Productos no encontrados"** agrupaba frases crudas ("Quiero una amoladora") y las duplicaba contra la versión limpia ("Amoladora"). Ahora el término se canoniza y los grupos se fusionan uniendo sesiones, no sumándolas.

*Campos retirados:*
- **`pricesIncludeIgv` e `igvRate` salen de `GET/PUT /stores/{id}`.** Era una declaración que solo cambiaba una etiqueta del PDF; un control sin efecto observable se lee como roto. **No rompe apps instaladas**: mandar el campo en el `PUT` sigue dando 200, y su ausencia en la respuesta no rompe un DTO con default.
- **El PDF dejó de decir "by VitrIA"** (configurable, vacío por defecto).

*Infraestructura, sin cambio de contrato:*
- **Los tres modelos de IA configurados estaban dados de baja** desde julio y agosto. El fail-open lo escondía: descripciones vacías, sugerencias vacías, todo con 200. Ver `scripts/check-llm-models.sh`.

**Fase 24 — 2026-10-03 (métricas del kiosko y detalle de pedido):** correcciones sobre lo reportado desde la app.
- **`GET /api/v1/products/{id}` ahora registra la visita** cuando quien consulta es la **tablet** (rol DEVICE). Antes `kiosk_events` solo se llenaba si la app llamaba explícitamente a `/analytics/events`, y mientras no lo hiciera la pestaña Actividad quedaba en cero con gente usando la tablet todo el día.
- **Nunca cuenta al staff.** Un OWNER/SELLER/ADMIN abriendo la ficha desde su celular **no** suma: contarlo mediría el trabajo del propio comerciante.
- **Header nuevo `X-Kiosk-Session`** (opcional) en ese GET: identifica a la persona para el conteo de clientes distintos. Sin él la visita igual cuenta, pero no se puede atribuir a nadie.
- **`uniqueSessions` dejó de exigir `SESSION_START`**: cuenta sesiones distintas con **cualquier** evento. Antes, una tablet que reportaba vistas pero no la apertura de sesión mostraba *0 clientes y N productos consultados*.
- **`GET /dashboard/activity` suma `confirmedOrders`** (+ su delta): "Clientes" es **todo el que usó la tablet**, no el que compró. Con los dos números juntos el embudo se lee de corrido.
- **`GET /dashboard/ranking` suma `needsAttention.totalCount`**: cuántos productos no rotaron **en total**, no solo los 3 que devuelve la lista.
- **`GET /orders/{id}` suma `items[].productImageUrl`**: la miniatura que el modal "Detalle de solicitud" no podía pintar porque el backend nunca la mandaba.

**Fase 23 — 2026-09-13 (change `device-remote-commands`):** los botones "Actualizar catálogo" y "Reiniciar" de la card de Dispositivos.
- **`POST /api/v1/devices/{deviceId}/commands`** (OWNER/ADMIN) → 202. Encola `REFRESH_CATALOG` o `RESTART_APP`. **Idempotente mientras hay uno en vuelo**: tocar el botón cinco veces encola uno.
- **`GET /api/v1/devices/{deviceId}/commands`** → los últimos 20 con su estado, para pintar "Enviado hace 2 min" / "Listo" / "Falló".
- **`POST /api/v1/devices/me/commands/{commandId}/ack`** (DEVICE) → la tablet confirma qué pasó.
- **El heartbeat entrega los comandos**: `pendingCommands` en la respuesta. **No es instantáneo** — hay hasta un ciclo de latido de latencia, así que la pantalla debe decir "Enviado" y no "Listo" hasta el ack.
- Los comandos **expiran a los 10 min y no se reintentan**. Un "Reiniciar" del viernes no puede ejecutarse el lunes, y re-entregar un `RESTART_APP` dejaría la tablet en bucle.
- **auth V18**: tabla `device_commands`.

**Fase 22 — 2026-09-13 (change `store-display-settings`):** pantalla Configuración.
- **`GET/PUT /api/v1/stores/{storeId}`** gana `language` (`"es"` | `"en"`). **Sin endpoint nuevo** — es donde ya viven `whatsapp` y `address`.
- `language` **se persiste pero todavía no se aplica**: que el asistente de voz conteste en inglés es su propio change.
- Traía también `pricesIncludeIgv` e `igvRate`: **retirados en la Fase 27**, el toggle solo cambiaba una etiqueta del PDF.
- **auth V17**: `stores.language` + `stores.prices_include_igv`.
- **Además**: `GlobalExceptionHandler` (auth y biz) mapea `HttpMessageNotReadableException` a **400** con los valores válidos del enum. Antes un enum desconocido en el body salía como **500**.

**Fase 21 — 2026-09-13 (change `remote-tablet-settings`):** ajustes remotos de la tablet.
- **`GET/PUT /api/v1/devices/{deviceId}/settings`** (OWNER/ADMIN) — los 6 controles del mockup: vitrina en reposo, asistente por voz, brillo, modo kiosco, PIN de salida y sincronización automática. `PUT` es **parcial**.
- **`brightnessMode` es un selector** (`AUTO | LOW | MEDIUM | HIGH`), no un porcentaje: el comerciante declara una intención y Android resuelve el nit según su sensor.
- **El PIN es write-only**: se guarda con BCrypt y el comerciante solo ve `exitPinSet: true|false`. Para quitarlo, `removeExitPin: true` explícito.
- **`GET /api/v1/devices/me/settings`** (DEVICE) — la tablet lee **los suyos**; es el único endpoint que devuelve `exitPinHash`, para poder salir del modo kiosco **sin red**.
- **`POST /api/v1/devices/heartbeat` devuelve `settingsVersion`** — sube en cada escritura. La tablet re-lee sus ajustes solo cuando el número cambia: sin polling nuevo ni push a la tablet.
- **Intervalo del heartbeat: 60 s.** Nunca estuvo escrito en la API y cada app eligió un número por su cuenta: la tablet empezó mandándolo cada 10 min y la app del comerciante da por desconectada una tablet sin heartbeat en 10 min. Intervalo igual a ventana = "Desconectada" entre ticks, y ajustes que tardan 10 min en llegar. El contrato original (`docs/contracts/end-to-end-flow.md`) decía 30–60 s; se fija en **60 s**.
- **`devices.model`** (V16) — el bloque INFORMACIÓN de la pantalla pide el modelo comercial ("Lenovo Tab M11"). La tablet lo reporta en el heartbeat; `serialNumber`, `linkedAt` y `appVersion` ya existían.
- **auth V16**: tabla `device_settings`, 1:1 con `devices`, creada al vincular (y al vuelo para las tablets anteriores).

**Fase 20 — 2026-09-13 (change `notification-preferences`):** pantalla Notificaciones del perfil.
- **`GET /api/v1/notifications/preferences`** (OWNER/SELLER/ADMIN) — los 7 toggles del mockup ya resueltos + `activeCount`/`totalCount` para el "N de 7 alertas activas" del menú. **Nunca 404**: si el usuario nunca guardó nada devuelve los defaults (todos en `true`).
- **`PUT /api/v1/notifications/preferences`** — update **parcial**: manda solo lo que cambió.
- Los 7, en los grupos del mockup: **Pedidos** (`newOrder`, `orderExpiring`) · **Tablet** (`tabletDisconnected`, `productOutOfStock`, `lowBattery`) · **Tu negocio** (`weeklyReport`, `sellingSuggestions`).
- **Solo `newOrder` tiene emisor hoy** (el push `NEW_ORDER`). Los otros seis se guardan y esperan su job — el contrato ya está fijo para cuando existan.
- **`ORDER_TAKEN` no tiene toggle y sale siempre**: es sincronización (saca la orden de "Pendientes" en los demás celulares), no una alerta. Silenciarlo dejaría la lista mintiendo.
- **La ausencia de preferencias nunca silencia**: sin fila, o si la consulta falla, el push sale igual.
- **biz V17**: tabla `notification_preferences`, una fila por `(user_id, store_id)`.

**Fase 19 — 2026-09-13 (change `account-security-and-sessions`):** pantallas de Seguridad del perfil.
- **`POST /api/v1/auth/change-password`** (autenticado) — 204. Cambiar la contraseña **revoca las demás sesiones**; manda tu `currentRefreshToken` en el body para no cerrarte la sesión a ti mismo (si lo omites se cierran **todas**, incluida la tuya).
- **`GET /api/v1/auth/sessions`** (autenticado) — lista de equipos con sesión abierta. Manda tu refresh token en el header `X-Refresh-Token` para que la tuya venga con `current: true`. **Nunca** se devuelven refresh tokens.
- **`DELETE /api/v1/auth/sessions/{sessionId}`** → 204 · **`POST /api/v1/auth/sessions/revoke-others`** → 204.
- **`POST /api/v1/auth/login`**: acepta `deviceLabel` opcional (máx. 100) y devuelve `sessionId`. Si no mandas `deviceLabel` se usa el `User-Agent`; si tampoco, `"Dispositivo desconocido"`.
- Redis pasa de `rt:<token>` String a HASH + índice `user_sessions:<userId>`. **El deploy no desloguea a nadie**: los tokens viejos se siguen aceptando y se migran al primer refresh.
- **Sin migración SQL** — todo el cambio vive en Redis. `passwordUpdatedAt` ya existía desde auth V14 (Fase 18).

**Fase 18 — 2026-09-13 (change `profile-subscription-and-devices`):** soporte de las pantallas de Perfil.
- **`GET/PUT /api/v1/users/me`**: nuevo campo `phone` (móvil peruano `^9\d{8}$`, editable) y `passwordUpdatedAt` de solo lectura. **El `email` NO es editable** — es la credencial de login; si viene en el body se ignora.
- **`GET /api/v1/subscriptions/current`**: agrega `priceMonthlyPen`, `productLimit` y los 4 flags de features del plan → la card de "Mi suscripción" se pinta con **una sola llamada**.
- **`GET /api/v1/devices`**: cada dispositivo agrega `name`, `batteryLevel`, `networkStatus`, `lastSyncAt` y `storeName`. La batería y la red **ya venían en el heartbeat y se descartaban**; ahora se persisten (auth V15).
- **`PATCH /api/v1/devices/{deviceId}`** (OWNER/ADMIN) — renombrar la tablet.
- **`GET /api/v1/catalog/count`** (cualquier autenticado) — `{ activeProducts }` para la card "Catálogo visible: N productos" sin bajar el manifest completo.
- **auth V14/V15**: `users.phone`, `users.password_updated_at`, `devices.battery_level`, `devices.network_status`.
- ⚠️ **Pagos NO implementados**: método de pago, historial de pagos y cancelar suscripción **no existen** (no hay pasarela contratada). El front debe mostrarlos como "próximamente".


**Fase 17 — 2026-09-13 (change `store-home-dashboard`):**
- **`GET /api/v1/dashboard/sales`** (OWNER/SELLER/ADMIN) — serie de ventas por `MONTHLY|WEEKLY|DAILY|HOURLY` + el **periodo en curso** con sus KPIs (ingresos, nº ventas, ticket promedio, unidades) y la variación vs el periodo anterior. Buckets cortados en **hora de Perú**.
- **`GET /api/v1/dashboard/ranking`** — "lo que más vende" y "lo que necesita atención" (**dinero inmovilizado** = stock × precio de lo que no rotó).
- **`GET /api/v1/dashboard/activity`** — clientes que usaron la tablet y productos consultados, con deltas. Primera lectura de `kiosk_events`.
- **`GET /api/v1/dashboard/restock`** — productos con stock bajo el umbral.
- **`GET /api/v1/insights/unmet-searches`** (**vo-api**) — búsquedas del kiosko sin resultado, agrupadas por término canónico y ordenadas por **personas distintas**. vo ahora persiste `sessionId` y `subtype` en `search_events`.
- **biz V16**: índices de soporte (sin columnas nuevas). Detalle en la sección 17.


**Fase 16 — 2026-07-26 (change `orders-expiry-and-requests-screen`):**
- **Expiración de pedidos**: un `PENDING` vence a los `ORDERS_TTL_MINUTES` (default **60 min**). Nuevo estado **`EXPIRED`** + campo `expiresAt` en los shapes de orden (el countdown "Expira en X" lo pinta la app). Un job cada minuto hace la transición y emite push `ORDER_TAKEN` con `status=EXPIRED`.
- **`POST /api/v1/orders/{id}/recover`** (OWNER/SELLER/ADMIN) — revive un pedido `EXPIRED` a `PENDING` con TTL nuevo. Un rechazo manual NO se recupera.
- **Código corto**: `orderCode` (6 dígitos, único por tienda) en summary + detalle. Null en pedidos previos a V15 — la app debe tolerarlo.
- **`GET /orders`**: `status` acepta **lista separada por comas** (`REJECTED,EXPIRED` para la tab Rechazados) y nuevo param **`q`** (busca por nombre del cliente parcial o prefijo del código). El summary gana `rejectionReason`.
- **Detalle**: cada item trae `productStock` + `productAvailability` (para el warning "Quedan pocos disponibles"; el umbral lo decide la app).
- **biz V15**: `orders.expires_at`, `orders.order_code`, CHECK de status con `EXPIRED`, índices de búsqueda/expiración.


**Fase 15 — 2026-07-25 (change `order-push-notifications-fcm`):**
- **`POST /api/v1/notifications/tokens`** + **`DELETE /api/v1/notifications/tokens`** (OWNER/SELLER/ADMIN) — los celulares del staff registran su token FCM para recibir push cuando la tablet crea un pedido (`NEW_ORDER`) y cuando otro vendedor lo confirma/rechaza (`ORDER_TAKEN`). Upsert por token; DEVICE → 403. Detalle en sección 9.6.
- Push **data-only sin PII** (`{event, orderId, storeId, status}`) — la app hace fetch autenticado y arma la notificación local. Fan-out a todos los celulares activos de la tienda, despacho **después del commit** (fail-open total: ningún fallo de push afecta el pedido).
- **biz V14**: tabla `notification_tokens` (provider-agnóstica) + purga diaria de tokens muertos. Config vía `NOTIFICATIONS_PROVIDER=fcm` + `FCM_CREDENTIALS_JSON_B64`. Default `logging` (comportamiento anterior, sin Firebase).

**Fase 14 — 2026-07-05 (change `kiosk-cart-share-whatsapp`):**
- **`POST /api/v1/carts/share-pdf`** (DEVICE) — el botón "Compartir por WhatsApp" del carrito. Genera un **PDF cotización** de los productos (foto + descripción + cantidad + **precio unitario, subtotal y TOTAL REFERENCIAL**), lo sube a R2, y lo envía al WhatsApp del cliente vía **Qhatia/Qhathia** (bot de Vitria, Meta Cloud API). NO crea orden. Fail-soft: si el envío falla, `status=FAILED` + `pdfUrl` (el PDF siempre se genera). Detalle en sección 9.5.
- **auth-api V13**: `stores` gana `whatsapp` (formato peruano `^9\d{8}$`) + `address`. Editables por el OWNER en `PUT /stores/{id}`, expuestos en `GET /stores/{id}`. El PDF usa el `whatsapp` para el link "hablar con la tienda".
- **Qhatia**: bot centralizado de Vitria (un solo número Meta para todas las tiendas). Config vía `QHATIA_PROVIDER=meta` + `META_*` env vars. Default `stub` (no envía, solo genera el PDF).

**Fase 13 — 2026-06-29 (change `kiosk-cart-order`):**
- **`POST /api/v1/orders` rediseñado** para carrito del kiosko: ahora soporta **N items con cantidad** (1-20 items, 1-99 qty cada uno) + **datos del cliente** (`customerName` + `customerPhone` peruano `^9\d{8}$` requeridos) + **subtotal/total server-side** con snapshot del precio al momento de crear (no recalculado después). **BREAKING** vs el shape legacy single-product — nadie usaba el viejo en producción.
- **`POST /orders/{id}/confirm` ahora decrementa stock** de cada producto por su `quantity`. Si algún producto tiene insuficiente (`stock < qty`), responde **422 `STOCK_INSUFFICIENT`** con detail completo (productId, productName, available, requested) y la orden queda en PENDING (sin decremento parcial). Productos `CONSULT` (`stock=null`) no se tocan.
- **`GET /orders?customerPhone=<digits>`** — nuevo filtro para que el comerciante busque las órdenes de un cliente cuando le devuelve la llamada.
- **Schema V13**: nueva tabla `order_items`, `orders` agrega `customer_name/customer_phone/subtotal/total`, drop `orders.product_id`.
- **Sync con V12 OUT_OF_STOCK**: cuando un `confirm` decrementa el stock de un producto a 0, automáticamente queda `availability=OUT_OF_STOCK` y desaparece del catálogo público.
- Detalle de los 6 endpoints en sección 9.

**Hotfix 2026-06-29 (tarde, V12):** se agregó el valor `OUT_OF_STOCK` al enum `availability`. La sync ahora deriva `stock=0 → OUT_OF_STOCK` (antes era `LOW_STOCK`, semánticamente incorrecto). Los productos `OUT_OF_STOCK` NO aparecen en catálogo público ni suggestions (queries filtran por `AVAILABLE`); SÍ se incluyen en el CSV de WhatsApp con valor `"out of stock"` (Meta los muestra greyed-out). Detalle en sección 8.2.

**Hotfix 2026-06-29 (tarde):** `POST /api/v1/products/{id}/regenerate-description` cambió a **`POST /api/v1/products/regenerate-description`** (sin path param). El endpoint es stateless — sirve para producto en draft (antes de guardar) y para edición. Antes requería un id de producto ya creado, lo que rompía el flujo del mockup "Registro de producto". Detalle en sección 8.8.

**Fase 12 — 2026-06-29 (change `product-images-r2-upload`, Fase B):**
- **BizAPI: storage abstraction con Cloudflare R2** — nuevo `StorageGateway` (stub + R2 impl) siguiendo el patrón gateway+stub+impl ya usado por vision/llm. Activación vía `STORAGE_PROVIDER=r2` + credenciales R2 en `.env` del VPS. El frontend NUNCA toca el bucket.
- **Nuevo endpoint `POST /api/v1/products/with-images`** (multipart): recibe `product` part (JSON) + N parts `images` (binarias, max 10 × 5MB). El backend valida todo, sube las fotos a R2, crea el producto + media rows en **una transacción atómica** (rollback total si cualquier upload falla). Detalle en sección 8.9.
- **Nuevo endpoint `POST /api/v1/products/{id}/images`** (multipart): adjunta 1..N fotos adicionales a un producto existente. Posiciones continúan desde el max actual, primary=false. Detalle en sección 8.10.
- **Coexistencia Firebase + R2**: productos legacy con URLs Firebase siguen funcionando sin migración. `ProductMedia.url` es opaco al origen.
- **`POST /api/v1/products` JSON-only** (sección 8.2) y **`POST /api/v1/products/{id}/media` con URL pre-hosteado** (legacy) NO cambian — backward-compatible.

**Fase 11 — 2026-06-29 (change `product-stock-and-ai-draft`, Fase A):**
- **BizAPI: campo `stock` numérico en productos** — nueva columna `stock INTEGER NULL` (V11). Sync automática `stock ↔ availability` en `POST/PUT /api/v1/products`: si el caller omite `availability` y envía `stock`, el backend deriva `null→CONSULT`, `0→LOW_STOCK`, `>0→AVAILABLE`. Si el caller envía ambos, su `availability` gana. Productos legacy (sin `stock`) siguen funcionando sin cambios.
- **BizAPI: nuevo endpoint `POST /api/v1/products/draft-from-image`** (multipart, max 5 MB) — el LLM Groq multimodal genera descripción + categoría sugerida + tags a partir de UNA foto. **No crea producto** y **no sube la imagen a storage** (procesamiento en memoria con base64 inline a Groq). Defense-in-depth: categoría filtrada al conjunto del store, tags filtrados al catálogo cerrado. Fail-open total. Detalle en sección 8.7.
- **BizAPI: nuevo endpoint `POST /api/v1/products/{id}/regenerate-description`** — paráfrasis IA de la descripción existente, sin inventar facts. Reusa `LlmGateway` (Groq Llama 3.3 70B). Fail-open: si el LLM no responde, devuelve la descripción original. Detalle en sección 8.8.
- **Search projection (Mongo `vitria_search`)** ahora incluye `stock` para futuro filtrado por vo-api.
- **Fase B** (R2 storage + upload de fotos al guardar) queda fuera de scope; vendrá en un change posterior.

**Fase 10 — 2026-06-28:**
- **BizAPI: productos sugeridos en pantalla detalle**: nuevo endpoint `GET /api/v1/products/{id}/suggestions` devuelve hasta 5 productos complementarios del mismo store al producto base (mockup "Detalle de producto" → "Podrías acompañar este producto con…"). Heurística cascading: LLM Groq genera keywords complementarios → si no matchea, cae a misma categoría → si no llena 5, completa con productos populares del store. Campo `source` indica qué camino contribuyó (`"llm" | "same-category" | "store-popular" | "mixed" | "empty"`). Detalle en sección 8.6.
- **biz-api ahora también usa LLM Groq**: misma key `LLM_API_KEY` que vo-api. Property opt-in `LLM_COMPLEMENTARY_ENABLED=true`. Fail-open universal: si Groq cae, el endpoint sigue funcionando con cascading sin LLM.

**Fase 9 — 2026-06-16 (ClickUp 86e1r9epc):**
- **VoAPI search devuelve `filters` y `suggestions` para el sidebar del kiosko (Mockup 5)**: el response de `POST /api/v1/conversations/search` ahora incluye `filters: [{criterion, options[]}]` (max 5 criterios × 5 options, strings planos) y `suggestions: List<String>` (max 5). Cuando `noResults=true` ambos van `[]`. Detalle en sección 12.
- **`MAX_RESULTS` del search sube de 5 a 20**.
- **Cada `results[*]` ahora incluye `brand: String, nullable` y `tags: List<String>`** — el frontend filtra client-side cuando el cliente toca una opción del sidebar.
- **BizAPI tag-catalog acepta campo `group`**: nueva columna `tag_group VARCHAR(60) NULL` en la tabla. El admin asigna la dimensión (`"voltaje"`, `"tipo"`, `"material"`...) al crear/editar tag. Tags con `group=null` siguen contando al ranking pero no aparecen como criterio en `filters`. Detalle en sección 14.
- **LLM Groq activado en producción**: `LLM_PROVIDER=openai` con `OPENAI_BASE_URL=https://api.groq.com/openai` (sin /v1, el gateway lo agrega). Intent (confidence ya pasa de 0.5 a 0.8+), Response (frases naturales) y Suggestions (frases contextuales generadas por Llama 3.3 70B) todos activos. Cuando Groq falla, automáticamente fail-open a rule-based + templates.

**Fase 8 — junio 2026:**
- **VoAPI search devuelve hasta 3 sugerencias en el unhappy path**: cuando `POST /api/v1/conversations/search` no encuentra match al query, ahora `results` viene con hasta 3 productos del store en vez de array vacío. Mantiene `noResults=true`. Nuevo campo opcional `suggestedFromCategory: String, nullable` para personalizar el copy ("Te puede interesar de aretes:" vs "Te puede interesar:"). Backward compatible — clientes viejos siguen funcionando. Detalle en sección 12.
- **BizAPI: catálogo agregado por categoría**: nuevo endpoint `GET /api/v1/catalog/by-category` devuelve la lista de categorías del store con sus productos AVAILABLE/LOW_STOCK embebidos. Usado por la pantalla "ver catálogo" del kiosko. Detalle en sección nueva 8.5.
- **STT Deepgram activado en producción**: `POST /api/v1/stt/token` ahora emite tokens reales de Deepgram (no más stubs). El frontend ya puede transcribir audio de verdad. Detalle en sección 6.

**Cambios recientes (Fase 1-7)**

Esta sección resume los cambios contractuales más relevantes desde la versión inicial v0.1.0. Los detalles completos viven en las secciones individuales más abajo.

**Onboarding cerrado (Fase 3):**
- `POST /api/v1/auth/register` ya **NO es público** — requiere `Authorization: Bearer <admin>`. El comerciante NO se autoregistra; Vitria provisiona usuarios, tiendas y suscripciones vía endpoints `/api/v1/admin/*`.
- Login y refresh siguen siendo los únicos endpoints públicos para el comerciante.

**Multi-store con switch (Fase 1-2):**
- Un usuario puede tener varias tiendas (tabla `user_stores`).
- El JWT lleva claims nuevos: `storeId`, `shoppingCenterId`, `storeCategory`. Pueden estar ausentes para `ADMIN` global o multi-store sin elegir.
- Endpoint nuevo `POST /api/v1/auth/switch-store/{storeId}` emite un JWT con la tienda activa.
- Endpoint nuevo `GET /api/v1/users/me/stores` lista las tiendas del usuario logueado.
- `LoginResponse` y `UserProfileResponse` incluyen ahora un array `stores[]`.

**Centros comerciales y rubro cerrado (Fase 1):**
- Entidad nueva `ShoppingCenter` con CRUD admin en `/api/v1/shopping-centers`.
- `Store.category` ahora es enum cerrado `StoreCategory` (~13 valores) — antes era texto libre.
- Cada `Store` cuelga de un `ShoppingCenter` (FK `shopping_center_id`).

**Tags con vocabulario controlado (Fase 4-5):**
- Tabla `tag_catalog` indexada por (mall × rubro). CRUD admin en `/api/v1/admin/tag-catalog` con import masivo `/bulk`.
- Endpoint `GET /api/v1/tags/suggestions` para que el comerciante consulte sugerencias.
- VoAPI filtra los tags al rankear: solo los del catálogo cuentan al match. Los tags custom siguen visibles al frontend pero invisibles al LLM y al ranking.

**Auto-tag con visión (Fase 6 + 6.1):**
- `POST /api/v1/products/{id}/suggest-tags` — para producto ya existente (foto cargada vía `mediaId` o URL).
- `POST /api/v1/tags/suggest-from-image` — para foto en `drafts/` antes de crear el producto. Útil cuando el comerciante elige una foto, pide tags, y recién después confirma el guardado.
  Ambos llaman al `VisionGateway` (Groq Llama 4 Scout por default; `stub` en dev) y filtran defensivamente contra el catálogo del (mall × rubro).

---

## Índice

1. [Auth API — Autenticación](#1-auth-api--autenticación)
2. [Auth API — Gestión de Tienda](#2-auth-api--gestión-de-tienda)
3. [Auth API — Gestión de Usuarios](#3-auth-api--gestión-de-usuarios)
4. [Auth API — Planes y Suscripciones](#4-auth-api--planes-y-suscripciones)
5. [Auth API — Dispositivos](#5-auth-api--dispositivos)
6. [Auth API — STT Token](#6-auth-api--stt-token)
7. [BizAPI — Categorías](#7-bizapi--categorías)
8. [BizAPI — Productos](#8-bizapi--productos)
   8.5. [BizAPI — Catálogo agregado (Fase 8)](#85-bizapi--catálogo-agregado-fase-8)
9. [BizAPI — Órdenes](#9-bizapi--órdenes)
10. [BizAPI — Device Sync](#10-bizapi--device-sync)
11. [BizAPI — Analytics](#11-bizapi--analytics)
12. [VoAPI — Búsqueda Conversacional](#12-voapi--búsqueda-conversacional)
13. [Auth API — Centros Comerciales](#13-auth-api--centros-comerciales)
14. [Admin API — Onboarding cerrado por Vitria](#14-admin-api--onboarding-cerrado-por-vitria)
15. [BizAPI — Tags (sugerencias y auto-tag)](#15-bizapi--tags-sugerencias-y-auto-tag)
16. [Quick Start — Flujo Completo End-to-End](#16-quick-start--flujo-completo-end-to-end)
17. [Dashboard de inicio (Fase 17)](#17-dashboard-de-inicio-fase-17)
18. [Pantallas de Perfil (Fase 18)](#18-pantallas-de-perfil-fase-18)
19. [Seguridad de la cuenta y sesiones (Fase 19)](#19-seguridad-de-la-cuenta-y-sesiones-fase-19)
20. [Preferencias de notificaciones (Fase 20)](#20-preferencias-de-notificaciones-fase-20)
21. [Ajustes remotos de la tablet (Fase 21)](#21-ajustes-remotos-de-la-tablet-fase-21)
22. [Configuración de la tienda: idioma (Fase 22)](#22-configuración-de-la-tienda-idioma-fase-22-revisado-en-la-27)
23. [Comandos remotos a la tablet (Fase 23)](#23-comandos-remotos-a-la-tablet-fase-23)
24. [Métricas del kiosko: qué mide cada número (Fase 24)](#24-métricas-del-kiosko-qué-mide-cada-número-fase-24)
25. [Precio con descuento y aviso de expiración (Fase 25)](#25-precio-con-descuento-y-aviso-de-expiración-fase-25)
26. [Correcciones del triage del 2026-10-18 (Fase 26)](#26-correcciones-del-triage-del-2026-10-18-fase-26)
27. [Se retira el toggle de IGV (Fase 27)](#27-se-retira-el-toggle-de-igv-fase-27)
28. [Filtros de Inventario y rango de precio por voz (Fase 28)](#28-filtros-de-inventario-y-rango-de-precio-por-voz-fase-28)
29. [Búsqueda por voz: precio con descuento y productos no encontrados (Fase 29)](#29-búsqueda-por-voz-precio-con-descuento-y-productos-no-encontrados-fase-29)
30. [La IA de la búsqueda conoce la tienda (Fase 30)](#30-la-ia-de-la-búsqueda-conoce-la-tienda-fase-30)
31. [Búsqueda solo de precio (Fase 31)](#31-búsqueda-solo-de-precio-fase-31)
32. [Afinidad con el rubro en "Productos no encontrados" (Fase 32)](#32-afinidad-con-el-rubro-en-productos-no-encontrados-fase-32)
33. ["Productos no encontrados" solo con producto identificado (Fase 33)](#33-productos-no-encontrados-solo-con-producto-identificado-fase-33)
34. [El token de la tablet lleva el rubro (Fase 34)](#34-el-token-de-la-tablet-lleva-el-rubro-fase-34)

---

## Tipos comunes

| Tipo | Formato |
|------|---------|
| UUID | `"550e8400-e29b-41d4-a716-446655440000"` |
| Instant | `"2025-04-15T18:30:00Z"` (ISO 8601 UTC) |
| BigDecimal | `"189.90"` (string con decimales) |

## Enums de referencia

| Enum | Valores |
|------|---------|
| `PlanType` | `ESENCIAL` \| `PRO` \| `TOTAL` |
| `UserRole` | `OWNER` \| `SELLER` \| `ADMIN` \| `DEVICE` — `ADMIN` es admin global de Vitria (sin tienda); `OWNER`/`SELLER` son del comerciante |
| `UserStoreRole` | `OWNER` \| `SELLER` — rol del usuario en una tienda específica (vínculo `user_stores`) |
| `StoreCategory` | `ELECTRONICS` \| `LIGHTING` \| `TEXTILE` \| `FOOD` \| `BEAUTY` \| `JEWELRY` \| `FURNITURE` \| `DECORATION` \| `TOOLS` \| `SPORTS` \| `BOOKS` \| `TOYS` \| `OTHER` |
| `DeviceStatus` | `PENDING_LINK` \| `ACTIVE` \| `INACTIVE` \| `SUSPENDED` |
| `DevicePlatform` | `ANDROID` |
| `SubscriptionStatus` | `ACTIVE` \| `TRIAL` \| `SUSPENDED` \| `CANCELLED` |
| `AvailabilityStatus` | `AVAILABLE` \| `LOW_STOCK` \| `CONSULT` |
| `MediaType` | `IMAGE` \| `VIDEO` |
| `DiscountType` | `PERCENTAGE` \| `FIXED_AMOUNT` |
| `OrderStatus` | `PENDING` \| `CONFIRMED` \| `REJECTED` \| `CANCELLED` |
| `OrderSource` | `ASSISTANT` |

## Respuestas de error (RFC 7807 ProblemDetail)

Todos los errores siguen el mismo formato:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "El email no tiene un formato válido",
  "instance": "/api/v1/auth/register"
}
```

---

## 1. Auth API — Autenticación

### POST /api/v1/auth/register

**Descripción:** Crea User + Store + Subscription + vínculo `user_stores` en una sola transacción. Devuelve JWT del comerciante recién creado.

**⚠️ Requiere rol ADMIN** — desde Fase 3 el onboarding es cerrado. Solo Vitria provisiona comerciantes. Para casos donde solo se necesita una pieza (un user nuevo sin tienda, o vincular un user existente a otra tienda) usar los endpoints granulares en [Admin API](#14-admin-api--onboarding-cerrado-por-vitria).

**Autenticación:** Bearer (rol `ADMIN`)
**URL producción:** `http://87.99.156.164/api/v1/auth/register`

**Request Body:**
```json
{
  "fullName": "string (requerido, 2-150 chars)",
  "email": "string (requerido, formato email, max 200 chars)",
  "password": "string (requerido, 8-100 chars)",
  "storeName": "string (requerido, 2-120 chars)",
  "shoppingCenterId": "uuid (requerido) — id del ShoppingCenter al que pertenece la tienda",
  "storeCategory": "StoreCategory (requerido) — enum cerrado, ver tabla de enums",
  "storeLocation": "string (opcional, max 200 chars)",
  "storePhone": "string (opcional, max 20 chars)",
  "planType": "ESENCIAL | PRO | TOTAL (requerido)"
}
```

**Response 201:** (igual al de login — incluye `stores[]`)
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "uuid",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": { "id": "uuid", "email": "...", "fullName": "...", "role": "OWNER" },
  "store": {
    "id": "uuid",
    "shoppingCenterId": "uuid",
    "name": "Joyería María",
    "category": "JEWELRY"
  },
  "stores": [{ "id": "...", "shoppingCenterId": "...", "name": "...", "category": "JEWELRY" }]
}
```

**Errores:**
- `401` — Sin token
- `403` — Token sin rol ADMIN
- `400` — `shoppingCenterId` no existe; `planType` desconocido
- `409` — Email ya registrado
- `422` — Validación de campos (email, password, longitudes)

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/auth/register \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "María González",
    "email": "maria@joyeria.pe",
    "password": "miPass1234",
    "storeName": "Joyería María",
    "shoppingCenterId": "00000000-0000-0000-0001-000000000001",
    "storeCategory": "JEWELRY",
    "storeLocation": "Gal A, Stand 214",
    "planType": "PRO"
  }'
```

---

### POST /api/v1/auth/login

**Descripción:** Autentica con email y contraseña. Devuelve access token (24h) + refresh token (7d).  
**Autenticación:** Pública  
**URL producción:** `http://87.99.156.164/api/v1/auth/login`

**Request Body:**
```json
{
  "email": "string (requerido, formato email)",
  "password": "string (requerido)"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "uuid",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "email": "juan@tienda.com",
    "fullName": "Juan Pérez",
    "role": "OWNER"
  },
  "store": {
    "id": "uuid",
    "shoppingCenterId": "uuid",
    "name": "Tienda Gamarra",
    "category": "TEXTILE"
  },
  "stores": [
    { "id": "uuid-1", "shoppingCenterId": "...", "name": "Tienda Gamarra", "category": "TEXTILE" }
  ]
}
```

**Política de tienda activa en el JWT (multi-store):**
- Si el usuario tiene **1 sola tienda** → JWT incluye `storeId`, `shoppingCenterId`, `storeCategory`. `store` viene poblado.
- Si tiene **2+ tiendas** → JWT NO lleva `storeId`/`sc`/`cat`. `store=null` y el frontend debe llamar `POST /auth/switch-store/{id}` antes de operar.
- Si es `ADMIN` global (sin tiendas) → JWT sin claims de tenant; `store=null`, `stores=[]`.

**Errores:**
- `400` — Validación fallida
- `401` — Credenciales incorrectas

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "juan@tienda.com", "password": "mipass123"}'
```

---

### POST /api/v1/auth/refresh

**Descripción:** Renueva el access token usando el refresh token (token rotation — el refresh anterior queda inválido).  
**Autenticación:** Pública  
**URL producción:** `http://87.99.156.164/api/v1/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "string (requerido)"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

**Errores:**
- `400` — Refresh token requerido
- `401` — Refresh token inválido o expirado

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "eyJ..."}'
```

---

### POST /api/v1/auth/logout

**Descripción:** Invalida el refresh token del usuario autenticado.  
**Autenticación:** JWT requerido (cualquier rol de usuario)  
**URL producción:** `http://87.99.156.164/api/v1/auth/logout`

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body:**
```json
{
  "refreshToken": "string (requerido)"
}
```

**Response:** `204 No Content`

**Errores:**
- `401` — Token de acceso ausente o inválido

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/auth/logout \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "eyJ..."}'
```

---

### POST /api/v1/auth/switch-store/{storeId}

**Descripción:** Cambia la tienda activa del usuario autenticado. Emite un access token nuevo (y un refresh nuevo, token rotation) con los claims `storeId`, `shoppingCenterId`, `storeCategory` apuntando a `storeId`.

El usuario debe tener acceso a esa tienda vía `user_stores` (sino → 403). Útil tras un login multi-store cuando el JWT inicial no trae tienda activa, o para alternar entre tiendas en una misma sesión.

**Autenticación:** JWT requerido (cualquier rol con tiendas)
**URL producción:** `http://87.99.156.164/api/v1/auth/switch-store/{storeId}`

**Headers:** `Authorization: Bearer <accessToken>`

**Response 200:** mismo shape que `/auth/login`. El JWT nuevo lleva la tienda activa.

**Errores:**
- `401` — Token ausente / inválido
- `403` — La tienda no pertenece al usuario, o el vínculo está desactivado
- `404` — Store no existe

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/auth/switch-store/<storeId> \
  -H "Authorization: Bearer $USER_TOKEN"
```

---

## 2. Auth API — Gestión de Tienda

### GET /api/v1/stores/{storeId}

**Descripción:** Retorna los datos de la tienda especificada. El usuario solo puede ver su propia tienda (ADMIN puede ver cualquiera).  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/stores/{storeId}`

**Headers:** `Authorization: Bearer <accessToken>`

**Response 200:**
```json
{
  "id": "uuid",
  "name": "Tienda Gamarra",
  "tradeName": "TG S.A.C.",
  "category": "Ropa",
  "location": "Gamarra, La Victoria",
  "phone": "987654321",
  "active": true,
  "createdAt": "2025-04-15T18:30:00Z"
}
```

**Errores:**
- `401` — No autenticado
- `403` — Acceso a tienda ajena

**curl:**
```bash
curl http://87.99.156.164/api/v1/stores/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..."
```

---

### PUT /api/v1/stores/{storeId}

**Descripción:** Actualiza los datos de la tienda. Solo el OWNER de la tienda o ADMIN.  
**Autenticación:** JWT requerido (roles: `OWNER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/stores/{storeId}`

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body** (todos los campos opcionales — se actualizan solo los enviados):
```json
{
  "name": "string (opcional, 2-120 chars)",
  "tradeName": "string (opcional, max 120 chars)",
  "category": "string (opcional, max 80 chars)",
  "location": "string (opcional, max 200 chars)",
  "phone": "string (opcional, max 20 chars)"
}
```

**Response 200:** mismo formato que `GET /stores/{storeId}`

**Errores:**
- `401` — No autenticado
- `403` — Rol insuficiente o tienda ajena

**curl:**
```bash
curl -X PUT http://87.99.156.164/api/v1/stores/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"name": "Tienda Gamarra Plus", "phone": "999888777"}'
```

---

## 3. Auth API — Gestión de Usuarios

### GET /api/v1/users/me

**Descripción:** Retorna el perfil del usuario autenticado.  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/users/me`

**Headers:** `Authorization: Bearer <accessToken>`

**Response 200:**
```json
{
  "id": "uuid",
  "email": "juan@tienda.com",
  "fullName": "Juan Pérez",
  "role": "OWNER",
  "active": true,
  "emailVerified": false,
  "lastLoginAt": "2026-04-15T18:30:00Z",
  "createdAt": "2026-04-01T10:00:00Z",
  "stores": [
    {
      "storeId": "uuid",
      "storeName": "Tienda Gamarra",
      "storeCategory": "TEXTILE",
      "shoppingCenterId": "uuid",
      "shoppingCenterName": "Centro Comercial Las Malvinas",
      "role": "OWNER",
      "active": true
    }
  ]
}
```

> Cambio Fase 2: el campo `storeId` (singular) fue eliminado y reemplazado por `stores[]` para soportar multi-store. Para ADMIN global, `stores=[]`.

**Errores:**
- `401` — No autenticado

**curl:**
```bash
curl http://87.99.156.164/api/v1/users/me \
  -H "Authorization: Bearer eyJ..."
```

---

### GET /api/v1/users/me/stores

**Descripción:** Lista las tiendas a las que el usuario autenticado tiene acceso (vía `user_stores`). El frontend la usa para construir el selector de tienda y disparar `POST /auth/switch-store/{id}`.

**Autenticación:** JWT requerido (cualquier rol)
**URL producción:** `http://87.99.156.164/api/v1/users/me/stores`

**Response 200:** array de `UserStoreReference` (mismo shape que `stores[]` en `/users/me`).

```json
[
  {
    "storeId": "uuid",
    "storeName": "Joyería María 1",
    "storeCategory": "JEWELRY",
    "shoppingCenterId": "uuid",
    "shoppingCenterName": "Centro Comercial Las Malvinas",
    "role": "OWNER",
    "active": true
  },
  {
    "storeId": "uuid",
    "storeName": "Cosméticos María",
    "storeCategory": "BEAUTY",
    "shoppingCenterId": "uuid",
    "shoppingCenterName": "Mall Plaza Norte",
    "role": "OWNER",
    "active": true
  }
]
```

**curl:**
```bash
curl http://87.99.156.164/api/v1/users/me/stores \
  -H "Authorization: Bearer $USER_TOKEN"
```

---

### PUT /api/v1/users/me

**Descripción:** Actualiza el perfil del usuario autenticado.  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/users/me`

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body:**
```json
{
  "fullName": "string (opcional, 2-150 chars)"
}
```

**Response 200:** mismo formato que `GET /users/me`

**curl:**
```bash
curl -X PUT http://87.99.156.164/api/v1/users/me \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Juan Carlos Pérez"}'
```

---

## 4. Auth API — Planes y Suscripciones

### GET /api/v1/plans

**Descripción:** Lista todos los planes activos disponibles.  
**Autenticación:** Pública  
**URL producción:** `http://87.99.156.164/api/v1/plans`

**Response 200:**
```json
[
  {
    "id": "uuid",
    "type": "ESENCIAL",
    "name": "Plan Esencial",
    "priceMonthlyPen": 189.00,
    "productLimit": 30,
    "hasAdvancedAnalytics": false,
    "hasVideoProducts": false,
    "hasBulkPriceUpdate": false,
    "hasPrioritySupport": false
  },
  {
    "id": "uuid",
    "type": "PRO",
    "name": "Plan Pro",
    "priceMonthlyPen": 219.00,
    "productLimit": 100,
    "hasAdvancedAnalytics": true,
    "hasVideoProducts": false,
    "hasBulkPriceUpdate": true,
    "hasPrioritySupport": false
  },
  {
    "id": "uuid",
    "type": "TOTAL",
    "name": "Plan Total",
    "priceMonthlyPen": 249.00,
    "productLimit": null,
    "hasAdvancedAnalytics": true,
    "hasVideoProducts": true,
    "hasBulkPriceUpdate": true,
    "hasPrioritySupport": true
  }
]
```

**Nota:** `productLimit: null` significa ilimitado (Plan Total).

**curl:**
```bash
curl http://87.99.156.164/api/v1/plans
```

---

### GET /api/v1/subscriptions/current

**Descripción:** Retorna la suscripción activa de la tienda del usuario autenticado.  
**Autenticación:** JWT requerido (cualquier rol de usuario)  
**URL producción:** `http://87.99.156.164/api/v1/subscriptions/current`

**Headers:** `Authorization: Bearer <accessToken>`

**Response 200:**
```json
{
  "id": "uuid",
  "storeId": "uuid",
  "planId": "uuid",
  "planType": "ESENCIAL",
  "planName": "Plan Esencial",
  "status": "ACTIVE",
  "startedAt": "2025-04-01T00:00:00Z",
  "endsAt": null
}
```

**Errores:**
- `401` — No autenticado
- `404` — La tienda no tiene suscripción activa

**curl:**
```bash
curl http://87.99.156.164/api/v1/subscriptions/current \
  -H "Authorization: Bearer eyJ..."
```

---

## 5. Auth API — Dispositivos

> **Flujo invertido (ClickUp 86e1tvkvq).** La **tablet** (app discovery, sin JWT) genera el código y un `sessionId` opaco, abre el WebSocket de estado y muestra el código. El **comercio** (celular, JWT OWNER) lo ingresa en `/pair` para **aprobar** o **rechazar**. Tras `APPROVED`, la tablet canjea su DEVICE JWT por `sessionId`. El JWT nunca viaja por el socket.

### POST /api/v1/devices/pairing-token

**Descripción:** La tablet genera un código de vinculación de 5 caracteres (TTL 5 min) y un `sessionId` opaco. Lo muestra en pantalla.  
**Autenticación:** **Pública** (la tablet no tiene JWT)  
**URL producción:** `http://87.99.156.164/api/v1/devices/pairing-token`

**Request Body:** ninguno

**Response 200:**
```json
{
  "pairingToken": "AB3CD",
  "sessionId": "uuid",
  "expiresAt": "2026-06-09T18:40:00Z",
  "expiresInSeconds": 300
}
```

> **`expiresInSeconds`** es el TTL relativo (`expiresAt − createdAt`, por defecto 300). La tablet debe correr el regresivo desde este valor en lugar de restar `expiresAt` contra su reloj local: así el contador queda inmune al desfase de reloj del dispositivo (clock skew) frente al servidor.

**Errores:**
- `503` — `PAIRING_GENERATION_EXHAUSTED` (no se pudo generar un código único)

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/devices/pairing-token
```

---

### WSS /api/v1/devices/pairing-session/{sessionId}

**Descripción:** WebSocket (raw, ADR-009) de notificación de estado. Al conectar emite el estado actual; al resolverse empuja un único mensaje terminal y cierra. Vida = TTL del token (5 min).  
**Autenticación:** Pública. El handshake se rechaza si el `sessionId` no existe o ya venció.

**Mensajes (servidor → tablet):**
```json
{ "status": "WAITING|APPROVED|REJECTED|EXPIRED", "sessionId": "uuid" }
```

---

### POST /api/v1/devices/pair

**Descripción:** El comercio ingresa el código mostrado en la tablet y lo **aprueba** (vincula la tienda y crea el dispositivo) o lo **rechaza**. La tablet recibe el desenlace por WebSocket. **No** devuelve el DEVICE JWT.  
**Autenticación:** JWT requerido (roles: `OWNER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/devices/pair`

**Headers:** `Authorization: Bearer <accessToken>`  
**Request Body:**
```json
{
  "pairingToken": "string (requerido)",
  "decision": "APPROVE | REJECT (requerido)",
  "deviceName": "string (requerido, max 200 chars, ej: 'Tablet Mostrador 1')",
  "platform": "ANDROID (requerido)",
  "appVersion": "string (requerido, max 20 chars, ej: '1.0.0')",
  "deviceFingerprint": "string (opcional, max 500 chars)"
}
```

**Response 200 (APPROVE):**
```json
{
  "sessionId": "uuid",
  "status": "APPROVED",
  "storeId": "uuid",
  "deviceId": "uuid",
  "deviceName": "Tablet Mostrador 1",
  "platform": "ANDROID"
}
```
En `REJECT`: `{ "sessionId": "uuid", "status": "REJECTED", "storeId": "uuid" }`.

**Errores:**
- `400` — `INVALID_PAIRING_TOKEN` / `PAIRING_TOKEN_EXPIRED` / `PAIRING_TOKEN_ALREADY_USED`
- `401` — No autenticado · `403` — Rol insuficiente (SELLER/DEVICE)
- `409` — `CATALOG_THRESHOLD_NOT_MET` (catálogo bajo el umbral; la sesión sigue `WAITING`)
- `503` — `CATALOG_READINESS_UNAVAILABLE` (header `Retry-After: 30`)

---

### GET /api/v1/devices/pairing-session/{sessionId}/token

**Descripción:** Tras `APPROVED`, la tablet canjea (one-shot) su DEVICE JWT por `sessionId`.  
**Autenticación:** **Pública** (la tablet solo posee su `sessionId`)

**Response 200:**
```json
{
  "deviceId": "uuid",
  "deviceAccessToken": "eyJ...",
  "storeId": "uuid",
  "deviceName": "Tablet Mostrador 1",
  "platform": "ANDROID",
  "status": "ACTIVE"
}
```

**Nota:** El `deviceAccessToken` es el JWT con `role=DEVICE` (TTL ~1 año, sin refresh). Guardarlo de forma persistente.

**Errores:**
- `404` — `NOT_FOUND` (sesión inexistente)
- `409` — `PAIRING_NOT_APPROVED` (aún `WAITING`) / `PAIRING_REJECTED`
- `410` — `PAIRING_TOKEN_EXPIRED` (sesión expirada) / `PAIRING_TOKEN_ALREADY_USED` (segundo canje)

**curl:**
```bash
curl http://87.99.156.164/api/v1/devices/pairing-session/<sessionId>/token
```

---

### GET /api/v1/devices/me

**Descripción:** Retorna los datos del dispositivo autenticado (desde el JWT del dispositivo).  
**Autenticación:** JWT requerido (rol: `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/devices/me`

**Headers:** `Authorization: Bearer <deviceAccessToken>`

**Response 200:**
```json
{
  "deviceId": "uuid",
  "storeId": "uuid",
  "deviceName": "Tablet Mostrador 1",
  "platform": "ANDROID",
  "appVersion": "1.0.0",
  "status": "ACTIVE",
  "pairedAt": "2025-04-15T18:30:00Z",
  "lastSeenAt": "2025-04-15T20:00:00Z",
  "lastSyncVersion": 1713200000000,
  "lastSyncAt": "2025-04-15T20:00:00Z"
}
```

**Errores:**
- `401` — Token de dispositivo ausente o inválido
- `403` — Token de usuario (se requiere DEVICE)
- `404` — Dispositivo no encontrado

**curl:**
```bash
curl http://87.99.156.164/api/v1/devices/me \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/devices/heartbeat

**Descripción:** El dispositivo reporta su estado operativo periódicamente. Actualiza `last_ping_at`, `app_version` y `last_sync_version`.  
**Autenticación:** JWT requerido (rol: `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/devices/heartbeat`

**Headers:** `Authorization: Bearer <deviceAccessToken>`

**Request Body:**
```json
{
  "appVersion": "string (requerido)",
  "networkStatus": "string (opcional, ej: 'ONLINE', 'LIMITED')",
  "lastSyncVersion": 1713200000000,
  "lastSyncAt": "2025-04-15T20:00:00Z",
  "batteryLevel": 85,
  "uptimeSeconds": 3600
}
```

**Response 200:**
```json
{
  "received": true,
  "serverTime": "2025-04-15T20:05:00Z",
  "catalogVersion": 1713200000000
}
```

**Nota:** Si `catalogVersion` difiere del `lastSyncVersion` enviado, el dispositivo debe sincronizar el catálogo.

**Errores:**
- `401` — Token de dispositivo ausente o inválido
- `422` — `appVersion` requerido

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/devices/heartbeat \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"appVersion": "1.0.0", "networkStatus": "ONLINE", "batteryLevel": 85}'
```

---

### POST /api/v1/devices/logs

**Descripción:** El dispositivo envía lotes de logs para trazabilidad remota.  
**Autenticación:** JWT requerido (rol: `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/devices/logs`

**Headers:** `Authorization: Bearer <deviceAccessToken>`

**Request Body:**
```json
{
  "logs": [
    {
      "level": "INFO | DEBUG | WARN | ERROR | FATAL (requerido)",
      "code": "string (opcional, ej: 'SYNC_FAILED')",
      "message": "string (requerido)",
      "timestamp": "2025-04-15T20:00:00Z (requerido)",
      "context": { "key": "valor" }
    }
  ]
}
```

**Notas de validación:**
- `logs` no puede estar vacío, máximo 100 entradas por request

**Response:** `204 No Content`

**Errores:**
- `401` — Token de dispositivo ausente o inválido
- `422` — Formato inválido (level no reconocido, message vacío)

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/devices/logs \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "logs": [
      {
        "level": "WARN",
        "code": "SYNC_SLOW",
        "message": "Catalog sync took 5 seconds",
        "timestamp": "2025-04-15T20:00:00Z",
        "context": {"durationMs": 5000}
      }
    ]
  }'
```

---

### POST /api/v1/devices

**Descripción:** Provisiona un nuevo dispositivo en el sistema (flujo QR legacy RF-03). Solo ADMIN.  
**Autenticación:** JWT requerido (rol: `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/devices`

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body:**
```json
{
  "serialNumber": "string (opcional, max 100 chars)",
  "androidVersion": "string (opcional, max 20 chars)"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "storeId": null,
  "serialNumber": "SN-001",
  "linkQrCode": "QR-TOKEN-GENERADO",
  "status": "PENDING_LINK",
  "androidVersion": "13",
  "appVersion": null,
  "lastPingAt": null,
  "linkedAt": null,
  "createdAt": "2025-04-15T18:30:00Z"
}
```

**Errores:**
- `401` — No autenticado
- `403` — Rol insuficiente (solo ADMIN)

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/devices \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"serialNumber": "SN-001", "androidVersion": "13"}'
```

---

### GET /api/v1/devices

**Descripción:** Lista todos los dispositivos de la tienda del usuario autenticado.  
**Autenticación:** JWT requerido (roles: `OWNER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/devices`

**Headers:** `Authorization: Bearer <accessToken>`

**Response 200:**
```json
[
  {
    "id": "uuid",
    "storeId": "uuid",
    "serialNumber": null,
    "linkQrCode": null,
    "status": "ACTIVE",
    "androidVersion": "13",
    "appVersion": "1.0.0",
    "lastPingAt": "2025-04-15T20:00:00Z",
    "linkedAt": "2025-04-15T18:30:00Z",
    "createdAt": "2025-04-15T18:30:00Z"
  }
]
```

**curl:**
```bash
curl http://87.99.156.164/api/v1/devices \
  -H "Authorization: Bearer eyJ..."
```

---

### GET /api/v1/devices/{deviceId}

**Descripción:** Retorna el estado de un dispositivo específico.  
**Autenticación:** JWT requerido (roles: `OWNER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/devices/{deviceId}`

**Headers:** `Authorization: Bearer <accessToken>`

**Response 200:** mismo formato que la lista de dispositivos (objeto único).

**Errores:**
- `404` — Dispositivo no encontrado

**curl:**
```bash
curl http://87.99.156.164/api/v1/devices/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/devices/link

**Descripción:** Vincula un dispositivo provisionado (QR) a la tienda del OWNER (flujo RF-03 legacy).  
**Autenticación:** JWT requerido (roles: `OWNER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/devices/link`

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body:**
```json
{
  "qrCode": "string (requerido — código QR del dispositivo)"
}
```

**Response 200:** objeto `DeviceStatusResponse` con `status: "ACTIVE"`.

**Errores:**
- `404` — QR code no encontrado
- `409` — Dispositivo ya vinculado

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/devices/link \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"qrCode": "QR-TOKEN-GENERADO"}'
```

---

### POST /api/v1/devices/{deviceId}/unlink

**Descripción:** Desvincula un dispositivo de la tienda. El dispositivo pasa a estado `INACTIVE`.  
**Autenticación:** JWT requerido (roles: `OWNER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/devices/{deviceId}/unlink`

**Headers:** `Authorization: Bearer <accessToken>`  
**Request Body:** ninguno

**Response 200:** objeto `DeviceStatusResponse` con `status: "INACTIVE"`.

**Errores:**
- `404` — Dispositivo no encontrado
- `403` — Dispositivo de otra tienda

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/devices/550e8400-e29b-41d4-a716-446655440000/unlink \
  -H "Authorization: Bearer eyJ..."
```

---

## 6. Auth API — STT Token

### POST /api/v1/stt/token

**Descripción:** Emite un token temporal de Deepgram para que el frontend pueda abrir una conexión WebSocket directa a Deepgram y transcribir audio en vivo. La API key real de Deepgram (la "Project API key") nunca se expone al cliente — solo se emite este token de corta duración.

**Autenticación:** JWT requerido (cualquier rol autenticado, típicamente `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/stt/token`

**Headers:** `Authorization: Bearer <accessToken>`  
**Request Body:** ninguno (POST con body vacío)

**Response 200:**
```json
{
  "token": "af7ca4b94c64428e51b6e305691342fb7784b713",
  "expiresIn": 60,
  "provider": "deepgram",
  "issuedAt": "2026-06-10T06:38:55.988Z"
}
```

**Campos:**
| Campo | Tipo | Descripción |
|---|---|---|
| `token` | string | Token temporal de Deepgram. Formato hex de 40 chars. Usarlo como `Authorization: Token <token>` al abrir el WSS a Deepgram. |
| `expiresIn` | int | Segundos hasta que el token expire (típicamente 60). |
| `provider` | string | Siempre `"deepgram"` en producción. Si llega `"stub"` significa que el backend tiene `STT_STUB_ENABLED=true` (no debería ocurrir en producción). |
| `issuedAt` | Instant | Timestamp UTC de emisión. |

### Flujo end-to-end de transcripción (lado frontend)

1. **Pedir token** al endpoint anterior (con `Authorization: Bearer <accessToken>` del usuario).
2. **Abrir WebSocket** a Deepgram con el token. URL típica:
   ```
   wss://api.deepgram.com/v1/listen?model=nova-2&language=es&smart_format=true
   ```
   Header de auth en el WSS:
   ```
   Authorization: Token <token-del-paso-1>
   ```
3. **Stream del audio del micrófono** al WSS en chunks PCM/WebM/OGG (lo que Deepgram acepta).
4. **Recibir transcripciones** en mensajes JSON de Deepgram (`channel.alternatives[0].transcript`).
5. **Cuando el usuario termina** de hablar (o cuando llega `speech_final=true`), tomar el texto completo y mandarlo a `POST /api/v1/conversations/search` (sección 12).
6. **Renovar el token** si pasaron >55s desde `issuedAt` y la sesión sigue activa — pedís otro al endpoint.

**Importante:**
- No cacheen el token más de su `expiresIn`. Está pensado para una sesión corta de voz.
- No pasen la API key real de Deepgram al cliente — esto es justamente lo que evita el endpoint.
- Si el endpoint devuelve `provider=stub` con un token tipo `stub-token-xxx`, avisar a backend: la env var `STT_STUB_ENABLED` quedó en `true`. En prod debe ser `false`.

**Errores:**
- `401` — No autenticado (falta o expiró el `accessToken` del usuario)
- `502` — Error en el proveedor Deepgram (rate limit, problema en su lado)
- `503` — STT no configurado en backend (`DEEPGRAM_API_KEY` ausente o `STT_STUB_ENABLED=false` sin keys reales)

**curl de prueba:**
```bash
curl -X POST http://87.99.156.164/api/v1/stt/token \
  -H "Authorization: Bearer eyJ..."
```

---

## 7. BizAPI — Categorías

### GET /api/v1/categories

**Descripción:** Lista todas las categorías activas de la tienda del JWT.  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/categories`

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
[
  {
    "id": "uuid",
    "storeId": "uuid",
    "name": "Lámparas",
    "slug": "lamparas",
    "position": 0,
    "active": true,
    "createdAt": "2025-04-15T18:30:00Z",
    "updatedAt": "2025-04-15T18:30:00Z"
  }
]
```

**Errores:**
- `401` — No autenticado

**curl:**
```bash
curl http://87.99.156.164/api/v1/categories \
  -H "Authorization: Bearer eyJ..."
```

---

### GET /api/v1/categories/{id}

**Descripción:** Retorna una categoría por ID.  
**Autenticación:** JWT requerido (cualquier rol)

**Response 200:** objeto `CategoryResponse` (mismo formato que lista).

**Errores:**
- `404` — Categoría no encontrada

**curl:**
```bash
curl http://87.99.156.164/api/v1/categories/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/categories

**Descripción:** Crea una nueva categoría. El slug se genera automáticamente desde el nombre si no se proporciona.  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/categories`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "string (requerido, max 100 chars)",
  "slug": "string (opcional, max 120 chars — auto-generado si no se envía)",
  "position": 0
}
```

**Response 201:** objeto `CategoryResponse`.

**Errores:**
- `409` — Slug ya existe en la tienda

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/categories \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"name": "Lámparas de Pared", "position": 1}'
```

---

### PATCH /api/v1/categories/{id}

**Descripción:** Actualiza nombre, slug o posición de una categoría. Al actualizar, todos los productos de la categoría se re-sincronizan en MongoDB automáticamente (async).  
**Autenticación:** JWT requerido (cualquier rol)

**Request Body:** mismo formato que `POST /categories` (todos opcionales).

**Response 200:** objeto `CategoryResponse`.

**curl:**
```bash
curl -X PATCH http://87.99.156.164/api/v1/categories/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"name": "Lámparas Decorativas", "position": 2}'
```

---

### DELETE /api/v1/categories/{id}

**Descripción:** Elimina una categoría. Los productos asociados pierden su categoría (se ponen a null).  
**Autenticación:** JWT requerido (cualquier rol)

**Response:** `204 No Content`

**Errores:**
- `404` — Categoría no encontrada

**curl:**
```bash
curl -X DELETE http://87.99.156.164/api/v1/categories/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..."
```

---

## 8. BizAPI — Productos

### GET /api/v1/products

**Descripción:** Lista productos de la tienda. Filtrables por categoría y disponibilidad.  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/products`

**Headers:** `Authorization: Bearer <token>`

**Query Params (opcionales):**
- `categoryId` — UUID de categoría
- `availability` — `AVAILABLE` | `LOW_STOCK` | `CONSULT`

**Response 200:**
```json
[
  {
    "id": "uuid",
    "categoryId": "uuid",
    "categoryName": "Lámparas",
    "name": "Lámpara de Pared Vintage",
    "price": 89.90,
    "availability": "AVAILABLE",
    "allowDiscountInquiry": false,
    "position": 0,
    "primaryImageUrl": "https://..."
  }
]
```

**curl:**
```bash
curl "http://87.99.156.164/api/v1/products?availability=AVAILABLE" \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/products

**Descripción:** Crea un nuevo producto. Respeta el límite del plan (ESENCIAL=30, PRO=100, TOTAL=ilimitado).  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/products`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "string (requerido, max 200 chars)",
  "description": "string (opcional)",
  "price": 89.90,
  "availability": "AVAILABLE | LOW_STOCK | OUT_OF_STOCK | CONSULT (opcional — si se omite y se envía stock, se deriva automáticamente)",
  "stock": 5,
  "allowDiscountInquiry": false,
  "categoryId": "uuid (opcional)",
  "tags": ["string", "string"],
  "position": 0
}
```

**Sync `stock` ↔ `availability`** (change `product-stock-and-ai-draft` Fase A + fix V12):
- Si el caller envía **ambos**, `availability` del caller gana siempre.
- Si el caller envía **sólo `stock`** (sin `availability`):
    - `stock = null` → `availability = CONSULT`
    - `stock = 0`    → `availability = OUT_OF_STOCK` *(corregido en V12 — antes era LOW_STOCK, semánticamente incorrecto)*
    - `stock > 0`    → `availability = AVAILABLE`
- En `PUT`, si el caller omite `stock`, el valor actual del producto se preserva.

**Visibilidad por `availability`:**

| Valor | Aparece en `GET /products` | Aparece en `/catalog/by-category` | Aparece en `/suggestions` | Va al CSV WhatsApp (RF-51) |
|---|---|---|---|---|
| `AVAILABLE` | ✓ | ✓ | ✓ | `"in stock"` |
| `LOW_STOCK` | ✓ (si filtras) | ✗ | ✗ | `"in stock"` |
| `OUT_OF_STOCK` | ✓ (si filtras) | ✗ | ✗ | `"out of stock"` |
| `CONSULT` | ✓ (si filtras) | ✗ | ✗ | `"preorder"` |

**Response 201:**
```json
{
  "id": "uuid",
  "storeId": "uuid",
  "categoryId": "uuid",
  "categoryName": "Lámparas",
  "name": "Lámpara de Pared Vintage",
  "description": "Lámpara decorativa estilo vintage",
  "price": 89.90,
  "availability": "AVAILABLE",
  "stock": 5,
  "allowDiscountInquiry": false,
  "tags": ["decoracion", "vintage"],
  "position": 0,
  "active": true,
  "media": [],
  "discounts": [],
  "createdAt": "2025-04-15T18:30:00Z",
  "updatedAt": "2025-04-15T18:30:00Z"
}
```

**Errores:**
- `400` — Precio inválido (debe ser > 0)
- `403` — Límite de productos del plan alcanzado
- `422` — `stock` negativo (Bean Validation `@Min(0)`)

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/products \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lámpara de Pared Vintage",
    "description": "Estilo vintage, ideal para sala",
    "price": 89.90,
    "categoryId": "550e8400-e29b-41d4-a716-446655440000",
    "tags": ["decoracion", "vintage"],
    "availability": "AVAILABLE"
  }'
```

---

### GET /api/v1/products/{id}

**Descripción:** Retorna un producto completo con media y descuentos.  
**Autenticación:** JWT requerido (cualquier rol)

**Response 200:** objeto `ProductResponse` completo (con `media[]` y `discounts[]`).

**Errores:**
- `404` — Producto no encontrado

**curl:**
```bash
curl http://87.99.156.164/api/v1/products/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..."
```

---

### PUT /api/v1/products/{id}

**Descripción:** Reemplaza todos los campos editables del producto. También re-sincroniza en MongoDB (async).  
**Autenticación:** JWT requerido (cualquier rol)

**Request Body:** mismo formato que `POST /products` (todos los campos).

**Response 200:** objeto `ProductResponse` actualizado.

**curl:**
```bash
curl -X PUT http://87.99.156.164/api/v1/products/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"name": "Lámpara Vintage Premium", "price": 99.90, "availability": "LOW_STOCK"}'
```

---

### DELETE /api/v1/products/{id}

**Descripción:** Soft-delete — marca el producto como inactivo (`is_active=false`). No lo elimina físicamente.  
**Autenticación:** JWT requerido (cualquier rol)

**Response:** `204 No Content`

**curl:**
```bash
curl -X DELETE http://87.99.156.164/api/v1/products/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/products/{id}/deactivate

**Descripción:** Alias de soft-delete (mismo efecto que DELETE). Conveniente para clientes que prefieren POST.  
**Autenticación:** JWT requerido (cualquier rol)

**Response:** `204 No Content`

---

### GET /api/v1/products/{id}/media

**Descripción:** Lista toda la media (fotos/videos) de un producto.  
**Autenticación:** JWT requerido (cualquier rol)

**Response 200:**
```json
[
  {
    "id": "uuid",
    "url": "https://storage.vitria.io/img/producto-1.jpg",
    "mediaType": "IMAGE",
    "position": 0,
    "primary": true,
    "createdAt": "2025-04-15T18:30:00Z"
  }
]
```

---

### POST /api/v1/products/{id}/media

**Descripción:** Agrega un archivo de media al producto. La URL debe ser la URL final ya subida al storage (MinIO/S3). El backend solo guarda el metadato.  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/products/{id}/media`

**Request Body:**
```json
{
  "url": "string (requerido, max 500 chars)",
  "mediaType": "IMAGE | VIDEO (opcional, default: IMAGE)",
  "position": 0,
  "primary": false
}
```

**Response 201:** objeto `MediaResponse`.

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/products/550e8400-e29b-41d4-a716-446655440000/media \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://storage.vitria.io/img/lampara-vintage.jpg",
    "mediaType": "IMAGE",
    "position": 0,
    "primary": true
  }'
```

---

### DELETE /api/v1/products/{id}/media/{mediaId}

**Descripción:** Elimina un archivo de media del producto.  
**Autenticación:** JWT requerido (cualquier rol)

**Response:** `204 No Content`

**curl:**
```bash
curl -X DELETE \
  http://87.99.156.164/api/v1/products/prod-uuid/media/media-uuid \
  -H "Authorization: Bearer eyJ..."
```

---

### GET /api/v1/products/{id}/discounts

**Descripción:** Lista todos los descuentos del producto.  
**Autenticación:** JWT requerido (cualquier rol)

**Response 200:**
```json
[
  {
    "id": "uuid",
    "discountType": "PERCENTAGE",
    "value": 15.00,
    "startsAt": "2025-04-15T00:00:00Z",
    "endsAt": "2025-04-30T23:59:59Z",
    "active": true,
    "createdAt": "2025-04-15T18:30:00Z"
  }
]
```

---

### POST /api/v1/products/{id}/discounts

**Descripción:** Agrega un descuento al producto.  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/products/{id}/discounts`

**Request Body:**
```json
{
  "discountType": "PERCENTAGE | FIXED_AMOUNT (requerido)",
  "value": 15.00,
  "startsAt": "2025-04-15T00:00:00Z (requerido)",
  "endsAt": "2025-04-30T23:59:59Z (requerido)"
}
```

**Validaciones:**
- `value` debe ser > 0
- `endsAt` debe ser posterior a `startsAt` (validado en base de datos)

**Response 201:** objeto `DiscountResponse`.

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/products/prod-uuid/discounts \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "discountType": "PERCENTAGE",
    "value": 15.00,
    "startsAt": "2025-04-15T00:00:00Z",
    "endsAt": "2025-04-30T23:59:59Z"
  }'
```

---

### DELETE /api/v1/products/{id}/discounts/{discountId}

**Descripción:** Elimina un descuento del producto.  
**Autenticación:** JWT requerido (cualquier rol)

**Response:** `204 No Content`

**curl:**
```bash
curl -X DELETE \
  http://87.99.156.164/api/v1/products/prod-uuid/discounts/discount-uuid \
  -H "Authorization: Bearer eyJ..."
```

---

## 8.5. BizAPI — Catálogo agregado (Fase 8)

### GET /api/v1/catalog/by-category

**Descripción:** Devuelve los productos AVAILABLE y activos del store del JWT, **agrupados por las categorías que el merchant haya creado**. Usado por el flujo "Catálogo directo" del kiosko (cuando el cliente entra al catálogo sin pasar por la búsqueda conversacional), y también como preview en el panel del merchant.

**Autenticación:** JWT requerido (cualquier rol autenticado: `OWNER`, `SELLER`, `ADMIN`, `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/catalog/by-category`

**Headers:** `Authorization: Bearer <accessToken>`  
**Request:** sin query params, sin body. El store sale del JWT.

**Response 200:**
```json
[
  {
    "categoryId": "uuid-iluminacion",
    "categoryName": "Iluminación",
    "categorySlug": "iluminacion",
    "position": 0,
    "products": [
      {
        "id": "uuid-producto-1",
        "categoryId": "uuid-iluminacion",
        "categoryName": "Iluminación",
        "name": "Lámpara LED colgante negra industrial",
        "price": 89.00,
        "availability": "AVAILABLE",
        "allowDiscountInquiry": true,
        "position": 0,
        "primaryImageUrl": "https://...",
        "brand": "Philips"
      },
      { "id": "...", "...": "..." }
    ]
  },
  {
    "categoryId": "uuid-electrico",
    "categoryName": "Eléctrico",
    "categorySlug": "electrico",
    "position": 1,
    "products": [ /* ... */ ]
  }
]
```

**Reglas del response:**

- **Top-level: array de categorías**. Si el store no tiene productos AVAILABLE, devuelve `[]`.
- **Filtros de productos**:
    - Solo `availability == AVAILABLE` (LOW_STOCK y CONSULT se omiten en este endpoint).
    - Solo productos activos (`isActive=true`).
    - Solo productos con `categoryId != null` (productos sin categoría NO se incluyen).
- **Categorías que se omiten**:
    - Categorías sin ningún producto AVAILABLE no aparecen en el array.
    - Categorías inactivas no aparecen.
- **Orden**:
    - Categorías: por `position ASC, name ASC`.
    - Productos dentro de cada categoría: por `created_at ASC` (más antiguo primero).
- **Multi-tenant**: filtrado por `storeId` del JWT. Nunca se mezclan productos de otras tiendas.

**Campos del producto** (`ProductSummaryResponse` — reutilizado del listado plano `/api/v1/products`):

| Campo | Tipo | Notas |
|---|---|---|
| `id` | UUID | Id del producto. |
| `categoryId` | UUID | Id de la categoría a la que pertenece. |
| `categoryName` | string | Nombre humano de la categoría. |
| `name` | string | Nombre del producto. |
| `price` | BigDecimal | Precio en soles. |
| `availability` | AvailabilityStatus | Siempre `"AVAILABLE"` en este endpoint. |
| `allowDiscountInquiry` | boolean | Si el cliente puede preguntar por descuentos. |
| `position` | int | Orden dentro de la categoría (intención del merchant). |
| `primaryImageUrl` | string \| null | URL de la imagen principal. |
| `brand` | string \| null | Marca del producto. Nullable. |

**Errores:**
- `401` — JWT ausente o inválido.

**Notas para el frontend:**

- Diseñado para pantallas con scroll vertical de varias categorías en cards apilables.
- Si el array viene vacío (`[]`) → mostrar empty state ("Esta tienda aún no tiene productos disponibles").
- El campo `brand` es opcional — algunos productos no lo tendrán. Útil para futuros filtros de marca en el sidebar.
- Si la cantidad de productos por categoría es grande, considerar paginación en cliente; el backend devuelve todo lo AVAILABLE.

**curl:**
```bash
curl -X GET http://87.99.156.164/api/v1/catalog/by-category \
  -H "Authorization: Bearer eyJ..."
```

---

## 8.6. BizAPI — Productos sugeridos (Fase 10)

### GET /api/v1/products/{id}/suggestions

**Descripción:** Devuelve hasta 5 productos complementarios al producto base (mockup "Detalle de producto" del kiosko → sección "Podrías acompañar este producto con…"). Heurística cascading en 4 caminos: (1) LLM Groq genera keywords complementarios y busca productos del store cuyos campos los matcheen; (2) si vacío, productos de la misma categoría del base; (3) si <5, completa con productos populares del store; (4) si el store solo tiene el base, `[]`. El campo `source` indica qué camino contribuyó al resultado.

**Autenticación:** JWT requerido (cualquier rol autenticado: `OWNER`, `SELLER`, `ADMIN`, `DEVICE`)
**URL producción:** `http://87.99.156.164/api/v1/products/{id}/suggestions`

**Headers:** `Authorization: Bearer <accessToken>`
**Path params:**
- `id` — UUID del producto base.

**Request:** sin query params, sin body. El `storeId` sale del JWT.

**Response 200 (happy path con LLM):**
```json
{
  "productId": "b2c1d3cc-a91d-4a2b-b134-faba31325300",
  "suggestions": [
    {
      "id": "uuid-brocas",
      "categoryId": "uuid-herramientas",
      "categoryName": "Herramientas",
      "name": "Brocas SDS Plus 6mm",
      "price": 25.00,
      "availability": "AVAILABLE",
      "allowDiscountInquiry": true,
      "position": 3,
      "primaryImageUrl": "https://...",
      "brand": "Bosch"
    },
    {
      "id": "uuid-disco",
      "name": "Disco de corte 4.5\"",
      "...": "..."
    }
  ],
  "source": "llm"
}
```

**Reglas del response:**

- **`productId`**: id del producto base (eco del path param).
- **`suggestions`**: lista de **0 a 5** productos. Reusa `ProductSummaryResponse` (mismo shape que `/products` y `/catalog/by-category`).
- **El producto base NUNCA aparece en `suggestions`** — siempre se excluye.
- **Dedup por id** entre caminos: si LLM y "misma categoría" devuelven el mismo producto, aparece una sola vez.
- **Filtros**: cada producto sugerido es `availability="AVAILABLE"` AND `isActive=true`.
- **Multi-tenant**: todos los productos son del **mismo store** del producto base (= store del JWT).
- **`source`**: indica qué camino del cascading contribuyó al resultado. Valores legales:

| Valor | Significado |
|---|---|
| `"llm"` | El LLM Groq matcheó productos del catálogo. |
| `"same-category"` | Fallback a la misma categoría del producto base. |
| `"store-popular"` | Fallback a productos populares del store (cualquier categoría). |
| `"mixed"` | Combinación de 2 o más caminos anteriores. |
| `"empty"` | El store solo tiene el producto base. `suggestions=[]`. El frontend oculta la sección. |

**Errores:**
- `401` — JWT ausente o inválido.
- `404` — `RESOURCE_NOT_FOUND`: el producto no existe o pertenece a otro store (multi-tenant).
- `400` — `BAD_REQUEST`: el path param no es un UUID válido.

**Fail-open**: si el LLM Groq falla (timeout, 5xx, JSON malformado), el endpoint NO devuelve 5xx. Loguea un WARN y procede al Camino 2 (misma categoría). El cliente nunca ve un error de LLM.

**Notas para el frontend:**

- El campo `source` es informativo. Si solo querés renderizar/ocultar la sección, mirá si `suggestions` es vacío.
- Si `source === "empty"` → ocultar la sección "Podrías acompañar".
- Si `source === "store-popular"` y querés un copy distinto ("Otros productos del store"), podés diferenciarlo. Opcional.
- Cada `suggestions[*]` trae `brand` y `tags` para filtrado client-side futuro (consistente con `/conversations/search` Fase 9).

**curl:**
```bash
curl -X GET http://87.99.156.164/api/v1/products/b2c1d3cc-a91d-4a2b-b134-faba31325300/suggestions \
  -H "Authorization: Bearer eyJ..."
```

---

## 8.7. BizAPI — Borrador IA del producto (Fase 11)

### POST /api/v1/products/draft-from-image

**Descripción:** Recibe una foto en multipart y devuelve descripción + categoría sugerida + tags generados por el LLM Groq multimodal (Llama 4 Scout). Alimenta el formulario "Registro de producto" del mockup del front. **NO crea el producto** y **NO sube la imagen a ningún storage** — la imagen se procesa en memoria y se descarta cuando el response sale. El producto se crea luego con `POST /api/v1/products`.

**Autenticación:** JWT requerido. Roles permitidos: `OWNER`, `SELLER`, `ADMIN`.
**URL producción:** `http://87.99.156.164/api/v1/products/draft-from-image`

**Headers:** `Authorization: Bearer <accessToken>` · `Content-Type: multipart/form-data`

**Form Part:**
- `image` — archivo `image/jpeg` / `image/png` / `image/webp`. Tamaño máximo **5 MB**.

**Response 200 (happy path):**
```json
{
  "description": "Foco LED de 9W con luz cálida 2700K y rosca E27. Ideal para iluminación de hogar.",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "tags": ["foco-led", "luz-calida", "e27"],
  "source": "groq"
}
```

**Reglas del response:**

- **`description`** — string en español, nunca null (vacío = el LLM no pudo describir).
- **`categoryId`** — UUID de una categoría existente del store del caller, o `null` si el LLM no reconoció ninguna. **Defense-in-depth**: doble validación (gateway + service) contra el conjunto de categorías del store; categorías ajenas se filtran a `null`.
- **`tags`** — subset del catálogo cerrado por `(shopping_center_id × store_category)` del caller. Tags inventados se filtran defensivamente.
- **`source`** — `"groq"` cuando el LLM activo respondió, `"stub"` cuando se usó el provider stub (dev o fail-open).

**Fail-open:** si el LLM Groq falla (timeout, 5xx, JSON malformado, API key inválida), el endpoint **siempre responde 200** con `{description:"", categoryId:null, tags:[], source:"stub"}`. El front muestra los campos vacíos y el comerciante llena a mano — sin error visible al usuario.

**Errores:**
- `400 MULTIPART_INVALID` — `image` ausente, vacío, content-type no es `image/*`, o tamaño > 5 MB.
- `400 BAD_REQUEST` — JWT no lleva tienda activa (hacer `POST /auth/switch-store/{storeId}` antes).
- `401` — JWT ausente o inválido.
- `403 FORBIDDEN` — rol distinto a `OWNER`/`SELLER`/`ADMIN` (ej. `DEVICE`).

**Notas para el frontend:**

- El front carga la primera foto LOCAL (`URL.createObjectURL` en blob), llama este endpoint, pinta los campos del formulario con la respuesta. Las fotos adicionales que el usuario cargue después NO disparan este endpoint a menos que el usuario presione "Generar con IA" sobre otra foto.
- La imagen NO queda persistida en el backend — el front mantiene el blob local hasta que el usuario haga "Guardar". El upload final a storage llegará en **Fase B** del change (`product-stock-and-ai-draft`).
- El campo `tags` es subset CERRADO del catálogo del mall × rubro: si el front quiere mostrar chips para que el comerciante elimine alguno antes de guardar, todos los tags son válidos sin re-validar.

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/products/draft-from-image \
  -H "Authorization: Bearer eyJ..." \
  -F "image=@./foco.jpg;type=image/jpeg"
```

---

## 8.8. BizAPI — Regenerar descripción (stateless, Fase 11)

### POST /api/v1/products/regenerate-description

**Descripción:** Recibe una descripción y devuelve una **paráfrasis en español** preservando todos los hechos (especificaciones, marca, modelo, dimensiones, garantía, voltaje). El LLM Groq Llama 3.3 70B (text) está instruido para NO inventar facts nuevos.

**Stateless** — NO requiere `productId`. Sirve tanto para producto en draft (antes de guardar, mockup "Registro de producto") como para edición de producto ya existente. El front decide cuándo persistir el resultado con `PUT /products/{id}` o como parte del `POST /products/with-images`.

**Autenticación:** JWT requerido. Roles: `OWNER`, `SELLER`, `ADMIN`.
**URL producción:** `http://87.99.156.164/api/v1/products/regenerate-description`

**Headers:** `Authorization: Bearer <accessToken>` · `Content-Type: application/json`

**Request Body:**
```json
{
  "currentDescription": "Foco LED 9W luz cálida E27 para iluminación de hogar."
}
```

Validaciones:
- `currentDescription` requerido (`@NotBlank`), max 2000 caracteres.

**Response 200 (happy path):**
```json
{
  "description": "Bombilla LED de 9W con rosca E27 y luz cálida, ideal para iluminar tu hogar."
}
```

**Fail-open:** si el LLM no está disponible, throws, o devuelve vacío/blanco, el endpoint **siempre responde 200** con `{description: <currentDescription original>}` (echo). El front no ve cambio pero tampoco un error.

**Errores:**
- `422 VALIDATION_ERROR` — `currentDescription` vacío, blank, o > 2000 chars.
- `401` — JWT ausente.
- `403 FORBIDDEN` — rol distinto a `OWNER`/`SELLER`/`ADMIN`.

**curl:**
```bash
curl -X POST "http://87.99.156.164/api/v1/products/regenerate-description" \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"currentDescription":"Foco LED 9W luz cálida E27 para iluminación de hogar."}'
```

**NOTA — breaking change menor (2026-06-29):** este endpoint inicialmente se diseñó como `POST /api/v1/products/{id}/regenerate-description` (con path param). Se quitó el `{id}` para soportar el flujo del mockup "Registro de producto" (regenerar antes de guardar). El path actual es **`/products/regenerate-description`** sin id.

---

## 8.9. BizAPI — Crear producto con imágenes en multipart (Fase 12)

### POST /api/v1/products/with-images

**Descripción:** Crea un producto y sube N imágenes al storage configurado (Cloudflare R2 en producción) en **una sola transacción atómica**. El frontend nunca toca el bucket — el backend recibe el multipart, valida, sube las fotos, crea el producto y persiste las URLs. Si CUALQUIER upload falla, rollback completo (no se crea producto huérfano sin sus imágenes).

**Autenticación:** JWT requerido. Roles: `OWNER`, `SELLER`, `ADMIN`.
**URL producción:** `http://87.99.156.164/api/v1/products/with-images`

**Headers:** `Authorization: Bearer <accessToken>` · `Content-Type: multipart/form-data`

**Parts del multipart:**

- **`product`** (1, JSON) — body de un `ProductRequest` válido (los mismos campos que `POST /products` sección 8.2: `name`, `description`, `price`, `availability`, `stock`, `allowDiscountInquiry`, `categoryId`, `tags`, `brand`, `position`). La sync `stock ↔ availability` del change Fase A aplica igual.
- **`images`** (1 a 10, binario) — cada part es un archivo `image/jpeg` / `image/png` / `image/webp`. Tamaño máximo **5 MB por imagen**. El request multipart total máximo **60 MB**.

El orden de los parts `images` determina el orden de `position` en la BD: el primero queda con `position=0` y `primary=true`; los siguientes con `position=1..N` y `primary=false`.

**Response 201 (happy path):**
```json
{
  "id": "uuid",
  "storeId": "uuid",
  "categoryId": "uuid",
  "categoryName": "Iluminación",
  "name": "Foco LED Smart 12W RGB",
  "description": "...",
  "price": 39.90,
  "availability": "AVAILABLE",
  "stock": 5,
  "allowDiscountInquiry": false,
  "tags": ["led", "rgb"],
  "brand": "Philips",
  "position": 0,
  "active": true,
  "media": [
    { "id": "uuid-1", "url": "https://<r2-public>/products/<storeId>/<productId>/<mediaId-1>.jpg", "mediaType": "IMAGE", "position": 0, "primary": true, "createdAt": "2026-06-29T..." },
    { "id": "uuid-2", "url": "https://<r2-public>/products/<storeId>/<productId>/<mediaId-2>.jpg", "mediaType": "IMAGE", "position": 1, "primary": false, "createdAt": "2026-06-29T..." }
  ],
  "discounts": [],
  "createdAt": "2026-06-29T...",
  "updatedAt": "2026-06-29T..."
}
```

**Reglas y errores:**

- `400 MULTIPART_INVALID` — falta el part `product`, falta `images`, content-type de una imagen no es `image/*`, una imagen vacía, o más de 10 imágenes.
- `400 MULTIPART_INVALID` — request total > 60 MB o una imagen > 5 MB (Spring `MaxUploadSizeExceededException`).
- `422 VALIDATION_ERROR` — el `product` part falla Bean Validation (precio ≤ 0, name vacío, stock negativo, currentDescription > 2000, etc.). NO se sube ninguna imagen.
- `402 PLAN_LIMIT_EXCEEDED` — la tienda alcanzó el límite de productos del plan. NO se sube ninguna imagen.
- `503 STORAGE_UNAVAILABLE` — R2 down, credenciales inválidas, timeout. Rollback total del producto + media.
- `401` — JWT ausente.
- `403 FORBIDDEN` — rol `DEVICE` u otro fuera de `OWNER`/`SELLER`/`ADMIN`.

**Atomicidad:**
- Si la 1ra foto se sube OK y la 2da falla → todo rollback. El producto NO se persiste. Los objetos en R2 que alcanzaron a subirse quedan como huérfanos (tech debt: scheduled job futuro que limpia).
- Si validation falla antes de iterar imágenes → ningún upload, ningún DB write.

**curl:**
```bash
curl -X POST "http://87.99.156.164/api/v1/products/with-images" \
  -H "Authorization: Bearer eyJ..." \
  -F 'product={"name":"Foco LED Smart","price":39.90,"stock":5,"categoryId":"550e8400-e29b-41d4-a716-446655440000","tags":["led","rgb"]};type=application/json' \
  -F "images=@./foco1.jpg;type=image/jpeg" \
  -F "images=@./foco2.jpg;type=image/jpeg"
```

---

## 8.10. BizAPI — Adjuntar imágenes a un producto existente (Fase 12)

### POST /api/v1/products/{id}/images

**Descripción:** Adjunta 1..N imágenes adicionales a un producto existente del store del caller. Las nuevas posiciones continúan desde el max actual (`primary=false` para todas — la foto primaria no cambia). Total final no puede exceder el límite configurado (default 10 por producto). Para edición desde el panel del comerciante.

**Autenticación:** JWT requerido. Roles: `OWNER`, `SELLER`, `ADMIN`.
**URL producción:** `http://87.99.156.164/api/v1/products/{id}/images`

**Headers:** `Authorization: Bearer <accessToken>` · `Content-Type: multipart/form-data`
**Path params:**
- `id` — UUID del producto. Debe pertenecer al store del caller (multi-tenant).

**Parts del multipart:**
- **`images`** (1 a N, binario) — mismas reglas de content-type / tamaño que `/products/with-images` (5 MB por imagen, request total 60 MB).

**Response 201:**
```json
[
  { "id": "uuid-a", "url": "https://<r2-public>/...", "mediaType": "IMAGE", "position": 3, "primary": false, "createdAt": "..." },
  { "id": "uuid-b", "url": "https://<r2-public>/...", "mediaType": "IMAGE", "position": 4, "primary": false, "createdAt": "..." }
]
```

**Errores:**
- `400 MULTIPART_INVALID` — `images` vacío, content-type inválido, request > 60 MB, imagen > 5 MB.
- `400 BAD_REQUEST` — la suma `medias actuales + nuevas` excede el límite (default 10) por producto.
- `404 RESOURCE_NOT_FOUND` — producto no existe en el store del caller (404 también si existe en otro store — no leak).
- `503 STORAGE_UNAVAILABLE` — R2 falla. Rollback de la fila(s) `ProductMedia` que se hubieran persistido.
- `401` — JWT ausente.
- `403 FORBIDDEN` — rol no autorizado.

**curl:**
```bash
curl -X POST "http://87.99.156.164/api/v1/products/<id>/images" \
  -H "Authorization: Bearer eyJ..." \
  -F "images=@./extra1.jpg;type=image/jpeg" \
  -F "images=@./extra2.jpg;type=image/jpeg"
```

---

## 9. BizAPI — Órdenes (carrito del kiosko, Fase 13)

### POST /api/v1/orders

**Descripción:** El dispositivo kiosk crea una orden con **N items** (carrito) más los datos del cliente final (nombre + celular). Estado inicial: `PENDING`. El backend valida cada producto (existe en el store, está activo, `availability=AVAILABLE`), calcula el `subtotal`/`total` **server-side** con snapshot del precio actual, y persiste todo en una transacción atómica.

**Autenticación:** JWT requerido (rol: `DEVICE`)
**URL producción:** `http://87.99.156.164/api/v1/orders`

**Headers:** `Authorization: Bearer <deviceAccessToken>` · `Content-Type: application/json`

**Request Body:**
```json
{
  "customerName":  "Juan Perez",
  "customerPhone": "987654321",
  "items": [
    {"productId": "uuid-1", "quantity": 2},
    {"productId": "uuid-2", "quantity": 1}
  ],
  "sessionId":    "sess-abc-123",
  "customerNote": "Quiero verlo antes de pagar"
}
```

**Validaciones (Bean Validation):**
- `customerName` — `@NotBlank`, max 200 caracteres.
- `customerPhone` — `@NotBlank`, formato peruano móvil (regex `^9\d{8}$` — exactamente 9 dígitos empezando en 9; sin espacios). El front debe normalizar antes de mandar.
- `items` — `@NotEmpty`, max 20 entries. Cada item: `productId` requerido, `quantity` entre 1 y 99.
- `sessionId` — opcional, max 200 caracteres.
- `customerNote` — opcional, sin límite.
- **No duplicados**: el mismo `productId` repetido en `items[]` rechazado con 400 (el front debe sumar al qty del item existente).

**Response 201:**
```json
{
  "id": "uuid",
  "storeId": "uuid",
  "status": "PENDING",
  "source": "ASSISTANT",
  "customerName": "Juan Perez",
  "customerPhone": "987654321",
  "subtotal": 250.00,
  "total": 250.00,
  "items": [
    {
      "id": "item-uuid-1",
      "productId": "uuid-1",
      "productName": "Taladro Percutor Bosch GSB 13 RE",
      "quantity": 2,
      "unitPriceSnapshot": 100.00,
      "lineTotal": 200.00
    },
    {
      "id": "item-uuid-2",
      "productId": "uuid-2",
      "productName": "Brocas SDS Plus 6mm",
      "quantity": 1,
      "unitPriceSnapshot": 50.00,
      "lineTotal": 50.00
    }
  ],
  "sessionId": "sess-abc-123",
  "customerNote": "Quiero verlo antes de pagar",
  "rejectionReason": null,
  "createdAt": "2026-06-29T20:00:00Z",
  "updatedAt": "2026-06-29T20:00:00Z",
  "lastStatusChangedAt": "2026-06-29T20:00:00Z"
}
```

**Reglas del response:**
- `subtotal` = SUM(`item.quantity × item.unitPriceSnapshot`) — calculado server-side, NUNCA recalculado después.
- `total` = `subtotal` por ahora (columna separada para futuro de descuentos / envío).
- `items[*].unitPriceSnapshot` — snapshot del `product.price` al momento de crear. NO cambia si el comerciante actualiza el precio después.
- `items[*].productName` — nombre actual del producto del entity (NO snapshot inmutable — si el comerciante renombra, las órdenes viejas muestran el nombre nuevo).

**Errores:**
- `400 BAD_REQUEST` — `productId` duplicado en items, producto inactivo, o producto con `availability != AVAILABLE`.
- `403 FORBIDDEN` — JWT no es DEVICE.
- `404 RESOURCE_NOT_FOUND` — algún `productId` no pertenece al store del DEVICE.
- `422 VALIDATION_ERROR` — `customerName`/`customerPhone` faltan o formato inválido, `items` empty, `quantity` fuera de [1,99], items > 20.

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/orders \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "customerName":"Juan Perez",
    "customerPhone":"987654321",
    "items":[
      {"productId":"550e8400-e29b-41d4-a716-446655440000","quantity":2},
      {"productId":"550e8400-e29b-41d4-a716-446655440001","quantity":1}
    ],
    "sessionId":"sess-abc-123"
  }'
```

---

### GET /api/v1/orders

**Descripción:** Lista órdenes de la tienda, paginadas. Filtrables por `status` y/o `customerPhone` (lookup cuando el cliente llama de vuelta).
**Autenticación:** JWT requerido (roles: `OWNER`, `SELLER`, `ADMIN`)
**URL producción:** `http://87.99.156.164/api/v1/orders`

**Headers:** `Authorization: Bearer <accessToken>`

**Query Params:**
- `status` — uno o **varios separados por coma**: `PENDING` | `CONFIRMED` | `REJECTED` | `CANCELLED` | `EXPIRED` (opcional; ausente = todos). Ej. la tab "Rechazados" usa `status=REJECTED,EXPIRED`.
- `q` — **búsqueda** por nombre del cliente (parcial, case-insensitive) o por prefijo del `orderCode` (opcional). Si viene junto con `customerPhone`, **`q` gana**.
- `customerPhone` — celular del cliente (opcional, formato `9XXXXXXXX`, match exacto)
- `page` — número de página (default: 0)
- `size` — tamaño de página (default: 20)
- `sort` — campo,dirección (default: `createdAt,DESC`)

> **Badge de pendientes**: usar `?status=PENDING&size=1` y leer `totalElements` (no hay endpoint de count dedicado).

**Response 200 (shape compacto — sin items[] para mantener payload chico):**
```json
{
  "content": [
    {
      "id": "uuid",
      "storeId": "uuid",
      "status": "PENDING",
      "source": "ASSISTANT",
      "customerName": "Juan Perez",
      "customerPhone": "987654321",
      "itemCount": 2,
      "total": 250.00,
      "sessionId": "sess-123",
      "orderCode": "859653",
      "expiresAt": "2026-06-29T21:00:00Z",
      "rejectionReason": null,
      "createdAt": "2026-06-29T20:00:00Z",
      "lastStatusChangedAt": "2026-06-29T20:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

Para ver el detalle con `items[]` completo, usar `GET /orders/{id}`.

**curl:**
```bash
# Solo PENDING
curl "http://87.99.156.164/api/v1/orders?status=PENDING&size=10" \
  -H "Authorization: Bearer eyJ..."

# Buscar por cliente (caso: cliente llama de vuelta)
curl "http://87.99.156.164/api/v1/orders?customerPhone=987654321" \
  -H "Authorization: Bearer eyJ..."

# Tab "Rechazados" (rechazados manualmente + expirados por TTL)
curl "http://87.99.156.164/api/v1/orders?status=REJECTED,EXPIRED" \
  -H "Authorization: Bearer eyJ..."

# Búsqueda "por nombre o código" de la pantalla Solicitudes
curl "http://87.99.156.164/api/v1/orders?q=mari" \
  -H "Authorization: Bearer eyJ..."
```

---

### GET /api/v1/orders/{id}

**Descripción:** Retorna el detalle completo de una orden, con `items[]`, customer fields y totales. Desde la Fase 16 incluye además `orderCode`, `expiresAt`, y por cada item `productStock` + `productAvailability` (stock vivo del producto, para el warning "quedan pocos").
**Autenticación:** JWT requerido (roles: `OWNER`, `SELLER`, `ADMIN`)

**Response 200:** mismo shape que el response 201 de `POST /orders` (objeto `OrderResponse` completo con `items[]`).

**Errores:**
- `404` — Orden no encontrada (también si pertenece a otro store — no leak)

**curl:**
```bash
curl http://87.99.156.164/api/v1/orders/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/orders/{id}/confirm

**Descripción:** Confirma la orden (transición `PENDING → CONFIRMED`). **Decrementa el stock** de cada producto por la cantidad pedida en su item. Si algún producto tiene `stock != null` y `stock < quantity` → 422 `STOCK_INSUFFICIENT` y la orden queda en `PENDING` (rollback total, no decremento parcial). Productos con `stock=null` (CONSULT) no se tocan — el comerciante maneja manualmente.

Cuando el decremento lleva `stock` a 0, la sync de V12 setea `availability=OUT_OF_STOCK` automáticamente (el producto deja de aparecer en catálogo público).

**Autenticación:** JWT requerido (roles: `OWNER`, `SELLER`, `ADMIN`)
**URL producción:** `http://87.99.156.164/api/v1/orders/{id}/confirm`

**Headers:** `Authorization: Bearer <accessToken>`
**Request Body:** ninguno

**Response 200:** objeto `OrderResponse` con `status: "CONFIRMED"`.

**Errores:**
- `404` — Orden no encontrada
- `422 STOCK_INSUFFICIENT` — algún item requiere más stock del disponible. Response extra fields: `productId`, `productName`, `available`, `requested`. La orden queda PENDING.
- `422 INVALID_STATE_TRANSITION` — la orden ya está terminal (CONFIRMED/REJECTED/CANCELLED).

**Ejemplo response 422 `STOCK_INSUFFICIENT`:**
```json
{
  "type": "/errors/stock-insufficient",
  "title": "Stock Insufficient",
  "status": 422,
  "detail": "Stock insuficiente para 'Foco LED Bosch 9W': disponible 3, requerido 10.",
  "errorCode": "STOCK_INSUFFICIENT",
  "productId": "550e8400-...",
  "productName": "Foco LED Bosch 9W",
  "available": 3,
  "requested": 10,
  "timestamp": "2026-06-29T20:05:00Z"
}
```

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/orders/550e8400-e29b-41d4-a716-446655440000/confirm \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/orders/{id}/reject

**Descripción:** Rechaza la orden (transición `PENDING → REJECTED`). Acepta motivo opcional.  
**Autenticación:** JWT requerido (roles: `OWNER`, `SELLER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/orders/{id}/reject`

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body (opcional):**
```json
{
  "reason": "string (opcional — motivo del rechazo)"
}
```

**Response 200:** objeto `OrderResponse` con `status: "REJECTED"`.

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/orders/550e8400-e29b-41d4-a716-446655440000/reject \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"reason": "No hay stock disponible en este momento"}'
```

---

### GET /api/v1/orders/{id}/history

**Descripción:** Retorna el historial completo de transiciones de estado de la orden, ordenado cronológicamente.  
**Autenticación:** JWT requerido (roles: `OWNER`, `SELLER`, `ADMIN`)

**Response 200:**
```json
[
  {
    "id": "uuid",
    "orderId": "uuid",
    "oldStatus": null,
    "newStatus": "PENDING",
    "changedBy": "DEVICE:device-uuid",
    "reason": null,
    "changedAt": "2025-04-15T20:00:00Z"
  },
  {
    "id": "uuid",
    "orderId": "uuid",
    "oldStatus": "PENDING",
    "newStatus": "CONFIRMED",
    "changedBy": "user-uuid",
    "reason": null,
    "changedAt": "2025-04-15T20:05:00Z"
  }
]
```

**Nota:** `oldStatus: null` en la primera entrada (creación).

**curl:**
```bash
curl http://87.99.156.164/api/v1/orders/550e8400-e29b-41d4-a716-446655440000/history \
  -H "Authorization: Bearer eyJ..."
```

---

## 9.4. BizAPI — Expiración de pedidos y pantalla "Solicitudes" (Fase 16)

Soporte backend de la pantalla **Solicitudes de compra** de la app Android del staff
(change `orders-expiry-and-requests-screen`).

### Expiración automática

Un pedido creado por la tablet nace `PENDING` con `expiresAt = createdAt + TTL`
(`ORDERS_TTL_MINUTES`, default **60 min**). Un job del backend corre **cada minuto** y
transiciona a **`EXPIRED`** los `PENDING` vencidos:

- Registra la transición en `GET /orders/{id}/history` (`changedBy: "system:expiry"`).
- Emite push **`ORDER_TAKEN` con `status=EXPIRED`** → la app saca el pedido de "Pendientes"
  y lo muestra en "Rechazados" (por expirado) sin refresh manual.
- **El staff siempre gana la carrera**: si un vendedor confirma/rechaza en la misma ventana,
  el job no toca nada (UPDATE condicional).
- `EXPIRED` es **terminal**: `confirm`/`reject` sobre un expirado devuelven `422 INVALID_STATE_TRANSITION`.

El **countdown** ("Expira en 20 minutos") lo renderiza la app con `expiresAt`; el servidor
es la autoridad final del vencimiento.

### POST /api/v1/orders/{id}/recover

**Descripción:** revive un pedido **`EXPIRED`** devolviéndolo a `PENDING` con un `expiresAt`
nuevo (TTL completo). Emite push `NEW_ORDER`: reaparece en "Pendientes" de todos los celulares.
No toca stock (el stock solo se decrementa al confirmar).
**Autenticación:** JWT (roles: `OWNER`, `SELLER`, `ADMIN`) — DEVICE → 403.

**Response 200:** `OrderResponse` con `status: "PENDING"` y `expiresAt` renovado.

**Errores:**
- `422 INVALID_STATE_TRANSITION` — el pedido no está `EXPIRED` (un **rechazo manual no se deshace**).
- `404 RESOURCE_NOT_FOUND` — la orden no es de la tienda del token.
- `403` — rol DEVICE.

```bash
curl -X POST "http://87.99.156.164/api/v1/orders/<uuid>/recover" \
  -H "Authorization: Bearer eyJ..."
```

### Campos nuevos para la pantalla

| Campo | Dónde | Para qué |
|---|---|---|
| `orderCode` | summary + detalle | "Código: 859653" en la card. 6 dígitos únicos por tienda. **Null en pedidos previos a V15** → la app necesita fallback |
| `expiresAt` | summary + detalle | Countdown "Expira en X minutos" |
| `rejectionReason` | summary (ya estaba en detalle) | Motivo en las cards de la tab Rechazados |
| `productStock` / `productAvailability` | items del detalle | Warning "Quedan pocos disponibles" (umbral client-side; `productStock: null` = modo consultar) |

### Notas para el front

- **Estados de las tabs**: Pendientes → `status=PENDING`; Aceptados → `status=CONFIRMED`; Rechazados → `status=REJECTED,EXPIRED`.
- **Doble confirmación**: si dos vendedores tocan "Confirmar" a la vez, el segundo recibe `422 INVALID_STATE_TRANSITION` — mostrar "otro vendedor ya atendió este pedido" y refrescar.
- **Stock insuficiente al confirmar**: puede aparecer entre el warning y el tap → manejar el ProblemDetail `STOCK_INSUFFICIENT`.
- El push es el *acelerador*: la fuente de verdad sigue siendo `GET /orders` al abrir/refrescar la pantalla.

---

## 9.5. BizAPI — Compartir carrito por WhatsApp (Qhatia, Fase 14)

### POST /api/v1/carts/share-pdf

**Descripción:** El botón verde "Compartir por WhatsApp" del carrito del kiosko. Genera un **PDF cotización** con los productos del carrito (foto + descripción + cantidad + **precio unitario, subtotal y total referencial**) + datos de la tienda (nombre + UBICACIÓN/stand) + CTA al comerciante, lo sube a R2 (`Cotizacion-Qhathia.pdf`), y lo envía al **WhatsApp del cliente** a través de **Qhatia/Qhathia** (el bot de Vitria, Meta Cloud API). **NO crea orden** (eso es "Realizar pedido"). Los precios son **referenciales** — el precio final se confirma en la tienda.

**Autenticación:** JWT requerido (rol: `DEVICE` — la tablet).
**URL producción:** `http://87.99.156.164/api/v1/carts/share-pdf`

**Headers:** `Authorization: Bearer <deviceAccessToken>` · `Content-Type: application/json`

**Request Body:**
```json
{
  "customerName":  "Juan Perez",
  "customerPhone": "987654321",
  "items": [
    { "productId": "uuid-1", "quantity": 2 },
    { "productId": "uuid-2", "quantity": 1 }
  ]
}
```

Validaciones (mismas que `POST /orders`):
- `customerName` `@NotBlank` max 200
- `customerPhone` `^9\d{8}$` (peruano móvil)
- `items` 1 a 20, `quantity` 1 a 99, sin `productId` duplicados
- Cada producto: del store del DEVICE, `active`, `availability=AVAILABLE`

**Response 201:**
```json
{
  "status": "SENT",
  "messageId": "wamid.HBgLNTE5...",
  "pdfUrl": "https://pub-<hash>.r2.dev/carts/<uuid>.pdf",
  "error": null
}
```

**El campo `status`:**

| Valor | Significado |
|---|---|
| `SENT` | Meta aceptó el mensaje. El cliente recibe el PDF en su WhatsApp. `messageId` presente. |
| `STUB` | Provider stub (dev/test). El PDF se generó y subió pero NO se envió por WhatsApp. |
| `FAILED` | El envío falló (número no registrado en test mode, template no aprobado, etc.). **El PDF SÍ se generó** — el front puede usar `pdfUrl` como fallback. |

**Fail-soft:** el `pdfUrl` SIEMPRE está presente en un 201 (el PDF se genera y sube antes de intentar el envío). Solo si la generación/subida del PDF falla el endpoint devuelve **503 `PDF_GENERATION_FAILED`**.

**Contenido del PDF (formato cotización):**
- Encabezado: marca **Qhathia**, "COTIZACIÓN SIN COMPROMISO" + fecha; franja con **TIENDA** (nombre), **UBICACIÓN** (el campo `location`/stand de la tienda, ej. "AZ-104"; la `address` de calle como línea secundaria) y **PARA** (nombre del cliente).
- Tabla de ítems (miniatura + nombre + descripción + **P. Unit + Cant + Subtotal**), con **TOTAL REFERENCIAL** y disclaimer "el precio final se confirma en la tienda".
- Bloque de cierre con botón clickeable **"Escríbeles por WhatsApp"**. Paginación con "Página X de Y".

**Errores:**
- `400 BAD_REQUEST` — producto duplicado, inactivo, o no AVAILABLE.
- `404 RESOURCE_NOT_FOUND` — algún `productId` no es del store del DEVICE.
- `422 VALIDATION_ERROR` — customer/items inválidos.
- `503 PDF_GENERATION_FAILED` — no se pudo generar/subir el PDF.
- `403 FORBIDDEN` — rol distinto a DEVICE.
- `401` — sin JWT.

**Notas para el front:**
- El botón "Escríbeles por WhatsApp" del PDF abre `wa.me/51<whatsapp>` con un **texto pre-cargado que incluye el link del propio PDF** (wa.me no adjunta archivos, solo texto → así la tienda recibe la cotización al abrir el chat).
- El WhatsApp del comerciante se toma del campo `whatsapp` de la tienda (o `phone` como fallback). La **UBICACIÓN/stand** sale del campo `location`. El comerciante los carga en `PUT /api/v1/stores/{id}` (auth-api).
- En **test mode de Meta** (sin business verification), solo se puede enviar a números **pre-registrados como test recipient** (máx 5). Con `customerPhone` no registrado → `status=FAILED` (el PDF igual queda en `pdfUrl`).
- ⚠️ Este endpoint es **DEVICE** (tablet emparejada). Con token OWNER/SELLER/ADMIN → **403**. `POST /orders` es otra cosa (registra pedido, NO envía WhatsApp).

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/carts/share-pdf \
  -H "Authorization: Bearer <deviceToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName":"Juan Perez",
    "customerPhone":"987654321",
    "items":[{"productId":"<uuid>","quantity":2}]
  }'
```

**Store contact (auth-api):** el `PUT /api/v1/stores/{storeId}` (OWNER/ADMIN) acepta `whatsapp` (formato `^9\d{8}$`), `address` (dirección de calle) y `location` (**stand/ubicación dentro del C.C.**, ej. "AZ-104" — campo pre-existente reusado). Todos opcionales, expuestos en `GET /stores/{storeId}`. El comerciante los carga en su panel para que aparezcan en el PDF (UBICACIÓN = `location`).

---

## 9.6. BizAPI — Push notifications al staff (FCM, Fase 15)

Aviso en tiempo real a los **celulares del staff** (app Android `com.qhathia.mobile.staff`) cuando la **tablet** del kiosko crea un pedido. Dos roles de dispositivo: la tablet (DEVICE) crea pedidos; los celulares (OWNER/SELLER) los confirman/rechazan y son los que reciben el push.

### POST /api/v1/notifications/tokens

**Descripción:** registra/refresca el token FCM del celular. **Upsert por token**: re-registrar re-apunta la tienda/usuario al JWT vigente (cubre `switch-store`). La app lo llama en cada arranque, en `onNewToken()` y tras cambiar de tienda.

**Autenticación:** JWT (rol `OWNER`/`SELLER`/`ADMIN`). **DEVICE → 403** (la tablet no recibe pushes).
**URL producción:** `http://87.99.156.164/api/v1/notifications/tokens`

**Request Body:**
```json
{ "token": "<fcm-token>", "platform": "ANDROID", "deviceLabel": "Pixel 7" }
```
Validaciones: `token` `@NotBlank` max 512; `platform` opcional (default ANDROID); `deviceLabel` opcional max 120. El `storeId`/`userId` salen SIEMPRE del JWT — nunca del body.

**Response:** `204 No Content`.

### DELETE /api/v1/notifications/tokens

**Descripción:** da de baja el token (logout / opt-out). Idempotente.
**Request Body:** `{ "token": "<fcm-token>" }` → **Response:** `204`.

### Comportamiento del push

| Evento | Cuándo | Payload (data-only) |
|---|---|---|
| `NEW_ORDER` | La tablet crea un pedido (`POST /orders` → PENDING) | `{event, orderId, storeId, status:"PENDING"}` |
| `ORDER_TAKEN` | Un vendedor confirma/rechaza | `{event, orderId, storeId, status:"CONFIRMED"\|"REJECTED"}` |

- **Fan-out por tienda**: el push va a TODOS los tokens activos del `storeId` del pedido.
- **Sin PII por diseño**: solo IDs — Google nunca ve nombres, productos ni montos. La app hace `GET /orders/{id}` autenticado y arma la notificación local.
- **Post-commit + fail-open**: el push se despacha después del commit de la transacción (rollback → no hay push) y ningún fallo de push afecta la creación/confirmación del pedido.
- **Carrera de dos vendedores**: el segundo que confirma/rechaza un pedido ya terminal recibe el ProblemDetail de `INVALID_STATE_TRANSITION` — nunca doble transición.
- **Higiene de tokens**: token `UNREGISTERED` en FCM → `active=false` inmediato; purga diaria (inactivos >30 días borrados, sin re-registro >90 días desactivados).
- El push es el *acelerador*, no la fuente de verdad: la app debe seguir sincronizando con `GET /orders?status=PENDING` al abrir/refrescar.

**Config (biz-api):** `NOTIFICATIONS_PROVIDER` (`logging` default | `fcm`), `FCM_CREDENTIALS_JSON_B64` (service account base64, SOLO en `.env` del VPS), `FCM_TIMEOUT_SECONDS`. Con `logging` el comportamiento es el anterior (solo log, sin Firebase).

**Errores:** `401` sin JWT · `403` rol DEVICE · `422 VALIDATION_ERROR` token blank.

---

## 10. BizAPI — Device Sync

### GET /api/v1/device-sync/manifest

**Descripción:** Descarga el catálogo completo (categorías, productos activos, media, descuentos vigentes). Usar en la primera sincronización del dispositivo o cuando `fullSyncRequired=true`.  
**Autenticación:** JWT requerido (rol: `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/device-sync/manifest`

**Headers:** `Authorization: Bearer <deviceAccessToken>`  
**Request Body:** ninguno

**Response 200:**
```json
{
  "manifestVersion": 1713200000000,
  "generatedAt": "2025-04-15T20:00:00Z",
  "storeId": "uuid",
  "categories": [
    {
      "id": "uuid",
      "name": "Lámparas",
      "slug": "lamparas",
      "position": 0,
      "active": true,
      "updatedAt": "2025-04-15T18:30:00Z"
    }
  ],
  "products": [
    {
      "id": "uuid",
      "categoryId": "uuid",
      "name": "Lámpara Vintage",
      "description": "...",
      "price": 89.90,
      "availability": "AVAILABLE",
      "allowDiscountInquiry": false,
      "tags": ["decoracion"],
      "position": 0,
      "active": true,
      "updatedAt": "2025-04-15T18:30:00Z"
    }
  ],
  "productMedia": [
    {
      "id": "uuid",
      "productId": "uuid",
      "url": "https://...",
      "mediaType": "IMAGE",
      "position": 0,
      "primary": true
    }
  ],
  "activeDiscounts": [
    {
      "id": "uuid",
      "productId": "uuid",
      "discountType": "PERCENTAGE",
      "value": 15.00,
      "startsAt": "2025-04-15T00:00:00Z",
      "endsAt": "2025-04-30T23:59:59Z"
    }
  ]
}
```

**Nota:** `manifestVersion` es un epoch ms. Guardar este valor para usarlo en `/delta?sinceVersion=`.

**Errores:**
- `403` — Token de usuario (se requiere DEVICE)

**curl:**
```bash
curl http://87.99.156.164/api/v1/device-sync/manifest \
  -H "Authorization: Bearer eyJ..."
```

---

### GET /api/v1/device-sync/delta

**Descripción:** Retorna solo los cambios del catálogo desde la versión indicada (epoch ms). Optimizado para sincronizaciones frecuentes.  
**Autenticación:** JWT requerido (rol: `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/device-sync/delta`

**Headers:** `Authorization: Bearer <deviceAccessToken>`

**Query Params:**
- `sinceVersion` — epoch ms de la última versión sincronizada (default: `0` = full sync)

**Response 200:**
```json
{
  "sinceVersion": 1713200000000,
  "currentVersion": 1713250000000,
  "generatedAt": "2025-04-15T21:00:00Z",
  "storeId": "uuid",
  "fullSyncRequired": false,
  "categoryChanges": [...],
  "productChanges": [...],
  "mediaChanges": [...],
  "discountChanges": [...]
}
```

**Nota:** Si `fullSyncRequired=true`, ignorar los arrays de cambios y llamar a `/manifest`.

**curl:**
```bash
curl "http://87.99.156.164/api/v1/device-sync/delta?sinceVersion=1713200000000" \
  -H "Authorization: Bearer eyJ..."
```

---

## 11. BizAPI — Analytics

### POST /api/v1/analytics/events

**Descripción:** El dispositivo kiosk registra eventos de interacción en lote. Máximo 50 eventos por request.  
**Autenticación:** JWT requerido (rol: `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/analytics/events`

**Headers:** `Authorization: Bearer <deviceAccessToken>`

**Request Body:**
```json
{
  "events": [
    {
      "eventType": "SESSION_START | SESSION_END | PRODUCT_VIEW | SEARCH_MADE | ORDER_CREATED (requerido)",
      "sessionId": "string (opcional)",
      "productId": "uuid (opcional — requerido para PRODUCT_VIEW y ORDER_CREATED)",
      "occurredAt": "2025-04-15T20:00:00Z (requerido)",
      "metadata": { "key": "valor" }
    }
  ]
}
```

**Nota:** `events` es requerido y no puede estar vacío. Máximo 50 elementos.

**Response:** `204 No Content`

**Errores:**
- `403` — Token de usuario (se requiere DEVICE)
- `422` — `eventType` inválido, `occurredAt` requerido

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/analytics/events \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "events": [
      {
        "eventType": "SESSION_START",
        "sessionId": "sess-abc123",
        "occurredAt": "2025-04-15T20:00:00Z"
      },
      {
        "eventType": "PRODUCT_VIEW",
        "sessionId": "sess-abc123",
        "productId": "550e8400-e29b-41d4-a716-446655440000",
        "occurredAt": "2025-04-15T20:01:00Z",
        "metadata": {"viewDurationMs": 3000}
      }
    ]
  }'
```

---

## 12. VoAPI — Búsqueda Conversacional

### POST /api/v1/conversations/search

**Descripción:** Recibe texto ya transcrito (por el dispositivo vía Deepgram), extrae la intención de búsqueda y retorna hasta **20** productos relevantes (subido desde 5 — Fase 9, ClickUp 86e1r9epc) más `filters` y `suggestions` para el sidebar del kiosko. Si el ranking no encuentra match, devuelve hasta 3 sugerencias de productos del store para mantener la conversación viva (unhappy path — Fase 8); en ese caso `filters` y `suggestions` van vacíos.

**Autenticación:** JWT requerido (cualquier rol — típicamente `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/conversations/search`

**Headers:** `Authorization: Bearer <accessToken>`

**Request Body:**
```json
{
  "transcribedText": "string (requerido, max 500 chars — acepta alias 'transcript')",
  "sessionId": "string (opcional — para trazabilidad multi-turno futura)",
  "language": "string (opcional, ISO 639-1, default: 'es')"
}
```

**Response 200 (happy path — hay matches reales):**
```json
{
  "understood": true,
  "interpretedQuery": {
    "intent": "PRODUCT_SEARCH",
    "category": "taladro",
    "subtype": "percutor",
    "style": null,
    "useContext": null,
    "maxPrice": null,
    "minPrice": null,
    "color": null,
    "sizePreference": null,
    "attributes": [],
    "rawText": "busco un taladro percutor",
    "confidence": 0.92
  },
  "results": [
    {
      "id": "uuid",
      "name": "Taladro Percutor Bosch GSB 13 RE",
      "description": "Taladro percutor 650W",
      "price": 189.90,
      "discountedPrice": 161.42,
      "discountEndsAt": "2026-10-31T05:00:00Z",
      "availability": "AVAILABLE",
      "categoryName": "Herramientas",
      "primaryImageUrl": "https://...",
      "relevanceScore": 92,
      "brand": "Bosch",
      "tags": ["12v", "percutor"]
    }
  ],
  "filters": [
    { "criterion": "Marca",   "options": ["Bosch", "DeWalt", "Makita"] },
    { "criterion": "Voltaje", "options": ["12V", "18V", "20V"] },
    { "criterion": "Tipo",    "options": ["Percutor", "Inalámbrico"] }
  ],
  "suggestions": [
    "Para uso doméstico",
    "Para uso profesional",
    "Inalámbrico",
    "Kit completo",
    "Percutor"
  ],
  "noResults": false,
  "suggestedFromCategory": null,
  "spokenText": "Encontré varios taladros. El más relevante es un Bosch a 189 soles.",
  "displayText": "20 resultados para 'taladro percutor'",
  "fallbackMessage": null
}
```

**`discountedPrice` / `discountEndsAt` (Fase 29)** — en **todos** los `results[*]`,
también en las sugerencias de los dos unhappy path de abajo. `null` = sin descuento
vigente: se pinta solo `price`. Misma regla y mismo número que
`GET /catalog/by-category` (ver [sección 25](#25-precio-con-descuento-y-aviso-de-expiración-fase-25)).
`minPrice`/`maxPrice` filtran contra `discountedPrice ?: price`.

**Response 200 (unhappy path con categoría entendida — Fase 8):**
```json
{
  "understood": true,
  "interpretedQuery": { "category": "lámpara", "...": "..." },
  "results": [
    { "id": "uuid-1", "name": "Lámpara LED colgante", "price": 89.0, "relevanceScore": 0, "...": "..." },
    { "id": "uuid-2", "name": "Lámpara de mesa industrial", "price": 159.0, "relevanceScore": 0, "...": "..." },
    { "id": "uuid-3", "name": "Lámpara LED de techo", "price": 120.0, "relevanceScore": 0, "...": "..." }
  ],
  "noResults": true,
  "suggestedFromCategory": "lámpara",
  "filters": [],
  "suggestions": [],
  "spokenText": "Lo sentimos, no encontramos productos para tu búsqueda.",
  "displayText": "Sin resultados",
  "fallbackMessage": "No encontramos productos para tu búsqueda. Puedes consultar con nuestros asesores."
}
```

**Response 200 (unhappy path sin categoría entendida — Fase 8):**
```json
{
  "understood": false,
  "interpretedQuery": { "category": null, "...": "..." },
  "results": [
    { "id": "...", "name": "...", "relevanceScore": 0, "...": "..." }
  ],
  "noResults": true,
  "suggestedFromCategory": null,
  "filters": [],
  "suggestions": [],
  "fallbackMessage": "..."
}
```

**Notas sobre `filters` y `suggestions` (Fase 9, ClickUp 86e1r9epc):**

- **`filters`** es un array de objetos `{criterion, options}` para el sidebar de filtros del kiosko (Mockup 5). Máximo 5 criterios, máximo 5 opciones por criterio. Strings planos en `options` — NO llevan `count`. Vacío `[]` cuando `noResults=true`.
    - El criterio **"Marca"** viene del campo `brand` de cada producto resuelto. Aparece primero si tiene opciones. Ordenadas por frecuencia descendente, desempate alfabético.
    - Otros criterios vienen de **tags agrupados** por la nueva columna `group` del tag catalog (ej. "voltaje" → criterio "Voltaje" con opciones "12V/18V/20V"). Solo los tags con `group` no nulo aparecen. El admin del catálogo asigna el `group` a cada tag. Tags con `group` null siguen contando al ranking pero no aparecen como filtros.
- **`suggestions`** es un array plano de hasta 5 strings con frases cortas para que el cliente refine su búsqueda ("Para uso doméstico", "Inalámbrico", etc.). Generado por LLM (Groq) cuando `vitria.llm.suggestions.enabled=true`; fallback a templates por categoría cuando el LLM falla. Si no hay templates para la categoría (o la categoría no se entendió) → `[]`. Vacío `[]` cuando `noResults=true`.
- **Frontend filtra client-side**: cada producto ahora trae `brand` y `tags` para que el frontend filtre la lista localmente cuando el cliente toca una opción del sidebar (no hace falta nuevo request al backend).
- **`MAX_RESULTS` subió de 5 a 20** — el sidebar tiene sentido con más opciones para filtrar.

**Notas sobre el unhappy path (Fase 8 — Flow C):**

- **`noResults` indica "no hubo match al query"**, NO "array vacío". `results` puede tener hasta 3 sugerencias aunque `noResults=true`. Pensar el flag como "el ranking falló" y `results` como "lo que sirve mostrar al usuario".
- **`suggestedFromCategory` (NUEVO, nullable)**:
    - Si `noResults=true` y el LLM entendió la categoría Y al menos 1 sugerencia vino de esa categoría → string en minúsculas con la categoría (ej `"lámpara"`, `"aretes"`). Usarlo para personalizar el copy: *"Te puede interesar de lámparas:"*.
    - Si `noResults=true` pero las 3 sugerencias vinieron del store sin filtrar (categoría no entendida o no encontrada) → `null`. Usar copy genérico: *"Te puede interesar:"*.
    - Si `noResults=false` (happy path) → siempre `null`.
- **`fallbackMessage`** sigue presente cuando `noResults=true` con el mensaje de "Lo sentimos…". Es el copy "de cabecera" de la pantalla unhappy. Las sugerencias van debajo.
- **`results[*].relevanceScore = 0`** identifica que el producto vino de las sugerencias del unhappy path, no del ranking real. Útil para tracking.
- **Tamaño**:
    - Happy path: hasta 5 productos.
    - Unhappy path: hasta 3 sugerencias. Si el store tiene menos de 3 AVAILABLE, devuelve los que haya (puede ser 0, 1, 2 o 3).
- **Filtros de las sugerencias**: solo `AVAILABLE` + `isActive=true`. LOW_STOCK y CONSULT quedan fuera.
- **Orden de las sugerencias**: `position ASC, name ASC` (determinístico, lo mismo en consultas sucesivas).
- **Backward compat**: si renderizan `results` siempre, los clientes viejos funcionarán igual. El campo `suggestedFromCategory` es opcional — ignorenlo si no lo usan todavía.

**Otras notas:**
- Cuando `understood=false`, la búsqueda fue ambigua — `fallbackMessage` guía al usuario, y `results` puede traer sugerencias del store si el ranking quedó vacío.
- El path primario de datos es MongoDB (`vitria_search`); si no hay datos, hace fallback a BizAPI.
- Las sugerencias del unhappy path siempre se leen desde MongoDB. Si Mongo cae al construirlas → `results=[]` (fail-open), nunca 5xx.

**Errores:**
- `401` — No autenticado
- `422` — `transcribedText` requerido o muy largo

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/conversations/search \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "transcribedText": "busco una lámpara de pared vintage",
    "sessionId": "sess-abc123"
  }'
```

---

### POST /api/v1/conversations/no-results

**Descripción:** Registra en MongoDB una búsqueda que no encontró resultados, para análisis de oportunidades del catálogo.  
**Autenticación:** JWT requerido (cualquier rol)  
**URL producción:** `http://87.99.156.164/api/v1/conversations/no-results`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "rawText": "string (requerido, max 500 chars)"
}
```

**Response:** `204 No Content`

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/conversations/no-results \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"rawText": "busco una cafetera industrial"}'
```

---

### GET /api/v1/conversations/health/dependencies

**Descripción:** Verifica la conectividad de VoAPI con MongoDB y BizAPI. Útil para monitoreo.  
**Autenticación:** Pública  
**URL producción:** `http://87.99.156.164/api/v1/conversations/health/dependencies`

**Response 200:**
```json
{
  "mongodb": "UP",
  "bizApi": "UP",
  "overall": "UP"
}
```

**Valores posibles:** `"UP"` | `"DOWN"` | `"DEGRADED"` (para `overall`)

**curl:**
```bash
curl http://87.99.156.164/api/v1/conversations/health/dependencies
```

---

## 13. Auth API — Centros Comerciales

Catálogo curado por Vitria. La lectura es para cualquier autenticado (la app necesita listar SCs en formularios y derivar nombres en `/me/stores`); la escritura requiere `ADMIN`.

### GET /api/v1/shopping-centers

**Descripción:** Lista los centros comerciales activos. Acepta `?activeOnly=false` para incluir desactivados.
**Autenticación:** JWT requerido (cualquier rol)
**Response 200:**
```json
[
  {
    "id": "00000000-0000-0000-0001-000000000001",
    "code": "MALVINAS",
    "name": "Centro Comercial Las Malvinas",
    "address": "Av. Argentina, Cercado de Lima",
    "active": true,
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

### GET /api/v1/shopping-centers/{id}
Mismo shape (singular). 404 si no existe.

### POST /api/v1/shopping-centers   (ADMIN)
**Body:** `{ "code": "PLAZA_NORTE", "name": "Mall Plaza Norte", "address": "..." }`
- `code`: UPPERCASE alfanumérico + underscore, único.
- 201 con el `ShoppingCenterResponse`. 409 si el code ya existe.

### PUT /api/v1/shopping-centers/{id}   (ADMIN)
Body con campos parciales (`code`, `name`, `address`, `active`).

### DELETE /api/v1/shopping-centers/{id}   (ADMIN)
Soft delete: marca `active=false`. Las tiendas existentes mantienen su FK.

---

## 14. Admin API — Onboarding cerrado por Vitria

Todo este bloque requiere `Authorization: Bearer <ADMIN>`. El comerciante NO usa estos endpoints.

### `/api/v1/admin/users` — Gestión de usuarios

| Método | Path | Descripción |
|---|---|---|
| GET    | `/api/v1/admin/users` | Lista todos |
| GET    | `/api/v1/admin/users/{id}` | Detalle |
| POST   | `/api/v1/admin/users` | Crea usuario huérfano (sin tienda) |
| PUT    | `/api/v1/admin/users/{id}` | Update parcial |
| DELETE | `/api/v1/admin/users/{id}` | Soft delete |

**POST body:**
```json
{
  "fullName": "Ana Vega",
  "email": "ana@bisuteria.pe",
  "password": "miPass1234",
  "role": "OWNER"
}
```
- `role` ∈ `OWNER` \| `SELLER` \| `ADMIN`. `DEVICE` está bloqueado (400).
- 409 si el email ya existe.

### `/api/v1/admin/stores` — Gestión de tiendas

CRUD de tiendas. **Crea tiendas vacías** — para vincular owner usar `/admin/users/{userId}/stores/{storeId}`; para asignar plan usar `/admin/subscriptions`.

**POST body:**
```json
{
  "shoppingCenterId": "uuid",
  "name": "Joyas Lima",
  "tradeName": "(opcional)",
  "category": "JEWELRY",
  "location": "Gal A, Stand 5",
  "phone": "(opcional)"
}
```
- 400 si el `shoppingCenterId` no existe o está desactivado.

### `/api/v1/admin/subscriptions` — Gestión de suscripciones

| Método | Path | Descripción |
|---|---|---|
| GET    | `/api/v1/admin/subscriptions` | Lista todas |
| GET    | `/api/v1/admin/subscriptions/{id}` | Detalle |
| POST   | `/api/v1/admin/subscriptions` | Crea con `status=ACTIVE` |
| PUT    | `/api/v1/admin/subscriptions/{id}` | Update plan/status/fechas/flags |
| DELETE | `/api/v1/admin/subscriptions/{id}` | Cancela: `status=CANCELLED, endsAt=now` |

**POST body:**
```json
{
  "storeId": "uuid",
  "planType": "PRO",
  "startedAt": "2026-05-01T00:00:00Z",
  "endsAt": null,
  "onboardingFeePaid": true,
  "depositPaid": true
}
```
- 409 si la tienda ya tiene una suscripción `ACTIVE` (índice parcial garantiza una sola activa por tienda).

### `/api/v1/admin/users/{userId}/stores/{storeId}` — Vínculo M:N

Multi-store: un usuario puede estar vinculado a varias tiendas.

| Método | Path | Descripción |
|---|---|---|
| GET    | `/api/v1/admin/users/{userId}/stores` | Tiendas del usuario |
| GET    | `/api/v1/admin/stores/{storeId}/users` | Usuarios de la tienda |
| POST   | `/api/v1/admin/users/{userId}/stores/{storeId}` | Vincular (o reactivar) |
| DELETE | `/api/v1/admin/users/{userId}/stores/{storeId}` | Soft delete del vínculo |

**POST body:**
```json
{ "role": "OWNER" }
```
- `role` ∈ `OWNER` \| `SELLER`. Bloqueado para `UserRole.ADMIN` y `UserRole.DEVICE` (400).

### `/api/v1/admin/tag-catalog` — Catálogo de tags (biz)

Curado por Vitria (`role=ADMIN` requerido). Indexado por (`shoppingCenterId`, `storeCategory`). El campo `group` (Fase 9) agrupa tags por dimensión para el sidebar del kiosko (ej "voltaje", "tipo", "material") — solo los tags con `group` poblado aparecen como criterios en el `filters` del search.

| Método | Path | Descripción |
|---|---|---|
| GET    | `/api/v1/admin/tag-catalog?shoppingCenterId=…&storeCategory=…` | Lista activos del par |
| GET    | `/api/v1/admin/tag-catalog/{id}` | Detalle |
| POST   | `/api/v1/admin/tag-catalog` | Crea individual (409 si duplicado) |
| PUT    | `/api/v1/admin/tag-catalog/{id}` | Update parcial |
| DELETE | `/api/v1/admin/tag-catalog/{id}` | Soft delete |
| POST   | `/api/v1/admin/tag-catalog/bulk` | Import masivo (UPSERT/REPLACE) |

**Shape común (request/response):**
```json
{
  "shoppingCenterId": "uuid",
  "storeCategory": "JEWELRY",
  "value": "oro-amarillo",
  "label": "Oro Amarillo",
  "description": "(opcional)",
  "group": "material",
  "position": 0
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `value` | string | Lowercase alfanumérico (`^[a-z0-9][a-z0-9_-]*$`), max 60. Lo que se persiste en `Product.tags`. |
| `label` | string | Texto visible al cliente del kiosko. Max 120. |
| `description` | string \| null | Pista para el LLM al sugerir. Max 2000. |
| **`group`** | string \| null | **(Fase 9, NUEVO)** Dimensión: `"voltaje"`, `"tipo"`, `"material"`, etc. Vocabulario libre. Max 60. NULL = el tag no aparece en `filters` del search pero sigue contando al ranking. |
| `position` | int | Orden del sidebar. |
| `active` | bool | Soft delete flag (solo response). |

**Bulk body:**
```json
{
  "shoppingCenterId": "uuid",
  "storeCategory": "JEWELRY",
  "mode": "REPLACE",
  "tags": [
    { "value": "oro", "label": "Oro", "group": "material", "position": 0 },
    { "value": "vintage", "label": "Vintage", "group": "estilo", "position": 1 }
  ]
}
```
- `mode=UPSERT` (default): crea o actualiza por `value`, deja intactos los existentes.
- `mode=REPLACE`: desactiva los tags ausentes en el payload y upserts los del payload.
- `value`: lowercase alfanumérico, guiones/underscore permitidos, sin espacios.
- `group`: opcional en cada row. Si se omite, el tag queda sin grupo y no aparece como filtro.

**`PUT /api/v1/admin/tag-catalog/{id}` (update parcial):**
```json
{ "group": "voltaje" }
```
- Solo se envían los campos a modificar. `null` no cambia el campo (a diferencia de POST donde sí persiste null). Para limpiar el `group` de un tag, mandar `""` (string vacío).

---

## 15. BizAPI — Tags (sugerencias y auto-tag)

### GET /api/v1/tags/suggestions

**Descripción:** Devuelve el catálogo de tags aplicable al contexto del comerciante autenticado.

Tres modos de resolución:
- **Sin params** → claims del JWT (tienda activa post `switch-store`).
- **`?storeId=`** → debe coincidir con la tienda activa del JWT (sino 400 con mensaje "haz switch-store primero").
- **`?productId=`** → desde `shoppingCenterId`/`storeCategory` persistidos en el producto. Cae a los claims si el producto es legacy.

**Autenticación:** JWT requerido
**Response 200:**
```json
{
  "shoppingCenterId": "uuid",
  "storeCategory": "JEWELRY",
  "tags": [
    {
      "id": "uuid",
      "shoppingCenterId": "uuid",
      "storeCategory": "JEWELRY",
      "value": "oro",
      "label": "Oro",
      "description": null,
      "group": "material",
      "position": 0,
      "active": true,
      "createdAt": "...",
      "updatedAt": "..."
    }
  ]
}
```

**Notas:**
- El campo **`group`** (Fase 9) indica la dimensión del tag (`"material"`, `"voltaje"`, `"tipo"`, etc.). El frontend del comerciante puede **agruparlos visualmente** en el form de creación/edición de producto (ej: chips de "Material" separados de chips de "Estilo"). NULL = tag sin dimensión asignada.
- El response sigue `tag_catalog` activo del mall × rubro del JWT. Para que el sidebar del kiosko muestre criterios, el admin debe haber asignado `group` a los tags relevantes via `/admin/tag-catalog`.

**Errores:**
- `400` si el JWT no lleva tienda activa (multi-store sin switch).

### POST /api/v1/tags/suggest-from-image

**Descripción:** Auto-tag por visión a partir de una URL de imagen, **sin requerir productId**. Pensado para el flujo "el usuario eligió foto pero aún no guardó el producto":

1. Frontend sube la foto a Firebase Storage (carpeta `drafts/`) y obtiene la URL pública.
2. Frontend llama este endpoint con esa URL.
3. Backend resuelve el catálogo del (mall × rubro) **desde los claims del JWT** (no necesita Product en BD).
4. Llama al `VisionGateway` (Groq o stub), filtra defensivamente contra el catálogo, retorna sugerencias.
5. Usuario edita/acepta los tags. Frontend mueve la foto de `drafts/` a `products/<productId>/` y hace `POST /products` con los tags + URL final.

Para foto ya asociada a un producto existente, usar [`POST /api/v1/products/{id}/suggest-tags`](#post-apiv1productsidsuggest-tags) (siguiente endpoint).

**Autenticación:** JWT requerido (cualquier rol con tienda activa).

**Body:**
```json
{
  "imageUrl": "https://firebasestorage.googleapis.com/v0/b/<bucket>/o/drafts%2F<userId>%2F<uuid>.jpg?alt=media&token=<...>"
}
```
- `imageUrl`: URL pública accesible por Groq. Firebase Storage con download URL funciona out-of-the-box. Signed URLs temporales también, mientras estén vigentes en el momento de la llamada. Máximo 500 caracteres.

**Response 200:**
```json
{
  "productId": null,
  "shoppingCenterId": "00000000-0000-0000-0001-000000000001",
  "storeCategory": "JEWELRY",
  "suggestedTags": ["oro", "vintage"],
  "source": "groq"
}
```
- `productId: null` — correcto, el producto aún no existe.
- `source`: `"groq"` confirma que el modelo real corrió. `"stub"` indica que el provider está en modo stub (dev) — los tags son los primeros 3 del catálogo determinísticamente.

**Errores:**
- `400` — el JWT no lleva tienda activa (multi-store sin switch).
- `422` — `imageUrl` vacía o más de 500 caracteres.
- Si Groq no puede descargar la URL o la foto es inválida, retorna `200` con `suggestedTags: []` (fail-open, defensivo).

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/tags/suggest-from-image \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"imageUrl":"https://firebasestorage.googleapis.com/v0/b/vitria/o/drafts%2Fmaria%2Fanillo.jpg?alt=media&token=abc123"}'
```

### POST /api/v1/products/{id}/suggest-tags

**Descripción:** Auto-tag por visión. El backend toma una imagen, llama al modelo (Groq Llama 4 Scout por default; `stub` en dev), y retorna sugerencias **filtradas defensivamente** contra el catálogo del (mall × rubro) del producto.

**Autenticación:** JWT requerido (ownership del producto verificada).

**Body:** exactamente UNO de los dos campos.
```json
{ "mediaId": "uuid" }                  // foto ya guardada en ProductMedia
{ "imageUrl": "https://firebase…/x.jpg" }  // foto recién subida, aún no asociada
```

**Response 200:**
```json
{
  "productId": "uuid",
  "shoppingCenterId": "uuid",
  "storeCategory": "JEWELRY",
  "suggestedTags": ["oro", "plata", "vintage"],
  "source": "stub"
}
```
- `source` ∈ `stub` | `groq` (útil para auditoría).
- Las URLs de imagen deben ser accesibles públicamente (Firebase Storage funciona por default; signed URLs no).

**Errores:**
- `404` — producto ajeno al usuario o no existe
- `422` — payload sin / con ambos campos
- `400` — el JWT no lleva tienda activa

---

## 16. Quick Start — Flujo Completo End-to-End

Flujo completo en el modelo de **onboarding cerrado** (Vitria provisiona, comerciante solo se loguea). Ejecutar en orden; reemplazar `$VAR` con los valores retornados por cada llamada anterior.

### Paso 1 — ADMIN se loguea (seed inicial)

```bash
ADMIN_TOKEN=$(curl -s -X POST http://87.99.156.164/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@vitria.com","password":"<rotada-en-deploy>"}' \
  | jq -r .accessToken)
```

> La contraseña por defecto del seed (V8) es `Vitria@Admin2026`. **Rotar en el primer deploy a producción** vía `PUT /api/v1/admin/users/{id}` con el campo `password`.

### Paso 2 — ADMIN crea ShoppingCenter + tag_catalog (una vez por mall)

```bash
SC_ID=$(curl -s -X POST http://87.99.156.164/api/v1/shopping-centers \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"MALVINAS","name":"Centro Comercial Las Malvinas"}' \
  | jq -r .id)

# Catálogo de 5 tags para JEWELRY en MALVINAS
curl -X POST http://87.99.156.164/api/v1/admin/tag-catalog/bulk \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"shoppingCenterId\": \"$SC_ID\",
    \"storeCategory\": \"JEWELRY\",
    \"mode\": \"REPLACE\",
    \"tags\": [
      {\"value\":\"oro\",\"label\":\"Oro\",\"position\":0},
      {\"value\":\"plata\",\"label\":\"Plata\",\"position\":1},
      {\"value\":\"vintage\",\"label\":\"Vintage\",\"position\":2}
    ]
  }"
```

### Paso 3 — ADMIN crea comerciante (User + Store + Subscription)

```bash
# Atajo: register hace User + Store + Subscription + vínculo en una sola llamada
RESP=$(curl -s -X POST http://87.99.156.164/api/v1/auth/register \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"fullName\": \"María González\",
    \"email\": \"maria@joyeria.pe\",
    \"password\": \"miPass1234\",
    \"storeName\": \"Joyería María\",
    \"shoppingCenterId\": \"$SC_ID\",
    \"storeCategory\": \"JEWELRY\",
    \"planType\": \"PRO\"
  }")
STORE_ID=$(echo "$RESP" | jq -r .store.id)
```

### Paso 4 — Comerciante se loguea

```bash
TOKEN=$(curl -s -X POST http://87.99.156.164/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@joyeria.pe","password":"miPass1234"}' \
  | jq -r .accessToken)

# Si tiene varias tiendas, el JWT no lleva storeId — debe hacer switch:
curl http://87.99.156.164/api/v1/users/me/stores -H "Authorization: Bearer $TOKEN"
TOKEN=$(curl -s -X POST http://87.99.156.164/api/v1/auth/switch-store/$STORE_ID \
  -H "Authorization: Bearer $TOKEN" | jq -r .accessToken)
```

### Paso 5 — Comerciante crea producto y le sube foto

```bash
PROD_ID=$(curl -s -X POST http://87.99.156.164/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Anillo Vintage","price":150,"availability":"AVAILABLE"}' \
  | jq -r .id)

# La foto se sube DESDE EL FRONTEND a Firebase Storage; aquí solo registramos la URL pública
curl -X POST http://87.99.156.164/api/v1/products/$PROD_ID/media \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://firebasestorage.googleapis.com/v0/b/vitria/o/anillo.jpg","mediaType":"IMAGE","primary":true}'
```

### Paso 6 — Auto-tag con visión (Groq)

```bash
curl -X POST http://87.99.156.164/api/v1/products/$PROD_ID/suggest-tags \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"imageUrl":"https://firebasestorage.googleapis.com/v0/b/vitria/o/anillo.jpg"}'
# → {"suggestedTags":["oro","vintage"],"source":"groq"}
```

### Paso 7 — Pairing del kiosk

```bash
# 1) La tablet (sin JWT) genera el código y obtiene el sessionId.
GEN=$(curl -s -X POST http://87.99.156.164/api/v1/devices/pairing-token)
PAIR=$(echo "$GEN" | jq -r .pairingToken)
SESSION=$(echo "$GEN" | jq -r .sessionId)
# (La tablet abre wss /api/v1/devices/pairing-session/$SESSION y muestra $PAIR)

# 2) El comercio (JWT OWNER) aprueba el código.
curl -s -X POST http://87.99.156.164/api/v1/devices/pair \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"pairingToken\":\"$PAIR\",\"decision\":\"APPROVE\",\"deviceName\":\"Tablet 1\",\"platform\":\"ANDROID\",\"appVersion\":\"1.0.0\"}"

# 3) La tablet canjea su DEVICE JWT por sessionId.
DEVICE_TOKEN=$(curl -s http://87.99.156.164/api/v1/devices/pairing-session/$SESSION/token \
  | jq -r .deviceAccessToken)
```

### Paso 8 — Búsqueda conversacional desde el kiosk

```bash
curl -X POST http://87.99.156.164/api/v1/conversations/search \
  -H "Authorization: Bearer $DEVICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"transcribedText":"busco algo vintage de oro","sessionId":"s1"}'
# Solo tags del catálogo cuentan al rankear; tags custom del producto son invisibles.
```

### Paso 9 — Orden y confirmación

```bash
ORDER_ID=$(curl -s -X POST http://87.99.156.164/api/v1/orders \
  -H "Authorization: Bearer $DEVICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PROD_ID\",\"sessionId\":\"s1\"}" \
  | jq -r .id)

curl -X POST http://87.99.156.164/api/v1/orders/$ORDER_ID/confirm \
  -H "Authorization: Bearer $TOKEN"
```

---

---

## 17. Dashboard de inicio (Fase 17)

Backend de la pantalla **Inicio** de la app del staff y de su detalle **Resumen de ventas**
(tabs Resumen / Ranking / Actividad). Change `store-home-dashboard`.

**Auth de todos los endpoints:** JWT de usuario con rol `OWNER`, `SELLER` o `ADMIN`.
El token `DEVICE` (tablet) recibe **403**: genera los eventos, no consume la analítica.

### Modelo temporal (aplica a sales, ranking y activity)

| Param | Valores | Default |
|---|---|---|
| `granularity` | `MONTHLY` \| `WEEKLY` \| `DAILY` \| `HOURLY` | `WEEKLY` |
| `buckets` (solo sales) | 1..48 | 6 / 4 / 7 / 12 según granularidad |
| `categoryId` (sales, ranking) | UUID de categoría | todas |

- El **último bucket es el periodo en curso**: de ahí sale el número grande y los KPIs.
  El gráfico (`buckets[]`) es contexto histórico, del más antiguo al más reciente.
- Los cortes se hacen en **hora local** (`America/Lima`), no en UTC — si no, el "día"
  del comerciante empezaría a las 19:00.
- Cuentan solo las órdenes **`CONFIRMED`**, fechadas por `lastStatusChangedAt`. Los importes
  usan el **precio snapshot** de cada línea, así el histórico no cambia si se reajustan precios.

### GET /api/v1/dashboard/sales

```json
{
  "granularity": "WEEKLY",
  "timezone": "America/Lima",
  "current":  { "label": "Sem 13", "periodStart": "...", "periodEnd": "...",
                "revenue": 1690.00, "salesCount": 24, "averageTicket": 70.42, "unitsSold": 55 },
  "previous": { "label": "Sem 12", "revenue": 1360.00, "...": "..." },
  "deltaPercent": 24.3,
  "buckets": [ { "label": "Sem 10", "revenue": 1240.00, "...": "..." }, "..." ]
}
```
`deltaPercent` es **null** cuando el periodo anterior fue 0 (no hay base de comparación —
la app no debe pintar "+100%"). El texto del highlight ("La Semana 4 generó S/1,690, 24% más
que la semana anterior") lo arma el front con estos datos.

### GET /api/v1/dashboard/ranking

```json
{
  "periodStart": "...", "periodEnd": "...",
  "topSellers": { "totalRevenue": 4090.00,
    "items": [ { "productId": "...", "name": "Taladro percutor Bosch GSB",
                 "imageUrl": "...", "revenue": 1870.00, "unitsSold": 34 } ] },
  "needsAttention": { "totalImmobilized": 3400.00,
    "items": [ { "productId": "...", "name": "...", "imageUrl": "...",
                 "stock": 34, "price": 100.00, "immobilizedAmount": 3400.00 } ] }
}
```
`needsAttention` = productos **activos con stock que no vendieron nada en el periodo**;
`immobilizedAmount = stock × price` es la plata parada. Los productos en modo consultar
(`stock = null`) se excluyen: no hay inventario que medir.

### GET /api/v1/dashboard/activity

```json
{ "periodStart": "...", "periodEnd": "...",
  "uniqueSessions": 63, "uniqueSessionsDeltaPercent": 12.5,
  "productViews": 148,  "productViewsDeltaPercent": 8.8 }
```
`uniqueSessions` = sesiones distintas con evento `SESSION_START` (clientes que usaron la
tablet); `productViews` = eventos `PRODUCT_VIEW`. **Si la tablet no emite eventos (o los emite
sin `sessionId`), devuelve ceros** — no es un error, pero conviene validarlo con el equipo del kiosko.

### GET /api/v1/dashboard/restock

```json
{ "threshold": 5,
  "items": [ { "productId": "...", "name": "...", "imageUrl": "...",
               "stock": 1, "price": 200.00, "availability": "LOW_STOCK" } ] }
```
Estado **actual** del inventario (no depende del periodo). Umbral configurable con
`DASHBOARD_RESTOCK_THRESHOLD` (default 5). Param `limit` 1..50 (default 10).

### GET /api/v1/insights/unmet-searches (vo-api)

Búsquedas del kiosko que **no encontraron producto** — la sección "Productos no encontrados".
Params: `days` (1..90, default 7), `limit` (1..50, default 10).

```json
{ "periodStart": "...", "periodEnd": "...", "totalPeople": 12,
  "groups": [
    { "term": "taladro", "personCount": 9, "searchCount": 14,
      "examples": [ { "rawText": "taladro percutor bosch", "count": 5 },
                    { "rawText": "taladro para concreto",  "count": 4 } ] } ] }
```

**Consolidación en 2 niveles** — el mismo producto se pide de muchas formas, así que se agrupa
por el **término canónico**: `categoryKeyword` + `subtype` ("mesa giratoria"). Desde la
Fase 33, las búsquedas sin `categoryKeyword` no entran. **Nunca el `subtype` solo**, que suele ser un adjetivo
(Fase 29). Cada grupo trae los textos crudos más frecuentes como evidencia de qué comprar
exactamente.

**Qué no entra (Fase 29):** las búsquedas sin sustantivo ("dame algo de más de 50 soles"), y
las que el LLM interpretó con `confidence < 0.4`. Se siguen registrando; no se muestran como
producto faltante.

- **`personCount` (personas distintas) es la métrica principal**, no `searchCount`: alguien que
  insiste 9 veces porque no encuentra es 1 persona de demanda, no 9. Los grupos vienen ordenados
  por `personCount`.
- Los `search_events` **anteriores a este change no tienen `sessionId`**, así que su `personCount`
  queda subestimado (cuentan como una sesión desconocida).
- Fail-open: si la agregación falla, devuelve `groups: []` en vez de romper la pantalla.

### Notas para el front

- **Home** = `sales` (WEEKLY) + `restock` + `insights/unmet-searches` (3 llamadas, dos servicios).
  **Resumen** = `sales` / `ranking` / `activity` según la tab.
- "Última actualización a las 11:50 pm" es client-side (timestamp del último fetch OK).
- El umbral visual de "quedan pocos" lo decide la app con `stock`; el backend solo da el número.
- `revenue` con `categoryId` suma solo las líneas de esa categoría; sin filtro suma todas las
  líneas de la orden.

**Errores:** `401` sin JWT · `403` rol DEVICE · `400` `buckets`/`limit`/`granularity` fuera de rango.

---

## 18. Pantallas de Perfil (Fase 18)

Backend de **Mi perfil**, **Datos personales**, **Dispositivos** y **Mi suscripción** de la app
del staff. Change `profile-subscription-and-devices`.

### Perfil del usuario

`GET /api/v1/users/me` · `PUT /api/v1/users/me` (cualquier usuario autenticado)

```json
{
  "id": "...", "email": "carla.mendoza@gmail.com", "fullName": "Carla Mendoza",
  "phone": "999888777",
  "passwordUpdatedAt": null,
  "role": "OWNER", "active": true, "emailVerified": false,
  "lastLoginAt": "...", "createdAt": "...",
  "stores": [ { "storeId": "...", "storeName": "...", "role": "OWNER" } ]
}
```

- `PUT` acepta `fullName` y `phone`; es **update parcial** (los campos ausentes no se tocan).
- `phone`: móvil peruano de 9 dígitos, sin el `+51` (eso lo pinta la app). Inválido → 422.
- **`email` no editable**: es la credencial de login y cambiarlo sin verificación permitiría
  secuestrar o inutilizar la cuenta. Si llega en el body se ignora; la pantalla debe mostrarlo
  deshabilitado.
- `passwordUpdatedAt` es **null** para usuarios que nunca cambiaron su contraseña → la app
  muestra "nunca actualizada", no una fecha inventada.
- `stores[]` da el "Dueña · 3 tiendas" del encabezado. El avatar son iniciales client-side
  (no hay subida de foto).

### Suscripción

`GET /api/v1/subscriptions/current` (OWNER/ADMIN)

```json
{
  "planName": "Plan Pro", "planType": "PRO", "status": "ACTIVE",
  "priceMonthlyPen": 89.00, "productLimit": 200,
  "hasAdvancedAnalytics": true, "hasVideoProducts": true,
  "hasBulkPriceUpdate": false, "hasPrioritySupport": true,
  "startedAt": "2025-08-15T...", "endsAt": "2026-09-15T..."
}
```
`startedAt` + `endsAt` alimentan la barra de progreso y el "Se renueva el …".

> **No existe** método de pago, historial de pagos ni cancelación: no hay pasarela integrada.
> Esas secciones de la pantalla van como "próximamente".

### Dispositivos

`GET /api/v1/devices` (OWNER/ADMIN) — cada entrada:

```json
{
  "id": "...", "name": "Tablet vitrina 1", "status": "ACTIVE",
  "storeName": "El Martillo",
  "batteryLevel": 82, "networkStatus": "WIFI",
  "lastPingAt": "...", "lastSyncAt": "...",
  "serialNumber": "HA1P1C2K", "appVersion": "1.4.2", "androidVersion": "13",
  "linkedAt": "..."
}
```

- `batteryLevel` y `networkStatus` reflejan el **último heartbeat**, no el instante actual:
  léelos junto a `lastPingAt` y degrada la UI si el ping está viejo.
- `batteryLevel` es `null` si la tablet nunca lo reportó.

`PATCH /api/v1/devices/{deviceId}` (OWNER/ADMIN) → `{ "name": "Tablet mostrador" }` → 200 con el
dispositivo actualizado. `404` si el dispositivo no es de la tienda del JWT, `422` si el nombre
viene en blanco.

`GET /api/v1/catalog/count` (cualquier autenticado) → `{ "activeProducts": 12 }`.

**Heartbeat**: `POST /api/v1/devices/heartbeat` (DEVICE) ya aceptaba `batteryLevel` y
`networkStatus`; ahora se persisten. La telemetría **nunca rompe el heartbeat**: un valor fuera
de 0-100 se descarta y el ping igual responde 200 — el latido es lo que mantiene viva la tablet.

### Qué NO cubre esta fase

Toggles de notificaciones, ajustes remotos de la tablet (modo kiosco, PIN, brillo…) y todo lo de
pagos. Cambiar contraseña y sesiones activas están en la **Fase 19**.

---

## 19. Seguridad de la cuenta y sesiones (Fase 19)

Backend de **Seguridad** y **Cambiar contraseña** del perfil. Change
`account-security-and-sessions`. **No hay migración SQL**: las sesiones viven en Redis.

### Qué es una "sesión"

Una sesión es **un equipo**, no una credencial. El refresh token rota en cada `/auth/refresh`
pero el `sessionId` se conserva, así que la pantalla no muestra 40 entradas para una tablet que
lleva un mes prendida — muestra una, con su `lastUsedAt` al día.

El **refresh token nunca sale del backend** hacia la lista: si lo devolviéramos para poder cerrar
una sesión, cualquiera que listara las suyas tendría la credencial de sus otros equipos. Lo que
viaja es el `sessionId`, un UUID opaco.

### Login con etiqueta de equipo

`POST /api/v1/auth/login` acepta un campo nuevo, opcional:

```json
{ "email": "carla@tienda.pe", "password": "...", "deviceLabel": "Redmi Note 12" }
```

y la respuesta trae un campo nuevo:

```json
{ "accessToken": "...", "refreshToken": "...", "sessionId": "3f0c…", "tokenType": "Bearer", "...": "..." }
```

- `deviceLabel`: máx. 100 caracteres. **Recomendado**: manda el modelo real del equipo, es lo que
  el comerciante va a leer para decidir qué cerrar.
- Si no lo mandas se usa el `User-Agent` (truncado a 100); si tampoco hay, queda
  `"Dispositivo desconocido"`.
- `sessionId`: guárdalo, es "este equipo".

`POST /api/v1/auth/register` también emite tokens y por tanto **crea una sesión**.

### GET /api/v1/auth/sessions

**Auth:** cualquier usuario autenticado. **Header opcional:** `X-Refresh-Token: <tu refresh token>`.

```json
[
  {
    "sessionId": "3f0c9e2a-...",
    "deviceLabel": "Redmi Note 12",
    "createdAt": "2026-09-10T14:22:03Z",
    "lastUsedAt": "2026-09-13T09:41:55Z",
    "current": true
  },
  {
    "sessionId": "8b21d4f7-...",
    "deviceLabel": "Tablet vitrina",
    "createdAt": "2026-08-02T11:05:00Z",
    "lastUsedAt": "2026-09-13T08:10:12Z",
    "current": false
  }
]
```

- Orden: **la sesión actual primero**, luego por `lastUsedAt` descendente.
- `current` sale `false` en todas si no mandaste el header — la app debe mandarlo o resolver
  "este equipo" comparando con el `sessionId` que guardó al hacer login.
- Las sesiones cuyo token ya expiró **no aparecen** (se podan al leer).

### DELETE /api/v1/auth/sessions/{sessionId}

**Auth:** cualquier usuario autenticado → `204`.

El equipo afectado deja de poder hacer `/auth/refresh` de inmediato; su **access token sigue
siendo válido hasta que expire** (es un JWT, no se consulta Redis para validarlo). En la práctica
son minutos. Cerrar tu propia sesión es válido.

- `404 RESOURCE_NOT_FOUND` — el `sessionId` no existe o **no es tuyo**. No se distingue entre
  ambos casos a propósito: revelar que un id ajeno existe es filtrar información.

### POST /api/v1/auth/sessions/revoke-others

**Auth:** cualquier usuario autenticado. **Header opcional:** `X-Refresh-Token` → `204`.

Cierra todas las sesiones **menos** la del token que mandas. Sin el header **cierra todas,
incluida la tuya** — es el default seguro para el caso "creo que me robaron la cuenta".

### POST /api/v1/auth/change-password

**Auth:** cualquier usuario autenticado → `204`.

```json
{
  "currentPassword": "MiClaveVieja1",
  "newPassword": "MiClaveNueva2",
  "currentRefreshToken": "b7e1…"
}
```

- `newPassword`: 8-100 caracteres (las mismas reglas del registro).
- `currentRefreshToken` **opcional pero recomendado**: sin él se cierran **todas** las sesiones y
  el usuario tiene que volver a entrar en el propio equipo desde el que cambió la clave.
- Efecto: se guarda el hash nuevo, se actualiza `passwordUpdatedAt` (visible en
  `GET /api/v1/users/me`) y se revocan las demás sesiones.

**Errores**

| Código | errorCode | Cuándo |
|---|---|---|
| `401` | `UNAUTHORIZED` | `currentPassword` incorrecta |
| `400` | `BAD_REQUEST` | `newPassword` igual a la actual |
| `422` | `VALIDATION_ERROR` | `newPassword` fuera de 8-100 o campos en blanco |

### Notas para el front

- Tras `change-password` **no cierres la app**: el access token vigente sigue sirviendo y el
  refresh token que mandaste como `currentRefreshToken` sigue vivo.
- Tras `revoke-others` sin header, el siguiente `/auth/refresh` dará `401` → mandar a login.
- Los tokens emitidos **antes** de esta fase siguen funcionando pero **no aparecen en la lista**
  hasta que roten (primer `/auth/refresh`). Es esperado y transitorio.

---

## 20. Preferencias de notificaciones (Fase 20)

Backend de la pantalla **Notificaciones** del perfil. Change `notification-preferences`.
Vive en **biz** (no en auth) porque es biz quien emite los pushes: poner las preferencias en auth
obligaría a un Feign call sincrónico dentro del fan-out.

### Los 7 toggles

Los grupos son presentación y los arma el front; el backend devuelve los siete planos.

| Grupo | Campo | Label del mockup | Default | ¿Emisor hoy? |
|---|---|---|---|---|
| Pedidos | `newOrder` | "Nuevo pedido" | `true` | **Sí** — push `NEW_ORDER` |
| Pedidos | `orderExpiring` | "Pedido por expirar" (aviso ~2 min antes) | `true` | No |
| Tablet | `tabletDisconnected` | "Tablet desconectada" | `true` | No |
| Tablet | `productOutOfStock` | "Producto agotado en vitrina" | `true` | No |
| Tablet | `lowBattery` | "Batería baja" | `true` | No |
| Tu negocio | `weeklyReport` | "Reporte semanal" (lunes, por WhatsApp) | `true` | No |
| Tu negocio | `sellingSuggestions` | "Sugerencias para vender más" | `true` | No |

> **Solo `newOrder` está cableado.** Los otros seis se guardan y se devuelven, pero **nada los
> emite todavía**: cada uno necesita su job (umbral, ventana y dedup para no spamear). El contrato
> ya está fijo, así que cuando existan no hay que migrar la tabla ni tocar la app.
>
> Si prefieres no prometer lo que aún no llega, píntalos con un "pronto".

**`orderExpiring` no es el push de `EXPIRED`.** El backend ya manda un `ORDER_TAKEN` con
`status=EXPIRED` cuando un pedido vence — eso es **sincronización**: le dice a los demás celulares
que lo saquen de "Pendientes". Este toggle es para un aviso **previo** ("te quedan 2 min"), que es
otra cosa y todavía no existe.

### GET /api/v1/notifications/preferences

**Auth:** OWNER/SELLER/ADMIN (DEVICE → 403; la tablet no recibe pushes).

```json
{
  "newOrder": true,
  "orderExpiring": true,
  "tabletDisconnected": true,
  "productOutOfStock": true,
  "lowBattery": false,
  "weeklyReport": true,
  "sellingSuggestions": true,
  "activeCount": 6,
  "totalCount": 7
}
```

- **Nunca devuelve 404 ni nulls.** Sin fila guardada responde con los defaults, así la pantalla se
  pinta igual la primera vez que se abre.
- `activeCount`/`totalCount` alimentan el subtítulo del menú sin que el front hardcodee el 7.
- Leer **no crea** la fila: aparece con el primer `PUT`.
- Las preferencias son **por tienda activa**: tras un `switch-store` la respuesta puede cambiar.

### PUT /api/v1/notifications/preferences

**Auth:** OWNER/SELLER/ADMIN. Update **parcial** → 200 con el set completo ya aplicado.

```json
{ "lowBattery": false }
```

- Los campos ausentes (o `null`) **no se tocan**. Manda solo el toggle que el usuario acaba de
  mover; así dos celulares del mismo usuario editando a la vez no se pisan.
- Es upsert: el primer `PUT` crea la fila partiendo de los defaults.
- Body vacío `{}` es válido y no cambia nada.

### Qué pasa con los pushes

| Evento del push | Bandera |
|---|---|
| `NEW_ORDER` | `newOrder` |
| `ORDER_TAKEN` (`CONFIRMED`/`REJECTED`/`EXPIRED`) | **ninguna — siempre se entrega** |

**La ausencia de preferencias nunca silencia.** El filtro consulta a quienes *apagaron* la
bandera, no a quienes la encendieron; sin fila, con la fila ilegible o con la consulta caída, el
push sale igual. Silenciar por accidente un pedido nuevo le cuesta una venta al comerciante.

### Qué NO cubre esta fase

Horario de silencio ("no molestar"), preferencias por canal, y **los seis jobs que faltan** para
que todo lo que no sea `newOrder` llegue a emitir algo.

---

## 21. Ajustes remotos de la tablet (Fase 21)

Backend de la pantalla **Ajustes de la tablet** del perfil. Change `remote-tablet-settings`.
Vive en **auth**, que es quien posee `devices`.

### Los ajustes

| Grupo del mockup | Campo | Label | Default |
|---|---|---|---|
| Pantalla del cliente | `idleShowcaseEnabled` | "Modo Vitrina en reposo" | `true` |
| Pantalla del cliente | `voiceAssistantEnabled` | "Asistente por voz" | `true` |
| Pantalla del cliente | `brightnessMode` | "Brillo" | `AUTO` |
| Seguridad | `kioskMode` | "Modo kiosco" | `true` |
| Seguridad | `exitPin` | "PIN de salida" (**write-only**) | sin PIN |
| Catálogo | `autoSyncEnabled` | "Sincronizar automáticamente" | `true` |

`brightnessMode` es un **selector**: `AUTO | LOW | MEDIUM | HIGH`. No es un porcentaje porque un
valor fijo se ve distinto en cada modelo y queda ilegible bajo el sol de un pasillo; el
comerciante declara una intención y Android resuelve el nit con su sensor.

**No hay intervalo de sync ni timeout de reposo configurables** — el mockup no los expone y el
backend no tiene por qué opinar sobre cada cuánto late una tablet.

### El bloque INFORMACIÓN

Sale de `GET /api/v1/devices` / `GET /api/v1/devices/{id}`, no de este endpoint:

| Fila del mockup | Campo |
|---|---|
| Modelo ("Lenovo Tab M11") | `model` — **nuevo en V16** |
| N.º de serie | `serialNumber` |
| Vinculada desde | `linkedAt` |
| Versión de la app | `appVersion` |
| Tienda asignada | `storeName` |
| Nombre | `name` (editable con `PATCH /devices/{id}`) |

`model` lo reporta la tablet en el heartbeat (`{"model": "Lenovo Tab M11"}`, opcional, máx. 100).
Es telemetría como la batería: si viene se guarda, y **nunca rompe el latido**. Las tablets
vinculadas antes de V16 lo llenan en su siguiente heartbeat.

### GET /api/v1/devices/{deviceId}/settings

**Auth:** OWNER/ADMIN, solo dispositivos de la tienda activa.

```json
{
  "deviceId": "…",
  "idleShowcaseEnabled": true,
  "voiceAssistantEnabled": true,
  "brightnessMode": "AUTO",
  "kioskMode": true,
  "exitPinSet": true,
  "autoSyncEnabled": true,
  "settingsVersion": 4
}
```

- **El PIN nunca vuelve** — ni en claro ni hasheado. Solo `exitPinSet`. Si el dueño lo olvidó, lo
  reemplaza; no hay razón para poder leerlo.
- `404` si el dispositivo no es de la tienda activa (no se distingue de "no existe": revelar que
  un id ajeno existe es filtrar información).
- Un dispositivo vinculado antes de esta fase no tiene fila; la lectura la crea con los defaults.

### PUT /api/v1/devices/{deviceId}/settings

**Auth:** OWNER/ADMIN. Update **parcial** → 200 con el set completo ya aplicado.

```json
{ "kioskMode": false, "brightnessMode": "HIGH" }
```

- Los campos ausentes **no se tocan**.
- `exitPin`: 4-6 dígitos, se guarda con BCrypt. **Para borrarlo hay que mandar
  `"removeExitPin": true`** — un `exitPin` ausente significa "no lo toques", y un borrado
  accidental dejaría la tablet abierta en el stand sin que nadie se entere.
- Cada escritura **sube `settingsVersion`**.
- `422` si el PIN no cumple el formato o `brightnessMode` no es uno de los cuatro valores.

### GET /api/v1/devices/me/settings

**Auth:** DEVICE. Resuelve el dispositivo del JWT — no hay path param.

Devuelve lo mismo que el endpoint del comerciante pero cambiando `exitPinSet` por
**`exitPinHash`** (BCrypt, o `null` si no hay PIN).

> Se manda el hash para que el kiosko valide la salida **sin red**. Una tablet offline que no
> puede salir del modo kiosco es una tablet inservible clavada en el stand — y el PIN frena a un
> cliente curioso, no a alguien que ya tiene el dispositivo en la mano y puede reiniciarlo.

Cachearlo en almacenamiento cifrado del dispositivo. Se invalida solo: al cambiar el PIN sube
`settingsVersion` y la tablet re-lee.

### Cómo se entera la tablet: `settingsVersion`

`POST /api/v1/devices/heartbeat` gana un campo:

```json
{ "received": true, "serverTime": "…", "catalogVersion": 172…, "settingsVersion": 4 }
```

```
heartbeat (**cada 60 s** — ver la nota de intervalo en la Fase 21)
   └─ ¿settingsVersion ≠ el que tengo guardado?
        └─ sí → GET /devices/me/settings  y aplicar
```

Nada de polling nuevo y nada de push a la tablet (los tokens FCM son de los celulares del staff,
la tablet no tiene). Es la misma semántica que `catalogVersion` ya tenía.

### Qué NO cubre esta fase

Horarios ("kiosco activo de 9 a 21"), ajustes por tienda que se propaguen a varias tablets, los
botones remotos "Actualizar catálogo" y "Reiniciar" de la pantalla Dispositivos, y **aplicar** los
ajustes: el backend los guarda y los sirve; bajar el brillo o arrancar la vitrina es trabajo de la
app del kiosko. Mientras la app no lea `settingsVersion`, los ajustes se guardan sin efecto
visible — es aditivo, nada se rompe.

---

## 22. Configuración de la tienda: idioma (Fase 22, revisado en la 27)

Backend de la pantalla **Configuración**. Change `store-display-settings`.
**No hay endpoint nuevo**: el campo entra por `GET/PUT /api/v1/stores/{storeId}`, que ya es
donde viven `whatsapp` y `address`.

> **El toggle de IGV se retiró** en la Fase 27 (triage 2026-10-18, BUG-012).
> `pricesIncludeIgv` e `igvRate` ya no viajan. Lo que sigue describe solo `language`.

```json
{
  "id": "…", "name": "El Martillo", "slug": "el-martillo",
  "language": "es",
  "…": "…"
}
```

| Campo | Valores | Default |
|---|---|---|
| `language` | `"es"` \| `"en"` | `"es"` |

`PUT` es parcial (los campos ausentes no se tocan) y requiere OWNER/ADMIN. Un `language` fuera de
los dos valores da **400** con el detalle listando los válidos.

### `language` se guarda, todavía no se aplica

Es el idioma del **kiosko** — lo que lee y habla el cliente frente a la tablet — no el de la app
del comerciante, que es una preferencia local de Android.

Hoy se persiste y se sirve, nada más. Que `vo-api` conteste en inglés exige traducir el prompt del
sistema y validar que las respuestas no salgan en spanglish con un catálogo cuyos productos tienen
nombre en español. Eso se prueba con el LLM delante; es su propio change.

### Qué hace el PDF

Bajo el TOTAL REFERENCIAL imprime siempre la misma línea: **"Precios con IGV incluido."**

**No suma el IGV al total.** Ya es un documento referencial que dice que el precio final se
confirma en la tienda; calcularle un tributo le daría una precisión que no tiene.

Antes la línea tenía dos variantes según la tienda. Ver la Fase 27 para por qué ahora hay una sola.

---

## 23. Comandos remotos a la tablet (Fase 23)

Backend de los botones **"Actualizar catálogo"** y **"Reiniciar"** de la card de Dispositivos.
Change `device-remote-commands`.

### No es instantáneo, y la pantalla tiene que decirlo

No hay push hacia la tablet: los tokens FCM son de los celulares del staff, y el WebSocket del
repo es exclusivo del pairing. El comando viaja en el **heartbeat**, que la tablet ya emite.

```
comerciante pulsa  →  POST /devices/{id}/commands   →  202 PENDING
                              ⋮  (hasta un ciclo de heartbeat)
tablet late        →  pendingCommands: [{id, command}]  →  DELIVERED
tablet ejecuta     →  POST /devices/me/commands/{id}/ack →  ACKED | FAILED
```

**La card debe mostrar "Enviado" y recién "Listo" con el ack.** Prometer inmediatez que no existe
es peor que la espera.

### POST /api/v1/devices/{deviceId}/commands

**Auth:** OWNER/ADMIN, solo dispositivos de la tienda activa → **202**.

```json
{ "command": "REFRESH_CATALOG" }
```

| Comando | Qué hace la tablet |
|---|---|
| `REFRESH_CATALOG` | Fuerza un `GET /device-sync/manifest` fuera de ciclo |
| `RESTART_APP` | Reinicia **la app del kiosko**, no el sistema operativo |

Respuesta:

```json
{
  "id": "…", "deviceId": "…",
  "command": "REFRESH_CATALOG",
  "status": "PENDING",
  "requestedAt": "…", "expiresAt": "…",
  "deliveredAt": null, "ackedAt": null, "resultDetail": null
}
```

- **Idempotente mientras hay uno en vuelo**: si ya existe uno del mismo tipo en `PENDING` o
  `DELIVERED`, devuelve **ese mismo id**. Un comerciante impaciente genera un comando, no cinco.
- Contra comandos ya terminados **no** deduplica: volver a pulsar media hora después es legítimo.
- `404` si el dispositivo no es de la tienda activa · `400` si el comando no existe.

### GET /api/v1/devices/{deviceId}/commands

**Auth:** OWNER/ADMIN. Los últimos 20, más nuevo primero, con `status`, `requestedAt`, `ackedAt` y
`resultDetail`.

### El heartbeat entrega

`POST /api/v1/devices/heartbeat` gana:

```json
{ "received": true, "…": "…", "settingsVersion": 4,
  "pendingCommands": [ { "id": "…", "command": "REFRESH_CATALOG" } ] }
```

Lista vacía cuando no hay nada. **Entregarlos los marca `DELIVERED` y no se repiten.**

### POST /api/v1/devices/me/commands/{commandId}/ack

**Auth:** DEVICE. El dispositivo sale del JWT → **204**.

```json
{ "success": false, "detail": "sin conexión al catálogo" }
```

`success: true` → `ACKED`. `false` → `FAILED`, y el `detail` (máx. 300, truncado) aparece en la
card para que el comerciante vea algo más útil que "Falló". Un comando de **otra** tablet → `404`.

### Expiración: 10 minutos, sin reintentos

Un comando que nadie recoge dentro de su TTL pasa a `EXPIRED`, y el heartbeat nunca lo entrega. Un
"Reiniciar" pulsado el viernes por la tarde no puede ejecutarse el lunes cuando alguien prende la
tablet.

**Un comando entregado tampoco se re-entrega.** Si la tablet muere entre recibirlo y confirmarlo,
expira. Para `REFRESH_CATALOG` reintentar sería inofensivo, pero para `RESTART_APP` no: una tablet
que reinicia, no alcanza a confirmar y vuelve a recibir la orden queda en bucle — inutilizada por
el mismo mecanismo que debía arreglarla. El reintento es un botón, no un job.

### Estados

| Estado | Qué mostrar |
|---|---|
| `PENDING` | "Enviado" |
| `DELIVERED` | "Enviado" (la tablet lo recibió, aún no confirma) |
| `ACKED` | "Listo" |
| `FAILED` | "Falló: {resultDetail}" |
| `EXPIRED` | "No se pudo entregar" — ofrecer reintentar |

### Qué NO cubre esta fase

Comandos a varias tablets a la vez, comandos programados, reintentos automáticos, y **ejecutarlos**:
el backend encola, entrega y guarda el desenlace; ejecutar es la app del kiosko. Mientras no lea
`pendingCommands`, los comandos se encolan y expiran solos — aditivo, nada se rompe.

---

## 24. Métricas del kiosko: qué mide cada número (Fase 24)

### Cómo se registra una visita

Hasta la Fase 23, `kiosk_events` **solo** se llenaba desde
`POST /api/v1/analytics/events`, que la tablet tiene que llamar a propósito.
Mientras la app no lo hiciera, la pestaña Actividad quedaba en cero aunque
hubiera clientes usando la tablet todo el día.

Ahora el backend aprovecha una llamada que la tablet **ya hace**:

```
GET /api/v1/products/{id}
X-Kiosk-Session: <id de la sesión del kiosko>     ← opcional
```

Si quien consulta tiene rol **DEVICE**, se registra un `PRODUCT_VIEW`. El
endpoint sigue respondiendo exactamente lo mismo; el registro es un efecto
lateral.

| Quién llama | ¿Suma a las métricas? |
|---|---|
| Tablet (rol `DEVICE`) | **Sí** |
| App del staff (OWNER / SELLER / ADMIN) | **No** |

El comerciante revisando un precio desde su celular no es un cliente parado
frente a la vitrina. Contarlo inflaría la métrica con el trabajo del propio
dueño y la volvería inútil para decidir nada.

**`X-Kiosk-Session`** es el mismo identificador que el kiosko ya manda en
`POST /orders` y en la búsqueda por voz. Sin el header la visita se cuenta
igual, pero esa persona no entra en "clientes distintos" — mandalo siempre que
puedas.

**Nunca rompe la ficha**: si el registro falla, se loguea un warning y el
producto se devuelve igual. Que falle la analítica no puede impedirle a un
cliente ver un producto.

`POST /api/v1/analytics/events` **sigue existiendo y sigue siendo necesario**
para lo que el backend no puede observar: `SESSION_START`, `SESSION_END` y
`SEARCH_MADE`.

### Qué mide cada número de la pestaña Actividad

`GET /api/v1/dashboard/activity`

```json
{
  "granularity": "WEEKLY",
  "periodStart": "…", "periodEnd": "…",
  "uniqueSessions": 72,          "uniqueSessionsDeltaPercent": -12.5,
  "productViews": 264,           "productViewsDeltaPercent": 8.1,
  "confirmedOrders": 9,          "confirmedOrdersDeltaPercent": 12.5
}
```

| Campo | Qué es exactamente |
|---|---|
| `uniqueSessions` | **Personas que usaron la tablet**, hayan comprado o no. Sesiones distintas con cualquier evento en el periodo. |
| `productViews` | Fichas de producto abiertas en la tablet. Una persona puede generar varias. |
| `confirmedOrders` | **Pedidos confirmados** en el periodo. Este sí es "cliente que concretó". |

> **`uniqueSessions` NO es "clientes confirmados".** Es tráfico. Si la pantalla
> rotula ese número como "Clientes" a secas, se lee como clientes que
> compraron, que es otra cosa y mucho menor. Por eso `confirmedOrders` viene en
> la misma respuesta: los tres juntos son el embudo
> (*gente → fichas miradas → pedidos*) y por separado se malinterpretan.

`uniqueSessions` cuenta sesiones con **cualquier** evento. Antes exigía un
`SESSION_START`, lo que ataba la métrica a que la app emitiera ese evento
concreto: una tablet que reportaba vistas pero no la apertura de sesión
mostraba *0 clientes y 264 productos consultados*, que es una combinación
imposible de leer.

### "Lo que necesita atención": qué es y qué no

`GET /api/v1/dashboard/ranking` → `needsAttention`

```json
"needsAttention": {
  "totalImmobilized": 2990.00,
  "totalCount": 17,
  "items": [ { "name": "…", "stock": 34, "price": 85.00, "immobilizedAmount": 2890.00 } ]
}
```

La definición exacta es: **productos activos, con stock mayor a cero, que no
registraron ninguna venta confirmada dentro del periodo consultado**, ordenados
de mayor a menor `stock × precio`.

Tres cosas que conviene tener claras:

1. **No es ingreso, es lo contrario.** `immobilizedAmount` es la plata que está
   quieta en el estante. Rotularlo "generado por" en la pantalla lo vuelve
   indistinguible de las ventas que están justo al lado.
2. **`totalImmobilized` es la suma de los `items` devueltos**, no del universo
   completo. Para eso está `totalCount` — permite decir "3 de 17 sin rotación"
   en vez de dar a entender que son tres y nada más.
3. **Depende del periodo elegido.** En una vista semanal, un producto que rota
   una vez al mes va a aparecer casi siempre. Cuanto más corto el periodo, más
   ruido: para esta tarjeta, mensual dice más que diario.

### Miniatura en el detalle del pedido

`GET /api/v1/orders/{id}` → cada entrada de `items` suma:

```json
{ "productId": "…", "productName": "Taladro percutor", "quantity": 1,
  "unitPriceSnapshot": 50.00, "productImageUrl": "https://pub-….r2.dev/…jpg" }
```

Es la imagen **principal vigente** del producto, no un snapshot: si el
comerciante cambia la foto, el pedido viejo muestra la nueva. Mismo criterio
que `productName`, y el correcto acá — el vendedor mira el pedido para
reconocer el producto que tiene en el estante hoy.

`null` si el producto no tiene imágenes; la app pinta el placeholder.

Se resuelve con **una consulta por pedido**, no una por línea: `Product.media`
es `LAZY` y tocarla por ítem sería un N+1 contra `product_media`.

---

## 25. Precio con descuento y aviso de expiración (Fase 25)

Dos correcciones nacidas del triage de bugs del 2026-10-18
(`docs/qa/TRIAGE-BUGS.md`, BUG-008 y BUG-013).

### El backend ahora calcula el precio con descuento

Hasta esta fase el backend **guardaba** los descuentos y los devolvía crudos en
`discounts[]`, dejando la aritmética a cada cliente. Ninguno la hacía: el mismo
producto se veía a precio lleno en la grilla del Discovery, en la lista de
Inventario y en el PDF.

Ahora el precio efectivo lo calcula el servidor y viaja en **dos campos nuevos**:

| Campo | Tipo | Significado |
|---|---|---|
| `discountedPrice` | `number \| null` | Precio ya con el descuento aplicado |
| `discountEndsAt` | ISO-8601 `\| null` | Fin del descuento aplicado |

`null` significa **sin descuento vigente**: el cliente pinta solo `price`.

Están en:

- `GET /api/v1/products` — cada fila (`ProductSummaryResponse`)
- `GET /api/v1/products/{id}` — además de `discounts[]`, que se mantiene
- `GET /api/v1/catalog/by-category` — cada producto de cada grupo
- `POST /api/v1/conversations/search` (vo) — cada `results[*]`, desde la [Fase 29](#29-búsqueda-por-voz-precio-con-descuento-y-productos-no-encontrados-fase-29)

```json
{
  "id": "…",
  "name": "Foco LED Philips Globo E27 13W Luz Cálida",
  "price": 35.90,
  "discountedPrice": 30.90,
  "discountEndsAt": "2026-10-25T05:00:00Z"
}
```

#### Las reglas, que ahora son una sola

| Regla | Decisión |
|---|---|
| Varios descuentos vigentes | **Gana el más barato**, no se encadenan |
| Ventana | `startsAt <= ahora < endsAt` (fin exclusivo) |
| Redondeo | `HALF_UP` a 2 decimales |
| Monto mayor que el precio | Tope en 0, nunca negativo |
| Descuento que no baja nada | `discountedPrice: null` |

**No se encadenan a propósito.** Dos promociones solapadas podrían regalar el
producto sin que nadie lo haya decidido.

**Fail-open**: si la consulta de descuentos falla, el listado responde 200 con
los precios de lista y registra un warning. Un catálogo sin etiqueta de oferta se
sigue vendiendo; uno que no carga, no.

`discounts[]` sigue en el detalle para quien quiera mostrar el detalle de la
promo, pero **el número autoritativo es `discountedPrice`** — repetir la
aritmética en el cliente es volver al bug.

### Aviso "este pedido está por vencer"

El toggle `orderExpiring` de la Fase 20 existía sin emisor. Ahora lo tiene.

| Pieza | Valor |
|---|---|
| Evento del push | `ORDER_EXPIRING` |
| `status` del payload | `PENDING` — el pedido **no** cambió de estado |
| Antelación | `ORDERS_EXPIRING_WARNING_MINUTES`, default **10** |
| Apagarlo | Poner la variable en `0` |
| Respeta preferencia | Sí, el toggle `orderExpiring` (opt-out) |

```json
{ "event": "ORDER_EXPIRING", "orderId": "…", "storeId": "…", "status": "PENDING" }
```

**Se manda una sola vez por pedido**, garantizado por la columna
`orders.expiring_notified_at` (biz V18) y un `UPDATE` condicional: varias
instancias del backend barriendo a la vez mandan un solo aviso.

**El `status` se mantiene en `PENDING` a propósito.** Mandar otra cosa haría que
la app sacara el pedido de "Pendientes", que es lo contrario de lo que pide el
aviso.

#### Lo que falta del lado de la app

`OrderNotificationPresenter.contentFor` no conoce el evento todavía, así que cae
en el texto genérico *"Pedido actualizado"*. Necesita su rama con los strings
propios. El push llega y suena igual.

---

## 26. Correcciones del triage del 2026-10-18 (Fase 26)

Cinco arreglos de backend del segundo lote de bugs
(`docs/qa/TRIAGE-BUGS.md`, BUG-015 a BUG-020). Ninguno rompe un contrato
existente: todo es campo nuevo o comportamiento que antes estaba mal.

### `stock` en el listado de productos

`GET /api/v1/products` ahora devuelve `stock` por fila, el mismo
`products.stock` del detalle.

```json
{ "id": "…", "name": "Caja de herramientas metálica 20\"", "price": 95.00, "stock": 1 }
```

`null` en productos legacy y en modo "consultar".

**Por qué importa.** Faltaba, y la app de staff lo estaba **inventando**:
derivaba un número del hash del UUID (`(abs(id.hashCode()) % 90) + 10`). El
comerciante veía "19 stock", editaba y encontraba 1. El front tiene que borrar
ese helper y leer el campo.

### "Clientes" ya no se queda en cero

`GET /api/v1/dashboard/activity` → `uniqueSessions` contaba
`COUNT(DISTINCT session_id)`, y como la tablet no manda `X-Kiosk-Session` todas
las filas entraban con `NULL`. Resultado: *0 clientes · 27 productos
consultados*.

Ahora, cuando el header **no** viene, el backend infiere la sesión por ventana de
inactividad del mismo `device_id`:

| Caso | Sesión asignada |
|---|---|
| Vino `X-Kiosk-Session` | Esa |
| Actividad de la tablet hace < N min | La misma |
| Sin actividad reciente | Nueva, con prefijo `srv-` |

`N` = `DASHBOARD_KIOSK_SESSION_GAP_MINUTES`, default **15**. `0` apaga la
inferencia.

**Precisión.** Dos personas separadas por menos de N minutos cuentan como una. Es
el error de cualquier analítica por inactividad, y es menor que contar cero. Para
exactitud, la app debe mandar el header — **siempre gana sobre lo inferido**.

### El botón de WhatsApp del PDF

`buildMerchantWaLink` concatenaba `"51"` **siempre**, sin mirar si el número ya lo
traía: `+51 987 654 321` terminaba en `wa.me/5151987654321` y WhatsApp respondía
que el número no existe.

Ahora se normaliza a MSISDN peruano:

| Entrada | Salida |
|---|---|
| `987654321` | `51987654321` |
| `+51 987 654 321` | `51987654321` |
| `0051987654321` | `51987654321` |
| otra cosa | **se omite el botón** |

Omitir es deliberado: un botón que lleva a un número inexistente es peor que
ninguno.

### La bajada del PDF

`vitria.qhatia.pdf-sub-brand` (`QHATIA_PDF_SUB_BRAND`), **vacío por defecto**.
Estaba fijo en `"by VitrIA"`, el nombre técnico del repositorio; la marca del
cliente final es Qhathia. Vacío = no se dibuja ni bajo el logo ni en el pie.

### Comandos a la tablet: los vencidos dejan de bloquear

`POST /api/v1/devices/{deviceId}/commands` rechazaba encolar si ya había uno en
vuelo, pero "en vuelo" miraba solo el estado y nunca `expires_at`. Con la tablet
apagada el comando se quedaba en `DELIVERED` hasta la purga de las **03:30**, y
el comerciante no podía reenviar el catálogo hasta la madrugada.

Dos cambios:

1. La deduplicación exige `expires_at > now`. Un comando vencido ya no bloquea.
2. `GET /api/v1/devices/{deviceId}/commands` cierra los vencidos antes de
   responder, así la card muestra `EXPIRED` en vez de `DELIVERED` para siempre.

**La política de no reintentar no cambió**: re-entregar un `RESTART_APP` dejaría
la tablet en bucle.

### "Productos no encontrados": términos canónicos y sin duplicar

`GET /api/v1/insights/unmet-searches` (vo) devolvía frases enteras como productos
—*"Quiero una amoladora"*— y además duplicadas contra la versión limpia
—*"Amoladora"*— porque Mongo agrupa por el valor literal y la normalización
corría después.

Ahora el término se canoniza antes de comparar:

| Paso | Ejemplo |
|---|---|
| Puntuación final | `"lámparas."` → `"lamparas"` |
| Arranque conversacional, en cadena | `"quiero ver una amoladora"` → `"amoladora"` |
| Plural regular | `"lamparas"` → `"lampara"` |

Y los grupos que colapsan al mismo término se fusionan **después** de la
agregación, **uniendo las sesiones en vez de sumarlas**: quien buscó de las dos
formas es una persona, no dos.

`examples[]` sigue trayendo los textos crudos — el término se limpia para
agrupar, pero la evidencia de qué modelo comprar está en lo que la gente dijo.

---

## 27. Se retira el toggle de IGV (Fase 27)

**Cambio de contrato.** `pricesIncludeIgv` e `igvRate` salen de
`GET /api/v1/stores/{storeId}` y de `PUT /api/v1/stores/{storeId}`.

### Por qué

El flag era una **declaración, no un cálculo**: su único efecto era elegir entre dos líneas de
texto del PDF. Apagarlo no movía ningún precio, y así se reportó como bug (triage 2026-10-18,
BUG-012) — con razón, porque un control que no produce efecto observable se lee como roto.

Se retiró en vez de hacerlo calcular. En Perú el precio al consumidor lleva IGV incluido por ley
y así es como las tiendas ya cargaban `products.price`: **todas estaban en `true`**. Hacerlo
calcular habría sido trabajo en doce superficies con precio para un caso de uso que no está
confirmado que exista.

### Qué significa ahora

`products.price` **es el precio final que paga el consumidor**, siempre. No hay configuración por
tienda y el backend no hace aritmética de impuestos en ningún lado.

### Compatibilidad: no rompe nada instalado

| Dirección | Qué pasa |
|---|---|
| App vieja **manda** `pricesIncludeIgv` en el `PUT` | **200**. Spring Boot ignora propiedades desconocidas; hay un test que lo fija |
| App vieja **espera** el campo en la respuesta | El DTO de Kotlin tiene default, así que su ausencia no rompe la deserialización |

### La columna sigue en la base

`stores.prices_include_igv` se conserva (**auth V19** solo reescribe su `COMMENT`). Queda sin
mapear en la entidad y fuera de la API.

Borrarla es destructivo a cambio de un byte por tienda. Y si algún día aparece un comerciante que
carga precios netos, el diseño correcto **no** es reescribir el catálogo: es derivar el precio al
público en **lectura** (`price × 1.18`) dejando `products.price` intacto — reversible, sin
migración— y esta columna es donde vive esa declaración.

### Queda del front

Borrar el toggle y sus 14 archivos de alcance (`AppSettingsScreen`,
`SetPricesIncludeIgvUseCase`, el campo de `StoreSettings`, el parámetro de `StoreApiService`,
mappers, fakes y los strings `settings_igv*`). Mientras tanto el toggle manda un campo que se
ignora y lee uno que no llega: se queda fijo en "encendido" sin hacer nada.

---

## 28. Filtros de Inventario y rango de precio por voz (Fase 28)

Triage 2026-10-18, BUG-022 y BUG-024.

### `GET /api/v1/products`: los filtros se cruzan

`categoryId` y `availability` **se combinan**. Antes el backend elegía uno con un
`if/else if` y la categoría ganaba: pedir "Iluminación interior" + "Stock bajo"
devolvía la categoría entera, con productos de 78 de stock.

### `availability=LOW_STOCK` ahora significa algo

La sync `stock ↔ availability` produce solo `CONSULT` (stock null),
`OUT_OF_STOCK` (0) y `AVAILABLE` (>0). **`LOW_STOCK` no lo asigna nadie**, así que
filtrar por ese valor devolvía cero siempre.

Ahora el backend lo traduce a un rango: `0 < stock <= DASHBOARD_RESTOCK_THRESHOLD`
(default **5**) — **el mismo umbral** que "Productos para reponer" en Inicio. El
valor del parámetro no cambia, así que la app no necesita tocar nada.

```
GET /api/v1/products?categoryId=<uuid>&availability=LOW_STOCK
→ los productos de esa categoría con 1..5 de stock
```

### El rango de precio de la búsqueda por voz

`POST /api/v1/conversations/search` (vo) aplica ahora **piso y techo**.

`minPrice` ya se extraía —el prompt del LLM lo pedía y el DTO lo llevaba— pero
**nadie filtraba por él**: solo se aplicaba el máximo. *"Lámpara de más de 50
soles"* devolvía las de menos de 50.

El extractor por reglas también invertía la frase: tenía el prefijo opcional, así
que el `50` suelto de "más de 50" entraba como techo. Ahora hay un patrón por
sentido, con prefijo obligatorio:

| Frase | Resultado |
|---|---|
| "menos de 50", "hasta 50", "por debajo de 50" | techo 50 |
| "más de 50", "desde 50", "a partir de 50" | piso 50 |
| "no más de 80" | techo 80 (no piso) |
| "desde 20 hasta 60" | rango 20–60 |
| "lámpara 200 soles" | techo 200 — un número suelto sigue siendo presupuesto |

`search_events` guarda también `minPrice`, para que la analítica vea la consulta
completa.

---

## 29. Búsqueda por voz: precio con descuento y productos no encontrados (Fase 29)

Reporte del front del 2026-10-23. Dos arreglos en vo, ninguno rompe un contrato.

### `POST /conversations/search` trae el precio con descuento

Hasta esta fase la búsqueda por voz devolvía solo `price`. El mismo producto se
veía rebajado en el catálogo y a precio lleno en el resultado de la voz, en la
misma tablet.

Ahora cada `results[*]` lleva los mismos dos campos que biz:

| Campo | Tipo | Significado |
|---|---|---|
| `discountedPrice` | `number \| null` | Precio con el mejor descuento vigente |
| `discountEndsAt` | ISO-8601 `\| null` | Fin del descuento aplicado |

```json
{
  "id": "…",
  "name": "Lámpara colgante",
  "price": 55.00,
  "discountedPrice": 45.00,
  "discountEndsAt": "2026-10-31T05:00:00Z",
  "relevanceScore": 3
}
```

Aplica a los tres casos: resultados normales, sugerencias con
`suggestedFromCategory` y sugerencias sin él.

**Las reglas son las de la [sección 25](#25-precio-con-descuento-y-aviso-de-expiración-fase-25), sin excepción.**
Gana el más barato, no se acumulan, ventana `startsAt <= ahora < endsAt`, `HALF_UP` a 2
decimales, tope en 0, `null` si no baja nada. vo es un proyecto aparte y no puede
importar la clase de biz, así que la regla está **copiada** con los mismos tests y
los mismos números. Si cambia una, cambia la otra.

**Se calcula al responder, no al indexar.** La proyección de Mongo guarda cada
descuento con su ventana; el precio sale en el momento de la request. Un descuento
que vence a las 18:00 deja de aplicar a las 18:00 sin re-proyectar nada.

**Si la búsqueda cae al respaldo** (Mongo vacío o caído), vo usa el
`discountedPrice` que ya calcula `GET /products` de biz.

### El rango de precio mide lo que paga el cliente

`minPrice` y `maxPrice` comparan contra `discountedPrice ?: price`:

| Pedido | Producto | ¿Entra? |
|---|---|---|
| "más de 50 soles" | S/ 55 rebajado a S/ 45 | No |
| "hasta 50 soles" | S/ 55 rebajado a S/ 45 | Sí |
| "más de 50 soles" | S/ 60 sin descuento | Sí |

### El texto hablado dice el precio rebajado

`spokenText` lo arma el LLM a partir de una lista de productos con su precio.
Antes esa lista llevaba el de lista; ahora lleva el rebajado y aclara que tiene
descuento. El asistente ya no dice en voz alta un número que la pantalla muestra
tachado.

### `GET /insights/unmet-searches`: menos oraciones, ningún adjetivo

Cinco tarjetas reales del reporte, y qué pasa con cada una:

| Lo que dijo el cliente | `term` antes | `term` ahora |
|---|---|---|
| "Dame algo de más de 50 soles." | `algo de mas de 50 sol` | **no aparece**: no pide un producto |
| "Algo algo más de 50 soles" | `algo algo mas de 50 sol` | **no aparece** |
| "Una mesa giratoria" | `giratoria` | `mesa giratoria` |
| "…decirle algo Amoladora T 50 soles." | `amoladora` | `amoladora` |
| "Paladros." | `pallet` | **no aparece si el LLM declaró baja confianza** |

Lo que cambió, en orden:

1. **La clave del grupo es `categoría + subtipo`.** Antes prefería el `subtype`, que
   el LLM llena con adjetivos.
2. **Se quita la expresión de precio y las muletillas** ("algo", "cosa", "eso"). Si
   no queda sustantivo, el grupo se descarta. Un número sin moneda se conserva:
   "amoladora 4.5" es un modelo.
3. **`search_events` guarda `confidence`.** Debajo de **0.4** la interpretación es
   una adivinanza y no entra. Los eventos anteriores, sin ese campo, entran.

La búsqueda se sigue registrando igual; solo cambia lo que se le muestra al
comerciante como algo que podría comprar.

**Lo que esto no arreglaba solo**: un error del STT que el LLM adivinaba con
confianza alta. Eso lo ataca la [Fase 30](#30-la-ia-de-la-búsqueda-conoce-la-tienda-fase-30).

---

## 30. La IA de la búsqueda conoce la tienda (Fase 30)

Sin cambio de contrato: mismos endpoints, mismos campos. Cambia lo que la IA sabe
cuando interpreta lo que dijo el cliente.

### Antes

Los dos prompts de vo —el que interpreta la consulta y el que arma el texto
hablado— decían **"tienda de muebles y decoración en Perú"** para todas las
tiendas, con silla, mesa y lámpara de ejemplo. Una ferretería se interpretaba con
ojos de mueblería. Y cuando el reconocimiento de voz transcribía mal
("Paladros" por "taladros"), la IA adivinaba sin pistas: "pallet".

### Ahora, y de dónde sale cada dato

| Dato | De dónde sale | Quién lo llena |
|---|---|---|
| **Rubro** ("ferretería y herramientas") | Claim `storeCategory` del JWT | Nadie: se elige al crear la tienda |
| **Productos que vende** | Su propio catálogo en la proyección de Mongo: nombres de categoría y el sustantivo con que empieza cada producto | Nadie: se arma solo y se renueva cada 10 min |
| **Tags permitidos** | Catálogo de tags del mall y rubro | Sin cambios (Fase 5) |

Ejemplo de vocabulario de una ferretería:
`herramientas eléctricas, iluminación interior, taladro, amoladora, foco, …`

El sustantivo se saca salteando lo genérico y la marca: "Kit de brocas" da
`brocas`, "Truper martillo de uña" da `martillo`. Tope de 20 categorías y 40
sustantivos.

### Qué se le pide a la IA

- **Usar la lista para corregir errores de transcripción**, no para filtrar. Si el
  cliente pide claramente algo que la tienda no tiene, se respeta: eso es lo que
  muestra "Productos no encontrados".
- **`category: null` cuando no se nombra un producto**: un precio solo, "algo",
  un saludo.
- **`confidence` de 0.3 o menos cuando adivinó** a partir de una palabra sin
  sentido. Con la Fase 29, eso la saca de "Productos no encontrados".

### Fail-open

Sin Mongo o sin Redis, el vocabulario sale vacío y el prompt va sin esa sección.
Sin rubro en el token, dice "tienda de productos variados". La búsqueda nunca
falla por esto.

### Lo que queda

El intérprete de respaldo por reglas, que se usa cuando el LLM no responde,
todavía tiene una lista fija de palabras de muebles.

---

## 31. Búsqueda solo de precio (Fase 31)

Sin cambio de contrato. Reporte del 2026-10-25: "algo menor a 150 soles" no
devolvía nada, y "menor" aparecía como producto no encontrado.

### "Algo menor a 150 soles" ahora devuelve productos

El ranking solo puntuaba si la consulta traía producto, estilo, uso o color. Sin
ninguno forzaba cero, para que un "hola" no devolviera el catálogo entero. Pero eso
también atrapaba las búsquedas de precio puro.

| Consulta | Antes | Ahora |
|---|---|---|
| "algo menor a 150 soles" | `noResults: true` | Los productos de hasta S/ 150, más baratos primero |
| "algo de más de 100 soles" | `noResults: true` | Los de S/ 100 o más, más baratos primero |
| "hola" | `noResults: true` | Igual: sugerencias de siempre |

El rango y el orden usan lo que paga el cliente: `discountedPrice ?: price`. Hasta
20 resultados, con `filters` y `suggestions` normales. Como hay resultados, no se
registra como búsqueda fallida.

### Las sugerencias del "sin resultados" miran el rango

Si la búsqueda no encuentra nada y trae un rango, primero se ofrecen las
sugerencias que entran en él. A quien pidió "menos de 150" ya no se le ofrece uno
de S/ 159 si hay otros que sí entran. Si **ninguna** entra, se ofrecen igual las de
siempre, para no dejar la pantalla vacía.

### "Menor" deja de ser un producto

Cuando el LLM no da categoría, la tarjeta de "Productos no encontrados" limpia la
frase original y le quita la expresión de precio. Esa limpieza no conocía "menor a":
de "Algo menor a 150 soles" quitaba "a 150 soles" y quedaba "menor". Ahora reconoce
las mismas formas que el intérprete de precio: "menor a/que", "mayor a/que", "por
debajo de", "por encima de", "máximo", "mínimo", "no más de". También quita
"barato" y "económico". Como se limpia al leer, la tarjeta "menor" que ya estaba
registrada también desaparece.

---

## 32. Afinidad con el rubro en "Productos no encontrados" (Fase 32)

"Pintura" en una tienda de focos no es un producto que le falte a la tienda: es
demanda de otro rubro. Hasta esta fase salía en la misma tarjeta, con el mismo peso
y el mismo botón "Agregar" que una amoladora en una ferretería.

### Cómo se mide

La IA que interpreta la búsqueda ya recibe el rubro de la tienda y la lista de lo
que vende (Fase 30). Ahora devuelve un campo más, `storeFit`, de 0 a 1:

| Valor | Significa |
|---|---|
| 1.0 | Es lo que vende la tienda, o una variante |
| 0.5 | Es del mismo rubro aunque no lo tenga |
| 0.0 | No tiene nada que ver con el rubro ni con sus productos |

No hay llamada extra: es un campo más en la misma respuesta. Se guarda en
`search_events.storeFit`.

### Qué cambia en la respuesta

```json
{
  "periodStart": "…", "periodEnd": "…", "totalPeople": 5,
  "groups": [
    { "term": "lampara solar", "personCount": 5, "searchCount": 7,
      "examples": [ { "rawText": "una lámpara solar", "count": 4 } ],
      "storeFit": 0.6 }
  ],
  "otherRequests": [
    { "term": "pintura", "personCount": 3, "searchCount": 3,
      "examples": [ { "rawText": "pintura", "count": 3 } ],
      "storeFit": 0.1 }
  ]
}
```

| Campo | Qué es |
|---|---|
| `groups` | Lo que falta **en el rubro**: `storeFit >= 0.5`, o sin medir. Es lo que pinta hoy la tarjeta |
| `otherRequests` | Lo que piden pero **no es del rubro**. Misma forma que `groups`. Nuevo |
| `groups[].storeFit` | Promedio de las búsquedas del grupo. `null` si ninguna lo trae |
| `totalPeople` | Suma solo de `groups`. Lo de otro rubro no infla el número |

**Nada se borra.** Lo que no es del rubro sigue registrado y viaja en
`otherRequests`; el comerciante puede querer verlo como demanda del pasillo.

**Los eventos anteriores a esta fase** no tienen `storeFit` y cuentan como del
rubro. Salen de la ventana de 7 días solos.

### Para la app

**No hay que tocar nada.** La tarjeta lee `groups`, que ahora solo trae lo del
rubro. `otherRequests` es opcional: si se quiere, una sección aparte "También te
piden", sin botón "Agregar".

---

## 33. "Productos no encontrados" solo con producto identificado (Fase 33)

Sin cambio de contrato. Reporte del 2026-10-25: "quiero algo negro" aparecía en la
tarjeta como `negro`.

### Qué pasaba

Cuando la IA no identificaba un producto, el backend intentaba rescatar uno de la
frase: borraba las palabras que conocía ("quiero", "algo", los precios) y tomaba lo
que sobraba como producto. Era una lista de palabras prohibidas, y siempre se
colaba alguna: `menor` (Fase 31), `negro`, y vendrían `bonito`, `grande`,
`moderno`…

Ese rescate nació cuando la IA estaba caída y nunca identificaba nada. Con la IA
funcionando y conociendo la tienda (Fase 30), ya no tiene sentido.

### Qué cambia

Solo entran a la tarjeta las búsquedas en las que la IA identificó un producto.

| Lo que dijo el cliente | ¿La IA identificó producto? | ¿Sale en la tarjeta? |
|---|---|---|
| "quiero una amoladora" | Sí | `amoladora` |
| "una mesa giratoria" | Sí | `mesa giratoria` |
| "quiero algo negro" | No | No |
| "algo menor a 150 soles" | No | No |
| "algo bonito para regalar" | No | No |

**La búsqueda en la tablet no cambia.** "Quiero algo negro" sigue buscando productos
negros. Solo cambia qué llega a la tarjeta del comerciante.

**Costo aceptado:** si la IA vuelve a caerse, la tarjeta queda vacía en lugar de
llenarse de frases. `scripts/check-llm-models.sh` detecta esa caída.

---

## 34. El token de la tablet lleva el rubro (Fase 34)

Reporte del 2026-10-25: "martillos" y "ventiladores", pedidos en una ferretería,
no aparecían en "Productos no encontrados".

### Qué pasaba

Se registraron bien, pero con afinidad 0 (`storeFit: 0`), así que fueron a
`otherRequests`. La causa: **el token de la tablet llevaba solo `storeId`**. El
rubro y el centro comercial viajaban solo en el token del staff. La búsqueda por
voz viene siempre de la tablet, así que la IA recibía "tienda de productos
variados" y una lista de productos casi toda de iluminación. Contra eso,
"martillo" parecía ajeno.

Por el mismo hueco, la tablet tampoco recibía el vocabulario de tags de su rubro
(Fase 5), porque necesita el centro comercial.

### Qué cambia

**1. auth: el token de la tablet lleva lo mismo que el del staff.** Además de
`storeId` y `role`, ahora incluye:

| Claim | Ejemplo |
|---|---|
| `storeCategory` | `TOOLS` |
| `shoppingCenterId` | UUID del centro comercial |
| `storeSlug` | `el-martillo` |
| `shoppingCenterSlug` | slug del centro comercial |

Se emite así en `GET /api/v1/devices/pairing-session/{sessionId}/token`, el canje
que hace la tablet al vincularse. La forma de la respuesta no cambia: el token es
opaco para la app.

**2. vo: completa los tokens que no traen esos datos.** El token de la tablet dura
un año y solo se emite al vincular, así que las tablets ya vinculadas siguen con el
viejo. Cuando el token no trae rubro o centro comercial, vo los lee de su
proyección del catálogo: cada producto guarda el rubro y el centro comercial de su
tienda. Cache de 1 hora. Si la tienda no tiene productos, el contexto queda como
vino.

**Las tablets no necesitan volver a vincularse.** Con la parte 2 funcionan desde el
deploy de vo. La parte 1 deja el token correcto para las que se vinculen de ahora
en adelante.

### Datos ya registrados

Las búsquedas guardadas antes del deploy tienen una afinidad calculada sin rubro.
Se limpian con un comando, después del deploy; quedan como "sin medir" y vuelven
a la tarjeta.
