---
name: android-developer
description: Load all Android development standards and context before implementing any Kotlin/Compose code. Use whenever implementing a feature, creating a module, modifying architecture, writing tests, or reviewing Android code.
metadata:
  author: qhathia
  version: "1.1"
---

# Android Developer Skill

Load and apply all Android development standards for this project before writing any code.

> **Single source of truth:** The standards live under `docs/android/` (and `docs/product/`).
> This skill intentionally references those canonical files directly — it no longer keeps its
> own duplicate copies, so there is nothing to drift out of sync.

---

## When to use this skill

Load this skill before:
- Implementing any feature (new screen, ViewModel, UseCase, Repository, etc.)
- Creating a new Gradle module
- Writing or modifying Compose UI
- Writing tests (unit, integration, or UI)
- Reviewing or refactoring existing Android code
- Implementing any API integration

---

## Step 1 — Read architecture and module standards

Read the full contents of:

```
docs/android/android-frontend-standards.md
```

This is the **primary reference**. Pay special attention to:
- Module Taxonomy — every new layer must be a Gradle module, not a package inside `:app`
- Creating a New Module checklist — follow it step by step when a module does not yet exist
- Module `build.gradle.kts` templates — use the correct template for each module type
- Dependency Graph Rules — never violate the allowed dependency directions
- MVI Contract — UiState / UiEvent / UiEffect structure
- Naming Convention Reference Table — all artifact names must match exactly
- Current project state note — the project is in single-module transitional state

---

## Step 2 — Read Compose UI/UX guidelines

Read the full contents of:

```
docs/android/android-ui-ux-compose-guidelines.md
```

Pay special attention to:
- Stateless vs. stateful composable split (`*Content` vs. `*Screen`)
- State hoisting rules
- Material Design 3 theming — always use design tokens, never hardcoded colors or dimensions
- Accessibility requirements
- Preview requirements (light + dark mode mandatory)

---

## Step 3 — Read API contract (when the task touches network or data layer)

If the task involves any API call, DTO, repository, or remote data source, read:

```
docs/android/api-contract.md
```

Pay special attention to:
- Available endpoints and their exact request/response shapes
- Authentication flow (Bearer JWT, switch-store)
- Recent contract changes — do not assume the API matches an older mental model

---

## Step 4 — Read development guide (when the task involves build, test, or environment)

If the task involves running Gradle tasks, writing tests, or setting up a module, read:

```
docs/android/android-development-guide.md
```

Pay special attention to:
- Correct `gradlew` invocation per OS
- Required environment variables (`ANDROID_HOME`, etc.)
- Gradle tasks for build, test, and lint validation
- AGP 9 / Hilt compatibility flags and the Kotlin metadata override (gradle.properties + ksp)

---

## Step 5 — Read product definition (when the task involves new business logic)

If the task introduces new business rules, domain models, or user flows, read:

```
docs/product/product-definition.md
```

Validate that the implementation aligns with the product vision, is within scope, and uses correct domain terminology from the Glossary.

---

## Implementation rules

After loading the above context, apply these rules without exception:

1. **Never add code to `:app`** for anything other than the application shell (NavHost, theme entry, Hilt wiring, manifest). All business logic, features, and core layers go into their own Gradle modules.

2. **Never create packages as a substitute for modules.** If a module does not exist yet, create it following the "Creating a New Module" checklist in `docs/android/android-frontend-standards.md`.

3. **Always update `settings.gradle.kts`** when creating a new module. A module directory without an `include()` entry does not exist for Gradle.

4. **Use KSP2, never kapt.** All annotation processors (Hilt, Room) must use `ksp(...)`, never `kapt(...)`.

5. **Never apply `kotlin-android` plugin explicitly.** AGP 9.x applies the Kotlin Android compiler automatically — adding it manually is a hard build error. (Do not set `android.builtInKotlin=false` either.)

6. **Every Hilt module needs the kotlin-metadata-jvm override.** On AGP 9 / Kotlin 2.3, add `ksp(libs.kotlin.metadata.jvm)` alongside `ksp(libs.hilt.compiler)`, or the build fails with "Provided Metadata instance has version 2.3.0".

7. **All ViewModels must be `@HiltViewModel`** with constructor injection. No manual ViewModel factories.

8. **No LiveData.** State is always `StateFlow` (for UI state) or `SharedFlow` (for one-shot effects).

9. **Never call `StateFlow.value` inside a composable body.** Always use `collectAsStateWithLifecycle()`.

10. **Every composable Screen must have a `@Preview`** for both light and dark mode before the task is considered done. (`@Preview` requires `implementation(libs.compose.ui.tooling.preview)` in the module.)

11. **Definition of Done:** unit tests written, `./gradlew lint` passes, previews exist for light and dark mode.

12. **Manifest audit after every task.** Before marking a task complete, check whether the implementation introduces any of the following and update `app/src/main/AndroidManifest.xml` accordingly:

    | What was introduced | What to add to the manifest |
    |---|---|
    | Any network call (Ktor) | `<uses-permission android:name="android.permission.INTERNET" />` |
    | Network state check | `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` |
    | ForegroundService | `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />` + `<service>` with `android:foregroundServiceType` |
    | Static BroadcastReceiver | `<receiver android:name="..." android:exported="false">` + optional `<intent-filter>` |
    | Background Service | `<service android:name="..." android:exported="false" />` |
    | FileProvider / ContentProvider | `<provider>` block with `android:authorities` and `<meta-data>` for paths |
    | Additional Activity | `<activity android:name="..." />` |
    | Camera, storage, location, etc. | Corresponding `<uses-permission>` — see the Manifest Reference section in `docs/android/android-frontend-standards.md` |

    Dynamic receivers registered in code (`context.registerReceiver(...)`) do **not** need a manifest entry. Static receivers (those that must work even when the app is not running) do.

---

## Output contract

When this skill is active, every implementation response must:
- State which module(s) are being created or modified
- List every file being created with its full path relative to the project root
- Show the complete `settings.gradle.kts` diff if a new module is added
- Show the `AndroidManifest.xml` diff if any permission or component was added
- Confirm DoD criteria at the end of the implementation
