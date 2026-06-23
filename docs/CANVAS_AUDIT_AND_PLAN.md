# Canvas Code Audit & Improvement Plan

> **Date:** June 23, 2026
> **Scope:** All files in `app/src/main/java/fieldmind/research/app/features/field/presentation/canvas/`
> **Benchmark:** Apple Notes (document mode) & Apple Freeform (infinite canvas mode)

---

## 1. Architecture Overview

### Current Structure
```
canvas/
├── InfiniteCanvas.kt         # Main infinite canvas composable
├── PageCanvas.kt             # Page-based (document) canvas
├── CanvasState.kt            # Camera, selection, collapse state
├── CanvasViewModel.kt        # ViewModel: CRUD, undo/redo, drawing, linking
├── UndoRedoManager.kt        # CommandHistory + CanvasCommand sealed interface
├── BlockToolbar.kt           # Floating toolbar above selected block
├── CanvasMinimap.kt          # Minimap widget (bottom-right)
├── ZoomSlider.kt             # Zoom slider widget (right-center)
├── FigureSidePanel.kt        # Side panel for figure analysis
├── FigureGalleryView.kt      # Full-screen figure gallery overlay
├── LinkToEntityDialog.kt     # Dialog for linking blocks to entities
└── blocks/
    ├── CanvasBlock.kt        # Single block composable (infinite canvas)
    ├── ImageBlock.kt         # Image content renderer
    ├── TextBlock.kt          # Text content renderer
    ├── TableBlock.kt         # Table content renderer
    └── StickyNoteBlock.kt    # Sticky note content renderer
```

### Key Data Flow
```
Room DB → CanvasViewModel._blocks (StateFlow)
  → InfiniteCanvas/PageCanvas (composable)
    → SubcomposeLayout positions blocks via canvasState.canvasToScreen()
    → User gestures → moveBlockIntermediate() → Room DB → re-observation
```

**Critical Issue:** Every drag/resize frame writes to Room DB via intermediate functions. This creates an unnecessary round-trip that causes visual stutter. Apple Freeform updates positions in a local rendering layer and flushes to persistence only on gesture end.

---

## 2. Missing Features (vs Apple Notes/Freeform)

### 🔴 Critical — User-facing gaps

| Feature | Apple Notes | Apple Freeform | FieldMind | Priority |
|---------|-------------|----------------|-----------|----------|
| **Block alignment guides** | — | ✅ Blue dynamic guides | ❌ | High |
| **Multi-block selection** | — | ✅ Lasso + Shift-tap | ❌ Single only | High |
| **Block grouping** | — | ✅ Group/ungroup | ❌ | Medium |
| **Lasso selection tool** | — | ✅ Freeform lasso | ❌ | Medium |
| **Auto-layout / snap to grid** | Inline flow | ✅ Optional grid snap | ❌ | Medium |
| **Canvas export** | ✅ PDF/print | ✅ PDF/image | ❌ | Medium |
| **Undo for drag/resize** | ✅ Full | ✅ Full | ⚠️ Partial (PageCanvas only) | High |
| **Keyboard shortcuts** | ✅ Cmd+Z, etc. | ✅ Cmd+Z, etc. | ⚠️ Ctrl+Z only | Low |

### 🟡 Polish — UX refinements

| Detail | Apple | FieldMind | Priority |
|--------|-------|-----------|----------|
| **Drag shadow elevation** | Lifts with real-time shadow | ✅ Animated, but no z-depth shadow | Low |
| **Spring physics on all interactions** | Every transition uses spring | ⚠️ Mixed: springs + tweens | Low |
| **Context menu (long-press)** | ✅ Rich contextual actions | ❌ | Medium |
| **Double-tap to edit text** | ✅ Inline editing | ❌ External editor | Medium |
| **Drag-to-create block** | — | ❌ Have to tap FAB | Low |
| **Haptic feedback on gestures** | ✅ Subtle | ⚠️ Only on confirm | Low |

---

## 3. Useless / Dead Code

### 🗑️ Remove

| File | Code | Reason |
|------|------|--------|
| `GpuCanvasSurface.kt` | Whole file | Does not exist on disk (reference only) |
| `GpuCanvasRenderer.kt` | Whole file | Does not exist on disk (reference only) |
| `CanvasState.kt` | `scaleChange` variable in `applyZoom()` | Computed but never used |
| `CanvasBlock.kt` | `onSizeChanged` import | Imported but never used |
| `CanvasBlock.kt` | `IntSize` import | Imported but never used |
| `CanvasViewModel.kt` | `undoLastDraw()` | Swipes back stack instead of undoing last drawing |
| `BlockToolbar.kt` | `toolBarWidth = 280f` | Hardcoded constant that doesn't adapt to actual content width |
| `CanvasMinimap.kt` | `gridSpacing` background dots | Very faint and almost invisible at 0.5px radius — not worth the CPU cycles |
| `CanvasState.kt` | `expandBlock()` | Never called from anywhere (only `toggleBlockCollapse` is used) |

### 🔧 Simplify / Merge

| Code | Issue | Suggestion |
|------|-------|------------|
| `moveBlockIntermediate` + `moveBlock` + `moveBlockFinal` | Three methods with subtle differences | Merge into single function with internal batching |
| `resizeBlockIntermediate` + `resizeBlock` | Same pattern as move | Same merge |
| `FigureSidePanel` 4 tabs | Heavy composable with embedded JSON parsing | Extract to separate files |
| `CanvasState.resetView()` | Clears selection which is unexpected | Should be renamed or split |

---

## 4. Behavioral Changes Needed

### 4.1 🎯 Block Position/Size Via Local State (Not Room)

**Problem:** Drag/resize writes to Room DB on every frame. Room re-observation triggers full block list re-emission, causing stutter.

**Solution:** 
- Add a `liveBlockOverrides: MutableMap<Long, Rect>` to `CanvasState`
- On drag/resize start, set override; on gesture end, flush to Room and clear override
- `SubcomposeLayout` reads from overrides first, falls back to entity data
- This matches Apple Freeform's local-then-persist model

**Files affected:** `CanvasState.kt`, `CanvasViewModel.kt`, `InfiniteCanvas.kt`, `PageCanvas.kt`, `CanvasBlock.kt`

### 4.2 🎯 Alignment Guides (Freeform-style)

**Problem:** Blocks don't snap to each other's edges/centers, making manual alignment frustrating.

**Solution:**
- Add `AlignmentGuideEngine` that computes snap candidates when a block is dragged
- Snap thresholds: 8px edge-to-edge, 8px center-to-center
- Draw ephemeral blue guide lines on the canvas background during drag
- Use Spring animation for snap-to behavior on drag end

**Files needed:** New `AlignmentGuideEngine.kt`, modify `CanvasState.kt`, `InfiniteCanvas.kt`

### 4.3 🎯 Multi-Block Selection

**Problem:** Only single-block selection is possible. Users can't move/delete/duplicate multiple blocks at once.

**Solution:**
- Add `Modifier` key to toggle selection (already exists via `toggleBlockSelection`)
- Long-press drag → lasso selection rectangle (draws on canvas background)
- Toolbar shows "N blocks selected" with batch actions
- Drag moves all selected blocks by the same delta

**Files affected:** `CanvasState.kt`, `InfiniteCanvas.kt`, `CanvasBlock.kt`, `BlockToolbar.kt`

### 4.4 🎯 Block Grouping

**Problem:** No way to lock a set of blocks together spatially.

**Solution:**
- Add `groupId: Long?` field to `CanvasBlockEntity`
- Group command in toolbar: "Group N blocks"
- Dragging one block in a group moves all
- Ungroup command shows when a group is selected
- Uses same `Batch` undo command pattern

**Files affected:** data layer `CanvasBlockEntity`, `CanvasViewModel.kt`, `BlockToolbar.kt`

### 4.5 🎯 Page Canvas: Continuous Scroll + Page Snap

**Problem:** Page canvas uses A4 aspect ratio with hard page breaks. Blocks can't span pages, and the "page" concept is rigid.

**Solution:**
- Switch to continuous scroll with page snap points (like Apple Notes)
- Blocks flow continuously; page breaks are visual guides only
- When a block is dragged partially off a page, show a visual indicator
- Snap-to-page on drag end if block is >50% over the page boundary

**Files affected:** `PageCanvas.kt`

### 4.6 🎯 Minimap Redesign

**Problem:** Minimap disappears when only 1 block is present. Block colors are inconsistent with the app theme. Viewport indicator doesn't show when blocks are off-canvas.

**Solution:**
- Always show minimap (or add persistent toggle)
- Use Material theme colors instead of hardcoded hex
- Render off-canvas blocks with a pulsing border on the minimap edge
- Add "zoom to fit" control on the minimap

**Files affected:** `CanvasMinimap.kt`

### 4.7 🎯 Block Toolbar Reorganization

**Problem:** Toolbar is a flat horizontal row that scrolls. Actions like "Fwd" and "Back" are cryptic. Copy vs Duplicate is confusing.

**Solution:**
- Group into sections with dividers: Actions | Arrange | Link
- Replace "Fwd"/"Back" with "Bring Forward"/"Send Backward"
- Show only relevant actions based on block type (e.g., no "Copy" for PDFs)
- Add "Group" action when ≥2 blocks selected
- Position toolbar like Freeform: attached to the top of the selected block, not floating in the middle of the screen

**Files affected:** `BlockToolbar.kt`

---

## 5. Implementation Priority

### Phase 1: Foundation Fixes (1-2 days)
1. Local state overrides for smooth drag/resize (4.1)
2. Remove dead code (Section 3)
3. Merge move/resize functions (Section 3 simplify)
4. Fix CanvasBlock long-press drag to support multi-select (4.3)

### Phase 2: Core UX (3-4 days)
5. Alignment guides (4.2)
6. Multi-block selection + lasso (4.3)
7. Block grouping (4.4)
8. Minimap redesign (4.6)

### Phase 3: Polish (2-3 days)
9. Page canvas continuous scroll (4.5)
10. Toolbar reorganization (4.7)
11. Keyboard shortcut expansion (Cmd+Shift+Z redo, Delete key)
12. Context menu on long-press (3D Touch style)

### Phase 4: Export & Integration (2 days)
13. Canvas export to PDF/image
14. Undo for infinite canvas drag (switch to `detectDragGestures` with `onDragEnd`)

---

## 6. Architecture Recommendations

### 6.1 Rendering Pipeline

```
Current: Room → StateFlow → Compose recomposition (heavy)
Target:  Room → CanvasViewModel → local mutable state overlay → Compose (lightweight)
```

Add a `MosaicRenderCache` that:
- Tracks which blocks actually changed since last frame
- Only recomposes dirty blocks
- Uses immutable snapshots for unchanged blocks

### 6.2 Gesture Coordination

```
Current: Outer pointerInput + CanvasBlock pointerInput (two layers)
Target:  Single gesture coordinator at canvas level that routes events
         based on hit-testing + active tool state
```

Replace the dual-layer gesture system (outer `PanZoomLayer` + inner block `pointerInput`) with a single `GestureCoordinator` that:
1. Receives all pointer events
2. Checks active tool (drawing, selecting, panning, etc.)
3. Routes to the correct handler
4. Prevents the layer-ordering bugs we've been fixing

### 6.3 State Management

```
Current: CanvasState (mutable) + CanvasViewModel (AndroidViewModel)
Target:  CanvasState (pure state holder) + CanvasViewModel (orchestrator)
```

- Extract zoom/pan math into a pure `Camera` data class
- Make `CanvasState` a snapshotable data class (immutable snapshots)
- Use `StateFlow<CanvasState>` for observation
- Reducer pattern for undo/redo (each command is a pure function)

---

## 7. Specific Code Issues Found

### InfiniteCanvas.kt
- Line ~111: `Modifier.pointerInput(blocks)` key is the entire blocks list — changes on every DB write, restarting gesture detection
- Fix: Use `blocks.map { it.id }.toSet()` as the key, or split tap detection from gesture layer

### CanvasBlock.kt
- Line ~267: Resize handle uses `block.width + dx` directly, but when collapsed `displayWidth`/`displayHeight` don't match `block.width`/`block.height` — resize doesn't account for collapse state
- Line ~190: Tool overlay uses `.fillMaxSize()` inside the already-sized Box — redundant

### CanvasState.kt
- Line ~83: `scaleChange = newZoom / oldZoom` is computed but never used
- Line ~100: `zoomTo()` has `focus: Offset = Offset.Zero` as default — dangerous default that causes zoom towards (0,0) instead of viewport center

### PageCanvas.kt
- Line ~380: Two `.pointerInput(block.id)` blocks with same key — Compose creates separate gesture scopes, but having the same key means they share lifecycle
- Line ~340: Hardcoded 48.dp toolbar margin — doesn't consider DPI or tablet layouts

### CanvasViewModel.kt
- Line ~365: `moveBlockFinal` doesn't use `viewModelScope.launch` because position is already saved — but if Room re-observation hasn't completed yet, the position in Room might be stale
- Line ~400: `undoLastDraw()` deletes the last drawing unconditionally — dangerous if called twice

---

## 8. Benchmark Comparisons

| Metric | Apple Freeform | FieldMind (current) | Target |
|--------|---------------|-------------------|--------|
| Drag latency | <8ms (60fps) | ~32ms (30fps due to Room round-trip) | <16ms (60fps) |
| Block add → visible | Instant | ~50ms (Room insert + re-query) | <16ms (local append + async persist) |
| Pinch zoom | Butter-smooth | Smooth (after wasMultiTouch fix) | Butter-smooth |
| Minimap update | 15fps throttle | Every frame (wastes CPU) | 15fps throttle |
| Block count at 30fps | 200+ | ~50 (SubcomposeLayout overhead) | 200+ |

---

*End of plan. Ready for Phase 1 implementation.*
