package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Phase 11: Reports Redesign - Report Builder & Templates
 */

enum class ReportType(val displayName: String, val difficulty: String, val description: String) {
    OBSERVATION("Observation Report", "Beginner", "Simple record of one observation"),
    SPECIES("Species Report", "Beginner", "Detailed species identification and behavior"),
    SUMMARY("Project Summary", "Beginner", "Overview of project findings"),
    SURVEY("Site Survey", "Intermediate", "Structured field site assessment"),
    FIELD("Field Report", "Intermediate", "Comprehensive field study findings"),
    LAB("Lab Report", "Advanced", "Formal laboratory-style report"),
    LITERATURE("Literature Review", "Advanced", "Synthesis of published research")
}

/**
 * Report Template Card - for selecting templates
 */
@Composable
fun ReportTemplateCard(
    reportType: ReportType,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = if (isSelected) border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(reportType.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(reportType.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        reportType.difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Report Section Editor
 */
@Composable
fun ReportSectionEditor(
    sectionTitle: String,
    content: String,
    onContentChange: (String) -> Unit,
    hint: String = "",
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(sectionTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            placeholder = { Text(hint) },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

/**
 * Report Preview Card
 */
@Composable
fun ReportPreviewCard(
    title: String,
    type: String,
    sections: Map<String, String>,  // section name to preview text
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📄", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                sections.take(3).forEach { section ->
                    val (name, _) = section
                    AssistChip(onClick = {}, label = { Text(name) })
                }
                if (sections.size > 3) {
                    AssistChip(onClick = {}, label = { Text("+${sections.size - 3}") })
                }
            }
        }
    }
}

/**
 * Report Export Options
 */
@Composable
fun ReportExportMenu(
    onExportPDF: () -> Unit,
    onExportWord: () -> Unit,
    onExportMarkdown: () -> Unit,
    onShareLink: () -> Unit,
    onGenerateSlides: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Export as", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ExportButton("PDF", onClick = onExportPDF, modifier = Modifier.weight(1f))
                ExportButton("Word", onClick = onExportWord, modifier = Modifier.weight(1f))
                ExportButton("MD", onClick = onExportMarkdown, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ExportButton("Link", onClick = onShareLink, modifier = Modifier.weight(1f))
                ExportButton("Slides", onClick = onGenerateSlides, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExportButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(40.dp), shape = RoundedCornerShape(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Auto-generate Report Prompt
 */
@Composable
fun AutoGenerateReportOption(
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onGenerate),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
        border = border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✨", style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f)) {
                Text("Generate draft report", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text("Auto-create from your observations, data, and hypotheses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
