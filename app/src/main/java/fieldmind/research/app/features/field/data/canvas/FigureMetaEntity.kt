package fieldmind.research.app.features.field.data.canvas

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata for a figure/image block on the canvas.
 *
 * Stores AI-generated interpretations, user notes, linked entity references,
 * and generated questions — enabling the Figure Analysis side panel (Phase 2).
 */
@Entity(tableName = "canvas_figure_meta")
data class FigureMetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockId: Long,                     // FK → CanvasBlockEntity.id
    val sourceFilename: String = "",
    val caption: String = "",
    val figureNumber: Int = 0,
    val pageNumber: Int? = null,            // page number if extracted from a PDF/paper
    val interpretation: String = "",        // AI-generated or user-written analysis
    val userNotes: String = "",             // free-text notes about this figure
    val relatedIdeas: String = "",          // JSON array of linked entity IDs
    val questionsGenerated: String = "",    // JSON array of {question, category}
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
