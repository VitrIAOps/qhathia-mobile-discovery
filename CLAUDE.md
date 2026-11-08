# Qhathia Kiosk — Claude Project Context

## Project Overview

This is the **Merchant Operational Tool** of a dual-interface commerce ecosystem. It is an Android application that gives store owners and sales staff a real-time inventory and order management console, designed to be 100% mobile-operated with minimal learning curve.

It is **not** a POS, not a payment gateway, and not a logistics tool. Its core specialization is order orchestration and real-time stock visibility.

Full product definition: `docs/product/product-definition.md`

---

## Technology Stack

- **Language:** Kotlin (Jetpack Compose, Clean Architecture + MVI)
- **DI:** Hilt (KSP2 — never kapt)
- **Network:** Ktor Client (CIO engine)
- **Build:** Gradle 9.x with Version Catalog (`gradle/libs.versions.toml`)
- **Min SDK:** 26 · Target/Compile SDK: 36

---

## Key Rules — Read Before Implementing Anything

1. Read all relevant files in `docs/` before implementing any change.
2. Follow the architecture strictly — no undocumented patterns.
3. Reuse existing project patterns.
4. Every new feature or core layer must be a **proper Gradle module** — never a package inside `:app`. See `docs/android/android-frontend-standards.md` → "Creating a New Module".
5. Validate Definition of Done before marking a task complete.

---

## Documentation Map

| What you need | Where to look |
|---|---|
| Product vision, personas, glossary | `docs/product/product-definition.md` |
| Android architecture, module taxonomy, build templates | `docs/android/android-frontend-standards.md` |
| Compose UI/UX patterns, theming, accessibility | `docs/android/android-ui-ux-compose-guidelines.md` |
| Environment setup, Gradle tasks, build commands | `docs/android/android-development-guide.md` |
| API contracts, endpoints, DTOs | `docs/android/api-contract.md` |

---

## Skills

Skills live under `.claude/skills/`. Load the appropriate skill before starting any task.

| Task type | Skill to load |
|---|---|
| Implementing Android code | `.claude/skills/android-developer/SKILL.md` |
| OpenSpec (propose, apply, archive, explore) | In the workspace repo — see "Specs (OpenSpec)" |

---

## Commands

Slash commands live under `.claude/commands/`.

| Command | Purpose |
|---|---|
| `/write-user-story` | Enrich a ClickUp task or feature request with business + technical context |
| `/enrich-user-story` | Lighter enrichment — business context only, no ClickUp write-back |
| `/opsx:*` | OpenSpec commands — run them from the workspace root |

---

## Specs (OpenSpec)

Specs and changes for this app no longer live in this repo. They live in the
**qhathia-mobile-workspace** repository, which includes this repo as a git submodule:

- Living specs: `openspec/specs/discovery/` in the workspace.
- Changes and archive: `openspec/changes/` in the workspace.
- OpenSpec skills and `/opsx:*` commands: `.claude/` in the workspace.

Run every OpenSpec workflow from the workspace root, not from this repo.

---

## Standard Implementation Workflow

Run from the workspace root; code lands in this submodule.

```
ClickUp Task / feature request
        ↓
/write-user-story          → .openspec/enriched/<change>.md
        ↓
/opsx:propose              → generates OpenSpec artifacts
        ↓
/opsx:apply                → loads android-developer skill + implements
        ↓
/opsx:archive
```

---

## Project Structure — Gradle Modules

The project is in a **single-module transitional state** (`settings.gradle.kts` currently only declares `:app`). All new features and core layers must be created as separate Gradle modules. See the full module taxonomy and `build.gradle.kts` templates in `docs/android/android-frontend-standards.md`.

```
root/
├── app/                  # Application shell only — no business logic
├── core/
│   ├── domain/           # Pure Kotlin — UseCases, domain models, repository interfaces
│   ├── data/             # Repository implementations, datasources, mappers
│   ├── network/          # Ktor client, API services, DTOs
│   ├── ui/               # Shared Compose components, theme, design tokens
│   ├── testing/          # Shared test utilities, fakes, fixtures
│   └── common/           # Kotlin extensions, utilities, constants
├── features/
│   └── <feature-name>/   # Screen, ViewModel, UiState, UiEvent, NavGraph
└── build-logic/          # Convention plugins (future)
```
