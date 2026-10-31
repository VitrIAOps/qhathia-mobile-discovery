# Pendientes del lado Android — guía de implementación

> **Para quién es esto.** Para quien vaya a implementar los cambios en las dos
> apps Android — persona o agente de IA. Está escrito para que se pueda ejecutar
> sin tener que redescubrir nada: cada ítem trae el código **actual** tal cual
> está en el repo (para ubicarse), el código **objetivo** (para aplicar), el
> contrato **exacto** que el backend garantiza, y una **verificación** que se
> puede correr al terminar.

Sale del triage del 2026-10-18 ([`docs/qa/TRIAGE-BUGS.md`](../qa/TRIAGE-BUGS.md)),
donde está la evidencia forense de cada hallazgo. **Este documento es el plan
de trabajo**, ordenado por lo que más devuelve por el esfuerzo.

**Todo lo que acá dice "el backend ya lo manda" está desplegado en producción
y verificado** (`http://87.99.156.164`, 2026-10-18/19). No hay que esperar nada
del servidor. El contrato completo está en
[`docs/api/API_REFERENCE.md`](../api/API_REFERENCE.md); las formas nuevas
están en las fases 25 a 28 y resumidas en el Apéndice A de este documento.

---

## Estado al 2026-10-24 — leé esto aunque no leas el resto

Revisión contra el código real: `qhathia-mobile-discovery@f8622dd` y
`qhathia-mobile-staff@0ca3de8`. Cada fila cita el commit o el archivo que lo
prueba. Esta tabla manda sobre el resto del documento.

### Lo nuevo del backend (Fase 29, 2026-10-24) — no requiere tocar Android

Los dos pedidos del 2026-10-23 quedaron resueltos del lado del servidor. Con el
código que ya tienen, se ven solos en cuanto se despliega vo.

| Pedido | Qué hizo el backend | Qué tiene que hacer la app |
|---|---|---|
| `discountedPrice` / `discountEndsAt` en `POST /conversations/search` | Van en **todos** los `results[*]`, también en las sugerencias de "sin resultados". Misma regla y mismo número que `/catalog/by-category` | **Nada.** `SearchProductDto` ya los declara con default `null` y `ProductMappers.kt` ya los pasa |
| Q1: ¿"más de 50" compara contra qué? | Contra `discountedPrice ?: price`. Uno de S/ 55 rebajado a S/ 45 no es "más de 50" | Nada |
| Q2: ¿el texto hablado dice el precio rebajado? | Sí. El LLM recibe el rebajado y que tiene descuento | Nada |
| `term` de "Productos no encontrados" a veces es una oración | Las búsquedas sin producto ("dame algo de más de 50 soles") ya no aparecen. "Una mesa giratoria" sale `mesa giratoria`, no `giratoria`. Las adivinanzas del LLM con baja confianza no entran | Nada. Ver §3.2 para la línea secundaria con `examples` |

Contrato en [`API_REFERENCE.md` §29](../api/API_REFERENCE.md#29-búsqueda-por-voz-precio-con-descuento-y-productos-no-encontrados-fase-29).

**Fases 31 y 32 (2026-10-25), tampoco requieren tocar Android:**

- "Algo menor a 150 soles" devuelve los productos del rango, más baratos primero. Antes caía en "no pude encontrar".
- "Productos no encontrados" (`groups`) trae solo lo del rubro de la tienda. Lo que piden de otro rubro ("pintura" en una tienda de focos) viaja aparte, en el campo nuevo `otherRequests`. **Opcional:** una sección "También te piden" que lo muestre sin botón "Agregar". Contrato en [§32](../api/API_REFERENCE.md#32-afinidad-con-el-rubro-en-productos-no-encontrados-fase-32).

**Errores del reconocimiento de voz ("Paladros" por "taladros").** Desde la
Fase 30 la IA recibe el rubro de la tienda y la lista de productos que vende, y
usa esa lista para reconocer palabras mal transcritas. Es del lado del servidor:
no lo corrijan en el cliente.

### Staff — 21 de 22 hechos

| § | Qué | Estado | Evidencia |
|---|---|---|---|
| 2.1 | Borrar `mockedStock`, usar `stock` real | ✅ | 0 referencias; `ProductDto.stock`, `Product.stock` · `4813b31` |
| 2.3 | Snackbar: consumir antes de `showSnackbar` | ✅ | `InventoryScreen.kt:107` consume **antes** del `when`, `productId` capturado · `1f6484f` |
| 3.1 | `discountedPrice` en la **lista** | ✅ | `ProductRow.kt:266-276` `ProductPrice(price, discountedPrice)` · `4813b31` |
| 3.1 | `discountedPrice` en el **detalle** | ⚠️ parcial | Ver nota 1 abajo |
| 3.2 | "Productos no encontrados": titular = `term` | ✅ | `StoreInsightsMappers.kt:138` `name = canonical…` · `44b5c30`. Opcional pendiente: la línea secundaria con `examples` (§3.2, paso 3) |
| 3.3 | `confirmedOrders` | ✅ | DTO, dominio, mapper, `ActivityTab.kt` · `255b418` |
| 3.4 | `totalCount` + label | ✅ | "Inmovilizados en X de los Y productos sin ventas" · `255b418` |
| 3.5 | Texto de `ORDER_EXPIRING` | ✅ | `Kind.EXPIRING` → "Orden por vencer" · `1f6484f` |
| 4.1 | Badge: `ORDER_TAKEN` cancela | ✅ | `OrderPushAction.Cancel` → `cancel(orderId)`, y reconcilia las viejas · `1f6484f` |
| 4.2 | "Quedan pocos" con stock 0 | ✅ | `stock <= 0 -> OutOfStockWarning()` "Sin existencias" · `558904f` |
| 4.3 | Inicio recarga al volver | ✅ | `LifecycleEventEffect(ON_START)` · `558904f` |
| 4.4 | Chevron muerto | ✅ | Reemplazado por `Spacer` · `558904f` |
| 5.1 | Editar datos personales | ✅ | `PUT users/me`, VM con detección de cambios · `23c5f45` |
| 5.2 | Agregar / quitar fotos | ✅ | `POST /{id}/images`, `DELETE /{id}/media/{mediaId}` · `ed6eb4d` |
| 5.3 | Campana del detalle | ✅ | `onOpenOrderRequests` · `fa66eb9` |
| 5.4 | PIN del kiosko | ✅ | `exitPin` / `removeExitPin` exactos · `be843c3` |
| 5.5 | Tres puntos: "Eliminar" | ✅ | `DELETE /products/{id}`, maneja "ya borrado" · `7c1c7d4` `ab815e9` |
| 5.6 | Compartir cotización por WhatsApp | ❌ | 0 referencias a `share-pdf` |
| 5.7 | "Ver producto" tras registrar | ✅ | Pre-existente |
| 5.8 | Vista previa del Discovery | ✅ | `productPreviewMapper` · `ac3c890` |
| 5.9 | "X" en el buscador | ✅ | `InventoryHeader.kt:142` |
| 6 | Retirar el toggle de IGV | ✅ | Solo queda un comentario en `ProfileDtos.kt` · `cf465d3` |

Además, cosas que **no estaban en el plan** y están bien: desvincular tablet
(`c80f246`), "retry time" en comandos — muestra `expiresAt`, **no reenvía**
(`352f3f2`), ocultar acciones en pedidos resueltos (`a19acbb`), plural de
Inventario (`0677ffb`).

### Discovery — 6 de 6 hechos

| § | Qué | Estado | Evidencia |
|---|---|---|---|
| 1 | Heartbeat cada **60 s**, telemetría, `settingsVersion`, comandos + ack | ✅ | `DeviceConstants.kt:14`, `SyncDeviceSettingsUseCase`, `ProcessDeviceCommandsUseCase` · `aaf07d5` `ef8442f` `0e6e29b` |
| 1 | Ajustes aplicados **en caliente** + modo kiosko + PIN de salida | ✅ | `MainActivity.kt:95` observa `deviceSettings` · `d8f4a6a` |
| 2.2 | Pestaña "Descuentos" por `discountedPrice` | ✅ | `CatalogConstants.kt:25` `isDiscountCandidate = discountedPrice != null` · `c0cc199` |
| 3.1 | `discountedPrice` en grilla, detalle, búsqueda y carrito | ✅ | `CatalogMappers`, `ProductDetailMappers`, `ProductMappers`; el carrito cobra `discountedPrice ?: price` · `c0cc199` |
| 5.10 | "Ir al catálogo" en Búsqueda | ✅ | Botón nuevo junto a "Nueva búsqueda" (que se queda, y ahora vacía el carrito con aviso, CAR-040) · change `search-go-to-catalog` (2026-10-30) |
| 5.11 | Mostrar el texto que entendió por voz | ✅ | Transcripción en vivo mientras escucha, `ProductSearchContent.kt:266` · `e39ee01` |

### Lo que queda, en orden

1. **§5.6 Staff** — compartir la cotización por WhatsApp. Un botón sobre un
   endpoint que ya existe entero.
2. **Nota 1 Staff** — el detalle toma `discountedPrice` del servidor como valor
   inicial. Chico, evita que el detalle y la lista muestren números distintos.
3. ~~**§5.10 Discovery**~~ — hecho (2026-10-30, `search-go-to-catalog`): "Ir al catálogo" se agregó al lado de "Nueva búsqueda".
4. **§3.2, paso 3 Staff** — opcional: debajo del término, el primer ejemplo de
   cómo lo pidieron.

### Nota 1 — `discountedPrice` en el detalle (staff)

`ProductDetailUiState.discountedPrice` es un **cálculo del formulario**: precio
tipeado × descuento del borrador. Para el *preview mientras se edita* está
bien — el servidor todavía no tiene ese borrador. Pero `ProductDetailDto` **no
lee** `discountedPrice` del servidor, así que el valor inicial al abrir el
producto también sale de la aritmética del cliente. Riesgo: divergir del
servidor (redondeo, qué descuento gana si hay varios). Arreglo chico: agregar
`discountedPrice` a `ProductDetailDto` y usarlo como valor inicial; dejar la
fórmula solo para el preview en vivo, y que replique la regla del servidor
(gana el más barato, `HALF_UP` a 2 decimales).

**Los números que son contrato — no los cambien:** tablet **60 s** entre
heartbeats; staff **10 min** de ventana para "conectada". Si una tablet se ve
"Desconectada" o con "Modelo —" con este código, ningún heartbeat llegó (build
vieja o 401): se verifica en el servidor, §1 → Verificación, punto 4.

---

## Cómo leer este documento

### Los dos repos

| Nombre corto | Repo | Qué es | Stack |
|---|---|---|---|
| **Discovery** | `qhathia-mobile-discovery` | La tablet del kiosko. La usa el cliente final | Compose, Hilt, **Ktor**, rol `DEVICE` |
| **Staff** | `qhathia-mobile-staff` | El celular del comerciante | Compose, Hilt, Ktor, roles `OWNER/SELLER/ADMIN` |

Las rutas de archivo son relativas a la raíz de cada repo. Los números de línea
corresponden al estado del código al 2026-10-21; si el archivo cambió, buscar el
fragmento citado en **Estado actual**, que es verbatim.

### Estructura de cada ítem

Cada ítem tiene las mismas secciones, en el mismo orden:

| Sección | Qué contiene |
|---|---|
| **Síntoma** | Lo que ve el comerciante o el cliente. Es lo que hay que hacer desaparecer |
| **Causa** | Por qué pasa, con el código actual citado tal cual |
| **Qué garantiza el backend** | El contrato exacto en el que se puede confiar |
| **Cambio** | Archivos y código objetivo, numerado en el orden en que conviene hacerlo |
| **No hacer** | Caminos que parecen razonables y están mal — y por qué |
| **Verificación** | Cómo saber que quedó bien |

### Reglas que aplican a todo

1. **Nunca recalcular en el cliente algo que el backend ya calcula.** Precio
   con descuento, término canónico de una búsqueda, disponibilidad derivada del
   stock. La aritmética duplicada en tres clientes es la causa de la mitad de
   los bugs de este triage. Si el campo viene, se usa el campo.
2. **Nunca inventar un dato que no viene.** Si un campo es `null`, se muestra
   que no hay dato, no un relleno plausible. (Ver §2.1 para lo que pasa cuando
   se hace lo contrario.)
3. **Los campos nuevos son nullable con significado.** `discountedPrice: null`
   quiere decir "sin descuento vigente", no "cargando" ni "error".
   `stock: null` quiere decir "modo consultar". Esa distinción es parte del
   contrato y hay que respetarla al pintar.
4. **Un control que no hace nada no debe parecer un control.** Varios bugs
   fueron eso: un chevron con `onClick = {}`, campos dibujados como inputs que
   eran solo lectura, un toggle sin efecto. Si algo no es accionable, no se
   dibuja como accionable.
5. **Los JSON del backend son `camelCase`.** Los DTOs de Kotlin usan los mismos
   nombres; `ignoreUnknownKeys` está activo en ambas apps, así que agregar un
   campo al DTO nunca rompe por campos extra del servidor, y un campo faltante
   en el servidor cae al default del DTO.
6. **Autenticación.** Staff manda el JWT de usuario; Discovery manda el token
   `DEVICE` que guardó al emparejarse (`StoreSessionRepository.deviceAccessToken()`).
   El `HttpClient` de Discovery **no tiene plugin de auth**: cada llamada pone
   el `Authorization: Bearer …` a mano (ver `ProductApiService.kt`).

---

## 0 · Contexto: el patrón que explica casi todo

De los 31 bugs del triage, **la mayoría no eran cálculos mal hechos**. Eran
datos que existían, viajaban por la red, y se descartaban en el último paso.
Cinco veces, en sitios distintos:

| Dato | Dónde se caía |
|---|---|
| El stock de un producto | El backend no lo mandaba **y la app inventaba un número** desde el hash del id |
| El precio con descuento | El backend no lo calculaba; cada cliente debía hacerlo y ninguno lo hacía |
| El precio mínimo de una búsqueda por voz | El LLM lo extraía, el DTO lo llevaba, el filtro lo ignoraba |
| `confirmedOrders` del dashboard | Llega en la respuesta, el mapper no lo mapea |
| El término canónico de "Productos no encontrados" | Llega en `term`, el mapper pinta `examples[].rawText` |

Vale tenerlo presente al leer lo que sigue, porque **tres de los ítems de acá
son borrar código, no escribirlo**. Son los más baratos y los que más arreglan.

Y un segundo patrón, más incómodo: pantallas con controles que **parecen
accionables y no hacen nada**. Todos fueron reportados como bugs, y todos
tenían razón.

---

## 1 · El heartbeat de la tablet — una llamada, cinco funciones

**Repo: Discovery** · Prioridad: **1** · Triage: BUG-025 · ✅ **Implementado por el front (PRs #17–#20) y verificado contra el contrato el 2026-10-22.** Se deja la sección completa como referencia del contrato.

### Síntoma

En la app staff, Perfil → Dispositivos dice **"0 tablets conectadas"** con la
tablet encendida y en uso. La card de la tablet dice "Desconectada", batería
"—", conexión "—". "Actualizar catálogo" y "Reiniciar" no hacen nada. Los
ajustes remotos nunca llegan. El PIN del kiosko no se puede configurar.

### Causa

**La app discovery nunca llama al heartbeat.** Cero referencias en sus 295
archivos Kotlin. Su único `POST` a `/devices` es el del pairing:

```kotlin
// core/network/.../service/DevicePairingApiService.kt — los únicos tres endpoints de dispositivo
/** `POST /api/v1/devices/pairing-token` — generates a code + opaque session (public). */
suspend fun generatePairingToken(): NetworkResult<PairingTokenResponseDto> = ...

/** `GET /api/v1/devices/pairing-session/{sessionId}/token` — one-shot DEVICE JWT exchange (public). */
suspend fun exchangeToken(sessionId: String): NetworkResult<DeviceTokenResponseDto> = ...

/** `GET /api/v1/devices/me` — paired-device info, authenticated with the DEVICE token. */
suspend fun getPairedDevice(deviceToken: String): NetworkResult<DeviceInfoResponseDto> = ...
```

Después del pairing, la tablet no vuelve a hablarle al servidor sobre sí misma.

La app staff calcula "conectada" así, y está bien:

```kotlin
// staff: core/domain/.../model/Device.kt:31-37
fun isConnected(now: Instant): Boolean {
    val lastPing = lastPingAt ?: return false
    return status == DeviceStatus.ACTIVE && !lastPing.isBefore(now.minus(CONNECTED_PING_WINDOW))
}
companion object { val CONNECTED_PING_WINDOW: Duration = Duration.ofMinutes(10) }
```

Como `last_ping_at` se queda en el instante del pairing, a los diez minutos la
tablet es "Desconectada" para siempre.

### Por qué una sola llamada arregla cinco cosas

El heartbeat **no es un "sigo vivo"**: es el canal de bajada completo hacia la
tablet. Todo lo que el comerciante configura o manda desde su celular viaja en
la **respuesta** del heartbeat.

| Función | Cómo depende del heartbeat |
|---|---|
| "N tablets conectadas" / "Desconectada" | `last_ping_at` solo se actualiza ahí |
| Batería y tipo de conexión en la card | Viajan en el **request** |
| "Actualizar catálogo" / "Reiniciar" | Los comandos vienen en `pendingCommands` del **response** |
| Ajustes remotos (vitrina en reposo, brillo, asistente de voz…) | El **response** trae `settingsVersion`; si cambió, hay que re-leer los ajustes |
| PIN de salida del modo kiosko | Es uno de esos ajustes |

Las Fases 21 y 23 del backend están completas, con tests, **y sin cliente**.

### Por qué esto y no una cola, un tópico o un WebSocket

Es la pregunta que surge apenas se ve que los ajustes "solo llegan al reabrir
la app": ¿no habría que publicar un evento y que la tablet se suscriba? Se
evaluó. La respuesta es no, y conviene saber por qué para no volver a abrirla.

**1. Una tablet no se suscribe a una cola.** Kafka, RabbitMQ, Event Grid y
similares son canales servidor-a-servidor. Para que algo llegue a un cliente
móvil hace falta igual un WebSocket, MQTT, SSE o un push FCM. El broker sería
un salto extra *detrás* de eso, no lo que la tablet consume. La propuesta
"un tópico al que el front se suscribe" es, en la práctica, "un WebSocket".

**2. El canal ya existe y está diseñado para esto.** La respuesta del
heartbeat trae `settingsVersion`. La tablet guarda el último que vio; si el
nuevo difiere, re-lee `GET /devices/me/settings`. Latencia: un tick (60 s).
Los comentarios del backend lo dicen textualmente: *"Sin polling nuevo ni
push a la tablet."* Lo que falta no es un mecanismo — es la llamada.

**3. Es durable sin broker.** El estado es la fila de `device_settings` con
su versión. Si la tablet pierde diez ticks por WiFi malo, el undécimo
converge igual. Una cola con *replay* no agrega nada que la fila versionada
no tenga ya.

**4. Un WebSocket sería un segundo canal haciendo lo mismo que el primero.**
Y en un kiosko detrás del WiFi de un centro comercial necesita reconexión,
ping/pong propio y un fallback para cuando se cae — que es… el heartbeat
HTTP. Existe un WebSocket en auth para el estado del pairing, pero es de vida
corta (segundos) y por una sola vez; no es el mismo problema.

**5. Infraestructura.** Producción es un solo VPS con Postgres, Mongo, Redis
y tres servicios, despliegue manual por SSH, sin CI/CD. Un broker es otro
contenedor, otro punto de falla y otra cosa que nadie monitorea, para resolver
algo que una llamada HTTP cada 60 segundos resuelve.

**Si algún día hace falta que sea inmediato** —y para brillo, vitrina en reposo
o asistente de voz nadie lo ha pedido—, el camino es un push FCM *data-only*
a la tablet con `{"event":"SETTINGS_CHANGED","settingsVersion":N}`,
reutilizando el `PushGateway` que ya manda los pedidos al celular del staff.
Cero contenedores nuevos, latencia de segundos. Pero FCM es *best-effort*
(Doze, sin red): solo puede ser un acelerador **encima** del heartbeat, nunca
su reemplazo. Y la app discovery hoy no tiene Firebase. No construirlo hasta
que alguien necesite de verdad menos de un minuto.

### Qué garantiza el backend

**`POST /api/v1/devices/heartbeat`** · rol `DEVICE` · `Authorization: Bearer <deviceAccessToken>`

Request — solo `appVersion` es obligatorio:

```json
{
  "appVersion": "1.4.0",
  "networkStatus": "ONLINE",
  "batteryLevel": 73,
  "uptimeSeconds": 86400,
  "model": "Lenovo Tab M11",
  "lastSyncVersion": 1729000000000,
  "lastSyncAt": "2026-10-21T15:00:00Z"
}
```

| Campo | Tipo | Obligatorio | Nota |
|---|---|---|---|
| `appVersion` | string | **sí** (`@NotBlank`) | `BuildConfig.VERSION_NAME` |
| `networkStatus` | string | no | `"ONLINE"`, `"OFFLINE"`, `"LIMITED"` |
| `batteryLevel` | int 0–100 | no | Se muestra en la card |
| `uptimeSeconds` | long | no | |
| `model` | string ≤100 | no | `Build.MODEL` o similar. Se muestra en ajustes |
| `lastSyncVersion` | long (epoch ms) | no | Versión del catálogo que la tablet tiene |
| `lastSyncAt` | ISO-8601 | no | |

Response:

```json
{
  "received": true,
  "serverTime": "2026-10-21T15:00:01Z",
  "catalogVersion": 1729000000000,
  "settingsVersion": 7,
  "pendingCommands": [
    { "id": "0f3c…", "command": "REFRESH_CATALOG" }
  ]
}
```

| Campo | Qué hacer con él |
|---|---|
| `catalogVersion` | Si difiere del `lastSyncVersion` local, re-sincronizar el catálogo |
| `settingsVersion` | Si difiere del guardado localmente, llamar `GET /api/v1/devices/me/settings` y guardar el nuevo valor |
| `pendingCommands[]` | Ejecutar cada uno y confirmar. **Vacío cuando no hay nada.** Los `command` posibles: `REFRESH_CATALOG`, `RESTART_APP` |

**`POST /api/v1/devices/me/commands/{commandId}/ack`** · rol `DEVICE`

```json
{ "success": true, "detail": null }
```

`detail` (≤300 chars) es el motivo cuando `success: false`; lo muestra la card
del comerciante.

**`GET /api/v1/devices/me/settings`** · rol `DEVICE`

```json
{
  "deviceId": "…",
  "idleShowcaseEnabled": true,
  "voiceAssistantEnabled": true,
  "brightnessMode": "AUTO",
  "kioskMode": true,
  "exitPinHash": "$2a$10$…",
  "autoSyncEnabled": true,
  "settingsVersion": 7
}
```

`exitPinHash` es BCrypt del PIN, o `null` si no hay. **Se manda para que la
tablet valide la salida sin red**: cachearlo y comparar localmente con BCrypt.

**Regla importante sobre comandos:** un comando entregado **no se re-entrega**.
Si la tablet muere antes de confirmar, expira a los 10 minutos y el comerciante
ve "Expirado". Es deliberado — re-entregar un `RESTART_APP` dejaría la tablet
en bucle. No hay que implementar reintentos del lado de la tablet.

### Cambio

> **Ya está hecho.** Lo que sigue era la propuesta; la implementación real quedó
> en los mismos archivos (el front usó incluso el mismo `DeviceHeartbeatDtos.kt`).
> Se conserva porque describe el contrato con precisión.

**1. DTOs** — `core/network/.../dto/DeviceHeartbeatDtos.kt` (nuevo)

```kotlin
package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/** Body of `POST /api/v1/devices/heartbeat`. Only [appVersion] is required by the server. */
@Serializable
data class HeartbeatRequestDto(
    val appVersion: String,
    val networkStatus: String? = null,
    val batteryLevel: Int? = null,
    val uptimeSeconds: Long? = null,
    val model: String? = null,
    val lastSyncVersion: Long? = null,
    val lastSyncAt: String? = null,
)

/** Response of `POST /api/v1/devices/heartbeat`. */
@Serializable
data class HeartbeatResponseDto(
    val received: Boolean = false,
    val serverTime: String? = null,
    val catalogVersion: Long? = null,
    val settingsVersion: Long? = null,
    val pendingCommands: List<PendingCommandDto> = emptyList(),
)

/** One remote command the tablet must run now and then confirm. */
@Serializable
data class PendingCommandDto(
    val id: String,
    val command: String,   // "REFRESH_CATALOG" | "RESTART_APP"
)

/** Body of `POST /api/v1/devices/me/commands/{id}/ack`. */
@Serializable
data class AckCommandRequestDto(
    val success: Boolean,
    val detail: String? = null,
)

/** Response of `GET /api/v1/devices/me/settings`. */
@Serializable
data class DeviceSettingsDto(
    val deviceId: String = "",
    val idleShowcaseEnabled: Boolean = true,
    val voiceAssistantEnabled: Boolean = true,
    val brightnessMode: String = "AUTO",
    val kioskMode: Boolean = true,
    val exitPinHash: String? = null,
    val autoSyncEnabled: Boolean = true,
    val settingsVersion: Long = 0,
)
```

**2. Servicio de red** — agregar a `DevicePairingApiService.kt` o crear
`DeviceHeartbeatApiService.kt` en el mismo paquete, siguiendo exactamente el
patrón de `getPairedDevice` (bearer a mano, `runCatching { … }.toNetworkResult()`):

```kotlin
suspend fun heartbeat(deviceToken: String, body: HeartbeatRequestDto): NetworkResult<HeartbeatResponseDto> =
    runCatching {
        client.post("${BuildConfig.QHATHIA_BASE_URL}/api/v1/devices/heartbeat") {
            bearerAuth(deviceToken)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<HeartbeatResponseDto>()
    }.toNetworkResult()

suspend fun ackCommand(deviceToken: String, commandId: String, body: AckCommandRequestDto): NetworkResult<Unit> =
    runCatching {
        client.post("${BuildConfig.QHATHIA_BASE_URL}/api/v1/devices/me/commands/$commandId/ack") {
            bearerAuth(deviceToken)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        Unit
    }.toNetworkResult()

suspend fun getMySettings(deviceToken: String): NetworkResult<DeviceSettingsDto> =
    runCatching {
        client.get("${BuildConfig.QHATHIA_BASE_URL}/api/v1/devices/me/settings") {
            bearerAuth(deviceToken)
        }.body<DeviceSettingsDto>()
    }.toNetworkResult()
```

**3. El bucle** — un `@Singleton` en `core/data` (p. ej.
`DeviceHeartbeatScheduler`) que:

- Observa `StoreSessionRepository.observe()` y **solo corre mientras
  `session.isPaired`**. Antes del pairing no hay token y la llamada daría 401.
- Cada **60 segundos** — es el contrato, y la aritmética importa: la app staff
  da por desconectada una tablet sin heartbeat en **10 min**. A 60 s, se
  toleran 9 ticks perdidos antes de mostrarla desconectada. A **10 min**
  (lo que se implementó primero), intervalo = ventana: cualquier retraso de
  un segundo la muestra "Desconectada", y un solo tick perdido son 10 min
  de "Desconectada". Arma el request con `BuildConfig.VERSION_NAME`, batería
  (`BatteryManager`), conectividad (`ConnectivityManager`) y `Build.MODEL`.
- Al recibir la respuesta:
  - compara `settingsVersion` con el guardado en `StoreSession` (agregar el
    campo); si cambió, llama `getMySettings`, guarda el resultado y aplica lo
    que corresponda (incluido cachear `exitPinHash`).
  - por cada `pendingCommands[i]`: ejecuta, y llama `ackCommand` con el
    resultado. `REFRESH_CATALOG` = disparar la sincronización de catálogo que ya
    existe; `RESTART_APP` = reiniciar la app (`ProcessPhoenix` o relanzar
    `MainActivity` y `finishAffinity`).
- **Nunca propaga una excepción.** Un tick que falla se loguea y se espera el
  siguiente. Un heartbeat que tumba la app es peor que ninguno.

Dónde arrancarlo: `DiscoveryApp` es un `Application` vacío y no hay WorkManager
en el proyecto. La opción más simple y consistente con el código existente es
inyectar el scheduler en `MainViewModel` (`app/.../MainViewModel.kt`) y
arrancarlo en su `init`, ya que ese ViewModel ya observa la sesión para decidir
el `startDestination`. Vive lo que vive la app en primer plano, que para un
kiosko es siempre.

**4. `StoreSession`** — `core/domain/.../model/StoreSession.kt`: agregar
`settingsVersion: Long? = null` y persistirlo en `StoreSessionLocalDataSource`
para comparar entre ticks y sobrevivir reinicios.

### No hacer

- **No usar `GET /devices/me` como heartbeat.** Es solo lectura; no toca
  `last_ping_at` ni entrega nada.
- **No reintentar comandos** que fallaron o se perdieron. Expiran solos.
- **No mandar el heartbeat sin `appVersion`**: el servidor responde 422.
- **No interpretar un `pendingCommands` vacío como error.** Es el caso normal.
- **No cambiar el intervalo a 10 minutos** (ya está en 60 s). 10 min es la ventana de "conectada" de la app
  staff, no el intervalo. Con los dos iguales la tablet se ve desconectada casi
  siempre y los ajustes tardan 10 min en llegar — que es exactamente el
  síntoma de "solo al reabrir la app".

### Verificación

1. Con la tablet emparejada y la app abierta, en la app staff → Perfil →
   Dispositivos: la card pasa a **"Conectada"** en menos de 2 minutos, y
   batería/conexión muestran valores.
2. Tocar "Actualizar catálogo" en staff: la card pasa por "Enviado" → "Listo"
   en menos de 2 minutos, y la tablet re-sincroniza.
3. Cambiar un ajuste en staff (p. ej. brillo): la tablet lo aplica en el
   siguiente tick.
4. Si hace falta mirar el servidor: el `last_ping_at` del dispositivo se mueve
   en cada tick (`GET /api/v1/devices` desde staff lo expone como `lastSeenAt`).
   Directo en el VPS:

   ```bash
   docker compose -f docker-compose.prod.yml exec -T postgres      psql -U vitria -d vitria_auth -c "SELECT name, app_version, model, last_ping_at, now()-last_ping_at AS hace FROM devices ORDER BY last_ping_at DESC NULLS LAST;"
   ```

   `hace` tiene que ser de segundos. Si es de horas o días con la tablet
   encendida, el heartbeat no llega: mirar `docker compose logs --tail=200 auth-api | grep -i "heartbeat\|401\|422"`.

---

## 2 · Borrar código que hoy miente

Tres cambios que son **quitar líneas**. Los más baratos de la lista y los que
más arreglan.

### 2.1 · El stock de Inventario está inventado

**Repo: Staff** · Prioridad: **2** · Triage: BUG-019 y BUG-021 · **Ya causó una venta mal tomada**

#### Síntoma

La lista de Inventario muestra un stock que no coincide con el del producto. El
comerciante vio "19 stock" en la lista, abrió el producto y tenía **1**, pidió 2
unidades y el pedido se rechazó. "Hice la prueba con los demás productos y
también pasa con los demás": pasa con **los 41**.

#### Causa

```kotlin
// features/product-inventory/.../ui/ProductRow.kt:238-246
/**
 * Interim stock placeholder. `GET /api/v1/products` exposes no stock quantity yet; backend will add
 * it later. Derived deterministically from the product id so the value is stable across
 * recompositions. **Replace this single helper (and the DTO/domain field) when the real field
 * lands** — the row layout above does not change.
 */
internal fun mockedStock(product: Product): Int = (abs(product.id.hashCode()) % 90) + 10
```

Un número entre 10 y 99 derivado del hash del UUID. Es estable entre
recomposiciones, y por eso **convincente**: siempre dice lo mismo. No tiene
ninguna relación con el inventario.

Se usa en:

```kotlin
// ProductRow.kt:131
text = stringResource(R.string.inventory_stock_format, mockedStock(product)),
```

El propio comentario pedía borrarlo cuando llegara el campo real. **El campo
llegó.**

#### Qué garantiza el backend

`GET /api/v1/products` devuelve `stock` por fila:

```json
{
  "id": "…", "name": "Caja de herramientas metálica 20\"", "price": 95.00,
  "stock": 1,
  "availability": "AVAILABLE", "discountedPrice": null, "discountEndsAt": null, "…": "…"
}
```

`stock` es `Integer` nullable: **`null` = producto en modo "consultar" o legacy**
sin cantidad registrada. Es el mismo valor que devuelve `GET /products/{id}`.

#### Cambio

**1. DTO** — `core/network/.../dto/ProductDtos.kt:13-23`

Estado actual:

```kotlin
@Serializable
data class ProductDto(
    val id: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val name: String,
    val price: Double = 0.0,
    val availability: String? = null,
    val allowDiscountInquiry: Boolean = false,
    val position: Int = 0,
    val primaryImageUrl: String? = null,
)
```

Agregar `stock` (y de paso `discountedPrice` y `discountEndsAt`, que §3.1
necesita):

```kotlin
@Serializable
data class ProductDto(
    val id: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val name: String,
    val price: Double = 0.0,
    val availability: String? = null,
    /** Null = consult-mode or legacy product without a tracked quantity. */
    val stock: Int? = null,
    val allowDiscountInquiry: Boolean = false,
    val position: Int = 0,
    val primaryImageUrl: String? = null,
    /** Price with the active discount applied. Null = no discount in force right now. */
    val discountedPrice: Double? = null,
    val discountEndsAt: String? = null,
)
```

Y corregir el KDoc de arriba del DTO (`ProductDtos.kt:7-11`), que hoy dice
*"the products API exposes no stock quantity yet"*.

**2. Modelo de dominio** — `core/domain/.../model/Product.kt`

Estado actual:

```kotlin
data class Product(
    val id: String,
    val categoryId: String?,
    val categoryName: String?,
    val name: String,
    val price: Double,
    val availability: Availability,
    val primaryImageUrl: String?,
)
```

Objetivo:

```kotlin
data class Product(
    val id: String,
    val categoryId: String?,
    val categoryName: String?,
    val name: String,
    val price: Double,
    val availability: Availability,
    val primaryImageUrl: String?,
    /** Tracked quantity, or null when the product is in consult mode. */
    val stock: Int?,
    /** Price after the active discount, or null when there is none. */
    val discountedPrice: Double?,
)
```

**3. Mapper** — `core/data/.../mapper/ProductMappers.kt:18-27`, agregar las dos
líneas en `ProductDto.toDomain()`:

```kotlin
stock = stock,
discountedPrice = discountedPrice,
```

**4. La fila** — `ProductRow.kt:131`:

```kotlin
// antes
text = stringResource(R.string.inventory_stock_format, mockedStock(product)),

// después — con null se pinta "Consultar" (o se omite), nunca un número
text = product.stock?.let { stringResource(R.string.inventory_stock_format, it) }
    ?: stringResource(R.string.inventory_stock_consult),
```

Agregar `inventory_stock_consult` a `strings.xml` (y `values-en`).

**5. Borrar `mockedStock`** (`ProductRow.kt:238-246`) y el `import kotlin.math.abs`
si queda huérfano. También el comentario de `ProductRow.kt:58` que lo menciona.

#### No hacer

- **No** inferir el stock desde `availability`. `AVAILABLE` no dice cuánto hay.
- **No** pintar `0` cuando `stock` es `null`: cero y "no se registra cantidad"
  son cosas distintas para el comerciante.

#### Verificación

El número de la fila de Inventario tiene que ser **idéntico** al del campo
"Stock" al abrir el detalle del mismo producto. Hoy difieren en los 41. Si
algún producto muestra "Consultar", al abrirlo el campo Stock debe estar vacío.

### 2.2 · La pestaña "Descuentos" del Discovery no muestra descuentos

**Repo: Discovery** · Prioridad: **4** · Triage: BUG-013 · Depende de §3.1 · ✅ **Hecho** en `c0cc199` (verificado 2026-10-24)

#### Síntoma

La pestaña "Descuentos" de la grilla lista productos sin oferta y omite los que
sí la tienen.

#### Causa

```kotlin
// core/domain/.../CatalogConstants.kt:19-26
/**
 * Predicate for the synthetic "Descuentos" section. The catalog endpoint exposes no discount data,
 * only [Product.allowDiscountInquiry] ("the consumer may ask about discounts"); the Descuentos
 * section aggregates products where this is true. This is an approximation of "has an active
 * discount" — see the change design doc's open question.
 */
fun isDiscountCandidate(product: Product): Boolean = product.allowDiscountInquiry
```

`allowDiscountInquiry` significa *"el cliente puede **preguntar** por un
descuento"*. No es lo mismo que tener uno. El propio comentario lo reconocía
como aproximación; ya no hace falta aproximar.

#### Qué garantiza el backend

`GET /api/v1/catalog/by-category` trae `discountedPrice` en cada producto. Ver
§3.1.

#### Cambio

Después de §3.1 (que agrega `discountedPrice` al `Product` de Discovery):

```kotlin
fun isDiscountCandidate(product: Product): Boolean = product.discountedPrice != null
```

Y reescribir el KDoc: ya no es una aproximación.

#### Verificación

Poner un descuento a un producto desde staff. En la tablet, la pestaña
"Descuentos" lo muestra y **solo** muestra productos con descuento vigente.

### 2.3 · El snackbar "Producto actualizado correctamente" no se va

**Repo: Staff** · Prioridad: **3** · Triage: BUG-007

#### Síntoma

Tras actualizar un producto, el mensaje quedó pegado. Al navegar a Solicitudes y
volver a Inventario reaparecía; "salió como 3 veces y se fue al fin".

#### Causa

```kotlin
// features/product-inventory/.../ui/InventoryScreen.kt:79-115
LaunchedEffect(registerResult) {
    suspend fun showRegisteredSnackbar(@StringRes messageRes: Int) {
        val result = snackbarHostState.showSnackbar(
            message = resources.getString(messageRes),
            actionLabel = registeredProductId?.let {
                resources.getString(R.string.inventory_register_view_product)
            },
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed && registeredProductId != null) {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked(registeredProductId))
        }
    }

    when (registerResult) {
        INVENTORY_REGISTER_RESULT_SUCCESS -> {
            viewModel.refresh()
            showRegisteredSnackbar(R.string.inventory_register_success)
            onRegisterResultConsumed()                 // ← línea 98
        }
        INVENTORY_REGISTER_RESULT_PLAN_LIMIT -> {
            snackbarHostState.showSnackbar(resources.getString(R.string.inventory_register_plan_limit))
            onRegisterResultConsumed()                 // ← línea 102
        }
        INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED -> {
            viewModel.refresh()
            showRegisteredSnackbar(R.string.inventory_register_discount_failed)
            onRegisterResultConsumed()                 // ← línea 107
        }
        INVENTORY_RESULT_PRODUCT_UPDATED -> {
            viewModel.refresh()
            snackbarHostState.showSnackbar(resources.getString(R.string.inventory_product_updated))
            onRegisterResultConsumed()                 // ← línea 112
        }
    }
}
```

`showSnackbar` es **suspend**: no retorna hasta que el snackbar se cierra o
expira. Y `onRegisterResultConsumed()` está **después**.

Al navegar a Solicitudes, el composable sale de composición, el
`LaunchedEffect` se **cancela**, `showSnackbar` lanza `CancellationException`, y
**`onRegisterResultConsumed()` nunca corre**. La bandera vive en el
`SavedStateHandle` del back stack entry, que sobrevive a la navegación:

```kotlin
// .../navigation/InventoryNavigation.kt:42-55
composable(route = INVENTORY_ROUTE) { entry ->
    val registerResult by entry.savedStateHandle
        .getStateFlow<String?>(INVENTORY_REGISTER_RESULT_KEY, null)
        .collectAsStateWithLifecycle()
    InventoryScreen(
        ...
        registerResult = registerResult,
        onRegisterResultConsumed = {
            entry.savedStateHandle[INVENTORY_REGISTER_RESULT_KEY] = null
            entry.savedStateHandle[INVENTORY_REGISTERED_PRODUCT_ID_KEY] = null
        },
    )
}
```

Al volver, `registerResult` sigue siendo `product_updated`, el `LaunchedEffect`
se relanza y el snackbar vuelve. Las "3 veces" son tres regresos; paró cuando
el usuario se quedó lo suficiente para que `showSnackbar` retornara.

> **Corrección de un diagnóstico anterior.** Una versión previa del triage decía
> que el ViewModel "trataba un evento como estado". **Era incorrecto**:
> `InventoryUiEffect` es un `SharedFlow` one-shot bien implementado. La causa es
> la de arriba.

#### Cambio

Consumir la bandera **antes** de esperar al snackbar, en las **cuatro ramas**.
La bandera solo sirve para disparar el efecto una vez; una vez disparado, ya
cumplió.

```kotlin
when (registerResult) {
    INVENTORY_REGISTER_RESULT_SUCCESS -> {
        val productId = registeredProductId            // capturar ANTES de limpiar
        viewModel.refresh()
        onRegisterResultConsumed()
        showRegisteredSnackbar(R.string.inventory_register_success, productId)
    }
    INVENTORY_REGISTER_RESULT_PLAN_LIMIT -> {
        onRegisterResultConsumed()
        snackbarHostState.showSnackbar(resources.getString(R.string.inventory_register_plan_limit))
    }
    INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED -> {
        val productId = registeredProductId
        viewModel.refresh()
        onRegisterResultConsumed()
        showRegisteredSnackbar(R.string.inventory_register_discount_failed, productId)
    }
    INVENTORY_RESULT_PRODUCT_UPDATED -> {
        viewModel.refresh()
        onRegisterResultConsumed()
        snackbarHostState.showSnackbar(resources.getString(R.string.inventory_product_updated))
    }
}
```

Y `showRegisteredSnackbar` pasa a recibir el id como parámetro en vez de leer
`registeredProductId` del closure — porque `onRegisterResultConsumed()` lo pone
en `null` y la acción "Ver producto" lo necesita después:

```kotlin
suspend fun showRegisteredSnackbar(@StringRes messageRes: Int, productId: String?) {
    val result = snackbarHostState.showSnackbar(
        message = resources.getString(messageRes),
        actionLabel = productId?.let { resources.getString(R.string.inventory_register_view_product) },
        duration = SnackbarDuration.Long,
    )
    if (result == SnackbarResult.ActionPerformed && productId != null) {
        viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked(productId))
    }
}
```

Alternativa equivalente: envolver cada rama en
`try { … } finally { onRegisterResultConsumed() }`, que también limpia cuando
la corrutina se cancela.

#### Verificación

Editar un producto → volver a Inventario → ir a Solicitudes **mientras el
snackbar está visible** → volver a Inventario. El snackbar **no** reaparece.
Registrar un producto nuevo y tocar "Ver producto" en el snackbar: sigue
funcionando (es lo que el cambio podría romper si se captura mal el id).

---

## 3 · Consumir campos que ya llegan

### 3.1 · El precio con descuento

**Repos: ambos** · Prioridad: **4** · Triage: BUG-013 · Staff ✅ lista / ⚠️ detalle · Discovery ✅ `c0cc199` (verificado 2026-10-24)

#### Síntoma

Se le puso un descuento de 5 soles a un producto (35.90 → 30.90). La pantalla
de edición mostraba 30.90, pero la lista de Inventario y la grilla del
Discovery seguían en 35.90.

#### Causa

Hasta el 2026-10-18 el backend **nunca calculó** un precio con descuento:
devolvía `discounts[]` crudo (tipo, valor, ventana) y cada cliente tenía que
hacer la aritmética. Ninguno la hacía. Ahora el precio efectivo lo calcula el
servidor; falta consumirlo.

#### Qué garantiza el backend

Dos campos nuevos en `GET /products`, `GET /products/{id}` y
`GET /catalog/by-category`:

| Campo | Tipo | Significado |
|---|---|---|
| `discountedPrice` | `number` o `null` | Precio **ya con el descuento aplicado**, redondeado a 2 decimales |
| `discountEndsAt` | ISO-8601 o `null` | Fin del descuento aplicado, para el "termina en X" |

`null` = **sin descuento vigente ahora mismo**. Esa es la señal: con `null`
se pinta solo `price`; con valor se pinta `price` tachado y `discountedPrice`
destacado.

Reglas que el backend ya aplicó (no hay que replicarlas):

- Si hay varios descuentos vigentes, gana el que deja el precio más bajo; **no
  se encadenan**.
- Ventana `startsAt <= ahora < endsAt`.
- Un descuento que no baja nada (0 %, o que redondea igual) da `null`.
- Monto mayor que el precio → tope en `0.00`.

#### Cambio

**Staff:**

- `ProductDto` y `Product` — ya cubierto en §2.1 (los campos se agregan juntos).
- `ProductRow.kt` — donde hoy pinta `product.price` (línea ~126), mostrar
  `discountedPrice` cuando no sea `null`, con `price` tachado
  (`TextDecoration.LineThrough`).
- `ProductDetailDto` (`ProductDtos.kt`) — agregar `discountedPrice: Double? = null`
  y `discountEndsAt: String? = null`. El KDoc actual dice *"`discounts[]` is
  present in the payload but not consumed"*; ahora el detalle puede mostrar el
  precio final sin tocar `discounts[]`.

**Discovery:**

- `core/network/.../dto/CatalogDtos.kt`, `CatalogProductDto` — estado actual:

  ```kotlin
  @Serializable
  data class CatalogProductDto(
      val id: String = "",
      val categoryId: String = "",
      val categoryName: String = "",
      val name: String = "",
      val price: Double = 0.0,
      val availability: String = "",
      val allowDiscountInquiry: Boolean = false,
      @SerialName("primaryImageUrl") val primaryImageUrl: String? = null,
      val brand: String? = null,
  )
  ```

  Agregar `val discountedPrice: Double? = null` y `val discountEndsAt: String? = null`.

- `core/domain/.../model/Product.kt` — agregar
  `val discountedPrice: Double? = null` (con default, porque `Product` también
  se construye desde los resultados de búsqueda, que en su momento **no**
  traían el campo — ver "Limitación conocida" abajo, ya resuelta en la Fase 29).
- `core/data/.../catalog/CatalogMappers.kt`, `CatalogProductDto.toDomain()` —
  agregar `discountedPrice = discountedPrice`.
- `features/catalog/.../ui/CatalogContent.kt:108` — hoy:

  ```kotlin
  priceText = stringResource(UiR.string.discovery_price_format, product.price),
  ```

  Pasar también `discountedPrice` al card y pintar tachado + destacado cuando
  no sea `null`.

- `core/network/.../dto/ProductDetailDtos.kt`, `ProductDetailResponseDto` —
  agregar `discountedPrice`/`discountEndsAt`. Hoy el detalle lee `discounts[]`;
  puede seguir mostrándolos como detalle de la promo, pero **el número que se
  muestra como precio final es `discountedPrice`**.

#### No hacer

- **No recalcular.** `discounts[]` sigue viniendo en el detalle para mostrar
  "20 % hasta el viernes", pero el precio final es `discountedPrice`. Volver a
  hacer la aritmética en el cliente —qué descuento gana si hay varios, cómo se
  redondea, qué pasa si el monto supera el precio— es volver al bug.
- **No tratar `null` como "cargando".** Es un estado final: no hay descuento.

#### ~~Limitación conocida (backend, no del cliente)~~ — resuelta

> **Resuelta en la Fase 29 (2026-10-24), verificado 2026-10-31.** `POST /api/v1/conversations/search`
> trae `discountedPrice` y `discountEndsAt` en todos los `results[*]`, también en las sugerencias de
> "sin resultados", con la misma regla y el mismo número que `/catalog/by-category`. Discovery ya los
> consume: `SearchProductDto` los declara y `ProductMappers.kt` los pasa al `Product`, así que la
> pantalla de resultados pinta el precio tachado y el carrito cobra `discountedPrice ?: price`. Ver
> [`API_REFERENCE.md` §29](../api/API_REFERENCE.md#29-búsqueda-por-voz-precio-con-descuento-y-productos-no-encontrados-fase-29).

Texto original, para contexto: los resultados de búsqueda por voz no traían `discountedPrice`
porque ese endpoint vive en otro servicio (vo), que proyectaba los productos sin el cálculo. Mientras
tanto se pintaba `price` sin tachado.

#### Verificación

Poner un descuento desde staff a un producto. Verlo tachado y con el precio
nuevo en: Inventario (lista), detalle del producto, y grilla del Discovery.
Quitar el descuento: vuelve el precio normal en los tres.

### 3.2 · "Productos no encontrados" muestra la oración, no el producto

**Repo: Staff** · Prioridad: **4** · Triage: BUG-020 · ✅ **Titular hecho** en `44b5c30`; la línea con `examples` (paso 3) queda opcional (verificado 2026-10-24)

#### Síntoma

La tarjeta de Inicio muestra *"Dame algo de más de 50 soles."* o *"Quiero una
amoladora"* como si fueran nombres de producto, con un botón "Agregar" al
lado. Y la misma demanda aparece partida: "Amoladora" en una tarjeta y "Quiero
una amoladora" en otra.

#### Causa

El backend **ya manda el término limpio** en `groups[].term` — sin muletillas,
sin puntuación, en singular — y **ya fusiona** los grupos equivalentes. El
mapper lo descarta:

```kotlin
// core/data/.../mapper/StoreInsightsMappers.kt:124-138
private fun UnmetSearchGroupDto.toMissingProductSuggestion(): MissingProductSuggestion? {
    val canonical = term.trim().takeIf { it.isNotEmpty() } ?: return null
    val mostFrequent = examples
        .filter { it.rawText.isNotBlank() }
        .maxByOrNull { it.count }
        ?.rawText
        ?.trim()
    return MissingProductSuggestion(
        id = canonical,                                                   // term → solo como clave
        name = (mostFrequent ?: canonical).replaceFirstChar { it.titlecase(SPANISH_PERU) },
        personCount = personCount,
    )
}
```

`name` sale de `examples[].rawText`, no de `term`. Por eso la frase aparece con
mayúscula inicial y punto final: es el texto crudo title-cased.

El razonamiento está escrito arriba del mapper (`:104-109`):

> *"Each group is named after the raw text shoppers typed most often — 'taladro
> percutor bosch' says what to stock, 'taladro' does not"*

**Y no es tonto**: para una búsqueda *escrita* es cierto. Lo que no previó es la
*hablada*, donde el crudo es el término más relleno — "quiero una amoladora"
no aporta nada sobre "amoladora". Encima, con el LLM de Groq caído de agosto a
octubre (BUG-028), **todos** los `rawText` eran oraciones completas: una
decisión razonable alimentada solo con ruido.

#### Qué garantiza el backend

`GET /api/v1/insights/unmet-searches?days=7&limit=10` (vo):

```json
{
  "periodStart": "…", "periodEnd": "…", "totalPeople": 5,
  "groups": [
    {
      "term": "amoladora",
      "personCount": 5,
      "searchCount": 7,
      "examples": [
        { "rawText": "Quiero una amoladora", "count": 4 },
        { "rawText": "Amoladora", "count": 3 }
      ]
    },
    {
      "term": "amoladora angular 4.5",
      "personCount": 1, "searchCount": 1,
      "examples": [ { "rawText": "Quiero una amoladora angular 4.5", "count": 1 } ]
    }
  ]
}
```

- `term`: canónico. Es lo que el comerciante compraría. Minúsculas, sin tildes.
- `personCount`: personas distintas (sesiones). **Es la métrica principal.**
- `examples[]`: los textos crudos más repetidos, ordenados por `count`. Es la
  evidencia de cómo lo pidieron. Puede venir vacío.

> **Puede que esto llegue ya resuelto a medias.** Si del lado del backend se
> filtran los `examples` que no aportan nada sobre el término, el mapper actual
> cae a `canonical` por su propio `?:` y la tarjeta se arregla sin tocar
> Android. Aun así los cuatro pasos de abajo siguen valiendo: mostrar la
> evidencia *además* del término es mejor que no mostrarla.

#### Cambio

**1. Modelo** — `core/domain/.../model/StoreSuggestions.kt:8-12`

Estado actual:

```kotlin
data class MissingProductSuggestion(
    val id: String,
    val name: String,
    val personCount: Int,
)
```

Objetivo:

```kotlin
data class MissingProductSuggestion(
    val id: String,
    /** Canonical term: what the merchant would stock. Goes on the headline. */
    val name: String,
    /** How shoppers actually asked for it, most frequent first. Empty when it adds nothing over [name]. */
    val examples: List<String> = emptyList(),
    val personCount: Int,
)
```

**2. Mapper** — `StoreInsightsMappers.kt:124-138`

```kotlin
private fun UnmetSearchGroupDto.toMissingProductSuggestion(): MissingProductSuggestion? {
    val canonical = term.trim().takeIf { it.isNotEmpty() } ?: return null
    return MissingProductSuggestion(
        id = canonical,
        name = canonical.replaceFirstChar { it.titlecase(SPANISH_PERU) },
        examples = examples
            .filter { it.rawText.isNotBlank() }
            .sortedByDescending { it.count }
            .map { it.rawText.trim() },
        personCount = personCount,
    )
}
```

Y reescribir el KDoc de `:104-109`, que hoy dice que el grupo se nombra por el
texto crudo.

**3. Tarjeta** — `features/home/.../ui/SuggestionSection.kt`, `MissingProductCard`

Debajo del nombre, una línea secundaria con `examples.firstOrNull()` entre
comillas, en `bodySmall` atenuado — algo como *«"quiero una amoladora"»*. Si
`examples` está vacío, no se pinta nada: no hay nada que agregar sobre el
término.

**4. "Agregar"** — el `onAdd` que recibe `MissingProductsSection` en
`SuggestionSection.kt:45-47` y se cablea en `HomeContent`

Tiene que pre-llenar el registro de producto con `suggestion.name` (el
término), **nunca con la oración**. Si hoy pasa `name` tal cual, con el cambio 2
ya queda bien solo.

**5. Tests** — `core/data/src/test/.../StoreInsightsMappersTest.kt` tiene
casos que **fijan el comportamiento actual** (que `name` sale del `rawText` más
frecuente). Hay que darlos vuelta: el nombre pasa a ser el término y el crudo
se verifica dentro de `examples`.

#### No hacer

- **No** re-canonizar el `rawText` en el cliente (quitar "quiero una", etc.).
  Eso ya lo hizo el backend y está en `term`.

#### Verificación

Con las búsquedas de prueba *"quiero una amoladora"* y *"amoladora"* desde la
tablet, Inicio muestra **una** tarjeta "Amoladora · 2 personas" con la línea
*"quiero una amoladora"* debajo. "Agregar" abre el registro con nombre
"Amoladora".

### 3.3 · "Clientes" se lee como "clientes que compraron"

**Repo: Staff** · Prioridad: **5** · Triage: BUG-003

#### Síntoma

La pestaña Actividad muestra "Clientes: 27" y se lee como 27 ventas. Son 27
personas que usaron la tablet, hayan comprado o no.

#### Qué garantiza el backend

`GET /api/v1/dashboard/activity` suma dos campos:

```json
{
  "uniqueSessions": 27, "uniqueSessionsDeltaPercent": 12.5,
  "productViews": 143,  "productViewsDeltaPercent": -3.1,
  "confirmedOrders": 4, "confirmedOrdersDeltaPercent": 33.3
}
```

| Campo | Qué es |
|---|---|
| `uniqueSessions` | Personas que usaron la tablet (tráfico) |
| `productViews` | Fichas de producto abiertas |
| `confirmedOrders` | **Pedidos confirmados** — este sí es "concretó" |

Los `…DeltaPercent` son `null` cuando el período anterior fue 0.

#### Cambio

**1. DTO** — `core/network/.../dto/DashboardDtos.kt:105-112`, agregar a
`StoreActivityDto`:

```kotlin
val confirmedOrders: Int = 0,
val confirmedOrdersDeltaPercent: Double? = null,
```

**2. Modelo** — `core/domain/.../model/StoreActivity.kt:12-19`, agregar:

```kotlin
val confirmedOrders: Int,
val confirmedOrdersDeltaPercent: Double?,
```

**3. Mapper** — `StoreInsightsMappers.kt:97-103`, `StoreActivityDto.toStoreActivity()`:

```kotlin
confirmedOrders = confirmedOrders,
confirmedOrdersDeltaPercent = confirmedOrdersDeltaPercent.finiteOrNull(),
```

**4. UI** — la pestaña Actividad de `features/sales-statistics`: una tercera
celda "Pedidos confirmados" al lado de las dos existentes, con el mismo
tratamiento del delta. Así el embudo se lee de corrido: *usaron la tablet →
miraron productos → compraron*.

#### Verificación

Confirmar un pedido desde Solicitudes; la celda "Pedidos confirmados" del
período en curso sube en 1.

### 3.4 · "Lo que necesita atención"

**Repo: Staff** · Prioridad: **5** · Triage: BUG-004

#### Síntoma

La tarjeta no se entiende: aparece el taladro más caro de la tienda como "lo
que necesita atención", y el monto se lee como ingreso.

#### Definición exacta (del backend)

Productos **activos**, con **stock > 0**, que **no registraron ninguna venta
confirmada dentro del período consultado**, ordenados por `stock × precio`
descendente. Eso explica el taladro: con la semana en curso casi vacía, *todo*
califica como "no vendido en el período".

#### Qué garantiza el backend

`GET /api/v1/dashboard/ranking` → `needsAttention`:

```json
"needsAttention": {
  "totalImmobilized": 3120.00,
  "totalCount": 17,
  "items": [ { "productId": "…", "name": "…", "stock": 12, "price": 149.00, "immobilizedAmount": 1788.00 }, … ]
}
```

- `totalImmobilized`: Σ (stock × precio) **de los ítems devueltos** (máx. 3).
- `totalCount`: **cuántos productos cumplen la condición en total**, sin el
  límite. Permite decir "3 de 17".

#### Cambio

**1. El label** — `features/sales-statistics/src/main/res/values/strings.xml:40-43`

Estado actual:

```xml
<plurals name="statistics_needs_attention_caption">
    <item quantity="one">Generados por el producto no vendido</item>
    <item quantity="other">Generados por los %1$d productos no vendidos</item>
</plurals>
```

"Generados" dice ingreso. Ese número es **plata parada en inventario**, y está
al lado de las ventas con el mismo lenguaje visual. Es el cambio de una línea
que más mejora esa pantalla:

```xml
<item quantity="one">Inmovilizado en el producto sin ventas del periodo</item>
<item quantity="other">Inmovilizado en %1$d productos sin ventas del periodo</item>
```

(Y el equivalente en `values-en`.) Se usa en `RankingTab.kt:82`.

**2. `totalCount`** — `DashboardDtos.kt:86`, `NeedsAttentionDto`:

```kotlin
val totalImmobilized: Double = 0.0,
val totalCount: Int = 0,          // nuevo
```

Llevarlo a `SalesRanking` (`core/domain/.../model/SalesRanking.kt:32-39`,
agregar `val stalledCount: Int`) y al mapper (`StoreInsightsMappers.kt:62-95`).
Con él, el caption puede decir *"3 de 17 productos sin ventas"*.

**3. Consideración de producto, no de código:** la tarjeta tiene sentido sobre
todo en vista **mensual**. En semanal, a media semana, es ruido por definición.

### 3.5 · El aviso "pedido por expirar"

**Repo: Staff** · Prioridad: **3** · Triage: BUG-008 · **Tres líneas**

#### Síntoma

El toggle "Pedido por expirar" estaba encendido y nunca llegó un aviso. Desde
el 2026-10-18 el backend **sí lo manda** — pero la app lo muestra con el texto
equivocado.

#### Qué garantiza el backend

Push FCM data-only, **10 minutos antes** del vencimiento, una sola vez por
pedido, respetando el toggle:

```json
{ "event": "ORDER_EXPIRING", "orderId": "…", "storeId": "…", "status": "PENDING" }
```

`status` se mantiene en `PENDING` a propósito: **el pedido no cambió de
estado**, solo se le acaba el tiempo. No hay que sacarlo de Pendientes.

#### Causa

```kotlin
// core/notifications/.../OrderNotificationPresenter.kt:54-64
private fun contentFor(event: String?, status: String?): Pair<String, String> = when (event) {
    EVENT_NEW_ORDER -> context.getString(R.string.notif_new_order_title) to
        context.getString(R.string.notif_new_order_body)
    EVENT_ORDER_TAKEN -> context.getString(R.string.notif_order_taken_title) to when (status) {
        STATUS_CONFIRMED -> context.getString(R.string.notif_order_confirmed_body)
        STATUS_REJECTED -> context.getString(R.string.notif_order_rejected_body)
        else -> context.getString(R.string.notif_order_updated_body)
    }
    else -> context.getString(R.string.notif_order_generic_title) to
        context.getString(R.string.notif_order_updated_body)
}
```

`ORDER_EXPIRING` no está, cae en el `else`, y la notificación dice
*"Actualización de órdenes / El estado de una orden cambió"*. Llega y suena,
pero no dice lo que importa.

#### Cambio

En `OrderNotificationPresenter.kt`:

```kotlin
// companion object
private const val EVENT_ORDER_EXPIRING = "ORDER_EXPIRING"

// contentFor, antes del else
EVENT_ORDER_EXPIRING -> context.getString(R.string.notif_order_expiring_title) to
    context.getString(R.string.notif_order_expiring_body)
```

En `core/notifications/src/main/res/values/strings.xml`:

```xml
<string name="notif_order_expiring_title">Un pedido está por vencer</string>
<string name="notif_order_expiring_body">Quedan 10 minutos para atenderlo</string>
```

(Y en `values-en`.)

#### Verificación

Crear un pedido desde la tablet y esperar 50 minutos (TTL 60, aviso a los 50):
llega una notificación con ese título, y el pedido **sigue** en Pendientes.

---

## 4 · Controles que mienten

### 4.1 · El badge fantasma del ícono

**Repo: Staff** · Prioridad: **3** · Triage: BUG-006

#### Síntoma

El ícono de la app muestra un badge "3" sin solicitudes pendientes. Al entrar
no hay nada; al salir, el badge se limpia.

#### Causa

```kotlin
// core/notifications/.../OrderNotificationPresenter.kt:31-52
fun showOrderNotification(event: String?, status: String?, orderId: String?) {
    ensureChannel()
    if (!notificationsAllowed()) return
    val (title, body) = contentFor(event, status)      // ORDER_TAKEN → "Orden actualizada"
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        ...
        .setAutoCancel(true)
        .build()
    try {
        NotificationManagerCompat.from(context).notify(notificationId(orderId), notification)
    } catch (_: SecurityException) { }
}
```

Publica una notificación para **todos** los eventos, `ORDER_TAKEN` incluido, y
**nunca cancela ninguna**. Secuencia real:

1. `NEW_ORDER` del pedido X → notificación con id `X.hashCode()`.
2. Alguien confirma X → el backend manda `ORDER_TAKEN`.
3. La app publica **otra** notificación con el **mismo id**, que reemplaza a
   la anterior por *"Orden actualizada"*.
4. Esa notificación **se queda en la bandeja**, y el badge del launcher la
   cuenta.

`setAutoCancel(true)` solo la quita si el usuario la **toca**; abrir la app
desde el ícono no la borra.

#### Qué garantiza el backend

- El push es data-only: **nunca manda un badge** ni un bloque `notification`.
- `ORDER_TAKEN` llega para confirmado, rechazado **y expirado**, siempre con el
  `orderId`. Es la señal para limpiar.
- `ORDER_TAKEN` es el único evento que **no** se filtra por los toggles del
  usuario, justo para que la señal de limpieza siempre llegue.

#### Cambio

**1. `ORDER_TAKEN` debe cancelar, no publicar.** Significa "otro ya lo
atendió": no hay nada que hacer.

```kotlin
fun showOrderNotification(event: String?, status: String?, orderId: String?) {
    ensureChannel()
    if (event == EVENT_ORDER_TAKEN) {
        NotificationManagerCompat.from(context).cancel(notificationId(orderId))
        return
    }
    if (!notificationsAllowed()) return
    // … resto igual para NEW_ORDER y ORDER_EXPIRING
}
```

**2. El badge del launcher no debe acumularse desde los pushes.** Un push es
best-effort: si el celular estuvo sin red o en Doze, el `ORDER_TAKEN` nunca
llega y el badge queda mintiendo.

**El patrón correcto ya existe en la app.** El badge *interno* de la pestaña
Solicitudes se deriva del servidor:

```kotlin
// app/.../navigation/MainShell.kt:112,147-153
val pendingOrderCount by viewModel.pendingOrderCount.collectAsStateWithLifecycle()
...
val badged = destination.route == ORDER_REQUESTS_ROUTE && pendingOrderCount > 0
badgeCount = if (badged) pendingOrderCount else null,
```

Y `pendingOrderCount` sale de `OrderRepositoryImpl` conservando
`totalElements` de `GET /api/v1/orders?status=PENDING&size=1`. El badge del
**launcher** (si se quiere mantener) debería usar esa misma fuente, no la
bandeja de notificaciones.

#### Verificación

Crear un pedido desde la tablet (llega la notificación) → confirmarlo desde
otro celular o desde la misma app. La notificación **desaparece** de la bandeja
sin tocarla, y el badge del ícono queda en 0.

### 4.2 · "Quedan pocos disponibles" sobre un producto agotado

**Repo: Staff** · Prioridad: **5** · Triage: BUG-017

#### Síntoma

En el detalle de la solicitud, un producto con **0** de stock mostraba la
franja amarilla *"Quedan pocos disponibles"*. Suavizar un agotado a "quedan
pocos" es justo lo que llevó a confirmar un pedido que no se podía despachar.

#### Causa

```kotlin
// features/order-requests/.../ui/OrderDetailDialog.kt:67
private const val LOW_STOCK_THRESHOLD = 5

// :291-295
// Null stock means a consult-mode product: nothing is tracked, so nothing is warned about.
val stock = item.productStock
if (stock != null && stock <= LOW_STOCK_THRESHOLD) {
    LowStockWarning()
}
```

`0 <= 5` es cierto, así que **cero entra por el camino de "quedan pocos"**.

El backend manda el stock vivo y crudo (`items[].productStock`); el umbral es
decisión del cliente y está bien que lo sea.

**La misma app ya resuelve esto bien en otra pantalla**, y dejó escrito por qué:

```kotlin
// features/home/.../ui/RestockProductCard.kt:121-134
/**
 * "Solo queda 1" / "Solo quedan 3" — the plural is part of the warning, not an afterthought. An
 * exhausted product reads "Sin existencias": "Solo quedan 0" would soften a stock-out into a
 * low-stock warning.
 */
val text = if (remainingStock <= 0) {
    stringResource(R.string.home_out_of_stock)
} else {
    pluralStringResource(R.plurals.home_remaining_stock, remainingStock, remainingStock)
}
```

#### Cambio

En `OrderDetailDialog.kt:291-295`:

```kotlin
val stock = item.productStock
when {
    stock == null -> Unit                           // modo consultar: nada que advertir
    stock <= 0 -> OutOfStockWarning()               // nuevo: rojo, "Sin stock"
    stock <= LOW_STOCK_THRESHOLD -> LowStockWarning()
}
```

`OutOfStockWarning` con el mismo layout que `LowStockWarning` pero color de
error y el string `order_item_out_of_stock` = *"Sin stock"* (nuevo en
`features/order-requests/.../strings.xml`, hoy solo existe
`order_item_low_stock`).

Adicionalmente, el botón "Confirmar pedido" puede deshabilitarse cuando algún
ítem tiene `stock == 0`: el backend lo va a rechazar igual con *"Ya no hay
stock suficiente de …"*, pero evitar el viaje es mejor UX.

#### Verificación

Dejar un producto en stock 0, tener un pedido pendiente que lo incluya, abrir
el detalle: la franja dice "Sin stock" en rojo, no "Quedan pocos".

### 4.3 · Inicio no se refresca al volver

**Repo: Staff** · Prioridad: **5** · Triage: BUG-009

#### Síntoma

Se repuso el stock de un producto y en Inicio seguía en "Productos para
reponer" con "Sin existencias".

#### Causa

El backend lee el stock vivo y **no hay caché de ningún tipo** en biz. El
problema es que `HomeViewModel` solo recarga con `HomeUiEvent.Refresh` o al
cambiar de período:

```kotlin
// features/home/.../ui/HomeViewModel.kt
private val reloadTrigger = MutableStateFlow(0)
...
private fun observeLoads() {
    combine(selectedPeriod, reloadTrigger) { period, _ -> period }
        .onEach { _uiState.update { state -> state.copy(isLoading = true, isError = false) } }
        .flatMapLatest { period -> flow { emit(load(period)) } }
        .onEach { outcome -> applyOutcome(outcome) }
        .launchIn(viewModelScope)
}
```

**No hay recarga por ciclo de vida.** Como Inicio es una pestaña del bottom
nav, su entry del back stack sobrevive y el ViewModel se retiene: al volver
desde Inventario se re-muestra el snapshot inicial. `HomeScreen.kt` solo tiene
un `LaunchedEffect(Unit)` para los efectos de navegación.

Es el mismo patrón que §2.3: estado que sobrevive a la navegación.

#### Cambio

En `HomeScreen.kt`, disparar `Refresh` cada vez que la pantalla vuelve a
primer plano:

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) viewModel.onEvent(HomeUiEvent.Refresh)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

`load()` ya está protegido con `flatMapLatest`, así que dos `Refresh` seguidos
no duplican trabajo.

#### Verificación

Inicio → Inventario → reponer stock del producto que está en "Productos para
reponer" → volver a Inicio. La tarjeta desaparece sin pull-to-refresh.

### 4.4 · El chevron que no vuelve a ningún lado

**Repo: Staff** · Prioridad: **5** · Triage: BUG-011

#### Causa

```kotlin
// features/order-requests/.../ui/OrderRequestsHeader.kt:73-80
Row(verticalAlignment = Alignment.CenterVertically) {
    // Visual-only chevron, matching the DS header used across the app's top-level tabs.
    IconButton(onClick = {}) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.order_requests_back_cd),
            tint = QhathiaColors.onHeaderGradient,
        )
    }
```

Decorativo, con el `onClick` vacío. Y es correcto que no navegue: Solicitudes
es una pestaña raíz, no hay pantalla anterior.

**El problema es que se ve como un botón** — y peor, tiene `contentDescription`,
así que TalkBack lo anuncia como una acción de volver que no existe.

#### Cambio

Quitar el `IconButton` de los encabezados de las pestañas raíz (Solicitudes, y
revisar Inventario e Inicio, que usan el mismo DS header). Si se quiere
conservar la simetría visual, dejar un `Spacer` del mismo ancho — pero no un
`IconButton`, que dibuja ripple al tocarlo y promete algo que no pasa.

---

## 5 · Pantallas incompletas

Todas tienen el endpoint listo en el servidor. Dónde haga falta un método de
cliente que todavía no existe, se indica.

### 5.1 · Editar datos personales

**Repo: Staff** · Triage: BUG-010

#### Estado actual

```kotlin
// features/profile/.../personal/PersonalDataScreen.kt:58-60
/**
 * Stateless Datos personales. Every field is read-only by product decision, and "Cambiar foto" is
 * shown but not available: the backend has no avatar upload.
 */
```

Los tres campos se pintan con `ReadOnlyField`, un `Box` con borde que *parece*
un input pero nunca lo fue. `PersonalDataViewModel` solo carga; su único evento
es `Retry`.

**"Cambiar foto · Próximamente" sí es correcto**: no hay endpoint de avatar.

#### Qué garantiza el backend

`PUT /api/v1/users/me` · cualquier rol autenticado · update **parcial** (solo
se aplican los campos no-null):

```json
{ "fullName": "Rosa Quispe Mamani", "phone": "987654321" }
```

| Campo | Validación |
|---|---|
| `fullName` | 2–150 caracteres |
| `phone` | `^9\d{8}$` — 9 dígitos, empieza en 9. **Sin `+51`**: el prefijo lo pinta el cliente |
| `email` | **No se acepta.** Es la credencial de login; cambiarlo sin verificación permite secuestrar la cuenta. Si se manda, se ignora |

Respuesta: el `UserProfileResponse` completo (`id`, `email`, `fullName`,
`phone`, `role`, …). Error de validación → 422 con `fieldErrors`.

#### Cambio

1. `core/network/.../service/UserApiService.kt` hoy solo tiene
   `client.get("api/v1/users/me")`. Agregar:

   ```kotlin
   suspend fun updateProfile(fullName: String?, phone: String?): NetworkResult<UserProfileDto> =
       runCatching {
           val body = buildJsonObject {
               fullName?.let { put("fullName", it) }
               phone?.let { put("phone", it) }
           }
           client.put("api/v1/users/me") { setBody(body) }.body<UserProfileDto>()
       }.toNetworkResult()
   ```

2. Repositorio y use case (`UpdateUserProfileUseCase`) siguiendo el patrón de
   `SetPricesIncludeIgvUseCase` (que se va a borrar en §6, pero sirve de molde).
3. `PersonalDataViewModel`: estado editable para `fullName` y `phone`, evento
   `Save`, `isSaving`, y el error de validación mapeado al campo.
4. `PersonalDataScreen`: `fullName` y `phone` como `OutlinedTextField`; **el
   correo se queda como `ReadOnlyField`** con un subtítulo que diga por qué
   ("para cambiar tu correo, contáctanos"). Botón Guardar habilitado solo si
   hay cambios válidos.

Si la decisión de producto sigue siendo "solo lectura", entonces los campos no
deberían parecer inputs — regla 4.

### 5.2 · Agregar y quitar fotos del producto

**Repo: Staff** · Triage: BUG-014

#### Estado actual

```kotlin
// features/product-detail/.../ui/ProductImagesRow.kt
/**
 * Read-only gallery of the product's registered photos, in backend order (primary first). Adding and
 * removing photos is not part of this slice, so the tiles carry no affordances; a product with no
 * media shows an explanatory placeholder instead of an empty strip.
 */
```

#### Qué garantiza el backend

| Endpoint | Qué hace |
|---|---|
| `POST /api/v1/products/{id}/images` · multipart, parts `images` (1..N) | Sube y adjunta. Posiciones continúan desde la máxima actual, `primary=false`. Responde `201` con `List<MediaResponse>`. **Tope 10 fotos por producto** (default) |
| `DELETE /api/v1/products/{id}/media/{mediaId}` | Quita una foto. `204` |
| `GET /api/v1/products/{id}/media` | Lista |

`MediaResponse`: `{ "id", "url", "mediaType": "IMAGE", "position", "primary", "createdAt" }`.

El registro de producto (`POST /products/with-images`) **ya sube fotos con
multipart desde la app** — `ProductApiService` tiene el patrón; reutilizar.

#### Cambio

- `ProductImagesRow`: un tile "+" al final que abre el selector (el mismo que
  usa el registro) y una "×" por tile para quitar.
- `ProductDetailViewModel`: eventos `AddPhotos(uris)` / `RemovePhoto(mediaId)`
  que llaman a los endpoints y **recargan `media`** desde la respuesta (no
  mantener una lista local optimista: las posiciones las decide el servidor).
- `ProductDetailDto.media` ya tiene `id` por foto para el `DELETE`.

### 5.3 · La campana del detalle de producto

**Repo: Staff** · Triage: BUG-014

```kotlin
// features/product-detail/.../ui/ProductDetailHeader.kt:49-52
IconButton(onClick = {}) {
    Icon(imageVector = Icons.Outlined.Notifications, ...
```

Mismo caso que §4.4: un ícono que parece accionable y no lo es. O navega a
Solicitudes (como la campana de Inicio, que emite
`HomeUiEffect.NavigateToOrderRequests`) o no se dibuja.

> **Lo que sí ya está bien:** el diálogo "¿descartar cambios?" **existe** y el
> botón de volver del header **sí** pasa por él —
> `ProductDetailContent.kt:68` cablea `onBack = { onEvent(CancelRequested) }`,
> y `ProductDetailScreen.kt:47` tiene `BackHandler(enabled = uiState.isDirty …)`.
> No hay nada que hacer ahí.

### 5.4 · El PIN del kiosko

**Repo: Staff** · Triage: BUG-027 · ✅ **Implementado (PR #29)**, y §1 también, así que el PIN sí llega a la tablet

#### Qué garantiza el backend

`PUT /api/v1/devices/{deviceId}/settings` · OWNER/ADMIN · update **parcial**:

```json
{ "exitPin": "4821" }           // poner o cambiar: 4–6 dígitos
{ "removeExitPin": true }       // quitar
```

`exitPin` es **write-only**: se guarda con BCrypt y **nunca vuelve** en ninguna
respuesta del comerciante. `GET /devices/{deviceId}/settings` devuelve el resto
de los ajustes (`idleShowcaseEnabled`, `voiceAssistantEnabled`,
`brightnessMode`, `kioskMode`, `autoSyncEnabled`, `settingsVersion`) pero no el
PIN ni su hash.

Un `null` en `exitPin` significa "no lo toques"; por eso quitarlo es un campo
aparte — un borrado accidental dejaría la tablet abierta sin que nadie se
entere.

#### Cambio

1. `core/network/.../service/DeviceApiService.kt` hoy tiene `getDevices`,
   `pairDevice`, `sendCommand`, `getCommands`. Agregar `getSettings(deviceId)`
   y `updateSettings(deviceId, body)`.
2. En la pantalla de ajustes de la tablet (`features/device-linking` o donde
   vivan los ajustes remotos): un campo "PIN de salida" con dos acciones,
   *Cambiar* (pide 4–6 dígitos, confirma, manda `exitPin`) y *Quitar* (manda
   `removeExitPin: true`). Nunca mostrar el valor actual: no existe del lado
   del comerciante.

⚠️ **Aunque esto se implemente, el PIN no llegará a la tablet** hasta que la app
discovery implemente el heartbeat (§1): los ajustes se entregan por ahí. Los
dos cambios tienen que ir juntos para que esto se vea funcionar.

### 5.5 · Los tres puntos del producto: falta "Eliminar"

**Repo: Staff** · Triage: BUG-030

#### Estado actual — corrección de un diagnóstico anterior

El menú **sí existe y sí abre**:

```kotlin
// features/product-inventory/.../ui/ProductRow.kt:179-200
DropdownMenu(expanded = expanded, ...) {
    DropdownMenuItem(text = { Text(stringResource(R.string.inventory_action_edit_product)) }, ...)
    DropdownMenuItem(text = { Text(stringResource(
        if (hasDiscount == false) R.string.inventory_action_register_discount
        else R.string.inventory_action_edit_discount)) }, ...)
}
```

Tiene "Editar producto" y "Registrar/Editar descuento". Lo que **falta** es lo
que el reporte pedía como ejemplo: **eliminar**.

#### Qué garantiza el backend

`DELETE /api/v1/products/{id}` y `POST /api/v1/products/{id}/deactivate` hacen
**lo mismo**: un *soft delete* (`active = false`). El producto desaparece de
Inventario, del Discovery, de la búsqueda y del feed de WhatsApp, pero la fila
y su historial de pedidos se conservan. No hay endpoint de reactivación hoy.

#### Cambio

Un tercer `DropdownMenuItem` "Eliminar" (string nuevo
`inventory_action_delete_product`) → diálogo de confirmación → `DELETE
/products/{id}` → `viewModel.refresh()`. Como es soft delete, el texto del
diálogo puede ser "Se quitará del catálogo" en vez de "Se borrará para
siempre".

### 5.6 · Compartir la cotización por WhatsApp desde el detalle del pedido

**Repo: Staff** · Triage: BUG-030 · **La función ya existe entera** · ❌ **Pendiente** (verificado 2026-10-24)

El reporte: *"por si mi cliente me lo vuelve a pedir, se lo genero y le mando
la mini cotización de su compra"*. **Eso ya está construido y probado**: es el
flujo del carrito del kiosko. Genera el PDF, lo sube a R2 y lo envía por
WhatsApp. Solo le falta el botón en esta pantalla.

#### Qué garantiza el backend

`POST /api/v1/carts/share-pdf` · cualquier rol de tienda:

```json
{
  "customerName": "Meisy",
  "customerPhone": "987654321",
  "items": [ { "productId": "…", "quantity": 2 }, { "productId": "…", "quantity": 1 } ]
}
```

- `items` máx. 20; `quantity` 1–99. Mismo shape que `POST /orders`.
- `customerPhone`: `^9\d{8}$`.

Respuesta:

```json
{ "status": "SENT", "messageId": "wamid.…", "pdfUrl": "https://…/carts/….pdf", "error": null }
```

`status` ∈ `SENT | STUB | FAILED`. **`pdfUrl` viene siempre**, aunque el envío
falle: se puede ofrecer "Abrir PDF" como respaldo.

#### Cambio

En `OrderDetailDialog`, un botón "Enviar cotización por WhatsApp" que arme el
request desde `order.items[]` (`productId`, `quantity`) y
`order.customerName`/`customerPhone`, y muestre el resultado. Si `status` es
`FAILED`, ofrecer el `pdfUrl`.

### 5.7 · "Ver producto" tras registrar

**Repo: Staff** · Triage: BUG-030 · **Ya existe**

```kotlin
// features/product-inventory/.../navigation/InventoryNavigation.kt:49
registeredProductId = entry.savedStateHandle[INVENTORY_REGISTERED_PRODUCT_ID_KEY],

// .../ui/InventoryScreen.kt:84-85
actionLabel = registeredProductId?.let {
    resources.getString(R.string.inventory_register_view_product)
},
```

El snackbar de "Producto registrado" **ya ofrece "Ver producto"** y hace scroll
hasta la fila. Si en el dispositivo no aparece la acción, es que
`INVENTORY_REGISTERED_PRODUCT_ID_KEY` no se está escribiendo al volver del
registro — revisar en la pantalla de registro que se ponga en el
`SavedStateHandle` del entry anterior junto con el resultado. Y aplicar §2.3,
que cambia cómo se captura ese id.

### 5.8 · Vista previa del Discovery

**Repo: Staff** · Triage: BUG-030

El botón "Vista previa" existe y `ProductDetailUiEvent.PreviewRequested` /
`PreviewDismissed` están definidos; `productPreviewMapper.kt` arma el modelo.
Falta la pantalla que lo pinte como lo vería el cliente en la tablet (card con
foto, nombre, precio — y precio tachado si hay descuento, §3.1).

### 5.9 · "X" para limpiar el buscador de Inventario

**Repo: Staff** · Triage: BUG-030 · UI pura

Un `trailingIcon` con `Icons.Filled.Close` en el `TextField` de búsqueda,
visible solo cuando el texto no está vacío, que lo limpie.

### 5.10 · "Nueva búsqueda" → "Ir al catálogo"

**Repo: Discovery** · Triage: BUG-030 · ✅ **Hecho** en `search-go-to-catalog` (2026-10-30). Decisión de producto: "Ir al catálogo" se **agrega** (sin diálogo, conserva el carrito) y "Nueva búsqueda" se queda; su "Sí, cerrar" ahora vacía el carrito y lo avisa (CAR-040)

#### Estado actual

```kotlin
// features/product-search/.../ui/ProductSearchViewModel.kt:119-120
ProductSearchUiEvent.Back -> onBack()
ProductSearchUiEvent.NewSearch -> _uiState.update { it.copy(showCloseConfirmDialog = true) }

// :382-389
private fun onBack() {
    if (history.size > 1) { history.removeAt(history.lastIndex); showEntry(history.last()) }
    else { _uiState.update { it.copy(showCloseConfirmDialog = true) } }
}
```

"Volver" retrocede en el historial de refinamientos y, al llegar al principio,
pide confirmación y va a Home. "Nueva búsqueda" pide confirmación y va a Home.
**Desde el primer resultado, los dos terminan en el mismo lugar.**

#### Cambio (pedido de producto)

Que el segundo botón sea **"Ir al catálogo"** y navegue a la grilla
(`features/catalog`), no a Home. Razón: cuando la búsqueda no encuentra lo que
el cliente quiere, lo útil es ver todo, no volver a la pantalla de bienvenida.
Strings en `core/ui/.../values/strings.xml:20,27`
(`discovery_action_new_search`, `discovery_cd_new_search`).

### 5.11 · Mostrar lo que la tablet entendió por voz

**Repo: Discovery** · Triage: BUG-030 · ✅ **Hecho** en `e39ee01`: transcripción en vivo (verificado 2026-10-24)

#### Estado actual

La transcripción **ya existe en el cliente** — la app manda texto a vo, no audio:

```kotlin
// features/home/.../ui/HomeViewModel.kt:~197
private fun onTranscribed(transcript: String) {
    _uiState.update { it.copy(micState = MicState.Success(transcript), query = "") }
    emitEffect(HomeUiEffect.NavigateToProductSearch(transcript, SearchSource.Voice))
}
```

El `transcript` viaja como argumento a la pantalla de resultados.

#### Cambio

En `ProductSearchContent`, arriba de "Encontré N productos para ti", una línea
*«Buscaste: "quiero una amoladora"»*. Así el cliente sabe si el STT lo entendió
(el reporte: *"no sé si lo que pedí lo interpretó bien la IA"*). Sin llamadas
nuevas: el texto ya está.

---

## 6 · Borrar: el toggle de IGV

**Repo: Staff** · Triage: BUG-012 · **Cambio de contrato** · Sin prisa

### Qué pasó

`pricesIncludeIgv` era una declaración, no un cálculo: su único efecto era
elegir entre dos líneas de texto del PDF. Apagarlo no movía ningún precio, y
se reportó como bug — con razón, porque un control sin efecto observable se
lee como roto. Se retiró en vez de hacerlo calcular: en Perú el precio al
consumidor lleva IGV incluido por ley y así lo cargaban **todas** las tiendas.

### Qué garantiza el backend

`pricesIncludeIgv` e `igvRate` **ya no existen** en `GET/PUT /api/v1/stores/{id}`.

**No rompe apps instaladas**, verificado con test:

- Mandar `pricesIncludeIgv` en el `PUT` → **200**, el campo se ignora.
- Su ausencia en la respuesta → el DTO tiene default (`StoreSettingsDto.kt:52-53`),
  la deserialización no falla.

Mientras no se borre, el toggle manda un campo que se ignora y lee uno que no
llega: queda fijo en "encendido" sin hacer nada — exactamente lo que hacía antes.

### Qué borrar — 14 archivos de alcance

| Archivo | Qué |
|---|---|
| `features/profile/.../settings/AppSettingsScreen.kt` | `IgvSetting` (`:122-150`) y su llamada (`:115`), el efecto `ShowIgvSaveError` (`:67-68`), `PreviewStore` (`:191`) |
| `features/profile/.../settings/AppSettingsViewModel.kt` | `isIgvSaving`, `ToggleIgv`, `ShowIgvSaveError`, `toggleIgv()`, la inyección de `SetPricesIncludeIgvUseCase` |
| `features/profile/src/test/.../AppSettingsViewModelTest.kt` | Los casos del toggle |
| `core/domain/.../usecase/SetPricesIncludeIgvUseCase.kt` | Entero |
| `core/domain/.../model/StoreSettings.kt` | `pricesIncludeIgv`, `igvRate` |
| `core/domain/.../repository/StoreSettingsRepository.kt` | `updatePricesIncludeIgv` |
| `core/data/.../repository/StoreSettingsRepositoryImpl.kt` | Su implementación |
| `core/data/.../mapper/ProfileMappers.kt` | `:89-90` |
| `core/data/src/test/.../ProfileMappersTest.kt`, `ProfileRepositoriesTest.kt` | Los casos del flag |
| `core/network/.../dto/ProfileDtos.kt` | `StoreSettingsDto.pricesIncludeIgv`, `igvRate` (`:52-53`) |
| `core/network/.../service/StoreApiService.kt` | El parámetro `pricesIncludeIgv` de `updateStore` (`:29,34`) |
| `core/testing/.../FakeProfileRepositories.kt`, `TestFixtures.kt` | Los fakes |
| `features/profile/src/main/res/values/strings.xml` y `values-en` | `settings_igv`, `settings_igv_subtitle`, `settings_igv_read_only`, `settings_igv_save_error` |

La pantalla Configuración queda solo con el idioma, y el subtítulo del ítem en
el hub ("Idioma y precios") pasa a "Idioma".

---

## Apéndice A · Campos nuevos y retirados, en un vistazo

Todo desplegado y verificado el 2026-10-18/19. **Estado en el front actualizado el 2026-10-31**
contra `qhathia-mobile-discovery@main` y `qhathia-mobile-staff@0ca3de8`; la evidencia de staff es la
de la tabla "Estado al 2026-10-24" de arriba.

| Endpoint | Campo | Tipo | Estado en el front |
|---|---|---|---|
| `GET /products` | `stock` | `Int?` | ✅ Staff: `ProductDto.stock`, `mockedStock` borrado · `4813b31` (§2.1) |
| `GET /products`, `/products/{id}`, `/catalog/by-category`, `POST /conversations/search` | `discountedPrice` | `Double?` | ✅ Discovery: grilla, detalle, relacionados, búsqueda y carrito · `c0cc199`. Staff: ✅ lista · `4813b31`; ⚠️ el detalle todavía lo calcula en el cliente (Nota 1) (§3.1) |
| idem | `discountEndsAt` | ISO-8601 `?` | ◐ Las dos apps lo reciben y lo guardan en el modelo, pero ninguna pantalla lo muestra todavía. Opcional: el "termina en X" (§3.1) |
| `GET /dashboard/activity` | `confirmedOrders`, `confirmedOrdersDeltaPercent` | `Int`, `Double?` | ✅ Staff: `ActivityTab.kt` · `255b418` (§3.3) |
| `GET /dashboard/ranking` | `needsAttention.totalCount` | `Int` | ✅ Staff: "Inmovilizados en X de los Y productos sin ventas" · `255b418` (§3.4) |
| `GET /insights/unmet-searches` | `term` (canónico) | `String` | ✅ Staff: titular = `term` · `44b5c30`. Opcional: la línea con `examples` (§3.2, paso 3) |
| `GET /orders/{id}` | `items[].productImageUrl` | `String?` | ✅ ya consumido |
| Push FCM | evento `ORDER_EXPIRING` | — | ✅ Staff: "Orden por vencer" · `1f6484f` (§3.5) |
| `POST /devices/heartbeat` | — | — | ✅ Discovery: cada 60 s, telemetría, ajustes y comandos · `aaf07d5`; verificado con staff el 2026-10-31 (change `device-heartbeat`) (§1) |
| `GET/PUT /stores/{id}` | ~~`pricesIncludeIgv`~~, ~~`igvRate`~~ | — | ✅ Staff: toggle retirado · `cf465d3` (§6) |

~~**Pendiente del backend, no del cliente:** `discountedPrice` en los resultados
de búsqueda por voz (`POST /conversations/search`).~~ Resuelto en la Fase 29 (ver §3.1).

## Apéndice B · Verificación de punta a punta

Lo que se puede comprobar hoy contra producción, en el orden en que conviene:

1. **Stock** (§2.1) — Inventario vs detalle del mismo producto: mismo número.
   Hoy difiere en los 41.
2. **Descuento** (§3.1, §2.2) — poner uno desde staff; tachado en Inventario,
   detalle, grilla del Discovery y pestaña "Descuentos".
3. **Heartbeat** (§1) — card de Dispositivos pasa a "Conectada"; "Actualizar
   catálogo" pasa a "Listo"; un ajuste cambiado en staff se aplica en la
   tablet.
4. **Snackbar** (§2.3) — no reaparece al volver.
5. **Badge** (§4.1) — confirmar un pedido limpia la notificación sin tocarla.
6. **"Quedan pocos"** (§4.2) — con stock 0 dice "Sin stock".
7. **Sugerencias** (§3.2) — *"quiero una amoladora"* y *"amoladora"* son una
   tarjeta "Amoladora".
8. **Aviso por vencer** (§3.5) — a los 50 min del pedido llega con su título.

Lo que **no** depende de Android y ya funciona en el backend (no hace falta
probarlo desde la app): los filtros de Inventario que se cruzan, "Stock bajo"
con umbral 5, el rango de precio por voz, el botón de WhatsApp del PDF, los
comandos que ya no quedan bloqueados.

## Orden sugerido

| | Qué | Por qué primero |
|---|---|---|
| 1 | **Heartbeat** (§1) | Una llamada, cinco funciones muertas |
| 2 | **Borrar `mockedStock`** (§2.1) | Hoy el comerciante toma decisiones sobre un número falso |
| 3 | **Snackbar** (§2.3), **badge** (§4.1), **texto de `ORDER_EXPIRING`** (§3.5) | Molestan en cada uso; los tres son chicos |
| 4 | **`discountedPrice`** (§3.1) + pestaña Descuentos (§2.2) + **término canónico** (§3.2) | Funciones de venta invisibles |
| 5 | **"Quedan pocos" con 0** (§4.2), Inicio sin recargar (§4.3), chevron (§4.4), `confirmedOrders` (§3.3), label (§3.4) | Pantallas que mienten o confunden |
| 6 | Pantallas incompletas (§5) y el toggle de IGV (§6) | Independientes entre sí; pueden ir en paralelo |
