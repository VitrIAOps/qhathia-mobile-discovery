# Android Mobile Development Guide (Jetpack)

This guide provides step-by-step instructions for setting up the Android development environment, executing build pipelines, validating database schemas, and running test suites for the Android Jetpack application. It is the primary runbook for autonomous AI agents operating through CLI interfaces.

---

## 🚀 Setup Instructions

### Prerequisites

Ensure the following toolchain is installed and available on the `PATH` before any build or test command is executed:

- **JDK 21 or higher** — Required by Gradle 9.x and AGP 9.x. Use a distribution such as [Eclipse Temurin 21](https://adoptium.net/). JDK 17 is insufficient for Gradle 9+ — the Gradle daemon will fail to start.
- **Android SDK Command-line Tools** — Full Android Studio installation is NOT required. Only the SDK command-line tools package is needed for headless/agent environments.
- **Gradle Wrapper** (`gradlew`) — Checked into the repository at the project root. Always invoke Gradle through the wrapper; never use a globally installed Gradle binary.
- **Git** — Version control. Required to clone the repository and manage branches.
- **Android Emulator** *(optional, for instrumented tests)* — Requires the `emulator` and `platform-tools` SDK packages. A running AVD (Android Virtual Device) must be available before executing `connectedDebugAndroidTest`.

**Verify prerequisites from the CLI:**

> **Shell note:** Throughout this guide, `./gradlew` is used as the canonical wrapper invocation. On **Windows PowerShell** use `.\gradlew`; on **Windows Command Prompt (cmd)** use `gradlew.bat`. On macOS and Linux, ensure the wrapper is executable (`chmod +x gradlew`) before first use. Git Bash on Windows also accepts `./gradlew`.

```bash
# macOS / Linux / Git Bash — confirm JDK version (must be 21+)
java -version

# Windows PowerShell equivalent
# java -version   ← identical; java is on PATH regardless of OS

# Confirm SDK tools are reachable
sdkmanager --version

# Confirm the Gradle wrapper is executable
./gradlew --version      # macOS / Linux / Git Bash
# .\gradlew --version    # Windows PowerShell
# gradlew.bat --version  # Windows cmd
```

---

### 1. Clone the Repository

```bash
git clone git@github.com:<org>/<android-repo>.git
cd <android-repo>
```

---

### 2. Required Environment Variables

The following variables must be set in the agent's shell session (or declared in the CI environment) before running any `gradlew` command. An absent `ANDROID_HOME` will cause the build to fail immediately.

Select the tab that matches the agent's host operating system.

---

#### 🪟 Windows — PowerShell

```powershell
# Default Android SDK location after installing Android Studio or cmdline-tools
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Add SDK tools to PATH for this session
$env:PATH += ";$env:ANDROID_HOME\platform-tools"
$env:PATH += ";$env:ANDROID_HOME\emulator"
$env:PATH += ";$env:ANDROID_HOME\cmdline-tools\latest\bin"

# Java home — adjust version path to match the installed JDK 21 location
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x.x-hotspot"
# If using SDKMAN or Scoop, verify with: Get-Command java | Select-Object -ExpandProperty Source
```

**Persist for the current user (survives reboots):**

```powershell
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.x.x-hotspot", "User")
```

---

#### 🍎 macOS — zsh / bash

```bash
# Default Android SDK location when installed via Android Studio
export ANDROID_HOME=$HOME/Library/Android/sdk

# Add SDK tools to PATH
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# Java home — requires JDK 21+ installed (e.g., via Homebrew or Temurin)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

**Persist:** Add the above lines to `~/.zshrc` (or `~/.bashrc` for bash).

---

#### 🐧 Linux — bash / zsh

```bash
# Default Android SDK location (adjust if installed elsewhere)
export ANDROID_HOME=$HOME/Android/Sdk

# Add SDK tools to PATH
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# Java home — Temurin 21 via apt (Debian/Ubuntu)
export JAVA_HOME=/usr/lib/jvm/temurin-21-amd64
# Or use: export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
```

**Persist:** Add the above lines to `~/.bashrc` or `~/.zshrc`, then run `source ~/.bashrc`.

---

> **Agent note — CI environments:** In GitHub Actions, GitLab CI, or Bitrise, declare these as pipeline-level environment variables or secrets rather than shell exports. Most hosted Android CI runners already have `ANDROID_HOME` pre-configured; confirm with `echo $ANDROID_HOME` (Linux/macOS runners) or `echo $env:ANDROID_HOME` (Windows runners) at the start of the job.

---

## ⚙️ Environment Configuration

### `local.properties` (SDK Path — Git-ignored)

The Android Gradle Plugin requires a `local.properties` file at the project root to locate the SDK. This file is **never committed** to version control.

```properties
# local.properties — generated by the agent or the developer; never committed
sdk.dir=/home/agent/Android/Sdk
```

**Agent automation:** If `local.properties` is absent, generate it dynamically before the first build:

```bash
# macOS / Linux / Git Bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

```powershell
# Windows PowerShell — backslashes in the path must be escaped as forward slashes for Gradle
$sdkPath = $env:ANDROID_HOME -replace '\\', '/'
"sdk.dir=$sdkPath" | Out-File -FilePath local.properties -Encoding utf8 -NoNewline
```

> **Windows path note:** Gradle's `local.properties` parser requires forward slashes (`/`) in the `sdk.dir` value even on Windows. A value like `sdk.dir=C:\Users\agent\AppData\Local\Android\Sdk` **will fail**; it must be `sdk.dir=C:/Users/agent/AppData/Local/Android/Sdk`. The PowerShell command above handles this conversion automatically.

---

### `gradle.properties` (Build Tunables — Committed)

Project-wide Gradle properties live in `gradle.properties` at the project root. Non-secret build tunables belong here.

```properties
# gradle.properties

# JVM memory for the Gradle daemon — increase for large projects
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC

# Enable Kotlin incremental compilation
kotlin.incremental=true

# Enable Gradle configuration cache (speeds up repeated builds)
org.gradle.configuration-cache=true

# Enable build cache
org.gradle.caching=true

# KSP schema export — Room will write JSON snapshots to this path
room.schemaLocation=./schemas

# --- AGP 9.x / Hilt 2.57.x compatibility workaround ---
# The Hilt 2.57.x Gradle plugin queries the legacy Android BaseExtension, which
# AGP 9 no longer creates under its new DSL — the build fails with
# "Android BaseExtension not found". Opt out of the new DSL so the legacy
# extension exists again. With the new DSL off, built-in Kotlin's source-set
# guard then rejects the KSP-generated source dirs, so it must be relaxed too.
#
# Do NOT set android.builtInKotlin=false — this project relies on AGP's built-in
# Kotlin and never applies org.jetbrains.kotlin.android. Do NOT add
# android.enableLegacyVariantApi either: it was REMOVED in AGP 9.0 and now errors
# out ("has no effect, use android.newDsl instead"); android.newDsl already covers it.
# Remove both flags once on Hilt 2.59.2+ (first release with native AGP 9 support).
android.newDsl=false
android.disallowKotlinSourceSets=false
```

> **AGP 9.x note:** This project uses AGP 9's **built-in Kotlin** — never apply `org.jetbrains.kotlin.android` in a module's `plugins {}` block (it is a hard build error). The two flags above are a **temporary workaround** for Hilt 2.57.x and will be removed in AGP 10.0. They do *not* re-enable the old kotlin-android plugin; they only restore the legacy `BaseExtension` that Hilt's Gradle plugin still looks for. Remove them when upgrading to a Hilt release with native AGP 9 support (2.59.2+).

> **Hilt 2.57.x + Kotlin 2.3 — metadata version mismatch:** Hilt's annotation processor reads class metadata via `kotlin-metadata-jvm`, and the copy bundled with 2.57.x only understands metadata up to version 2.2.0. Kotlin 2.3.x emits metadata 2.3.0, so the build fails with *"Provided Metadata instance has version 2.3.0, while maximum supported version is 2.2.0"*. Since Dagger/Hilt 2.57+ no longer shades that library, override it on the KSP processor classpath of **every** Hilt module: add `ksp(libs.kotlin.metadata.jvm)` (alias pinned to `version.ref = "kotlin"` so it always tracks the Kotlin version).

---

### `secrets.properties` (API Keys — Git-ignored)

Sensitive runtime values (API base URLs, third-party service keys) must **never** be hard-coded in source files or committed to the repository. Store them in a git-ignored `secrets.properties` file and inject them into the build via the `BuildConfig` mechanism.

**`secrets.properties` (create at project root, add to `.gitignore`):**

```properties
# secrets.properties — Git-ignored. Never commit this file.

# Backend API base URL for the development/debug environment
API_BASE_URL="https://dev-api.yourapp.com/v1/"

# Deepgram API key for the local development environment
DEEPGRAM_API_KEY="your_deepgram_key_here"
```

**`build.gradle.kts` (app module) — load and inject into `BuildConfig`:**

```kotlin
import java.util.Properties

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) load(secretsFile.inputStream())
}

android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Inject secrets as BuildConfig constants available at runtime
        buildConfigField(
            "String",
            "API_BASE_URL",
            secrets.getProperty("API_BASE_URL", "\"https://dev-api.yourapp.com/v1/\"")
        )
        buildConfigField(
            "String",
            "DEEPGRAM_API_KEY",
            secrets.getProperty("DEEPGRAM_API_KEY", "\"\"")
        )
    }
}
```

**Runtime access in Kotlin source:**

```kotlin
// Access in any Kotlin file within the app module
val apiBaseUrl = BuildConfig.API_BASE_URL
val deepgramApiKey = BuildConfig.DEEPGRAM_API_KEY
```

> **Agent note:** If `secrets.properties` is absent (e.g., fresh CI clone), the build will still succeed using the fallback default values defined in `build.gradle.kts`. For production or staging environments, inject secrets via CI environment variables and write them to `secrets.properties` as part of the pipeline setup step.

---

## 🔨 Build & Local Verification (Gradle Commands)

All build operations are performed through the **Gradle Wrapper** (`./gradlew`). Never invoke a globally installed `gradle` binary — the wrapper guarantees the correct Gradle version is used.

### Standard Build Sequence for an Agent

**macOS / Linux / Git Bash:**

```bash
# 1. Generate local.properties if missing
[ -f local.properties ] || echo "sdk.dir=$ANDROID_HOME" > local.properties

# 2. Clean previous build outputs to ensure a reproducible build
./gradlew clean

# 3. Compile and package the debug variant (development build)
./gradlew assembleDebug
```

**Windows PowerShell:**

```powershell
# 1. Generate local.properties if missing
if (-not (Test-Path local.properties)) {
    $sdkPath = $env:ANDROID_HOME -replace '\\', '/'
    "sdk.dir=$sdkPath" | Out-File -FilePath local.properties -Encoding utf8 -NoNewline
}

# 2. Clean previous build outputs
.\gradlew clean

# 3. Compile and package the debug variant
.\gradlew assembleDebug
```

The resulting APK will be located at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Additional Useful Build Tasks

```bash
# Compile all source sets without packaging (faster, catches compilation errors)
./gradlew compileDebugKotlin

# Generate the release variant (requires signing config)
./gradlew assembleRelease

# Run lint checks and generate an HTML report
./gradlew lintDebug

# Show all available Gradle tasks for the project
./gradlew tasks --group=build
```

> **Agent note:** Always run `assembleDebug` (not just `compileDebugKotlin`) before marking a feature task as complete. Assembly validates resource merging, manifest merging, and dependency resolution in addition to Kotlin compilation.

---

## 🗄️ Database Schema Tracking (Room Schema Tracker)

Room generates a JSON snapshot of every database version at compile time. These snapshots are the source of truth for migration validation and must be committed alongside every change that modifies a Room entity, DAO, or database definition.

### Why This Matters for Agents

A missing or stale schema snapshot breaks automated migration tests and makes it impossible for future agents to reason about the persistence layer's history. Generating and committing the snapshot is a **mandatory step** — not optional.

### Step 1 — Configure Schema Export

Ensure `gradle.properties` contains the schema output path (already listed in the Environment Configuration section above):

```properties
room.schemaLocation=./schemas
```

And confirm the Room annotation processor reads this property in the app module's `build.gradle.kts`:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
```

### Step 2 — Generate Schema Snapshots

After modifying any Room entity, DAO, or database class, run the KSP compilation task to force schema export:

```bash
# Triggers KSP annotation processing for the debug variant,
# which exports the Room schema JSON to the /schemas directory
./gradlew kspDebugKotlin
```

### Step 3 — Validate the Output

After running `kspDebugKotlin`, verify the snapshot was created or updated:

```bash
# macOS / Linux / Git Bash
# List schema files — each database version produces one JSON file
ls -lh schemas/

# Inspect the generated JSON for the current database version
cat schemas/<your.database.ClassName>/<version_number>.json
```

```powershell
# Windows PowerShell equivalents
Get-ChildItem schemas\ -Recurse

Get-Content "schemas\<your.database.ClassName>\<version_number>.json"
```

Expected output structure:

```
schemas/
└── com.yourapp.data.local.AppDatabase/
    ├── 1.json
    ├── 2.json
    └── 3.json   ← newly generated after the schema change
```

### Step 4 — Agent Verification Gate

**Before committing any task that touches persistence**, the agent must confirm:

1. The `/schemas` directory contains a new or updated JSON file matching the new `version` value in the `@Database` annotation.
2. The diff shows only the expected structural changes (new tables, new columns, dropped columns).
3. A corresponding migration class exists in the `AppDatabase` migrations block for every version increment.

```bash
# macOS / Linux / Git Bash
grep -r "version =" app/src/main/java/**/*Database*.kt
ls schemas/com.yourapp.data.local.AppDatabase/
```

```powershell
# Windows PowerShell
Select-String -Path "app\src\main\java\**\*Database*.kt" -Pattern "version =" -Recurse
Get-ChildItem "schemas\com.yourapp.data.local.AppDatabase\"
```

---

## 🧪 Testing Suite

### Local Unit Tests (JVM — no emulator required)

Unit tests run on the JVM in the local development machine or CI container. They cover ViewModels, Use Cases, Repositories, and any pure Kotlin logic. This is the fastest feedback loop and must pass before proceeding to instrumented tests.

```bash
# Run all unit tests for the debug build variant
./gradlew testDebugUnitTest

# Run unit tests and generate an HTML coverage report (requires JaCoCo setup)
./gradlew testDebugUnitTestCoverage

# Run a specific test class
./gradlew testDebugUnitTest --tests "com.yourapp.feature.auth.viewmodel.LoginViewModelTest"
```

Test reports are generated at:

```
app/build/reports/tests/testDebugUnitTest/index.html
```

---

### Instrumented Tests — UI & Integration (requires a running emulator or device)

Instrumented tests run on a physical device or Android emulator. They cover Jetpack Compose UI interactions, Navigation component flows, and Room database integration.

**macOS / Linux / Git Bash:**

```bash
# Start an AVD in headless mode (CI-friendly)
$ANDROID_HOME/emulator/emulator -avd <AVD_NAME> -no-window -no-audio &

# Wait for the device to be fully booted
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done; input keyevent 82'

# Run all instrumented tests on the connected device/emulator
./gradlew connectedDebugAndroidTest

# Run a specific instrumented test class
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yourapp.feature.home.ui.HomeScreenTest
```

**Windows PowerShell:**

```powershell
# Start an AVD in headless mode
Start-Process -FilePath "$env:ANDROID_HOME\emulator\emulator.exe" `
  -ArgumentList "-avd <AVD_NAME> -no-window -no-audio" -NoNewWindow

# Wait for the device to be fully booted (poll adb)
do { Start-Sleep -Seconds 2 } until ((adb shell getprop sys.boot_completed) -eq "1")
adb shell input keyevent 82

# Run all instrumented tests
.\gradlew connectedDebugAndroidTest

# Run a specific instrumented test class
.\gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.yourapp.feature.home.ui.HomeScreenTest"
```

Test reports are generated at:

```
app/build/reports/androidTests/connected/debug/index.html
```

---

### Code Style & Lint Checks

Run these checks after every code change and before any commit. Both must exit with zero errors.

```bash
# Kotlin code style check (ktlint via Gradle plugin)
# Reports violations without auto-fixing
./gradlew ktlintCheck

# Auto-fix style violations where possible
./gradlew ktlintFormat

# Android Lint — catches correctness, security, and performance issues
./gradlew lintDebug
```

Lint report location:

```
app/build/reports/lint-results-debug.html
```

> **Agent note:** `ktlintFormat` is safe to run automatically and should be part of every agent's standard pre-commit sequence. Never commit code that produces `ktlintCheck` or `lintDebug` failures.

> **ktlint availability:** The `ktlintCheck` and `ktlintFormat` tasks are only available when the ktlint Gradle plugin is configured in the project. Before running these tasks, confirm the plugin is present: `./gradlew tasks --group=verification | grep ktlint`. If absent, skip these tasks and note them as N/A in the Definition of Done.

---

### Full Pre-Commit Verification Pipeline

Run this sequence as a single agent step before staging files for commit. All tasks must succeed (exit code `0`) for the commit to be valid:

> **ktlint is optional:** Include `ktlintFormat` and `ktlintCheck` only if the ktlint Gradle plugin is configured in the project. If those tasks are not registered, omit them and note N/A in the Definition of Done checklist.

**macOS / Linux / Git Bash** (backslash line continuation):

```bash
./gradlew clean \
  kspDebugKotlin \
  ktlintFormat \
  ktlintCheck \
  assembleDebug \
  lintDebug \
  testDebugUnitTest
```

**Windows PowerShell** (backtick line continuation):

```powershell
.\gradlew clean `
  kspDebugKotlin `
  ktlintFormat `
  ktlintCheck `
  assembleDebug `
  lintDebug `
  testDebugUnitTest
```

**Windows cmd / one-liner (any OS):**

```
gradlew.bat clean kspDebugKotlin ktlintFormat ktlintCheck assembleDebug lintDebug testDebugUnitTest
```

---

## ✅ Definition of Done — Agent Success Checklist

Before creating a commit or opening a Pull Request, the autonomous agent must self-evaluate every item below. A task is only complete when all applicable items are checked. Non-applicable items (e.g., Room schema when no persistence was changed) must be explicitly noted as N/A with a reason.

### Compilation & Build

- [ ] `./gradlew clean assembleDebug` exits with code `0` — no compilation errors, no resource merge failures, no manifest conflicts.
- [ ] No unresolved `TODO` markers were introduced in the scope of this task.
- [ ] No hardcoded secrets, API keys, or local file paths appear in committed source files.

### Code Quality

- [ ] `./gradlew ktlintCheck` exits with code `0` — all Kotlin files conform to the project's style rules. *(Skip and mark N/A if ktlint plugin is not configured in the project.)*
- [ ] `./gradlew lintDebug` exits with code `0` (or all remaining issues are pre-existing and documented with `@SuppressLint` with a justification comment).
- [ ] All new public functions, classes, and composables have KDoc documentation.
- [ ] No unused imports, dead code, or commented-out code blocks were left in modified files.

### Unit Tests

- [ ] `./gradlew testDebugUnitTest` exits with code `0` — zero test failures.
- [ ] Every new ViewModel, Use Case, or Repository introduced in this task has corresponding unit tests covering the happy path and at least one error/edge-case path.
- [ ] Test names follow the pattern `given_<context>_when_<action>_then_<expected>` or equivalent descriptive English.

### Room Database / Persistence (skip if no persistence changes)

- [ ] `./gradlew kspDebugKotlin` ran successfully and the `/schemas` directory contains a new or updated JSON file for the current database version.
- [ ] The `@Database(version = N)` annotation was incremented if any entity or DAO was modified.
- [ ] A `Migration(N-1, N)` object is defined and registered in the database builder — no `fallbackToDestructiveMigration()` unless explicitly approved.
- [ ] The generated schema JSON is staged for commit alongside the source code changes.

### Network Contract

- [ ] No changes were made to existing API request/response data classes that would break the network contract defined in `docs/api-spec.yml` without a corresponding spec update.
- [ ] If new API endpoints were consumed, the `Retrofit` service interface and its models are type-safe and match the spec.
- [ ] Error response handling (network failures, 4xx, 5xx) is implemented and tested for every new network call.

### UI / Jetpack Compose (skip if no UI changes)

- [ ] All new composables are stateless where possible (state hoisted to ViewModel or caller).
- [ ] New screens are connected to the Navigation graph (`nav_graph.xml` or Compose Navigation) and reachable from the intended entry points.
- [ ] No hardcoded dimension values, colors, or strings appear in composables — all values reference theme tokens (`MaterialTheme.colorScheme`, `MaterialTheme.typography`) or string resources (`stringResource()`).
- [ ] Content descriptions are provided for all non-decorative `Image` and `Icon` composables.

### Git Hygiene

- [ ] The commit message follows the Conventional Commits format: `<type>(<scope>): <short English description>` (e.g., `feat(auth): add biometric login flow`).
- [ ] The branch is up-to-date with `main` (or the target base branch) — no unresolved merge conflicts.
- [ ] Only files related to the current task are staged — no accidental inclusion of `local.properties`, `secrets.properties`, `*.iml`, or build output directories.
