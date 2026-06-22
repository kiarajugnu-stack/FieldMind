package fieldmind.research.app.features.field.data.canvas

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [DrawingEntity].
 *
 * Supports observe (Flow), CRUD, stroke data updates, and batch operations.
 */
@Dao
interface DrawingDao {

    // ── Observe ──

    @Query("SELECT * FROM canvas_drawings WHERE noteId = :noteId AND deletedAt IS NULL ORDER BY layerIndex ASC")
    fun observeDrawingsForNote(noteId: Long): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM canvas_drawings WHERE blockId = :blockId AND deletedAt IS NULL ORDER BY layerIndex ASC")
    fun observeDrawingsForBlock(blockId: Long): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM canvas_drawings WHERE id = :id LIMIT 1")
    suspend fun getDrawing(id: Long): DrawingEntity?

    // ── Insert / Upsert ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDrawing(drawing: DrawingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDrawings(drawings: List<DrawingEntity>)

    // ── Update ──

    @Query("UPDATE canvas_drawings SET strokeDataJson = :json, updatedAt = :now WHERE id = :id")
    suspend fun updateStrokeData(id: Long, json: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_drawings SET color = :color, strokeWidth = :width, updatedAt = :now WHERE id = :id")
    suspend fun updateDrawingStyle(id: Long, color: Long, width: Float, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_drawings SET toolType = :tool, updatedAt = :now WHERE id = :id")
    suspend fun updateDrawingTool(id: Long, tool: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_drawings SET layerIndex = :layerIndex WHERE id = :id")
    suspend fun updateDrawingLayer(id: Long, layerIndex: Int)

    // ── Delete ──

    @Query("UPDATE canvas_drawings SET deletedAt = :time, updatedAt = :time WHERE id = :id")
    suspend fun softDeleteDrawing(id: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM canvas_drawings WHERE id = :id")
    suspend fun hardDeleteDrawing(id: Long)

    @Query("UPDATE canvas_drawings SET deletedAt = :time, updatedAt = :time WHERE noteId = :noteId")
    suspend fun softDeleteAllDrawingsForNote(noteId: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM canvas_drawings WHERE noteId = :noteId")
    suspend fun hardDeleteAllDrawingsForNote(noteId: Long)

    // ── Counts ──

    @Query("SELECT COUNT(*) FROM canvas_drawings WHERE noteId = :noteId AND deletedAt IS NULL")
    suspend fun drawingCountForNote(noteId: Long): Int

    @Query("SELECT COUNT(*) FROM canvas_drawings WHERE blockId = :blockId AND deletedAt IS NULL")
    suspend fun drawingCountForBlock(blockId: Long): Int
}
