# Preparar una tablet para el modo kiosko

Discovery bloquea la tablet con la **fijación de pantalla** de Android (change `kiosk-lock-mode`).
Las tablets no se provisionan como Device Owner y la app no es launcher, así que hay que hacer
estos pasos **una vez por tablet**, antes de dejarla en el stand.

## 1. Bloqueo de pantalla

*Ajustes → Seguridad → Bloqueo de pantalla* → configurar un **PIN** del dispositivo (distinto del
PIN de salida del kiosko). Sin bloqueo de pantalla, el paso 2 no protege nada.

## 2. Fijación de apps con PIN

*Ajustes → Seguridad → Más ajustes de seguridad → Fijar apps* (el nombre cambia según el modelo):

- Activar **Fijar apps**.
- Activar **Solicitar PIN para dejar de fijar**.

Android siempre deja quitar la fijación con el gesto del sistema (mantener Atrás + Recientes). Con
este ajuste, quien lo intente cae en la pantalla de bloqueo en vez de salir libremente. Además,
Discovery se vuelve a fijar sola cada vez que vuelve al frente.

## 3. Rotación: solo horizontal

Discovery se usa siempre en horizontal (regla de negocio) y la app ya fija la orientación. Igual se
fija también en la tablet, por si algún fabricante o una versión futura de Android ignora la app:

- *Ajustes → Pantalla → Girar pantalla automáticamente* (o el ícono de rotación en los ajustes
  rápidos): **desactivado**, con la tablet en **horizontal**.
- Por adb, al aprovisionar:

  ```bash
  adb shell settings put system accelerometer_rotation 0   # sin rotación automática
  adb shell settings put system user_rotation 1            # horizontal (90°)
  ```

  Si el soporte monta la tablet al revés, usar `user_rotation 3` (270°).

**Nota para desarrollo (Android 16 y target SDK 37).** En Android 16, en pantallas grandes, el
sistema ignora la orientación fija de las apps que apuntan a SDK 36. La app lo evita con la propiedad
`android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` en el manifiesto. Google la elimina
con target SDK 37: al subir `targetSdk` a 37, la regla de solo horizontal dependerá de este paso de
configuración de la tablet.

## 4. Primer arranque

1. Abrir Discovery y emparejar la tablet. Antes del emparejamiento la app **no** se bloquea.
2. Al emparejar, la app se fija. Si Android pregunta "¿Fijar app?", confirmar.

## Uso diario

- **Salir del kiosko:** Configuración (engranaje en el inicio) → PIN de salida, si hay uno →
  **Salir del modo kiosko**. Si no hay PIN configurado desde la app staff, no se pide nada.
- **La salida es temporal:** el bloqueo vuelve al regresar al inicio o al reiniciar la app.
- **Wi-Fi nueva:** salir del kiosko antes de conectar. Android pide aprobar la red con una
  notificación, y las notificaciones están bloqueadas mientras la app está fijada.
- **Tras reiniciar la tablet:** abrir Discovery a mano. No arranca sola.
- **Apagar el kiosko desde staff:** *Ajustes de la tablet → Modo kiosco*. La tablet se desbloquea al
  recibir los ajustes (heartbeat, 1–2 min).

Límites pendientes de validar con el equipo: ver `docs/handoff/android-pendientes.md` →
"Pendiente de validar con el equipo — modo kiosko".
