# Figma reference — merchant pairing

File: Qhatia-DS (`uLhEDY8ZMxdNiQZU4uYA1V`). Screenshots saved under `assets/`. Kiosk landscape (1440×900).
Both screens reuse the existing settings chrome: fixed dark side menu (`blue-800`) with "Volver", store title "Tienda", and Wi-Fi / Vincular items — Vincular selected (filled `blue-500` shadow). Main panel is light with a "TIENDA" toolbar label + divider.

## Design tokens (from Figma variables)

| Token | Hex |
|---|---|
| `primitive-blue-50` | `#e8edf2` (card bg / info banner) |
| `primitive-blue-200` | `#95aabf` (divider) |
| `primitive-blue-500` | `#1c4466` (selected item) |
| `primitive-blue-800` | `#0d1f38` (side menu) |
| `primitive-orange-500` | `#ff4500` (TTL progress fill, bullet arrows) |
| `primitive-neutral-0/50/200/600/900` | `#ffffff` / `#f8f8f8` / `#e0e0e0` / `#616161` / `#0a0a0a` |
| `primitive-teal-50` | `#e6f4f8` |
| `color-text-primary` | `#0a2540` |

Type: Urbanist — H1 32/700, H3 20/600, Body XL 20/400, Body LG 16/400, Body MD 14/400, Button XL 16/600.
Map to existing `DiscoveryColors` / `DiscoveryTheme` tokens during apply (don't hardcode hex).

## Código generado — node `1055-14805` (`assets/codigo-generado-1055-14805.png`)

- H1: **"Usa este código para vincular comercio"**
- Subtitle (`neutral-600`): "Ingresa el código en la sección de dispositivos vinculados de tu aplicación de gestión."
- Code card (`blue-50`, rounded ~16dp): **five separate cells** (white rounded squares), one char each — e.g. `2 Y 5 6 7`.
- Below cells: horizontal **TTL progress bar** (`orange-500` fill over `neutral-200` track) depleting as the code ages.
- Clock icon + **"Expira en M:SS"** countdown (e.g. "Expira en 0:52").
- Footer line: "Ingresa este código en tu aplicación para vincular la tablet".
- Two bullet hints (orange `→`): "Sincroniza la información del catálogo de productos." / "Gestiona la configuración de la tablet de forma remota."

## Comercio vinculado — node `1080-1587` (`assets/comercio-vinculado-1080-1587.png`)

- Top-right status chip: green dot + **"Vinculado"**.
- Section **"NOMBRE DEL COMERCIO"** → row card: store name (e.g. "Ferretería Don Pepe"), right-aligned tag "Cuenta de gestión".
- Section **"ESTADO DEL DISPOSITIVO"** → three rows (label left / value right):
  - **Actividad** → "Sincronizado hace 2 minutos" (from `/devices/me` `lastSyncAt`)
  - **Plataforma** → "Qhathia Gestión · Web" (model/platform string)
  - **Código usado** → pairing code (e.g. "2Y567")
- Section **"HISTORIAL DE CATÁLOGO"** → info banner (`blue-50`): "Se sincronizaron N productos." + "Última actualización: …".
- Action row: **"Desvincular comercio"** (icon + label) → opens the unpair confirmation dialog.

## API ↔ field mapping (confirms `pending` fields in specs)

- `Actividad` ← `/devices/me` `lastSyncAt` (relative). `Plataforma` ← `platform`/`deviceName` (Figma shows "Qhathia Gestión · Web" — the human label is **not** in the API → render the API value, label text pending). `Código usado` ← the code held from generation.
- **Pending (no API):** merchant name ("NOMBRE DEL COMERCIO"), catalog history count ("Se sincronizaron N productos") — `/devices/me` has no store name or product count. Show as pending until a story supplies them.
- **Unpair** ("Desvincular comercio"): no backend API → stubbed per design.md.
