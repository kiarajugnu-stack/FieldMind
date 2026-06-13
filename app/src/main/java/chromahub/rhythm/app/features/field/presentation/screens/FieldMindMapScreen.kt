package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
// ═════════════════════════════════════════════════
//  Map Screen (full-screen OSM map)
// ═════════════════════════════════════════════════

@Composable
fun MapFieldScreen(
    viewModel: FieldMindViewModel,
    onNavigate: (FieldMindScreen) -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val observations by viewModel.observations.collectAsState()
    val points = observations.mapNotNull { o -> o.latitude?.let { lat -> o.longitude?.let { lon -> lat to lon } } }
    val colors = FieldMindTheme.colors
    var fullScreen by remember { mutableStateOf(false) }

    if (fullScreen && points.isNotEmpty()) {
        // Full-screen map mode
        Box(Modifier.fillMaxSize()) {
            OsmMap(points = points, modifier = Modifier.fillMaxSize())
            Row(
                Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(onClick = { fullScreen = false }) {
                    Icon(FieldMindIcons.Close, null, size = 20.dp)
                }
                Text("Field Map • ${points.size} locations", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(Modifier.size(40.dp))
            }
            Text("© OpenStreetMap contributors", modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { FieldScreenHeader("Field Map", "Interactive GPS map of your observation locations.", icon = FieldMindIcons.MapFull, actionIcon = FieldMindIcons.Close, onAction = { onNavigate(FieldMindScreen.Home) }) }
        if (points.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(icon = FieldMindIcons.Location, contentDescription = null, tint = colors.observation, size = 48.dp)
                        Text("No GPS-tagged observations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Enable GPS when capturing observations to plot them on the map. You can also use the mini-map in Insight.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { onNavigate(FieldMindScreen.Observe) }, shape = RoundedCornerShape(16.dp)) { Text("Go to Capture") }
                    }
                }
            }
        } else {
            item {
                Box {
                    OsmMap(points = points)
                    IconButton(
                        onClick = { fullScreen = true },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(FieldMindIcons.MapFull, null, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${points.size} observation${if (points.size == 1) "" else "s"} with GPS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Pinch to zoom and pan the map. Tap a marker to see coordinates. Go to Insights for charts, achievements, and knowledge graph.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { onNavigate(FieldMindScreen.Insights) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Icon(icon = FieldMindIcons.Insights, contentDescription = null, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Open Insights")
                        }
                    }
                }
            }
            val geoObs = observations.filter { it.latitude != null }.sortedByDescending { it.timestamp }.take(5)
            if (geoObs.isNotEmpty()) {
                item { SectionHeader("Recent tagged observations") }
                items(geoObs) { obs ->
                    EntityCard(
                        title = obs.subject,
                        kind = "observation",
                        body = obs.manualLocation.ifBlank { "%.5f, %.5f".format(obs.latitude!!, obs.longitude!!) },
                        meta = listOf(obs.category, obs.date),
                        onClick = { onOpenDetail("observation", obs.id) }
                    )
                }
            }
        }
    }
}
