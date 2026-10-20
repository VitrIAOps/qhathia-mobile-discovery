# Pendientes del lado Android

Qué cambiar en las dos apps, dónde, y por qué importa. Sale del triage del
2026-10-18 ([`docs/qa/TRIAGE-BUGS.md`](../qa/TRIAGE-BUGS.md)), donde está el detalle forense de cada
hallazgo; **este documento es el plan de trabajo**, ordenado por lo que más
devuelve por el esfuerzo.

Todo lo que acá dice "el backend ya lo manda" **está desplegado en producción**
desde el 2026-10-18 y verificado. No hay que esperar nada del servidor.

- **Discovery** = `qhathia-mobile-discovery-main` (la tablet del kiosko)
- **Staff** = `qhathia-mobile-staff-main` (el celular del comerciante)

Las referencias son `archivo:línea` sobre esos repos.

---

## Antes de empezar: el patrón que explica casi todo

De los 31 bugs del triage, **la mayoría no eran cálculos mal hechos**. Eran datos
que existían, viajaban por la red, y se descartaban en el último paso. Cuatro
veces, en sitios distintos:

| Dato | Dónde se caía |
|---|---|
| El stock de un producto | El backend no lo mandaba **y la app inventaba un número** |
| El precio con descuento | El backend no lo calculaba; cada cliente debía hacerlo y ninguno lo hacía |
| El precio mínimo de una búsqueda | El LLM lo extraía, el DTO lo llevaba, el filtro lo ignoraba |
| `confirmedOrders` del dashboard | Llega en la respuesta, el mapper no lo mapea |

Vale tenerlo presente al leer lo que sigue, porque **tres de los ítems de acá son
borrar código, no escribirlo**. Son los más baratos y los que más arreglan.

Y un segundo patrón, más incómodo: varias pantallas tienen controles que
**parecen accionables y no hacen nada** — un chevron con `onClick = {}`, campos
dibujados como inputs que son solo lectura, un toggle que no produce ningún
efecto observable. Todos fueron reportados como bugs, y con razón.

---

## 1 · El heartbeat de la tablet — una llamada, cinco funciones

**Repo: Discovery** · Triage: [BUG-025] · **Es lo que más devuelve de toda esta lista.**

La app discovery **nunca llama al heartbeat**. Cero referencias en sus 295
archivos Kotlin: su único `POST` a `/devices` es el del pairing, y después solo
hace `GET /devices/me` para leer con qué tienda quedó emparejada.

El heartbeat no es un "sigo vivo". **Es el canal de bajada completo** hacia la
tablet, y sin él hay cinco funciones construidas, probadas y desplegadas que
nunca se ejecutan:

| Lo que no funciona hoy | Por qué |
|---|---|
| "N tablets conectadas" / card en "Desconectada" | `last_ping_at` se queda en el instante del pairing |
| Batería y conexión en gris | La telemetría viaja **dentro** del heartbeat |
| "Actualizar catálogo" y "Reiniciar" | Los comandos **se entregan en** la respuesta del heartbeat |
| Ajustes remotos de la tablet | La tablet se entera por `settingsVersion`, que viene ahí |
| PIN de salida del modo kiosko | Se entrega con esos ajustes — bloqueo implementado en el change `kiosk-lock-mode` (guía: `docs/android/tablet-kiosk-setup.md`) |

Las Fases 21 y 23 están completas del lado del servidor **y sin cliente**.

### Qué implementar

`POST /api/v1/devices/heartbeat`, autenticado con el token DEVICE, cada **1–2
minutos**. Manda la telemetría que la tablet ya tiene a mano y procesa lo que
vuelve.

La app staff considera "conectada" una tablet con `lastPingAt` dentro de los
**últimos 10 minutos** (`Device.kt:31-37`), así que un intervalo de 1–2 min deja
margen para un tick perdido.

De la respuesta hay que procesar dos cosas:

1. **Los comandos pendientes** — ejecutarlos y confirmar con
   `POST /api/v1/devices/me/commands/{commandId}/ack`. Un comando entregado **no
   se re-entrega**: si la tablet muere antes de confirmar, expira a los 10 min.
   Es deliberado — re-entregar un `RESTART_APP` la dejaría en bucle.
2. **`settingsVersion`** — si subió, re-leer `GET /api/v1/devices/me/settings`.

Ver la Fase 21 y 23 del API reference para las formas exactas.

---

## 2 · Borrar código que hoy miente

Tres cambios que son **quitar líneas**. Los más baratos de la lista y los que más
arreglan.

### 2.1 · El stock de Inventario está inventado

**Staff** · Triage: [BUG-019] y [BUG-021] · **Esto ya causó una venta mal tomada.**

```kotlin
// ProductRow.kt:246
internal fun mockedStock(product: Product): Int = (abs(product.id.hashCode()) % 90) + 10
```

Un número entre 10 y 99 derivado del hash del UUID. Es estable entre
recomposiciones, y por eso **convincente**: siempre dice lo mismo. No tiene
ninguna relación con el inventario.

El comerciante vio "19 stock" en la lista, abrió el producto y tenía 1, pidió 2
unidades y el pedido se rechazó. Pasa con **los 41 productos**, no con algunos.

**Qué hacer.** `GET /api/v1/products` ahora devuelve `stock` por fila. Hay que:

1. Agregar `val stock: Int? = null` a `ProductDto` (`ProductDtos.kt`)
2. Mapearlo en `ProductDto.toDomain()` (`ProductMappers.kt`) y agregarlo a
   `Product`
3. Usarlo en `ProductRow.kt:131` y **borrar `mockedStock`**

Es exactamente lo que pedía el comentario del propio helper: *"Replace this
single helper (and the DTO/domain field) when the real field lands"*. El campo
llegó.

`stock` es nullable: `null` significa modo "consultar" o producto legacy, y ahí
no se pinta un número.

### 2.2 · La pestaña "Descuentos" del Discovery no muestra descuentos

**Discovery** · Triage: [BUG-013]

```kotlin
// CatalogConstants.kt:26
fun isDiscountCandidate(product: Product): Boolean = product.allowDiscountInquiry
```

`allowDiscountInquiry` significa *"el cliente puede **preguntar** por un
descuento"*. No es lo mismo que tener uno. La pestaña lista productos sin oferta
y omite los que sí la tienen.

El propio código lo tenía anotado: *"This is an approximation of 'has an active
discount'"*.

**Qué hacer.** El catálogo ahora trae `discountedPrice`:

```kotlin
fun isDiscountCandidate(product: Product): Boolean = product.discountedPrice != null
```

Requiere antes el 3.1.

### 2.3 · El snackbar que no se va

**Staff** · Triage: [BUG-007]

```kotlin
// InventoryScreen.kt — las cuatro ramas, líneas 98, 102, 107 y 112
snackbarHostState.showSnackbar(...)   // SUSPENDE hasta que se cierre
onRegisterResultConsumed()            // limpia la bandera DESPUÉS
```

`showSnackbar` es **suspend**: no retorna hasta que el snackbar se cierra. Al
navegar, el `LaunchedEffect` se cancela, lanza `CancellationException` y
**`onRegisterResultConsumed()` nunca corre**. La bandera vive en el
`SavedStateHandle` del back stack entry, que sobrevive a la navegación, así que
al volver el snackbar reaparece.

Eso explica el reporte de "salió como 3 veces": cada regreso lo mostraba de
nuevo, y paró cuando el usuario se quedó lo suficiente.

**Qué hacer.** Consumir **antes** de esperar al snackbar. La bandera solo sirve
para disparar el efecto una vez; una vez disparado ya cumplió.

```kotlin
viewModel.refresh()
onRegisterResultConsumed()            // primero
snackbarHostState.showSnackbar(...)
```

**Afecta a las cuatro ramas**, no solo a `PRODUCT_UPDATED`.

Ojo con `showRegisteredSnackbar`, que usa el **resultado** de `showSnackbar` para
la acción "Ver producto": ahí conviene capturar `registeredProductId` en una
variable local antes de limpiar. Alternativa equivalente:
`try { ... } finally { onRegisterResultConsumed() }`.

> Una corrección: una versión anterior del triage decía que el ViewModel
> "trataba un evento como estado". **Era incorrecto** — `InventoryUiEffect` es un
> `SharedFlow` one-shot bien implementado. La causa es la de arriba.

---

## 3 · Consumir campos que ya llegan

### 3.1 · El precio con descuento

**Las dos apps** · Triage: [BUG-013]

Hasta ahora el backend devolvía los descuentos **crudos** (`discounts[]` con
tipo, valor y ventana) y cada cliente tenía que hacer la aritmética. Ninguno la
hacía: el mismo producto se veía a precio lleno en la grilla del Discovery, en
Inventario y en el PDF.

Ahora el precio efectivo **lo calcula el servidor** y viaja en dos campos:

| Campo | Significado |
|---|---|
| `discountedPrice` | Precio ya con el descuento aplicado. `null` = sin descuento |
| `discountEndsAt` | Fin del descuento, ISO-8601. Para el "termina en X" |

Están en `GET /products`, `GET /products/{id}` y `GET /catalog/by-category`.

**Que `discountedPrice` sea nullable es la señal**: `null` → pintar solo `price`;
con valor → `price` tachado y `discountedPrice` destacado.

**No recalcular en el cliente.** `discounts[]` sigue ahí para mostrar el detalle
de la promo, pero el número autoritativo es `discountedPrice`. Repetir la
aritmética —qué descuento gana si hay varios, cómo se redondea, qué pasa si el
monto supera el precio— es volver al bug.

Dónde tocar:

- **Discovery**: `CatalogDtos.kt` y `Product.kt` no tienen el campo
- **Staff**: `ProductDto` (fila de Inventario). `ProductDetailDto` ya ignora
  `discounts[]` a conciencia; ahora le basta con leer `discountedPrice`

### 3.2 · "Clientes" se lee como "clientes que compraron"

**Staff** · Triage: [BUG-003]

`uniqueSessions` es **tráfico**: gente que usó la tablet, haya comprado o no. Un
"Clientes: 72" solo en pantalla se lee como 72 ventas, que es otra cosa y mucho
menor.

`GET /dashboard/activity` devuelve ahora **`confirmedOrders`** con su delta —
ese sí es "concretó".

`StoreInsightsMappers.kt:99-102` mapea solo `uniqueSessions` y `productViews`.
Falta mapear y mostrar el tercero al lado, para que el embudo se lea completo.

### 3.3 · "Lo que necesita atención"

**Staff** · Triage: [BUG-004]

La definición exacta: productos **activos**, con **stock > 0**, que **no
registraron ninguna venta confirmada en el período consultado**, ordenados por
`stock × precio` descendente.

Dos cosas del front:

1. **El label es engañoso.** `sales-statistics/.../strings.xml:42` dice
   *"Generados por los %1$d productos no vendidos"*. Ese número es **plata
   parada en inventario**, no ingreso, y está al lado de las ventas con el mismo
   lenguaje visual. Es el cambio de una línea que más mejora esa pantalla.
2. `needsAttention.totalCount` ya viaja (cuántos cumplen la condición en total,
   no solo los 3 de la lista). `DashboardDtos.kt:86` tiene `totalImmobilized`
   pero no `totalCount`. Con él se puede mostrar "3 de N".

La tarjeta tiene sentido sobre todo en vista **mensual**: con la semana en curso
casi vacía, *todo* califica como "no vendido en el período".

### 3.4 · El aviso "pedido por expirar"

**Staff** · Triage: [BUG-008]

El backend ahora manda un push nuevo, **10 minutos antes** de que el pedido
venza:

```json
{ "event": "ORDER_EXPIRING", "orderId": "…", "storeId": "…", "status": "PENDING" }
```

`status` se mantiene en `PENDING` a propósito: el pedido **no cambió de estado**,
solo se le acaba el tiempo. Mandar otra cosa haría que la app lo sacara de
Pendientes, que es lo contrario de lo que el aviso pide.

`OrderNotificationPresenter.contentFor` no conoce el evento, así que hoy cae en
el `else` y la notificación dice *"Pedido actualizado"*. **Llega y suena, pero
con el texto equivocado.**

Falta una rama `EVENT_ORDER_EXPIRING` con sus dos strings. Tres líneas.

---

## 4 · Controles que mienten

### 4.1 · El badge fantasma del ícono

**Staff** · Triage: [BUG-006]

`OrderNotificationPresenter.showOrderNotification` publica una notificación para
**todos** los eventos, `ORDER_TAKEN` incluido (`OrderNotificationPresenter.kt:57`),
y **nunca cancela ninguna**. Secuencia real:

1. `NEW_ORDER` del pedido X → notificación con id `X.hashCode()`
2. Alguien confirma X → el backend manda `ORDER_TAKEN`
3. La app publica **otra** notificación con el **mismo id**, que reemplaza a la
   anterior por *"Pedido atendido"*
4. Esa notificación **se queda en la bandeja**, y el badge la cuenta

`setAutoCancel(true)` solo la quita si el usuario la **toca**; abrir la app desde
el ícono no la borra.

**Dos cambios:**

1. **`ORDER_TAKEN` debe CANCELAR, no publicar.** Significa "otro ya lo atendió":
   no hay nada que hacer.

```kotlin
when (event) {
    EVENT_NEW_ORDER   -> notify(notificationId(orderId), build(...))
    EVENT_ORDER_TAKEN -> NotificationManagerCompat.from(context)
                             .cancel(notificationId(orderId))
}
```

2. **El badge no debe acumularse desde los pushes.** Un push es best-effort: si
   el celular estuvo sin red o en Doze, el `ORDER_TAKEN` nunca llega y el badge
   queda mintiendo. Sumar en uno y restar en otro es un contador que deriva.

**El patrón correcto ya existe en la app.** El badge *interno* de la pestaña
Solicitudes se deriva del servidor: `OrderRepositoryImpl` conserva
`totalElements` de la consulta de pendientes. El badge del **launcher** debería
usar la misma fuente.

```
GET /api/v1/orders?status=PENDING&size=1  →  { "totalElements": 0, ... }
```

### 4.2 · "Quedan pocos disponibles" sobre un producto agotado

**Staff** · Triage: [BUG-017]

```kotlin
// OrderDetailDialog.kt:293-294
if (stock != null && stock <= LOW_STOCK_THRESHOLD) LowStockWarning()
```

`0 <= umbral` es cierto, así que **cero entra por el camino de "quedan pocos"**.
Suavizar un agotado a "quedan pocos" es justo lo que llevó a confirmar un pedido
que no se podía despachar.

El backend manda el stock vivo y crudo; el umbral es decisión del cliente, y está
bien que lo sea.

**La misma app ya lo resuelve bien en otra pantalla**, y dejó escrito por qué:

```kotlin
// RestockProductCard.kt:123 — "an exhausted product reads 'Sin existencias':
// 'Solo quedan 0' would soften a stock-out into a low-stock warning."
```

Falta la rama `stock == 0` con su texto y su color. El criterio ya está decidido.

### 4.3 · Inicio no se refresca al volver

**Staff** · Triage: [BUG-009]

Se repuso el stock de un producto y en Inicio seguía en "Productos para reponer"
con "Sin existencias".

El backend lee el stock vivo y **no hay caché de ningún tipo** en biz. El
problema es que `HomeViewModel` solo recarga con `HomeUiEvent.Refresh` o al
cambiar de período:

```kotlin
combine(selectedPeriod, reloadTrigger) { period, _ -> period }
    .flatMapLatest { period -> flow { emit(load(period)) } }
```

**No hay recarga por ciclo de vida.** Como Inicio es una pestaña del bottom nav,
su entry del back stack sobrevive y el ViewModel se retiene: al volver desde
Inventario se re-muestra el snapshot inicial.

Es el mismo patrón que 2.3: estado que sobrevive a la navegación.

**Qué hacer.** Disparar `HomeUiEvent.Refresh` desde un efecto atado al
`Lifecycle.Event.ON_START` del destino.

### 4.4 · El chevron que no vuelve a ningún lado

**Staff** · Triage: [BUG-011]

```kotlin
// OrderRequestsHeader.kt:75
IconButton(onClick = {}) { Icon(KeyboardArrowLeft, contentDescription = ...) }
```

Decorativo, con el `onClick` vacío. Y es correcto que no navegue: Solicitudes es
una pestaña raíz, no hay pantalla anterior.

**El problema es que se ve como un botón** — y peor, tiene `contentDescription`,
así que TalkBack lo anuncia como una acción de volver que no existe.

Quitarlo de los encabezados de las pestañas raíz. Si se quiere conservar la
simetría visual, dejar el espacio en blanco, pero no un `IconButton`, que dibuja
ripple al tocarlo y promete algo que no pasa.

---

## 5 · Pantallas incompletas

Todas tienen el endpoint listo en el servidor.

| Qué falta | Endpoint | Nota |
|---|---|---|
| **Editar datos personales** | `PUT /api/v1/users/me` | Acepta `fullName` (2–150) y `phone` (9 dígitos, empieza en 9). **El correo NO** — es la credencial de login, cambiarlo sin verificación permite secuestrar la cuenta. Hoy los tres campos son `ReadOnlyField`, un `Box` con borde que *parece* un input |
| **Agregar y quitar fotos** del producto | `POST /products/{id}/images` (multipart, 1..N) · `DELETE /products/{id}/media/{mediaId}` | Tope de 10 por producto. Hoy `ProductDetailDto.media` solo se lee |
| **PIN del kiosko** | `PUT /devices/{deviceId}/settings` con `exitPin` | 4–6 dígitos, **write-only**, BCrypt. `removeExitPin: true` lo quita. ⚠️ No llegará a la tablet hasta que exista el heartbeat (§1) |
| **Los tres puntos del producto** | `DELETE /products/{id}` · `POST /products/{id}/deactivate` | Hoy no abren nada |
| **Compartir cotización por WhatsApp** desde el detalle del pedido | `POST /api/v1/cart/share` | **Ya existe entero y probado**: genera el PDF y lo envía. Hoy lo dispara el kiosko; falta el botón en staff |
| **"Ver producto"** tras registrar | — | **Ya existe en el código**: `registeredProductId` llega y el snackbar ofrece la acción (`InventoryScreen.kt:85`). Si no aparece, revisar que el id se esté pasando. Depende de 2.3 |
| **Vista previa del Discovery** | — | El botón está; falta la pantalla |
| **"X" para limpiar** el buscador | — | UI pura |
| **"Ir al catálogo"** en vez de "Nueva búsqueda" | — | Hoy hace lo mismo que "Volver". Navegación pura |
| **Mostrar el texto transcrito** de la voz | — | La app ya manda texto, no audio: lo tiene a mano |

---

## 6 · Borrar: el toggle de IGV

**Staff** · Triage: [BUG-012] · **Cambio de contrato**

`pricesIncludeIgv` e `igvRate` **ya no existen** en `GET/PUT /api/v1/stores/{id}`.

El flag era una declaración, no un cálculo: su único efecto era elegir entre dos
líneas de texto del PDF. Apagarlo no movía ningún precio, y se reportó como bug —
con razón, porque un control sin efecto observable se lee como roto.

Se retiró en vez de hacerlo calcular: en Perú el precio al consumidor lleva IGV
incluido por ley y así lo cargaban **todas** las tiendas.

**No hay prisa ni riesgo**: una app vieja que siga mandando el campo en el `PUT`
recibe 200 (Spring ignora propiedades desconocidas, y hay un test que lo fija), y
el DTO de Kotlin tiene default, así que su ausencia en la respuesta no rompe la
deserialización. Mientras tanto el toggle manda un campo que se ignora y lee uno
que no llega: se queda fijo en "encendido" sin hacer nada.

**Qué borrar** — 14 archivos de alcance: el toggle en `AppSettingsScreen` y su
ViewModel, `SetPricesIncludeIgvUseCase`, el campo en `StoreSettings`,
`StoreSettingsRepository.updatePricesIncludeIgv`, el parámetro de
`StoreApiService.updateStore`, los mappers, los fakes de testing y los strings
`settings_igv*`.

---

## Resumen de campos nuevos

Todo desplegado y verificado el 2026-10-18.

| Endpoint | Campo nuevo | Tipo | Estado en el front |
|---|---|---|---|
| `GET /products` | `stock` | `Int?` | ⬜ falta — hoy se inventa |
| `GET /products`, `/products/{id}`, `/catalog/by-category` | `discountedPrice` | `Number?` | ⬜ falta |
| idem | `discountEndsAt` | ISO-8601 `?` | ⬜ falta |
| `GET /dashboard/activity` | `confirmedOrders` + delta | `Int` | ⬜ falta |
| `GET /dashboard/ranking` | `needsAttention.totalCount` | `Int` | ⬜ falta |
| `GET /orders/{id}` | `items[].productImageUrl` | `String?` | ✅ ya consumido |
| Push FCM | evento `ORDER_EXPIRING` | — | ⬜ falta el texto |
| `GET/PUT /stores/{id}` | ~~`pricesIncludeIgv`~~, ~~`igvRate`~~ | — | ⬜ borrar |

Las formas exactas están en [`docs/api/API_REFERENCE.md`](../api/API_REFERENCE.md), fases 25 a 28.

---

## Cómo verificar

Lo que se puede comprobar sin backend nuevo, porque ya está en producción:

1. **Stock** — el número de la lista de Inventario tiene que ser el mismo que el
   del detalle. Hoy difiere en los 41 productos.
2. **Descuento** — poner uno a un producto y verlo tachado en Inventario y en la
   grilla del Discovery.
3. **Filtros** — "Iluminación interior" + "Stock bajo" ya se cruzan, y "Stock
   bajo" devuelve algo. El umbral es 5, el mismo que "Productos para reponer".
4. **Búsqueda por voz** — "lámpara más de 50 soles" devuelve las caras. Antes
   devolvía las baratas.
5. **Heartbeat** — una vez implementado, la card de Dispositivos pasa a
   "Conectada" y "Actualizar catálogo" deja de quedarse en gris.

---

## Orden sugerido

| | Qué | Por qué primero |
|---|---|---|
| 1 | **Heartbeat** (§1) | Una llamada, cinco funciones muertas |
| 2 | **Borrar `mockedStock`** (§2.1) | Hoy el comerciante toma decisiones sobre un número falso |
| 3 | **Snackbar** (§2.3) y **badge** (§4.1) | Molestan en cada uso |
| 4 | **`discountedPrice`** (§3.1) + pestaña Descuentos (§2.2) | Una función de venta invisible |
| 5 | **"Quedan pocos" con 0** (§4.2) | Llevó a aceptar un pedido imposible |
| 6 | El resto | |

Lo de §5 y §6 puede ir en paralelo: son pantallas independientes.

---

## Pendiente de validar con el equipo — modo kiosko

Change `kiosk-lock-mode` (Discovery). Las tablets **no se pueden provisionar como Device Owner**
y la app **no es launcher**, así que el bloqueo usa la fijación de pantalla de Android. Se
implementa con estos límites aceptados; falta confirmarlos con producto/operaciones:

- [ ] **El gesto del sistema (Atrás + Recientes) siempre quita la fijación.** Mitigación por
  tablet: bloqueo de pantalla + "Solicitar PIN para dejar de fijar". ¿Es aceptable operativamente?
- [ ] **Wi-Fi:** la aprobación de una red nueva llega como notificación, bloqueada mientras está
  fijada. Hay que salir del kiosko antes de conectar. ¿Aceptable?
- [ ] **Sin arranque automático tras reiniciar la tablet:** alguien debe abrir Discovery a mano.
- [ ] **La salida es temporal:** dura hasta volver a Home o reiniciar la app, y no se guarda.
- [ ] **Diálogo "¿Fijar app?"** (si el modelo lo muestra): un cliente podría descartarlo; la app
  lo vuelve a pedir al volver al frente.

Detalle en `openspec/changes/kiosk-lock-mode/design.md` → "Pending team validation".
