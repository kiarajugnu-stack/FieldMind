# FieldMind App — Comprehensive Analysis

> Based on full codebase review of `app/src/main/java/chromahub/rhythm/app/features/field/`

---

## 🔴 PLACEHOLDER / UNFINISHED FEATURES

### 1. Species Classifier — Completely Placeholder

**File:** `SpeciesClassifier.kt`

- `identifyFromImage()` never runs actual TFLite inference. Everything goes through `placeholderInference()` which does **filename string matching** against a species dictionary — not real image recognition.
- The "~15-50 MB TFLite model" (`species_classifier.tflite`) is never bundled. `loadModelFile()` always returns `null`.
- `placeholderInference()` returns **random confidence scores** (`0.65f + Math.random() * 0.20`).
- **Result**: The "AI species identification" button exists in the UI but fundamentally does not work.

### 2. Regional Species Packs — Never Downloaded

**File:** `SpeciesDatabase.kt`

`RegionalPack` system has boilerplate for 4 regions (NA, EU, Asia, Tropics) with model URLs, but there is **no actual download mechanism**. `downloadState` is always empty, and `markPackDownloaded()` is never called from any screen.

### 3. AI / Gemini Assistant — Shell Only

**File:** `GeminiResearchAssistant.kt`

The file exists but the Settings screen's "AI assistant" page is just UI scaffolding with **no evidence of actual API integration working end-to-end**.

### 4. Weather Backup Providers — Dead Code

**Files:** `WeatherApiDotComProvider.kt`, `OpenWeatherMapProvider.kt`

Both providers exist with full implementations but all weather fetching routes through `OpenMeteoProvider` by default. The others are dead code unless the user manually switches providers and enters API keys.

### 5. Export System — Wiring Incomplete

**File:** `FieldMindExport.kt`

The export engine exists but there's **no actual export-to-PDF or share-to-file flow** visible in the UI screens. Report templates in `FieldTemplate.kt` are never wired into a template picker in the actual report creation dialogs.

### 6. MaplibreMapView — Likely Stub

**File:** `MaplibreMapView.kt`

Many map interactions (offline tiles, GeoJSON drawing, actual GPS tracking) appear skeletal or incomplete.

---

## 🔶 BAD UX DESIGN

### 1. Onboarding → Feature Wall Shock

The app has **9+ entity types** (Observations, Notes, Questions, Hypotheses, Projects, Sources, Data Records, Reports, Flashcards) all presented with equal weight on the home screen. A first-time user is **overwhelmed by choice** with no guided path.

### 2. Mega-Forms with No Progressive Disclosure

**File:** `FieldMindDialogs.kt`

- **New Source dialog**: 20+ text fields in a flat scroll. FieldTextField, ChoiceChips, file pickers, rating sliders — all visible at once.
- **New Project dialog**: ~15 fields with 7 "required" sections. No step-by-step wizard.
- **Bad pattern**: `CollapsibleSection` hides "Advanced options" but these contain fields like "Project type" and "Methods" that should be upfront, while less important fields like "Connection map" are equally exposed.

### 3. Inconsistent Navigation & Modal Patterns

- `DialogWrapper` has TWO modes: `fullScreen` and modal. Some dialogs use one, some the other, with no consistent rule.
- `HomeScreen` uses `Dialog` directly for camera and category picker (not DialogWrapper), while note creation uses a separate custom dialog (`HomeNoteCaptureDialog`).
- Some screens are `Dialog` (ObserveScreen), some are full composable screens (QuestionsScreen), some are bottom sheets (SpeciesIdentificationSheet).
- **No consistent navigation paradigm**.

### 4. Weather Dashboard: Over-designed Card in Wrong Context

`LiveWeatherDashboardWidget` is a **~320-line function** inside `FieldMindHomeScreen.kt` with:
- Animated weather scenes, glass-morphism gradients, pulsing live indicators
- 7-day forecast with expandable day tiles, animated content, temperature range bars
- Developer mode test panel
- All in a single `Card` composable on the home screen

This makes the home screen slow to render and hard to maintain. Weather should be a separate screen with a smaller widget on home.

### 5. Research Session UX Gap

The Research Session feature has CTA cards and timer UI, but:
- No way to see active session status while navigating away
- Session observations filter by tag `"research-session"` — a hidden convention users won't know
- Timer is defined in `FieldTimer.kt` but session management is spread across ViewModel, DAO, and UI with no cohesive UX

### 6. Task & Species Registry — No Dedicated UI

`TaskEntity` and `SpeciesEntity` are defined in the database and DAO with full CRUD, but there is **no dedicated UI screen** for either. Tasks appear only as linked references. Species has `SpeciesIdentificationSheet` as a bottom sheet but no dedicated species management screen.

---

## 🔶 BAD UI / CODE QUALITY ISSUES

### 1. Monolithic Screen Files

**`FieldMindHomeScreen.kt`**: **2,713 lines** containing:
- HomeScreen
- LiveWeatherDashboardWidget
- HomeHeroSection
- HomeNoteCaptureDialog
- HeroStatBubble, HeroActionChip
- timeOfDay()
- ResearchPulseCard, PulseMetric
- QuickActionsRow, ReadingReviewCard
- MiniActionTile, ObservationTimelinePreview
- CurrentProjectResearchCard, ProjectAssetChip
- RecentActivityGroupCard
- recommendedResources(), RecommendedLearningCard
- plus ~15 more private composables

**`FieldMindDialogs.kt`**: ~2,200+ lines containing:
- DialogWrapper, DialogHeader, DialogDividerSection, DialogActions
- ChoiceChipsField, CollapsibleSection, ProgressiveSection
- **14 separate dialog composables** (NewQuestion, NewProject, NewSource, NewHypothesis, NewDataRecord, NewReport, NewFlashcard, NewNote, EditEntity, plus all entity-specific editors)

### 2. Inconsistent Coding Patterns

- Some dialogs use `DialogWrapper` (the centralized wrapper), others use raw `Dialog` + `Card` directly.
- Some use `FieldTextField` (custom component), others use raw `OutlinedTextField`.
- `HomeNoteCaptureDialog` reimplements what `NewNoteDialog` already does — **duplicate code**.

### 3. No Loading/Error States for Data Operations

- `addObservation()` and similar ViewModel methods launch coroutines but provide **no UI feedback** for loading or errors.
- `restoreArchiveJson()` takes a callback but most entity operations just silently fire-and-forget.

### 4. Hardcoded Strings Everywhere

Categories (`"Bird"`, `"Mammal"`, etc.), confidence levels (`"Sure"`, `"Likely"`), source types, report types — all hardcoded inline. Makes localization impossible.

### 5. Redundant Imports

**File:** `FieldMindHomeScreen.kt`
- `expandVertically` and `shrinkVertically` imported twice (lines 7-8 and again at lines 13-14).

### 6. Inconsistent Indentation & Brace Style

The file uses a mix of tab and space indentation, leading to mismatched braces that are impossible to spot visually (as evidenced by prior compilation errors).

### 7. Massive Constants File for Minimal Use

**File:** `TaxonomyData.kt`
Contains a comprehensive biological taxonomy (kingdoms, phyla, classes, orders, families) but only surfaced through `TaxonomyPickerDialog` in the species registration form — a rarely-used feature.

---

## 📊 PRIORITY TABLE

| Area | Status | Priority | Effort |
|------|--------|----------|--------|
| Species identification (ML) | Placeholder — never works | **High** | Large |
| HomeScreen file size | 2,713 lines — needs splitting | **High** | Medium |
| Dialogs file size | ~2,200 lines — needs splitting | **High** | Medium |
| No loading/error states | All data ops fire-and-forget | **High** | Medium |
| Progressive disclosure missing | 20-field flat forms | **High** | Medium |
| Mega-forms overwhelm users | New Source = 20+ fields | **High** | Medium |
| Missing Task UI screen | DB entity with no screen | Medium | Small |
| Missing Species Registry UI | DB entity with no screen | Medium | Small |
| Inconsistent dialog patterns | Mix of DialogWrapper, raw Dialog | Medium | Medium |
| Weather widget over-engineered | 320-line Card on home screen | Medium | Medium |
| Hardcoded strings everywhere | Cannot localize | Medium | Large |
| Export system incomplete | Wiring not connected | Low | Medium |
| Regional species packs | Never downloaded | Low | Small |
| TaxonomyData over-engineered | Rarely-used massive file | Low | Small |

---

## 🎯 RECOMMENDED NEXT STEPS

1. **Split monolithic files** — Extract `LiveWeatherDashboardWidget`, `HomeNoteCaptureDialog`, and all stat helper composables from `FieldMindHomeScreen.kt` into separate files. Split the dialog composables out of `FieldMindDialogs.kt`.

2. **Add loading/error states** — Create a sealed `UiState<T>` class (`Loading | Success | Error`) and wrap all ViewModel data flows. Surface errors via Snackbar.

3. **Add progressive disclosure** — Convert the 20-field Source form into a multi-step wizard with clear section headers and field grouping.

4. **Fix the species classifier** — Either bundle a real TFLite model or remove the feature entirely.

5. **Extract strings** — Move all hardcoded strings into `strings.xml` for localization readiness.
