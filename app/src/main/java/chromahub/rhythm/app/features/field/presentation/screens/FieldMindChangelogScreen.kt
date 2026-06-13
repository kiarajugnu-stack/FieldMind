package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.FieldScreenHeader
import fieldmind.research.app.features.field.presentation.components.InfoChip
import fieldmind.research.app.shared.presentation.components.icons.Icon

internal data class FieldMindChangelogEntry(
    val version: String,
    val date: String,
    val title: String,
    val importance: String,
    val tags: List<String>,
    val sections: List<Pair<String, List<String>>>
)

private val fieldMindChangelog = listOf(
    FieldMindChangelogEntry(
        version = "0.9.0-field-research-dashboard",
        date = "2026-06-13",
        title = "Research Dashboard & Interactive Data Tools",
        importance = "Major",
        tags = listOf("Dashboard", "Charts", "Data Table", "Analytics", "Insights"),
        sections = listOf(
            "Research Dashboard" to listOf(
                "Insights screen completely redesigned into a 9-section Research Dashboard with profile card, performance metrics, time-series analytics, category/tag analysis, knowledge graph timeline, research health score, weather correlation, achievements, and data records table.",
                "New calendar heatmap shows daily observation activity across 12 months (GitHub-style contribution grid).",
                "New radar/spider chart compares observation categories across multiple dimensions.",
                "New tag co-occurrence matrix reveals which tags appear together most often.",
                "New activity-by-hour and day-of-week charts show when you observe most.",
                "New moving average chart with 7-day rolling overlay on daily counts.",
                "New weather correlation scatter plot with trend line (requires GPS + weather on capture).",
                "New data quality meter scores research health across 5 dimensions (evidence, questions, hypotheses, tags, GPS).",
                "New network graph timeline visualizes how your knowledge graph evolved over time with an interactive slider.",
                "15 achievements with progress tracking, tiered unlocks, and snackbar celebration."
            ),
            "Interactive Data Table" to listOf(
                "New FieldDataTable composable with sortable columns, search across all fields, column-specific filters, numerical aggregates (count/sum/avg), pivot table view, CSV export, and row selection.",
                "Data records now display in a spreadsheet-style table within the Research Dashboard."
            ),
            "Extended Chart Library" to listOf(
                "8 new Canvas-based chart composables: CalendarHeatmap, RadarChart, TagCoOccurrenceMatrix, ActivityByHourChart, DayOfWeekChart, MovingAverageChart, WeatherCorrelationChart, DataQualityMeter.",
                "All charts are fully offline, zero external dependencies, and built entirely with Compose Canvas."
            ),
            "Observation Improvements" to listOf("Observations now support structured category-specific fields (JSON), live stopwatch tracking, manual duration override, change timing markers, and context presets.", "Field mode buttons allow one-tap observation per category with undo support.")
        )
    ),
    FieldMindChangelogEntry(
        version = "0.8.0-field-redesign",
        date = "2026-06-13",
        title = "Immersive research workspace foundation",
        importance = "Major",
        tags = listOf("Camera", "Observations", "Projects", "Migration"),
        sections = listOf(
            "Capture" to listOf("Camera V2 now opens as an immersive full-screen surface.", "Quick Snap can attach GPS and weather metadata when permissions and settings allow it."),
            "Research records" to listOf("Observations gained stopwatch, manual duration, change timing, and structured category fields.", "Projects, reports, and data records gained metadata for connections, attachments, templates, and chart preferences."),
            "Data safety" to listOf("The FieldMind database now uses an explicit Room migration instead of destructive migration for the new schema."),
            "Navigation" to listOf("Bottom navigation icons are larger and use a subtle spring motion for a more tactile feel.")
        )
    )
)

@Composable
fun FieldMindChangelogScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { FieldScreenHeader("What’s new", "FieldMind-specific changes separate from the wider Rhythm app.", icon = FieldMindIcons.Info, actionIcon = FieldMindIcons.Back, onAction = onBack) }
        items(fieldMindChangelog) { entry -> ChangelogEntryCard(entry) }
    }
}

@Composable
private fun ChangelogEntryCard(entry: FieldMindChangelogEntry) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(FieldMindIcons.Sparkle, null, tint = MaterialTheme.colorScheme.primary, size = 28.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${entry.version} • ${entry.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                InfoChip(entry.importance)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { entry.tags.forEach { InfoChip(it) } }
            entry.sections.forEach { (heading, bullets) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    bullets.forEach { bullet -> Text("• $bullet", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
