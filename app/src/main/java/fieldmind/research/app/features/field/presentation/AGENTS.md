# Field Feature Presentation Layer — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` ← `features/field/AGENTS.md` ← `features/field/presentation/AGENTS.md` (this file)

Read parent docs first, then this file for presentation-layer-specific contracts.

## Purpose

The `presentation/` package contains all UI code for the FieldMind research tool: screens, reusable composables, navigation graph, ViewModel state management, and feature-specific theming.

## Ownership

- `screens/` — All screen composables (~25+ screens)
  - Home, Observe, Detail, Settings, Map, Weather, Library, Learn, Timer, Lock
  - Backup/Export, Archive, Projects, Reports, Insights, Research Session, Changelog
  - Onboarding, Flashcards, Questions, Species Browser, Taxonomic Browser, Data Tools
  - `screens/species/` — Species identification bottom sheet
- `components/` — Reusable composables
  - Weather animations (`AnimatedWeatherScene`), icons (`FieldMindIcons`), charts (`FieldMindCharts`, `FieldMindChartsExtended`)
  - Camera capture (`FieldMindCameraCapture`, `FieldMindCameraV2`), audio player, PDF viewer, image viewer/gallery
  - Data tables, quality components, privacy components (`ScreenSecurityUtils`, `ClipboardSecurityUtils`)
  - Snackbars, transitions, shared transitions, motion helpers
  - Timeline, species detail sheet, home species catalog, components library
- `navigation/` — Navigation graph (`FieldMindNavigation.kt`) and screen routes (`FieldMindScreens.kt`)
- `viewmodel/` — `FieldMindViewModel` (central state management)
- `theme/` — Feature-level theme (colors, typography via `FieldMindTheme`)
- `utils/` — `AppLifecycleManager`

## Local Contracts

### Screen Architecture
- All screens are top-level `@Composable` functions
- Each screen receives navigation callbacks as parameters (`onBack: () -> Unit`, `onNavigateTo: (String) -> Unit`)
- Screens use `FieldMindViewModel` via `LocalFieldMindViewModel.current` or composition local
- All screens registered in `FieldMindScreens.kt` (sealed class with route strings)
- Routes registered in `FieldMindNavigation.kt` using `composable(route)`

### Component Architecture
- Reusable components in `components/` — no screen-specific logic
- Components receive all data as parameters (no direct ViewModel access in leaf components)
- Follow Material3 design patterns with `FieldMindTheme.colors` for theming

### State Management
- ViewModel exposes state via `StateFlow` collected as Compose state
- UI state modeled as sealed classes or data classes
- Events flow up to ViewModel, state flows down to composables

## Work Guidance

- New screen: create composable → register in `FieldMindScreens.kt` → add route in `FieldMindNavigation.kt`
- New reusable component: add to `components/` package, keep it stateless/parameter-driven
- Changelog entries: update `FieldMindChangelogScreen.kt` (see root `AGENTS.md` "Updating What's New")
- Privacy-sensitive screens: use `ScreenSecurityUtils` to prevent screenshots in recents
- Theming: use `FieldMindTheme.colors` consistently; avoid hardcoded colors

## Verification

- Navigation routes resolve correctly (no 404s in app)
- Compose previews render (when added)
- No infinite-height constraint crashes (use `Modifier.heightIn(max=...)` inside scrollable columns)

## Child DOX Index

No child AGENTS.md files defined yet. Each screen/component is small enough to be covered by this file.
