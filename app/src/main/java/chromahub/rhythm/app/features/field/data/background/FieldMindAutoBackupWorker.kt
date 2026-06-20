package fieldmind.research.app.features.field.data.background

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fieldmind.research.app.features.field.data.database.FieldMindDatabase
import fieldmind.research.app.features.field.data.database.entity.EvidenceAttachmentEntity
import fieldmind.research.app.features.field.data.export.FieldMindExport
import fieldmind.research.app.features.field.data.export.FieldMindExportMediaPacker
import fieldmind.research.app.features.field.data.repository.FieldMindRepository
import fieldmind.research.app.features.field.data.settings.FieldMindSettings
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Creates a .fieldmind package with all data + media attachments when auto-backup is enabled.
 * Saves to the user's chosen SAF folder (if set) or falls back to app-private storage.
 */
class FieldMindAutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val settings = FieldMindSettings.getInstance(applicationContext)
        if (!settings.autoBackupEnabled.value) return@runCatching Result.success()

        val repository = FieldMindRepository(FieldMindDatabase.getInstance(applicationContext).fieldMindDao())
        val observations = repository.observations.first()
        val notes = repository.notes.first()
        val questions = repository.questions.first()
        val hypotheses = repository.hypotheses.first()
        val projects = repository.projects.first()
        val sources = repository.sources.first()
        val dataRecords = repository.dataRecords.first()
        val reports = repository.reports.first()
        val flashcards = repository.flashcards.first()

        val archiveJson = FieldMindExport.archiveJson(
            observations = observations,
            notes = notes,
            questions = questions,
            hypotheses = hypotheses,
            projects = projects,
            sources = sources,
            dataRecords = dataRecords,
            reports = reports,
            flashcards = flashcards
        )

        // Build .fieldmind package with media attachments
        val tempDir = File(applicationContext.cacheDir, "auto_backup").apply { mkdirs() }
        val dao = FieldMindDatabase.getInstance(applicationContext).fieldMindDao()
        val allAttachments = mutableMapOf<Long, List<EvidenceAttachmentEntity>>()
        observations.forEach { obs ->
            val atts = dao.observeAttachmentsForObservation(obs.id).first()
            if (atts.isNotEmpty()) allAttachments[obs.id] = atts
        }

        val result = FieldMindExportMediaPacker.buildPackage(
            context = applicationContext,
            archiveJson = archiveJson,
            observations = observations,
            notes = notes,
            projects = projects,
            sources = sources,
            attachments = allAttachments,
            outputDir = tempDir
        )

        // Try to save to the user's chosen SAF folder, fall back to private storage
        val folderUriStr = settings.backupFolderUri.value
        if (folderUriStr.isNotBlank()) {
            try {
                val folderUri = Uri.parse(folderUriStr)
                val fileName = result.packageFile.name
                val mimeType = "application/octet-stream"
                val createdDoc = android.provider.DocumentsContract.createDocument(
                    applicationContext.contentResolver,
                    folderUri,
                    mimeType,
                    fileName
                )
                if (createdDoc != null) {
                    applicationContext.contentResolver.openOutputStream(createdDoc)?.use { out ->
                        out.write(result.packageFile.readBytes())
                    }
                }
                result.packageFile.delete()
            } catch (_: Exception) {
                // Fall through to private storage below
                result.packageFile.delete()
            }
        }

        // Fallback: save to private storage
        if (result.packageFile.exists()) {
            val backupDir = File(applicationContext.filesDir, "fieldmind/backups").apply { mkdirs() }
            result.packageFile.renameTo(File(backupDir, result.packageFile.name))
        }

        pruneOldBackups(applicationContext)
        Result.success()
    }.getOrElse { Result.retry() }

    private fun pruneOldBackups(context: Context) {
        val backupDir = File(context.filesDir, "fieldmind/backups")
        if (!backupDir.exists()) return
        backupDir.listFiles { file -> file.isFile && file.extension == "fieldmind" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(8)
            ?.forEach { it.delete() }
    }
}
