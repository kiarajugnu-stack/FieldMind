# 🔬 FieldMind App — Comprehensive Analysis & Restructuring Plan

> **Date:** June 13, 2026 (Implementation Update)
> **Scope:** Complete codebase analysis with implementation status, Screen-by-Screen audit, new research-based suggestions.
> **Branch:** `feat/fieldmind-research-redesign` (commit `3e478af7`)
> **Previous:** `main` (commit `44561502`)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Implemented & Working Features](#2-implemented--working-features)
3. [🔴 Critical Issues](#3-critical-issues)
4. [🟡 Code Quality Concerns](#4-code-quality-concerns)
5. [🎨 UI/UX Audit — prompt.md Screen-by-Screen Implementation Status](#5-uiux-audit--promptmd-screen-by-screen-implementation-status)
6. [🔵 Missing Features — New Requirements](#6-missing-features--new-requirements)
7. [🔄 Duplicate / Fragmented Implementations](#7-duplicate--fragmented-implementations)
8. [📱 Screen-by-Screen Redesign Plan](#8-screen-by-screen-redesign-plan)
9. [🔧 Full Restructuring Plan](#9-full-restructuring-plan)
10. [Priority Matrix](#10-priority-matrix)
11. [Key Metrics](#11-key-metrics)
12. [🆕 New Research-Based Suggestions](#12-new-research-based-suggestions)

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
│   ├── FieldMindDatabase.kt               # Room database (v7, 21 entities) ✅ Updated
│   ├── dao/FieldMindDao.kt                # All DAOs in one file (60+ queries)
│   └── entity/FieldEntities.kt            # All 21 entity types ✅ Updated
├── export/FieldMindExport.kt              # Export (Markdown, CSV, JSON, PNG, SVG, PDF)
├── flashcard/SM2Engine.kt                 # SM-2 spaced repetition algorithm
├── learn/LearnLibrary.kt                  # Offline learning resources
├── location/FieldLocationProvider.kt      # GPS & geocoding (on-demand only)
├── repository/FieldMindRepository.kt      # Single repository (70+ methods)
├── security/FieldMindPrivacyManager.kt    # BiometricPrompt implementation ✅
├── settings/FieldMindSettings.kt          # SharedPreferences wrapper (37 keys) ✅ Updated
└── stats/FieldMindStreaks.kt              # Streak calculation logic
```

### Presentation Layer

```
features/field/presentation/
├── components/
│   ├── FieldMindCameraCapture.kt          # CameraX — legacy basic capture (deprecated)
│   ├── FieldMindCameraV2.kt               # CameraX — NEW: full-screen, zoom, focus, grid, timer ✅
│   ├── FieldMindCharts.kt                 # Canvas-based charts + OSM map
│   ├── FieldMindComponents.kt             # Shared UI (headers, chips, cards, inputs)
│   └── FieldMindIcons.kt                  # Icon definitions ✅ Updated
├── navigation/FieldMindNavigation.kt      # Nav graph + bottom bar + rail ✅ Updated
├── screens/
│   ├── FieldMindScreenUtils.kt            # Shared constants + utilities
│   ├── FieldMindHomeScreen.kt             # Today / Home dashboard
│   ├── FieldMindObserveScreen.kt          # Capture + Field mode ✅ Updated (uses CameraV2)
│   ├── FieldMindProjectsScreen.kt         # Workspace (3 tabs: Projects/Evidence/Analysis) ✅ Rewritten
│   ├── FieldMindLibraryScreen.kt          # Knowledge library + learn
│   ├── FieldMindArchiveScreen.kt          # Search archive
│   ├── FieldMindBackupExportScreen.kt     # Export studio
│   ├── FieldMindSettingsScreen.kt         # Settings hub + sub-pages ✅ Updated
│   ├── FieldMindDetailScreen.kt           # Entity detail + backlinks
│   ├── FieldMindDialogs.kt               # All create/edit dialogs + forms
│   ├── FieldMindMapScreen.kt              # Full-screen OSM map
│   ├── FieldMindLockScreen.kt             # Biometric privacy lock ✅
│   ├── FieldMindOnboardingScreen.kt       # Onboarding flow
│   ├── FieldMindResearchSession.kt        # Timer-based research session ✅ NEW
│   ├── InsightsScreen.kt                  # Insights/composables/charts
│   └── FlashcardSessionScreen.kt          # Flashcard review
├── theme/FieldMindTheme.kt                # Brand palette + semantic colors
└── viewmodel/FieldMindViewModel.kt        # Single ViewModel for everything
```

### What's Working Well

- **21 Room entities** with 10 cross-reference tables + 2 new session entities ✅
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
- **Security settings** — Dedicated page with lock timeout, auto-lock ✅ NEW
- **Camera V2** — Full-screen, zoom, focus, grid, timer, post-capture flow ✅ NEW
- **Research Session** — Timer-based multi-observation capture ✅ NEW
- **Workspace** — 3-tab redesign (Projects/Evidence/Analysis) ✅ NEW
- **Achievement system** with 10 milestones
- **Onboarding flow** with 7-step intro

---

## 2. ✅ Implemented & Working Features

### 2.1 Privacy Lock — ✅ Production-ready

- `FieldMindPrivacyManager.kt` implements full `BiometricPrompt` with `CryptoObject`
- `FieldMindLockScreen.kt` provides full-screen lock with biometric and device credential fallback
- **NEW:** Dedicated `SecuritySettingsPage` with lock timeout and auto-lock settings
- Navigation integration — lock gates the entire app on launch
- PIN fallback via `KeyguardManager` device credentials

### 2.2 CameraX — ✅ V2 Full Redesign (NEW)

**FieldMindCameraV2.kt** provides:
- ✅ Immersive full-screen preview (edge-to-edge)
- ✅ Pinch-to-zoom gesture + on-screen slider
- ✅ Tap-to-focus with animated ring
- ✅ Flash toggle (Off/On/Auto)
- ✅ Front/rear camera switch
- ✅ Rule-of-thirds grid overlay toggle
- ✅ Capture timer (3s/5s/10s) with countdown animation
- ✅ Aspect ratio toggle (4:3/16:9/1:1)
- ✅ Post-capture bottom sheet: "Add to Observation" / "Add Question" / "Just Save"
- ❌ Quick annotation after capture (not yet implemented)
- ❌ Auto GPS/weather metadata on capture (settings added, API not yet integrated)

### 2.3 Capture Flow — ✅ Updated to use CameraV2

Both camera overlays in `FieldMindObserveScreen` now use `FieldMindCameraV2` instead of the legacy `FieldMindCameraCapture`. The post-capture flow from CameraV2 feeds directly into observation saving.

**Still needs:** Full evidence-first redesign (Phase 4 remaining work — collapse category chips, add "What's next?" prompt after save).

### 2.4 Workspace — ✅ Redesigned (3 Tabs)

**FieldMindProjectsScreen.kt** rewritten:
- ✅ Reduced from 5 tabs to 3: Projects | Evidence | Analysis
- ✅ Removed `ResearchFlowGuide` — no more forced sequential wizard
- ✅ Simplified project creation: Name + Question → Create (was 11 fields)
- ✅ Unified Evidence tab merges observations, notes, questions, sources chronologically
- ✅ Analysis tab merges hypotheses, data tools, reports
- ✅ Research Session shortcut card on Projects tab

### 2.5 Research Session — ✅ NEW

**FieldMindResearchSession.kt** provides:
- ✅ Session start with optional name and project link
- ✅ Running timer with pulsing indicator
- ✅ Quick observation input (subject + facts + category)
- ✅ Observation count tracking
- ✅ Session summary with duration and observation count
- ✅ Navigation route wired in `FieldMindNavigation`

### 2.6 Security Settings — ✅ NEW

**SecuritySettingsPage** in `FieldMindSettingsScreen.kt`:
- ✅ App lock toggle (moved from Backup & Import)
- ✅ Lock timeout setting (Immediate/1min/5min/15min/When screen off)
- ✅ Auto-lock on background toggle
- ✅ Informational card about privacy
- ✅ Dedicated navigation route `field_settings_security`

### 2.7 Weather & GPS Settings — ✅ Settings Added

New settings in `FieldMindSettings.kt`:
- ✅ `autoWeatherEnabled` — toggle for weather API integration
- ✅ `gpsMode` — "Off" / "On capture only" / "Background weather only"
- ✅ `lockTimeout` — Security lock timeout
- ✅ `autoLockOnBackground` — Auto-lock when app backgrounded
- ❌ Weather API integration (Open-Meteo) — settings only, no API calls yet
- ❌ Weather entity fields — not added to ObservationEntity yet

---

## 3. 🔴 Critical Issues

### 3.1 Monolithic Screens File — Partially Resolved

The original 262K+ character `FieldMindScreens.kt` was split into 12 files. However:
- `FieldMindDialogs.kt` is **967 lines** and growing
- `FieldMindLibraryScreen.kt` is **620 lines**
- `FieldMindObserveScreen.kt` is **690 lines** — needs splitting for capture redesign
- The navigation wildcard import pattern makes dependency tracking harder

### 3.2 No Dependency Injection — Unchanged 🔴

`FieldMindViewModel` extends `AndroidViewModel` and directly instantiates repository and settings. No unit testing possible.

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

| Issue | Severity | Status |
|-------|----------|--------|
| Single ViewModel (god object) | 🟡 High | ⚠️ Unchanged |
| Single DAO (60+ queries) | 🟡 High | ⚠️ Unchanged |
| Single entities file (21 types) | 🟡 Medium | ⚠️ Unchanged |
| Single repository (70+ methods) | 🟡 Medium | ⚠️ Unchanged |
| Tag sync can drift | 🟡 Medium | ⚠️ Unchanged |
| Settings uses SharedPreferences | 🟡 Medium | ⚠️ Unchanged (37 keys now) |
| Database uses destructive fallback | 🟡 High | ⚠️ DB bumped to v7 |
| Inconsistent card radius | 🟢 Low | ⚠️ Unchanged |
| No loading/error states | 🟡 Medium | ⚠️ Unchanged |
| OpenAI uses v1/responses endpoint | 🟡 Medium | ⚠️ Unchanged |

---

## 5. 🎨 UI/UX Audit — prompt.md Screen-by-Screen Implementation Status

### 5.1 Home Screen — ❌ Not yet redesigned

| Aspect | Current State | prompt.md Vision | Status |
|--------|--------------|-----------------|--------|
| Icon size | 32dp | 64×64dp minimum | ❌ Not started |
| Label size | titleSmall | Larger, bolder | ❌ Not started |
| Tap targets | Standard Material | 64×64dp minimum | ❌ Not started |
| Feel | Dashboard toolbar | Open field journal | ❌ Not started |
| Active state | Basic clickable | Subtle ripple/active state | ❌ Not started |
| Weather widget | None | Current conditions | ❌ Not started |
| Research Session CTA | None | Prominent button | ❌ Not started |
| Empty states | Passive text | Guided prompts | ❌ Not started |

### 5.2 Capture Screen — 🟡 Partially redesigned

| Aspect | Before | After (Current) | Status |
|--------|--------|-----------------|--------|
| Camera | Basic 3:4 box | CameraV2: full-screen, zoom, focus, grid, timer | ✅ Done |
| Post-capture | None | Bottom sheet: Observation/Question/Just Save | ✅ Done |
| Category | REQUIRED before writing | Still required in form | ❌ Not started |
| Required fields | Subject + Facts required | Still required | ❌ Not started |
| "What's next?" prompt | None | None after form save | ❌ Not started |
| Time to first capture | ~30+ seconds | Faster with CameraV2 | 🟡 Improved |
| Advanced metadata | Always visible | Still always visible | ❌ Not collapsed |

### 5.3 CameraX — ✅ Fully redesigned

| Aspect | Before | After (Current) | Status |
|--------|--------|-----------------|--------|
| Layout | 3:4 box | Full-screen immersive | ✅ Done |
| Zoom | None | Pinch-to-zoom + slider | ✅ Done |
| Focus | None | Tap-to-focus with ring | ✅ Done |
| Grid | None | Rule-of-thirds toggle | ✅ Done |
| Timer | None | 3s/5s/10s countdown | ✅ Done |
| Aspect ratio | Fixed 3:4 | 4:3/16:9/1:1 toggle | ✅ Done |
| Post-capture | None | Bottom sheet with 3 options | ✅ Done |
| Annotation | None | Not yet implemented | ❌ Not started |
| Auto metadata | None | Settings added, API pending | 🟡 Partial |

### 5.4 Workspace (Projects) — ✅ Redesigned

| Aspect | Before | After (Current) | Status |
|--------|--------|-----------------|--------|
| Project creation | 11-field form | Name + Question → Create | ✅ Done |
| Tab structure | 5 tabs | 3 tabs (Projects/Evidence/Analysis) | ✅ Done |
| Guided flow | ResearchFlowGuide wizard | Removed — freeform | ✅ Done |
| Evidence view | None (separate tabs) | Unified chronological list | ✅ Done |
| Research Session | None | Shortcut card + full screen | ✅ Done |
| Project attachments | None | ❌ Not yet added | ❌ Not started |

### 5.5 Notes Screen — ❌ Not yet redesigned

| Aspect | Current State | prompt.md Vision | Status |
|--------|--------------|-----------------|--------|
| Entry | Category selection first | Start writing immediately | ❌ Not started |
| Required fields | Title OR body | None required | ❌ Not started |
| Category | Prominent chips | Optional metadata | ❌ Not started |
| Connections | Limited | Link to project/source | 🟡 Already supported |

### 5.6 Library (Knowledge Hub) — ❌ Not yet redesigned

| Aspect | Current State | prompt.md Vision | Status |
|--------|--------------|-----------------|--------|
| Name | "Library" | "Knowledge Hub" | ❌ Not renamed |
| Structure | 5 tabs (Sources/Notes/Reading/Flashcards/Learn) | Connected, not siloed | ❌ Not unified |
| PDF reader | WebView fallback only | Inline PDF viewer | ❌ Not started |
| Sources form | 15+ fields | Progressive disclosure | ❌ Not started |

### 5.7 Insights Screen — ❌ Not yet redesigned

| Aspect | Current State | prompt.md Vision | Status |
|--------|--------------|-----------------|--------|
| Content | Charts + achievements | "What am I discovering?" patterns | ❌ Not started |
| Empty states | Widget placeholders | Guided prompts | ❌ Not started |
| Patterns | None | Repeated observations, themes | ❌ Not started |
| Weather trends | None | Historical weather data | ❌ Not started |

### 5.8 Settings — ✅ Security Section Added

| Aspect | Before | After (Current) | Status |
|--------|--------|-----------------|--------|
| Privacy lock | In Backup & Import | Dedicated Security page | ✅ Done |
| Lock timeout | None | Immediate/1m/5m/15m/screen off | ✅ Done |
| Auto-lock | None | Background auto-lock toggle | ✅ Done |
| GPS mode | None | "Off"/"On capture only"/"Background weather" | ✅ Done |
| Weather toggle | None | Auto-weather enabled setting | ✅ Done |

### 5.9 Global Issues

| Issue | Status |
|-------|--------|
| Category chip overload | 🟡 Partially reduced in workspace |
| Visual hierarchy | 🟡 Partially improved |
| Connections as core | 🟡 Already supported, not prominent |
| Empty states | 🟡 Improved in workspace, not elsewhere |

---

## 6. 🔵 Missing Features — New Requirements

### 6.1 Real Weather Report — ❌ Settings only, API not integrated

**Status:** Settings added (`autoWeatherEnabled`, `gpsMode`), but no Open-Meteo API integration, no weather entity fields in ObservationEntity, no weather widget on Home screen.

**Implementation needed:**
- Add `WeatherApiService.kt` with Open-Meteo REST calls
- Add weather fields to `ObservationEntity` (temperature, humidity, wind, cloud cover)
- Add weather widget to HomeScreen
- Add WorkManager periodic weather cache

### 6.2 GPS Tracking — ✅ Already on-demand

`FieldLocationProvider` already uses on-demand GPS. Settings added for GPS mode. No continuous tracking exists.

### 6.3 CameraX — ✅ Mostly complete

See section 5.3. Remaining: quick annotation, auto GPS/weather metadata.

### 6.4 Offline Automatic Question Preparation — ❌ Not implemented

Rule-based question generation after each observation. Needs a new `QuestionGenerator` utility.

### 6.5 Research Session — ✅ Implemented

See section 2.5. Remaining: historical sessions list, DAO methods for session queries, linking observations to sessions.

### 6.6 Remove Guided Research Flow — ✅ Done

`ResearchFlowGuide` removed from `FieldMindProjectsScreen.kt`.

### 6.7 Privacy Lock as Separate Security Section — ✅ Done

See section 2.6.

### 6.8 Quick Capture Redesign — 🟡 Partially done

CameraV2 post-capture flow is evidence-first. The observation form itself still has required fields and prominent category chips.

### 6.9 Workspace Redesign — ✅ Done

3-tab structure, simplified creation, removed guided flow.

### 6.10 PDF Reader — ❌ Not implemented

Currently uses WebView fallback or Google Docs viewer. Needs native PDF rendering.

### 6.11 Project Attachments — ❌ Not implemented

Projects should support file attachments (photos, PDFs, documents) linked directly.

### 6.12 Multiple Observation Types — ✅ Supported

Observations, notes, questions, sources, data records, reports — all supported. Research Session adds rapid multi-observation.

---

## 7. 🔄 Duplicate / Fragmented Implementations

### 7.1 Backup Workers — Unchanged

`FieldMindBackupWorker` in infrastructure is orphaned. Remove it.

### 7.2 Streak Tracking — Unchanged

`FieldMindStreakWorker` in infrastructure is orphaned. Remove it.

### 7.3 Camera Components — ⚠️ Two versions exist

`FieldMindCameraCapture.kt` (legacy) and `FieldMindCameraV2.kt` (new) both exist. The ObserveScreen now uses V2. Legacy should be deprecated or removed.

---

## 8. 📱 Screen-by-Screen Redesign Plan — Implementation Audit

| Screen | Plan Status | Implementation |
|--------|-------------|----------------|
| Home Screen | ❌ Plan exists, not implemented | Needs: bigger icons, weather widget, session CTA |
| Capture Screen | 🟡 Partially implemented | CameraV2 done; form needs evidence-first redesign |
| CameraX | ✅ Fully implemented | CameraV2 with all features |
| Workspace | ✅ Fully implemented | 3 tabs, simplified creation, no guided flow |
| Notes Screen | ❌ Plan exists, not implemented | Needs: write-first, no category barrier |
| Library / Knowledge Hub | ❌ Plan exists, not implemented | Needs: rename, unify, PDF reader |
| Insights Screen | ❌ Plan exists, not implemented | Needs: patterns, themes, weather trends |
| Settings / Security | ✅ Fully implemented | Security page with timeout, auto-lock |
| Research Session | ✅ Fully implemented | Timer, quick input, summary |
| Detail Screen | ❌ Plan exists, not implemented | Needs: entity-specific layouts, related items |

---

## 9. 🔧 Full Restructuring Plan

### Phase 1: Critical Fixes — ❌ Not started

| # | Task | Status |
|---|------|--------|
| 1 | Remove duplicate workers | ❌ Not started |
| 2 | Fix settings init side-effects | ❌ Not started |
| 3 | Fix database destructive fallback | ⚠️ DB bumped to v7, fallback still active |
| 4 | Fix OpenAI v1/responses endpoint | ❌ Not started |

### Phase 2: Security & Weather — ✅ Partially done

| # | Task | Status |
|---|------|--------|
| 5 | Create Security settings page | ✅ Done |
| 6 | Integrate Open-Meteo weather API | ❌ Not started |
| 7 | Add weather fields to ObservationEntity | ❌ Not started |
| 8 | Add GPS mode settings | ✅ Done |
| 9 | Add weather widget to Home screen | ❌ Not started |

### Phase 3: CameraX Redesign — ✅ Done

| # | Task | Status |
|---|------|--------|
| 10 | Full-screen immersive camera | ✅ Done |
| 11 | Add pinch-to-zoom | ✅ Done |
| 12 | Add tap-to-focus | ✅ Done |
| 13 | Add grid overlay | ✅ Done |
| 14 | Add capture timer | ✅ Done |
| 15 | Add aspect ratio toggle | ✅ Done |
| 16 | Add post-capture bottom sheet | ✅ Done |
| 17 | Add auto metadata | 🟡 Settings only |

### Phase 4: Capture Redesign — 🟡 Partially done

| # | Task | Status |
|---|------|--------|
| 18 | Redesign capture flow | 🟡 CameraV2 done, form needs work |
| 19 | Add "What's next?" prompt | ❌ Not started |
| 20 | Make category optional | ❌ Not started |
| 21 | Add one-tap quick capture | ❌ Not started |
| 22 | Add quick annotation | ❌ Not started |

### Phase 5: Workspace Redesign — ✅ Done

| # | Task | Status |
|---|------|--------|
| 23 | Remove ResearchFlowGuide | ✅ Done |
| 24 | Simplify project creation | ✅ Done |
| 25 | Restructure tabs | ✅ Done |
| 26 | Add Research Session mode | ✅ Done |
| 27 | Add offline question generation | ❌ Not started |

### Phase 6: Research Session — ✅ Done

| # | Task | Status |
|---|------|--------|
| 28 | Create ResearchSessionEntity | ✅ Done |
| 29 | Create ResearchSessionScreen | ✅ Done |
| 30 | Add session-to-observation linking | ❌ Not started (DAO methods missing) |
| 31 | Add session summary view | ✅ Done (inline) |
| 32 | Add historical sessions list | ❌ Not started |

### Phase 7: Architecture Improvements — ❌ Not started

| # | Task | Status |
|---|------|--------|
| 33 | Add Hilt/Dagger | ❌ Not started |
| 34 | Split FieldMindDao.kt | ❌ Not started |
| 35 | Split FieldEntities.kt | ❌ Not started |
| 36 | Split FieldMindRepository.kt | ❌ Not started |
| 37 | Migrate settings to Proto DataStore | ❌ Not started |
| 38 | Add FTS virtual tables | ❌ Not started |

### Phase 8: UI Polish — ❌ Not started

| # | Task | Status |
|---|------|--------|
| 39 | Standardize design system | ❌ Not started |
| 40 | Add skeleton loaders | ❌ Not started |
| 41 | Add error states | ❌ Not started |
| 42 | Improve empty states | ❌ Not started |
| 43 | Reduce chip overload | ❌ Not started |
| 44 | Fix visual hierarchy | ❌ Not started |

### Phase 9: Advanced Features — ❌ Not started

| # | Task | Status |
|---|------|--------|
| 45 | Add undo/redo | ❌ Not started |
| 46 | Add bulk operations | ❌ Not started |
| 47 | Add timeline view | ❌ Not started |
| 48 | Add templates | ❌ Not started |
| 49 | Cloud sync | ❌ Not started |
| 50 | Export to Zotero/Mendeley | ❌ Not started |
| 51 | Implement local on-device model | ❌ Not started |
| 52 | Add PDF reader (androidx.pdf) | ❌ Not started |
| 53 | Add project attachments | ❌ Not started |
| 54 | Add camera annotation | ❌ Not started |

---

## 10. Priority Matrix

### ✅ Completed (This Session)

| Task | Phase | Status |
|------|-------|--------|
| Security settings page | P2 | ✅ Done |
| GPS mode settings | P2 | ✅ Done |
| CameraV2 full redesign | P3 | ✅ Done |
| Workspace 3-tab redesign | P5 | ✅ Done |
| Remove guided research flow | P5 | ✅ Done |
| Simplify project creation | P5 | ✅ Done |
| Research Session screen | P6 | ✅ Done |
| ResearchSessionEntity | P6 | ✅ Done |
| DB bumped to v7 | P1 | ✅ Done |
| CameraV2 integration in ObserveScreen | P4 | ✅ Done |

### 🔴 P0 — Must Fix Next

| Task | Effort | Impact |
|------|--------|--------|
| Remove duplicate workers | Low | Critical |
| Fix settings init side-effects | Low | Critical |
| Capture form evidence-first redesign | High | Major |
| Home screen redesign | High | Major |

### 🟡 P1 — High Priority

| Task | Effort | Impact |
|------|--------|--------|
| Weather API integration (Open-Meteo) | Medium | High |
| Weather entity fields | Medium | High |
| Notes screen redesign | Medium | High |
| Library → Knowledge Hub rename | Medium | High |
| PDF reader integration | High | High |
| Project attachments | Medium | High |
| Offline question generation | Medium | High |
| Session observation linking (DAO) | Low | High |
| Historical sessions list | Low | Medium |

### 🟢 P2 — Medium Priority

| Task | Effort | Impact |
|------|--------|--------|
| Quick annotation on photos | High | Medium |
| Skeleton loaders | Low | Medium |
| Error states | Low | Medium |
| Empty states improvement | Low | High |
| Chip overload reduction | Medium | High |
| Timeline view | Medium | Medium |

### 🔵 P3 — Future

| Task | Effort | Impact |
|------|--------|--------|
| Hilt/Dagger DI | High | Critical |
| Split DAO/entities/repo | Medium | High |
| DataStore migration | Medium | High |
| FTS search | Medium | High |
| Cloud sync | High | High |
| Undo/redo | Medium | Medium |
| Templates | Medium | Medium |
| Zotero/Mendeley export | Medium | Low |

---

## 11. Key Metrics

| Metric | Before | After |
|--------|--------|-------|
| Total entities | 19 (10 data + 9 xref) | 21 (11 data + 10 xref) |
| Room database version | 6 | 7 ✅ |
| Settings keys | 33 | 37 ✅ |
| Screen files | 14 | 15 (+ ResearchSession) |
| Camera components | 1 (basic) | 2 (legacy + V2) |
| Workspace tabs | 5 | 3 ✅ |
| Guided flow | Present | Removed ✅ |
| Security settings | In Backup | Dedicated page ✅ |
| Research sessions | Missing | Implemented ✅ |
| AI providers | 2 | 2 |
| Export formats | 8 | 8 |
| Navigation routes | 17+ | 19+ ✅ |
| SM-2 implementation | Complete ✅ | Complete ✅ |
| Privacy lock | Complete ✅ | Complete ✅ |
| Haptic feedback | Present ✅ | Present ✅ |
| Tablet adaptation | Present ✅ | Present ✅ |
| Dark mode | Present ✅ | Present ✅ |
| Dependency injection | Missing ❌ | Missing ❌ |
| Unit tests | Missing ❌ | Missing ❌ |
| Weather integration | Missing ❌ | Settings only 🟡 |
| PDF reader | Missing ❌ | WebView fallback 🟡 |
| Project attachments | Missing ❌ | Missing ❌ |
| Camera annotation | Missing ❌ | Missing ❌ |

---

## 12. 🆕 New Research-Based Suggestions

Based on research into KoBoToolbox, Survey123, Fulcrum, and field research best practices:

### 12.1 Behavioral Event Logger (Timer-Based)

**What:** A dedicated behavioral observation tool where users tap buttons for different behaviors (e.g., "Grooming", "Eating", "Moving") while a master session timer runs. This is essential for ethological field research.

**Implementation:**
- New `EventLoggerEntity` with `sessionId`, `behaviorTag`, `startMs`, `endMs`
- New `EventLoggerScreen` with big tap buttons for each behavior
- Background service to survive app sleep
- Export as CSV with timestamps

### 12.2 PDF Viewer with Annotation

**What:** Use `androidx.pdf:pdf-viewer-fragment` for native PDF rendering with stylus support, search, zoom, and annotation.

**Implementation:**
- Add `androidx.pdf:pdf-viewer-fragment` dependency
- Create `PdfViewerScreen` composable
- Support opening PDFs from sources, attachments, and shared files
- Add annotation layer for marking up research PDFs

### 12.3 Offline-First Data Sync Pattern

**What:** Even though FieldMind is local-first, prepare for future cloud sync with proper patterns:
- UUID-based content hashing for deduplication
- Sync-ready entity fields (`syncedAt`, `syncId`)
- WorkManager sync task that only runs on WiFi

### 12.4 Camera Annotation Layer

**What:** After capturing a photo, allow drawing circles, arrows, and text annotations directly on the image.

**Implementation:**
- Custom Canvas overlay on captured bitmap
- Tools: circle, arrow, text, freehand
- Save annotated version alongside original
- Use `MotionEvent` for touch input → `Path` objects → render onto `Bitmap`

### 12.5 Weather API Integration (Open-Meteo)

**What:** Free, no-API-key weather data from Open-Meteo using device GPS coordinates.

**Implementation:**
```kotlin
// Open-Meteo API (no key needed)
GET https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true
```
- Temperature, wind speed, wind direction, cloud cover, weather code
- Cache for 6 hours per location
- Attach to observations automatically
- Show on Home screen

### 12.6 Progressively Revealed Data Tools

**What:** Instead of showing all 8 data tools at once, reveal them as the user's research grows:
- Starter: Counter, Measurement Log
- Intermediate: Checklist, Event Log, Weather Log
- Advanced: Species Tracker, Site Log, Comparison Table

### 12.7 Side-by-Side Hypothesis Comparison

**What:** Let users view two hypotheses next to each other to compare predictions, evidence criteria, and results.

### 12.8 Observation Templates

**What:** Pre-built templates for common field scenarios:
- "Species Observation" — subject, habitat, behavior, weather, photo
- "Water Quality" — site, pH, temperature, clarity, turbidity
- "Weather Station" — temperature, humidity, wind, clouds, precipitation
- "Transect Survey" — location, distance, species count per interval

---

*This analysis was updated on June 13, 2026 after implementing Phases 2-6 + 9 changes on branch `feat/fieldmind-research-redesign` (commit `3e478af7`). CameraV2, workspace redesign, security settings, research sessions, and DB v7 migration are now live. Remaining work: capture form redesign, home screen, notes, library, weather API, PDF reader, project attachments.*
