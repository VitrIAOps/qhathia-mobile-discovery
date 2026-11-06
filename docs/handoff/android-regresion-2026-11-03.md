# Regresión QA del 03/11 — lo que falta en las apps Android

> **Para quién es esto.** Para la IA (o la persona) que va a implementar los cambios en las dos apps
> Android: **Discovery** (tablet del kiosko) y **Staff** (celular del comerciante). Cada arreglo trae lo
> que QA vio hoy, lo que la app ya tiene, la causa en el código **con archivo y línea**, el contrato
> exacto del backend, el cambio a aplicar y cómo verificarlo. Está escrito para ejecutarse sin
> redescubrir nada.

---

## 1 · Empieza aquí

### Qué se probó y qué salió

| | Versión probada | Commit |
|---|---|---|
| Backend (auth, biz, vo) | fases 35–43, en producción | master `a2da54b` |
| Discovery (tablet) | 1.23.0-SNAPSHOT | `490694e` |
| Staff (celular) | 1.37.0-SNAPSHOT | `2c6aa0f` |

La regresión del 03/11 re-ejecutó los 104 arreglos `F-nn` de la campaña QA del 25–28/10:

| Corregidos | Parciales | No corregidos | Bloqueados |
|---|---|---|---|
| 33 | 40 | 28 | 3 |

**Del lado del backend está todo corregido** salvo F-59 (nombres "Tablet N" repetidos, que se arregla
aparte en el servidor). Lo que queda es de las apps:

- **Staff no implementó ninguno de los arreglos de la campaña.** Entre `0ca3de8` (sobre el que se
  escribieron los `fixes-staff-*.md`) y `2c6aa0f` solo entraron cambios ajenos: precio con descuento en
  el Detalle, cuenta regresiva del descuento y ejemplos de "Productos no encontrados".
- **Discovery trae las olas 1–3 de QA, pero nada de lo que el backend agregó en las fases 35–42:** no
  reacciona a `DEVICE_UNLINKED`, no manda `expectedUnitPrice`, no consulta `/orders/{id}/status`, no manda
  `filters` en la búsqueda, no usa `replacesSessionId`, `/cancel` ni `/status` de la vinculación, no lee
  `storeName` y no envía `POST /devices/logs`.

### Cómo usar este documento

1. Lee esta sección 1 entera: orden de trabajo, piezas comunes, decisiones pendientes.
2. Ve a la parte de tu app (secciones 2 y 3) y trabaja **en el orden de la tabla de la sección 1.4**.
3. Cada arreglo tiene siempre las mismas partes: **Qué ve QA hoy · Qué ya está hecho · Causa en el código ·
   Qué garantiza el backend · Cambio · No hacer · Verificación**.
4. Los números de línea se verificaron en los commits de arriba. Si el archivo cambió, busca el fragmento
   citado tal cual.

**Qué manda sobre qué:**
- **Este documento manda sobre `qa/consolidado/fixes/fixes-*.md`.** Esos archivos tienen líneas
  desfasadas y, en algunos puntos, contratos que el backend implementó de otra forma (F-48, F-60, F-73,
  F-29, F-30, F-93…). Se escribieron antes de implementar el servidor.
- **[`docs/api/API_REFERENCE.md`](../api/API_REFERENCE.md) §35–43 manda sobre todo lo demás** para el contrato.
  Si dudas, el código del backend en `services/` es la verdad final.
- La evidencia de cada caso (pasos y capturas) está en `qa/regresion/qa-reports/2026-11-03-Rnn.md`,
  `2026-11-03-pendientes.md` y `2026-11-03-resumen.md`. Lo que "debe pasar ahora" de cada arreglo, en
  `qa/regresion/qa-plan/Rnn-*.md`.

### 1.1 · Reglas que valen para todo

1. **Nunca recalcular en el cliente lo que el backend ya calcula** (precio con descuento, disponibilidad,
   totales). Si el campo viene, se usa el campo.
2. **Nunca inventar un dato que no viene.** `null` tiene significado: `discountedPrice: null` = sin
   descuento vigente; `stock: null` = modo "a consultar".
3. **Un control que no hace nada no debe parecer un control.** Si no es accionable, no se dibuja como tal.
4. **Leer el `errorCode` del `problem+json` antes que el código HTTP.** Dos 409 distintos
   (`PRICE_CHANGED`, `PRODUCT_NOT_SELLABLE`) piden reacciones distintas. Hoy las dos apps descartan el
   cuerpo de error en casi todas las llamadas: es la pieza común número 1 (abajo).
5. Los JSON son `camelCase` y `ignoreUnknownKeys` está activo: agregar un campo al DTO nunca rompe.
6. Strings nuevos en `values/` **y** `values-en/`. Tests JUnit 5 (+ Turbine en Staff) con los fakes de
   `core/testing`. Al terminar cada módulo, `./gradlew :<módulo>:testDebugUnitTest`, **una compilación a la vez**.
7. Nunca loguear tokens, contraseñas ni el celular del cliente.

### 1.2 · Piezas comunes: hazlas primero

| App | Pieza | Dónde está descrita | La usan |
|---|---|---|---|
| Discovery | Leer el cuerpo `problem+json` de los errores HTTP (hoy `NetworkResult.HttpError` guarda solo el status) | §2.2, F-30, paso 1 | F-30, F-32, F-33, F-44 y F-101 (`DEVICE_UNLINKED`) |
| Staff | **P-1** · Leer `errorCode` y `detail` del ProblemDetail en cualquier servicio | §3.1, P-1 | F-58, F-60, F-62, F-48, F-93, F-83, F-85, F-87 |
| Staff | **P-2** · Dispositivos se refresca sola mientras está visible | §3.1, P-2 | F-65, F-09, F-61, F-01 |
| Staff | Señal compartida "los pedidos cambiaron" (`ordersChanged`) | §3.2, F-80 | F-54, F-90 |

> La "P-1" de §3.3 (llamadas de producto) es **la misma pieza** que la P-1 de §3.1 aplicada a
> `ProductRepositoryImpl`: constrúyela una sola vez, en `core/network`, y úsala en los dos lugares.

### 1.3 · Decisiones que no son del front (no las inventes)

Para estos puntos el documento deja el cambio listo y marcado. Implementa lo que dice "Debe pasar ahora"
salvo que producto diga otra cosa:

| Clave | Decisión pendiente | Qué hace el documento |
|---|---|---|
| F-13 | Bloqueo del PIN: 30 s (lo que pide QA) o 24 h (lo que eligió la ola 1) | Aplica 30 s, con la constante aislada |
| F-07 | Pedir el permiso del micrófono al llegar a Home (ola 3) o al tocar el micrófono (QA) | Aplica lo de QA |
| F-69 | Número y horario de WhatsApp de soporte | Marcador `<<DECISIÓN PENDIENTE: …>>` |
| F-71 | Qué hace "Atrás" en una pestaña raíz | Marcador con opciones A y B |
| F-55 | Ícono de la app y de las notificaciones (no hay logo en los repos) | Marcador `<<DEPENDE DE DISEÑO: …>>` |
| F-26 | Si la tablet debe mostrar el vencimiento a los 60 min (la pantalla de éxito se cierra mucho antes) | Solo confirmado/rechazado mientras la pantalla está abierta |
| F-32 | Si un producto `LOW_STOCK` se vende desde la tablet (la API dice que no; el plan de QA pedía venderlo) | Sigue la API |

### 1.4 · Orden de trabajo

**Discovery (tablet):**

| # | Gravedad | Clave | Qué falla | Esfuerzo | Sección |
|---|---|---|---|---|---|
| 0 | — | pieza | Leer `problem+json` | S | §2.2 F-30 paso 1 |
| 1 | CRÍTICO | F-101 | Ignora 401 `DEVICE_UNLINKED`; sigue fijada en Home | M | §2.1 |
| 2 | CRÍTICO | F-30 | Muestra S/ 210 y graba S/ 199: sin `expectedUnitPrice` ni manejo de 409 `PRICE_CHANGED` | L | §2.2 |
| 3 | ALTO | F-26 | No consulta `GET /orders/{id}/status`: no se entera del rechazo | M | §2.2 |
| 4 | ALTO | F-44 | Sin red, "Algo salió mal" genérico | S | §2.2 |
| 5 | ALTO | F-37 | Los chips concatenan el texto; no manda `filters` | M | §2.2 |
| 6 | ALTO | F-13 | PIN bloqueado 24 h en vez de 30 s; sin `POST /devices/logs` | M | §2.1 |
| 7 | ALTO | F-12 | No arranca tras reiniciar: sin receiver de arranque ni filtro HOME | S (+ operación) | §2.1 |
| 8 | MEDIO | F-02 | Tras force-stop no cancela el código viejo | S | §2.1 |
| 9 | MEDIO | F-05 | El canje no guarda `storeName`; títulos de Ajustes fijos | S | §2.1 |
| 10 | MEDIO | F-10 | Tras NO THANKS no vuelve a pedir la fijación | M | §2.1 |
| 11 | MEDIO | F-38 | "Volver" en Búsqueda desde el Catálogo lleva a Home | S | §2.1 |
| 12 | MEDIO | F-17 | Error de voz sin red se descarta en silencio | S | §2.1 |
| 13 | MEDIO | F-32 | Sin tope de stock; un eliminado con el Detalle abierto entra al carrito | M | §2.2 |
| 14 | MEDIO | F-59 | Vincular no muestra el nombre de la tablet | S | §2.1 |
| 15 | BAJO | F-33 | Nombre de más de 200 caracteres; 422 genérico | S | §2.2 |
| 16 | BAJO | F-29 | El éxito no muestra el total grabado | S | §2.2 |
| 17 | BAJO | F-34 | Compartir sin red no dice que falta internet | S | §2.2 |
| 18 | BAJO | F-35 | El carrito quieto se cierra antes de 3 min y deja el teclado | S | §2.2 |
| 19 | BAJO | F-07 | Pide el permiso del micrófono al llegar a Home | S | §2.1 |
| 20 | BAJO | F-24 | Latido cada 60 s fijos, sin reintento al volver la red | S | §2.1 |
| 21 | BAJO | F-63 | El primer latido manda `uptimeSeconds: 0` | S | §2.1 |
| 22 | BAJO | F-43 | Sin foto: tres miniaturas grises, sin "Sin foto" | S | §2.1 |
| — | bloqueado | F-20 | HTTP plano (los logs ya están bien); espera dominio + TLS | S | §2.2 |
| — | hecho | F-01, F-09, F-27 | Corregidos en la tablet; lo que falta es del celular o de Meta | — | §2.1, §2.2 |

**Staff (celular):**

| # | Gravedad | Clave | Qué falla | Esfuerzo | Sección |
|---|---|---|---|---|---|
| 0 | — | P-1, P-2 | ProblemDetail y refresco de Dispositivos | S/M | §3.1 |
| 1 | CRÍTICO | F-86 | Al corregir el precio, **borra el descuento en el servidor** (Registro y Detalle) | M | §3.3 |
| 2 | CRÍTICO | F-93 | `PUT` sin `version`: pisa al otro vendedor (incluye vaciar la descripción y F-81) | L | §3.3 |
| 3 | ALTO | F-80 | Solicitudes no se recarga con el push ni al volver | L | §3.2 |
| 4 | ALTO | F-79 | Inventario no recarga al volver ni tirando hacia abajo | S | §3.3 |
| 5 | ALTO | F-58 | Código de vinculación sin normalizar ni validar; un solo mensaje de error | M | §3.1 |
| 6 | ALTO | F-60 | Tras aprobar no espera el canje; sin aviso ni "Rechazar" | L | §3.1 |
| 7 | ALTO | F-65 | Dispositivos y Ajustes de la tablet con datos viejos | M | §3.1 |
| 8 | ALTO | F-85 | No manda `Idempotency-Key`; Atrás cancela el alta | M | §3.3 |
| 9 | ALTO | F-96 | Guardado a medias si se corta la red; descuento no atómico | M | §3.3 |
| 10 | ALTO | F-83 | 409 por pedidos pendientes da error genérico; sin `force` | M | §3.3 |
| 11 | MEDIO | F-01 | Tarjeta "Inactivo" tras vincular, sin aviso | S | §3.1 |
| 12 | MEDIO | F-09 | "Listo" con `ACKED` aunque la tablet no vuelva | M | §3.1 |
| 13 | MEDIO | F-61 | Comando vencido sigue "Enviado" y desaparece | S | §3.1 |
| 14 | MEDIO | F-62 | Unlink 404 da "Inténtalo de nuevo" | S | §3.1 |
| 15 | MEDIO | F-59 | No se puede nombrar ni renombrar la tablet | M | §3.1 |
| 16 | MEDIO | F-48 | 401 de contraseña incorrecta dispara refresh y reintento | S | §3.1 |
| 17 | MEDIO | F-52 | Con huella, la notificación abre Inicio | S | §3.1 |
| 18 | MEDIO | F-73 | Sesiones sin agrupar ni "Cerrar todas" | M | §3.1 |
| 19 | MEDIO | F-54 | El grupo de notificaciones no abre Solicitudes | M | §3.2 |
| 20 | MEDIO | F-68 | "2 min" y 5 alertas sin emisor mostradas como activas | M | §3.2 |
| 21 | MEDIO | F-56 | "Agregar" y "Reabastecer" de Inicio no hacen nada | M | §3.2 |
| 22 | MEDIO | F-89 | "Compartir" en Resumen de ventas no hace nada | M | §3.2 |
| 23 | MEDIO | F-87 | Producto borrado: avisa pero se queda en el formulario | S | §3.3 |
| 24 | MEDIO | F-81 | Producto "a consultar" no deja guardar sin stock | S | §3.3 |
| 25 | MEDIO | F-82 | Montos en notación científica; coma o punto según la pantalla | M | §3.3 |
| 26 | MEDIO | F-97 | Borrar con cambios sin guardar: nombre editado, sin preguntar | S | §3.3 |
| 27 | MEDIO | F-64 | Doble "Volver" en Resumen de ventas deja la pantalla en blanco | S | §3.3 |
| 28 | MEDIO | F-94 | "Catálogo visible" usa `activeProducts` y no `visibleProducts` | S | §3.3 |
| 29 | MEDIO | F-100 | Ranking sin "S/ X de S/ Y" ni "Eliminado" | S | §3.3 |
| 30 | MEDIO | F-72 | `allowBackup="true"` | S | §3.3 |
| 31 | MEDIO | F-20 | Cuerpos con secretos en el log de debug; HTTPS bloqueado | S | §3.3 |
| 32 | BAJO | F-90 | Confirmado sin red sigue en Pendientes (necesita F-80) | S | §3.2 |
| 33 | BAJO | F-91 | Error sin red sale detrás del velo del diálogo | S | §3.2 |
| 34 | BAJO | F-53 | Huella borrada del sistema: entra sin pedir nada | S | §3.1 |
| 35 | BAJO | F-63 | "ETHERNET" en crudo con ícono de Wi-Fi | S | §3.1 |
| 36 | BAJO | F-49 | Token push sin sesión (401) y sin reintento | M | §3.1 |
| 37 | BAJO | F-50 | La ✓ del teclado no inicia sesión | S | §3.1 |
| 38 | BAJO | F-51 | Ojo de la contraseña en inglés | S | §3.1 |
| 39 | BAJO | F-70 | Sin "¿Descartar cambios?" en Perfil | M | §3.1 |
| 40 | BAJO | F-67 | No se puede vaciar el teléfono guardado | S | §3.1 |
| 41 | BAJO | F-95 | El pie dice "sin ventas"; no se refresca | S | §3.3 |
| 42 | BAJO | F-78 | El registro acepta 11 fotos | S | §3.3 |
| 43 | BAJO | F-57 | Campana y pestaña con badge sin rol de botón | S | §3.2 |
| 44 | BAJO | F-75 | `GET size=1` doble; Perfil recarga de más | S | §3.2 |
| 45 | BAJO | F-76 | Hora en 24 h con el sistema en 12 h | S | §3.2 |
| 46 | BAJO | F-55 | Ícono del robot de Android (falta el logo) | S | §3.2 |
| 47 | BAJO | F-66 | Detalles visuales en Dispositivos | S | §3.3 |
| 48 | BAJO | F-84 | Contraste en modo oscuro; cortes con fuente 2.0 | M | §3.3 |
| 49 | BAJO | F-92 | Accesibilidad: campos sin etiqueta, miniaturas, chips | M | §3.3 |
| — | bloqueado | F-69, F-71 | Decisión de negocio (sección 1.3) | S | §3.2 |

### 1.5 · Lo que hace el backend (no es tuyo; no lo esperes salvo donde se diga)

- **F-59, "Tablet N" repetido:** el servidor calcula N = tablets activas + 1 y repite nombres tras un
  unlink. Se corrige en el backend (primer N libre). La app igual debe mandar un nombre al vincular y
  permitir renombrar con `PATCH /devices/{id}` (§3.1).
- **No implementado, y la app no lo necesita para avanzar:** aviso al dueño por `SETTINGS_PIN_LOCKOUT`
  (F-13; la tablet manda el log igual), 409 al acusar un comando vencido (F-09), nombre por heartbeat
  (F-59), emisores de 5 preferencias (F-68, llegan con `supported=false`: la app debe dejar de prometerlas).
- **Bloqueado por infraestructura:** HTTPS (F-20, falta dominio y TLS) y la entrega real de WhatsApp
  (F-27, Meta responde subcode 33).
- **Mejoras opcionales que el backend puede agregar después** (el documento da cambios que funcionan sin ellas):
  validar que un descuento fijo no supere el precio (hoy el producto quedaría a S/ 0), reemplazar un
  descuento en un solo paso, `GET /products?ids=…` para revalidar el carrito, `Idempotency-Key` en
  `POST /orders`, y documentar si cambiar fotos o descuentos sube la `version` del producto.

### 1.6 · Dónde QA y el código no coinciden

Estos veredictos no reflejan del todo el código. Al verificar, ten esto en cuenta:

- **F-33, caso CAR-024:** "25 productos guardados" cuenta unidades, no líneas; el límite de 20 líneas ya
  existe (`AddToCartUseCase.kt:40`). Hay que repetir el caso contando líneas distintas.
- **F-24:** QA midió 8 s por suerte; el bucle espera siempre 60 s fijos.
- **F-10, caso EXP-007:** no puede pasar sin device owner (Android exige tocar GOT IT). Lo que sí falla es
  que tras NO THANKS solo se vuelve a pedir la fijación si alguien toca Home.
- **F-49:** QA lo dio PARCIAL, pero solo re-ejecutó un caso que ya pasaba. `pm clear` → 401 y la falta de
  reintento al volver la red siguen igual.
- **F-58:** un código en minúsculas vincula porque el backend normaliza, no por la app.
- **F-92:** parte puede ser un artefacto de `uiautomator` (el switch sí declara su rol). Confirmar con TalkBack.
- **Métricas infladas (tablet):** `RevalidateCartUseCase` llama `GET /products/{id}` con el token de la
  tablet, y cada llamada suma una visita `PRODUCT_VIEW` falsa. §2.2 propone usar `GET /products`.

### 1.7 · Cómo se verifica al terminar

Cada arreglo dice qué caso de QA debe pasar. Cuando una app tenga versión nueva, QA corre la regresión
desde `qa/regresion/` con `/qa <bloque>` (R01…R12) o `/qa all`. Avisa la versión (`versionName` y commit)
para que quede en el reporte.

---

## 2 · Discovery (tablet)

Repo `android/qhathia-mobile-discovery`, commit `490694e`.

### 2.1 · Vinculación, kiosko, voz, ajustes y navegación (R01–R05)

**Versión probada:** Discovery 1.23.0-SNAPSHOT, repo `android/qhathia-mobile-discovery` en el commit `490694e`. Backend en master `a2da54b` (fases 35–43, en producción). Corrida QA del 03/11.

**Convenciones**
- Las rutas son relativas al repo de la app. `‹pkg›` = `src/main/java/com/qhathia/mobile/discovery`. Ejemplo: `core/data/‹pkg›/core/data/session/StoreSessionLocalDataSource.kt`.
- Reportes de QA: `qa/regresion/qa-reports/2026-11-03-Rnn.md`. Planes con "Debe pasar ahora": `qa/regresion/qa-plan/Rnn-*.md`. Contrato: `docs/api/API_REFERENCE.md` §35–§43 (repo backend `D:\Vitria`).
- **Qué ya tiene la app.** El repo ya trae las olas 1–3 de QA: `openspec/changes/archive/2026-10-30-qa-wave-{1,2,3}-*`, `…kiosk-lock-mode`, `…device-remote-commands`, `…device-heartbeat`. La app **no** trae nada de lo que el backend agregó en las fases 35–42: no lee `DEVICE_UNLINKED`, no manda `replacesSessionId`, no llama a `/cancel` ni a `/status`, no lee `storeName` del canje y no llama a `POST /devices/logs`. El `docs/handoff/android-pendientes.md` del repo de la app es anterior a esa campaña: no tiene la sección "Campaña QA 25–28/10".
- **Dos reglas de la app chocan con "Debe pasar ahora"**: el bloqueo del PIN de 24 h (F-13) y el permiso del micrófono que se pide al llegar a Home (F-07). Las dos se eligieron a propósito en las olas 1 y 3. La regresión manda el comportamiento de QA. Si producto quiere mantener la regla actual, tiene que decirlo y corregir el plan de QA.
- Patrones del repo que hay que respetar: Hilt (`@Inject constructor`, módulos `@Binds` en `core/data/‹pkg›/core/data/di/`), Ktor con `runCatching { … }.toNetworkResult()`, puertos en `core/domain/repository`, use cases `operator fun invoke`, ViewModel con `UiState`/`UiEvent`/`UiEffect`, DataStore Preferences (`store_session`), tests con fakes en `core/testing`.

Orden: de más grave a menos grave. Al final van F-01 y F-09, que ya están corregidos en la tablet.

---

##### F-101 · La tablet desvinculada sigue fijada en Home y no se entera — CRÍTICO · NO CORREGIDO

**Qué ve QA hoy (03/11):** X:XVI-040. [C] Perfil → Dispositivos → "Tablet 3" → "Desvincular tablet" → "Desvincular" (06:52:45, `POST /devices/{id}/unlink 200`). Durante 144 s la tablet recibe `401 DEVICE_UNLINKED` en cada latido (06:53:20, 06:54:20, 06:55:20). En logcat aparece `KtorClient: RESPONSE: 401 {"type":"/errors/device-unlinked",…}` cada 60 s. Aun así sigue en Home con `mLockTaskModeState=PINNED`, y "Ir al catálogo" muestra "Perdí la conexión… Parece que la red está inestable" + "Reintentar". **Esperado:** en ≤ 2 latidos, Welcome con aviso, sin fijar y sin token. El backend sí corta bien: biz y vo dan 401 a los 26 s. (`2026-11-03-R01.md`, sección F-101.)

**Qué ya está hecho:**
- Si se borra el token, la app ya reacciona sola. `MainViewModel.startDestination` (`app/‹pkg›/MainViewModel.kt:87-92`) pasa a `Onboarding` y `DiscoveryAppContent` (`app/‹pkg›/MainActivity.kt:193-197`) monta el NavHost con Welcome.
- `ObserveKioskLockUseCase` (`core/domain/‹pkg›/core/domain/usecase/ObserveKioskLockUseCase.kt:27`, `active = session.isPaired && …`) apaga el kiosko, y `applyKioskLock(false)` (`MainActivity.kt:129-136`) hace `stopLockTask()`.
- `RunDeviceHeartbeatUseCase` (`…/usecase/RunDeviceHeartbeatUseCase.kt:30-35`, `collectLatest` sobre `isPaired`) corta el latido.
- `clearDeviceSession()` ya existe (`core/data/‹pkg›/core/data/session/StoreSessionLocalDataSource.kt:46-52`).
- **Lo que falta: nadie lo llama.**

**Causa en el código:**
- `core/data/‹pkg›/core/data/heartbeat/DeviceHeartbeatRepositoryImpl.kt:53`:
  `is NetworkResult.HttpError -> Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))`
  Después `RunDeviceHeartbeatUseCase.kt:45-46` lo descarta: `catch (_: Exception) { // Ignored by design`.
- `core/network/‹pkg›/core/network/NetworkResult.kt:21`: `data class HttpError(val code: Int, val message: String)`. Ahí se pierde el `errorCode` del ProblemDetail: `toNetworkResult()` (`:30-41`) solo lee el status.
- Catálogo, detalle, búsqueda, pedido y compartir reintentan el 401 con el mismo token: `sessionTokenProvider.clear()` es un no-op (`core/data/‹pkg›/core/data/stt/SessionTokenProvider.kt:41`, `fun clear() = Unit`). Ejemplo: `core/data/‹pkg›/core/data/catalog/ProductCatalogRepositoryImpl.kt:32-34`. Al final cada pantalla muestra su error genérico.
- `clearDeviceSession()` no borra `store_name` (`StoreSessionLocalDataSource.kt:47-51`). Si la tablet se re-vincula a otra tienda, mostraría el nombre viejo.
- `DeviceSettingsLocalDataSource` no tiene `clear()`: el hash del PIN y el `kioskMode` de la tienda anterior quedarían guardados.

**Qué garantiza el backend:** §35. Una tablet desvinculada recibe en **cualquier** llamada con su token `HTTP 401`, `application/problem+json`, `{"type":"/errors/device-unlinked","status":401,"errorCode":"DEVICE_UNLINKED","detail":"Esta tablet fue desvinculada de la tienda…"}`. En auth es inmediato. En biz y vo tarda ≤ 60 s, porque cachean `/devices/me`. Pedir un código nuevo (`POST /devices/pairing-token`) sigue funcionando aunque se mande el token viejo. Un 403 `FORBIDDEN` (por ejemplo `POST /products` con token DEVICE) **no** es desvinculación. Desde la Fase 42, 401 y 403 de filtro también son problem+json con `errorCode`.

**Cambio:**
1. **Detectar el 401 en un solo lugar (core:network).** Crear `core/network/‹pkg›/core/network/DeviceUnlinkedSignal.kt`:
   ```kotlin
   @Singleton
   class DeviceUnlinkedSignal @Inject constructor() {
       private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
       val events: SharedFlow<Unit> = _events.asSharedFlow()
       fun notifyUnlinked() { _events.tryEmit(Unit) }
   }
   ```
   Mover `readErrorCode` de `DevicePairingApiService.kt:61-64` a un helper público en `core/network`: `suspend fun HttpResponse.problemErrorCode(): String?`, que lee `errorCode` o `code`. En `KtorClientFactory.configure` (`core/network/‹pkg›/core/network/KtorClientFactory.kt:38`) agregar un parámetro `onDeviceUnlinked: () -> Unit = {}` e instalar:
   ```kotlin
   HttpResponseValidator {
       handleResponseExceptionWithRequest { cause, _ ->
           val response = (cause as? ClientRequestException)?.response ?: return@handleResponseExceptionWithRequest
           if (response.status == HttpStatusCode.Unauthorized && response.problemErrorCode() == "DEVICE_UNLINKED") {
               onDeviceUnlinked()
           }
       }
   }
   ```
   El handler no se traga la excepción: cada repositorio sigue recibiendo su `HttpError` como hoy. En `NetworkModule.provideHttpClient(signal: DeviceUnlinkedSignal)` pasar `onDeviceUnlinked = signal::notifyUnlinked`. Comprobar con un test de `MockEngine` que el body del error se puede leer dos veces: Ktor 3 lo guarda, y `DevicePairingApiService` ya lo vuelve a leer.
2. **Puerto y use case (core:domain).**
   - Puerto `DeviceUnlinkEvents { fun observe(): Flow<Unit> }`, implementado en core:data sobre `DeviceUnlinkedSignal.events` y bindeado en un módulo `di/` nuevo o en `DeviceHeartbeatDataModule`.
   - Use case `HandleDeviceUnlinkedUseCase`, que en cada evento hace:
   ```kotlin
   unlinkEvents.observe().collect {
       if (!storeSessionRepository.observe().first().isPaired) return@collect // ya revocada
       endCustomerSession()                         // carrito y formulario del cliente
       deviceSettingsRepository.clear()             // nuevo: PIN, kioskMode, versión, voz, brillo
       heldPairingCodeRepository.set(null)
       kioskSessionRepository.setExitedFromSettings(false)
       storeSessionRepository.revokeDeviceSession() // nuevo: ver paso 3
   }
   ```
3. **`StoreSessionLocalDataSource.revokeDeviceSession()`** (nuevo, más el método del puerto `StoreSessionRepository`): hace lo mismo que `clearDeviceSession()`, más `remove(KEY_STORE_NAME)` y `this[KEY_UNLINKED_NOTICE] = true` (nueva `booleanPreferencesKey("unlinked_notice")`). Exponer `unlinkedNotice: Boolean` en `StoreSession`. Agregar también `DeviceSettingsLocalDataSource.clear()`, que borra todas sus claves. El bloqueo de PIN se libera solo: `SettingsPinAttemptsRepositoryImpl` compara la huella del hash, y sin hash da `NONE`.
4. **Arranque:** en `DiscoveryApp.onCreate` (`app/‹pkg›/DiscoveryApp.kt:43-45`) agregar `@Inject lateinit var handleDeviceUnlinked: Lazy<HandleDeviceUnlinkedUseCase>` y `applicationScope.launch { handleDeviceUnlinked.get()() }`.
5. **Aviso en Welcome:** crear `WelcomeViewModel` (Hilt) en `features/onboarding`, que observa `ObserveStoreSessionUseCase`. Si `unlinkedNotice`, `WelcomeContent` muestra un banner: "Esta tablet se desvinculó de la tienda. Para volver a usarla, vincúlala de nuevo desde la app del comercio." Al tocar "Continuar", borrar la marca con un use case nuevo, `DismissUnlinkedNoticeUseCase`.
6. Limpieza opcional: quitar los reintentos de 401 con el mismo token en los 5 repositorios (`ProductCatalogRepositoryImpl.kt:32`, `ProductDetailRepositoryImpl.kt:34,55`, `ProductSearchRepositoryImpl.kt:34`, `OrderRepositoryImpl.kt:37`, `CartShareRepositoryImpl.kt:37`) y el parámetro `forceRefresh`. No es necesario para que pase QA.

**No hacer:**
- No borrar la sesión ante cualquier 401 o 403: un 403 `FORBIDDEN` de biz (`POST /products` con token DEVICE) o un 401 de Deepgram no son desvinculación. La regla es solo `errorCode == "DEVICE_UNLINKED"`.
- No implementar el "Desvincular" local de la tablet (`DevicePairingRepositoryImpl.kt:216-220`, stub): no está en el alcance.
- No hacer polling de `/devices/me`: el backend ya da el 401.

**Verificación:** X:XVI-040. Desvincular desde el celular. En ≤ 60 s (un latido) la tablet debe:
- mostrar Welcome con el aviso;
- tener `adb shell dumpsys activity activities | grep mLockTaskModeState` → `NONE`;
- no tener más `POST /devices/heartbeat` en nginx;
- no tener token: "Ajustes" no existe en Welcome, y "Continuar" genera un código.

X:XVI-041: los comandos al id viejo siguen en 404. Volver a vincular (XVI-001): Home sin el nombre de la tienda anterior y sin el PIN viejo. Tests: `HandleDeviceUnlinkedUseCaseTest` (evento → sesión vacía, idempotente) y un test de `KtorClientFactory` con `MockEngine` (401 `DEVICE_UNLINKED` → señal; 403 `FORBIDDEN` → sin señal).

---

##### F-13 · El PIN se bloquea 24 h, no 30 s, y no avisa — ALTO · PARCIAL

**Qué ve QA hoy (03/11):** D:AJU-011 / X:XCR-013. Se pone el PIN `1234` por API (llega a la tablet a las 07:13:16). En Ajustes, 5 × "9999" + ✓. Resultado: teclado deshabilitado y "Demasiados intentos. Podrás intentarlo de nuevo en 24:00 h"; a los 63 s, "23:59 h". Ningún `POST /devices/logs` en nginx. Con `removeExitPin` el bloqueo se levanta, pero el diálogo abierto sigue pidiendo PIN hasta cerrarlo. **Esperado:** bloqueo de 30 s con cuenta regresiva, que siga tras matar la app, y `POST /devices/logs 204` con `SETTINGS_PIN_LOCKOUT`. (`2026-11-03-R02.md`, sección F-13.)

**Qué ya está hecho:**
- Contador de intentos persistido en DataStore (`core/data/‹pkg›/core/data/settings/SettingsPinAttemptsRepositoryImpl.kt:70-72`, claves `settings_pin_failed_count`, `settings_pin_locked_at`, `settings_pin_fingerprint`). Sobrevive a matar la app.
- Bloqueo al 5.º intento (`RegisterPinFailureUseCase`).
- Teclado deshabilitado.
- Desbloqueo cuando el comerciante cambia o quita el PIN (huella del hash).
- Protección contra un reloj atrasado (`PinLockPolicy.normalize`).

Es lo que pidió la ola 1 de la app (`openspec/changes/archive/2026-10-30-qa-wave-1-critical-fixes/proposal.md`: "the settings PIN locks for 24 hours"). **Es una decisión de la app, no un bug, pero contradice el plan de QA.**

**Causa en el código:**
- `core/domain/‹pkg›/core/domain/SettingsConstants.kt:23`: `val PIN_LOCK_DURATION: Duration = 24.hours`, y `:26`: `val PIN_LOCK_REFRESH_INTERVAL: Duration = 1.minutes`.
- `features/settings/‹pkg›/feature/settings/ui/SettingsViewModel.kt:142-148`: `minutesLeft()` redondea a minutos. `SettingsPasswordDialogContent.kt:280-282` (`formatHoursMinutes`) y `features/settings/src/main/res/values/strings.xml:8` (`…en %1$s h`) pintan "HH:mm h".
- No hay ningún cliente de `POST /api/v1/devices/logs` (no hay `DeviceLog*` en `core/network`).
- `PinLockPolicy.kt:27`: `!now.isBefore(lockedAt.plus(lockDuration)) -> PinAttempts.NONE`. Al vencer el bloqueo vuelve a cero. Con 30 s fijos serían 5 intentos cada 30 s, demasiado débil: por eso el paso 1 hace el bloqueo progresivo.

**Qué garantiza el backend:** `POST /api/v1/devices/logs` (DEVICE). Body `{"logs":[{"level":"WARN","code":"SETTINGS_PIN_LOCKOUT","message":"…","timestamp":"<ISO-8601>","context":{…}}]}`. `level` ∈ `DEBUG|INFO|WARN|ERROR|FATAL`; `message` y `timestamp` son obligatorios; 1 a 100 entradas. Respuestas: `204`, `401` (token) y `422` (formato) (API_REFERENCE, "POST /api/v1/devices/logs"; `services/auth-api-service/.../dto/device/DeviceLogEntry.java`). **El aviso al dueño por `SETTINGS_PIN_LOCKOUT` NO está implementado** (`android-pendientes.md`, tabla "Donde el contrato real…"; plan R02: "Ojo"). Hoy el log solo queda guardado; el celular no se entera. Hay que enviarlo igual: QA verifica el 204.

**Cambio:**
1. **`SettingsConstants`:** reemplazar `PIN_LOCK_DURATION` por `PIN_LOCK_BASE = 30.seconds` y `PIN_LOCK_MAX = 15.minutes`, y poner `PIN_LOCK_REFRESH_INTERVAL = 1.seconds`.
2. **Bloqueo progresivo:** agregar `lockCount: Int = 0` a `PinAttempts` (`core/domain/‹pkg›/core/domain/model/PinAttempts.kt`) y persistirlo en `SettingsPinAttemptsRepositoryImpl` (nueva `intPreferencesKey("settings_pin_lock_count")`, borrada junto con las otras en `NONE`).
   - En `PinLockPolicy`: `fun lockDuration(count: Int) = minOf(PIN_LOCK_BASE * (1 shl (count - 1).coerceIn(0, 5)), PIN_LOCK_MAX)`. Da 30 s, 1, 2, 4, 8 y 15 min.
   - Al vencer, `normalize` devuelve `PinAttempts(failedCount = 0, lockedAt = null, lockCount = attempts.lockCount)`, no `NONE`.
   - `RegisterPinFailureUseCase`: al llegar a 5, `PinAttempts(failedCount = count, lockedAt = now, lockCount = current.lockCount + 1)`.
   - Un PIN correcto sigue llamando a `ResetPinAttemptsUseCase` (→ `NONE`).
3. **UI:**
   - `SettingsUiState.lockedMinutesLeft` pasa a `lockedSecondsLeft: Long?`.
   - En el VM, `PinLockState.Locked(remaining)` → `remaining.inWholeSeconds.coerceAtLeast(1)`.
   - En `SettingsPasswordDialogContent`: `String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)`.
   - String: `<string name="settings_password_locked">Demasiados intentos. Vuelve a intentarlo en %1$s</string>`.
   - Actualizar los previews (`:334-352`) y el KDoc que dice "24 hours".
4. **Log al backend:**
   - `core/network`: `DeviceLogApiService.send(token, DeviceLogsRequestDto)` → `POST /api/v1/devices/logs`. DTOs `DeviceLogsRequestDto(val logs: List<DeviceLogEntryDto>)` y `DeviceLogEntryDto(level, code, message, timestamp: String, context: Map<String, String>?)`.
   - `core/domain`: puerto `DeviceLogRepository { suspend fun reportPinLockout(failedAttempts: Int, lockSeconds: Long): Result<Unit> }`, con su implementación en `core/data` usando `storeSessionRepository.deviceAccessToken()`.
   - Use case `ReportPinLockoutUseCase`.
   - En `SettingsViewModel.submitPassword` (`:113`), si `registerPinFailure()` devuelve `Locked` y antes no lo estaba: `viewModelScope.launch { reportPinLockout(...) }`. Fire-and-forget: un fallo no cambia la UI.
   - Body: `level "WARN"`, `code "SETTINGS_PIN_LOCKOUT"`, `message "Varios intentos de PIN fallidos en la tablet"`, `context {failedAttempts, lockoutSeconds}`.
   - Si no hay red: opcional, guardar uno pendiente en DataStore y reenviarlo tras el siguiente latido correcto.
5. Opcional (lo que vio QA con `removeExitPin`): en `followLock()` (`SettingsViewModel.kt:86-91`), observar también si el PIN sigue siendo obligatorio. Si deja de serlo con el diálogo abierto, emitir `SettingsUiEffect.Unlocked`.

**No hacer:**
- No guardar el contador solo en el ViewModel: se pierde al recrear el proceso. Ya está bien en DataStore; no moverlo.
- No mandar el PIN tecleado ni el hash en el log.

**Verificación:**
- D:AJU-011 / X:XCR-013: 5 PIN incorrectos → "Demasiados intentos. Vuelve a intentarlo en 0:30" con la cuenta bajando de a 1 s.
- Un 6.º intento durante el bloqueo no hace nada (teclado deshabilitado).
- `am force-stop` + reabrir + engranaje → sigue bloqueado con el tiempo restante.
- A los 30 s se habilita. Otros 5 fallos → 1:00.
- En nginx, `POST /api/v1/devices/logs 204` al empezar cada bloqueo.
- Tests: `PinLockPolicyTest` (30 s, 60 s, tope de 15 min, reloj atrasado) y `SettingsViewModelTest` (formato m:ss, un solo log por bloqueo).

---

##### F-12 · Tras reiniciar la tablet la vitrina no arranca — ALTO · NO CORREGIDO (device owner/launcher: BLOQUEADO por operación)

**Qué ve QA hoy (03/11):** X:XRE-033. `adb reboot` a las 09:12:12. Último latido 09:11:53 y ninguno en 7 min: la vitrina queda en el launcher de Android. Después del reinicio el emulador quedó `unauthorized` para adb (hay que aceptar la depuración USB en la pantalla). **Esperado:** al terminar el arranque, Home de la vitrina al frente y fijada, con el primer latido en < 30 s. (`2026-11-03-R02.md`, "No ejecutados", y `qa-plan/99-matriz.md`, fila F-12.)

**Qué ya está hecho:** nada en el código. `docs/android/tablet-kiosk-setup.md:4` dice explícitamente "la app no es launcher".

**Causa en el código:**
- `app/src/main/AndroidManifest.xml:45-49`: el único `intent-filter` es `MAIN` + `LAUNCHER`. No hay `CATEGORY_HOME`, ni `RECEIVE_BOOT_COMPLETED`, ni `<receiver>`.
- `app/‹pkg›/DiscoveryApp.kt:40-46`: latido, comandos y ajustes arrancan en `Application.onCreate`, o sea, solo cuando alguien abre la app.

**Qué garantiza el backend:** nada específico. Una tablet sin latidos pasa a "Inactivo" en el celular a los 10 min (§18, "Conectada" = ping < 10 min).

**Cambio:**
1. **Receiver de arranque** (funciona en el emulador de QA, API 26: el bloqueo de abrir Activities desde segundo plano llegó en API 29). Archivo nuevo `app/‹pkg›/kiosk/BootCompletedReceiver.kt`:
   ```kotlin
   @AndroidEntryPoint
   class BootCompletedReceiver : BroadcastReceiver() {
       @Inject lateinit var storeSessionRepository: StoreSessionRepository
       override fun onReceive(context: Context, intent: Intent) {
           if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
           val pending = goAsync()
           CoroutineScope(Dispatchers.IO).launch {
               try {
                   if (storeSessionRepository.observe().first().isPaired) {
                       context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                   }
               } finally { pending.finish() }
           }
       }
   }
   ```
   En el manifest: `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>` y `<receiver android:name=".kiosk.BootCompletedReceiver" android:exported="false"><intent-filter><action android:name="android.intent.action.BOOT_COMPLETED"/></intent-filter></receiver>`. Al abrirse `MainActivity`, `ApplyKioskLock` ya pide la fijación (`MainActivity.kt:228-235`). El proceso arranca, así que `DiscoveryApp` empieza a latir.
2. **La app como pantalla de inicio** (camino válido en API 29+ sin device owner): agregar a `MainActivity` un segundo `<intent-filter>` con `MAIN` + `HOME` + `DEFAULT`. En cada tablet de campo se elige una vez "Qhathia Discovery → Siempre" (operación). Con device owner (ver F-10, paso 3) se fija por código: `dpm.addPersistentPreferredActivity(admin, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME); addCategory(Intent.CATEGORY_DEFAULT) }, ComponentName(this, MainActivity::class.java))`.
3. Documentar en `docs/android/tablet-kiosk-setup.md`: elegir la app como inicio y, si la tablet tiene PIN de bloqueo, que alguien la desbloquee una vez tras reiniciar. `BOOT_COMPLETED` llega después del primer desbloqueo, y DataStore vive en el almacenamiento cifrado por credencial.

**No hacer:**
- No poner `android:launchMode="singleTask"` sin volver a probar `RESTART_APP`: el trampolín `RestartActivity` (`AndroidManifest.xml:54-61`, proceso `:restart`) relanza `MainActivity` dentro de la tarea fijada (F-09 está corregido y no hay que romperlo).
- No arrancar el latido desde el receiver sin abrir la Activity: la tablet diría "Activo" con la pantalla en el launcher.

**Verificación:** X:XRE-033. `adb reboot` → aceptar la depuración USB en el emulador → Home de la vitrina al frente (`dumpsys activity activities | grep mResumedActivity` → `com.qhathia.mobile.discovery/.MainActivity`), diálogo de fijación (o fijada directo con device owner), y en nginx un `POST /devices/heartbeat` antes de 30 s tras el arranque. La parte "sin diálogo" queda BLOQUEADA hasta provisionar device owner.

---

##### F-02 · El código abandonado al matar la app sigue aprobable (tablet huérfana) — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):** D:VIN-022. [T] código A (WM7AJ) → `am force-stop` → reabrir → "Continuar" → código B (HH9M7). Luego, por curl, aprobar `wm7aj` → `200 APPROVED` y nace la tablet huérfana `a547ee33`. **Esperado:** 400 `PAIRING_TOKEN_CANCELLED` o `EXPIRED`, sin device nuevo. **Ya funciona:** Volver + Continuar reutiliza el código (cuenta en 4:34); rotar no genera un `POST /pairing-token` (VIN-030); el backend con `replacesSessionId` da `CANCELLED` → 400 `PAIRING_TOKEN_CANCELLED`. (`2026-11-03-R01.md`, sección F-02.)

**Qué ya está hecho:**
- Código retenido fuera del VM: `HeldPairingCodeRepositoryImpl`, `PairingViewModel.resumeOrStartPairing` (`features/pairing/‹pkg›/feature/pairing/ui/PairingViewModel.kt:139-157`).
- Orientación fija en horizontal (manifest `:41`).
- Toques rápidos ignorados (`onGenerateCode`, `:134-137`).

**Causa en el código:**
- `core/data/‹pkg›/core/data/pairing/HeldPairingCodeRepositoryImpl.kt:16`: `private var held: HeldPairingCode? = null`. Vive solo en memoria: "Process-scoped on purpose". Tras un force-stop, la app no sabe qué sesión abandonó.
- `core/network/‹pkg›/core/network/service/DevicePairingApiService.kt:36`: `client.post("${BuildConfig.QHATHIA_BASE_URL}$PAIRING_TOKEN_PATH")` no manda body. Nunca envía `replacesSessionId`.
- No existe ninguna llamada a `POST …/pairing-session/{id}/cancel`.

**Qué garantiza el backend:** §37, "Códigos abandonados".
- `POST /api/v1/devices/pairing-token` acepta un body opcional `{"replacesSessionId":"<uuid>"}`. Cancela la sesión anterior si estaba `WAITING` y es tolerante: un id desconocido o ya resuelto no da error.
- `POST /api/v1/devices/pairing-session/{sessionId}/cancel` es público e idempotente: `204`, y `404` si no existe. El WS de esa sesión recibe `{"status":"CANCELLED"}`.
- Aprobar un código cancelado → `400 PAIRING_TOKEN_CANCELLED`; canjearlo → `410`.

**Cambio:**
1. **Persistir la sesión pendiente:** en `StoreSessionLocalDataSource` (o en una clase nueva `PendingPairingSessionLocalDataSource` sobre el mismo DataStore), nueva clave `stringPreferencesKey("pending_pairing_session_id")`, con `get()`, `set(id)` y `clear()`.
2. **`DevicePairingApiService`:**
   ```kotlin
   @Serializable data class GeneratePairingTokenRequestDto(val replacesSessionId: String? = null)

   suspend fun generatePairingToken(replacesSessionId: String?): NetworkResult<PairingTokenResponseDto> =
       runCatching {
           client.post("${BuildConfig.QHATHIA_BASE_URL}$PAIRING_TOKEN_PATH") {
               if (replacesSessionId != null) {
                   contentType(ContentType.Application.Json)
                   setBody(GeneratePairingTokenRequestDto(replacesSessionId))
               }
           }.body<PairingTokenResponseDto>()
       }.toNetworkResult()

   suspend fun cancelPairingSession(sessionId: String): NetworkResult<Unit> =
       runCatching { client.post("${BuildConfig.QHATHIA_BASE_URL}$PAIRING_SESSION_PATH/$sessionId/cancel"); Unit }
           .toNetworkResult()
   ```
3. **`DevicePairingRepositoryImpl.generateCode()`** (`:67-76`):
   - Leer `pending = pendingSession.get()`.
   - Llamar a `apiService.generatePairingToken(replacesSessionId = pending)`.
   - Si tiene éxito, `pendingSession.set(code.sessionId)`.
   - En `exchangeWithRetry`, cuando el canje sale bien (`:176-183`), `pendingSession.clear()`.

   Así cubre "Generar nuevo código", el vencimiento de la cuenta y el caso de reabrir tras force-stop.
4. **Cerrar la ventana entre reabrir y "Continuar":**
   - Nuevo `DevicePairingRepository.cancelAbandonedSession()`: si no está vinculada y hay `pending`, `cancelPairingSession(pending)` best-effort; luego `clear()` con 204 o 404.
   - Use case `CancelAbandonedPairingUseCase`, lanzado en `DiscoveryApp.onCreate` (`applicationScope.launch { … }`).
   - Es seguro: al arrancar un proceso nuevo `HeldPairingCodeRepositoryImpl` está vacío, así que ese código ya no se iba a reanudar.
5. Mapear `"CANCELLED"` en `PairingStatusMessageDto.toDomainOrNull()` (`core/data/‹pkg›/core/data/pairing/PairingMappers.kt:53-59`) a `PairingStatus.Expired`. Así, si otra instancia cancela el código en pantalla, la tablet pide uno nuevo en vez de quedarse mostrando uno muerto. Hoy cae en `else -> null` y se ignora.

**No hacer:**
- No cancelar desde `PairingViewModel.stop()` ni al tocar "Volver": rompería el reuso del código, que QA ya aprobó (Volver + Continuar con la misma cuenta).
- No usar `DELETE`: el contrato real es `POST …/cancel`.

**Límite conocido (no es de la app):** si el comerciante aprobó A antes de que la tablet lo reemplazara, `cancel` no lo deshace (solo cancela `WAITING`). Eso lo cubre el aviso del celular (F-60).

**Verificación:** D:VIN-022 / X:XVI-011.
- Código A → `am force-stop` → reabrir. En nginx, `POST /pairing-session/{A}/cancel 204` al abrir.
- "Continuar" → código B (con `{"replacesSessionId":"A"}` en logcat `KtorClient`).
- Aprobar A por curl → `400 PAIRING_TOKEN_CANCELLED`, y `GET /devices` sin device nuevo.
- Repetir VIN-030 y Volver + Continuar: mismo código, sin `POST /pairing-token` extra.
- Test en `DevicePairingRepositoryImplTest`: el segundo `generateCode` manda el `sessionId` del primero.

---

##### F-05 · "TIENDA" en Home justo después de vincular y en el título de Ajustes — MEDIO · PARCIAL

**Qué ve QA hoy (03/11):** D:VIN-003 / X:XVI-003. [C] vincula (06:59:13) → [T] Home "TIENDA" y título de Ajustes "TIENDA". Tras un force-stop, Home muestra "QA DISCOVERY — FERRETERÍA". AJU-030 ("Actividad") y AJU-032 ("Código usado" tras reinicio) **pasan**. (`2026-11-03-R01.md`, sección F-05.)

**Qué ya está hecho:**
- `DeviceInfoResponseDto.storeName` (`core/network/‹pkg›/core/network/dto/DevicePairingDtos.kt:47`).
- `getPairedDevice()` lo guarda en la sesión (`core/data/‹pkg›/core/data/pairing/DevicePairingRepositoryImpl.kt:204-205`), y Home lo lee (`features/home/‹pkg›/feature/home/ui/HomeViewModel.kt:65`).
- "Actividad" con `lastSeenAt` (`PairingContent.kt:399-401`).
- "Código usado" persistido (`StoreSessionLocalDataSource.kt:37-43`).

**Causa en el código:**
- `/devices/me` solo se llama al abrir Ajustes → Vincular (`PairingViewModel.loadPairedDevice`, `:119-128`). El canje no lee el nombre: `DevicePairingDtos.kt:22-30` (`DeviceTokenResponseDto`) no tiene `storeName`, y `exchangeWithRetry` (`DevicePairingRepositoryImpl.kt:178-182`) guarda token, nombre del device y código, pero no la tienda. Por eso, justo tras vincular, Home cae en `home_store_name_placeholder` (`features/home/‹pkg›/feature/home/ui/HomeContent.kt:144-145`).
- Título de Ajustes fijo: `features/settings/‹pkg›/feature/settings/ui/SettingsScreen.kt:56` (`PairingScreen(title = stringResource(R.string.network_toolbar_title))`), `NetworkSettingsContent.kt:144` (`network_toolbar_title` = "TIENDA", `strings.xml:19`) y `SettingsSideMenu.kt:90` (`network_store_title` = "Tienda", `strings.xml:16`).

**Qué garantiza el backend:** §37, "Otros". El canje `GET /api/v1/devices/pairing-session/{sessionId}/token` devuelve `storeName` y `storeCategory` (`PairDeviceResponse.java`). `GET /api/v1/devices/me` devuelve `storeName`, `storeCategory` y `shoppingCenterName` (null si está desvinculada).

**Cambio:**
1. `DeviceTokenResponseDto`: agregar `val storeName: String? = null`.
2. `DevicePairingRepositoryImpl.exchangeWithRetry` (`:176-183`): después de `saveDeviceSession(...)`, `dto.storeName?.takeIf { it.isNotBlank() }?.let { storeSessionRepository.updateStoreName(it) }`. También pasarlo al `PairedDevice`: `toPairedDevice` (`PairingMappers.kt:30-37`) recibe `storeName`.
3. Título de Ajustes:
   - Crear `SettingsHostViewModel` (Hilt, en `features/settings`) que exponga `storeName: StateFlow<String?>` desde `ObserveStoreSessionUseCase`.
   - En `SettingsScreen`, pasar `storeName?.uppercase() ?: stringResource(R.string.network_toolbar_title)` a `PairingScreen(title = …)` y al `Toolbar` de `NetworkSettingsContent` (nuevo parámetro `title: String`).
   - En `SettingsSideMenu`, mostrar `storeName ?: stringResource(R.string.network_store_title)`.

**No hacer:** no llamar a `/devices/me` en cada arranque solo para el nombre: el canje ya lo trae, y el valor queda persistido.

**Verificación:** D:VIN-003 / X:XVI-003: `pm clear` → vincular → Home muestra "QA DISCOVERY — FERRETERÍA" en ≤ 1 s, sin abrir Ajustes. Engranaje → título "QA DISCOVERY — FERRETERÍA" en Red Wifi y Vincular. AJU-030 y AJU-032 siguen pasando.

---

##### F-10 · La fijación depende del diálogo del sistema; tras NO THANKS no vuelve — MEDIO · NO CORREGIDO (device owner: BLOQUEADO)

**Qué ve QA hoy (03/11):**
- D:EXP-007: Ajustes → "Salir del modo kiosko" → Volver. A los 2 s, `mLockTaskModeState=NONE`: aparece "Screen is pinned", pero no queda fijada hasta GOT IT.
- D:EXP-002: tocar NO THANKS → `NONE` a los 5, 10, 15 y 20 s, y **el diálogo no vuelve**.
- X:XRE-030 (force-stop + reabrir → diálogo) pasa.
- Device owner: BLOQUEADO (no está provisionado en el emulador).
- **Esperado:** sin device owner, tras NO THANKS el diálogo vuelve en ≤ 15 s; con device owner, fijada sin diálogo.

(`2026-11-03-R02.md`, sección F-10.)

**Qué ya está hecho (ola 1, `kiosk-lock-mode`):**
- `requestPinning(throttled)` con un mínimo de 15 s entre pedidos (`app/‹pkg›/MainActivity.kt:144-153`, `PinningRequestThrottle.kt`).
- Se pide de nuevo al llegar a Home (`:296-299`) y en cada toque sobre Home (`:301`, `:350`).
- Las barras se ocultan al recuperar el foco (`:113-116`).

**Evidencia de que EXP-007 no puede pasar sin device owner:** con fijación de usuario, Android exige GOT IT. La app hace lo correcto: vuelve a pedir la fijación y el diálogo aparece, como vio QA. El criterio "≠ NONE en ≤ 2 s" solo se cumple con device owner.

**Causa en el código:**
- `MainActivity.kt:298-301`:
  ```kotlin
  LaunchedEffect(currentRoute, kioskActive) {
      if (kioskActive && currentRoute == HOME_ROUTE) activity?.requestPinning(throttled = true)
  }
  ```
  Después de NO THANKS solo se vuelve a pedir si alguien **toca** Home (`:350`). Sin toque, nunca. Además `requestPinning` traga cualquier error: `runCatching { startLockTask() }` (`:152`).
- `app/src/main/AndroidManifest.xml`: no hay `DeviceAdminReceiver`, así que la app no puede ser device owner ni llamar a `setLockTaskPackages`.

**Qué garantiza el backend:** nada (es solo de la app y de operación).

**Cambio:**
1. **Reintento sin toque:** reemplazar el `LaunchedEffect` de `MainActivity.kt:298-300` por:
   ```kotlin
   LaunchedEffect(currentRoute, kioskActive) {
       if (!kioskActive || currentRoute != HOME_ROUTE) return@LaunchedEffect
       while (isActive) {
           activity?.requestPinning(throttled = true)   // no hace nada si ya está fijada
           delay(DeviceConstants.PINNING_REQUEST_MIN_INTERVAL)
       }
   }
   ```
   El throttle de 15 s evita encadenar diálogos. Como `isPinned()` corta antes, una vez fijada no se pide nada.
2. `requestPinning`: cambiar `runCatching { startLockTask() }` por `try { startLockTask() } catch (e: Exception) { Log.w(TAG, "startLockTask rejected", e) }`. En API 26, `IllegalArgumentException("Invalid task, not in foreground")` hoy queda oculto.
3. **Device owner** (solución de fondo para las tablets de campo):
   - `app/‹pkg›/kiosk/KioskDeviceAdminReceiver.kt : DeviceAdminReceiver()`, más `res/xml/device_admin.xml` (`<device-admin><uses-policies/></device-admin>`).
   - En el manifest: `<receiver android:name=".kiosk.KioskDeviceAdminReceiver" android:permission="android.permission.BIND_DEVICE_ADMIN" android:exported="true"><meta-data android:name="android.app.device_admin" android:resource="@xml/device_admin"/><intent-filter><action android:name="android.app.action.DEVICE_ADMIN_ENABLED"/></intent-filter></receiver>`.
   - En `MainActivity.onCreate`:
   ```kotlin
   val dpm = getSystemService(DevicePolicyManager::class.java)
   if (dpm.isDeviceOwnerApp(packageName)) {
       val admin = ComponentName(this, KioskDeviceAdminReceiver::class.java)
       dpm.setLockTaskPackages(admin, arrayOf(packageName))
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
   }
   ```
   - Con esto, `startLockTask()` entra en `LOCK_TASK_MODE_LOCKED` sin diálogo, y `stopLockTask()` sigue sirviendo para "Salir del modo kiosko".
   - Provisión (operación, en un equipo sin cuentas Google): `adb shell dpm set-device-owner com.qhathia.mobile.discovery/.kiosk.KioskDeviceAdminReceiver`.
   - Documentarlo en `docs/android/tablet-kiosk-setup.md`.

**No hacer:**
- No reintentar en un bucle sin throttle: el cliente vería el diálogo sin parar.
- No pedir la fijación fuera de Home (Catálogo, Búsqueda): el plan solo lo pide en Home.

**Verificación:**
- D:EXP-002: tocar NO THANKS → sin tocar nada, el diálogo vuelve antes de 15 s → GOT IT → `PINNED`.
- D:EXP-007 / X:XRE-030: Salir → Volver → el diálogo aparece; queda `PINNED` tras GOT IT. Con device owner: `LOCKED` en ≤ 2 s y sin diálogo (BLOQUEADO hasta provisionar).
- X:XCR-002: kiosco ON desde el celular con la pantalla apagada → al encenderla, el diálogo o fijada.

---

##### F-38 · "Volver" en Búsqueda abierta desde el Catálogo lleva a Home — MEDIO · PARCIAL

**Qué ve QA hoy (03/11):** D:BUS-060. [T] Catálogo → barra inferior "taladro" → resultados → "Volver". Aparece "¿Cerrar la búsqueda actual? Se cerrará la búsqueda y volverás al inicio." (Cancelar / Sí, cerrar). **Esperado:** "Volver" y Atrás vuelven al Catálogo. CAT-024 y CAT-025 **pasan**. BUS-052 (sin red + chip → "Volver") no se volvió a ejecutar. (`2026-11-03-R05.md`, sección F-38.)

**Qué ya está hecho (ola 2):** Atrás del sistema = "Volver" en Búsqueda (`features/product-search/‹pkg›/feature/productsearch/ui/ProductSearchScreen.kt:57`, `BackHandler { …Back }`) y en Catálogo. "Sí, cerrar" vacía el carrito.

**Causa en el código:**
- `ProductSearchViewModel.kt:421-426` (`onBack`):
  ```kotlin
  if (history.size > 1) { … } else { _uiState.update { it.copy(showCloseConfirmDialog = true) } }
  ```
  Con un solo resultado siempre abre el diálogo, y `confirmCloseSearch` termina en `NavigateHomeIdle` (`:484`). No mira desde dónde se abrió la búsqueda.
- `openspec/specs/product-search/spec.md:238` lo fija así: "When no previous result exists, Volver SHALL open the new-search confirmation dialog".
- BUS-052, según el código (sin re-ejecutar hoy): `settleNetworkError` (`:393`) no apila en `history`. Con un solo resultado más el error de red, `onBack` (`:421`) abre el diálogo en vez de volver a los resultados.

**Qué garantiza el backend:** no aplica.

**Cambio:**
1. Nuevo efecto `ProductSearchUiEffect.ExitRequested`. En `onBack()`:
   ```kotlin
   when {
       _uiState.value.isVoiceCapturing -> cancelVoiceCapture()
       _uiState.value.phase is ProductSearchPhase.NetworkError && history.isNotEmpty() -> showEntry(history.last()) // BUS-052
       history.size > 1 -> { history.removeAt(history.lastIndex); showEntry(history.last()) }
       else -> emit(ProductSearchUiEffect.ExitRequested)
   }
   ```
2. `productSearchScreen(…)` (`ProductSearchNavigation.kt`) y `ProductSearchScreen` reciben `previousIsHome: () -> Boolean`. En `ExitRequested`: si `previousIsHome()`, `viewModel.onEvent(ProductSearchUiEvent.NewSearch)` (el diálogo de hoy); si no, `onNavigateBack()`. En `MainActivity.kt:389-401`: `previousIsHome = { navController.previousBackStackEntry?.destination?.route == HOME_ROUTE }`.
3. Actualizar `openspec/specs/product-search/spec.md:238-248`: sin resultado previo, "Volver" vuelve a la pantalla anterior; solo desde Home abre el diálogo de cierre.
4. Opcional (BUS-052): un botón "Reintentar" en el error de red de Búsqueda que repita la última petición.

**No hacer:** no cerrar la sesión del cliente al volver al Catálogo: el carrito tiene que seguir ahí.

**Verificación:**
- D:BUS-060: Catálogo → "taladro" → "Volver" → Catálogo (sin diálogo), con el carrito intacto. Lo mismo con el Atrás del sistema.
- Desde Home → buscar → "Volver" → el diálogo de hoy.
- D:BUS-052: resultados → modo avión → chip → "Perdí la conexión…" → "Volver" → los resultados anteriores.
- CAT-024 y CAT-025 siguen pasando.

---

##### F-17 · Sin red, el micrófono de Búsqueda no hace nada ni avisa — MEDIO · PARCIAL

**Qué ve QA hoy (03/11):** D:VOZ-010. [T] búsqueda escrita "taladro" → 3 resultados → modo avión → tocar el micrófono de la barra inferior. La pantalla no cambia a 1 s ni a 5 s: no hay "Escuchando…", ni banner, ni mensaje. **Esperado:** un aviso con el motivo (sin conexión). VOZ-008 (sin permiso → diálogo → DENY → Toast "Necesitamos acceso al micrófono…") y DET-040 (mic del Detalle → "Escuchando…") **pasan**. (`2026-11-03-R03.md`, sección F-17.)

**Qué ya está hecho (ola 2):**
- `rememberMicPermissionGate` en `core/ui/‹pkg›/core/ui/permission/MicPermissionGate.kt`, usado en Home, Búsqueda y Detalle.
- El micrófono del Detalle abre la búsqueda por voz (`ProductDetailViewModel.kt:120`).

**Causa en el código:** `features/product-search/‹pkg›/feature/productsearch/ui/ProductSearchViewModel.kt:225-231`:
```kotlin
// No speech or an STT error does NOT trigger an alternative search — restore the previous screen.
VoiceSessionEvent.NoAudio,
is VoiceSessionEvent.Failed,
-> { cancelVoiceSlowResponseTimer(); restorePreVoicePhase() }
```
Sin red, la sesión de voz falla al pedir `POST /stt/token` (`Failed(Network)`) antes de emitir `Listening`. La pantalla se restaura en silencio, sin mostrar nada.

**Qué garantiza el backend:** no aplica: es un fallo de red local.

**Cambio:**
1. Nuevo efecto `ProductSearchUiEffect.VoiceFailed(val category: SttErrorCategory)`. En `onVoiceEvent`, el caso `is VoiceSessionEvent.Failed` llama a `emit(ProductSearchUiEffect.VoiceFailed(event.category))` antes de `restorePreVoicePhase()`. `NoAudio` queda como está: la pantalla ya vuelve al estado anterior y el plan solo pide el motivo de un error.
2. En `ProductSearchScreen.kt:38-50`, mostrarlo igual que `LineLimitReached`, con Toast largo. El texto sale de un mapeo local:
   - `Network` → `UiR.string.discovery_error_network_title` + `discovery_error_network_message` ("Perdí la conexión… Parece que la red está inestable…").
   - El resto → un string nuevo en `core/ui`: `discovery_voice_failed` = "No pude usar el micrófono. Escribe tu búsqueda o inténtalo otra vez."

   Se usa Toast porque es el mismo recurso que QA aceptó en VOZ-008, y sobrevive al `NavigateBack` cuando la búsqueda se abrió desde el micrófono del Catálogo (`phaseBeforeVoice == null`, `:308`). Si producto prefiere un banner dentro de la pantalla, usar un `voiceNotice` en `ProductSearchUiState` que se borre a los 4 s, pero entonces el caso "abierta desde el Catálogo" pierde el aviso.

**No hacer:** no lanzar una búsqueda alternativa ni pasar a `NetworkError` de pantalla completa: se perderían los 3 resultados que el cliente tenía.

**Verificación:**
- D:VOZ-010: resultados de "taladro" → modo avión → mic → en ≤ 1 s, "Perdí la conexión…" y los 3 resultados siguen en pantalla.
- Catálogo → mic sin red → vuelve al Catálogo con el mismo aviso.
- VOZ-008 y DET-040 siguen pasando.

---

##### F-59 · La tablet no muestra con qué nombre la conoce el comercio — MEDIO · PARCIAL (nombre por latido: NO IMPLEMENTADO en backend)

**Qué ve QA hoy (03/11):** X:XVI-030 / S:DIS-006. [T] Ajustes → Vincular no muestra el nombre de la tablet. QA anotó "`/devices/me` → `name: null`", pero el campo del backend se llama `deviceName` (`DeviceMeResponse.java`), no `name`, y la app ya lo lee. El backend repite nombres ("Tablet 3" dos veces activas a la vez); el celular no tiene campo de nombre ni forma de renombrar. (`2026-11-03-R01.md`, sección F-59.)

**Qué ya está hecho:**
- El latido manda el `appVersion` real y `model` (`core/data/‹pkg›/core/data/heartbeat/DeviceTelemetryProviderImpl.kt:27,30`). La parte "1.0.0 hasta el primer latido" ya funciona.
- `DeviceInfoResponseDto.deviceName` (`DevicePairingDtos.kt:40`) llega a `PairedDevice.deviceName`.

**Causa en el código:** `features/pairing/‹pkg›/feature/pairing/ui/PairingContent.kt:374-415` (`PairedState`) pinta tienda, Actividad, Plataforma y Código usado, pero no `device.deviceName`.

**Qué garantiza el backend:** §37, `POST /devices/pair`. Sin nombre, o con el genérico "Tablet", el backend asigna "Tablet N", con N = activas + 1. Se renombra con `PATCH /api/v1/devices/{deviceId} {"name"}` desde el celular. **El nombre por heartbeat NO está implementado** (plan R01, "Ojo"): la tablet no debe mandar `deviceName` en el latido.

**Cambio (solo tablet):**
1. En `PairedState`, agregar arriba de "Actividad" un `DetailRow(label = stringResource(R.string.pairing_device_name_label), value = device.deviceName)`. String nuevo: `<string name="pairing_device_name_label">Nombre en la app del comercio</string>`. Opcional: una fila "Modelo" con el modelo del equipo (`Build.MANUFACTURER`/`Build.MODEL`, que ya formatea `formatDeviceModel`).
2. Como `/devices/me` se pide cada vez que se abre Vincular, un renombrado desde el celular se ve al reabrir la sección.

**No hacer:** no agregar `deviceName` al `HeartbeatRequestDto`: el backend lo ignora (no está en `HeartbeatRequest.java`).

**Verificación:** X:XVI-030 / X:EXP-011: con dos tablets, cada una muestra en Ajustes → Vincular el mismo nombre que su tarjeta en el celular. Renombrar con `PATCH` → al reabrir Vincular aparece el nombre nuevo. X:XCR-034: "Versión de la app" = versionName desde el primer latido (ya pasa). Que no se repitan los "Tablet N" depende del backend (ver nota final).

---

##### F-07 · El permiso de micrófono salta solo al llegar a Home — BAJO · PARCIAL

**Qué ve QA hoy (03/11):** D:EXP-004. `pm clear` → Welcome sin diálogo (OK). Tras vincular, "Allow QhathiaDiscovery to record audio?" aparece encima de Home sin tocar el micrófono. **Esperado:** el diálogo aparece al tocar el micrófono en Home. (`2026-11-03-R01.md`, sección F-07.)

**Qué ya está hecho:** ya no se pide sobre Welcome (ola 3). Además Home, Búsqueda y Detalle ya piden el permiso al tocar el micrófono (`rememberMicPermissionGate`; `features/home/‹pkg›/feature/home/ui/HomeScreen.kt:30`). La ola 3 eligió a propósito pedirlo "when Home is shown for the first time" (`openspec/changes/archive/2026-10-30-qa-wave-3-low-fixes/proposal.md`): choca con el plan.

**Causa en el código:** `app/‹pkg›/MainActivity.kt:303-306`:
```kotlin
LaunchedEffect(currentRoute) {
    if (currentRoute == HOME_ROUTE) activity?.requestMicPermissionOnHome()
}
```
Además `:79-80` (`requestMicPermission`), `:100-110` (`requestMicPermissionOnHome`) y `:448-449` (`micPermissionAskedOnHome`).

**Qué garantiza el backend:** no aplica.

**Cambio:**
1. Borrar ese `LaunchedEffect`, `requestMicPermission`, `requestMicPermissionOnHome`, la variable `micPermissionAskedOnHome` y los imports que queden sin uso (`Manifest`, `PackageManager`, `ActivityResultContracts`).
2. Actualizar el comentario de `core/data/‹pkg›/core/data/stt/AudioRecorderImpl.kt:50` ("requested at app open").
3. Ajustar el spec de onboarding/home de la ola 3 en `openspec/specs/` para que diga "al primer toque del micrófono".

**No hacer:** no quitar `rememberMicPermissionGate` de Home: es justamente el camino correcto.

**Verificación:** D:EXP-004: `pm clear` → vincular → Home **sin** diálogo → tocar el micrófono → aparece el diálogo del sistema → ALLOW → "Escuchando…". DENY → Toast "Necesitamos acceso al micrófono…".

---

##### F-24 · Al volver la red, el latido tarda hasta 60 s — BAJO · PARCIAL

**Qué ve QA hoy (03/11):** X:XCR-006 / X:XRE-001. Modo avión a las 07:18:34 → [C] voz OFF + REFRESH_CATALOG → red a las 07:20:09 (Wi-Fi validada a las 07:20:12.6) → `POST /devices/heartbeat` a las 07:20:17: **8 s** (4,5 s tras la Wi-Fi). Lo pedido es ≤ 3 s. En ese latido se aplicaron los ajustes y el ack. (`2026-11-03-R02.md`, sección F-24.)

**Qué ya está hecho:** nada del reintento corto. Los 8 s fueron suerte: coincidieron con el tic de 60 s.

**Causa en el código:** `core/domain/‹pkg›/core/domain/usecase/RunDeviceHeartbeatUseCase.kt:37-50`:
```kotlin
while (true) {
    try { sendHeartbeat() } catch (…) { … } // el Result.failure ni se mira
    delay(interval)                          // siempre 60 s (DeviceConstants.kt:15)
}
```
El dominio no expone "hay internet": `NetworkConnectivityState` (`core/domain/‹pkg›/core/domain/model/NetworkConnectivityState.kt`) no tiene ese campo, aunque `ConnectivitySnapshot.hasInternet` existe (`core/data/‹pkg›/core/data/network/ConnectivityDataSource.kt:27`) y el mapeo lo descarta (`NetworkSettingsRepositoryImpl.kt:41-47`).

**Qué garantiza el backend:** el latido es idempotente y barato. La respuesta trae `settingsVersion` y `pendingCommands` (API_REFERENCE, "POST /api/v1/devices/heartbeat"; §21 y §23 para ajustes y comandos).

**Cambio:**
1. Agregar `val hasInternet: Boolean = false` a `NetworkConnectivityState`, mapeado desde `snapshot.hasInternet` en `NetworkSettingsRepositoryImpl.kt:41-47`. Mejor aún, agregar `NET_CAPABILITY_VALIDATED` en `ConnectivityDataSourceImpl.snapshot()` (`:74-87`).
2. `DeviceConstants`: `val HEARTBEAT_RETRY_DELAYS = listOf(5.seconds, 10.seconds, 20.seconds, 40.seconds)`.
3. `RunDeviceHeartbeatUseCase`: inyectar `NetworkSettingsRepository` y cambiar el bucle:
   ```kotlin
   var failures = 0
   while (true) {
       val ok = try { sendHeartbeat().isSuccess } catch (c: CancellationException) { throw c } catch (_: Exception) { false }
       failures = if (ok) 0 else failures + 1
       val wait = if (ok) interval else retryDelays.getOrElse(failures - 1) { interval }
       withTimeoutOrNull(wait) {
           networkSettingsRepository.observeConnectivity().map { it.hasInternet }
               .distinctUntilChanged().drop(1).first { it }   // la red volvió: latir ya
       }
   }
   ```
   Agregar los parámetros nuevos al constructor secundario `@Inject` (`:24-28`), igual que `interval`.
4. Tests con tiempo virtual (`RunDeviceHeartbeatUseCaseTest`): fallo → reintento a +5 s; la red vuelve 2 s después de un fallo → latido a los 2 s; éxito → +60 s.

**No hacer:** no reintentar en corto ante `DEVICE_UNLINKED`. Con F-101 la sesión se borra y `collectLatest` corta el bucle solo.

**Verificación:** X:XCR-006. Modo avión 90 s → quitarlo → `POST /devices/heartbeat` en nginx en ≤ 3 s desde que vuelve la red, y el ajuste cambiado se aplica en ≤ 10 s.

---

##### F-63 · El primer latido manda `uptimeSeconds: 0` — BAJO · PARCIAL (lado tablet)

**Qué ve QA hoy (03/11):** X:EXP-005. Primer latido tras reabrir (07:12:15): `"networkStatus":"WIFI"` (OK) pero `"uptimeSeconds":0`. Lo pedido es > 0. El "ETHERNET" en crudo es del celular. (`2026-11-03-R02.md`, sección F-63.)

**Qué ya está hecho:** el primer latido ya no manda `OFFLINE`. `toNetworkStatus()` usa `wifiActive || connectedSsid` (`core/data/‹pkg›/core/data/heartbeat/DeviceTelemetryFormatting.kt:35-39`).

**Causa en el código:** `DeviceTelemetryProviderImpl.kt:45-48`:
```kotlin
private fun processUptimeSeconds(): Long? {
    val elapsedMs = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
```
Mide el tiempo del **proceso**, y el primer latido sale en `Application.onCreate`, así que siempre manda 0.

**Qué garantiza el backend:** `HeartbeatRequest.uptimeSeconds` es "Tiempo de actividad continua del **dispositivo** en segundos. Opcional" (`services/auth-api-service/.../dto/device/HeartbeatRequest.java`). `networkStatus` acepta cualquier texto de ≤ 20 caracteres.

**Cambio:**
1. Reemplazar `processUptimeSeconds()` por `deviceUptimeSeconds() = SystemClock.elapsedRealtime() / MILLIS_PER_SECOND`, y quitar el import de `Process`.
2. Opcional: Ethernet. Agregar `ethernetActive` (`TRANSPORT_ETHERNET`) a `ConnectivitySnapshot`, `ETHERNET` a `DeviceNetworkStatus` y su mapeo en `toNetworkStatus()`. Hoy una tablet por cable diría `OFFLINE`.

**Verificación:** X:EXP-005: `am force-stop` + reabrir con Wi-Fi → el primer `POST /devices/heartbeat` en logcat `KtorClient` lleva `"networkStatus":"WIFI"` y `"uptimeSeconds"` > 0 (≈ el tiempo desde el arranque del emulador).

---

##### F-43 · Detalle sin foto: tres miniaturas vacías y sin "Sin foto" — BAJO · PARCIAL

**Qué ve QA hoy (03/11):** D:DET-062 ("QA Foco sin foto") y D:DET-063 ("QA Foco imagen rota"). La foto principal muestra el ícono (OK), pero hay 3 miniaturas vacías y no dice "Sin foto". Los productos con una sola foto (Foco LED 9W, Interruptor) también muestran 3 miniaturas vacías. En el Catálogo las tarjetas sin foto muestran el ícono (XCA-005 OK). (`2026-11-03-R05.md`, sección F-43.)

**Qué ya está hecho (ola 3):** `ProductImage` con placeholder (ícono `ic_image` sobre `Neutral50`) cuando la URL viene vacía o falla (`core/ui/‹pkg›/core/ui/component/ProductImage.kt`), usado en tarjetas, carrito, resumen y galería.

**Causa en el código:** `core/ui/‹pkg›/core/ui/component/ProductImageGallery.kt:60-67`:
```kotlin
val thumbnails: List<String?> = imageUrls.drop(1).take(lateralSlots)
Column(…) { repeat(lateralSlots) { index -> Thumbnail(url = thumbnails.getOrNull(index), …) } }
```
Con `url == null` pinta un cuadro gris (`:85-88`). El KDoc (`:28-29`) dice que es una regla de Figma ("the lateral slots are filled with neutral gray placeholder squares"). `ProductImage` no tiene la etiqueta "Sin foto".

**Qué garantiza el backend:** no aplica. Sin foto, el detalle trae `images: []` / `primaryImageUrl: null`.

**Cambio:**
1. `ProductImage`: agregar el parámetro `showMissingLabel: Boolean = false`. Con el placeholder y `showMissingLabel`, poner debajo del ícono un `Text(stringResource(R.string.discovery_image_missing))` (`<string name="discovery_image_missing">Sin foto</string>` en `core/ui`), en `Neutral500`, `bodySmall`.
2. `ProductImageGallery`:
   - Principal con `showMissingLabel = imageUrls.isEmpty()`.
   - Pintar la columna de miniaturas solo si `imageUrls.size > 1`, y solo con las fotos que existen (`imageUrls.drop(1).take(lateralSlots)`), sin rellenar con cuadros grises.
   - Actualizar el KDoc (`:26-36`) y los previews (`:94-114`).
   - Si producto quiere mantener el relleno gris de Figma para los productos **con** foto, dejarlo solo para `imageUrls.size >= 1`. Lo que pide QA (DET-062) es que el producto **sin** foto no tenga miniaturas vacías.

**No hacer:** no quitar el placeholder de la imagen principal: DET-063 ya pasa.

**Verificación:** D:DET-062: ícono y "Sin foto", sin miniaturas. D:DET-063: ícono en la principal (sigue OK). Un producto con una sola foto: sin cuadros vacíos, si producto acepta el cambio. X:XCA-005 sigue OK.

---

##### F-01 · Vinculación tras 61 s — tablet CORREGIDO (el arreglo queda PARCIAL por el celular)

**Qué ve QA hoy (03/11):** D:VIN-031 / X:XVI-021. Aprobado a los 119 s: el WS se cierra tras APPROVED (453 B), y canje, latido y settings llegan en el mismo segundo: Home en ≤ 1 s. Falla solo el celular, que no avisa si la tablet no canjeó (F-60). (`2026-11-03-R01.md`, sección "Corregidos".)

**Qué ya está hecho:** reconexión con backoff y al volver la red, canje único y fallback al rechazar el handshake (`core/data/‹pkg›/core/data/pairing/DevicePairingRepositoryImpl.kt:78-143`), más el keepalive del servidor cada 25 s (§37).

**Cambio:** ninguno obligatorio. Opcional, para endurecer: el contrato (§37) sugiere consultar `GET /api/v1/devices/pairing-session/{sessionId}/status` cada 3–5 s mientras se muestra el código y canjear al ver `APPROVED`, como respaldo del WS. No es necesario para que QA pase.

**Lado celular (no desarrollar aquí):** polling de `/status` y aviso "La tablet no respondió" si `tokenExchanged=false` en 30 s (F-60).

---

##### F-09 · `RESTART_APP` — tablet CORREGIDO (el arreglo queda PARCIAL por el celular)

**Qué ve QA hoy (03/11):** X:XCR-021. [C] "Reiniciar" con 1 ítem en el carrito. El comando llega con el latido de las 07:09:28, se crea un proceso nuevo (pid 7560 → 8007), Home queda `PINNED` y con el carrito vacío, y no aparece "Attempt to violate Lock Task Mode". El ack 204 sale a las 07:09:29, después del latido del proceso nuevo. Falla el celular: dice "Reinicio: Listo" sin pasar por "Reiniciando…" y no avisa si la tablet no vuelve. (`2026-11-03-R02.md`, sección F-09.)

**Qué ya está hecho:** trampolín `RestartActivity` en el proceso `:restart` (`app/‹pkg›/RestartActivity.kt`) y ack después del relanzamiento con `PendingRestartAck` (`core/domain/‹pkg›/core/domain/usecase/ProcessDeviceCommandsUseCase.kt:62-77`).

**Cambio:** ninguno en la tablet. **No tocar** el `launchMode` ni el manifest de `RestartActivity` al hacer F-12 y F-10 sin volver a correr XCR-021.

**Lados que faltan:** celular ("Listo" solo con `ACKED` y aviso a los 3 min) y backend (409 al acusar un comando vencido: **no implementado**; el celular decide con `status`/`ackedAt` de `GET /devices/{id}/commands`).

---

#### Pendiente del backend detectado en este bloque

1. **F-59: nombres "Tablet N" repetidos.**
   - `services/auth-api-service/src/main/java/com/vitria/auth/service/DeviceService.java:504-511` hace `return "Tablet " + (active + 1);`, con N = número de activas + 1.
   - Tras desvincular una tablet intermedia, el nombre se repite: QA vio `0d881a2d` y `35067926`, ambas "Tablet 3" activas a la vez.
   - Corrección: elegir el menor N ≥ 1 que no use ninguna tablet activa de la tienda.
2. **F-13: aviso al dueño por `SETTINGS_PIN_LOCKOUT` sin implementar.** El log llega a `device_logs`, pero nadie avisa al OWNER: falta un push "Intentos de PIN fallidos en <tablet>" o un `lastPinLockoutAt` en `GET /devices`. La tablet hará su parte (enviar el log) igual.
3. **F-09: 409 al acusar un comando `EXPIRED`** sin implementar. No bloquea a la tablet.
4. **Documentación desactualizada (menor):** `API_REFERENCE.md`, "POST /api/v1/devices/heartbeat", y el comentario de `HeartbeatRequest.java` ponen `networkStatus` = 'ONLINE'/'LIMITED' como ejemplo, pero la tablet manda `WIFI`/`MOBILE`/`OFFLINE`. El ejemplo de body tampoco muestra `model`.
5. **Operación (no es backend):** provisionar device owner en las tablets de campo (F-10, F-12) o, como mínimo, elegir la app como pantalla de inicio.

---

### 2.2 · Carrito, pedido, búsqueda y transporte (R06, R07, R12)

Repositorio: `D:\Vitria\android\qhathia-mobile-discovery`, commit `490694e` (Discovery 1.23.0-SNAPSHOT, la versión que QA probó el 31/10 y el 03/11). Las rutas son relativas a la raíz de ese repo. Los números de línea se verificaron abriendo los archivos en ese commit.

**Cómo leer este bloque.** Los `fixes-discovery-B.md` del 28/10 se escribieron sobre `f8622dd`. Desde entonces la app cambió mucho: ya existen `ShareOutcome`, `RevalidateCartUseCase`, `IsOrderableUseCase`, el límite de 20 líneas y `LocalUserActivity`. Por eso, en cada arreglo, "Qué ya está hecho" dice lo que hay hoy y "Cambio" dice solo lo que falta. Si un `fixes-*.md` y la `API_REFERENCE.md` (§38, §39, §41) no coinciden, **manda la API**.

**Pieza compartida.** F-30, F-32, F-33 y F-44 necesitan leer el cuerpo `problem+json` de los errores HTTP, y hoy la app lo descarta. Se construye una sola vez, en el paso 1 de F-30, y los demás arreglos la reutilizan.

Orden: F-30 → F-26 → F-44 → F-37 → F-32 → F-33 → F-29 → F-34 → F-35 → F-20 → F-27.

---

##### F-30 · El cliente ve S/ 210 y el pedido se graba a S/ 199 — CRÍTICO · NO CORREGIDO

**Qué ve QA hoy (03/11):** D:CAR-026 / X:XCA-010 (`2026-11-03-R06.md`, "[CRÍTICO] F-30").
1. En la tablet se agrega "QA R06 OK sierra" al carrito a S/ 210.
2. Otro vendedor hace `PUT price 199`.
3. En la tablet se abre el carrito y se toca "Realizar pedido".

Resultado:
- El carrito sigue en **S/ 210.00, sin aviso**.
- El cuerpo de `POST /orders` es `{"productId":…,"quantity":1}`, **sin `expectedUnitPrice`**. Responde 201 y el pedido 878986 queda grabado a **S/ 199.00**.

Lo esperado:
- Al abrir el carrito, el aviso "cambió de S/ 210.00 a S/ 199.00" y el total en 199.
- Si el carrito ya estaba abierto, el primer "Realizar pedido" no crea nada y muestra el aviso; el segundo da 201 con el total que se ve.

El backend sí está bien: con `expectedUnitPrice 210` responde 409 `PRICE_CHANGED`, y el reintento con 199 da 201.

**Qué ya está hecho:**
- El carrito usa el precio con descuento al agregar: `core/domain/.../model/CartItemMappers.kt:14` y `:29`, `priceUnit = discountedPrice ?: price`.
- Existe `RevalidateCartUseCase` (`core/domain/.../usecase/RevalidateCartUseCase.kt`), pero solo corre **después** de un 400/404/409 y solo marca líneas no disponibles. No mira precios.
- No hay nada de `expectedUnitPrice`, de `PRICE_CHANGED` ni de refresco del carrito al abrirlo.

**Causa en el código:**
1. `core/data/src/main/java/com/qhathia/mobile/discovery/core/data/order/OrderMappers.kt:30`, `items = items.map { OrderItemRequestDto(productId = it.productId, quantity = it.quantity) }`. No manda el precio visto. `OrderItemRequestDto` (`core/network/.../dto/OrderDtos.kt:20-23`) solo tiene `productId` y `quantity`.
2. El carrito congela el precio al agregar y nada lo vuelve a leer:
   - `core/data/.../cart/CartSessionLocalDataSource.kt:19-33` solo tiene `addProduct/setQuantity/removeProduct/clear`.
   - `features/cart/.../ui/CartDialog.kt:57-60` abre el `Dialog` sin disparar ninguna carga.
3. El cuerpo del error se pierde. En `core/network/.../NetworkResult.kt:21` está `data class HttpError(val code: Int, val message: String)`, y en `:34-37` solo se guardan el status y su descripción.
4. Un 409 se trata siempre como "no disponible". En `core/data/.../order/OrderErrorMapper.kt:23-27`, `400, 409 -> OrderErrorCategory.Unavailable`. Después `CartViewModel.kt:162` llama a `flagUnavailableLines`, que hace un GET por línea, no encuentra nada no disponible (el producto sigue a la venta) y cae en `OrderSubmission.Error`, el genérico "Algo salió mal".

**Qué garantiza el backend:** API_REFERENCE §38 y `services/biz-api-service/.../service/OrderService.java:105-119`.
- `POST /api/v1/orders` acepta `items[].expectedUnitPrice` (opcional, `≥ 0`).
- El backend compara con 2 decimales (`HALF_UP`) contra el precio efectivo, que es el `discountedPrice` vigente o, si no hay, el `price`.
- Si alguno difiere, responde esto y **no crea el pedido**:

```json
HTTP 409  application/problem+json
{ "errorCode": "PRICE_CHANGED",
  "detail": "El precio de 'QA X OK' cambió de S/ 210.00 a S/ 199.00. Revisa tu carrito.",
  "changes": [ { "productId": "…", "productName": "QA X OK", "expectedUnitPrice": 210.00, "currentUnitPrice": 199.00 } ] }
```

- Antes del precio valida la vendibilidad, en este orden:
  - 404 `RESOURCE_NOT_FOUND`;
  - 409 `PRODUCT_NOT_SELLABLE`;
  - 422 `STOCK_INSUFFICIENT`.
- **Un 409 puede ser `PRICE_CHANGED` o `PRODUCT_NOT_SELLABLE`: hay que decidir por `errorCode`.**
- `GET /api/v1/products` sirve para refrescar el carrito (`ProductController.java:53-59`):
  - acepta el token DEVICE;
  - devuelve solo productos activos, con `price`, `discountedPrice`, `availability` y `stock`;
  - **no** cuenta `PRODUCT_VIEW`, a diferencia de `GET /products/{id}`.

**Cambio:**

1. **Leer el cuerpo de los errores (pieza compartida).**
   - En `core/network/.../NetworkResult.kt`:
     - `data class HttpError(val code: Int, val message: String, val body: String? = null)`. El valor por defecto mantiene compilando los tests que crean `HttpError(500, "…")`.
     - Convertir `toNetworkResult()` en `suspend`. Las 13 llamadas están dentro de funciones `suspend`.
     - Para `ResponseException`, añadir `body = runCatching { throwable.response.bodyAsText() }.getOrNull()`. Con `expectSuccess = true` (`KtorClientFactory.kt:39`, Ktor 3.4.3) el cuerpo debería seguir legible; confírmalo con un test sobre `KtorClientFactory.create(MockEngine)`.
   - Archivo nuevo `core/network/.../dto/ProblemDetailDto.kt`:
     ```kotlin
     @Serializable data class PriceChangeDto(val productId: String = "", val productName: String = "",
         val expectedUnitPrice: Double = 0.0, val currentUnitPrice: Double = 0.0)
     @Serializable data class ProblemDetailDto(val errorCode: String? = null, val detail: String? = null,
         val productId: String? = null, val productName: String? = null, val reason: String? = null,
         val available: Int? = null, val requested: Int? = null,
         val changes: List<PriceChangeDto> = emptyList(), val fieldErrors: Map<String, String> = emptyMap())
     ```
     Parsear con un `Json { ignoreUnknownKeys = true }` dentro de `runCatching`. Si el cuerpo no se puede leer, `null`.
2. **Mandar el precio visto.**
   - `OrderItemRequestDto(productId, quantity, val expectedUnitPrice: Double? = null)`.
   - En `OrderMappers.kt:30`: `expectedUnitPrice = BigDecimal.valueOf(it.priceUnit).setScale(2, RoundingMode.HALF_UP).toDouble()`.
3. **Errores de dominio.**
   - `OrderErrorCategory` (`core/domain/.../model/OrderErrorCategory.kt:16`) suma `PriceChanged`, `ProductNotSellable`, `StockInsufficient`, `Validation` y `Timeout`. Estos tres últimos los usan F-32, F-33 y F-44.
   - `OrderException` lleva datos opcionales: `class OrderException(val category: OrderErrorCategory, val priceChanges: List<CartPriceChange> = emptyList(), val productId: String? = null, val productName: String? = null, val available: Int? = null)`. Además, en el dominio, `data class CartPriceChange(val productId: String, val productName: String, val oldUnitPrice: Double, val newUnitPrice: Double)`.
   - En `OrderRepositoryImpl.kt:44-48`, para `HttpError`, parsear el body y decidir por `errorCode`, no por status:
     - `PRICE_CHANGED` → `PriceChanged` con los `changes`;
     - `PRODUCT_NOT_SELLABLE` → `ProductNotSellable`;
     - `STOCK_INSUFFICIENT` → `StockInsufficient`;
     - `VALIDATION_ERROR` → `Validation`;
     - 404 → `NotFound`;
     - sin `errorCode`, el mapeo actual de `OrderErrorMapper.fromHttpError`.
4. **Refresco del carrito al abrirlo** (cubre "abrir el carrito → aviso"):
   - `ProductApiService`: `suspend fun listProducts(token: String): NetworkResult<List<CatalogProductDto>>` sobre `GET ${BuildConfig.QHATHIA_BASE_URL}/api/v1/products`, con timeout de 15 s. Añadir `val stock: Int? = null` a `CatalogProductDto` (`CatalogDtos.kt:25-37`).
   - Dominio:
     - `data class ProductSnapshot(val id: String, val unitPrice: Double, val availability: String, val stock: Int?)`, donde `unitPrice = discountedPrice ?: price`.
     - Puerto `CartPricingRepository { suspend fun snapshots(): Result<Map<String, ProductSnapshot>> }`, implementado en `core/data/.../cart/CartPricingRepositoryImpl.kt` con `SessionTokenProvider` y el mismo reintento ante 401 que `OrderRepositoryImpl.kt:36-42`. Enlazarlo en `di/CartDataModule.kt`.
   - En `Cart.kt`, función pura `fun refreshedWith(current: Map<String, ProductSnapshot>): CartRefresh`:
     - devuelve `CartRefresh(cart, priceChanges: List<CartPriceChange>, removed: List<CartItem>, capped: List<CartItem>)`;
     - actualiza `priceUnit`;
     - quita las líneas que no vienen en el mapa (inactivas o eliminadas) o que no son vendibles (`IsOrderableUseCase.isOrderable`);
     - recorta la cantidad al `stock` (F-32).
   - `CartRepository`/`CartSessionLocalDataSource`: `suspend fun replace(cart: Cart)` = `state.value = cart`. No debe disparar el "fin de sesión" (`ObserveCustomerSessionEndUseCase`), que limpia el formulario.
   - `RefreshCartUseCase` (core/domain): lee el carrito, pide los snapshots, aplica `refreshedWith` y guarda con `replace`. Si falla la red, no toca nada y devuelve `Result.failure`.
5. **ViewModel y UI.**
   - Nuevo `CartUiEvent.Opened`, emitido desde `CartDialog.kt` con `LaunchedEffect(Unit) { viewModel.onEvent(CartUiEvent.Opened) }`.
   - `CartUiState` suma `notice: CartNotice?`, con `sealed interface CartNotice { data class PricesChanged(val changes: List<CartPriceChange>); data class ItemsRemoved(val names: List<String>); data class QuantityCapped(val name: String, val max: Int) }`.
   - `Opened` → `refreshCart()` → si hubo cambios, `notice = …`.
   - En `placeOrder()` (`CartViewModel.kt:149-167`), `.onFailure`, si la categoría es `PriceChanged`:
     ```kotlin
     OrderErrorCategory.PriceChanged -> {
         updatePrices(cause.priceChanges)            // caso de uso que hace setPrice por línea vía replace()
         _uiState.update { it.copy(submission = OrderSubmission.Idle, notice = CartNotice.PricesChanged(cause.priceChanges)) }
     }
     ```
     No es un error: vuelve al carrito con el total nuevo y el cliente toca "Realizar pedido" otra vez.
   - En `CartDialogContent.FooterPanel` (`CartDialogContent.kt:327-356`), encima de `TotalsSection`, un recuadro ámbar con el aviso. Strings nuevos en `features/cart/src/main/res/values/strings.xml`:
     - `cart_notice_price_changed` = "El precio de «%1$s» cambió de %2$s a %3$s. Revisa el total antes de hacer tu pedido." Una línea por cambio, como máximo 3, y después "y %d más".
     - `cart_notice_items_removed` = "Quitamos de tu carrito lo que ya no está disponible: %1$s."
   - El aviso se borra al cambiar una cantidad, al enviar o en `resetCustomerData()` (`CartViewModel.kt:245-257`).
6. **Tests:**
   - `CartViewModelTest.placeOrder_priceChanged_updatesPricesAndShowsNotice`;
   - `CartViewModelTest.opened_refreshesPrices`;
   - `CartTest.refreshedWith_*`;
   - `OrderRepositoryImplTest.http409PriceChanged_mapsChanges`.

**No hacer:**
- No mandar `expectedUnitPrice` sin el paso 1. Con el mapeo actual (`409 → Unavailable`), cada cambio de precio terminaría en "Algo salió mal" después de N GETs inútiles. Sería peor que hoy.
- No refrescar con `GET /products/{id}` por línea: cada llamada con token DEVICE suma una visita `PRODUCT_VIEW` falsa a las métricas del comerciante (`ProductController.java:97-110`). Usar `GET /products`.
- No recalcular el precio en la tablet aplicando `discounts[]`. El precio efectivo lo da el backend (`discountedPrice`).

**Verificación:** D:CAR-026, X:XCA-010, X:XCO-020, X:XJO-011.
- Agregar a 210 → el vendedor pone 199 → abrir el carrito: aviso "cambió de S/ 210.00 a S/ 199.00" y total S/ 199.00.
- Con el carrito ya abierto durante el cambio, el primer "Realizar pedido" muestra en nginx `POST /orders 409` y el aviso. El segundo da `201` con `total` = el total que se ve.
- En logcat (`KtorClient`), el body lleva `"expectedUnitPrice":210.0`.

---

##### F-26 · La tablet no se entera del rechazo, la confirmación ni el vencimiento — ALTO · NO CORREGIDO

**Qué ve QA hoy (03/11):** X:XPE-004 (`2026-11-03-R06.md`, "[ALTO] F-26").
1. En la tablet se hace el pedido 241182.
2. En el celular, Solicitudes → "Rechazar" → "Cliente se desanimó".

Resultado:
- A los 12 s y a los 32 s no hay aviso. Después el éxito se cerró solo y volvió a Home.
- **nginx no registra ningún `GET /orders/{id}/status`.**

Lo esperado: en ≤ 10 s, "El vendedor no pudo atender tu pedido". El backend está bien: `/status` con el token DEVICE pasa de PENDING a REJECTED, con `rejectionReason`; desde otra tablet da 404.

**Qué ya está hecho:** nada de este arreglo. El dominio `Order` (`core/domain/.../model/Order.kt:16-24`) ya trae `id`, pero nadie lo usa.

**Causa en el código:**
- `features/cart/.../ui/CartViewModel.kt:154`: `.onSuccess {` sin parámetro. El `Order` devuelto (id, status) se descarta.
- `OrderSubmission.Success` (`features/cart/.../model/OrderSubmission.kt:42-46`) solo tiene `summary`, `isQuoteOnly` y `shareDelivered`.
- `core/network/.../service/OrderApiService.kt` solo tiene `createOrder`; no existe la llamada a `/status`.
- `OrderResponseDto` (`OrderDtos.kt:30-42`) no modela `orderCode` ni `expiresAt`, y el backend los devuelve (`OrderResponse.java:44,50`).
- La pantalla de éxito es estática (`CartDialogContent.kt:68-92`).
- El éxito "se cierra solo" por el timer de inactividad de 60 s (`app/.../MainViewModel.kt:136`, `delay(CartConstants.INACTIVITY_TIMEOUT_SECONDS * 1_000L)`). Con el diálogo abierto, la ruta de debajo no es Home y el carrito sigue con ítems hasta "Aceptar", así que el timer corre.

**Qué garantiza el backend:** API_REFERENCE §38 y `OrderController.java:101-107`.
- `GET /api/v1/orders/{id}/status`, con `Authorization: Bearer <token DEVICE>`. Solo responde para los pedidos que creó esa tablet; uno de otra tablet o de otra tienda da 404 `RESOURCE_NOT_FOUND`.
- Respuesta 200 con `Cache-Control: no-store`:

```json
{ "id": "…", "orderCode": "624731", "status": "PENDING|CONFIRMED|REJECTED|EXPIRED|CANCELLED",
  "rejectionReason": "Cliente se desanimó", "expiresAt": "…", "lastStatusChangedAt": "…", "serverTime": "…" }
```

- `rejectionReason` solo viene en `REJECTED`.
- Recomendación del backend: consultar cada 5 s mientras se vea "¡Pedido generado!" (máximo 10 min) y parar en un estado terminal.
- **No hay push ni socket hacia la tablet:** el único canal es este polling.

**Cambio:**
1. **Red.**
   - `OrderResponseDto` suma `orderCode: String? = null` y `expiresAt: String? = null`. Propagarlos a `Order` y a `OrderMappers.kt:34-42`.
   - DTO nuevo `@Serializable data class OrderStatusDto(val id: String = "", val orderCode: String? = null, val status: String = "", val rejectionReason: String? = null, val expiresAt: String? = null, val lastStatusChangedAt: String? = null, val serverTime: String? = null)`.
   - En `OrderApiService`:
     ```kotlin
     suspend fun getOrderStatus(accessToken: String, orderId: String): NetworkResult<OrderStatusDto> =
         runCatching {
             client.get("${BuildConfig.QHATHIA_BASE_URL}/api/v1/orders/$orderId/status") {
                 header(HttpHeaders.Authorization, "Bearer $accessToken")
                 timeout { requestTimeoutMillis = 10_000L }
             }.body<OrderStatusDto>()
         }.toNetworkResult()
     ```
2. **Dominio y datos.**
   - `enum class OrderStatus { Pending, Confirmed, Rejected, Expired, Cancelled, Unknown }` y `data class OrderStatusInfo(val status: OrderStatus, val orderCode: String?, val rejectionReason: String?)`.
   - `OrderRepository.getOrderStatus(orderId): Result<OrderStatusInfo>`, en `OrderRepositoryImpl` con el mismo reintento ante 401.
   - `ObserveOrderStatusUseCase`:
     ```kotlin
     operator fun invoke(orderId: String): Flow<OrderStatusInfo> = flow {
         val deadline = clock.now() + 10.minutes
         while (clock.now() < deadline) {
             repository.getOrderStatus(orderId).onSuccess { info ->
                 emit(info)
                 if (info.status != OrderStatus.Pending) return@flow   // terminal
             }                                                         // fallo de red: seguir intentando
             delay(5.seconds)
         }
     }
     ```
     Usar el reloj que inyecta `core/data/.../di/ClockDataModule.kt`, para poder testear con tiempo virtual.
3. **Estado.** `OrderSubmission.Success` suma `orderId: String? = null`, `orderCode: String? = null`, `status: OrderStatus = OrderStatus.Pending` y `rejectionReason: String? = null`, más `total` (F-29).
4. **ViewModel** (`CartViewModel.kt:149-167`):
   ```kotlin
   createOrder(state.name, state.phone, state.items)
       .onSuccess { order ->
           val delivered = shareQuote(state.name, state.phone, state.items) is ShareOutcome.Delivered
           _uiState.update { it.copy(submission = OrderSubmission.Success(summary, shareDelivered = delivered,
               orderId = order.id, orderCode = order.orderCode, total = order.total)) }
           statusJob?.cancel()
           statusJob = viewModelScope.launch {
               observeOrderStatus(order.id).collect { info ->
                   _uiState.update { s ->
                       val sub = s.submission as? OrderSubmission.Success ?: return@update s
                       s.copy(submission = sub.copy(status = info.status, rejectionReason = info.rejectionReason))
                   }
               }
           }
       }
   ```
   Cancelar `statusJob` en `finishAndReturnHome()` (`:232-239`), en `resetCustomerData()` (`:245-257`) y en `onCleared()`.
5. **UI** (`CartDialogContent.kt:68-92`), según `submission.status`:

   | Estado | Título | Subtítulo |
   |---|---|---|
   | `Pending` | el actual | el actual + "Tu código de pedido: %1$s" |
   | `Confirmed` | "¡Tu pedido fue confirmado!" | "Acércate al vendedor para pagar y recibirlo." |
   | `Rejected` | "El vendedor no pudo atender tu pedido" | `rejectionReason` si viene; si no, "Acércate al vendedor para más información." |
   | `Expired` | "Tu pedido venció" | "Vuelve a realizarlo si aún lo necesitas." |
   | `Cancelled` | "Tu pedido fue cancelado" | — |

   Strings nuevos: `cart_status_confirmed_title`, `cart_status_rejected_title`, `cart_status_expired_title`, etc. El footnote "Revisa tu celular…" solo en `Pending`/`Confirmed`.
6. **Que la pantalla viva lo suficiente.** Con el timer de 60 s, la pantalla de éxito se cierra antes de que el vendedor actúe. Usa el mismo arreglo de F-35 (tope de 180 s con el diálogo del carrito abierto), que cubre también esta pantalla. No pauses el timer sin límite: la tablet quedaría en el éxito para siempre.
7. **Tests** con `runTest` y tiempo virtual:
   - `CartViewModelTest.success_pollsStatus_showsRejectedWithReason`;
   - `success_accept_stopsPolling`;
   - `ObserveOrderStatusUseCaseTest.stopsOnTerminal`.

**No hacer:**
- No usar `GET /orders/{id}`: es solo para OWNER/SELLER/ADMIN y da 403 con DEVICE.
- No meter el estado en el heartbeat: va cada 60 s y el backend no lo implementa.
- No hacer polling después de "Aceptar" ni en segundo plano.

**Verificación:**
- X:XPE-004 y X:XJO-009: pedido desde la tablet → "Rechazar" en el celular. En ≤ 10 s la tablet muestra "El vendedor no pudo atender tu pedido" con el motivo, y nginx registra `GET /api/v1/orders/{id}/status` cada ~5 s, que se detiene al llegar a REJECTED.
- Confirmar desde el celular → "¡Tu pedido fue confirmado!".
- **X:XPE-043 (vence a los 60 min):** con este diseño no se puede ver en la tablet, porque la pantalla de éxito cierra mucho antes (180 s de inactividad, o 10 min como mucho de polling). Que QA lo dé por no aplicable, o que producto decida otra cosa. No es un defecto de la app.

---

##### F-44 · Sin red, el pedido da "Algo salió mal" genérico — ALTO · NO CORREGIDO

**Qué ve QA hoy (03/11):** X:XRE-002 (`2026-11-03-R06.md`, "[ALTO] F-44"). Con un Foco LED en el carrito y los datos llenos, se pone el modo avión y se toca "Realizar pedido".
- Sale "Algo salió mal · Hubo un problema generando la orden. Inténtalo de nuevo en unos momentos.", con "Entendido" y "Cancelar orden". No menciona la conexión.
- "Cancelar orden" vacía el carrito y vuelve a Home.
- Lo esperado: "Perdí la conexión…" con el mensaje de internet, y con red, "Reintentar" hace un solo `POST /orders 201`.

**Qué ya está hecho:**
- La capa de datos sí distingue la falta de red: en `core/data/.../order/OrderErrorMapper.kt:30`, `NetworkErrorKind.Connectivity -> OrderErrorCategory.Network`.
- Los textos de red ya existen en `core/ui/src/main/res/values/strings.xml:10-11`: `discovery_error_network_title` ("Perdí la conexión…") y `discovery_error_network_message`.

**Causa en el código:**
- `features/cart/.../ui/CartViewModel.kt:163`: `else -> _uiState.update { it.copy(submission = OrderSubmission.Error) }`. Cualquier categoría que no sea Unavailable o NotFound, también `Network`, termina en el mismo `Error`.
- `OrderSubmission.Error` es un `data object` sin motivo (`OrderSubmission.kt:49`).
- `CartDialogContent.kt:94-104` siempre pinta `cart_error_title`/`cart_error_message` con "Cancelar orden", que llama a `finishAndReturnHome()` (`CartViewModel.kt:123`).
- Además, en `OrderErrorMapper.kt:31`, `NetworkErrorKind.Timeout -> OrderErrorCategory.OrderApi`: un timeout se trata igual que un error del servidor.

**Qué garantiza el backend:** nada específico, es solo de la app. Contexto: `POST /orders` no es idempotente (no hay `Idempotency-Key` para pedidos). Un **timeout** pudo haber creado el pedido, y un error de **conectividad** (sin red, DNS) no.

**Cambio:**
1. `OrderErrorMapper.fromNetworkError`: `NetworkErrorKind.Timeout -> OrderErrorCategory.Timeout`, usando la categoría que se añade en F-30 paso 3.
2. `OrderSubmission`: cambiar `data object Error` por `data class Error(val reason: OrderFailure = OrderFailure.Generic)`, con `enum class OrderFailure { Network, Timeout, Validation, Generic }` (`Validation` lo usa F-33). Hay que actualizar:
   - el `when` de `onDismissRequest` (`CartViewModel.kt:137`) a `is OrderSubmission.Error`;
   - el `fallback` de `flagUnavailableLines` (`:211`);
   - los tests de `features/cart/src/test/.../CartViewModelTest.kt` que comparan con `OrderSubmission.Error` (`:365`, `:551`, `:567`).
3. `CartViewModel.placeOrder().onFailure`:
   ```kotlin
   when ((cause as? OrderException)?.category) {
       OrderErrorCategory.PriceChanged -> …                       // F-30
       OrderErrorCategory.ProductNotSellable, OrderErrorCategory.StockInsufficient,
       OrderErrorCategory.Unavailable, OrderErrorCategory.NotFound -> …   // F-32
       OrderErrorCategory.Network -> _uiState.update { it.copy(submission = OrderSubmission.Error(OrderFailure.Network)) }
       OrderErrorCategory.Timeout -> _uiState.update { it.copy(submission = OrderSubmission.Error(OrderFailure.Timeout)) }
       else -> _uiState.update { it.copy(submission = OrderSubmission.Error()) }
   }
   ```
4. Evento nuevo `CartUiEvent.RetryOrder`: `submission = Idle` y luego `placeOrder()`.
5. UI (`CartDialogContent.kt:94-104`), según `reason`:

   | Motivo | Título | Mensaje | Botón principal | Botón secundario |
   |---|---|---|---|---|
   | `Network` | `UiR.string.discovery_error_network_title` | nuevo `cart_error_network_message`: "No pudimos enviar tu pedido porque la tablet no tiene internet. Tu carrito sigue guardado; inténtalo de nuevo en un momento." | `UiR.string.discovery_action_retry` → `RetryOrder` | `cart_error_back_to_cart` = "Volver al carrito" → `DismissError` (no "Cancelar orden") |
   | `Timeout` | el actual | nuevo `cart_error_timeout_message`: "No pudimos confirmar si tu pedido se registró. Pregúntale al vendedor antes de intentarlo de nuevo." | "Entendido" → `DismissError` | ninguno |
   | `Generic` | el actual | el actual | "Entendido" | "Cancelar orden" |

6. Test: `CartViewModelTest.placeOrder_networkError_showsNetworkReason` y `retryOrder_afterNetworkError_createsOnce`.

**No hacer:** no ofrecer "Reintentar" ante un `Timeout`. El pedido pudo crearse y se duplicaría (QA vio en D:CAR-043 un pedido creado con red lenta).

**Verificación:**
- X:XRE-002: en modo avión, "Realizar pedido" muestra "Perdí la conexión…" con el mensaje de internet y "Reintentar". Con la red de vuelta, "Reintentar" hace un solo `POST /orders 201` en nginx, y el carrito sigue lleno hasta entonces.
- X:XCA-012: un producto agotado muestra el mensaje de F-32, no este.

---

##### F-37 · Los chips de Búsqueda concatenan el texto y no mandan `filters` — ALTO · NO CORREGIDO

**Qué ve QA hoy (03/11):** D:BUS-003 / D:EXP-004 (`2026-11-03-R07.md`, "[ALTO] F-37"). Se busca "taladro" y se toca el chip "Bosch".
- La petición es `{"transcribedText":"taladro marca bosch","language":"es"}`, sin `filters`.
- Salen 3 resultados: uno sin marca, uno DeWalt y uno Bosch.
- Lo esperado: `transcribedText:"taladro"` + `filters.brands:["Bosch"]` y solo productos Bosch.
- El backend está bien: con `filters.brands:["Bosch"]` devuelve 1 resultado; con Bosch + `tags:["inalambrico"]` devuelve `understood:true`, `noResults:true` y 3 alternativas.

**Qué ya está hecho:** el panel de chips, la selección y la restauración desde el historial (`HistoryEntry` con `filters`, `ProductSearchViewModel.kt:398` y `:434`). No hay nada de `filters` estructurados.

**Causa en el código:**
- `features/product-search/.../ui/ProductSearchViewModel.kt:169-172`:
  ```kotlin
  private fun refineSearch() {
      val composed = composeSearchQuery(baseQuery, selectedFilters.toList(), selectedSuggestion)
      performSearch(composed, _uiState.value.source, isRefinement = true)
  ```
- `core/domain/.../usecase/ComposeSearchQueryUseCase.kt:26`: `selectedFilters.forEach { add("${it.criterion} ${it.option}".trim().lowercase()) }` produce "taladro marca bosch".
- `core/network/.../dto/ConversationSearchDtos.kt:12-16`: `ConversationSearchRequestDto(transcribedText, sessionId, language)`, sin `filters`.
- `:45-48`: `SearchFilterDto(criterion, options)`, sin `values`.
- `ConversationSearchApiService.kt:27-46`, `ProductSearchRepository.search(query, language)` (`core/domain/.../repository/ProductSearchRepository.kt:16`) y `SearchProductsUseCase` no tienen parámetro de filtros.
- `settleResults` (`ProductSearchViewModel.kt:361-374`) reemplaza el panel con los `filters` de la respuesta refinada. El backend los calcula **sobre los resultados ya filtrados** (`vo .../service/SearchOrchestrator.java` `buildFilters`), así que con filtros reales el panel se reduciría a "Bosch" solo.
- Un refinamiento con `noResults` cae en `settleUnhappy` (`:344-345`) y se pierden los chips.

**Qué garantiza el backend:** API_REFERENCE §41, `vo .../dto/SearchFilters.java` y `FilterCriterion.java`.
- Request:

```json
POST /api/v1/conversations/search
{ "transcribedText": "taladro", "language": "es",
  "filters": { "brands": ["Bosch"], "tags": ["inalambrico"], "minPrice": 100, "maxPrice": 500 } }
```

- Reglas del filtro:
  - marcas en OR;
  - tags en OR dentro del mismo criterio y en AND entre criterios;
  - el precio se mide sobre el precio con descuento.
- Respuesta: `filters[]` trae `criterion`, `options` (etiqueta visible) y `values` (lo que se manda; `values[i]` ↔ `options[i]`).
  - Para "Marca", `values == options` y se manda en `brands`.
  - Para el resto de criterios (grupos de tags: "Voltaje", "Tipo"…), `values[i]` es el value del tag y se manda en `tags`.
- Si ningún producto cumple los filtros: `noResults:true`, `fallbackMessage` "No hay productos con esos filtros…", y en `results` **los mejores 3 del texto sin filtros**.

**Cambio:**
1. **Dominio** (`core/domain/.../model/SearchFilter.kt`):
   - `SearchFilter(criterion, options, val values: List<String> = options)`.
   - `data class StructuredSearchFilters(val brands: List<String> = emptyList(), val tags: List<String> = emptyList())`.
   - `ProductSearchRepository.search(query, language, filters: StructuredSearchFilters? = null)` y lo mismo en `SearchProductsUseCase`.
2. **Red:**
   - `ConversationSearchRequestDto` suma `val filters: SearchFiltersDto? = null`, con `@Serializable data class SearchFiltersDto(val brands: List<String>? = null, val tags: List<String>? = null)`. Con la configuración actual del `Json` (sin `encodeDefaults`), un `null` por defecto no se envía.
   - `SearchFilterDto` suma `val values: List<String> = emptyList()`. Mapearlo en `core/data/.../search/ProductMappers.kt:24-27` como `values = values.ifEmpty { options }`.
   - `ConversationSearchApiService.search(…, filters)` y `ProductSearchRepositoryImpl.kt:27-46`.
3. **ViewModel:**
   - Guardar `baseFilters: List<SearchFilter>` y `baseSuggestions` cuando una búsqueda nueva entra a resultados (`settleResults` con `!isRefinement`).
   - `refineSearch()`:
     ```kotlin
     val structured = StructuredSearchFilters(
         brands = selectedFilters.filter { it.criterion == BRAND_CRITERION }.map { it.option },
         tags = selectedFilters.filter { it.criterion != BRAND_CRITERION }.mapNotNull { sel ->
             baseFilters.firstOrNull { it.criterion == sel.criterion }?.let { f -> f.values.getOrNull(f.options.indexOf(sel.option)) }
         },
     )
     val text = composeSearchQuery(baseQuery, emptyList(), selectedSuggestion)   // la sugerencia sigue como texto
     performSearch(text, source, isRefinement = true, filters = structured.takeUnless { it.brands.isEmpty() && it.tags.isEmpty() })
     ```
     Con `BRAND_CRITERION = "Marca"`, que es el nombre fijo de vo.
   - En un refinamiento, `Results(filters = baseFilters, suggestions = baseSuggestions)`: el panel no cambia.
   - Si el refinamiento vuelve con `noResults`, no ir a `Unhappy`. Quedarse en `Results(products = emptyList())` con los chips marcados y el texto `ps_filters_no_match` = "No hay productos con esos filtros. Quita alguno para ver más." Los 3 `results` de ese caso son del texto **sin** filtros: si se muestran, que sea bajo "Otras opciones", nunca como coincidencias.
4. `ComposeSearchQueryUseCase` deja de recibir filtros (o recibe `emptyList()`). Actualizar su test.
5. Tests:
   - `ProductSearchViewModelTest.filterToggled_sendsOriginalTextAndBrands`;
   - `twoChips_noResults_keepsChipsAndPanel`;
   - `tagChip_sendsValueNotLabel`.

**No hacer:**
- No mandar la etiqueta (`options`) en `tags`: el backend filtra por el value del tag.
- No reemplazar el panel con los `filters` de la respuesta refinada.

**Verificación:**
- D:BUS-003, D:EXP-004 y X:XBU-032: "taladro" → chip "Bosch". En logcat, `{"transcribedText":"taladro",…,"filters":{"brands":["Bosch"]}}` y solo productos Bosch.
- "Bosch" + "Inalámbrico" → resultados filtrados, o "No hay productos con esos filtros" con los dos chips marcados. Nunca "no entendí" ni chips perdidos.

---

##### F-32 · No vendibles: stock sin tope y producto eliminado con el Detalle abierto — MEDIO · PARCIAL

**Qué ve QA (31/10; el 03/11 no se re-ejecutó, misma app):** `2026-10-31-R06.md`, "[MEDIO] F-32".
- **OK:**
  - un desactivado → "Producto no encontrado…", sin botones;
  - al pedir con un eliminado en el carrito → 409, la línea queda "No disponible" con aviso y "Realizar pedido" deshabilitado, sin bucle.
- **NO (X:EXP-004):** con el Detalle de "QA R06 Tmp3 broca" abierto, el producto se elimina y "Agregar al carrito" lo agrega sin aviso. El carrito no lo quita ni avisa al abrirse.
- **NO (X:XCA-004):** "QA R06 Pocas lijadora" (stock 3) muestra "Disponible", sin "Pocas unidades (quedan 3)", y el "+" llega a 6.
- El backend está bien: 409 `PRODUCT_NOT_SELLABLE` con `reason` y 422 `STOCK_INSUFFICIENT` (03/11: "disponible 3, requerido 5").

**Qué ya está hecho:**
- `IsOrderableUseCase` (`core/domain/.../usecase/IsOrderableUseCase.kt:21-22`): exige `AVAILABLE` y `active != false`.
- El Detalle deshabilita "Agregar"/"Compartir" y muestra `NotOrderableNote` (`ProductDetailContent.kt:269`, `:281`).
- `AddToCartUseCase` rechaza los no `AVAILABLE`.
- `RevalidateCartUseCase` marca las líneas tras un 400/404/409.
- La búsqueda ya no recibe no vendibles (lo filtra vo).

**Causa en el código:**
- **Sin stock en ningún DTO ni modelo:**
  - `ProductDetailResponseDto` (`ProductDetailDtos.kt:14-27`);
  - `CatalogProductDto` (`CatalogDtos.kt:25-37`);
  - `SearchProductDto` (`ConversationSearchDtos.kt:57-68`);
  - `ProductDetail` (`core/domain/.../model/ProductDetail.kt:20-31`).
- **El tope es de relleno:** en `core/domain/.../CartConstants.kt:46-51`, `"AVAILABLE" -> DEFAULT_MAX_QUANTITY` (99; `TODO(inventory-stock)`). Lo usan `CartItemMappers.kt:18` y `:33`, `ProductDetailViewModel.kt:209` y `ProductDetailContent.kt:280`.
- **La etiqueta solo mira `availability`:**
  - `ProductDetailContent.kt:267`, `StockRow(availability = detail.availability)`;
  - `:545-550` `availabilityDisplay`, donde `OUT_OF_STOCK` cae en `else -> null` y no pinta nada.
- **El carrito no se revalida al abrirse:** es el mismo hueco de F-30 paso 4. Con el Detalle ya cargado, `onAddToCart` (`ProductDetailViewModel.kt:194-199`) usa el `detail` en memoria.
- **422 cae en el genérico:** `OrderErrorMapper.kt:26`, `else -> OrderErrorCategory.OrderApi` → `CartViewModel.kt:163` → "Algo salió mal".
- **La revalidación usa `GET /products/{id}`** (`RevalidateCartUseCase.kt:34-38`, vía `ProductDetailRepository`). Con token DEVICE cada llamada suma un `PRODUCT_VIEW` falso a las métricas (`ProductController.java:97-110`).

**Qué garantiza el backend:** API_REFERENCE §39 y `OrderService.java:83-99`.
- "Vendible" = `AVAILABLE` y, si lleva stock, `> 0`. **`LOW_STOCK` no se vende desde la tablet** (manda la API sobre el plan de QA, que pedía venderlo; QA ya lo verificó contra la API).
- `stock` (null = "a consultar") llega en:
  - `GET /products` y `GET /catalog/by-category` (`ProductSummaryResponse.stock`);
  - `GET /products/{id}`;
  - los resultados de la búsqueda.
- `POST /orders`, por cada ítem y en este orden:
  - 404 `RESOURCE_NOT_FOUND`: el producto no existe;
  - 409 `{errorCode:"PRODUCT_NOT_SELLABLE", productId, productName, reason: INACTIVE|OUT_OF_STOCK|CONSULT|LOW_STOCK, detail}`;
  - 422 `{errorCode:"STOCK_INSUFFICIENT", productId, productName, available, requested}`.
- `GET /products/{id}` de un producto eliminado, con token de tablet → 404.

**Cambio:**
1. Añadir `stock: Int? = null` a los tres DTO y a `Product`/`ProductDetail`, y mapearlo en `CatalogMappers`, `ProductDetailMappers` y `search/ProductMappers.kt:11-21`. Añadir `stock: Int? = null` a `CartItem`.
2. En `CartConstants`, `fun maxQuantityFor(availability: String, stock: Int?): Int = (stock ?: maxQuantityFor(availability)).coerceIn(1, DEFAULT_MAX_QUANTITY)`. Quitar el `TODO(inventory-stock)` y usarlo en `CartItemMappers`, `ProductDetailViewModel.adjustQuantity` y `ProductDetailContent.kt:280`.
3. Detalle: `StockRow(availability, stock)` (`ProductDetailContent.kt:288-297`).
   - Si `stock in 1..5`: "Pocas unidades (quedan %d)" (nuevo `pd_availability_low_stock_count`), aunque `availability` diga `AVAILABLE`.
   - Si `stock == 0` o `OUT_OF_STOCK`: "Agotado" (nuevo `pd_availability_out_of_stock`) en color de error.
4. Al abrir el carrito, el refresco de F-30 (paso 4) cubre X:EXP-004: el eliminado no viene en `GET /products`, se quita y aparece "Quitamos de tu carrito lo que ya no está disponible: «…»". También recorta la cantidad al `stock`, con `CartNotice.QuantityCapped`.
5. Con el body de F-30 paso 1, en `placeOrder().onFailure`:
   - `ProductNotSellable` con `productId` → marcar esa línea directo (`unavailableIds += productId`, `submission = Idle`) sin la ronda de GETs.
   - `StockInsufficient` → `updateQuantity(productId, available)` (o quitar si `available == 0`) + `CartNotice.QuantityCapped(productName, available)` + `submission = Idle`.
   - Dejar `RevalidateCartUseCase` solo como respaldo cuando el body no se pueda leer, y cambiarlo para que use `CartPricingRepository.snapshots()` (`GET /products`) en vez de `GET /products/{id}` por línea.
6. Hacer lo mismo en Compartir (`CartShareErrorMapper.kt:20-26`, `CartViewModel.share()` y `ProductDetailViewModel.confirmShare()`): decidir por `errorCode`. Los 4xx ya no se reintentan.

**No hacer:**
- No sumar `LOW_STOCK` a lo vendible: el backend responde 409 `reason: LOW_STOCK`.
- No usar `availability` para el tope cuando llega `stock`.

**Verificación:**
- X:XCA-004: stock 3 → el Detalle dice "Pocas unidades (quedan 3)" y el "+" se detiene en 3.
- X:EXP-004 / XCA-031: Detalle abierto → eliminar desde el celular → "Agregar" → al abrir el carrito, la línea desaparece con el aviso y no hay ningún `POST /orders`.
- Pedir más unidades que el stock (si se fuerza) → la cantidad baja al stock con aviso, nunca "Algo salió mal".
- D:DET-013, D:CAR-023 y X:XCA-030 siguen como hoy (OK).

---

##### F-33 · Validaciones: nombre de más de 200 caracteres y 422 genérico — BAJO · PARCIAL

**Qué ve QA (31/10; el 03/11 solo re-ejecutó CAR-020, que da OK):** `2026-10-31-R06.md`, "[MEDIO] F-33".
- CAR-011: "Nombres" acepta 262 caracteres → `POST /orders 422` → "Hubo un problema generando la orden…".
- CAR-024: "el carrito acepta 25 productos distintos sin aviso ('25 productos guardados')".

**Qué ya está hecho:**
- Carrito vacío → botones deshabilitados (`features/cart/.../model/CartUiState.kt:48`, `canSubmit = !isEmpty && isOrderEnabled && !hasUnavailableLines`). CAR-020 pasa.
- **Límite de 20 líneas implementado:**
  - `CartConstants.MAX_DISTINCT_LINES = 20` (`CartConstants.kt:22`);
  - `AddToCartUseCase.kt:40`: `if (isNewLine && lines.size >= CartConstants.MAX_DISTINCT_LINES) return AddToCartResult.LineLimitReached`;
  - aviso `discovery_cart_line_limit` en Catálogo, Búsqueda y Detalle.

**QA probablemente leyó mal CAR-024.** "25 productos guardados" es la suma de **unidades**, no de líneas: el subtítulo usa `totalItemCount = items.sumOf { it.quantity }` (`Cart.kt:14`, `CartDialogContent.kt:188`). La captura `qa/regresion/qa-reports/shots/F-33_CAR-024-25lineas-tablet.png` muestra líneas con cantidades 3, 2, 1 y 2, y "Subtotal (25 productos)". Hay que re-ejecutar CAR-024 contando **líneas distintas**: la 21.ª no debe entrar y debe salir el aviso. No toques el código por esto.

**Causa en el código (lo que falta):**
- `core/domain/.../usecase/ValidateConsumerNameUseCase.kt:20-27` no tiene largo máximo. El backend exige `@Size(max = 200)` (`CreateOrderRequest.java:37`).
- Un 422 `VALIDATION_ERROR` termina en `OrderErrorCategory.OrderApi` (`OrderErrorMapper.kt:26`), que es el mensaje genérico.

**Qué garantiza el backend:**
- Límites: `customerName` ≤ 200, `items` 1..20, `quantity` 1..99.
- Un 422 trae `{errorCode:"VALIDATION_ERROR", fieldErrors:{campo: mensaje}}` (`GlobalExceptionHandler.java:80-93`).

**Cambio:**
1. `CartConstants.MAX_CONSUMER_NAME_CHARS = 200`.
2. En `CartViewModel.onNameChanged` (`CartViewModel.kt:264-267`) y `ProductDetailViewModel.onShareNameChanged` (`:153-156`), recortar la entrada: `val name = raw.take(CartConstants.MAX_CONSUMER_NAME_CHARS)`. Así no se puede escribir el carácter 201.
3. Con el body de F-30 paso 1, mapear `VALIDATION_ERROR` a `OrderErrorCategory.Validation` → `OrderSubmission.Error(OrderFailure.Validation)` con un mensaje por campo:
   - `customerName` → "Revisa tu nombre (máximo 200 caracteres).";
   - `items` → "Tu carrito está vacío o tiene demasiados productos.";
   - `customerPhone` → "Revisa tu número de celular.".
   Es una red de seguridad: con el paso 2 no debería ocurrir.

**Verificación:**
- D:CAR-011: no se pueden escribir más de 200 caracteres y el pedido da 201.
- D:CAR-024 re-ejecutado contando líneas: la 21.ª línea distinta no entra y aparece "Tu carrito ya tiene 20 productos distintos…".
- D:CAR-020 sigue OK.

---

##### F-29 · La pantalla de éxito no muestra el total grabado — BAJO · PARCIAL

**Qué ve QA hoy (03/11):** D:CAR-012 (`2026-11-03-R06.md`, "[BAJO] F-29"). "QA R06 Desc pulidora" (100, −20 %).
- El carrito muestra S/ 80.00, y el pedido 241182 queda con `total 80.00` y `unitPriceSnapshot 80.00`. El backend y el celular están bien.
- La pantalla de éxito de la tablet solo muestra producto y cantidad, sin el total.

**Qué ya está hecho:**
- El carrito usa `discountedPrice ?: price` (`CartItemMappers.kt:14`).
- El backend graba el precio con descuento (fase 38).
- `Order.total` y `OrderItem.unitPriceSnapshot` ya se mapean (`OrderMappers.kt:34-51`), pero nadie los usa.

**Causa en el código:**
- `CartViewModel.kt:151`, `val summary = state.summary()`, y `:228-230`, `OrderedLine(imageUrl, name, quantity)`, sin precio (`OrderSubmission.kt:13-17`).
- `.onSuccess {` (`:154`) ignora el `Order`.
- `SubmissionLine` (`core/ui/.../component/SubmissionScaffold.kt:45-49`) y `SubmissionSuccessContent(…)` (`:96-104`) no tienen campo de precio ni de total.

**Qué garantiza el backend:** la respuesta 201 de `POST /orders` trae `total`, `items[].unitPriceSnapshot`, `items[].lineTotal`, `items[].listPriceSnapshot` y `orderCode` (§38).

**Cambio:**
1. `OrderedLine` suma `val lineTotal: Double? = null`, y `OrderSubmission.Success` suma `val total: Double? = null` (es el mismo `Success` que se amplía en F-26).
2. En `placeOrder().onSuccess { order -> … }`, construir el resumen desde `order.items` (`productName`, `quantity`, `lineTotal`) y `total = order.total`. Para "Compartir" (sin pedido), se queda como hoy.
3. `SubmissionLine` suma `priceText: String? = null`, y `SubmissionLineRow` pinta "Cantidad: N · S/ X.XX". `SubmissionSuccessContent` suma `totalText: String? = null`, que pinta "Total del pedido  S/ X.XX" encima del botón. En `CartDialogContent.kt:81` y `:134-135`, alimentarlo con `stringResource(UiR.string.discovery_price_format, …)`.
4. Si `abs(order.total - state.totalAmount) >= 0.01`, aviso en el éxito: "El total registrado es S/ X.XX (en tu carrito viste S/ Y.YY). Confírmalo con el vendedor." Con F-30 no debería pasar.

**Verificación:** D:CAR-012 / X:XPE-007. Un producto con 20 % → pedido → el éxito muestra "Total del pedido S/ 80.00", igual al carrito y al "Total: S/ 80,00" del celular.

---

##### F-34 · Compartir sin red: mensaje genérico, no el de conexión — BAJO · PARCIAL

**Qué ve QA hoy (03/11, tarde):** D:CAR-033 (`2026-11-03-pendientes.md`, "[BAJO] F-34"). Con el carrito y los datos llenos, se pone el modo avión y se toca Compartir.
- Sale "No pudimos enviar la cotización / Hubo un problema enviando la cotización. Inténtalo de nuevo en unos momentos.". No dice que falta internet.
- "Entendido" conserva el carrito, el nombre y el celular (OK).

**Qué ya está hecho:**
- Compartir tiene su propio estado y sus textos: `OrderSubmission.ShareError` (`OrderSubmission.kt:61`) y `cart_share_error_title`/`cart_share_error_message` (`features/cart/src/main/res/values/strings.xml:66-67`).
- Solo tiene "Entendido", sin "Cancelar orden" (`CartDialogContent.kt:118-127`), y no vacía el carrito.
- `ShareQuoteUseCase` solo reintenta `Failed` (red, timeout, 5xx).
- La categoría de red llega hasta el ViewModel: `ShareOutcome.Failed(category = CartShareErrorCategory.Network)`, desde `CartShareErrorMapper.kt:28-33`.

**Causa en el código:**
- `features/cart/.../ui/CartViewModel.kt:186`: `is ShareOutcome.Rejected, is ShareOutcome.Failed -> OrderSubmission.ShareError`. La categoría `Network` se descarta.
- Lo mismo en el Detalle, `ProductDetailViewModel.kt:184`: `is ShareOutcome.Rejected, is ShareOutcome.Failed -> ProductDetailUiState.ShareSubmission.Error`.

**Qué garantiza el backend:** nada, es solo de la app.

**Cambio:**
1. `OrderSubmission.ShareError` pasa a `data class ShareError(val network: Boolean = false)`. Actualizar el `when` de `CartViewModel.kt:137` y los tests.
2. En `CartViewModel.share()`, `is ShareOutcome.Failed -> OrderSubmission.ShareError(network = outcome.category == CartShareErrorCategory.Network)`, y `is ShareOutcome.Rejected -> OrderSubmission.ShareError()`.
3. En `CartDialogContent.kt:118-127`, `message = if (submission.network) stringResource(R.string.cart_share_error_network_message) else stringResource(R.string.cart_share_error_message)`, con el string nuevo "No pudimos enviar la cotización porque la tablet no tiene internet. Tu carrito sigue guardado; inténtalo de nuevo en un momento.".
4. Lo mismo en el Detalle: `ShareSubmission.Error(network: Boolean)` y su texto en `features/product-detail/src/main/res/values/strings.xml`.

**Verificación:** D:CAR-033. Sin red → Compartir → "No pudimos enviar la cotización" con el mensaje de internet → "Entendido". El diálogo vuelve al carrito, el badge conserva los ítems y siguen el nombre y el celular.

---

##### F-35 · El carrito quieto se cierra a los ~50 s, no a los 3 min — BAJO · PARCIAL

**Qué ve QA hoy (03/11, tarde):** D:CAR-042 (`2026-11-03-pendientes.md`, "[BAJO] F-35").
- Escribiendo una letra cada 9 s, el carrito no se vacía (OK).
- Quieto desde la última tecla, el diálogo se cierra a los ~50 s, vuelve a Home y el carrito queda vacío. Se pedían 3 min. QA registró la última tecla a las 16:45:47 y lo encontró cerrado entre las 16:46:00 y las 16:46:40.
- Además, **el teclado queda abierto sobre Home**.
- X:XPE-040: ningún `POST /orders` con `items` vacío (OK).

**Qué ya está hecho:**
- El diálogo informa sus toques: `CartDialog.kt:80`, `Modifier.fillMaxSize().reportUserActivity()`.
- Escribir cuenta como actividad: `DiscoveryTextField` llama a `LocalUserActivity` en `onValueChange`.
- `app/.../MainActivity.kt:179` provee `LocalUserActivity provides mainViewModel::onUserActivity`.
- El cierre por inactividad cierra el carrito (`MainActivity.kt:316-320`).

**Causa en el código:**
- Hay un solo tope para todo. En `app/.../MainViewModel.kt:136`, `delay(CartConstants.INACTIVITY_TIMEOUT_SECONDS * 1_000L)`, con `INACTIVITY_TIMEOUT_SECONDS: Long = 60` (`CartConstants.kt:15`).
- `MainViewModel` no sabe si el diálogo del carrito está abierto: `combine(currentRoute, basketHasItems, activityTick)` (`:132`).
- Los "~50 s" son esos 60 s contados desde la última interacción que la app registró. No es otro timer. Hay que medirlo de nuevo con el arreglo.
- Teclado: el recolector `sessionTimeout` (`MainActivity.kt:316-320`) cierra el diálogo y vuelve a Home, pero no oculta el IME. La causa exacta de que el IME quede visible no está confirmada en el código, así que conviene ocultarlo explícitamente.

**Qué garantiza el backend:** nada, es solo de la app.

**Cambio:**
1. `CartConstants.CART_DIALOG_INACTIVITY_SECONDS: Long = 180`.
2. En `MainViewModel`:
   ```kotlin
   private val cartDialogOpen = MutableStateFlow(false)
   fun onCartDialogVisibilityChanged(open: Boolean) { cartDialogOpen.value = open; onUserActivity() }
   // startInactivityTimer (:129-141):
   combine(currentRoute, basketHasItems, activityTick, cartDialogOpen) { route, hasItems, _, cartOpen ->
       if (!isSessionTimed(route, hasItems)) null
       else if (cartOpen) CartConstants.CART_DIALOG_INACTIVITY_SECONDS else CartConstants.INACTIVITY_TIMEOUT_SECONDS
   }.collectLatest { seconds ->
       if (seconds == null) return@collectLatest
       delay(seconds * 1_000L); endCustomerSession(); _sessionTimeout.emit(Unit)
   }
   ```
3. En `MainActivity.DiscoveryNavHost`, `LaunchedEffect(showCart) { mainViewModel.onCartDialogVisibilityChanged(showCart) }`. `showCart` está en `MainActivity.kt:278`.
4. Teclado: en el recolector de `sessionTimeout` (`MainActivity.kt:316`), antes de cerrar, `keyboardController?.hide()` (`LocalSoftwareKeyboardController.current`) y `focusManager.clearFocus(force = true)` (`LocalFocusManager.current`).
5. Esto también deja viva la pantalla de éxito el tiempo que necesita F-26. Opcional: el mismo tope para `ShareProductDialog` del Detalle.
6. Tests en `MainViewModelTest`: `cartDialogOpen_usesLongerBudget` y `cartDialogClosed_uses60s`.

**Verificación:**
- D:CAR-042: escribir con pausas más de 60 s → no se vacía. Dejar el diálogo quieto → se cierra y se vacía a los 180 s (±5), y Home queda **sin teclado**.
- X:XPE-040: ningún `POST /orders {"items":[]}`.

---

##### F-20 · HTTP plano (la parte de logs está corregida) — BAJO · BLOQUEADO (infra: dominio + TLS)

**Qué ve QA hoy (03/11):** `2026-11-03-R12.md`, "F-20".
- El buffer de logcat de la tablet solo tenía heartbeats.
- El 31/10, con la misma app, quedó **corregido** en logs (`2026-10-31-R12.md`): el body de `POST /orders` y `share-pdf` sale con `"customerName":"***","customerPhone":"***"`, sin Bearer, sin `deviceAccessToken` y sin token de STT.
- Las dos apps siguen en `http://87.99.156.164`.

**Qué ya está hecho:**
- `core/network/.../KtorClientFactory.kt:56-67`: `LogRedactor.redact(message)` y `sanitizeHeader { header -> header == HttpHeaders.Authorization }`.
- `core/network/.../LogRedactor.kt:19-28` enmascara `customerName`, `customerPhone`, `phone`, `deviceAccessToken`, `accessToken`, `token`, `key` y `exitPinHash`.

**Causa en el código (lo que falta, bloqueado):**
- `core/network/build.gradle.kts:15`: `buildConfigField("String", "QHATHIA_BASE_URL", "\"http://87.99.156.164\"")`.
- `app/src/main/AndroidManifest.xml:27`: `android:usesCleartextTraffic="true"`.
- Relacionado, y no bloqueado: `AndroidManifest.xml:20`, `android:allowBackup="true"`. El DataStore con el token DEVICE y el hash del PIN entra en los backups.

**Qué garantiza el backend:** nada todavía. Infra no publicó dominio ni TLS (`https://87.99.156.164` no responde). Es la línea "Ojo" del plan R12.

**Cambio** (preparar ahora, activar cuando Infra dé el dominio):
1. `app/src/main/res/xml/network_security_config.xml` con `<base-config cleartextTrafficPermitted="false"/>`, y `app/src/debug/res/xml/network_security_config.xml` que permita cleartext solo a `87.99.156.164` y `10.0.2.2`.
2. En el manifest, quitar `usesCleartextTraffic` y poner `android:networkSecurityConfig="@xml/network_security_config"`. **Solo cuando exista el dominio:** antes de eso, release dejaría de conectar.
3. `QHATHIA_BASE_URL` por build type: `https://<dominio>` en `release`/`profile`. `PairingSocketClient` ya deriva `wss://` de una base `https://`.
4. Independiente del bloqueo: `android:allowBackup="false"`, o excluir `datastore/` en `app/src/main/res/xml/backup_rules.xml` y `data_extraction_rules.xml`.

**Verificación:**
- D:RED-060 ya pasa: `adb logcat -s KtorClient` no muestra tokens ni datos del cliente.
- D:RED-061, cuando haya dominio: build de release, todo el tráfico por `https://`. Una petición `http://` falla con `CLEARTEXT communication … not permitted`.

---

##### F-27 · Cotización por WhatsApp — CORREGIDO en la tablet · entrega BLOQUEADA (Meta)

Confirmado el 03/11 (`2026-11-03-R06.md`, "Corregidos"):
- Con `share-pdf` → `201 {status: FAILED}`, la tablet dice "Se generó tu pedido. Acércate al vendedor…" y hace un solo `share-pdf`.
- "Compartir" dice "No pudimos enviar la cotización…" y nunca "¡Cotización enviada!".
- Implementado con `ShareOutcome.Delivered` solo para `SENT` (`core/domain/.../model/ShareOutcome.kt:11`).

No hay que tocar nada en la app. Sigue bloqueada la entrega real: Meta responde `400 … Object with ID '…' does not exist … error_subcode 33` (el id del número de WhatsApp no existe o perdió permisos), y es tarea de infra. Tampoco se implementó en biz el `delivered/errorCode` en `share-pdf` (línea "Ojo" del plan R06).

---

## 3 · Staff (celular)

Repo `android/qhathia-mobile-staff`, commit `2c6aa0f`.

### 3.1 · Vinculación, Dispositivos y comandos, login, sesión y seguridad (R01, R02, R08)

**Para:** la IA que implementa en `qhathia-mobile-staff`.
**Alcance:** F-01, F-58, F-59, F-60, F-62 (bloque R01) · F-09, F-61, F-63, F-65 (R02) · F-48, F-49, F-50, F-51, F-52, F-53, F-73, F-67, F-70 (R08). Solo el lado celular.
**Versión probada:** Staff 1.37.0-SNAPSHOT, commit `2c6aa0f`, contra el backend master `a2da54b` (fases 35–43) en producción. QA 03/11: `qa/regresion/qa-reports/2026-11-03-R01.md`, `-R02.md`, `-R08.md` y `-pendientes.md`.

#### Antes de empezar: lo que hay que saber

1. **Del traspaso anterior no se implementó nada de estas 18 claves.** Entre `0ca3de8` (el commit sobre el que se escribió `qa/consolidado/fixes/fixes-staff-A.md`) y `2c6aa0f` solo entraron tres cambios: precio con descuento en el detalle, cuenta regresiva del descuento y ejemplos en "Productos no encontrados". `git diff --stat 0ca3de8 2c6aa0f` no toca `device-linking`, `auth`, `profile`, `core/network`, `MainViewModel` ni `QhathiaTextField`. Las rutas y líneas de `fixes-staff-A.md` siguen valiendo, y las de aquí las volví a comprobar en `2c6aa0f`.
2. **Manda el contrato real (`docs/api/API_REFERENCE.md` §35–37 y §42), no lo que proponían los fixes.** Donde difieren:
   - Contraseña actual incorrecta: sigue siendo **401**, con `errorCode: CURRENT_PASSWORD_INCORRECT`, no 400/422.
   - Sesiones: hay un **tope de 20 por usuario**, así que no hace falta paginar en la app.
   - Canje de la tablet: se ve en `GET /devices/pairing-session/{id}/status` → `tokenExchanged`. No existe `pairingExchangedAt` en el device.
   - No hay 409 al acusar un comando vencido. El celular decide con `status`, `ackedAt` y `lastPingAt`.
   - El nombre de la tablet no viaja en el heartbeat. Se pone al aprobar o con `PATCH /devices/{id}`.
3. **Prefijos de ruta** (relativos a la raíz del repo):
   - `app/` = `app/src/main/java/com/qhathia/mobile/staff/`
   - `core/<m>/` = `core/<m>/src/main/java/com/qhathia/mobile/staff/core/<m>/`
   - `dl/` = `features/device-linking/src/main/java/com/qhathia/mobile/staff/feature/devicelinking/`
   - `auth/` = `features/auth/src/main/java/com/qhathia/mobile/staff/feature/auth/`
   - `profile/` = `features/profile/src/main/java/com/qhathia/mobile/staff/feature/profile/`
   - Recursos: `features/<f>/src/main/res/values/strings.xml` (español, el idioma por defecto) y `values-en/strings.xml` (inglés). Todo texto nuevo va a los dos.
4. **Patrones del repo que hay que seguir:**
   - MVI: `UiState` inmutable, `UiEvent` sellado y `UiEffect` por `MutableSharedFlow(extraBufferCapacity = 1, DROP_OLDEST)`.
   - Los repositorios convierten `NetworkResult` en `Result` con una excepción de dominio tipada (`PairDeviceException`, `DeviceSettingsException`, `ChangePasswordException`).
   - Tests con JUnit 5 + `MainDispatcherExtension` + fakes de `core/testing` (`FakeDeviceRepository`, `MutableClock`…). Si cambias una interfaz de repositorio, actualiza su fake.
5. **Dos piezas comunes** (abajo, P-1 y P-2) las usan varios arreglos. Hazlas primero.

---

#### P-1 · Leer el `errorCode` del ProblemDetail en cualquier servicio (lo usan F-58, F-60, F-62 y F-48)

**Hoy:** `core/network/NetworkResult.kt:11` es `data class HttpError(val code: Int, val message: String)`, y `toNetworkResult()` (`:16-27`) descarta el cuerpo. Solo los pedidos leen el ProblemDetail (`core/network/service/OrderApiService.kt:92-94`, `parseProblem()`), con un DTO propio. Por eso el celular no distingue `INVALID_PAIRING_TOKEN` de `PAIRING_TOKEN_EXPIRED` ni de `CURRENT_PASSWORD_INCORRECT`.

**Backend:** todos los errores son `application/problem+json` con `errorCode` y `detail` en español, incluidos los 401/403 del filtro (API_REFERENCE §42).

**Cambio:**
1. `core/network/NetworkResult.kt`: ampliar sin romper a quienes ya lo usan:
   ```kotlin
   data class HttpError(
       val code: Int,
       val message: String,
       val errorCode: String? = null,
       val detail: String? = null,
   ) : NetworkResult<Nothing>()
   ```
2. Archivo nuevo `core/network/ProblemDetail.kt`, que copia el patrón de `OrderApiService.parseProblem()`:
   ```kotlin
   @Serializable
   data class ProblemDto(val errorCode: String? = null, val detail: String? = null)

   /** Como toNetworkResult(), pero lee (best effort) el ProblemDetail de un error HTTP. */
   suspend fun <T> Result<T>.toNetworkResultWithProblem(json: Json): NetworkResult<T> = fold(
       onSuccess = { NetworkResult.Success(it) },
       onFailure = { t ->
           when (t) {
               is ResponseException -> {
                   val problem = runCatching {
                       json.decodeFromString<ProblemDto>(t.response.bodyAsText())
                   }.getOrNull()
                   NetworkResult.HttpError(
                       code = t.response.status.value,
                       message = t.response.status.description,
                       errorCode = problem?.errorCode,
                       detail = problem?.detail,
                   )
               }
               else -> NetworkResult.NetworkError(t)
           }
       },
   )
   ```
   `Result.fold` es `inline`, así que se puede llamar a `bodyAsText()` (suspend) dentro de la lambda.
3. `core/network/di/NetworkModule.kt`: `provideDeviceApiService` (`:91-94`) y `provideAuthApiService` (`:59-64`) reciben `json: Json` y lo pasan al constructor de `DeviceApiService` y `AuthApiService`, igual que ya se hace con `OrderApiService` (`:79-84`).
4. En `DeviceApiService` y `AuthApiService`, cambiar `.toNetworkResult()` por `.toNetworkResultWithProblem(json)` en las llamadas que lo necesitan: `pairDevice`, `unlinkDevice`, `renameDevice` (nueva), `getPairingStatus` (nueva) y `changePassword`.

---

#### P-2 · Dispositivos se refresca sola mientras está visible (base de F-65, F-09, F-61 y F-01)

**Hoy:**
- `dl/ui/DeviceLinkingViewModel.kt:129-150`: `loadDevices()` solo corre en `init`, en `Retry` y tras vincular.
- `ensurePolling()` (`:191-212`) solo existe mientras hay comandos en vuelo, solo relee comandos (nunca `GET /devices`) y fusiona en vez de reemplazar (`:205`).
- `ScreenStarted` (`:81-84`) no recarga. `LifecycleStartEffect` (`dl/ui/DeviceLinkingScreen.kt:57-60`) sí avisa al volver de Ajustes, pero el VM solo arranca el polling.

**Cambio en `DeviceLinkingViewModel`:**
1. Un solo bucle mientras la pantalla está visible:
   ```kotlin
   private var lastDevicesLoadAt: Instant? = null

   private fun ensurePolling() {
       if (!isScreenStarted || pollJob?.isActive == true) return
       pollJob = viewModelScope.launch {
           while (isActive) {
               val fast = _uiState.value.needsFastPolling          // comandos en vuelo o reinicio esperando latido (F-09)
               delay((if (fast) COMMAND_POLL_INTERVAL else DEVICES_REFRESH_INTERVAL).toMillis())
               refreshSilently()
           }
       }
   }

   /** GET /devices + comandos de las tablets que lo necesitan, sin spinner; si falla, se queda lo que había. */
   private suspend fun refreshSilently() {
       val now = clock.instant()
       getDevicesUseCase().onSuccess { devices ->
           lastDevicesLoadAt = now
           _uiState.update { it.copy(devices = devices.toImmutableList(), now = now) }
       }
       val ids = _uiState.value.deviceIdsNeedingCommandPoll   // en vuelo, o RESTART ACKED esperando latido
       val results = ids.map { id -> viewModelScope.async { id to getDeviceCommandsUseCase(id) } }.awaitAll()
       _uiState.update { state ->
           val replaced = state.commands.toMutableMap()
           results.forEach { (id, r) -> r.onSuccess { replaced[id] = it.latestPerType(now) } }  // REEMPLAZA (F-61)
           state.copy(commands = replaced, now = now)
       }
   }
   ```
   Constantes: `COMMAND_POLL_INTERVAL = 15 s` (ya existe) y `DEVICES_REFRESH_INTERVAL = 30 s` (nueva).
2. `ScreenStarted`: además de `ensurePolling()`, `if (lastDevicesLoadAt?.let { Duration.between(it, clock.instant()) >= Duration.ofSeconds(5) } == true) viewModelScope.launch { refreshSilently() }`. Cubre volver de Ajustes y volver del fondo, y no duplica la carga inicial. `loadDevices()` también debe fijar `lastDevicesLoadAt`.
3. `loadCommands` (`:152-165`) ya reemplaza por dispositivo. Déjalo así.
4. `DeviceUnlinked` (`:100-109`): además de filtrar la tarjeta, `viewModelScope.launch { refreshSilently() }`.
5. `DeviceLinkingUiState` (`dl/model/DeviceLinkingUiState.kt:44-45`): agregar `needsFastPolling` y `deviceIdsNeedingCommandPoll`. Los dos son `hasCommandsInFlight` ampliado con el caso "RESTART ACKED esperando latido" de F-09.

**Coste:** con 1–3 tablets es un `GET /devices` cada 30 s, y los comandos solo de las que tienen algo pendiente. Se corta en `ScreenStopped` (`:85-88`), como ahora.

---

#### ALTO

##### F-60 · Tras aprobar no se comprueba que la tablet canjeó; no hay "Rechazar" — ALTO · NO CORREGIDO

(Arreglo PARCIAL: backend corregido, celular no.)

**Qué ve QA hoy (03/11):** X:XVI-010 (R01, F-60).
- Pasos: un código EVWMB que nadie canjea, tecleado en el celular → "Vincular" (07:01:40).
- Resultado: el diálogo se cierra y aparece "Tablet 3 · Inactivo" sin ningún aviso a los 5, 15, 30 y 40 s. nginx no registra ninguna llamada a `/status`.
- El diálogo solo tiene el campo y "Vincular". El REJECT (X:XVI-020) solo se pudo probar por curl, y ahí la tablet mostró "El comercio rechazó la vinculación".
- Esperado: aviso "la tablet no respondió" en ≤ 30 s, opción de deshacer, sin tarjeta huérfana, y un botón Rechazar.

**Qué ya está hecho:** nada.
- `core/network/dto/DeviceDtos.kt:47-55`: `PairDeviceResponseDto` ya deserializa `sessionId`, `status` y `deviceId`, pero el repositorio los tira.

**Causa en el código:**
- `dl/ui/DeviceLinkingViewModel.kt:219-225` trata el 200 como "vinculada":
  ```kotlin
  pairDeviceUseCase(code)
      .onSuccess {
          _uiState.update {
              it.copy(isPairDialogVisible = false, isPairing = false, pairCode = "")
          }
          loadDevices()
  ```
  Ese 200 solo significa "aprobado". La tablet canjea después, y si no lo hace queda un device `ACTIVE` sin dueño. La ventana de canje es de 10 min; pasada, el canje da 410 y el device queda huérfano para siempre.
- `core/data/repository/DeviceRepositoryImpl.kt:46-48`: `is NetworkResult.Success -> Result.success(Unit)`. Descarta `deviceId` y `sessionId`.
- `DeviceRepositoryImpl.kt:103`: `DECISION_APPROVE = "APPROVE"` es la única decisión. `dl/ui/PairDeviceDialog.kt:96-101` tiene un solo `QhathiaButton` ("Vincular").

**Qué garantiza el backend** (API_REFERENCE §37):
- `POST /api/v1/devices/pair` (OWNER/ADMIN). Body mínimo para aprobar: `{"pairingToken":"CXZC7","decision":"APPROVE"}`. `deviceName`, `platform` y `appVersion` son opcionales. Respuesta 200:
  ```json
  {"sessionId":"…","status":"APPROVED","storeId":"…","deviceId":"…","deviceName":"Tablet 3","platform":"ANDROID"}
  ```
- Rechazar: `{"pairingToken":"CXZC7","decision":"REJECT"}` → 200 `{"sessionId":"…","status":"REJECTED","storeId":"…"}`. La tablet muestra "El comercio rechazó la vinculación".
- `GET /api/v1/devices/pairing-session/{sessionId}/status` es público y de solo lectura (no consume el canje). Responde:
  ```json
  {"sessionId":"…","status":"APPROVED","expiresAt":"…","tokenExchanged":false,"tokenExchangedAt":null,"deviceId":"…"}
  ```
  - 404 si la sesión no existe.
  - La guía del backend: "tras aprobar, consultarlo unos 30 s; si `tokenExchanged` sigue en `false`, avisar y ofrecer desvincular".
  - Con la tablet bien, el canje llega en ≤ 1 s (R01, VIN-031).
- Deshacer: `POST /api/v1/devices/{deviceId}/unlink` → 200, o 404 si ya no está (§35).

**Cambio:**
1. **Dominio** (`core/domain/model/`):
   ```kotlin
   data class PairedDevice(val deviceId: String, val sessionId: String, val name: String?)
   data class PairingSessionStatus(val status: String, val tokenExchanged: Boolean)
   ```
   En `DeviceRepository` (`core/domain/repository/DeviceRepository.kt:22`):
   - `pairDevice(code: String, name: String?): Result<PairedDevice>`
   - `rejectPairing(code: String): Result<Unit>` (nuevo)
   - `getPairingStatus(sessionId: String): Result<PairingSessionStatus>` (nuevo)

   Casos de uso:
   - `PairDeviceUseCase(code, name)`
   - `RejectPairingUseCase` (nuevo)
   - `GetPairingStatusUseCase` (nuevo)

   Actualiza `core/testing/FakeDeviceRepository.kt` (`pairResult` pasa a `Result<PairedDevice>`, más `rejectResult` y `statusResults: ArrayDeque<Result<PairingSessionStatus>>`).
2. **Red** (`core/network/`):
   - `PairDeviceRequestDto` (`dto/DeviceDtos.kt:34-41`): `deviceName`, `platform` y `appVersion` pasan a `String? = null`. El `Json` de `NetworkModule.kt:37-42` no tiene `encodeDefaults`, así que los `null` por defecto no se envían.
   - DTO nuevo `PairingSessionStatusDto(sessionId: String? = null, status: String? = null, expiresAt: String? = null, tokenExchanged: Boolean = false, tokenExchangedAt: String? = null, deviceId: String? = null)`.
   - `DeviceApiService.getPairingStatus(sessionId)` = `client.get("api/v1/devices/pairing-session/$sessionId/status")`. El cliente autenticado vale: el endpoint es público y el Bearer no molesta.
3. **Repositorio** `DeviceRepositoryImpl.pairDevice` (`:38-54`):
   - Body `PairDeviceRequestDto(pairingToken = code, decision = "APPROVE", deviceName = name)`. No mandes `appVersion "1.0.0"` (F-59).
   - En `Success`: `PairedDevice(data.deviceId!!, data.sessionId!!, data.deviceName)`. Si falta alguno → `Result.failure(PairDeviceException(Unknown))`.

   `rejectPairing`: el mismo endpoint con `decision = "REJECT"` y nada más. Mapea los errores igual que F-58.
4. **Estado** (`dl/model/DeviceLinkingUiState.kt`):
   ```kotlin
   sealed interface PairingPhase {
       data object EnteringCode : PairingPhase
       data class WaitingForTablet(val paired: PairedDevice) : PairingPhase
       data class TabletDidNotRespond(val paired: PairedDevice) : PairingPhase
   }
   // en DeviceLinkingUiState:
   val pairingPhase: PairingPhase = PairingPhase.EnteringCode,
   val isRejecting: Boolean = false,
   ```
   Eventos nuevos en `DeviceLinkingUiEvent.kt`: `RejectPair`, `KeepWaiting`, `UndoPair`. Efectos nuevos: `ShowPaired(name: String?)`, `ShowPairRejected` y `ShowPairUndone`.
5. **VM** `submitPair()` (`dl/ui/DeviceLinkingViewModel.kt:214-232`): en éxito, no cerrar el diálogo.
   ```kotlin
   .onSuccess { paired -> waitForTablet(paired) }

   private fun waitForTablet(paired: PairedDevice) {
       pairWaitJob?.cancel()
       _uiState.update { it.copy(isPairing = false, pairingPhase = PairingPhase.WaitingForTablet(paired)) }
       pairWaitJob = viewModelScope.launch {
           repeat(PAIR_STATUS_MAX_POLLS) {                       // 15 × 2 s = 30 s
               if (getPairingStatusUseCase(paired.sessionId).getOrNull()?.tokenExchanged == true) {
                   _uiState.update { it.copy(isPairDialogVisible = false, pairingPhase = PairingPhase.EnteringCode, pairCode = "", pairName = "") }
                   _uiEffect.tryEmit(DeviceLinkingUiEffect.ShowPaired(paired.name))
                   loadDevices()
                   delay(FIRST_HEARTBEAT_GRACE.toMillis())        // 3 s: el primer latido sale junto al canje
                   refreshSilently()                              // P-2: la tarjeta pasa a "Activo"
                   return@launch
               }
               delay(PAIR_STATUS_POLL.toMillis())                 // 2 s
           }
           _uiState.update { it.copy(pairingPhase = PairingPhase.TabletDidNotRespond(paired)) }
       }
   }
   ```
   - `KeepWaiting` → `waitForTablet(paired)` otra vez.
   - `UndoPair` → `unlinkDeviceUseCase(paired.deviceId)` (inyectar `UnlinkDeviceUseCase`). Tanto el éxito como el 404 cierran el diálogo, emiten `ShowPairUndone` y llaman a `loadDevices()`. Un fallo de red deja el diálogo con "Sin conexión. Inténtalo nuevamente.".
   - `RejectPair` → `rejectPairingUseCase(pairCode)`. En éxito, cerrar el diálogo y emitir `ShowPairRejected`; en fallo, el mismo `pairErrorRes` de F-58.
   - `DismissPairDialog` (`:114-122`) no hace nada si la fase no es `EnteringCode`, y siempre cancela `pairWaitJob`.
6. **UI** `dl/ui/PairDeviceDialog.kt`: recibe `phase` y pinta tres variantes.
   - `EnteringCode`: lo de hoy (más F-58/F-59), y un `TextButton` "Rechazar" (`device_pair_reject`) habilitado con el mismo `canSubmit`.
   - `WaitingForTablet`: `CircularProgressIndicator` + "Esperando a que la tablet confirme…" (`device_pair_waiting`), sin X.
   - `TabletDidNotRespond`: "La tablet no recibió la aprobación. Revisa que siga mostrando el código y que tenga internet." (`device_pair_not_responding`), con primario "Deshacer vinculación" (`device_pair_undo`) y secundario "Seguir esperando" (`device_pair_keep_waiting`).

   Mientras la fase no sea `EnteringCode`: `DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)`.
7. **Snackbars en `DeviceLinkingScreen.kt:41-50`:**
   - "Tablet «%1$s» vinculada" (`device_pair_success`).
   - "Rechazaste la vinculación. La tablet mostrará un aviso." (`device_pair_rejected`).
   - "Vinculación deshecha. Pide un código nuevo en la tablet." (`device_pair_undone`).
8. **Tests** (`DeviceLinkingViewModelTest`):
   - `submitPair_whenTokenNeverExchanged_showsNotResponding`
   - `submitPair_whenExchanged_closesDialogAndReloads`
   - `undoPair_unlinksPairedDevice`
   - `rejectPair_postsRejectDecision`

**No hacer:**
- No esperar el canje leyendo `GET /devices[].lastPingAt`. Es indirecto y no distingue "no canjeó" de "canjeó pero aún no latió".
- No borrar el device por tu cuenta si el usuario no elige "Deshacer": "Seguir esperando" es legítimo.

**Verificación:**
- X:XVI-010: código tecleado sin que la tablet canjee → "La tablet no recibió la aprobación" a los ≤ 30 s → "Deshacer" → `POST /devices/{id}/unlink 200` y ninguna tarjeta nueva. nginx muestra `GET …/status` cada 2 s.
- X:XVI-020: "Rechazar" → `POST /devices/pair` con `"decision":"REJECT"` → la tablet muestra "El comercio rechazó la vinculación".
- X:XVI-001: aprobar dentro de 60 s → snackbar "vinculada" en ≤ 5 s.

---

##### F-58 · El campo del código no normaliza ni valida, y los errores salen todos iguales — ALTO · NO CORREGIDO

(Arreglo PARCIAL: backend corregido, celular no.)

**Qué ve QA hoy (03/11):** S:DIS-030, S:DIS-024 y X:XVI-021 (R01, F-58).
- El campo muestra minúsculas.
- `abcd` (4 caracteres) se envía: `POST /devices/pair` → 400 a las 06:58:40.
- `o0i1l` se escribe y se envía.
- Inexistente (`zzzzz`), vencido (`jwwgp`) y usado (`9evqu`) muestran el mismo "Código inválido o expirado.".
- Esperado: MAYÚSCULAS en el campo, "Vincular" deshabilitado con 4 caracteres, O/0/I/1/L que no se escriben y tres mensajes distintos.

**Qué ya está hecho:** nada en la app. Un código válido en minúsculas ya vincula, pero solo porque **el backend normaliza** (`cqrqc` → 200), no por la app.

**Causa en el código:**
- `dl/ui/DeviceLinkingViewModel.kt:123-124`, `PairCodeChanged` guarda el texto tal cual:
  ```kotlin
  _uiState.update { it.copy(pairCode = event.code, pairErrorRes = null) }
  ```
- `dl/model/DeviceLinkingUiState.kt:35`: `val canSubmitPair: Boolean get() = pairCode.isNotBlank() && !isPairing`. Acepta cualquier longitud.
- `dl/ui/PairDeviceDialog.kt:87-90`: `KeyboardCapitalization.Characters` es solo una sugerencia al teclado (Gboard escribe en minúsculas igual). No hay filtro, tope ni `keyboardActions`.
- `core/data/repository/DeviceRepositoryImpl.kt:96-100`: `400 -> PairDeviceError.InvalidCode` para todo, por status.
- `dl/ui/DeviceLinkingViewModel.kt:234-238`: solo `InvalidCode` tiene texto propio.
- `core/domain/model/PairDeviceError.kt:9` tiene un único `InvalidCode`.

**Qué garantiza el backend** (API_REFERENCE §37; `services/auth-api-service/.../exception/VitriaException.java`):
- Normaliza (sin espacios ni guiones, mayúsculas). Si el código no tiene 5 caracteres de `23456789ABCDEFGHJKMNPQRSTUVWXYZ` (`application.yml`, `vitria.devices.pairing.code.alphabet`), responde 400 `INVALID_PAIRING_TOKEN` con un `detail` que lo explica.
- `errorCode` en `POST /devices/pair`:

  | HTTP | `errorCode` | `detail` del servidor |
  |---|---|---|
  | 400 | `INVALID_PAIRING_TOKEN` | "El código de vinculación no es válido." (o el de longitud/alfabeto) |
  | 400 | `PAIRING_TOKEN_EXPIRED` | "El código de vinculación ha expirado. Genera uno nuevo." |
  | 400 | `PAIRING_TOKEN_ALREADY_USED` | "El código de vinculación ya fue utilizado." |
  | 400 | `PAIRING_TOKEN_CANCELLED` | "La tablet descartó este código. Ingresa el código que muestra ahora." |
  | 409 | `CATALOG_THRESHOLD_NOT_MET` | "Necesitas al menos N productos activos con nombre, precio y foto…" |
  | 503 | — | no se pudo verificar el catálogo (fail-closed) |

**Cambio:**
1. Constantes en `dl/model/DeviceLinkingUiState.kt`: `const val PAIRING_CODE_LENGTH = 5` y `const val PAIRING_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"`. Son los mismos valores que el backend.
2. VM `PairCodeChanged`:
   ```kotlin
   val normalized = event.code.uppercase(Locale.ROOT).filter { it in PAIRING_ALPHABET }.take(PAIRING_CODE_LENGTH)
   _uiState.update { it.copy(pairCode = normalized, pairErrorRes = null) }
   ```
3. `canSubmitPair = pairCode.length == PAIRING_CODE_LENGTH && !isPairing && !isRejecting`.
4. `PairDeviceDialog`:
   - `KeyboardOptions(capitalization = Characters, keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, imeAction = ImeAction.Done)`.
   - `keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit() })` (requiere el parámetro nuevo de `QhathiaTextField`, F-50).
   - `helperText` "5 caracteres, sin O, 0, I, L ni 1 · 3/5" (`device_pair_code_helper`, con `%1$d/5`).
5. `PairDeviceError`: reemplazar `InvalidCode` por `InvalidCode`, `ExpiredCode`, `AlreadyUsedCode`, `CancelledCode`, `CatalogNotReady(val detail: String?)`, `CatalogCheckUnavailable`, `Network` y `Unknown`. `DeviceRepositoryImpl.toPairError(code, errorCode, detail)`, usando P-1:
   ```kotlin
   when {
       errorCode == "PAIRING_TOKEN_EXPIRED" -> ExpiredCode
       errorCode == "PAIRING_TOKEN_ALREADY_USED" -> AlreadyUsedCode
       errorCode == "PAIRING_TOKEN_CANCELLED" -> CancelledCode
       errorCode == "CATALOG_THRESHOLD_NOT_MET" || code == 409 -> CatalogNotReady(detail)
       code == 503 -> CatalogCheckUnavailable
       code == 400 -> InvalidCode
       else -> Unknown("HTTP $code")
   }
   ```
   Además, `pairingToken = code.trim().uppercase(Locale.ROOT)`, como defensa.
6. Textos (es; en `values-en` la traducción):
   - `device_pair_error_invalid_code`: "Ese código no existe. Revisa que sea el que muestra la tablet."
   - `device_pair_error_expired`: "El código venció. En la tablet toca «Generar nuevo código»."
   - `device_pair_error_used`: "Ese código ya se usó. Pide uno nuevo en la tablet."
   - `device_pair_error_cancelled`: "La tablet descartó ese código. Ingresa el que muestra ahora."
   - `device_pair_error_catalog`: "Tu catálogo aún no tiene suficientes productos con nombre, precio y foto para vincular una tablet."
   - `device_pair_error_catalog_unavailable`: "No pudimos verificar tu catálogo. Inténtalo en unos minutos."
   - `device_pair_error_network`: "Sin conexión. Inténtalo nuevamente."

   `toPairErrorRes()` (`:234-238`) los mapea uno a uno.
7. Tests: `pairCodeChanged_lowercase_isUppercased`, `pairCodeChanged_ambiguousChars_areDropped`, `canSubmit_requiresFiveChars` y, en `DeviceRepositoryImplTest`, `pair_givenExpiredErrorCode_mapsToExpired` (con `MockEngine` devolviendo el ProblemDetail).

**No hacer:** no decidir por el status 400. Los cuatro primeros casos son 400 y solo el `errorCode` los separa.

**Verificación:**
- X:XVI-021: escribir "rwnza" → el campo muestra "RWNZA" → 200.
- S:DIS-030: con 4 caracteres "Vincular" queda deshabilitado y ningún `POST /pair` en nginx; "O0I1L" no aparece en el campo.
- S:DIS-024: "ZZZZZ" → "no existe"; uno vencido (más de 5 min) → "venció"; uno ya usado → "ya se usó".
- S:DIS-026: tras el error, corregir y reintentar sin cerrar el diálogo.

---

##### F-65 · Las tarjetas de Dispositivos y Ajustes de la tablet muestran datos viejos — ALTO · NO CORREGIDO

**Qué ve QA hoy (03/11):** R02, F-65, 0/5.
- La lista no se recarga sola ni al volver de Ajustes: una tablet simulada desvinculada por curl siguió como "Tablet 2 · Activo" (R01, F-62).
- Justo tras el RESTART, con la tablet latiendo (07:09:28), la tarjeta decía "Actualizado hace 3 min".
- Sim B mostraba "Actualizado hace 13 min" y al reabrir pasó a "hace 10 min".
- Esperado:
  - X:EXP-006: "Activo / hace un momento" sin salir de la pantalla.
  - X:XRE-001: "hace un momento" ≤ 30 s después del ping.
  - X:XCO-031: un cambio hecho por curl se ve en Ajustes en ≤ 30 s.
  - S:DIS-066: una tablet desvinculada por curl desaparece al volver de Ajustes.

**Qué ya está hecho:** nada.

**Causa en el código:**
- Lista: ver P-2. El dato es la foto del `GET /devices` de cuando se abrió la pantalla; después solo avanza `now`.
- `dl/ui/DeviceCard.kt:91`: `updatedAt = listOfNotNull(device.lastSyncAt, device.lastPingAt).maxOrNull()`. El campo es correcto (incluye `lastPingAt`) y lo viejo es el dato. QA tenía razón en el síntoma, pero no es que use otra marca.
- Ajustes: `dl/settings/TabletSettingsViewModel.kt:57-59` carga una sola vez en `init` (`load()`, `:86-102`). Nada relee los ajustes.

**Qué garantiza el backend:**
- `GET /api/v1/devices` trae `lastPingAt`, `lastSyncAt`, `batteryLevel`, `networkStatus`, `model` y `name`, ordenado por `linkedAt` ascendente (§37).
- La tablet late cada 60 s.
- `GET /api/v1/devices/{id}/settings` trae `settingsVersion`; `DeviceSettingsDto.kt` ya lo deserializa, pero `DeviceMappers.kt:36-44` no lo pasa al dominio.

**Cambio:**
1. Implementar P-2 entero.
2. `TabletSettingsViewModel`:
   - Evento nuevo `ScreenStarted`, enviado desde `TabletSettingsScreen` con `LifecycleStartEffect`. En cada `ScreenStarted` después del primero, y cada 30 s mientras la pantalla esté visible, `getDeviceSettingsUseCase(deviceId)` + `getDevicesUseCase()`, en silencio.
   - Solo se reemplazan `settings` y `device` si `saving.isEmpty()` y `pinDialog == null`, para no pisar lo que el dueño está cambiando.
   - Si la respuesta es 404, ir a `NOT_FOUND`, como hace `load()`.
   - Opcional: mapear `settingsVersion` en `DeviceSettings` y, si sube sin que este equipo haya guardado, mostrar el snackbar "Otro equipo cambió estos ajustes" (`tablet_settings_changed_elsewhere`).
3. Opcional, de claridad: en `CatalogBox` usar solo `lastPingAt`, con el texto "Último reporte %1$s" (`device_last_report`). "Actualizado" junto a "Catálogo visible" se lee como sincronización del catálogo.
4. Tests con `MutableClock` y `FakeDeviceRepository`:
   - `refresh_every30s_updatesLastPing`
   - `screenStarted_afterSettings_reloadsDevices`
   - `tabletSettings_reloadsOnStart_unlessSaving`

**Verificación:** X:EXP-006, X:XRE-001, X:XCO-031 y S:DIS-066, con lo esperado de arriba. En nginx, con la pantalla quieta: `GET /api/v1/devices` cada ~30 s y nada con la app en segundo plano.

---

#### MEDIO

##### F-01 · Tras vincular, el celular no avisa si la tablet no respondió y la tarjeta se queda en "Inactivo" — MEDIO · NO CORREGIDO

(Arreglo PARCIAL: tablet, backend e infraestructura corregidos: WS vivo 119 s y canje inmediato.)

**Qué ve QA hoy (03/11):**
- X:XVI-010: igual que F-60, sin aviso y con una tarjeta "Inactivo" huérfana (R01, F-01).
- 31/10 (`2026-10-31-R01.md`, F-01 lado celular): en X:XJO-001 y X:XVI-001, la tablet manda el heartbeat al segundo 0 y luego cada 60 s, pero la tarjeta "Tablet 3" sigue "Inactivo" a los 60 s. Solo pasa a "Activo" al salir y volver a entrar a Dispositivos.

**Qué ya está hecho:** nada.

**Causa en el código:**
- `dl/ui/DeviceLinkingViewModel.kt:224`: `loadDevices()` se llama en el mismo instante del 200, antes del canje y del primer latido. Después nadie vuelve a pedir `GET /devices` (P-2).
- `core/domain/model/Device.kt:32-35`: con `lastPingAt == null`, `isConnected` es `false`, y `dl/ui/DeviceCard.kt:159` pinta "Inactivo" igual que para una tablet apagada.

**Qué garantiza el backend:** lo de F-60 (`/status` con `tokenExchanged`). El primer heartbeat de la tablet sale en el mismo segundo que el canje (R01, Corregidos: "canje, heartbeat y settings en el mismo segundo").

**Cambio:**
1. Hacer F-60: espera el canje, recarga y refresca 3 s después.
2. Hacer P-2: refresco periódico.
3. `DeviceCard.DeviceIdentity` (`dl/ui/DeviceCard.kt:158-162`): si `device.lastPingAt == null`, mostrar "Aún no se conecta" (`device_never_reported`) en lugar de "Inactivo".

**Verificación:**
- X:XVI-001: vincular en < 60 s → snackbar "Tablet «…» vinculada" en ≤ 5 s, y la tarjeta en "Activo" sin salir de la pantalla.
- X:XVI-010: ver F-60.

---

##### F-09 · "Reinicio: Listo" aunque la tablet no vuelva a latir — MEDIO · NO CORREGIDO

(El arreglo cuenta PARCIAL: la tablet ya reinicia y vuelve fijada.)

**Qué ve QA hoy (03/11):** X:XCR-021 (R02, F-09).
- Pasos: "Reiniciar" → "Reiniciar". El celular muestra "Reinicio: Listo" sin pasar por "Reiniciando…".
- Con Sim B, que acusa (07:07:46) y no vuelve a latir, el celular sigue en "Reinicio: Listo" a los 3 min 45 s y a los 10 min.
- Esperado: "Reiniciando…" → "Listo" solo cuando la tablet vuelve a latir. Con la tablet muerta, a los 3 min: "no volvió a reportar".

**Qué ya está hecho:** nada.
- `core/network/dto/DeviceDtos.kt:72-73` ya deserializa `deliveredAt` y `ackedAt`, pero `core/data/mapper/DeviceMappers.kt:70-81` no los pasa al dominio.

**Causa en el código:**
- `dl/ui/DeviceCard.kt:324`: `DeviceCommandStatus.ACKED -> stringResource(R.string.device_command_ready)`. Un ACKED de `RESTART_APP` es "Listo" sin mirar `lastPingAt`.
- `dl/model/DeviceLinkingUiState.kt:44-45`: con ACKED, `hasCommandsInFlight` es `false`, el polling se detiene y nunca relee `GET /devices`.
- `core/domain/model/DeviceCommand.kt:12-23` no tiene `ackedAt`.

**Qué garantiza el backend:**
- `GET /api/v1/devices/{id}/commands` devuelve los últimos 20, el más nuevo primero, con `status` (`PENDING`, `DELIVERED`, `ACKED`, `FAILED`, `EXPIRED`), `requestedAt`, `expiresAt`, `deliveredAt`, `ackedAt` y `resultDetail`.
- **No hay** 409 al acusar un comando vencido ni un estado "reinicio completado" (línea "Ojo" de R02). Eso solo afecta a la tablet. El celular decide con lo que hay: `ackedAt` del comando y `lastPingAt` del device.
- Dato de QA para calibrar: con la tablet real, el ack (07:09:29) llega **después** del primer latido del proceso nuevo (07:09:28). "Listo" llegará con el latido siguiente, ~60 s después, y eso es aceptable.

**Cambio:**
1. Dominio: `DeviceCommand` + `val deliveredAt: Instant? = null` y `val ackedAt: Instant? = null` (con default, para no romper tests). Mapéalos en `DeviceMappers.toDomain` con `parseIsoInstant()`.
2. Fase del reinicio en `DeviceLinkingUiState`:
   ```kotlin
   enum class RestartPhase { SENT, RESTARTING, BACK, NOT_BACK }

   fun restartPhase(device: Device): RestartPhase? {
       val cmd = commandFor(device.id, DeviceCommandType.RESTART_APP) ?: return null
       return when (cmd.status) {
           DeviceCommandStatus.PENDING -> RestartPhase.SENT
           DeviceCommandStatus.DELIVERED -> RestartPhase.RESTARTING
           DeviceCommandStatus.ACKED -> {
               val acked = cmd.ackedAt ?: return RestartPhase.BACK
               val ping = device.lastPingAt
               when {
                   ping != null && ping.isAfter(acked) -> RestartPhase.BACK
                   Duration.between(acked, now) >= RESTART_BACK_TIMEOUT -> RestartPhase.NOT_BACK   // 3 min
                   else -> RestartPhase.RESTARTING
               }
           }
           else -> null   // FAILED / EXPIRED: como hoy
       }
   }
   ```
   - `isCommandBusy(deviceId, RESTART_APP)` también es `true` en `RESTARTING`.
   - `needsFastPolling` y `deviceIdsNeedingCommandPoll` (P-2) incluyen los devices en `RESTARTING`, para que el bucle relea `GET /devices` cada 15 s.
3. `DeviceCard`: pasar `restartPhase` a la línea de reinicio y pintar:
   - `SENT`: el "Enviado · …" actual.
   - `RESTARTING`: "Reiniciando… esperando a que la tablet vuelva a reportar" (`device_command_restarting`), neutro.
   - `BACK`: "Listo", en verde.
   - `NOT_BACK`: "La tablet no volvió a reportar tras el reinicio. Revísala." (`device_command_restart_not_back`), en `colorScheme.error`, con el botón habilitado.

   `REFRESH_CATALOG` no cambia: su ack sí llega después de ejecutar.
4. Tests:
   - `restart_ackedWithoutNewPing_isRestarting`
   - `restart_pingAfterAck_isBack`
   - `restart_noPing3MinAfterAck_isNotBack`

**No hacer:** no esperar un 409 del backend ni un `RESTART_COMPLETED`. No existen.

**Verificación:**
- X:XCR-021 con la tablet real: "Reinicio: Enviado…" → "Reiniciando…" → "Listo" ≤ 75 s después del ack, sin salir de la pantalla.
- Sim B (acusa y no late): a los 3 min, "no volvió a reportar" en rojo, y el botón Reiniciar habilitado.

---

##### F-61 · Un comando vencido nunca dice "No se pudo entregar" y desaparece al reabrir — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):** S:DIS-043 (R02, F-61).
- Pasos: "Actualizar catálogo" a Sim A, que no late, a las 07:07:06.
- El backend lo pasa a `EXPIRED` a las 07:17.
- El celular sigue consultando (`GET …/commands` a las 07:17:14 y 07:17:30) y muestra "Catálogo: Enviado · … podrás reintentar a las 7:17", con el botón deshabilitado.
- Al reabrir, la línea desaparece sin mensaje.
- Esperado: "No se pudo entregar…" en ≤ 15 s tras vencer, el botón habilitado, y la línea sigue visible al reabrir.

**Qué ya está hecho:** el texto `device_command_expired` ("No se pudo entregar") y su rama en `DeviceCard.kt:328` existen, pero nunca llegan a pintarse.

**Causa en el código:**
- `dl/ui/DeviceLinkingViewModel.kt:250-257`, `latestPerType`:
  ```kotlin
  command.isInFlight ||
      command.requestedAt?.isAfter(now.minus(DeviceLinkingViewModel.FINISHED_COMMAND_TTL)) == true
  ```
  El backend vence el comando justo a `requestedAt + 10 min`, así que un EXPIRED nace ya "terminado hace más de 10 min" y se filtra.
- `:205`, en el polling: `merged[deviceId] = merged[deviceId].orEmpty() + commands.latestPerType(now)`. Como el tipo filtrado no viene, se queda el PENDING viejo: "Enviado…" para siempre, y el polling sigue cada 15 s.
- `:160`, al reabrir: el reemplazo deja el tipo vacío y la línea desaparece.

**Qué garantiza el backend:**
- `GET /devices/{id}/commands` trae `status: EXPIRED`, `expiresAt` y `ackedAt` (comando `DeviceCommandResponse`; enum `DeviceCommandStatus`).
- Vence a los 10 min y no se reintenta.
- Desvincular cierra como `EXPIRED` los que estaban en vuelo (§35).

**Cambio:**
1. Con `ackedAt` ya en el dominio (F-09), en `DeviceCommand`:
   ```kotlin
   val finishedAt: Instant? get() = when (status) {
       DeviceCommandStatus.ACKED, DeviceCommandStatus.FAILED -> ackedAt
       DeviceCommandStatus.EXPIRED -> expiresAt
       else -> null
   }
   ```
2. `latestPerType`: `command.isInFlight || (command.finishedAt ?: command.requestedAt)?.isAfter(now.minus(FINISHED_COMMAND_TTL)) == true`. El TTL se cuenta desde que terminó, así que un EXPIRED se ve 10 min más.
3. Polling: **reemplazar** por dispositivo, nunca fusionar (ya incluido en P-2, `refreshSilently`).
4. Red de seguridad local: si `isInFlight && expiresAt != null && now.isAfter(expiresAt.plusSeconds(30))`, tratarlo como EXPIRED en `statusLabel` y en `isCommandBusy`/`hasCommandsInFlight`.
5. Texto: `device_command_expired` = "No se pudo entregar: la tablet no se conectó en 10 min. Revisa que esté encendida y con internet." (es/en).
6. Test con `MutableClock`:
   - PENDING a T0.
   - Polling a T0+10:15 que devuelve EXPIRED → se muestra EXPIRED y `hasCommandsInFlight == false`.
   - Reabrir a T0+15 min → sigue visible.
   - A T0+21 min → oculto.

**Verificación:**
- S:DIS-043 / X:XCR-023: con la tablet detenida, "Actualizar catálogo"; a los 10 min la línea pasa a "No se pudo entregar…" en ≤ 15 s y el botón se habilita.
- Al salir y volver a entrar, la línea sigue visible.
- Un nuevo toque crea un comando con otro `id`.

---

##### F-62 · Desvincular una tablet que ya no está: "Inténtalo de nuevo" para siempre — MEDIO · NO CORREGIDO

(Arreglo PARCIAL: el backend ya responde 404.)

**Qué ve QA hoy (03/11):** S:DIS-082 (R01, F-62).
- Pasos: Ajustes de "Tablet 2" (simulada) abierto → unlink por curl → en la app, "Desvincular tablet" → "Desvincular".
- Resultado: "No se pudo desvincular la tablet. Inténtalo de nuevo." y se queda en Ajustes. Al volver, la lista sigue con "Tablet 2 · Activo".
- Backend: `unlink 404 RESOURCE_NOT_FOUND`.
- Esperado: "Esta tablet ya no está vinculada a tu tienda" y vuelta a la lista sin esa tarjeta.

**Qué ya está hecho:**
- El texto `tablet_settings_not_found` ("Esta tablet ya no está vinculada a tu tienda", `strings.xml:96`) existe y se usa al cargar (`TabletSettingsViewModel.kt:96`).
- La navegación de vuelta con el id ya existe: `app/navigation/MainShell.kt:256-259` y `dl/navigation/DeviceLinkingNavigation.kt:23,40-47`.

**Causa en el código:**
- `core/data/repository/DeviceRepositoryImpl.kt:56-57`: `apiService.unlinkDevice(deviceId).asDomainResult { }`. `asDomainResult` (`NetworkResults.kt:12`) convierte cualquier HTTP en `IllegalStateException("HTTP 404: …")`, así que el VM no puede distinguir 404 de un fallo de red.
- `dl/settings/TabletSettingsViewModel.kt:176-179`: todo fallo → `ShowMessage(R.string.device_unlink_error)` y se queda.

**Qué garantiza el backend** (§35): `POST /api/v1/devices/{deviceId}/unlink` → 200 con el device ya `INACTIVE`, o **404** `RESOURCE_NOT_FOUND` si ya estaba desvinculada o es de otra tienda. Nunca 403.

**Cambio:**
1. Dominio: `sealed interface UnlinkDeviceError { data object NotLinked; data object Network; data class Unknown(val message: String?) }` + `class UnlinkDeviceException(val error: UnlinkDeviceError) : Exception()`. Mismo patrón que `PairDeviceError.kt`.
2. `DeviceRepositoryImpl.unlinkDevice`: `HttpError` 404 → `NotLinked`; `NetworkError` → `Network`; lo demás → `Unknown`.
3. `TabletSettingsViewModel.unlink()` (`:169-181`):
   - `NotLinked` → `_uiEffect.emit(TabletSettingsUiEffect.Unlinked(deviceId, alreadyGone = true))`.
   - `Network` → "Sin conexión. Inténtalo nuevamente." (`device_unlink_error_network`).
   - `Unknown` → el texto actual.
4. Pasar `alreadyGone` hasta Dispositivos:
   - `TabletSettingsUiEffect.Unlinked(deviceId, alreadyGone: Boolean = false)` (`dl/settings/TabletSettingsModels.kt:81`).
   - `onUnlinked(deviceId, alreadyGone)` en `TabletSettingsScreen.kt:25,38` y `DeviceLinkingNavigation.kt:37,54`.
   - En `MainShell.kt:256-259`, guardar también `TABLET_UNLINKED_ALREADY_GONE_KEY = "tablet_unlinked_already_gone"` en el `savedStateHandle`.
   - `DeviceLinkingUiEvent.DeviceUnlinked(deviceId, alreadyGone)` → efecto `ShowUnlinked(alreadyGone)`: `tablet_settings_not_found` si ya no estaba y `device_unlinked` si no.
   - Además, `refreshSilently()` (P-2).
5. Tests: `unlink_when404_navigatesBackAsAlreadyGone`, `unlink_whenNetwork_staysWithNetworkMessage`.

**Verificación:** S:DIS-082 / S:EXP-029: desvincular por curl → en la app, "Desvincular" → vuelve a la lista sin esa tarjeta, con el snackbar "Esta tablet ya no está vinculada a tu tienda". Un único `POST …/unlink 404` en nginx.

---

##### F-59 · No se puede nombrar ni renombrar la tablet; la vinculada desde el celular queda con versión "1.0.0" — MEDIO · NO CORREGIDO

(Arreglo PARCIAL: la tablet y el backend tienen su parte. El backend repite nombres; ver al final.)

**Qué ve QA hoy (03/11):** R01, F-59.
- Al vincular no hay campo de nombre, y Ajustes no permite renombrar.
- La tablet vinculada desde el celular queda con `appVersion 1.0.0` hasta su primer latido.
- El backend asignó "Tablet 3" a dos tablets activas a la vez (`0d881a2d` a las 06:48 y `35067926` a las 07:01).
- El 31/10 se vio además que el modelo aparece en Información, pero no en la tarjeta.
- Esperado:
  - S:DIS-006: vincular con el nombre "Mostrador" → tarjeta "Mostrador"; renombrar desde Ajustes → la tarjeta cambia.
  - X:XVI-030 / X:EXP-011: dos tablets con nombres distintos, la antigua arriba y el modelo visible.

**Qué ya está hecho:** el orden "la antigua arriba" ya sale bien, porque el backend ordena por `linkedAt` (QA OK). No hay que ordenar en la app.

**Causa en el código:**
- `core/data/repository/DeviceRepositoryImpl.kt:42-44` y `:105-106`: `deviceName = DEFAULT_DEVICE_NAME` ("Tablet") y `appVersion = DEFAULT_APP_VERSION` ("1.0.0"), fijos.
- `dl/ui/PairDeviceDialog.kt:80-92`: un solo campo, el código.
- `dl/settings/TabletSettingsContent.kt:229` y `:240`: la fila "Nombre" se pinta con `onClick = null`.
- `core/network/service/DeviceApiService.kt`: no existe ningún `PATCH`.
- `dl/ui/DeviceCard.kt:146-153`: la identidad muestra solo `name`, sin `model`.

**Qué garantiza el backend:**
- `POST /devices/pair`: `deviceName` es opcional (máx. 200). Sin nombre, o con exactamente "Tablet", el backend pone "Tablet N" con N = tablets ACTIVE + 1 (`DeviceService.defaultDeviceName`). Cualquier otro nombre se respeta tal cual (§37).
- `PATCH /api/v1/devices/{deviceId}` con `{"name":"Mostrador"}` (OWNER/ADMIN; `@NotBlank`, máx. 200) → 200 con el device. 404 si no es de la tienda y 422 si el nombre no es válido.
- `appVersion` llega con el primer heartbeat. El nombre **no** viaja en el heartbeat ("Ojo" de R01): solo se pone al aprobar o con el PATCH.

**Cambio:**
1. **Nombre al vincular.**
   - `DeviceLinkingUiState.pairName: String` y evento `PairNameChanged(name)`, con `take(40)`.
   - Al abrir el diálogo (`OpenPairDialog`, `:110-113`), proponer un nombre libre:
     ```kotlin
     internal fun suggestTabletName(devices: List<Device>): String {
         val used = devices.mapNotNull { it.name?.trim()?.lowercase(Locale.ROOT) }.toSet()
         return generateSequence(1) { it + 1 }.map { "Tablet $it" }.first { it.lowercase(Locale.ROOT) !in used }
     }
     ```
     El dueño puede editarlo, y se envía `deviceName = pairName.trim().ifBlank { null }` (ver F-60, paso 3). Como el nombre que manda la app no es el genérico "Tablet", el backend lo respeta, y así la app evita el nombre repetido **sin esperar** el arreglo del backend.
   - En `PairDeviceDialog`, un segundo `QhathiaTextField` con el placeholder "Nombre de la tablet (ej. Mostrador)" (`device_pair_name_placeholder`).
2. **No mandar `appVersion`.** Quitar `DEFAULT_APP_VERSION` y `DEFAULT_DEVICE_NAME` (ver F-60, paso 2). Con `appVersion == null`, `InformationSection` ya pinta "—" (`TabletSettingsContent.kt:233`).
3. **Renombrar.**
   - `RenameDeviceRequestDto(name: String)`.
   - `DeviceApiService.renameDevice(id, body)` = `client.patch("api/v1/devices/$id") { setBody(body) }.body<DeviceDto>()`, con P-1.
   - `DeviceRepository.renameDevice(id, name): Result<Device>`, con 404 → `DeviceSettingsException(NotFound)` y 422 → `InvalidInput`, reutilizando `DeviceSettingsError`.
   - `RenameDeviceUseCase`.
   - En `TabletSettingsContent.InformationSection` (`:226-251`), la fila "Nombre" con `onClick` → `RenameTabletDialog` (campo prellenado, máx. 40, "Guardar" y "Cancelar").
   - `TabletSettingsViewModel`: eventos `RenameClicked`, `RenameChanged`, `RenameConfirmed` y `RenameDismissed`, y estado `renameDialog: RenameDialogState(value, isSaving, errorRes)`.
     - Éxito → `device = updated` + "Nombre actualizado" (`tablet_settings_renamed`).
     - 404 → `NOT_FOUND`.
     - 422 → "Escribe un nombre de 1 a 40 caracteres".
   - Al volver, Dispositivos recarga (P-2) y la tarjeta muestra el nombre nuevo.
4. **Distinguir tablets en la tarjeta.** En `DeviceCard.DeviceIdentity`, una segunda línea con `device.model` (p. ej. "Lenovo Tab M11") junto al estado. Si `model == null`, solo el estado.
5. Tests:
   - `openPairDialog_suggestsFirstFreeTabletName`
   - `pair_sendsTypedName`
   - `rename_updatesDeviceName`
   - `rename_when404_showsNotFound`

**No hacer:**
- No mandar "Tablet" como nombre: el backend lo trata como genérico.
- No intentar que la tablet se ponga el nombre por heartbeat: el backend no lo implementó.

**Verificación:**
- S:DIS-006: vincular con "Mostrador" → tarjeta "Mostrador"; renombrar a "Caja 2" en Ajustes → al volver, la tarjeta dice "Caja 2" (`PATCH /devices/{id} 200`).
- X:XVI-030 / X:EXP-011: dos tablets vinculadas sin tocar el nombre propuesto → "Tablet 1" y "Tablet 2", distintos, con el modelo visible y la antigua arriba.
- X:XCR-034: "Versión de la app" muestra "—" hasta el primer latido, nunca "1.0.0".

---

##### F-48 · Una contraseña actual incorrecta dispara un refresh y repite la petición — MEDIO · NO CORREGIDO

(Arreglo PARCIAL: el backend ya manda `CURRENT_PASSWORD_INCORRECT`.)

**Qué ve QA hoy (03/11):** S:LOG-071 (R08, F-48).
- Pasos: Perfil → Seguridad → Cambiar contraseña, con la contraseña actual incorrecta → "Guardar" (07:57:14).
- nginx: `change-password 401` → `auth/refresh 200` → `change-password 401` otra vez.
- El mensaje "La contraseña actual no es correcta" sale bien y la sesión y la tienda siguen bien (el refresh de la fase 36 conserva la tienda).
- Esperado: un solo `POST /auth/change-password` y ningún `POST /auth/refresh`.

**Qué ya está hecho:** el mapeo 401 → `WrongCurrent` (`core/data/repository/AccountSecurityRepositoryImpl.kt:64-66`) da el texto correcto.

**Causa en el código:**
- `core/network/KtorClientFactory.kt:68-75` instala el bearer `Auth`. Ante **cualquier** 401 del cliente autenticado, Ktor llama a `refreshTokens` y reintenta una vez; el reintento lleva `AuthCircuitBreaker`, por eso son 2 y no infinitos.
- `core/network/service/AuthApiService.kt:81-85`: `authenticatedClient.get().post("api/v1/auth/change-password") { setBody(request) }` va por ese cliente.
- Si el refresh fallara (red), `core/data/repository/SessionAuthTokenProvider.kt:32-35` borraría la sesión: el usuario quedaría fuera por equivocarse de clave.

**Qué garantiza el backend** (§36, `VitriaException.CurrentPasswordIncorrectException`):
```json
HTTP 401  application/problem+json
{ "title": "Current Password Incorrect", "detail": "La contraseña actual es incorrecta", "errorCode": "CURRENT_PASSWORD_INCORRECT", … }
```
Sigue siendo **401**, no 400/422. "El cliente no debe refrescar ante ese código: no es un token vencido."

**Cambio:**
1. `AuthApiService.changePassword` (con `json` de P-1):
   ```kotlin
   suspend fun changePassword(request: ChangePasswordRequestDto): NetworkResult<Unit> {
       val client = authenticatedClient.get()
       val first = runCatching {
           client.post("api/v1/auth/change-password") {
               attributes.put(AuthCircuitBreaker, Unit)   // io.ktor.client.plugins.auth.AuthCircuitBreaker: sin refresh ni reintento
               setBody(request)
           }
           Unit
       }.toNetworkResultWithProblem(json)
       // Un 401 que NO es de la contraseña (access vencido) sí debe pasar por el refresh, una vez.
       if (first is NetworkResult.HttpError && first.code == 401 && first.errorCode != CURRENT_PASSWORD_INCORRECT) {
           return runCatching { client.post("api/v1/auth/change-password") { setBody(request) }; Unit }
               .toNetworkResultWithProblem(json)
       }
       return first
   }
   ```
   `AuthCircuitBreaker` solo salta el bloque de refresh; el `Authorization` lo sigue poniendo `loadTokens`. Compílalo para confirmar que el símbolo existe en Ktor 3.4.3 (`gradle/libs.versions.toml:26`). Si no existe: usar `publicClient` con `header(HttpHeaders.Authorization, "Bearer ${token}")` y pasar el access desde `AccountSecurityRepositoryImpl` (`sessionRepository.currentAccessToken()`).
2. `AccountSecurityRepositoryImpl.toChangePasswordError` (`:64-69`): decidir por `errorCode`. `CURRENT_PASSWORD_INCORRECT` → `WrongCurrent`; los demás 401 → `Unknown` (la sesión la resuelve el plugin).
3. Opcional, relacionado (S:RED-032, no re-ejecutado hoy): en `SessionAuthTokenProvider.refreshTokens`, no borrar la sesión cuando el refresh falla por red, y borrarla solo ante 401/403 del propio refresh.

**No hacer:** no cambiar a 400/422 esperando que el backend lo haga. El contrato es 401 + `errorCode`.

**Verificación:** S:LOG-071: con la contraseña actual mal, nginx muestra un solo `POST /auth/change-password 401` y ningún `POST /auth/refresh`. Se ve "La contraseña actual no es correcta" en el campo 1, y la sesión y la tienda siguen igual (Inventario carga).

---

##### F-52 · Con huella activa, la notificación de pedido abre Inicio — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11, tarde):** S:LOG-052 (`2026-11-03-pendientes.md`, R08).
- Pasos: app en segundo plano 309 s → pedido 276317 → tocar "Nueva orden de compra" → "Ingresar a Qhathia" → huella.
- Resultado: abre **Inicio**, con el badge en 1. nginx solo registra `GET /orders?…size=1` y el dashboard, y ningún `size=50`.
- Sin huella (S:SOL-060) abre bien Solicitudes · Pendientes.
- Esperado: Solicitudes · Pendientes, con `GET /orders?status=PENDING&…&size=50`.

**Qué ya está hecho:** el deep link sin huella funciona: `app/MainActivity.kt:56-60` (`onNewIntent` → `onDeepLink`) y `app/navigation/MainShell.kt:150-154`.

**Causa en el código:** la secuencia que siguen el código y nginx es esta.
- `app/MainViewModel.kt:98-111`: `authState` es `stateIn(WhileSubscribed(5_000))`. Al volver tras más de 5 min, la UI todavía tiene `Authenticated` en caché y el `MainShell` viejo sigue compuesto.
- `MainShell.kt:150-154`: su efecto cambia de pestaña y **consume** el destino:
  ```kotlin
  LaunchedEffect(pendingDestination) {
      if (pendingDestination != DESTINATION_ORDER_REQUESTS) return@LaunchedEffect
      switchTab(ORDER_REQUESTS_ROUTE)
      onPendingDestinationConsumed()
  }
  ```
- Enseguida, `onAppForegrounded` (`MainViewModel.kt:80-84`) pone `unlocked = false`. El root (`MainActivity.kt:82-99`) cambia a `BiometricUnlockScreen` y descarta ese shell.
- Tras la huella se compone un `QhathiaNavHost` nuevo, que arranca en Inicio, y `onPendingDestinationConsumed` (`MainViewModel.kt:65-67`) ya lo había puesto a `null`.

**Qué garantiza el backend:** nada en juego. La app hace bien `GET /orders?status=PENDING&size=50` cuando llega a Solicitudes (S:SOL-060).

**Cambio** (`app/MainViewModel.kt`):
1. Estado de la huella "caliente", **aparte** del `combine` (no cambies el `combine` de `authState`: con un `initialValue = false` mostraría la app un instante antes del gate en un arranque en frío):
   ```kotlin
   private val biometricEnabledNow: StateFlow<Boolean?> =
       observeBiometricEnabled().stateIn(viewModelScope, SharingStarted.Eagerly, null)

   private fun isLockDue(): Boolean {
       val biometric = biometricEnabledNow.value ?: return true          // aún sin leer: no consumir
       if (!biometric) return false
       if (!unlocked.value) return true
       val since = backgroundedAt ?: return false
       return Duration.between(since, clock.instant()) >= RELOCK_AFTER
   }

   fun onPendingDestinationConsumed() {
       if (isLockDue()) return            // el shell que lo "consumió" está por ser reemplazado por el gate
       _pendingDestination.value = null
   }
   ```
   El constructor recibe `observeBiometricEnabled` como parámetro, no como propiedad. Guárdalo en un `private val` o crea los dos flows en `init`.
2. `onDeepLink` (`:60-62`): si `backgroundedAt` existe y pasó `RELOCK_AFTER`, poner ya `unlocked.value = false`. Así no depende del orden entre `onNewIntent` y el `ON_START` del proceso.
3. No hace falta tocar `MainShell`. Tras desbloquear, el shell nuevo ve el destino aún pendiente, cambia a Solicitudes y lo consume, esta vez de verdad.
4. No cambies la rama `Unauthenticated` de `QhathiaRoot` (`MainActivity.kt:94-98`). Ahí el destino debe sobrevivir al login (KDoc de `MainViewModel`, líneas 33-36).
5. Test en `MainViewModelTest`, `deepLink_whileRelockDue_survivesConsumeUntilUnlocked`:
   - `onAppBackgrounded`, avanzar 6 min con `MutableClock` y `onDeepLink(DESTINATION_ORDER_REQUESTS)`.
   - `onPendingDestinationConsumed()` → sigue no nulo.
   - `onUnlocked()`, luego `onPendingDestinationConsumed()` → `null`.

**Verificación:**
- S:LOG-052: con huella activa, más de 5 min en segundo plano → push → tocarlo → huella → **Solicitudes · Pendientes**, y nginx muestra `GET /orders?status=PENDING&…&size=50` después del desbloqueo.
- S:SOL-060: sin huella, sigue abriendo Solicitudes.

---

##### F-73 · Las sesiones no se agrupan — MEDIO · NO CORREGIDO

(Arreglo PARCIAL: el backend pagina y pone un tope de 20.)

**Qué ve QA hoy (03/11):** X:XCO-040 (R08, F-73).
- Seguridad lista cada "qa-sim-tablet" en su propia fila con "Cerrar", sin agrupar y sin "Cerrar todas".
- Backend: `GET /auth/sessions?size=5` → 5 ítems + `X-Total-Count: 19`.
- Esperado: un grupo "qa-sim-tablet · N sesiones" con "Cerrar todas".

**Qué ya está hecho:** "Cerrar sesión en otros equipos" (`revoke-others`) ya existe (`profile/security/SecurityViewModel.kt:163-183`), y el 404 al cerrar una sesión cuenta como éxito (`AccountSecurityRepositoryImpl.kt:30-37`).

**Causa en el código:** `profile/security/SecurityContent.kt:113-121`.
```kotlin
is LoadState.Loaded -> sessions.value.forEachIndexed { index, session ->
    SessionRow(
```
Hay una fila por sesión, dentro de un `Column` con `verticalScroll` (`:53-58`). Nada agrupa por `deviceLabel`.

**Qué garantiza el backend** (§36):
- `GET /api/v1/auth/sessions?page=0&size=50`: por defecto 50, máximo 200. El cuerpo es un array y el total va en la cabecera `X-Total-Count`.
- **Tope de 20 sesiones por usuario**: al abrir la 21.ª se cierran las más viejas.
- Con ese tope, la llamada actual sin parámetros ya trae todas. **No hace falta paginar ni pantalla "Ver todas"** (lo que proponía `fixes-staff-A.md` queda obsoleto).
- `DELETE /auth/sessions/{id}` → 204, o 404 si ya no está.

**Cambio:**
1. `profile/security/SecurityModels.kt`:
   ```kotlin
   data class SessionGroup(val label: String, val sessions: List<ActiveSession>) {
       val lastUsedAt: Instant? get() = sessions.mapNotNull { it.lastUsedAt }.maxOrNull()
   }
   // en SecurityUiState:
   val currentSession: ActiveSession? get() = sessions.valueOrNull?.firstOrNull { it.isCurrent }
   val otherGroups: List<SessionGroup> get() = sessions.valueOrNull.orEmpty()
       .filterNot { it.isCurrent }
       .groupBy { it.deviceLabel.trim() }
       .map { (label, list) -> SessionGroup(label, list) }
       .sortedByDescending { it.lastUsedAt }
   val pendingRevokeGroup: SessionGroup? = null   // campo del data class
   ```
2. `SessionsBody`: primero la sesión actual ("Este equipo", como hoy) y luego cada grupo.
   - Un grupo de 1 se pinta con `SessionRow`, igual que hoy.
   - Un grupo de 2 o más se pinta con una fila "%1$s · %2$d sesiones" (`security_session_group`), el subtítulo "Última vez %1$s" y un `TextButton` "Cerrar todas" (`security_session_close_all`). La etiqueta vacía va como `security_session_unknown_device`.
3. VM: eventos `RevokeGroupRequested(group)`, `RevokeGroupConfirmed` y `RevokeGroupDismissed`.
   - Al confirmar, revocar los ids uno a uno con `revokeSessionUseCase` (son 19 como máximo), marcándolos en `revokingIds`, y después `loadSessions()`.
   - Si alguno falla, el mensaje `security_revoke_error` que ya existe.
   - Diálogo de confirmación: "¿Cerrar %1$d sesiones de %2$s?" (`security_revoke_group_title`).
4. Test: `SecurityViewModelTest.sessions_groupedByDeviceLabel_currentFirst`, `revokeGroup_revokesEveryIdThenReloads`.

**Verificación:** X:XCO-040: con varias sesiones del helper, Seguridad muestra "Este equipo" y un grupo "qa-sim-tablet · N sesiones" con "Cerrar todas". Al confirmar, N `DELETE /auth/sessions/{id}` y el grupo desaparece; la sesión del celular sigue.

---

#### BAJO

##### F-53 · Si se borra la huella del sistema, la app entra sin pedir nada y apaga el switch en silencio — BAJO · NO CORREGIDO

**Qué ve QA hoy (03/11, tarde):** S:LOG-049 (`2026-11-03-pendientes.md`, R08).
- Pasos: huella activa → borrar "Finger 1" en Ajustes del sistema → arranque en frío.
- Resultado: entra directo a Inicio, sin "Tu huella ya no está configurada…" y sin Login. En Seguridad, "Ingresar con huella" aparece apagado sin aviso.
- Esperado: el aviso + "Continuar" → Login. Tras entrar con la contraseña, el switch aparece apagado.

**Qué ya está hecho:** la pantalla del aviso existe y nunca llega a verse:
- textos `unlock_biometric_gone` y `unlock_continue` (`features/auth/src/main/res/values/strings.xml:24-25`);
- la rama `isBiometricGone` en `auth/ui/BiometricUnlockScreen.kt:155` y `:170-173`, con "Continuar" → `UsePassword`.

**Causa en el código:**
- `auth/ui/BiometricUnlockViewModel.kt:69-72`:
  ```kotlin
  BiometricUnlockUiEvent.BiometricGone -> {
      _uiState.update { it.copy(isBiometricGone = true, lastError = null) }
      viewModelScope.launch { setBiometricEnabledUseCase(false) }
  }
  ```
  Apagar la preferencia hace que `MainViewModel.authState` (`app/MainViewModel.kt:99-104`, `biometric && !isUnlocked -> Locked`) pase a `Authenticated`, y el gate desaparece al frame siguiente.
- `BiometricGone` se dispara solo, desde `promptOnce` (`BiometricUnlockScreen.kt:98-101`), lanzado en el `LaunchedEffect` de `:79-81`.
- `profile/security/SecurityViewModel.kt:96-101`: `onResumed` también apaga en silencio.

**Qué garantiza el backend:** no aplica. "Continuar" cierra sesión con el `LogoutUseCase` que ya existe (token push + `POST /auth/logout`).

**Cambio:**
1. `BiometricUnlockViewModel`, rama `BiometricGone`: solo `_uiState.update { it.copy(isBiometricGone = true, lastError = null) }`. **No** tocar la preferencia ahí.
2. `signOut()` (`:77-81`):
   ```kotlin
   viewModelScope.launch {
       val gone = _uiState.value.isBiometricGone
       logoutUseCase()                                  // 1.º: sesión a null → Unauthenticated → Login
       if (gone) setBiometricEnabledUseCase(false)      // 2.º: que el próximo login no vuelva a un gate muerto
   }
   ```
   El orden importa: si se apaga primero, vuelve el bug.
3. Actualizar el KDoc de la clase (`:25-33`).
4. `SecurityViewModel.onResumed`: además de apagar, emitir `SecurityUiEffect.ShowMessage(R.string.security_biometric_disabled_gone)` = "Desactivamos «Ingresar con huella» porque este equipo ya no tiene huellas registradas." (nuevo, es/en).
5. Tests: cambiar `a removed enrollment clears the preference and continuing goes to login` (`BiometricUnlockViewModelTest.kt:83`), que hoy espera el apagado inmediato, por `biometricGone_keepsPreferenceUntilContinue` y `continue_whenGone_logsOutThenDisablesBiometric`.

**Verificación:** S:LOG-049: huella activa → borrar la huella del sistema → arranque en frío → "Tu huella ya no está configurada en este equipo…" + "Continuar" → Login. Tras entrar con la contraseña, Seguridad muestra el switch apagado.

---

##### F-63 · Tipo de red en crudo ("ETHERNET") con ícono de Wi-Fi — BAJO · NO CORREGIDO

(La parte de la tablet está corregida: ya manda `WIFI` y no `OFFLINE`. Le falta `uptimeSeconds > 0`, que es de la tablet.)

**Qué ve QA hoy (03/11):** S:DIS-005 (R02, F-63). Sim A, con heartbeat `ETHERNET`, se ve como "ETHERNET · Conexión" en crudo y con el ícono de Wi-Fi. Esperado: "Cable", con un ícono de cable.

**Qué ya está hecho:** nada.

**Causa en el código:**
- `dl/ui/DeviceCard.kt:332-340`, `networkLabel`: `else -> networkStatus` devuelve el valor crudo.
- `:101-106`: el ícono es siempre `Icons.Rounded.Wifi`.
- `features/device-linking/src/main/res/values/strings.xml:26`: `device_network_wifi` = "Wi Fi", sin guion.

**Qué garantiza el backend:** `networkStatus` es texto libre del heartbeat (`HeartbeatRequest.networkStatus`, sin validar). La tablet manda `WIFI`, `MOBILE`, `ETHERNET` u `OFFLINE`.

**Cambio:**
1. Reemplazar `networkLabel` por:
   ```kotlin
   @Composable
   private fun networkPresentation(status: String?, connected: Boolean): Pair<ImageVector, String> =
       when (status?.uppercase(Locale.ROOT)) {
           null -> Icons.Rounded.Public to stringResource(R.string.device_missing)
           "WIFI" -> Icons.Rounded.Wifi to stringResource(R.string.device_network_wifi)
           "MOBILE", "CELLULAR", "LTE", "4G", "5G" -> Icons.Rounded.SignalCellularAlt to stringResource(R.string.device_network_mobile)
           "ETHERNET" -> Icons.Rounded.SettingsEthernet to stringResource(R.string.device_network_ethernet)
           "OFFLINE" -> if (connected) Icons.Rounded.Public to stringResource(R.string.device_network_online)
                        else Icons.Rounded.WifiOff to stringResource(R.string.device_network_offline)
           "ONLINE" -> Icons.Rounded.Public to stringResource(R.string.device_network_online)
           "LIMITED" -> Icons.Rounded.SignalWifiStatusbarConnectedNoInternet4 to stringResource(R.string.device_network_limited)
           else -> Icons.Rounded.Public to stringResource(R.string.device_network_other)
       }
   ```
   Usa el resultado en el `Tile` de `:101-106`, con el ícono y el valor. Nunca se muestra el valor crudo. Los íconos salen de `material-icons-extended`, que ya es dependencia del módulo (`features/device-linking/build.gradle.kts:36`).
2. Strings (es/en):
   - `device_network_wifi`: "Wi-Fi"
   - `device_network_ethernet`: "Cable"
   - `device_network_offline`: "Sin conexión"
   - `device_network_other`: "Otra red"

**Verificación:**
- S:DIS-005: `sim-tablet.sh heartbeat 50 ETHERNET` → la tarjeta dice "Cable", con el ícono de cable.
- `MOBILE` → "Datos móviles".
- X:EXP-005: al reabrir la vitrina, nunca "OFFLINE" junto a "Activo".

---

##### F-49 · Registro del token push sin sesión y sin reintento — BAJO · PARCIAL

**Qué ve QA hoy (03/11):** R08, F-49.
- Tras logout + login, `POST /notifications/tokens 204` dos veces y ningún 401. Ese es el único caso ejecutado hoy, y ya pasaba antes.
- **S:EXP-001 y S:INI-050 no se re-ejecutaron.** El 31/10, con la misma app (`2026-10-31-R08.md`, F-49):
  - `pm clear` → abrir (Login) → `POST /notifications/tokens 401` a las 10:23:03.
  - Arrancar sin red → volver la red → 60 s sin ningún `POST /notifications/tokens`.

**Ojo con el veredicto:** el PARCIAL de hoy no refleja un avance. El código no cambió (`git diff 0ca3de8 2c6aa0f` no toca estos archivos), así que EXP-001 e INI-050 siguen fallando.

**Qué ya está hecho:** nada nuevo.
- El "dos veces" tiene explicación:
  - el logout borra el token FCM (`core/domain/usecase/UnregisterPushTokenUseCase.kt`, `deleteToken()`);
  - al volver a entrar, el login registra una vez (`MainViewModel.kt:89-91`);
  - el token nuevo llega por `onNewToken` y registra otra.
- Es inocuo, pero cuenta para F-75.

**Causa en el código:**
- `core/notifications/QhathiaMessagingService.kt:28-31`:
  ```kotlin
  override fun onNewToken(token: String) {
      // Re-register the rotated token. No-ops server-side (401 → fail-open) when no session exists.
      runBlocking { registerPushToken() }
  }
  ```
  No mira si hay sesión: tras `pm clear`, el token nuevo produce un 401.
- `core/domain/usecase/RegisterPushTokenUseCase.kt:16-21`: `registerToken` devuelve un `Result` que se ignora (el `runCatching` no ve fallos) y no hay reintento.
- `MainViewModel.kt:86-96` solo registra en la transición a autenticado.

**Qué garantiza el backend:** `POST /api/v1/notifications/tokens` → 204 con sesión, 401 sin ella. Idempotente por token.

**Cambio:**
1. `RegisterPushTokenUseCase`: inyectar `SessionRepository` y salir sin llamar si `observeSession().first() == null`. Devolver `Result<Unit>`:
   ```kotlin
   suspend operator fun invoke(deviceLabel: String? = null): Result<Unit> {
       if (sessionRepository.observeSession().first() == null) return Result.success(Unit)
       val token = runCatching { pushTokenProvider.currentToken() }.getOrNull() ?: return Result.failure(IllegalStateException("no token"))
       return notificationTokenRepository.registerToken(token, deviceLabel)
   }
   ```
   Así `onNewToken` queda cubierto sin tocar el servicio. Actualiza los tests de dominio y `FakeSessionRepository`.
2. Reintento al volver la red:
   - Clase nueva `core/notifications/NetworkMonitor` (`@Singleton`, `@ApplicationContext`) con `val isOnline: Flow<Boolean>`, hecho con `callbackFlow` sobre `ConnectivityManager.registerDefaultNetworkCallback`. Hoy no hay ningún observador de red en el repo.
   - En `MainViewModel`, sustituir `viewModelScope.launch { registerPushToken() }` (`:91`) por `ensurePushRegistered()`:
   ```kotlin
   private var pushJob: Job? = null
   private fun ensurePushRegistered() {
       pushJob?.cancel()
       pushJob = viewModelScope.launch {
           repeat(MAX_PUSH_ATTEMPTS) {                                   // 10
               if (registerPushToken().isSuccess) return@launch
               withTimeoutOrNull(15_000) { networkMonitor.isOnline.first { it } }   // vuelve la red → sigue al instante
               delay(1_000)
           }
       }
   }
   ```
   Cancelar `pushJob` cuando la sesión pase a `null`.
3. La sesión huérfana por matar la app durante el login (S:LOG-022) no tiene arreglo en el cliente. Déjala fuera.

**Verificación:**
- S:EXP-001: `pm clear` → abrir → nginx sin `POST /notifications/tokens 401`.
- S:INI-050: `force-stop` → modo avión → abrir → quitar el avión → `POST /notifications/tokens 204` en ≤ 30 s sin tocar nada.

---

##### F-50 · La tecla ✓ del teclado no inicia sesión — BAJO · NO CORREGIDO

**Qué ve QA hoy (03/11):** S:LOG-017 (R08, F-50). Pasos: correo + contraseña → ✓ del teclado (07:58:04). Resultado: solo se cierra el teclado, sin `POST /auth/login` hasta tocar "Iniciar sesión" (07:58:17).

**Qué ya está hecho:** nada.

**Causa en el código:**
- `auth/ui/LoginContent.kt:96`: `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),` sin `keyboardActions`.
- `core/ui/component/QhathiaTextField.kt:52-68` no tiene el parámetro, y el `BasicTextField` (`:93-116`) se crea sin `keyboardActions`. La acción por defecto de Done solo cierra el teclado.

**Qué garantiza el backend:** no aplica.

**Cambio:**
1. `QhathiaTextField`: parámetro nuevo `keyboardActions: KeyboardActions = KeyboardActions.Default`, que se pasa a `BasicTextField(keyboardActions = keyboardActions, …)`.
2. `LoginContent`, campo Contraseña: `keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); onEvent(LoginUiEvent.Submit) })`. `keyboard` ya existe (`:48`), y el VM ignora los envíos repetidos mientras `isSubmitting`.
3. El mismo patrón en:
   - `dl/ui/PairDeviceDialog.kt` (Done → `onSubmit` si `canSubmit`; F-58);
   - `profile/security/ChangePasswordScreen.kt:147` (el `LabelledPasswordField` recibe un `onImeAction`; en el tercer campo, Done → `Submit` si `canSubmit`);
   - `profile/personal/PersonalDataScreen.kt:184` (Done → `Save` si `canSave`).

**Verificación:** S:LOG-017: con las credenciales escritas, ✓ del teclado → `POST /auth/login` y entra a Inicio.

---

##### F-51 · El ojo de la contraseña dice "Show password" en inglés — BAJO · PARCIAL

**Qué ve QA hoy (03/11):** S:LOG-090 (`inspect_screen` en Login y en Cambiar contraseña, R08, F-51).
- Las etiquetas de los campos están OK: un TextView hijo del EditText expone "Correo electrónico" y "Contraseña".
- El ojo dice "Show password" con la app en español.

**Qué ya está hecho:** la etiqueta de los campos. QA lo da por bueno y no hay que tocarlo.

**Causa en el código:** `core/ui/component/QhathiaTextField.kt:148-152`.
```kotlin
contentDescription = if (passwordVisible) {
    "Hide password"
} else {
    "Show password"
},
```
Son literales en inglés.

**Qué garantiza el backend:** no aplica.

**Cambio:**
1. En `core/ui/src/main/res/values/strings.xml`: `password_show_cd` = "Mostrar contraseña" y `password_hide_cd` = "Ocultar contraseña". En `values-en/strings.xml`: "Show password" y "Hide password".
2. En `QhathiaTextField`: `contentDescription = stringResource(if (passwordVisible) R.string.password_hide_cd else R.string.password_show_cd)`.

**Verificación:** S:LOG-090: `inspect_screen` en Login y en Cambiar contraseña → el ojo expone "Mostrar contraseña", y "Ocultar contraseña" tras tocarlo.

---

##### F-70 · Datos personales y Cambiar contraseña salen sin "¿Descartar cambios?" — BAJO · NO CORREGIDO

**Qué ve QA hoy:**
- 03/11, S:LOG-077 (R08, F-70): Cambiar contraseña con los tres campos escritos → Atrás → vuelve a Seguridad sin preguntar.
- 31/10, S:PER-030: Datos personales con el teléfono cambiado → Volver → Perfil, sin preguntar.
- Esperado: "¿Descartar cambios?", y "Seguir editando" conserva el texto.

**Qué ya está hecho:** `PersonalDataUiState.isDirty` existe (`profile/personal/PersonalDataViewModel.kt:64`). El patrón a copiar está en `features/product-detail/.../ui/ProductDetailScreen.kt:52` (`BackHandler(enabled = uiState.isDeleting || (uiState.isDirty && !uiState.isSaving))`), y el diálogo en `ProductDetailContent.kt:427-436`, con los textos `detail_discard_title`, `detail_discard_message`, `detail_discard_confirm` ("Descartar") y `detail_discard_dismiss` ("Seguir editando"), en `features/product-detail/src/main/res/values/strings.xml:63-66`.

**Causa en el código:**
- `profile/personal/PersonalDataScreen.kt:100` (`QhathiaGradientTopBar(title = …, onBack = onBack)`) y `profile/security/ChangePasswordScreen.kt:66`: el chevron y el Atrás del sistema van directo al `popBackStack`, sin `BackHandler`.
- `ChangePasswordUiState` (`profile/security/ChangePasswordViewModel.kt:29-47`) no tiene `isDirty`.

**Qué garantiza el backend:** no aplica.

**Cambio:**
1. Mover el diálogo a `core/ui` como `QhathiaDiscardChangesDialog(onConfirm, onDismiss)`, con los cuatro textos copiados a `core/ui/src/main/res/values{,-en}/strings.xml` (`discard_changes_*`). Si quieres, úsalo también en Detalle y Registro.
2. Datos personales:
   - `PersonalDataUiState.isDiscardDialogVisible`.
   - Eventos `BackRequested`, `DiscardConfirmed` y `DiscardDismissed`; efecto `NavigateBack`.
   - `BackRequested`: si `isSaving`, ignorar; si `isDirty`, mostrar el diálogo; si no, `NavigateBack`.
   - En la pantalla: `BackHandler(enabled = uiState.isDirty || uiState.isSaving) { onEvent(BackRequested) }`, y el `onBack` del top bar también → `onEvent(BackRequested)`. Al colectar `NavigateBack` → `onBack()`.
3. Cambiar contraseña:
   - `val isDirty get() = currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmation.isNotEmpty()`.
   - El mismo flujo de eventos y el mismo `BackHandler`.
   - Durante `isSubmitting`, ignorar Atrás.
4. Tests:
   - `PersonalDataViewModelTest`: `backRequested_whenDirty_showsDiscardDialog`
   - `ChangePasswordViewModelTest`: `backRequested_whenEmpty_navigatesBack` y `discardDismissed_keepsTypedValues`

**Verificación:** S:PER-030 y S:LOG-077: escribir → Atrás (sistema y chevron) → "¿Descartar cambios?". "Seguir editando" conserva el texto y "Descartar" vuelve.

---

##### F-67 · No se puede quitar un teléfono guardado — BAJO · NO CORREGIDO

(Hoy no se re-ejecutó el lado celular: QA no quiso tocar los datos del dueño. El veredicto es del 31/10, y el código sigue igual.)

**Qué ve QA:**
- 31/10, S:EXP-021 (`2026-10-31-R08.md`, F-67): Datos personales → vaciar el teléfono → "Ingresa un celular de 9 dígitos que empiece con 9" y "Guardar cambios" deshabilitado.
- 03/11: el backend responde `{"phone":"12345"}` → 422 (OK).
- Esperado: con el teléfono vacío, "Guardar" habilitado → 200 → `phone: null`.

**Qué ya está hecho:** el envío ya está preparado.
- `profile/personal/PersonalDataViewModel.kt:135`: `val phone = state.phone.takeIf { it != saved.phone.orEmpty() }` manda `""` cuando se vacía.
- `core/network/service/UserApiService.kt:31`: `phone?.let { put("phone", it) }` lo pone en el JSON.

Solo lo bloquea la validación local.

**Causa en el código:** `profile/personal/PersonalDataValidation.kt:15-20`.
```kotlin
 * A 9-digit Peruvian mobile starting with 9. Empty is valid only when the profile had no phone:
 * the update is partial, so an existing phone cannot be removed.
 */
fun isValidPhone(phone: String, storedPhone: String?): Boolean =
    if (phone.isEmpty()) storedPhone == null else PERUVIAN_MOBILE.matches(phone)
```

**Qué garantiza el backend** (§36): `PUT /api/v1/users/me {"phone": ""}` → 200 con `phone: null`. Un `null` o la ausencia del campo = no tocar. Un número inválido → 422 `VALIDATION_ERROR`.

**Cambio:**
1. `isValidPhone(phone, storedPhone)` → `phone.isEmpty() || PERUVIAN_MOBILE.matches(phone)`. Actualiza el KDoc. El parámetro `storedPhone` puede quedarse o quitarse; si lo quitas, cambia `PersonalDataViewModel.kt:57`.
2. UX:
   - `personal_phone_hint` = "Celular de 9 dígitos (opcional)".
   - Si se vacía un teléfono guardado, debajo del campo: "Se quitará tu celular al guardar" (`personal_phone_will_remove`, es/en).
3. Tests: reemplazar `an existing phone cannot be cleared` (`PersonalDataViewModelTest.kt:116`) por `clearing a stored phone enables save and sends an empty string`. Debe esperar `FakeUserProfileRepository.Update(fullName = null, phone = "")` y, tras el éxito con `phone = null`, el campo vacío.

**Verificación:** S:EXP-021: vaciar el teléfono → "Guardar cambios" habilitado → `PUT /users/me` con `"phone":""` → 200. Al volver a entrar, el campo está vacío y `GET /users/me` da `phone: null`. S:PER-026: sin teléfono previo, cambiar solo el nombre → el body lleva solo `fullName`.

---

#### Orden sugerido de implementación

1. **P-1** (ProblemDetail) y el parámetro `keyboardActions` de `QhathiaTextField` (F-50). Son pequeños y los demás los usan.
2. **Vinculación:** F-58 → F-60 + F-01 → F-59. Mismo diálogo, mismo VM y mismo repositorio; conviene hacerlos en una sola pasada.
3. **Dispositivos:** P-2 → F-65 → F-09 → F-61 → F-62 → F-63. Todo en `DeviceLinkingViewModel`, `DeviceCard` y `TabletSettingsViewModel`.
4. **Sesión y seguridad:** F-48 → F-52 → F-53 → F-73 → F-49 → F-51 → F-70 → F-67.

Al terminar, en cada módulo tocado, `./gradlew :<módulo>:testDebugUnitTest`, una compilación a la vez.

#### Lo que depende del backend (no lo puede resolver la app)

- **F-59, nombres repetidos.** `DeviceService.defaultDeviceName` (`services/auth-api-service/.../service/DeviceService.java:505-511`) calcula `"Tablet " + (tablets ACTIVE + 1)`. Repite nombres no solo cuando se vinculan dos a la vez: también en secuencia después de un unlink. Por ejemplo, si están activas "Tablet 1" y "Tablet 3" (la 2 se desvinculó), la siguiente vuelve a ser "Tablet 3". Eso encaja con QA: `0d881a2d` y `35067926` se vincularon a las 06:48 y a las 07:01, con 13 minutos de diferencia, no a la vez. La app lo esquiva mandando siempre un nombre libre (F-59, paso 1), pero el backend debería buscar el primer N libre entre los nombres de las ACTIVE.
- **F-09, 409 al acusar un comando vencido.** No implementado. Solo afecta a la tablet (una tablet que acusa un RESTART ya EXPIRED). El celular no lo necesita.
- **F-59, nombre por heartbeat.** No implementado y no hace falta para el celular: el nombre se pone al vincular o con `PATCH`.
- **F-74 (relacionado con F-48), biz y vo no verifican el `sid`** (§36). Una sesión cerrada sigue leyendo biz hasta que vence su access. No bloquea ningún arreglo de esta lista.
- **F-73.** Nada pendiente: el tope de 20 y `X-Total-Count` ya están. Ojo para QA: `sim-tablet.sh` abre una sesión por llamada y el tope desaloja la del celular (trampa ya anotada en `2026-11-03-R02.md`).

---

### 3.2 · Notificaciones, Solicitudes, Inicio, Perfil y Estadísticas (R09, R10)

**Arreglos de este bloque:** F-80, F-54, F-68, F-56, F-89, F-90, F-91, F-55, F-57, F-75, F-76, F-69, F-71. Vienen de los bloques de regresión R09 y R10.

**Versión probada.** `qhathia-mobile-staff` en `2c6aa0f` (Staff 1.37.0-SNAPSHOT), la misma que QA probó el 31/10 y el 03/11.

**Rutas.** Son relativas a la raíz del repo y usan estos prefijos:
- `app/` = `app/src/main/java/com/qhathia/mobile/staff/`
- `core/<m>/` = `core/<m>/src/main/java/com/qhathia/mobile/staff/core/<m>/`
- `features/<f>/` = `features/<f>/src/main/java/com/qhathia/mobile/staff/feature/<pkg>/`

Abrí cada línea citada en `2c6aa0f`.

**Contexto que vale para todo el bloque:**
- Los fixes de `qa/consolidado/fixes/fixes-staff-A.md` y `fixes-staff-B.md` se escribieron sobre `0ca3de8`. Entre `0ca3de8` y `2c6aa0f` solo entraron dos cambios: los ejemplos y "También te piden" de Inicio (`a85f6f1`) y la cuenta regresiva del descuento en Detalle (`ee8608e`, `db2d5c1`). **Ninguno toca estos 13 arreglos.** El traspaso anterior (`docs/handoff/android-pendientes.md`) solo los listaba con la etiqueta "Solo app" o "Backend listo" y remitía a los fixes, así que en todos falta la implementación entera. Cada bloque dice qué piezas viejas ya sirven.
- **Orden recomendado:** primero **F-80**, porque crea la señal "los pedidos cambiaron" que también usan F-54 y F-90. Después F-90, F-54 y F-91, que son la misma pantalla, y por último el resto.
- **Reglas del repo que hay que respetar:**
  - MVI: `UiState` / `UiEvent` / `UiEffect` + ViewModel con `StateFlow`.
  - Los casos de uso van en `core/domain/usecase`.
  - Los fakes de `core/testing` tienen que seguir las interfaces.
  - Los tests son JUnit 5 + Turbine y van en `src/test`.
  - Todo string nuevo va en `values/` y en `values-en/`.

---

##### F-80 · Solicitudes no se refresca con el push — ALTO · NO CORREGIDO

> Es el más grave de este bloque: el vendedor no ve los pedidos nuevos y sí ve como pendientes pedidos que ya se atendieron.

**Qué ve QA hoy (03/11):**
- **S:SOL-061 / X:XPE-021** (`qa-reports/2026-11-03-R09.md`):
  - Con la app abierta en Solicitudes, la tablet simulada crea 3 pedidos a las 07:59:23 y los pushes llegan.
  - A los 3 s y a los 15 s la pantalla sigue en "Pendientes (0) · Sin solicitudes pendientes".
  - nginx no registra ningún `GET /orders` después de las 07:59:11. Tocar el grupo de notificaciones tampoco recarga.
  - Esperado: en ≤ 3 s la lista, el chip "Pendientes (N)" y el badge suben en 3.
- **Por la tarde** (`2026-11-03-pendientes.md`, nota "Además (F-80)" en F-90): la lista seguía mostrando dos pedidos que ya se habían rechazado por curl y ninguno de los tres nuevos, hasta tocar "Actualizar".
- **El 31/10, con la misma app** (`2026-10-31-R09.md`):
  - X:EXP-001: al volver a la pestaña, el chip decía "Pendientes (3)" y había 0 tarjetas.
  - El badge marcaba 2 con 0 pendientes.

**Qué ya está hecho:**
- `ORDER_TAKEN` borra la notificación del pedido: `core/notifications/OrderPushAction.kt:76-81` → `OrderNotificationPresenter.kt:85-87`.
- Cada `ON_START` del shell concilia la bandeja con el servidor y refresca el contador: `app/navigation/MainShell.kt:120` → `MainShellViewModel.kt:37-39` → `core/domain/usecase/ReconcileOrderNotificationsUseCase.kt:24-44`.
- Inicio ya recarga al volver con `LifecycleEventEffect(ON_START)` y se salta el primero (`features/home/ui/HomeScreen.kt:44`, `HomeViewModel.kt:99-102`). **Ese es el patrón que hay que copiar.**
- Sin red, "Ver detalle" ya abre el diálogo con "No se pudo cargar el detalle de la solicitud" (QA, pendientes F-91 · EXP-008), pero sin botón "Reintentar".

**Causa en el código.** Son cinco huecos que se suman:
1. **El push no toca la capa de datos.** `core/notifications/QhathiaMessagingService.kt:33-39` solo pinta o borra la notificación:
   ```kotlin
   override fun onMessageReceived(message: RemoteMessage) {
       val data = message.data
       presenter.handleOrderPush(
           event = data[KEY_EVENT],
           orderId = data[KEY_ORDER_ID],
       )
   }
   ```
2. **La lista solo se recarga a mano.** `features/order-requests/ui/OrderRequestsViewModel.kt:168-178` combina `searchQuery`, `selectedFilter` y `reloadTrigger`. `reloadTrigger` solo sube con `Load`, `Retry` y `Refresh` (`:98-103`, el botón "Actualizar") o después de confirmar o rechazar.
3. **La pantalla no mira el ciclo de vida.** `features/order-requests/ui/OrderRequestsScreen.kt:30-81` no tiene `LifecycleEventEffect`. Como `switchTab` guarda y restaura el estado (`MainShell.kt:132-138`, `saveState = true` / `restoreState = true`), el ViewModel sobrevive al cambio de pestaña y al volver se ve la lista vieja.
4. **El contador y la lista no salen de la misma fuente.**
   - El chip y el badge leen `OrderRepositoryImpl.pendingCount` (`core/data/repository/OrderRepositoryImpl.kt:38-40`), que se actualiza con cualquier consulta de pendientes sin filtro (`:82-84`).
   - El reconcile del `ON_START` pide `size=50` y publica el contador, pero la pantalla nunca recibe esa página. Así sale "Pendientes (3)" con 0 tarjetas.
5. **Tocar la notificación estando ya en Solicitudes no hace nada.** `MainShell.kt:150-154` llama a `switchTab(ORDER_REQUESTS_ROUTE)`, que con `launchSingleTop` no navega. La Activity solo pasa por `onPause` / `onNewIntent` / `onResume`, sin `ON_STOP`, así que tampoco llega un `ON_START`.

**Qué garantiza el backend** (`API_REFERENCE.md` §9.6, §20 "Qué pasa con los pushes", §25 "Aviso este pedido está por vencer"):
- El push es FCM **data-only y de prioridad HIGH** (`FcmPushGateway.java:79-80`). Solo lleva IDs, sin datos personales:
  ```json
  { "event": "NEW_ORDER | ORDER_TAKEN | ORDER_EXPIRING", "orderId": "…", "storeId": "…", "status": "PENDING | CONFIRMED | REJECTED | EXPIRED" }
  ```
- Cada evento significa:
  - `NEW_ORDER`: la tablet creó un pedido.
  - `ORDER_TAKEN`: el pedido se confirmó, se rechazó o venció. **Se entrega siempre**, sin importar las preferencias, porque sirve para sincronizar.
  - `ORDER_EXPIRING`: el pedido sigue en `PENDING` y vence en 10 min.
- Va a **todos** los tokens de la tienda (fan-out).
- "El push es el acelerador, no la fuente de verdad." La lista sale de `GET /api/v1/orders?status=PENDING&page=0&size=50` y el contador de `?status=PENDING&size=1` → `totalElements` (§ pedidos).
- QA midió el push en ≤ 3 s.

**Cambio:**
1. **Señal de cambio en el repositorio** (`core/domain/repository/OrderRepository.kt` y `core/data/repository/OrderRepositoryImpl.kt`):
   ```kotlin
   // OrderRepository
   /** Fires when orders changed elsewhere (push, notification tap); screens re-query on it. */
   val ordersChanged: Flow<Unit>
   fun notifyOrdersChanged()

   // OrderRepositoryImpl
   private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
   override val ordersChanged: Flow<Unit> = changes.asSharedFlow()
   override fun notifyOrdersChanged() { changes.tryEmit(Unit) }
   ```
   Agrega lo mismo a `core/testing/FakeOrderRepository.kt`, con un contador `notifyOrdersChangedCount` para los tests.
2. **Casos de uso nuevos** en `core/domain/usecase/`:
   - `ObserveOrdersChangedUseCase` (`operator fun invoke() = repository.ordersChanged`)
   - `NotifyOrdersChangedUseCase`
   - `HandleOrderPushUseCase`:
   ```kotlin
   class HandleOrderPushUseCase @Inject constructor(private val repository: OrderRepository) {
       /** Fail-open: a push that cannot refresh anything must never crash the FCM callback. */
       suspend operator fun invoke(event: String?) {
           if (event != EVENT_NEW_ORDER && event != EVENT_ORDER_TAKEN) return
           repository.notifyOrdersChanged()
           runCatching { repository.refreshPendingOrderCount() }
       }
       private companion object {
           const val EVENT_NEW_ORDER = "NEW_ORDER"
           const val EVENT_ORDER_TAKEN = "ORDER_TAKEN"
       }
   }
   ```
   `ORDER_EXPIRING` no cambia la lista: no hace falta recargar.
3. **`QhathiaMessagingService`.** Inyecta el caso de uso y llámalo después del presenter. Usa el mismo patrón `runBlocking` que `onNewToken` (`:28-31`), con un tope de tiempo:
   ```kotlin
   @Inject lateinit var handleOrderPush: HandleOrderPushUseCase

   override fun onMessageReceived(message: RemoteMessage) {
       val data = message.data
       presenter.handleOrderPush(event = data[KEY_EVENT], orderId = data[KEY_ORDER_ID])
       runBlocking { withTimeoutOrNull(PUSH_SYNC_TIMEOUT_MS) { handleOrderPush(data[KEY_EVENT]) } }
   }
   // companion: const val PUSH_SYNC_TIMEOUT_MS = 5_000L
   ```
   Esto corrige el badge y el chip al instante, aunque la app esté en otra pestaña o en segundo plano con el proceso vivo.
4. **`OrderRequestsViewModel`:**
   - En `init`, después de `observeLoads()`:
     ```kotlin
     observeOrdersChangedUseCase()
         .debounce(ORDERS_CHANGED_DEBOUNCE_MS) // 3 pedidos seguidos = 3 pushes = una sola recarga
         .onEach { reload(quiet = true) }
         .launchIn(viewModelScope)
     ```
   - Evento nuevo `OrderRequestsUiEvent.ScreenStarted` con el mismo manejo que Inicio:
     ```kotlin
     private var hasStarted = false
     OrderRequestsUiEvent.ScreenStarted -> { if (hasStarted) reload(quiet = true); hasStarted = true }
     ```
   - Recarga silenciosa: `private var suppressNextLoadError = false`. `reload(quiet)` lo pone en `quiet`. En `applyOutcome` (`:189-206`) emite `ShowError(R.string.order_error_load)` solo si `!suppressNextLoadError`, y después lo vuelve a `false`. `isError` se sigue marcando (si la lista está vacía se ve el `ErrorState` con "Reintentar").
   - `const val ORDERS_CHANGED_DEBOUNCE_MS = 300L`.
5. **`OrderRequestsScreen`.** Igual que `HomeScreen.kt:44`, agrega:
   ```kotlin
   LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onEvent(OrderRequestsUiEvent.ScreenStarted) }
   ```
   Esto cubre volver a la pestaña, volver del segundo plano (S:RED-042) y desbloquear.
6. **Tocar una notificación estando ya en Solicitudes.**
   - En `MainShell.kt:150-154`, después de `switchTab(ORDER_REQUESTS_ROUTE)`, llama a `viewModel.onOrdersDeepLink()`.
   - En `MainShellViewModel` agrega `fun onOrdersDeepLink() = notifyOrdersChanged()`, inyectando `NotifyOrdersChangedUseCase`.
   - Lo necesita también F-54 (el toque en el grupo).
7. **Diálogo sin red.** `OrderDetailDialog.kt:134-136` y `:443-457`: `DialogErrorState(onRetry = { onEvent(OpenDetail(state.orderId)) })`, con el texto en `colorScheme.error` y un `TextButton(R.string.order_retry)`. Hay que pasar un `onRetryDetail` a `OrderDetailDialog`.
8. **Tests:**
   - `OrderRequestsViewModelTest`:
     - `ordersChanged_reloadsPendingList`
     - `ordersChanged_burst_reloadsOnce`
     - `screenStarted_firstIsIgnored_secondReloads`
     - `quietReloadFailure_emitsNoSnackbar`
   - `HandleOrderPushUseCaseTest`:
     - `newOrder_notifiesAndRefreshesCount`
     - `orderExpiring_doesNothing`
   - `MainShellViewModelTest.onOrdersDeepLink_notifies`.

**No hacer:**
- No pasarle a la pantalla la página del reconcile ni montar una caché global de pedidos en el repositorio. La fuente de la lista sigue siendo la consulta de la pantalla (filtro + búsqueda); la señal solo dice "vuelve a pedir".
- No armar la tarjeta con el payload del push: no trae datos y no debe traerlos (§9.6, sin datos personales).
- No agregar polling periódico mientras la pantalla está abierta: con push + `ON_START` + reconcile alcanza.
- No poner `isLoading` de forma que borre la lista: `OrderRequestsContent.kt:120` ya muestra el spinner solo si la lista está vacía. Déjalo así.

**Verificación:**
- **S:SOL-061 / X:XPE-021.** Con Solicitudes abierta, crea 3 pedidos con `qa/conjunto/qa-plan/sim-tablet.sh order …`. En ≤ 3 s la lista, "Pendientes (N)" y el badge muestran N+3 sin tocar "Actualizar". En nginx debe verse **un** `GET /orders?status=PENDING&page=0&size=50` por ráfaga, gracias al debounce.
- **X:EXP-001.** Celular bloqueado → nuevo pedido → tocar la notificación. La lista trae el pedido nuevo.
- **Rechazo por curl.** La tarjeta desaparece al llegar `ORDER_TAKEN`.
- **X:EXP-013.** Sin red, "Ver detalle" muestra el error en rojo con "Reintentar".
- **S:RED-042.** Fondo 10 min → volver. Solicitudes recarga sola.

---

##### F-54 · Tocar el grupo de notificaciones no abre Solicitudes ni recarga — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):**
- **X:XPE-022** (`2026-11-03-R09.md`):
  - Con 3 pushes, el sistema arma su propio grupo (`tag g:Aggregate_AlertingSection`, no `orders_summary`).
  - Con la app en Solicitudes, tocar el grupo borra las 3 notificaciones y la lista no se recarga.
- **S:EXP-002, el 31/10.** Con la app en segundo plano sobre Configuración, tocar el grupo vuelve a Configuración.
- **Esperado:** abre Solicitudes · Pendientes con la lista recargada, y `dumpsys notification` muestra un resumen con tag `orders_summary`.

**Qué ya está hecho:**
- Hay una notificación por pedido, con el `orderId` como tag y el id fijo `1` (`OrderNotificationPresenter.kt:62-67`).
- Tocar una notificación **suelta** abre Solicitudes: SOL-060 pasó hoy, con `GET …size=50` y sin `GET /orders/{id}`.

**Causa en el código:**
- `core/notifications/OrderNotificationPresenter.kt:52-59`: el builder no llama a `setGroup(...)`, así que con 2 o más notificaciones Android crea su resumen automático, que no lleva el `PendingIntent` de la app:
  ```kotlin
  val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification_order)
      .setContentTitle(title)
      .setContentText(body)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(openAppIntent(orderId))
      .build()
  ```
- `:119-134`: el `contentIntent` con `EXTRA_DESTINATION = DESTINATION_ORDER_REQUESTS` solo existe en las hijas.
- Que no recargue estando ya en Solicitudes es el punto 5 de F-80.

**Qué garantiza el backend:** un push data-only por pedido (§9.6). El agrupado lo decide la app. `ORDER_TAKEN` llega siempre y borra la hija del pedido.

**Cambio** (en `OrderNotificationPresenter.kt`):
1. Constantes en el `companion`:
   ```kotlin
   private const val GROUP_KEY = "com.qhathia.mobile.staff.ORDERS"
   private const val SUMMARY_TAG = "orders_summary"
   private const val SUMMARY_ID = 2
   ```
2. En `post()`: agrega `.setGroup(GROUP_KEY)` al builder de la hija y, después del `notify`, `refreshSummary(including = orderId)`, dentro del mismo `try`.
3. Resumen propio:
   ```kotlin
   /** Ids of the order notifications in the tray, never a group summary (ours or the system's). */
   private fun childOrderIds(): Set<String> = orderNotifications()
       .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
       .mapNotNullTo(HashSet()) { it.tag }

   /**
    * Keeps one summary over the order notifications so a tap on the group opens Solicitudes.
    * [including]/[excluding] correct for the tray lagging behind a notify/cancel just issued.
    */
   private fun refreshSummary(including: String? = null, excluding: String? = null) {
       val ids = childOrderIds().toMutableSet().apply {
           including?.let(::add); excluding?.let(::remove)
       }
       val manager = NotificationManagerCompat.from(context)
       if (ids.isEmpty()) { manager.cancel(SUMMARY_TAG, SUMMARY_ID); return }
       if (!notificationsAllowed()) return
       val count = ids.size
       val summary = NotificationCompat.Builder(context, CHANNEL_ID)
           .setSmallIcon(R.drawable.ic_notification_order)
           .setContentTitle(context.resources.getQuantityString(R.plurals.notif_orders_summary_title, count, count))
           .setStyle(NotificationCompat.InboxStyle())
           .setGroup(GROUP_KEY)
           .setGroupSummary(true)
           .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
           .setAutoCancel(true)
           .setContentIntent(openAppIntent(SUMMARY_TAG))
           .build()
       try { manager.notify(SUMMARY_TAG, SUMMARY_ID, summary) } catch (_: SecurityException) { }
   }
   ```
4. `cancel(orderId)`: después del `cancel`, llama a `refreshSummary(excluding = orderId)`. `cancelUnmatched()`: al final, `refreshSummary()`.
5. `activeOrderIds()` (`:82-83`) debe devolver `childOrderIds()`, para que el reconcile no trate el resumen como un pedido.
6. Plural nuevo en `core/notifications/src/main/res/values/strings.xml`:
   ```xml
   <plurals name="notif_orders_summary_title">
       <item quantity="one">%d orden por atender</item>
       <item quantity="other">%d órdenes por atender</item>
   </plurals>
   ```
   Y en `values-en`: "%d order to handle" / "%d orders to handle".
7. Toque en el resumen con la app ya en Solicitudes: cubierto por F-80, paso 6 (`onOrdersDeepLink`).
8. Test con Robolectric o instrumentado: 2 `NEW_ORDER` → existe la notificación `orders_summary` con `contentIntent` cuyo extra es `DESTINATION_ORDER_REQUESTS`; después, 2 `ORDER_TAKEN` → el resumen desaparece.

**No hacer:**
- No cancelar el resumen mientras queden hijas: Android cancela con él a las hijas del grupo. Con una sola hija, deja el resumen con el conteo en 1.
- Que el toque en el grupo limpie las notificaciones es aceptable (`setAutoCancel`) **siempre que** abra Solicitudes con la lista recargada. Lo que estaba mal era el destino.

**Verificación:**
- **S:EXP-002 / X:XPE-022.**
  - Con la app en segundo plano sobre Perfil → Configuración, crea 2 o 3 pedidos.
  - La bandeja muestra "Qhathia Staff · 3 órdenes por atender".
  - Al tocarla se abre Solicitudes · Pendientes con los 3 pedidos.
  - `adb shell dumpsys notification --noredact | grep -i orders_summary` encuentra el resumen.
- **Repetir con la app en primer plano en Solicitudes:** la lista se recarga (`GET …size=50`).

---

##### F-68 · Las preferencias prometen lo que el backend no envía — MEDIO · PARCIAL (backend corregido, celular no)

**Qué ve QA hoy (03/11):**
- **S:PER-045** (`2026-11-03-R09.md`):
  - "Pedido por expirar" dice "Aviso cuando queden 2 min".
  - Las 5 preferencias sin emisor tienen el switch encendido.
  - Perfil dice "7 de 7 alertas activas" (también en "Observado de paso" de `2026-11-03-R10.md`).
- **Backend: OK.** `orderExpiringWarningMinutes 10`. En `supported`, `newOrder` y `orderExpiring` vienen en `true` y las otras 5 en `false`.
- **Esperado** (`qa-plan/R09`): "Aviso 10 minutos antes…", las 5 filas con "Próximamente" y sin switch, y el hub en "2 de 2 alertas activas".
- **Ojo del plan:** los emisores de esas 5 preferencias **no existen** (BLOQUEADO del lado del backend). Lo que se puede hacer ya en la app es **no prometerlas**.

**Qué ya está hecho:** nada de la Fase 42.
- `NotificationPreferencesDto` (`core/network/dto/ProfileDtos.kt:66-77`) solo declara los 7 booleanos más `activeCount` y `totalCount`.
- `supported`, `supportedCount`, `supportedActiveCount` y `orderExpiringWarningMinutes` se descartan en silencio (`ignoreUnknownKeys = true`, `core/network/di/NetworkModule.kt:38`).

**Causa en el código:**
- `features/profile/src/main/res/values/strings.xml:130`: `<string name="notifications_order_expiring_subtitle">Aviso cuando queden 2 min</string>`. Además, `:135` dice "Cada lunes por WhatsApp" y en `values-en` están las líneas 127 y 132.
- `features/profile/notifications/NotificationPreferencesScreen.kt:48-79`: `Groups` pinta las 7 preferencias. `ToggleGroupSection` (`:145-164`) usa siempre `QhathiaSwitchRow`, que no tiene modo "Próximamente" (`core/ui/component/settings/QhathiaSettingsRows.kt:146-186`). En cambio, `QhathiaNavigationRow` sí lo tiene (`:92-140`).
- `features/profile/hub/ProfileHubViewModel.kt:109-114`: `AlertCounts(prefs.activeCount, prefs.totalCount)` cuenta las 7.
- `core/data/mapper/ProfileMappers.kt:52-64`: el mapper no lleva nada de lo nuevo.

**Qué garantiza el backend** (`API_REFERENCE.md` §42, "GET / PUT /api/v1/notifications/preferences"; `NotificationPreferencesResponse.java`):
```json
{
  "newOrder": true, "orderExpiring": true, "tabletDisconnected": true, "productOutOfStock": true,
  "lowBattery": true, "weeklyReport": true, "sellingSuggestions": true,
  "activeCount": 7, "totalCount": 7,
  "supported": { "newOrder": true, "orderExpiring": true, "tabletDisconnected": false,
                 "productOutOfStock": false, "lowBattery": false, "weeklyReport": false,
                 "sellingSuggestions": false },
  "supportedCount": 2, "supportedActiveCount": 2, "orderExpiringWarningMinutes": 10
}
```
- `activeCount` y `totalCount` no cambian, por compatibilidad.
- El `PUT` es parcial: los campos ausentes no se tocan.
- Las preferencias con `supported=false` se guardan, pero no envían nada.
- El aviso sale `orderExpiringWarningMinutes` antes del vencimiento (TTL de 60 min, así que el aviso llega a los 50 min).

**Cambio:**
1. **DTO.** Agrega a `NotificationPreferencesDto`:
   ```kotlin
   val supported: Map<String, Boolean> = emptyMap(),
   val supportedCount: Int? = null,
   val supportedActiveCount: Int? = null,
   val orderExpiringWarningMinutes: Int = 10,
   ```
2. **Dominio** (`core/domain/model/NotificationPreferences.kt`):
   ```kotlin
   data class NotificationPreferences(
       val values: Map<NotificationPreference, Boolean>,
       val activeCount: Int,
       val totalCount: Int,
       val supported: Set<NotificationPreference> = DEFAULT_SUPPORTED,
       val orderExpiringWarningMinutes: Int = 10,
   ) {
       fun isEnabled(preference: NotificationPreference): Boolean = values[preference] ?: true
       fun isSupported(preference: NotificationPreference): Boolean = preference in supported
       /** "N de M alertas activas" only counts alerts the backend actually sends. */
       val supportedActiveCount: Int get() = supported.count(::isEnabled)
       val supportedCount: Int get() = supported.size
       companion object {
           val DEFAULT_SUPPORTED = setOf(NotificationPreference.NEW_ORDER, NotificationPreference.ORDER_EXPIRING)
       }
   }
   ```
3. **Mapper** (`ProfileMappers.kt:52-64`):
   ```kotlin
   supported = NotificationPreference.entries
       .filterTo(mutableSetOf()) { supported[it.fieldName] == true }
       .ifEmpty { NotificationPreferences.DEFAULT_SUPPORTED },
   orderExpiringWarningMinutes = orderExpiringWarningMinutes,
   ```
   `fieldName` ya existe en `:67`.
4. **`QhathiaSwitchRow`.** Agrega `comingSoon: Boolean = false`, con el mismo patrón que `QhathiaNavigationRow`:
   - Si `comingSoon`, no se dibuja el `Switch` ni el `toggleable`.
   - Se pinta `RowTexts(…, comingSoon = true)`, que ya agrega la etiqueta `QhathiaComingSoonTag`.
   - La semántica es `Modifier.clearAndSetSemantics { contentDescription = stringResource(R.string.coming_soon_cd, title); disabled() }`. El string `coming_soon_cd` ("%1$s, próximamente") ya está en `core/ui/src/main/res/values/strings.xml:42`.
5. **Pantalla** (`ToggleGroupSection`):
   ```kotlin
   val supported = preferences.isSupported(toggle.preference)
   QhathiaSwitchRow(
       title = stringResource(toggle.title),
       subtitle = if (!supported) null else when (toggle.preference) {
           NotificationPreference.ORDER_EXPIRING -> pluralStringResource(
               R.plurals.notifications_order_expiring_subtitle,
               preferences.orderExpiringWarningMinutes, preferences.orderExpiringWarningMinutes)
           else -> toggle.subtitle?.let { stringResource(it) }
       },
       checked = preferences.isEnabled(toggle.preference),
       comingSoon = !supported,
       enabled = toggle.preference !in saving,
       onCheckedChange = { onEvent(NotificationPreferencesUiEvent.Toggle(toggle.preference, it)) },
       showDivider = index < group.toggles.lastIndex,
   )
   ```
   Con `supported=false` desaparece "Cada lunes por WhatsApp", que hoy no es verdad.
6. **Strings.** Reemplaza el `<string>` de `strings.xml:130` por un plural (y lo mismo en `values-en`):
   ```xml
   <plurals name="notifications_order_expiring_subtitle">
       <item quantity="one">Aviso %1$d minuto antes de que venza</item>
       <item quantity="other">Aviso %1$d minutos antes de que venza</item>
   </plurals>
   ```
   En inglés: "Alert %1$d minute(s) before it expires".
7. **Hub** (`ProfileHubViewModel.kt:112`): usa `AlertCounts(prefs.supportedActiveCount, prefs.supportedCount)`. El plural `menu_notifications_active` ya existe y da "2 de 2 alertas activas".
8. **`NotificationPreferencesViewModel.kt:122-131`.** No hace falta tocar `with()`: `supportedActiveCount` se deriva de `values`. Revisa que la UI optimista no mande `PUT` para filas `comingSoon`; no pueden mandarlo, porque no tienen `onCheckedChange`.
9. **Tests:**
   - `NotificationPreferencesViewModelTest.unsupportedPreference_isComingSoon`.
   - `ProfileHubViewModelTest.alertCounts_useSupportedOnly` ("2 de 2").
   - Mapper con `supported` vacío → `DEFAULT_SUPPORTED`.
   - Ajusta `FakeProfileRepositories.kt` si construye `NotificationPreferences`.

**No hacer:**
- No escribir el "10" fijo en el texto: usa `orderExpiringWarningMinutes`, que el VPS puede cambiar con `ORDERS_EXPIRING_WARNING_MINUTES`.
- No borrar las 5 filas ni dejar de mandar sus valores: el contrato ya está fijo para cuando existan los emisores. El día que el backend ponga una en `supported=true`, se habilita sola, sin tocar la app.

**Verificación:**
- **S:PER-045.**
  - Notificaciones muestra "Aviso 10 minutos antes de que venza".
  - Tablet desconectada, Producto agotado, Batería baja, Reporte semanal y Sugerencias aparecen con "Próximamente" y sin switch. `inspect_screen` muestra la descripción "Tablet desconectada, próximamente" en estado deshabilitado.
  - El hub dice "2 de 2 alertas activas"; si apagas "Nuevo pedido", pasa a "1 de 2".
- **X:EXP-002.** El push `ORDER_EXPIRING` ("Orden por vencer") llega unos 10 min antes de que venza el pedido.
- **BLOQUEADO (no es de la app):** la parte del plan que pide un push `PRODUCT_OUT_OF_STOCK` al confirmar un pedido que deja el stock en 0. Ese emisor no existe.

---

##### F-56 · "Agregar" y "Reabastecer" de Inicio no hacen nada — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):**
- **S:INI-013** (`2026-11-03-R10.md`):
  - En Inicio, "Productos no encontrados · Soldadora (3 personas)" → "Agregar" no hace nada.
  - "Productos para reponer · Cinta LED solar exterior" → "Reabastecer" tampoco.
  - La pantalla no cambia y nginx no registra peticiones.
- **Esperado:**
  - "Reabastecer" abre el Detalle de ese producto.
  - "Agregar" abre Registro con el nombre del término buscado.
  - "Guardar" vuelve a Inventario con la snackbar.

**Qué ya está hecho:**
- Las secciones ya pasan la sugerencia a la lambda: `features/home/ui/SuggestionSection.kt:50,64` (`onAdd: (MissingProductSuggestion) -> Unit`) y `:101,115` (`onRestock: (RestockSuggestion) -> Unit`).
- "También te piden" (`a85f6f1`) ya va sin botón (`:91`, `onAdd = null`). Está bien así y no hay que tocarlo.
- `RestockSuggestion.id` es el id real del producto (`core/domain/model/StoreSuggestions.kt:16-22`).
- `MissingProductSuggestion.name` ya viene con la primera letra en mayúscula (`:9-14`).

**Causa en el código:**
- `features/home/ui/HomeContent.kt:114-130`:
  ```kotlin
  onAdd = { /* Inert by design — see MissingProductCard. */ },
  …
  onRestock = { /* Inert by design — see RestockProductCard. */ },
  ```
- `features/home/navigation/HomeNavigation.kt:21-31`: `homeGraph` solo expone `onNavigateToOrderRequests` y `onNavigateToSalesStatistics`.
- `features/product-register/navigation/ProductRegisterNavigation.kt:9,25`: `PRODUCT_REGISTER_ROUTE = "product_register"`, sin argumentos.
- `features/product-register/ui/ProductRegisterViewModel.kt:47-53`: no recibe `SavedStateHandle`.

**Qué garantiza el backend:** no hace falta nada nuevo.
- El Detalle usa `GET /api/v1/products/{id}`. Ver al producto desde el celular no suma una `PRODUCT_VIEW`; eso solo pasa con el token DEVICE (Fase 24).
- El alta usa `POST /api/v1/products` como hoy.

**Cambio** (sigue el patrón MVI de Inicio: evento → efecto → callback del shell):
1. `HomeUiEvent`: `data class AddMissingProduct(val name: String)` y `data class RestockProduct(val productId: String)`.
2. `HomeUiEffect`: `data class NavigateToProductRegister(val prefillName: String)` y `data class NavigateToProductDetail(val productId: String)`. En `HomeViewModel.onEvent`, los dos eventos emiten su efecto con `viewModelScope.launch { _uiEffect.emit(…) }`.
3. En `HomeContent.kt:117` y `:128`:
   ```kotlin
   onAdd = { s -> onEvent(HomeUiEvent.AddMissingProduct(s.name)) }
   onRestock = { s -> onEvent(HomeUiEvent.RestockProduct(s.id)) }
   ```
   Quita los comentarios "Inert by design" y actualiza los KDoc de `MissingProductCard.kt:37-38` y `RestockProductCard.kt:49`.
4. `HomeScreen` y `homeGraph` reciben `onNavigateToProductRegister: (String) -> Unit` y `onNavigateToProductDetail: (String) -> Unit`, y los atienden en el `collect` de `HomeScreen.kt:32-40` con `rememberUpdatedState`, como los otros dos.
5. Ruta de Registro con nombre opcional (`ProductRegisterNavigation.kt`):
   ```kotlin
   const val PRODUCT_REGISTER_ARG_NAME = "name"
   private const val PRODUCT_REGISTER_BASE = "product_register"
   const val PRODUCT_REGISTER_ROUTE = "$PRODUCT_REGISTER_BASE?$PRODUCT_REGISTER_ARG_NAME={$PRODUCT_REGISTER_ARG_NAME}"
   fun productRegisterRoute(prefillName: String? = null): String =
       if (prefillName.isNullOrBlank()) PRODUCT_REGISTER_BASE
       else "$PRODUCT_REGISTER_BASE?$PRODUCT_REGISTER_ARG_NAME=${Uri.encode(prefillName)}"

   composable(
       route = PRODUCT_REGISTER_ROUTE,
       arguments = listOf(navArgument(PRODUCT_REGISTER_ARG_NAME) {
           type = NavType.StringType; nullable = true; defaultValue = null
       }),
   ) { … }
   fun NavController.navigateToProductRegister() = navigate(productRegisterRoute())
   ```
   **Atención:** `MainShell.kt:203` hoy navega a `PRODUCT_REGISTER_ROUTE`. Con el patrón nuevo mandaría el literal `{name}`. Cámbialo a `navController.navigate(productRegisterRoute())`.
6. `ProductRegisterViewModel`: agrega `savedStateHandle: SavedStateHandle` al constructor. En `init`, antes de `loadCategories()`:
   ```kotlin
   savedStateHandle.get<String>(PRODUCT_REGISTER_ARG_NAME)?.takeIf { it.isNotBlank() }?.let { name ->
       _uiState.update { it.copy(name = name) }   // no marcar isDirty: el comerciante aún no escribió nada
   }
   ```
   Actualiza `ProductRegisterViewModelTest.createViewModel()` (`:56`) para que pase `SavedStateHandle()`.
7. `MainShell.kt:193-200`, wiring:
   ```kotlin
   homeGraph(
       onNavigateToOrderRequests = { switchTab(ORDER_REQUESTS_ROUTE) },
       onNavigateToSalesStatistics = { period -> navController.navigate(salesStatisticsRoute(period)) },
       // Through Inventario, so onSaved/onRegistered (which write to previousBackStackEntry with the
       // Inventario keys, lines 207-245) land on the list and its snackbar shows.
       onNavigateToProductDetail = { id ->
           switchTab(INVENTORY_ROUTE)
           navController.navigate(productDetailRoute(id))
       },
       onNavigateToProductRegister = { name ->
           switchTab(INVENTORY_ROUTE)
           navController.navigate(productRegisterRoute(name))
       },
   )
   ```
8. Tests:
   - `HomeViewModelTest.restock_emitsNavigateToProductDetailWithProductId`
   - `HomeViewModelTest.addMissing_emitsNavigateToRegisterWithName`
   - `ProductRegisterViewModelTest.prefilledName_isSetAndNotDirty`

**No hacer:**
- No navegar al Detalle directamente encima de Inicio: `onSaved` escribiría el resultado en el `savedStateHandle` de Inicio y no aparecería la snackbar de Inventario.
- Si producto decidiera no tener este flujo, no dejes los botones inertes: no los dibujes (pasa `null` como en "También te piden").

**Verificación:**
- **S:INI-013.**
  - "Reabastecer" abre el Detalle del producto correcto (`GET /products/{id}` con el id de la sugerencia).
  - "Agregar" abre Registro con "Nombre" = "Soldadora".
  - "Guardar" vuelve a Inventario con la snackbar de alta.
  - Atrás desde el Detalle vuelve a Inventario.
- **X:XBU-007.** Igual que INI-013, desde la tarjeta de un término buscado en la tablet.

---

##### F-89 · "Compartir" en Resumen de ventas no hace nada — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):**
- **S:EST-003** (`2026-11-03-pendientes.md`, R10):
  - En Resumen de ventas "2 – 8 de noviembre", ya cargado, se toca Compartir a las 17:04.
  - No se abre el selector de Android y la actividad en primer plano sigue siendo la de Staff.
- **Esperado:** se abre el selector con el texto del periodo, y el botón está deshabilitado mientras la pestaña carga.

**Qué ya está hecho:** nada.

**Causa en el código:**
- `features/sales-statistics/ui/SalesStatisticsViewModel.kt:110-111` descarta el evento:
  ```kotlin
  // Designed, but there is nothing to share to yet.
  SalesStatisticsUiEvent.Share -> Unit
  ```
- `StatisticsTopBar.kt:65-71`: el `IconButton` siempre está habilitado.
- `model/SalesStatisticsUiEffect.kt:4-8`: solo existe `NavigateBack`.
- `SalesStatisticsScreen.kt:25-31`: no maneja otro efecto.

**Qué garantiza el backend:** no hace falta nada. Se comparte lo que la pantalla ya cargó: el resumen, el ranking (§42: `asOf`, `periodRevenue`, `productsSold`) y la actividad. El texto **no** lleva datos de clientes.

**Cambio:**
1. Efecto nuevo: `data class Share(val tab: StatisticsTab) : SalesStatisticsUiEffect`. En el ViewModel:
   ```kotlin
   SalesStatisticsUiEvent.Share -> {
       val state = _uiState.value
       if (state.isShareEnabled) viewModelScope.launch { _uiEffect.emit(SalesStatisticsUiEffect.Share(state.selectedTab)) }
   }
   ```
   Y en `SalesStatisticsUiState`:
   ```kotlin
   val isShareEnabled: Boolean get() = when (selectedTab) {
       StatisticsTab.SUMMARY -> summary.data != null && !summary.isLoading
       StatisticsTab.RANKING -> ranking.data != null && !ranking.isLoading
       StatisticsTab.ACTIVITY -> activity.data != null && !activity.isLoading
   }
   ```
2. `StatisticsTopBar(…, shareEnabled: Boolean)` → `IconButton(onClick = onShare, enabled = shareEnabled)`. En `SalesStatisticsContent.kt:69-72` pasa `uiState.isShareEnabled`. Actualiza el KDoc de `:29-31`.
3. En `SalesStatisticsScreen`, maneja el efecto con el contexto. El texto se arma en la UI porque el ViewModel no tiene `Resources`:
   ```kotlin
   is SalesStatisticsUiEffect.Share -> {
       val text = shareText(uiState, effect.tab, resources, locale)   // función interna en StatisticsShare.kt
       ShareCompat.IntentBuilder(context)
           .setType("text/plain")
           .setText(text)
           .setChooserTitle(R.string.statistics_share_title)
           .startChooser()
   }
   ```
   Pásale `uiState` actualizado con `rememberUpdatedState`.
4. `StatisticsShare.kt`, nuevo y puro, para poder testearlo. Usa `periodRangeLabel(...)` (`StatisticsFormatters.kt:28`) para el rango y `BigDecimal.toSolesLabel()` (`core/ui/component/sales/SalesFormatters.kt:10`) para los montos. Strings nuevos en `features/sales-statistics/src/main/res/values{,-en}/strings.xml`:
   - `statistics_share_title` "Compartir resumen"
   - `statistics_share_summary` "Resumen de ventas · %1$s\nVentas: %2$s (%3$d ventas, ticket promedio %4$s, %5$d unidades)"
   - `statistics_share_ranking_header` "Lo que más vende · %1$s"
   - `statistics_share_ranking_row` "%1$d. %2$s — %3$s"
   - `statistics_share_activity` "Actividad · %1$s\nClientes que usaron la tablet: %2$d · Productos consultados: %3$d · Pedidos confirmados: %4$d"

   Los campos salen de `SalesSummary.total` y `.kpis` (`core/domain/model/SalesSummary.kt:63`), de `SalesRanking.topSellers` y de `StoreActivity.uniqueSessions`, `.productViews` y `.confirmedOrders`.
5. Test: `StatisticsShareTest.summaryText_includesRangeAndTotal` y `SalesStatisticsViewModelTest.share_whileLoading_emitsNothing`.

**No hacer:** no compartir una imagen ni un PDF; para lo que pide el plan alcanza con texto plano.

**Verificación:**
- **S:EST-003.** En Resumen de ventas, Compartir abre el selector de Android con "Resumen de ventas · 2 – 8 de noviembre…".
- Al cambiar de periodo, mientras carga, el ícono está deshabilitado.

---

##### F-90 · Tras confirmar con la red cayéndose, la tarjeta sigue en Pendientes — BAJO · NO CORREGIDO

**Qué ve QA hoy (03/11):**
- **S:RED-012** (`2026-11-03-pendientes.md`, R09):
  - En el Detalle #858918 se toca "Confirmar pedido" (16:59:03.4) y se activa el modo avión (16:59:04.0).
  - `POST /orders/…/confirm 200` llega y el pedido queda CONFIRMED.
  - La app dice "Solicitud confirmada", pero la tarjeta 858918 **sigue** con "Rechazar" y "Ver detalle". Hay tres tarjetas con el chip "Pendientes (2)", y 8 s después sigue igual.
  - Con red, "Actualizar" la quita (eso está bien).
- **Esperado:** la tarjeta desaparece y el badge baja aunque la recarga falle.

**Qué ya está hecho:**
- La tarjeta ya esconde las acciones si el pedido no está `PENDING` (`features/order-requests/ui/OrderCard.kt:63`, `canAct = order.status == OrderStatus.PENDING && !expired`, `a19acbb`).
- `OrderRepositoryImpl.confirmOrder` relee el contador después del 200 (`:101-107`). Esa lectura alcanzó a salir: el chip pasó a 2.

**Causa en el código:**
- `features/order-requests/ui/OrderRequestsViewModel.kt:241-245`. Tras el 200 solo se pide una recarga:
  ```kotlin
  .onSuccess {
      finishMutation(closeDialog = true)
      _uiEffect.emit(OrderRequestsUiEffect.ShowConfirmedSnackbar)
      reload()
  }
  ```
- `:189-206`: si esa recarga falla, `applyOutcome` conserva `state.orders`, con el pedido ya confirmado.
- Pasa lo mismo al rechazar (`:272-276`) y en la rama "ya lo atendió otro" (`:255-260`).
- Si el vendedor toca "Rechazar" sobre esa tarjeta, recibe 422 y "Otro vendedor ya atendió este pedido" por un pedido que confirmó él mismo.

**Qué garantiza el backend** (§ pedidos, `POST /orders/{id}/confirm` y `/reject`):
- `200` devuelve el `OrderResponse` con `status: "CONFIRMED"` o `"REJECTED"` (con `confirmedAt` / `rejectedAt`, §38).
- Repetir sobre un pedido terminal da `422 INVALID_STATE_TRANSITION`. Nunca hay doble transición.
- Además, al resto de los celulares les llega un `ORDER_TAKEN`.

**Cambio** (necesita el `reload(quiet)` de F-80):
1. Quita la tarjeta en el momento del 200 y, si es un pedido que ya atendió otro, también. En `OrderRequestsViewModel`:
   ```kotlin
   /** Orders this screen saw leave PENDING; a stale list must never offer actions on them again. */
   private val settledOrderIds = mutableSetOf<String>()

   private fun dropSettled(orderId: String) {
       settledOrderIds += orderId
       _uiState.update { s ->
           if (s.selectedFilter != OrderRequestsFilter.PENDING) s
           else s.copy(orders = s.orders.filterNot { it.id == orderId }.toImmutableList())
       }
   }
   ```
   - En `confirmOrder` y `rejectOrder`, `onSuccess`: `finishMutation(true)`, luego `dropSettled(orderId)`, luego el snackbar y luego `reload(quiet = true)`.
   - En las ramas `AlreadyHandled` y `NotFound` (`:255-260` y `:285-289`): `dropSettled(orderId)` antes de `reload()`.
2. En `applyOutcome`, filtra lo ya atendido de una respuesta vieja que estaba en vuelo:
   ```kotlin
   orders = page?.orders
       ?.filterNot { it.id in settledOrderIds && it.status == OrderStatus.PENDING }
       ?.toImmutableList() ?: state.orders,
   ```
   Si el servidor todavía lo trae `PENDING`, es una lectura anterior a la mutación. Limpia `settledOrderIds` cuando una respuesta exitosa ya no lo trae.
3. Si falla la lectura del contador tras el 200, ajústalo a mano. En `OrderRepositoryImpl.confirmOrder` y `rejectOrder` (`:104-105` y `:117`):
   ```kotlin
   refreshPendingOrderCount().onFailure { pendingCount.update { (it - 1).coerceAtLeast(0) } }
   ```
4. Tests:
   - `OrderRequestsViewModelTest.confirmSuccess_reloadFails_cardRemoved_noErrorSnackbar`
   - `OrderRequestsViewModelTest.staleResponseAfterConfirm_doesNotBringCardBack`
   - `OrderRepositoryImplTest.confirmSuccess_countRefreshFails_decrements`

**No hacer:** no cambies la tarjeta a "confirmada" dentro de Pendientes; la pestaña es solo de `PENDING`. Sácala.

**Verificación:**
- **S:RED-012.** Ver detalle → "Confirmar pedido" → modo avión en < 1 s.
  - Sale "Solicitud confirmada", la tarjeta desaparece de Pendientes y el chip y el badge bajan, sin snackbar de error de carga.
  - Al volver la red, "Actualizar" no la trae de vuelta.

---

##### F-91 · El error sin red sale detrás del velo del diálogo — BAJO · PARCIAL

**Qué ve QA hoy (03/11)** (`2026-11-03-pendientes.md`, R09):
- **S:EXP-007 (OK).** Rechazar con "Otro motivo" → el teclado se cierra y se ve "Solicitud rechazada".
- **S:INV-033 (OK, adaptado).** "Aplicar" del descuento ya no llama a la red. Guardar sin red muestra "Sin conexión…" visible y conserva los valores.
- **S:EXP-008 (NO).**
  - Con el detalle de #885636 abierto y con red, se activa el modo avión y se toca "Confirmar pedido".
  - El diálogo sigue abierto (eso está bien), pero "Sin conexión. Inténtalo nuevamente" aparece **atenuada detrás del velo**.
  - Esperado: el error se lee dentro del diálogo.

**Qué ya está hecho:**
- EXP-007 e INV-033 pasan.
- Nota: `OrderRejectForm.kt` no oculta el teclado de forma explícita (`:139` llama a `onConfirm(reason)` sin `clearFocus`). Hoy funciona porque el `Dialog` se cierra y se lleva su ventana. Opcionalmente, agrega `LocalFocusManager.current.clearFocus(force = true)` antes de `onConfirm` como defensa.

**Causa en el código:**
- `features/order-requests/ui/OrderRequestsViewModel.kt:249-254`. El error de red de una acción del diálogo va al snackbar de la pantalla:
  ```kotlin
  ConfirmOrderError.Network, null -> {
      finishMutation(closeDialog = false)
      _uiEffect.emit(
          OrderRequestsUiEffect.ShowError(R.string.order_error_network),
      )
  }
  ```
  Al rechazar pasa lo mismo (`:279-284`).
- `OrderRequestsScreen.kt:64-79`: el `SnackbarHost` vive en la ventana de la Activity.
- `OrderDetailDialog.kt:93`: el diálogo es un `Dialog(onDismissRequest = onDismiss)`, con ventana y velo propios, que quedan por encima del snackbar.

**Qué garantiza el backend:** sin red no hay respuesta. Si el `POST` sí llegó (caso F-90), reintentar devuelve `422 INVALID_STATE_TRANSITION`.

**Cambio:**
1. En `model/OrderRequestsUiState.kt:125-133`, agrega a `OrderDetailUiState`: `@param:StringRes val submitErrorRes: Int? = null`.
2. ViewModel:
   - En `startMutation` (`:296-304`), pon `submitErrorRes = null`.
   - En las ramas de red de `confirmOrder` (`:249-254`) y `rejectOrder` (`:279-284`), en lugar de `ShowError`:
     ```kotlin
     mutatingOrderId = null
     _uiState.update { s -> s.copy(detail = s.detail?.copy(isSubmitting = false, submitErrorRes = R.string.order_error_network)) }
     ```
3. UI: en `OrderDetailBody`, justo encima de `OrderDetailActions` (`OrderDetailDialog.kt:194-199`), y en `OrderRejectForm`, encima de "Confirmar":
   ```kotlin
   state.submitErrorRes?.let { res ->
       Text(
           text = stringResource(res),
           style = MaterialTheme.typography.bodyMedium,
           color = MaterialTheme.colorScheme.error,
           textAlign = TextAlign.Center,
           modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
       )
   }
   ```
   Pasa `submitErrorRes` a `OrderDetailBody` y a `OrderRejectForm`.
4. Test: `OrderRequestsViewModelTest.confirmNetworkError_keepsDialogWithInlineError_noSnackbar`.

**No hacer:** no muevas el `SnackbarHost` dentro del `Dialog` como solución general, porque los snackbars de éxito se muestran con el diálogo ya cerrado.

**Verificación:**
- **S:EXP-008.** Detalle abierto → modo avión → "Confirmar pedido". "Sin conexión. Inténtalo nuevamente" se lee en rojo dentro del diálogo, sin velo encima.
- EXP-007 e INV-033 siguen pasando.

---

##### F-55 · Las notificaciones usan el ícono de Android — BAJO · NO CORREGIDO

**Qué ve QA hoy (03/11):** S:EXP-003 (`2026-11-03-R09.md`). En la bandeja, "QhathiaStaff · Nueva orden de compra" sale con el robot verde de Android. Esperado: el ícono de Qhathia en la bandeja y en el launcher.

**Qué ya está hecho:** nada.

**Causa en el código:** el proyecto conserva el ícono de plantilla de Android Studio.
- `app/src/main/res/drawable/ic_launcher_background.xml:7-9` tiene `android:fillColor="#3DDC84"`, y `ic_launcher_foreground.xml` es el robot.
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml` usa ese mismo foreground también como `<monochrome>`.
- Los `mipmap-*/ic_launcher*.webp` también son de la plantilla.
- `app/src/main/AndroidManifest.xml:13,17` apunta a `@mipmap/ic_launcher` y `@mipmap/ic_launcher_round`.
- El ícono pequeño de la notificación, `core/notifications/src/main/res/drawable/ic_notification_order.xml`, es la campana genérica de Material.
- `OrderNotificationPresenter.kt:52-59` no llama a `setColor`.

**Qué garantiza el backend:** el push es data-only. La notificación la arma la app, así que **no** aplica `default_notification_icon` de Firebase.

**Cambio:** `<<DEPENDE DE DISEÑO: isotipo de Qhathia en SVG (color) y su silueta monocroma>>`. En el repo no hay ningún logo.
1. Con Image Asset Studio, reemplaza `drawable/ic_launcher_foreground.xml` y `drawable/ic_launcher_background.xml`. Como fondo, usa el color de marca `#FF4500` (`QhathiaColors.BrandAccent`, `core/ui/theme/Color.kt:24`) o el que indique diseño. Reemplaza también todos los `mipmap-*/ic_launcher*.webp`.
2. Agrega `drawable/ic_launcher_monochrome.xml` (la silueta) y úsalo en `<monochrome>` de `mipmap-anydpi/ic_launcher.xml` e `ic_launcher_round.xml`.
3. Ícono pequeño: `core/notifications/src/main/res/drawable/ic_notification_qhathia.xml`. Silueta blanca de 24 dp, sin degradados ni alfa parcial. Úsalo en `post()` y en el resumen de F-54, y agrega `.setColor(ContextCompat.getColor(context, R.color.notification_accent))`, con un color nuevo en `core/notifications/src/main/res/values/colors.xml`.

**Verificación:** S:EXP-003. Un push de pedido muestra el ícono de Qhathia en la bandeja; el launcher y la pantalla de apps recientes también.

---

##### F-57 · La campana y la pestaña con badge no se anuncian como botón — BAJO · NO CORREGIDO

**Qué ve QA hoy (03/11):** S:INI-062 (`2026-11-03-pendientes.md`, R10, con `inspect_screen` y 1 pedido pendiente).
- La campana `[904,176][1036,308]`, con descripción "Notificaciones, 1 pendiente", es un `View` sin `clickable` ni rol Button.
- La pestaña `[540,2057][810,2181]`, "Solicitudes, 1 pendiente", tampoco tiene `clickable`.
- Lo que está bien: "Ver más" es Button, el gráfico es un solo nodo y las pills exponen `checked`.

**Qué ya está hecho:** al tocarlas funcionan; el problema es solo de semántica.

**Causa en el código:**
- `features/home/ui/HomeHeader.kt:101-104`:
  ```kotlin
  IconButton(
      onClick = onOpenNotifications,
      modifier = Modifier.clearAndSetSemantics { contentDescription = bellDescription },
  ) {
  ```
  El modificador del llamador va antes del `clickable` interno del `IconButton`. `clearAndSetSemantics` borra la acción de clic y el rol (pasa con o sin badge: el 31/10 se vio sin pendientes).
- `core/ui/component/QhathiaBottomBar.kt:91-108`: con badge, `.clearAndSetSemantics { selected; contentDescription }` va antes de `.clickable(onClick)`. Sin badge, las otras pestañas quedan `clickable`, pero sin rol de pestaña.

**Qué garantiza el backend:** no aplica.

**Cambio:**
1. Campana: declara todo de forma explícita en el mismo `clearAndSetSemantics`, que es determinista:
   ```kotlin
   IconButton(
       onClick = onOpenNotifications,
       modifier = Modifier.clearAndSetSemantics {
           contentDescription = bellDescription
           role = Role.Button
           onClick { onOpenNotifications(); true }
       },
   )
   ```
2. Pestaña (`QhathiaBottomBarItem`): una sola cadena para todas las pestañas, con o sin badge:
   ```kotlin
   Column(
       modifier = modifier
           .weight(1f)
           .selectable(selected = selected, role = Role.Tab, onClick = onClick)
           .then(
               if (badge != null && badgeContentDescription != null) {
                   Modifier.clearAndSetSemantics {
                       this.selected = selected
                       role = Role.Tab
                       contentDescription = badgeContentDescription
                       onClick { onClick(); true }
                   }
               } else Modifier
           )
           .padding(vertical = Dimens.SpaceXs),
       …
   )
   ```
   Borra el `.semantics { this.selected = selected }` (`:94`) y el `.clickable` (`:107`); `selectable` los reemplaza.
3. Test de UI con Compose:
   ```kotlin
   onNodeWithContentDescription("Solicitudes, 2 pendientes")
       .assertHasClickAction()
       .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
   ```
   Y lo mismo para "Notificaciones, 2 pendientes" con `Role.Button`.

**Verificación:**
- **S:INI-062.** `inspect_screen` muestra la campana y la pestaña con badge como `clickable=true`, y las 4 pestañas con `selected`/`checked`.
- Con TalkBack: "Notificaciones, 1 pendiente, botón" y "Solicitudes, 1 pendiente, pestaña".

---

##### F-75 · Peticiones duplicadas al cargar Inicio — BAJO · NO CORREGIDO

**Qué ve QA hoy (03/11):**
- **S:INI-004** (`2026-11-03-R10.md`). Al entrar (07:58:17) salen, en el mismo segundo, `GET /orders?status=PENDING&page=0&size=1` ×2 y `POST /notifications/tokens` ×2.
- **El 31/10:** el `size=1` doble salió en cada carga de Inicio (07:28:16, 10:22:07/08, 10:35:05/06).
- **S:PER-004 (hub de Perfil):** no se midió ninguno de los dos días.
- **Esperado:** un solo `GET …size=1` por carga de Inicio. En Perfil, un solo `GET /users/me`; al volver de Ayuda, ninguna consulta.

**Qué ya está hecho:** nada.

**Causa en el código:**
- **`size=1` doble** (atribución segura). Dos dueños del contador disparan la misma consulta en el mismo arranque:
  - `features/home/ui/HomeViewModel.kt:132-138`: cada `load()` hace `async { refreshPendingOrderCountUseCase() }`.
  - `app/navigation/MainShell.kt:120` → `MainShellViewModel.kt:37-39` → `ReconcileOrderNotificationsUseCase.kt:27-31`. Con la bandeja vacía llama otra vez a `repository.refreshPendingOrderCount()`.
  - `core/data/repository/OrderRepositoryImpl.kt:42-63` no deduplica las llamadas concurrentes.
- **`POST /notifications/tokens` ×2** (no lo pude atribuir con certeza). En el código, una sola vía llama al registro en cada arranque: `app/MainViewModel.kt:85-96`, al pasar a autenticado, con `distinctUntilChanged`. La otra vía, `onNewToken`, solo se dispara cuando FCM rota el token.
  - Hipótesis fuerte: a las 07:58 era el primer uso del día, con el access token vencido. El plugin Bearer de Ktor (`core/network/KtorClientFactory.kt:69-72`, `refreshTokens { … }`) **repite la petición después del 401 + refresh**. Así, cada petición aparece dos veces en nginx: una con 401 y otra con 200/204.
  - **Hay que pedirle a QA el código de estado de cada línea** antes de tocar nada por esto.
- **Hub de Perfil:** `features/profile/hub/ProfileHubViewModel.kt:59-61` hace `init { refresh() }` y además `ProfileHubScreen.kt:32-35` llama a `LifecycleResumeEffect(Unit) { viewModel.onEvent(ProfileHubUiEvent.Refresh) }` en cada resume. Volver de cualquier sub-pantalla dispara 4 consultas.

**Qué garantiza el backend:** nada que cambiar. El contador sale de `?status=PENDING&size=1` → `totalElements`.

**Cambio:**
1. Una sola consulta del contador en vuelo, sin caché temporal: así un push (F-80) siempre relee. En `OrderRepositoryImpl`:
   ```kotlin
   private val countMutex = Mutex()
   private var countGeneration = 0L

   override suspend fun refreshPendingOrderCount(): Result<Int> {
       val seen = countGeneration
       return countMutex.withLock {
           // Someone refreshed while we waited for the lock: their answer is ours.
           if (countGeneration != seen) return@withLock Result.success(pendingCount.value)
           fetchPendingCount().onSuccess { countGeneration++ }
       }
   }
   ```
   El cuerpo actual (`:43-63`) pasa a ser `private suspend fun fetchPendingCount()`.
2. Hub de Perfil:
   - Quita `refresh()` del `init`: el primer resume ya lo dispara.
   - Agrega una ventana de frescura: `Refresh(force: Boolean = false)`, que no hace nada si `clock.instant() - lastRefreshedAt < 30 s` y no es `force`.
   - Las sub-pantallas que cambian datos (Notificaciones, Datos personales, Dispositivos) devuelven `previousBackStackEntry?.savedStateHandle?.set("profile_changed", true)`. El hub lo lee en el resume y hace `Refresh(force = true)`.
3. Tests:
   - `OrderRepositoryImplTest.concurrentRefresh_singleRequest`, con un `MockEngine` que cuente llamadas.
   - `ProfileHubViewModelTest.resumeTwiceWithin30s_loadsOnce`.

**No hacer:** no quites el `refreshPendingOrderCount` de Inicio ni el del reconcile. Cada uno cubre un caso distinto: abrir Inicio y volver del segundo plano en otra pestaña. Lo que se arregla es que coincidan en el tiempo.

**Verificación:**
- **S:INI-004.** nginx muestra un solo `GET /orders?status=PENDING&page=0&size=1` por carga de Inicio. Hay que mirar el status: un par 401 + 200 no es este bug.
- **S:PER-004.** Abrir Perfil da un `GET /users/me`. Volver de Ayuda no dispara ninguna consulta. Volver de Notificaciones después de mover un switch sí recarga.

---

##### F-76 · "Última actualización" en 24 h con el sistema en 12 h — BAJO · NO CORREGIDO

**Qué ve QA hoy (03/11):** S:INI-004 (`2026-11-03-pendientes.md`, R10). Con `time_12_24=12` (la barra muestra "5:04"), la app dice "Última actualización a las 17:04". Esperado: "5:04 p. m."; con 24 h, "17:04".

**Qué ya está hecho:** nada.

**Causa en el código:** el formato sale del idioma de la app (en español el formato corto es `H:mm`) y no del ajuste "Usar formato de 24 horas" del teléfono. En inglés coincide por casualidad.
- `features/home/ui/HomeFormatters.kt:9-12`:
  ```kotlin
  internal fun Instant.toLocalTimeLabel(): String =
      DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
          .withZone(ZoneId.systemDefault())
          .format(this)
  ```
- La misma función está copiada en `features/order-requests/ui/OrderRequestsHeader.kt:169-172` y en `features/device-linking/ui/DeviceFormatters.kt:9-12` ("podrás reintentar a las …").
- `features/sales-statistics/ui/StatisticsFormatters.kt:17` usa `ofPattern("HH:mm")` fijo (en el rango por hora, `:39`).

**Qué garantiza el backend:** no aplica. Los instantes llegan en UTC y se muestran en la zona del teléfono.

**Cambio:**
1. Helper común en `core/ui/component/time/TimeOfDay.kt` (la carpeta ya existe, junto a `RelativeTime.kt`):
   ```kotlin
   /** Short time of day honouring the app locale AND the phone's 12/24-hour setting. */
   fun shortTimeFormatter(locale: Locale, is24Hour: Boolean): DateTimeFormatter =
       DateTimeFormatter.ofPattern(
           android.text.format.DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"),
           locale,
       ).withZone(ZoneId.systemDefault())

   @Composable
   fun rememberShortTimeFormatter(): DateTimeFormatter {
       val context = LocalContext.current
       val locale = LocalConfiguration.current.locales[0]
       val is24 = android.text.format.DateFormat.is24HourFormat(context)
       return remember(locale, is24) { shortTimeFormatter(locale, is24) }
   }
   ```
   En español da "5:04 p. m." con 12 h, y "17:04" o "00:36" con 24 h.
2. Reemplaza las tres copias de `toLocalTimeLabel` / `toShortLocalTime` por `rememberShortTimeFormatter().format(instant)`. Los usos están en `HomeHeader.kt:93`, `OrderRequestsHeader.kt:84` y `DeviceCard.kt:322`; los tres son `@Composable`.
3. Estadísticas: `periodRangeLabel(…, locale, is24Hour: Boolean = true)`. Usa `shortTimeFormatter(locale, is24Hour)` en lugar de `HOUR_FORMAT`. `SalesStatisticsContent.kt:56-61` le pasa `DateFormat.is24HourFormat(LocalContext.current)`. Por el valor por defecto, `StatisticsFormattersTest` sigue pasando.
4. Test: `TimeOfDayTest` con `Locale("es")` y los dos valores de `is24Hour`. Usa Robolectric, porque `getBestDateTimePattern` es de Android.

**Verificación:** S:INI-004.
- Sistema en 12 h + app en español → "Última actualización a las 5:04 p. m.".
- Sistema en 24 h → "17:04".
- De madrugada → "00:36".
- Mismo criterio en Solicitudes, en Dispositivos ("reintentar a las …") y en el rango "Por hora" de Estadísticas.

---

##### F-69 · "Soporte por WhatsApp" sin enlace — BAJO · BLOQUEADO

**Qué ve QA hoy (03/11):** S:PER-073, BLOQUEADO (`2026-11-03-R10.md`: "falta el número de WhatsApp de soporte (negocio)").
- Comportamiento actual: el hub de Perfil promete "Soporte por WhatsApp y guías", y la pantalla Ayuda solo tiene preguntas frecuentes, "Guías rápidas" con la etiqueta "Próximamente" y la versión. No hay ningún contacto.
- Esperado cuando se desbloquee: Perfil → Ayuda → "Escríbenos por WhatsApp" abre `wa.me/<número>` con el mensaje prellenado.

**Qué ya está hecho:** nada.

**Causa en el código:**
- `features/profile/src/main/res/values/strings.xml:52`: `<string name="menu_help_subtitle">Soporte por WhatsApp y guías</string>`. En inglés, `values-en/strings.xml:50`.
- `features/profile/help/HelpScreen.kt:92-118`: `HelpContent` = FAQ (`:101-105`) + guías con `comingSoon = true` (`:106-115`) + `VersionFooter` (`:161-193`). No hay fila de contacto.
- No existe ningún número de soporte en `BuildConfig`: `app/build.gradle.kts:39` solo define `BASE_URL`.

**Qué garantiza el backend:** no interviene. El enlace es `https://wa.me/<número>?text=…` y no pasa por el backend.

**Cambio (listo para aplicar en cuanto llegue el número):**
1. `app/build.gradle.kts`, en `defaultConfig`, junto a `BASE_URL`:
   ```kotlin
   buildConfigField("String", "SUPPORT_WHATSAPP", "\"<<DECISIÓN PENDIENTE: número de WhatsApp de soporte, internacional sin '+' (p. ej. 51 + 9 dígitos)>>\"")
   ```
2. Exponlo sin que el feature dependa de `BuildConfig`. En `core/common`, crea `data class SupportContact(val whatsAppNumber: String)`. En `app/di/AppModule.kt`:
   ```kotlin
   @Provides @Singleton
   fun provideSupportContact(): SupportContact = SupportContact(BuildConfig.SUPPORT_WHATSAPP)
   ```
   Como `HelpScreen` no tiene ViewModel (`:74-77`), crea un `HelpViewModel` mínimo que exponga el número, o pásalo desde `ProfileNavigation.kt:87` con un `hiltViewModel` del grafo.
3. `HelpContent`: sección "Contacto" antes de las FAQ.
   ```kotlin
   QhathiaSettingsSection(title = stringResource(R.string.help_contact_title)) {
       QhathiaNavigationRow(
           title = stringResource(R.string.help_whatsapp_title),        // "Escríbenos por WhatsApp"
           subtitle = stringResource(R.string.help_whatsapp_subtitle),  // <<DECISIÓN PENDIENTE: horario de atención>>
           icon = Icons.AutoMirrored.Rounded.Chat,
           onClick = onOpenWhatsApp,
           showDivider = false,
       )
   }
   ```
   Y `onOpenWhatsApp` en `HelpScreen`:
   ```kotlin
   val message = context.getString(R.string.help_whatsapp_message, versionName ?: "")  // "Hola, necesito ayuda con Qhathia (app %1$s)"
   val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}")
   runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
       .onFailure { /* ActivityNotFoundException: snackbar "No encontramos una app para abrir WhatsApp" */ }
   ```
4. Strings nuevos `help_contact_title`, `help_whatsapp_title`, `help_whatsapp_subtitle` y `help_whatsapp_message`, en `values` y `values-en`.
5. **Mientras no haya número** (recomendado, salvo que producto diga otra cosa): cambia `menu_help_subtitle` a "Preguntas frecuentes y guías" / "FAQ and guides" y no dibujes la sección Contacto, para no prometer lo que no existe.

**No hacer:** no escribir un número de prueba ni uno personal. El número es un dato de negocio y va en `BuildConfig`.

**Verificación:** S:PER-073. Perfil → Ayuda → "Escríbenos por WhatsApp" abre WhatsApp (o el navegador) en `wa.me/<número>` con el mensaje prellenado. Sin WhatsApp instalado, se ve el aviso y la app no se cae.

---

##### F-71 · Atrás en una pestaña raíz vuelve a Inicio — BAJO · BLOQUEADO

**Qué ve QA hoy (03/11):** S:SOL-058, S:PER-010 y S:INI-005, BLOQUEADO por decisión de producto (`2026-11-03-R10.md`). El plan pide solo anotar el comportamiento actual (línea "Ojo" de `qa-plan/R10`).
- Comportamiento actual: Atrás en Solicitudes, Inventario o Perfil lleva a Inicio, y Atrás en Inicio saca la app al segundo plano.

**Qué ya está hecho:** no es un error de implementación. Es el patrón estándar de Navigation (la pestaña de inicio fija en la base de la pila), y el plan original de QA (`qa/staff/qa-plan/00-mapa.md` §1) esperaba otra cosa.

**Dónde está el código:**
- `app/navigation/MainShell.kt:132-138`. Deja la pila como `[home, <pestaña>]`:
  ```kotlin
  fun switchTab(route: String) {
      navController.navigate(route) {
          popUpTo(navController.graph.findStartDestination().id) { saveState = true }
          launchSingleTop = true
          restoreState = true
      }
  }
  ```
- `MainShell.kt:190`: `startDestination = HOME_ROUTE` del NavHost interno.
- La campana, el push y las tarjetas usan este mismo `switchTab` (`:147`, `:152`, `:194`, `:205`, `:251`).

**Qué garantiza el backend:** no aplica.

**Cambio, según lo que decida producto:** `<<DECISIÓN PENDIENTE: qué hace "Atrás" en una pestaña raíz distinta de Inicio — (A) volver a Inicio, como hoy, o (B) mandar la app al segundo plano>>`
- **(A) Mantener el comportamiento actual** (es lo que esperan los usuarios de Android): no se toca código. Se corrige el plan de QA (`00-mapa.md` §1 y los casos SOL-058, PER-010 e INI-005) para que espere "vuelve a Inicio".
- **(B) Salir de la app.** En `MainShell`, después de `currentRoute` (`:125`):
  ```kotlin
  val activity = LocalActivity.current
  val onNonHomeRoot = topLevelDestinations.any { d ->
      d.route != HOME_ROUTE && currentRoute?.hierarchy?.any { it.route == d.route } == true
  } && navController.previousBackStackEntry?.destination?.route == HOME_ROUTE
  BackHandler(enabled = onNonHomeRoot) { activity?.moveTaskToBack(true) }
  ```
  - `LocalActivity` viene de `activity-compose` 1.13, que ya está en `libs.versions.toml:12`.
  - La condición `previousBackStackEntry == HOME` hace que el handler solo actúe en la raíz de la pestaña, no sobre un Detalle o un Registro apilado encima.
  - Usa `moveTaskToBack(true)`, no `finish()`, para conservar el proceso y la sesión, igual que el Atrás de Inicio en Android 12+.
  - No cambies `switchTab`.

**Verificación:** S:SOL-058, S:PER-010, S:INI-005.
- Con (A): Atrás en Solicitudes, Inventario o Perfil lleva a Inicio, y desde Inicio la app pasa al segundo plano.
- Con (B): Atrás en cualquiera de esas pestañas manda la app al segundo plano. Al volver aparece en la misma pestaña, sin pedir login (salvo la huella, si pasaron más de 5 min).
- En los dos casos, Atrás desde un Detalle de Inventario vuelve a Inventario.

---

### 3.3 · Inventario, registro, detalle, layout, logs y backup (R11, R12)

Regresión QA del 03/11/2026 (bloques R11 y R12) contra **Staff 1.37.0-SNAPSHOT, commit `2c6aa0f`** del repo `android/qhathia-mobile-staff`. Backend en producción con las fases 35 a 43.

#### Antes de empezar: tres cosas que cambian todo

1. **La app no aplicó ningún arreglo de esta lista.** Los commits de `2c6aa0f` hacia atrás (del 31/10: cuenta regresiva del descuento, precio del servidor en el Detalle, ejemplos en Inicio) son anteriores a la campaña QA o no tienen relación con ella. Busqué `version`, `Idempotency-Key`, `PullToRefresh`, `visibleProducts`, `periodRevenue`, `productActive`, `errorCode` en los productos, `allowBackup`: nada de eso está en el código. Por eso en cada bloque, "Qué ya está hecho" se refiere a lo que **ya existe y se puede reutilizar**, no a avances del traspaso anterior. El traspaso anterior (`docs/handoff/android-pendientes.md`, sección "Campaña QA 25–28/10") solo remitía a `qa/consolidado/fixes/fixes-staff-A.md` / `fixes-staff-B.md`. Esos archivos siguen sirviendo, pero **sus números de línea están desfasados** y algunas partes del contrato cambiaron (por ejemplo, F-93 usa `version` en el body, no `If-Match`). Lo que dice este documento manda sobre ellos.
2. **Desde la Fase 40, `PUT /api/v1/products/{id}` ya no es de reemplazo total: lo que falta en el body se conserva.** La app sigue escrita como si fuera reemplazo total (KDoc de `ProductUpdate.kt`, `ProductApiService.updateProduct`). Además del arreglo F-93, esto deja un bug nuevo: **hoy no se puede vaciar la descripción.** La app manda `description = null` (`[detail]/ui/ProductDetailViewModel.kt:400`, `.ifBlank { null }`). El `Json` no serializa los valores por defecto (`core/network/.../di/NetworkModule.kt:37-42`, sin `encodeDefaults`), así que el campo no viaja y el backend conserva la descripción vieja. Para vaciarla hay que mandar `""` (API_REFERENCE §40). Se arregla dentro de F-93 con un DTO de update propio.
3. **Los errores de producto se clasifican solo por el status HTTP.** `NetworkResult.HttpError` (`core/network/.../NetworkResult.kt:11`) guarda `code` y `message`, nunca el cuerpo ProblemDetail. Por eso F-93 (409 `PRODUCT_VERSION_CONFLICT`), F-83 (409 `PRODUCT_IN_PENDING_ORDERS`) y F-87 (404 `PRODUCT_DELETED`) no se distinguen entre sí. El patrón correcto ya existe en Pedidos: `OrderApiService.parseProblem()` (`core/network/.../service/OrderApiService.kt:92-94`) más `OrderProblemDto`. El **paso común P-1** (abajo) lo generaliza y lo usan F-93, F-83, F-87 y F-85.

##### Abreviaturas de rutas (todas relativas a `android/qhathia-mobile-staff/`)

| Alias | Ruta |
|---|---|
| `[detail]` | `features/product-detail/src/main/java/com/qhathia/mobile/staff/feature/productdetail` |
| `[register]` | `features/product-register/src/main/java/com/qhathia/mobile/staff/feature/productregister` |
| `[inventory]` | `features/product-inventory/src/main/java/com/qhathia/mobile/staff/feature/productinventory` |
| `[stats]` | `features/sales-statistics/src/main/java/com/qhathia/mobile/staff/feature/salesstatistics` |
| `[devices]` | `features/device-linking/src/main/java/com/qhathia/mobile/staff/feature/devicelinking` |
| `[network]` | `core/network/src/main/java/com/qhathia/mobile/staff/core/network` |
| `[data]` | `core/data/src/main/java/com/qhathia/mobile/staff/core/data` |
| `[domain]` | `core/domain/src/main/java/com/qhathia/mobile/staff/core/domain` |
| `[ui]` | `core/ui/src/main/java/com/qhathia/mobile/staff/core/ui` |

##### Paso común P-1 · Leer el ProblemDetail en las llamadas de producto

Lo necesitan F-93, F-83, F-87 y F-85. Hazlo primero.

1. Crea `[network]/dto/ProblemDto.kt`:
   ```kotlin
   @Serializable
   data class ProblemDto(
       val errorCode: String? = null,
       val detail: String? = null,
       val currentVersion: Long? = null,                       // PRODUCT_VERSION_CONFLICT
       val pendingOrders: List<PendingOrderRefDto> = emptyList(), // PRODUCT_IN_PENDING_ORDERS
       val productName: String? = null,
       val reason: String? = null,                             // PRODUCT_NOT_SELLABLE
   )
   @Serializable
   data class PendingOrderRefDto(val id: String, val orderCode: String? = null)
   ```
2. `[network]/NetworkResult.kt:11`: `data class HttpError(val code: Int, val message: String, val problem: ProblemDto? = null)`. El valor por defecto mantiene compilando el resto. Agrega una variante suspend (el cliente tiene `expectSuccess = true`, `[network]/KtorClientFactory.kt:43`):
   ```kotlin
   suspend fun <T> Result<T>.toNetworkResult(json: Json): NetworkResult<T> = fold(
       onSuccess = { NetworkResult.Success(it) },
       onFailure = { t ->
           if (t is ResponseException) NetworkResult.HttpError(
               code = t.response.status.value,
               message = t.response.status.description,
               problem = runCatching { json.decodeFromString<ProblemDto>(t.response.bodyAsText()) }.getOrNull(),
           ) else NetworkResult.NetworkError(t)
       },
   )
   ```
3. En `[network]/service/ProductApiService.kt`, usa `.toNetworkResult(json)` en `getProduct`, `updateProduct`, `createProductWithImages`, `deleteProduct`, `createDiscount`, `deleteDiscount`, `addProductImages` y `deleteProductMedia`. La clase ya recibe `json` en el constructor (`:33-36`).
4. `[domain]/model/UpdateProductError.kt`: agrega `data class Conflict(val currentVersion: Long?)` y `data class HasPendingOrders(val orderCodes: List<String>, val detail: String?)`.
5. `[data]/repository/ProductRepositoryImpl.kt:140-145`: cambia `Int.toUpdateError()` por `NetworkResult.HttpError.toUpdateError()`:
   ```kotlin
   private fun NetworkResult.HttpError.toUpdateError(): UpdateProductError = when {
       code == 404 -> UpdateProductError.NotFound   // RESOURCE_NOT_FOUND y PRODUCT_DELETED
       code == 409 && problem?.errorCode == "PRODUCT_VERSION_CONFLICT" -> UpdateProductError.Conflict(problem.currentVersion)
       code == 409 && problem?.errorCode == "CONCURRENT_MODIFICATION" -> UpdateProductError.Conflict(null)
       code == 409 && problem?.errorCode == "PRODUCT_IN_PENDING_ORDERS" ->
           UpdateProductError.HasPendingOrders(problem.pendingOrders.mapNotNull { it.orderCode }, problem.detail)
       code == 400 || code == 422 -> UpdateProductError.InvalidInput
       code == 503 -> UpdateProductError.Network
       else -> UpdateProductError.Unknown(problem?.detail ?: "HTTP $code")
   }
   ```
   Actualiza los cuatro llamadores (`:82`, `:92`, `:102`, `:129`). Agrega las ramas nuevas a cada `when` exhaustivo sobre `UpdateProductError` (`[detail]/ui/ProductDetailViewModel.kt:506-511` y el de Inventario).

---

##### F-93 · Guarda sin `version` y pisa el cambio de otro vendedor — CRÍTICO · NO CORREGIDO (app; backend OK)

**Qué ve QA hoy (03/11):** caso X:XCO-024 (`2026-11-03-R11.md`, F-93). En el celular, abre el Detalle de "QA R11 Idem renombrado". Por curl, el vendedor B cambia la descripción (08:04:13). En el celular, cambia el nombre y toca Guardar (08:04:30). Resultado: `PUT 200` **sin `version`**, la descripción de B se pierde y no hay aviso. Esperado: el `PUT` lleva `version` y responde 409, y la app muestra "Otro usuario cambió este producto…". Por curl, el backend sí responde 409 `PRODUCT_VERSION_CONFLICT` con una versión vieja. Captura: `F-93_XCO-024-guardar-celular-1103.png`. El 31/10 pasó lo mismo.

**Qué ya está hecho:** nada en la app. Se puede reutilizar: `load()` (`[detail]/ui/ProductDetailViewModel.kt:175-203`) recarga y vuelve a sembrar el formulario, y sirve para "Ver lo nuevo".

**Causa en el código:**
- `[network]/dto/ProductDtos.kt:47-64`: `ProductDetailDto` no tiene `version`, aunque el backend la manda (`ProductResponse.java:53`).
- `[domain]/model/ProductDetail.kt:16-30` y `[detail]/model/ProductDetailUiState.kt:38-70` no guardan la versión cargada.
- `[detail]/ui/ProductDetailViewModel.kt:394-405`:
  ```kotlin
  // The endpoint is a full replace, so the two fields the form does not expose travel back
  // unchanged rather than defaulting.
  val product = ProductUpdate(
      name = current.name.trim(),
      price = price,
      stock = quantity,
  ```
  Manda el formulario entero, sin versión, y **siempre manda `stock`**. Si se confirmó una venta mientras el formulario estaba abierto, el guardado devuelve el stock al valor viejo.
- `[data]/repository/ProductRepositoryImpl.kt:87-95`: `updateProduct` descarta la respuesta del `PUT`, que trae la versión nueva. `:140-145`: un 409 cae en `Unknown` ("Algo salió mal").

**Qué garantiza el backend (API_REFERENCE §40 "Versión optimista"; `ProductService.java:311-314`):**
- `GET /products/{id}` y la respuesta del `PUT` traen `"version": <long>`.
- `PUT /products/{id}` con `"version"` distinta de la actual responde:
  ```json
  HTTP 409
  { "errorCode": "PRODUCT_VERSION_CONFLICT",
    "detail": "Otra persona modificó este producto mientras lo editabas. Revisa los cambios y vuelve a guardar.",
    "currentVersion": 8 }
  ```
- Si dos escrituras chocan en la base: 409 `CONCURRENT_MODIFICATION`.
- **Confirmar un pedido también sube la versión** (el stock cambia).
- Sin `version`, el comportamiento es el de antes (pisa). Es `version` en el body, **no** `If-Match` (línea "Ojo" del plan R11).
- Lo que falta en el body se conserva: `description`/`brand` se vacían con `""`, `tags` con `[]`, `categoryId` con `clearCategory: true`. `stock` ausente también se conserva.

**Cambio:**
1. Haz el paso P-1.
2. DTO de update propio en `[network]/dto/ProductDtos.kt`. Deja `CreateProductRequestDto` solo para el alta:
   ```kotlin
   @OptIn(ExperimentalSerializationApi::class)
   @Serializable
   data class UpdateProductRequestDto(
       val name: String,
       val price: Double,
       @EncodeDefault val description: String = "",       // "" vacía; siempre viaja
       val stock: Int? = null,                             // null = no viaja = el backend conserva
       val categoryId: String? = null,
       @EncodeDefault val tags: List<String> = emptyList(), // [] vacía
       val version: Long? = null,
   )
   ```
   Deja de mandar `allowDiscountInquiry` y `position`: ausentes se conservan, y así no pisas un cambio de otro.
3. `ProductDetailDto`: agrega `val version: Long? = null`. En el dominio, `ProductDetail.version: Long?`, mapeado en `[data]/mapper/ProductMappers.kt` (`ProductDetailDto.toDomain()`, `:38-55`). `ProductUpdate` (`[domain]/model/ProductUpdate.kt`): `stock: Int?`, `description: String` (no nulo), `version: Long?`, sin `allowDiscountInquiry`/`position`. Corrige el KDoc ("full replace" ya no es cierto). `ProductUpdate.toUpdateRequestDto()` (`ProductMappers.kt:62-71`) pasa a construir `UpdateProductRequestDto`.
4. `ProductApiService.updateProduct(id, body: UpdateProductRequestDto)`. `ProductRepository.updateProduct(...)` devuelve `Result<ProductDetail>` (el cuerpo del `PUT`) en vez de `Result<Unit>`. Ajusta `UpdateProductUseCase` y los fakes de test.
5. `ProductDetailUiState`: agrega `loadedVersion: Long? = null`, `loadedQuantity: String = ""`, `showConflictDialog: Boolean = false` y `conflictVersion: Long? = null`. En `seededFrom` (`ProductDetailViewModel.kt:240-262`): `loadedVersion = product.version`, `loadedQuantity = product.stock?.toString().orEmpty()`.
6. `save()` (`:387-450`). Firma: `private fun save(overrideVersion: Long? = null)`.
   ```kotlin
   val stockChanged = current.quantity.trim() != current.loadedQuantity
   val stock: Int? = if (!stockChanged) null else parseQuantity(current.quantity) ?: return   // F-81 añade el caso CONSULT
   val product = ProductUpdate(
       name = current.name.trim(), price = price, stock = stock,
       description = current.description.trim(),            // "" vacía de verdad
       categoryId = categoryId, tags = parseTags(current.tags),
       version = overrideVersion ?: current.loadedVersion,
   )
   ```
   En `onSuccess { updated -> … }`, lo primero: `_uiState.update { it.copy(loadedVersion = updated.version, loadedQuantity = updated.stock?.toString().orEmpty()) }`. Si no, un reintento tras un fallo de fotos o descuento choca **contra tu propio PUT**. En las ramas que se quedan en el formulario (fotos `:411-420`, descuento `:424-441`), vuelve a leer el producto y actualiza `loadedVersion`. `resyncPhotos` ya hace `getProductDetailUseCase`: ponlo ahí y añade la lectura en la rama del descuento. No sé si `POST/DELETE` de fotos y descuentos suben la versión (son colecciones `@OneToMany` de `Product`, y Hibernate puede incrementarla); no lo supongas y vuelve a leer.
   En `onFailure`, si `error is UpdateProductError.Conflict`: `_uiState.update { it.copy(isSaving = false, showConflictDialog = true, conflictVersion = error.currentVersion) }`, sin snackbar.
7. Eventos nuevos en `ProductDetailUiEvent`:
   - `ConflictReload`: cierra el diálogo y llama a `load()`. Descarta lo local y muestra lo del otro vendedor.
   - `ConflictOverwrite`: cierra y llama a `save(overrideVersion = state.conflictVersion)`. Si `conflictVersion` es null (fue `CONCURRENT_MODIFICATION`), primero lee el producto y usa su `version`.
   - `ConflictDismissed`: cierra y sigue editando.
   El diálogo es un `AlertDialog` en `ProductDetailContent.kt`, junto a los otros (`:135-149`), con título "Otro usuario cambió este producto", texto "Mientras lo editabas, otra persona (u otra venta) lo modificó." y botones "Ver lo nuevo", "Guardar lo mío" y "Seguir editando". Strings `detail_conflict_*` en `values` y `values-en`.
8. Tests: en `[detail]` test, el fake de repositorio responde `Conflict(8)` y el test comprueba que se abre el diálogo. "Guardar lo mío" manda `version = 8` y **no** manda `stock` si no se tocó. También un test de que vaciar la descripción manda `""`.

**No hacer:**
- No uses `If-Match` ni `updatedAt`: el contrato real es `version` en el body.
- No mandes `stock` cuando el campo no cambió, ni siquiera en "Guardar lo mío". Esa es la opción que el usuario elige sabiendo que pisa, pero no debe deshacer una venta.
- No conviertas el 409 en "Algo salió mal".

**Verificación:** X:XCO-024.
1. [C] Abre el Detalle de un producto QA. [curl] `PUT` con otra descripción. [C] Cambia el precio a 205 y toca Guardar. Debe aparecer el diálogo, y nginx debe mostrar `PUT 409`. "Ver lo nuevo" muestra la descripción del otro vendedor.
2. [C] Abre el Detalle. Confirma una venta del producto en Solicitudes. Cambia solo el nombre y toca Guardar: sale el diálogo; "Guardar lo mío" debe dejar el stock descontado (`GET /products/{id}`).
3. Vaciar la descripción y guardar debe dejar `"description": null` en `GET`.

---

##### F-86 · Al corregir el precio, el descuento se borra sin aviso (Registro y Detalle) — CRÍTICO · NO CORREGIDO

**Qué ve QA hoy (03/11):** `2026-11-03-pendientes.md`, F-86.
- REG-046: en Registro, precio 189.90, aplica "Descuento de 35% · S/ 123,44", borra el precio y escribe 200. El switch queda apagado y la línea del descuento desaparece, sin aviso.
- **Nuevo:** en el Detalle de un producto **con un descuento ya guardado**, al borrar el precio para escribir otro, el switch se apaga. Capturas: `F-86_REG-046-antes/vacio/200-celular-pend.png` y `F-86_DET-detalle-descuento-apagado-celular-pend.png`. QA no guardó.
- Esperado (plan R11): el switch sigue con "Descuento de 35%" y S/ 130.00. Con precio 1000 y un descuento fijo de 900, bajar a 100 muestra el aviso "Quitamos el descuento…".

**Investigación a fondo: es un bug de la app, no del backend. En el Detalle, guardar BORRA el descuento del servidor.**

1. **El backend guarda el descuento aparte del producto.** Son filas de `product_discounts` con sus propios endpoints: `GET/POST /api/v1/products/{id}/discounts` y `DELETE …/discounts/{discountId}` (`ProductController.java:161-183`, `ProductDiscountService.java`). `ProductRequest` (el body del `PUT`) no tiene ningún campo de descuento, y `ProductService.update` (`:306-349`) no toca los descuentos. Un `PUT` que cambia el precio **conserva** el descuento, y el backend recalcula `discountedPrice` (`DiscountCalculator`). El backend no pierde nada por sí solo.
2. **La app saca el descuento del estado con cada tecla.** `[detail]/ui/ProductDetailViewModel.kt:305-317`:
   ```kotlin
   val newPrice = parsePrice(value)
   ...
   // A discount that no longer fits the price (or has no price to apply to) is dropped.
   discount = state.discount?.takeIf { newPrice != null && it.fits(newPrice) },
   ```
   `parsePrice` (`core/common/.../validation/ProductValidators.kt:12-23`, regex `^\d+(\.\d{1,2})?$`) devuelve `null` para `""` **y también para `"150."`**. Por eso el descuento se pierde de tres formas:
   - al vaciar el campo;
   - al escribir el punto decimal (pasar de "150" a "150.50" pasa por "150.");
   - con un descuento fijo, en cualquier valor intermedio menor que el monto. Para pasar de 1000 a 1500 borrando dígitos, "1" ya no alcanza para 900.
   Una vez fuera, no vuelve.
3. **Al guardar, la app borra el descuento en el servidor.** `ProductDetailUiState.kt:123-124`: `isDiscountChanged = discount != loadedDiscount`. Con `discount = null` y `loadedDiscount = 35 %` da true, y `persistDiscountChange` (`ProductDetailViewModel.kt:488-496`) llama a `removeProductDiscountUseCase(productId)`. Esa llamada hace `GET …/discounts` y `DELETE …/discounts/{id}` por cada descuento (`[domain]/usecase/RemoveProductDiscountUseCase.kt:10-18`). El descuento desaparece de la tablet y del PDF, y **sube el precio real** que ve el cliente. El comerciante solo vio el switch apagado.
4. En Registro es la misma línea: `[register]/ui/ProductRegisterViewModel.kt:123-135`. Ahí no hay nada guardado todavía, pero `submit()` (`:292-294`) crea el producto **sin** el descuento que el comerciante configuró.
5. La UI muestra el switch apagado porque `checked = uiState.discount != null || uiState.discountDraft != null` (`[detail]/ui/ProductDetailContent.kt:261`) y `summary = uiState.discount?.summaryText()` (`:265`). El switch también queda deshabilitado (`isDiscountToggleEnabled` exige un precio válido, `ProductDetailUiState.kt:130-131`). Ese bloqueo está bien; apagarlo no.
6. Interacción con F-82: un producto con precio ≥ 10^7 se siembra como "1.23456785E7" (`formatPrice`, `ProductDetailViewModel.kt:515-516`). El precio queda inválido de entrada, y para corregirlo hay que borrarlo, con lo que el descuento se pierde.
7. Riesgo opuesto, del backend: el backend **no** valida que un descuento `FIXED_AMOUNT` quepa en el precio, ni en `POST …/discounts` (`DiscountRequest.java`) ni cuando un `PUT` baja el precio. `DiscountCalculator.priceAfter` deja el precio en **0** ("un descuento mayor que el precio deja 0"). Si la app deja de quitar el descuento y no lo valida al guardar, el producto quedaría gratis en la tablet. Por eso el chequeo al guardar (paso 3) es obligatorio.

**Qué ya está hecho:** nada. `Discount.fits` (`[domain]/model/Discount.kt:33-36`) es correcto; lo que está mal es cuándo se evalúa.

**Qué garantiza el backend:** lo del punto 1. No hay `PUT` de descuento ni reemplazo atómico; ver F-96.

**Cambio (Detalle y Registro por igual):**
1. `onPriceChanged`, en `ProductDetailViewModel.kt:305-317` y `ProductRegisterViewModel.kt:123-135`: **no toques el descuento**.
   ```kotlin
   private fun onPriceChanged(value: String) {
       val newPrice = parsePrice(value)
       val error = if (value.isNotBlank() && newPrice == null) R.string.detail_error_price else null
       _uiState.update { it.copy(price = value, priceError = error, isDirty = true) }
   }
   ```
2. En los dos UiState, agrega:
   ```kotlin
   /** El descuento del formulario ya no cabe en el precio escrito (solo un monto fijo mayor que el precio). */
   val isDiscountOverPrice: Boolean
       get() = discount != null && parsePrice(price)?.let { !discount.fits(it) } == true
   ```
   Pásalo a `QhathiaDiscountToggleRow` (`[ui]/component/QhathiaDiscountToggleRow.kt:46-55`) con un parámetro nuevo `warning: String? = null`. Bajo el resumen, en `colorScheme.error`, muestra "El descuento es mayor que el precio. Edítalo o quítalo." Con el precio vacío, el switch sigue **encendido y deshabilitado**, y el resumen "Descuento de 35%" se muestra sin precio final (`discountedPrice` ya devuelve null).
3. Chequeo al guardar, en `save()` del Detalle (`:387`, antes de lanzar) y `submit()` del Registro (`:271`):
   ```kotlin
   if (current.isDiscountOverPrice) {
       _uiState.update { it.copy(discount = null, isDirty = true) }
       emit(ProductDetailUiEffect.ShowError(R.string.detail_discount_removed_over_price))
       return   // no se envía nada; el comerciante revisa y vuelve a tocar Guardar
   }
   ```
   Strings nuevos:
   - `detail_discount_removed_over_price`: "Quitamos el descuento porque es mayor que el nuevo precio. Revisa y vuelve a guardar."
   - `register_discount_removed_over_price`: el mismo texto con "agregar".
   - Inglés en los dos.
4. Defensa: `isSaveEnabled` / `isSubmitEnabled` **no** se bloquean por el descuento; el paso 3 lo resuelve con aviso. `persistDiscountChange` no cambia: solo borra el descuento si el comerciante lo apagó a propósito o tras el aviso del paso 3.
5. Tests que **codifican el bug** y hay que invertir:
   - `[detail]` test `ProductDetailViewModelTest.kt:343`, "a price change that no longer fits the discount removes it from the form".
   - `[register]` test `ProductRegisterViewModelTest.kt:431`, "a price change that no longer fits the discount removes it".
   - `[register]` test `ProductRegisterViewModelTest.kt:444`, "clearing the price removes the discount and locks the toggle".
   Tests nuevos:
   - 189.90 + 35 %, luego `PriceChanged("")` y `PriceChanged("200")`: `discount` sigue en 35 % y `discountedPrice == 130.0`.
   - `"150"`, `"150."`, `"150.5"`: el descuento sigue.
   - En el Detalle, con un descuento cargado: borra el precio, escribe otro y guarda. **Ninguna** llamada a `deleteDiscount` (`discountCallsAfterLoad()` vacío).
   - Fijo 900, precio 100, Guardar: aviso, sin `PUT`, y `discount == null`.

**No hacer:**
- No "restaures" el descuento cuando el precio vuelve a ser válido (guardar uno en la sombra): el estado se vuelve opaco y el bug reaparece por otro camino. Mantenlo simplemente en el estado.
- No quites el descuento por debounce ni al perder el foco: el comerciante no lo ve.

**Verificación:**
- S:REG-046: en Registro, precio 189.90, descuento 35 %, borra el precio y escribe 200. Debe seguir "Descuento de 35%" con S/ 130.00. Al Agregar, nginx registra `POST …/discounts`.
- S:EXP-025: precio 1000 con descuento fijo de 900, bajar a 100. Aviso en la fila. Al guardar, "Quitamos el descuento…" y no se envía nada.
- Caso nuevo: en el Detalle de un producto con 20 % guardado, cambia el precio de 150 a 150.50 (pasando por "150.") y luego a 200 (borrando todo), y guarda. nginx muestra solo `PUT`, ningún `DELETE …/discounts`. `GET /products/{id}` trae `discountedPrice 160.00`.

---

##### F-79 · Inventario no se recarga ni al volver ni tirando hacia abajo — ALTO · NO CORREGIDO

**Qué ve QA hoy (03/11):** EXP-004 / EXP-012 (`2026-11-03-pendientes.md`, F-79). Con el Inventario ya cargado (`GET /products` 17:05:45), un curl crea "QA Otro vendedor pend" (17:06:31). Luego [C] Inicio, Inventario, buscar "QA Otro": sale "0 productos" y **no hay ningún `GET /products` nuevo**. Tirar hacia abajo (17:07:21) no hace nada. El Inventario también mostraba "QA R11 Idem celularo", ya borrado en la API. El 31/10 QA vio una recarga al volver; con el código actual no puede venir de cambiar de pestaña (ver Causa). Probablemente fue el `refresh()` de un resultado de Detalle o Registro.

**Qué ya está hecho:** nada. Se puede reutilizar:
- `fetchProducts(quiet = true)` (`[inventory]/ui/InventoryViewModel.kt:310-342`): conserva las filas en pantalla y no muestra error si falla.
- El patrón de Inicio: `LifecycleEventEffect(ON_START)` (`features/home/.../ui/HomeScreen.kt:44`) más `hasStarted` (`HomeViewModel.kt:99-102`).

**Causa en el código:**
- `InventoryViewModel.kt:70-72`: `init { onEvent(InventoryUiEvent.Load) }`. Es la única carga, aparte de `refresh()`, que solo se llama al volver con un resultado (`[inventory]/ui/InventoryScreen.kt:104-135`). Volver con Atrás desde un Detalle sin cambios no refresca.
- `app/src/main/java/com/qhathia/mobile/staff/navigation/MainShell.kt:132-138`: `switchTab` usa `saveState = true` / `restoreState = true`. Al volver a la pestaña, el mismo ViewModel sobrevive con su lista y nadie la vuelve a pedir.
- `[inventory]/ui/InventoryContent.kt:86-112`: la lista va en un `Box` sin `PullToRefreshBox`. La búsqueda filtra en cliente sobre la lista vieja (`InventoryViewModel.kt:93-99`, `applySearch`), y por eso "0 productos".

**Qué garantiza el backend:** `GET /api/v1/products` siempre está al día. Un producto borrado sale de la lista, y un pedido confirmado descuenta el stock.

**Cambio:**
1. `[inventory]/model/InventoryUiEvent.kt`: agrega `data object ScreenStarted` y `data object PullToRefresh`. `InventoryUiState`: agrega `isRefreshing: Boolean = false`.
2. `InventoryViewModel`:
   ```kotlin
   private var hasStarted = false
   ...
   InventoryUiEvent.ScreenStarted -> {
       if (hasStarted && !_uiState.value.isLoading) { loadCategories(); fetchProducts(quiet = true) }
       hasStarted = true                     // el primer ON_START coincide con el Load de init
   }
   InventoryUiEvent.PullToRefresh -> {
       _uiState.update { it.copy(isRefreshing = true) }
       fetchProducts(quiet = true)           // y poner isRefreshing = false en onSuccess y onFailure
   }
   ```
   En `fetchProducts`, baja `isRefreshing` en las dos ramas. Si el pull falla, emite `ShowError(R.string.inventory_error)` aunque la carga sea silenciosa: el usuario la pidió.
3. `InventoryScreen.kt`: `LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onEvent(InventoryUiEvent.ScreenStarted) }`. Dispara al volver a la pestaña (el destino restaurado pasa por STARTED), al volver de un Detalle o Registro y al volver la app del fondo.
4. `InventoryContent.kt:86-112`: envuelve el `when` en `PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = { onEvent(InventoryUiEvent.PullToRefresh) })` (Material3 del BOM `2026.05.00`, `gradle/libs.versions.toml:19`). `EmptyState` y `ErrorState` no son scrollables: dales `Modifier.verticalScroll(rememberScrollState())` dentro del box para que el gesto funcione con la lista vacía (justo el caso de QA). El FAB queda fuera.
5. Stock tras una venta confirmada en Solicitudes: con el paso 3 alcanza, porque volver a la pestaña refresca. No hace falta un bus de invalidación.
6. Test: `ScreenStarted` dos veces da una sola llamada extra a `getProducts` y no limpia `loadedProducts`.

**No hacer:** no quites `saveState/restoreState` de `switchTab`. Perderías el filtro, el scroll y la búsqueda de cada pestaña.

**Verificación:** S:EXP-004, X:EXP-012, X:XJO-010.
- Crea un producto por curl y luego Inicio, Inventario: aparece sin matar la app y nginx registra `GET /products`.
- Arrastra hacia abajo, también con "0 productos": nuevo `GET /products`.
- Confirma un pedido en Solicitudes y vuelve a Inventario: el stock de la fila cambió.
- Borra un producto por curl y vuelve de su Detalle con Atrás: la fila desaparece.

---

##### F-85 · Salir durante "Agregar" o reintentar duplica el producto — ALTO · PARCIAL (backend OK; la app no manda la clave)

**Qué ve QA hoy (03/11):** solo backend (`2026-11-03-R11.md`): dos `POST /products` con la misma `Idempotency-Key` dan 201 y luego 200 con `Idempotent-Replayed: true`, mismo id. El celular no se volvió a probar. El código confirma que el arreglo no está.

**Qué ya está hecho:** nada.

**Causa en el código:**
- `[network]/service/ProductApiService.kt:74-99`: `createProductWithImages` no manda `Idempotency-Key`.
- `[register]/ui/ProductRegisterScreen.kt:55`: `BackHandler(enabled = uiState.isDirty && !uiState.isSubmitting)`. **Se desactiva justo durante el envío**, y el Atrás del sistema saca la pantalla y cancela la corrutina del `viewModelScope`.
- `[register]/ui/ProductRegisterViewModel.kt:101`: `CancelRequested -> onCancelRequested()` no mira `isSubmitting`.
- `[register]/ui/ProductRegisterHeader.kt:35`: `IconButton(onClick = onBack)`, el chevron, siempre habilitado.
- `[register]/ui/ProductRegisterContent.kt:172`: el velo `if (uiState.isSubmitting) Box(...)` no consume toques.

**Qué garantiza el backend (§40 "Alta idempotente"):** `POST /api/v1/products/with-images` acepta `Idempotency-Key: <uuid>` (debe ser UUID; otro formato da 400). Un reintento con la misma clave responde **200** con el producto ya creado y `Idempotent-Replayed: true`; no sube fotos ni consume cupo.

**Cambio:**
1. Clave: en `ProductRegisterViewModel`, `private val idempotencyKey: String = savedStateHandle.get<String>(KEY) ?: UUID.randomUUID().toString().also { savedStateHandle[KEY] = it }`. Inyecta `SavedStateHandle`. Pásala por `NewProduct` (campo nuevo `idempotencyKey`) hasta `createProductWithImages(..., idempotencyKey)`, que hace `header("Idempotency-Key", idempotencyKey)`. Reúsala en cada reintento del mismo formulario. No hace falta regenerarla tras un éxito: la pantalla se cierra.
2. `ProductRegisterScreen.kt:55`: `BackHandler(enabled = uiState.isDirty || uiState.isSubmitting) { if (!uiState.isSubmitting) viewModel.onEvent(ProductRegisterUiEvent.CancelRequested) }`.
3. `ProductRegisterViewModel.kt:101`: `CancelRequested -> if (!_uiState.value.isSubmitting) onCancelRequested()`.
4. `ProductRegisterHeader`: parámetro `backEnabled`, `IconButton(onClick = onBack, enabled = backEnabled)`. Desde `ProductRegisterContent.kt:76` pásale `!uiState.isSubmitting`.
5. Velo (`ProductRegisterContent.kt:172`): `.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}` y `semantics { contentDescription = "Guardando producto…"; liveRegion = LiveRegionMode.Polite }`.
6. `registerProduct` (`[data]/repository/ProductRepositoryImpl.kt:53-75`): un 200 replay se trata igual que un 201 (`body<ProductResponseDto>()` ya lee el `id`).

**Verificación:** S:EXP-022, S:REG-094, S:RED-023, S:REG-085.
- Registro con 10 fotos, "Agregar" y Atrás de inmediato: no sale, el spinner sigue y termina en Inventario con "Producto registrado". nginx `201`, no `499`.
- Chevron durante el envío: no hace nada.
- Modo avión justo tras "Agregar", luego reintento: `GET /products` muestra un solo producto, y nginx ve el segundo `POST` con `200`.

---

##### F-96 · Guardado a medias si se corta la red durante "Guardar" — ALTO · BLOQUEADO en QA (la causa sí está en el código)

**Qué ve QA hoy (03/11):** BLOQUEADO (`2026-11-03-pendientes.md`, "Bloqueados"). "QA Guardado a medias", precio 105 y descuento 20 %, red gprs/gsm, Guardar (17:21:11.4), modo avión (17:21:13.3). La cadena entera (`PUT` 200, `GET …/discounts`, `POST …/discounts` 201) terminó antes del corte. Además, la foto no se pudo quitar porque el producto tenía una sola ("El producto necesita al menos una foto"). Esperado: el formulario muestra "Se guardaron los datos del producto, pero no el descuento…"; Atrás durante el guardado no sale; con red, Guardar solo manda `POST …/discounts`.

**Qué ya está hecho:** la app detecta el fallo parcial, pero solo lo muestra en un snackbar corto:
- `ProductDetailViewModel.kt:411-420`: fotos, string `detail_error_photos_save`.
- `:421-442`: descuento, string `detail_error_discount_save` = "El producto se guardó, pero no se pudo actualizar el descuento. Guarda de nuevo para reintentar".
- El formulario queda sucio y vuelve a leer las fotos y el descuento guardado.

**Causa en el código:**
1. "Guardar" es una cadena de peticiones independientes (`ProductDetailViewModel.kt:407-449`): `PUT /products/{id}`, luego `DELETE …/media/{id}` × n y `POST …/images`, y por último `GET …/discounts`, `DELETE …/discounts/{id}` × n y `POST …/discounts`. El backend no las une: el descuento no es parte del `PUT` (ver F-86, punto 1).
2. El reemplazo del descuento no es atómico: `[domain]/usecase/SaveProductDiscountUseCase.kt:21-30` **borra todos y después crea**. Si el `POST` falla, el producto se queda **sin el descuento que tenía**. El KDoc (`:11-16`) lo admite.
3. Se puede salir a mitad de la cadena:
   - `[detail]/ui/ProductDetailScreen.kt:52`: `BackHandler(enabled = uiState.isDeleting || (uiState.isDirty && !uiState.isSaving))`. Mientras se guarda, el Atrás del sistema no se intercepta: la pantalla sale, el `viewModelScope` se cancela y no hay ningún mensaje.
   - `ProductDetailViewModel.kt:121-122`: `CancelRequested -> if (!_uiState.value.isDeleting) onCancelRequested()` no mira `isSaving`.
   - `ProductDetailHeader.kt:35`: el chevron está siempre habilitado (solo la campana se deshabilita, `ProductDetailContent.kt:81`).
   - `ProductDetailContent.kt:115-124`: el velo no consume toques.
4. El reintento repite el `PUT` aunque ya entró. Con F-93, si no se refresca `loadedVersion`, ese `PUT` chocaría contra sí mismo.
5. Caso borde del descuento: si el `DELETE` del descuento viejo entró, pero el `POST` falló y la relectura (`:430`) también falló por estar sin red, `loadedDiscount` sigue mostrando el viejo aunque el servidor ya no lo tiene. Si el comerciante deja el formulario con ese mismo descuento, `isDiscountChanged` da false y nunca se vuelve a crear.

**Qué garantiza el backend:** endpoints separados (`ProductController.java:161-183`). `POST …/discounts` exige `discountType`, `value`, `startsAt` y `endsAt`, con `endsAt > startsAt`. Se permiten varios descuentos a la vez (no hay índice único en `product_discounts`, `V1__init_biz_schema.sql`), y la tablet aplica el que deje el precio más bajo (`DiscountCalculator`). Sobre un producto eliminado: 404 `PRODUCT_DELETED`. No hay endpoint de reemplazo atómico.

**Cambio:**
1. No dejar salir durante el guardado:
   - `ProductDetailScreen.kt:52`: `BackHandler(enabled = uiState.isDeleting || uiState.isSaving || uiState.isDirty) { if (!uiState.isSaving && !uiState.isDeleting) viewModel.onEvent(ProductDetailUiEvent.CancelRequested) }`.
   - `ProductDetailViewModel.kt:121`: `if (!isDeleting && !isSaving)`.
   - `ProductDetailHeader`: parámetro `backEnabled = !isSaving && !isDeleting`.
   - Velo (`ProductDetailContent.kt:115-124`): consume toques, igual que en F-85, paso 5.
2. Estado parcial visible:
   - `ProductDetailUiState.partialSave: PartialSave? = null`, con `enum class PartialSave { PHOTOS, DISCOUNT }`. Se pone en las ramas `:411-420` y `:424-441` y se limpia al terminar todo.
   - En `EditForm` (`ProductDetailContent.kt:204-217`), al principio de la `Column`, un aviso fijo en `errorContainer`: "Se guardaron los datos del producto, pero no el descuento. Toca Guardar para reintentar." (o "…no las fotos…").
   - El snackbar pasa a `SnackbarDuration.Long`.
3. Reintentar solo lo pendiente: guarda `lastAppliedUpdate: ProductUpdate?` tras un `PUT` exitoso. En `save()`, si el `ProductUpdate` nuevo es igual (sin contar `version`), sáltate el `PUT` y ejecuta solo fotos y/o descuento.
4. Descuento menos frágil, en `SaveProductDiscountUseCase`: primero `createDiscount` y después borra los demás (todos menos el recién creado). Si el borrado falla, devuelve un fallo. Así el formulario queda en `partialSave = DISCOUNT` y el producto **nunca queda sin descuento**: en el peor caso tiene dos, y la tablet aplica el mejor para el cliente. En la rama de fallo, ya no hace falta adivinar con `loadedDiscount`: relee con `getProductDiscountUseCase` y, si la relectura falla, pon `loadedDiscount = null`. Así `isDiscountChanged` queda en true y el reintento vuelve a crear el descuento.
5. Si el comerciante descarta con `partialSave != null`, el diálogo de descarte dice "El descuento no se guardó. Si sales, el producto queda sin ese cambio."
6. Tests en `ProductDetailViewModelTest` con el fake de descuentos que ya existe (`discountRepository`):
   - `create` falla después del `PUT`: queda `partialSave = DISCOUNT`, y el segundo Guardar no llama `updateProduct` (`updateCallCount == 1`) y sí `create`.
   - Atrás durante `isSaving`: no emite navegación.

**Cómo probarlo, ya que el emulador no frena la red:**
- Con los tests de ViewModel del paso 6 (no dependen de la red).
- A mano, con un proxy (mitmproxy o Charles) que bloquee solo `POST */discounts`.
- O con un interceptor solo de `debug` que falle `…/discounts` cuando una bandera de desarrollador esté activa.

**Verificación:** S:RED-011, S:DET-039. Usa un producto con **2 fotos**: con una sola no se puede quitar.
- Quita una foto, pon precio 105 y descuento 20 %, Guardar, y bloquea `/discounts`. El formulario muestra el aviso fijo.
- Atrás durante el guardado no sale.
- Con red, Guardar manda solo `POST …/discounts` (sin `PUT`) y vuelve a Inventario.

---

##### F-83 · Borrar un producto con pedidos pendientes: error genérico, no se puede forzar — ALTO · PARCIAL (backend OK; app no)

**Qué ve QA hoy (03/11):** solo backend (`2026-11-03-R11.md`):
- `DELETE` sobre un producto en un pedido pendiente: 409 `PRODUCT_IN_PENDING_ORDERS`.
- `?force=true`: 204.
- Confirmar ese pedido después: 409 `PRODUCT_NOT_SELLABLE` con `reason INACTIVE`.
- El pedido trae `productActive:false`.
El celular no se volvió a probar.

**Qué ya está hecho:** nada. Con el backend nuevo, **la app ahora no puede borrar ningún producto que esté en un pedido pendiente**: el 409 cae en `Unknown` y aparece "No se pudo eliminar el producto".

**Causa en el código:**
- `[data]/repository/ProductRepositoryImpl.kt:97-105` + `:140-145`: 409 → `Unknown`.
- `[inventory]/ui/InventoryViewModel.kt:135-156` y `[detail]/ui/ProductDetailViewModel.kt:151-168`: ante cualquier otro error, `inventory_delete_error` / `detail_delete_error`.
- `ProductApiService.deleteProduct` (`:188-192`) no tiene `force`.
- `[network]/dto/OrderDtos.kt:63-73`: `OrderItemDto` no tiene `productActive` ni `currentProductName`.
- `[data]/repository/OrderRepositoryImpl.kt:126-135`: confirmar con un 409 `PRODUCT_NOT_SELLABLE` cae en `ConfirmOrderError.Unknown`.

**Qué garantiza el backend (§39):**
```json
HTTP 409
{ "errorCode": "PRODUCT_IN_PENDING_ORDERS",
  "detail": "Este producto está en 1 pedido pendiente (#400776). Confírmalo o recházalo antes, o elimínalo igual.",
  "pendingOrders": [ { "id": "…", "orderCode": "400776" } ] }
```
Con `?force=true` se borra igual. `items[]` del pedido: `productName` (el nombre al crear el pedido), `currentProductName` (solo si se renombró) y `productActive` (`false` si se eliminó). Confirmar con un producto eliminado responde 409 `PRODUCT_NOT_SELLABLE` (`reason: INACTIVE`).

**Cambio:**
1. Paso P-1. `ProductRepository.deleteProduct(id, force: Boolean = false)`; `ProductApiService.deleteProduct(id, force)` con `if (force) parameter("force", true)`.
2. En los dos ViewModels de borrado, ante `HasPendingOrders`, no cierres el flujo: guarda `pendingOrdersWarning = error` en el estado. `QhathiaDeleteProductDialog` (`[ui]/component/QhathiaDeleteProductDialog.kt:24-29`) recibe `pendingOrdersDetail: String?`. Si viene, muestra el `detail` del servidor (ya está en español), confirma con "Eliminar igual" (`DELETE ?force=true`) y cancela con "Cancelar".
3. `OrderItemDto`: agrega `val productActive: Boolean = true` y `val currentProductName: String? = null`, en el dominio `OrderItem` y en `[data]/mapper/OrderMappers.kt`. En `OrderItemRow` (`features/order-requests/.../ui/OrderDetailDialog.kt`), si `!productActive`, muestra el aviso "Producto eliminado del inventario" en lugar del aviso de stock, y deshabilita "Confirmar pedido" (rechazar sigue permitido). Si viene `currentProductName`, muéstralo debajo en `bodySmall` ("Ahora: …").
4. `OrderProblemDto`: agrega `reason`. `toConfirmError`: `409 + PRODUCT_NOT_SELLABLE` → nuevo `ConfirmOrderError.ProductNotSellable(productName, reason)`, con un texto propio.

**Verificación:** S:INV-048, X:EXP-003, X:XCA-032.
- Con un pedido pendiente de un producto QA: Inventario, ⋮, Eliminar. El diálogo muestra el pedido; "Eliminar igual" llama `DELETE ?force=true` (204).
- En Solicitudes, Ver detalle: el ítem dice "Producto eliminado del inventario" y "Confirmar pedido" está deshabilitado.

---

##### F-87 · Editar un producto ya borrado: mensaje correcto, pero se queda en el formulario — MEDIO · PARCIAL (backend OK)

**Qué ve QA hoy:** el 03/11 solo se probó el backend: `PUT` a un producto borrado da 404 `PRODUCT_DELETED` ("Este producto ya fue eliminado.") y un segundo `DELETE` da 404. El 31/10 (`2026-10-31-R11.md`, F-87), en el celular: Guardar sobre un producto borrado por curl da `PUT 404` y "Este producto ya no existe" (bien), pero **se queda en el formulario**. El doble borrado desde la app no se ejecutó.

**Qué ya está hecho:**
- El 404 del `PUT` se traduce bien: `toUpdateError` 404 → `NotFound` → `detail_error_not_found`.
- Borrar con 404 ya vuelve a Inventario con "Este producto ya había sido eliminado": `ProductDetailViewModel.kt:160-161` e `InventoryViewModel.kt:142-150`. El doble borrado debería pasar ya; no se probó en la app.

**Causa en el código:**
- `ProductDetailViewModel.kt:444-448`: en `save()`, un `NotFound` solo muestra el snackbar y deja la pantalla abierta.
- `load()`: el staff recibe `GET` 200 con `active:false` (§39), pero `ProductDetail` no mapea `active` (`ProductDetailDto.active` existe en `ProductDtos.kt:60` y `toDomain()` lo ignora). Un producto borrado se abre como editable.

**Cambio:**
1. `ProductDetail.active: Boolean`, mapeado en `ProductMappers.kt`.
2. En `save().onFailure`: `if (error == UpdateProductError.NotFound) { _uiEffect.emit(ProductDetailUiEffect.NavigateBackDeleted(alreadyDeleted = true)); return }`. Inventario ya refresca y muestra "Este producto ya había sido eliminado" (`InventoryScreen.kt:129-132`). Lo mismo si falla con `NotFound` el paso de fotos o descuento (las subrutas también dan 404 `PRODUCT_DELETED`).
3. En `load()`, si `product.active == false`, muestra un estado "Este producto fue eliminado" con un botón "Volver" en lugar del formulario.

**Verificación:** S:DET-080, S:INV-044, S:DET-073, X:XCO-025. Abre el Detalle, borra el producto por curl, cambia el nombre y toca Guardar. Debe volver a Inventario con "Este producto ya había sido eliminado", y el nombre no cambia en el backend.

---

##### F-81 · Un producto "a consultar" no se puede editar sin ponerle stock — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):** DET-014 (`2026-11-03-pendientes.md`). "QA Consult" (precio 50, stock null, CONSULT): cambias el precio a 55 y "Guardar" sigue `enabled=false`. La API no cambió. Captura: `F-81_DET-014-guardar-deshabilitado-celular-pend.png`.

**Qué ya está hecho:** nada.

**Causa en el código:**
- `ProductDetailViewModel.kt:248`: `quantity = product.stock?.toString().orEmpty()`, que queda vacío para CONSULT. El KDoc de `:234-239` dice "the merchant must fill before saving".
- `ProductDetailUiState.kt:85`: `isValidQuantity(quantity)` en `isSaveEnabled`. Con el campo vacío nunca se habilita.
- `ProductDetailViewModel.kt:391`: `val quantity = parseQuantity(current.quantity) ?: return`.
- `[domain]/model/ProductUpdate.kt`: `stock: Int`.
- `ProductDetailFormFields.kt:198-216`: `DetailQuantityField` con placeholder "0" (`strings.xml:21`).

**Qué garantiza el backend:** `stock` ausente en el `PUT` se conserva (`ProductService.java:337-339`). Con `stock` null, la disponibilidad se deriva como `CONSULT` (`resolveAvailability`, `:362-367`).

**Cambio (sobre lo de F-93, que ya hace `stock: Int?`):**
1. `ProductDetailUiState.isConsult: Boolean`, puesto en `seededFrom` con `product.stock == null`. En `isSaveEnabled`, cambia la condición a `((quantity.isBlank() && isConsult) || isValidQuantity(quantity))`.
2. En `save()`, el stock queda así: `if (!stockChanged || (current.quantity.isBlank() && current.isConsult)) null else parseQuantity(current.quantity) ?: return`.
3. `DetailQuantityField(isConsult)`: placeholder "A consultar" y ayuda "Déjalo vacío para seguir mostrándolo como «Consultar disponibilidad»". Strings nuevos en `values` y `values-en`.

**Verificación:** S:DET-014. "QA Consult", precio 50 → 55: "Guardar" habilitado. El `PUT` sale sin `stock`, y `GET` trae `price 55`, `stock null` y `availability CONSULT`. Un producto con stock 10 sigue exigiendo un número.

---

##### F-82 · Formato de montos: notación científica, coma o punto según la pantalla, sin miles — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):**
- DET-015: "QA Precio grande" (12345678.5) muestra "S/ 1.23456785E7" en el campo Precio y el switch del descuento deshabilitado.
- EXP-011: Inventario muestra "S/ 12345678,50".
- El resumen del descuento dice "S/ 123,44" con coma, y el campo "189.90" con punto.
Capturas: `F-82_*-pend.png`.

**Qué ya está hecho:** nada. `parsePrice` acepta coma y punto (`ProductValidators.kt:18-23`).

**Causa en el código:**
- `ProductDetailViewModel.kt:515-516`: `if (price % 1.0 == 0.0) price.toLong().toString() else price.toString()`. Usa `Double.toString()`, que da "1.23456785E7" desde 10^7. `parsePrice` lo rechaza: el campo arranca inválido y el descuento queda bloqueado (y se pierde al corregirlo, ver F-86 punto 6).
- `%1$.2f` formatea con el locale del idioma (coma en español) y sin miles en:
  - `features/product-inventory/src/main/res/values/strings.xml:29-30`, usado en `ProductRow.kt:269,283,290`;
  - `features/order-requests/src/main/res/values/strings.xml:18`, usado en `OrderCard.kt:109` y `OrderDetailDialog.kt:186,303,315`;
  - `core/ui/src/main/res/values/strings.xml:59-62,85`. `discount_original_price_format` es `%1$.2f` **sin "S/"**. Se usan en `QhathiaDiscountDialog.kt:90,98`, `QhathiaDiscountToggleRow.kt:116,119` y `DiscoveryPreviewParts.kt:58`.
- `[ui]/component/sales/SalesFormatters.kt:10-13`: `Locale.US` sin decimales ("S/ 2,849").

**Cambio:**
1. Un solo formateador: `[ui]/format/MoneyFormat.kt`.
   ```kotlin
   private val PEN: Locale = Locale.forLanguageTag("es-PE")   // CLDR es-PE: punto decimal, coma de miles
   fun Double.toPenPrice(): String = "S/ " + NumberFormat.getNumberInstance(PEN).apply {
       minimumFractionDigits = 2; maximumFractionDigits = 2; roundingMode = RoundingMode.HALF_UP
   }.format(this)                                              // "S/ 12,345,678.50"
   fun BigDecimal.toPenAmount(): String = "S/ " + NumberFormat.getIntegerInstance(PEN).format(setScale(0, RoundingMode.HALF_UP))
   ```
   Es independiente del idioma de la app. Si producto prefiere otro separador, se cambia solo ese locale.
2. Cambia los strings anteriores a `%1$s` (en `values` y `values-en`), añade "S/" en `discount_original_price_format` y pasa `price.toPenPrice()` desde cada llamador. `toSolesLabel` delega en `toPenAmount()`.
3. Campo de precio: reemplaza `formatPrice` por `formatPriceInput(price)` en `ProductValidators.kt`: `BigDecimal.valueOf(price).setScale(2, HALF_UP)`; si los decimales son cero, devuelve el entero ("149"); si no, `toPlainString()` ("189.90", "12345678.50"). Nunca `Double.toString()`. Úsalo en `seededFrom` (`:242,244`) y añade un test con 12345678.5, 189.9 y 149.0. Hay un test existente en `:129` ("a whole price renders without a trailing decimal") que debe seguir pasando.

**Verificación:** S:DET-015, S:EXP-011, S:PER-059.
- 12345678.5: el Detalle muestra "12345678.50", el switch del descuento está habilitado y deja guardar.
- Inventario, el resumen del descuento, el diálogo "Aplicar descuento" (el tachado con "S/"), Solicitudes e Inicio usan el mismo separador en español y en inglés.

---

##### F-97 · "Eliminar producto" con cambios sin guardar muestra el nombre editado — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):** DET-072. Cambias el nombre a "QA Sin foto xyz" y tocas Eliminar producto: el diálogo dice «QA Sin foto xyz» y no menciona los cambios. Captura: `F-97_DET-072-eliminar-celular-pend.png`.

**Qué ya está hecho:** nada. El KDoc de `ProductDetailViewModel.kt:146-150` dice "the confirmation already said so", pero el texto no lo dice.

**Causa en el código:**
- `ProductDetailContent.kt:137`: `productName = uiState.name`, que es el campo editado.
- `core/ui/src/main/res/values/strings.xml:127`: un único texto.
- `QhathiaDeleteProductDialog.kt:24-29`: solo recibe `productName`.

**Cambio:**
1. `ProductDetailUiState.loadedName`, puesto en `seededFrom`; en `:137`, `productName = uiState.loadedName`.
2. `QhathiaDeleteProductDialog(hasUnsavedChanges: Boolean = false)`. Con true usa `delete_product_message_unsaved`: "«%1$s» dejará de verse en el inventario y en la tablet. También se perderán los cambios que no guardaste. Esta acción no se puede deshacer." (y en inglés). Desde el Detalle pásale `uiState.isDirty`; Inventario sigue con false. Combínalo con el texto de pedidos pendientes de F-83.

**Verificación:** S:DET-072. Detalle de "QA Sin foto det", nombre cambiado a "…x", Eliminar producto: el diálogo dice «QA Sin foto det» y menciona los cambios sin guardar.

---

##### F-64 · Doble toque en Volver de Resumen de ventas deja la pantalla en blanco — MEDIO · PARCIAL

**Qué ve QA hoy (03/11):**
- EST-061 (falla): en Resumen de ventas, doble toque en Volver (17:10). La pantalla queda en blanco con Inicio marcado y sigue así a los 6 s.
- EXP-020 (OK): doble Volver en el Detalle lleva a Inventario.
- DIS-090 (OK): doble toque en el engranaje da un solo `GET …/settings` (el 31/10 apilaba dos).
Capturas: `F-64_*-pend.png`.

**Qué ya está hecho:** nada. Que el Detalle y el engranaje pasaran hoy es suerte de tiempos: tampoco tienen protección (ver más abajo).

**Causa en el código:**
- `[stats]/ui/SalesStatisticsViewModel.kt:106-108`: `Back` emite `NavigateBack` por cada toque.
- `SalesStatisticsScreen.kt:25-31` llama a `onBack()` y `MainShell.kt:201`: `salesStatisticsGraph(onBack = { navController.popBackStack() })`. El segundo pop saca `home`, la raíz, y el `NavHost` queda sin destino.
- Lo mismo puede pasar en `MainShell.kt:220` (Detalle, `leave()` en `ProductDetailViewModel.kt:357-364` también emite una vez por toque), `:243` y `:254`.
- `MainShell.kt:198,204,255`: `navigate(...)` sin `launchSingleTop`.

**Cambio:**
1. En `MainShell.kt`:
   ```kotlin
   fun NavController.popIfOn(route: String) { if (currentDestination?.route == route) popBackStack() }
   fun NavController.navigateOnce(route: String) {
       if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route) { launchSingleTop = true }
   }
   ```
   Usa `popIfOn(SALES_STATISTICS_ROUTE / PRODUCT_DETAIL_ROUTE / PRODUCT_REGISTER_ROUTE / DEVICE_LINKING_ROUTE …)` en los `onBack` de `:201,220,243,254`. Compara con la constante de ruta de cada grafo, la que tiene los `{args}`. Usa `navigateOnce` en `:198,203,204,250,255`.
2. En `SalesStatisticsViewModel` (`:106`) y `ProductDetailViewModel.leave()` / `ProductRegisterViewModel.leave()`, agrega `private var isLeaving = false`, y en la rama que emite la salida: `if (isLeaving) return; isLeaving = true`.

**Verificación:** S:EST-061, S:EXP-020, S:DIS-090. Doble toque rápido en Volver de Resumen de ventas y del Detalle: vuelve a Inicio o Inventario, nunca a una pantalla en blanco. Doble toque en el engranaje: una sola "Ajustes de la tablet".

---

##### F-94 · "Catálogo visible" cuenta productos que la tablet no muestra — MEDIO · PARCIAL (backend OK; app no)

**Qué ve QA:** el 03/11 solo se probó el backend: `/catalog/count` da `activeProducts 50`, `visibleProducts 46`, `hidden {outOfStock 2, lowStock 1, consult 1}`. El 31/10, en Dispositivos: "Catálogo visible 50 productos", sin "· 4 no se muestran" (`F-94_XCA-050-dispositivos-celular.png`).

**Qué ya está hecho:** la línea "Actualizado hace…" bajo el catálogo (`[devices]/ui/DeviceCard.kt:203`, commit `352f3f2`, anterior a QA).

**Causa en el código:**
- `[network]/dto/ProfileDtos.kt:57-59`: `CatalogCountDto(val activeProducts: Int = 0)`.
- `[data]/repository/DeviceRepositoryImpl.kt:59-60`: devuelve `activeProducts`.
- `features/device-linking/src/main/res/values/strings.xml:15`: lo rotula "Catálogo visible".
- `[devices]/ui/DeviceLinkingViewModel.kt:81-84`: `ScreenStarted` no vuelve a pedir el conteo; solo se pide en `loadDevices()` (`:132-134`).

**Qué garantiza el backend (§42):** `{ "activeProducts": 58, "visibleProducts": 52, "hidden": { "outOfStock": 3, "lowStock": 1, "consult": 2, "noActiveCategory": 0 } }`. `visibleProducts` usa el mismo predicado que `/catalog/by-category`.

**Cambio:**
1. Agrega `visibleProducts: Int? = null` y `hidden` al DTO. Crea un modelo `CatalogCount(active, visible, hidden)` y haz que `getCatalogCount()` lo devuelva.
2. `CatalogBox`: "Catálogo visible {visible} productos" y, debajo, "{active − visible} no se muestran (agotados, pocas unidades o a consultar)" cuando la diferencia sea mayor que 0 (plurals nuevos).
3. En `ScreenStarted`, vuelve a pedir el conteo sin spinner, y también cuando un `REFRESH_CATALOG` pase a `ACKED`.

**Verificación:** X:XCA-050. Dispositivos muestra "Catálogo visible 46 productos · 4 no se muestran" = `/catalog/count`. Crea un producto, vuelve a Dispositivos y el número sube sin matar la app.

---

##### F-100 · El Ranking no muestra "S/ X de S/ Y" ni marca los eliminados — MEDIO · PARCIAL (backend OK; app no)

**Qué ve QA:** el 03/11 la pantalla no se volvió a probar (`2026-11-03-R10.md`, F-95/F-100). El 31/10 (`2026-10-31-R10.md`): "S/ 2,385 · Generados por tus 3 productos principales", 3 filas, sin "S/ X de S/ Y vendidos", y los eliminados (QA X OK/LOW/UNICO) sin "· Eliminado". El backend ya manda `periodRevenue 2848.50`, `productsSold 11` e `items[].active:false`.

**Qué ya está hecho:** nada.

**Causa en el código:**
- `[network]/service/DashboardApiService.kt:39-45`: `getRanking` no manda `limit`. El backend usa 3 por defecto, con un máximo de 50 (`DashboardController.java:73`).
- `[network]/dto/DashboardDtos.kt:66-79`: `TopSellersDto` solo tiene `totalRevenue` e `items`; `TopSellerItemDto` no tiene `active`.
- `[stats]/ui/RankingTab.kt:61-63`: el total grande es `topSellersRevenue` con el pie `statistics_top_sellers_caption` ("Generados por tus %1$d productos principales", `strings.xml:27-30`).
- `RankingTab.kt:185`: `TopSellerRow` pinta igual un producto activo que uno eliminado.

**Qué garantiza el backend (§42):** `asOf`, `topSellers.periodRevenue` (todo lo de la tienda en el periodo, igual que Inicio), `topSellers.productsSold` (productos distintos vendidos), `topSellers.items[].active`. `totalRevenue` sigue siendo la suma de los listados.

**Cambio:**
1. En los DTO: `TopSellersDto.periodRevenue: Double? = null`, `productsSold: Int? = null`; `TopSellerItemDto.active: Boolean = true`; `SalesRankingDto.asOf: String? = null`. Mapéalos en `[data]/mapper/StoreInsightsMappers.kt`.
2. `getRanking(granularity, categoryId, limit: Int = 5)` con `parameter("limit", limit)`, propagado por el use case.
3. Pie: "Top {n} de {productsSold} · S/ {totalRevenue} de S/ {periodRevenue}" (plurals nuevos; el formato del dinero, de F-82). Si `periodRevenue` llega null, deja el texto actual.
4. `TopSellerRow`: si `!active`, añade " · Eliminado" (`statistics_product_deleted`) en `onSurfaceVariant` y en la descripción accesible. No los filtres.

**Verificación:** X:XJO-013, X:XBU-020. En un día con ventas de 4 o más productos, Ranking Diario muestra 5 filas y "S/ X de S/ Y", con Y igual al total de Inicio. Un producto vendido y luego eliminado aparece con "· Eliminado".

---

##### F-95 · El pie de "Lo que necesita atención" dice "sin ventas" en vez de "con stock sin ventas" y no se refresca — BAJO · PARCIAL (backend OK)

**Qué ve QA:** el 31/10 (`2026-10-31-R10.md`, F-95), "Inmovilizados en 3 de los 46 productos sin ventas" coincidía con `needsAttention.totalCount 46`. **La app muestra el número correcto**; falta "con stock" en el texto, y no se probó el refresco (reponer stock, salir y volver). El 03/11 no se volvió a probar.

**Qué ya está hecho:** el número es el del backend, sin cálculo propio (`RankingTab.kt:85-94`).

**Causa en el código:**
- `features/sales-statistics/src/main/res/values/strings.xml:40-48`: "…productos sin ventas".
- `[stats]/ui/SalesStatisticsViewModel.kt:148-150`: `alreadyShown` impide volver a pedir el ranking con los mismos filtros, y no hay recarga en `ON_START`.

**Qué garantiza el backend (§42):** `needsAttention.totalCount` cuenta los productos activos con `stock > 0` sin ventas confirmadas en el periodo, y cambia en cuanto se repone stock.

**Cambio:**
1. Texto: "Inmovilizados en los %1$d productos con stock sin ventas" y "…en %1$d de los %2$d productos con stock sin ventas" (y en inglés).
2. `SalesStatisticsUiEvent.ScreenStarted` con el patrón `hasStarted`: en el segundo y siguientes, `loadedQueries.clear()` y `loadActiveTab(force = true)`, conservando los datos en pantalla. `LifecycleEventEffect(ON_START)` en `SalesStatisticsScreen`.
3. "Actualizado a las HH:mm" con `asOf` (F-100), bajo el rango.

**Verificación:** X:XBU-021. Ranking Diario: anota "N". Repón por curl un producto agotado, sal y vuelve: el total pasa a N+1 y coincide con `GET /dashboard/ranking`, y el pie dice "con stock sin ventas".

---

##### F-72 · `allowBackup` activo — MEDIO · NO CORREGIDO

**Qué ve QA hoy (03/11):** S:RED-062 (`2026-11-03-R12.md`). `dumpsys package com.qhathia.mobile.staff` muestra `flags=[ HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP KILL_AFTER_RESTORE ]`.

**Causa en el código:** `app/src/main/AndroidManifest.xml:10-12` tiene `android:allowBackup="true"`, `dataExtractionRules="@xml/data_extraction_rules"` y `fullBackupContent="@xml/backup_rules"`. `app/src/main/res/xml/data_extraction_rules.xml` es la plantilla de Android Studio, con todo comentado: se respaldan los DataStore de sesión (`qhathia_session.preferences_pb`) y de preferencias.

**Cambio:**
1. `android:allowBackup="false"` y quita `android:fullBackupContent`.
2. En `data_extraction_rules.xml` (Android 12+, que también rige el device-transfer): `<cloud-backup><exclude domain="root" path="."/></cloud-backup><device-transfer><exclude domain="root" path="."/></device-transfer>`. La app no tiene nada que valga la pena restaurar. Si producto quiere conservar el idioma, incluye solo `datastore/qhathia_app_prefs.preferences_pb`, nunca la sesión.

**Verificación:** S:RED-062. `adb shell dumpsys package com.qhathia.mobile.staff | grep flags` sin `ALLOW_BACKUP`.

---

##### F-20 · Datos personales y tokens en logs; HTTP plano — MEDIO · PARCIAL (celular OK en la build probada; HTTPS BLOQUEADO)

**Qué ve QA hoy (03/11):** `2026-11-03-R12.md`. En el celular, el logcat no tiene `Bearer ey…` ni `accessToken` (**CORREGIDO** en la build probada). HTTPS sigue **BLOQUEADO**: falta dominio y TLS, y las dos apps siguen en `http://87.99.156.164`.

**Qué hace bien la app:** `app/src/main/java/com/qhathia/mobile/staff/di/AppModule.kt:24` tiene `enableLogging = BuildConfig.DEBUG`. La build de QA no es debug, por eso no loguea. `network_security_config.xml` permite texto plano **solo** hacia `87.99.156.164`.

**Lo que la app puede hacer ya (sin dominio):** en **debug**, `[network]/KtorClientFactory.kt:55-62` usa `LogLevel.BODY` con `println`. `sanitizeHeader` oculta las cabeceras, pero **los cuerpos se imprimen enteros**: la contraseña de login y de cambio de clave, `accessToken`/`refreshToken` de las respuestas y el teléfono de `PUT /users/me`. Baja el nivel a `LogLevel.HEADERS` o filtra el cuerpo con un regex que reemplace `"(password|currentPassword|newPassword|accessToken|refreshToken|phone)"\s*:\s*"[^"]*"` por `"$1":"***"`. Usa `Log.d("KtorClient", …)` en vez de `println`.

**Bloqueado (espera a Infra):**
- `app/build.gradle.kts:39`: `BASE_URL "http://87.99.156.164"` para todos los build types.
- El `domain-config` de cleartext en `app/src/main/res/xml/network_security_config.xml:7-11`.
Cuando haya dominio: `BASE_URL "https://<dominio>"`, `<base-config cleartextTrafficPermitted="false"/>` y quita la excepción, que solo puede quedar en `src/debug`.

**Verificación:** D:RED-061 (parte celular), con build **debug**: el logcat no muestra contraseña ni tokens. S:RED-060 queda para cuando exista el dominio.

---

##### F-78 · El registro acepta 11 fotos — BAJO · NO CORREGIDO

**Qué ve QA hoy (03/11):** REG-031. El selector deja marcar 11 y la tira las acepta, sin aviso y con "Agregar" habilitado. Capturas: `F-78_REG-031-*-pend.png`.

**Qué ya está hecho:** el Detalle sí tiene el tope (`ProductDetailViewModel.kt:269-289`, `MAX_PHOTOS = 10` en `ProductDetailUiState.kt:180`, string `detail_photos_limit_reached`). `QhathiaPhotoStrip` ya acepta `canAdd` (`[ui]/component/QhathiaPhotoStrip.kt:99,132`).

**Causa en el código:**
- `[register]/ui/ProductRegisterViewModel.kt:167-179`: `onImagesPicked` une todas las fotos, sin tope.
- `[register]/ui/ImagePickerSection.kt:46-52`: no pasa `canAdd`.
- `[register]/model/ProductRegisterUiState.kt:49-55`: `isSubmitEnabled` solo mira `images.isNotEmpty()`.

**Cambio:**
1. Mueve `MAX_PHOTOS` a `core/common/.../validation/ProductValidators.kt` como `MAX_PRODUCT_PHOTOS = 10` y úsalo en los dos formularios.
2. En `onImagesPicked`, copia la lógica del Detalle: `room = MAX - images.size`, `take(room)`, y si sobran, `ShowError(R.string.register_photos_limit_reached)` ("Solo se pueden agregar 10 fotos; algunas no se agregaron").
3. `ProductRegisterUiState.canAddPhotos`, pasado como `canAdd`, y la ayuda "Máximo 10 fotos". En `isSubmitEnabled`, `images.size <= MAX`.
4. No le pases `maxItems` a `PickMultipleVisualMedia`: exige un valor mayor que 1 y falla cuando queda 1 hueco.

**Verificación:** S:REG-031. Elige 11 fotos: se quedan 10, sale el aviso y la tile "Agregar" se deshabilita. El `POST /products/with-images` lleva 10 partes y responde 201.

---

##### F-66 · Detalles visuales en Dispositivos — BAJO · NO CORREGIDO

QA, 03/11 (`2026-11-03-pendientes.md`, F-66): "Próximament / e", "+ Actualizar catálo…", "N.o de serie" y "1 vinculado (1 activo)". Capturas: `F-66_EXP-009-*-pend.png`.
- Chip partido: `[ui]/component/settings/QhathiaSettingsRows.kt:189-198` (`QhathiaComingSoonTag`: un `Text` sin `maxLines`/`softWrap`). Añade `maxLines = 1, softWrap = false`. En `[devices]/ui/DeviceCard.kt:251-256` (celda "Clientes hoy"), pon el tag debajo del label o quita la celda mientras sea maqueta.
- Botón cortado: `DeviceCard.kt:284-298` tiene dos botones en una `Row` con pesos 1.4/1. "Actualizar catálogo" usa `maxLines = 1, Ellipsis` (`:288`) y "Reiniciar" `maxLines = 1` sin ellipsis (`:298`). Usa `FlowRow`, o apílalos (`Column`, ancho completo) si `LocalDensity.current.fontScale > 1.15f` o si no cabe.
- "N.o de serie": el string es correcto (`features/device-linking/src/main/res/values/strings.xml:88`, `N.º`), pero la fuente Urbanist dibuja `º` como una "o". Cámbialo a "Número de serie" (en inglés, "Serial number").
- Masculino: `features/profile/src/main/res/values/strings.xml:40-47` (`menu_devices_linked` / `menu_devices_connected`). Cámbialo a "%d vinculada(s)" / "%d activa(s)".
- Verificación: S:EXP-009, S:DIS-094 (fuente 1.3) y S:DIS-010 ("3 vinculadas (1 activa)").

##### F-84 · Tema oscuro y fuente 2.0 — BAJO · NO CORREGIDO

QA, 03/11: en modo oscuro, las pestañas inactivas y "Ver más" casi no se leen. Con fuente 2.0, la barra muestra "InventariSolicitud" y las pills se cortan ("Mensu", "Seman", "Por"). Capturas: `F-84_INI-060-oscuro-inicio-celular-pend.png` y `F-84_EXP-016-fuente2-celular-pend.png`.
- Contraste: `[ui]/theme/Theme.kt:35` usa `tertiary = QhathiaPalette.NavInactive` en el esquema **oscuro**, con `#1C4466` (`Color.kt:31`). Crea `DarkNavInactive` (por ejemplo `#8FB3D9`, contraste ≥ 4,5:1 sobre la superficie oscura) y úsalo solo en `DarkColors`.
- Barra inferior: `[ui]/component/QhathiaBottomBar.kt:130-137` usa `maxLines = 1` sin autoajuste. Usa `autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp)` con `softWrap = false`.
- Pills de periodo:
  - en Inicio, `features/home/src/main/java/com/qhathia/mobile/staff/feature/home/ui/PeriodTabs.kt:81-92` (`maxLines = 1`, en `:91`);
  - en Estadísticas, `[ui]/component/QhathiaSegmentedControl.kt:74-85` (`maxLines = 1`, en `:84`; usado en `SalesStatisticsContent.kt:98` y `FiltersPanel.kt:110`).
  Usa el mismo autoajuste. Si `fontScale ≥ 1.5`, abrevia con strings cortos ("Mes", "Sem.", "Hora").
- Otros puntos del plan, no probados hoy:
  - el FAB tapa el ⋮ de la última fila: `[inventory]/ui/InventoryContent.kt:190`, `contentPadding = PaddingValues(Dimens.SpaceMd)`; deja un `bottom` de unos 84 dp;
  - en horizontal se ve una sola fila: la cabecera y los filtros fijos en `:70-85`;
  - las pills "vs semana" se cortan: `[stats]/ui/ActivityTab.kt:210`, `maxLines = 1`;
  - el scroll no vuelve arriba al cambiar de categoría: `InventoryScreen.kt:59`.
  Detalle en `fixes-staff-B.md` F-84 (sus líneas están algo desfasadas).
- Verificación: S:EXP-015 (modo oscuro legible), S:EXP-016 ("Inventario" y "Solicitudes" completos con fuente 2.0), S:EXP-013/014/019/018 y S:SOL-091.

##### F-92 · Accesibilidad — BAJO · NO CORREGIDO (una parte puede ser un artefacto de la herramienta)

QA, 03/11 (`inspect_screen`, sin TalkBack): la fila "Descuento" es un `View` clickable sin Switch ni `checked`; Precio, Stock y Tags son `EditText` sin etiqueta; las miniaturas dicen "Imagen del producto"; los chips de Solicitudes son `TextView` con `checked` y sin rol de pestaña.
- Switch: la app **sí** declara el rol: `[ui]/component/QhathiaDiscountToggleRow.kt:62-67` tiene `.toggleable(value = checked, role = Role.Switch, …)`. Compose expone `Role.Switch` como `roleDescription`, que `uiautomator`/`inspect_screen` no muestra, así que parte del hallazgo puede ser de la herramienta: confírmalo con TalkBack. Añade igualmente `.semantics { stateDescription = if (checked) "Activado" else "Desactivado" }` (strings `discount_toggle_on` / `discount_toggle_off`).
- Campos: `[ui]/component/QhathiaTextField.kt:45` ("placeholder-only (no label)") y `:93` (`BasicTextField`). Añade un parámetro `label: String?` aplicado con `Modifier.semantics { contentDescription = label }` sobre el `BasicTextField`, y pásalo desde `LabeledField` (`[detail]/ui/ProductDetailFormFields.kt:68`) y su equivalente en Registro.
- Miniaturas:
  - en Inventario, `[inventory]/ui/ProductRow.kt:106` usa una descripción genérica, y los estados de carga y error no dicen nada; pon "Foto de %1$s" / "Sin foto" en el contenedor y `null` en la imagen;
  - la tira de fotos, `QhathiaPhotoStrip.kt:259-262` (`PhotoThumbnail`, `contentDescription = null`), debe decir "Foto %1$d de %2$d" + ", principal".
- Chips de Solicitudes: `features/order-requests/src/main/java/com/qhathia/mobile/staff/feature/orderrequests/ui/OrderFilterChips.kt:86-87` usa `.semantics { selected }.clickable(onClick)`. Cámbialo por `.selectable(selected, role = Role.Tab, onClick)` y añade `Modifier.selectableGroup()` a la `Row` (`:39-43`). Toma como modelo `QhathiaSegmentedControl.kt:56-69`, que ya lo hace bien.
- Verificación: S:EXP-010, S:INV-080, S:REG-098, S:DET-090 y S:SOL-090, con TalkBack y con `inspect_screen`.

---

#### Orden sugerido

1. P-1.
2. F-86, que no depende de nada y es lo que más daño hace hoy porque borra descuentos reales.
3. F-93, junto con F-81 y el arreglo de vaciar la descripción, todo en el mismo DTO.
4. F-79.
5. F-85 y F-96, que comparten el bloqueo de Atrás y el velo.
6. F-83 y F-87.
7. F-97, F-82 y F-64.
8. F-94, F-100 y F-95.
9. F-72 y los logs de F-20.
10. F-78.
11. Los visuales: F-66, F-84 y F-92.
