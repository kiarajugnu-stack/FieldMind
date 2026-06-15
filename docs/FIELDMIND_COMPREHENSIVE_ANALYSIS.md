# FieldMind — Complete Feature Analysis & Improvement Plan

> Generated: June 15, 2026

---

## Executive Summary

FieldMind is an ambitious field research app with impressive breadth: Observations, Evidence, Projects, Hypotheses, Questions, Data Notes, Sources, Reports, Flashcards, Research Sessions, Weather, Maps (offline tiles/drawing/tracks/geofences), Species ID, AI assistant, Backup/Export, Lock screen, Streaks, Achievements, and a Learning library. However, many features feel "phase-planned but partially implemented" — skeletons of great ideas that need UX polish, bug fixes, and feature completion.

---

## Part 1: Feature-by-Feature Analysis

### Observations (The Core Feature)

**Current State:** Good foundation.
- Evidence-first capture (camera/gallery/file/audio)
- Quick observation form with subject, facts, category, confidence
- Structured details (species confidence, distance, checklist, measurements)
- Timer integration
- Weather + GPS auto-attachment
- Re-observation chains (parent/child linking)

**What's Missing (compared to iNaturalist/eBird):**
- ❌ No batch editing — can't select multiple observations and tag/categorize them
- ❌ No observation templates — researchers reuse protocols; no way to save a "Bird Survey" template
- ❌ No observation maps gallery — eBird/iNat show photos on a map grid
- ❌ No species checklist completion — "you've seen 12/45 bird species this month"
- ❌ No observation quality scoring that works — QualityScoreCard exists but doesn't drive decisions
- ❌ No observation export as CSV/Excel — only Markdown export exists
- ❌ No observation search by image — can't search by photo content
- ❌ No observation sharing URL — no deep link to share a single observation

**How It Should Behave:**
- Home screen "Capture" → Opens camera immediately (not a form)
- User takes photo → Toast appears centered horizontally and vertically (not top-left)
- User is prompted with category picker (fullscreen bottom sheet)
- After category selection, auto-navigates to new observation detail with photo pre-attached
- GPS/weather auto-fetched in background
- User fills subject + facts → Save
- Species identification auto-runs after photo is taken (background), showing results as non-blocking chip

### Evidence (Attachments & Media Gallery)

**Current State:** Camera, gallery, file picker, audio recording — all work. Attachments stored in DB.

**What's Missing:**
- ❌ No in-app PDF viewer — opens externally or via WebView's broken PDF rendering
- ❌ No photo editor — no crop, rotate, annotate, or highlight on photos
- ❌ No audio player — can record audio but can't play it back in-app
- ❌ No evidence map — no way to see all photo locations on a map
- ❌ No bulk evidence export — can't export all photos from a project

**How It Should Behave:**
- Evidence should have its own gallery tab (not just a horizontal row)
- Photos viewable full-screen with pinch-to-zoom, swipe between photos
- Audio recordings have play/pause/seek
- PDFs render inline (use Android PDF renderer API, not WebView)
- Camera flow: capture → confirm/retake → category → edit details

### Projects (Research Workspace)

**Current State:** Most polished feature.
- 5 tabs: Overview, Observations, Hypotheses, Data, Reports
- Dashboard metrics (total obs, this week, sites, field hours, sessions)
- Project templates
- Method builder
- Entity linking

**What's Missing:**
- ❌ No Gantt chart / timeline view — research happens over time
- ❌ No project milestones — no way to mark phases
- ❌ No project sharing — can't collaborate
- ❌ No data collection status — "12/30 transects completed"
- ❌ No project notebook — free-form journal within a project

### Hypotheses

**Current State:** Phase 8 components exist but feel unfinished.

**What's Missing:**
- ❌ No hypothesis testing workflow — no flow from Test → Collecting → Analysis → Conclusion
- ❌ No evidence linking UI — no visual indicator of supporting observations
- ❌ No prediction accuracy tracking
- ❌ No Bayesian updates — confidence should update as evidence accumulates
- ❌ No hypothesis visualization — tree/matrix with evidence strength

**How It Should Behave:**
- Hypothesis card shows: prediction, current confidence, supporting observations count, contradicting observations count
- "Test this hypothesis" button creates a focused observation session
- Saving an observation suggests which hypotheses it relates to

### Questions

**Current State:** Good auto-builder, filters, stats.

**What's Missing:**
- ❌ No question-answer workflow — answering should generate a mini-report
- ❌ No explicit question-to-observation linking
- ❌ No priority-based sorting on home screen
- ❌ No recurring questions — auto-create daily observation prompts

### Data (Notes / Data Records)

**Current State:** Counter, measurement, weather log, species tools — functional.

**What's Missing:**
- ❌ No data visualization in tools — charts exist in Insights but not in tool UIs
- ❌ No data export as CSV
- ❌ No data validation — can type anything in value field
- ❌ No data aggregation — "average temperature this week"
- ❌ No data comparison — side-by-side comparison

### Sources (Library / Reading)

**Current State:** Comprehensive source management with DOI, citation, files, reading status, notes.

**What's Missing:**
- ❌ No working PDF viewer — WebView PDF approach is broken
- ❌ No Zotero/Mendeley integration
- ❌ No citation export (BibTeX/CSL)
- ❌ No annotation system — can't highlight and save as notes
- ❌ No reading progress tracking — "page 45/120"

### Reports

**Current State:** Markdown-based report builder with templates.

**What's Missing:**
- ❌ No PDF generation — reports are Markdown but can't export as proper PDF
- ❌ No IMRAD structure (Introduction, Methods, Results, Discussion)
- ❌ No figure/table insertion
- ❌ No citation insertion — can't add `(Author, 2023)` references
- ❌ No abstract generation
- ❌ No DOCX export

### Research Paper Maker (New Feature Request)

Needs to be built from the ground up. Should include:
- **IMRAD template** (Introduction, Methods, Results, Discussion, Conclusion)
- **Citation manager** — pull from Sources library, insert `(Author, Year)`
- **Figure insertion** — embed photos, charts, data tables inline
- **Auto-abstract** — generate from intro + conclusion
- **PDF export** — proper PDF with header, page numbers, TOC
- **DOCX export** — for journal submission
- **LaTeX export** — for academic submission
- **Collaborative editing** — or at least sharing drafts
- **Reference list auto-generation** — from cited sources
- **Revision history** — track changes

### Home Screen Capture Flow (Specific Bug)

**Current Issue:** Home "Capture" button navigates to ObserveScreen. User wants:
1. Tap Capture → **Camera opens immediately** (not the form)
2. Take photo → **Toast appears centered** (currently top-left via Snackbar)
3. **Category picker** dialog appears
4. → Navigate to observation detail with photo attached

**Fix Required:**
- Move camera capture before the form
- Change snackbar positioning from top to center
- Add category picker as full-screen bottom sheet after capture

### Species Identification (Doesn't Work)

**Current Issue:** `SpeciesClassifier` has a **placeholder** `placeholderInference()` returning random confidence values. TFLite model is never implemented — code says `// TODO: In production, load and run TFLite Interpreter here`. Fallback species dictionary works for search but "Identify from photo" returns simulated confidences.

**Fix Required:**
- Either implement actual TFLite model inference
- Or integrate with a real species API (iNaturalist API, PlantNet API)
- Or at minimum show an error message instead of fake confidence values

### WebView Usage (For Images & PDFs)

**Current Issue:** `LearnReaderScreen` uses WebView for everything:
- PDFs: opens in WebView with Google Docs viewer (broken)
- Images: opens in WebView (should use proper image viewer)
- URLs: WebView is appropriate here

**Fix Required:**
- Images: Use `AsyncImage` + pinch-to-zoom composable (remove WebView)
- PDFs: Use Android `PdfRenderer` API or `OpenDocument` intent
- Keep WebView only for HTML web pages (articles, papers)

---

## Part 2: Competitive Analysis

| Feature | FieldMind | iNaturalist | eBird | Epicollect5 | Fulcrum | What Users Want |
|---------|-----------|-------------|-------|-------------|---------|-----------------|
| Offline-first | ✅ | ✅ | ✅ | ✅ | ✅ | Critical |
| Photo ID | ⚠️ (broken) | ✅ (AI) | ❌ | ❌ | ❌ | High demand |
| Sound recording | ✅ | ❌ | ❌ | ❌ | ❌ | Unique advantage |
| Custom forms | ⚠️ (partial) | ❌ | ❌ | ✅ | ✅ | Needed for research |
| Data export | ⚠️ (Markdown) | ✅ (CSV/GeoJSON) | ✅ (CSV) | ✅ (CSV) | ✅ (Excel) | Critical |
| Research papers | ❌ | ❌ | ❌ | ❌ | ❌ | **Huge gap** |
| PDF annotation | ❌ | ❌ | ❌ | ❌ | ❌ | **Huge gap** |
| Offline maps | ✅ | ❌ | ❌ | ❌ | ✅ | Niche but powerful |
| Community ID | ❌ | ✅ | ✅ | ❌ | ❌ | Users want this |
| API access | ❌ | ✅ | ✅ | ✅ | ✅ | Researchers need |
| Drawing/sketch | ⚠️ (map only) | ❌ | ❌ | ❌ | ❌ | Niche but useful |

---

## Part 3: Prioritized Implementation Plan

### Phase 1 — Critical Fixes (1-2 weeks)

| # | Task | Files Affected | Priority |
|---|------|---------------|----------|
| 1 | Fix Species ID — implement real TFLite inference or API | `SpeciesClassifier.kt` | 🔴 HIGH |
| 2 | Fix Home Capture Flow — Camera → Toast(centered) → Category → Edit | `FieldMindHomeScreen.kt`, `FieldMindObserveScreen.kt` | 🔴 HIGH |
| 3 | Fix Toast Position — move snackbar from top-left to center | `FieldMindSnackbar.kt` | 🔴 HIGH |
| 4 | Fix PDF Viewer — replace WebView with PdfRenderer | `FieldMindLibraryScreen.kt` | 🔴 HIGH |
| 5 | Fix Image Viewer — replace WebView with pinch-to-zoom | `FieldMindLibraryScreen.kt` | 🔴 HIGH |
| 6 | Fix compile errors (variable shadowing, try/catch, missing onError) | Various | 🔴 HIGH |

### Phase 2 — Research Paper Maker (2-3 weeks)

| # | Feature | Key Components |
|---|---------|---------------|
| 1 | Paper Entity — new Room entity with IMRAD fields | `PaperEntity.kt`, DB migration |
| 2 | Paper Editor — rich text editor with Markdown | `PaperEditorScreen.kt` |
| 3 | Citation Manager — insert citations from Sources | Citation picker bottom sheet |
| 4 | Figure/Table Insertion — embed charts and photos | Media picker + chart snapshot |
| 5 | PDF Export — proper PDF with iText or Android Canvas | `PaperExporter.kt` |
| 6 | DOCX/LaTeX Export — alternative formats | Apache POI / template engine |
| 7 | Auto-Abstract — Gemini/OpenAI generated summary | `GeminiResearchAssistant.kt` |

### Phase 3 — Feature Completion (2-3 weeks)

| # | Feature | Description |
|---|---------|-------------|
| 1 | Observation Templates | Save/load reusable observation protocols |
| 2 | Photo Editor | Crop, rotate, annotate photos before saving |
| 3 | Audio Player | Play back field recordings in-app |
| 4 | Data CSV Export | Export data records as CSV/Excel |
| 5 | Report PDF Export | Export reports as proper formatted PDF |
| 6 | Reading Progress | Track pages read in sources |
| 7 | Hypothesis Evidence Linking | Link observations to hypotheses with evidence strength |

### Phase 4 — Advanced Features (3-4 weeks)

| # | Feature | Description |
|---|---------|-------------|
| 1 | Sketch/Drawing Tool | Canvas-based annotation for photos and maps |
| 2 | Observation Gallery Map | Photo grid with location pins |
| 3 | Data Charts in Tool | Real-time charts in measurement/counter tools |
| 4 | Question Auto-Observation | "Track this question" generates daily prompts |
| 5 | Project Timeline | Gantt chart for project milestones |
| 6 | Batch Operations | Select multiple observations → tag/categorize/archive |
| 7 | Zotero Integration | Sync sources with Zotero API |

### Phase 5 — Polish & Professional (2-3 weeks)

| # | Feature | Description |
|---|---------|-------------|
| 1 | API Access | REST API for external tools |
| 2 | Collaboration | Share projects with other users |
| 3 | Deep Links | Share individual observations via URL |
| 4 | iNaturalist Import | Import observations from iNaturalist |
| 5 | Community ID | Submit observations to iNaturalist for expert ID |
| 6 | Web Dashboard | Companion web app for desktop analysis |
| 7 | Citation Export | BibTeX, RIS, CSL export from Sources |

---

## Part 4: Specific UI/UX Changes

### Camera Capture Flow (From Home)

```
Current: Home → ObserveScreen(form) → Camera → Save
Desired: Home → Camera → Photo taken → Toast(center) → Category picker → Detail/edit → Save
```

**Implementation:**
1. Home "Capture" button → launches camera via `FieldMindCameraV2` directly
2. On photo capture → center-positioned toast/snackbar "Photo captured"
3. Immediately show full-screen bottom sheet with category options
4. On category selection → navigate to observation detail with photo pre-attached
5. User fills subject + facts → Save

### Toast/Snackbar Position Change

Current `FieldMindSnackbarOverlay` is at `Alignment.TopCenter`. Change to `Alignment.Center` for capture confirmations.

---

## Part 5: WebView Cleanup

**Remove WebView for:**
1. Images in ObservationAttachmentsPanel
2. PDFs in SourcePreviewPanel
3. Any `content://` URIs

**Keep WebView for:**
1. HTML article URLs (free web pages)
2. YouTube embedded previews (in SourcePreviewCard)

---

## Summary

| Category | Count | Urgency |
|----------|-------|---------|
| Critical bugs to fix | 6 | 🔴 Immediate |
| Research Paper Maker (new) | 7 | 🟠 Next |
| Feature completion | 7 | 🟡 Within 2 weeks |
| Advanced features | 7 | 🟢 Within 1 month |
| Polish/Professional | 7 | 🔵 Within 2 months |

**Total: ~34 significant changes across the app**
