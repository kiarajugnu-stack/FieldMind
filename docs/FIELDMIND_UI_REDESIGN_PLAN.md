# FieldMind — Full Visual UI Redesign Plan

> **Date:** June 15, 2026
> **Scope:** Complete visual overhaul of every screen except the Today (Home) page
> **Constraint:** Keep existing theme colors, Material 3 design system, fonts, and icon set
> **Goal:** Every feature feels like a standalone professional app with deep interconnections

---

## Table of Contents

1. [Design System & Foundation](#1-design-system--foundation)
2. [Group A: Navigation & Shell Architecture](#2-group-a-navigation--shell-architecture)
3. [Group B: Observation System (Capture + Gallery + Detail)](#3-group-b-observation-system)
4. [Group C: Research Workspace (Projects + Data + Reports + Paper Maker)](#4-group-c-research-workspace)
5. [Group D: Knowledge Hub (Library + Sources + Reading + Flashcards + Learn)](#5-group-d-knowledge-hub)
6. [Group E: Question-Hypothesis Engine](#6-group-e-question-hypothesis-engine)
7. [Group F: Analysis Dashboard (Insights + Maps + Archive)](#7-group-f-analysis-dashboard)
8. [Group G: Research Sessions & Field Mode](#8-group-g-research-sessions--field-mode)
9. [Group H: Tools & Utilities (Settings + Lock + Backup + Onboarding)](#9-group-h-tools--utilities)
10. [Cross-Cutting: Navigation Reorganization & Button Remapping](#10-cross-cutting-navigation-reorganization--button-remapping)
11. [Feature Interconnection Map](#11-feature-interconnection-map)
12. [Implementation Phases](#12-implementation-phases)

---

## 1. Design System & Foundation

### Current State
FieldMind uses Material 3 with a custom `FieldMindTheme` with semantic accent colors per entity type (Observation, Question, Project, Source, Data, Report, Hypothesis, Flashcard). The current UI is functional but has:
- Inconsistent card shapes (mix of 24dp, 28dp, 16dp corner radii)
- No standardized spacing grid (mix of 14dp, 16dp, 18dp, 20dp)
- Overlapping navigation layers (tabs within tabs, multi-level lists)
- No consistent bottom bar or top-level navigation paradigm
- Many screens overflow with `LazyColumn` of cards in cards with no visual hierarchy

### New Design System Rules

**Spacing Scale (strict):**
- XS: 4dp, SM: 8dp, MD: 12dp, LG: 16dp, XL: 20dp, XXL: 24dp, XXXL: 32dp
- Screen content padding: 16dp (reduced from 20dp)
- Card internal padding: 16dp (standardized)
- List spacing: 12dp (reduced from 14dp)

**Card Corner Radii:**
- Hero/large cards: 20dp (reduced from 28dp/32dp)
- Standard cards: 16dp (reduced from 24dp)
- Compact cards: 12dp (reduced from 18dp)
- Chips/buttons: 12dp (reduced from 16dp)
- Input fields: 14dp (reduced from 18dp)

**Elevation & Surface:**
- 0dp elevation everywhere (no shadows) — use `surfaceContainerLow`/`surfaceContainerHigh` for hierarchy
- `surfaceContainerLow` for standard cards
- `surfaceContainerHigh` for compact/inline cards
- `surfaceContainerHighest` for dividers and separators

**Typography Consistency:**
- All headings: `headlineSmall` or `titleLarge` (never mix `headlineMedium` and `titleMedium` in same context)
- All section headers: `titleMedium` + `FontWeight.Bold`
- All metrics: `displaySmall` or `headlineSmall` + `FontWeight.ExtraBold`
- Body: `bodyMedium` or `bodySmall` consistently per context
- Labels: `labelMedium` consistently for metadata

**Icon Set:**
- ALL action icons: 20dp
- ALL entity/tag icons: 18dp
- ALL badge/provenance icons: 16dp
- Standardize icon usage — no mixing of sizes within same context

**Color Usage:**
- Semantic accent per entity (Observation, Question, Project, Source, Data, Report, Hypothesis, Flashcard) — maintained
- `primaryContainer` for hero sections and active states — maintained
- Confirmation: `positive` accent — maintained
- Errors: `MaterialTheme.colorScheme.error` — maintained

**New Component Vocabulary:**
1. **HeroSurface** — full-width gradient/colored card for the top of each screen
2. **MetricBar** — horizontal row of 3-4 metrics with compact styling
3. **ActionRow** — row of equally-spaced icon+label buttons
4. **QuickActionFAB** — persistent floating button for primary screen action
5. **MiniCard** — compact card for grid layouts (used in widget grids)
6. **EntityRow** — single-line entity display with icon + title + subtitle + chevron
7. **TabBar** — top-level tab navigation (used in Workspace, Knowledge Hub)

---

## 2. Group A: Navigation & Shell Architecture

### Current Pain Points
- No bottom navigation bar — users must scroll to top to navigate
- Navigation is done entirely through `FieldMindScreen` enum with `onNavigate` calls
- No persistent back button at the shell level
- Tab bars exist inside Projects (5 tabs) and Library (5 tabs) with different implementations
- No unified "home" navigation — Home is just one screen among many
- `SectionHeader` is used inconsistently with different subtitle styles

### Redesign: Bottom Navigation Bar + Floating Action Row

**New Shell Layout (every screen inherits):**
```
┌─────────────────────────────────┐
│  [StatusBar]                    │
├─────────────────────────────────┤
│                                 │
│  Screen Content                 │
│  (varies by screen)             │
│                                 │
│                                 │
├─────────────────────────────────┤
│ [Search] [QuickNav] [Researcher]│ ← Floating Action Row (above nav)
├─────────────────────────────────┤
│ 🏠  📷  📚  📊  ⚙️            │ ← Bottom Navigation Bar
└─────────────────────────────────┘
```

**Bottom Navigation Items:**
1. **Home** (🏠) — Today dashboard (current Home screen, UNCHANGED)
2. **Capture** (📷) — Opens ObserveScreen directly in capture-first mode
3. **Workspace** (📚) — Projects + Data + Reports (current ProjectScreen + Insights)
4. **Analyze** (📊) — Insights + Maps + Archive
5. **Library** (📚) — Knowledge Hub (Sources + Notes + Reading + Flashcards + Learn)

**Floating Action Row (always visible above bottom nav):**
- Search icon → opens ArchiveScreen with search focused
- QuickNav → speed dial to: Questions, Hypothesis, Research Session, Field Mode
- Researcher → opens AI assistant / GeminiResearchAssistant

### Screen Header Standardization

Every screen now has a **standardized hero header**:

```kotlin
@Composable
fun StandardScreenHeader(
    title: String,
    subtitle: String?,
    icon: MaterialSymbolIcon,
    heroColor: Color = theme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trailing: (@Composable () -> Unit)? = null
)
```

```                    
┌─────────────────────────────────┐
│  [Icon]  Title                  │
│          Subtitle               │
│  ─────────────────────────────  │
│  (screen-specific content)      │
└─────────────────────────────────┘
```

The header is a rounded card at the top of every screen, always the same height, always with the entity's semantic accent color as the icon background.

---

## 3. Group B: Observation System

### Screens: Capture (ObserveScreen), Observation Detail, Evidence Gallery, Species ID

### Current Pain Points
- ObserveScreen is a massive file (~800 lines) mixing field mode, quick capture, and full form
- Camera opens *after* the form, not before
- Evidence attachment is a secondary section, not primary
- Species ID button is buried inside an expandable section
- Detail screen has no swipeable photo gallery (it uses a HorizontalPager but it's tucked away)
- No "edit photo" or "crop after capture" flow
- Audio recordings can't be played back

### Redesign: Evidence-First Capture App

**New Capture Flow (complete remap):**

```
Step 1: Home → Tap Capture button
Step 2: → Camera opens IMMEDIATELY (full-screen, no form)
Step 3: → Take photo → "Photo captured" toast (CENTERED, not top)
Step 4: → Full-screen bottom sheet: "What category?"
         [Bird] [Mammal] [Plant] [Insect] ... [Other]
Step 5: → Navigate to Observation Detail with photo pre-attached
Step 6: → User fills subject + facts + confidence
Step 7: → Background species ID runs automatically (non-blocking chip)
Step 8: → Save
```

**New ObserveScreen Layout (when accessed from bottom nav):**

```
┌─────────────────────────────────┐
│  ┌──────────────────────────┐   │ ← Hero: Big camera preview area
│  │    📷 Tap to capture      │   │    (empty state when no photo)
│  │    or drag media here     │   │
│  └──────────────────────────┘   │
│                                 │
│  [📷 Camera] [🖼 Gallery]    │ ← Evidence Action Row (3 buttons)
│  [📎 File]                     │
│                                 │
│  ┌─ Quick form ────────────┐   │
│  │ Subject: _______________ │   │
│  │ Facts: _________________ │   │
│  │ Category: [Bird] [Mammal]│   │
│  │ Confidence: [Certain]    │   │
│  │ [Save]                    │   │
│  └──────────────────────────┘   │
│                                 │
│  ┌─ Species ID ─────────────┐  │ ← Auto-runs, non-blocking
│  │ 🔬 Analyzing... 45%       │  │
│  └──────────────────────────┘   │
│                                 │
│  Recent captures (list)         │
└─────────────────────────────────┘
```

**Key UI Changes:**
- Camera evidence area takes 40% of screen (was a small button row)
- Form is compact, always visible below the camera area
- Species ID is a live card that shows progress (was a button you had to tap)
- Timer is an overlay pill, not a full card
- Quick capture buttons (categories) are LARGE chips in a 2-column grid

**New Observation Detail Screen:**

```
┌─────────────────────────────────┐
│  ← Back   [Edit] [Share] [Del]  │
├─────────────────────────────────┤
│  ┌── Swipeable Photo Gallery ─┐  │ ← HorizontalPager, 320dp tall
│  │  ←  [  photo  ]  →  (n/N) │  │
│  └────────────────────────────┘  │
│                                 │
│  Bird · Certain                  │ ← EntityBadge + ConfidenceChip
│  Crow on wire                    │ ← Subject (large, bold)
│                                 │
│  2026-06-15 14:30 · GPS · 📍    │ ← Metadata row
│                                 │
│  ┌─ Facts ───────────────────┐  │
│  │  Observed crow carrying    │  │
│  │  a twig in its beak...     │  │
│  └────────────────────────────┘  │
│                                 │
│  ┌─ Evidence ────────────────┐  │
│  │ [Photo_1] [Photo_2] [Mic] │  │
│  └────────────────────────────┘  │
│                                 │
│  ┌─ Weather & Location ──────┐  │
│  │ 22°C · Partly cloudy      │  │
│  │ 51.5074°N, 0.1278°W       │  │
│  └────────────────────────────┘  │
│                                 │
│  ┌─ Provenance ──────────────┐  │ ← Collapsible
│  │ Created: 14:30             │  │
│  │ Timer: 3m 42s              │  │
│  │ Tags: birds, behavior      │  │
│  └────────────────────────────┘  │
│                                 │
│  ┌─ Re-observation Chain ────┐  │
│  │ This follows up: Obs #142  │  │
│  │ Follow-ups: Obs #144, #145 │  │
│  └────────────────────────────┘  │
└─────────────────────────────────┘
```

**Key Changes from Current:**
- Photo gallery is the hero element (was just an attachment row)
- Provenance is collapsed by default (was always expanded)
- Weather + Location in a single compact row (was scattered)
- Edit/Share/Delete moved to the top bar (was a separate action bar card)
- Re-observation chain is visually linked, not standalone cards

**Evidence Gallery (Standalone sub-screen):**

```
┌─────────────────────────────────┐
│  ← Back  Evidence (4)  [Select] │
├─────────────────────────────────┤
│  ┌────────  ┌────────  ┌──────┐ │
│  │ Photo 1│ │ Photo 2│ │Audio│ │ │ ← Masonry grid
│  └────────  └────────  └──────┘ │
│  ┌────────  ┌────────           │
│  │ Photo 3│ │ Photo 4│         │
│  └────────  └────────           │
│                                 │
│  [Add evidence]                 │ ← Bottom button
└─────────────────────────────────┘
```

**Audio Player (New component):**
- Tap audio tile → expands inline with play/pause/seek bar
- Shows waveform visualization (simple bars)
- Recording duration + current position
- Share/Export button

**In-App Photo Editor (New component):**
- After camera capture → overlay with crop/rotate/annotate tools
- Simple: draw arrows, circles, text on photo
- Save as new evidence or replace original

---

## 4. Group C: Research Workspace

### Screens: ProjectsScreen (5 tabs), Reports, Data Tools (Counter, Measurement, Weather Log, Species Tool), Research Paper Maker (New)

### Current Pain Points
- ProjectsScreen is 700+ lines with 5 tabs all crammed into one file
- Data Tools are separate screens (CounterToolScreen, MeasurementToolScreen) but feel disconnected from Projects
- Reports are Markdown-only with no PDF export
- No Research Paper Maker exists yet
- "Data" tab in Workspace just lists data records — no visualization
- Project cards show too many metrics (7+ chips) in one row
- FlowRow usage makes layout inconsistent across screen sizes

### Redesign: Workspace as a True Research Studio

**New Workspace Layout:**

```
┌─────────────────────────────────┐
│  [📊] Workspace                  │
│  Research overview & analysis    │
├─────────────────────────────────┤
│  [Overview] [Data] [Reports]     │ ← Only 3 tabs (simplified)
│  [Papers] [Export]               │
├─────────────────────────────────┤
│                                 │
│  (content varies by tab)        │
│                                 │
└─────────────────────────────────┘
```

**Tab 1: Overview — Project Dashboard (REDESIGNED)**

```
┌─────────────────────────────────┐
│  ┌─ Research Dashboard ──────┐  │ ← Hero surface
│  │ 📈 Total: 142 obs         │  │
│  │    This week: 12          │  │
│  │    Field hours: 8.5       │  │
│  │    Sessions: 14           │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Quick actions ──────────┐   │
│  │ [New project] [New paper] │  │ ← Prominent buttons
│  │ [New data record]         │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Active projects ────────┐   │
│  │ ┌─ Bird Survey ────────┐ │  │ ← Each project is a STANDALONE card
│  │ │ 📷 12 obs · 3 Qs · 1 │ │  │    with a "Open workspace" button
│  │ │ hyp · Last: 2h ago   │ │  │
│  │ │ [Open]               │ │  │
│  │ └──────────────────────┘ │  │
│  │ ┌─ Soil Study ─────────┐ │  │
│  │ │ 📊 8 obs · 2 datasets │ │  │
│  │ │ [Open]               │ │  │
│  │ └──────────────────────┘ │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**Tab 2: Data — Data Studio (REDESIGNED)**

Each data tool is no longer a separate screen — they're inline workspaces within a unified Data Studio:

```
┌─────────────────────────────────┐
│  ┌─ Tool selector (chips) ──┐   │
│  │ [Counter] [Measure]      │   │
│  │ [Weather] [Species] [+-] │   │  ← Inline, not separate screens
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Active tool (Counter) ───┐  │
│  │                           │  │
│  │        47                 │  │  ← Large animated counter
│  │     [−] [+]               │  │
│  │    Label: "Birds seen"   │  │
│  │    [Save]                 │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Recent data (mini chart)─┐ │
│  │ ██▁▂▃▅▇▆▄▃▁▂▃▄▅▆▇█  │ │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Data table ─────────────┐   │
│  │ 10:30    Birds seen  12  │   │
│  │ 10:35    Birds seen  15  │   │
│  │ 10:40    Birds seen   8  │   │
│  └──────────────────────────┘   │
└─────────────────────────────────┘
```

**Key Change:** Data tools are now tabs WITHIN the Data tab, not separate screens. Switch between Counter/Measure/Weather/Species with a chip row, not navigation.

**Tab 3: Reports — Report Studio (REDESIGNED)**

```
┌─────────────────────────────────┐
│  ┌─ New Report ─────────────┐   │
│  │ Title: _____________      │   │
│  │ Type: [Field Report]      │   │
│  │ [Start with template]     │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Recent reports ─────────┐   │
│  │ ┌─ Soil Analysis ──────┐ │   │ ← Each with status badge
│  │ │ Draft · 3 sections    │ │   │
│  │ │ [Edit] [Export PDF]   │ │   │
│  │ └──────────────────────┘ │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Tab 4: Papers — Research Paper Maker (NEW)**

```
┌─────────────────────────────────┐
│  ┌─ Paper structure ─────────┐  │  ← IMRAD template
│  │ ✓ 1. Introduction        │  │
│  │ ✓ 2. Methods             │  │
│  │ 📝 3. Results (active)   │  │  ← Current section highlighted
│  │ ○ 4. Discussion          │  │
│  │ ○ 5. Conclusion          │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Editor ─────────────────┐   │
│  │ Rich text area            │   │
│  │ [B] [I] [U] [🔗] [📷]   │   │  ← Formatting toolbar
│  │ [📊 Insert chart] [📰]  │   │
│  │                           │   │
│  │ Type your results here... │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Actions ────────────────┐   │
│  │ [Auto-abstract 🪄]        │   │  ← Gemini-powered
│  │ [Export PDF] [Export DOCX]│  │
│  │ [Insert citation]         │   │  ← Pulls from Sources
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Tab 5: Export — Export Studio (NEW)**

```
┌─────────────────────────────────┐
│  ┌─ Select data to export ───┐  │
│  │ ✓ Observations (142)      │  │
│  │ ✓ Data records (56)       │  │
│  │ ☐ Reports (3)             │  │
│  │ ☐ Sources (12)            │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Format ─────────────────┐   │
│  │ [CSV] [Excel] [JSON]     │   │  ← New export format options
│  │ [Markdown] [GeoJSON]     │   │
│  └───────────────────────────┘   │
│                                 │
│  [Export]                        │
└─────────────────────────────────┘
```

### Card Redesign for Project EntityCard

**Current:** Shows 7+ metric chips (obs, Qs, src, data, reports, sessions, hrs) crammed with FlowRow.

**New Compact Project Card:**
```
┌─────────────────────────────────┐
│ 📁 Bird Survey     Status:Active│
│ What species occur here?        │ ← Research question
│ ┌────┬────┬────┬────┐          │
│ │ 12 │ 3  │ 1  │ 2h │          │ ← 4 compact metrics
│ │obs │Qs  │hyp │field│          │
│ └────┴────┴────┴────┘          │
│ [Open workspace →]              │
└─────────────────────────────────┘
```

---

## 5. Group D: Knowledge Hub

### Screens: Library (Sources + Notes + Reading + Flashcards + Learn), Reader

### Current Pain Points
- Library has 5 tabs but they're jammed into one screen
- Each tab has a different layout pattern (SourcePanel uses CaptureStep, NotePanel has categories, LearnPanel is a massive LazyColumn)
- PDF/image viewer uses WebView (broken for content:// URIs)
- No in-app PDF rendering
- Reading progress tracking doesn't exist (no "page 45/120")
- No Zotero/Mendeley integration
- Flashcards don't have spaced repetition visible in the UI
- Book suggestions are static, not personalized

### Redesign: Knowledge Hub as a Research Library App

**New Tab Layout:**
```
[Sources] [Notes] [Reading] [Flashcards] [Learn]
```

**Tab 1: Sources — Citation Manager Redesign**

```
┌─────────────────────────────────┐
│  ┌─ Add Source ──────────────┐  │  ← Prominent, always visible
│  │ [Search DOI/ISBN] [Manual] │  │  ← NEW: quick DOI lookup
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Filters ────────────────┐   │  ← NEW: filter bar (always visible)
│  │ Type: [All] [Papers] ... │   │
│  │ Status: [All] [Read] ... │   │
│  │ Project: [All] [Bird]    │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Source cards (list view) ┐  │
│  │ 📄 "Climate Change..."   │  │  ← Cleaner cards with less text
│  │    Smith, 2023 · Read     │  │
│  │    ⭐ Important           │  │
│  └───────────────────────────┘  │
│  ┌─ Source cards (grid view) ┐  │  ← NEW: toggleable list/grid
│  │ (alternative layout)       │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**Source Detail Redesign (was a massive LazyColumn with 20+ fields):**

```
┌─────────────────────────────────┐
│ ← Back        [Edit] [Cite]    │
├─────────────────────────────────┤
│  📄 "Climate Change and..."     │ ← Title (large)
│  Jane Smith · 2023 · Nature    │ ← Metadata row
│  ⭐ Important · 📖 Read        │ ← Status badges
│                                 │
│  ┌─ Reading notes ──────────┐   │
│  │ Main idea: ...            │   │  ← Cornell-style cards
│  │ Key findings: ...         │   │
│  │ What it taught me: ...    │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Paper prompts ──────────┐   │
│  │ ❓ What problem?          │   │  ← NEW: active reading prompts
│  │ ❓ Key results?           │   │       always visible
│  │ ❓ Unclear points?        │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Actions ────────────────┐   │
│  │ [Create flashcards]       │   │
│  │ [Copy citation]           │   │
│  │ [Open in browser]         │   │
│  │ [Zotero export]           │   │  ← NEW
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Linked records ─────────┐   │
│  │ 🗂 Project: Bird Survey  │   │
│  │ 🃏 3 flashcards          │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Tab 2: Notes — Notes App Redesign**

```
┌─────────────────────────────────┐
│  [Blan] [Obs] [Lit] [Meet] [Fld]│  ← Template chips (inline)
│                                 │
│  ┌─ Compose note ────────────┐  │  ← Always visible at top
│  │ Title: _______________     │  │
│  │ ┌──────────────────────┐  │  │
│  │ │ Start writing...     │  │  │  ← Large body area
│  │ └──────────────────────┘  │  │
│  │ Category: [Obs] [...]    │  │
│  │ [Save]                     │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Recent notes ───────────┐   │  ← Grid of mini cards
│  │ ┌──┐ ┌──┐ ┌──┐          │   │
│  │ │💡│ │📝│ │📊│          │   │
│  │ └──┘ └──┘ └──┘          │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Tab 3: Reading — Reading Mode Redesign**

```
┌─────────────────────────────────┐
│  ┌─ Reading Progress ────────┐  │
│  │ 📚 12/45 sources read     │  │  ← Progress ring
│  │ ████████░░░░░░░░░░ 26%   │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Next up: ───────────────┐   │  ← "Continue reading" card
│  │ "Soil Analysis Methods"   │   │
│  │ Paused at page 23/45      │   │  ← NEW: reading progress
│  │ [Continue →]              │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Active reading prompts ──┐  │  ← Always visible reminder
│  │ For each paper, answer:   │  │
│  │ 1. Main topic/thesis?     │  │
│  │ 2. Problem addressed?     │  │
│  │ 3. Method used?           │  │
│  │ 4. Key results?           │  │
│  │ 5. Unclear/missing?       │  │
│  │ 6. New questions?         │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

**PDF/Image Viewer Replacement (NEW):**

- Remove WebView for PDFs and images
- Use Android `PdfRenderer` API for PDFs (zoomed scrollable view)
- Use `AsyncImage` + pinch-to-zoom composable for images
- Keep WebView ONLY for HTML web pages (articles, papers)

**Tab 4: Flashcards — Study App Redesign**

```
┌─────────────────────────────────┐
│  ┌─ Stats bar ──────────────┐   │
│  │ Total: 24 cards          │   │
│  │ Due today: 5   📊        │   │  ← NEW: SM-2 due count
│  └───────────────────────────┘   │
│                                 │
│  [Review due cards →]           │  ← Prominent CTA
│                                 │
│  ┌─ Card grid ──────────────┐   │  ← Grid layout (2 columns)
│  │ ┌──┐ ┌──┐                │   │
│  │ │💡│ │💡│                │   │  ← Each card shows front only
│  │ │Term│ │Concept│          │   │       tap to flip inline
│  │ └──┘ └──┘                │   │
│  └───────────────────────────┘   │
│                                 │
│  [Create flashcard]             │
└─────────────────────────────────┘
```

**Tab 5: Learn — Learning App Redesign**

```
┌─────────────────────────────────┐
│  ┌─ Recommended next step ───┐  │  ← Personalize
│  │ 🎯 "Observe carefully"     │  │
│  │ Science starts with...     │  │
│  │ [Open resource →]          │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Based on your activity ─┐   │  ← Personalized
│  │ 📖 "Bird Behavior Guide"  │   │
│  │ 📺 "Intro to Field Notes" │   │  ← Cards with thumbnails
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Learn categories ───────┐   │  ← Collapsible sections
│  │ [▼ Field Research Basics] │   │
│  │ [▼ Biology & Ecology]     │   │  ← Smaller, cleaner
│  │ [▶ Geology & Soil]        │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

---

## 6. Group E: Question-Hypothesis Engine

### Screens: QuestionsScreen, Hypothesis tabs within Workspace

### Current Pain Points
- Questions and Hypotheses are in separate screens with separate navigation
- No visible link between a question and its hypotheses
- Auto-builder is buried inside a collapsible section in QuestionsScreen
- Hypothesis testing workflow doesn't exist (no "Test → Collecting → Analysis → Conclusion")
- No evidence linking UI for hypotheses
- No prediction accuracy tracking

### Redesign: Question-Hypothesis Engine as a Unified App

**New Unified Questions & Hypotheses Screen:**

```
┌─────────────────────────────────┐
│  [❓] Questions & Hypotheses     │
│  Track, test, and refine ideas   │
├─────────────────────────────────┤
│  ┌─ Stats bar ──────────────┐   │
│  │ 12 open · 8 answered     │   │
│  │ 5 supp · 2 refuted       │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Auto-builder (always) ───┐  │  ← Always visible, not collapsible
│  │ "What are you curious      │  │
│  │  about?" [Generate from    │  │
│  │  observations →]           │  │  ← NEW: one-tap generation
│  └───────────────────────────┘  │
│                                 │
│  ┌─ Question cards ─────────┐   │  ← New card design
│  │ ❓ Do birds visit more    │   │     Shows hypothesis count
│  │    after rain?            │   │     Shows evidence count
│  │    🧪 1 hypothesis        │   │
│  │    📷 3 observations      │   │  ← Linked evidence count
│  │    [Test →]               │   │  ← NEW: Go to test mode
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Expanded: Hypothesis view ┐  │  ← Inline expansion
│  │ ┌─ Hypothesis card ──────┐ │  │
│  │ │ If: Bird visits ↑      │ │  │
│  │ │ after rain because...  │ │  │
│  │ │ 📊 Confidence: 65%     │ │  │  ← Progress bar
│  │ │ Status: Testing        │ │  │
│  │ │ [Add observation]      │ │  │  ← Link evidence
│  │ │ [Mark as tested]       │ │  │
│  │ └────────────────────────┘ │  │
│  └────────────────────────────┘  │
└─────────────────────────────────┘
```

**New Hypothesis Testing Workflow:**

```
Step 1: Question → Tap [Test →]
Step 2: → Enter Hypothesis form (prediction, reasoning, evidence rules)
Step 3: → Hypothesis card appears with status "Untested"
Step 4: → Tap [Add observation] → Opens quick-capture with hypothesis tag
Step 5: → Each linked observation shows supporting/weakening evidence count
Step 6: → Tap [Mark as tested] → Enter conclusion (Supported/Refuted/Inconclusive)
Step 7: → Confidence bar updates based on evidence strength
```

**Key UI Changes:**
- Questions and Hypotheses are in ONE unified screen, not two
- Auto-builder is always visible (not collapsible)
- Each question card shows linked evidence count
- Each hypothesis shows a confidence progress bar with supporting/refuting counts
- [Test →] button directly starts the hypothesis workflow
- Hypothesis cards show visual indicator for supporting (green) vs refuting (red) observations

---

## 7. Group F: Analysis Dashboard

### Screens: InsightsScreen, MapScreen, ArchiveScreen (Search)

### Current Pain Points
- InsightsScreen is 500+ lines with 9+ sections — too much information on one page
- Map tab has 5 subtabs (Map, Offline Tiles, Drawings, Tracks, Geofences) — feels like a separate app
- Archive/Search is basic string matching with no FTS
- Insights shows a "Knowledge Graph" that's just text nodes — not interactive
- No data export from Insights
- Weather correlation, Category radar, Achievements are all mixed together with no hierarchy

### Redesign: Analysis Dashboard as a Data Science Hub

**New Tab Layout:**
```
[Overview] [Map] [Search]
```

**Tab 1: Overview — Structured Analytics (REDESIGNED)**

```
┌─────────────────────────────────┐
│  ┌─ Profile ────────────────┐   │  ← Compact hero card
│  │ 👤 Researcher            │   │
│  │ Today: 3 · This week: 12 │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Metrics row ────────────┐   │
│  │ 📷 142   ❓ 12/20   🗂 3 │   │
│  │   obs     Qs      projects│   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Activity calendar ──────┐   │  ← Interactive heatmap
│  │ ░█░░█░░███░░░█░░█░░███░  │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Category breakdown ─────┐   │  ← Bar chart
│  │ Bird ████████ 45          │   │
│  │ Plant ██████ 32           │   │
│  │ Insect ████ 20            │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Data quality ───────────┐   │  ← Health meter
│  │ Research Health: 72%     │   │
│  │ ⚠ 3 issues: add GPS...  │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Achievements ───────────┐   │
│  │ 🏆 4/15 unlocked         │   │  ← Compact grid, not inline
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Key Changes:**
- Removed: Knowledge Graph (too complex, no interactivity)
- Removed: Weather Correlation (requires too many data points to be useful)
- Removed: Radar chart (category ranking bar is cleaner)
- Simplified: Achievements shown as compact grid with progress, not expandable card
- Maintained: Activity heatmap, metrics, category breakdown, data quality
- New: Category breakdown is a horizontal bar chart (visual, not text)

**Tab 2: Map — Field Map Redesign**

```
┌─────────────────────────────────┐
│  ← Back  Field Map  [Fullscreen]│
├─────────────────────────────────┤
│  ┌──────────────────────────┐   │
│  │                          │   │
│  │     MAPLIBRE MAP         │   │  ← Full height (no tab within tab)
│  │     (no subtabs)         │   │
│  │                          │   │
│  └──────────────────────────┘   │
│                                 │
│  ┌─ Overlay panel ──────────┐   │  ← Bottom sheet (drag up)
│  │ [📍 Points (12)]          │   │
│  │ [📏 Tracks (3)]          │   │  ← Inline, no tabs
│  │ [📐 Drawings (5)]        │   │
│  │ [🔔 Geofences (2)]       │   │
│  │ [📥 Offline tiles]       │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Drawing toolbar ────────┐   │
│  │ [📍] [📏] [📐] [✏️]     │   │  ← Floating bottom toolbar
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Key Change:** The 5 subtabs (Map, OfflineTiles, Drawings, Tracks, Geofences) are replaced by a single map view with a bottom sheet overlay panel. No more tab-within-tab navigation.

**Tab 3: Search — Search Redesign (NEW)**

```
┌─────────────────────────────────┐
│  ┌─ Search bar ─────────────┐   │
│  │ 🔍 Search observations,   │   │
│  │    notes, sources...      │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Filters ────────────────┐   │
│  │ Type: [All] [Obs] [Notes]│   │
│  │ Date: [Today] [Week] [All]│  │  ← NEW: date filter
│  │ Project: [All projects]  │   │
│  └───────────────────────────┘   │
│                                 │
│  Results:                        │
│  ┌─ EntityRow ──────────────┐   │  ← Cleaner result cards
│  │ 📷 Crow on wire · Bird    │   │
│  │    2h ago                 │   │
│  └───────────────────────────┘   │
│  ┌─ EntityRow ──────────────┐   │
│  │ 📝 Field notes · Journal  │   │
│  │    Yesterday              │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Key Change:** Search is now its own tab in Analysis, with type/date/project filters and cleaner result cards (EntityRow).

---

## 8. Group G: Research Sessions & Field Mode

### Screens: ResearchSessionScreen, FieldMode (within ObserveScreen)

### Current Pain Points
- ResearchSessionScreen has a complex multi-state system (session setup → active → summary)
- Timer card is large and takes up too much space
- Evidence tools are in a separate card below the timer
- Field mode is embedded inside ObserveScreen as a private function (FieldModeScreen)
- Quick snap flow in Field Mode launches camera after category selection (should be camera first)
- No session sharing or export

### Redesign: Research Session as a Focused Timer App

```
┌─────────────────────────────────┐
│  ← Back Research Session   [⚙] │
├─────────────────────────────────┤
│  ┌─ Session timer (hero) ───┐   │
│  │ ● 00:12:34   3 obs       │   │  ← Timer centered, prominent
│  │ "Bird Survey · Field #2" │   │
│  │ [⏸ Pause] [⏹ End]       │   │  ← Pause/End as large buttons
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Evidence tools (compact) ┐  │  ← Single row, not separate card
│  │ [📷] [🖼] [📎] [🎤] [📍] │  │  ← Icon-only buttons
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Quick capture form ─────┐   │
│  │ Category: [Bird] [Mammal] │   │  ← Compact chips
│  │ Subject: _______________  │   │
│  │ Facts: _________________  │   │
│  │ [Save]                     │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Session log ────────────┐   │
│  │ 12:34  Crow on wire      │   │  ← Scrollable list
│  │ 12:36  Sparrow in bush   │   │     10 items shown
│  │ 12:38  Blue jay call     │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Key UI Changes:**
- Timer is centered and takes up top third of screen (was a row with timer + controls)
- Evidence tools are ICON-ONLY in a compact row (was labeled buttons in a separate card)
- Quick capture form is more compact (less padding)
- Session log shows captured observations in real-time (was not shown)
- Start session → quick setup dialog (name + project), not inline form

**Field Mode Redesign (extracted from ObserveScreen):**

```
┌─────────────────────────────────┐
│  ← Back Field Mode  ⏱ 00:15:24 │  ← Timer in header
├─────────────────────────────────┤
│                                 │
│  ┌── Quick capture (hero) ──┐   │
│  │     [📷 Tap to capture]   │   │  ← Big camera button
│  │    ─ or select category ─ │   │
│  │  [Bird] [Mammal] [Plant]  │   │  ← Category chips below
│  │  [Insect] [Other]         │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Auto-save preferences ───┐  │
│  │ ✓ Auto-locate GPS         │   │
│  │ ✓ Auto-fetch weather      │   │
│  │ ✓ 1m spacing              │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Session stats ───────────┐  │
│  │ 📷 8 captures · 📍 all    │   │
│  │ 🌤 6 with weather         │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

**Key UI Changes:**
- Camera capture is the HERO action (was a button among many)
- Category selection is below the camera (was in a dialog)
- Auto-save preferences are shown clearly (were hidden in settings)
- Session stats show what was captured (was not shown)

---

## 9. Group H: Tools & Utilities

### Screens: Settings, Lock Screen, Backup/Export, Onboarding

### Current Pain Points
- Settings are inline in FieldMindDialogs.kt or spread across the app
- Lock screen is functional but visually plain
- Backup/Export screen exists but is inconsistent with rest of UI
- Onboarding is a basic screen with no visual design
- No "welcome" experience for new users

### Redesign: Settings App

```
┌─────────────────────────────────┐
│  ← Back  Settings               │
├─────────────────────────────────┤
│  ┌─ Profile ────────────────┐   │
│  │ 👤 Researcher             │   │  ← Editable profile card
│  │    Field Biologist        │   │
│  │    Bird ecology           │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Research preferences ────┐   │  ← Grouped by category
│  │ Default confidence: Sure   │   │
│  │ Daily goal: 5             │   │
│  │ GPS mode: High accuracy   │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Display ────────────────┐   │
│  │ Theme: System             │   │
│  │ Temperature: °C           │   │
│  │ Distance: Metric          │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ Data & Privacy ─────────┐   │
│  │ 🔒 App lock: Enabled      │   │
│  │ 💾 Auto-backup: Daily     │   │
│  │ 🗑 Clear all data         │   │
│  └───────────────────────────┘   │
│                                 │
│  ┌─ About ──────────────────┐   │
│  │ Version 2.0.0             │   │
│  │ Open source licenses      │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

### Lock Screen Redesign

- Center the lock icon and message more
- Add animated background (subtle particle effect or gradient shift)
- Add app version/icon at the very bottom
- Use larger PIN dots for in-app PIN mode
- Add haptic feedback on PIN entry

### Backup/Export Redesign

```
┌─────────────────────────────────┐
│  ┌─ Backup ────────────────┐    │
│  │ Last backup: Today 14:30│    │
│  │ [Backup now]             │    │  ← Primary action
│  │ [Auto-backup: Daily]     │    │
│  └──────────────────────────┘    │
│                                 │
│  ┌─ Restore ───────────────┐    │
│  │ [Restore from backup]    │    │
│  │ ⚠ Overwrites current     │    │
│  └──────────────────────────┘    │
└─────────────────────────────────┘
```

---

## 10. Cross-Cutting: Navigation Reorganization & Button Remapping

### Current Navigation Map

```
Home → Observe (via "Capture" button)
Home → Library (via "Note" button)
Home → Research Session (via CTA card)
Home → Questions (via widget grid)
Home → Insights (via widget grid)
Home → Weather (via live dashboard tap)
Home → Settings (via gear icon)

Bottom bars: None

Tab bars: Projects (5 tabs), Library (5 tabs)
```

### New Navigation Map

```
Bottom Navigation:
  🏠 Home → Today dashboard (UNCHANGED)
  📷 Capture → ObserveScreen (camera-first)
  📚 Workspace → Projects + Data + Reports + Papers + Export
  📊 Analyze → Insights + Map + Search
  📖 Library → Sources + Notes + Reading + Flashcards + Learn

Floating Action Row (above bottom nav):
  🔍 Search → ArchiveScreen (search focused)
  📋 QuickNav → Speed dial: Questions, Hypotheses, Sessions, Field Mode
  🤖 AI → GeminiResearchAssistant

Cross-links (interconnections):
  Observation Detail → [Link to project] → Project Detail
  Observation Detail → [Link to hypothesis] → Hypothesis card
  Question Detail → [Test] → Hypothesis creation workflow
  Source Detail → [Create flashcards] → Flashcard creation
  Report → [Insert citation] → Source picker
  Paper Editor → [Insert figure] → Evidence gallery picker
  Data record → [View chart] → Insights chart
  Hypothesis → [Add observation] → Quick capture with hypothesis tag
```

### Button Remapping Summary

| Screen | Current Button | New Button |
|--------|---------------|------------|
| Home | "Capture" → Navigates to Observe | "Capture" → OPENS CAMERA DIRECTLY |
| Home | "Note" → Navigates to Library | "Note" → Opens quick-note dialog over anything |
| Observe | "Start observation session" → Toggle | REMOVED (always in capture mode) |
| Observe | Evidence buttons in a row | Evidence = HERO AREA (40% of screen) |
| Projects | 5 tabs → "Overview, Obs, Hyp, Data, Reports" | 3 tabs → "Overview, Data, Reports" + Papers + Export |
| Library | 5 tabs → "Sources, Notes, Reading, Flashcards, Learn" | KEPT but redesigned internally |
| Insights | 9+ sections in LazyColumn | 3 tabs → "Overview, Map, Search" |
| Map | 5 tabs → "Map, Offline, Drawings, Tracks, Geofences" | Single map + bottom sheet overlay |
| Questions | Auto-builder collapsed | Auto-builder ALWAYS VISIBLE |
| Research Session | Timer as card row | Timer as CENTERED HERO |
| Field Mode | Camera button among others | Camera = LARGE HERO BUTTON |

---

## 11. Feature Interconnection Map

### How features should connect visually

```
                        ┌─────────────┐
                        │    HOME     │ ← UNCHANGED (Today page)
                        └──────┬──────┘
                               │
            ┌──────────────────┼──────────────────┐
            │                  │                  │
            ▼                  ▼                  ▼
     ┌──────────┐     ┌──────────────┐    ┌───────────┐
     │ CAPTURE  │     │  WORKSPACE   │    │  LIBRARY  │
     │ (Camera) │     │  (Projects)  │    │ (Sources) │
     └────┬─────┘     └──────┬───────┘    └─────┬─────┘
          │                  │                   │
          ▼                  ▼                   ▼
   ┌───────────┐    ┌──────────────┐    ┌──────────────┐
   │Obs Detail │◄──►│ Data Studio  │    │Flashcards    │
   └─────┬─────┘    └──────┬───────┘    └──────┬───────┘
         │                 │                    │
         ▼                 ▼                    ▼
   ┌───────────┐    ┌──────────────┐    ┌──────────────┐
   │Questions  │◄──►│  Reports     │◄──►│    Learn     │
   │& Hypoth.  │    │  + Papers    │    │              │
   └───────────┘    └──────────────┘    └──────────────┘
```

### Key Interconnection Actions (New)

| From | To | Action | UI Element |
|------|----|--------|------------|
| Observation | Project | [Link to project] | Chip in detail screen |
| Observation | Hypothesis | [Supports hypothesis] | Quick chip in detail |
| Observation | Question | [Generates question] | One-tap button |
| Observation | Data | [Add measurement] | "Measure this" button |
| Question | Hypothesis | [Create hypothesis] | [Test] button |
| Question | Observation | [View evidence] | Evidence count chip |
| Source | Flashcard | [Create flashcards] | "Smart cards" button |
| Source | Report | [Insert citation] | Citation picker |
| Project | Report | [Generate report from data] | Auto-generate button |
| Project | Session | [Start session for project] | "Field session" button |
| Data | Chart | [View as chart] | Chart toggle on data record |
| Report | Paper | [Convert to paper] | "Turn into paper" button |

---

## 12. Implementation Phases

### Phase 1 — Foundation (Week 1-2)
1. Create new component vocabulary (HeroSurface, MetricBar, ActionRow, EntityRow)
2. Implement bottom navigation bar + floating action row
3. Standardize spacing scale across all screens
4. Standardize card corner radii
5. Create StandardScreenHeader and replace all existing headers
6. Build in-app PDF renderer (replace WebView)

### Phase 2 — Capture System Redesign (Week 2-3)
1. Remap Home "Capture" button → opens camera directly
2. Build camera-first ObserveScreen layout
3. Add center-positioned toast for "Photo captured"
4. Add full-screen category picker bottom sheet after capture
5. Add auto-run species ID (non-blocking chip)
6. Build in-app photo editor (crop/rotate/annotate)
7. Add audio player to evidence gallery
8. Build Evidence Gallery as standalone sub-screen

### Phase 3 — Workspace Redesign (Week 3-4)
1. Simplify ProjectsScreen to 3 tabs (Overview, Data, Reports)
2. Add "Papers" and "Export" as new tabs
3. Redesign project cards (4 metrics, not 7+)
4. Build Data Studio — unify all data tools inline
5. Build Research Paper Maker with IMRAD template
6. Build Export Studio with CSV/Excel/JSON export
7. Add citation manager for Reports

### Phase 4 — Knowledge Hub Redesign (Week 4-5)
1. Redesign Source panel (add DOI search, grid/list toggle)
2. Redesign Source Detail (Cornell cards + reading prompts)
3. Redesign Notes panel (template-first composition)
4. Build in-app PDF reader (PdfRenderer API)
5. Build in-app image viewer (pinch-to-zoom)
6. Redesign Flashcards (SM-2 due count, grid layout)
7. Redesign Learn panel (personalized recommendations)

### Phase 5 — Question-Hypothesis Engine (Week 5-6)
1. Merge Questions and Hypotheses into unified screen
2. Make auto-builder always visible
3. Add hypothesis testing workflow (Test → Collecting → Analysis → Conclusion)
4. Add evidence linking UI for hypotheses
5. Show linked evidence counts on question cards
6. Add confidence progress bar with supporting/refuting counts

### Phase 6 — Analysis Dashboard Redesign (Week 6-7)
1. Restructure Insights into 3 tabs (Overview, Map, Search)
2. Simplify Insights content (remove KG, weather correlation, radar)
3. Redesign Map: single map view with bottom sheet overlay
4. Redesign Search: add type/date/project filters
5. Add data export from Analysis dashboard

### Phase 7 — Sessions & Field Mode (Week 7-8)
1. Redesign timer as centered hero
2. Make evidence tools icon-only compact row
3. Add real-time session log
4. Extract FieldMode as standalone screen
5. Make camera the hero action in field mode

### Phase 8 — Polish (Week 8-9)
1. Settings redesign
2. Lock screen animations
3. Backup/Export screen
4. Navigation consistency check
5. Edge case handling (empty states, errors, loading)
6. Performance optimization (lazy loading, caching)

---

## Appendix: File Changes Summary

| File | Action | Notes |
|------|--------|-------|
| `FieldMindNavigation.kt` | REWRITE | Add bottom nav + floating action row |
| `FieldMindComponents.kt` | REWRITE | New component vocabulary |
| `FieldMindScreenUtils.kt` | REWRITE | Standardized headers |
| `FieldMindHomeScreen.kt` | UNCHANGED | Today page not touched |
| `FieldMindObserveScreen.kt` | REWRITE | Camera-first layout |
| `FieldMindDetailScreen.kt` | REWRITE | New detail layouts |
| `FieldMindQuestionsScreen.kt` | REWRITE | Unified Q+H screen |
| `FieldMindProjectsScreen.kt` | REWRITE | 3 tabs + papers + export |
| `FieldMindDataTools.kt` | MERGE | Unified Data Studio |
| `FieldMindResearchSession.kt` | REWRITE | Hero timer + compact tools |
| `FieldMindLibraryScreen.kt` | REWRITE | Redesigned all 5 tabs |
| `FieldMindDialogs.kt` | REWRITE | New dialog designs |
| `InsightsScreen.kt` | REWRITE | 3-tab analysis dashboard |
| `FieldMindMapScreen.kt` | REWRITE | Single map + bottom sheet |
| `FieldMindArchiveScreen.kt` | REWRITE | Search with filters |
| `FieldMindLockScreen.kt` | UPDATE | Animations + polish |
| NEW: `PaperEditorScreen.kt` | CREATE | Research Paper Maker |
| NEW: `ExportStudioScreen.kt` | CREATE | Export Studio |
| NEW: `DataStudioScreen.kt` | CREATE | Unified data tools |
| NEW: `AudioPlayerComponent.kt` | CREATE | Audio playback |
| NEW: `PhotoEditorComponent.kt` | CREATE | Crop/rotate/annotate |
| NEW: `PdfViewerComponent.kt` | CREATE | PdfRenderer wrapper |
| NEW: `ImageGalleryScreen.kt` | CREATE | Evidence gallery |
