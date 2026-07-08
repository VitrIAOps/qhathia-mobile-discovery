# VitrIA — Referencia de API

**Base URL:** `http://87.99.156.164` (producción — IP fija del VPS, no hay dominio HTTPS)  
**Versión:** 0.1.0-SNAPSHOT  
**Formato:** JSON / UTF-8  
**Autenticación:** Bearer JWT en header `Authorization`

> **Nota para el frontend**: producción es `http://87.99.156.164` (IP directa, sin HTTPS, sin dominio). Local dev: `http://localhost` (todos los servicios via nginx en :80).

---

## Cambios recientes (Fase 1-13)

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
- `status` — `PENDING` | `CONFIRMED` | `REJECTED` | `CANCELLED` (opcional)
- `customerPhone` — celular del cliente (opcional, formato `9XXXXXXXX`)
- `page` — número de página (default: 0)
- `size` — tamaño de página (default: 20)
- `sort` — campo,dirección (default: `createdAt,DESC`)

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
```

---

### GET /api/v1/orders/{id}

**Descripción:** Retorna el detalle completo de una orden, con `items[]`, customer fields y totales.
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

