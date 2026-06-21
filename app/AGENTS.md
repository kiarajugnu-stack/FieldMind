# Android Application Module — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. It follows the root `AGENTS.md` as its parent DOX rail.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then this file for app-specific contracts.

## Purpose

The `app/` module is a single-module Android application written in Kotlin that delivers **FieldMind** — a field research tool for scientists and citizen scientists. It supports observations, species identification, hypotheses, evidence collection, mapping, data analysis, weather tracking, flashcard-based learning, and AI-assisted research.

## Ownership

### Application Layer
- `app/src/main/java/fieldmind/research/app/activities/` — Android activities (`MainActivity`, `FieldMindCrashActivity`)
- `app/src/main/java/fieldmind/research/app/FieldMindApplication.kt` — Application class (init, crash reporting, lifecycle)

### Feature Layer (`features/field/`)
#### Data Layer (`data/`)
- `weather/` — 7 weather provider integrations (Open-Meteo, OpenWeatherMap, Met Norway, WeatherAPI, IMD, NWS, WeatherUnitConverter)
- `vision/` — Species classification (image analysis, pHash database, Perenual API integration)
- `ai/` — GeminiResearchAssistant (AI-powered research assistant)
- `database/` — Room database with `entity/` and `dao/` packages
- `repository/` — FieldMindRepository (central data access)
- `security/` — FieldMindPrivacyManager (biometric lock, screen protection, clipboard security)
- `export/` — Report generation, export/import with encryption, media packing
- `location/` — GPS provider, track recording, map drawing tools, Maplibre offline maps, geo-fencing
- `species/` — Taxonomy data and species catalogs
- `settings/` — App settings persistence
- `stats/` — Streaks and statistics tracking
- `flashcard/` — SM-2 spaced repetition engine + smart flashcard generator
- `question/` — Research question generator
- `timer/` — Field timer utilities
- `learn/` — Learn library and curriculum content
- `analysis/` — Pattern detection engine
- `attachment/` — File attachment management
- `background/` — Background workers (reminders, auto-backup)
- `bulk/` — Bulk operations
- `undo/` — Undo/redo support

#### Presentation Layer (`presentation/`)
- `screens/` — All screen composables (Home, Observe, Detail, Settings, Map, Weather, Library, Learn, Timer, Lock, Backup/Export, Archive, Projects, Reports, Insights, Research Session, Changelog, Onboarding, Flashcards, Questions, Species Browser, Taxonomic Browser, Data Tools, etc.)
- `screens/species/` — Species identification bottom sheet
- `components/` — Reusable composables (weather animations, icons, charts, camera capture, audio player, PDF viewer, image gallery, data tables, privacy components, snackbars, transitions, etc.)
- `navigation/` — Navigation graph and routes
- `viewmodel/` — FieldMindViewModel (central state management)
- `theme/` — FieldMind theme (colors, typography)
- `utils/` — App lifecycle manager

### Shared Layer (`shared/`)
- `data/model/AppSettings.kt` — Shared settings data model
- `presentation/theme/` — Base theme system (Color, Theme, Type, Shape, Dimensions, Festive overlays)
- `presentation/viewmodel/ThemeViewModel.kt` — Theme state management
- `presentation/components/icons/` — Shared icon components

### Infrastructure Layer (`infrastructure/`)
- `worker/` — WorkManager-based background jobs (streak tracking, backups)
- `widget/glance/` — Glance app widgets (FieldMind Dashboard, Quick Capture, Widget Receiver)

### Utility Layer
- `util/` — Crash reporter, ANR watchdog

### Resources
- `app/src/main/res/` — Layouts, drawables, strings, themes, animations, fonts, XML configs
- `app/src/main/assets/` — Static assets (privacy policy, species catalog JSON)
- `app/build.gradle.kts` — Module build configuration

## Local Contracts

### Build System
- Kotlin 2.3.x, AGP 9.2.x, KSP, Compose Compiler plugin
- `gradle/libs.versions.toml` is the **single source of truth** for dependency versions
- Two product flavors: `fdroid` and `github` (identical features, different distribution channels)
- Two build types: `debug` (`.debug` suffix) and `release` (minified, shrunk, R8 optimized)
- APK outputs follow pattern: `FieldMind-{version}-{variant}-{signature}-universal.apk`
- Keystore: `my-release-key.jks` at project root (for local signed builds)

### Architecture
- **UI**: Jetpack Compose with Material3 (including Material Color Utilities for dynamic theming)
- **Database**: Room (KSP annotation processing, schema exports committed for migration history)
- **Navigation**: Compose Navigation
- **Async**: Kotlin Coroutines + StateFlow
- **Image loading**: Coil (Compose integration)
- **Networking**: Retrofit + OkHttp (for weather providers, species APIs)
- **Background work**: WorkManager (reminders, backups, streak tracking)
- **Maps**: Maplibre (via `org.maplibre.gl:android-sdk`) for offline-capable maps
- **Location**: Google Play Services Location API + fused location provider
- **DI**: No DI framework; manual constructor injection throughout
- **State**: ViewModel + MutableStateFlow; sealed classes for UI state
- **Learning**: SM-2 spaced repetition algorithm for flashcards
- **Weather**: Configurable provider backend (7 providers with different API key requirements)

### Code Conventions
- Package structure: `fieldmind.research.app.{layer}.{feature}`
- Compose components use `@Composable` with explicit parameter naming
- ViewModels expose state via `StateFlow` collected as Compose state
- Room entities annotated with `@Entity`, DAOs with `@Dao`
- Weather providers implement a common `WeatherProvider` interface
- Species classifiers implement `SpeciesClassifier` interface
- Feature data packages follow: `data/{domain}/` with `entity/`, `dao/`, repository sub-packages

### Key Dependencies
- `androidx.media3` — ExoPlayer-based media playback (audio recordings, field notes)
- `androidx.glance` — App widgets (Material3 glance with responsive layouts)
- `org.maplibre.gl:android-sdk` — Offline OpenStreetMap maps
- `androidx.camera` — In-app camera capture for observations
- `androidx.biometric` — Privacy/biometric lock
- `com.google.android.gms:play-services-location` — Geo-fencing and location services
- `com.google.ai.client.generativeai` — Gemini API for research assistant
- `io.coil-kt:coil-compose` — Async image loading
- `com.squareup.retrofit2` — HTTP API clients
- `com.google.mlkit:object-detection` — On-device ML for species recognition

## Work Guidance

- When adding new dependencies, update `gradle/libs.versions.toml` first, then reference via `libs.` in build files
- Compose compiler is managed by `kotlin.plugin.compose` — no manual version pinning needed
- Room schema exports (`app/schemas/`) should be committed for migration history
- Widget layouts in `res/xml/` must match glance composable specs
- Weather providers: add new provider class in `data/weather/`, implement `WeatherProvider` interface, register in FieldMindRepository
- Species: update `assets/species/species_catalog.json` when adding new species
- All screens should be registered in `FieldMindScreens.kt` for navigation
- Background workers extend `CoroutineWorker` and are scheduled via `FieldMindBackgroundScheduler`
- Privacy-sensitive UI (lock screen, clipboard) uses `ScreenSecurityUtils` and `ClipboardSecurityUtils`

## Verification

- Run `./gradlew lint` for static analysis
- Run `./gradlew assembleGithubDebug` for a compile check (fastest verification)
- Run `./gradlew assembleGithubRelease` for release build verification (includes R8 minification)
- APK outputs verified by CI (`app/build/outputs/apk/`)
- No unit test suite defined yet; manual testing on device/emulator

## Child DOX Index

### App-Level
- `src/main/res/AGENTS.md` — Android resources: layouts, drawables, strings, themes, animations, fonts, XML configs
- `src/main/assets/` — Static assets (privacy policy, species catalog JSON) — managed via `app/build.gradle.kts`

### Java/Kotlin Code
- `src/main/java/fieldmind/research/app/features/field/AGENTS.md` — Field feature module (core product)
  - `features/field/data/AGENTS.md` — Data layer: weather, vision, AI, database, repository, all domain packages
  - `features/field/presentation/AGENTS.md` — Presentation layer: screens, components, navigation, viewmodel, theme
- `src/main/java/fieldmind/research/app/shared/AGENTS.md` — Shared code: base theme, common components, settings model
- `src/main/java/fieldmind/research/app/infrastructure/AGENTS.md` — Infrastructure: workers, widgets
- `src/main/java/fieldmind/research/app/activities/` — Android activities (covered by `app/AGENTS.md`)
- `src/main/java/fieldmind/research/app/util/` — Utility functions (coverage through parent AGENTS.md)
