package fieldmind.research.app.features.field.data.canvas

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [FigureMetaEntity].
 *
 * Stores per-figure metadata for the Figure Analysis side panel (Phase 2).
 */
@Dao
interface FigureMetaDao {

    // ── Observe ──

    @Query("SELECT * FROM canvas_figure_meta WHERE blockId = :blockId AND deletedAt IS NULL LIMIT 1")
    fun observeFigureMeta(blockId: Long): Flow<FigureMetaEntity?>

    @Query("SELECT * FROM canvas_figure_meta WHERE id = :id LIMIT 1")
    suspend fun getFigureMeta(id: Long): FigureMetaEntity?

    @Query("SELECT * FROM canvas_figure_meta WHERE blockId IN (SELECT id FROM canvas_blocks WHERE noteId = :noteId) AND deletedAt IS NULL")
    fun observeAllFigureMetaForNote(noteId: Long): Flow<List<FigureMetaEntity>>

    // ── Upsert ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFigureMeta(meta: FigureMetaEntity): Long

    // ── Update ──

    @Query("UPDATE canvas_figure_meta SET interpretation = :interpretation, updatedAt = :now WHERE blockId = :blockId")
    suspend fun updateInterpretation(blockId: Long, interpretation: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_figure_meta SET userNotes = :notes, updatedAt = :now WHERE blockId = :blockId")
    suspend fun updateUserNotes(blockId: Long, notes: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_figure_meta SET relatedIdeas = :ideas, updatedAt = :now WHERE blockId = :blockId")
    suspend fun updateRelatedIdeas(blockId: Long, ideas: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_figure_meta SET questionsGenerated = :questions, updatedAt = :now WHERE blockId = :blockId")
    suspend fun updateQuestionsGenerated(blockId: Long, questions: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_figure_meta SET caption = :caption, updatedAt = :now WHERE blockId = :blockId")
    suspend fun updateCaption(blockId: Long, caption: String, now: Long = System.currentTimeMillis())

    // ── Delete ──

    @Query("UPDATE canvas_figure_meta SET deletedAt = :time, updatedAt = :time WHERE blockId = :blockId")
    suspend fun softDeleteFigureMeta(blockId: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM canvas_figure_meta WHERE blockId = :blockId")
    suspend fun hardDeleteFigureMeta(blockId: Long)

    @Query("DELETE FROM canvas_figure_meta WHERE blockId NOT IN (SELECT id FROM canvas_blocks)")
    suspend fun cleanOrphanedFigureMeta()

    // ── Search ──

    @Query("SELECT * FROM canvas_figure_meta WHERE (interpretation LIKE '%' || :query || '%' OR userNotes LIKE '%' || :query || '%' OR caption LIKE '%' || :query || '%') AND deletedAt IS NULL")
    suspend fun searchFigureMeta(query: String): List<FigureMetaEntity>
}
