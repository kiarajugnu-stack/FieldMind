package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

enum class CanvasMode {
    INFINITE,  // Infinite scrollable canvas
    PAGES      // Page-by-page (like document)
}

/**
 * Mutable state holder for the infinite canvas.
 *
 * Manages:
 * - Camera zoom level (clamped 0.1x – 5x)
 * - Camera pan offset (the logical origin at the canvas center)
 * - Selection state (set of selected block IDs)
 *
 * Designed to be observed by both the GL rendering layer
 * and the Compose overlay layer simultaneously.
 */
class CanvasState(
    initialZoom: Float = 1f,
    initialPanX: Float = 0f,
    initialPanY: Float = 0f
) {
    /** Current zoom level. 1.0 = 100%. Clamped [minZoom, maxZoom]. */
    var zoom: Float by mutableFloatStateOf(initialZoom.coerceIn(minZoom, maxZoom))
        private set

    /** Canvas origin X offset in logical px. Positive = content moves right. */
    var panX: Float by mutableFloatStateOf(initialPanX)
        private set

    /** Canvas origin Y offset in logical px. Positive = content moves down. */
    var panY: Float by mutableFloatStateOf(initialPanY)
        private set

    /** The set of currently selected block IDs. */
    var selectedBlockIds: Set<Long> by mutableStateOf(emptySet())
        private set

    /** The most recently tapped block ID (for context menus). */
    var lastTappedBlockId: Long? by mutableStateOf(null)
        private set

    /** Set of collapsed block IDs (minimized to small preview cards). */
    var collapsedBlockIds: Set<Long> by mutableStateOf(emptySet())
        private set

    /** Whether to show the dot grid background. */
    var showGrid: Boolean by mutableStateOf(true)
        private set

    /** Canvas mode: infinite or page-by-page. */
    var canvasMode: CanvasMode by mutableStateOf(CanvasMode.INFINITE)
        private set

    // ── Clamp values ──

    companion object {
        const val minZoom = 0.1f
        const val maxZoom = 5.0f
    }

    // ── Zoom ──

    /**
     * Apply a zoom gesture centered at [focus] (screen coordinates).
     * Each gesture event provides a [zoomDelta] (e.g. 1.1 = zoom in 10%).
     * Keeps the point under the finger stationary by adjusting pan.
     */
    fun applyZoom(zoomDelta: Float, focus: Offset) {
        val oldZoom = zoom
        val newZoom = (oldZoom * zoomDelta).coerceIn(minZoom, maxZoom)
        if (newZoom == oldZoom) return

        // Zoom towards the focus point: keep that coordinate under the finger
        val focusX = focus.x
        val focusY = focus.y
        val scaleChange = newZoom / oldZoom

        // Convert focus from screen coords to canvas coords BEFORE zoom
        val canvasFocusX = (focusX - panX) / oldZoom
        val canvasFocusY = (focusY - panY) / oldZoom

        zoom = newZoom

        // Adjust pan so focus stays at the same canvas point under the finger
        panX = focusX - canvasFocusX * newZoom
        panY = focusY - canvasFocusY * newZoom
    }

    /**
     * Zoom to a specific level, keeping [focus] (screen coordinates) stationary.
     * If [focus] is not provided, defaults to the viewport center (caller should
     * always pass [focus] for correct behavior — see [ZoomSlider]).
     */
    fun zoomTo(newZoom: Float, focus: Offset = Offset.Zero) {
        val clampedZoom = newZoom.coerceIn(minZoom, maxZoom)
        if (clampedZoom == zoom) return

        // Convert focus point to canvas coords BEFORE zoom
        val canvasFocusX = (focus.x - panX) / zoom
        val canvasFocusY = (focus.y - panY) / zoom

        zoom = clampedZoom

        // Adjust pan so focus stays at the same canvas point
        panX = focus.x - canvasFocusX * zoom
        panY = focus.y - canvasFocusY * zoom
    }

    // ── Pan ──

    /** Apply a delta pan (screen-space drag pixels). */
    fun applyPan(deltaX: Float, deltaY: Float) {
        panX += deltaX
        panY += deltaY
    }

    /** Set absolute pan position. */
    fun setPan(x: Float, y: Float) {
        panX = x
        panY = y
    }

    /** Reset camera to origin. */
    fun resetView() {
        zoom = 1f
        panX = 0f
        panY = 0f
        selectedBlockIds = emptySet()
    }

    // ── Selection ──

    /** Select a single block, deselecting others. */
    fun selectBlock(id: Long) {
        selectedBlockIds = setOf(id)
        lastTappedBlockId = id
    }

    /** Toggle a block in multi-select. */
    fun toggleBlockSelection(id: Long) {
        selectedBlockIds = if (id in selectedBlockIds) {
            selectedBlockIds - id
        } else {
            selectedBlockIds + id
        }
        lastTappedBlockId = id
    }

    /** Clear all selections. */
    fun clearSelection() {
        selectedBlockIds = emptySet()
        lastTappedBlockId = null
    }

    /** Toggle collapse state of a block. */
    fun toggleBlockCollapse(id: Long) {
        collapsedBlockIds = if (id in collapsedBlockIds) {
            collapsedBlockIds - id
        } else {
            collapsedBlockIds + id
        }
    }

    /** Expand a collapsed block. */
    fun expandBlock(id: Long) {
        collapsedBlockIds = collapsedBlockIds - id
    }

    /** Toggle the dot grid visibility. */
    fun toggleGrid() {
        showGrid = !showGrid
    }

    /** Toggle between infinite and page-by-page modes. */
    fun toggleCanvasMode() {
        canvasMode = if (canvasMode == CanvasMode.INFINITE) CanvasMode.PAGES else CanvasMode.INFINITE
    }

    // ── Coordinate transforms ──

    /** Convert screen coordinates to canvas (logical) coordinates. */
    fun screenToCanvas(screenX: Float, screenY: Float): Offset {
        return Offset(
            (screenX - panX) / zoom,
            (screenY - panY) / zoom
        )
    }

    /** Convert canvas (logical) coordinates to screen coordinates. */
    fun canvasToScreen(canvasX: Float, canvasY: Float): Offset {
        return Offset(
            canvasX * zoom + panX,
            canvasY * zoom + panY
        )
    }
}
