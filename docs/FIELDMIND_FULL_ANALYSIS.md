# 🔬 FieldMind App — Comprehensive Analysis & Restructuring Plan

> **Date:** June 12, 2026
> **Scope:** Full codebase analysis covering architecture, missing features, broken features, UI/UX, design system, and a prioritized restructuring plan.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Broken / Non-Functional Features](#2--broken--non-functional-features)
3. [Poorly Implemented Features](#3--poorly-implemented-features)
4. [Missing Features](#4--missing-features)
5. [UI/UX Problems & Redesign Suggestions](#5--uiux-problems--redesign-suggestions)
6. [Restructuring Plan](#6--restructuring-plan)
7. [Priority Matrix](#7--priority-matrix)

---

## 1. Architecture Overview

### What's Working Well

- **Clean Room database** with 10+ entity types, DAOs, and cross-reference tables
- **Repository pattern** with `FieldMindRepository` → `FieldMindViewModel`
- **StateFlow-based reactive data layer**
- **Dedicated `FieldMindSettings`** with SharedPreferences
- **Separate `FieldMindDatabase`** from the main music app database
- **Soft-delete pattern** across all entities (archivedAt, deletedAt fields)
- **Comprehensive entity model** covering the full research lifecycle

### Architecture Problems

| Problem | Severity | Detail |
|---------|----------|--------|
| **Monolithic screens file** | 🔴 Critical | `FieldMindScreens.kt` is **262,718+ characters** — a single file containing Home, Capture, Field Mode, Projects, Library, Insights, Settings, Detail, Learn, Reader, Flashcards, Search, Backup, and all dialogs. This is unmaintainable. |
| **No dependency injection** | 🟡 High | `FieldMindViewModel` directly instantiates `FieldMindDatabase` and `FieldMindRepository` via `application` context. No Hilt/Dagger — makes testing impossible. |
| **ViewModel does too much** | 🟡 High | One `FieldMindViewModel` holds state for ALL entity types (observations, notes, questions, hypotheses, projects, sources, data, reports, flashcards, tags). Should be split into feature-specific ViewModels. |
| **No use-case layer** | 🟡 Medium | Business logic lives directly in ViewModel methods. A use-case layer would improve testability and reusability. |
| **Settings uses raw SharedPreferences** | 🟡 Medium | No DataStore migration. No type safety. Manual serialization. |

### File Structure Summary

```
features/field/
├── data/
│   ├── ai/GeminiResearchAssistant.kt          # AI integration (broken)
│   ├── database/
│   │   ├── FieldMindDatabase.kt               # Room database
│   │   ├── dao/FieldMindDao.kt                # All DAOs in one file
│   │   └── entity/FieldEntities.kt            # All entities in one file
│   ├── export/FieldMindExport.kt              # Export utilities
│   ├── learn/LearnLibrary.kt                  # Offline learning resources
│   ├── location/FieldLocationProvider.kt      # GPS & geocoding
│   ├── repository/FieldMindRepository.kt      # Data repository
│   └── settings/FieldMindSettings.kt          # SharedPreferences wrapper
└── presentation/
    ├── components/
    │   ├── FieldMindCharts.kt                 # Custom chart composables
    │   ├── FieldMindComponents.kt             # Shared UI components
    │   └── FieldMindIcons.kt                  # Icon definitions
    ├── navigation/FieldMindNavigation.kt      # Nav graph + bottom bar
    ├── screens/FieldMindScreens.kt            # ALL screens (262K+ chars!)
    ├── theme/FieldMindTheme.kt                # Brand colors & semantic tokens
    └── viewmodel/FieldMindViewModel.kt        # Single ViewModel for everything
```

---

## 2. 🔴 Broken / Non-Functional Features

### 2.1 AI Assistant — Broken

- `GeminiResearchAssistant` uses raw `HttpURLConnection` — no retry, no error handling beyond status codes
- OpenAI integration uses `v1/responses` endpoint which may not exist for all models
- No streaming support — entire response must buffer before display
- **No error UI** — if the API fails, the user sees a raw error string in a card

**What to fix:**
- Add proper error handling with user-friendly messages
- Add loading indicators during API calls
- Implement streaming for better UX
- Add retry logic with exponential backoff
- Create a proper error state composable

### 2.2 Local Model — Placeholder Only

- `FieldMindSettings` has `localModelEnabled`, `localModelOption`, `localModelDownloaded` — but **there is zero code that actually downloads, loads, or runs a local model**
- The settings toggle is purely cosmetic

**What to fix:**
- Either implement on-device inference (TensorFlow Lite, ONNX Runtime) or remove the settings entirely
- If implementing: add model download manager, inference engine, and UI for model selection

### 2.3 Knowledge Graph (Insights) — Incomplete

- `InsightsScreen.kt` exists but the knowledge graph visualization is likely basic
- Entity relationships exist in cross-reference tables but the graph UI is not fully wired

**What to fix:**
- Build a proper interactive graph using a canvas-based or library-based approach
- Add tap-to-navigate from graph nodes to entity details
- Add filtering by entity type and date range

### 2.4 Backup/Export — Partial

- `FieldMindExport` supports Markdown, CSV, JSON, PNG, SVG, PDF
- But **auto-backup** (`autoBackupEnabled` setting) has no background worker implementation — it's just a settings toggle with no actual WorkManager job

**What to fix:**
- Implement `BackupWorker` using WorkManager with periodic scheduling
- Add backup destination selection (local storage, cloud)
- Add backup status notifications

### 2.5 Reminders/Streaks — Settings Only

- `remindersEnabled` and `streaksEnabled` settings exist but there's **no notification scheduler, no WorkManager job, no streak tracking logic**

**What to fix:**
- Implement `ReminderWorker` for daily observation reminders
- Add streak calculation logic based on observation timestamps
- Display streak counter on Home screen

### 2.6 Privacy Lock — Placeholder

- `privacyLockEnabled` setting exists but there's **no biometric/PIN lock implementation**

**What to fix:**
- Implement BiometricPrompt for app launch
- Add PIN fallback using EncryptedSharedPreferences
- Lock sensitive screens (Settings, exports)

### 2.7 Flashcard Spaced Repetition — Basic

- `FlashcardEntity` has `reviewCount`, `lastReviewedAt`, `nextReviewAt` fields but the review logic in `FlashcardSessionScreen` doesn't implement actual spaced repetition (SM-2 or similar)

**What to fix:**
- Implement SM-2 algorithm for review scheduling
- Add "Again", "Hard", "Good", "Easy" buttons during review
- Update `nextReviewAt` based on quality rating

### 2.8 Evidence Attachments — URI Only

- Attachments store URIs but there's **no offline copy mechanism** — if the user revokes permissions or the URI becomes stale, attachments are lost
- No attachment size limits or cleanup

**What to fix:**
- Copy attachments to app-private storage on save
- Add size limits and cleanup for old attachments
- Handle URI permission revocation gracefully

---

## 3. 🟡 Poorly Implemented Features

### 3.1 Search — Full-Text SQL LIKE

- All search queries use `LIKE '%query%'` — no FTS (Full-Text Search) table, no indexing
- 6 separate search functions in the DAO, all scanning entire tables
- No search result highlighting or relevance ranking

**Improvement plan:**
- Add Room FTS entities for each searchable table
- Create FTS virtual tables with appropriate tokenizers
- Implement relevance scoring
- Add search result highlighting in the UI

### 3.2 Tags — String + Cross-Reference Hybrid

- Tags are stored as comma-separated strings AND in cross-reference tables
- `setObservationTags()` parses the string, creates/finds tags, and links them — but the string field and cross-reference can drift out of sync
- No tag auto-completion or suggestion

**Improvement plan:**
- Remove the comma-separated string field from entities
- Use only cross-reference tables for tag associations
- Add tag auto-completion in text fields
- Add tag frequency display in the tag picker

### 3.3 Navigation — Flat Structure

- All 17+ screens are defined as top-level `composable()` routes in `FieldMindNavHost`
- No nested navigation graphs — Questions, Hypotheses, Data, Reports all share `ProjectsScreen` with a `startTab` parameter
- Deep linking is not set up

**Improvement plan:**
- Create nested navigation graphs for each tab
- Add deep link support for sharing specific entities
- Implement proper back stack management

### 3.4 Detail Screen — Overloaded

- `DetailScreen` is a single composable that handles ALL entity types via a `kind` string parameter
- This means every entity type's edit UI is in one massive when/switch block

**Improvement plan:**
- Create separate detail screen composables per entity type
- Use type-safe navigation arguments instead of string routing
- Add entity-specific action bars

### 3.5 Charts — Custom Drawing

- `FieldMindCharts.kt` implements bar, line, donut, and graph charts using custom Canvas drawing
- No chart library — means no tooltips, no animations, no accessibility, no responsive sizing

**Improvement plan:**
- Migrate to Vico chart library (Material 3 compatible)
- Add interactive tooltips and tap-to-drill
- Add chart animations
- Ensure accessibility with content descriptions

### 3.6 Onboarding — Too Long

- 6 informational pages + 1 permission page = 7 steps before the user can use the app
- Many research apps skip onboarding entirely or make it optional

**Improvement plan:**
- Reduce to 3 steps: Welcome → Permissions → Go
- Make onboarding skippable at any point
- Add progressive disclosure (teach features as they're first used)

---

## 4. 🔵 Missing Features

### 4.1 Core Research Workflow

| Feature | Status | Priority |
|---------|--------|----------|
| **Undo/redo for edits** | Missing | 🔴 High |
| **Bulk operations** (select multiple, bulk delete, bulk tag) | Missing | 🔴 High |
| **Templates** for observations, questions, reports | Missing | 🟡 Medium |
| **Observation clustering** (auto-group similar observations) | Missing | 🟡 Medium |
| **Timeline view** (chronological across all entities) | Missing | 🟡 Medium |
| **Map view** for observations with GPS | Missing | 🔴 High |
| **Photo comparison** (before/after, side-by-side) | Missing | 🟡 Medium |

### 4.2 Collaboration & Sync

| Feature | Status | Priority |
|---------|--------|----------|
| **Cloud sync** | Missing | 🔴 High |
| **Multi-device access** | Missing | 🔴 High |
| **Share observations** (share sheet) | Missing | 🟡 Medium |
| **Export to Zotero/Mendeley** | Missing | 🟡 Medium |

### 4.3 Learning & Intelligence

| Feature | Status | Priority |
|---------|--------|----------|
| **Smart suggestions** (based on observation patterns) | Partial | 🟡 Medium |
| **Auto-categorization** using ML | Missing | 🟢 Low |
| **Duplicate detection** | Missing | 🟡 Medium |
| **Related observation finder** | Missing | 🟡 Medium |

### 4.4 Data Collection Tools

| Feature | Status | Priority |
|---------|--------|----------|
| **Timer/stopwatch** for behavioral observations | Missing | 🟡 Medium |
| **Photo timer** (burst mode) | Missing | 🟢 Low |
| **Audio transcription** | Missing | 🟢 Low |
| **Barcode/QR scanner** for specimen tracking | Missing | 🟢 Low |
| **Custom data collection forms** | Missing | 🟡 Medium |

### 4.5 Reporting

| Feature | Status | Priority |
|---------|--------|----------|
| **Report templates** | Missing | 🟡 Medium |
| **Citation manager** (auto-format citations) | Missing | 🟡 Medium |
| **Figure/table insertion** in reports | Missing | 🟡 Medium |
| **Collaborative editing** | Missing | 🟢 Low |

---

## 5. 🎨 UI/UX Problems & Redesign Suggestions

### 5.1 Navigation Redesign

**Current:** 5 bottom tabs + FAB + hidden overflow destinations

**Problems:**
- Users can't discover Questions, Hypotheses, Data Tools, Reports, Learn, Flashcards, Search, Backup without exploring
- "Projects" tab is overloaded (Projects + Questions + Hypotheses + Data + Reports)
- No back stack management for nested screens

**Suggested redesign:**

```
Bottom Nav (4 tabs):
├── 🏠 Dashboard (Today) — quick capture, daily goal, recent activity
├── 📝 Capture — observation + note creation (prominent)
├── 📂 Workspace — projects, questions, hypotheses, data, reports
├── 📚 Library — sources, notes, reading, flashcards, learn

Overflow menu:
├── 🔍 Search
├── 📊 Insights (charts, map, knowledge graph)
├── 💾 Backup/Export
├── ⚙️ Settings
```

### 5.2 Capture Flow Redesign

**Current:** Quick capture → Choose mode → Choose category → Fill form (4+ steps)

**Problems:**
- Too many steps for "quick" capture
- The category-first flow is unintuitive
- Field mode is a separate screen instead of a mode toggle

**Suggested redesign:**
- **One-tap capture:** Single button that opens a minimal form with category as a horizontal scrollable chip bar at the top
- **Smart defaults:** Remember last category, auto-fill location if enabled
- **Camera-first option:** Direct camera launch with post-capture metadata entry
- **Field mode as overlay:** Instead of a separate screen, make it a floating action that shows a minimal capture sheet

### 5.3 Detail Screen Redesign

**Current:** Single `DetailScreen` composable with massive when/switch on `kind`

**Problems:**
- One file handles 10+ entity types
- Edit mode is inline (no dedicated edit screen)
- No visual distinction between entity types
- Actions (delete, archive, export) are buried in the header

**Suggested redesign:**
- **Separate detail screens per entity type** — each with tailored layouts
- **Dedicated edit mode** — separate screen or bottom sheet
- **Entity-specific actions** — floating action buttons relevant to each type
- **Related entities section** — show linked questions, observations, sources

### 5.4 Insights Screen Redesign

**Current:** Charts + achievements + knowledge graph in one screen

**Problems:**
- Knowledge graph is likely too complex for a mobile screen
- No filtering or drill-down
- Charts are custom-drawn without interactivity

**Suggested redesign:**
- **Tabbed Insights:** Overview | Charts | Map | Graph | Achievements
- **Interactive charts:** Use a chart library (Vico, MPAndroidChart) for tooltips and tap-to-drill
- **Map-first:** Observations with GPS should default to map view
- **Knowledge graph as optional:** Power users can access it, but don't make it the default

### 5.5 Design System Issues

| Issue | Current | Suggested |
|-------|---------|-----------|
| **Card radius** | Mixed (18dp, 22dp, 24dp, 28dp) | Standardize to 20dp for cards, 28dp for sheets |
| **Spacing** | Inconsistent (8dp, 12dp, 14dp, 16dp, 18dp) | Use 8dp grid system consistently |
| **Typography** | Uses Material3 defaults | Add FieldMind-specific heading styles |
| **Color usage** | Semantic colors defined but not always used | Enforce semantic colors for all entity badges |
| **Empty states** | Good but repetitive | Add illustrations, not just icons |
| **Loading states** | Missing on many screens | Add skeleton loaders |
| **Error states** | Snackbar only | Add error cards with retry actions |
| **Animations** | Basic fade/scale transitions | Add shared element transitions for entity cards |
| **Dark mode** | Supported but brand colors don't adapt well | Test and adjust contrast ratios |

### 5.6 Accessibility Issues

- No content descriptions on many icons
- No semantic markup for screen readers
- Touch targets may be too small on some chips
- No high-contrast mode support

---

## 6. 🔧 Restructuring Plan

### Phase 1: Foundation (Week 1-2)

1. **Split `FieldMindScreens.kt`** into separate files per screen:
   - `HomeScreen.kt`
   - `ObserveScreen.kt` + `FieldModeScreen.kt`
   - `ProjectsScreen.kt` + sub-panels
   - `KnowledgeLibraryScreen.kt` + sub-panels
   - `InsightsScreen.kt`
   - `DetailScreen.kt` → split into per-entity screens
   - `FieldMindSettingsScreen.kt`
   - `FlashcardSessionScreen.kt`
   - `ArchiveScreen.kt` (search)
   - `BackupExportScreen.kt`
   - Dialogs into separate files

2. **Add Hilt/Dagger** for dependency injection:
   - Create `@HiltViewModel` annotation for ViewModels
   - Create database and repository modules
   - Inject dependencies via constructor

3. **Migrate Settings to DataStore**:
   - Replace SharedPreferences with Proto DataStore
   - Add type-safe property delegates
   - Migrate existing preferences

4. **Add FTS (Full-Text Search)** table:
   - Create FTS entities for observations, notes, questions, sources
   - Update DAO with FTS queries
   - Add relevance scoring

5. **Fix tag sync**:
   - Remove redundant comma-separated string field from entities
   - Use only cross-reference tables
   - Add migration to clean up existing data

### Phase 2: Navigation & Core UX (Week 3-4)

6. **Redesign navigation** with nested graphs:
   - Create `DashboardNavGraph`, `CaptureNavGraph`, `WorkspaceNavGraph`, `LibraryNavGraph`
   - Implement proper back stack management
   - Add deep link support

7. **Separate detail screens** per entity type:
   - `ObservationDetailScreen.kt`
   - `NoteDetailScreen.kt`
   - `QuestionDetailScreen.kt`
   - `HypothesisDetailScreen.kt`
   - `ProjectDetailScreen.kt`
   - `SourceDetailScreen.kt`
   - `DataRecordDetailScreen.kt`
   - `ReportDetailScreen.kt`

8. **Add undo/redo** for edits:
   - Implement command pattern for entity modifications
   - Store edit history in memory (last 20 actions)
   - Add undo snackbar after each edit

9. **Add map view** for observations:
   - Integrate Google Maps or Mapbox
   - Plot observations as markers
   - Add clustering for dense areas
   - Tap marker to open detail

10. **Add timeline view** across all entities:
    - Create `TimelineScreen.kt`
    - Sort all entities by timestamp
    - Add filter chips for entity type
    - Add date range picker

### Phase 3: Data & Intelligence (Week 5-6)

11. **Implement auto-backup** with WorkManager:
    - Create `BackupWorker` extending `CoroutineWorker`
    - Schedule periodic backup (daily/weekly)
    - Add backup destination selection
    - Show backup status in Settings

12. **Add streak tracking** logic:
    - Calculate consecutive days with observations
    - Store streak data in Settings
    - Display streak counter on Home screen
    - Add streak milestones and celebrations

13. **Add notification reminders**:
    - Create `ReminderWorker` for daily prompts
    - Allow user to set reminder time
    - Send notification with observation count for the day
    - Snooze/dismiss functionality

14. **Fix AI assistant**:
    - Add proper error handling with user-friendly messages
    - Add loading indicators during API calls
    - Implement streaming for better UX
    - Add retry logic with exponential backoff
    - Create error state composable

15. **Implement spaced repetition** for flashcards:
    - Implement SM-2 algorithm
    - Add quality rating buttons (Again, Hard, Good, Easy)
    - Update `nextReviewAt` based on rating
    - Show review queue on Home screen

### Phase 4: Polish & Advanced Features (Week 7-8)

16. **Add chart library** (Vico):
    - Replace custom Canvas charts with Vico
    - Add interactive tooltips
    - Add tap-to-drill on data points
    - Add chart animations

17. **Add shared element transitions**:
    - Define shared element keys for entity cards
    - Add `AnimatedContentScope` transitions
    - Smooth navigation between list and detail

18. **Add skeleton loaders**:
    - Create skeleton composables for each screen
    - Show skeletons while data loads
    - Fade to real content when ready

19. **Add bulk operations**:
    - Add selection mode to list screens
    - Multi-select with checkboxes
    - Bulk delete, tag, archive actions
    - Select all / deselect all

20. **Add templates** for observations and reports:
    - Create template entity in database
    - Pre-fill forms from templates
    - Allow users to save observations as templates
    - Template library in Settings

### Phase 5: Scale (Week 9+)

21. **Cloud sync** architecture:
    - Design sync protocol (CRDT or last-write-wins)
    - Add Firebase/Supabase backend
    - Implement conflict resolution
    - Add offline-first with sync queue

22. **Export to reference managers**:
    - Add Zotero export (RIS/BibTeX format)
    - Add Mendeley export
    - Add citation formatting (APA, MLA, Chicago)

23. **Collaboration features**:
    - Add project sharing via link
    - Real-time collaboration on reports
    - Comment system on observations
    - Activity feed for shared projects

24. **Advanced analytics**:
    - Observation frequency heatmaps
    - Category distribution charts
    - Confidence level trends
    - Research progress metrics

---

## 7. Priority Matrix

| Priority | Feature | Effort | Impact |
|----------|---------|--------|--------|
| 🔴 P0 | Split monolithic screens file | Medium | High |
| 🔴 P0 | Add DI (Hilt) | Medium | High |
| 🔴 P0 | Fix AI assistant error handling | Low | High |
| 🔴 P0 | Add map view for observations | Medium | High |
| 🔴 P0 | Implement auto-backup worker | Low | High |
| 🟡 P1 | Redesign navigation | High | High |
| 🟡 P1 | Separate detail screens | High | Medium |
| 🟡 P1 | Migrate to DataStore | Low | Medium |
| 🟡 P1 | Add FTS for search | Low | Medium |
| 🟡 P1 | Implement streaks/reminders | Medium | Medium |
| 🟢 P2 | Add chart library | Medium | Medium |
| 🟢 P2 | Add templates | Medium | Medium |
| 🟢 P2 | Cloud sync | High | High |
| 🟢 P2 | Collaboration | High | Medium |

---

## 8. June 2026 Follow-Up Implementation Status

This section compares the original analysis against the current codebase and records what has now been converted from placeholder UI into runnable app logic.

### Implemented since the original audit

| Original finding | Current status | Code area |
|------------------|----------------|-----------|
| AI assistant compile/runtime fragility | The request helper now supports retrying network/server failures while keeping trailing-lambda call sites valid. | `data/ai/GeminiResearchAssistant.kt` |
| Flashcard spaced repetition was basic | SM-2 scheduling now has a dedicated engine, per-card scheduling fields, and review rating controls. | `data/flashcard/SM2Engine.kt`, `presentation/screens/FlashcardSessionScreen.kt` |
| Map view missing despite GPS data | Insights and observation detail now expose an offline mini-map preview from saved coordinates. | `presentation/screens/InsightsScreen.kt`, `presentation/components/FieldMindCharts.kt` |
| Auto-backup was a settings-only toggle | Auto-backup now schedules WorkManager and writes rotating private JSON archives under app-private storage. | `data/background/FieldMindBackgroundScheduler.kt`, `data/background/FieldMindAutoBackupWorker.kt`, `data/settings/FieldMindSettings.kt` |
| Reminders/streaks were settings-only toggles | Reminders now schedule WorkManager notifications that skip days with existing observations; the dashboard streak now calculates consecutive observation days instead of lifetime distinct days. | `data/background/FieldMindReminderWorker.kt`, `data/stats/FieldMindStreaks.kt`, `presentation/screens/FieldMindScreens.kt` |

### Still remaining after this pass

| Remaining item | Why it remains | Recommended next step |
|----------------|----------------|-----------------------|
| Split the 260K+ character `FieldMindScreens.kt` | This is high-risk because many composables share private helpers and state. | Move one feature at a time behind identical public composable signatures, starting with Settings and Backup/Export. |
| Dependency injection | Adding Hilt changes app startup, generated code, and tests. | Introduce lightweight provider interfaces first, then migrate to Hilt once feature seams are stable. |
| DataStore settings migration | Requires migration/compatibility handling for existing SharedPreferences keys. | Add Preferences DataStore migration preserving current keys and defaults. |
| Full FTS search | Requires Room schema entities and migrations. | Add FTS virtual tables for observations, notes, questions, and sources with relevance-ranked DAO queries. |
| Privacy lock | Needs a launch/sensitive-screen gate plus biometric/PIN fallback UX. | Implement BiometricPrompt with a non-destructive opt-out path if authentication fails. |
| Attachment offline copy | Requires storage quotas, cleanup, and URI persistence behavior. | Copy selected evidence to app-private storage and keep original URI as provenance metadata. |
| Interactive map provider | Current mini-map is offline and dependency-free, not a full map SDK. | Add a dedicated Map screen with clustering once a provider/privacy policy is chosen. |

---

## Summary

The FieldMind app has a **solid data model** and **good conceptual foundation** (research workflow: Observe → Question → Hypothesize → Collect Data → Analyze → Report). However, it suffers from:

1. **One massive 260K+ character file** that's unmaintainable
2. **Multiple placeholder features** (local model, privacy lock, auto-backup, streaks, reminders) that have settings but no implementation
3. **No dependency injection** making testing impossible
4. **Overloaded ViewModels and screens** that handle too many concerns
5. **Missing map view** despite having GPS data
6. **Basic search** without FTS
7. **Inconsistent design system** with mixed spacing and radius values

The highest-impact improvements would be splitting the monolithic file, adding DI, implementing the placeholder features, and adding a map view.

---

*This analysis was generated on June 12, 2026 by analyzing the full FieldMind codebase.*
