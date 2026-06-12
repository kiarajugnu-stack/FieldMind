package chromahub.rhythm.app.features.field.presentation.screens

import chromahub.rhythm.app.features.field.presentation.components.FieldMindIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val observationCategories = listOf("Bird", "Animal", "Insect", "Plant", "Rock", "Weather", "Water", "Human Behavior", "Reading Insight", "Other")
internal val confidenceOptions = listOf("Sure", "Guess", "Needs Verification")
private val contextPresets = listOf("Calm", "Windy", "After rain", "Dawn", "Dusk", "Crowded", "Disturbed", "Healthy", "Stressed", "Needs follow-up")
private val sourceTypes = listOf("Observation", "Reading", "Video", "Thought", "Discussion")
private val questionStatuses = listOf("New", "Researching", "Tested", "Answered", "Abandoned")
private val sourceLibraryTypes = listOf("Article", "Paper", "Book", "Video", "Website", "PDF", "Image", "Local document", "Note")
private val readingStatuses = listOf("Not started", "In progress", "Read", "Skimmed", "To revisit")
private val sourceImportanceLevels = listOf("Normal", "Important", "Critical")
private val dataTools = listOf("Counter", "Measurement Log", "Checklist", "Event Log", "Weather Log", "Site Log", "Species Tracker", "Comparison Table")
private val reportTypes = listOf("Summary", "Field Report", "Literature Review", "Project Draft", "Findings Note", "Final Report")
internal val learningModules = listOf(
    "Beginner" to listOf("Scientific thinking", "Observation", "Note-taking", "Identifying bias", "Basic biology", "Basic geology", "Reading graphs", "Asking testable questions", "Variables", "Simple data collection"),
    "Intermediate" to listOf("Research design", "Sampling", "Comparison", "Classification", "Literature review", "Writing summaries", "Data interpretation"),
    "Advanced" to listOf("Proposal writing", "Structured projects", "Analysis", "Citations", "Presentation", "Field methods", "Ethics")
)


internal fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

internal fun recentRelativeTime(time: Long): String {
    val diff = System.currentTimeMillis() - time
    val mins = diff / 60_000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        mins < 1440 * 7 -> "${mins / 1440}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
    }
}

internal fun learnKindIcon(kind: String): MaterialSymbolIcon = when (kind.lowercase()) {
    "article" -> FieldMindIcons.Article
    "book" -> FieldMindIcons.Book
    "video" -> FieldMindIcons.Video
    "course" -> FieldMindIcons.School
    "paper" -> FieldMindIcons.Paper
    "tool" -> FieldMindIcons.Data
    else -> FieldMindIcons.Book
}

internal fun youtubeVideoId(url: String): String? {
    val patterns = listOf(
        Regex("youtube\..+?/watch\?.*v=([^&]+)"),
        Regex("youtu\.be/([^?&]+)"),
        Regex("youtube\..+?/embed/([^?&]+)"),
        Regex("youtube\..+?/shorts/([^?&]+)")
    )
    for (p in patterns) p.find(url)?.let { return it.groupValues[1] }
    return null
}

internal fun createFieldMindFile(context: android.content.Context, prefix: String, suffix: String): File {
    val dir = File(context.filesDir, "fieldmind")
    if (!dir.exists()) dir.mkdirs()
    return File(dir, "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}$suffix")
}

internal fun createFieldMindFileUri(context: android.content.Context, prefix: String, suffix: String): android.net.Uri {
    val file = createFieldMindFile(context, prefix, suffix)
    return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fieldmind.fileprovider", file)
}

internal fun durableEvidenceAttachment(context: android.content.Context, type: String, uri: android.net.Uri, caption: String): chromahub.rhythm.app.features.field.presentation.viewmodel.DraftEvidenceAttachment {
    val input = context.contentResolver.openInputStream(uri)
    if (input != null) {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val ext = when {
            mime.startsWith("image/") -> "." + mime.substringAfter("image/").substringBefore(";").ifBlank { "jpg" }
            mime.startsWith("video/") -> "." + mime.substringAfter("video/").substringBefore(";").ifBlank { "mp4" }
            mime.startsWith("audio/") -> "." + mime.substringAfter("audio/").substringBefore(";").ifBlank { "m4a" }
            mime == "application/pdf" -> ".pdf"
            else -> ".dat"
        }
        val local = createFieldMindFile(context, type.lowercase().replace(" ", "-"), ext)
        input.use { i -> java.io.FileOutputStream(local).use { o -> i.copyTo(o) } }
        return chromahub.rhythm.app.features.field.presentation.viewmodel.DraftEvidenceAttachment(type, android.net.Uri.fromFile(local).toString(), caption, localPath = local.absolutePath, mimeType = mime)
    }
    return chromahub.rhythm.app.features.field.presentation.viewmodel.DraftEvidenceAttachment(type, uri.toString(), caption, mimeType = context.contentResolver.getType(uri))
}
