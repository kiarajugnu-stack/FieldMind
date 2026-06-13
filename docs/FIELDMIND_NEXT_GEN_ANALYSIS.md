# 🔬 FieldMind App — Next-Generation Redesign & Feature Analysis

> **Date:** June 13, 2026  
> **Version:** 0.9.0  
> **Status:** Comprehensive redesign plan & implementation prompt  
> **Branch:** `feat/fieldmind-redesign-plan-v2`  
> **Previous PR:** #35 (Research Dashboard, Charts, Data Table)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Quick Capture Redesign](#2-quick-capture-redesign)
3. [Observation Timing & Live Counter](#3-observation-timing--live-counter)
4. [Collapsible Categories & Multi-Select Presets](#4-collapsible-categories--multi-select-presets)
5. [Number-Only Input Fields](#5-number-only-input-fields)
6. [Research Session & Workshop Redesign](#6-research-session--workshop-redesign)
7. [Create Project UI Redesign](#7-create-project-ui-redesign)
8. [Result Analysis Page (Black Screen)](#8-result-analysis-page-black-screen)
9. [Live Data Workshop](#9-live-data-workshop)
10. [Category-Specific Data Records](#10-category-specific-data-records)
11. [Auto-Build Reports](#11-auto-build-reports)
12. [Share as PDF with Images](#12-share-as-pdf-with-images)
13. [Question Page & Auto Question Builder](#13-question-page--auto-question-builder)
14. [Library Redesign (Sources, Notes, Reading, Learn)](#14-library-redesign)
15. [Map Improvements](#15-map-improvements)
16. [CameraX Pro Mode & UI Fixes](#16-camerax-pro-mode--ui-fixes)
17. [Home Screen Redesign](#17-home-screen-redesign)
18. [Research UI Redesign (Detail Pages)](#18-research-ui-redesign-detail-pages)
19. [Priority Roadmap](#19-priority-roadmap)

---

## 1. Executive Summary

FieldMind has evolved from a basic research notebook into a comprehensive field research platform. The next phase focuses on:

- **Making capture faster and more intuitive** — Quick Capture with Snap Notes, live timers, collapsible categories, number-only fields
- **Making research sessions powerful** — Workshop mode with live timers, auto-question generation, proper result analysis
- **Making data useful** — Category-specific fields, interactive pivot tables, auto-built reports, PDF sharing with images
- **Making the library a knowledge hub** — Built-in PDF reader, redesigned notes UI, proper reading/learn experience
- **Fixing map and camera UX** — Proper OSM tiles with place names, auto-fit bounds, CameraX pro settings, zoom slider repositioning
- **Polishing every screen** — Home page research session CTA, project creation simplified, question page restored with AI builder

---

## 2. Quick Capture Redesign

### Current State
- `ObserveScreen.kt` has a multi-step flow: Choose Mode → Choose Category → Snap/Note
- Camera launches as a Dialog overlay (separate window from form)
- Categories are required BEFORE writing
- No inline edit after snap

### Redesigned Flow

```
┌──────────────────────────────────────────────┐
│  [Large Camera Button]  [Gallery]  [Mic]      │
│                                                │
│  ┌─────────────── Evidence Preview ──────────┐ │
│  │  [Thumbnail]  Caption:_______________     │ │
│  │  [Thumbnail]  Caption:_______________     │ │
│  └──────────────────────────────────────────┘ │
│                                                │
│  ┌─── Snap Details (auto-expand) ───────────┐ │
│  │  Subject: ________________________________│ │
│  │  Category: [🌿 Plant] [🐦 Bird] [+more]  │ │
│  │  Category presets: [Collapse ▼]           │ │
│  │  Timing: [⏱ 00:00] ▸                   │ │
│  │  Tags: ___________________________________│ │
│  │  [Save] [Cancel] [Edit Evidence]          │ │
│  └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

### Key Changes

**A. Evidence-First Layout:**
- Camera, gallery, and file attachments are the PRIMARY action at the top
- After capturing evidence, the form auto-expands
- "Snap Note" — capture a photo, immediately annotate with drawing tools or text overlay, then save with metadata

**B. Snap Note Editing Inside:**
- After capturing a photo, user can:
  - Draw circles/arrows/annotations on the photo (Canvas overlay)
  - Add text labels
  - Crop the image
  - Add caption
- All within the same screen, no Dialog separation

**C. Smart Defaults:**
- Auto-suggest category based on recent observations (last 5 categories used)
- Auto-fill time and date from device
- Auto-suggest tags from recent usage

**D. Quick Snap from Home Screen Widget:**
- Already have `FieldMindQuickCaptureWidget` — wire it to launch ObserveScreen directly in Snap mode with pre-selected category

### Implementation Files
- `FieldMindObserveScreen.kt` — Major rewrite of the capture flow
- `FieldMindCameraV2.kt` — Add photo annotation mode after capture
- `FieldMindComponents.kt` — Add annotation tools (draw, text, arrow)

---

## 3. Observation Timing & Live Counter

### Current State
- Stopwatch exists in `ObservationCaptureCard` but starts/stops manually
- No live counter display in the form header
- No persistent timer across observations

### Redesigned Approach

**A. Live Timer in Capture Form Header:**
```
┌──────────────────────────────────────────┐
│  ⏱ 00:00:00  [▶ Start] [⏸ Pause] [⏹ Reset] │
│  📊 0 observations this session            │
└──────────────────────────────────────────┘
```

**B. Timer Behavior:**
- Timer starts when first observation in session begins
- Timer persists in memory across multiple observations in same session
- Shows elapsed time in real-time (updates every second)
- Counter shows number of observations saved during this session
- Timer auto-resets when user closes the capture screen or explicitly resets

**C. Timer Component:**
```kotlin
@Composable
fun LiveObservationTimer(
    elapsedMs: Long,
    observationCount: Int,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
)
```

**D. Implementation Details:**
- Use `LaunchedEffect` with `delay(1000)` for the timer loop
- Store timer state in `ViewModel` so it survives configuration changes
- Save elapsed time as `durationMs` on each observation
- Show "Session: 3 observations in 12:34" on the save confirmation snackbar

---

## 4. Collapsible Categories & Multi-Select Presets

### Current State
- `ChoiceChips` shows all categories inline (no collapse)
- Single-select only
- Categories are always visible and take up space

### Redesigned Approach

**A. Collapsible Category Group:**
```
┌──────────────────────────────────────────┐
│  Category [▼]  Current: Plant              │
│  ┌──────────────────────────────────────┐ │
│  │ [🌿 Plant] [🐦 Bird] [🪲 Insect]     │ │
│  │ [🪨 Rock] [🌤 Weather] [💧 Water]    │ │
│  │ [👥 Human] [📖 Reading] [🔬 Other]   │ │
│  └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

**B. Multi-Select Presets:**
- User can select MULTIPLE category presets (e.g., "Bird" AND "Weather" for observations that span categories)
- Presets appear as tags/chips after selection
- Auto-populate structured fields based on selected categories

**C. Category Definition System:**
```kotlin
data class CategoryPreset(
    val label: String,
    val icon: MaterialSymbolIcon,
    val structuredFields: List<CategoryField>,
    val defaultUnit: String?,
    val prompt: String
)

val observationCategories = listOf(
    CategoryPreset("Bird", FieldMindIcons.Bird, listOf(
        CategoryField("species", "Species name", "Common or scientific name"),
        CategoryField("count", "Count", "Number of individuals", isNumeric = true),
        CategoryField("behavior", "Behavior", "Feeding, flying, calling..."),
        CategoryField("habitat", "Habitat", "Tree, water, ground...")
    ), "count", "Observe birds with species, count, and behavior"),
    // ... more presets
)
```

**D. Implementation:**
- Wrap `ChoiceChips` in an `AnimatedVisibility` card with collapse button
- Change single-select to multi-select with `Set<String>` state
- Show selected presets as `TagChip` badges in the collapsed header
- On expand, show checkboxes instead of radio-style chips

---

## 5. Number-Only Input Fields

### Current State
- `FieldTextField` accepts any text input
- Numeric fields (like `value`, `count`, `duration`) still show full keyboard

### Redesigned Approach

**A. NumberField Composable:**
```kotlin
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    min: Double? = null,
    max: Double? = null,
    decimalPlaces: Int = 0,
    suffix: String = "",  // e.g., "°C", "cm", "count"
    supportingText: String? = null,
    stepper: Boolean = false  // Show +/- buttons
)
```

**B. Keyboard Types:**
- `KeyboardType.Number` for integers (counts, scores)
- `KeyboardType.Decimal` for measurements (temperature, length)
- `KeyboardType.Phone` for large numbers

**C. Stepper Mode:**
- Show [+] and [-] buttons alongside the field
- For counters: big +/- buttons with animated number transition
- For measurements: smaller increment/decrement by 0.1 or 1

**D. Input Validation:**
- Filter non-numeric characters on input
- Show error if value outside min/max range
- Auto-format: `1000` → `1,000`, `0.5` → `0.50`

**E. Implementation:**
- Add `NumberField` to `FieldMindComponents.kt`
- Update all numeric fields in ObserveScreen, Dialogs, and DataRecord forms
- Add `KeyboardType` parameter to `FieldTextField`

---

## 6. Research Session & Workshop Redesign

### Current State
- `ResearchSessionScreen` has timer + quick observation form
- No workshop mode with tools
- No session history view
- Observations not properly linked to sessions in DAO

### Redesigned Approach

**A. Workshop Mode:**
```
┌──────────────────────────────────────────────┐
│  🔬 Research Workshop                        │
│  ───────────────────────────────────────────  │
│  ⏱ Session: 00:45:23 ■ [End]                │
│  📊 12 observations | 3 photos | 0 notes     │
│                                                │
│  ┌─ Quick Log ────────────────────────────┐  │
│  │  Subject: ___________________________  │  │
│  │  [🐦 Bird] [🌿 Plant] [General]        │  │
│  │  Facts: _____________________________  │  │
│  │  _______________________________       │  │
│  │  [📷 Snap] [📝 Note] [➕ Count]       │  │
│  │  [Save Observation]                    │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Live Feed ────────────────────────────┐  │
│  │  🐦 Crow on wire          12:34        │  │
│  │  🌿 Fern frond            12:28        │  │
│  │  🌤 Overcast, 22°C        12:15        │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  [📊 Summary] [📤 Export Session] [🗑 Clear]  │
└──────────────────────────────────────────────┘
```

**B. Workshop Features:**
- Big, tappable category buttons (one-tap observation per category)
- Quick Snap: camera launches directly with current category preselected
- Quick Count: +/- buttons for species counts
- Live Feed: scrollable list of observations captured in this session
- Summary: end-of-session report with all observations, stats, duration

**C. Session Linking:**
- Add DAO methods: `getObservationsForSession(sessionId)`, `getAllSessions()`
- Add `ResearchSessionHistoryScreen` listing past sessions with stats
- Add session export as CSV/Markdown

**D. Add to Home Screen:**
- Prominent "Research Workshop" card on HomeScreen (above the widget grid)
- Shows last session stats: "Your last session: 12 obs in 45 min"
- One-tap to start new session

---

## 7. Create Project UI Redesign

### Current State
- `NewProjectDialog` has 11 form fields across 4 sections
- The dialog is full-screen and overwhelming
- `ProjectsTab` uses simplified creation (title + question) but the dialog still has all fields

### Redesigned Approach

**A. Minimal Creation (Inline):**
```
┌──────────────────────────────────────────┐
│  New Project                              │
│                                                │
│  Project Title: [____________________]    │
│  Research Question: [________________]    │
│  Topic: [Biology] [Geology] [Wildlife]    │
│                                                │
│  [Cancel] [Create Project]               │
│                                                │
│  ── or start with a template ──           │
│  [📋 Species Survey] [🌡 Weather Study]   │
└──────────────────────────────────────────┘
```

**B. Progressive Disclosure:**
- Start with just Title + Question + Topic (like the simplified version already done)
- Add a "+ Add details" button that reveals optional fields (objective, methods, background)
- Add template system: pre-built project templates with pre-filled fields

**C. Template Presets:**
```kotlin
data class ProjectTemplate(
    val name: String,
    val icon: MaterialSymbolIcon,
    val objective: String,
    val researchQuestion: String,
    val methods: String,
    val dataFields: List<String>
)
```

---

## 8. Result Analysis Page (Black Screen)

### Current State
- No dedicated analysis page after observing
- "Start Observing" button leads to the standard capture screen
- No "black screen" focused analysis mode

### Redesigned Approach

**A. Focused Analysis Mode:**
```
┌──────────────────────────────────────────────┐
│  [← Back]  [📊 Analysis Mode]  [⚙ Filter]   │
│                                                │
│  ┌─ Key Metrics ──────────────────────────┐  │
│  │  24 observations    3 categories       │  │
│  │  2 questions raised  1 hypothesis       │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Category Breakdown ──────────────────┐  │
│  │  [Bar Chart: Bird ████████ 12]        │  │
│  │  [Bar Chart: Plant ████ 6]            │  │
│  │  [Bar Chart: Insect ███ 3]            │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ AI Analysis ─────────────────────────┐  │
│  │  "Your bird observations peak in the   │  │
│  │  morning hours. Consider comparing     │  │
│  │  species diversity across sites."      │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Questions Generated ────────────────┐  │
│  │  ❓ Do crow visits correlate with...  │  │
│  │  ❓ What's the species diversity...   │  │
│  │  [Save as Questions] [Dismiss]       │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  [📤 Export Analysis] [📝 Add Note]         │
└──────────────────────────────────────────────┘
```

**B. Features:**
- Dark/black background for focus (like a presentation mode)
- Shows immediate stats from the current session
- AI-generated insights (if AI provider configured)
- Auto-generated questions from observations
- Quick export to share analysis
- "Add Note" to record immediate reflections

**C. Implementation:**
- New composable: `AnalysisResultScreen.kt`
- Linked from "Start Observing" button and session end
- Uses existing charts from `FieldMindChartsExtended.kt`
- Wire to AI assistant for auto insights

---

## 9. Live Data Workshop

### Current State
- Data tools in Analysis tab are basic: add record form + entity cards
- No unified workspace with multiple chart types
- No category-specific data views

### Redesigned Approach

**A. Data Workshop Layout:**
```
┌──────────────────────────────────────────────┐
│  [←]  📊 Live Data Workshop                  │
│                                                │
│  ┌─ Data Entry ──────────────────────────┐  │
│  │  Category: [Bird]                     │  │
│  │  ┌────────────────────────────────┐   │  │
│  │  │ Species:    [Crow] ████ 12    │   │  │
│  │  │ Count:      [___] +/-         │   │  │
│  │  │ Location:   [Garden]          │   │  │
│  │  │ Notes:      [________________]│   │  │
│  │  └────────────────────────────────┘   │  │
│  │  [➕ Add Entry] [📤 Batch Import]     │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Live Charts ─────────────────────────┐  │
│  │  [Bar ▼] [Line ▼] [Pie ▼] [Table ▼]  │  │
│  │  ┌────────────────────────────────┐   │  │
│  │  │   [Interactive chart canvas]  │   │  │
│  │  └────────────────────────────────┘   │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Data Table ─────────────────────────┐  │
│  │  Species    │ Count │ Date           │  │
│  │  ──────────│──────│──────          │  │
│  │  Crow       │ 12   │ Jun 13         │  │
│  │  Sparrow    │ 5    │ Jun 13         │  │
│  │  [Sort ▼] [Filter ▼] [Export CSV]  │  │
│  └──────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

**B. Category-Specific Data Forms:**
```kotlin
// When category = "Bird", show:
val birdFields = listOf(
    DataField("species", "Species", DataType.Text),
    DataField("count", "Count", DataType.Number),
    DataField("behavior", "Behavior", DataType.Choice(listOf("Feeding", "Flying", "Calling", "Resting", "Nesting"))),
    DataField("habitat", "Habitat", DataType.Text),
    DataField("weather", "Weather", DataType.Text)
)

// When category = "Water Quality", show:
val waterFields = listOf(
    DataField("site", "Site", DataType.Text),
    DataField("pH", "pH Level", DataType.Decimal),
    DataField("temperature", "Temperature (°C)", DataType.Decimal),
    DataField("turbidity", "Turbidity", DataType.Choice(listOf("Clear", "Slightly cloudy", "Cloudy", "Opaque"))),
    DataField("flowRate", "Flow Rate", DataType.Text)
)
```

**C. Use Cases for Charts:**
- Species counts → Bar chart (default)
- Temperature over time → Line chart
- Category distribution → Pie/Donut chart
- Behavior breakdown → Stacked bar
- Site comparison → Grouped bar
- Time series → Line + moving average

---

## 10. Category-Specific Data Records

### Current State
- `DataRecordEntity` has generic fields: toolType, label, value, unit, location, notes
- No category-specific fields
- All data records look the same regardless of what's being measured

### Redesigned Approach

**A. Dynamic Field System:**
```kotlin
@Entity
data class DataRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,  // NEW: links to observation category
    val toolType: String,
    val label: String,
    val value: String,
    val unit: String,
    val dynamicFields: String = "{}",  // NEW: JSON blob for category-specific fields
    val location: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val projectId: Long? = null,
    val observationId: Long? = null,
    val sessionId: Long? = null
)
```

**B. Field Definitions by Category:**
| Category | Fields | Default Chart |
|----------|--------|---------------|
| Bird | species, count, behavior, habitat, weather | Bar (species × count) |
| Plant | species, height, coverage, phenology, soil | Bar (species × height) |
| Insect | species, count, lifeStage, hostPlant | Bar (species × count) |
| Rock | type, hardness, color, location, formation | Pie (type distribution) |
| Weather | temperature, humidity, wind, cloudCover, precipitation | Line (temp over time) |
| Water | pH, temperature, clarity, flowRate, depth | Line (pH over time) |
| Human | activity, count, duration, context, location | Stacked bar |
| Reading | source, pageRange, keyIdea, importance | Table |

**C. Multiple Entries View:**
- New `DataEntriesScreen` showing all entries for a specific category
- Grouped by date, location, or project
- Aggregated stats: total count, average value, min/max, etc.
- Quick chart toggle: tap icon to see entries as chart

---

## 11. Auto-Build Reports

### Current State
- `ReportForm` has 11 manual text fields
- Reports are written entirely by hand
- No auto-population from existing data

### Redesigned Approach

**A. Auto-Build Report:**
```
┌──────────────────────────────────────────────┐
│  📄 Build Report                              │
│                                                │
│  Step 1: Select scope                         │
│  ┌─────────────────────────────────────────┐ │
│  │ Project: [Campus Bird Survey ▼]         │ │
│  │ Date range: [Jun 1] to [Jun 13]         │ │
│  │ Include: [✓] Obs [✓] Data [✓] Charts   │ │
│  │          [✓] Questions [✓] Hypotheses   │ │
│  └─────────────────────────────────────────┘ │
│                                                │
│  Step 2: Auto-generate sections              │
│  ┌─────────────────────────────────────────┐ │
│  │ [🔄 Auto-Generate from Project Data]    │ │
│  │                                          │ │
│  │  Background: [auto-filled from project] │ │
│  │  Methods: [auto-filled from project]    │ │
│  │  Observations: 24 records across 3...   │ │
│  │  Results: Charts + tables auto-inserted │ │
│  │  Conclusions: [AI suggested]            │ │
│  └─────────────────────────────────────────┘ │
│                                                │
│  Step 3: Review & edit                        │
│  ┌─────────────────────────────────────────┐ │
│  │  [Editable report preview]              │ │
│  │  Each section is a text field to tweak  │ │
│  └─────────────────────────────────────────┘ │
│                                                │
│  [📝 Save as Draft] [📤 Export PDF] [📤 Share] │
└──────────────────────────────────────────────┘
```

**B. Auto-Population Sources:**
- Background: from project entity
- Methods: from project entity
- Observations: list from DAO filtered by project/date
- Data: charts auto-generated from data records
- Questions: list of linked questions
- Hypotheses: list with status
- Conclusions: AI-generated summary (if AI provider set up)

**C. Implementation:**
- New `AutoReportBuilder.kt` utility class
- `buildReportFromProject(projectId): ReportEntity`
- Report preview with editable sections
- Export as PDF (see section 12)

---

## 12. Share as PDF with Images

### Current State
- Share sends plain text/markdown via `ACTION_SEND`
- No PDF generation with images
- `FieldMindExport` has `simplePdfBytes` but uses text-only PDF

### Redesigned Approach

**A. PDF Generation with Images:**
```kotlin
fun buildPdfWithImages(
    context: Context,
    title: String,
    sections: List<PdfSection>,
    imageAttachments: List<AttachmentInfo>,
    outputFile: File
): File
```

**B. PDF Structure:**
- Cover page with title, date, author
- Each section has heading + body text
- Images embedded inline (not as attachments)
- Charts rendered as bitmaps and embedded
- Footer with page numbers, app attribution

**C. Share Flow:**
```
[Share Button] → Generate PDF on background thread
                → Save to cache
                → Launch ACTION_SEND with FileProvider URI
                → User picks: Email, Drive, WhatsApp, etc.
```

**D. Technical Approach:**
- Use `android.graphics.pdf.PdfDocument` API (native, no dependencies)
- Render images to PDF page canvas using `canvas.drawBitmap()`
- Render charts by capturing Compose Canvas to bitmap
- Use `FileProvider` for sharing URI

**E. Implementation Files:**
- `FieldMindExport.kt` — Add `buildPdfReportWithImages()`, `captureChartToBitmap()`
- `FieldMindDetailScreen.kt` — Update share to use PDF instead of text
- `FieldMindDialogs.kt` — Update share in all dialog forms

---

## 13. Question Page & Auto Question Builder

### Current State
- Questions tab was removed from navigation (only available in Workspace > Analysis)
- No auto question generation
- Questions are manually entered

### Redesigned Approach

**A. Restore Dedicated Questions Screen:**
- Add `QuestionsScreen` navigation route
- Bottom nav or accessible from Home screen
- Lists all questions with status, priority, category filters

**B. Auto Question Builder:**
```
┌──────────────────────────────────────────────┐
│  🤖 Generate Questions from Observations     │
│                                                │
│  Select observations for context:             │
│  [✓] Crow on wire (Jun 13)                   │
│  [✓] Sparrow bathing (Jun 12)                │
│  [✓] Finch at feeder (Jun 11)                │
│                                                │
│  Question style:                              │
│  [Comparative] [Causal] [Descriptive]         │
│                                                │
│  [Generate Questions]                         │
│                                                │
│  ┌─ Generated Questions ──────────────────┐  │
│  │  ❓ Do crow visit times correlate...   │  │
│  │  ❓ What's the species diversity at... │  │
│  │  ❓ How does weather affect bird...    │  │
│  │  [✓ Save Selected] [Edit] [Regenerate]│  │
│  └──────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

**C. Smart Question Makers:**
- **Pattern Detection:** "You observed crows at 7 AM three times → Is there a morning pattern?"
- **Gap Detection:** "You haven't observed at this site in 2 weeks → Has the species composition changed?"
- **Comparison Detection:** "You observed at Site A and Site B → How do the communities differ?"
- **Weather Correlation:** "Temperatures ranged from 15-30°C → Does temperature affect activity?"

**D. Implementation:**
- Rule-based question generation (no AI required):
```kotlin
fun generateQuestions(observations: List<ObservationEntity>): List<String> {
    // Pattern detection logic
    // Gap detection logic
    // Comparison logic
    // Weather correlation logic
}
```
- Optional AI-powered generation if AI provider set up

---

## 14. Library Redesign

### Current State
- 5 tabs: Sources, Notes, Reading, Flashcards, Learn
- Sources form has 15+ fields all visible
- PDF reader is WebView fallback (Google Docs viewer)
- Notes UI is basic list
- Reading/Learn sections are text-heavy

### Redesigned Approach

**A. Knowledge Hub Layout:**
```
┌──────────────────────────────────────────────┐
│  📚 Knowledge Hub                             │
│  [Sources] [Notes] [Reading] [Flashcards]     │
│                                                │
│  ┌─ Active Reading ──────────────────────┐  │
│  │  🔖 Bird Field Guide (12% complete)   │  │
│  │  📄 Smith et al. 2024 (45% complete)  │  │
│  │  [Continue Reading]                   │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Recent Notes ────────────────────────┐  │
│  │  📝 Observation at wetland...  2h ago │  │
│  │  📝 Literature notes on...    yesterday│  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Quick Actions ──────────────────────┐  │
│  │  [📷 Add Source] [✏ New Note]       │  │
│  │  [📖 Start Reading] [🔄 Review Due] │  │
│  └──────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

**B. Built-in PDF Reader:**
```kotlin
// Dependency: implementation("androidx.pdf:pdf-viewer-fragment:1.0.0-alpha18")
@Composable
fun PdfViewerScreen(
    uri: String,
    title: String,
    onBack: () -> Unit,
    onAnnotate: ((Uri) -> Unit)? = null
) {
    // Use PdfViewer composable from androidx.pdf
    // Features: pinch-to-zoom, scroll, text selection, search
    // Annotations: highlight, underline, draw (if EditablePdfViewerFragment)
}
```

**C. Source Form Redesign (Progressive Disclosure):**
- Step 1: Type, Title, Author (always visible)
- Step 2: [Expand] → DOI, Publisher, Year, Link
- Step 3: [Expand] → Reading Notes, Summary, Key Findings
- Step 4: [Expand] → Status, Importance, Credibility
- Collapsed by default, 85% of users only need Steps 1-2

**D. Notes UI Redesign:**
- Rich text editing with markdown support
- Inline image embedding
- Pin/unpin notes
- Category color coding
- Search bar with full-text search
- Note linking: @reference to observations, sources, questions

**E. Reading/Learn Redesign:**
- Reading mode: large font, dark mode toggle, progress tracking
- Learn mode: interactive cards with topic navigation
- "Continue where you left off" bookmark
- Reading timer: "You spent 12 min reading today"

---

## 15. Map Improvements

### Current State
- `OsmMap` uses osmdroid with default tiles
- Map is embedded in Insights screen and Map screen
- No place names visible on tiles
- Zoom is manual (pinch-to-zoom)
- No auto-fit bounds
- No full-screen toggle

### Redesigned Approach

**A. Auto-Show Map on Home Screen:**
- Map section appears automatically when GPS-tagged observations exist
- Shows mini-map with markers in the Home dashboard
- "Tap to expand" → opens full Map screen

**B. Proper Map Tiles with Place Names:**
```kotlin
// Switch to tile source with explicit place name labels
mapView.setTileSource(TileSourceFactory.MAPNIK) // already set
// For better labels, consider:
// TileSourceFactory.USGS_TOPO
// Or custom tile source from MapTiler/Stadia with styled labels
```

**C. Auto-Fit Bounds:**
```kotlin
// After adding markers:
val minLat = points.minOf { it.first }
val maxLat = points.maxOf { it.first }
val minLon = points.minOf { it.second }
val maxLon = points.maxOf { it.second }
val boundingBox = BoundingBox(maxLat, maxLon, minLat, minLon)
mapView.zoomToBoundingBox(boundingBox, true, 48)
```

**D. Less Zoomed In:**
- Default zoom should show all markers
- Use `zoomToBoundingBox` with padding for better framing
- Don't zoom into single marker on launch

**E. Full-Screen Map View:**
```
┌──────────────────────────────────────────────┐
│  [← Back]  Field Map  [⛶ Full Screen]       │
│                                                │
│  ┌──────────────────────────────────────────┐ │
│  │                                          │ │
│  │          [Interactive OSM Map]           │ │
│  │     ●           ●                        │ │
│  │          ●                 ●              │ │
│  │  ●                             ●         │ │
│  │      ●      ●                            │ │
│  │                                          │ │
│  │  [Place Name: Botanical Garden]          │ │
│  └──────────────────────────────────────────┘ │
│                                                │
│  ┌─ Observation List ─────────────────────┐  │
│  │  ● Crow (Botanical Garden)   Jun 13    │  │
│  │  ● Sparrow (Botanical Garden) Jun 12   │  │
│  └──────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

**F. Full-Screen Toggle:**
- Button to hide header/bottom bar
- Map fills entire screen
- Tap again or press back to exit full-screen
- Use `WindowInsetsController` to hide system bars

**G. Place Name Resolution:**
- Already partially implemented: `FieldLocationProvider.resolvePlaceName()`
- Cache place names in observation entity
- Display place name on map markers and in detail views

**H. Implementation Files:**
- `FieldMindMapScreen.kt` — Major rewrite
- `FieldMindCharts.kt` — Update `OsmMap` with auto-fit bounds
- `FieldMindHomeScreen.kt` — Add auto-show map section

---

## 16. CameraX Pro Mode & UI Fixes

### Current State
- `FieldMindCameraV2` has zoom slider at bottom center
- Zoom slider is positioned between the capture button and controls
- No exposure compensation
- No manual ISO/focus controls
- Frame indicator (crop guide) exists but doesn't align properly with preview

### Redesigned Approach

**A. Zoom Slider Repositioning:**
```
┌──────────────────────────────────────────────┐
│  [⚡ Flash] [🔲 Grid] [⏱ Timer] [4:3]  [✕] │
│                                                │
│                                                │
│              Camera Preview                    │
│                                                │
│                                                │
│  ┌───── Zoom Slider ─────────────────────┐    │
│  │  ──●───────────────────────────       │    │
│  │  1x                          5x       │    │
│  └─────────────────────────────────────┘    │
│                                                │
│       [◀ Flip]    [● Capture]    [Flash ▶]   │
└──────────────────────────────────────────────┘
```

**B. Pro Camera Settings Panel:**
- Swipe up or tap "Pro" button to reveal:
```
┌──────────────────────────────────────────┐
│  Pro Controls                             │
│  EV: [-2] [-1] [0] [+1] [+2]  ▲         │
│  ISO: [Auto] [100] [200] [400] [800]     │
│  Focus: [AF] [MF ───●────]              │
│  WB: [Auto] [☀️] [☁️] [💡] [🏠]       │
└──────────────────────────────────────────┘
```

**C. Implementation:**
- Exposure Compensation: `Camera2Interop.Extender` with `CONTROL_AE_EXPOSURE_COMPENSATION`
- ISO: `Camera2Interop.Extender` with `SENSOR_SENSITIVITY` (when AE off)
- Manual Focus: `Camera2Interop.Extender` with `LENS_FOCUS_DISTANCE`
- White Balance: `Camera2Interop.Extender` with `CONTROL_AWB_MODE`
- Move zoom slider to top of bottom controls area, above capture button
- Frame indicator: fix alignment using proper coordinate mapping from camera sensor to viewfinder

**D. Frame Indicator Fix:**
- Current: `CropGuideOverlay` draws lines at fixed positions
- Problem: doesn't account for preview scaling (ScaleType.FILL_CENTER)
- Fix: calculate actual preview rect and map overlay coordinates accordingly

---

## 17. Home Screen Redesign

### Current State
- Header + Daily Goal Card + Widget Grid + Learning + Quick Actions
- No Research Session CTA on home
- No map section
- Weather widget missing (settings exist, API not integrated)

### Redesigned Elements

**A. Research Session CTA Card:**
- Prominent card between daily goal and widget grid
- Shows: "Start a Research Session" with timer icon
- Shows last session stats: "Last: 12 obs in 45 min"
- Tap to start new session

**B. Map Section (Auto-Show):**
- If user has GPS-tagged observations, show mini-map
- Shows 3 most recent observation locations
- "View full map" button

**C. Weather Widget:**
- If weather API integrated: show current conditions based on last known location
- "78°F • Partly Cloudy • Observed at 7:32 AM"

**D. Today's Stats:**
- Move from daily goal card to more visible position
- "Today: 3 obs • Streak: 12 days • 1 question answered"

---

## 18. Research UI Redesign (Detail Pages)

### Current State
- Single `DetailScreen` handles all entity types
- Text-heavy layout with basic fields
- No entity-specific visual design
- Share sends text only

### Redesigned Approach

**A. Observation Detail:**
```
┌──────────────────────────────────────────────┐
│  [←]  [Badge: Sure] [Chip: Bird] [Date]      │
│                                                │
│  ┌─ Subject ──────────────────────────────┐  │
│  │  Crow on wire                           │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Facts ────────────────────────────────┐  │
│  │  Large crow, perched on telephone wire,│  │
│  │  calling repeatedly, 7:32 AM           │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Evidence Gallery ────────────────────┐  │
│  │  [📷 Photo 1] [📷 Photo 2] [🎙 Audio]│  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Location ────────────────────────────┐  │
│  │  [Mini Map with marker]               │  │
│  │  Botanical Garden • 37.42, -122.15    │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  ┌─ Linked Records ─────────────────────┐  │
│  │  📊 3 data records                    │  │
│  │  ❓ 2 questions raised                │  │
│  │  🔗 Campus Bird Survey project        │  │
│  └──────────────────────────────────────┘  │
│                                                │
│  [✏️ Edit] [📤 Share as PDF] [🗑 Delete]        │
└──────────────────────────────────────────────┘
```

**B. Entity-Specific Detail Layouts:**
| Entity | Key Sections |
|--------|-------------|
| Observation | Subject, Facts, Evidence Gallery, Location Map, Linked Records, Timing |
| Note | Rich Markdown Render, Attachments, Backlinks, Version History |
| Question | Question Text, Answer, Status, Linked Hypotheses, AI Suggestions |
| Hypothesis | Prediction, Reasoning, Support/Weaken Criteria, Test Method, Result |
| Project | Overview, Objective, Question, Evidence Summary, Data Charts, Reports |
| Source | Citation, Preview (PDF/Image/Link), Reading Notes, Flashcards |
| Data Record | Label, Value, Unit, Dynamic Fields, Charts, Related Observations |
| Report | Formatted Sections, Auto-Built Content, Export Options |

**C. Share as PDF:**
- ALL entity details now share as PDF with images
- Uses `FieldMindExport.buildPdfWithImages()`
- Includes: entity header, all field values, attachment thumbnails, map screenshot

---

## 19. Priority Roadmap

### Phase 1: Foundation (This Sprint)
| Priority | Task | Files | Est. Effort |
|----------|------|-------|-------------|
| 🔴 P0 | Quick Capture evidence-first redesign | `ObserveScreen.kt`, `CameraV2.kt` | 3 days |
| 🔴 P0 | Live observation timer | `ObserveScreen.kt`, `ViewModel` | 1 day |
| 🔴 P0 | Number-only input fields | `FieldMindComponents.kt` | 0.5 day |
| 🔴 P0 | Camera zoom slider repositioning | `CameraV2.kt` | 0.5 day |

### Phase 2: Data & Analysis (Next Sprint)
| Priority | Task | Files | Est. Effort |
|----------|------|-------|-------------|
| 🔴 P0 | Category-specific data records | `FieldEntities.kt`, `Dao`, `Dialogs` | 2 days |
| 🔴 P0 | Interactive data workshop | `FieldMindDialogs.kt`, `FieldDataTable.kt` | 3 days |
| 🟡 P1 | Auto-build reports from project data | `FieldMindExport.kt`, `Reports` | 2 days |
| 🟡 P1 | PDF sharing with images | `FieldMindExport.kt`, `DetailScreen` | 1.5 days |

### Phase 3: Library & Knowledge (Following Sprint)
| Priority | Task | Files | Est. Effort |
|----------|------|-------|-------------|
| 🟡 P1 | Built-in PDF reader | `LibraryScreen.kt`, build.gradle | 2 days |
| 🟡 P1 | Notes UI redesign with rich text | `LibraryScreen.kt`, `Dialogs` | 2 days |
| 🟡 P1 | Source form progressive disclosure | `Dialogs.kt` | 1 day |
| 🟢 P2 | Reading/Learn UI enhancements | `LibraryScreen.kt` | 1.5 days |

### Phase 4: Questions & Maps (Following Sprint)
| Priority | Task | Files | Est. Effort |
|----------|------|-------|-------------|
| 🟡 P1 | Restore Questions screen with auto-builder | Navigation, new `QuestionsScreen.kt` | 2 days |
| 🟡 P1 | Map auto-show, auto-fit bounds, place names | `MapScreen.kt`, `Charts.kt` | 2 days |
| 🟢 P2 | Full-screen map toggle | `MapScreen.kt` | 0.5 day |

### Phase 5: Professional Camera (Future Sprint)
| Priority | Task | Files | Est. Effort |
|----------|------|-------|-------------|
| 🟢 P2 | Exposure compensation | `CameraV2.kt` | 1 day |
| 🟢 P2 | Manual ISO | `CameraV2.kt` | 1 day |
| 🔵 P3 | Manual focus | `CameraV2.kt` | 1 day |
| 🔵 P3 | White balance presets | `CameraV2.kt` | 0.5 day |
| 🔵 P3 | Frame indicator alignment fix | `CameraV2.kt` | 0.5 day |

### Phase 6: Home & Polish (Future Sprint)
| Priority | Task | Files | Est. Effort |
|----------|------|-------|-------------|
| 🟡 P1 | Add Research Session CTA to Home | `HomeScreen.kt` | 0.5 day |
| 🟡 P1 | Create Project UI with templates | `Dialogs.kt`, `ProjectsScreen.kt` | 1 day |
| 🟢 P2 | Weather widget on Home | `HomeScreen.kt`, `WeatherAPI` | 1.5 days |
| 🟢 P2 | Detail page entity-specific layouts | `DetailScreen.kt` | 3 days |

---

## Appendix: Quick Reference — All Redesigns Summary

| # | Feature | Current State | Target State | Phase |
|---|---------|--------------|-------------|-------|
| 1 | Quick Capture | Multi-step form, evidence second | Evidence-first, snap note with inline edit | P1 |
| 2 | Live Timer | Manual stopwatch in form | Persistent live counter in header | P1 |
| 3 | Categories | Single-select, always visible | Collapsible, multi-select presets | P1 |
| 4 | Number Input | Generic text field | Numeric keyboard, stepper, validation | P1 |
| 5 | Research Session | Basic timer + form | Full workshop with live feed, summary | P2 |
| 6 | Session on Home | Not present | Prominent CTA card with last session stats | P6 |
| 7 | Create Project | 11-field dialog | Minimal (3 fields) + templates | P6 |
| 8 | Analysis Page | No dedicated page | Dark focus mode with AI insights | P2 |
| 9 | Data Workshop | Basic data entry | Interactive charts + category-specific forms | P2 |
| 10 | Category Data | Generic fields | Dynamic fields per category | P2 |
| 11 | Auto Reports | Manual text entry | Auto-populated from project data | P2 |
| 12 | Share as PDF | Plain text only | PDF with embedded images and charts | P2 |
| 13 | Question Builder | Not present | Restored page + rule-based/AI generation | P4 |
| 14 | PDF Reader | WebView fallback | Native androidx.pdf viewer | P3 |
| 15 | Library UI | Text-heavy lists | Knowledge hub with progressive forms | P3 |
| 16 | Map | Basic OSM map | Auto-show, auto-fit, place names, full-screen | P4 |
| 17 | Camera Pro | Zoom only | Exposure, ISO, manual focus, WB | P5 |
| 18 | Zoom Slider | Below capture button | Above capture button | P1 |
| 19 | Home Screen | No session CTA | Session card + map section | P6 |
| 20 | Detail Pages | Generic text | Entity-specific visual layouts | P6 |

---

*This document was generated as a comprehensive redesign prompt for FieldMind v0.9.0+. All referenced code files exist in the `app/src/main/java/chromahub/rhythm/app/features/field/presentation/` directory tree.*
