package fieldmind.research.app.features.field.data.canvas

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [CanvasBlockEntity].
 *
 * Supports observe (Flow), CRUD, position/size/content updates, z-index reordering,
 * and full-text search within block content.
 */
@Dao
interface CanvasBlockDao {

    // ── Observe ──

    @Query("SELECT * FROM canvas_blocks WHERE noteId = :noteId AND deletedAt IS NULL ORDER BY zIndex ASC")
    fun observeBlocksForNote(noteId: Long): Flow<List<CanvasBlockEntity>>

    @Query("SELECT * FROM canvas_blocks WHERE id = :id LIMIT 1")
    suspend fun getBlock(id: Long): CanvasBlockEntity?

    @Query("SELECT * FROM canvas_blocks WHERE noteId = :noteId AND type = :type AND deletedAt IS NULL ORDER BY zIndex ASC")
    fun observeBlocksByType(noteId: Long, type: String): Flow<List<CanvasBlockEntity>>

    // ── Insert / Upsert ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlock(block: CanvasBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlocks(blocks: List<CanvasBlockEntity>)

    // ── Position / Size / Content ──

    @Query("UPDATE canvas_blocks SET positionX = :x, positionY = :y, updatedAt = :now WHERE id = :id")
    suspend fun updateBlockPosition(id: Long, x: Float, y: Float, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_blocks SET width = :w, height = :h, updatedAt = :now WHERE id = :id")
    suspend fun updateBlockSize(id: Long, w: Float, h: Float, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_blocks SET contentJson = :json, updatedAt = :now WHERE id = :id")
    suspend fun updateBlockContent(id: Long, json: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_blocks SET zIndex = :z WHERE id = :id")
    suspend fun updateBlockZIndex(id: Long, z: Int)

    @Query("UPDATE canvas_blocks SET rotation = :rotation, updatedAt = :now WHERE id = :id")
    suspend fun updateBlockRotation(id: Long, rotation: Float, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_blocks SET opacity = :opacity, updatedAt = :now WHERE id = :id")
    suspend fun updateBlockOpacity(id: Long, opacity: Float, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_blocks SET pinned = :pinned WHERE id = :id")
    suspend fun updateBlockPinned(id: Long, pinned: Boolean)

    // ── Entity linking ──

    @Query("UPDATE canvas_blocks SET linkedEntityType = :entityType, linkedEntityId = :entityId WHERE id = :id")
    suspend fun linkBlockToEntity(id: Long, entityType: String, entityId: Long)

    @Query("SELECT * FROM canvas_blocks WHERE linkedEntityType = :entityType AND linkedEntityId = :entityId AND deletedAt IS NULL")
    suspend fun getBlocksLinkedToEntity(entityType: String, entityId: Long): List<CanvasBlockEntity>

    // ── Delete ──

    @Query("UPDATE canvas_blocks SET deletedAt = :time, updatedAt = :time WHERE id = :id")
    suspend fun softDeleteBlock(id: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM canvas_blocks WHERE id = :id")
    suspend fun hardDeleteBlock(id: Long)

    @Query("UPDATE canvas_blocks SET deletedAt = :time, updatedAt = :time WHERE noteId = :noteId")
    suspend fun softDeleteAllBlocksForNote(noteId: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM canvas_blocks WHERE noteId = :noteId")
    suspend fun hardDeleteAllBlocksForNote(noteId: Long)

    // ── Search ──

    @Query("SELECT * FROM canvas_blocks WHERE noteId = :noteId AND contentJson LIKE '%' || :query || '%' AND deletedAt IS NULL ORDER BY zIndex ASC")
    suspend fun searchBlocks(noteId: Long, query: String): List<CanvasBlockEntity>

    @Query("SELECT * FROM canvas_blocks WHERE contentJson LIKE '%' || :query || '%' AND deletedAt IS NULL LIMIT 50")
    suspend fun searchAllBlocks(query: String): List<CanvasBlockEntity>

    // ── Counts ──

    @Query("SELECT COUNT(*) FROM canvas_blocks WHERE noteId = :noteId AND deletedAt IS NULL")
    suspend fun blockCountForNote(noteId: Long): Int

    @Query("SELECT MAX(zIndex) FROM canvas_blocks WHERE noteId = :noteId AND deletedAt IS NULL")
    suspend fun maxZIndexForNote(noteId: Long): Int?
}
