# Field Feature Module — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` ← `features/field/AGENTS.md` (this file)

Read parent docs first, then this file for field-feature-specific contracts.

## Purpose

The `features/field/` module is the core product boundary of FieldMind. It contains all domain logic, data access, and UI for the field research tool — observations, species ID, hypotheses, evidence, maps, weather, flashcards, projects, reports, and analysis.

## Ownership

- `features/field/data/` — All data-layer packages (weather, vision, ai, database, repository, security, export, location, species, settings, stats, flashcard, question, timer, learn, analysis, attachment, background, bulk, undo)
  - See `features/field/data/AGENTS.md` for detailed ownership
- `features/field/presentation/` — All UI-layer packages (screens, components, navigation, viewmodel, theme, utils)
  - See `features/field/presentation/AGENTS.md` for detailed ownership

## Local Contracts

### Module Boundaries
- Data layer is in `data/` subpackages, presentation layer in `presentation/` — clean separation
- Data layer never depends on presentation layer
- Presentation layer depends on data layer through `FieldMindViewModel` and `FieldMindRepository`
- No DI framework; manual constructor injection flows from ViewModel down

### Key Architecture Rules
- All screens registered in `FieldMindScreens.kt` (sealed class) + `FieldMindNavigation.kt` (composable routes)
- Weather providers implement `WeatherProvider` interface, registered in `FieldMindRepository`
- Species classifiers implement `SpeciesClassifier` interface
- Room entities annotated with `@Entity`, DAOs with `@Dao`
- Background workers extend `CoroutineWorker`, scheduled via `FieldMindBackgroundScheduler`

## Work Guidance

- When adding a new data domain (e.g., a new weather provider):
  1. Create package in `data/{domain}/`
  2. Implement the appropriate interface (`WeatherProvider`, `SpeciesClassifier`, etc.)
  3. Register in `FieldMindRepository`
- When adding a new screen:
  1. Create composable in `presentation/screens/`
  2. Register in `FieldMindScreens.kt`
  3. Add route in `FieldMindNavigation.kt`
- When adding new Room entities/DAOs:
  1. Add entity in `data/database/entity/`
  2. Add DAO in `data/database/dao/`
  3. Update `FieldMindDatabase.kt` version and migrations
  4. Export schema by setting `room.schemaLocation` in build config
- See child AGENTS.md files for granular guidance

## Verification

- Feature compiles when parent app module compiles (CI validates)
- Navigation routes resolve correctly
- Data layer unit tests (when added)

## Child DOX Index

- `data/AGENTS.md` — Data layer: weather, vision, AI, database, repository, all domain packages
- `presentation/AGENTS.md` — Presentation layer: screens, components, navigation, viewmodel, theme
