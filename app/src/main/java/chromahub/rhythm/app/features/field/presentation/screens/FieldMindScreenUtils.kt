package chromahub.rhythm.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chromahub.rhythm.app.features.field.presentation.components.ChoiceChips
import chromahub.rhythm.app.features.field.presentation.components.FieldMindIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.io.File
import kotlin.text.RegexOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val observationCategories = listOf("Bird", "Animal", "Insect", "Plant", "Rock", "Weather", "Water", "Human Behavior", "Reading Insight", "Other")
internal val confidenceOptions = listOf("Sure", "Guess", "Needs Verification")
internal val contextPresets = listOf("Calm", "Windy", "After rain", "Dawn", "Dusk", "Crowded", "Disturbed", "Healthy", "Stressed", "Needs follow-up")
internal val sourceTypes = listOf("Observation", "Reading", "Video", "Thought", "Discussion")
internal val questionStatuses = listOf("New", "Researching", "Tested", "Answered", "Abandoned")
internal val sourceLibraryTypes = listOf("Article", "Paper", "Book", "Video", "Website", "PDF", "Image", "Local document", "Note")
internal val readingStatuses = listOf("Not started", "In progress", "Read", "Skimmed", "To revisit")
internal val sourceImportanceLevels = listOf("Normal", "Important", "Critical")
internal val dataTools = listOf("Counter", "Measurement Log", "Checklist", "Event Log", "Weather Log", "Site Log", "Species Tracker", "Comparison Table")
internal val reportTypes = listOf("Summary", "Field Report", "Literature Review", "Project Draft", "Findings Note", "Final Report")
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

internal fun learnKindIcon(kind: String): MaterialSymbolIcon = when (kind.lowercase().trim()) {
    "article", "paper" -> FieldMindIcons.Article
    "book" -> FieldMindIcons.Book
    "video" -> FieldMindIcons.Play
    "course", "lesson" -> FieldMindIcons.School
    "tool" -> FieldMindIcons.Data
    "dataset", "data" -> FieldMindIcons.Data
    else -> FieldMindIcons.Book
}

internal fun youtubeVideoId(url: String): String? {
    val patterns = listOf(
        Regex("youtube\\.+?/watch\\?.*v=([^&]+)"),
        Regex("youtu\\.be/([^?&]+)"),
        Regex("youtube\\.+?/embed/([^?&]+)"),
        Regex("youtube\\.+?/shorts/([^?&]+)")
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

/** An inline form card that replaces dialog-based adding. Shows title, scrollable content, and save/cancel. */
@Composable
internal fun InlineFormCard(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Save",
    saveEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onSave, shape = RoundedCornerShape(16.dp), enabled = saveEnabled) { Text(saveLabel) }
            }
        }
    }
}

/** A dialog wrapper with a title, scrollable content, save/cancel action bar. */
@Composable
internal fun FormDialog(title: String, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                content()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.size(8.dp))
                    Button(onClick = onSave, shape = RoundedCornerShape(16.dp)) { Text("Save") }
                }
            }
        }
    }
}

/** A labeled chip-chooser row for dialogs. */
@Composable
internal fun FormChoice(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceChips(options, selected, onSelected = onSelected)
    }
}

/** A section label within a dialog form. */
@Composable
internal fun FormSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** A labeled capture step: an icon + title/subtitle header above its grouped inputs. */
@Composable
internal fun CaptureStep(title: String, subtitle: String, icon: MaterialSymbolIcon, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 18.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.padding(start = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

internal fun uriLooksImage(uri: String): Boolean = uri.contains(Regex("\\.(jpg|jpeg|png|webp|gif|heic|bmp)(\\?.*)?$", RegexOption.IGNORE_CASE))
internal fun uriLooksPdf(uri: String): Boolean = uri.contains(Regex("\\.pdf(\\?.*)?$", RegexOption.IGNORE_CASE))

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
