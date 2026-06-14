## Phase 3-12 Complete Redesign: Full Feature Implementation

This PR contains the complete Phase 3-12 redesign with 5 groups of features spanning the home screen, detail screen, projects dashboard, settings, field mode, and more.

---

### Group 1: Projects & Dashboard

- **5-tab Project workspace** — Overview, Observations, Hypotheses, Data Tools, Reports tabs with deep-link support (`startTab` param)
- **ProjectDashboardCard** — Sampling-effort metrics: total observations, obs this week/month, unique sites, open questions, supported/refuted/untested hypotheses, total field hours, total sessions
- **ProjectDashboardCardCompact** — Per-project compact cards with linked entity counts (obs, questions, sources, data, reports)
- **Tab-level composables** — `ObservationsTab`, `HypothesesTab`, `DataTab`, `ReportsTab` with entity lists and filtering
- **Nav routes** — Projects, Hypotheses, Data Tools, Analysis, Reports all wired to `ProjectsScreen` with appropriate `startTab`

### Group 2: Detail Screen Revamp

- **ObservationHeroCarousel** — Swipeable `HorizontalPager` for observation media (photos/gallery attachments) with page indicator dots and counter badge
- **Compact weather chips** — `WeatherChip` composable for temp, condition, humidity, wind, cloud cover, pressure in a compact row
- **Collapsible provenance section** — Clickable card expanding to show date/time, session start/end, duration, GPS coordinates, place, weather snapshot timestamp, record IDs, created/updated timestamps
- **Evidence Hub improvements** — `EvidenceFilterState` with type/category/date/confidence/tags/location filters, `CompletenessIndicator` for observation metadata completeness, `BulkSelectionToolbar` for batch operations, `MissingEvidenceFilter` chips, `ViewModeToggle` for list/grid, `EvidenceGridCard` for grid view
- **Entity-specific detail layouts** — Note, Question, Hypothesis, Project, Source, Data Record, Report, Flashcard detail content with rich layouts, badges, stats bars, and backlinks panels

### Group 3: Home Screen Weather Centerpiece

- **Animated weather centerpiece** — Live weather data with animated temperature, condition icon, humidity, wind, pressure, cloud cover
- **Moon phase calculation** — `getMoonPhase()` function computing lunar phase (new → waxing crescent → ... → waning crescent)
- **Conditions nudge banner** — `computeFieldworkNudge()` analyzing temp/wind/precipitation thresholds for actionable fieldwork advice
- **Yesterday vs today delta** — `DailyGoalCard` shows observation count comparison with delta label
- **Sunrise/sunset display** — Added `sunrise`, `sunset` fields to `WeatherSnapshot` with Open-Meteo daily API data

### Group 4: Settings Improvements

- **11 new settings keys** — `distanceUnit`, `windSpeedUnit`, `timeFormat`, `dateFormat`, `mapType`, `mapShowLocation`, `fieldModeDefaultSession`, `fieldModeAutoStartTimer`, `fieldModeObservationSpacing`, `developerMode`, `debugLogging`, `dataIntegrityCheckOnLaunch`
- **UnitsFormatSettingsPage** — Choice chips for temperature, distance, wind speed, time, date formats
- **MapSettingsPage** — Map type selector (Standard/Satellite/Terrain) + show location toggle
- **DataIntegritySettingsPage** — Launch check toggle, live database summary stats, orphaned record detection, "Run integrity check" button
- **DeveloperSettingsPage** — Developer mode + debug logging toggles, conditional dev tools section, version info card
- **4 new nav routes** — Fully wired in `FieldMindNavigation.kt` with composable entries

### Group 5: Templated Protocols, Re-observation Linking, Numpad Fields

- **Protocol system** — `FieldProtocol`, `ProtocolStep`, `ProtocolInputType` data classes with 5 built-in protocols:
  - Point Count (bird/auditory survey), Transect Survey (line transect sampling), Water Quality (field chemistry), Phenology Check (lifecycle events), Soil Pit (soil profile)
- **ProtocolStepField composable** — Renders appropriate input per step type (Text, Number, Decimal, Choice, Photo, Location)
- **ProtocolPicker dialog** — Dialog for selecting from available protocols, auto-sets category and tags
- **Re-observation linking** — `ReObservationLink` composable in detail screen showing parent observation (this is a re-observation of) and child observations (follow-ups of this one)
- **Numpad keyboard** — Replaced `FieldTextField` with `NumberField` for count, distance, height, width, length, diameter, weight measurements
- **Field-mode defaults wiring** — Auto-start timer on field mode open, session type in header, observation spacing cooldown (30s/1m/5m) with snackbar notifications

---

## Files Changed

**Data Layer:**
- `WeatherApiService.kt` — Sunrise/sunset + daily Open-Meteo API
- `FieldMindSettings.kt` — 11 new settings with StateFlow/setter/KEY patterns
- `FieldEntities.kt` — `parentObservationId` for re-observation linking
- `FieldMindDatabase.kt` — Migration for new columns

**Components:**
- `FieldMindComponents.kt` — Protocol system, `NumberField` numpad, `ProtocolStepField`, `ProtocolPicker`
- `EvidenceHubPhase6.kt` — `EvidenceFilterState`, `CompletenessIndicator`, `BulkSelectionToolbar`, `MissingEvidenceFilter`, `ViewModeToggle`, `EvidenceGridCard`
- `FieldMindIcons.kt` — New icon additions
- `ObservationQualityComponents.kt` — Quality/confidence components

**Screens:**
- `FieldMindProjectsScreen.kt` — 5-tab dashboard with `ProjectDashboardCard`, `EvidenceTab`
- `FieldMindDetailScreen.kt` — `ObservationHeroCarousel`, compact weather chips, collapsible provenance, entity-specific layouts, `ReObservationLink`
- `FieldMindHomeScreen.kt` — Weather centerpiece, moon phase, conditions nudge, yesterday/today delta, sunrise/sunset
- `FieldMindSettingsScreen.kt` — 4 new settings pages with 11 nav entries in SettingsHub
- `FieldMindObserveScreen.kt` — Protocol integration, numpad fields, field-mode defaults (auto-timer, spacing cooldown)
- `FieldMindResearchSession.kt` — Session linking improvements
- `WeatherDatabaseScreen.kt` — Weather data display

**Navigation:**
- `FieldMindNavigation.kt` — 4 new settings routes, project tab support

## Type

Feature + Refactor + UI Polish

## Impact

High — significant UX improvements across home, detail, projects, settings, and field mode
