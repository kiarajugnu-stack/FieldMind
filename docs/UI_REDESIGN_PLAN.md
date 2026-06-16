# FieldMind UI Redesign — Comprehensive Implementation Plan

## Overview

Three major feature areas specified:
1. **Research Hub Screen** — New screen (replaces/extends ProjectsScreen)
2. **Observation Timeline / My Observations** — Full redesign with 4 view modes
3. **Project Management** — Full detail, builders, data modules, relationship linking

---

## AREA 1: Research Hub Screen

### Status: Needs New Screen

The spec describes a dedicated "Research Hub" screen that doesn't exist as a standalone entity. Currently, the **ProjectsScreen** serves as the workspace with 5 tabs (Overview, Observations, Hypotheses, Data, Reports).

#### What Exists:
- `FieldMindProjectsScreen.kt` — `ProjectsScreen` with 5 tabs, dashboard metrics, project cards
- `ResearchSessionScreen.kt` — Timer-based research session
- Navigation has `FieldMindScreen.Projects` mapped to `ProjectsScreen`

#### What's Needed:
| Element | Current Status | Action |
|---------|---------------|--------|
| Research Hub header | Not exists | New composable `ResearchHubScreen` |
| "Start Research Session" button | Exists in ProjectsScreen overview tab | Move/duplicate to hub |
| [+ New Project] | Exists in-line in ProjectsScreen | Keep |
| Templates grid (Bird Survey, Plant Study, etc.) | 5 templates exist in ProjectsScreen | Expand to 18 types + 17 templates |
| Overview/Questions/Hypothesis/Data/Reports tabs | 5 tabs exist in ProjectsScreen | Already matches spec |
| Project cards with metrics (obs, Qs, sources, datasets, reports, sessions) | Exists as `ProjectDashboardCardCompact` | Already shows all metrics |

#### Implementation:
1. Create `FieldMindResearchHubScreen.kt` as a new top-level screen
2. Add `FieldMindScreen.ResearchHub` to navigation
3. Port existing ProjectsScreen content into it
4. Add the template grid with 18 project types + 17 templates from spec
5. Keep the tab structure (Overview, Questions, Hypothesis, Data, Reports)

---

## AREA 2: Observation Timeline / My Observations

### Status: Partial

#### What Exists:
- `ObserveScreen` in `FieldMindObserveScreen.kt` — Capture screen, no observation list
- `InsightsScreen` in `FieldMindInsightsScreen.kt` — Dashboard with charts, timeline preview
- `ObservationTimelinePreview` in `FieldMindHomeScreen.kt` — Date-grouped preview (limited)
- `ArchiveScreen` in `FieldMindArchiveScreen.kt` — Basic search with text filter
- `MapFieldScreen` in `FieldMindMapScreen.kt` — Full map screen with drawing tools

#### What's Needed:

**A) List View (default):**
| Element | Current Status | Action |
|---------|---------------|--------|
| Search bar | Exists in ArchiveScreen | Reuse pattern |
| [List] [Gallery] [Map] [Cal] tabs | Not exists | New segmented control |
| Filter/Sort/Select toolbar | Not exists | New toolbar composable |
| Observation cards (photo, species, meta) | `EntityCard` exists but simpler | New `ObservationTimelineCard` with full layout |
| [+ Observe] FAB | Not in observations view | Add FAB |

**B) Advanced Filter Sheet:**
| Filter | Status |
|--------|--------|
| Species, Category, Confidence | New bottom sheet |
| Date Range, Location Radius | New |
| Habitat, Weather, Duration | New |
| Tags, Projects, Evidence Type | New |
| AI Identified, Favorites, Drafts, Re-observations | New |

**C) Gallery View:**
- New photo grid view filtering observations with image attachments

**D) Map View:**
- Exists as `MapFieldScreen` but needs integration with observation filtering

**E) Calendar View:**
- `CalendarHeatmap` exists in InsightsScreen
- Need month/week/day view with observation dots

**F) Timeline View:**
- `ObservationTimelinePreview` exists in HomeScreen
- Need full-screen version with Morning/Afternoon/Evening grouping

**G) Observation Statistics Dashboard:**
- Large parts already exist in `InsightsScreen`
- Need dedicated stats screen with species distribution, activity graph, etc.

#### Implementation:
1. Create new `FieldMindObservationsScreen.kt` as dedicated screen
2. Add view-mode segmented control (List/Gallery/Map/Calendar)
3. Build `AdvancedFilterSheet` as modal bottom sheet
4. Implement each view mode as separate composable
5. Reuse existing components where possible (CalendarHeatmap, MaplibreMapView, EntityCard)

---

## AREA 3: Project Management

### 3A: Project Creation Screen

#### What Exists:
- Inline project creation in `ProjectsScreen.kt` (`InlineFormCard`)
- `NewProjectDialog` in `FieldMindDialogs.kt`

#### What's Needed (per spec):
| Element | Status |
|---------|--------|
| Project Title | Exists |
| Project Type (19 types) | 5 exist, need 14 more |
| Template selector (17 templates) | 5 exist, need 12 more |
| Project Icon picker (emoji) | Not exists |
| Description | Not exists (only objective/question) |
| Research Category dropdown | Not exists |
| Priority (Low/Medium/High) | Not exists |
| Status dropdown (Planning →) | Exists as "Active" default |
| Start/End Date | Not exists |
| Team Members | Not exists |
| Tags | Not exists |
| Research Question Builder | Exists partially |
| Hypothesis Builder | Exists in dialogs |
| Project Tasks Builder | Not exists |
| Species Registry Builder | Not exists |
| Research Method Builder | Exists in ProjectsScreen |
| Data Collection Modules checklist | Not exists |

### 3B: Project Detail View (Tabbed)

#### What Exists:
- `ProjectDetailContent` in `DetailScreen.kt` — Single card with all fields
- `BacklinksPanel` showing connected entities

#### What's Needed (per spec):
- **Top action bar**: Edit/Share/More menu
- **Tab layout**: Overview, Questions, Hypothesis, Observations, Evidence, Species, Tasks, Sessions, Data, Sources, Reports, Team, Activity
- **Overview Tab**: Project statistics (10 metrics), Quick actions, Project health
- **Questions Tab**: Research Questions with [Add], linked counts
- **Hypothesis Tab**: Hypothesis cards with status, confidence
- **Observations Tab**: Filtered observation list
- **Evidence Tab**: Evidence counts (Photo/Audio/Video/PDF/Notes)
- **Species Tab**: Species registry with taxonomy, conservation status
- **Tasks Tab**: Task list with priority, due date, subtasks
- **Sessions Tab**: Research sessions linked to project

### 3C: Builders (per spec)

| Builder | Current Status | Action |
|---------|---------------|--------|
| Research Questions Builder | Basic field in dialogs | Full builder with type, priority, linked species |
| Hypothesis Builder | Exists in dialogs | Add confidence slider, success criteria |
| Project Tasks Builder | Not exists | New: title, type, priority, due date, assignee, subtasks |
| Species Registry Builder | Not exists | New: full taxonomy, conservation, auto-count |
| Research Method Builder | Partial in ProjectsScreen | Expand with categories |
| Weather Logging Builder | Exists as WeatherLogTool | Integrate into project |
| Audio Survey Builder | Not exists | New |
| GPS Track Builder | Exists in MapScreen | Integrate |
| Source Library | Exists in LibraryScreen | Integrate into project |

### 3D: Data Collection Modules (checklist from spec)
- Species Counter ✅ (SpeciesTool)
- Weather Logging ✅ (WeatherLogTool)
- GPS Tracking ✅ (MapScreen/TrackRecorder)
- Audio Recording ✅ (in ObserveScreen)
- Video Recording ✅ (in ObserveScreen)
- Habitat Assessment ❌ New
- Camera Trap Import ❌ New
- Manual Notes ✅ (note system)
- Sketch Pad ❌ New
- Measurement Collection ✅ (MeasurementTool)
- Water Testing ❌ New
- Vegetation Sampling ❌ New
- Behavioral Tracking ❌ New
- Specimen Documentation ❌ New

### 3E: Relationship Links (per spec)
All entities support cross-linking. Currently:
- Question ↔ Hypothesis ✅ (via LinkedListQuestionId)
- Question ↔ Observation ✅ (via QuestionObservationCrossRef)
- Observation ↔ Project ✅ (via ProjectObservationCrossRef)
- Observation ↔ Session ✅ (via SessionObservationCrossRef)
- Source ↔ Project ✅ (via ProjectSourceCrossRef)
- Data ↔ Project ✅ (via ProjectDataRecordCrossRef)
- Report ↔ Project ✅ (via projectId field)

Missing:
- Question ↔ Species / Observation ↔ Species (no SpeciesEntity table!)
- Hypothesis ↔ Data / Hypothesis ↔ Reports
- Task ↔ everything (no TaskEntity table!)
- Evidence ↔ Reports / Evidence ↔ Questions
- Species ↔ Questions / Species ↔ Hypotheses

---

## Database Schema Gaps

| Missing Table | Related Features |
|--------------|------------------|
| `TaskEntity` | Tasks, Subtasks, Project Tasks |
| `SpeciesEntity` | Species Registry, Conservation Status |
| `SpeciesObservationCrossRef` | Species-observation linking |
| `HypothesisDataCrossRef` | Hypothesis-data linking |
| `HypothesisReportCrossRef` | Hypothesis-report linking |
| `QuestionSpeciesCrossRef` | Question-species linking |
| `TaskObservationCrossRef` | Task-observation linking |
| `TaskEvidenceCrossRef` | Task-evidence linking |
| `EvidenceReportCrossRef` | Evidence-report linking |
| `TeamMemberEntity` | Team members for projects |

---

## Implementation Phases (Recommended Order)

### Phase 1: Observations Timeline Screen
- Create `FieldMindObservationsScreen.kt` (list view)
- Build observation cards matching spec layout
- Add search bar, filter/sort toolbar
- Create `AdvancedFilterSheet` modal
- Add view-mode segmented control (list → gallery → map → cal)
- Create `ObservationStatsDashboard` composable

### Phase 2: Research Hub Screen
- Create `FieldMindResearchHubScreen.kt`
- Expand project types (5 → 19)
- Expand templates (5 → 17)
- Add emoji icon picker
- Add priority, dates, tags to project creation
- Create Project Tasks Builder, Species Registry Builder

### Phase 3: Project Detail Tabbed View
- Restructure `ProjectDetailContent` into tabbed layout
- Build species registry, tasks, team, activity tabs
- Build data collection modules checklist (from spec 14 modules)
- Integrate weather logging, GPS tracking into project view

### Phase 4: Database Schema & Relationship Linking
- Add `TaskEntity`, `SpeciesEntity` tables
- Add cross-ref tables for all relationship pairs
- Create ViewModel methods for each
- Build UI in detail screens for relationship visualization

---

## Current Strengths to Preserve

1. **ProjectsScreen tabs** — Already has 5-tab layout matching spec
2. **InsightsScreen analytics** — Rich charts, metrics, achievements already built
3. **DetailScreen routing** — Pattern for opening entity details is solid
4. **MapScreen** — Full MapLibre with offline tiles, drawing, tracks, geofences
5. **ResearchSessionScreen** — Timer-based capture with pause/resume
6. **SpeciesIdentificationSheet** — Camera-based identification with search

---

## Estimation

- **Phase 1** (Observations Timeline): ~1,200-1,800 lines across 2-3 new files
- **Phase 2** (Research Hub): ~800-1,200 lines across 1-2 new files
- **Phase 3** (Project Detail): ~1,500-2,500 lines across 3-4 modified files
- **Phase 4** (Database): ~400-600 lines across 4-6 modified files
