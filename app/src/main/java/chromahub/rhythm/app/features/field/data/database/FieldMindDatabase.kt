package chromahub.rhythm.app.features.field.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import chromahub.rhythm.app.features.field.data.database.dao.FieldMindDao
import chromahub.rhythm.app.features.field.data.database.entity.DataRecordEntity
import chromahub.rhythm.app.features.field.data.database.entity.HypothesisEntity
import chromahub.rhythm.app.features.field.data.database.entity.ObservationEntity
import chromahub.rhythm.app.features.field.data.database.entity.ObservationTagCrossRef
import chromahub.rhythm.app.features.field.data.database.entity.ProjectEntity
import chromahub.rhythm.app.features.field.data.database.entity.ProjectObservationCrossRef
import chromahub.rhythm.app.features.field.data.database.entity.ProjectSourceCrossRef
import chromahub.rhythm.app.features.field.data.database.entity.QuestionEntity
import chromahub.rhythm.app.features.field.data.database.entity.QuestionObservationCrossRef
import chromahub.rhythm.app.features.field.data.database.entity.QuestionSourceCrossRef
import chromahub.rhythm.app.features.field.data.database.entity.ReportEntity
import chromahub.rhythm.app.features.field.data.database.entity.ReportSourceCrossRef
import chromahub.rhythm.app.features.field.data.database.entity.SourceEntity
import chromahub.rhythm.app.features.field.data.database.entity.TagEntity

@Database(
    entities = [
        ObservationEntity::class,
        QuestionEntity::class,
        HypothesisEntity::class,
        ProjectEntity::class,
        SourceEntity::class,
        DataRecordEntity::class,
        ReportEntity::class,
        TagEntity::class,
        ObservationTagCrossRef::class,
        QuestionObservationCrossRef::class,
        QuestionSourceCrossRef::class,
        ProjectObservationCrossRef::class,
        ProjectSourceCrossRef::class,
        ReportSourceCrossRef::class
    ],
    version = 1,
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
            ).build().also { INSTANCE = it }
        }
    }
}
