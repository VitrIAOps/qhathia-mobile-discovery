# Instrucción de corrección — Backend/Infra: handshake WebSocket de pairing

> **Para:** equipo Backend / Infra
> **Componente:** `WSS /api/v1/devices/pairing-session/{sessionId}` (ADR-009)
> **Severidad:** bloqueante — el pairing de la tablet no completa
> **Lado del fix:** reverse proxy **nginx** (no requiere cambios en el backend Spring ni en la app Android)

## Asunto

El endpoint `WSS /api/v1/devices/pairing-session/{sessionId}` rechaza **todo** handshake
WebSocket con `400 Bad Request`. El cliente (tablet Android) envía un handshake correcto; el
fallo está en el **reverse proxy nginx**, que no reenvía el *upgrade* de WebSocket al upstream Spring.

## Evidencia (reproducible con curl, sin la app)

Handshake manual contra producción:

```bash
SESSION=$(curl -s -X POST http://87.99.156.164/api/v1/devices/pairing-token | jq -r .sessionId)

curl -i \
  -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  "http://87.99.156.164/api/v1/devices/pairing-session/$SESSION"
```

Respuesta observada:

```
HTTP/1.1 400
Server: nginx/1.29.8
Content-Length: 34

Can "Upgrade" only to "WebSocket".
```

## Diagnóstico — por qué pasa

1. El mensaje `Can "Upgrade" only to "WebSocket".` (exactamente 34 bytes) lo emite el
   **handshake handler de Spring** (`DefaultHandshakeHandler`), no nginx. Es decir: **la petición
   sí llega al upstream**, pero el upstream la ve como un GET normal porque le falta el header `Upgrade`.
2. `Upgrade` y `Connection` son headers **hop-by-hop** (RFC 7230 §6.1). nginx los **descarta por
   defecto** y no los pasa al backend, salvo que se le indique explícitamente.
3. Resultado: con o sin headers correctos en el cliente, el Spring siempre recibe una request sin
   `Upgrade` válido → `400`. Confirmado: un `GET` plano sin headers produce el mismo `400` con el
   mismo cuerpo.

> No es case-sensitivity ni el `sessionId`: se probó `Upgrade: WebSocket` (mayúsculas) y un
> `sessionId` recién generado y válido — mismo `400`.

## Corrección requerida — config nginx

En el `location` que enruta hacia el backend, el path de WebSocket necesita HTTP/1.1 y el reenvío
explícito de los headers de upgrade:

```nginx
location /api/v1/devices/pairing-session/ {
    proxy_pass         http://<upstream-backend>;

    # Obligatorio para WebSocket:
    proxy_http_version 1.1;                       # WS no funciona sobre HTTP/1.0
    proxy_set_header   Upgrade    $http_upgrade;   # reenvía el header Upgrade al backend
    proxy_set_header   Connection $connection_upgrade;  # ver map de abajo

    # Buenas prácticas / passthrough:
    proxy_set_header   Host              $host;
    proxy_set_header   X-Real-IP         $remote_addr;
    proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header   X-Forwarded-Proto $scheme;

    proxy_read_timeout 360s;   # > TTL de 5 min del token; evita corte del socket mientras espera aprobación
    proxy_send_timeout 360s;
}
```

En el bloque `http { }` (fuera del `server`), definir el `map` que activa el upgrade solo cuando
corresponde:

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;     # peticiones normales => Connection: close (no upgrade)
}
```

### Notas importantes

- **No romper el endpoint REST hermano.** `GET /api/v1/devices/pairing-session/{sessionId}/token`
  (canje del DEVICE JWT) viaja por el **mismo prefijo** y **no** debe llevar `Connection: upgrade`.
  El `map $connection_upgrade` de arriba lo resuelve: en una request sin `Upgrade`, manda
  `Connection: close` y el REST funciona normal. (Alternativa: separar el path del token en su
  propia `location` más específica, declarada **antes** que la del WS.)
- Si hay **más de un proxy/capa** (LB, Cloudflare, etc.) delante, cada salto necesita el mismo
  passthrough de `Upgrade`/`Connection`.

## Verificación (Definition of Done del fix)

Tras aplicar y recargar nginx (`nginx -t && nginx -s reload`), el mismo curl de arriba debe responder:

```
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

- Debe ser **`101`**, no `400`.
- Para el `Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==` de ejemplo, el `Sec-WebSocket-Accept`
  esperado es `s3pPLMBiTxaQ9kYGzzhZRbK+xOo=`.
- Verificar también que `GET .../{sessionId}/token` (sin headers de upgrade) sigue respondiendo
  JSON normal y **no** se ve afectado por el cambio.

Con `101`, la tablet recibe el push `WAITING/APPROVED/REJECTED/EXPIRED` y el flujo de pairing queda
funcional.

---

### Referencias

- Contrato del endpoint: `docs/android/api-contract.md` → "WSS /api/v1/devices/pairing-session/{sessionId}"
- Cliente Android (correcto, sin cambios necesarios): `core/network/.../service/PairingSocketClient.kt`
- nginx WebSocket proxying: <https://nginx.org/en/docs/http/websocket.html>
- Hop-by-hop headers: RFC 7230 §6.1
