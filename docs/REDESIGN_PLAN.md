# FieldMind Redesign Plan
## Widgets · Toasts · Validation · Pages · Details

> **Created:** 2026-06-13  
> **Status:** Planning — awaiting implementation  
> **Branch:** `feat/fieldmind-research-redesign`

---

## Table of Contents

1. [Widget System](#1-widget-system)
2. [Toast / Snackbar Redesign](#2-toast--snackbar-redesign)
3. [Required Field Validation](#3-required-field-validation)
4. [App Pages Redesign](#4-app-pages-redesign)
5. [Notes Redesign](#5-notes-redesign)
6. [Detail Pages Redesign](#6-detail-pages-redesign)

---

## 1. Widget System

### Current State
- **Music widgets only**: `RhythmMusicWidget`, `RhythmLyricsWidget` via Glance framework
- **Home screen grid**: `HomeWidgetGrid` shows 6 static tiles (Capture, Notes, Questions, Sources, Projects, Outputs) — navigation-only, no data
- **No FieldMind research widgets** exist for Android home screen

### Plan: FieldMind Research Home Screen Widgets

#### 1A. Quick Capture Widget (2×1)
- **Purpose**: One-tap observation capture from Android home screen
- **Layout**: Camera icon + "Quick Observe" text + category dropdown
- **Action**: Opens `ObserveScreen` with pre-selected category, or launches camera directly
- **Size**: Small (2×1 cells)
- **Glance implementation**: `FieldMindQuickCaptureWidget : GlanceAppWidget`

#### 1B. Research Dashboard Widget (4×3)
- **Purpose**: Overview of active research status
- **Layout**:
  ```
  ┌──────────────────────────────┐
  │ 🔬 FieldMind Research        │
  │ ─────────────────────────── │
  │ 📷 12 observations          │
  │ ❓ 5 open questions         │
  │ 📊 3 active projects        │
  │ ⏱️ Session: 23m active      │
  │ ─────────────────────────── │
  │ [+ New Observation] [Session]│
  └──────────────────────────────┘
  ```
- **Data source**: Query `FieldMindDatabase` for counts, read last session timestamp
- **Refresh**: On data change via `Flow` observation + periodic 15-min refresh
- **Size**: Large (4×3 cells)

#### 1C. Daily Summary Widget (4×1)
- **Purpose**: Today's research activity at a glance
- **Layout**: "Today: 3 obs, 1 note, 2 questions" with progress ring
- **Size**: Small (4×1 cells)

#### Implementation Steps
1. Create `FieldMindQuickCaptureWidget.kt` in `infrastructure/widget/glance/`
2. Create `FieldMindDashboardWidget.kt` in `infrastructure/widget/glance/`
3. Create `FieldMindDailyWidget.kt` in `infrastructure/widget/glance/`
4. Create `FieldMindWidgetReceiver.kt` for each widget
5. Register widgets in `AndroidManifest.xml`
6. Create widget info XML in `res/xml/widget_info_fieldmind_*.xml`
7. Create widget layouts in `res/layout/widget_fieldmind_*.xml`
8. Create `FieldMindWidgetUpdater.kt` to push data from ViewModel → Glance
9. Add widget configuration screen in Settings
10. Add "Add to home screen" prompt in onboarding

---

## 2. Toast / Snackbar Redesign

### Current State
- **107+ `android.widget.Toast.makeText()` calls** across the codebase — old-style Android toasts
- **Mixed system**: Some screens use `SnackbarHostState` (ObserveScreen, ResearchSession, InsightsScreen, BackupExportScreen) while most use raw `Toast`
- **No consistency**: FieldMind screens use a mix of both

### Plan: Unified Material3 Snackbar System

#### 2A. Create `FieldMindSnackbar` Utility
- **New file**: `app/src/main/java/chromahub/rhythm/app/features/field/presentation/components/FieldMindSnackbar.kt`
- **Centralized snackbar host**: Single `SnackbarHostState` shared across all FieldMind screens via `CompositionLocal`
- **Action types**: Success (green accent), Error (red), Info (primary), Warning (orange)
- **Auto-dismiss**: 2s for success, 4s for errors, 3s for info
- **Action buttons**: "Undo" for deletions, "View" for navigations, "Retry" for failed operations

```kotlin
// Proposed API
@Composable
fun FieldMindSnackbarProvider(content: @Composable () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalFieldMindSnackbar provides snackbarHostState) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { content() }
    }
}

// Usage
val snackbar = LocalFieldMindSnackbar.current
scope.launch { snackbar.showSnackbar("Observation saved", actionLabel = "View", duration = Short) }
```

#### 2B. Toast → Snackbar Migration (by priority)
| Priority | File | Current | Target |
|----------|------|---------|--------|
| P0 | `FieldMindObserveScreen.kt` | Mixed (both) | Snackbar only |
| P0 | `FieldMindDetailScreen.kt` | `Toast.makeText` | Snackbar |
| P0 | `FieldMindDialogs.kt` | `Toast.makeText` | Snackbar |
| P0 | `FieldMindLibraryScreen.kt` | `Toast.makeText` | Snackbar |
| P1 | `FieldMindSettingsScreen.kt` | `Toast.makeText` | Snackbar |
| P1 | `FieldMindHomeScreen.kt` | None | Add snackbar for actions |
| P2 | All `shared/presentation/screens/settings/` | `Toast.makeText` | Snackbar (lower priority — music player area) |

#### 2C. Toast-to-Snackbar Migration Pattern
```kotlin
// BEFORE (old)
android.widget.Toast.makeText(context, "Photo captured.", android.widget.Toast.LENGTH_SHORT).show()

// AFTER (new)
scope.launch { snackbar.showSnackbar("Photo captured ✓") }
```

#### Implementation Steps
1. Create `FieldMindSnackbar.kt` with `CompositionLocal` provider
2. Wrap FieldMind navigation root with `FieldMindSnackbarProvider`
3. Migrate P0 files (ObserveScreen, DetailScreen, Dialogs, LibraryScreen)
4. Migrate P1 files (SettingsScreen, HomeScreen)
5. Add "Undo" action snackbar for entity deletions
6. Add progress snackbar for long operations (export, backup)
7. Clean up unused `android.widget.Toast` imports

---

## 3. Required Field Validation

### Current State
- `FieldTextField` has no `required` parameter
- `saveEnabled` on `InlineFormCard` checks individual conditions per-form (e.g., `title.isNotBlank()`)
- No visual indicator for required fields
- No real-time validation feedback

### Plan: Required Field System

#### 3A. Enhanced `FieldTextField` Component
```kotlin
@Composable
fun FieldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    supportingText: String? = null,
    required: Boolean = false,           // NEW
    validator: ((String) -> String?)? = null,  // NEW: returns error or null
    enabled: Boolean = true
)
```

**Visual treatment for required fields:**
- Red asterisk (*) after label: `"$label *"`
- Error state: red border + error text below
- Real-time validation on blur or after 500ms debounce

#### 3B. `RequiredFieldState` Composable
```kotlin
@Composable
fun rememberRequiredFieldState(
    initialValue: String = "",
    validator: (String) -> String? = { null }
): RequiredFieldState

class RequiredFieldState(initialValue: String, validator: (String) -> String?) {
    var value: String
    val isError: Boolean
    val errorMessage: String?
    fun validate(): Boolean
}
```

#### 3C. Form-Level Validation
Enhance `InlineFormCard` to accept a list of `RequiredFieldState` and auto-manage `saveEnabled`:

```kotlin
InlineFormCard(
    title = "New Observation",
    requiredFields = listOf(subjectState, categoryState),
    onDismiss = { ... },
    onSave = { ... }
)
```

#### 3D. Required Fields by Entity
| Entity | Required Fields | Optional Fields |
|--------|----------------|-----------------|
| Observation | Subject, Category | Facts, Confidence, GPS, Tags, Evidence |
| Note | Title OR Body (at least one) | Category, Tags, Attachments |
| Question | Question text, Category | Priority, Status |
| Hypothesis | Prediction | Reasoning, Evidence, Linked Question |
| Project | Title, Research Question | Objective, Methods, Background |
| Source | Title, Type | Author, Year, DOI, Link, Reading Notes |
| Data Record | Label, Value | Tool, Unit, Notes |
| Report | Title, Type | Question, Conclusion |

#### Implementation Steps
1. Add `required`, `validator` params to `FieldTextField`
2. Create `RequiredFieldState` class and `rememberRequiredFieldState` composable
3. Add error state styling (red border, error text, asterisk)
4. Update `InlineFormCard` to support `requiredFields` parameter
5. Update all form usages in ObserveScreen, ProjectsScreen, LibraryScreen, Dialogs
6. Add real-time validation feedback (debounced, on-blur)

---

## 4. App Pages Redesign

### Current State
- **HomeScreen**: 6-tile grid + recent items + quick actions
- **ObserveScreen**: Tab-based (Capture, Notes, GPS) — functional but dense
- **ProjectsScreen**: 3 tabs (Projects, Evidence, Analysis) — recently redesigned
- **LibraryScreen**: Sources + Flashcards — minimal
- **InsightsScreen**: Charts + achievements — functional

### Plan: Page Redesigns

#### 4A. Home Screen Redesign
- **Hero section**: Welcome message + today's stats (observations today, active projects, streak)
- **Quick actions row**: "📷 Capture", "📝 Note", "❓ Question", "⏱️ Session" — horizontal scrollable chips
- **Research pulse card**: Animated card showing last 24h activity with mini chart
- **Recent activity feed**: Last 5 items with entity icons and timestamps
- **Widget preview**: Show home screen widget setup prompt

#### 4B. Observe Screen Redesign
- **Evidence-first layout**: Camera/gallery as primary action, not a tab
- **Step indicator**: Visual progress through capture → categorize → save flow
- **Quick capture mode**: Floating action button that opens camera directly
- **Smart defaults**: Auto-suggest category based on recent observations
- **Batch mode**: Select multiple photos → batch-categorize

#### 4C. Library Screen Redesign
- **Unified evidence list**: All sources, flashcards, and documents in one scrollable view
- **Filter bar**: Entity type + reading status + importance
- **Reading progress**: Visual indicator for in-progress sources
- **Quick add**: Persistent bottom action bar for adding sources

#### 4D. Insights Screen Redesign
- **Research timeline**: Visual timeline of all entities with connections
- **Achievement system**: Redesigned badges with unlock animations
- **Export hub**: Centralized export with format selection
- **Stats dashboard**: Interactive charts with drill-down

---

## 5. Notes Redesign

### Current State
- Notes are simple text fields with category/tags
- No rich text editing
- No markdown support
- Basic attachment system (pipe-delimited string)
- Edit form is minimal: title, body, category, tags, attachments

### Plan: Enhanced Notes System

#### 5A. Rich Note Editor
- **Markdown support**: Headings, bold, italic, lists, code blocks
- **Inline images**: Embed observation photos directly in note body
- **Checklists**: Toggle-able task lists within notes
- **Voice notes**: Record audio attachments inline
- **Templates**: Pre-built note templates (Observation Log, Literature Notes, Meeting Notes)

#### 5B. Note Templates
```kotlin
val noteTemplates = listOf(
    "Blank" to NoteTemplate(emptyList()),
    "Observation Log" to NoteTemplate(listOf(
        "Subject:", "Location:", "Weather:", "Observations:", "Notes:"
    )),
    "Literature Notes" to NoteTemplate(listOf(
        "Source:", "Key Arguments:", "Evidence:", "My Analysis:", "Questions:"
    )),
    "Meeting Notes" to NoteTemplate(listOf(
        "Date:", "Attendees:", "Agenda:", "Decisions:", "Action Items:"
    )),
    "Field Journal" to NoteTemplate(listOf(
        "Date:", "Time:", "Location:", "Conditions:", "Findings:", "Follow-up:"
    ))
)
```

#### 5C. Note Organization
- **Folders/sections**: Group notes by project or topic
- **Pin notes**: Pin important notes to top
- **Note linking**: Link notes to observations, sources, questions
- **Backlinks**: Show which entities reference this note
- **Full-text search**: Search within note body content

#### 5D. Note Widgets
- **Pinned notes widget**: Show 3 pinned notes on home screen
- **Quick note widget**: One-tap note creation from home screen

#### Implementation Steps
1. Create `FieldMindRichEditor.kt` with markdown rendering
2. Create `NoteTemplate` data class and template picker
3. Update `NoteEntity` with optional `templateId` and `isPinned` fields
4. Create `NoteEditorScreen` with sections/steps
5. Add note linking UI (select related entities)
6. Create note widgets (Glance)
7. Add full-text search support in DAO

---

## 6. Detail Pages Redesign

### Current State
- Single `DetailScreen` composable handles all entity types
- Read-only display with Edit/Delete actions
- Basic field listing with `DetailBody` composable
- `InlineEditNote` and `InlineEditObservation` for inline editing
- `BacklinksPanel` for related entities
- Uses `android.widget.Toast` for feedback

### Plan: Enhanced Detail Pages

#### 6A. Entity-Specific Detail Layouts

**Observation Detail:**
- Hero image gallery (top)
- Subject + category badge
- GPS map card with location pin
- Weather snapshot card
- Facts section with confidence indicator
- Timeline of related observations
- Related data records chart

**Note Detail:**
- Rich markdown rendering
- Inline image viewer
- Checklist progress (if applicable)
- Related entities panel
- Version history (last 5 edits)

**Question Detail:**
- Question text (large, prominent)
- Status badge (New → Researching → Tested → Answered)
- Answer card with edit capability
- Related hypotheses as cards
- Suggested next steps from AI assistant

**Project Detail:**
- Project overview card with progress ring
- Tabs: Overview, Evidence, Analysis, Reports
- Research timeline
- Data visualization (charts)
- Export project button

**Source Detail:**
- Source preview (PDF viewer / image / web link)
- Citation card with copy button
- Reading progress tracker
- Flashcard generation panel
- Related questions and hypotheses

**Report Detail:**
- Formatted report view (not raw markdown)
- Section navigation (Introduction, Methods, Results, Discussion)
- Export as PDF/Markdown
- Share button

#### 6B. Detail Screen Improvements
- **Tabbed layout**: Each entity detail uses horizontal tabs for sections
- **Floating action button**: Context-sensitive FAB (e.g., "Add data" on observation detail)
- **Swipe actions**: Swipe to delete, swipe to link
- **Share button**: Share entity as formatted text or image
- **Export button**: Export single entity as PDF/Markdown
- **Print support**: Print-optimized layout for reports

#### 6C. Detail Page Components
```kotlin
// New components for detail pages
@Composable fun DetailHeroSection(...)     // Image gallery / map at top
@Composable fun DetailStatBar(...)         // Quick stats row
@Composable fun DetailTimeline(...)        // Related entity timeline
@Composable fun DetailActionBar(...)       // Bottom action bar
@Composable fun DetailSectionTabs(...)     // Tabbed content sections
@Composable fun DetailExportSheet(...)     // Export options bottom sheet
```

#### Implementation Steps
1. Create entity-specific detail composables (ObservationDetail, NoteDetail, etc.)
2. Create `DetailHeroSection` with image gallery
3. Create `DetailStatBar` with quick metrics
4. Create `DetailActionBar` with share/export/print
5. Create `DetailTimeline` for related entities
6. Update `DetailScreen` to route to entity-specific layouts
7. Add export-as-PDF functionality
8. Add print-optimized layout
9. Replace all `Toast.makeText` in DetailScreen with snackbar
10. Add swipe gestures for quick actions

---

## Implementation Priority

### Phase 1 (Immediate)
1. ✅ Toast → Snackbar migration (P0 files)
2. ✅ Required field validation system
3. ✅ `FieldTextField` required param + error state

### Phase 2 (Next Sprint)
4. Home screen redesign
5. Note editor with markdown
6. Detail page entity-specific layouts

### Phase 3 (Following)
7. Widget system (Quick Capture + Dashboard)
8. Note templates
9. Export as PDF
10. Rich note editor with inline images

### Phase 4 (Future)
11. Batch operations
12. Note folders/sections
13. Research timeline visualization
14. Achievement system redesign

---

## Files to Create
| File | Purpose |
|------|---------|
| `FieldMindSnackbar.kt` | Centralized snackbar system |
| `FieldMindQuickCaptureWidget.kt` | Home screen quick capture widget |
| `FieldMindDashboardWidget.kt` | Home screen research dashboard widget |
| `FieldMindDailyWidget.kt` | Home screen daily summary widget |
| `FieldMindWidgetUpdater.kt` | Widget data synchronization |
| `RequiredFieldState.kt` | Form validation state class |
| `NoteEditorScreen.kt` | Rich note editor |
| `NoteTemplates.kt` | Note template definitions |
| `ObservationDetail.kt` | Entity-specific observation detail |
| `NoteDetail.kt` | Entity-specific note detail |
| `QuestionDetail.kt` | Entity-specific question detail |
| `ProjectDetail.kt` | Entity-specific project detail |
| `SourceDetail.kt` | Entity-specific source detail |
| `ReportDetail.kt` | Entity-specific report detail |
| `DetailHeroSection.kt` | Image gallery hero component |
| `DetailTimeline.kt` | Related entity timeline |

## Files to Modify
| File | Changes |
|------|---------|
| `FieldMindComponents.kt` | Add `required`, `validator` to `FieldTextField` |
| `FieldMindScreenUtils.kt` | Add `requiredFields` to `InlineFormCard` |
| `FieldMindDetailScreen.kt` | Route to entity-specific details |
| `FieldMindObserveScreen.kt` | Toast → Snackbar, evidence-first layout |
| `FieldMindHomeScreen.kt` | Redesign hero section, add widget prompt |
| `FieldMindLibraryScreen.kt` | Toast → Snackbar, unified list |
| `FieldMindDialogs.kt` | Toast → Snackbar |
| `FieldMindSettingsScreen.kt` | Toast → Snackbar, widget settings |
| `FieldMindNavigation.kt` | Add new routes |
| `AndroidManifest.xml` | Register widget receivers |
| `FieldMindDatabase.kt` | Add note template fields |
| `FieldMindDao.kt` | Add note full-text search |
