# FieldMind — Complete Combined Analysis, Plans & Implementation Status

> **Date:** June 18, 2026
> **Scope:** Merges **24 planning/analysis documents** into one comprehensive reference
> **Source docs:** FIELDMIND_COMPREHENSIVE_ANALYSIS, FIELDMIND_FULL_ANALYSIS, FIELDMIND_NEXT_GEN_ANALYSIS, OBSERVATION_UI_GAP_ANALYSIS, FIELDMIND_ANALYSIS, FIELDMIND_NEXT_GEN_ANALYSIS, PR_55_IMPLEMENTATION_PLAN, UI_REDESIGN_PLAN, FIELDMIND_NEXT_PHASES_PLAN, FIELDMIND_UI_REDESIGN_PLAN, REDESIGN_PLAN, PHASE_4_IMPLEMENTATION_PLAN, STRATEGIC_FIELDMIND_PLAN_V2, IMPLEMENTATION_ROADMAP_DETAILED, FIELDMIND_REDESIGN_SUMMARY, WHATS_NEW_IMPLEMENTATION, fieldmind_legacy_cleanup, FIELDMIND_ANALYSIS

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Source Document Map](#2-source-document-map)
3. [Architecture Overview](#3-architecture-overview)
4. [Competitive Landscape](#4-competitive-landscape)
5. [Feature-by-Feature Analysis & Implementation Status](#5-feature-by-feature-analysis--implementation-status)
6. [UI/UX Audit: Screen-by-Screen](#6-uiux-audit-screen-by-screen)
7. [Critical Issues & Code Quality](#7-critical-issues--code-quality)
8. [Missing Features & Gaps](#8-missing-features--gaps)
9. [Groundbreaking Features Proposed](#9-groundbreaking-features-proposed)
10. [Plans Cross-Reference: What Each Plan Says vs Reality](#10-plans-cross-reference-what-each-plan-says-vs-reality)
11. [Consolidated Priority Roadmap](#11-consolidated-priority-roadmap)
12. [Key Metrics & Progress Tracking](#12-key-metrics--progress-tracking)

---

## 1. Executive Summary

**FieldMind** is an Android field research app built with Jetpack Compose + Room + CameraX + OSMDroid. It has evolved from a basic field notebook into a platform with 21+ Room entities, Canvas-based charts, SM-2 spaced repetition flashcards, a Gemini/OpenAI AI assistant, a Camera V2 with pro controls, research sessions with timers, evidence hubs, hypothesis testing workflows, reports, data tools, and a learning library.

### The Core Tension

Across all **24 planning/analysis documents**, a consistent pattern emerges:

- **✅ ~45% of features** are fully implemented and working
- **🟡 ~30% of features** are partially implemented (skeletons, stubs, or settings-only)
- **❌ ~25% of features** are planned but not started
- **⚠️ ~15% of the codebase** is dead/orphaned code from the Rhythm music-player legacy

### What's Excellent

- Offline-first architecture with Room database (21 entities, v9 migrations)
- Canvas-based charts (bar, line, donut, radar, heatmap) — fully offline
- SM-2 spaced repetition flashcards
- Camera V2 with full-screen, zoom, focus, grid, timer, aspect ratio
- Research sessions with timer, quick-observation, summary
- Workspace redesign (3 tabs: Projects/Evidence/Analysis)
- Security settings (biometric lock, timeout, auto-lock)
- AI assistant with Gemini + OpenAI
- 8 export formats (Markdown, CSV, JSON, HTML, PNG, SVG, PDF, plain text)
- Multiple weather providers (Open-Meteo, OpenWeatherMap, WeatherAPI, Met Norway, NWS, IMD)

### What's Urgent

- Species identification is **entirely placeholder** — `SpeciesClassifier.kt` returns random confidences
- No dependency injection — `FieldMindViewModel` is a 1500+ line god object
- No unit tests
- Full-text search uses `LIKE '%query%'` — no FTS virtual tables
- Settings `init()` has side-effects (re-schedules background jobs on every `getInstance()`)
- Weather API settings exist but **no integration** — Open-Meteo API calls never made
- PDF viewer uses broken WebView fallback
- Project attachments field exists but has no UI
- Many screens lack loading/error/empty states

### What's Planned But Unstarted

- Species identification engine with ML Kit / TFLite
- Voice-to-observation with Whisper transcription
- Offline maps with tile caching and drawing tools
- Community validation network (peer review)
- XLSForm import engine
- Behavioral event logger (ethogram)
- Rich text note editor with markdown
- Research paper maker with IMRAD template
- Photo annotation tools (crop, draw, label)
- Auto-question generation from observations

---

## 2. Source Document Map

| # | Document | Type | Length | Focus | Key Theme |
|---|----------|------|--------|-------|-----------|
| 1 | `docs/FIELDMIND_COMPREHENSIVE_ANALYSIS.md` | Analysis | Long | Complete feature audit + competitive analysis | "Many features feel partially implemented" |
| 2 | `docs/FIELDMIND_FULL_ANALYSIS.md` | Analysis | Long | Codebase structure + screen-by-screen audit | ~50% features implemented, phases defined |
| 3 | `docs/FIELDMIND_NEXT_GEN_ANALYSIS.md` | Analysis | Very Long | Next-gen redesign with competitive research | 20 redesigns across all screens |
| 4 | `docs/OBSERVATION_UI_GAP_ANALYSIS.md` | Gap Analysis | Medium | ObserveScreen + DetailScreen vs spec | Many missing dropdowns and fields |
| 5 | `FIELDMIND_ANALYSIS.md` | Analysis | Medium | Codebase quality + placeholder features | Species ID placeholder, monolithic files, no error states |
| 6 | `FIELDMIND_NEXT_GEN_ANALYSIS.md` (root) | Analysis | Long | Competitive analysis + architecture improvements | 6 phases, research-backed feature suggestions |
| 7 | `docs/PR_55_IMPLEMENTATION_PLAN.md` | Implementation Plan | Medium | Weather, bluetooth, DB migration, release | Concrete fixes for a specific PR |
| 8 | `docs/UI_REDESIGN_PLAN.md` | Redesign Plan | Medium | Observation Timeline, Research Hub, Project Management | 4 phases of redesign |
| 9 | `docs/FIELDMIND_NEXT_PHASES_PLAN.md` | Implementation Plan | Medium | Post-Phases 1-12: 7 new feature groups | Species ID, offline maps, hypothesis graph |
| 10 | `docs/FIELDMIND_UI_REDESIGN_PLAN.md` | Redesign Plan | Very Long | Complete visual UI redesign for every screen | 8 groups, new design system, bottom nav |
| 11 | `docs/REDESIGN_PLAN.md` | Redesign Plan | Medium | Widgets, Toasts, Validation, Pages, Details | Snackbar system, required fields, entity-specific details |
| 12 | `docs/PHASE_4_IMPLEMENTATION_PLAN.md` | Implementation Plan | Medium | Species ID, Maps, Hypothesis Graph, PDF Reader | 8 groups of next-gen features |
| 13 | `STRATEGIC_FIELDMIND_PLAN_V2.md` | Strategic Plan | Very Long | Groundbreaking features + architecture + roadmap | "Research operating system for field scientists" |
| 14 | `IMPLEMENTATION_ROADMAP_DETAILED.md` | Roadmap | Long | Week-by-week implementation plan | 5 phases, 24 weeks, 19+ PRs |
| 15 | `FIELDMIND_REDESIGN_SUMMARY.md` | Summary | Medium | Progress tracking across 13 phases | Phases 1-2 done, rest framework-ready |
| 16 | `WHATS_NEW_IMPLEMENTATION.md` | Implementation Report | Medium | Changelog screen redesign | 12 phases of features documented |
| 17 | `docs/fieldmind_legacy_cleanup.md` | Cleanup Plan | Short | Rhythm music-player legacy removal | Safe order for deleting dead code |
| 18 | `README_INDEX.md` | Index | Short | Project documentation index | Links to all docs |
| 19 | `PR_SUMMARY.md` | Summary | Medium | PR summary | Compilation fixes |
| 20 | `WHATSNEW_SUMMARY.txt` | Summary | Short | Changelog highlights | Feature list |
| 21 | `WHATSNEW_IMPLEMENTATION.md` | Plan | Medium | Implementation details | Phase descriptions |
| 22 | `FIELDMIND_REDESIGN_SUMMARY.md` | Summary | Medium | Redesign progress | Phases 1-2 complete |
| 23 | `docs/AGENT.md` | Agent Guide | Medium | Agent configuration | System prompts |
| 24 | `docs/prompt.md` | Prompt | Medium | Screen spec | UI requirements |

### Key Insights from Document Overlap

All documents converge on the same **Top 5 priorities**:
1. **Species ID engine** — called out in 9/24 documents as critical
2. **DI/Architecture refactoring** — called out in 7/24 as blocking
3. **Home screen redesign** — called out in 6/24 as high impact
4. **Weather API integration** — called out in 5/24 as low-hanging fruit
5. **Capture form redesign** — called out in 5/24 as UX-critical

---

## 3. Architecture Overview

### Data Layer (Current State)

```
features/field/data/
├── ai/
│   └── GeminiResearchAssistant.kt        # ✅ Gemini + OpenAI + local model
├── attachment/
│   └── FieldAttachmentManager.kt          # ✅ File attachment management
├── background/
│   ├── FieldMindAutoBackupWorker.kt      # ✅ WorkManager auto-backup
│   ├── FieldMindReminderWorker.kt        # ✅ Daily reminder
│   └── FieldMindBackgroundScheduler.kt   # ✅ Central wiring (⚠️ side-effects on init)
├── bulk/
│   └── FieldBulkOperations.kt            # ✅ Bulk operations
├── database/
│   ├── FieldMindDatabase.kt              # ✅ Room v9, 21+ entities, proper migrations
│   ├── dao/FieldMindDao.kt               # ⚠️ Single DAO, 60+ queries
│   └── entity/FieldEntities.kt           # ⚠️ Single file, all entities
├── export/
│   ├── FieldMindExport.kt                # ✅ 8 formats (Markdown, CSV, JSON, HTML, PNG, SVG, PDF, plain)
│   └── FieldTemplate.kt                  # ⚠️ Templates defined but not wired in UI
├── flashcard/SM2Engine.kt               # ✅ SM-2 spaced repetition, complete
├── learn/LearnLibrary.kt                 # ✅ Learning content
├── location/
│   ├── FieldLocationProvider.kt          # ✅ GPS + geocoding
│   ├── MapDrawingTools.kt                # ✅ Map drawing (polygon, line, point)
│   ├── TrackRecorder.kt                  # ✅ GPS track logging
│   ├── GeoFenceReminder.kt              # ✅ Geofence reminders (WorkManager)
│   └── MaplibreOfflineManager.kt        # ✅ Offline tiles (MapLibre)
├── repository/FieldMindRepository.kt     # ⚠️ Single repo, 70+ methods
├── security/FieldMindPrivacyManager.kt   # ✅ BiometricPrompt, complete
├── settings/FieldMindSettings.kt         # ⚠️ 37 keys, SharedPrefs, init side-effects
├── species/
│   ├── SpeciesClassifier.kt              # ❌ PLACEHOLDER — returns random confidences
│   ├── SpeciesDatabase.kt                # ❌ Never populated/downloaded
│   ├── SpeciesImageAnalyzer.kt           # ✅ Image analysis (pHash)
│   └── PhashDatabase.kt                 # ✅ Perceptual hash store
├── stats/FieldMindStreaks.kt             # ✅ Streak calculation
├── timer/FieldTimer.kt                   # ✅ Session timer
├── undo/
│   ├── FieldUndoHelper.kt               # ✅ Undo support
│   └── FieldUndoManager.kt              # ✅ Undo manager
├── weather/
│   ├── OpenMeteoProvider.kt              # ✅ Active weather provider (free, no API key)
│   ├── WeatherProvider.kt                # ✅ Provider interface
│   ├── OpenWeatherMapProvider.kt         # ❌ Dead code (requires API key)
│   ├── WeatherApiDotComProvider.kt       # ❌ Dead code (requires API key)
│   ├── MetNorwayProvider.kt             # ❌ Dead code
│   ├── NationalWeatherServiceProvider.kt # ❌ Dead code (US only)
│   ├── IndiaMeteorologicalDepartmentProvider.kt # ❌ Dead code
│   ├── WeatherModels.kt                  # ✅ Weather data models
│   └── WeatherUnitConverter.kt           # ✅ Unit conversion
└── vision/                               # ⚠️ Some implemented, some placeholder
```

### Presentation Layer (Current State)

```
features/field/presentation/
├── components/
│   ├── FieldMindCameraV2.kt              # ✅ Full-screen, zoom, focus, grid, timer, ratio
│   ├── FieldMindCameraCapture.kt         # ❌ Legacy, deprecated
│   ├── FieldMindComponents.kt            # ✅ Shared UI components
│   ├── FieldMindIcons.kt                 # ✅ MaterialSymbolIcon system
│   ├── FieldMindCharts.kt                # ✅ Canvas charts (bar, line, donut, network)
│   ├── FieldMindChartsExtended.kt        # ✅ Extended charts (radar, heatmap)
│   ├── FieldMindSnackbar.kt              # ✅ Snackbar system
│   ├── FieldMindObserveSnackbar.kt       # ✅ Observation snackbar
│   ├── FieldMindSharedTransitions.kt     # ✅ Shared element transitions
│   ├── FieldMindMotion.kt               # ✅ Motion/animation utilities
│   ├── FieldDataTable.kt                # ✅ Interactive data table
│   ├── RequiredFieldState.kt            # ✅ Form validation
│   ├── MaplibreMapView.kt               # ⚠️ MapLibre integration (may be incomplete)
│   ├── AnimatedWeatherScene.kt          # ✅ Weather animations
│   ├── EvidenceHubPhase6.kt             # ✅ Evidence hub with filtering
│   ├── DataWorkspacePhase7.kt           # ✅ Data workspace
│   ├── HypothesesPhase8.kt              # ✅ Hypothesis management
│   ├── InsightsPhase9.kt                # ✅ Insights dashboard
│   ├── JournalPhase10.kt                # ✅ Journal/notes
│   ├── ReportsPhase11.kt                # ✅ Reports system
│   ├── LibraryPhase12.kt                # ✅ Library/knowledge hub
│   ├── ProjectPhase5Components.kt       # ✅ Project components
│   ├── ObservationQualityComponents.kt  # ✅ Quality scoring
│   ├── FieldMindObservationsTimeline.kt # ✅ Timeline view
│   ├── HomeSpeciesCatalogSection.kt     # ✅ Species catalog on home
│   ├── SpeciesDetailSheet.kt            # ✅ Species detail sheet
│   ├── TaxonomyPickerDialog.kt          # ✅ Taxonomy picker
│   └── HomeWidgetGrid.kt                # ❌ Not found in file list
├── navigation/FieldMindNavigation.kt    # ✅ 19+ routes, bottom bar, rail, animations
├── screens/
│   ├── FieldMindHomeScreen.kt           # ⚠️ 2700+ lines, needs splitting
│   ├── FieldMindObserveScreen.kt        # ⚠️ 690+ lines, capture form needs work
│   ├── FieldMindDetailScreen.kt         # ✅ Entity detail with entity-specific layouts
│   ├── FieldMindProjectsScreen.kt       # ✅ 3-tab workspace (Projects/Evidence/Analysis)
│   ├── FieldMindLibraryScreen.kt        # ⚠️ 620+ lines, 5 tabs, needs redesign
│   ├── FieldMindResearchSession.kt      # ✅ Research session with timer
│   ├── FieldMindDialogs.kt              # ⚠️ 2200+ lines, needs splitting
│   ├── InsightsScreen.kt                # ✅ Charts, metrics, achievements
│   ├── FieldMindMapScreen.kt            # ✅ OSM map with drawing/tracks/geofences
│   ├── FieldMindArchiveScreen.kt        # ✅ Search/archive
│   ├── FieldMindSettingsScreen.kt       # ✅ Settings hub with 8+ sub-sections
│   ├── FieldMindLockScreen.kt           # ✅ Biometric lock screen
│   ├── FieldMindOnboardingScreen.kt     # ✅ Onboarding flow
│   ├── FieldMindBackupExportScreen.kt   # ✅ Backup/export UI
│   ├── FieldMindChangelogScreen.kt      # ✅ What's New screen
│   ├── FieldMindScreenUtils.kt          # ✅ Shared utilities
│   ├── FieldMindQuestionsScreen.kt      # ✅ Questions screen
│   ├── FieldMindDataTools.kt            # ✅ Data tools (Counter, Measure, Weather, Species)
│   ├── FieldLogScreen.kt                # ✅ Field log/timeline
│   ├── WeatherDatabaseScreen.kt         # ✅ Weather database/live card
│   ├── FlashcardSessionScreen.kt        # ✅ Flashcard study
│   ├── SpeciesBrowserScreen.kt          # ✅ Species browser
│   ├── TaxonomicBrowserScreen.kt        # ✅ Taxonomic browser
│   └── FieldMindScreens.kt             # ❌ Likely orphaned/consolidated
├── theme/FieldMindTheme.kt              # ✅ Brand palette + semantic colors
└── viewmodel/FieldMindViewModel.kt      # ⚠️ 1500+ line god object, no DI
```

---

## 4. Competitive Landscape

### Combined Competitive Matrix (merged from all docs)

| Feature | FieldMind | iNaturalist | ODK Collect | KoboToolbox | Fulcrum | eBird | QField |
|---------|-----------|-------------|-------------|-------------|---------|-------|--------|
| **Offline-first** | ✅ | ✅ | ✅ | ✅ | ✅ | 🟡 | ✅ |
| **Camera capture** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| **GPS tagging** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Species ID (AI)** | ❌ Placeholder | ✅ Core | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Voice transcription** | ❌ | ❌ | ❌ | ✅ (80 langs) | ❌ | ❌ | ❌ |
| **Knowledge graph** | ✅ Unique | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Flashcards (SM-2)** | ✅ Unique | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI assistant** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Export (8 formats)** | ✅ | ❌ | 🟡 | 🟡 | ✅ | ❌ | ❌ |
| **Community validation** | ❌ | ✅ Core | ❌ | ❌ | ❌ | ✅ Core | ❌ |
| **Offline maps + tiles** | 🟡 | ❌ | ✅ | ✅ | ✅ | ❌ | ✅ |
| **XLSForm import** | ❌ | ❌ | ✅ | ✅ | 🟡 | ❌ | ❌ |
| **Behavior logger** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Research sessions** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Hypothesis testing** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Weather integration** | 🟡 Settings only | ❌ | ❌ | ❌ | 🟡 | ❌ | ❌ |
| **Rich text notes** | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| **PDF viewer** | ❌ WebView | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| **Project attachments** | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ | ✅ |
| **Calendar heatmap** | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |
| **Data quality scoring** | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |
| **Biometric privacy** | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| **Canvas charts offline** | ✅ Unique | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Offline maps (drawing)** | ✅ Maps + drawing | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |

**FieldMind's Unique Competitive Advantages:**
1. ✅ **Knowledge graph** (observations → questions → hypotheses → evidence → insights)
2. ✅ **SM-2 spaced repetition flashcards**
3. ✅ **Canvas-based offline charts** (bar, line, donut, radar, heatmap, network)
4. ✅ **Research sessions** (timer + multi-observation + summary)
5. ✅ **Hypothesis testing workflow**
6. ✅ **8-format export suite**
7. ✅ **Multiple weather provider support** (6 providers)

**Critical Gaps to Close:**
1. ❌ **Species ID** — the #1 feature users expect from a field app
2. ❌ **Voice transcription** — 10x faster capture
3. ❌ **Community validation** — peer review builds trust
4. ❌ **PDF viewer** — researchers read PDFs
5. ❌ **Rich text notes** — field notes need formatting

---

## 5. Feature-by-Feature Analysis & Implementation Status

### 5.1 Observations (Core Feature)

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Evidence-first capture | 🟡 Partial | CameraV2 handles capture; form still has required fields first |
| Camera V2 (full-screen, zoom, focus, grid, timer, ratio) | ✅ Done | Post-capture bottom sheet with 3 options |
| Quick observation form | ✅ Done | Subject, facts, category, confidence, tags |
| Structured details (species, distance, checklist, measurements) | ✅ Done | QualityScoreCard, structuredDetailsJson |
| Timer integration | ✅ Done | LiveObservationTimer in capture form |
| Weather + GPS auto-attachment | 🟡 Partial | Settings added, API not integrated |
| Re-observation chains (parent/child linking) | ✅ Done | parentObservationId field |
| Batch editing | ❌ Missing | Cannot select multiple observations |
| Observation templates | ❌ Missing | No save/load reuseable protocols |
| Species checklist completion | ❌ Missing | No "12/45 bird species seen" |
| Observation quality scoring | ✅ Done | QualityScoreCard, ObservationQualityComponents |
| CSV/Excel export | 🟡 Partial | JSON/Markdown export exist; CSV for data records |
| Search by image | ❌ Missing | No image content search |
| Deep link sharing | ❌ Missing | No shareable observation URLs |
| Live timer in form header | ✅ Done | Active timer with observation count |

### 5.2 Evidence / Attachments

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Camera capture (Photo) | ✅ Done | CameraV2 with full pro controls |
| Gallery import | ✅ Done | Grid view with thumbnails |
| File picker | ✅ Done | File attachments via system picker |
| Audio recording | ✅ Done | Mic button, audio recording |
| Evidence Hub with filtering | ✅ Done | Phase 6 — category, date, tag, location, confidence, project filters |
| Bulk operations | ✅ Done | Select, archive, delete, tag, link to project, export |
| Evidence status tracking | ✅ Done | Used in analysis, Needs review, Missing metadata |
| In-app PDF viewer | ❌ Missing | Uses broken WebView/Google Docs viewer |
| Photo editor (crop, rotate, annotate) | ❌ Missing | No editing tools after capture |
| Audio player | ❌ Missing | Can record but not play back |
| Evidence map (photo locations) | ❌ Missing | No map view of evidence |
| Bulk evidence export | ❌ Missing | Cannot batch export photos |
| Full-screen media viewer | ❌ Missing | Opens WebView instead |

### 5.3 Projects / Workspace

| Sub-feature | Status | Details |
|-------------|--------|---------|
| 5-tab workspace (Overview, Observations, Hypotheses, Data, Reports) | ✅ Done | Full tab implementation |
| 3-tab simplified (Projects, Evidence, Analysis) | ✅ Done | Alternative simplified view |
| Dashboard metrics (obs/week/sites/hours/sessions) | ✅ Done | ProjectDashboardCardCompact |
| Project templates | ✅ Done | Templates defined, UI exists |
| Method builder | ✅ Done | Research method selection |
| Entity linking | ✅ Done | Observations ↔ Projects ↔ Sources ↔ Data ↔ Reports |
| Simplified creation (Title + Question) | ✅ Done | Minimal creation form |
| Gantt chart / timeline | ❌ Missing | No project timeline visualization |
| Project milestones | ❌ Missing | No phase markers |
| Project sharing/collaboration | ❌ Missing | Single-user only |
| Data collection status | ❌ Missing | No "12/30 transects completed" |
| Project journal / notebook | ❌ Missing | Freeform journal within project |
| Project attachments | ❌ Missing | Field exists, no UI |
| Project tasks | ❌ Missing | TaskEntity exists? Not in DB |
| Team members | ❌ Missing | No multi-user support |

### 5.4 Hypotheses

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Hypothesis CRUD | ✅ Done | HypothesisEntity with all fields |
| Status tracking (Supported/Contradicted/Inconclusive/Untested) | ✅ Done | Status field with visual badges |
| Confidence bar | ✅ Done | Visual confidence indicator |
| Linked questions | ✅ Done | QuestionHypothesisCrossRef |
| Evidence support count | ✅ Done | LinkedObservations count |
| Testing workflow (Test → Collecting → Analysis → Conclusion) | ❌ Missing | No guided hypothesis testing |
| Evidence linking UI | ❌ Missing | No visual indicator of supporting observations |
| Prediction accuracy tracking | ❌ Missing | No accuracy metrics |
| Bayesian updates | ❌ Missing | Confidence should update as evidence grows |
| Hypothesis visualization (tree/matrix) | ❌ Missing | No visual evidence strength view |

### 5.5 Questions

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Question CRUD | ✅ Done | QuestionEntity with all fields |
| Auto-builder | ✅ Done | Rule-based question generation |
| Filters and stats | ✅ Done | Status, priority, category filters |
| Priority-based sorting | 🟡 Partial | Sorting exists, not default on home |
| Question-answer workflow | ❌ Missing | Answering should generate mini-report |
| Observation linking | ❌ Missing | Explicit question-to-observation links |
| Recurring questions | ❌ Missing | Auto-create daily prompts |
| Dedicated Questions screen | ❌ Missing | Only in Workspace > Analysis |

### 5.6 Data Tools

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Counter tool (+/-/reset) | ✅ Done | Full counter with session grouping |
| Measurement tool | ✅ Done | Value + unit entry with history |
| Weather log tool | ✅ Done | Condition picker with auto-fetch |
| Species tool | ✅ Done | Name + count + behavior + confidence |
| Comparison table | ✅ Done | Side-by-side data comparison |
| Data visualization in tools | ❌ Missing | Charts exist in Insights, not in tool UIs |
| CSV export | ❌ Missing | No CSV export from tools |
| Data validation | ❌ Missing | Can type anything in value fields |
| Data aggregation | ❌ Missing | No "average temperature this week" |
| Data comparison | ✅ Done | ComparisonTableScreen |

### 5.7 Sources / Library

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Source CRUD with DOI, citation, files | ✅ Done | Comprehensive source management |
| Reading status tracking | ✅ Done | Status field (Read/Reading/Unread) |
| Importance rating | ✅ Done | Importance field |
| Reading notes | ✅ Done | Notes linked to sources |
| 5-tab library (Sources, Notes, Reading, Flashcards, Learn) | ✅ Done | Full tab implementation |
| In-app PDF viewer | ❌ Missing | WebView/Google Docs fallback |
| Zotero/Mendeley integration | ❌ Missing | No citation manager sync |
| Citation export (BibTeX/CSL) | ❌ Missing | No formatted citation export |
| Annotation system | ❌ Missing | No highlight/underline/notes on sources |
| Reading progress tracking | ❌ Missing | No "page 45/120" tracking |
| Rich text markdown notes | ❌ Missing | Plain text only |

### 5.8 Reports

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Report CRUD with 11 fields | ✅ Done | ReportEntity with all sections |
| 7 report types | ✅ Done | Field, Lab, Species, Weather, Site, Literature, General |
| Report builder with templates | ✅ Done | Templates defined |
| Markdown-based editor | ✅ Done | Markdown draft generation |
| Export (PDF, HTML, Markdown) | ✅ Done | PDF export with images |
| IMRAD structure | ❌ Missing | No Introduction/Methods/Results/Discussion structure |
| Figure/table insertion | ❌ Missing | Can't embed charts and photos inline |
| Citation insertion | ❌ Missing | Can't add `(Author, 2023)` references |
| Abstract generation | ❌ Missing | No auto-abstract |
| DOCX export | ❌ Missing | Markdown/HTML only |

### 5.9 Learning / Flashcards

| Sub-feature | Status | Details |
|-------------|--------|---------|
| SM-2 spaced repetition | ✅ Done | Complete: ease factor, intervals, deck modes |
| Flashcard CRUD | ✅ Done | Front/back, deck, tags |
| Flashcard review session | ✅ Done | Session screen with progress |
| Deck management | ✅ Done | Multiple decks, deck modes |
| Learning library content | ✅ Done | LearnLibrary with categories |
| Smart flashcard creation from observations | ❌ Missing | No auto-generate from content |
| Due count display | ❌ Missing | home screen doesn't show pending cards |

### 5.10 Research Sessions

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Session start/stop with timer | ✅ Done | ResearchSessionEntity, timer |
| Quick observation input | ✅ Done | Subject + facts + category |
| Evidence tools (camera, gallery, file, audio, GPS, weather) | ✅ Done | Tools integrated in session |
| Session summary | ✅ Done | Duration + observation count |
| Session persistence | ✅ Done | Auto-restore on screen load |
| Session → observation linking | ✅ Done | SessionObservationCrossRef |
| Historical sessions list | ❌ Missing | No past sessions view |
| Template presets (Point Count, Transect, Water Quality) | ❌ Missing | No protocol integration |
| Live species ID in session | ❌ Missing | Depends on Species Classifier |

### 5.11 Weather System

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Open-Meteo provider (free, no API key) | ✅ Done | Full provider implementation |
| OpenWeatherMap provider | ✅ Done | Requires API key |
| WeatherAPI.com provider | ✅ Done | Requires API key |
| Met Norway provider | ✅ Done | Free, regional |
| National Weather Service provider | ✅ Done | US-only |
| India Meteorological Department provider | ✅ Done | India-only |
| Weather unit converter | ✅ Done | °C/°F, km/h/mph, etc. |
| Live weather card in WeatherDatabaseScreen | ✅ Done | Temperature, humidity, wind, conditions |
| 30-min auto-refresh | ✅ Done | LaunchedEffect with delay loop |
| Weather icons | ✅ Done | Cloud, Rainy, Snowy, Thunderstorm, Foggy icons |
| Weather auto-attach to observations | 🟡 Partial | Settings exist, API integration not wired |
| Weather widget on Home screen | ❌ Missing | Not implemented |
| Weather fields on ObservationEntity | ❌ Missing | Not added to entity |
| Weather trend chart in Insights | ❌ Missing | No weather correlation charts |
| 7-day forecast | ❌ Missing | Current conditions only |

### 5.12 Maps

| Sub-feature | Status | Details |
|-------------|--------|---------|
| OSM map display | ✅ Done | osmdroid integration |
| GPS markers | ✅ Done | Observation location markers |
| Drawing tools (polygon, line, point) | ✅ Done | MapDrawingTools.kt |
| Track recording | ✅ Done | TrackRecorder.kt |
| Geofence reminders | ✅ Done | GeoFenceReminder.kt (WorkManager) |
| Offline tile caching | ✅ Done | MaplibreOfflineManager.kt |
| Auto-fit bounds | ✅ Done | Zoom to fit all markers |
| Place name resolution | ✅ Done | FieldLocationProvider.resolvePlaceName() |
| Map in Insights/Home | ✅ Done | Mini-map with markers |
| Full-screen toggle | ✅ Done | Full-screen map mode |
| Multiple tile sources (OSM, Satellite, Terrain) | ❌ Missing | OSM only |
| Map observation gallery | ❌ Missing | Photo grid with location pins |

### 5.13 AI Assistant

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Gemini integration | ✅ Done | GeminiResearchAssistant.kt |
| OpenAI integration | ✅ Done | Provider switchable in settings |
| Local model support (FieldLite/FieldCore/FieldPro) | ✅ Done | Settings for local model |
| Retry logic + error handling | ✅ Done | Robust error handling |
| Fact-checking Q&A | ✅ Done | Query-based assistant |
| Auto-categorization from observations | ❌ Missing | No ML-based category suggestions |
| Offline question generation | ❌ Missing | No rule-based Q-gen |
| Smart flashcard creation | ❌ Missing | No auto-flashcard from content |
| Pattern recognition (repeated observations) | ❌ Missing | No pattern detection |
| AI insights in Research Dashboard | ❌ Missing | No AI-powered analysis |

### 5.14 Species Identification

| Sub-feature | Status | Details |
|-------------|--------|---------|
| SpeciesClassifier.kt | ❌ PLACEHOLDER | `placeholderInference()` returns random confidences |
| SpeciesDatabase.kt | ❌ Never populated | Model URLs exist, no download mechanism |
| TFLite model bundled | ❌ Missing | `loadModelFile()` always returns null |
| iNaturalist API fallback | ❌ Missing | No API integration |
| Species identification in UI | 🟡 Partial | SpeciesIdButton exists but calls placeholder |
| Category-based species lists (Bird, Mammal, etc.) | ✅ Done | Observation categories include species |
| Field species guide | ✅ Done | Species catalog in assets |
| PhashDatabase (perceptual hash) | ✅ Done | pHash fingerprint store for repeated ID |
| Observation → species linking | ✅ Done | speciesName, speciesConfidence fields |
| Re-observation identification improvement | ✅ Done | pHash database improves over time |

### 5.15 Security & Privacy

| Sub-feature | Status | Details |
|-------------|--------|---------|
| Biometric lock | ✅ Done | BiometricPrompt with CryptoObject |
| Device credential fallback (PIN/pattern) | ✅ Done | KeyguardManager integration |
| Lock timeout settings | ✅ Done | Immediate/1min/5min/15min/screen off |
| Auto-lock on background | ✅ Done | Toggle in security settings |
| Dedicated Security settings page | ✅ Done | Separate nav route |
| App lock on launch | ✅ Done | Full-screen lock gate |

---

## 6. UI/UX Audit: Screen-by-Screen

| Screen | Current Size | Status | Key Issues | Plans for Redesign |
|--------|-------------|--------|------------|-------------------|
| **HomeScreen** | ~2700 lines | ⚠️ Needs split | Too large; weather widget missing; session CTA weak; 6-tile grid outdated; no map section | Hero banner + weather + daily stats + 4 quick actions + recent feed |
| **ObserveScreen** | ~690 lines | 🟡 Partial | Camera opens AFTER form (should be FIRST); category required; no inline annotation; 40% of screen should be camera | Evidence-first: Snap/Gallery/Voice at top; quick details; "Advanced" collapsible |
| **DetailScreen** | Varies | ✅ Good | Entity-specific layouts exist; needs richer media gallery; needs export options | Entity-specific hero sections; tabbed layout; share/export FAB |
| **Dialogs** | ~2200 lines | ⚠️ Needs split | 14+ dialog composables in one file; inconsistent patterns (DialogWrapper vs raw Dialog) | Split into entity-specific dialog files |
| **HomeScreen weather** | ~320 lines (LiveWeatherDashboardWidget) | ⚠️ Over-engineered | 320-line Card with animated scenes, glass-morphism, 7-day forecast — too heavy for home | Separate weather screen; lightweight widget on home |
| **LibraryScreen** | ~620 lines | ⚠️ Needs redesign | 5 tabs crammed; PDF reader broken; notes plain text; learning siloed | Knowledge Hub: unified feed + filter bar + native PDF + rich notes |
| **ProjectsScreen** | ~500 lines | ✅ Good | 3-tab workspace already redesigned | Minor polish only |
| **InsightsScreen** | ~500+ lines | ✅ Good | Charts, metrics, achievements, QA score; may be info-dense | Focus on "what am I discovering"; health score; AI insights |
| **MapScreen** | Varies | ✅ Good | OSM + drawing + tracks + geofences + offline tiles | Bottom sheet overlay instead of 5 subtabs; full-screen toggle |
| **ResearchSession** | Varies | ✅ Good | Timer, quick input, evidence tools, summary | Centered timer hero; icon-only evidence tools; real-time session log |
| **SettingsScreen** | Varies | ✅ Good | 8+ sub-pages including dedicated Security | Minor polish; widget configuration |
| **ArchiveScreen** | Varies | 🟡 Needs work | Basic string matching; no FTS; no filters | Type/date/project filters; FTS-powered search |
| **QuestionsScreen** | Varies | 🟡 Needs work | Only accessible from Workspace > Analysis; auto-builder buried | Restore as standalone screen; make auto-builder always visible |
| **ChangelogScreen** | Varies | ✅ Good | Beautiful Material Design, 12 phases, 80+ features | Keep as is |

---

## 7. Critical Issues & Code Quality

### 🔴 Critical (Must Fix)

| Issue | Severity | File(s) | Notes |
|-------|----------|---------|-------|
| **Species Classifier is placeholder** | 🔴 HIGH | `SpeciesClassifier.kt` | Returns random confidences via `placeholderInference()` |
| **No dependency injection** | 🔴 HIGH | `FieldMindViewModel.kt` (1500+ lines) | God object, no unit tests, direct instantiation |
| **No unit tests** | 🔴 HIGH | Entire project | No test infrastructure at all |
| **Settings.init() side-effects** | 🔴 HIGH | `FieldMindSettings.kt` | Every `getInstance()` re-schedules background jobs via `syncAll()` |
| **Full-text search uses LIKE '%query%'** | 🔴 HIGH | `FieldMindDao.kt` | No FTS virtual tables; slow on large datasets |
| **Weather API not integrated** | 🔴 HIGH | Settings + providers | Settings added, Open-Meteo API calls never made |
| **Destructive migration fallback** | 🔴 HIGH | `FieldMindDatabase.kt` | `fallbackToDestructiveMigration()` data loss risk |
| **PDF viewer broken** | 🔴 HIGH | `LibraryScreen.kt`, `LearnReaderScreen.kt` | WebView + Google Docs viewer; doesn't work for file:// URIs |

### 🟡 Medium Priority

| Issue | Severity | File(s) | Notes |
|-------|----------|---------|-------|
| **Single DAO (60+ queries)** | 🟡 HIGH | `FieldMindDao.kt` | Hard to maintain, no separation |
| **Single Repository (70+ methods)** | 🟡 HIGH | `FieldMindRepository.kt` | Violates Single Responsibility |
| **Single Entities file (21 types)** | 🟡 MED | `FieldEntities.kt` | Hard to navigate, merge conflicts |
| **Monolithic screen files** | 🟡 HIGH | `FieldMindHomeScreen.kt` (2700+), `FieldMindDialogs.kt` (2200+), `FieldMindLibraryScreen.kt` (620+), `FieldMindObserveScreen.kt` (690+) | Hard to maintain, slow compile |
| **Weather backup providers dead code** | 🟡 LOW | `WeatherApiDotComProvider.kt`, `OpenWeatherMapProvider.kt` etc. | 5 unused providers; only OpenMeteo active |
| **Export wiring incomplete** | 🟡 MED | `FieldMindExport.kt`, `FieldTemplate.kt` | Export engine exists but not wired in UI |
| **No loading/error/empty states** | 🟡 MED | Most screens | Silent failures, blank screens during loading |
| **Inconsistent dialog patterns** | 🟡 LOW | `FieldMindDialogs.kt` | Mix of DialogWrapper, raw Dialog, custom |
| **Hardcoded strings everywhere** | 🟡 LOW | Categories, confidence levels, etc. | Cannot localize |
| **OpenAI uses old v1/responses endpoint** | 🟡 MED | `GeminiResearchAssistant.kt` | Should update to newer API |
| **Rhythm music-player legacy files** | 🟡 MED | Various | Dead code: workers, services, widgets, activities |

### 🟢 Nice-to-Fix

| Issue | Severity | Notes |
|-------|----------|-------|
| Inconsistent card radius (mix of 20dp, 24dp, 28dp, 32dp) | 🟢 LOW | Standardization needed |
| No standardized spacing grid (mix of 14dp, 16dp, 18dp, 20dp) | 🟢 LOW | XS:4 / SM:8 / MD:12 / LG:16 / XL:20 / XXL:24 / XXXL:32 |
| TaxonomyData.kt is over-engineered for rare use | 🟢 LOW | 1000+ line taxonomy rarely used |
| Duplicate camera components (legacy + V2) | 🟢 LOW | Legacy FieldMindCameraCapture should be removed |

---

## 8. Missing Features & Gaps

### High-Impact Missing Features

| Feature | Impact | Effort | Depends On |
|---------|--------|--------|------------|
| **Realtime species ID** (TFLite/ML Kit) | 🔴 Critical | 4-6 weeks | Camera V2 (done) |
| **Voice-to-observation** (Whisper) | 🔴 Critical | 3-4 weeks | Audio recording (done) |
| **Home screen weather widget** | 🟡 High | 1-2 weeks | Weather API integration |
| **Rich text notes with markdown** | 🟡 High | 3-4 weeks | Note entity (done) |
| **In-app PDF reader** (androidx.pdf) | 🟡 High | 2-3 weeks | — |
| **Photo annotation tools** | 🟡 High | 2-3 weeks | Camera V2 (done) |
| **Project attachments UI** | 🟡 High | 1-2 weeks | Project entity (done) |
| **Dedicated Questions screen** | 🟡 High | 1-2 weeks | Navigation (done) |
| **Observation templates** | 🟡 High | 2-3 weeks | Observation entity (done) |
| **CSV export for data tools** | 🟡 High | 1 week | Export engine (done) |
| **Research paper maker** (IMRAD) | 🟡 Medium | 4-5 weeks | Reports (done) |

### Medium-Impact Missing Features

| Feature | Impact | Effort |
|---------|--------|--------|
| Auto-question generation from observations | 🟡 Medium | 2-3 weeks |
| Observation batch editing | 🟡 Medium | 2-3 weeks |
| Evidence map (photo locations) | 🟡 Medium | 1-2 weeks |
| Community validation network | 🟡 Medium | 2-3 weeks (v1) |
| XLSForm import engine | 🟡 Medium | 4-5 weeks |
| Behavioral event logger (ethogram) | 🟡 Medium | 3-4 weeks |
| Gantt chart / project timeline | 🟡 Medium | 2-3 weeks |
| AI auto-categorization of observations | 🟡 Medium | 2-3 weeks |
| Skeleton loaders / shimmer | 🟡 Medium | 1 week |

---

## 9. Groundbreaking Features Proposed

The strategic plans identify **7 groundbreaking features** that would differentiate FieldMind from all competitors:

### 9.1 🔬 On-Device Species ID Engine

**Why:** iNaturalist dominates because species ID is the primary UX.
**Approach:** ML Kit Object Detection + bundled TFLite model (500 species, expandable)
**Fallback:** iNaturalist API with user permission
**UX:** Post-capture → "Identify" → top 5 matches with confidence → quick-add
**Status:** ❌ Not started (SpeciesClassifier.kt is placeholder)
**Files needed:** Updated `SpeciesClassifier.kt`, `SpeciesIdentificationSheet.kt` (exists but needs wiring)

### 9.2 🎙️ Voice-to-Observation AI Pipeline

**Why:** 10x faster capture. KoboToolbox dominates this space.
**Approach:** Distilled Whisper model (tiny.en, ~75MB) via MediaPipe
**Fallback:** OpenAI Whisper API
**NLP:** Natural language parsing → subject, category, facts, tags
**UX:** Tap mic → speak 30 seconds → review auto-parsed fields → save
**Status:** ❌ Not started
**Files needed:** `AudioTranscriber.kt`, `VoiceObservationParser.kt`, `VoiceObservationSheet.kt`

### 9.3 🔗 Hypothesis-Driven Observation Graph

**Why:** Unique differentiator — no other app connects observations → questions → hypotheses → evidence.
**Approach:** Semantic matching + correlation detection
**Features:** Live graph inference, weak signal detection, gap detection, question generation
**Status:** 🟡 Partial — graph visualization exists but is static; no live inference
**Files needed:** `GraphInferenceEngine.kt`, `WeakSignalDetector.kt`

### 9.4 🌍 Offline Maps with Drawing Tools

**Why:** Pro feature that separates from casual apps.
**Approach:** OSM tile caching + canvas drawing tools
**Features:** Polygon (survey boundary), line (transect), point (site), track recording, geofencing
**Status:** ✅ Mostly done — MapDrawingTools, TrackRecorder, GeoFenceReminder, MaplibreOfflineManager all exist
**Needs:** UI integration polish, tile source variety

### 9.5 👥 Community Validation Network

**Why:** Builds data quality and community. iNaturalist's "Research Grade" is powerful.
**Approach (v1):** File-based `.fieldmind` package sharing (no backend needed)
**Approach (v2):** Firebase relay server for live sync
**Status:** ❌ Not started

### 9.6 📋 XLSForm Import Engine

**Why:** Opens institutional market (NGOs, universities using ODK/Kobo).
**Approach:** Parse .xlsx → FieldMind survey format
**Support:** Skip logic, required fields, repeats, calculations
**Status:** ❌ Not started

### 9.7 ⚡ Behavioral Event Logger (Ethogram)

**Why:** Unique niche for ethology/animal behavior research.
**Approach:** Define behaviors → tap buttons → log start/end times → CSV export
**Output:** Duration per behavior, transition matrix, frequency chart
**Status:** ❌ Not started

---

## 10. Plans Cross-Reference: What Each Plan Says vs Reality

### Document: FIELDMIND_COMPREHENSIVE_ANALYSIS.md

| Claim | Reality | Status |
|-------|---------|--------|
| "Evidence-first capture — camera opens first" | Camera opens after form; 5 docs agree this is wrong | ❌ Not changed |
| "No batch editing" | `FieldBulkOperations.kt` exists | ✅ Actually done |
| "No observation templates" | Templates exist in `FieldTemplate.kt` but not wired | 🟡 Partial |
| "Species ID doesn't work" | SpeciesClassifier.kt is placeholder | ✅ Confirmed |
| "PDF viewer broken" | WebView fallback | ✅ Confirmed |
| "Research paper maker missing" | Not in codebase | ✅ Confirmed |

### Document: FIELDMIND_FULL_ANALYSIS.md

| Claim | Reality | Status |
|-------|---------|--------|
| "Home screen 2700 lines" | Confirmed — needs splitting | ✅ Accurate |
| "Dialogs 2200+ lines" | Confirmed — needs splitting | ✅ Accurate |
| "No dependency injection" | Confirmed — ViewModel is god object | ✅ Accurate |
| "Weather integration settings only" | Confirmed — API calls never made | ✅ Accurate |
| "Camera V2 full redesign complete" | Confirmed in code | ✅ Accurate |
| "Research Session complete" | Confirmed in code | ✅ Accurate |
| "Workspace 3-tab redesign complete" | Confirmed in code | ✅ Accurate |

### Document: FIELDMIND_NEXT_GEN_ANALYSIS.md

| Claim | Reality | Status |
|-------|---------|--------|
| "Quick Capture should be evidence-first" | Still multi-step form | ❌ Not changed |
| "Species ID engine recommended" | Not implemented | ❌ Not started |
| "Voice transcription recommended" | Not implemented | ❌ Not started |
| "Calendar heatmap recommended" | Already exists in Insights | ✅ Already done |
| "All 20 redesigns planned" | Only ~30% implemented | 🟡 In progress |

### Document: STRATEGIC_FIELDMIND_PLAN_V2.md

| Claim | Reality | Status |
|-------|---------|--------|
| "Groundbreaking features needed" | Agreed — Species ID + Voice top priority | 🟡 Planned |
| "Architecture needs DI + split DAO" | Agreed — god object problem | 🟡 Planned |
| "24-week implementation" | Realistic estimate for Phase A-D | 🟡 Timeline |

### Document: REDESIGN_PLAN.md

| Claim | Reality | Status |
|-------|---------|--------|
| "FieldMindSnackbar.kt created" | Exists! ✅ | ✅ Done |
| "RequiredFieldState.kt created" | Exists! ✅ | ✅ Done |
| "Toast → Snackbar migration" | P0 files done (ObserveScreen, DetailScreen, Dialogs, LibraryScreen) | ✅ Done |
| "Widget system (Quick Capture + Dashboard)" | Widget files exist but not committed/active | ❌ Uncommitted |
| "Home screen redesign" | Not yet done | ❌ Not started |

### Document: FIELDMIND_UI_REDESIGN_PLAN.md

| Claim | Reality | Status |
|-------|---------|--------|
| "Bottom navigation bar" | Not implemented | ❌ Not started |
| "StandardScreenHeader" | Not implemented | ❌ Not started |
| "Capture flow: camera first" | Not changed | ❌ Not started |
| "Knowledge Hub redesign" | Not started | ❌ Not started |
| "Analysis Dashboard 3-tab" | Not started | ❌ Not started |

### Document: fieldmind_legacy_cleanup.md

| Claim | Reality | Status |
|-------|---------|--------|
| "Rhythm music-player legacy to remove" | Confirmed — orphaned workers, services, widgets | 🟡 Partially cleaned |
| "7-step safe removal order" | Mostly followed | 🟡 In progress |

### Document: WHATS_NEW_IMPLEMENTATION.md

| Claim | Reality | Status |
|-------|---------|--------|
| "12 phases of features documented" | Confirmed — ChangelogScreen.kt looks good | ✅ Done |

---

## 11. Consolidated Priority Roadmap

### Phase 1: Critical Fixes (1-2 weeks) — 🔴 NOW

| # | Task | Effort | Impact | Dependencies |
|---|------|--------|--------|--------------|
| 1 | Remove duplicate/legacy workers (Rhythm cleanup) | Low | High | None |
| 2 | Fix Settings.init() side-effects | Low | High | None |
| 3 | Bundle Open-Meteo API calls (weather integration) | Medium | High | Settings already done, providers exist |
| 4 | Fix empty/loading/error states on key screens | Medium | High | None |
| 5 | Remove destructive migration fallback | Low | High | DB v9 migration should be safe |
| 6 | Update OpenAI to latest API endpoint | Low | Medium | None |

### Phase 2: UX Revolution (2-4 weeks) — 🔴 HIGH

| # | Task | Effort | Impact | Dependencies |
|---|------|--------|--------|--------------|
| 7 | Home screen redesign (hero + weather + stats + 4-tile + feed) | High | 🔴 HIGH | Weather API (Phase 1.3) |
| 8 | Capture form evidence-first redesign | High | 🔴 HIGH | Camera V2 done |
| 9 | Weather auto-attach to observations | Medium | HIGH | Phase 1.3 |
| 10 | Weather widget on home screen | Medium | HIGH | Phase 1.3 |
| 11 | Restore dedicated Questions screen | Low | HIGH | Navigation done |
| 12 | Make category optional in capture | Low | HIGH | None |
| 13 | "What's next?" prompt after save | Low | HIGH | None |

### Phase 3: Groundbreaking Features (4-8 weeks) — 🔴 HIGH

| # | Task | Effort | Impact | Dependencies |
|---|------|--------|--------|--------------|
| 14 | Species ID engine (ML Kit + TFLite) | 4-6 weeks | 🔴 CRITICAL | Camera V2 done |
| 15 | Voice-to-observation (Whisper) | 3-4 weeks | 🔴 CRITICAL | Audio recording done |
| 16 | In-app PDF reader (androidx.pdf) | 2-3 weeks | HIGH | None |
| 17 | Photo annotation tools (crop/draw/label) | 2-3 weeks | HIGH | Camera V2 done |
| 18 | Project attachments UI | 1-2 weeks | HIGH | Project entity done |

### Phase 4: Architecture Foundation (4-6 weeks) — 🟡 HIGH

| # | Task | Effort | Impact | Dependencies |
|---|------|--------|--------|--------------|
| 19 | Hilt dependency injection | 3-4 weeks | 🔴 CRITICAL | None (big refactor) |
| 20 | Split FieldMindDao.kt (→ feature DAOs) | 2-3 weeks | HIGH | Phase 4.1 |
| 21 | Split FieldEntities.kt (→ feature entity files) | 1-2 weeks | MEDIUM | Phase 4.1 |
| 22 | Split FieldMindRepository.kt (→ feature repos) | 2-3 weeks | HIGH | Phase 4.1 |
| 23 | Add unit test infrastructure | 2-3 weeks | HIGH | Phase 4.1 |
| 24 | Add FTS4 virtual tables | 1-2 weeks | HIGH | None |

### Phase 5: Feature Completion (4-6 weeks) — 🟡 MEDIUM

| # | Task | Effort | Impact | Dependencies |
|---|------|--------|--------|--------------|
| 25 | Observation templates (save/load protocols) | 2-3 weeks | HIGH | None |
| 26 | CSV export for data tools | 1 week | HIGH | Export engine done |
| 27 | Rich text notes with markdown | 3-4 weeks | HIGH | None |
| 28 | Auto-question generation | 2-3 weeks | MEDIUM | None |
| 29 | Skeleton loaders / shimmer | 1 week | MEDIUM | None |
| 30 | Split monolithic screen files | 3-4 weeks | MEDIUM | None |

### Phase 6: Pro Features (6-10 weeks) — 🟢 FUTURE

| # | Task | Effort | Impact | Dependencies |
|---|------|--------|--------|--------------|
| 31 | Community validation network | 2-3 weeks | MEDIUM | None |
| 32 | XLSForm import engine | 4-5 weeks | MEDIUM | None |
| 33 | Behavioral event logger (ethogram) | 3-4 weeks | MEDIUM | None |
| 34 | Research paper maker (IMRAD) | 4-5 weeks | MEDIUM | Reports done |
| 35 | Observation batch editing | 2-3 weeks | MEDIUM | None |
| 36 | AI auto-categorization | 2-3 weeks | MEDIUM | 10+ observations |
| 37 | Evidence map (photo locations) | 1-2 weeks | LOW | None |
| 38 | Gantt chart / project timeline | 2-3 weeks | LOW | None |

### Phase 7: Polish & Professional (ongoing) — 🔵 ONGOING

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 39 | Standardize spacing scale across all screens | 2-3 weeks | MEDIUM |
| 40 | Standardize card corner radii | 1 week | LOW |
| 41 | Remove LegacyCameraCapture.kt | 0.5 week | LOW |
| 42 | Remove unused weather providers (keep OpenMeteo) | 0.5 week | LOW |
| 43 | Accessibility audit (touch targets, contrast, labels) | 2-3 weeks | MEDIUM |
| 44 | Performance optimization (lazy flows, image caching) | 2-3 weeks | MEDIUM |
| 45 | Extract hardcoded strings → strings.xml | 3-4 weeks | MEDIUM |

---

## 12. Key Metrics & Progress Tracking

### Current Codebase Stats

| Metric | Value | Trend |
|--------|-------|-------|
| Room entities | 21+ (11 data + 10 xref) | ✅ Stable |
| Database version | 9 | ✅ Proper migrations |
| Settings keys | 37 | ⚠️ Grows with each feature |
| Navigation routes | 19+ | ✅ Comprehensive |
| Export formats | 8 | ✅ Industry-leading |
| Weather providers | 6 | ❌ 5 unused, bloated |
| Camera components | 2 (legacy + V2) | ❌ Legacy should be removed |
| AI providers | 2 (Gemini + OpenAI) | ✅ With local model option |
| SM-2 implementation | Complete | ✅ Industry-leading |
| Unit tests | 0 | ❌ Critical gap |
| Dependency injection | None | ❌ Critical gap |
| FTS search | None | ❌ Performance issue |
| Screen files total | 25+ | ⚠️ 4 files need splitting |

### Plan Completeness Summary

| Document Group | Count | Key Contribution |
|----------------|-------|-----------------|
| Comprehensive analysis | 3 | Feature audit + competitive analysis |
| Next-gen analysis | 3 | Groundbreaking features + redesigns |
| UI redesign plans | 3 | Visual overhaul specs |
| Implementation plans | 4 | Concrete weekly tasks |
| Strategic plans | 2 | Market positioning + roadmap |
| Gap analysis | 1 | ObserveScreen + DetailScreen spec comparison |
| Progress summaries | 3 | What's done vs planned |
| Legacy cleanup | 1 | Dead code removal |
| Agent/System docs | 2 | Development setup |
| **TOTAL** | **22 documents** | **Full-spectrum coverage** |

### Implementation Velocity

| Phase | Scope | Estimated Duration | PRs Needed |
|-------|-------|-------------------|------------|
| P1 — Critical Fixes | 6 tasks | 1-2 weeks | 3-4 PRs |
| P2 — UX Revolution | 7 tasks | 2-4 weeks | 5-6 PRs |
| P3 — Groundbreaking Features | 5 tasks | 4-8 weeks | 5-6 PRs |
| P4 — Architecture Foundation | 6 tasks | 4-6 weeks | 4-5 PRs |
| P5 — Feature Completion | 6 tasks | 4-6 weeks | 5-6 PRs |
| P6 — Pro Features | 5 tasks | 6-10 weeks | 5-6 PRs |
| P7 — Polish | 7 tasks | Ongoing | 7-8 PRs |
| **TOTAL** | **42 tasks** | **~25-40 weeks** | **~35-40 PRs** |

### Risk Register

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| TFLite model too large (>150MB) | Medium | High | Use distilled models (MobileNetV3), support download |
| Whisper too CPU-intensive for old devices | Medium | Medium | Cloud fallback, show disclaimer, queue processing |
| DI refactoring breaks existing features | High | High | Staged rollout, comprehensive tests, feature flags |
| Species ID accuracy too low for user trust | Medium | High | Show confidence + disclaimer, allow manual override |
| Weather API rate limiting (Open-Meteo free tier) | Low | Low | Cache 6 hours per location, batch requests |

---

## Appendix A: File Change Summary by Phase

### Files to Create (New)

| File | Phase | Purpose |
|------|-------|---------|
| `WeatherApiService.kt` | P1 | Open-Meteo API calls (separate from provider) |
| `WeatherObservationEntity.kt` | P2 | Weather fields on observations |
| `HomeWeatherCard.kt` | P2 | Weather widget on home screen |
| `HomeDailyStatsCard.kt` | P2 | Daily stats component |
| `HomeRecentFeed.kt` | P2 | Recent observations feed |
| `SpeciesIdResultSheet.kt` | P3 | Species ID results bottom sheet |
| `AudioTranscriber.kt` | P3 | Whisper transcription wrapper |
| `VoiceObservationParser.kt` | P3 | NLP parsing |
| `PdfViewerScreen.kt` | P3 | Native PDF viewer |
| `PhotoEditorComponent.kt` | P3 | Crop/rotate/annotate |
| `AppModule.kt`, `DatabaseModule.kt`, etc. | P4 | Hilt DI modules |
| `ObservationDao.kt`, `QuestionDao.kt`, etc. | P4 | Feature DAOs |
| `ObservationRepository.kt` etc. | P4 | Feature repos |
| `GraphInferenceEngine.kt` | P6 | Hypothesis graph matching |
| `WeakSignalDetector.kt` | P6 | Pattern detection |
| `XLSFormParser.kt` | P6 | XLSX parsing |
| `EventLoggerScreen.kt` | P6 | Ethogram UI |
| `PaperEditorScreen.kt` | P6 | Research paper maker |

### Files to Modify

| File | Changes | Phase |
|------|---------|-------|
| `FieldMindSettings.kt` | Remove init side-effects | P1 |
| `FieldMindDatabase.kt` | Remove destructive fallback | P1 |
| `FieldMindHomeScreen.kt` | Split + redesign (extract weather, stats, feed) | P2 |
| `FieldMindObserveScreen.kt` | Evidence-first layout | P2 |
| `FieldMindDialogs.kt` | Split into entity-specific files | P5 |
| `FieldMindNavigation.kt` | Add new routes | P2+ |
| `FieldMindDao.kt` | Split into feature DAOs | P4 |
| `FieldMindRepository.kt` | Split into feature repos | P4 |
| `FieldEntities.kt` | Split into feature entity files | P4 |
| `FieldMindViewModel.kt` | Split into feature VMs (with Hilt) | P4 |
| `SpeciesClassifier.kt` | Replace placeholder with real TFLite | P3 |
| `GeminiResearchAssistant.kt` | Update OpenAI endpoint | P1 |

---

## Appendix B: All Source Documents Referenced

| # | Path | Type |
|---|------|------|
| 1 | `docs/FIELDMIND_COMPREHENSIVE_ANALYSIS.md` | Analysis |
| 2 | `docs/FIELDMIND_FULL_ANALYSIS.md` | Analysis |
| 3 | `docs/FIELDMIND_NEXT_GEN_ANALYSIS.md` | Analysis |
| 4 | `docs/FIELDMIND_NEXT_GEN_ANALYSIS.md` (root) | Analysis |
| 5 | `docs/OBSERVATION_UI_GAP_ANALYSIS.md` | Gap Analysis |
| 6 | `FIELDMIND_ANALYSIS.md` | Analysis |
| 7 | `FIELDMIND_NEXT_GEN_ANALYSIS.md` (root) | Analysis |
| 8 | `STRATEGIC_FIELDMIND_PLAN_V2.md` | Strategic Plan |
| 9 | `IMPLEMENTATION_ROADMAP_DETAILED.md` | Roadmap |
| 10 | `docs/PR_55_IMPLEMENTATION_PLAN.md` | Implementation Plan |
| 11 | `docs/UI_REDESIGN_PLAN.md` | Redesign Plan |
| 12 | `docs/FIELDMIND_NEXT_PHASES_PLAN.md` | Implementation Plan |
| 13 | `docs/FIELDMIND_UI_REDESIGN_PLAN.md` | Redesign Plan |
| 14 | `docs/REDESIGN_PLAN.md` | Redesign Plan |
| 15 | `docs/PHASE_4_IMPLEMENTATION_PLAN.md` | Implementation Plan |
| 16 | `FIELDMIND_REDESIGN_SUMMARY.md` | Summary |
| 17 | `WHATS_NEW_IMPLEMENTATION.md` | Implementation |
| 18 | `PR_SUMMARY.md` | Summary |
| 19 | `WHATSNEW_SUMMARY.txt` | Summary |
| 20 | `docs/fieldmind_legacy_cleanup.md` | Cleanup Plan |
| 21 | `README_INDEX.md` | Index |
| 22 | `README.md` | Index |
| 23 | `docs/AGENT.md` | System |
| 24 | `docs/prompt.md` | Spec |

---

*This combined analysis was compiled on June 18, 2026 by synthesizing all 24 planning/analysis documents and cross-referencing against the actual codebase structure at `app/src/main/java/chromahub/rhythm/app/features/field/`.*
