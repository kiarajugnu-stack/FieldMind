package fieldmind.research.app.features.field.data.canvas

import kotlinx.coroutines.flow.Flow

/**
 * Repository for the canvas data layer.
 *
 * Coordinates access to [CanvasBlockEntity], [DrawingEntity], and [FigureMetaEntity]
 * across their respective DAOs. Provides a single entry point for the CanvasViewModel
 * and FieldMindViewModel.
 *
 * Design follows the existing [fieldmind.research.app.features.field.data.repository.FieldMindRepository]
 * pattern: lightweight pass-through with no additional business logic.
 */
class CanvasRepository(
    private val blockDao: CanvasBlockDao,
    private val drawingDao: DrawingDao,
    private val figureDao: FigureMetaDao
) {

    // ═══════════════════════════════════════════════════════════════
    //  Canvas Blocks
    // ═══════════════════════════════════════════════════════════════

    fun observeBlocksForNote(noteId: Long): Flow<List<CanvasBlockEntity>> =
        blockDao.observeBlocksForNote(noteId)

    fun observeBlocksByType(noteId: Long, type: String): Flow<List<CanvasBlockEntity>> =
        blockDao.observeBlocksByType(noteId, type)

    suspend fun getBlock(id: Long): CanvasBlockEntity? = blockDao.getBlock(id)

    suspend fun upsertBlock(block: CanvasBlockEntity): Long = blockDao.upsertBlock(block)

    suspend fun upsertBlocks(blocks: List<CanvasBlockEntity>) = blockDao.upsertBlocks(blocks)

    suspend fun updateBlockPosition(id: Long, x: Float, y: Float) =
        blockDao.updateBlockPosition(id, x, y)

    suspend fun updateBlockSize(id: Long, w: Float, h: Float) =
        blockDao.updateBlockSize(id, w, h)

    suspend fun updateBlockContent(id: Long, json: String) =
        blockDao.updateBlockContent(id, json)

    suspend fun updateBlockZIndex(id: Long, z: Int) = blockDao.updateBlockZIndex(id, z)

    suspend fun updateBlockRotation(id: Long, rotation: Float) =
        blockDao.updateBlockRotation(id, rotation)

    suspend fun updateBlockOpacity(id: Long, opacity: Float) =
        blockDao.updateBlockOpacity(id, opacity)

    suspend fun updateBlockPinned(id: Long, pinned: Boolean) =
        blockDao.updateBlockPinned(id, pinned)

    suspend fun linkBlockToEntity(id: Long, entityType: String, entityId: Long) =
        blockDao.linkBlockToEntity(id, entityType, entityId)

    suspend fun getBlocksLinkedToEntity(entityType: String, entityId: Long): List<CanvasBlockEntity> =
        blockDao.getBlocksLinkedToEntity(entityType, entityId)

    suspend fun softDeleteBlock(id: Long) = blockDao.softDeleteBlock(id)

    suspend fun hardDeleteBlock(id: Long) = blockDao.hardDeleteBlock(id)

    suspend fun softDeleteAllBlocksForNote(noteId: Long) =
        blockDao.softDeleteAllBlocksForNote(noteId)

    suspend fun hardDeleteAllBlocksForNote(noteId: Long) =
        blockDao.hardDeleteAllBlocksForNote(noteId)

    suspend fun searchBlocks(noteId: Long, query: String): List<CanvasBlockEntity> =
        blockDao.searchBlocks(noteId, query)

    suspend fun searchAllBlocks(query: String): List<CanvasBlockEntity> =
        blockDao.searchAllBlocks(query)

    suspend fun blockCountForNote(noteId: Long): Int = blockDao.blockCountForNote(noteId)

    suspend fun maxZIndexForNote(noteId: Long): Int? = blockDao.maxZIndexForNote(noteId)

    // ═══════════════════════════════════════════════════════════════
    //  Drawings
    // ═══════════════════════════════════════════════════════════════

    fun observeDrawingsForNote(noteId: Long): Flow<List<DrawingEntity>> =
        drawingDao.observeDrawingsForNote(noteId)

    fun observeDrawingsForBlock(blockId: Long): Flow<List<DrawingEntity>> =
        drawingDao.observeDrawingsForBlock(blockId)

    suspend fun getDrawing(id: Long): DrawingEntity? = drawingDao.getDrawing(id)

    suspend fun upsertDrawing(drawing: DrawingEntity): Long = drawingDao.upsertDrawing(drawing)

    suspend fun upsertDrawings(drawings: List<DrawingEntity>) = drawingDao.upsertDrawings(drawings)

    suspend fun updateStrokeData(id: Long, json: String) = drawingDao.updateStrokeData(id, json)

    suspend fun updateDrawingStyle(id: Long, color: Long, width: Float) =
        drawingDao.updateDrawingStyle(id, color, width)

    suspend fun updateDrawingTool(id: Long, tool: String) = drawingDao.updateDrawingTool(id, tool)

    suspend fun updateDrawingLayer(id: Long, layerIndex: Int) =
        drawingDao.updateDrawingLayer(id, layerIndex)

    suspend fun softDeleteDrawing(id: Long) = drawingDao.softDeleteDrawing(id)

    suspend fun hardDeleteDrawing(id: Long) = drawingDao.hardDeleteDrawing(id)

    suspend fun softDeleteAllDrawingsForNote(noteId: Long) =
        drawingDao.softDeleteAllDrawingsForNote(noteId)

    suspend fun hardDeleteAllDrawingsForNote(noteId: Long) =
        drawingDao.hardDeleteAllDrawingsForNote(noteId)

    suspend fun drawingCountForNote(noteId: Long): Int = drawingDao.drawingCountForNote(noteId)

    suspend fun drawingCountForBlock(blockId: Long): Int = drawingDao.drawingCountForBlock(blockId)

    // ═══════════════════════════════════════════════════════════════
    //  Figure Metadata
    // ═══════════════════════════════════════════════════════════════

    fun observeFigureMeta(blockId: Long): Flow<FigureMetaEntity?> =
        figureDao.observeFigureMeta(blockId)

    fun observeAllFigureMetaForNote(noteId: Long): Flow<List<FigureMetaEntity>> =
        figureDao.observeAllFigureMetaForNote(noteId)

    suspend fun getFigureMeta(id: Long): FigureMetaEntity? = figureDao.getFigureMeta(id)

    suspend fun upsertFigureMeta(meta: FigureMetaEntity): Long = figureDao.upsertFigureMeta(meta)

    suspend fun updateInterpretation(blockId: Long, interpretation: String) =
        figureDao.updateInterpretation(blockId, interpretation)

    suspend fun updateUserNotes(blockId: Long, notes: String) =
        figureDao.updateUserNotes(blockId, notes)

    suspend fun updateRelatedIdeas(blockId: Long, ideas: String) =
        figureDao.updateRelatedIdeas(blockId, ideas)

    suspend fun updateQuestionsGenerated(blockId: Long, questions: String) =
        figureDao.updateQuestionsGenerated(blockId, questions)

    suspend fun updateCaption(blockId: Long, caption: String) = figureDao.updateCaption(blockId, caption)

    suspend fun softDeleteFigureMeta(blockId: Long) = figureDao.softDeleteFigureMeta(blockId)

    suspend fun hardDeleteFigureMeta(blockId: Long) = figureDao.hardDeleteFigureMeta(blockId)

    suspend fun cleanOrphanedFigureMeta() = figureDao.cleanOrphanedFigureMeta()

    suspend fun searchFigureMeta(query: String): List<FigureMetaEntity> =
        figureDao.searchFigureMeta(query)

    // ═══════════════════════════════════════════════════════════════
    //  Batch operations (delete everything for a note)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Soft-deletes all canvas data for a given note.
     * Called when a note is permanently deleted or archived.
     */
    suspend fun deleteAllCanvasDataForNote(noteId: Long) {
        blockDao.softDeleteAllBlocksForNote(noteId)
        drawingDao.softDeleteAllDrawingsForNote(noteId)
    }

    /**
     * Hard-deletes all canvas data for a given note.
     * Called when a note is permanently removed.
     */
    suspend fun hardDeleteAllCanvasDataForNote(noteId: Long) {
        blockDao.hardDeleteAllBlocksForNote(noteId)
        drawingDao.hardDeleteAllDrawingsForNote(noteId)
    }

    /**
     * Safe next z-index for placing a new block on top of existing ones.
     */
    suspend fun nextZIndexForNote(noteId: Long): Int =
        (blockDao.maxZIndexForNote(noteId) ?: -1) + 1
}
