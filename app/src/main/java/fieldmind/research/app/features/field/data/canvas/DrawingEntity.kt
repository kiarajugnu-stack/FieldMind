package fieldmind.research.app.features.field.data.canvas

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single drawing stroke on the canvas handwriting layer.
 *
 * Stroke data is stored as a compressed JSON array of [Stroke] objects:
 * ```json
 * [{"points":[{"x":100,"y":200,"pressure":0.8,"ts":123456}], "color":4279375641, "width":2.0, "tool":"pen", "opacity":1.0}]
 * ```
 *
 * Each stroke can belong to a specific [CanvasBlockEntity] (when inside a DrawingBlock)
 * or directly to a [NoteEntity] (when on the freeform handwriting layer over the canvas).
 */
@Entity(tableName = "canvas_drawings")
data class DrawingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val blockId: Long? = null,             // nullable — standalone or inside a DrawingBlock
    val strokeDataJson: String = "",       // serialized stroke array
    val toolType: String = "pen",          // pen, highlighter, shape, eraser
    val color: Long = 0xFF1C1B19,         // default near-black
    val strokeWidth: Float = 2f,
    val layerIndex: Int = 0,               // for layering multiple drawing passes
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
