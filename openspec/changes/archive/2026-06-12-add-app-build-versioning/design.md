## Context

This is **build/release configuration**, scoped to the `:app` Gradle module and the root version catalog — no new Gradle module, no `core:*`/`features:*` source changes, no runtime/product behavior change. It draws together two threads:

1. **Versioning (ClickUp 86e1vq6h5, new work):** the app currently hardcodes `versionCode = 1` / `versionName = "1.0"` in `app/build.gradle.kts` and ships APKs named `app-<buildType>.apk`. The task asks for semantic versioning, a `SNAPSHOT` prerelease, and a versioned artifact name (`qhathia-discovery-v1.0.0-SNAPSHOT`).
2. **QA/profiling variant (already implemented):** a `profile` build type was added to produce a non-debuggable, `profileable`, debug-signed APK that still emits Ktor API logs (via `ENABLE_NETWORK_LOGGING` in `:core:network`, decoupled from `BuildConfig.DEBUG`). This change formalizes it so the OpenSpec record matches the code.

**Constraints:** Gradle 9 / AGP 9, version catalog is the canonical place for shared build constants (project standards), KSP2 + Hilt already wired per build type, the Qhathia backend is plain HTTP on a fixed IP (cleartext already enabled), and the real `release` build must remain production-clean.

## Goals / Non-Goals

**Goals:**
- One source of truth for the semantic version (`gradle/libs.versions.toml`), with `versionName = <semver>-SNAPSHOT` and a deterministic `versionCode`.
- Versioned APK output names: `qhathia-discovery-v<semver>-SNAPSHOT[-<buildType>].apk`.
- Capture the `profile` build variant and its API-logging behavior as part of the build capability.

**Non-Goals:**
- No automated version bumping, tagging, or CI release pipeline (manual catalog edit for now).
- No prerelease identifiers other than `SNAPSHOT` yet (the task fixes it to `SNAPSHOT` initially).
- No R8/minification enablement; no production signing config (the real `release` stays unsigned here).
- No change to product behavior or any feature module.

## Decisions

### Version as catalog constants, composed in `:app`
Add to `gradle/libs.versions.toml` `[versions]`: `appVersion = "1.0.0"` and `appPrerelease = "SNAPSHOT"`. In `app/build.gradle.kts`:
- `versionName = "${libs.versions.appVersion.get()}-${libs.versions.appPrerelease.get()}"`.
- `versionCode` derived from `appVersion`: split on `.`, compute `major*10000 + minor*100 + patch` (e.g. `1.0.0` → `10000`). Deterministic and monotonic for the supported range.

**Alternatives considered:** hardcoding in `build.gradle.kts` (rejected — not a single source of truth, the standards put shared constants in the catalog); a `version.properties` file (rejected — the catalog already serves this role for the project).

### Versioned artifact naming — exact `…-SNAPSHOT.apk` (decided)
The APK file name must be exactly `qhathia-discovery-v<versionName>.apk` (e.g. `qhathia-discovery-v1.0.0-SNAPSHOT.apk`), **without** a build-type suffix. `archivesName` is therefore not enough — it appends `-<buildType>`. The apply step SHALL set the per-variant APK output file name directly via the AGP variant output API (`androidComponents { onVariants { variant -> variant.outputs.forEach { it.outputFileName = "qhathia-discovery-v$versionName.apk" } } }`, or the equivalent stable hook for the installed AGP). If that AGP build doesn't expose a stable output-filename setter, fall back to a `Copy`/rename task wired into `assemble`. Each build type writes to its own `outputs/apk/<buildType>/` directory, so the identical file name across variants does not collide.

**Alternatives considered:** `archivesName = "qhathia-discovery-v<version>-SNAPSHOT"` (rejected — yields `…-SNAPSHOT-<buildType>.apk`, and the team wants the exact `…-SNAPSHOT.apk`); legacy `applicationVariants.all { outputFileName = … }` (rejected — constrained/removed under AGP 9's new Variant API).

### `profile` build type (already implemented — documented here)
`app/build.gradle.kts` defines `profile` via `initWith(release)` + `isProfileable = true` + `signingConfig = signingConfigs.getByName("debug")` + `matchingFallbacks += "release"`. Non-debuggable (clean profiler metrics) yet `profileable`; debug-signed (installable by QA); R8 off (no keep-rule risk). API logs are retained by `:core:network`'s `ENABLE_NETWORK_LOGGING` BuildConfig field (`true` only in the network module's `profile` build type), consumed at `NetworkModule.provideHttpClient` as `BuildConfig.DEBUG || BuildConfig.ENABLE_NETWORK_LOGGING`. Variant resolution: `:app` and `:core:network` declare `profile`; other modules fall back to `release` via `matchingFallbacks`.

## Risks / Trade-offs

- **`versionCode` scheme caps minor/patch at 99** → `major*10000 + minor*100 + patch` breaks monotonicity if minor or patch exceeds 99. Mitigation: document the cap; revisit the multiplier if the scheme is ever exceeded (well beyond current needs).
- **Identical APK file name across build types** → all variants emit `qhathia-discovery-v1.0.0-SNAPSHOT.apk`. Mitigation: each build type writes to its own `outputs/apk/<buildType>/` directory, so there is no on-disk collision; whoever shares an APK picks it from the right variant folder.
- **Always-on logging in `profile`** → API request/response bodies appear in logs of QA builds (acceptable: internal kiosk app, cleartext HTTP, dev credentials already non-secret). The real `release` keeps logs off, so production is unaffected.
- **`SNAPSHOT` fixed for all builds** → no differentiation between `release` and QA artifacts by prerelease tag yet. Decided: this stays `SNAPSHOT` for all cases here; per-variant / dynamic prerelease tagging is handled later in the **CI/CD pipeline** (out of scope for this change).

## Open Questions

- None. Naming is fixed to `qhathia-discovery-v<version>-SNAPSHOT.apk`; per-variant prerelease tagging is deferred to the pipeline (out of scope).
