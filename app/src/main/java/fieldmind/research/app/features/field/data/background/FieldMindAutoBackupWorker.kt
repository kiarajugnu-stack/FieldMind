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
        // ── Added missing entity types (was missing from auto-backup) ──
        val species = repository.species.first()
        val weatherCatalog = repository.weatherCatalog.first()
        val researchSessions = repository.researchSessions.first()
        val tasks = repository.tasks.first()

        // ── Collect cross-references for backup ──
        val crossRefs = buildList {
            dao.getAllObservationTagCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("observationTag", it.observationId, it.tagId)) }
            dao.getAllQuestionObservationCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("questionObservation", it.questionId, it.observationId)) }
            dao.getAllQuestionSourceCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("questionSource", it.questionId, it.sourceId)) }
            dao.getAllProjectObservationCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("projectObservation", it.projectId, it.observationId)) }
            dao.getAllProjectSourceCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("projectSource", it.projectId, it.sourceId)) }
            dao.getAllReportSourceCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("reportSource", it.reportId, it.sourceId)) }
            dao.getAllProjectDataRecordCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("projectDataRecord", it.projectId, it.dataRecordId)) }
            dao.getAllTaskObservationCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("taskObservation", it.taskId, it.observationId)) }
            dao.getAllTaskEvidenceCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("taskEvidence", it.taskId, it.evidenceId)) }
            dao.getAllSpeciesObservationCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("speciesObservation", it.speciesId, it.observationId)) }
            dao.getAllSpeciesQuestionCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("speciesQuestion", it.speciesId, it.questionId)) }
            dao.getAllEvidenceReportCrossRefs().forEach { add(FieldMindExport.CrossReferenceEntry("evidenceReport", it.evidenceId, it.reportId)) }
            // SessionObservationCrossRef and HypothesisEvidenceCrossRef use observeAll flows (not direct query)
            // Collect them via the database session and hypothesis_evidence tables
            dao.observeAllSessionObservationCrossRefs().first().forEach {
                add(FieldMindExport.CrossReferenceEntry("sessionObservation", it.sessionId, it.observationId))
            }
            dao.observeAllHypothesisEvidence().first().forEach {
                add(FieldMindExport.CrossReferenceEntry("hypothesisEvidence", it.hypothesisId, it.observationId))
            }
        }

        // ── Collect PhashDatabase and GeoFenceRegions for backup ──
        val phashDb = fieldmind.research.app.features.field.data.vision.PhashDatabase(applicationContext)
        val geoFence = fieldmind.research.app.features.field.data.location.GeoFenceReminder(applicationContext)

        // Merge extra data into settings JSON
        val settingsObj = org.json.JSONObject(settings.toExportJson())
        val phashJson = phashDb.exportEntriesJson()
        if (phashJson.isNotBlank() && phashJson != "[]") {
            settingsObj.put("phashData", phashJson)
        }
        val geoFenceJson = geoFence.exportRegionsJson()
        if (geoFenceJson.isNotBlank() && geoFenceJson != "[]") {
            settingsObj.put("geoFenceData", geoFenceJson)
        }
        val settingsJson = settingsObj.toString(2)

        val archiveJson = FieldMindExport.archiveJson(
            observations = observations,
            notes = notes,
            questions = questions,
            hypotheses = hypotheses,
            projects = projects,
            sources = sources,
            dataRecords = dataRecords,
            reports = reports,
            flashcards = flashcards,
            species = species,
            weatherCatalog = weatherCatalog,
            researchSessions = researchSessions,
            tasks = tasks,
            crossReferences = crossRefs,
            settingsJson = settingsJson
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
                val treeDocumentId = android.provider.DocumentsContract.getTreeDocumentId(folderUri)
                val parentDocumentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(folderUri, treeDocumentId)
                val createdDoc = android.provider.DocumentsContract.createDocument(
                    applicationContext.contentResolver,
                    parentDocumentUri,
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
        val settings = FieldMindSettings.getInstance(context)
        val intervalLabel = settings.autoBackupInterval.value
        // Keep more backups for shorter intervals, fewer for longer ones
        val keepCount = when (intervalLabel) {
            "Every 6 hours" -> 30
            "Every 12 hours" -> 30
            "Daily" -> 14
            "Weekly" -> 8
            "Monthly" -> 6
            else -> 8
        }
        val backupDir = File(context.filesDir, "fieldmind/backups")
        if (!backupDir.exists()) return
        backupDir.listFiles { file -> file.isFile && file.extension == "fieldmind" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(keepCount)
            ?.forEach { it.delete() }
    }
}
