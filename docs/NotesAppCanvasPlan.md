# FieldMind Notes App & Infinite Canvas — Implementation Plan

> **Status:** Planning phase. Ready for implementation.
> **Builds on:** existing `NoteEntity`, `ProjectEntity`, `ReportEntity` foundations.
> **Canvas engine:** NDK/OpenGL (Skija or raw OpenGL) via `AndroidSurfaceView`/`TextureView`.
> **Text editor:** Custom Markdown rich text composable.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                       UI Layer (Compose)                         │
│  ┌─────────┐  ┌──────────────┐  ┌──────────┐  ┌─────────────┐  │
│  │NotesList│  │ NoteCanvas   │  │  Figure   │  │  Paper      │  │
│  │  Screen │  │   Screen     │  │ SidePanel │  │  Export     │  │
│  └─────────┘  └──────┬───────┘  └──────────┘  └─────────────┘  │
│                      │                                           │
│        ┌─────────────┼─────────────┬─────────────────┐          │
│  ┌─────▼─────┐ ┌────▼────┐ ┌──────▼──────┐ ┌───────▼───────┐  │
│  │ Infinite  │ │Drawing  │ │    Block    │ │  Split Pane   │   │
│  │ Canvas    │ │ Layer   │ │Components   │ │  Manager      │   │
│  │(NDK/GL)   │ │(Handwr.)│ │(Text, Img… )│ │               │   │
│  └───────────┘ └─────────┘ └─────────────┘ └───────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                   ViewModel Layer                                │
│  ┌─────────────────────────┐  ┌──────────────────────────────┐  │
│  │   FieldMindViewModel    │  │    CanvasViewModel            │  │
│  │  (existing + new methods)│  │  (canvas state, undo/redo)   │  │
│  └──────────┬──────────────┘  └──────────────┬───────────────┘  │
├──────────────┼────────────────────────────────┼──────────────────┤
│              ▼                                ▼                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Data Layer (Room DB)                    │  │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────┐   │  │
│  │  │ NoteEntity     │  │ CanvasBlockEnt.│  │DrawingEnt. │   │  │
│  │  │ (existing)     │  │ (new)          │  │ (new)      │   │  │
│  │  └────────────────┘  └────────────────┘  └────────────┘   │  │
│  │  ┌────────────────┐  ┌────────────────┐                    │  │
│  │  │ProjectEntity   │  │ FigureMetaEnt. │                    │  │
│  │  │ (existing)     │  │ (new)          │                    │  │
│  │  └────────────────┘  └────────────────┘                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase 0: Foundation — Data Layer Extensions

**Goal:** Extend the Room database with new entities for canvas blocks, drawings, and figure metadata. Add Room migration from current schema → v4.

### 0.1 — CanvasBlockEntity (Room Entity)

```kotlin
@Entity(tableName = "canvas_blocks")
data class CanvasBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,                          // FK → NoteEntity.id
    val type: String,                          // text, image, pdf, figure, table, sticky, drawing, voice, equation
    val contentJson: String = "",              // type-specific serialized content
    val positionX: Float = 0f,                 // canvas X coord (px)
    val positionY: Float = 0f,                 // canvas Y coord (px)
    val width: Float = 300f,                   // block width
    val height: Float = 200f,                  // block height
    val zIndex: Int = 0,                       // stacking order
    val rotation: Float = 0f,                  // degrees
    val opacity: Float = 1f,
    val linkedEntityType: String = "",          // "observation", "question", "hypothesis", "source", "data", "report"
    val linkedEntityId: Long? = null,           // FK to linked entity
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 0.2 — DrawingEntity (Room Entity)

```kotlin
@Entity(tableName = "canvas_drawings")
data class DrawingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val blockId: Long?,                        // nullable — drawing can be standalone or inside a block
    val strokeDataJson: String = "",           // [{points:[{x,y,pressure,ts}],color,width,tool,opacity}]
    val toolType: String = "pen",              // pen, highlighter, shape, eraser
    val color: Long = 0xFF1C1B19,             // ARGB
    val strokeWidth: Float = 2f,
    val layerIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 0.3 — FigureMetaEntity (Room Entity)

```kotlin
@Entity(tableName = "canvas_figure_meta")
data class FigureMetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockId: Long,
    val sourceFilename: String = "",
    val caption: String = "",
    val figureNumber: Int = 0,
    val pageNumber: Int? = null,
    val interpretation: String = "",           // AI-generated or user-written
    val userNotes: String = "",
    val relatedIdeas: String = "",             // JSON array of linked entity IDs
    val questionsGenerated: String = "",       // JSON array of {question, category}
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 0.4 — CanvasBlockDao

```kotlin
@Dao
interface CanvasBlockDao {
    @Query("SELECT * FROM canvas_blocks WHERE noteId = :noteId ORDER BY zIndex ASC")
    fun observeBlocksForNote(noteId: Long): Flow<List<CanvasBlockEntity>>

    @Query("SELECT * FROM canvas_blocks WHERE id = :id")
    suspend fun getBlock(id: Long): CanvasBlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlock(block: CanvasBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlocks(blocks: List<CanvasBlockEntity>)

    @Query("UPDATE canvas_blocks SET positionX = :x, positionY = :y, updatedAt = :now WHERE id = :id")
    suspend fun updateBlockPosition(id: Long, x: Float, y: Float, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_blocks SET width = :w, height = :h, updatedAt = :now WHERE id = :id")
    suspend fun updateBlockSize(id: Long, w: Float, h: Float, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_blocks SET contentJson = :json, updatedAt = :now WHERE id = :id")
    suspend fun updateBlockContent(id: Long, json: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE canvas_blocks SET zIndex = :z WHERE id = :id")
    suspend fun updateBlockZIndex(id: Long, z: Int)

    @Query("DELETE FROM canvas_blocks WHERE id = :id")
    suspend fun deleteBlock(id: Long)

    @Query("DELETE FROM canvas_blocks WHERE noteId = :noteId")
    suspend fun deleteAllBlocksForNote(noteId: Long)

    // Search
    @Query("SELECT * FROM canvas_blocks WHERE noteId = :noteId AND contentJson LIKE '%' || :query || '%'")
    suspend fun searchBlocks(noteId: Long, query: String): List<CanvasBlockEntity>
}
```

### 0.5 — DrawingDao

```kotlin
@Dao
interface DrawingDao {
    @Query("SELECT * FROM canvas_drawings WHERE noteId = :noteId ORDER BY layerIndex ASC")
    fun observeDrawingsForNote(noteId: Long): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM canvas_drawings WHERE blockId = :blockId ORDER BY layerIndex ASC")
    fun observeDrawingsForBlock(blockId: Long): Flow<List<DrawingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDrawing(drawing: DrawingEntity): Long

    @Query("UPDATE canvas_drawings SET strokeDataJson = :json, updatedAt = :now WHERE id = :id")
    suspend fun updateStrokeData(id: Long, json: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM canvas_drawings WHERE id = :id")
    suspend fun deleteDrawing(id: Long)

    @Query("DELETE FROM canvas_drawings WHERE noteId = :noteId")
    suspend fun deleteAllDrawingsForNote(noteId: Long)
}
```

### 0.6 — FigureMetaDao

```kotlin
@Dao
interface FigureMetaDao {
    @Query("SELECT * FROM canvas_figure_meta WHERE blockId = :blockId")
    fun observeFigureMeta(blockId: Long): Flow<FigureMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFigureMeta(meta: FigureMetaEntity): Long

    @Query("DELETE FROM canvas_figure_meta WHERE blockId = :blockId")
    suspend fun deleteFigureMeta(blockId: Long)
}
```

### 0.7 — Extended NoteEntity

Add these fields to the existing `NoteEntity`:

```kotlin
// New fields on NoteEntity:
val canvasVersion: Int = 1,                    // schema version for canvas data
val color: Long? = null,                        // note color accent
val iconName: String = "",                       // Material Symbol icon name
val isPinned: Boolean = false,
val parentNoteId: Long? = null,                  // for note nesting
val linkedProjectId: Long? = null,               // quick link to project
val isTemplate: Boolean = false,
val viewMode: String = "canvas",                 // "canvas" or "list" (blocks as list)
val canvasZoomLevel: Float = 1f,                 // saved zoom state
val canvasPanX: Float = 0f,                      // saved pan state
val canvasPanY: Float = 0f
```

### 0.8 — Database Migration v3 → v4

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS canvas_blocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                noteId INTEGER NOT NULL,
                type TEXT NOT NULL DEFAULT 'text',
                contentJson TEXT NOT NULL DEFAULT '',
                positionX REAL NOT NULL DEFAULT 0.0,
                positionY REAL NOT NULL DEFAULT 0.0,
                width REAL NOT NULL DEFAULT 300.0,
                height REAL NOT NULL DEFAULT 200.0,
                zIndex INTEGER NOT NULL DEFAULT 0,
                rotation REAL NOT NULL DEFAULT 0.0,
                opacity REAL NOT NULL DEFAULT 1.0,
                linkedEntityType TEXT NOT NULL DEFAULT '',
                linkedEntityId INTEGER,
                pinned INTEGER NOT NULL DEFAULT 0,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS canvas_drawings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                noteId INTEGER NOT NULL,
                blockId INTEGER,
                strokeDataJson TEXT NOT NULL DEFAULT '',
                toolType TEXT NOT NULL DEFAULT 'pen',
                color INTEGER NOT NULL DEFAULT 4279375641,
                strokeWidth REAL NOT NULL DEFAULT 2.0,
                layerIndex INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS canvas_figure_meta (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                blockId INTEGER NOT NULL,
                sourceFilename TEXT NOT NULL DEFAULT '',
                caption TEXT NOT NULL DEFAULT '',
                figureNumber INTEGER NOT NULL DEFAULT 0,
                pageNumber INTEGER,
                interpretation TEXT NOT NULL DEFAULT '',
                userNotes TEXT NOT NULL DEFAULT '',
                relatedIdeas TEXT NOT NULL DEFAULT '',
                questionsGenerated TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        // Add new columns to field_notes
        db.execSQL("ALTER TABLE field_notes ADD COLUMN canvasVersion INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN color INTEGER")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN iconName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN parentNoteId INTEGER")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN linkedProjectId INTEGER")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN viewMode TEXT NOT NULL DEFAULT 'canvas'")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN canvasZoomLevel REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN canvasPanX REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE field_notes ADD COLUMN canvasPanY REAL NOT NULL DEFAULT 0.0")
    }
}
```

### 0.9 — CanvasRepository

```kotlin
class CanvasRepository(private val blockDao: CanvasBlockDao, private val drawingDao: DrawingDao, private val figureDao: FigureMetaDao) {
    // Block methods
    fun observeBlocksForNote(noteId: Long) = blockDao.observeBlocksForNote(noteId)
    suspend fun upsertBlock(block: CanvasBlockEntity): Long = blockDao.upsertBlock(block)
    suspend fun updateBlockPosition(id: Long, x: Float, y: Float) = blockDao.updateBlockPosition(id, x, y)
    suspend fun updateBlockSize(id: Long, w: Float, h: Float) = blockDao.updateBlockSize(id, w, h)
    suspend fun updateBlockContent(id: Long, json: String) = blockDao.updateBlockContent(id, json)
    suspend fun deleteBlock(id: Long) = blockDao.deleteBlock(id)
    suspend fun reorderBlocks(noteId: Long, blockIds: List<Long>) = viewModelScope.launch {
        blockIds.forEachIndexed { index, id -> blockDao.updateBlockZIndex(id, index) }
    }

    // Drawing methods
    fun observeDrawingsForNote(noteId: Long) = drawingDao.observeDrawingsForNote(noteId)
    fun observeDrawingsForBlock(blockId: Long) = drawingDao.observeDrawingsForBlock(blockId)
    suspend fun upsertDrawing(drawing: DrawingEntity): Long = drawingDao.upsertDrawing(drawing)
    suspend fun deleteDrawing(id: Long) = drawingDao.deleteDrawing(id)

    // Figure meta methods
    fun observeFigureMeta(blockId: Long) = figureDao.observeFigureMeta(blockId)
    suspend fun upsertFigureMeta(meta: FigureMetaEntity): Long = figureDao.upsertFigureMeta(meta)

    // Batch operations
    suspend fun deleteAllForNote(noteId: Long) {
        blockDao.deleteAllBlocksForNote(noteId)
        drawingDao.deleteAllDrawingsForNote(noteId)
    }
}
```

---

## Phase 1: Core Canvas — Infinite Canvas Engine

**Goal:** A performant, pannable, zoomable infinite canvas using NDK/OpenGL rendering with Compose overlay for interactive UI elements.

### 1.1 — GPU Canvas Surface

| Task | Details |
|---|---|
| **1.1.1** | Create `GpuCanvasSurface.kt` — wraps `TextureView` or `SurfaceView` with OpenGL ES 3.0 context |
| **1.1.2** | Implement viewport management: pan (`detectDragGestures`), zoom (`detectTransformGestures`), clamp 0.1x–5x |
| **1.1.3** | Render dot-grid background on GPU: 40px spacing dots, subtle (alpha 0.08), fades out at high zoom |
| **1.1.4** | Viewport culling: only render blocks + grid tiles visible in current viewport |
| **1.1.5** | Compose overlay layer: `SubcomposeLayout` on top of GPU surface for text inputs, buttons, selection handles |
| **1.1.6** | Coordinate mapping: canvas ↔ screen coordinate conversion for drag/resize operations |

### 1.2 — CanvasBlock Composable

| Task | Details |
|---|---|
| **1.2.1** | Base block wrapper: position + size via `Modifier.offset` and `Modifier.size`, rendered in Compose overlay |
| **1.2.2** | Selection state: tap to select, blue border + resize handles. Multi-select with Ctrl+click / lasso |
| **1.2.3** | Drag to reposition: `pointerInput` + `detectDragGestures`, update position in real-time, save on drag end |
| **1.2.4** | Resize handles: bottom-right corner drag handle, maintain aspect ratio for images |
| **1.2.5** | Context menu: long-press shows floating menu — Delete, Duplicate, Copy, Link to Entity, Move Forward/Back |
| **1.2.6** | Block selection toolbar: floating bar below selection with actions relevant to block type |

### 1.3 — Content Block Types

#### 1.3.1 — TextBlock

| Task | Details |
|---|---|
| **1.3.1a** | Custom Markdown text editor using `BasicTextField` with custom `VisualTransformation` for inline styling |
| **1.3.1b** | Formatting toolbar: **Bold**, *Italic*, ~~Strikethrough~~, `Code`, Headings (H1–H3), Bullet List, Numbered List, Link |
| **1.3.1c** | Markdown rendering: parse selected text and apply `SpanStyle` for formatted display |
| **1.3.1d** | Auto-expand block height as text grows. Min 60px, max 600px with scroll |
| **1.3.1e** | `/` command menu: type `/` to trigger quick-insert menu (Table, Image, Figure, Equation, etc.) |

#### 1.3.2 — ImageBlock

| Task | Details |
|---|---|
| **1.3.2a** | Image picker integration (gallery/camera via existing `FieldMindCameraV2`) |
| **1.3.2b** | Display using `Coil AsyncImage` with loading spinner and error state |
| **1.3.2c** | Caption overlay at bottom: editable text |
| **1.3.2d** | Double-tap for full-screen image viewer |
| **1.3.2e** | Alt text accessibility field |

#### 1.3.3 — PDFBlock

| Task | Details |
|---|---|
| **1.3.3a** | File picker for PDF selection |
| **1.3.3b** | Thumbnail preview (first page) |
| **1.3.3c** | Tap opens existing `FieldMindPdfViewer` composable |
| **1.3.3d** | Page range selector: "Show pages X–Y in canvas" (renders as image strip) |

#### 1.3.4 — FigureBlock

| Task | Details |
|---|---|
| **1.3.4a** | Same as ImageBlock but with figure annotation layer (Phase 2) |
| **1.3.4b** | Figure number badge top-left: "Fig. 1" |
| **1.3.4c** | Tap → opens `FigureSidePanel` (Phase 2) |
| **1.3.4d** | Annotation layer: highlight regions, add arrows, text callouts on the figure |

#### 1.3.5 — TableBlock

| Task | Details |
|---|---|
| **1.3.5a** | Simple editable grid: `LazyColumn` of `LazyRow` with `BasicTextField` cells |
| **1.3.5b** | Header row styling (bold, background) |
| **1.3.5c** | Add/remove row on long-press → context menu. Add/remove column from toolbar |
| **1.3.5d** | Cell merging — long-press two selected cells → "Merge" |
| **1.3.5e** | Export as Markdown table |

#### 1.3.6 — StickyNoteBlock

| Task | Details |
|---|---|
| **1.3.6a** | Yellow card with drop shadow, slightly rotated (random -3° to +3° on creation) |
| **1.3.6b** | Editable text content, auto-grows with text |
| **1.3.6c** | Color picker: yellow, green, pink, blue, orange, purple |
| **1.3.6d** | Rotation handle: small circle at top, drag to rotate freely |

#### 1.3.7 — DrawingBlock

| Task | Details |
|---|---|
| **1.3.7a** | Embedded drawing canvas inside a block (wraps the handwriting layer from Phase 3) |
| **1.3.7b** | Toolbar appears inside block: pen, highlighter, shapes, eraser, lasso |
| **1.3.7c** | Drawing layer renders on GPU surface, text UI on Compose overlay |

#### 1.3.8 — VoiceNoteBlock

| Task | Details |
|---|---|
| **1.3.8a** | Audio record button → uses existing media recorder |
| **1.3.8b** | Audio player card with waveform visualization |
| **1.3.8c** | Transcription display (using existing voice-to-text or ML Kit) |
| **1.3.8d** | Play/pause/progress bar using existing `FieldMindAudioPlayer` |

#### 1.3.9 — EquationBlock

| Task | Details |
|---|---|
| **1.3.9a** | LaTeX/MathJax input field |
| **1.3.9b** | Render equation as bitmap (using `jlatexmath` or WebView with MathJax) |
| **1.3.9c** | Common equation templates: integrals, sums, fractions, matrices, Greek letters |

### 1.4 — Block Toolbar

| Task | Details |
|---|---|
| **1.4.1** | Floating toolbar appears on block tap (single selection) or long-press |
| **1.4.2** | Actions: Delete, Duplicate, Move Forward, Move Backward, Copy, Link to Entity, Color (sticky only) |
| **1.4.3** | Toolbar position: above the selected block, or below if near screen top |
| **1.4.4** | Multi-select toolbar: Group, Align (left/center/right), Distribute vertically/horizontally |

### 1.5 — Canvas Minimap

| Task | Details |
|---|---|
| **1.5.1** | Small (120x80dp) floating widget in bottom-right corner |
| **1.5.2** | Renders all blocks as colored rectangles scaled to fit |
| **1.5.3** | Red rectangle shows current viewport position |
| **1.5.4** | Tap/drag on minimap pans the main canvas to that position |
| **1.5.5** | Toggle visibility from canvas toolbar |

### 1.6 — Undo/Redo System

| Task | Details |
|---|---|
| **1.6.1** | Command interface: `interface CanvasCommand { fun execute(); fun undo() }` |
| **1.6.2** | Concrete commands: `AddBlockCommand`, `DeleteBlockCommand`, `MoveBlockCommand`, `ResizeBlockCommand`, `EditContentCommand`, `ChangeZIndexCommand` |
| **1.6.3** | `CommandHistory` — `MutableList<CanvasCommand>` + cursor index. Max 100 entries |
| **1.6.4** | Touch gesture: three-finger swipe left = undo, three-finger swipe right = redo |
| **1.6.5** | Toolbar buttons: undo/redo arrows in the canvas top bar |

### 1.7 — Auto-Save

| Task | Details |
|---|---|
| **1.7.1** | Debounced save (500ms) after any block change: position, size, content, z-index |
| **1.7.2** | Canvas pan/zoom state saved to `NoteEntity.canvasPanX/Y/ZoomLevel` on navigate away |
| **1.7.3** | Save indicator: small dot in top bar turns green when saved, orange when unsaved |

---

## Phase 2: Figure Analysis System

**Goal:** Tap on a figure → side panel opens with AI-assisted analysis, figure notes, and entity linking.

### 2.1 — FigureSidePanel

| Task | Details |
|---|---|
| **2.1.1** | Slide-in panel from right edge (300dp wide), overlaid on canvas |
| **2.1.2** | Panel tabs: **Notes** | **Interpretation** | **Related Ideas** | **Questions** |
| **2.1.3** | Panel header: figure thumbnail + "Figure X" label + figure number |
| **2.1.4** | Source info: filename, page number, source link (if from a SourceEntity) |
| **2.1.5** | Close button → panel slides out, canvas returns to full width |

### 2.2 — Notes Tab

| Task | Details |
|---|---|
| **2.2.1** | Free-text `OutlinedTextField` for user's notes on the figure |
| **2.2.2** | Auto-saved to `FigureMetaEntity.userNotes` |
| **2.2.3** | Character count + last saved timestamp |

### 2.3 — Interpretation Tab

| Task | Details |
|---|---|
| **2.3.1** | "Generate AI Interpretation" button → calls existing `GeminiResearchAssistant` |
| **2.3.2** | Prompt: "Analyze this field research figure/image. Describe what it shows, its significance, and any patterns or anomalies." |
| **2.3.3** | Editable AI response saved to `FigureMetaEntity.interpretation` |
| **2.3.4** | Manual edit: user can refine AI output |

### 2.4 — Related Ideas Tab

| Task | Details |
|---|---|
| **2.4.1** | Shows linked entities: observations, questions, sources, data records |
| **2.4.2** | "Link to entity" button → entity picker dialog (filtered by current project) |
| **2.4.3** | Each linked entity shown as a small card (icon + title + kind badge) |
| **2.4.4** | Tap linked entity → navigate to entity detail |

### 2.5 — Questions Tab

| Task | Details |
|---|---|
| **2.5.1** | "New Question" button → inline form with question text + category |
| **2.5.2** | Saved as `QuestionEntity` linked to the figure block via `linkedEntityType = "canvas_block"` |
| **2.5.3** | Existing questions for this figure listed with status |
| **2.5.4** | Quick-suggest questions via AI: "What questions does this figure raise?" |

### 2.6 — Figure Gallery View

| Task | Details |
|---|---|
| **2.6.1** | Alternative canvas view mode: tap gallery icon to show all figures as a grid |
| **2.6.2** | Each thumbnail shows figure number + caption |
| **2.6.3** | Tap opens the figure's note in canvas mode, centered on that figure |

---

## Phase 3: Handwriting & Drawing Layer

**Goal:** Full Samsung Notes-style handwriting with pen, highlighter, shapes, lasso, and scribble-to-text.

### 3.1 — GPU Drawing Surface

| Task | Details |
|---|---|
| **3.1.1** | `handwriting.GpuDrawingSurface.kt` — OpenGL texture overlay for handwriting strokes |
| **3.1.2** | Stroke capture: `onTouchEvent` → `MotionEvent` points → pressure-sensitive stroke recording |
| **3.1.3** | Stroke rendering on GPU: line strip with round caps, variable width based on pressure |
| **3.1.4** | Bitmap layer caching: rasterize completed strokes to texture, only redraw active stroke per frame |
| **3.1.5** | Performance: 60fps drawing, max 50,000 points per stroke before forced segment |

### 3.2 — Drawing Tools

| Task | Details |
|---|---|
| **3.2.1 — Pen** | Width 1–10px, color picker (custom palette + color wheel), alpha. Rendered as smooth quadratic bezier curves |
| **3.2.2 — Highlighter** | Width 20px, alpha 0.3, colors: yellow (#FFEB3B), green (#4CAF50), pink (#E91E63), blue (#2196F3). Rendered as translucent wide stroke |
| **3.2.3 — Shapes** | Line, Rectangle, Circle, Arrow, Polygon. On finger lift: snap to perfect shape (e.g., roughly drawn circle → perfect circle) |
| **3.2.4 — Lasso** | Freeform selection: draw closed loop around strokes. Selected strokes highlighted. Move/resize/delete/color-change selected strokes |
| **3.2.5 — Eraser** | Stroke eraser: tap a stroke to delete it entirely. Area eraser: brush-based (20px radius), erases portions of strokes |
| **3.2.6 — Tool settings panel** | Bottom sheet with: active tool, color palette, width slider, opacity slider |

### 3.3 — Scribble-To-Text

| Task | Details |
|---|---|
| **3.3.1** | Integration with Google ML Kit `com.google.mlkit:text-recognition` |
| **3.3.2** | Lasso-select strokes → "Convert to text" button in context menu |
| **3.3.3** | Render selected strokes to bitmap → pass to ML Kit handwriting recognizer |
| **3.3.4** | Insert recognized text as a new TextBlock positioned at the drawing's location |
| **3.3.5** | Language selector for handwriting recognition: English, Spanish, French, etc. |

### 3.4 — Stroke Data Format

```kotlin
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
)

data class Stroke(
    val points: List<StrokePoint>,
    val color: Long = 0xFF1C1B19,
    val width: Float = 2f,
    val tool: String = "pen",
    val opacity: Float = 1f
)

// Serialized as JSON array: [{"points":[...],"color":...,"width":...,"tool":"pen","opacity":1.0}, ...]
// Compressed with GZIP for storage (typically 70-85% reduction)
```

### 3.5 — Drawing Optimizations

| Task | Details |
|---|---|
| **3.5.1** | Viewport culling: only render strokes visible in the current canvas viewport |
| **3.5.2** | Stroke LOD: render simplified version at low zoom, detailed at high zoom |
| **3.5.3** | Bitmap tile cache for static strokes (not actively being edited) |
| **3.5.4** | Background save: strokes serialized asynchronously, UI never blocks |

---

## Phase 4: Split Workspace

**Goal:** Multi-pane workspace for working on multiple notes/projects simultaneously.

### 4.1 — SplitPane Composable

| Task | Details |
|---|---|
| **4.1.1** | Horizontal split (side-by-side) for landscape/tablet. Vertical split (top-bottom) for portrait |
| **4.1.2** | Uses `Layout` composable with measured child sizes + drag handle |
| **4.1.3** | Each pane hosts a `NoteCanvasScreen` instance or any other screen (project workspace, library) |
| **4.1.4** | Pane min width: 30% of total. Default: 50/50 |

### 4.2 — Drag Handle

| Task | Details |
|---|---|
| **4.2.1** | Thin (4dp) vertical bar between panes. Shows as subtle line with a grip icon at center |
| **4.2.2** | `detectDragGestures` → updates pane weights in real-time |
| **4.2.3** | Double-tap handle → reset to 50/50 |
| **4.2.4** | Swipe handle to edge → close pane / go full-screen |

### 4.3 — Note Tabs

| Task | Details |
|---|---|
| **4.3.1** | Tab bar at top of each pane: shows open notes as chips |
| **4.3.2** | Tab chip: icon + truncated title + close (X) button |
| **4.3.3** | Swipe tab to close. Long-press + drag to reorder |
| **4.3.4** | "+" button at end of tab bar → opens note list to select a note |
| **4.3.5** | Active tab highlighted with primary color underline |

### 4.4 — Quick Reference / Drag & Drop

| Task | Details |
|---|---|
| **4.4.1** | Long-press block → "Reference" → mini-pane slides up showing linked entity details |
| **4.4.2** | Drag block from one pane to the other (copy or move) |
| **4.4.3** | Drag observation/question/source from reference pane onto canvas → creates new block |

---

## Phase 5: Research Paper Export

**Goal:** Export a note (or project workspace) as a properly formatted research paper in Markdown or PDF.

### 5.1 — Paper Template System

| Task | Details |
|---|---|
| **5.1.1** | Template definitions: IMRaD (Intro, Methods, Results, Discussion), Field Report, Systematic Review, Personal Log |
| **5.1.2** | Each template has section definitions: which canvas block types map to which sections |
| **5.1.3** | Template metadata: author fields, citation style defaults, section ordering |

### 5.2 — Block → Section Mapping

| Task | Details |
|---|---|
| 5.2.1 | Text blocks → paper body paragraphs (sequential order by position) |
| 5.2.2 | Figures/Images → embedded in Results section with "Figure X:" captions |
| 5.2.3 | Tables → formatted Markdown tables |
| 5.2.4 | Drawings → rendered as PNG figures |
| 5.2.5 | Voice notes → transcript text (if available) or "[[Audio file: filename]]" |
| 5.2.6 | Equations → rendered LaTeX or MathML |
| 5.2.7 | Sticky notes → sidebar callout boxes |

### 5.3 — Markdown Generation

| Task | Details |
|---|---|
| **5.3.1** | Generate complete Markdown document: title, authors, abstract, sections, figures, references |
| **5.3.2** | YAML front matter: `title`, `author`, `date`, `template`, `project`, `tags` |
| **5.3.3** | Figure references auto-numbered: `![Figure 1: Caption](path/to/image.png)` |
| **5.3.4** | Table of contents generated from section headings |

### 5.4 — Citation Manager

| Task | Details |
|---|---|
| **5.4.1** | Collect all linked `SourceEntity` objects from blocks in the note |
| **5.4.2** | Format citations in selected style: APA, MLA, Chicago, IEEE |
| **5.4.3** | In-text citation markers: `(Author, Year)` or `[1]` inserted at relevant text positions |
| **5.4.4** | References section at end of document: alphabetically or numerically ordered |
| **5.4.5** | Export `.bib` file alongside Markdown for LaTeX users |

### 5.5 — PDF Generation

| Task | Details |
|---|---|
| **5.5.1** | Generate PDF using Android `PdfDocument` API or integrate a library (e.g., iText) |
| **5.5.2** | PDF layout: A4 or Letter, configurable margins, font embedding |
| **5.5.3** | Figures embedded at correct positions, not at end |
| **5.5.4** | Header/footer: page numbers, title, date |
| **5.5.5** | Export dialog: template selector, metadata fields, citation style, output format |

### 5.6 — Export Dialog UI

| Task | Details |
|---|---|
| **5.6.1** | Full-screen dialog or bottom sheet with: |
| | • Template selector (chips) |
| | • Paper title field (pre-filled from note title) |
| | • Author list (editable) |
| | • Abstract field (optional) |
| | • Citation style dropdown (APA/MLA/Chicago/IEEE) |
| | • Output format: Markdown / PDF / .fieldmind |
| | • Include/exclude checkboxes per content type |
| **5.6.2** | Preview button → generates and shows first page |
| **5.6.3** | Export button → saves file + shares via `FileProvider` (reuses existing export/share infrastructure) |

---

## Phase 6: Existing Logic Integration

**Goal:** Seamlessly tie notes into the existing research workflow.

### 6.1 — Note → Project Linking

| Task | Details |
|---|---|
| **6.1.1** | Notes list shows linked project badge (colored chip with project name) |
| **6.1.2** | Canvas header shows linked project name, tap to navigate to project workspace |
| **6.1.3** | Project workspace gets a new "Notes" tab showing linked notes |
| **6.1.4** | Note counts in project dashboard: "X notes" |

### 6.2 — Block → Entity Linking

| Task | Details |
|---|---|
| **6.2.1** | `CanvasBlockEntity.linkedEntityType` + `linkedEntityId` — links to any entity type |
| **6.2.2** | Block shows small colored dot: observation=green, question=blue, hypothesis=amber, source=purple, data=teal, report=orange |
| **6.2.3** | Tap dot → navigate to entity detail |
| **6.2.4** | Context menu option: "Link to..." → entity picker (observation, question, hypothesis, source, data record, report) |

### 6.3 — Note from Observation

| Task | Details |
|---|---|
| **6.3.1** | "Create note from observation" action in observation detail screen |
| **6.3.2** | Creates new note with TextBlock containing observation text, plus ImageBlocks for attachments |
| **6.3.3** | Auto-links: `CanvasBlockEntity.linkedEntityType = "observation"`, `linkedEntityId = obs.id` |

### 6.4 — Note from Research Session

| Task | Details |
|---|---|
| **6.4.1** | Auto-create a note when a research session starts |
| **6.4.2** | Each observation captured during the session is added as a block |
| **6.4.3** | Session timer shown in canvas header while session is active |

### 6.5 — Quick Capture

| Task | Details |
|---|---|
| **6.5.1** | FAB on home screen: "Quick Note" → opens a minimal composer |
| **6.5.2** | Minimal composer: title field + body text area + save button |
| **6.5.3** | Saves as a new note with a single TextBlock. Opens in full canvas after save |
| **6.5.4** | Widget support: "Quick Capture" widget → opens quick note |

### 6.6 — Search Integration

| Task | Details |
|---|---|
| **6.6.1** | Add `searchCanvasBlocks(query)` to DAO: searches `contentJson` field |
| **6.6.2** | Global search (existing `ArchiveScreen`) includes canvas block results |
| **6.6.3** | Search results link to the note, centered on the matching block |
| **6.6.4** | Full-text search index on `canvas_blocks.contentJson` for performance |

### 6.7 — Export/Backup Integration

| Task | Details |
|---|---|
| **6.7.1** | Canvas blocks, drawings, and figure metadata included in `archiveJson()` export |
| **6.7.2** | New archive sections: `"canvasBlocks"`, `"canvasDrawings"`, `"canvasFigureMeta"` |
| **6.7.3** | `parseArchiveJson()` imports canvas data and restores block positions |
| **6.7.4** | `.fieldmind` package includes drawing/media files for canvas blocks |

---

## Phase 7: UI & Navigation

**Goal:** Beautiful, intuitive UI consistent with the existing FieldMind design system.

### 7.1 — Notes List Screen

| Task | Details |
|---|---|
| **7.1.1** | `NotesListScreen.kt` — new screen showing all notes as styled cards |
| **7.1.2** | Card design: canvas thumbnail (first block preview), title, date, linked project badge, attachment count |
| **7.1.3** | Sort: by date (newest/oldest), title, last edited |
| **7.1.4** | Filter: by project, by type (pinned, templates, archived) |
| **7.1.5** | Search: inline search bar filters notes by title and block content |
| **7.1.6** | Swipe-to-delete with undo snackbar |
| **7.1.7** | Long-press for multi-select: archive, delete, link to project, export |
| **7.1.8** | Empty state: "No notes yet. Create your first note with the + button." |

### 7.2 — Note Canvas Screen

| Task | Details |
|---|---|
| **7.2.1** | `NoteCanvasScreen.kt` — full-screen canvas with minimal chrome |
| **7.2.2** | Top bar: back button, note title (editable inline), linked project badge (tap to navigate), pin toggle, export button, overflow menu |
| **7.2.3** | Overflow menu: Duplicate note, Export as paper, Link to project, View as list (toggle), Timer (research session), Show minimap |
| **7.2.4** | Bottom toolbar: + Add Block FAB (primary action), Undo/Redo buttons, Zoom controls, Toggle handwriting mode |
| **7.2.5** | Block type picker: radial menu from the + FAB showing all 9 block types with icons |

### 7.3 — Block Type Picker

| Task | Details |
|---|---|
| **7.3.1** | Radial menu: circle of 8 icons, 9th centered as "More" |
| **7.3.2** | Icons: Text (article), Image (image), PDF (picture_as_pdf), Figure (collections), Table (grid_on), Sticky (note_sticky), Drawing (draw), Voice (mic), Equation (functions) |
| **7.3.3** | Long-press any item → quick-add at default size without picker |

### 7.4 — Navigation Updates

| Task | Details |
|---|---|
| **7.4.1** | Add `FieldMindScreen.Notes` (notes list) and `FieldMindScreen.NoteEditor` (noteId) to `FieldMindNavigation.kt` |
| **7.4.2** | Notes tab in Workspace screen (5th tab alongside Overview, Observations, Hypotheses, Data, Reports) |
| **7.4.3** | Home screen gets "Recent notes" section |
| **7.4.4** | Quick Note FAB on Home screen |

### 7.5 — Design System Consistency

| Task | Details |
|---|---|
| **7.5.1** | Use `FieldMindTheme.colors.source` as the note accent color (matches existing "note" semantic color) |
| **7.5.2** | Use existing components: `StandardScreenHeader`, `EntityCard`, `InfoChip`, `EmptyState`, `FieldTextField`, `OptionPickerDialog` |
| **7.5.3** | New icons to add to `FieldMindIcons`: `Draw = MaterialSymbolIcon("draw")`, `Grid = MaterialSymbolIcon("grid_on")`, `StickyNote = MaterialSymbolIcon("note_sticky")`, `Functions = MaterialSymbolIcon("functions")` |
| **7.5.4** | Consistent 24dp rounded corners, 16dp padding, surfaceContainerLow card colors |

---

## File Organization

```
app/src/main/java/fieldmind/research/app/features/field/
├── data/
│   ├── canvas/
│   │   ├── CanvasBlockEntity.kt           (Room entity)
│   │   ├── DrawingEntity.kt               (Room entity)
│   │   ├── FigureMetaEntity.kt            (Room entity)
│   │   ├── CanvasBlockDao.kt              (Room DAO)
│   │   ├── DrawingDao.kt                  (Room DAO)
│   │   ├── FigureMetaDao.kt               (Room DAO)
│   │   └── CanvasRepository.kt            (data access layer)
│   ├── database/
│   │   ├── FieldMindDatabase.kt           (+ MIGRATION_3_4)
│   │   └── entity/
│   │       └── FieldEntities.kt           (+ new NoteEntity fields)
├── presentation/
│   ├── canvas/
│   │   ├── InfiniteCanvas.kt              (main canvas composable)
│   │   ├── GpuCanvasSurface.kt            (OpenGL surface wrapper)
│   │   ├── CanvasState.kt                 (zoom/pan state)
│   │   ├── CanvasViewModel.kt             (canvas block state + undo/redo)
│   │   ├── blocks/
│   │   │   ├── CanvasBlock.kt             (base block composable)
│   │   │   ├── TextBlock.kt
│   │   │   ├── ImageBlock.kt
│   │   │   ├── PdfBlock.kt
│   │   │   ├── FigureBlock.kt
│   │   │   ├── TableBlock.kt
│   │   │   ├── StickyNoteBlock.kt
│   │   │   ├── DrawingBlock.kt
│   │   │   ├── VoiceNoteBlock.kt
│   │   │   └── EquationBlock.kt
│   │   ├── BlockToolbar.kt
│   │   ├── BlockTypePicker.kt             (radial menu)
│   │   ├── CanvasMinimap.kt
│   │   └── UndoRedoManager.kt
│   ├── handwriting/
│   │   ├── GpuDrawingSurface.kt           (OpenGL handwriting layer)
│   │   ├── DrawingTools.kt                (toolbar: pen, highlighter, shapes, etc.)
│   │   ├── StrokeRecorder.kt              (motion event → stroke data)
│   │   ├── LassoSelection.kt
│   │   ├── ShapeSnapper.kt
│   │   └── ScribbleToText.kt              (ML Kit handwriting recognition)
│   ├── figure/
│   │   ├── FigureSidePanel.kt
│   │   ├── FigureGalleryView.kt
│   │   └── FigureAnnotationLayer.kt
│   ├── splitpane/
│   │   ├── SplitPane.kt
│   │   └── NoteTabBar.kt
│   ├── export/
│   │   ├── PaperTemplate.kt
│   │   ├── MarkdownGenerator.kt
│   │   ├── CitationFormatter.kt
│   │   ├── PdfGenerator.kt
│   │   └── PaperExportDialog.kt
│   ├── screens/
│   │   ├── NotesListScreen.kt
│   │   └── NoteCanvasScreen.kt
│   ├── viewmodel/
│   │   └── CanvasViewModel.kt
│   └── components/
│       ├── FieldMindIcons.kt              (+ new icons)
│       └── FieldMindComponents.kt
└── presentation/
    └── theme/
        └── FieldMindTheme.kt
```

---

## Estimated Effort

| Phase | Estimated days | Dependencies | Priority |
|-------|---------------|-------------|----------|
| **Phase 0: Data Layer** | **2-3 days** | None | 🔴 Critical |
| **Phase 1: Core Canvas** | **5-7 days** | Phase 0 | 🔴 Critical |
| **Phase 2: Figure Analysis** | **2-3 days** | Phase 1 | 🟡 High |
| **Phase 3: Handwriting** | **4-5 days** | Phase 1 | 🟡 High |
| **Phase 4: Split Workspace** | **2 days** | Phase 1 | 🟢 Medium |
| **Phase 5: Paper Export** | **3-4 days** | Phase 1, Phase 6 | 🟡 High |
| **Phase 6: Integration** | **2-3 days** | Phase 0, Phase 1 | 🔴 Critical |
| **Phase 7: UI/Navigation** | **2-3 days** | Phase 1 (can partially overlap) | 🔴 Critical |
| **Total** | **22-30 days** | | |

---

## Key Design Decisions

| Decision | Chosen approach | Rationale |
|----------|----------------|-----------|
| Canvas rendering | NDK/OpenGL via `TextureView` | Smoother handwriting, faster block rendering, lower battery drain for complex canvases |
| Block storage | Room DB rows (not JSON blob) | Efficient partial updates, undo/redo, search, cross-referencing |
| Stroke data | Compressed JSON arrays | Fast serialization, no NDK dependency for stroke format, works with Room |
| Text editor | Custom Markdown composable | Full control over canvas integration, styling, and `/` command menu |
| DI | Manual (existing pattern) | Consistent with codebase, no Hilt/Dagger overhead |
| Handwriting recognition | Google ML Kit | On-device, free, supports multiple languages |
| Figure analysis AI | Existing Gemini API | Already integrated, no new API key needed |
| PDF generation | Android `PdfDocument` + optional iText | No external dependency for basic PDF, iText for advanced features |

---

## DOX: AGENTS.md Updates

The following AGENTS.md files will need updates as implementation progresses:

| File | When to update |
|------|---------------|
| `app/AGENTS.md` — Ownership section | After Phase 0: add canvas/ sub-packages to ownership |
| `features/field/data/AGENTS.md` | After Phase 0: add canvas data layer ownership |
| `features/field/presentation/AGENTS.md` | After Phase 1, 3, 4: add canvas/handwriting/splitpane screen/component ownership |
| `docs/AGENTS.md` | After plan creation: add this doc to child index |

---

> **Next step:** Send the Phase 0 prompt to start implementation.
