package fieldmind.research.app.features.field.presentation.screens

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
import fieldmind.research.app.features.field.presentation.components.ChoiceChips
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.io.File
import kotlin.text.RegexOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class ObservationCategoryField(val key: String, val label: String, val hint: String = "")
internal data class ObservationCategoryDefinition(
    val label: String,
    val iconName: String,
    val prompt: String,
    val defaultTags: List<String>,
    val fields: List<ObservationCategoryField>
)

internal val observationCategoryDefinitions = listOf(
    ObservationCategoryDefinition("Bird", "raven", "What species, count, calls, and movement did you directly observe?", listOf("bird", "behavior"), listOf(ObservationCategoryField("species", "Species"), ObservationCategoryField("count", "Count"), ObservationCategoryField("behavior", "Behavior"), ObservationCategoryField("call", "Call / sound"), ObservationCategoryField("flightDirection", "Flight direction"), ObservationCategoryField("habitat", "Habitat"))),
    ObservationCategoryDefinition("Mammal", "pets", "Track body signs, behavior, group size, and habitat without guessing intent.", listOf("mammal", "wildlife"), listOf(ObservationCategoryField("species", "Species"), ObservationCategoryField("count", "Count"), ObservationCategoryField("tracks", "Tracks / sign"), ObservationCategoryField("behavior", "Behavior"), ObservationCategoryField("habitat", "Habitat"))),
    ObservationCategoryDefinition("Insect", "bug_report", "Capture life stage, host plant, count, behavior, and microhabitat.", listOf("insect"), listOf(ObservationCategoryField("species", "Species / morphotype"), ObservationCategoryField("count", "Count"), ObservationCategoryField("lifeStage", "Life stage"), ObservationCategoryField("host", "Host plant / substrate"), ObservationCategoryField("behavior", "Behavior"))),
    ObservationCategoryDefinition("Plant", "local_florist", "Describe visible structures, growth stage, substrate, and measurements.", listOf("plant"), listOf(ObservationCategoryField("growthStage", "Growth stage"), ObservationCategoryField("flowerFruit", "Flower / fruit"), ObservationCategoryField("leafShape", "Leaf shape"), ObservationCategoryField("height", "Height"), ObservationCategoryField("substrate", "Substrate"))),
    ObservationCategoryDefinition("Fungi", "psychiatry", "Log cap/gill/stem features, substrate, cluster pattern, and moisture.", listOf("fungi"), listOf(ObservationCategoryField("cap", "Cap"), ObservationCategoryField("gills", "Gills / pores"), ObservationCategoryField("stem", "Stem"), ObservationCategoryField("substrate", "Substrate"), ObservationCategoryField("cluster", "Cluster pattern"))),
    ObservationCategoryDefinition("Rock/Geology", "landscape", "Record texture, color, hardness, grain size, layering, and context.", listOf("geology", "rock"), listOf(ObservationCategoryField("texture", "Texture"), ObservationCategoryField("color", "Color"), ObservationCategoryField("hardness", "Hardness"), ObservationCategoryField("grainSize", "Grain size"), ObservationCategoryField("context", "Location context"))),
    ObservationCategoryDefinition("Water", "water_drop", "Log flow, clarity, depth estimate, surface conditions, and nearby evidence.", listOf("water"), listOf(ObservationCategoryField("flow", "Flow"), ObservationCategoryField("clarity", "Clarity"), ObservationCategoryField("depth", "Depth estimate"), ObservationCategoryField("surface", "Surface condition"), ObservationCategoryField("odor", "Odor"))),
    ObservationCategoryDefinition("Weather", "partly_cloudy_day", "Pair measured conditions with sky, visibility, precipitation, and wind evidence.", listOf("weather"), listOf(ObservationCategoryField("temperature", "Temperature"), ObservationCategoryField("wind", "Wind"), ObservationCategoryField("cloudCover", "Cloud cover"), ObservationCategoryField("precipitation", "Precipitation"), ObservationCategoryField("visibility", "Visibility"))),
    ObservationCategoryDefinition("Soil", "terrain", "Note moisture, texture, color, compaction, organisms, and nearby vegetation.", listOf("soil"), listOf(ObservationCategoryField("moisture", "Moisture"), ObservationCategoryField("texture", "Texture"), ObservationCategoryField("color", "Color"), ObservationCategoryField("compaction", "Compaction"), ObservationCategoryField("organisms", "Organisms"))),
    ObservationCategoryDefinition("Human Behavior", "groups", "Describe observable actions, setting, timing, and interaction patterns respectfully.", listOf("behavior"), listOf(ObservationCategoryField("setting", "Setting"), ObservationCategoryField("actors", "Actors / group size"), ObservationCategoryField("action", "Observed action"), ObservationCategoryField("duration", "Duration"), ObservationCategoryField("context", "Context"))),
    ObservationCategoryDefinition("Astronomy/Sky", "routine", "Capture sky condition, direction, altitude estimate, timing, and equipment.", listOf("sky", "astronomy"), listOf(ObservationCategoryField("object", "Object"), ObservationCategoryField("direction", "Direction"), ObservationCategoryField("altitude", "Altitude estimate"), ObservationCategoryField("skyCondition", "Sky condition"), ObservationCategoryField("equipment", "Equipment"))),
    ObservationCategoryDefinition("Reading Insight", "menu_book", "Link a source insight to evidence, question, and next action.", listOf("reading", "source"), listOf(ObservationCategoryField("source", "Source"), ObservationCategoryField("claim", "Claim"), ObservationCategoryField("evidence", "Evidence"), ObservationCategoryField("question", "Question generated"))),
    ObservationCategoryDefinition("Experiment", "science", "Track variable, method, trial, result, and control notes.", listOf("experiment"), listOf(ObservationCategoryField("variable", "Variable"), ObservationCategoryField("method", "Method"), ObservationCategoryField("trial", "Trial"), ObservationCategoryField("result", "Result"), ObservationCategoryField("control", "Control"))),
    ObservationCategoryDefinition("Site Visit", "pin_drop", "Summarize site purpose, transect/route, conditions, samples, and follow-ups.", listOf("site", "visit"), listOf(ObservationCategoryField("site", "Site"), ObservationCategoryField("route", "Route / transect"), ObservationCategoryField("conditions", "Conditions"), ObservationCategoryField("samples", "Samples"), ObservationCategoryField("followUp", "Follow-up"))),
    ObservationCategoryDefinition("Other", "category", "Use a facts-first template for anything that does not fit yet.", listOf("field-note"), listOf(ObservationCategoryField("detail", "Structured detail"), ObservationCategoryField("measurement", "Measurement"), ObservationCategoryField("next", "Next check")))
)
internal val observationCategories = observationCategoryDefinitions.map { it.label }

// Expanded category list matching the spec
internal val expandedObservationCategories = listOf(
    "Bird", "Mammal", "Reptile", "Amphibian",
    "Insect", "Arachnid", "Fish", "Plant",
    "Fungus", "Habitat", "Track/Sign", "Nest",
    "Other"
)

internal val confidenceOptions = listOf("Certain", "Likely", "Unsure")

// Expanded confidence per spec: Certain, Very Likely, Probable, Unsure, Needs Review
internal val expandedConfidenceOptions = listOf(
    "Certain", "Very Likely", "Probable", "Unsure", "Needs Review"
)

// Behavior dropdown per spec
internal val behaviorOptions = listOf(
    "Feeding", "Hunting", "Nesting", "Resting",
    "Flying", "Walking", "Swimming", "Calling",
    "Mating", "Territorial", "Social Interaction", "Other"
)

// Life Stage dropdown per spec
internal val lifeStageOptions = listOf(
    "Egg", "Juvenile", "Subadult", "Adult", "Unknown"
)

// Sex options per spec
internal val sexOptions = listOf(
    "Male", "Female", "Unknown"
)

// Habitat Type dropdown per spec
internal val habitatTypeOptions = listOf(
    "Forest", "Wetland", "Grassland", "Urban",
    "Agricultural", "Desert", "Coastal", "Freshwater", "Marine"
)

// Observation Quality dropdown per spec
internal val observationQualityOptions = listOf(
    "Excellent", "Good", "Fair", "Poor"
)

// Weather condition options for manual override
internal val weatherConditionOptions = listOf(
    "Auto", "Clear", "Partly Cloudy", "Overcast",
    "Fog", "Drizzle", "Rain", "Heavy Rain",
    "Snow", "Thunderstorm", "Windy"
)

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

/** An inline form card that replaces dialog-based adding. Parent LazyColumns own vertical scrolling. */
@Composable
internal fun InlineFormCard(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Save",
    saveEnabled: Boolean = true,
    requiredFields: List<fieldmind.research.app.features.field.presentation.components.RequiredFieldState> = emptyList(),
    content: @Composable ColumnScope.() -> Unit
) {
    val allValid = requiredFields.isEmpty() || requiredFields.all { it.isValid }
    val effectiveEnabled = saveEnabled && allValid
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onSave, shape = RoundedCornerShape(16.dp), enabled = effectiveEnabled) { Text(saveLabel) }
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

internal fun durableEvidenceAttachment(context: android.content.Context, type: String, uri: android.net.Uri, caption: String): fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment {
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
        return fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment(type, android.net.Uri.fromFile(local).toString(), caption, localPath = local.absolutePath, mimeType = mime)
    }
    return fieldmind.research.app.features.field.presentation.viewmodel.DraftEvidenceAttachment(type, uri.toString(), caption, mimeType = context.contentResolver.getType(uri))
}
