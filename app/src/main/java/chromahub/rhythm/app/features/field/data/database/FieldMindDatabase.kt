package fieldmind.research.app.features.field.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fieldmind.research.app.features.field.data.database.dao.FieldMindDao
import fieldmind.research.app.features.field.data.database.entity.*

@Database(
    entities = [
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
        SessionObservationCrossRef::class
    ],
    version = 9,
    exportSchema = false
)
abstract class FieldMindDatabase : RoomDatabase() {
    abstract fun fieldMindDao(): FieldMindDao

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
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
                .build()
                .also { INSTANCE = it }
        }
    }
}
