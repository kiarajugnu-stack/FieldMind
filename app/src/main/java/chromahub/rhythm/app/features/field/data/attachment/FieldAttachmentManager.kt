package fieldmind.research.app.features.field.data.attachment

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Manages offline copies of evidence attachments.
 * Copies selected files to app-private storage so they survive
 * permission revocation or URI provider unavailability.
 */
object FieldAttachmentManager {

    private const val MAX_ATTACHMENT_SIZE = 25L * 1024 * 1024 // 25 MB

    /**
     * Copy an attachment to app-private storage.
     * Returns the local URI string if successful, or the original URI if the file
     * exceeds the size limit or can't be read.
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

    /** Clean up all cached attachments for an observation. */
    fun cleanObservationAttachments(context: Context, observationId: Long) {
        val dir = File(context.filesDir, "fieldmind/evidence/$observationId")
        if (dir.exists()) dir.deleteRecursively()
    }

    /** Total size of all locally cached attachments. */
    fun totalCacheSize(context: Context): Long {
        val dir = File(context.filesDir, "fieldmind/evidence")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    data class AttachmentCopyResult(
        val uri: String,
        val copied: Boolean,
        val error: String? = null,
        val localPath: String? = null,
        val mimeType: String? = null
    )
}
