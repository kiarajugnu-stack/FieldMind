package fieldmind.research.app.features.field.data.export

import android.content.Context
import android.net.Uri
import fieldmind.research.app.features.field.data.database.entity.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages FieldMind data — archive JSON + media attachments — into a portable
 * .fieldmind ZIP archive with a metadata manifest.
 *
 * Package structure:
 *   archive.json       — Full JSON archive (same format as JSON export)
 *   manifest.json      — Package metadata (version, date, checksums, media count)
 *   media/             — Copied media files, organized by entity type & ID
 *     observations/{id}/{filename}
 *     notes/{id}/{filename}
 *     projects/{id}/{filename}
 *     sources/{id}/{filename}
 */
object FieldMindExportMediaPacker {

    data class MediaEntry(
        val entityType: String,      // "observation", "note", "project", "source"
        val entityId: Long,
        val uri: String,
        val fileName: String,
        val mimeType: String = "application/octet-stream",
        val caption: String = ""
    )

    data class PackageResult(
        val packageFile: File,
        val mediaCount: Int,
        val totalSizeBytes: Long,
        val checksum: String,
        val manifest: String,
        val mediaManifestJson: String = ""
    )

    // ── Estimate size before building ──

    /**
     * Roughly estimate the total package size (media + JSON) in bytes.
     * Runs locally and is fast — queries content resolver for file sizes.
     */
    fun estimatePackageSize(
        context: Context,
        archiveJson: String,
        observations: List<ObservationEntity>,
        notes: List<NoteEntity>,
        projects: List<ProjectEntity>,
        sources: List<SourceEntity>,
        attachments: Map<Long, List<EvidenceAttachmentEntity>>
    ): Long {
        val jsonSize = archiveJson.toByteArray().size.toLong()
        val mediaSize = collectMediaEntries(context, observations, notes, projects, sources, attachments)
            .sumOf { entry -> resolveFileSize(context, entry.uri) }
        return jsonSize + mediaSize
    }

    /**
     * Collect all media attachment entries from all entity types.
     * Returns a flat list of [MediaEntry] with resolved file names.
     */
    fun collectMediaEntries(
        context: Context,
        observations: List<ObservationEntity>,
        notes: List<NoteEntity>,
        projects: List<ProjectEntity>,
        sources: List<SourceEntity>,
        attachments: Map<Long, List<EvidenceAttachmentEntity>>
    ): List<MediaEntry> {
        val entries = mutableListOf<MediaEntry>()

        // ── Observations: evidence attachments from Room ──
        observations.forEach { obs ->
            attachments[obs.id]?.forEach { att ->
                if (att.uri.isNotBlank() && att.status != "Deleted") {
                    val fileName = deriveFileName(context, att.uri, att.caption)
                    entries.add(
                        MediaEntry(
                            entityType = "observation",
                            entityId = obs.id,
                            uri = att.localPath ?: att.uri,
                            fileName = fileName,
                            mimeType = att.type,
                            caption = att.caption
                        )
                    )
                }
            }
        }

        // ── Notes: attachmentUris is newline-delimited "type|caption|uri" ──
        notes.forEach { note ->
            if (note.attachmentUris.isNotBlank()) {
                note.attachmentUris.split("\n").filter { it.isNotBlank() }.forEach { line ->
                    val parts = line.split("|")
                    if (parts.size >= 3) {
                        val uri = parts[2].trim()
                        if (uri.isNotBlank()) {
                            val caption = parts.getOrElse(1) { "" }
                            entries.add(
                                MediaEntry(
                                    entityType = "note",
                                    entityId = note.id,
                                    uri = uri,
                                    fileName = deriveFileName(context, uri, caption),
                                    caption = caption
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Projects: attachmentUris is comma-separated URIs ──
        projects.forEach { project ->
            if (project.attachmentUris.isNotBlank()) {
                project.attachmentUris.split(",").filter { it.isNotBlank() }.forEach { uri ->
                    val trimmed = uri.trim()
                    entries.add(
                        MediaEntry(
                            entityType = "project",
                            entityId = project.id,
                            uri = trimmed,
                            fileName = deriveFileName(context, trimmed, "")
                        )
                    )
                }
            }
        }

        // ── Sources: fileUri ──
        sources.forEach { source ->
            if (source.fileUri.isNotBlank()) {
                entries.add(
                    MediaEntry(
                        entityType = "source",
                        entityId = source.id,
                        uri = source.fileUri,
                        fileName = deriveFileName(context, source.fileUri, source.title)
                    )
                )
            }
        }

        return entries
    }

    /**
     * Build the .fieldmind ZIP package.
     *
     * @param context Android context for content resolver access
     * @param archiveJson The full JSON archive string
     * @param observations Observations with their attachments
     * @param notes Notes with their attachment URIs
     * @param projects Projects with their attachment URIs
     * @param sources Sources with their file URIs
     * @param attachments Map of observationId -> list of EvidenceAttachmentEntity
     * @param outputDir Directory to write the package file to
     * @return [PackageResult] with file, count, size, checksum, and manifest JSON string
     */
    fun buildPackage(
        context: Context,
        archiveJson: String,
        observations: List<ObservationEntity>,
        notes: List<NoteEntity>,
        projects: List<ProjectEntity>,
        sources: List<SourceEntity>,
        attachments: Map<Long, List<EvidenceAttachmentEntity>>,
        outputDir: File
    ): PackageResult {
        val dateStamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
        val packageFile = File(outputDir, "fieldmind-package-$dateStamp.fieldmind")
        val mediaEntries = collectMediaEntries(context, observations, notes, projects, sources, attachments)
        val digest = MessageDigest.getInstance("SHA-256")

        var checksum: String = ""
        var manifest: String = ""
        var mediaManifestJson: String = ""

        ZipOutputStream(FileOutputStream(packageFile)).use { zos ->

            // ── 1. Write archive.json ──
            zos.putNextEntry(ZipEntry("archive.json"))
            val jsonBytes = archiveJson.toByteArray()
            zos.write(jsonBytes)
            digest.update(jsonBytes)
            zos.closeEntry()

            // ── 2. Write media files ──
            var mediaCopied = 0
            mediaEntries.forEach { entry ->
                try {
                    val inputStream = try {
                        // Try content resolver first (works for content:// URIs)
                        context.contentResolver.openInputStream(Uri.parse(entry.uri))
                    } catch (_: Exception) {
                        // Fallback: read directly from file path for schemeless file URIs
                        // (localPath on EvidenceAttachmentEntity stores raw file paths).
                        // ContentResolver throws IllegalArgumentException for URIs without a scheme,
                        // so we catch all exceptions and try direct file access.
                        java.io.File(entry.uri).inputStream()
                    }

                    if (inputStream != null) {
                        val entryPath = "media/${entry.entityType}s/${entry.entityId}/${entry.fileName}"
                        zos.putNextEntry(ZipEntry(entryPath))

                        BufferedInputStream(inputStream).use { bis ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (bis.read(buffer).also { bytesRead = it } != -1) {
                                zos.write(buffer, 0, bytesRead)
                                digest.update(buffer, 0, bytesRead)
                            }
                        }
                        zos.closeEntry()
                        mediaCopied++
                    }
                } catch (_: Exception) {
                    // Skip media files that can't be read (permission revoked, missing, etc.)
                }
            }

            // ── 3. Build and write media-manifest.json ──
            mediaManifestJson = buildMediaManifestJson(mediaEntries)
            zos.putNextEntry(ZipEntry("media-manifest.json"))
            val mediaManifestBytes = mediaManifestJson.toByteArray()
            zos.write(mediaManifestBytes)
            zos.closeEntry()

            // ── 4. Build and write manifest.json ──
            checksum = digest.digest().joinToString("") { "%02x".format(it) }
            manifest = buildManifestJson(
                jsonSize = jsonBytes.size.toLong(),
                mediaCount = mediaCopied,
                totalMediaSize = packageFile.length(),
                checksum = checksum,
                entityCounts = mapOf(
                    "observations" to observations.size,
                    "notes" to notes.size,
                    "projects" to projects.size,
                    "sources" to sources.size
                )
            )

            zos.putNextEntry(ZipEntry("manifest.json"))
            val manifestBytes = manifest.toByteArray()
            zos.write(manifestBytes)
            zos.closeEntry()
        }

        val finalSize = packageFile.length()

        return PackageResult(
            packageFile = packageFile,
            mediaCount = mediaEntries.size,
            totalSizeBytes = finalSize,
            checksum = checksum,
            manifest = manifest,
            mediaManifestJson = mediaManifestJson
        )
    }

    // ── Manifest builder ──

    private fun buildManifestJson(
        jsonSize: Long,
        mediaCount: Int,
        totalMediaSize: Long,
        checksum: String,
        entityCounts: Map<String, Int>
    ): String = JSONObject().apply {
        put("format", "fieldmind-package-v1")
        put("exportedAt", System.currentTimeMillis())
        put("version", 1)
        put("jsonSizeBytes", jsonSize)
        put("mediaCount", mediaCount)
        put("totalSizeBytes", totalMediaSize)
        put("checksumSha256", checksum)
        put("entityCounts", JSONObject(entityCounts))
        put("appName", "FieldMind")
        put("appVersion", "4.3.0")
    }.toString(2)

    // ── Helpers ──

    private fun deriveFileName(context: Context, uri: String, caption: String): String {
        // Try to get display name from content resolver
        try {
            val cursor = context.contentResolver.query(
                Uri.parse(uri), null, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) return sanitizeFileName(name)
                    }
                }
            }
        } catch (_: Exception) { }

        // Fall back to caption or timestamp
        val base = caption.ifBlank { "attachment" }
        val ext = when {
            uri.contains("image/") || uri.endsWith(".jpg") || uri.endsWith(".png") -> ".jpg"
            uri.contains("video/") || uri.endsWith(".mp4") -> ".mp4"
            uri.contains("audio/") || uri.endsWith(".m4a") -> ".m4a"
            uri.endsWith(".pdf") -> ".pdf"
            else -> ".dat"
        }
        return sanitizeFileName("${base}_${System.currentTimeMillis()}$ext")
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(120)
            .ifBlank { "attachment" }
    }

    private fun buildMediaManifestJson(entries: List<MediaEntry>): String {
        val arr = org.json.JSONArray()
        entries.forEach { entry ->
            arr.put(org.json.JSONObject().apply {
                put("entityType", entry.entityType)
                put("entityId", entry.entityId)
                put("fileName", entry.fileName)
                put("mimeType", entry.mimeType)
                put("caption", entry.caption)
            })
        }
        return arr.toString()
    }

    private fun resolveFileSize(context: Context, uri: String): Long {
        return try {
            context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { fd ->
                fd.statSize
            } ?: 0L
        } catch (_: Exception) { 0L }
    }

    // ── Package parsing (for import) ──

    /**
     * Parse a .fieldmind package and extract the archive JSON and media manifest.
     * Returns null if the package is invalid.
     */
    data class ExtractedPackage(
        val archiveJson: String,
        val manifest: String,
        val mediaFiles: List<MediaEntry>,
        val tempDir: File
    )

    /**
     * Extract a .fieldmind package to a temp directory and return the
     * archive JSON string and list of extracted media files.
     *
     * After import is complete, call [cleanupExtractedPackage] to delete temp files.
     */
    fun extractPackage(context: Context, packageUri: Uri): ExtractedPackage? {
        return try {
            val tempDir = File(context.cacheDir, "fieldmind_extracted_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val inputStream = context.contentResolver.openInputStream(packageUri) ?: return null
            val javaZip = java.util.zip.ZipInputStream(inputStream)
            val mediaFiles = mutableListOf<MediaEntry>()
            var archiveJson = ""
            var manifest = ""

            // First pass: read archive.json, manifest.json, and all media file entries
            // Also read media-manifest.json for metadata enrichment
            val rawMediaEntries = mutableMapOf<String, File>()
            var mediaManifestJsonStr = ""

            javaZip.use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    val content = zip.readBytes()

                    when {
                        entryName == "archive.json" -> {
                            archiveJson = String(content, Charsets.UTF_8)
                        }
                        entryName == "manifest.json" -> {
                            manifest = String(content, Charsets.UTF_8)
                        }
                        entryName == "media-manifest.json" -> {
                            mediaManifestJsonStr = String(content, Charsets.UTF_8)
                        }
                        entryName.startsWith("media/") -> {
                            // Extract media file to temp dir
                            val targetFile = File(tempDir, entryName)
                            targetFile.parentFile?.mkdirs()
                            targetFile.writeBytes(content)
                            rawMediaEntries[entryName] = targetFile
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // Build a lookup map from media-manifest.json if present
            val manifestLookup = mutableMapOf<String, Pair<String, String>>() // entryPath -> (mimeType, caption)
            if (mediaManifestJsonStr.isNotBlank()) {
                try {
                    val arr = org.json.JSONArray(mediaManifestJsonStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entityType = obj.optString("entityType", "")
                        val entityId = obj.optLong("entityId", 0L)
                        val fileName = obj.optString("fileName", "")
                        val mimeType = obj.optString("mimeType", "application/octet-stream")
                        val caption = obj.optString("caption", "")
                        val entryPath = "media/${entityType}s/${entityId}/${fileName}"
                        manifestLookup[entryPath] = mimeType to caption
                    }
                } catch (_: Exception) { }
            }

            // Build MediaEntry list from extracted files, enriched with manifest metadata
            rawMediaEntries.forEach { (entryName, targetFile) ->
                val pathParts = entryName.split("/")
                if (pathParts.size >= 4) {
                    val entityType = pathParts[1].removeSuffix("s") // observations -> observation
                    val entityId = pathParts[2].toLongOrNull() ?: 0L
                    val fileName = pathParts.drop(3).joinToString("/")
                    val (mimeType, caption) = manifestLookup[entryName] ?: ("application/octet-stream" to "")
                    mediaFiles.add(
                        MediaEntry(
                            entityType = entityType,
                            entityId = entityId,
                            uri = Uri.fromFile(targetFile).toString(),
                            fileName = fileName,
                            mimeType = mimeType,
                            caption = caption
                        )
                    )
                }
            }

            if (archiveJson.isBlank()) {
                tempDir.deleteRecursively()
                return null
            }

            ExtractedPackage(
                archiveJson = archiveJson,
                manifest = manifest,
                mediaFiles = mediaFiles,
                tempDir = tempDir
            )
        } catch (_: Exception) { null }
    }

    /**
     * Clean up temp files from an extracted package.
     */
    fun cleanupExtractedPackage(extracted: ExtractedPackage) {
        try {
            extracted.tempDir.deleteRecursively()
        } catch (_: Exception) { }
    }
}
