# Field Feature Data Layer — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` ← `features/field/AGENTS.md` ← `features/field/data/AGENTS.md` (this file)

Read parent docs first, then this file for data-layer-specific contracts.

## Purpose

The `data/` package contains all domain logic, data access, external API integrations, persistence, and business rules for the FieldMind research tool. It is the single source of truth for all research data.

## Ownership

- `database/` — Room database: entities (`FieldEntities.kt`), DAOs (`FieldMindDao.kt`), database class (`FieldMindDatabase.kt`), migrations
- `repository/` — `FieldMindRepository` — central data access facade
- `weather/` — Weather provider integrations: `OpenMeteoProvider`, `OpenWeatherMapProvider`, `MetNorwayProvider`, `WeatherApiDotComProvider`, `IndiaMeteorologicalDepartmentProvider`, `NationalWeatherServiceProvider`, `WeatherUnitConverter`
- `vision/` — Species classification: `SpeciesClassifier`, `SpeciesImageAnalyzer`, `PhashDatabase`, `PerenualSpeciesProvider`, `SpeciesDatabase`
- `ai/` — AI research assistant: `GeminiResearchAssistant`
- `species/` — Taxonomy data and species catalogs
- `security/` — Privacy: `FieldMindPrivacyManager` (biometric lock, screen protection, clipboard security)
- `export/` — Export/import: `FieldMindReportGenerator`, `FieldMindExport`, `FieldMindExportEncryption`, `FieldMindExportMediaPacker`, `FieldTemplate`
- `location/` — Location services: `FieldLocationProvider`, `TrackRecorder`, `MapDrawingTools`, `MaplibreOfflineManager`, `GeoFenceReminder`
- `settings/` — `FieldMindSettings` persistence
- `stats/` — `FieldMindStreaks` tracking
- `flashcard/` — SM-2 spaced repetition engine (`SM2Engine`) + smart flashcard generator
- `question/` — Research question generator (`QuestionGenerator`)
- `timer/` — Field timer utilities (`FieldTimer`)
- `learn/` — Learn library and curriculum (`LearnLibrary`)
- `analysis/` — Pattern detection engine (`PatternDetectionEngine`)
- `attachment/` — File attachment manager (`FieldAttachmentManager`)
- `background/` — Background workers: `FieldMindReminderWorker`, `FieldMindBackgroundScheduler`, `FieldMindAutoBackupWorker`
- `bulk/` — Bulk operations (`FieldBulkOperations`)
- `undo/` — Undo/redo: `FieldUndoHelper`, `FieldUndoManager`

## Local Contracts

### Interface Contracts
- **Weather providers**: Implement `WeatherProvider` interface. Register via `WeatherProviders` object. Returns `WeatherData` sealed class.
- **Species classifiers**: Implement `SpeciesClassifier` interface. Returns ranked list of species matches with confidence scores.
- **Repository pattern**: `FieldMindRepository` wraps all DAOs and external API calls. ViewModels only access data through the repository.

### Database
- Room with KSP annotation processing
- Schema exports committed for migration history
- Entities in `database/entity/`, DAOs in `database/dao/`
- Use explicit migrations, not destructive fallback

### External APIs
- Weather: Retrofit + OkHttp (configurable provider backend)
- Species: Perenual API, iNaturalist fallback
- AI: Google Gemini API
- Maps: Maplibre GL (offline tiles via OSMy variant)

## Work Guidance

- Add new data domains as a package under `data/{domain}/`
- Implement the appropriate interface if one exists
- Register new providers/services in `FieldMindRepository`
- Database changes require: new entity + DAO + database version bump + migration
- Weather providers added in `weather/` package and registered in `WeatherProviders` object
- All async operations use Kotlin Coroutines

## Verification

- Compile check via parent module
- Room schema generation validates entity/DAO correctness
- No unit test suite yet; manual verification on device

## Child DOX Index

No child AGENTS.md files defined yet. Each data subdomain is small enough to be covered by this file.
