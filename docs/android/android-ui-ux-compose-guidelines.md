---
description: Android UI/UX implementation standards, Jetpack Compose conventions, Figma-driven development rules, and frontend design-system guidelines for Modern Android Development.
globs: [
  "**/src/main/**/*.kt",
  "**/src/debug/**/*.kt",
  "**/src/androidTest/**/*.kt"
]
alwaysApply: true
---

# Android UI/UX Compose — Guidelines & Design Implementation Playbook

## Table of Contents

- [Overview](#overview)
- [UI/UX Philosophy](#uiux-philosophy)
- [Figma-Driven Development](#figma-driven-development)
  - [Figma as Source of Truth](#figma-as-source-of-truth)
  - [Design Fidelity Requirements](#design-fidelity-requirements)
  - [Figma Validation Checklist](#figma-validation-checklist)
- [Jetpack Compose Standards](#jetpack-compose-standards)
  - [Composable Design Rules](#composable-design-rules)
  - [State Hoisting](#state-hoisting)
  - [Compose Stability & Recomposition](#compose-stability--recomposition)
  - [Side-Effects Handling](#side-effects-handling)
  - [Material Design 3 & Theming](#material-design-3--theming)
  - [Navigation](#navigation)
  - [Accessibility](#accessibility)
  - [Adaptive Layout & Responsiveness](#adaptive-layout--responsiveness)
  - [Animation & Motion Guidelines](#animation--motion-guidelines)
- [Design System & Tokens](#design-system--tokens)
  - [Spacing Rules](#spacing-rules)
  - [Typography Rules](#typography-rules)
  - [Color Usage Rules](#color-usage-rules)
  - [Elevation & Shape Rules](#elevation--shape-rules)
- [Component Architecture](#component-architecture)
  - [Component Hierarchy](#component-hierarchy)
  - [Component Reusability Rules](#component-reusability-rules)
  - [Preview Requirements](#preview-requirements)
- [Screen Implementation Workflow](#screen-implementation-workflow)
- [UI Testing Expectations](#ui-testing-expectations)
- [AI Agent UX Constraints](#ai-agent-ux-constraints)
- [Definition of Done for UI Features](#definition-of-done-for-ui-features)

---

# Overview

This document defines the mandatory UI/UX implementation standards for all Android frontend development using Jetpack Compose.

Its purpose is to ensure:
- visual consistency
- scalable UI architecture
- design-system compliance
- Figma implementation fidelity
- accessibility compliance
- reusable Compose patterns
- predictable AI-assisted development behavior

All UI implementations must follow these guidelines strictly.

Deviation from these standards requires explicit architectural or design approval.

---

# UI/UX Philosophy

The project follows a:
- design-system-first approach
- Figma-driven implementation workflow
- accessibility-first mindset
- reusable component architecture
- consistency-over-creativity philosophy

UI implementation must prioritize:
1. consistency
2. maintainability
3. accessibility
4. responsiveness
5. scalability

Visual creativity must never compromise:
- usability
- consistency
- accessibility
- performance
- implementation predictability

---

# Figma-Driven Development

## Figma as Source of Truth

Figma is the single source of truth for all UI implementation.

Whenever a Figma reference exists:
- layouts must follow Figma
- spacing must follow Figma
- typography hierarchy must follow Figma
- interaction behavior must follow Figma
- component structure must follow Figma

AI agents and engineers must never redesign screens if a Figma reference is available.

### Forbidden Behaviors

- Inventing new layouts
- Introducing speculative UX
- Replacing existing flows
- Adding non-approved visual patterns
- Ignoring design-system rules

### Required Behaviors

- Reuse existing shared composables
- Respect design tokens
- Preserve visual hierarchy
- Preserve interaction semantics
- Preserve responsive behavior

If implementation details are ambiguous:
1. inspect adjacent Figma screens
2. inspect existing app patterns
3. prioritize consistency

---

## Design Fidelity Requirements

The implementation must preserve:
- spacing rhythm
- typography scale
- visual hierarchy
- component sizing
- component states
- responsive structure
- interaction feedback

Supported states include:
- loading
- empty
- disabled
- pressed
- focused
- selected
- error
- success

Dark mode compatibility must be considered when required by the design system.

---

## Figma Validation Checklist

Before considering a screen complete, validate:

- spacing consistency
- typography hierarchy
- component alignment
- responsive behavior
- dark mode compatibility
- accessibility semantics
- loading states
- error states
- empty states
- TalkBack compatibility
- touch target sizes
- navigation consistency

---

# Jetpack Compose Standards

## Composable Design Rules

- Every composable must be a pure function of its inputs.
- Side-effects are forbidden inside composable bodies outside approved Compose side-effect APIs.
- Composables must remain small and focused.
- Composables larger than ~80 lines should be decomposed.
- Compose functions must use noun-based naming.
- Reusable composables must expose a `modifier` parameter.
- Business logic inside composables is forbidden.

### Naming Examples

```kotlin
// Correct
ProductCard()

// Avoid
ShowProductCard()
```

---

## State Hoisting

State must always be hoisted to the lowest common ancestor that requires it.

Rules:

- Screens own ViewModel connections.
- Stateless composables receive state via parameters.
- Reusable components must not own business state.
- UI events flow upward.
- State flows downward.

Naming conventions:

| Type | Convention |
|---|---|
| Stateful composable | `<Feature>Screen` |
| Stateless composable | `<Feature>Content` |
| Reusable component | noun-based |

Correct:

```kotlin
ProductListScreen()
ProductListContent()
ProductCard()
```

---

## Compose Stability & Recomposition

Unnecessary recompositions are a primary source of UI jank.

Rules:

- Use immutable state whenever possible.
- Prefer `@Immutable` or `@Stable`.
- Never pass mutable collections directly into composables.
- Memoize expensive computations using `remember`.
- Prefer stable function references.
- Use `derivedStateOf` for derived UI state.
- Always provide stable keys in lazy lists.

Correct:

```kotlin
ProductCard(
    onClick = viewModel::onOpenDetail,
)
```

Avoid:

```kotlin
ProductCard(
    onClick = { viewModel.onOpenDetail(id) },
)
```

---

## Side-Effects Handling

Use the appropriate Compose side-effect API for each scenario.

| API | Usage |
|---|---|
| `LaunchedEffect` | Launch coroutines tied to composition |
| `rememberCoroutineScope` | User-triggered coroutines |
| `rememberUpdatedState` | Preserve latest lambda references |
| `DisposableEffect` | Resource cleanup |
| `SideEffect` | Synchronize external state |
| `produceState` | Convert async sources into Compose state |

Side-effects must never execute directly inside composable bodies.

Correct:

```kotlin
LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
        // handle effect
    }
}
```

---

## Material Design 3 & Theming

The project uses Material Design 3 exclusively.

Rules:

- Themes must be centralized.
- Colors must come from `MaterialTheme`.
- Typography must come from `MaterialTheme`.
- Hardcoded colors are forbidden.
- Hardcoded typography values are forbidden.
- Strings must use `stringResource()`.
- Dimensions should come from tokens or shared dimension resources.

Correct:

```kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.typography.titleLarge
```

Avoid:

```kotlin
Color(0xFF000000)
Text(fontSize = 18.sp)
```

Dark mode support is mandatory whenever supported by the product.

---

## Navigation

Navigation must remain predictable and consistent.

Rules:

- Navigation actions should originate from ViewModel effects.
- Navigation must never happen directly during composition.
- Deep links must preserve expected flows.
- Complex objects must not be passed between destinations.
- Only IDs or primitives may be passed through routes.
- Navigation transitions must remain subtle and performant.

---

## Accessibility

Accessibility compliance is mandatory.

Rules:

- Every interactive component must expose semantics.
- Touch targets must be at least 48dp.
- Color alone must never communicate meaning.
- Decorative icons should use `contentDescription = null`.
- Important icons must provide localized descriptions.
- UI must support dynamic font scaling.
- Contrast ratios must remain accessible.

Correct:

```kotlin
Icon(
    imageVector = Icons.Default.Favorite,
    contentDescription = stringResource(R.string.cd_favorite),
)
```

TalkBack validation is part of the definition of done.

---

## Adaptive Layout & Responsiveness

The UI must support:

- compact phones
- large phones
- tablets
- foldables
- landscape mode
- dynamic text scaling

Rules:

- Avoid fixed widths and heights.
- Prefer adaptive layouts.
- Prevent clipping and overlap.
- Use responsive spacing.
- Validate layouts across multiple device sizes.
- Use `WindowSizeClass` when appropriate.

---

## Animation & Motion Guidelines

Animations should improve clarity, hierarchy, and UX feedback.

Rules:

- Prefer subtle motion.
- Avoid excessive animation.
- Motion should reinforce hierarchy.
- Loading states should feel responsive.
- Animations must remain performant.
- Infinite animations should be avoided unless explicitly required.

Recommended APIs:

- `AnimatedVisibility`
- `Crossfade`
- `animate*AsState`
- `updateTransition`

---

# Design System & Tokens

## Spacing Rules

Spacing must follow the centralized design token system.

Rules:

- Avoid arbitrary spacing values.
- Prefer spacing scales.
- Preserve spacing rhythm across screens.
- Shared spacing tokens must be reused consistently.

Recommended spacing scale:

| Token | Value |
|---|---|
| xs | 4dp |
| sm | 8dp |
| md | 16dp |
| lg | 24dp |
| xl | 32dp |

---

## Typography Rules

Typography hierarchy must remain consistent.

Rules:

- Use semantic typography roles.
- Avoid custom font sizing unless approved.
- Preserve hierarchy between titles, subtitles, and body text.
- Typography must remain accessible.

Typography should always come from `MaterialTheme.typography`.

---

## Color Usage Rules

Color usage must follow semantic meaning.

Rules:

- Avoid arbitrary colors.
- Use semantic roles.
- Preserve accessible contrast.
- Support dark mode.
- Color must never be the sole meaning carrier.

Correct:

```kotlin
MaterialTheme.colorScheme.error
```

Avoid:

```kotlin
Color.Red
```

---

## Elevation & Shape Rules

Elevation and shape usage must remain consistent across the application.

Rules:

- Use centralized shape tokens.
- Avoid inconsistent corner radii.
- Elevation should reinforce hierarchy.
- Prefer subtle elevation.
- Avoid excessive shadows.

---

# Component Architecture

## Component Hierarchy

UI should follow layered component architecture.

Recommended hierarchy:

```text
Screen
  ↓
Content
  ↓
Section
  ↓
Reusable Component
```

Rules:

- Screens connect ViewModels.
- Content composables remain stateless.
- Components remain reusable and isolated.
- Shared components belong to the design system.

---

## Component Reusability Rules

Before creating a new component:

1. inspect existing shared components
2. inspect the design system
3. extend existing APIs when possible
4. avoid duplicate implementations

Reusable components must:

- remain stateless
- expose modifiers
- support accessibility
- support previews
- support dark mode
- support responsive layouts

Feature modules must not create isolated design systems.

---

## Preview Requirements

Every reusable composable must provide previews.

Required previews:

- light mode
- dark mode
- loading state
- empty state
- populated state
- error state

Correct:

```kotlin
@Preview(showBackground = true)
@Composable
private fun ProductCardPreview()
```

---

# Screen Implementation Workflow

Recommended implementation workflow:

1. inspect Figma screen
2. identify reusable components
3. map design tokens
4. implement stateless UI
5. connect ViewModel state
6. validate responsiveness
7. validate accessibility
8. add previews
9. add UI tests
10. validate against Figma

Implementation priority:

```text
Figma
  ↓
Design System
  ↓
Reusable Components
  ↓
Feature Screen
```

---

# UI Testing Expectations

UI testing is mandatory for critical user flows.

Recommended coverage:

| Type | Scope |
|---|---|
| Component tests | Reusable composables |
| Screen tests | Stateful flows |
| Navigation tests | Navigation behavior |
| Accessibility tests | Semantics validation |

Rules:

- Compose tests must use AppTheme.
- Screens must remain testable in isolation.
- UI assertions should prefer semantics.
- Business logic inside UI tests should be avoided.

Correct:

```kotlin
composeTestRule
    .onNodeWithText("Continue")
    .assertIsDisplayed()
```

---

# AI Agent UX Constraints

AI agents must behave conservatively during UI implementation.

AI agents must:

- prioritize consistency over creativity
- preserve existing UX patterns
- reuse shared components
- follow design tokens
- preserve navigation consistency
- preserve accessibility semantics
- validate responsive behavior

AI agents must never:

- redesign flows without approval
- introduce speculative UI
- create parallel design systems
- ignore Figma references
- hardcode styling values
- bypass accessibility requirements

If ambiguity exists:

1. inspect adjacent screens
2. inspect existing patterns
3. prioritize consistency
4. avoid speculative behavior

---

# Definition of Done for UI Features

A UI implementation is not complete unless:

- implementation matches Figma
- dark mode is validated
- accessibility is validated
- previews are implemented
- responsive behavior is validated
- loading/error/empty states exist
- strings are externalized
- reusable components are extracted
- UI tests pass successfully
- TalkBack behavior is validated

All UI work must preserve consistency with the global design system.

---

This document is the living standard for Android UI/UX development in this project.

All contributors and AI agents are expected to follow these conventions rigorously to ensure:

- scalable frontend architecture
- maintainable Compose code
- accurate Figma implementation
- consistent user experience
- long-term design system integrity

