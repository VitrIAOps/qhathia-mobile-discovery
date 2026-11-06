# Playbook — Profile APK CI + auto-minor versioning (Android / AGP 9 / Gradle 9)

Reusable configuration for **building a `profile` APK on every push to `main`** via GitHub Actions,
with the **`versionName` MINOR auto-incrementing on each push** (and no signing secrets required).

This document is written to be applied by a coding agent to another Android project. Read the
**Assumptions** first, then apply the three edits in order, then run the **Verification** steps.
Replace every `<placeholder>` with the target project's value.

---

## What this sets up

1. A **`profile` build type** — non-debuggable but `profileable`, R8 off, signed with the
   auto-generated **debug keystore** (installable by QA, no release secrets).
2. **Automatic `versionName` minor** — `versionName = <major>.<minor>.0-<prerelease>` where `minor`
   is the CI run number (`+1` per push to `main`); local builds fall back to the version catalog.
3. A **GitHub Actions workflow** that assembles the profile APK on push to `main` and uploads it as a
   downloadable artifact.

---

## Assumptions (verify before applying)

| # | Assumption | How to check / adapt |
|---|---|---|
| 1 | Gradle **9.x** wrapper, AGP **9.x**, JDK **21** required | `gradle/wrapper/gradle-wrapper.properties`; the workflow pins Temurin 21 |
| 2 | Version is derived in `app/build.gradle.kts` from a **version catalog** (`gradle/libs.versions.toml`) with `appVersion` (`MAJOR.MINOR.PATCH`) + `appPrerelease` | If versions are hardcoded, adapt the snippet in step 1 to read your source of truth |
| 3 | The APK output filename is (or will be) driven by `versionName` | See the `androidComponents { onVariants … }` block in step 1 |
| 4 | A `debug` signing config exists (AGP provides one by default) | `signingConfigs.getByName("debug")` must resolve |
| 5 | The build needs **no signing/secret files** to compile (or has safe fallbacks, e.g. an optional `secrets.properties`) | Otherwise inject secrets in the workflow before the assemble step |
| 6 | `compileSdk` / platform version the runner must install | Update `platforms;android-<N>` + `build-tools;<N>.0.0` in the workflow |
| 7 | Hosted Linux runners have `ANDROID_HOME` preconfigured | Confirmed for `ubuntu-latest`; the workflow also runs `setup-android` |

**Placeholders used below**

- `<applicationId>` — e.g. `com.qhathia.mobile.discovery`
- `<artifact-base>` — APK base name, e.g. `qhathia-discovery`
- `<compileSdk>` — e.g. `36`
- `<catalog-version-key>` / `<catalog-prerelease-key>` — e.g. `appVersion` / `appPrerelease`

---

## Step 1 — `app/build.gradle.kts`

### 1a. Version derivation (auto-minor)

Replace the block that computes `versionName` / `versionCode` with:

```kotlin
// App version — the catalog holds the base (major + prerelease); CI makes the MINOR auto-increment
// on every push to main by passing -PappMinor=<github.run_number>. Patch stays 0 in this scheme.
//   versionName = "<major>.<minor>.0-<appPrerelease>"
//     · CI run 37   -> 1.37.0-SNAPSHOT
//     · local build -> 1.0.0-SNAPSHOT   (no -PappMinor -> falls back to the catalog's minor)
//   versionCode = major*1_000_000 + minor   (monotonic; safe even for large minors)
val appSemVer = libs.versions.appVersion.get()            // <catalog-version-key>, e.g. "1.0.0"
val appPrerelease = libs.versions.appPrerelease.get()     // <catalog-prerelease-key>, e.g. "SNAPSHOT"
val appMajor = appSemVer.substringBefore(".").toInt()
val appMinor = (project.findProperty("appMinor") as String?)?.toIntOrNull()
    ?: appSemVer.split(".")[1].toInt()
val appVersionName = "$appMajor.$appMinor.0-$appPrerelease"
val appVersionCode = appMajor * 1_000_000 + appMinor
```

Wire them in `defaultConfig`:

```kotlin
defaultConfig {
    applicationId = "<applicationId>"
    // …
    versionCode = appVersionCode
    versionName = appVersionName
}
```

> **`versionCode` note:** this formula (`major*1_000_000 + minor`) intentionally replaces any older
> `major*10000 + minor*100 + patch` scheme, which assumed `minor < 100` and would collide once the
> run-number minor grows. The new codes only ever increase, satisfying Google Play's monotonic rule.
> If the app was already published, confirm the new code is **greater** than the last published one.

### 1b. Profile build type (the "conditions")

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
    }
    // QA / Android Studio profiling build:
    //  · initWith(release)                 -> inherits release config as the baseline
    //  · isProfileable = true              -> allows the Android Studio profiler to attach
    //  · non-debuggable (inherited)        -> clean profiler metrics, close to release behavior
    //  · signingConfig = debug keystore    -> installable by QA WITHOUT release signing secrets
    //  · R8/minify left OFF (from release) -> matches release output without keep-rule risk
    //  · matchingFallbacks += "release"    -> lets library modules resolve when they lack a profile variant
    create("profile") {
        initWith(getByName("release"))
        isProfileable = true
        signingConfig = signingConfigs.getByName("debug")
        matchingFallbacks += "release"
    }
}
```

**Profile build-type conditions — summary**

| Condition | Value | Why |
|---|---|---|
| Base | `initWith(release)` | Start from release, override only what QA needs |
| Debuggable | `false` (inherited) | Realistic performance + clean profiler data |
| Profileable | `true` | Android Studio profiler can attach |
| Signing | `debug` keystore (auto-generated) | Installable by anyone; **no secrets in CI** |
| Minify / R8 | off (inherited from this project's release) | Avoid keep-rule surprises; match release bytecode shape |
| `matchingFallbacks` | `["release"]` | Feature/library modules without a `profile` variant fall back to `release` |
| Assemble task | `assembleProfile` | Output at `app/build/outputs/apk/profile/` |

### 1c. Deterministic APK filename (optional but recommended)

Names every variant's APK `"<artifact-base>-v<versionName>.apk"` (each build type writes to its own
`outputs/apk/<buildType>/` folder, so identical names never collide):

```kotlin
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName
                ?.set("<artifact-base>-v$appVersionName.apk")
        }
    }
}
```

Resulting names:

| Context | APK filename |
|---|---|
| CI, run `N` | `<artifact-base>-v1.N.0-SNAPSHOT.apk` |
| Local build (no `-PappMinor`) | `<artifact-base>-v1.0.0-SNAPSHOT.apk` |

---

## Step 2 — `.github/workflows/build-profile-apk.yml`

```yaml
name: Build Profile APK

# Builds the `profile` build type (non-debuggable but profileable, signed with the auto-generated
# debug keystore) on every push to main and publishes the APK as a downloadable artifact.
# The profile variant needs no signing secrets.

on:
  push:
    branches: [ main ]
  workflow_dispatch:

# Only keep the latest build for a given ref running — cancel superseded pushes.
concurrency:
  group: build-profile-apk-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  build-profile-apk:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      # Gradle 9.x / AGP 9.x require JDK 21 — JDK 17 fails to start the daemon.
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      # Ensure the compileSdk platform + build-tools are present. Adjust <compileSdk> to the project.
      - name: Install SDK packages
        run: |
          yes | sdkmanager --licenses > /dev/null
          sdkmanager "platforms;android-<compileSdk>" "build-tools;<compileSdk>.0.0"

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      # -PappMinor=<run_number> makes the versionName MINOR auto-increment (+1 per push to main).
      - name: Assemble profile APK
        run: ./gradlew assembleProfile -PappMinor=${{ github.run_number }} --stacktrace

      - name: Upload profile APK
        uses: actions/upload-artifact@v4
        with:
          name: <artifact-base>-profile-apk
          path: app/build/outputs/apk/profile/*.apk
          if-no-files-found: error
          retention-days: 30
```

**Adapt per project**

- `platforms;android-<compileSdk>` / `build-tools;<compileSdk>.0.0` → match `compileSdk`. If that exact
  build-tools version doesn't exist, remove it (AGP auto-provisions build-tools) and keep only the platform.
- If the build reads secrets, add a step that writes `secrets.properties`/`local.properties` from
  `${{ secrets.* }}` **before** the assemble step.
- The workflow file must exist **on `main`** for `push`-to-`main` runs to pick it up (merge it first);
  `workflow_dispatch` lets you trigger it manually once it's on `main`.

---

## Step 3 — Verification

Run locally (needs JDK 21 + the compileSdk platform installed):

```bash
# Point at a JDK 21 (e.g. Android Studio's bundled JBR on Windows):
#   JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew :app:assembleProfile -PappMinor=37 --console=plain
```

Confirm the produced name + embedded version:

```bash
ls app/build/outputs/apk/profile/*.apk
# -> <artifact-base>-v1.37.0-SNAPSHOT.apk

python -c "import json;e=json.load(open('app/build/outputs/apk/profile/output-metadata.json'))['elements'][0];print(e['versionName'], e['versionCode'], e['outputFile'])"
# -> 1.37.0-SNAPSHOT 1000037 <artifact-base>-v1.37.0-SNAPSHOT.apk
```

Expected: `minor` equals the `-PappMinor` value, `-SNAPSHOT` preserved, `versionCode = 1_000_000 + minor`.

---

## Gotchas / design notes

- **Minor = `github.run_number`, not commits.** It increments once per workflow run (≈ per push to
  `main`). Re-running a run manually also bumps it. It is **CI-only**; local builds use the catalog minor.
  For a commit-count-based minor instead, compute it in Gradle from `git rev-list --count HEAD` and set
  `actions/checkout` `fetch-depth: 0` (a push with N commits then bumps the minor by N).
- **Major is still manual** in the catalog (`appVersion`). Bump it there to cut a new major line.
- **No version write-back.** This derives the version at build time rather than committing a bump to
  `main` — avoids bot-commit noise, push races, and workflow loops.
- **Prerelease suffix.** Keep `-<appPrerelease>` (e.g. `-SNAPSHOT`) to signal CI/non-release builds;
  drop it (`"$appMajor.$appMinor.0"`) if you want release-looking names.
- **`profile` vs `release`.** `profile` is for QA/profiling (debug-signed). A store-bound `release`
  build still needs a real signing config + secrets — out of scope for this playbook.
