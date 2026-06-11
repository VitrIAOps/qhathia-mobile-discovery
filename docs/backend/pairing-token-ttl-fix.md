# Requerimiento de corrección — Backend: TTL del token de pairing debe ser 5 min (actualmente 15)

> **Para:** equipo Backend
> **Componente:** `POST /api/v1/devices/pairing-token` y canal `WSS /api/v1/devices/pairing-session/{sessionId}`
> **Tipo:** corrección de comportamiento (TTL incorrecto)
> **Lado del fix:** backend (la app Android respeta el `expiresAt` autoritativo del servidor; no requiere cambios)

## Requerimiento

El token de pairing (código + sesión) y la vida del WebSocket de estado deben expirar a los
**5 minutos**, según el contrato (`docs/android/api-contract.md`). Hoy el backend está emitiendo un
`expiresAt` a **15 minutos**, lo que contradice la especificación y se refleja en la tablet como un
contador de 15:00 en lugar de 05:00.

| | Esperado (contrato) | Actual (observado) |
|---|---|---|
| TTL del token | **5 min** (300 s) | 15 min (900 s) |
| Vida del WSS de estado | = TTL (5 min) | = TTL (15 min) |

## Evidencia (reproducible con curl)

Comparando el `Date` del servidor (su reloj real) contra el `expiresAt` que devuelve, en la misma
llamada:

```bash
curl -s -D - -X POST http://87.99.156.164/api/v1/devices/pairing-token -o /dev/null | grep -i '^date:'
curl -s    -X POST http://87.99.156.164/api/v1/devices/pairing-token | jq -r .expiresAt
```

Salida observada:

```
Date: Thu, 11 Jun 2026 20:55:24 GMT          # reloj del servidor
expiresAt: 2026-06-11T21:10:24.055444261Z    # expiry emitido
```

`21:10:24 − 20:55:24 = 900 s = exactamente 15 minutos`.

> No es desfase de zona horaria ni del cliente: ambos valores provienen del **mismo servidor** en la
> misma ventana de tiempo. El delta es exactamente 900 s, no ~300 s.

## Comportamiento esperado tras el fix

`POST /api/v1/devices/pairing-token` debe devolver `expiresAt = serverNow + 300 s`:

```bash
curl -s -D - -X POST http://87.99.156.164/api/v1/devices/pairing-token -o /dev/null | grep -i '^date:'
curl -s    -X POST http://87.99.156.164/api/v1/devices/pairing-token | jq -r .expiresAt
# expiresAt − Date  ==>  ~300 s (5 min)
```

Y de forma consistente:
- El WebSocket `WSS /api/v1/devices/pairing-session/{sessionId}` debe cerrarse / empujar `EXPIRED`
  alrededor de los **5 minutos** desde la generación (no 15).
- El canje `GET /api/v1/devices/pairing-session/{sessionId}/token` debe responder `410`
  (`PAIRING_TOKEN_EXPIRED`) pasados los 5 minutos.

## Criterios de aceptación (Definition of Done)

1. `expiresAt − serverDate ≈ 300 s` (tolerancia ±5 s) en cada `POST /pairing-token`.
2. El push `EXPIRED` por WebSocket ocurre a ~5 min de la generación.
3. El canje del DEVICE JWT devuelve `410 PAIRING_TOKEN_EXPIRED` después de 5 min.
4. Revisar la fuente del valor: probablemente una constante/propiedad de configuración del TTL
   (p. ej. `pairing.token.ttl`) está en `15m`/`900s` y debe quedar en `5m`/`300s`. Confirmar que no
   exista un segundo lugar (TTL del token JWT vs. TTL de la sesión de pairing) con valores
   distintos — ambos deben alinearse a 5 min.

## Notas para el lado cliente (no requieren acción del backend)

- La app Android ya usa el `expiresAt` **autoritativo del servidor** para el contador; al corregir
  el backend, la tablet mostrará 05:00 automáticamente, sin cambios de código.
- Internamente se actualizará la doc/constante de referencia (`PairingConstants.CODE_TTL_SECONDS`)
  para volver a documentar 5 min, una vez confirmado el fix.
- El `proxy_read_timeout` propuesto en `pairing-websocket-nginx-fix.md` (≥ TTL) queda holgado con
  5 min.

---

### Referencias

- Contrato: `docs/android/api-contract.md` → "WSS /api/v1/devices/pairing-session/{sessionId}"
  ("Vida = TTL del token (5 min)") y "POST /api/v1/devices/pairing-token".
- Fix de infra relacionado: `docs/backend/pairing-websocket-nginx-fix.md`.
- Cliente (referencia): `core/domain/.../model/PairingCode.kt` (`remainingSeconds` usa `expiresAt`).
