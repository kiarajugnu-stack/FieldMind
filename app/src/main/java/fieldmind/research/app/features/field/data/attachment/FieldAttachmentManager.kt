package fieldmind.research.app.features.field.data.attachment

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import fieldmind.research.app.features.field.data.database.FieldMindDatabase
import fieldmind.research.app.features.field.data.database.dao.FieldMindDao
import fieldmind.research.app.features.field.data.database.entity.EvidenceAttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manages evidence attachment files: saving to private storage, deleting,
 * generating shareable URIs, copying between observations, calculating
 * storage usage, and finding orphaned attachments.
 *
 * All file I/O happens on [Dispatchers.IO] via suspend functions.
 */
object FieldAttachmentManager {

    private const val MAX_ATTACHMENT_SIZE = 25L * 1024 * 1024 // 25 MB

    // ── Existing methods (kept for backward compatibility) ──

    /**
     * Copy an attachment to app-private storage (legacy API).
     * Returns an [AttachmentCopyResult] with the local URI or error.
     */
    fun copyToPrivateStorage(context: Context, uri: Uri, observationId: Long, caption: String): AttachmentCopyResult {
        val dir = File(context.filesDir, "fieldmind/evidence/$observationId")
        if (!dir.exists()) dir.mkdirs()

        val input = try { context.contentResolver.openInputStream(uri) } catch (e: Exception) { null }
            ?: return AttachmentCopyResult(uri.toString(), false, "Could not read source file")

        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val ext = when {
            mime.startsWith("image/") -> "." + mime.substringAfter("image/").substringBefore(";").ifBlank { "jpg" }
            mime.startsWith("video/") -> "." + mime.substringAfter("video/").substringBefore(";").ifBlank { "mp4" }
            mime.startsWith("audio/") -> "." + mime.substringAfter("audio/").substringBefore(";").ifBlank { "m4a" }
            mime == "application/pdf" -> ".pdf"
            else -> ".dat"
        }
        val localFile = File(dir, "${System.currentTimeMillis()}_${(1000..9999).random()}$ext")

        return try {
            // Check file size via content resolver before copying
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            val fileSize = fd?.statSize ?: -1L
            fd?.close()
            if (fileSize > MAX_ATTACHMENT_SIZE) {
                return AttachmentCopyResult(uri.toString(), false, "File too large (${fileSize / 1024 / 1024} MB, max 25 MB)")
            }
            input.use { i -> FileOutputStream(localFile).use { o -> i.copyTo(o) } }
            AttachmentCopyResult(Uri.fromFile(localFile).toString(), true, null, localFile.absolutePath, mime)
        } catch (e: Exception) {
            AttachmentCopyResult(uri.toString(), false, "Copy failed: ${e.localizedMessage}")
        }
    }

    /** Clean up all cached attachment files for an observation. */
    fun cleanObservationAttachments(context: Context, observationId: Long) {
        val dir = File(context.filesDir, "fieldmind/evidence/$observationId")
        if (dir.exists()) dir.deleteRecursively()
    }

    // ── Required Prompt E methods ──

    /**
     * Save an attachment from a source URI to app-private storage and
     * persist an [EvidenceAttachmentEntity] via the DAO.
     *
     * @return the saved [EvidenceAttachmentEntity] with its generated id
     */
    suspend fun saveAttachment(
        context: Context,
        sourceUri: Uri,
        observationId: Long,
        caption: String = "",
        type: String = detectMimeType(context, sourceUri)
    ): EvidenceAttachmentEntity = withContext(Dispatchers.IO) {
        val dao = FieldMindDatabase.getInstance(context).fieldMindDao()

        // Copy file to private storage
        val dir = File(context.filesDir, "fieldmind/evidence/$observationId")
        if (!dir.exists()) dir.mkdirs()

        val mime = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
        val ext = mimeToExtension(mime)
        val fileName = "${System.currentTimeMillis()}_${(1000..9999).random()}$ext"
        val localFile = File(dir, fileName)

        // Persist read permission for SAF URIs (same pattern as FieldMindObserveScreen)
        try {
            context.contentResolver.takePersistableUriPermission(
                sourceUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // URI doesn't support persistable permissions — proceed without it
        }

        // Read source and write to local file
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(localFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not read source URI: $sourceUri")

        val localPath = localFile.absolutePath

        // Create and insert the entity
        val entity = EvidenceAttachmentEntity(
            observationId = observationId,
            type = type,
            uri = Uri.fromFile(localFile).toString(),
            localPath = localPath,
            caption = caption,
            status = "Active",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = dao.insertEvidenceAttachment(entity)
        entity.copy(id = id)
    }

    /**
     * Delete the physical file for an attachment and soft-delete the
     * database record.
     *
     * @return true if the file was successfully deleted or didn't exist
     */
    suspend fun deleteAttachmentFile(
        context: Context,
        attachment: EvidenceAttachmentEntity
    ): Boolean = withContext(Dispatchers.IO) {
        val dao = FieldMindDatabase.getInstance(context).fieldMindDao()
        var success = true

        // Delete physical file if localPath exists
        if (!attachment.localPath.isNullOrBlank()) {
            val file = File(attachment.localPath)
            if (file.exists()) {
                success = file.delete()
            }
        }

        // Also try deleting from the uri-based path
        if (attachment.uri.startsWith("file://")) {
            val uriFile = File(Uri.parse(attachment.uri).path ?: "")
            if (uriFile.exists()) {
                uriFile.delete()
            }
        }

        // Soft-delete the database record
        dao.softDeleteAttachment(attachment.id, System.currentTimeMillis())

        success
    }

    /**
     * Return a shareable [Uri] for an attachment.
     * Uses [FileProvider] when a local path is available; falls back to
     * the stored URI otherwise.
     */
    fun getAttachmentUri(
        context: Context,
        attachment: EvidenceAttachmentEntity
    ): Uri {
        // Prefer local path via FileProvider
        if (!attachment.localPath.isNullOrBlank()) {
            val file = File(attachment.localPath)
            if (file.exists()) {
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            }
        }

        // Fall back to the stored URI
        val storedUri = Uri.parse(attachment.uri)
        if (storedUri.scheme in listOf("content", "file")) {
            return storedUri
        }

        // Last resort: return the uri string as a file URI
        return Uri.fromFile(File(attachment.uri))
    }

    /**
     * Copy all attachments from one observation to another.
     * Useful for merge / duplicate operations.
     */
    suspend fun copyAttachmentsBetweenObservations(
        context: Context,
        fromObsId: Long,
        toObsId: Long
    ): Int = withContext(Dispatchers.IO) {
        val dao = FieldMindDatabase.getInstance(context).fieldMindDao()

        // Collect all active attachments from the source observation
        val sourceAttachments = withContext(Dispatchers.IO) {
            // Use firstOrNull() to get a snapshot since this is a one-time copy
            dao.observeAttachmentsForObservation(fromObsId).firstOrNull() ?: emptyList()
        }

        if (sourceAttachments.isEmpty()) return@withContext 0

        var copiedCount = 0
        for (attachment in sourceAttachments) {
            try {
                // Copy the physical file to the new observation's folder
                val sourcePath = attachment.localPath
                val sourceUri = Uri.parse(attachment.uri)

                if (!sourcePath.isNullOrBlank() && File(sourcePath).exists()) {
                    // Local file exists — copy it
                    val destDir = File(context.filesDir, "fieldmind/evidence/$toObsId")
                    if (!destDir.exists()) destDir.mkdirs()

                    val sourceFile = File(sourcePath)
                    val destFile = File(destDir, sourceFile.name)
                    sourceFile.copyTo(destFile, overwrite = false)

                    val newEntity = attachment.copy(
                        id = 0,
                        observationId = toObsId,
                        uri = Uri.fromFile(destFile).toString(),
                        localPath = destFile.absolutePath,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.insertEvidenceAttachment(newEntity)
                    copiedCount++
                } else {
                    // No local file — copy via content URI
                    val result = saveAttachment(
                        context = context,
                        sourceUri = sourceUri,
                        observationId = toObsId,
                        caption = attachment.caption,
                        type = attachment.type
                    )
                    if (result.id > 0) copiedCount++
                }
            } catch (_: Exception) {
                // Skip attachments that can't be copied
            }
        }

        copiedCount
    }

    /**
     * Calculate total bytes used by all locally cached attachment files.
     * Mirrors the legacy [totalCacheSize].
     */
    suspend fun getStorageUsage(context: Context): Long = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "fieldmind/evidence")
        if (!dir.exists()) return@withContext 0L
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** Same as [getStorageUsage] but synchronous for legacy callers. */
    fun totalCacheSize(context: Context): Long {
        val dir = File(context.filesDir, "fieldmind/evidence")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Find all attachments whose observation no longer exists in the database.
     * Uses a dedicated DAO query that cross-references field_evidence_attachments
     * against field_observations to find orphaned records.
     *
     * To use, call with the DAO and collect the flow:
     * ```kotlin
     * FieldAttachmentManager.orphanedAttachments(dao).collect { orphans ->
     *     // handle orphans (e.g., ask user before deleting)
     * }
     * ```
     */
    fun orphanedAttachments(dao: FieldMindDao): Flow<List<EvidenceAttachmentEntity>> {
        return dao.observeOrphanedAttachments()
    }

    // ── Helper methods ──

    /** Detect the MIME type from a content URI, or infer from extension. */
    private fun detectMimeType(context: Context, uri: Uri): String {
        val fromResolver = context.contentResolver.getType(uri)
        if (!fromResolver.isNullOrBlank()) return fromResolver
        val path = uri.path ?: return "application/octet-stream"
        return when {
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".gif") -> "image/gif"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".m4a") || path.endsWith(".mp3") -> "audio/mpeg"
            path.endsWith(".wav") -> "audio/wav"
            path.endsWith(".pdf") -> "application/pdf"
            path.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    /** Map a MIME type to a file extension. */
    private fun mimeToExtension(mime: String): String = when {
        mime.startsWith("image/jpeg") -> ".jpg"
        mime.startsWith("image/png") -> ".png"
        mime.startsWith("image/gif") -> ".gif"
        mime.startsWith("image/webp") -> ".webp"
        mime.startsWith("video/") -> ".mp4"
        mime.startsWith("audio/mpeg") -> ".m4a"
        mime.startsWith("audio/wav") -> ".wav"
        mime == "application/pdf" -> ".pdf"
        mime == "text/plain" -> ".txt"
        else -> ".dat"
    }

    // ── Data classes ──

    data class AttachmentCopyResult(
        val uri: String,
        val copied: Boolean,
        val error: String? = null,
        val localPath: String? = null,
        val mimeType: String? = null
    )
}
