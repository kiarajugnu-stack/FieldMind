package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Available drawing tools.
 */
enum class DrawingTool(val displayName: String, val icon: String) {
    PEN("Pen", "draw"),
    HIGHLIGHTER("Highlighter", "highlighter"),
    SHAPE("Shape", "category"),
    ERASER("Eraser", "ink_eraser")
}

/**
 * Shape sub-types for the SHAPE tool.
 */
enum class ShapeType(val displayName: String, val icon: String) {
    RECTANGLE("Rectangle", "rectangle"),
    CIRCLE("Circle", "circle"),
    LINE("Line", "straight"),
    ARROW("Arrow", "arrow_forward")
}

/**
 * Eraser mode.
 */
enum class EraserMode(val displayName: String) {
    STROKE("Remove stroke"),     // tap a stroke to erase it entirely
    PRECISION("Erase path")      // drag to erase portions of strokes
}

/**
 * Mutable state holder for all drawing-related tool settings.
 *
 * Designed to be observed by the drawing overlay and toolbar simultaneously.
 * This is a standalone class (not in CanvasState) to keep concerns separated.
 */
class DrawingState(
    initialTool: DrawingTool = DrawingTool.PEN,
    initialColor: Long = 0xFF1C1B19,
    initialWidth: Float = 3f
) {
    /** Currently active drawing tool. */
    var activeTool: DrawingTool by mutableStateOf(initialTool)
        private set

    /** Currently selected color as ARGB long. */
    var color: Long by mutableStateOf(initialColor)
        private set

    /** Stroke width in logical (canvas-space) pixels. */
    var strokeWidth: Float by mutableFloatStateOf(initialWidth)
        private set

    /** Shape sub-type (only relevant when activeTool == SHAPE). */
    var shapeType: ShapeType by mutableStateOf(ShapeType.RECTANGLE)
        private set

    /** Eraser mode. */
    var eraserMode: EraserMode by mutableStateOf(EraserMode.STROKE)
        private set

    /** Whether to show the floating drawing toolbar. */
    var showToolbar: Boolean by mutableStateOf(false)

    // ── Actions ──

    fun setTool(tool: DrawingTool) {
        activeTool = tool
        showToolbar = true
    }

    fun updateColor(newColor: Long) {
        color = newColor
    }

    fun updateStrokeWidth(width: Float) {
        strokeWidth = width.coerceIn(1f, 40f)
    }

    fun updateShapeType(type: ShapeType) {
        shapeType = type
    }

    fun updateEraserMode(mode: EraserMode) {
        eraserMode = mode
    }

    fun toggleToolbar() {
        showToolbar = !showToolbar
    }

    fun hideToolbar() {
        showToolbar = false
    }

    // ── Helpers ──

    /** Get the compose Color from the current color long. */
    val composeColor: Color get() = Color(color)

    /** Whether the tool is an eraser (affects gesture handling). */
    val isEraser: Boolean get() = activeTool == DrawingTool.ERASER

    /** Whether the tool is a shape (renders differently than freehand). */
    val isShape: Boolean get() = activeTool == DrawingTool.SHAPE

    /** Highlighter uses a wider, semi-transparent stroke. */
    val isHighlighter: Boolean get() = activeTool == DrawingTool.HIGHLIGHTER

    /** Get the effective stroke width based on current tool. */
    val effectiveStrokeWidth: Float get() = when {
        isHighlighter -> strokeWidth * 3f
        else -> strokeWidth
    }

    /** Get the effective alpha based on current tool. */
    val effectiveAlpha: Float get() = when {
        isHighlighter -> 0.35f
        else -> 1f
    }

    companion object {
        /** Preset colors for the color picker. */
        val presetColors: List<Long> = listOf(
            0xFF1C1B19L,  // near-black
            0xFF3B3B3BL,  // dark gray
            0xFF8B8B8BL,  // medium gray
            0xFFD0D0D0L,  // light gray
            0xFFE53935L,  // red
            0xFFFF6D00L,  // orange
            0xFFFFC107L,  // amber
            0xFF43A047L,  // green
            0xFF00ACC1L,  // cyan
            0xFF1E88E5L,  // blue
            0xFF3949ABL,  // indigo
            0xFF8E24AAL,  // purple
            0xFFD81B60L,  // pink
            0xFF6D4C41L,  // brown
        )
    }
}
