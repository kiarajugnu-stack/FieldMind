# 🔬 FieldMind App — Comprehensive Analysis & Restructuring Plan

> **Date:** June 13, 2026 (Full Update)
> **Scope:** Complete codebase re-analysis covering architecture, all implemented features, UI/UX assessment aligned with `prompt.md` design philosophy, missing research-oriented features, and a prioritized restructuring + redesign plan.
> **Branch:** `main` (up to date with `origin/main`)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Implemented & Working Features](#2-implemented--working-features)
3. [🔴 Critical Issues](#3-critical-issues)
4. [🟡 Code Quality Concerns](#4-code-quality-concerns)
5. [🎨 UI/UX Audit — Current State vs. prompt.md Vision](#5-uiux-audit--current-state-vs-promptmd-vision)
6. [🔵 Missing Features — New Requirements](#6-missing-features--new-requirements)
7. [🔄 Duplicate / Fragmented Implementations](#7-duplicate--fragmented-implementations)
8. [📱 Screen-by-Screen Redesign Plan](#8-screen-by-screen-redesign-plan)
9. [🔧 Full Restructuring Plan](#9-full-restructuring-plan)
10. [Priority Matrix](#10-priority-matrix)
11. [Key Metrics](#11-key-metrics)

---

## 1. Architecture Overview

### Data Layer

```
features/field/data/
├── ai/GeminiResearchAssistant.kt          # Google Gemini + OpenAI providers
├── background/
│   ├── FieldMindAutoBackupWorker.kt       # WorkManager auto-backup (active)
│   ├── FieldMindReminderWorker.kt         # WorkManager daily reminder
│   └── FieldMindBackgroundScheduler.kt    # Central WorkManager wiring
├── database/
│   ├── FieldMindDatabase.kt               # Room database (v6, 19 entities)
│   ├── dao/FieldMindDao.kt                # All DAOs in one file (60+ queries)
│   └── entity/FieldEntities.kt            # All 19 entity types in one file
├── export/FieldMindExport.kt              # Export (Markdown, CSV, JSON, PNG, SVG, PDF)
├── flashcard/SM2Engine.kt                 # SM-2 spaced repetition algorithm
├── learn/LearnLibrary.kt                  # Offline learning resources
├── location/FieldLocationProvider.kt      # GPS & geocoding (on-demand only)
├── repository/FieldMindRepository.kt      # Single repository (70+ methods)
├── security/FieldMindPrivacyManager.kt    # BiometricPrompt implementation ✅
├── settings/FieldMindSettings.kt          # SharedPreferences wrapper (33 keys)
└── stats/FieldMindStreaks.kt              # Streak calculation logic
```

### Presentation Layer

```
features/field/presentation/
├── components/
│   ├── FieldMindCameraCapture.kt          # CameraX — basic capture only
│   ├── FieldMindCharts.kt                 # Canvas-based charts + OSM map
│   ├── FieldMindComponents.kt             # Shared UI (headers, chips, cards, inputs)
│   └── FieldMindIcons.kt                  # Icon definitions
├── navigation/FieldMindNavigation.kt      # Nav graph + bottom bar + rail
├── screens/
│   ├── FieldMindScreenUtils.kt            # Shared constants + utilities
│   ├── FieldMindHomeScreen.kt             # Today / Home dashboard
│   ├── FieldMindObserveScreen.kt          # Capture + Field mode
│   ├── FieldMindProjectsScreen.kt         # Workspace (5 tabs)
│   ├── FieldMindLibraryScreen.kt          # Knowledge library + learn
│   ├── FieldMindArchiveScreen.kt          # Search archive
│   ├── FieldMindBackupExportScreen.kt     # Export studio
│   ├── FieldMindSettingsScreen.kt         # Settings hub + sub-pages
│   ├── FieldMindDetailScreen.kt           # Entity detail + backlinks
│   ├── FieldMindDialogs.kt               # All create/edit dialogs + forms
│   ├── FieldMindMapScreen.kt              # Full-screen OSM map
│   ├── FieldMindLockScreen.kt             # Biometric privacy lock ✅
│   ├── FieldMindOnboardingScreen.kt       # Onboarding flow
│   ├── InsightsScreen.kt                  # Insights/composables/charts
│   └── FlashcardSessionScreen.kt          # Flashcard review
├── theme/FieldMindTheme.kt                # Brand palette + semantic colors
└── viewmodel/FieldMindViewModel.kt        # Single ViewModel for everything
```

### What's Working Well (Unchanged)

- **19 Room entities** with 9 cross-reference tables — comprehensive data model
- **Reactive data layer** via `Flow<List<T>>` from DAO through Repository to ViewModel
- **Soft-delete pattern** (`archivedAt`, `deletedAt`) across all entities
- **Canvas-based charts** — bar, line, donut, knowledge graph, mini-map — fully offline
- **OSM map integration** via osmdroid with markers and GPS overlay
- **Navigation** with bottom bar (phone) + rail (tablet), animated transitions, haptic feedback
- **Brand theme** with forest-green palette, semantic entity colors, dynamic Material You
- **SM-2 spaced repetition** fully implemented with ease factor, intervals, deck modes
- **AI assistant** with Gemini + OpenAI, retry logic, error handling
- **Auto-backup** with WorkManager, rotating JSON archives, pruning
- **Export suite** — Markdown, CSV, JSON, HTML, PNG, SVG, PDF
- **Privacy lock** — BiometricPrompt fully implemented with fallback ✅
- **Achievement system** with 10 milestones
- **Onboarding flow** with 7-step intro

---

## 2. ✅ Implemented & Working Features

### 2.1 Privacy Lock — ✅ Fully Implemented (Updated)

Previous analysis noted this as a placeholder. Since then:

- `FieldMindPrivacyManager.kt` implements full `BiometricPrompt` with `CryptoObject`
- `FieldMindLockScreen.kt` provides full-screen lock with biometric and device credential fallback
- Settings toggle in `FieldMindSettingsScreen.kt` → Backup & Import page
- Navigation integration — lock gates the entire app on launch
- PIN fallback via `KeyguardManager` device credentials

**Status:** Production-ready.

### 2.2 CameraX — Basic Implementation Only

Current `FieldMindCameraCapture.kt` provides:
- Live preview (3:4 aspect ratio, 28dp rounded corners)
- Flash toggle (Off → On → Auto cycle)
- Front/rear camera switch
- Photo saved to app-private storage + MediaStore

**Missing per prompt.md:**
- ❌ Not full screen (contained in 3:4 box with padding)
- ❌ No pinch-to-zoom
- ❌ No tap-to-focus
- ❌ No grid overlay (rule-of-thirds)
- ❌ No capture timer (3s/5s/10s)
- ❌ No aspect ratio toggle (4:3/16:9/1:1)
- ❌ No quick annotation after capture
- ❌ No observation tagging after capture
- ❌ No auto GPS/weather metadata on capture
- ❌ No post-capture bottom sheet with "Add to Observation" / "Add Question" / "Just Save"

### 2.3 Capture Flow — Form-Heavy (Not Research-Oriented)

Current `ObserveScreen` flow:
```
Quick capture button
  → Choose mode (Snap / Note)
  → [Snap path] Choose category (REQUIRED) → Full observation form
  → [Note path] Full note form with category, links, attachments
```

**Problems per prompt.md:**
- Category selection happens BEFORE thinking (forced classification)
- Full form requires subject, category, confidence, facts, location, evidence, tags, project link
- No "under 15 seconds" capture option
- Field mode is a slightly compacted version of the same form
- No post-capture "What do you want to do next?" prompt
- No one-tap quick capture

### 2.4 Workspace (Projects Screen) — 5-Tab Form Hub

Current `ProjectsScreen` has 5 tabs: Projects, Questions, Hypotheses, Data, Reports

**Problems:**
- Each tab starts with a creation form that's heavy on fields
- `ResearchFlowGuide` (Guided research flow) forces sequential Questions → Hypotheses → Data → Reports
- Project creation form has 11 fields (title, topic, objective, question, background, methods, hypothesis, data plan, analysis, conclusion, next action)
- Per prompt.md: Projects should be "Name + Question → Create. Everything else added after."
- Guided flow should be removed — user wants freeform research, not wizard-driven

### 2.5 Settings — Privacy Lock in Wrong Location

Currently, privacy lock toggle is inside **Backup & Import** settings page:
```kotlin
// BackupImportSettingsPage
ToggleItem("Privacy lock", "Requires authentication to access settings and export.", privacy, settings::setPrivacyLockEnabled, FieldMindIcons.Lock)
```

Per user request: Privacy/security lock should be in its **own dedicated Security section** in settings, not buried in Backup & Import.

---

## 3. 🔴 Critical Issues

### 3.1 Monolithic Screens File — Partially Resolved

The original 262K+ character `FieldMindScreens.kt` was split into 12 files in Phase 2. However:
- `FieldMindDialogs.kt` is **967 lines** and growing
- `FieldMindLibraryScreen.kt` is **620 lines**
- `FieldMindObserveScreen.kt` is **690 lines** — needs splitting further for redesign
- The navigation wildcard import pattern makes dependency tracking harder

### 3.2 No Dependency Injection — Unchanged 🔴

`FieldMindViewModel` extends `AndroidViewModel` and directly instantiates repository and settings. Same issues as before: no unit testing, no swapping, no previews with fakes.

### 3.3 Duplicate Workers — Unchanged 🔴

| Worker | Location | Status |
|--------|----------|--------|
| `FieldMindAutoBackupWorker` | `features.field.data.background` | ✅ Active |
| `FieldMindBackupWorker` | `infrastructure.worker` | ❓ Orphaned |
| `FieldMindStreakWorker` | `infrastructure.worker` | ❓ Orphaned |

### 3.4 Settings Init Side-Effects — Unchanged 🔴

`FieldMindSettings.init` calls `FieldMindBackgroundScheduler.syncAll()`. Every `getInstance()` re-schedules background jobs.

### 3.5 Full-Text Search Uses `LIKE '%query%'` — Unchanged 🟡

No FTS virtual tables. All 6 search methods scan entire tables.

---

## 4. 🟡 Code Quality Concerns

| Issue | Severity | Notes |
|-------|----------|-------|
| Single ViewModel (god object) | 🟡 High | 40+ methods, 9 entity flows, export logic, archive parsing |
| Single DAO (60+ queries) | 🟡 High | Should be split per-feature |
| Single entities file (19 types) | 🟡 Medium | Should be grouped by feature |
| Single repository (70+ methods) | 🟡 Medium | One-stop-shop pattern |
| Tag sync can drift | 🟡 Medium | Comma-string + cross-ref table out of sync |
| Settings uses SharedPreferences | 🟡 Medium | 33 keys, manual boilerplate, no DataStore |
| Database uses destructive fallback | 🟡 High | Schema changes destroy user data |
| Inconsistent card radius | 🟢 Low | 18dp to 28dp range |
| No loading/error states in screens | 🟡 Medium | Missing skeleton loaders, error cards |
| OpenAI uses v1/responses endpoint | 🟡 Medium | May not support all models |

---

## 5. 🎨 UI/UX Audit — Current State vs. prompt.md Vision

### 5.1 Home Screen

| Aspect | Current State | prompt.md Vision | Gap |
|--------|--------------|-----------------|-----|
| Icon size | 32dp | 64×64dp minimum | 🔴 Major |
| Label size | titleSmall | Larger, bolder | 🟡 Medium |
| Tap targets | Standard Material | 64×64dp minimum | 🔴 Major |
| Feel | Dashboard toolbar | Open field journal | 🔴 Major |
| Active state | Basic clickable | Subtle ripple/active state | 🟡 Medium |

### 5.2 Capture Screen

| Aspect | Current State | prompt.md Vision | Gap |
|--------|--------------|-----------------|-----|
| Entry point | "Quick capture" button | Camera-first, evidence-first | 🔴 Major |
| Category | REQUIRED before writing | Optional, collapsed, after thoughts | 🔴 Major |
| Required fields | Subject + Facts required | No required fields | 🔴 Major |
| Post-capture | None (form closes) | "What do you want to do next?" prompt | 🔴 Major |
| Time to first capture | ~30+ seconds | Under 15 seconds | 🔴 Major |
| Advanced metadata | Always visible | Collapsed by default | 🟡 Medium |

### 5.3 CameraX

| Aspect | Current State | prompt.md Vision | Gap |
|--------|--------------|-----------------|-----|
| Layout | 3:4 box with 28dp corners | True full screen, edge-to-edge | 🔴 Major |
| Zoom | None | Pinch-to-zoom + slider | 🔴 Major |
| Focus | None | Tap to focus with animated ring | 🔴 Major |
| Grid | None | Rule-of-thirds toggle | 🟡 Medium |
| Timer | None | 3s/5s/10s countdown | 🟡 Medium |
| Aspect ratio | Fixed 3:4 | 4:3/16:9/1:1 toggle | 🟡 Medium |
| Post-capture | None | Bottom sheet: "Add to Observation" / "Add Question" / "Just Save" | 🔴 Major |
| Annotation | None | Draw/annotate on photo before save | 🟡 Medium |
| Auto metadata | None | GPS + timestamp + weather on capture | 🔴 Major |

### 5.4 Workspace (Projects)

| Aspect | Current State | prompt.md Vision | Gap |
|--------|--------------|-----------------|-----|
| Project creation | 11-field form | Name + Question → Create | 🔴 Major |
| Tab structure | 5 tabs (Projects/Questions/Hypotheses/Data/Reports) | Unified, progressive disclosure | 🟡 Medium |
| Guided flow | ResearchFlowGuide forces sequence | Removed — freeform | 🔴 Major |
| Questions | Inside Workspace tab | First-class objects with own screen | 🔴 Major |

### 5.5 Notes Screen

| Aspect | Current State | prompt.md Vision | Gap |
|--------|--------------|-----------------|-----|
| Entry | Category selection first | Start writing immediately | 🟡 Medium |
| Required fields | Title OR body | None required | 🟡 Medium |
| Category | Prominent chips | Optional metadata | 🟡 Medium |

### 5.6 Insights Screen

| Aspect | Current State | prompt.md Vision | Gap |
|--------|--------------|-----------------|-----|
| Content | Charts + achievements + graph | "What am I discovering?" patterns | 🟡 Medium |
| Empty states | Widget placeholders | Guided prompts | 🟡 Medium |
| Patterns | None | Repeated observations, themes | 🔴 Major |

### 5.7 Global Issues

| Issue | Status |
|-------|--------|
| Category chip overload | 🔴 Every screen has large chip groups — needs 70% reduction |
| Visual hierarchy | 🟡 Inconsistent priority ordering per entity type |
| Connections as core | 🟡 Linking exists but isn't prominent enough |
| Empty states | 🟡 Passive ("No X yet") instead of guided prompts |

---

## 6. 🔵 Missing Features — New Requirements

### 6.1 Real Weather Report (GPS-Based, Background)

**User Request:** Add automatic real weather reports using GPS — not in quick snap, but as a background feature. GPS should not track always.

**Implementation Plan:**
- Use Android's `FusedLocationProviderClient` with `PRIORITY_BALANCED_POWER_ACCURACY`
- On-demand GPS fix only when: (a) user starts a new observation, (b) user taps "Get weather", (c) periodic background check (once per 6 hours, not continuous)
- Integrate with Open-Meteo API (free, no API key needed) for weather data:
  - Current temperature, humidity, wind speed/direction
  - Cloud cover, precipitation, visibility
  - UV index, pressure
- Store weather snapshot with each observation (optional, auto-attached)
- Show current weather on Home screen and in observation detail
- Use WorkManager for periodic weather cache (not continuous GPS)
- Weather data stored locally, never sent anywhere

**New Entity Fields:**
```kotlin
// ObservationEntity additions
val weatherTemperature: Double? = null,
val weatherCondition: String = "",
val weatherHumidity: Int? = null,
val weatherWindSpeed: Double? = null,
val weatherWindDirection: String = "",
val weatherCloudCover: Int? = null,
val weatherSnapshotAt: Long? = null
```

**New Settings:**
```kotlin
val autoWeatherEnabled: StateFlow<Boolean>  // default: false
val weatherProvider: StateFlow<String>       // "open-meteo" (only option initially)
```

### 6.2 GPS Tracking — On-Demand Only

**User Request:** GPS should NOT track continuously. Only on-demand.

**Current State:** Already mostly on-demand via `FieldLocationProvider.requestCurrentLocation()`. GPS is requested per-observation, not continuously.

**Improvements Needed:**
- Remove any possibility of continuous tracking
- Add explicit "GPS mode" settings: "Off" / "On capture only" / "Background weather only"
- Show GPS indicator only when actively fixing, not as a persistent status
- Cache last-known location for 30 minutes to avoid repeated fixes
- Battery impact disclosure in settings

### 6.3 CameraX — Full Feature Set

**User Request:** CameraX needs more features.

**Full Feature List (per prompt.md):**

| Feature | Implementation |
|---------|---------------|
| Full screen | `WindowCompat.setDecorFitsSystemWindows(window, false)`, immersive sticky |
| Pinch-to-zoom | `CameraControl.startZoom()` with gesture detection |
| Tap to focus | `MeteringPointFactory` + `cameraControl.startFocusAndMetering()` |
| Flash toggle | Already implemented (Off/On/Auto) ✅ |
| Front/rear switch | Already implemented ✅ |
| Grid overlay | Optional Canvas overlay with rule-of-thirds lines |
| Capture timer | Countdown animation → delayed capture |
| Aspect ratio | Toggle between `ImageCapture aspect ratios` |
| Quick annotation | `Canvas` overlay on captured bitmap for drawing |
| Observation tag | Bottom sheet after capture: "Tag to observation" |
| Auto metadata | GPS + timestamp + weather snapshot on capture |
| Post-capture flow | Bottom sheet: `[ Add to Observation ] [ Add Question ] [ Just Save ]` |

### 6.4 Offline Automatic Question Preparation

**User Request:** Automatic offline question preparation.

**Implementation Plan:**
- After each observation is saved, analyze the observation content offline
- Generate 2-3 potential research questions based on:
  - The subject and category
  - Confidence level ("Needs Verification" → auto-generate verification questions)
  - Field context (weather, time of day → environmental questions)
  - Tags and connections to existing questions
- Show generated questions in a "Suggested Questions" card on the observation detail screen
- User can tap to add any suggestion as a real question
- No AI provider needed — use rule-based NLP:
  - Pattern matching on subject words
  - Category-specific question templates
  - Confidence-based follow-up prompts
  - Time-of-day contextual questions

**Offline Question Generation Rules:**
```
IF confidence == "Needs Verification" → "Is [subject] correctly identified as [category]?"
IF category == "Bird" AND time == "Dawn" → "Does [subject] appear more active at dawn?"
IF category == "Plant" AND tags contain "flowering" → "What pollinators visit [subject]?"
IF weather is rainy → "How does rain affect [subject] activity?"
IF subject observed at same location repeatedly → "Is [subject] resident or migratory?"
```

### 6.5 Multiple Observations — Research Session Mode with Timer

**User Request:** Let user add multiple observations like the research way with timer.

**Implementation Plan — "Research Session" Mode:**
- New screen: `ResearchSessionScreen`
- User starts a session → optional session name, linked project, location
- Session has a running timer (visible at top)
- Within session, user can add multiple observations rapidly:
  - Quick subject + facts entry
  - One-tap category
  - Auto-timestamp and auto-GPS per observation
  - Photos attach directly to current observation
- Timer pauses when app goes to background (configurable)
- Session summary at end: all observations, time spent, photos taken
- Session can be linked to a project
- Historical sessions viewable in Workspace

**New Entity:**
```kotlin
@Entity(tableName = "field_research_sessions")
data class ResearchSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val projectId: Long? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val totalDurationMs: Long = 0,
    val observationCount: Int = 0,
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "Active", // Active, Paused, Completed
    val notes: String = "",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

**Session Observation Link:**
```kotlin
@Entity(tableName = "field_session_observations", primaryKeys = ["sessionId", "observationId"])
data class SessionObservationCrossRef(val sessionId: Long, val observationId: Long)
```

### 6.6 Remove Guided Research Flow

**User Request:** The guided research flow needs to be removed.

**Current State:** `ResearchFlowGuide` composable in `FieldMindProjectsScreen.kt` shows a 4-step wizard (Questions → Hypotheses → Data → Reports) with "Next" buttons.

**Action:** Remove `ResearchFlowGuide` entirely. Replace with contextual empty states that don't force sequence:
- Empty Questions: "What do you want to find out?" + [Add Question] button
- Empty Hypotheses: "Got a prediction? Test it." + [Add Hypothesis] button
- Empty Data: "Ready to measure or count?" + [Add Data Record] button
- Empty Reports: "Time to write up findings." + [Build Report] button

### 6.7 Privacy Lock as Separate Security Section

**User Request:** Privacy lock needs to be added in a separate section like security.

**Current State:** Privacy lock toggle is inside `BackupImportSettingsPage`.

**Action:**
- Create new `SecuritySettingsPage` composable
- Move privacy lock toggle there
- Add additional security features:
  - App lock timeout (immediate / 1 min / 5 min / 15 min / when screen off)
  - Lock type (biometric / PIN / both)
  - Data encryption status
  - Auto-lock on background toggle
- Add "Security" nav card in main Settings hub
- Remove privacy lock from Backup & Import page

### 6.8 Quick Capture Redesign — Research-Oriented

**User Request:** Quick capture and workspace need to be fully redesigned as a research way, not just like filling a form.

**New Capture Flow (per prompt.md):**
```
Take evidence (camera / audio / sketch)
     ↓
Write facts (single text field, no required fields)
     ↓
Save
     ↓
"What do you want to do next?"
   [ Add Question ]  [ Add Hypothesis ]  [ Continue Observing ]  [ Add To Project ]
```

- Advanced metadata (category, confidence, location, tags) → optional, collapsed by default
- Capture must take under 15 seconds for the happy path
- One-tap quick capture: camera button → photo → auto-timestamp → saved
- Category inferred from context or user's default, not forced selection
- Subject auto-generated from first line of facts if not provided

### 6.9 Workspace Redesign — Research Workspace

**New Workspace Flow (per prompt.md):**
- Project creation: Name + Research Question → Create. Done.
- Everything else (hypothesis, methods, data, reports) added after creation as cards
- Tabs simplified: Projects | Evidence | Analysis
  - Projects: list of active investigations
  - Evidence: unified view of observations, notes, sources
  - Analysis: hypotheses, data tools, reports
- Remove 11-field creation form
- Projects become containers for investigations, not forms

---

## 7. 🔄 Duplicate / Fragmented Implementations

### 7.1 Backup Workers — Unchanged

| Aspect | `FieldMindAutoBackupWorker` | `FieldMindBackupWorker` |
|--------|---------------------------|------------------------|
| Package | `features.field.data.background` | `worker` (infrastructure) |
| Scheduling | Via `FieldMindBackgroundScheduler` | Not wired to settings |
| Storage | `filesDir/fieldmind/backups/` | `getExternalFilesDir(null)/FieldMindBackups/` |
| Pruning | Keeps last 8 | Keeps last 7 |
| Settings aware | Yes | No |

**Recommendation:** Remove `FieldMindBackupWorker`.

### 7.2 Streak Tracking — Unchanged

| Aspect | `FieldMindStreaks` | `FieldMindStreakWorker` |
|--------|-------------------|------------------------|
| Calculation | Computed from observation dates | SharedPreferences-based |
| Used by | Reminder worker + dashboard | Nothing |

**Recommendation:** Remove `FieldMindStreakWorker`.

### 7.3 Reminder Implementations

Both `FieldMindReminderWorker` (feature) and `FieldMindStreakWorker` (infrastructure) send reminder-type notifications. Could conflict.

---

## 8. 📱 Screen-by-Screen Redesign Plan

### 8.1 Home Screen

**Current:** Dashboard with widget grid, daily goal, quick actions, recent activity.

**Redesign:**
- Increase all nav item icon sizes to 48dp minimum (64dp ideal)
- Increase label sizes and weights
- Add subtle active state animations
- Make the daily goal card the hero element
- Add weather widget (when weather feature is implemented)
- Simplify recent activity to a timeline view
- Add "Start Research Session" prominent CTA
- Empty state: "Go outside. Notice something." → [Tap Capture]

### 8.2 Capture Screen — Full Redesign

**Current:** Form with categories, chips, multiple required fields.

**Redesign:**
```
┌─────────────────────────────────────┐
│  [📷 Camera]  [🎤 Audio]  [📝 Note] │  ← Three big evidence buttons
│                                     │
│  ┌─────────────────────────────┐    │
│  │  What did you observe?      │    │  ← Single large text field
│  │  (start typing...)          │    │     No required fields
│  └─────────────────────────────┘    │
│                                     │
│  ▼ Details (collapsed)              │  ← Optional, expandable
│    Category: [chips]                │
│    Confidence: [chips]              │
│    Location: [GPS] [Manual]         │
│    Tags: [text field]               │
│                                     │
│  [ Save Observation ]               │  ← Prominent save
│                                     │
│  After save:                        │
│  "What's next?"                     │
│  [ Add Question ] [ Add Hypothesis ]│
│  [ Keep Observing ] [ Add to Project]│
└─────────────────────────────────────┘
```

### 8.3 Workspace Screen — Full Redesign

**Current:** 5-tab form hub with guided flow.

**Redesign:**
```
Tab 1: Projects
  - List of project cards with progress indicators
  - "New Project" → Name + Question → Create
  - Each project card shows: linked observations count, open questions, recent activity

Tab 2: Evidence (NEW — merges observations, notes, sources)
  - Unified chronological list
  - Filter by type, project, date
  - Quick-add buttons for each type

Tab 3: Analysis (NEW — merges hypotheses, data, reports)
  - Hypothesis cards with confidence meters
  - Data tool cards with latest values
  - Report pipeline (Draft → Review → Final)
```

**Remove:** `ResearchFlowGuide` composable entirely.

### 8.4 CameraX — Full Redesign

**Current:** Basic 3:4 preview box with flash/flip buttons.

**Redesign:**
- Full-screen immersive preview (edge-to-edge, no system bars)
- Overlay controls:
  - Top: Flash toggle | Grid toggle | Timer selector | Aspect ratio
  - Bottom: Gallery preview | Capture button | Flip camera
- Tap-to-focus with animated ring
- Pinch-to-zoom with slider
- Post-capture bottom sheet:
  ```
  [Thumbnail] Photo saved.
  ──────────────────────────
  [ Add to Observation ]  [ Add Question ]  [ Just Save ]
  ```
- Auto-attach GPS + timestamp + weather metadata

### 8.5 Settings Screen — Security Section

**Current:** Privacy lock buried in Backup & Import.

**Redesign:**
```
Settings Hub:
  ├── Research profile
  ├── Appearance
  ├── Capture defaults
  ├── Security (NEW)
  │   ├── App lock (biometric/PIN)
  │   ├── Lock timeout
  │   ├── Auto-lock on background
  │   └── Data encryption status
  ├── AI assistant
  ├── Local model
  ├── Backup & import
  ├── Export Studio
  └── About
```

### 8.6 Insights Screen

**Current:** Charts + achievements + knowledge graph.

**Redesign:**
- Focus on "What am I discovering?"
- Show patterns: repeated observations, frequent subjects, emerging themes
- Research progress per project
- Hide advanced analytics until meaningful data exists
- Replace empty widgets with guided prompts
- Add weather trends (when weather feature is implemented)

### 8.7 Notes Screen

**Current:** Starts with category selection.

**Redesign:**
```
Title (optional)
──────────────────
Body (start writing immediately, no barriers)
──────────────────
[ Tags ]  [ Link to... ]  [ Save ]
```
- Category becomes optional metadata, not a required first step
- Not visually dominant

---

## 9. 🔧 Full Restructuring Plan

### Phase 1: Critical Fixes (Immediate — Week 1)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 1 | **Remove duplicate workers** — canonical backup + streak, remove infrastructure duplicates | Low | 🔴 Critical |
| 2 | **Fix settings init side-effects** — move `syncAll()` out of init block | Low | 🔴 Critical |
| 3 | **Fix database destructive fallback** — add migration steps or schema export | Low | 🟡 High |
| 4 | **Fix OpenAI v1/responses endpoint** — add v1/chat/completions fallback | Low | 🟡 High |

### Phase 2: Security & Weather (Week 2-3)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 5 | **Create Security settings page** — move privacy lock, add lock timeout, auto-lock | Medium | 🟡 High |
| 6 | **Integrate Open-Meteo weather API** — on-demand weather snapshots with GPS | Medium | 🟡 High |
| 7 | **Add weather fields to ObservationEntity** — temperature, condition, humidity, wind | Medium | 🟡 High |
| 8 | **Add GPS mode settings** — "Off" / "On capture only" / "Background weather only" | Low | 🟡 High |
| 9 | **Add weather widget to Home screen** — current conditions with refresh | Medium | 🟡 Medium |

### Phase 3: CameraX Redesign (Week 3-4)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 10 | **Full-screen immersive camera** — edge-to-edge, no system bars | Medium | 🔴 Major |
| 11 | **Add pinch-to-zoom** — CameraControl.startZoom() with gesture | Medium | 🔴 Major |
| 12 | **Add tap-to-focus** — MeteringPointFactory + focus ring animation | Medium | 🔴 Major |
| 13 | **Add grid overlay** — rule-of-thirds Canvas overlay toggle | Low | 🟡 Medium |
| 14 | **Add capture timer** — 3s/5s/10s countdown with animation | Low | 🟡 Medium |
| 15 | **Add aspect ratio toggle** — 4:3/16:9/1:1 options | Low | 🟡 Medium |
| 16 | **Add post-capture bottom sheet** — "Add to Observation" / "Add Question" / "Just Save" | Medium | 🔴 Major |
| 17 | **Add auto metadata** — GPS + timestamp + weather on capture | Medium | 🔴 Major |

### Phase 4: Capture Redesign (Week 4-5)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 18 | **Redesign capture flow** — evidence-first, no required fields, under 15 seconds | High | 🔴 Major |
| 19 | **Add post-capture "What's next?" prompt** — Question / Hypothesis / Continue / Project | Medium | 🔴 Major |
| 20 | **Make category optional** — collapsed advanced section | Low | 🟡 High |
| 21 | **Add one-tap quick capture** — camera → photo → auto-save | Medium | 🔴 Major |
| 22 | **Add quick annotation** — draw/annotate on captured photo | High | 🟡 Medium |

### Phase 5: Workspace Redesign (Week 5-6)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 23 | **Remove ResearchFlowGuide** — replace with contextual empty states | Low | 🟡 High |
| 24 | **Simplify project creation** — Name + Question → Create | Low | 🔴 Major |
| 25 | **Restructure tabs** — Projects / Evidence / Analysis | High | 🔴 Major |
| 26 | **Add Research Session mode** — timer, multiple observations, session summary | High | 🔴 Major |
| 27 | **Add offline question generation** — rule-based question suggestions from observations | Medium | 🟡 High |

### Phase 6: Capture & Observations — Research Session (Week 6-7)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 28 | **Create ResearchSessionEntity** — session tracking with timer | Medium | 🔴 Major |
| 29 | **Create ResearchSessionScreen** — session UI with running timer | High | 🔴 Major |
| 30 | **Add session-to-observation linking** — cross-reference table | Low | 🟡 High |
| 31 | **Add session summary view** — all observations, time, photos | Medium | 🟡 High |
| 32 | **Add historical sessions list** — in Workspace Evidence tab | Low | 🟡 Medium |

### Phase 7: Architecture Improvements (Week 7-9)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 33 | **Add Hilt/Dagger** for dependency injection | High | 🔴 Critical |
| 34 | **Split FieldMindDao.kt** into per-feature DAOs | Medium | 🟡 High |
| 35 | **Split FieldEntities.kt** into per-feature entity files | Medium | 🟡 High |
| 36 | **Split FieldMindRepository.kt** into per-feature repos | Medium | 🟡 High |
| 37 | **Migrate settings to Proto DataStore** | Medium | 🟡 High |
| 38 | **Add FTS virtual tables** for search | Medium | 🟡 High |

### Phase 8: UI Polish (Week 9-10)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 39 | **Standardize design system** — consistent 20dp card radius, 8dp grid | Low | 🟡 Medium |
| 40 | **Add skeleton loaders** — per-screen loading composables | Low | 🟡 Medium |
| 41 | **Add error states** — error cards with retry actions | Low | 🟡 Medium |
| 42 | **Improve empty states** — guided prompts instead of passive text | Low | 🟡 High |
| 43 | **Reduce chip overload** — 70% fewer required chip groups | Medium | 🟡 High |
| 44 | **Fix visual hierarchy** — Evidence > Facts > Questions > Metadata | Medium | 🟡 Medium |

### Phase 9: Advanced Features (Week 10+)

| # | Task | Effort | Impact |
|---|------|--------|--------|
| 45 | **Add undo/redo** — command pattern for entity edits | Medium | 🟡 Medium |
| 46 | **Add bulk operations** — multi-select, bulk delete/tag/archive | Medium | 🟡 High |
| 47 | **Add timeline view** — unified chronological stream | Medium | 🟡 Medium |
| 48 | **Add templates** — observation/report templates | Medium | 🟡 Medium |
| 49 | **Cloud sync** — design sync protocol, backend, conflict resolution | High | 🟡 High |
| 50 | **Export to Zotero/Mendeley** — RIS/BibTeX format | Medium | 🟢 Low |
| 51 | **Implement local on-device model** — TensorFlow Lite downloader | High | 🟢 Low |

---

## 10. Priority Matrix

### 🔴 P0 — Must Fix Immediately

| Task | Effort | Impact | Status |
|------|--------|--------|--------|
| Remove duplicate workers | Low | Critical | ⚠️ Duplicate code |
| Fix settings init side-effects | Low | Critical | ❌ Existing |
| Fix database destructive fallback | Low | High | ❌ Existing |
| Split FieldMindScreens.kt further | High | Critical | ⚠️ Partially done |
| Add dependency injection (Hilt) | High | Critical | ❌ Not started |

### 🟡 P1 — High Priority (Next 4 Weeks)

| Task | Effort | Impact | Status |
|------|--------|--------|--------|
| Security settings page | Medium | High | ❌ Not started |
| Weather integration (Open-Meteo) | Medium | High | ❌ Not started |
| CameraX full redesign | High | Major | ❌ Basic only |
| Capture flow redesign | High | Major | ❌ Form-heavy |
| Remove guided research flow | Low | High | ❌ Guided wizard exists |
| Workspace redesign (3 tabs) | High | Major | ❌ 5-tab form hub |
| Research Session mode | High | Major | ❌ Not started |
| Offline question generation | Medium | High | ❌ Not started |
| One-tap quick capture | Medium | Major | ❌ Not started |
| Simplify project creation | Low | Major | ❌ 11-field form |
| Migrate to DataStore | Medium | High | ❌ SharedPreferences |
| Add FTS for search | Medium | High | ❌ LIKE '%query%' |

### 🟢 P2 — Medium Priority (Week 5+)

| Task | Effort | Impact | Status |
|------|--------|--------|--------|
| Add undo/redo | Medium | Medium | ❌ Not started |
| Add bulk operations | Medium | High | ❌ Not started |
| Add skeleton loaders | Low | Medium | ❌ Not started |
| Add timeline view | Medium | Medium | ❌ Not started |
| Standardize design system | Low | Medium | ⚠️ Inconsistent |
| Improve empty states | Low | High | ⚠️ Passive text |
| Reduce chip overload | Medium | High | ⚠️ Every screen |
| Quick annotation on photos | High | Medium | ❌ Not started |

### 🔵 P3 — Future (Week 10+)

| Task | Effort | Impact | Status |
|------|--------|--------|--------|
| Cloud sync | High | High | ❌ Not started |
| Zotero/Mendeley export | Medium | Low | ❌ Not started |
| Templates | Medium | Medium | ❌ Not started |
| Local on-device model | High | Low | ❌ Placeholder only |
| Share sheet integration | Low | Medium | ❌ Not started |

---

## 11. Key Metrics

| Metric | Value |
|--------|-------|
| Total entities | 19 (10 data + 9 cross-reference) |
| New entities needed | 2 (ResearchSession + SessionObservationCrossRef) |
| Room database version | 6 |
| Migration strategy | Destructive fallback ⚠️ |
| Settings keys | 33 SharedPreferences keys |
| Screen files | 14 (post Phase 2 split) |
| Backup implementations | 2 duplicate ⚠️ |
| Streak implementations | 2 fragmented ⚠️ |
| AI providers | 2 (Gemini + OpenAI) |
| Export formats | 8 (Markdown, CSV, JSON, HTML, PNG, SVG, PDF, Plain text) |
| Navigation routes | 17+ composable destinations |
| Chart types | 6 (bar, line, donut, breakdown, knowledge graph, mini-map) |
| Achievement milestones | 10 |
| SM-2 implementation | Complete ✅ |
| Privacy lock | Complete ✅ |
| Haptic feedback | Present ✅ |
| Tablet adaptation | Present ✅ |
| Dark mode | Present ✅ |
| Dependency injection | Missing ❌ |
| Unit tests | Missing ❌ |
| Weather integration | Missing ❌ |
| Research sessions | Missing ❌ |
| Full-screen camera | Missing ❌ |
| Camera zoom/focus | Missing ❌ |

---

## 12. Implementation Roadmap Summary

### Weeks 1-2: Foundation
- Remove duplicates, fix init side-effects, fix DB fallback
- Create Security settings page
- Add GPS mode settings

### Weeks 3-4: Camera & Weather
- Full-screen CameraX with zoom, focus, grid, timer, aspect ratio
- Post-capture flow with observation tagging
- Open-Meteo weather integration
- Weather fields in ObservationEntity

### Weeks 5-6: Capture & Workspace Redesign
- Evidence-first capture (under 15 seconds)
- Remove guided research flow
- Simplify project creation
- Restructure workspace to 3 tabs
- Add Research Session mode with timer

### Weeks 7-8: Architecture
- Add Hilt/Dagger
- Split DAO, entities, repository
- Migrate to DataStore
- Add FTS search

### Weeks 9-10: Polish
- Standardize design system
- Add skeleton loaders, error states
- Improve empty states
- Reduce chip overload
- Fix visual hierarchy

### Weeks 10+: Advanced
- Cloud sync, undo/redo, bulk ops, templates, timeline view

---

*This analysis was generated on June 13, 2026 by re-analyzing the full FieldMind codebase on `main` (commit `44561502`), incorporating `prompt.md` design philosophy, user requirements for research-oriented redesign, and field research app best practices from KoBoToolbox, Survey123, and Fulcrum.*

*Key new requirements: real weather reports, on-demand GPS, CameraX full features, offline question preparation, research sessions with timer, workspace redesign, guided flow removal, security section, and capture-first-not-form-first UX.*
