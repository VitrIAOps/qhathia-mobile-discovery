# TODO — Waveform de voz reactivo al audio real

**Estado:** pendiente (decidido NO implementar por ahora — 2026-05-31)
**Complejidad estimada:** baja-moderada (~6 archivos, ~1 sesión). El grueso es el *fan-out*
de enhebrar un campo nuevo, no algoritmo. Bajo riesgo: no toca el endpointing ni la transcripción.

## Contexto

Hoy `VoiceWaveform` (`core/ui/.../component/VoiceWaveform.kt`) anima 6 barras con un loop
senoidal **decorativo**, no reactivo al volumen real del micrófono — equivalente al
`listening-animation.gif` original (ya eliminado, recreado en Compose).

El dato crudo de amplitud **ya existe y hoy se descarta**: en `AudioRecorderImpl.pollLevel()`
se lee `recorder.maxAmplitude` (pico real 0–32767) pero solo se usa para devolver un binario
`Speech`/`Silence` contra `SILENCE_FLOOR`. Para reactividad basta con **dejar pasar ese número**
en vez de tirarlo. No hace falta FFT, ni hilos nuevos, ni librerías.

## Enfoque recomendado: reactivo "pragmático"

El nivel global modula la **envolvente** (altura/energía) de las barras; el movimiento senoidal
local se mantiene. Cadencia del VM intacta (200 ms) + interpolación en la UI para suavizar.

### Cambios por capa

1. **`core:domain` — `AudioRecorder`**
   Añadir `fun pollAmplitude(): Float` (0..1 normalizado). **No** tocar `pollLevel` para no
   romper el endpointing que ya funciona.

2. **`core:data` — `AudioRecorderImpl`**
   Normalizar `maxAmplitude` (÷ 32767). Idealmente escala log/raíz porque la voz humana es baja
   y en escala lineal apenas se movería. ~3 líneas.

3. **`features:home` — `MicState.Listening`**
   Añadir campo, p. ej. `Listening(remainingSeconds, level: Float)`. **Esto es lo que más se
   ramifica:** toca todos los sitios de construcción:
   - `HomeViewModel.startVoiceSearch()` / `updateListeningRemaining()`
   - el `@Preview` de `HomeContent` que hace `MicState.Listening(45)`
   - `HomeViewModelTest`

4. **`features:home` — `HomeViewModel`**
   En el bucle que ya corre cada 200 ms, leer `pollAmplitude()` y meterlo en el estado.

5. **UI — `HomeContent` → `MicButton` → `VoiceWaveform`**
   Pasar `level` por la cadena y usarlo como envolvente que escala la altura de las barras.

6. **Tests**
   El fake de `AudioRecorder` (añadir `pollAmplitude`) y las aserciones de `MicState.Listening`
   en `HomeViewModelTest`.

## Punto fino: cadencia (200 ms)

El bucle del VM hace `pollLevel` cada **200 ms** a propósito (endpointing testeable).
200 ms = 5 muestras/seg → mapeado directo a las barras **se ve a saltos**.

**Solución barata:** en `VoiceWaveform` envolver el `level` entrante en `animateFloatAsState`
para interpolar entre muestras. Mantiene los 200 ms del VM (cero riesgo en endpointing/batería)
y la animación se ve suave. **Esta es la vía a seguir.**

## Alternativa descartada

Reactivo "fiel" (muestreo ~30–60 fps, barras por banda de frecuencia tipo FFT): complejidad
moderada-alta, hilo de muestreo aparte, más batería. **Sobrediseño para un kiosko.**

## Archivos que tocaría

- `core/domain/.../audio/AudioRecorder.kt`
- `core/data/.../stt/AudioRecorderImpl.kt`
- `features/home/.../model/MicState.kt`
- `features/home/.../ui/HomeViewModel.kt`
- `features/home/.../ui/HomeContent.kt`
- `core/ui/.../component/MicButton.kt`
- `core/ui/.../component/VoiceWaveform.kt`
- `features/home/.../HomeViewModelTest.kt` (+ fake `AudioRecorder`)
