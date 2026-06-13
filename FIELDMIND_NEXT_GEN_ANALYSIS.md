# 🚀 FieldMind Next-Gen Analysis — Features, Insights & Roadmap

> **Date:** June 13, 2026
> **Scope:** Comprehensive competitive analysis, research-backed feature suggestions, insights redesign, architecture improvements, and priority roadmap
> **Branch:** `feat/fieldmind-research-redesign`
> **Research Sources:** KoboToolbox, ODK Collect, Fulcrum, Survey123, iNaturalist, eBird, Seek by iNaturalist, EpiCollect5, QField, Jetpack Compose 2026 ecosystem, Material Design 3

---

## Table of Contents

1. [Competitive Landscape](#1-competitive-landscape)
2. [Current State Audit](#2-current-state-audit)
3. [Impressive New Features](#3-impressive-new-features)
4. [Insights & Analytics Redesign](#4-insights--analytics-redesign)
5. [Data Layer & Architecture Improvements](#5-data-layer--architecture-improvements)
6. [UI/UX Overhaul: Screen-by-Screen](#6-uiux-overhaul-screen-by-screen)
7. [On-Device AI & ML Features](#7-on-device-ai--ml-features)
8. [Accessibility & Internationalization](#8-accessibility--internationalization)
9. [Performance & Offline-First Hardening](#9-performance--offline-first-hardening)
10. [Priority Roadmap](#10-priority-roadmap)
11. [Implementation Guides](#11-implementation-guides)

---

## 1. Competitive Landscape

### Market Analysis

| App | Primary Use Case | Strengths | Weaknesses vs FieldMind |
|:---|:---|:---|:---|
| **KoboToolbox** | Humanitarian/NGO | AI transcription (80+ languages), XLSForm standard, robust server | No offline knowledge graph, no flashcard/SM-2, no canvas charts |
| **ODK Collect** | Technical research | XLSForm, offline entities, advanced geometry | No AI assistant, no rich media annotations, no research session concept |
| **Fulcrum** | Commercial inspections | Drag-drop forms, GIS integration, polished UX | Paid, no flashcard system, no hypothesis management |
| **iNaturalist** | Citizen science | Computer vision species ID, community validation (Research Grade), 2M+ users | No hypothesis testing, no export studio, no project workspace |
| **eBird** | Ornithology | Expert review pipeline, data filters, regional validation | Single-species focus, no general research tools |
| **EpiCollect5** | Public health | Simple setup, multi-device aggregation, longitudinal tracking | No AI, no rich media, no insights/charts |

### Key Competitive Advantages FieldMind Already Has

- ✅ **Offline-first knowledge graph** connecting observations → questions → hypotheses → sources — unique in the market
- ✅ **SM-2 spaced repetition** for flashcard review — no other field research app has this
- ✅ **Canvas-based charts** (bar, line, donut, knowledge graph, OSM map) — fully offline
- ✅ **Research Session** with timer → multi-observation → summary flow
- ✅ **AI assistant** with Gemini + OpenAI + local model support
- ✅ **8 export formats** — Markdown, CSV, JSON, HTML, PNG, SVG, PDF, Plain text
- ✅ **Biometric privacy lock** with timeout settings
- ✅ **Camera V2** — full-screen, zoom, focus, grid, timer, post-capture

### Competitive Gaps to Address

| Gap | Competitor(s) That Have It | Impact |
|:---|:---|:---|
| **Computer vision species ID** | iNaturalist, Seek | 🔴 High — major onboarding hook |
| **Community validation / peer review** | iNaturalist, eBird | 🔴 High — unlocks data quality |
| **XLSForm / structured form import** | KoboToolbox, ODK | 🟡 Medium — power user feature |
| **Advanced geometry (polygons, lines)** | ODK, QField | 🟡 Medium — GIS use cases |
| **AI transcription (audio → text)** | KoboToolbox (80 languages) | 🔴 High — field accessibility |
| **Offline maps with tile caching** | QField, Survey123 | 🟡 Medium — deeper OSM integration |
| **Data validation & skip logic on forms** | ODK, KoboToolbox | 🟡 Medium — data quality |
| **Longitudinal entity tracking** | ODK (offline Entities) | 🟡 Medium — multi-visit studies |
| **Real-time collaboration dashboard** | Fulcrum, KoboToolbox | 🔵 Future — cloud sync |

---

## 2. Current State Audit

### What's Working Well

| Area | Status | Details |
|:---|:---|:---|
| Room database (21 entities, 10 xref tables) | ✅ Solid | Soft-delete pattern, v7 migration |
| Navigation (bottom bar + rail, animations) | ✅ Polished | Animated icons, haptic feedback, tablet adaptation |
| Camera V2 | ✅ Complete | Full-screen, zoom, focus, grid, timer, post-capture |
| Research Session | ✅ Complete | Timer, quick input, summary |
| Workspace redesign (3 tabs) | ✅ Complete | Projects / Evidence / Analysis |
| SM-2 flashcards | ✅ Complete | Ease factor, intervals, deck modes |
| AI assistant | ✅ Functional | Gemini + OpenAI, retry logic |
| Export suite (8 formats) | ✅ Complete | Markdown, CSV, JSON, HTML, PNG, SVG, PDF |
| Privacy lock + security settings | ✅ Complete | Biometric, timeout, auto-lock |
| Snackbar system | ✅ Complete | Replaced Toast, CompositionLocal provider |
| Required field validation | ✅ Complete | FieldTextField required, RequiredFieldState |

### Critical Issues Still Open

| Issue | Severity | Details |
|:---|:---|:---|
| Single ViewModel (god object, 1500+ lines) | 🔴 High | No DI, no unit testing possible |
| Single DAO file (60+ queries) | 🟡 Medium | Hard to maintain, no separation of concerns |
| Single Repository file (70+ methods) | 🟡 Medium | Violates Single Responsibility Principle |
| Single entities file (21 types) | 🟡 Medium | File is very long, hard to navigate |
| No dependency injection | 🔴 High | Hilt missing, direct instantiation everywhere |
| Weather API not integrated | 🟡 Medium | Settings added, no API calls |
| Full-text search uses LIKE '%query%' | 🟡 Medium | Slow on large datasets, no FTS tables |
| No loading/error states in most screens | 🟡 Medium | Blank screens during loading |
| Settings init side-effects | 🟡 Medium | getInstance() re-schedules background jobs |
| Legacy music infrastructure still present | 🟡 Medium | Unused workers, services, widgets |
| No quick annotation on photos | 🟡 Low | Post-capture can't annotate |
| Home screen still has 6-tile grid | 🟡 Low | Needs hero section, weather widget |
| No PDF reader (uses WebView fallback) | 🟡 Low | Native viewing needed |

---

## 3. Impressive New Features

### 3.1 🔬 Species Identification Engine (Computer Vision)

**Inspiration:** iNaturalist's offline model + Seek's on-device AI
**Impact:** 🔴 High — **single most impactful new feature**

**Implementation:**
```kotlin
// New: fieldmind.research.app.features.field.data.vision/
//   SpeciesClassifier.kt        — ML Kit / TensorFlow Lite wrapper
//   SpeciesDatabase.kt           — bundled species signatures
//   ObservationMatcher.kt        — match camera capture to species

// Use ML Kit Object Detection + custom TensorFlow Lite model
// Bundle a lightweight species classifier (500 local species initially)
// Fall back to online iNaturalist API if user opts in

// Feature: After camera capture → "Identify species" button → 
// shows top 5 matches with confidence scores → quick-add to observation
```

**Files to create:**
- `SpeciesClassifier.kt` — TFLite model wrapper with confidence scoring
- `SpeciesDatabase.kt` — bundled species signatures (expandable via download)
- `SpeciesResultSheet.kt` — bottom sheet showing top-5 matches with photos
- `SpeciesSearchScreen.kt` — manual search by common/scientific name

**Impact:**
- Transforms FieldMind from a *note-taking* app to a *field identification* app
- Major onboarding hook: "Identify plants, animals, and fungi with your camera"
- Community validation can follow (see Feature 3.4)

---

### 3.2 🎙️ AI Transcription & Voice-to-Observation

**Inspiration:** KoboToolbox's 80-language audio transcription
**Impact:** 🔴 High — **field accessibility game-changer**

**Implementation:**
```kotlin
// New: fieldmind.research.app.features.field.data/transcription/
//   AudioTranscriber.kt          — Speech-to-text wrapper
//   WhisperModelManager.kt       — Local Whisper model (distilled)
//   VoiceObservationParser.kt    — Parse natural language → structured fields

// On-device: use distilled Whisper model (via MediaPipe or llama.cpp)
// Cloud fallback: Google Cloud Speech-to-Text / OpenAI Whisper API
// Feature: "Voice observation" → speak naturally → auto-parsed into
//   subject, facts, category, tags → user reviews before saving
```

**Key user flow:**
1. Tap microphone FAB on Home or Capture screen
2. Speak naturally: *"Saw a red-tailed hawk perched on a maple branch near the stream, about 20 meters away, clear sunny day"*
3. AI parses: Subject="Red-tailed Hawk", Category="Wildlife", Facts="perched on maple near stream, ~20m distance", Context="clear sunny day"
4. User reviews, edits, and saves — 10-second capture vs 60+ second manual form

**Model options:**
- **Distilled Whisper (tiny.en):** ~75MB, runs on-device, English only
- **MediaPipe Audio Classifier:** lighter, keyword-based
- **Cloud fallback:** OpenAI Whisper API (user-provided key)

---

### 3.3 🗺️ Enhanced Offline Maps with Tile Caching

**Current:** Basic OSM map with GPS markers
**Target:** Full offline-capable GIS tool

**New capabilities:**
- **Downloadable tile regions:** User selects rectangle on map → downloads OSM tiles for offline use
- **Multiple tile sources:** OSM, Satellite (Mapbox/Bing with cache), Terrain
- **Drawing tools:** Draw polygons (survey areas), lines (transects), points (sites)
- **GPS tracking overlay:** Record track log during field session, visualize path on map
- **Geo-fenced reminders:** "When you arrive at Site A, remind you to observe water quality"

**Files to create:**
- `OfflineTileManager.kt` — download, cache, and prune OSM tiles
- `MapDrawingTools.kt` — polygon/line/point overlays on osmdroid
- `TrackRecorder.kt` — GPS track logging with start/stop
- `GeoFenceManager.kt` — proximity-based reminders using WorkManager

**Why this matters:** True offline maps with tile caching is what separates pro field tools from casual apps. ODK, QField, and Survey123 all have this.

---

### 3.4 👥 Community Validation & Peer Review Network

**Inspiration:** iNaturalist's "Research Grade" consensus, eBird's expert review pipeline
**Impact:** 🟡 High — builds data quality and user community

**Architecture (offline-first, sync-optional):**
```
Local observation → [Validate] → Status: "Needs ID" 
  → User shares as .fieldmind file (or future cloud sync)
  → Peer opens → adds identification → status: "Pending Consensus"
  → 2/3 agreement → status: "Research Grade" → achievement unlocked
```

**Implementation (v1 — local file sharing):**
- Export observation as `.fieldmind` package (JSON + photos)
- Import peer's validation -> update `identification` field
- Track validators in new `ValidationEntity` (validatorId, speciesGuess, confidence, notes, timestamp)
- Achievement: "Peer Reviewer" — validate 10 observations

**Implementation (v2 — future cloud):**
- Firebase or custom relay server for community sync
- Real-time notifications: "Someone identified your observation!"
- Leaderboard: top identifiers this month

---

### 3.5 📋 XLSForm Import & Structured Survey Engine

**Inspiration:** ODK Collect, KoboToolbox
**Impact:** 🟡 Medium — **power user & institutional adoption**

**Implementation:**
```kotlin
// New: fieldmind.research.app.features.field.data/survey/
//   XLSFormParser.kt             — Parse XLSForm → FieldMind survey
//   SurveyEntity.kt              — New entity for surveys
//   SurveyRunnerScreen.kt        — Step-through survey UI
//   FormLogicEngine.kt           — Skip logic, validation, calculations

// Users can import .xlsx files with XLSForm structure
// Surveys appear as a new entity type alongside Observations
// Supports: skip logic, required fields, repeats, calculations
// Output: Survey responses saved as structured data records
```

**Why this matters:** This opens FieldMind to institutional users who already have XLSForm-based workflows. Many NGOs, universities, and government agencies use ODK/Kobo and would consider FieldMind as an alternative if it supports their existing forms.

---

### 3.6 ⚡ Behavioral Event Logger (Ethogram)

**Inspiration:** Professional ethology tools, Fulcrum event logging
**Impact:** 🟡 Medium — **niche but highly valuable for wildlife research**

**New screen:** `EventLoggerScreen.kt`
**New entity:** `EventLogEntity` (sessionId, behaviorTag, startMs, endMs, notes, photo)

**Key user flow:**
1. Create an ethogram: define behaviors (e.g., "Feeding", "Grooming", "Moving", "Resting")
2. Start session → big tap buttons for each behavior
3. Tap a behavior → logs start time; tap again or tap different behavior → logs end time
4. Session summary: duration per behavior, transition matrix, frequency chart
5. Export as CSV with timestamps for statistical analysis (e.g., behavioral sequence analysis)

**This is a unique feature** that even ODK and KoboToolbox don't have natively. Researchers currently use dedicated ethogram apps or manual stopwatches.

---

### 3.7 📝 Rich Text Note Editor with Markdown & Inline Media

**Current:** Plain text notes with pipe-delimited attachments
**Target:** Full rich text editor with markdown, inline images, checklists, templates

**Implementation:**
```kotlin
// New: fieldmind.research.app.features.field.presentation/components/
//   FieldRichEditor.kt           — Markdown + rich text composable
//   NoteTemplate.kt              — Template definitions

// Using Compose Rich Editor library (MohamedRejeb) for WYSIWYG
// or custom markdown parser + rendered preview

// Templates:
//   - Observation Log: Subject:, Location:, Weather:, Observations:, Notes:
//   - Literature Notes: Source:, Key Arguments:, Evidence:, My Analysis:, Questions:
//   - Field Journal: Date:, Time:, Location:, Conditions:, Findings:, Follow-up:
//   - Meeting Notes: Date:, Attendees:, Agenda:, Decisions:, Action Items:
```

**Key features:**
- Markdown toolbar: bold, italic, headings, lists, code blocks
- Inline image embedding (from camera/gallery)
- Checklists (toggleable task list items)
- Voice note attachment
- Template picker (pre-populated prompts)

---

### 3.8 📱 One-Tap Quick Capture Widget (Android Home Screen)

**Current:** Fields created (QuickCaptureWidget + DashboardWidget) but uncommitted
**Target:** **Fully functional with real data**

**Implementation priority:**
1. ✅ QuickCaptureWidget (2×1) — opens capture screen (done)
2. ✅ DashboardWidget (4×3) — shows research stats (done)
3. ❌ **Widget data sync** — `FieldMindWidgetUpdater` needs to push real data from ViewModel/Database to Glance widget state using `PreferencesGlanceStateDefinition`
4. ❌ **Daily Summary Widget (4×1)** — "Today: 3 obs, 1 note, 2 questions" with progress ring
5. ❌ **Widget configuration** — Allow user to choose which project/stats to show
6. ❌ **"Add to home screen" prompt** — Suggest widget setup after 5th observation

---

### 3.9 📊 Interactive Data Tables & Pivot Views

**Current:** Basic list views
**Target:** Sortable, filterable, exportable data tables for structured data records

```kotlin
// New: FieldDataTable.kt — Compose-based interactive table
// Features:
//   - Column sorting (tap header)
//   - Filter by toolType, date range, tags
//   - Aggregate functions: sum, average, count
//   - Quick chart: select rows → "Chart this" → bar/line chart
//   - Export filtered data as CSV
//   - Pivot: group by category, summarize by value
```

---

### 3.10 🔔 Smart Notifications & Reminders

**Current:** Basic daily reminder in WorkManager
**Target:** Context-aware notification system

- **Daily reminder** with today's goal progress (current count / goal)
- **Streak at risk** notification if no observation by 6 PM
- **Research session reminder** — "You haven't started a session in 3 days"
- **Flashcard review prompt** — spaced repetition says you have cards due
- **Geo-fenced observation prompt** — "You're near Site A. Log an observation?"
- **Backup reminder** — "Last backup was 7 days ago"

---

## 4. Insights & Analytics Redesign

### 4.1 Current State

The Insights screen (`InsightsScreen.kt`) is functional but basic:
- Category bar chart
- Daily trend line chart
- Confidence breakdown
- OSM mini-map with markers
- Knowledge graph (connection graph)
- Tags list
- Open questions
- Active projects
- Collapsible achievements

### 4.2 Redesigned Insights — "Research Dashboard"

The Insights screen should be renamed to **"Research Dashboard"** and become the **analytical command center**.

#### Section 1: Research Profile Card (Top)
- Name, role, focus, local model status, backup interval ✅ (exists)
- **NEW:** Weekly activity summary: "This week: 12 obs, 3 notes, 1 question"
- **NEW:** Streak progress: "15-day streak 🔥" with motivational message
- **NEW:** Quick action: "Open weekly report" button

#### Section 2: Key Performance Metrics Row
- Observations count ✅ (exists)
- Hypotheses count ✅ (exists)
- Reports count ✅ (exists)
- **NEW:** Questions answered ratio (e.g., "3/8 answered — 37%")
- **NEW:** Sources read ratio (e.g., "5/12 read — 42%")
- **NEW:** Flashcard mastery ("85% mature cards")

#### Section 3: Time-Series Analytics (NEW)
- **NEW:** Calendar heatmap — GitHub-style contribution grid showing daily observations for the past 365 days
- **NEW:** Activity by hour — bar chart showing which hours of day you observe most
- **NEW:** Day-of-week breakdown — which days are most productive
- **NEW:** Moving average trend — 7-day rolling average overlay on daily counts

#### Section 4: Category & Tag Analytics (Enhanced)
- **NEW:** Category radar/spider chart — compare observation counts across categories
- **NEW:** Tag co-occurrence matrix — which tags appear together most often
- **NEW:** Tag timeline — how tag usage has changed over time
- **NEW:** Category confidence breakdown — which categories have highest/lowest confidence

#### Section 5: Knowledge Graph (Enhanced)
- **Current:** Static node-edge visualization ✅ (exists)
- **NEW:** Interactive — tap node to navigate to entity detail
- **NEW:** Filter by entity type (toggle observations/questions/hypotheses/sources)
- **NEW:** Timeline play — animate graph evolution over time
- **NEW:** Cluster detection — automatically detect topic clusters
- **NEW:** Weak signal detection — "These two observations might be related"

#### Section 6: Research Health (NEW)
- **Data quality score:** "85/100 — Good" based on:
  - % of observations with evidence (weight: 25%)
  - % of questions with linked hypotheses (weight: 25%)
  - % of sources with reading notes (weight: 20%)
  - % of observations with tags (weight: 15%)
  - % of observations with GPS (weight: 15%)
- **Gap analysis:** "You have 8 unanswered questions. Consider starting a research session."
- **Recommendations engine:** AI-powered suggestions based on your research patterns

#### Section 7: Weather Integration (NEW)
- **Current:** Weather settings exist, API not integrated ❌
- **Target:** Open-Meteo integration (free, no API key)
- Show weather conditions at time of each observation
- **Weather trend chart:** Temperature, humidity, cloud cover over time
- **Weather filter:** "Show observations when temperature > 25°C"
- **Weather correlation:** "Your bird observations peak between 6-8 AM on clear days"

#### Section 8: Achievements (Enhanced)
- **Current:** 10 achievements, progress bars, collapsible list ✅
- **NEW:** 25+ achievements across categories (observation, learning, analysis, community)
- **NEW:** Tiered achievements: Bronze → Silver → Gold → Diamond
- **NEW:** Achievement animation on unlock (particle burst or scale animation)
- **NEW:** Achievement sharing — export as card image
- **NEW:** Hidden achievements — "??? — discover by exploring"

#### Section 9: Export & Reporting (Enhanced)
- **NEW:** "Generate Research Summary" — AI-powered narrative of your research period
- **NEW:** "Weekly Digest" — auto-generated PDF with key stats, charts, and recent activity
- **NEW:** "Research Portfolio" — export all projects with their entities as a structured archive

### 4.3 New Charts & Visualizations Needed

| Visualization | Priority | Implementation | Library |
|:---|:---|:---|:---|
| Calendar heatmap | 🔴 High | Canvas composable | Custom |
| Radar/spider chart | 🔴 High | Canvas composable | Custom |
| Moving average overlay | 🟡 Medium | Additional LineChart data | Vico |
| Tag co-occurrence matrix | 🟡 Medium | Grid of colored cells | Custom |
| Activity by hour | 🟡 Medium | Bar chart with 24 bars | Vico |
| Day-of-week chart | 🟡 Low | 7-bar chart | Vico |
| Weather correlation | 🟡 Low | Scatter plot + line | Custom |
| Network graph timeline | 🟡 Low | Animated node positions | Custom Canvas |

---

## 5. Data Layer & Architecture Improvements

### 5.1 Phase 1: Critical Refactoring (Must Fix)

#### Dependency Injection (🔴 High)

**Current:** `FieldMindViewModel` extends `AndroidViewModel` and directly instantiates `FieldMindRepository`, `FieldMindSettings` — no DI, no testing possible.

**Target:** Hilt integration with proper module structure.

```kotlin
// New structure:
// features/field/di/
//   FieldMindModule.kt           — @Module with @Provides for DAO, Repository, Settings
//   FieldMindViewModelModule.kt  — @Module with @Binds for ViewModel
//   DatabaseModule.kt            — @Module for Room database

// Each ViewModel gets its own HiltViewModel:
//   @HiltViewModel class FieldMindViewModel @Inject constructor(
//       private val repository: FieldMindRepository,
//       private val settings: FieldMindSettings,
//       private val locationProvider: FieldLocationProvider,
//       private val weatherService: WeatherApiService,
//       private val privacyManager: FieldMindPrivacyManager
//   ) : ViewModel()
```

#### Split Monolithic Files (🟡 Medium)

| Current File | Lines | Target Split |
|:---|:---|:---|
| `FieldMindViewModel.kt` | ~1500+ | 6 ViewModels: HomeVM, ObserveVM, ProjectsVM, InsightsVM, SettingsVM, SessionVM |
| `FieldMindDao.kt` | 60+ queries | 3 DAOs: CoreDao (obs/notes/questions), ProjectDao, SystemDao (tags/settings) |
| `FieldMindRepository.kt` | 70+ methods | 3 Repositories mirroring DAOs |
| `FieldEntities.kt` | 21 types | 5 entity files: CoreEntities, ProjectEntities, MediaEntities, LearningEntities, SystemEntities |
| `FieldMindDialogs.kt` | ~967 lines | One file per dialog type or grouped by entity |
| `FieldMindObserveScreen.kt` | ~690 lines | CameraScreen, CaptureForm, FieldModeScreen |

#### Database Performance (🟡 Medium)

| Issue | Solution |
|:---|:---|
| Full-text search uses `LIKE '%query%'` | Add Room FTS4 tables for notes, observations, questions |
| No pagination | Add Paging 3 with `@RawQuery` + `LIMIT/OFFSET` |
| No indexing on frequent queries | Add indexes on `category`, `status`, `date`, `projectId` |
| Single table observer per screen | Split observers; use `Flow.distinctUntilChanged()` to reduce recomposition |

### 5.2 Phase 2: Offline-First Hardening

#### Sync Queue Architecture (for future cloud sync)

```kotlin
// New: SyncQueueEntity — tracks unsynchronized changes
// @Entity data class SyncQueueEntity(
//     @PrimaryKey val id: Long = 0,
//     val entityType: String,      // "observation", "note", etc.
//     val entityId: Long,
//     val action: String,           // "create", "update", "delete"
//     val payload: String,          // JSON of the entity
//     val createdAt: Long,
//     val syncedAt: Long? = null
// )

// Action Queue pattern (not full sync):
// 1. All writes go to Room + SyncQueue
// 2. WorkManager SyncWorker processes queue when online
// 3. On conflict: "last write wins" with user notification
// 4. User can view pending syncs in Settings > Sync Status
```

#### Data Integrity Checks

- Entity validation before save (required fields, format checks)
- Referential integrity: warn when deleting an entity that has backlinks
- Periodic integrity audit (WorkManager weekly): checks for orphaned references, missing parent entities

---

## 6. UI/UX Overhaul: Screen-by-Screen

### 6.1 Home Screen — "Research Hub" Redesign

**Current:** 6-tile grid + quick actions + recent activity
**Target:** Dynamic, personalized, data-rich dashboard

```
┌──────────────────────────────────────────┐
│ ☀️ 22°C Clear • City, US    [Today: 3/5] │  ← Weather + goal widget
│ "Good morning, Alex. You have 1 session" │  ← Personalized greeting
├──────────────────────────────────────────┤
│ 📷 Capture  📝 Note  ❓ Question  ⏱️ Sess │  ← Horizontal chip row
├──────────────────────────────────────────┤
│ 🔥 7-day streak!  → Log today's obs     │  ← Streak card (animated)
├──────────────────────────────────────────┤
│ ┌──────┐ ┌──────┐ ┌──────┐              │
│ │ 12   │ │ 3    │ │ 8    │              │  ← Big metric cards
│ │ Obs  │ │ Ques │ │ Proj │              │     (64×64dp min tap)
│ └──────┘ └──────┘ └──────┘              │
├──────────────────────────────────────────┤
│ ⏱️ Your last session: 23m, 4 obs         │  ← Research pulse
│ ████████░░░░░░░░ 2 days ago              │
├──────────────────────────────────────────┤
│ Recent activity                          │  ← Live feed
│ 📷 Blue jay at feeder  · 2m ago          │
│ 📝 Notes on migration patterns · 15m ago │
├──────────────────────────────────────────┤
│ Recommended learning                     │  ← AI-powered
│ "Species Identification Guide"           │
│ "Field Journal Methods"                  │
└──────────────────────────────────────────┘
```

**Changes needed:**
1. Replace 6-tile grid with 3 big metric cards (64dp min icon size, 64×64dp tap targets)
2. Add weather widget at top (Open-Meteo integration)
3. Add personalized greeting with user's name
4. Add horizontal scrollable quick action chips
5. Add research pulse card with last session data
6. Increase tap target sizes across the board
7. Add empty states with guided prompts

### 6.2 Capture Screen — "Evidence-First" Redesign

**Current:** Tab-based (Capture, Notes, GPS), camera + form
**Target:** Camera/gallery as primary action, minimal barriers

**Key changes:**
1. **Camera is the default view** — not a tab
2. **Remove required category** before writing — start with photo or text
3. **Post-capture: "What's next?"** prompt after each save:
   - "Add data to this observation?"
   - "Link to an existing project?"
   - "Create a question from this?"
   - "Start a research session?"
4. **Quick capture mode** from FAB: launches camera directly, one-tap save
5. **Batch mode:** Select multiple photos → batch-categorize with AI suggestions
6. **Smart defaults:** Auto-suggest category based on recent observations and location
7. **Collapse advanced metadata** by default (GPS, weather, tags — show on tap)

**Forms redesign:**
- **Observation:** Subject (optional text + voice), Camera, Category (optional chips)
- **Note:** Start typing immediately — category is metadata, not a gate
- **Question:** Question text (prominent), priority, category

### 6.3 Library Screen — "Knowledge Hub" Rename & Redesign

**Current:** 5 tabs (Sources, Notes, Flashcards, Learn, Reading)
**Target:** Unified knowledge space with smart filters

**Key changes:**
1. **Rename to "Knowledge Hub"**
2. **Unified evidence list:** All entities in one scrollable feed, filterable by type
3. **Quick filter bar:** Entity type chips + reading status + importance
4. **Reading progress:** Visual indicator for in-progress sources
5. **PDF viewer:** Native rendering via `androidx.pdf:pdf-viewer-fragment`
6. **Quick add FAB:** Long-press for entity type selector
7. **Smart search:** Full-text search across all entity types with highlights
8. **Collections:** User-created curated sets of entities (like playlists)

### 6.4 Settings Screen — Consolidation

**Current:** Hub page + 8 sub-pages (Profile, Appearance, Capture, AI, Local Model, Backup, Security, About)
**Target:** No major changes needed — current structure is clean. Add:
1. **Widget settings:** Configure which stats appear on Dashboard Widget
2. **Data management:** View database size, clear cache, FTS rebuild
3. **Sync status:** (future) pending syncs, last sync time, network status

### 6.5 Detail Screen — Entity-Specific Layouts

**Current:** Single `DetailScreen` handles all entity types
**Target:** Entity-specific detail layouts with rich interactions

**Observation Detail:**
```
┌──────────────────────────────────┐
│ [Photo gallery — horizontally    │  ← Hero image gallery
│  scrollable, tap to full-screen] │
├──────────────────────────────────┤
│ 🟢 Sure  •  Wildlife  •  Today   │  ← Confidence + category + date
│ "Red-tailed Hawk"                 │  ← Subject (large, bold)
│ #raptor #hawk #birdwatching      │  ← Tags
├──────────────────────────────────┤
│ 📍 40.7128° N, 74.0060° W        │  ← GPS card with mini-map
│ ☀️ 22°C, Clear — 8:30 AM         │  ← Weather snapshot
├──────────────────────────────────┤
│ Facts-only notes:                 │
│ Perched on maple branch ~20m     │
│ from stream. Eating.             │
├──────────────────────────────────┤
│ Related observations (3)  →      │  ← Timeline of related
│ Related questions (1)    →       │  ← Connected entities
│ Related hypotheses (0)           │
├──────────────────────────────────┤
│ [ Edit ] [ Share ] [ Export PDF ] │  ← Bottom action bar
└──────────────────────────────────┘
```

### 6.6 Project Detail — Research Workspace

**Current:** Basic entity card
**Target:** Full project workspace with tabs

```kotlin
// New: ProjectDetailScreen.kt
// Tabs: Overview | Evidence | Analysis | Reports | Timeline
// Overview: title, question, objective, progress ring, member count
// Evidence: unified feed of linked observations, notes, questions
// Analysis: linked hypotheses, data records, comparison view
// Reports: linked reports with export buttons
// Timeline: chronological view of all project activity
```

---

## 7. On-Device AI & ML Features

### 7.1 Current AI State
- ✅ Gemini + OpenAI integration for fact-checking and Q&A
- ✅ Settings for provider, model, API key
- ✅ Local model support (FieldLite/FieldCore/FieldPro)
- ❌ No on-device vision models
- ❌ No voice transcription
- ❌ No auto-categorization
- ❌ No question generation

### 7.2 New AI Features

#### On-Device Species Classification
- **Model:** MobileNetV3 or EfficientNet-Lite via TensorFlow Lite
- **Size:** 10-50MB
- **Capabilities:** 500-2000 common species (expandable via download)
- **Integration:** Post-capture → "Identify" button → bottom sheet with top-5 matches

#### Auto-Categorization
- **Trigger:** After 10 observations with categories
- **Mechanism:** Simple TF-IDF + k-NN classifier based on existing patterns
- **Output:** Suggested category when creating new observation
- **Privacy:** Runs entirely on-device

#### Offline Question Generation
- **Trigger:** After saving an observation
- **Mechanism:** Template-based: "What caused [subject] to [fact]?" "How does [subject] compare to [related]?"
- **Privacy:** Rule-based, no model needed (or use downloaded local model)

#### Smart Flashcard Creation
- **Trigger:** After saving a source or observation
- **Mechanism:** LLM (local or cloud) extracts key concepts → generates Q&A cards
- **Settings:** Toggle "Auto-create flashcards from new observations"

#### AI Research Assistant Enhancements
- **NEW:** "Summarize this observation" — generates concise summary
- **NEW:** "Find related research" — searches local archive + online sources
- **NEW:** "Suggest next steps" — based on research patterns
- **NEW:** "Generate hypothesis" — from unanswered questions + evidence

---

## 8. Accessibility & Internationalization

### 8.1 Accessibility (🔴 Medium Priority)

| Feature | Implementation | Impact |
|:---|:---|:---|
| **Content descriptions** on all icons/images | Add `contentDescription` to all Icon composables | Screen reader users |
| **Minimum 48×48dp tap targets** | Audit all interactive elements; increase where needed | Motor impairment |
| **High-contrast mode** | Add accessible color scheme variant | Visual impairment |
| **Font size scaling** | Use `MaterialTheme.typography` with `fontScale` | Visual impairment |
| **Reduce motion** | Respect `android:reducedMotion` system setting | Vestibular disorders |
| **Voice navigation** | Ensure all actions have accessibility labels | Motor impairment |
| **Focus indicators** | Visible focus rings for keyboard/TV navigation | Keyboard users |

### 8.2 Internationalization (🟡 Medium Priority)

| Language | Current | Target |
|:---|:---|:---|
| English | ✅ 100% | Maintain |
| Spanish | ✅ Existing strings | Review & complete |
| French | ✅ Existing strings | Review & complete |
| German | ✅ Existing strings | Review & complete |
| Hindi | ✅ Existing strings | Review & complete |
| Arabic | ✅ Existing strings | Review & complete |
| Chinese (Simplified) | ❌ Not present | Add |
| Portuguese | ✅ Existing strings | Review & complete |
| Russian | ✅ Existing strings | Review & complete |
| Japanese | ✅ Existing strings | Review & complete |

**Action:** Update all translated strings files to include FieldMind-specific strings. Currently only English `strings.xml` has the FieldMind widget strings.

### 8.3 Field-Specific Accessibility

- **Glove mode:** Increase default touch target to 56dp when "glove mode" is on
- **Sunlight readability:** High-contrast outdoor theme option
- **One-handed mode:** Bottom-anchored controls for large phones
- **Quick capture with voice:** "Hey FieldMind, capture observation" (future)

---

## 9. Performance & Offline-First Hardening

### 9.1 Performance Bottlenecks

| Area | Issue | Fix |
|:---|:---|:---|
| Home screen | Collects 8+ flows simultaneously | Lazy-load offscreen flows; use `Flow.combine` with debounce |
| Dialogs | Single 967-line file | Split into entity-specific dialog files |
| Search | `LIKE '%query%'` on entire tables | Room FTS4 + paging |
| Charts | Canvas redraws on every recomposition | Use `remember` + `derivedStateOf` for chart data |
| Images | No caching for evidence photos | Add Coil disk cache for observation images |
| Startup | ViewModel initializes everything at once | Lazy-init non-critical services |
| Widget | No data sync to Glance | Add `FieldMindWidgetUpdater` with DAO queries |

### 9.2 Offline-First Checklist

| Requirement | Status | Notes |
|:---|:---|:---|
| Room local DB | ✅ Done | 21 entities, v7 |
| Offline map tiles | ❌ Missing | Need offline tile caching |
| Offline species ID | ❌ Missing | Need bundled TFLite model |
| Offline AI assistant | 🟡 Partial | API key required; local model optional |
| WorkManager background sync | 🟡 Partial | Backup only; no sync queue |
| Data export without internet | ✅ Done | 8 formats |
| Offline search | 🟡 Partial | `LIKE` queries work offline but slow |
| Offline weather | ❌ Missing | Open-Meteo requires API call; consider cache |

---

## 10. Priority Roadmap

### Phase 1: Critical Foundations (Current Sprint)

| # | Task | Effort | Impact | Dependencies |
|:---|:---|:---|:---|:---|
| 1 | Commit and PR widget files | Low | Medium | None |
| 2 | Remove duplicate workers | Low | High | None |
| 3 | Fix settings init side-effects | Low | High | None |
| 4 | Weather API integration (Open-Meteo) | Medium | High | Settings already done |
| 5 | Add weather fields to ObservationEntity | Medium | High | DB migration needed |
| 6 | Weather widget on Home screen | Medium | Medium | Item 4, 5 |

### Phase 2: User Experience Boost (Next Sprint)

| # | Task | Effort | Impact | Dependencies |
|:---|:---|:---|:---|:---|
| 7 | Home screen redesign (hero, chips, metrics) | High | 🔴 Critical | None |
| 8 | Capture form evidence-first redesign | High | 🔴 Critical | Camera V2 done |
| 9 | Insights → Research Dashboard overhaul | High | 🔴 Critical | None |
| 10 | Calendar heatmap chart | Medium | High | None |
| 11 | Radar/spider chart | Medium | High | None |
| 12 | Entity-specific detail layouts | High | 🟡 High | None |
| 13 | "What's next?" prompt after save | Low | High | None |
| 14 | Make category optional in capture | Low | Medium | None |

### Phase 3: Mobile-Field Power Features (Next)

| # | Task | Effort | Impact | Dependencies |
|:---|:---|:---|:---|:---|
| 15 | Species ID engine (TFLite) | High | 🔴 Critical | Camera V2 done |
| 16 | AI transcription (Whisper) | High | 🔴 Critical | Settings done |
| 17 | Behavioral event logger | Medium | High | New entity needed |
| 18 | Enhanced offline maps (tile cache) | High | 🟡 High | OSM already integrated |
| 19 | Quick annotation on photos | Medium | High | Camera V2 done |
| 20 | Rich text note editor | High | 🟡 High | None |

### Phase 4: Architecture & Quality (Next)

| # | Task | Effort | Impact | Dependencies |
|:---|:---|:---|:---|:---|
| 21 | Hilt dependency injection | High | 🔴 Critical | Major refactor |
| 22 | Split ViewModel into 6 | High | 🟡 High | Item 21 |
| 23 | Split DAO into 3 | Medium | 🟡 Medium | Item 22 |
| 24 | Split Entities into 5 files | Medium | 🟡 Low | Item 22 |
| 25 | Add FTS4 tables for search | Medium | 🟡 High | None |
| 26 | Add Paging 3 for large lists | Medium | 🟡 High | None |
| 27 | Add loading/error states | Medium | 🟡 High | None |

### Phase 5: Community & Ecosystem (Future)

| # | Task | Effort | Impact | Dependencies |
|:---|:---|:---|:---|:---|
| 28 | XLSForm import | High | 🟡 Medium | None |
| 29 | Community validation | High | 🟡 Medium | Species ID done |
| 30 | Cloud sync (Firebase) | High | 🔵 Future | Sync queue needed |
| 31 | PDF reader (androidx.pdf) | Medium | 🟡 Medium | None |
| 32 | Project attachments | Medium | 🟡 Medium | None |
| 33 | Data quality score engine | Low | 🟡 Medium | None |

### Phase 6: AI & Intelligence (Future)

| # | Task | Effort | Impact | Dependencies |
|:---|:---|:---|:---|:---|
| 34 | Auto-categorization from patterns | Low | 🟡 Medium | 10+ observations |
| 35 | Smart flashcard generation | Medium | 🟡 Medium | Local/cloud LLM |
| 36 | Offline question generation | Low | 🟡 Medium | None |
| 37 | Research Summary (AI report) | Medium | 🟡 Low | AI assistant |
| 38 | Pattern recognition (repeated observations) | High | 🟡 Low | Species ID done |

---

## 11. Implementation Guides

### 11.1 Open-Meteo Weather Integration

**API:** Free, no API key needed

```kotlin
// GET https://api.open-meteo.com/v1/forecast
//   ?latitude={lat}&longitude={lon}
//   &current=temperature_2m,relative_humidity_2m,
//        wind_speed_10m,weather_code,cloud_cover
//   &timezone=auto

// Response (current_weather):
// {
//   "temperature": 22.5,
//   "windspeed": 12.3,
//   "weathercode": 0,  // WMO code: 0=clear, 1=mainly clear, etc.
//   "time": "2026-06-13T14:30"
// }

// Cache: Store in Room with 6-hour TTL per lat/lon pair
// New entity: WeatherCacheEntity(lat, lon, timestamp, temperature, humidity, wind, cloudCover, weatherCode)
```

### 11.2 Species Classification Model

**Model:** Use Google's MediaPipe or TensorFlow Hub model

```kotlin
// Option A: MediaPipe Image Classifier (easier, Google-maintained)
//   - Bundled model: 500 common North American species
//   - Downloadable: Regional packs (Europe, Asia, etc.)
//   - Output: List of (label, score) pairs

// Option B: TensorFlow Lite custom model
//   - Train on iNaturalist dataset (available for research)
//   - MobileNetV3 backbone for speed
//   - Quantized to INT8 for size (15-30MB)

// Fallback: iNaturalist API (requires internet)
//   - POST /v1/identifications with image
//   - Returns top-10 species with confidence
```

### 11.3 Calendar Heatmap Implementation

```kotlin
@Composable
fun CalendarHeatmap(
    dailyCounts: Map<LocalDate, Int>,
    maxCount: Int,
    months: Int = 12,
    modifier: Modifier = Modifier
) {
    // Layout: 53 columns (weeks) × 7 rows (days)
    // Each cell: color based on count / maxCount ratio
    // Color scale: empty → low → medium → high (5 shades of green)
    // Tap cell: show date + count tooltip
    // Month labels: abbreviated above first week of each month
    // Day labels: Mon, Wed, Fri on left side
    
    Canvas(modifier = modifier) {
        val cellSize = size.width / 53f // 53 weeks
        val cellPadding = 2.dp.toPx()
        // Draw cells...
    }
}
```

### 11.4 Radar/Spider Chart Implementation

```kotlin
@Composable
fun RadarChart(
    categories: List<Pair<String, Float>>,  // (label, value 0-1)
    maxValue: Float = 1f,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Draw: N-sided polygon (N = categories.size)
    // Grid: concentric N-sided polygons at 25%, 50%, 75%, 100%
    // Data: filled polygon with semi-transparent accent color
    // Labels: positioned at vertices outside the polygon
    // Interaction: tap vertex to show exact value
    
    val angleStep = (2 * PI / categories.size).toFloat()
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = min(center.x, center.y) * 0.8f
        // Draw grid polygons...
        // Draw data polygon...
        // Draw labels...
    }
}
```

### 11.5 Split ViewModel Pattern

```kotlin
// Current: FieldMindViewModel — one class for everything (1500+ lines)
// Target: 6 focused ViewModels

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FieldMindRepository
) : ViewModel() {
    val observations: StateFlow<List<ObservationEntity>>
    val todayCount: StateFlow<Int>
    val streakDays: StateFlow<Int>
    val recentActivity: StateFlow<List<RecentActivity>>
}

@HiltViewModel
class ObserveViewModel @Inject constructor(
    private val repository: FieldMindRepository,
    private val locationProvider: FieldLocationProvider,
    private val weatherService: WeatherApiService
) : ViewModel() {
    fun saveObservation(subject: String, facts: String, ...)
    fun saveNote(title: String, body: String, ...)
    val captureState: StateFlow<CaptureState>
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: FieldMindRepository
) : ViewModel() {
    val categoryDistribution: StateFlow<List<Pair<String, Int>>>
    val dailyTrend: StateFlow<List<Pair<String, Int>>>
    val knowledgeGraph: StateFlow<GraphData>
    val achievements: StateFlow<List<Achievement>>
}

// ProjectsVM, SettingsVM, SessionVM follow the same pattern
```

---

## Appendix A: Competitive Feature Matrix

| Feature | FieldMind | ODK | Kobo | Fulcrum | iNat | eBird |
|:---|:---|:---|:---|:---|:---|:---|
| Offline-first | ✅ | ✅ | ✅ | ✅ | 🟡 | 🟡 |
| Camera capture | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| GPS tagging | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Species ID (AI) | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| Voice transcription | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| Knowledge graph | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Flashcards (SM-2) | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| AI assistant | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Export (8 formats) | ✅ | 🟡 | 🟡 | ✅ | ❌ | ❌ |
| Community validation | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| Offline maps | 🟡 | ✅ | ✅ | ✅ | ❌ | ❌ |
| XLSForm import | ❌ | ✅ | ✅ | 🟡 | ❌ | ❌ |
| Behavior logger | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Research sessions | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Hypothesis testing | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Weather integration | 🟡 | ❌ | ❌ | ❌ | 🟡 | ❌ |
| Rich text notes | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| PDF viewer | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| Project attachments | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Multi-entity detail | 🟡 | ❌ | ❌ | ❌ | ❌ | ❌ |
| Calendar heatmap | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| Data quality scoring | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |

**Legend:** ✅ Full | 🟡 Partial | ❌ Missing

---

## Appendix B: Key Technology Recommendations

| Domain | Recommendation | Alternatives | Rationale |
|:---|:---|:---|:---|
| DI | Hilt | Koin, Dagger | Official Android recommendation; Compose-first |
| Charts | Vico + Custom Canvas | YCharts, MPAndroidChart | Native Compose; no WebView; extensible |
| Species ID | MediaPipe TFLite | TensorFlow Custom Model | Google-maintained; model zoo available |
| Transcription | Whisper (distilled) | MediaPipe ASR | Best accuracy-size tradeoff |
| PDF Viewer | `androidx.pdf:pdf-viewer-fragment` | Apryse, Nutrient | Free; official AndroidX; Compose interop |
| Rich Text | Compose Rich Editor (MohamedRejeb) | Custom Markdown parser | Active maintenance; Compose-native |
| Maps | osmdroid | Mapbox, Google Maps | Full offline; OSM tiles; no API key |
| Pagination | Paging 3 + RemoteMediator | Manual LIMIT/OFFSET | Official; lifecycle-aware; Compose integration |
| Cloud (future) | Firebase Firestore | Supabase, custom server | Managed sync; offline support; auth |
| Sound Analysis | MediaPipe Audio Classifier | librosa (Python) | On-device; keyword spotting |
| Vector Search (future) | MediaPipe Text Embedder | FAISS, Annoy | Semantic search across observations |

---

## Appendix C: Implementation Effort Estimates

| Effort Level | Hours | Examples |
|:---|:---|:---|
| 🟢 Low | 2-4 hours | Fix unused imports, remove workers, add snackbar calls |
| 🟡 Medium | 8-20 hours | Weather integration, calendar heatmap, radar chart, FTS4 tables |
| 🔴 High | 40-80 hours | Species ID engine, Hilt migration, split ViewModel, home screen redesign |
| 🔵 Very High | 80-200 hours | XLSForm import, cloud sync, community validation network |

---

*This analysis was generated on June 13, 2026, based on thorough codebase analysis, competitive research, web research into Android 2026 ecosystem trends, and field research best practices from KoboToolbox, ODK, Fulcrum, iNaturalist, eBird, and other platforms.*
