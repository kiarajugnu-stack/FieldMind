# FieldMind Animation & Gesture Unification Plan

**Date:** 2026-06-22
**BOM:** `2026.05.01` (includes `SharedTransitionLayout`, `Modifier.sharedElement`, `Modifier.anchoredDraggable`)

---

## Overview

Unify the FieldMind Android app with smooth, fluid animations with gestures for every screen and proper back state preservation.

---

## Phase 1: Animation System Enhancement (Foundation)

**Files:**
- `.../components/FieldMindMotion.kt` — Add new animation tokens
- `.../components/FieldMindSharedTransitions.kt` — Expand transition helpers

### 1a — New animation tokens in `FieldMindMotion`

| Token | Type | Purpose |
|-------|------|---------|
| `swipeBackSpring` | `spring<Float>` | Low-stiffness spring for edge-swipe back gesture (0.5f damping, 300f stiffness) |
| `sharedElementSpring` | `spring<Float>` | Optimized for card→detail morphing (0.65f damping, 600f stiffness) |
| `slideSpring` | `spring<Float>` | Smooth axis slide (0.75f damping, 700f stiffness) |

### 1b — Expanded `FieldMindTransitions`

- Replace unused `sharedElementScale()` / `containerBounds()` with real specs
- Add `sharedAxisHorizontal` for list→detail screen nav
- Add `fadeThrough` with configurable duration
- Add `scaleIn + fade` combo for modal transitions
- Add route-pair → transition spec map (e.g. tab→tab = slide, list→detail = shared axis)

---

## Phase 2: Shared Element Screen Transitions

**File:** `.../navigation/FieldMindNavigation.kt`

### 2a — Wrap NavHost in `SharedTransitionLayout`

```kotlin
SharedTransitionLayout {
    NavHost(
        enterTransition = { /* per-route transitions */ },
        exitTransition = { /* reversed */ }
    ) { ... }
}
```

### 2b — Per-route transition mapping

Replace `primaryTabDirection()` with `routeTransitionSpec()` that returns:
- **Tab→Tab**: Enhanced horizontal slide + fade (existing logic improved)
- **Tab→Sub-screen**: Fade + scale (0.97 initial scale)
- **List→Detail**: Shared axis horizontal (slide 25% of width)
- **Hub→Page** (settings): Fade-through
- **Back navigation**: Always reverse the spec

### 2c — Pass scope to screens

Pass `SharedTransitionScope` + `AnimatedContentScope` to screen composables for shared elements.

---

## Phase 3: Swipe-to-Go-Back

**Files:** `.../navigation/FieldMindNavigation.kt`, `.../components/FieldMindMotion.kt`

Create `SwipeBackHost` composable:
```kotlin
@Composable
fun SwipeBackHost(onBack: () -> Unit, content: @Composable () -> Unit)
```
- Use `Modifier.anchoredDraggable` with 3 anchors (0, threshold, full)
- On release past threshold → call `popBackStack()`, animate out
- Otherwise → snap back to 0
- Only on non-tab screens

---

## Phase 4: Back State Preservation

**Files:** Settings, Insights, Home, Library, Projects screens

- Add `rememberSaveable(saver = LazyListState.Saver) { LazyListState() }` to scrollable screens
- Save expanded UI sections via `rememberSaveable`
- Fix `restoreState` to avoid ViewModel crash (only use for tab destinations)

---

## Phase 5: Universal Press/Lift Animations

**File:** `.../components/FieldMindMotion.kt` (modifiers already exist)
**Apply to:** All interactive cards and `Surface(onClick=...)` patterns

- Create `ClickableCard` wrapper that applies `expressiveCardPress()`
- Add `pressScale` to all non-card clickable surfaces

---

## Phase 6: Staggered List Animations

**File:** `.../components/FieldMindComponents.kt`

`EntityCard` already has `animate: Boolean` + `index: Int` params with stagger-enter animation. Pass `animate = true` and `index` to all `items { }` usage sites.

---

## Implementation Order

```
Phase 1 → Phase 2 → Phase 5 → Phase 6 → Phase 3 → Phase 4
```

- **Phase 1-2**: Foundation + core nav transitions (biggest visual impact)
- **Phase 5-6**: Polish (press animations, stagger)
- **Phase 3**: Gesture (swipe back)
- **Phase 4**: State safety (rememberSaveable)
