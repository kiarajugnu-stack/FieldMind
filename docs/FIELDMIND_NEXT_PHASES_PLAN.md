# FieldMind Next Phases — Comprehensive Implementation Plan

> **Date:** June 14, 2026
> **Branch:** `fix/pr-64-build-fixes` (PR #65)
> **Status:** Phases 1-12 (Complete) → Next Phase Groups

---

## Current State Summary

Phases 1-12 delivered a comprehensive field research workspace:

- ✅ **Observation capture** — Evidence-first UI, live timer, structured fields, quality scores, protocols
- ✅ **Research sessions** — Timer-based multi-observation mode with pause/resume, evidence tools, notifications
- ✅ **Projects workspace** — 5 in-screen tabs, dashboard with metrics, templates, research method builder
- ✅ **Detail screens** — Entity-specific layouts, media carousel, weather chips, provenance, re-observation linking
- ✅ **Settings** — Profile, appearance, capture, AI, local model, security, backup, units, map, data integrity, developer
- ✅ **Home screen** — Weather centerpiece, daily goal, session CTA, widget grid, data tools, timeline, session grouping
- ✅ **Map screen** — OSM-based observation map with full-screen mode
- ✅ **Library & sources** — Source management, notes, paper reading, flashcards (SM-2), learning paths
- ✅ **Reports** — 7 report types, builder, templates, exports (PDF, HTML, Markdown, CSV, JSON)
- ✅ **Data tools** — Counter, measurement, species tracking, weather logging, comparison tables
- ✅ **Hypotheses** — Status tracking, confidence bars, linked questions, evidence support
- ✅ **Insights** — Calendar heatmap, radar chart, activity charts, research health, network graph
- ✅ **Evidence Hub** — Bulk selection, filtering, completeness indicator, grid/list view
- ✅ **Data workspace** — Question-first collection, quick tally, data record cards
- ✅ **Notes & Journal** — Rich block editor, templates, observation embeds
- ✅ **Weather** — Open-Meteo API, sunrise/sunset, moon phase, conditions nudge

---

## Key Gaps Identified

After thorough codebase analysis, the following gaps exist:

### Group A: 🔬 Species Identification Engine (CRITICAL)
**Current state:** Species is a text field with manual confidence selector. No ML/API integration.
**What's missing:**
- On-device ML Kit object detection + TFLite model
- iNaturalist API integration for species ID
- Post-capture "Identify" button with top-5 matches
- Species database with bundled metadata
- **New files needed:** `SpeciesClassifier.kt`, `SpeciesDatabase.kt`, `SpeciesIdentificationSheet.kt`

### Group B: 🗺️ Offline Maps with Drawing Tools (PRO FEATURE)
**Current state:** OSM maps display observation points. No offline caching or drawing tools.
**What's missing:**
- Tile caching (download OSM tiles for study area)
- Drawing tools: polygon (survey boundary), line (transect), point (site)
- Track recording (GPS path log during research session)
- Geo-fencing (reminders at marked sites)
- **New files needed:** `OfflineTileManager.kt`, `MapDrawingTools.kt`, `TrackRecorder.kt`, `GeoFenceReminder.kt`

### Group C: 🔗 Hypothesis-Driven Observation Graph (UNIQUE)
**Current state:** Graph visualization exists (static on Insights). `connectionMap` field on ProjectEntity unused.
**What's missing:**
- Live graph inference — suggesting related hypotheses as observations are added
- Weak signal detection — "These observations might support Hypothesis #2"
- Gap detection — "You're testing H1 but missing observations for alternative H3"
- Question generation — "Based on your observations, you might ask..."
- Citation chains — Track evidence used to support each hypothesis
- **New files needed:** `GraphInferenceEngine.kt`, `WeakSignalDetector.kt`

### Group D: 📄 Native PDF Reader
**Current state:** WebView + Google Docs Viewer fallback. No native PDF capabilities.
**What's missing:**
- Native PDF renderer (e.g., PdfRenderer/PDFium)
- Inline annotations (highlight, underline, notes)
- Table of contents navigation
- Full-text search within PDF
- Continuous scroll + page thumbnails
- Replace `LearnReaderScreen` WebView approach

### Group E: 📁 Project Attachments & File Management
**Current state:** `attachmentUris` field on ProjectEntity but no organized UI. Observations have evidence attachments.
**What's missing:**
- Attach photos, PDFs, documents to projects (beyond the raw URI field)
- Organize by folder/type within projects
- Quick preview in project detail
- Drag-and-drop reordering
- File size tracking and management

### Group F: 🛠️ Home Screen Data Tools — Interactive Tool UIs
**Current state:** Count/Measure/Weather/Species tiles on home screen all navigate to generic DataTools screen.
**What's missing:**
- **Counter tool** — Dedicated full-screen counter UI with +/-/reset, tally history, auto-save per press
- **Measurement tool** — Inline measurement form with number fields, unit selector, save button
- **Weather tool** — Quick weather log form with temperature, humidity, wind, condition picker
- **Species tool** — Species quick-capture form with name, count, behavior, confidence
- Tiles should open dedicated mini-tools instead of generic DataTools screen

### Group G: 🔧 Observation Session Polish & Redesign
**Current state:** Well-built but could be improved.
**What's missing/enhancement:**
- Batch observation mode (save multiple in succession without closing form)
- Improved session flow between QuickObservationForm and evidence capture
- `parentObservationId` selector in the capture form (set re-observation links during capture, not just in detail)
- Smarter auto-tagging based on observation history
- Observation templates integration with the protocol system

---

## Implementation Groups

### Group 1 (START HERE): Home Screen Interactive Data Tools
**Files to create:** `FieldMindCounterTool.kt`, `FieldMindMeasurementTool.kt`, `FieldMindWeatherTool.kt`, `FieldMindSpeciesTool.kt`
**Files to modify:** `FieldMindHomeScreen.kt` (wire tiles to open dedicated tools instead of generic DataTools)

### Group 2: Observation Session Enhancement
**Files to modify:** `FieldMindObserveScreen.kt`, `FieldMindScreenUtils.kt`
**Files to create:** None new — enhance existing

### Group 3: Species Identification Engine
**Files to create:** `SpeciesClassifier.kt`, `SpeciesDatabase.kt`, `SpeciesIdentificationSheet.kt`
**Files to modify:** `FieldMindObserveScreen.kt` (add Identify button after capture), `FieldMindDetailScreen.kt` (show species ID results)

### Group 4: Offline Maps with Drawing Tools
**Files to create:** `OfflineTileManager.kt`, `MapDrawingTools.kt`, `TrackRecorder.kt`, `GeoFenceReminder.kt`
**Files to modify:** `FieldMindMapScreen.kt` (integrate drawing tools)

### Group 5: Hypothesis-Driven Observation Graph
**Files to create:** `GraphInferenceEngine.kt`, `WeakSignalDetector.kt`
**Files to modify:** `InsightsScreen.kt`, `HypothesesPhase8.kt`, `FieldMindDetailScreen.kt`

### Group 6: Native PDF Reader
**Files to create:** Native `FieldMindPdfRenderer.kt` + annotation components
**Files to modify:** `FieldMindLibraryScreen.kt` (LearnReaderScreen), `LibraryPhase12.kt`

### Group 7: Project Attachments & File Management
**Files to create:** Project attachment management UI
**Files to modify:** `FieldMindProjectsScreen.kt`, `FieldMindDetailScreen.kt` (ProjectDetailContent), `ProjectPhase5Components.kt`

---

## Starting with Group 1: Home Screen Interactive Data Tools

### Sub-tasks:
1. Create `FieldMindCounterTool.kt` — Full counter UI with +/-/reset, auto-save to data records
2. Create `FieldMindMeasurementTool.kt` — Measurement form with number fields, unit selector
3. Create `FieldMindWeatherTool.kt` — Quick weather log with temp/humidity/wind/condition
4. Create `FieldMindSpeciesTool.kt` — Species capture with name/count/behavior/confidence
5. Wire tiles in `FieldMindHomeScreen.kt` to open dedicated tool screens
6. Add routes in `FieldMindNavigation.kt`
7. Update changelog
8. Review, commit, push
