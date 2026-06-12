package fieldmind.research.app.features.field.data.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fieldmind.research.app.features.field.data.database.FieldMindDatabase
import fieldmind.research.app.features.field.data.export.FieldMindExport
import fieldmind.research.app.features.field.data.repository.FieldMindRepository
import fieldmind.research.app.features.field.data.settings.FieldMindSettings
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes a portable archive JSON to app-private storage when auto-backup is enabled. */
class FieldMindAutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val settings = FieldMindSettings.getInstance(applicationContext)
        if (!settings.autoBackupEnabled.value) return@runCatching Result.success()

        val repository = FieldMindRepository(FieldMindDatabase.getInstance(applicationContext).fieldMindDao())
        val archive = FieldMindExport.archiveJson(
            observations = repository.observations.first(),
            notes = repository.notes.first(),
            questions = repository.questions.first(),
            hypotheses = repository.hypotheses.first(),
            projects = repository.projects.first(),
            sources = repository.sources.first(),
            dataRecords = repository.dataRecords.first(),
            reports = repository.reports.first(),
            flashcards = repository.flashcards.first()
        )
        val dir = File(applicationContext.filesDir, "fieldmind/backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        File(dir, "fieldmind-auto-$stamp.json").writeText(archive)
        pruneOldBackups(dir)
        Result.success()
    }.getOrElse { Result.retry() }

    private fun pruneOldBackups(dir: File) {
        dir.listFiles { file -> file.isFile && file.name.startsWith("fieldmind-auto-") && file.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(8)
            ?.forEach { it.delete() }
    }
}
