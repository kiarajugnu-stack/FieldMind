# Predictive Back Peek Animation — Why Mock Instead of Real?

**Generated:** 2026-06-24
**Files analyzed:**
- `app/src/main/java/fieldmind/research/app/features/field/presentation/components/FieldMindMotion.kt` — `TabSwipeHost` + `SwipeBackHost`
- `app/src/main/java/fieldmind/research/app/features/field/presentation/components/FieldMindSharedTransitions.kt` — Transition specs
- `app/src/main/java/fieldmind/research/app/features/field/presentation/navigation/FieldMindNavigation.kt` — NavHost, route transitions, `PreviousScreenInfo`

---

## The Short Answer

**You can't show the real screen during the peek because Compose Navigation only composes one destination at a time.** The previous destination is torn down when the new one enters. During the back gesture, the NavHost can't "reach back" and recompose the previous screen alongside the current one — there's no API for it. The system provides only a `progress: Float` (0→1) and expects you to animate a preview based on that progress. What you animate is up to you.

---

## 1. How the Current Peek Works

### The Mechanism

1. **`PredictiveBackHandler`** (Android 14+, `ExperimentalActivityApi`) is a composable that intercepts the system back gesture before it commits.

2. The handler receives a `progressFlow` that emits `BackEvent` objects with a single useful property: `progress` (0.0 → 1.0 as the user drags further from the edge).

3. The gesture either commits (user drags past threshold and releases) — then `onBack()` fires — or cancels (user releases before threshold) — then everything resets.

### What Currently Happens During the Peek

In `FieldMindMotion.kt` (~line 416):

```kotlin
if (progress > 0.01f && previousScreen != null && isSystemBack) {
    val previewWidth = contentWidth * 0.85f
    val previewOffset = animX.value - previewWidth
    val previewScale = 0.94f + (1f - 0.94f) * (1f - progress)
    // ...
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        // ── Mock content: fake status bar + 3 placeholder cards ──
        // NOT the real previous screen!
    }
}
```

It draws a **completely fake** preview — a generic `Surface` with placeholder card shapes and `previousScreen.label` as the title. It's a mock because:

- The real previous screen **is not composed** at this point
- The NavHost has no API to compose it alongside the current screen
- The system's `BackEventCompat` provides **no screen snapshot**, no bitmap, no composable handle

### The Pop Exit Transition (After Commit)

Once the gesture commits, Compose Navigation's `popExitTransition` runs:

```kotlin
// Current screen slides out to the right at full width
slideOutHorizontally(slideSpec) { it } + fadeOut(animationSpec = fadeSpec)
```

And `popEnterTransition` runs on the previous screen:

```kotlin
// Previous screen slides in from the left at full width
slideInHorizontally(slideSpec) { -it } + fadeIn(animationSpec = fadeSpec)
```

These **are** real screens — but they only appear after the gesture commits. During the gesture, it's all mock.

---

## 2. The Root Problem: Compose Navigation Doesn't Support Dual Composition

### Why the NavHost Can't Show Two Screens at Once

| Challenge | Details |
|-----------|---------|
| **Single composition tree** | The NavHost composes exactly one destination per back stack slot. The previous destination's composable is disposed when `popBackStack()` hasn't been called yet. |
| **No "preview" mode for destinations** | There's no `composable(route, preview = true) { }` or `rememberPreviousDestination()`. The NavHost has zero built-in support for rendering a destination that isn't the current one. |
| **No `previousBackStackEntry.composable` access** | `navController.previousBackStackEntry` exists and gives you the entry's saved state, but there's no way to force-compose its destination. It's a metadata holder, not a render target. |
| **State would need to be independent** | Even if you could compose the previous screen, it would need its own independent state (scroll position, text fields, loaded data). Compose doesn't support the same ViewModel/database flow being observed by two active composables simultaneously with different lifetimes. |

### The Technical Gap

```
Compose NavHost:
  ┌────────────────────────────────────────────────┐
  │                                                │
  │  ┌────────────────────────────────────┐        │
  │  │ CURRENT SCREEN (composed, active)  │        │
  │  │ - ViewModel collected              │        │
  │  │ - side effects running             │        │
  │  └────────────────────────────────────┘        │
  │                                                │
  │  ┌────────────────────────────────────┐        │
  │  │ PREVIOUS SCREEN (DISPOSED)         │ ✗ CANNOT│
  │  │ - ViewModel released               │ COMPOSE │
  │  │ - composition disposed             │ HERE    │
  │  └────────────────────────────────────┘        │
  │                                                │
  └────────────────────────────────────────────────┘
```

To show the real previous screen during the peek, **you'd need to be composing both simultaneously** — which the NavHost fundamentally cannot do.

---

## 3. Possible Approaches (And Why They're Hard)

### Approach A: Compose the Previous Destination Manually

**The idea:** When the peek starts, manually call the previous screen's composable function and overlay it behind the current screen.

**The problem:** You don't have a reference to the composable function. NavHost routes are registered as lambdas: `composable("field_detail/{kind}/{id}") { ... }`. There's no way to look up `"field_detail/observation/42"` and call its composable from outside the NavHost. The lambda is internal to the NavHost's `NavGraphBuilder`.

```kotlin
// This doesn't exist:
val previousComposable = navHostGraph.getComposable(previousRoute)
previousComposable() // Can't do this
```

### Approach B: Snapshot the Previous Screen as a Bitmap

**The idea:** Before navigating forward, capture the current screen as a bitmap. During the peek, show that bitmap as a preview.

**The problem:**
1. You need to know when navigation is about to happen (easy: intercept `onClick`).
2. Capturing a Compose hierarchy as a bitmap requires `View.drawToBitmap()`, which only works on a `View`-wrapped composable. You'd need access to the root `ComposeView`.
3. Bitmaps are expensive (~4MB for a 1080p screen × 4 bytes/pixel). Keeping them for every back stack entry uses significant RAM.
4. Static bitmaps don't show dynamic content (animations, live data, time-sensitive updates).
5. If the user navigates deep (detail → task → canvas → species), you'd need a chain of bitmaps.

```kotlin
// Pseudo-code — this IS possible but clunky:
val previousPageBitmap = remember { mutableStateOf<Bitmap?>(null) }
// On every navigation, capture current screen:
LaunchedEffect(navController.currentBackStackEntry) {
    previousPageBitmap.value = composeView.drawToBitmap()
}
// During peek, show bitmap:
previousPageBitmap.value?.let { bitmap ->
    Image(bitmap = bitmap.asImageBitmap(), ...)
}
```

**Verdict:** Possible but memory-heavy and low-res (no Retina). The bitmap is a static snapshot — not a living screen.

### Approach C: Use Two Overlapping NavHosts

**The idea:** Maintain two NavHosts — one for the current screen and one for the previous screen — stacked on top of each other. During the peek, make the previous one visible.

**The problem:**
1. Both NavHosts would need their own `NavController` and back stack.
2. Navigating forward would push to Host A and record the route in Host B.
3. On back, Host B would need to compose its last recorded route.
4. This completely breaks `SavedStateHandle`, `ViewModelStoreOwner`, and the standard navigation lifecycle.
5. Two ViewModels for the same entity would be observing the same Room queries, doubling CPU/DB load.
6. Gesture handling becomes a nightmare — which host receives touch events during the transition?

**Verdict:** Technically possible but architecturally catastrophic. Would require forking all lifecycle management.

### Approach D: Render the Previous Destination via `Modifier.drawWithCache`

**The idea:** Use `remember` + `drawWithCache` to capture the previous screen as a `Picture` (Compose's vector recording) before navigating away, then replay it during the peek.

**The problem:**
1. `Picture` captures draw commands but not recomposition — animations freeze, text doesn't update.
2. Only works for fully static content.
3. Same memory concerns as bitmaps (though `Picture` is more compact).
4. Compose doesn't expose a "record this subtree as a Picture" API readily. You'd need to use the internal `ClipboardManager` or render-into-`ImageBitmap` approach.

### Approach E: What Google's Own Apps Do

Google's own apps (Settings, Phone, Messages) **also use mock previews** for predictive back. They show:
- A generic placeholder with the previous screen's title/icon
- Blurred/skeleton content
- The app icon and accent color

This is the industry standard because **there is no better solution available** at the framework level. If Google can't show the real screen, no third-party app can either.

---

## 4. What Would Need to Change in Android/Compose

For true peek-to-real-screen to work, one of these would need to land in the Android framework:

### Option 1: Compose Navigation Supports "Peek Composable"

```kotlin
composable(
    route = "field_detail/{kind}/{id}",
    peekPreview = { entry -> 
        // Special composable rendered during the back gesture peek
        // Lightweight version of the screen — no ViewModel collection,
        // no side effects, just the visual shell
        PeekDetailScreen(kind = entry.arguments?.getString("kind"))
    }
) { entry ->
    // Full screen with ViewModel, database, animations
    RealDetailScreen(entry)
}
```

This would need Google to add a `peekPreview` parameter to `NavGraphBuilder.composable()` and update the NavHost to render it during back gestures. **No timeline from Google.**

### Option 2: System-Level Back Gesture Provides a Snapshot

Android's Predictive Back Gesture API could provide a developer-provided `Bitmap` or `ComposeNode` that it shows during the animation. Something like:

```kotlin
// Hypothetical API:
onBackInvokedDispatcher.registerBackCallback(object : BackCallback() {
    override fun onBackStarted(backEvent: BackEvent) {
        // System captures and shows our preview
        setPreview(previousScreenPreview)
    }
})
```

This doesn't exist either. The current API only provides `progress`.

### Option 3: Android 17+ Compose NavHost Back Stack Composition

Google could extend the NavHost to keep the previous destination **inactive but composed** in a hidden state. When the back gesture starts, it transitions from hidden → visible behind the current screen. This would require:
- Compose's `LayoutNode` to support "detached but alive" subtrees
- ViewModels to support "background but not cleared" mode
- Room/database flows to continue emitting while the screen is invisible but composed

**Status:** Not on any public roadmap.

---

## 5. What FieldMind COULD Do Right Now (Realistic Improvements)

Without waiting for framework changes, here are practical improvements:

### 5a. Render Previous Route as a Lightweight Composable

Create a *separate, lightweight* composable for each route that can be rendered during the peek. This wouldn't be the real screen, but it would be more accurate than the current generic mock.

**Example:**
```kotlin
@Composable
fun PeekPreview(route: String): @Composable () -> Unit = when {
    route.startsWith("field_detail/") -> { PeekDetailPreview(route) }
    route.startsWith("field_project_detail/") -> { PeekProjectPreview(route) }
    route == FieldMindScreen.Settings.route -> { PeekSettingsPreview() }
    else -> { PeekGenericPreview(previousScreenLabel(route)) }
}
```

This is **manual work** for every screen — maintaining a parallel "peek version" — but produces a much more convincing preview than generic placeholder cards.

### 5b. Use a Captured Picture (Approach D Refinement)

Use `ComposeView.drawToBitmap()` to capture the screen as an `ImageBitmap` **before navigating forward**. Store in a LRU cache (max 3 entries). During peek, render the bitmap.

```kotlin
// In a navigation-aware composable:
val screenCaptureProvider = LocalScreenCaptureProvider.current
// Before navigating to a new screen:
navController.addOnDestinationChangedListener { _, destination, _ ->
    view?.let { screenCaptureProvider.capture(it, destination.route) }
}
// In SwipeBackHost during peek:
screenCaptureProvider.getBitmap(previousScreen.route)?.let { bitmap ->
    Image(bitmap = bitmap.asImageBitmap(), ...)
}
```

**Limitations:**
- ~4MB per captured screen at 1080p
- Static content (no live data, no animations)
- The capture happens at navigation time, so fast sequential navigations might miss captures
- User sees a potentially stale snapshot if data changed

### 5c. Improve the Current Mock (Lowest Effort, Best Value)

Instead of 3 generic placeholder cards, customize the mock per screen type:

| Route Pattern | Mock Style |
|---------------|------------|
| `field_detail/observation/*` | Show mock observation card with icon, category color, date placeholder |
| `field_project_detail/*` | Show mock project header + 2-3 task placeholders |
| `field_settings*` | Show mock settings rows with toggle/chevron icons |
| Tab screens | Show mock feed with 3-4 colored card skeletons in the tab's accent color |

This is what I'd recommend as the most practical improvement. It makes the peek feel native without fighting Compose Navigation's architecture.

---

## 6. Summary

| Question | Answer |
|----------|--------|
| **Can we show the REAL previous screen during the peek?** | **No** — Compose Navigation only composes one destination at a time. |
| **Can we show a bitmap snapshot?** | **Yes** — but ~4MB/screen, static, potentially stale. Implementable with `drawToBitmap()`. |
| **Can we have two overlapping NavHosts?** | **Technically yes** — but architecturally fragile and breaks lifecycle/ViewModel management. |
| **Can we make the mock preview better?** | **Yes** — screen-type-specific mock templates are the highest-value improvement. |
| **What does Google's own code do?** | **Same thing** — mock placeholders. The Play Store shows a blurry app icon and accent color. Settings shows a grey outline with the page title. |
| **Is there a framework fix coming?** | **Not announced** — Android 18+ speculation at earliest. |
| **Should we remove the peek entirely?** | **Could, but the popExitTransition would then be the only feedback.** Users see the current screen slide out and the previous screen slide in — no hint during the gesture. Many users find peek previews helpful for orientation. |

### Bottom Line

The current approach — a generic mock preview — is the same approach used by virtually every Android app with predictive back support. The peek exists to give the user **spatial orientation** ("you're going back to Today/Capture/Settings"), not to show live data. A screen-type-specific mock (section 5c) would make it feel significantly more polished without any of the architectural costs of trying to show the real screen.
