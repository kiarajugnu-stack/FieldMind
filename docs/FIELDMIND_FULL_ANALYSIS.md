# 🔬 FieldMind App — Comprehensive Analysis & Restructuring Plan

> **Date:** June 12, 2026 (Updated)
> **Scope:** Full codebase analysis covering architecture, implemented vs. broken features, missing capabilities, UI/UX, duplication risks, and a prioritized restructuring plan based on the latest `main` branch.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Implemented & Working Features](#2--implemented--working-features)
3. [🔴 Critical Issues](#3--critical-issues)
4. [🟡 Code Quality Concerns](#4--code-quality-concerns)
5. [🔵 Missing Features](#5--missing-features)
6. [🎨 UI/UX Assessment](#6--uiux-assessment)
7. [🔄 Duplicate / Fragmented Implementations](#7--duplicate--fragmented-implementations)
8. [🔧 Restructuring Plan](#8--restructuring-plan)
9. [Priority Matrix](#9--priority-matrix)

---

## 1. Architecture Overview

### Data Layer

```
features/field/data/
├── ai/GeminiResearchAssistant.kt          # Google Gemini + OpenAI providers
├── background/
│   ├── FieldMindAutoBackupWorker.kt       # WorkManager auto-backup (current)
│   ├── FieldMindReminderWorker.kt         # WorkManager daily reminder
│   └── FieldMindBackgroundScheduler.kt    # Central WorkManager wiring
├── database/
│   ├── FieldMindDatabase.kt               # Room database (v6, 19 entities)
│   ├── dao/FieldMindDao.kt                # All DAOs in one file
│   └── entity/FieldEntities.kt            # All 19 entity types
├── export/FieldMindExport.kt              # Export (Markdown, CSV, JSON, PNG, SVG, PDF)
├── flashcard/SM2Engine.kt                 # SM-2 spaced repetition algorithm
├── learn/LearnLibrary.kt                  # Offline learning resources
├── location/FieldLocationProvider.kt      # GPS & geocoding
├── repository/FieldMindRepository.kt      # Single repository
├── settings/FieldMindSettings.kt          # SharedPreferences wrapper (33 keys)
└── stats/FieldMindStreaks.kt              # Streak calculation logic
```

### Presentation Layer

```
features/field/presentation/
├── components/
│   ├── FieldMindCharts.kt                 # Canvas-based charting (bar, line, donut, graph, map)
│   ├── FieldMindComponents.kt             # Shared UI (headers, chips, cards, inputs)
│   └── FieldMindIcons.kt                  # Icon definitions
├── navigation/FieldMindNavigation.kt      # Nav graph + bottom bar + rail
├── screens/
│   ├── FieldMindScreens.kt                # ALL main screens (262K+ chars!)
│   ├── InsightsScreen.kt                  # Dedicated insights/composables
│   └── FlashcardSessionScreen.kt          # Dedicated flashcard review
├── theme/FieldMindTheme.kt                # Brand palette + semantic colors
└── viewmodel/FieldMindViewModel.kt        # Single ViewModel for everything
```

### Infrastructure Workers (Separate Package)

```
infrastructure/worker/
├── FieldMindBackupWorker.kt               # DUPLICATE backup worker
├── FieldMindStreakWorker.kt               # DUPLICATE streak/reminder worker
└── ...
```

### What's Working Well

- **19 Room entities** with cross-reference tables — comprehensive data model
- **Reactive data layer** via `Flow<List<T>>` from DAO through Repository to ViewModel
- **Soft-delete pattern** (`archivedAt`, `deletedAt`) across all entities
- **StateFlow-based `FieldMindSettings`** with reactive observation
- **Canvas-based charts** — bar, line, donut, knowledge graph, mini-map — fully offline
- **Navigation** with bottom bar (phone) + rail (tablet), animated transitions, haptic feedback
- **Brand theme** (`FieldMindTheme.kt`) with forest-green palette, semantic entity colors, dynamic Material You option
- **Knowledge graph** visualization in insights with project→question→observation→hypothesis edges
- **Achievement system** with 10 milestones, toast notifications, progress tracking
- **Import/export** round-trip via portable `fieldmind-archive-v2` JSON format
- **Onboarding flow** with 7-step intro before entering the app

---

## 2. ✅ Implemented & Working Features

### 2.1 SM-2 Spaced Repetition — ✅ Fully Implemented

`SM2Engine.kt` implements the full SM-2 algorithm:
- Ease factor calculation with 1.3 minimum
- Interval scheduling: 1 day → 3 days → EF * interval
- Quality rating mapping (0=Again, 2=Good, 3=Easy)
- `isDue()` and `reviewPriority()` helpers
- `nextReviewLabel()` for human-readable intervals

`FlashcardSessionScreen.kt` supports two deck modes:
- **"basic"** — simple flip with Again/Good/Easy
- **"sm2"** — full SM-2 with interval preview, ease factor display, stat pills

`FlashcardEntity` now has SM-2 fields: `easeFactor`, `intervalDays`, `repetitionCount`, `deckMode`, `nextReviewAt`, `lastReviewedAt`.

### 2.2 AI Assistant — ✅ Functioning with Retry Logic

`GeminiResearchAssistant.kt` supports both Gemini and OpenAI:
- Retry with exponential backoff (up to 2 retries on 5xx / timeouts)
- 30s connect / 60s read timeouts
- User-friendly error messages with categorized HTTP codes
- API key redaction in error details
- 8 task types: factuality, testability, hypothesis, source summary, keywords, flashcards, mentor, writing

⚠️ **Still concerning:** OpenAI integration uses `v1/responses` endpoint (the newer Responses API) — this may not be supported by all models listed.

### 2.3 Auto-Backup — ✅ Fully Implemented

`FieldMindAutoBackupWorker.kt` writes rotating JSON archives:
- Respects `autoBackupEnabled` setting
- Archive includes all 9 entity types via `FieldMindExport.archiveJson()`
- Saves to `fieldmind/backups/` in app-private storage
- Prunes to keep the latest 8 backups
- Scheduled by `FieldMindBackgroundScheduler` with Daily / Weekly / Monthly intervals

### 2.4 Reminders — ✅ Fully Implemented

`FieldMindReminderWorker.kt` sends daily notification prompts:
- Respects `remindersEnabled` setting
- Skips notification if user already captured an observation today
- Includes current streak count in notification body
- Creates notification channel on API 26+
- Scheduled daily via `FieldMindBackgroundScheduler`

### 2.5 Streaks — ✅ Fully Implemented

`FieldMindStreaks.kt` provides deterministic streak calculation:
- `currentStreakDays()` works from date strings or epoch millis
- Counts consecutive days backward from today
- Used by reminder worker and dashboard

### 2.6 Export Suite — ✅ Comprehensive

`FieldMindExport.kt` supports:
- **JSON** archive (full round-trip import/export)
- **Markdown** reports with structured sections
- **CSV** observations, sources, data records
- **HTML** for PDF-ready export
- **PDF** via `android.graphics.pdf.PdfDocument`
- **PNG** dashboard snapshot (1200×675)
- **SVG** dashboard with animations
- Citation formatting for sources

### 2.7 Evidence Attachments — ✅ Stored

`EvidenceAttachmentEntity` stores attachments per observation with `localPath`, `caption`, soft-delete support.

### 2.8 Insights / Knowledge Graph — ✅ Built

`InsightsScreen.kt` includes:
- Bar chart (observations by category)
- Line chart (daily capture trend)
- Donut chart (confidence breakdown)
- Mini-map (GPS coordinate scatter plot)
- Knowledge graph (project→question→observation→source→hypothesis edges)
- Collapsible achievements (10 milestones)
- Top tags with frequency
- Open questions & active projects lists
- Research profile card

---

## 3. 🔴 Critical Issues

### 3.1 Monolithic Screens File — 🔴 Critical

`FieldMindScreens.kt` at **262,718+ characters** contains:
- HomeScreen, ObserveScreen, FieldModeScreen, ProjectsScreen
- KnowledgeLibraryScreen, LearnReaderScreen, DetailScreen
- SettingsScreen, BackupExportScreen, ArchiveScreen
- FieldMindOnboardingScreen
- All dialogs and sub-composables

This is the single biggest maintenance risk in the codebase. It makes navigation, understanding, testing, and parallel development nearly impossible.

### 3.2 No Dependency Injection — 🔴 Critical

`FieldMindViewModel` extends `AndroidViewModel` and directly instantiates:
```kotlin
val repository = FieldMindRepository(FieldMindDatabase.getInstance(application).fieldMindDao())
val fieldSettings: FieldMindSettings = FieldMindSettings.getInstance(application)
```

This makes it impossible to:
- Unit test the ViewModel (database dependency can't be mocked)
- Swap implementations
- Control lifecycle of dependencies
- Support previews with fake data

### 3.3 Duplicate Worker Implementations — 🔴 Critical

There are **two separate backup workers** and **two separate streak implementations**:

| What | Location | Status |
|------|----------|--------|
| `FieldMindAutoBackupWorker.kt` | `features/field/data/background/` | ✅ Active, used by `FieldMindBackgroundScheduler` |
| `FieldMindBackupWorker.kt` | `infrastructure/worker/` | ❓ Orphaned, not wired to settings |
| `FieldMindStreaks.kt` | `features/field/data/stats/` | ✅ Active, used by reminder + dashboard |
| `FieldMindStreakWorker.kt` | `infrastructure/worker/` | ❓ Orphaned, separate streak logic in SharedPreferences |

The infrastructure workers have their **own** SharedPreferences-based streak tracking (`KEY_LAST_STREAK_DATE`, `KEY_CURRENT_STREAK`, `KEY_BEST_STREAK`) that is independent of `FieldMindStreaks.currentStreakDays()`. This means streak values can diverge depending on which code path runs.

### 3.4 Settings Mixes Background Scheduling With Instantiation — 🔴 Critical

`FieldMindSettings.init` calls `FieldMindBackgroundScheduler.syncAll()` immediately. This means:
- Every time `getInstance()` is called, background jobs are re-scheduled
- Settings changes cause immediate side effects in the constructor
- Testing settings in isolation requires mocking WorkManager

### 3.5 Full-Text Search Uses `LIKE '%query%'` — 🟡 High

All 6 search DAO methods scan entire tables with `LIKE '%query%'`. On datasets with thousands of records, this will be unusably slow. No FTS virtual tables exist.

---

## 4. 🟡 Code Quality Concerns

### 4.1 Single ViewModel Does Everything

`FieldMindViewModel` has:
- 9 entity state flows
- 40+ methods for CRUD on all entity types
- Direct export logic (`buildMarkdown`, `buildSourceCitation`)
- Direct archive parsing (`restoreArchiveJson`)
- Attachment management

This violates the Single Responsibility Principle and makes the ViewModel a god object.

### 4.2 Single DAO File

`FieldMindDao.kt` contains all 60+ queries in one interface — observation queries, note queries, question queries, hypothesis queries, project queries, source queries, data record queries, report queries, flashcard queries, tag queries, attachment queries, cross-reference link queries, search queries.

Should be split into feature-specific DAOs.

### 4.3 Single Entities File

`FieldEntities.kt` contains all 19 entity types, 9 cross-reference tables, and 5 data classes. At this scale, entity files should be grouped by feature.

### 4.4 Single Repository

`FieldMindRepository.kt` proxies all DAO methods — 70+ functions. One-stop-shop pattern makes it hard to reason about data access patterns per entity.

### 4.5 Tag Sync Can Drift

Tags are stored both as:
1. Comma-separated string in `ObservationEntity.tags`
2. Normalized entries in `TagEntity` with cross-reference via `ObservationTagCrossRef`

`setObservationTags()` clears and re-creates cross-references, but the string field is never updated to match. Editing an observation's tags through the string field without calling `setObservationTags()` can cause drift.

### 4.6 Settings Still Uses SharedPreferences

33 settings keys are stored in raw `SharedPreferences` with manual get/put/edit boilerplate. No type-safe delegates, no DataStore migration, no migration path if keys change.

### 4.7 OpenAI Uses Responses API

The OpenAI provider sends requests to `v1/responses` which is OpenAI's newer Responses API. This endpoint:
- May not support all models (especially older ones)
- Has different response structure than Chat Completions
- The app extracts `output_text` which may not always be present
- Falls back to iterating `output` array for `content[0].text`

Consider adding a `v1/chat/completions` fallback path for wider model compatibility.

### 4.8 Onboarding Is Mandatory

The onboarding screen is shown before the main app and must be completed. It's 7 steps long with informational content. Users who want to explore the app first cannot skip.

### 4.9 No Loading / Error States in Many Screens

- `FieldMindScreens.kt` uses `collectAsState()` but few screens show skeleton loaders or error cards
- Empty states exist but loading states are mostly absent
- Network failures in AI assistant show a card with text, not a dedicated error composable

### 4.10 Inconsistent Card Radius

Card corner radius values: 18dp, 20dp, 22dp, 24dp, 26dp, 28dp, 999dp — inconsistent across the codebase.

### 4.11 Database Migration Uses Destructive Fallback

```kotlin
.fallbackToDestructiveMigration()
```

This means any schema change that Room can't auto-migrate will **destroy all user data**. After version 6, this is risky.

---

## 5. 🔵 Missing Features

### 5.1 Privacy Lock — Placeholder Only

`privacyLockEnabled` exists in settings but:
- No `BiometricPrompt` implementation
- No PIN fallback
- No `EncryptedSharedPreferences` for storing credentials
- Sensitive screens are not gated

### 5.2 Local Model — Placeholder Only

`localModelEnabled`, `localModelOption`, `localModelDownloaded`, `localModelUseForStudy` exist in settings but:
- No model download manager
- No inference engine (TensorFlow Lite, ONNX Runtime)
- No actual model loading code
- Settings are purely cosmetic

### 5.3 No Offline Copy for Attachments

`EvidenceAttachmentEntity` stores URIs and optional `localPath`, but the UI flow never copies attachments to app-private storage. If the user revokes permission or the URI provider is unavailable, attachments are lost.

### 5.4 No Undo/Redo

All edits are permanent with no undo snackbar or history. Users can accidentally delete or overwrite data.

### 5.5 No Bulk Operations

No multi-select, bulk delete, bulk tag, or bulk archive on any list screen. Users must delete items one at a time.

### 5.6 No Templates

No observation, report, or data collection templates. Users fill out forms from scratch each time.

### 5.7 No Map View (Full)

The mini-map in insights shows GPS point distribution, but there's no:
- Interactive map with tiles/terrain
- Marker clustering
- Tap-to-open observation detail from map
- Filter by category or date

### 5.8 No Timeline View

No chronological timeline that shows all entity types (observations, notes, questions, reports) in a unified stream.

### 5.9 No Cloud Sync

No sync protocol, no backend, no multi-device support. Data is entirely local.

### 5.10 No Share Sheet Integration

No Android share sheet to share observations, reports, or findings to other apps.

### 5.11 No Citation Export (Zotero/Mendeley)

Citations are formatted inline but there's no RIS/BibTeX export for reference managers.

### 5.12 No Timer/Stopwatch for Behavioral Observations

No built-in timer for timed behavioral observations — a common field research need.

---

## 6. 🎨 UI/UX Assessment

### Navigation Flow
- **4 bottom tabs** (Today, Capture, Workspace, Library) + **17+ composable routes** via nav controller
- Overflow destinations (Insights, Search, Backup, Settings, etc.) are reached from within tabs
- **Haptic feedback** on navigation taps ✅
- **Animated tab icons** with scale/lift/alpha ✅
- **Tablet support** via `NavigationRail` when width ≥ 720dp ✅

### Capture Flow
- Quick capture shows a form with subject, category, confidence, location, tags, evidence, and attachments
- Field mode is a `compactFieldMode` variant of the same screen
- **Missing:** one-tap capture, smart defaults, camera-first, progressive disclosure

### Detail Screen
- Single `DetailScreen` composable handles all entity types via `kind` string parameter
- Massive `when` block with entity-specific fields
- **Missing:** entity-specific layouts, dedicated edit mode, related entities section

### Design System
- Forest-green brand palette with semantic entity colors ✅
- Dynamic Material You support (Android 12+) ✅
- Light + dark color schemes with FieldMind-specific dark palette ✅
- Card radius inconsistent (ranges from 18dp to 28dp) ❌
- Spacing inconsistent (8dp, 12dp, 14dp, 16dp, 18dp) ❌
- Skeleton loaders missing on most screens ❌
- Loading states absent ❌
- Error states use snackbar only, no dedicated error cards ❌

### Accessibility
- Content descriptions missing on many icons ❌
- No semantic markup for screen readers ❌
- Some chip touch targets may be too small ❌
- No high-contrast mode ❌

---

## 7. 🔄 Duplicate / Fragmented Implementations

### 7.1 Backup Workers — ⚠️ Duplicate

| Aspect | `FieldMindAutoBackupWorker` | `FieldMindBackupWorker` |
|--------|---------------------------|------------------------|
| **Package** | `features.field.data.background` | `worker` (infrastructure) |
| **Scheduling** | Via `FieldMindBackgroundScheduler` | Not wired to settings |
| **Storage** | `filesDir/fieldmind/backups/` | `getExternalFilesDir(null)/FieldMindBackups/` |
| **Pruning** | Keeps last 8 backups | Keeps last 7 backups |
| **Settings awareness** | Reads `autoBackupEnabled` | No settings check |
| **Tag** | Auto-generated | `fieldmind_auto_backup` |

**Recommendation:** Decide on one canonical implementation, remove the other. Prefer `FieldMindAutoBackupWorker` as it's wired into settings and the scheduler.

### 7.2 Streak Tracking — ⚠️ Fragmented

| Aspect | `FieldMindStreaks` | `FieldMindStreakWorker` |
|--------|-------------------|------------------------|
| **Package** | `features.field.data.stats` | `worker` (infrastructure) |
| **Calculation** | `currentStreakDays()` — date-based iterative | SharedPreferences with `KEY_CURRENT_STREAK` |
| **Persistence** | Computed from observation dates each time | Persisted in `fieldmind_streak` SharedPreferences |
| **Notification** | No | Yes — sends reminder notification |
| **Used by** | `FieldMindReminderWorker`, dashboard | Nothing in the feature package |

**Recommendation:** Consolidate streak logic into `FieldMindStreaks` and remove `FieldMindStreakWorker`. The reminder worker can calculate streaks from observations directly.

### 7.3 Reminder Implementation — ⚠️ Fragmented

`FieldMindReminderWorker` (feature package) and `FieldMindStreakWorker` (infrastructure) both send reminder-type notifications. They could conflict or duplicate notifications.

---

## 8. 🔧 Restructuring Plan

### Phase 1: Critical Fixes (Immediate)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 1 | **Remove duplicate workers** — decide on `FieldMindAutoBackupWorker` as canonical backup, `FieldMindStreaks` as canonical streak, remove infrastructure duplicates | Low | 🔴 Critical |
| 2 | **Fix init-side-effect in `FieldMindSettings`** — move `syncAll()` call out of init block to explicit `initialize()` method | Low | 🔴 Critical |
| 3 | **Fix OpenAI `v1/responses` endpoint** — add `v1/chat/completions` fallback, validate response parsing with gpt-4.1-mini | Low | 🟡 High |
| 4 | **Add migration plan** — replace `fallbackToDestructiveMigration()` with actual migration steps or schema export | Low | 🟡 High |

### Phase 2: Architecture (Week 1-2)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 5 | **Split `FieldMindScreens.kt`** into separate files per screen (Home, Observe, Projects, Library, Insights, Detail, Settings, BackupExport, Search, Onboarding, etc.) | 🔴 High | 🔴 Critical |
| 6 | **Add Hilt/Dagger** for dependency injection — `@HiltViewModel`, database module, repository module | 🟡 Medium | 🔴 Critical |
| 7 | **Split `FieldMindDao.kt`** into per-feature DAOs (ObservationDao, NoteDao, QuestionDao, etc.) | 🟡 Medium | 🟡 High |
| 8 | **Split `FieldEntities.kt`** into per-feature entity files | 🟡 Medium | 🟡 High |
| 9 | **Split `FieldMindRepository.kt`** into per-feature repositories | 🟡 Medium | 🟡 High |

### Phase 3: Data Layer Improvements (Week 3-4)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 10 | **Migrate settings to Proto DataStore** — type-safe, coroutine-based, migration from SharedPreferences | 🟡 Medium | 🟡 High |
| 11 | **Add FTS virtual tables** for observations, notes, questions, sources + relevance-ranked DAO queries | 🟡 Medium | 🟡 High |
| 12 | **Fix tag sync** — remove comma-separated `tags` string field from entities, use only cross-reference tables | 🟡 Medium | 🟡 High |
| 13 | **Add attachment offline copy** — copy selected evidence to app-private storage on save, add size limits | 🟡 Medium | 🟡 High |

### Phase 4: New Features (Week 5-6)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 14 | **Implement privacy lock** — `BiometricPrompt` + PIN fallback + `EncryptedSharedPreferences` | 🟡 Medium | 🟡 High |
| 15 | **Add undo/redo** — command pattern for entity edits, undo snackbar after each action | 🟡 Medium | 🟡 High |
| 16 | **Add map view** — integrate Google Maps SDK or Mapbox with clustering, tap-to-detail | 🔴 High | 🟡 High |
| 17 | **Add timeline view** — unified chronological stream of all entity types with filters | 🟡 Medium | 🟡 Medium |
| 18 | **Add bulk operations** — multi-select with checkboxes, bulk delete/tag/archive | 🟡 Medium | 🟡 High |

### Phase 5: UI/UX Polish (Week 7-8)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 19 | **Standardize design system** — consistent 20dp card radius, 8dp grid spacing | 🟢 Low | 🟡 Medium |
| 20 | **Add skeleton loaders** — per-screen loading composables, fade-to-content transition | 🟢 Low | 🟡 Medium |
| 21 | **Add error states** — error cards with retry actions on all data screens | 🟢 Low | 🟡 Medium |
| 22 | **Add shared element transitions** — entity card → detail navigation | 🟡 Medium | 🟢 Low |
| 23 | **Improve capture flow** — one-tap quick capture, smart defaults, camera-first option | 🟡 Medium | 🟡 High |
| 24 | **Make onboarding skippable** — reduce to 3 steps (Welcome → Permissions → Go) + skip button | 🟢 Low | 🟡 Medium |

### Phase 6: Advanced Features (Week 9+)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 25 | **Cloud sync** — design sync protocol, add backend, conflict resolution, offline-first | 🔴 High | 🟡 High |
| 26 | **Export to Zotero/Mendeley** — RIS/BibTeX format, citation formatting (APA, MLA, Chicago) | 🟡 Medium | 🟢 Low |
| 27 | **Add templates** — template entity, pre-fill from templates, save observations as templates | 🟡 Medium | 🟡 Medium |
| 28 | **Add timer/stopwatch** — behavioral observation timer with lap recording | 🟡 Medium | 🟢 Low |
| 29 | **Add chart library** (Vico) — interactive tooltips, tap-to-drill, animations, accessibility | 🟡 Medium | 🟡 Medium |
| 30 | **Implement local on-device model** — TensorFlow Lite downloader + inference engine | 🔴 High | 🟢 Low |

---

## 9. Priority Matrix

| Priority | Task | Effort | Impact | Current Status |
|----------|------|--------|--------|----------------|
| 🔴 P0 | Consolidate duplicate workers | Low | 🛡️ Critical | ⚠️ Duplicate code |
| 🔴 P0 | Fix settings init side-effects | Low | 🛡️ Critical | ❌ Existing |
| 🔴 P0 | Split `FieldMindScreens.kt` | High | 🔥 High | ❌ Not started |
| 🔴 P0 | Add dependency injection (Hilt) | Medium | 🔥 High | ❌ Not started |
| 🟡 P1 | Split DAO into per-feature files | Medium | 🔥 High | ❌ Not started |
| 🟡 P1 | Migrate to DataStore | Medium | 🟡 Medium | ❌ Not started |
| 🟡 P1 | Add FTS for search | Medium | 🟡 Medium | ❌ Not started |
| 🟡 P1 | Fix tag sync | Medium | 🟡 Medium | ❌ Not started |
| 🟡 P1 | Add privacy lock | Medium | 🟡 Medium | ❌ Placeholder only |
| 🟡 P1 | Add offline attachment copy | Medium | 🟡 Medium | ❌ Not started |
| 🟡 P1 | Add map view | High | 🟡 Medium | ⚠️ Mini-map only |
| 🟡 P1 | Standardize design system | Low | 🟡 Medium | ⚠️ Inconsistent |
| 🟢 P2 | Add undo/redo | Medium | 🟡 Medium | ❌ Not started |
| 🟢 P2 | Add bulk operations | Medium | 🟡 Medium | ❌ Not started |
| 🟢 P2 | Add skeleton loaders | Low | 🟡 Medium | ❌ Not started |
| 🟢 P2 | Add timeline view | Medium | 🟢 Low | ❌ Not started |
| 🟢 P2 | Templates | Medium | 🟢 Low | ❌ Not started |
| 🟢 P2 | Cloud sync | High | 🟡 Medium | ❌ Not started |

---

## 10. Key Metrics

| Metric | Value |
|--------|-------|
| Total entities | 19 (10 data + 9 cross-reference) |
| Room database version | 6 |
| Migration strategy | Destructive fallback ⚠️ |
| Settings keys | 33 SharedPreferences keys |
| Screen files | 3 (1 monolithic ⚠️ + 2 dedicated) |
| Backup implementations | 2 duplicate ⚠️ |
| Streak implementations | 2 fragmented ⚠️ |
| AI providers | 2 (Gemini + OpenAI) |
| Export formats | 6 (Markdown, CSV, JSON, PNG, SVG, PDF) |
| Navigation routes | 17+ composable destinations |
| Chart types | 6 (bar, line, donut, breakdown, knowledge graph, mini-map) |
| Achievement milestones | 10 |
| SM-2 implementation | Complete ✅ |
| Haptic feedback | Present ✅ |
| Tablet adaptation | Present ✅ |
| Dark mode | Present ✅ |
| Dependency injection | Missing ❌ |
| Unit tests | Missing ❌ |

---

## Summary

The FieldMind feature has **significantly matured** with implemented SM-2 flashcards, auto-backup, reminders, streak tracking, AI assistant with retry logic, and comprehensive export. The **core data model is excellent** — 19 entities with cross-references, soft-delete, and full reactive data flow.

However, the codebase now has **critical architectural debt**:
1. **Duplicate implementations** — two backup workers and two streak trackers that could diverge
2. **Monolithic 262K+ character file** that grows more unmanageable with every feature
3. **No dependency injection** preventing testing
4. **Settings with constructor side-effects** that schedule background jobs on every access
5. **Single ViewModel, DAO, repository, and entities file** violating separation of concerns
6. **Placeholder features** (privacy lock, local model) with settings but no implementation

The highest-impact immediate actions: consolidate the duplicates, fix the settings init issue, then tackle the architectural debt by splitting files and adding DI.

---

## 11. June 2026 — Second Implementation Pass (PR #18)

This section documents changes made in the second follow-up pass addressing the user's feedback.

### Changes Made (PR #18: `codex/fix-sm2-map-navigation-export`)

| Issue | Change | Files |
|-------|--------|-------|
| **SM-2 flashcard not visible** | Added SM-2 toggle to NewFlashcardDialog and EditFlashcardDialog; added `deckMode` parameter to ViewModel.addFlashcard so users can create cards in SM-2 mode | `FieldMindScreens.kt`, `FieldMindViewModel.kt` |
| **Insights hidden** | Added Insights as 5th bottom navigation tab | `FieldMindNavigation.kt` |
| **Search hidden** | Search is now accessible via Insights header icon and Home QuickActionGrid | `FieldMindScreens.kt`, `InsightsScreen.kt` |
| **No proper GPS map** | Integrated osmdroid (OpenStreetMap) v6.1.20; created `OsmMap` composable with zoom, pan, markers, and GPS location overlay; replaced Canvas MiniMap in Insights and observation detail; added full-screen MapScreen route | `build.gradle.kts`, `libs.versions.toml`, `MainActivity.kt`, `FieldMindCharts.kt`, `FieldMindScreens.kt`, `InsightsScreen.kt`, `FieldMindNavigation.kt` |
| **No accessible achievements** | Insights (which has collapsible achievements with Toast notifications) is now in the bottom nav tab bar | `FieldMindNavigation.kt` |
| **Backup/Export not accessible** | Added ExportStudio route and Home QuickActionGrid tile for direct access to backup/export/import | `FieldMindNavigation.kt`, `FieldMindScreens.kt` |
| **No way to reach Map/Export/Search quickly** | Added QuickActionGrid on Home screen with Map, Export, Search, and Review (Flashcards) tiles | `FieldMindScreens.kt` |

### Updated Priority Matrix

| Priority | Task | Effort | Impact | Previous Status | New Status |
|----------|------|--------|--------|----------------|------------|
| 🟡 P1 | Add map view (OSM) | Medium | 🟡 Medium | ⚠️ Mini-map only | ✅ OSM integrated |
| 🟡 P1 | Make Insights/Search accessible | Low | 🟡 Medium | ❌ Hidden | ✅ Bottom tab + quick actions |
| 🟡 P1 | SM-2 flashcard toggle | Low | 🟡 Medium | ⚠️ Code existed, UI missing | ✅ Toggle added |

### Remaining High-Impact Items

| Item | Priority |
|------|----------|
| Consolidate duplicate workers | 🔴 P0 |
| Fix FieldMindSettings init side-effects | 🔴 P0 |
| Split FieldMindScreens.kt | 🔴 P0 |
| Add dependency injection | 🔴 P0 |
| Migrate to DataStore | 🟡 P1 |
| Add FTS search | 🟡 P1 |
| Fix OpenAI `v1/responses` endpoint | 🟡 P1 |
| Privacy lock | 🟡 P1 |
| Offline attachment copy | 🟡 P1 |
| Timeline view | 🟢 P2 |

---

*This analysis was generated on June 12, 2026 by re-analyzing the full FieldMind codebase on `main` (commit `e3fc8cd8`), with updates for PR #18.*

## 12. Phase 2 Architecture Splits

The monolithic FieldMindScreens.kt (4041 lines) has been split into 12 focused files:

| File | Lines | Contents |
|------|-------|----------|
| FieldMindScreenUtils.kt | 92 | Shared internal constants + utility functions |
| FieldMindOnboardingScreen.kt | 141 | Onboarding flow |
| FieldMindHomeScreen.kt | 380 | Today/Home dashboard |
| FieldMindObserveScreen.kt | 690 | Capture and field mode |
| FieldMindProjectsScreen.kt | 333 | Project workspace |
| FieldMindLibraryScreen.kt | 620 | Knowledge library and learn |
| FieldMindArchiveScreen.kt | 58 | Search archive |
| FieldMindBackupExportScreen.kt | 327 | Export studio and backups |
| FieldMindSettingsScreen.kt | 230 | Settings |
| FieldMindDetailScreen.kt | 526 | Entity detail and backlinks |
| FieldMindDialogs.kt | 967 | All create/edit dialogs + form helpers |
| FieldMindMapScreen.kt | 40 | Full-screen OSM map |

All files remain in the same package and are automatically discovered by the navigation wildcard import.
