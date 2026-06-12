package fieldmind.research.app.features.field.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        HypothesisEvidenceCrossRef::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FieldMindDatabase : RoomDatabase() {
    abstract fun fieldMindDao(): FieldMindDao

    companion object {
        @Volatile private var INSTANCE: FieldMindDatabase? = null

        fun getInstance(context: Context): FieldMindDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                FieldMindDatabase::class.java,
                "fieldmind_database"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
