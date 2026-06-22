package fieldmind.research.app.features.field.presentation.screens

import android.content.Context
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Data models
// ══════════════════════════════════════════════════════════════════════

enum class BackupTab { EXPORT, IMPORT, BACKUP }

enum class ExportScopeType { ALL, PROJECTS, OBSERVATIONS, SOURCES, REPORTS, CUSTOM }

data class FormatOption(
    val name: String,
    val desc: String,
    val icon: MaterialSymbolIcon,
    val color: Color,
    val group: String = "document" // document, data, image, package
)

val exportFormats = listOf(
    FormatOption("JSON", "Structured archive with media", FieldMindIcons.Archive, Color(0xFF1F6B4C), "data"),
    FormatOption("CSV", "Tabular data for spreadsheets", FieldMindIcons.Data, Color(0xFF006D7A), "data"),
    FormatOption("Markdown", "Readable text for docs & notes", FieldMindIcons.Article, Color(0xFF2E7D32), "document"),
    FormatOption("HTML", "Print-ready web layout", FieldMindIcons.Article, Color(0xFF1565C0), "document"),
    FormatOption("PDF", "Portable document format", FieldMindIcons.Report, Color(0xFF1F6B4C), "document"),
    FormatOption(".fieldmind", "Package with images & encryption", FieldMindIcons.Archive, Color(0xFF1F6B4C), "package"),
    FormatOption(".zip", "Compressed archive for smaller backups", FieldMindIcons.Archive, Color(0xFFD97706), "compressed")
)

data class ExportRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val format: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val entityCounts: Map<String, Int> = emptyMap(),
    val exportedAt: Long = System.currentTimeMillis(),
    val destination: String = "",
    val success: Boolean = true
)

// ══════════════════════════════════════════════════════════════════════
//  Export History Store (persisted in SharedPreferences)
// ══════════════════════════════════════════════════════════════════════

class ExportHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("fieldmind_export_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<ExportRecord>>() {}.type

    fun load(): List<ExportRecord> {
        val json = prefs.getString("records", "[]") ?: "[]"
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun add(record: ExportRecord) {
        val records = load().toMutableList()
        records.add(0, record)
        if (records.size > 50) records.removeAt(records.lastIndex)
        prefs.edit().putString("records", gson.toJson(records)).apply()
    }

    fun remove(id: String) {
        val records = load().toMutableList()
        records.removeAll { it.id == id }
        prefs.edit().putString("records", gson.toJson(records)).apply()
    }

    fun clear() {
        prefs.edit().remove("records").apply()
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Utility functions
// ══════════════════════════════════════════════════════════════════════

fun entityIconForLabel(label: String): MaterialSymbolIcon = when (label) {
    "Observations" -> FieldMindIcons.Observation
    "Notes" -> FieldMindIcons.Note
    "Questions" -> FieldMindIcons.Question
    "Hypotheses" -> FieldMindIcons.Hypothesis
    "Projects" -> FieldMindIcons.Project
    "Sources" -> FieldMindIcons.Source
    "Data Records" -> FieldMindIcons.Data
    "Reports" -> FieldMindIcons.Report
    "Flashcards" -> FieldMindIcons.Flashcard
    else -> FieldMindIcons.Info
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes.toFloat() / (1024 * 1024))
}

fun backupDirectory(context: Context): File = File(context.filesDir, "fieldmind/backups").apply { mkdirs() }

fun lastBackupSummary(context: Context): String {
    val latest = backupDirectory(context).listFiles { file -> file.isFile && (file.extension == "json" || file.extension == "encrypted" || file.extension == "fieldmind") }
        ?.maxByOrNull { it.lastModified() }
    return latest?.let {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(it.lastModified()))
    } ?: "Never"
}

/**
 * Decode a base64 data URI (e.g., "data:image/jpeg;base64,/9j...") back to raw bytes.
 * Used for embedding images in PDF exports.
 */
fun decodeBase64FromDataUri(dataUri: String): ByteArray? {
    return try {
        val commaIdx = dataUri.indexOf(',')
        if (commaIdx < 0) return null
        val base64 = dataUri.substring(commaIdx + 1)
        android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
    } catch (e: Exception) { null }
}

/**
 * Create a file in a SAF document tree URI.
 */
fun createFileInTree(context: Context, treeUri: android.net.Uri, mimeType: String, displayName: String): android.net.Uri {
    val resolver = context.contentResolver
    val treeDocumentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
    val parentDocumentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
    return android.provider.DocumentsContract.createDocument(resolver, parentDocumentUri, mimeType, displayName)
        ?: throw java.io.IOException("Could not create $displayName in selected folder")
}

/**
 * Estimate export file size based on format and entity counts.
 */
fun estimateExportSize(format: String, obsCount: Int, noteCount: Int, projCount: Int, srcCount: Int): String {
    val baseSize = 50.0
    val obsSize = obsCount * 2.5
    val noteSize = noteCount * 1.8
    val projSize = projCount * 3.0
    val srcSize = srcCount * 4.5
    val totalKB = when (format.lowercase()) {
        "json" -> baseSize + obsSize + noteSize + projSize + srcSize
        "csv" -> (baseSize + obsSize + noteSize) * 0.6
        "markdown" -> obsSize * 1.2 + noteSize
        "html" -> baseSize + obsSize + noteSize + (projSize * 0.8)
        "pdf" -> (baseSize + obsSize) * 1.5
        ".fieldmind" -> (baseSize + obsSize + noteSize + projSize + srcSize) * 1.3
        else -> baseSize + obsSize
    }
    return when {
        totalKB < 1024 -> "%.0f KB".format(totalKB)
        else -> "%.1f MB".format(totalKB / 1024)
    }
}
