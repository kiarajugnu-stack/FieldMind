package fieldmind.research.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fieldmind.research.app.features.field.data.database.FieldMindDatabase
import fieldmind.research.app.features.field.data.export.FieldMindExport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Periodic worker that exports FieldMind research data as JSON archive files.
 * Respects FieldMindSettings.autoBackupEnabled and autoBackupInterval.
 */
class FieldMindBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "FieldMindBackupWorker"
        const val WORK_NAME = "fieldmind_auto_backup"
        private const val BACKUP_FOLDER = "FieldMindBackups"
        private const val MAX_BACKUPS = 7
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting FieldMind auto-backup...")

            val dao = FieldMindDatabase.getInstance(applicationContext).fieldMindDao()

            // Gather all data
            val observations = dao.observeObservations().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.ObservationEntity> = emptyList()
                flow.first()
            }
            val notes = dao.observeNotes().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.NoteEntity> = emptyList()
                flow.first()
            }
            val questions = dao.observeQuestions().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.QuestionEntity> = emptyList()
                flow.first()
            }
            val hypotheses = dao.observeHypotheses().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.HypothesisEntity> = emptyList()
                flow.first()
            }
            val projects = dao.observeProjects().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.ProjectEntity> = emptyList()
                flow.first()
            }
            val sources = dao.observeSources().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.SourceEntity> = emptyList()
                flow.first()
            }
            val dataRecords = dao.observeDataRecords().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.DataRecordEntity> = emptyList()
                flow.first()
            }
            val reports = dao.observeReports().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.ReportEntity> = emptyList()
                flow.first()
            }
            val flashcards = dao.observeFlashcards().let { flow ->
                var result: List<fieldmind.research.app.features.field.data.database.entity.FlashcardEntity> = emptyList()
                flow.first()
            }

            // Build JSON archive
            val json = FieldMindExport.archiveJson(
                observations, notes, questions, hypotheses,
                projects, sources, dataRecords, reports, flashcards
            )

            // Write to backup directory
            val backupDir = File(applicationContext.getExternalFilesDir(null), BACKUP_FOLDER)
            if (!backupDir.exists()) backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "fieldmind_backup_$timestamp.json")
            backupFile.writeText(json)

            Log.d(TAG, "FieldMind backup saved: ${backupFile.absolutePath} (${backupFile.length()} bytes)")
            Log.d(TAG, "Contents: ${observations.size} obs, ${notes.size} notes, ${questions.size} questions, ${projects.size} projects, ${sources.size} sources, ${dataRecords.size} data, ${reports.size} reports, ${flashcards.size} flashcards")

            // Cleanup old backups
            cleanupOldBackups(backupDir)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "FieldMind backup failed", e)
            Result.retry()
        }
    }

    private fun cleanupOldBackups(backupDir: File) {
        try {
            val files = backupDir.listFiles { f -> f.name.startsWith("fieldmind_backup_") && f.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: return
            if (files.size > MAX_BACKUPS) {
                files.drop(MAX_BACKUPS).forEach { it.delete() }
                Log.d(TAG, "Cleaned up ${files.size - MAX_BACKUPS} old backup(s)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup cleanup failed", e)
        }
    }
}
