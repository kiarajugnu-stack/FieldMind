package fieldmind.research.app.features.field.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fieldmind.research.app.features.field.data.canvas.CanvasBlockDao
import fieldmind.research.app.features.field.data.canvas.CanvasBlockEntity
import fieldmind.research.app.features.field.data.canvas.DrawingDao
import fieldmind.research.app.features.field.data.canvas.DrawingEntity
import fieldmind.research.app.features.field.data.canvas.FigureMetaDao
import fieldmind.research.app.features.field.data.canvas.FigureMetaEntity
import fieldmind.research.app.features.field.data.database.dao.FieldMindDao
import fieldmind.research.app.features.field.data.database.entity.*

@Database(
    entities = [
        // ── Existing entities ──
        ObservationEntity::class,
        NoteEntity::class,
        QuestionEntity::class,
        HypothesisEntity::class,
        ProjectEntity::class,
        SourceEntity::class,
        DataRecordEntity::class,
        ReportEntity::class,
        FlashcardEntity::class,
        TagEntity::class,
        EvidenceAttachmentEntity::class,
        ObservationTagCrossRef::class,
        QuestionObservationCrossRef::class,
        QuestionSourceCrossRef::class,
        ProjectObservationCrossRef::class,
        ProjectSourceCrossRef::class,
        ReportSourceCrossRef::class,
        ProjectDataRecordCrossRef::class,
        HypothesisEvidenceCrossRef::class,
        ResearchSessionEntity::class,
        SessionObservationCrossRef::class,
        TaskEntity::class,
        SpeciesEntity::class,
        WeatherCatalogEntity::class,
        TeamMemberEntity::class,
        TaskObservationCrossRef::class,
        TaskEvidenceCrossRef::class,
        SpeciesObservationCrossRef::class,
        SpeciesQuestionCrossRef::class,
        EvidenceReportCrossRef::class,
        // ── Canvas / Notes App entities (Phase 0+) ──
        CanvasBlockEntity::class,
        DrawingEntity::class,
        FigureMetaEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class FieldMindDatabase : RoomDatabase() {
    abstract fun fieldMindDao(): FieldMindDao

    // ── Canvas / Notes App DAOs (Phase 0+) ──
    abstract fun canvasBlockDao(): CanvasBlockDao
    abstract fun drawingDao(): DrawingDao
    abstract fun figureMetaDao(): FigureMetaDao

    companion object {
        @Volatile private var INSTANCE: FieldMindDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("field_observations", "structuredDetailsJson", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfMissing("field_observations", "startedAt", "INTEGER")
                db.addColumnIfMissing("field_observations", "endedAt", "INTEGER")
                db.addColumnIfMissing("field_observations", "durationMs", "INTEGER")
                db.addColumnIfMissing("field_observations", "changeObservedAt", "INTEGER")
                db.addColumnIfMissing("field_observations", "changeDurationMs", "INTEGER")
                db.addColumnIfMissing("field_observations", "timeNote", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfMissing("field_projects", "connectionMap", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfMissing("field_projects", "attachmentUris", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfMissing("field_data_records", "datasetKind", "TEXT NOT NULL DEFAULT 'Manual'")
                db.addColumnIfMissing("field_data_records", "chartPreference", "TEXT NOT NULL DEFAULT 'Line'")
                db.addColumnIfMissing("field_data_records", "linkedSessionId", "INTEGER")
                db.addColumnIfMissing("field_reports", "templateId", "TEXT NOT NULL DEFAULT 'field_report'")
                db.addColumnIfMissing("field_reports", "preset", "TEXT NOT NULL DEFAULT 'Personal log'")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("field_observations", "weatherPressure", "REAL")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ObservationEntity additions
                db.addColumnIfMissing("field_observations", "qualityScore", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("field_observations", "parentObservationId", "INTEGER")
                db.addColumnIfMissing("field_observations", "followUpScheduledAt", "INTEGER")
                // ProjectEntity additions
                db.addColumnIfMissing("field_projects", "projectType", "TEXT NOT NULL DEFAULT 'Observation'")
                db.addColumnIfMissing("field_projects", "selectedMethods", "TEXT NOT NULL DEFAULT ''")
                // ResearchSessionEntity may be new — create table if it doesn't exist
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `field_research_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `projectId` INTEGER,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER,
                        `totalDurationMs` INTEGER NOT NULL DEFAULT 0,
                        `observationCount` INTEGER NOT NULL DEFAULT 0,
                        `location` TEXT NOT NULL DEFAULT '',
                        `latitude` REAL,
                        `longitude` REAL,
                        `status` TEXT NOT NULL DEFAULT 'Active',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `archivedAt` INTEGER,
                        `deletedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                // SessionObservationCrossRef may be new
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `field_session_observations` (
                        `sessionId` INTEGER NOT NULL,
                        `observationId` INTEGER NOT NULL,
                        PRIMARY KEY(`sessionId`, `observationId`)
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration 12 → 13: Adds canvas data layer tables + new NoteEntity columns.
         *
         * Creates:
         * - canvas_blocks table
         * - canvas_drawings table
         * - canvas_figure_meta table
         *
         * Adds columns to field_notes:
         * - canvasVersion, color, iconName, isPinned, parentNoteId, linkedProjectId,
         *   isTemplate, viewMode, canvasZoomLevel, canvasPanX, canvasPanY
         */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `canvas_blocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL DEFAULT 'text',
                        `contentJson` TEXT NOT NULL DEFAULT '',
                        `positionX` REAL NOT NULL DEFAULT 0.0,
                        `positionY` REAL NOT NULL DEFAULT 0.0,
                        `width` REAL NOT NULL DEFAULT 300.0,
                        `height` REAL NOT NULL DEFAULT 200.0,
                        `zIndex` INTEGER NOT NULL DEFAULT 0,
                        `rotation` REAL NOT NULL DEFAULT 0.0,
                        `opacity` REAL NOT NULL DEFAULT 1.0,
                        `linkedEntityType` TEXT NOT NULL DEFAULT '',
                        `linkedEntityId` INTEGER,
                        `pinned` INTEGER NOT NULL DEFAULT 0,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `archivedAt` INTEGER,
                        `deletedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `canvas_drawings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `blockId` INTEGER,
                        `strokeDataJson` TEXT NOT NULL DEFAULT '',
                        `toolType` TEXT NOT NULL DEFAULT 'pen',
                        `color` INTEGER NOT NULL DEFAULT 4279375641,
                        `strokeWidth` REAL NOT NULL DEFAULT 2.0,
                        `layerIndex` INTEGER NOT NULL DEFAULT 0,
                        `archivedAt` INTEGER,
                        `deletedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `canvas_figure_meta` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `blockId` INTEGER NOT NULL,
                        `sourceFilename` TEXT NOT NULL DEFAULT '',
                        `caption` TEXT NOT NULL DEFAULT '',
                        `figureNumber` INTEGER NOT NULL DEFAULT 0,
                        `pageNumber` INTEGER,
                        `interpretation` TEXT NOT NULL DEFAULT '',
                        `userNotes` TEXT NOT NULL DEFAULT '',
                        `relatedIdeas` TEXT NOT NULL DEFAULT '',
                        `questionsGenerated` TEXT NOT NULL DEFAULT '',
                        `archivedAt` INTEGER,
                        `deletedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Add new canvas-related columns to field_notes
                db.addColumnIfMissing("field_notes", "canvasVersion", "INTEGER NOT NULL DEFAULT 1")
                db.addColumnIfMissing("field_notes", "color", "INTEGER")
                db.addColumnIfMissing("field_notes", "iconName", "TEXT NOT NULL DEFAULT ''")
                db.addColumnIfMissing("field_notes", "isPinned", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("field_notes", "parentNoteId", "INTEGER")
                db.addColumnIfMissing("field_notes", "linkedProjectId", "INTEGER")
                db.addColumnIfMissing("field_notes", "isTemplate", "INTEGER NOT NULL DEFAULT 0")
                db.addColumnIfMissing("field_notes", "viewMode", "TEXT NOT NULL DEFAULT 'canvas'")
                db.addColumnIfMissing("field_notes", "canvasZoomLevel", "REAL NOT NULL DEFAULT 1.0")
                db.addColumnIfMissing("field_notes", "canvasPanX", "REAL NOT NULL DEFAULT 0.0")
                db.addColumnIfMissing("field_notes", "canvasPanY", "REAL NOT NULL DEFAULT 0.0")
            }
        }

        private fun SupportSQLiteDatabase.addColumnIfMissing(table: String, column: String, definition: String) {
            val cursor = query("PRAGMA table_info(`$table`)")
            cursor.use {
                while (it.moveToNext()) {
                    if (it.getString(it.getColumnIndexOrThrow("name")) == column) return
                }
            }
            execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
        }

        fun getInstance(context: Context): FieldMindDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                FieldMindDatabase::class.java,
                "fieldmind_database"
            )
                .addMigrations(
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_12_13
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
