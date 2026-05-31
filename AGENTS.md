# Qhathia Kiosk — Agent Instructions

## Before doing anything

Read `CLAUDE.md` at the project root. It contains the project overview, documentation map, skills table, and standard workflow. All instructions there apply to every task.

---

## Load the right skill for the task

| Task type | Skill to load first |
|---|---|
| Implementing Android code (any layer) | `.claude/skills/android-developer/SKILL.md` |
| Proposing a new OpenSpec change | `.claude/skills/openspec-propose/SKILL.md` |
| Implementing OpenSpec tasks | `.claude/skills/openspec-apply-change/SKILL.md` |
| Archiving a completed change | `.claude/skills/openspec-archive-change/SKILL.md` |
| Exploring OpenSpec change state | `.claude/skills/openspec-explore/SKILL.md` |

**Do not write any code before loading the android-developer skill.** It defines which files to read, module creation rules, and Definition of Done.

---

## Non-negotiable rules

1. Every new feature or core layer is a **separate Gradle module** — never a package inside `:app`. Follow the "Creating a New Module" checklist in `.claude/skills/android-developer/android-frontend-standards.md`.
2. Always update `settings.gradle.kts` when adding a module. A directory without `include()` does not exist for Gradle.
3. Use KSP2 (`ksp(...)`), never kapt.
4. Never apply `kotlin-android` plugin explicitly — AGP 9.x hard error.
5. No LiveData. No `GlobalScope`. No `!!` operator.
6. Validate Definition of Done before marking any task complete.

---

## Standard workflow

```
/write-user-story  →  /opsx:propose  →  /opsx:apply  →  /opsx:archive
```
