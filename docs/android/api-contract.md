# Qhathia — Referencia de API

**Base URL:** `http://87.99.156.164`  
**Versión:** 0.1.0-SNAPSHOT  
**Formato:** JSON / UTF-8  
**Autenticación:** Bearer JWT en header `Authorization`

---

## Cambios recientes (Fase 1-7)

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

### POST /api/v1/devices/pairing-token

**Descripción:** Genera un token temporal de pairing (TTL 15 min). El OWNER lo muestra en pantalla y el dispositivo lo ingresa para vincularse.  
**Autenticación:** JWT requerido (roles: `OWNER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/devices/pairing-token`

**Headers:** `Authorization: Bearer <accessToken>`  
**Request Body:** ninguno

**Response 200:**
```json
{
  "pairingToken": "X7K2P",
  "expiresAt": "2025-04-15T18:40:00Z",
  "storeId": "uuid"
}
```

> **Formato del código:** exactamente 5 caracteres alfanuméricos uppercase del alfabeto `23456789ABCDEFGHJKMNPQRSTUVWXYZ` (31 caracteres; excluye confusables visuales `0 O 1 I L`). Patrón regex: `^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{5}$`.  
> **Unicidad:** evaluada solo contra tokens activos (`consumed=false AND expires_at > now()`). Tokens expirados o consumidos con el mismo valor pueden reutilizarse.  
> **TTL:** 15 minutos desde la generación.

**Errores:**
- `401` — No autenticado
- `403` — Rol insuficiente (SELLER o DEVICE no pueden)
- `503` — `code: PAIRING_GENERATION_EXHAUSTED` — No se pudo generar un código único tras 5 intentos (muy poco frecuente; reintentar la petición)

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/devices/pairing-token \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/devices/pair

**Descripción:** El dispositivo usa el token de pairing para vincularse a la tienda y obtener su JWT propio (role=DEVICE, TTL 1 año).  
**Autenticación:** Pública (el dispositivo aún no tiene JWT)  
**URL producción:** `http://87.99.156.164/api/v1/devices/pair`

**Request Body:**
```json
{
  "pairingToken": "string (requerido)",
  "deviceName": "string (requerido, max 200 chars, ej: 'Tablet Mostrador 1')",
  "platform": "ANDROID (requerido)",
  "appVersion": "string (requerido, max 20 chars, ej: '1.0.0')",
  "deviceFingerprint": "string (opcional, max 500 chars)"
}
```

**Response 201:**
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

**Nota:** El `deviceAccessToken` es el JWT con `role=DEVICE`. Guardarlo de forma persistente — no hay refresh. TTL ~1 año.

**Errores:**
- `400` — Token de pairing inválido, expirado o ya utilizado

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/devices/pair \
  -H "Content-Type: application/json" \
  -d '{
    "pairingToken": "ABC123",
    "deviceName": "Tablet Mostrador 1",
    "platform": "ANDROID",
    "appVersion": "1.0.0"
  }'
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

**Descripción:** Emite una API key temporal de Deepgram (TTL configurado, default 60s) para que el dispositivo pueda transcribir audio directamente desde la tablet. La API key real nunca se expone al cliente.  
**Autenticación:** JWT requerido (cualquier rol autenticado, típicamente `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/stt/token`

**Headers:** `Authorization: Bearer <deviceAccessToken>`  
**Request Body:** ninguno

**Response 200:**
```json
{
  "token": "dg_temp_key_...",
  "expiresIn": 60,
  "provider": "deepgram",
  "issuedAt": "2025-04-15T20:00:00Z"
}
```

**Errores:**
- `401` — No autenticado
- `502` — Error en el proveedor Deepgram
- `503` — STT no configurado (modo stub activo o API key ausente)

**curl:**
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
  "availability": "AVAILABLE | LOW_STOCK | CONSULT (opcional, default: AVAILABLE)",
  "allowDiscountInquiry": false,
  "categoryId": "uuid (opcional)",
  "tags": ["string", "string"],
  "position": 0
}
```

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

## 9. BizAPI — Órdenes

### POST /api/v1/orders

**Descripción:** El dispositivo kiosk crea una orden de interés en un producto (estado inicial: `PENDING`).  
**Autenticación:** JWT requerido (rol: `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/orders`

**Headers:** `Authorization: Bearer <deviceAccessToken>`

**Request Body:**
```json
{
  "productId": "uuid (requerido)",
  "sessionId": "string (opcional — ID de sesión conversacional)",
  "customerNote": "string (opcional — nota del cliente al dueño)"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "storeId": "uuid",
  "productId": "uuid",
  "productName": "Lámpara de Pared Vintage",
  "productPrice": 89.90,
  "productAvailability": "AVAILABLE",
  "status": "PENDING",
  "source": "ASSISTANT",
  "sessionId": "sess-123",
  "customerNote": "¿Tiene en color negro?",
  "rejectionReason": null,
  "createdAt": "2025-04-15T20:00:00Z",
  "updatedAt": "2025-04-15T20:00:00Z",
  "lastStatusChangedAt": "2025-04-15T20:00:00Z"
}
```

**Errores:**
- `400` — Producto inactivo
- `403` — Token de usuario (se requiere DEVICE)
- `404` — Producto no encontrado
- `422` — `productId` requerido

**curl:**
```bash
curl -X POST http://87.99.156.164/api/v1/orders \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "550e8400-e29b-41d4-a716-446655440000",
    "sessionId": "sess-abc123",
    "customerNote": "¿Hay en color negro?"
  }'
```

---

### GET /api/v1/orders

**Descripción:** Lista órdenes de la tienda, paginadas. Filtrables por estado.  
**Autenticación:** JWT requerido (roles: `OWNER`, `SELLER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/orders`

**Headers:** `Authorization: Bearer <accessToken>`

**Query Params:**
- `status` — `PENDING` | `CONFIRMED` | `REJECTED` | `CANCELLED` (opcional)
- `page` — número de página (default: 0)
- `size` — tamaño de página (default: 20)
- `sort` — campo,dirección (default: `createdAt,DESC`)

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "storeId": "uuid",
      "productId": "uuid",
      "productName": "Lámpara de Pared Vintage",
      "status": "PENDING",
      "source": "ASSISTANT",
      "sessionId": "sess-123",
      "createdAt": "2025-04-15T20:00:00Z",
      "lastStatusChangedAt": "2025-04-15T20:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**curl:**
```bash
curl "http://87.99.156.164/api/v1/orders?status=PENDING&size=10" \
  -H "Authorization: Bearer eyJ..."
```

---

### GET /api/v1/orders/{id}

**Descripción:** Retorna el detalle completo de una orden.  
**Autenticación:** JWT requerido (roles: `OWNER`, `SELLER`, `ADMIN`)

**Response 200:** objeto `OrderResponse` completo.

**Errores:**
- `404` — Orden no encontrada

**curl:**
```bash
curl http://87.99.156.164/api/v1/orders/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJ..."
```

---

### POST /api/v1/orders/{id}/confirm

**Descripción:** Confirma la orden (transición `PENDING → CONFIRMED`). Solo aplicable desde estado `PENDING`.  
**Autenticación:** JWT requerido (roles: `OWNER`, `SELLER`, `ADMIN`)  
**URL producción:** `http://87.99.156.164/api/v1/orders/{id}/confirm`

**Headers:** `Authorization: Bearer <accessToken>`  
**Request Body:** ninguno

**Response 200:** objeto `OrderResponse` con `status: "CONFIRMED"`.

**Errores:**
- `404` — Orden no encontrada
- `422` — Transición inválida (ya está CONFIRMED/REJECTED/CANCELLED)

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

**Descripción:** Recibe texto ya transcrito (por el dispositivo vía Deepgram), extrae la intención de búsqueda y retorna hasta 5 productos relevantes con texto para síntesis de voz.  
**Autenticación:** JWT requerido (cualquier rol — tipicamente `DEVICE`)  
**URL producción:** `http://87.99.156.164/api/v1/conversations/search`

**Headers:** `Authorization: Bearer <deviceAccessToken>`

**Request Body:**
```json
{
  "transcribedText": "string (requerido, max 500 chars — acepta alias 'transcript')",
  "sessionId": "string (opcional — para trazabilidad multi-turno futura)",
  "language": "string (opcional, ISO 639-1, default: 'es')"
}
```

**Response 200:**
```json
{
  "understood": true,
  "interpretedQuery": {
    "intent": "PRODUCT_SEARCH",
    "category": "lámpara",
    "subtype": "pared",
    "style": "vintage",
    "useContext": null,
    "maxPrice": null,
    "minPrice": null,
    "color": null,
    "sizePreference": null,
    "attributes": [],
    "rawText": "busco una lámpara de pared vintage",
    "confidence": 0.9
  },
  "results": [
    {
      "id": "uuid",
      "name": "Lámpara de Pared Vintage",
      "description": "Estilo vintage, ideal para sala",
      "price": 89.90,
      "availability": "AVAILABLE",
      "categoryName": "Lámparas",
      "primaryImageUrl": "https://...",
      "relevanceScore": 85
    }
  ],
  "noResults": false,
  "spokenText": "Encontré 1 lámpara vintage. La más relevante cuesta 89 soles.",
  "displayText": "1 resultado para 'lámpara vintage'",
  "fallbackMessage": null
}
```

**Notas:**
- `results` contiene máximo 5 productos, ordenados por `relevanceScore` descendente
- Cuando `noResults=true`, `results` es vacío y `fallbackMessage` tiene texto sugerido
- Cuando `understood=false`, la búsqueda fue ambigua — `fallbackMessage` guía al usuario
- El path primario de datos es MongoDB (`vitria_search`); si no hay datos, hace fallback a BizAPI

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

Curado por Vitria. Indexado por (`shoppingCenterId`, `storeCategory`).

| Método | Path | Descripción |
|---|---|---|
| GET    | `/api/v1/admin/tag-catalog?shoppingCenterId=…&storeCategory=…` | Lista activos del par |
| GET    | `/api/v1/admin/tag-catalog/{id}` | Detalle |
| POST   | `/api/v1/admin/tag-catalog` | Crea individual (409 si duplicado) |
| PUT    | `/api/v1/admin/tag-catalog/{id}` | Update parcial |
| DELETE | `/api/v1/admin/tag-catalog/{id}` | Soft delete |
| POST   | `/api/v1/admin/tag-catalog/bulk` | Import masivo (UPSERT/REPLACE) |

**Bulk body:**
```json
{
  "shoppingCenterId": "uuid",
  "storeCategory": "JEWELRY",
  "mode": "REPLACE",
  "tags": [
    { "value": "oro", "label": "Oro", "description": "(opcional)", "position": 0 },
    { "value": "vintage", "label": "Vintage", "position": 1 }
  ]
}
```
- `mode=UPSERT` (default): crea o actualiza por `value`, deja intactos los existentes.
- `mode=REPLACE`: desactiva los tags ausentes en el payload y upserts los del payload.
- `value`: lowercase alfanumérico, guiones/underscore permitidos, sin espacios.

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
      "position": 0,
      "active": true,
      "createdAt": "...",
      "updatedAt": "..."
    }
  ]
}
```

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
PAIR=$(curl -s -X POST http://87.99.156.164/api/v1/devices/pairing-token \
  -H "Authorization: Bearer $TOKEN" | jq -r .pairingToken)

# Desde la tablet (sin JWT, endpoint público)
DEVICE_TOKEN=$(curl -s -X POST http://87.99.156.164/api/v1/devices/pair \
  -H "Content-Type: application/json" \
  -d "{\"pairingToken\":\"$PAIR\",\"deviceName\":\"Tablet 1\",\"platform\":\"ANDROID\",\"appVersion\":\"1.0.0\"}" \
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

