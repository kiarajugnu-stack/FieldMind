# 🚀 Phase 4 — Next-Gen Features: Species ID, Offline Maps, Enhanced Observation & More

> **Branch:** `fix/pr-64-build-fixes`
> **Date:** June 14, 2026
> **Status:** Planned

---

## 📋 Overview

Phase 4 tackles the most impactful missing features that separate FieldMind from professional field research tools. The focus is on **on-device intelligence**, **offline mapping**, **observation session redesign**, and **home screen data tool improvements**.

---

## 📦 Group 1: 🔬 Species Identification Engine (CRITICAL)

**Why:** iNaturalist dominates citizen science because species ID is the primary UX. FieldMind can win in the professional/institutional space with on-device + API hybrid approach.

### Implementation

#### 1A. ML Kit / TensorFlow Lite Species Classifier
- **File:** `app/src/main/java/chromahub/rhythm/app/features/field/data/vision/SpeciesClassifier.kt`
- Bundled TensorFlow Lite model (500 common species, expandable via download)
- On-device inference with confidence scoring
- Fallback to iNaturalist API when online and user opts in

#### 1B. Species Database
- **File:** `app/src/main/java/chromahub/rhythm/app/features/field/data/vision/SpeciesDatabase.kt`
- Bundled species metadata (common name, scientific name, category, image URL)
- Expandable via downloadable regional packs

#### 1C. Species Identification UI
- **File:** `app/src/main/java/chromahub/rhythm/app/features/field/presentation/screens/SpeciesIdentificationSheet.kt`
- Bottom sheet with top-5 matches with confidence scores
- Photo preview and quick-add to observation
- Manual search by common/scientific name

#### 1D. Integration with Observation Capture
- Post-capture → "Identify species" button in the evidence row
- Auto-populate species name, category, and confidence
- Save as structured observation with species metadata

---

## 📦 Group 2: 🌍 Offline Maps with Drawing Tools

**Current:** Basic OSM map with GPS markers (FieldMindMapScreen.kt)
**Target:** Full offline-capable GIS tool

### Implementation

#### 2A. Offline Tile Manager
- **File:** `OfflineTileManager.kt`
- Download OSM tile regions for offline use
- LRU cache with pruning
- Multiple tile sources: OSM, Satellite (Mapbox/Bing with cache)

#### 2B. Map Drawing Tools
- **File:** `MapDrawingTools.kt`
- Polygon (survey boundary), line (transect), point (site) overlays
- Tap-to-place and drag-to-resize
- Color-coded by entity type

#### 2C. Track Recorder
- **File:** `TrackRecorder.kt`
- GPS path logging during research session
- Start/stop with elapsed time
- Save tracks as GeoJSON

#### 2D. Geo-Fence Reminder
- **File:** `GeoFenceReminder.kt`
- WorkManager proximity alerts for marked sites
- "You're near Site A — log an observation?"

---

## 📦 Group 3: 🔗 Hypothesis-Driven Observation Graph

**Current:** Basic backlinks exist but no live graph inference
**Target:** Semantic matching of observations to hypotheses

### Implementation

#### 3A. Graph Inference Engine
- **File:** `GraphInferenceEngine.kt`
- Semantic matching of observation text to hypotheses
- Suggests related hypotheses when adding observations

#### 3B. Weak Signal Detector
- **File:** `WeakSignalDetector.kt`
- Statistical correlation detection across observations
- "These 3 observations might support Hypothesis #2"

#### 3C. Gap Detection & Question Generation
- "You're testing H1 but missing observations for H3"
- "Based on your observations, you might ask..."

#### 3D. UI: Enhanced Detail Screen
- "Related" section showing graph connections
- Interactive knowledge graph with tap-to-navigate

---

## 📦 Group 4: 📄 PDF Reader — Native Renderer

**Current:** URL-based reader (WebView fallback)
**Target:** Native PDF rendering with annotations

### Implementation

#### 4A. Native PDF Renderer
- `androidx.pdf:pdf-viewer-fragment` integration
- Continuous scroll + page thumbnails
- Table of contents navigation

#### 4B. Inline Annotations
- Highlight, underline, and sticky notes
- Save annotations to Room database
- Export annotated PDF

#### 4C. Full-Text Search
- Search within PDF content
- Results with page number and context snippet

---

## 📦 Group 5: 📁 Project Attachments

**Current:** Projects have `attachmentUris` string field but no dedicated UI
**Target:** Rich file management for projects

### Implementation

#### 5A. Attachment Manager
- Attach photos, PDFs, documents to projects
- Organize by folder/type
- Quick preview in project detail

#### 5B. Folder Organization
- Create folders within projects
- Drag-and-drop reordering
- Bulk operations

---

## 📦 Group 6: 🏠 Home Screen Data Counter Improvements

**Current:** HomeDataOptionsCard shows Count, Measure, Weather, Species but tools are basic
**Target:** Full-featured dedicated tool screens that open from the home screen

### Improvements

#### 6A. Counter Tool Enhancement
- **Current:** Basic tally with +/-/reset
- **Add:** Session grouping, running total, chart view, export to CSV

#### 6B. Measurement Tool Enhancement
- **Current:** Simple value + unit entry
- **Add:** Measurement history chart, batch entry, unit conversion

#### 6C. Weather Log Tool Enhancement
- **Current:** Manual condition picker
- **Add:** Open-Meteo auto-fetch, 7-day history chart, export

#### 6D. Species Tool Enhancement
- **Current:** Basic species name + count form
- **Add:** Auto-categorization, ID suggestions, photo upload, behavior logging

---

## 📦 Group 7: 📝 Observation Session Redesign

**Current:** Research session exists with timer, quick observation, evidence tools
**Target:** Professional field session with structured protocol support, live species ID, track recording

### Redesign

#### 7A. Structured Protocol Integration
- Session template presets (Point Count, Transect, Water Quality)
- Step-by-step guidance through protocol steps
- Protocol completion tracking

#### 7B. Live Species ID in Session
- Camera capture → instant species suggestion
- Quick-add identified species to session feed

#### 7C. Track Recording in Session
- GPS track log during session
- Visualize path on mini-map in session summary

#### 7D. Session Summary Enhancement
- Map of all observation locations
- Species list with counts
- Duration breakdown
- One-tap export to report

#### 7E. Session Feed Improvements
- Chronological feed with thumbnails
- Filter by species/category
- Inline editing of quick observations

---

## 📦 Group 8: 🧪 Additional Form & Field Improvements

### 8A. Numpad-Only Fields
- Dedicated numpad keyboard for measurement fields
- Configurable decimal places and units
- Quick +/- stepper buttons

### 8B. Re-Observation Linking
- "Re-observe" button on existing observation
- Links new observation as child of previous
- Shows chain in detail screen

### 8C. Required Fields per Category
- Different required fields based on category
- Species category requires species name + confidence
- Water category requires clarity + flow

---

## 🗺️ Implementation Order

| Group | Description | Effort | Impact | Dependencies |
|-------|-------------|--------|--------|--------------|
| **1** | Species Identification Engine | 🔴 High | 🔴 Critical | Camera V2 done |
| **7** | Observation Session Redesign | 🔴 High | 🔴 Critical | Groups 5 done |
| **6** | Home Data Counter Improvements | 🟡 Medium | 🔴 High | None |
| **2** | Offline Maps with Drawing Tools | 🔴 High | 🟡 High | OSM integrated |
| **8** | Form & Field Improvements | 🟡 Medium | 🟡 High | None |
| **3** | Hypothesis-Driven Observation Graph | 🟡 Medium | 🟡 High | None |
| **4** | PDF Reader | 🟡 Medium | 🟡 Medium | None |
| **5** | Project Attachments | 🟡 Medium | 🟡 Medium | None |

## ✅ What's Already Completed (Previous Phases)

### Groups 1-5 (Phase 3 Redesign)
| Group | Description | Status |
|-------|-------------|--------|
| **1** | Projects & Dashboard — 5 tabs, project dashboard with sampling-effort | ✅ |
| **2** | Detail screen revamp — hero carousel, compact weather, provenance | ✅ |
| **3** | Home screen — animated weather centerpiece, moon phase, conditions nudge | ✅ |
| **4** | Settings — Units/format, map, data integrity, developer pages | ✅ |
| **5** | Templated protocols (5 built-in), re-observation linking, numpad fields | ✅ |

---

*Plan generated for Phase 4 implementation — `fix/pr-64-build-fixes`*
