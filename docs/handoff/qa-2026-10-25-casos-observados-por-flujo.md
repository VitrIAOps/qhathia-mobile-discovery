# QA Discovery 2026-10-25 · Casos observados por flujo

Fuente: *Qhathia Discovery — Reporte de QA, corrida completa del 25/10/2026 (v2)*. Se incluyen los casos con hallazgo (✖ falló, △ menor, ◐ parcial con hallazgo, ✔ con anotación) y los exploratorios (EXP-). Los IDs agrupados comparten un mismo hallazgo.

> **Regla de negocio, 2026-10-29:** la app se usa solo en horizontal (`qa-wave-1-critical-fixes`, D12). Los casos de diseño en vertical (VIN-052, CAT-027, DET-024, CAR-048) quedan cerrados por la regla y salen de la ola 3. La rotación de VIN-030 desaparece con la misma regla; el resto de VIN-030 (código regenerado al recrear la pantalla) se corrige en la ola 1 (D6).

| Flujo | Caso(s) | Sev. | Qué se observó | Lado |
|---|---|---|---|---|
| **01 · Vinculación** | VIN-031 | CRÍTICO | El WebSocket de vinculación se cierra siempre a los 61 s; si el comercio aprueba después, la tablet nunca se vincula y no avisa. | Front + Backend |
| | VIN-032 | ALTO | Un corte de red de 20 s con el código en pantalla pierde el push APPROVED; no hay reconexión ni polling. | Front |
| | VIN-030 | ALTO | Rotar (o encender la pantalla con rotación pendiente) regenera el código; el viejo sigue aprobable y la tablet no se vincula. Intermitente. | Front + Backend |
| | VIN-022 | MEDIO | Los códigos abandonados (cierre, Volver, rotación) siguen aprobables y crean devices ACTIVE huérfanos. | Front + Backend |
| | VIN-024 · EXP-002 (vinc.) | MEDIO | Toques rápidos en "Generar nuevo código" terminan en el error falso "No se pudo generar…". | Front |
| | VIN-003 | BAJO | Home muestra "TIENDA" en lugar del nombre del comercio. | Front |
| | VIN-011 | BAJO | El backend no acepta el código de vinculación en minúsculas (400 INVALID_PAIRING_TOKEN). | Backend |
| | VIN-052 | BAJO | En vertical el 5.º recuadro del código queda recortado. **Cerrado por la regla de solo horizontal (`qa-wave-1-critical-fixes`, D12); sale de la ola 3.** | Front |
| | VIN-070 (◐) · EXP-005 (vinc.) | BAJO | Button clicable sin texto junto a "Volver" y "Generar nuevo código"; el código se expone como 5 TextView sueltos. | Front |
| | EXP-003 (vinc.) | BAJO | Antes de vincular, la pantalla se apaga a los 60 s mientras se muestra el código. | Front |
| | EXP-004 (vinc.) | BAJO | El permiso de micrófono se pide en el primer arranque, sobre Welcome. | Front |
| | EXP-001 (vinc.) | BAJO | Una ruta inexistente del backend responde 500 en vez de 404 (no se alcanza desde la UI). | Backend |
| **02 · Inicio (Home)** | INI-020 · DET-021 | MEDIO | El doble toque en "Ir al catálogo" apila pantallas duplicadas. | Front |
| | INI-051 · VOZ-003 | MEDIO | "No te escuché…" no vuelve nunca a reposo (más de 2 min en pantalla). | Front |
| | INI-015 · BUS-017 | BAJO | "-50 soles" se interpreta como maxPrice 50 y "150,50" como 150.5. | Backend (LLM) |
| | INI-070 · CAR-080 · DET-080 | BAJO | El engranaje tiene un Button hermano sin etiqueta (ver también Carrito y Detalle). | Front |
| **03 · Búsqueda por texto** | BUS-042 | ALTO | Un producto recién desactivado sigue en resultados y se deja agregar (grupo disponibilidad, ver CAR-023). | Front + Backend (B-14) |
| | BUS-003 · EXP-004 | MEDIO | Los chips de filtro no filtran (siguen DeWalt, Bosch y sin marca); "Bosch + Inalámbrico" cae en unhappy y se pierden los filtros. | Backend (LLM) + Front |
| | BUS-052 · BUS-060 | MEDIO | "Volver" y el Atrás del sistema se comportan distinto (error de red al refinar; Catálogo → Búsqueda → resultado). | Front |
| | BUS-017 | BAJO | Interpretación de números (ver INI-015). | Backend (LLM) |
| | EXP-005 | BAJO | Al volver por el historial, los chips aparecen desmarcados aunque los resultados están filtrados. | Front |
| **04 · Catálogo** | CAT-030 · VOZ-009 | ALTO | El micrófono del Catálogo manda una búsqueda vacía (422) sin escuchar y a los 30 s vuelve a Home. | Front |
| | CAT-024 · CAT-025 | MEDIO | "Volver" y Atrás del sistema con ítems en el carrito no hacen lo mismo. | Front |
| | CAT-027 | BAJO | En vertical se recortan las pestañas. **Cerrado por la regla de solo horizontal (D12); sale de la ola 3.** | Front |
| **05 · Detalle de producto** | DET-032 | CRÍTICO | Compartir con status FAILED (WhatsApp no entregado) se muestra como éxito. | Infra + Front + Backend |
| | DET-031 | ALTO | Compartir un producto con descuento: el PDF usa el precio lleno, no el que vio el cliente. | Backend (B-01) + Front |
| | DET-013 | ALTO | Un producto inactivo abre igual y se deja agregar. | Front + Backend (B-14) |
| | DET-034 | ALTO | Se puede compartir un producto no AVAILABLE (LOW_STOCK); reintenta 3 veces un 400 determinista. | Front + Backend |
| | DET-012 | MEDIO | Un relacionado con descuento se muestra y se cobra a precio lleno. | Backend (B-02) + Front |
| | DET-021 · DET-022 | MEDIO | Doble toque en relacionado y "Volver" rápido ×3 apilan/duplican pantallas. | Front |
| | DET-004 | BAJO | El stepper no vuelve a 1 tras "Agregar"; el segundo toque suma otra vez (3 + 3 = 6) sin aviso. | Front |
| | DET-024 | BAJO | En vertical se recortan los botones del Detalle. **Cerrado por la regla de solo horizontal (D12); sale de la ola 3.** | Front |
| | DET-063 · DET-062 | BAJO | Imagen rota o producto sin foto: área en blanco, sin placeholder. | Front |
| | DET-080 | BAJO | Las miniaturas no tienen etiqueta de accesibilidad. | Front |
| **06 · Carrito, pedido y cotización** | CAR-007 · CAR-032 | CRÍTICO | Ningún WhatsApp se entrega (Meta #133010 *Account not registered*), pero la tablet siempre dice "Revisa tu celular…" / "¡Cotización enviada!". | Infra + Front + Backend |
| | CAR-008 · EXP-012 | ALTO | "Compartir" muestra "¡Pedido generado con éxito!" aunque no existe ningún pedido. | Front |
| | CAR-012 · CAR-026 | ALTO | El total que ve el cliente (S/ 119.20) no coincide con el pedido ni el PDF (S/ 149.00); un cambio de precio no se refleja. | Backend (B-01) + Front |
| | CAR-040 · CAR-041 | ALTO | El siguiente cliente hereda el carrito y el nombre/celular del anterior. | Front |
| | CAR-023 · CAR-025 | ALTO | Productos inactivos o LOW_STOCK se dejan agregar; el pedido da 400 y la app entra en un error genérico en bucle. | Front + Backend (B-14) |
| | CAR-020 · CAR-021 · CAR-024 · CAR-011 | MEDIO | Validaciones que solo hace el servidor (carrito vacío, 21 productos) terminan en error genérico; Compartir reintenta 3 veces un 422. | Front |
| | CAR-033 | MEDIO | El error de Compartir usa el texto del pedido y "Cancelar orden" vacía el carrito. | Front |
| | CAR-042 | MEDIO | El carrito se vacía a los 60 s mientras el cliente escribe en el diálogo. | Front |
| | CAR-043 · CAR-044 | MEDIO | Los diálogos se cierran con la petición en curso y la pantalla de éxito queda colgada. | Front |
| | CAR-014 | BAJO | El celular acepta letras (teclado no numérico, a diferencia del Detalle). | Front |
| | CAR-031 | BAJO | "Compartir" se ve habilitado con el formulario vacío y no hace nada. | Front |
| | CAR-048 | BAJO | En vertical se recorta el precio del carrito. **Cerrado por la regla de solo horizontal (D12); sale de la ola 3.** | Front |
| | CAR-080 | BAJO | Los EditText del carrito y del diálogo Compartir no exponen hint ni etiqueta. | Front |
| **07 · Ajustes** | AJU-011 | ALTO | El PIN de Ajustes no tiene límite de intentos (50 fallidos seguidos). | Front |
| | AJU-031 · AJU-050 | MEDIO | Sin red, Vincular muestra un error de código incorrecto y "Reintentar" genera un código en una tablet ya vinculada. | Front |
| | AJU-030 · AJU-032 | BAJO | Comercio, actividad y "Código usado" muestran "Pendiente" (usa lastSyncAt, que es null). | Front |
| | AJU-022 · AJU-040 · AJU-041 · AJU-043 | BAJO | Wi-Fi: sin botón para volver a pedir permiso, "Buscar redes" no hace nada, "Datos móviles" no permitido, 0 redes la primera vez. | Front |
| **08 · Kiosko y comandos remotos** | KIO-041 · EXP-008 | CRÍTICO | RESTART_APP mata la app y deja la tablet en el escritorio de Android sin fijar; el comando queda ACKED. | Front |
| | EXP-002 · EXP-007 | ALTO | La fijación depende del diálogo del sistema (el cliente puede tocar NO THANKS) y no se reaplica al volver a Home. | Front |
| | EXP-001 · EXP-006 · EXP-010 | MEDIO | Las barras del sistema reaparecen con el kiosko activo y tapan controles (carrito, "Salir del modo kiosko"). | Front |
| **09 · Red y resiliencia** | RED-060 · RED-061 | MEDIO | Nombre y celular del cliente en logcat, celular en el log de biz, token STT en la respuesta; todo en HTTP plano. | Front + Backend + Infra |
| | RED-004 · EXP-011 | BAJO | Tras "Reintentar" en Detalle sin red no se recargan los relacionados. | Front |
| **10 · Búsqueda por voz** | VOZ-009 | ALTO | Mic del Catálogo (ver CAT-030). | Front |
| | VOZ-003 | MEDIO | "No te escuché…" no vuelve a reposo (ver INI-051). | Front |
| | VOZ-004 | MEDIO | La grabación sigue en segundo plano al salir de la app. | Front |
| | VOZ-008 · VOZ-010 | MEDIO | Los micrófonos de Búsqueda (sin permiso) y de Detalle no hacen nada ni avisan. | Front |
