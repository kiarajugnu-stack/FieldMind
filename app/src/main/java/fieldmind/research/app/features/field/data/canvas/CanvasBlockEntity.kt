package fieldmind.research.app.features.field.data.canvas

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single block placed on the infinite canvas.
 *
 * Each block has a type (text, image, pdf, figure, table, sticky, drawing, voice, equation),
 * a position/size on the canvas, content serialized as JSON, and optional links to existing
 * FieldMind entities (observations, questions, hypotheses, sources, data records, reports).
 */
@Entity(tableName = "canvas_blocks")
data class CanvasBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val type: String = "text",            // text, image, pdf, figure, table, sticky, drawing, voice, equation
    val contentJson: String = "",         // type-specific serialized content (Markdown text, image URI, LaTeX, etc.)
    val positionX: Float = 0f,           // canvas X coordinate in logical px
    val positionY: Float = 0f,           // canvas Y coordinate in logical px
    val width: Float = 300f,             // block width in logical px
    val height: Float = 200f,            // block height in logical px
    val zIndex: Int = 0,                 // stacking order (higher = on top)
    val rotation: Float = 0f,            // rotation in degrees
    val opacity: Float = 1f,             // 0.0 – 1.0
    val linkedEntityType: String = "",   // "observation", "question", "hypothesis", "source", "data", "report"
    val linkedEntityId: Long? = null,    // FK to the linked entity
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
