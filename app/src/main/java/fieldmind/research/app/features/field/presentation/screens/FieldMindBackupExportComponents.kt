package fieldmind.research.app.features.field.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Hero Status Card
// ══════════════════════════════════════════════════════════════════════

@Composable
fun HeroStatusCard(
    lastBackupLabel: String,
    autoBackupEnabled: Boolean,
    autoBackupInterval: String,
    entityCounts: Map<String, Int>,
    onAutoBackupToggle: (Boolean) -> Unit = {}
) {
    val colors = FieldMindTheme.colors
    val totalRecords = entityCounts.values.sum()

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Hero row: icon + last backup + auto-backup status ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        FieldMindIcons.Archive,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 28.dp
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "\uD83D\uDCE6 Last backup: $lastBackupLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "$totalRecords total records across 5 types",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                }
            }

            // ── Auto-backup status row ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Auto-backup: ${if (autoBackupEnabled) "ON ($autoBackupInterval)" else "OFF"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (autoBackupEnabled) "Backups are scheduled automatically"
                        else "Enable auto-backup to protect your data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.semantics { contentDescription = "Toggle auto-backup: ${if (autoBackupEnabled) "enabled" else "disabled"}" }
                ) {
                    Text(
                        if (autoBackupEnabled) "ON" else "OFF",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (autoBackupEnabled) colors.positive else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = { onAutoBackupToggle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.positive,
                            checkedTrackColor = colors.positive.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // ── Entity count row ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entityCounts.entries.take(5).forEach { (key, value) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            value.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            key.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Tab Pill Selector
// ══════════════════════════════════════════════════════════════════════

@Composable
fun TabPillSelector(
    activeTab: BackupTab,
    onTabChange: (BackupTab) -> Unit
) {
    val tabs = listOf(
        Triple(BackupTab.EXPORT, FieldMindIcons.Export, "Export"),
        Triple(BackupTab.IMPORT, FieldMindIcons.Download, "Import"),
        Triple(BackupTab.BACKUP, FieldMindIcons.Archive, "Backup")
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (tab, icon, label) ->
                val selected = activeTab == tab
                Surface(
                    onClick = { onTabChange(tab) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon = icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 18.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Export History Item Card
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ExportHistoryItemCard(
    record: ExportRecord,
    context: Context,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(record.exportedAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.exportedAt))
    }
    val formatColor = exportFormats.find {
        it.name.equals(record.format, ignoreCase = true) ||
        (record.format == "Encrypted" && it.name == ".fieldmind")
    }?.color ?: MaterialTheme.colorScheme.primary
    val formatIcon = when {
        record.format == "Encrypted" || record.format == ".fieldmind" -> FieldMindIcons.Archive
        record.format == "JSON" -> FieldMindIcons.Archive
        record.format == "CSV" -> FieldMindIcons.Data
        record.format == "Markdown" || record.format == "HTML" -> FieldMindIcons.Article
        record.format == "PDF" -> FieldMindIcons.Report
        record.format == "PNG" || record.format == "SVG" -> FieldMindIcons.Graph
        else -> FieldMindIcons.File
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.clickable { onShare() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(formatColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(formatIcon, null, tint = formatColor, size = 22.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(record.fileName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${record.format} • ${formatFileSize(record.fileSizeBytes)} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.destination.isNotBlank()) {
                    Text(
                        record.destination,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(FieldMindIcons.Close, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), size = 18.dp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Confirmation Dialog for backup
// ══════════════════════════════════════════════════════════════════════

@Composable
fun BackupConfirmationDialog(
    visible: Boolean,
    entityCounts: Map<String, Int>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(FieldMindIcons.Archive, null, size = 28.dp) },
            title = { Text("Create backup?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This will create a .fieldmind package with all your data and media attachments.")
                    Text("Summary:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    entityCounts.forEach { (key, count) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• $count $key", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("Create backup") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun ExportConfirmationDialog(
    visible: Boolean,
    format: String,
    entityCount: Int,
    estimatedSize: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(FieldMindIcons.Export, null, size = 28.dp) },
            title = { Text("Export $format?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Export $entityCount records as $format.")
                    Text("Estimated size: $estimatedSize", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
