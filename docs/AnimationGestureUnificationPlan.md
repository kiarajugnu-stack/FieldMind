# FieldMind Animation & Gesture Unification Plan

**Date:** 2026-06-22 (last updated: 2026-06-22)
**BOM:** `2026.05.01` (includes `SharedTransitionLayout`, `Modifier.sharedElement`; `Modifier.anchoredDraggable` NOT available — removed in BOM 2026)

---

## Implementation Status

| Phase | Status | Notes |
|-------|--------|-------|
| 1a — Animation tokens | ✅ Complete | All tokens in `FieldMindMotion.kt` |
| 1b — Expanded transitions | ❌ Not started | `FieldMindTransitions` file not created |
| 2a — SharedTransitionLayout | ✅ Complete | `NavHost` wrapped in `SharedTransitionLayout` in `FieldMindNavigation.kt` |
| 2b — Per-route transitions | ✅ Complete | `routeEnterTransition()` / `routeExitTransition()` / etc. with category-based routing |
| 2c — Pass scope to screens | ❌ Not started | `SharedTransitionScope` / `AnimatedContentScope` not passed to screen composables |
| 3 — Swipe-to-Go-Back | ✅ Complete (with deviation) | See Phases section below |
| 4 — Back State Preservation | ❌ Not started | `restoreState` fix partially done (intentionally disabled `restoreState` on non-tab routes to prevent ViewModel crash) |
| 5 — Press/Lift animations | ⚠️ Partially complete | Modifiers exist; applied to ~14 clickable items in LibraryScreen + ObserveScreen; `ClickableCard` wrapper not created |
| 6 — Staggered List Animations | ❌ Not started | `EntityCard` has params but not wired at all usage sites |

### Compilation Fixes Applied (2026-06-22)

During the implementation of Phases 1-3, several compilation errors were resolved across **8+ files** to get the app building on CI:

- `FieldMindDatabase.kt` — Added missing canvas DAO imports (`CanvasBlockDao`, `DrawingDao`, `FigureMetaDao`)
- `InfiniteCanvas.kt` — Fixed `Placeable.place()` API (→ `position=IntOffset`), added `IntOffset` import
- `CanvasBlock.kt` — Fixed `align` scope (moved `Modifier.align()` to `BoxScope` call site), fixed `padding` import
- `TextBlock.kt` — Added `AnnotatedString` import, fixed `TransformedText` fully-qualified path
- `TableBlock.kt` — Fixed `addRow()` type mismatch (`List<Any>` → `List<List<String>>`)
- `FieldMindMotion.kt` — Added `kotlinx.coroutines.launch` import
- `FieldMindNavigation.kt` — Added `AnimatedContentTransitionScope` / `NavBackStackEntry` imports; fixed brace imbalances in `species_detail` composable and at EOF
- `FieldMindCanvasScreen.kt` — Removed invalid `canvas.blocks.*` import
- `FieldMindHomeScreen.kt` / `LibraryScreen.kt` / `InsightsScreen.kt` — Fixed named/positional argument mixing

---

## Phase 1: Animation System Enhancement (Foundation)

**Files:**
- `.../components/FieldMindMotion.kt` — Build upon existing tokens
- `.../components/FieldMindSharedTransitions.kt` — Create transition helpers

### 1a — New animation tokens in `FieldMindMotion` ✅

| Token | Type | Purpose |
|-------|------|---------|
| `swipeBackSpring` | `spring<Float>` | Low-stiffness spring for edge-swipe back gesture (0.5f damping, 300f stiffness) |
| `sharedElementSpring` | `spring<Float>` | Optimized for card→detail morphing (0.65f damping, 600f stiffness) |
| `slideSpring` | `spring<Float>` | Smooth axis slide (0.75f damping, 700f stiffness) |

All three tokens already present. Also includes:
- `expressiveSpring`, `expressiveSoft`, `expressiveElastic`, `expressiveFloat`, `expressiveSnap`, `expressiveDramatic`
- `layoutSpring`, `pressSpring`, `confirmSpring`
- `morphSpring`, `cornerSpring`, `fadeTween`, `pressScaleTween`
- `isReduceMotion()` composable for accessibility
- `staggerDelay()` utility

### 1b — Expanded `FieldMindTransitions` ❌

- `FieldMindSharedTransitions.kt` does not exist yet
- `sharedAxisHorizontal`, `fadeThrough`, and the route-pair spec map are not implemented

---

## Phase 2: Shared Element Screen Transitions ✅

**File:** `.../navigation/FieldMindNavigation.kt`

### 2a — Wrap NavHost in `SharedTransitionLayout` ✅

Already implemented:
```kotlin
SharedTransitionLayout {
    NavHost(
        enterTransition = { routeEnterTransition() },
        exitTransition = { routeExitTransition() },
        popEnterTransition = { routePopEnterTransition() },
        popExitTransition = { routePopExitTransition() }
    ) { ... }
}
```

### 2b — Per-route transition mapping ✅

Implemented via `categorizeRoute()` + 4 transition functions:
- `routeEnterTransition()` — Computes enter animation based on route category pair
- `routeExitTransition()` — Corresponding exit animation
- `routePopEnterTransition()` — Reverse navigation enter
- `routePopExitTransition()` — Reverse navigation exit

Categories: `Tab`, `SettingsHub`, `SettingsSubPage`, `Detail`, `Tool`, `Creation`, `Other`
- Tab→Tab: Horizontal slide by index direction
- Tab→Sub-screen: Slide in from right (25% width) + fade
- Settings hub↔subpage: Fade-through
- Sub-screen→Tab (back): Slide from left + fade

### 2c — Pass scope to screens ❌

`SharedTransitionScope` and `AnimatedContentScope` are not passed to screen composables. Needed for `Modifier.sharedElement()` / `Modifier.sharedBounds()` to work.

---

## Phase 3: Swipe-to-Go-Back ✅ (with deviation)

**Files:** `.../components/FieldMindMotion.kt`, `.../navigation/FieldMindNavigation.kt`

### `SwipeBackHost` composable ✅

```kotlin
@Composable
fun SwipeBackHost(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

**Deviation from plan:** Uses `detectHorizontalDragGestures` + `animateFloatAsState` instead of `Modifier.anchoredDraggable`. Reason: `anchoredDraggable` / `rememberAnchoredDraggableState` were removed from Compose BOM `2026.05.01`. The implementation:

- Tracks drag offset via `detectHorizontalDragGestures` in a `Modifier.pointerInput` block
- Animates offset back with `swipeBackSpring` via `animateFloatAsState`
- On release past 30% threshold → animates to full width and calls `onBack()`
- Otherwise → snaps back to 0
- Renders a scrim overlay (darkens content as swipe progresses)
- Shows back arrow indicator on left edge during drag
- Respects reduce-motion accessibility setting

### Applied to all non-tab screens ✅

Every `composable()` route in `FieldMindNavigation.kt` except the 5 main tabs wraps content in `SwipeBackHost(onBack = { navController.popBackStack() }) { ... }`:

Settings, Settings subscreens, Learn, Reader, FieldMode, Questions, Hypotheses, DataTools, Analysis, Reports, Search, Map, Export, Changelog, Progress, Flashcards, ResearchSession, WeatherDatabase, SpeciesBrowser, TaxonomicBrowser, FieldLog, CounterTool, MeasurementTool, WeatherLogTool, Canvas, Detail screen, and all creation/editing screens.

---

## Phase 4: Back State Preservation ❌

Not implemented. The `restoreState = true` is used in `navigateToTab()` for tab navigation (with a guard against the ViewModel crash). Non-tab navigation explicitly avoids `restoreState` to prevent crashes. `rememberSaveable` for scroll positions / expanded sections is not in place.

---

## Phase 5: Universal Press/Lift Animations ⚠️

**File:** `.../components/FieldMindMotion.kt`

### Modifiers available ✅

- `Modifier.pressScale(scaleDown: Float)` — Scales down on press with spring return
- `Modifier.expressivePress(scaleDown: Float)` — Expressive spring bounce-back
- `Modifier.expressiveCardPress(liftDp, scaleDown)` — Lift + scale for cards

### Applied to clickable items ⚠️

| Screen | Items | Modifier |
|--------|-------|----------|
| LibraryScreen | 9 items (SourcePanel expander, SourceCard rows, NotePanel toggle, LearnCategoryCard expander/resource, OnlineApiProposalCard, GuidedPathCard + step) | `pressScale(0.97f)` |
| ObserveScreen | 5 items (session toggle Card, EvidenceButton Cards ×2, categories/structured/protocol toggles) | `expressivePress(0.97f)` / `pressScale(0.97f)` |

### Not done ❌

- `ClickableCard` wrapper composable not created
- `expressiveCardPress()` not applied to any cards yet
- Many screens still use plain `clickable` without press animation
- `Surface(onClick=...)` patterns not systematically wrapped

---

## Phase 6: Staggered List Animations ❌

Not started. `EntityCard` has `animate` and `index` params but they aren't wired at usage sites.

---

## Remaining Work

| Priority | Phase | Work |
|----------|-------|------|
| P1 | Phase 1b | Create `FieldMindTransitions` helper with `sharedAxisHorizontal`, `fadeThrough`, route spec map |
| P1 | Phase 2c | Pass `SharedTransitionScope` + `AnimatedContentScope` to screens for shared elements |
| P2 | Phase 5 | Create `ClickableCard`, apply `expressiveCardPress()` to card patterns, wrap remaining clickable items |
| P3 | Phase 6 | Wire `animate = true` + `index` at all `EntityCard` usage sites |
| P4 | Phase 4 | Add `rememberSaveable` for scroll state on detail/settings screens |

---

## Implementation Order (Revised)

```
[Completed] Phase 1a → Phase 2a → Phase 2b → Phase 3 → Phase 5 (partial)
[Remaining] Phase 1b → Phase 2c → Phase 5 (wrap) → Phase 6 → Phase 4
```
