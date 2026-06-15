package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.location.*
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════
//  Map Screen — full-screen Mapbox map with offline tiles, drawing tools,
//  track recording, and geo-fence reminders (PRO FEATURE)
// ══════════════════════════════════════════════════════════════════════

private enum class MapTab { Map, OfflineTiles, Drawings, Tracks, Geofences }

@Composable
fun MapFieldScreen(
    viewModel: FieldMindViewModel,
    onNavigate: (FieldMindScreen) -> Unit = {},
    onOpenDetail: (String, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val observations by viewModel.observations.collectAsState()
    val points = observations.mapNotNull { o ->
        o.latitude?.let { lat -> o.longitude?.let { lon -> lat to lon } }
    }
    val colors = FieldMindTheme.colors
    var fullScreen by remember { mutableStateOf(false) }

    // ── Pro Feature Managers (shared across tabs) ──
    val tileManager = remember { MapboxOfflineManager(context) }
    val trackRecorder = remember { TrackRecorder(context) }
    val geoFenceReminder = remember { GeoFenceReminder(context) }

    // Drawing tools state
    var savedOverlays by remember { mutableStateOf<List<MapOverlay>>(emptyList()) }
    var drawingMode by remember { mutableStateOf(DrawingMode.View) }
    val isRecording by trackRecorder.isRecording.collectAsState()
    val currentTrack by trackRecorder.currentRecording.collectAsState()
    val savedTracks by trackRecorder.savedTracks.collectAsState()
    val cachedRegions by tileManager.cachedRegions.collectAsState()
    val geofenceRegions by geoFenceReminder.activeRegions.collectAsState()

    // Restore saved geofences on first launch
    LaunchedEffect(Unit) {
        geoFenceReminder.restoreRegions()
    }

    // Tab state
    var activeTab by remember { mutableStateOf(MapTab.Map) }

    // ── Full-screen map mode ──
    if (fullScreen && points.isNotEmpty()) {
        FullScreenMapView(
            points = points,
            savedOverlays = savedOverlays,
            drawingMode = drawingMode,
            currentTrack = currentTrack,
            tileManager = tileManager,
            onClose = { fullScreen = false },
            onDrawingModeChanged = { drawingMode = it },
            onOverlaysChanged = { savedOverlays = it },
            onPointCreated = { overlay ->
                savedOverlays = savedOverlays + overlay
                // Auto-create geofence for new points
                geoFenceReminder.addRegion(
                    geoFenceReminder.regionFromPointOverlay(overlay)
                )
            },
            onLineCreated = { savedOverlays = savedOverlays + it },
            onPolygonCreated = { savedOverlays = savedOverlays + it }
        )
        return
    }

    // ── Main screen with tabs ──
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = { onNavigate(FieldMindScreen.Home) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(44.dp)
                        ) { Box(contentAlignment = Alignment.Center) { Icon(FieldMindIcons.Back, null, size = 22.dp) } }
                        Column {
                            Text("Field Map", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("PRO Feature", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    ScrollableTabRow(
                        selectedTabIndex = activeTab.ordinal,
                        edgePadding = 20.dp,
                        divider = {}
                    ) {
                        MapTab.entries.forEach { tab ->
                            Tab(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                text = {
                                    Text(
                                        when (tab) {
                                            MapTab.Map -> "Map"
                                            MapTab.OfflineTiles -> "Offline tiles"
                                            MapTab.Drawings -> "Drawings"
                                            MapTab.Tracks -> "Tracks"
                                            MapTab.Geofences -> "Geo-fences"
                                        },
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (activeTab) {
            MapTab.Map -> MapViewTab(
                modifier = Modifier.padding(padding),
                points = points,
                savedOverlays = savedOverlays,
                drawingMode = drawingMode,
                currentTrack = currentTrack,
                isRecording = isRecording,
                tileManager = tileManager,
                onFullScreen = { fullScreen = true },
                onDrawingModeChanged = { drawingMode = it },
                onOverlaysChanged = { savedOverlays = it },
                onPointCreated = { overlay ->
                    savedOverlays = savedOverlays + overlay
                    geoFenceReminder.addRegion(geoFenceReminder.regionFromPointOverlay(overlay))
                },
                onLineCreated = { savedOverlays = savedOverlays + it },
                onPolygonCreated = { savedOverlays = savedOverlays + it },
                onStartTrack = { trackRecorder.startRecording("Field session") },
                onStopTrack = { trackRecorder.stopRecording() },
                onToggleTrackPause = { trackRecorder.togglePause() }
            )
            MapTab.OfflineTiles -> OfflineTilesTab(
                modifier = Modifier.padding(padding),
                tileManager = tileManager,
                cachedRegions = cachedRegions
            )
            MapTab.Drawings -> DrawingsTab(
                modifier = Modifier.padding(padding),
                overlays = savedOverlays,
                onDeleteOverlay = { id -> savedOverlays = savedOverlays.filter { it.id != id } },
                onClearAll = { savedOverlays = emptyList() },
                onEditOverlay = { /* future: edit label/color */ }
            )
            MapTab.Tracks -> TracksTab(
                modifier = Modifier.padding(padding),
                savedTracks = savedTracks,
                currentTrack = currentTrack,
                isRecording = isRecording,
                trackRecorder = trackRecorder,
                context = context
            )
            MapTab.Geofences -> GeofencesTab(
                modifier = Modifier.padding(padding),
                geoFenceReminder = geoFenceReminder,
                geofenceRegions = geofenceRegions
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Full-Screen Map View (with drawing toolbar)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun FullScreenMapView(
    points: List<Pair<Double, Double>>,
    savedOverlays: List<MapOverlay>,
    drawingMode: DrawingMode,
    currentTrack: TrackRecording?,
    tileManager: MapboxOfflineManager? = null,
    onClose: () -> Unit,
    onDrawingModeChanged: (DrawingMode) -> Unit,
    onOverlaysChanged: (List<MapOverlay>) -> Unit,
    onPointCreated: (MapOverlay.PointOverlay) -> Unit,
    onLineCreated: (MapOverlay.LineOverlay) -> Unit,
    onPolygonCreated: (MapOverlay.PolygonOverlay) -> Unit
) {
    val colors = FieldMindTheme.colors

    Box(Modifier.fillMaxSize()) {
        MapboxMapView(
            points = points,
            savedOverlays = savedOverlays,
            drawingMode = drawingMode,
            currentTrackPoints = currentTrack?.points?.map { it.latitude to it.longitude } ?: emptyList(),
            tileManager = tileManager,
            onPointCreated = onPointCreated,
            onLineCreated = onLineCreated,
            onPolygonCreated = onPolygonCreated,
            onOverlaysChanged = onOverlaysChanged,
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalIconButton(onClick = onClose) {
                Icon(FieldMindIcons.Close, null, size = 20.dp)
            }
            if (drawingMode != DrawingMode.View) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        when (drawingMode) {
                            DrawingMode.PlacePoint -> "Tap to place point"
                            DrawingMode.DrawLine -> "Tap points to draw transect"
                            DrawingMode.DrawPolygon -> "Tap boundary points (tap first to close)"
                            else -> ""
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.size(40.dp))
        }

        // Drawing toolbar (bottom-center)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DrawToolButton(
                    icon = FieldMindIcons.Location,
                    label = "Point",
                    isActive = drawingMode == DrawingMode.PlacePoint,
                    onClick = { onDrawingModeChanged(if (drawingMode == DrawingMode.PlacePoint) DrawingMode.View else DrawingMode.PlacePoint) }
                )
                DrawToolButton(
                    icon = FieldMindIcons.Line,
                    label = "Line",
                    isActive = drawingMode == DrawingMode.DrawLine,
                    onClick = { onDrawingModeChanged(if (drawingMode == DrawingMode.DrawLine) DrawingMode.View else DrawingMode.DrawLine) }
                )
                DrawToolButton(
                    icon = FieldMindIcons.Shape,
                    label = "Polygon",
                    isActive = drawingMode == DrawingMode.DrawPolygon,
                    onClick = { onDrawingModeChanged(if (drawingMode == DrawingMode.DrawPolygon) DrawingMode.View else DrawingMode.DrawPolygon) }
                )
                DrawToolDivider()
                DrawToolButton(
                    icon = FieldMindIcons.Select,
                    label = "Select",
                    isActive = drawingMode == DrawingMode.Select,
                    onClick = { onDrawingModeChanged(if (drawingMode == DrawingMode.Select) DrawingMode.View else DrawingMode.Select) }
                )
                DrawToolButton(
                    icon = FieldMindIcons.Delete,
                    label = "Clear",
                    isActive = false,
                    onClick = { onOverlaysChanged(emptyList()) }
                )
            }
        }

        // Attribution
        Text(
            "© Mapbox",
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DrawToolButton(icon: MaterialSymbolIcon, label: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            icon = icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            size = 22.dp
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrawToolDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(40.dp)
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Tabs
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun MapViewTab(
    modifier: Modifier,
    points: List<Pair<Double, Double>>,
    savedOverlays: List<MapOverlay>,
    drawingMode: DrawingMode,
    currentTrack: TrackRecording?,
    isRecording: Boolean,
    tileManager: MapboxOfflineManager? = null,
    onFullScreen: () -> Unit,
    onDrawingModeChanged: (DrawingMode) -> Unit,
    onOverlaysChanged: (List<MapOverlay>) -> Unit,
    onPointCreated: (MapOverlay.PointOverlay) -> Unit,
    onLineCreated: (MapOverlay.LineOverlay) -> Unit,
    onPolygonCreated: (MapOverlay.PolygonOverlay) -> Unit,
    onStartTrack: () -> Unit,
    onStopTrack: () -> Unit,
    onToggleTrackPause: () -> Unit
) {
    val colors = FieldMindTheme.colors

    Column(modifier.fillMaxSize()) {
        // Map preview
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {                MapboxMapView(
                    points = points,
                    savedOverlays = savedOverlays,
                    drawingMode = drawingMode,
                    currentTrackPoints = currentTrack?.points?.map { it.latitude to it.longitude } ?: emptyList(),
                    tileManager = tileManager,
                    onPointCreated = onPointCreated,
                    onLineCreated = onLineCreated,
                    onPolygonCreated = onPolygonCreated,
                    onOverlaysChanged = onOverlaysChanged,
                    modifier = Modifier.fillMaxSize()
                )
            // Track recording indicator
            if (isRecording) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                        Text("REC ${currentTrack?.points?.size ?: 0} pts", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            // Full-screen button
            IconButton(
                onClick = onFullScreen,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(FieldMindIcons.MapFull, null, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
            }
        }

        // Action bar
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drawing mode chips
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Drawing tools", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = drawingMode == DrawingMode.PlacePoint, onClick = { onDrawingModeChanged(if (drawingMode == DrawingMode.PlacePoint) DrawingMode.View else DrawingMode.PlacePoint) }, label = { Text("Point") }, leadingIcon = { Icon(FieldMindIcons.Location, null, size = 16.dp) })
                            FilterChip(selected = drawingMode == DrawingMode.DrawLine, onClick = { onDrawingModeChanged(if (drawingMode == DrawingMode.DrawLine) DrawingMode.View else DrawingMode.DrawLine) }, label = { Text("Line") }, leadingIcon = { Icon(FieldMindIcons.Line, null, size = 16.dp) })
                            FilterChip(selected = drawingMode == DrawingMode.DrawPolygon, onClick = { onDrawingModeChanged(if (drawingMode == DrawingMode.DrawPolygon) DrawingMode.View else DrawingMode.DrawPolygon) }, label = { Text("Polygon") }, leadingIcon = { Icon(FieldMindIcons.Shape, null, size = 16.dp) })
                        }
                    }
                }
            }

            // Track recording card
            item {
                TrackRecordingCard(
                    isRecording = isRecording,
                    currentTrack = currentTrack,
                    onStart = onStartTrack,
                    onStop = onStopTrack,
                    onTogglePause = onToggleTrackPause
                )
            }

            // Stats card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${points.size} observation${if (points.size == 1) "" else "s"} with GPS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Use the full-screen map for drawing tools, track recording, and geo-fencing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRecordingCard(
    isRecording: Boolean,
    currentTrack: TrackRecording?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onTogglePause: () -> Unit
) {
    val colors = FieldMindTheme.colors
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FieldMindIcons.Track, null, tint = if (isRecording) MaterialTheme.colorScheme.error else colors.info, size = 20.dp)
                Text("Track recording", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (isRecording && currentTrack != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${currentTrack.points.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Points", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val dist = currentTrack.distanceMeters
                        Text(if (dist < 1000) "%.0f m".format(dist) else "%.2f km".format(dist / 1000), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val elapsed = System.currentTimeMillis() - currentTrack.startedAt
                        val sec = elapsed / 1000
                        Text("%02d:%02d".format(sec / 60, sec % 60), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Elapsed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStop, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Stop") }
                    OutlinedButton(onClick = onTogglePause, shape = RoundedCornerShape(14.dp)) { Text(if (currentTrack.isPaused) "Resume" else "Pause") }
                }
            } else {
                Text("Record GPS tracks during your field sessions to map your survey path.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onStart, shape = RoundedCornerShape(14.dp)) { Icon(FieldMindIcons.Track, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Start recording") }
            }
        }
    }
}

@Composable
private fun OfflineTilesTab(
    modifier: Modifier,
    tileManager: MapboxOfflineManager,
    cachedRegions: List<MapboxTileRegion>
) {
    val scope = rememberCoroutineScope()
    val isDownloading by tileManager.isDownloading.collectAsState()
    val downloadProgress by tileManager.downloadProgress.collectAsState()
    var showDownloadDialog by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf("—") }

    LaunchedEffect(Unit) {
        tileManager.refreshCachedRegions()
        val bytes = tileManager.getCacheSizeBytes()
        cacheSize = if (bytes < 1_000_000) "${bytes / 1024} KB" else "%.1f MB".format(bytes / 1_000_000.0)
    }

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(FieldMindIcons.Download, null, tint = FieldMindTheme.colors.info, size = 20.dp)
                        Text("Offline tile cache", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(                "Download Mapbox tiles for your study area so the map works without internet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Cache: $cacheSize", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${cachedRegions.size} region${if (cachedRegions.size == 1) "" else "s"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { showDownloadDialog = true }, shape = RoundedCornerShape(14.dp), enabled = !isDownloading) { Text("Download new region") }
                    if (cachedRegions.isNotEmpty()) {
                        OutlinedButton(onClick = {
                            scope.launch { tileManager.clearAllCaches(); cacheSize = "0 KB" }
                        }, shape = RoundedCornerShape(14.dp)) { Text("Clear all") }
                    }
                }
            }
        }
        if (cachedRegions.isNotEmpty()) {
            item { Text("Cached regions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(cachedRegions) { region ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(region.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Z${region.minZoom}-${region.maxZoom}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("%.4f, %.4f — %.4f, %.4f".format(region.north, region.west, region.south, region.east), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("%.1f%%".format(region.progress * 100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            if (region.downloadedAt > 0) {
                                Text(java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(region.downloadedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDownloadDialog) {
        DownloadRegionDialog(
            onDismiss = { showDownloadDialog = false },
            tileManager = tileManager
        )
    }
}

@Composable
private fun DownloadRegionDialog(
    onDismiss: () -> Unit,
    tileManager: MapboxOfflineManager
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var latNorth by remember { mutableStateOf("") }
    var latSouth by remember { mutableStateOf("") }
    var lonEast by remember { mutableStateOf("") }
    var lonWest by remember { mutableStateOf("") }
    var minZoom by remember { mutableStateOf("10") }
    var maxZoom by remember { mutableStateOf("16") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download tile region") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Download Mapbox tiles for offline use. Enter the bounding box coordinates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FieldTextField(name, { name = it }, "Region name (e.g. Study Area)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldTextField(latNorth, { latNorth = it }, "Lat N", modifier = Modifier.weight(1f))
                    FieldTextField(latSouth, { latSouth = it }, "Lat S", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldTextField(lonEast, { lonEast = it }, "Lon E", modifier = Modifier.weight(1f))
                    FieldTextField(lonWest, { lonWest = it }, "Lon W", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldTextField(minZoom, { minZoom = it }, "Min zoom", modifier = Modifier.weight(1f))
                    FieldTextField(maxZoom, { maxZoom = it }, "Max zoom", modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        tileManager.downloadRegion(
                            name = name.ifBlank { "Study area" },
                            north = latNorth.toDoubleOrNull() ?: 0.0,
                            south = latSouth.toDoubleOrNull() ?: 0.0,
                            east = lonEast.toDoubleOrNull() ?: 0.0,
                            west = lonWest.toDoubleOrNull() ?: 0.0,
                            minZoom = minZoom.toIntOrNull() ?: 10,
                            maxZoom = maxZoom.toIntOrNull() ?: 16
                        )
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                enabled = name.isNotBlank() && latNorth.isNotBlank() && latSouth.isNotBlank() && lonEast.isNotBlank() && lonWest.isNotBlank()
            ) { Text("Download") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DrawingsTab(
    modifier: Modifier,
    overlays: List<MapOverlay>,
    onDeleteOverlay: (String) -> Unit,
    onClearAll: () -> Unit,
    onEditOverlay: (MapOverlay) -> Unit
) {
    val colors = FieldMindTheme.colors
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(FieldMindIcons.Shape, null, tint = colors.info, size = 20.dp)
                        Text("Saved drawings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${overlays.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (overlays.isEmpty()) {
                        Text("No drawings yet. Use the drawing tools in full-screen map mode to mark sites, transects, and survey boundaries.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (overlays.isNotEmpty()) {
                        OutlinedButton(onClick = onClearAll, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Clear all drawings") }
                    }
                }
            }
        }
        items(overlays) { overlay ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.clickable { onEditOverlay(overlay) }
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val icon = when (overlay) {
                        is MapOverlay.PointOverlay -> FieldMindIcons.Location
                        is MapOverlay.LineOverlay -> FieldMindIcons.Line
                        is MapOverlay.PolygonOverlay -> FieldMindIcons.Shape
                    }
                    Icon(icon, null, tint = Color(overlay.color.toInt()), size = 22.dp)
                    Column(Modifier.weight(1f)) {
                        Text(overlay.label.ifBlank { when (overlay) {
                            is MapOverlay.PointOverlay -> "Site point"
                            is MapOverlay.LineOverlay -> "Transect"
                            is MapOverlay.PolygonOverlay -> "Survey boundary"
                        } }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(when (overlay) {
                            is MapOverlay.PointOverlay -> "%.5f, %.5f".format(overlay.latitude, overlay.longitude)
                            is MapOverlay.LineOverlay -> "${overlay.points.size} points"
                            is MapOverlay.PolygonOverlay -> "${overlay.points.size} vertices"
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDeleteOverlay(overlay.id) }, modifier = Modifier.size(36.dp)) {
                        Icon(FieldMindIcons.Delete, null, tint = MaterialTheme.colorScheme.error, size = 18.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TracksTab(
    modifier: Modifier,
    savedTracks: List<TrackRecording>,
    currentTrack: TrackRecording?,
    isRecording: Boolean,
    trackRecorder: TrackRecorder,
    context: Context
) {
    val scope = rememberCoroutineScope()
    val colors = FieldMindTheme.colors

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(FieldMindIcons.Track, null, tint = colors.info, size = 20.dp)
                        Text("Track recordings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text("Recorded GPS tracks appear here. Export them as GPX for use in other mapping tools.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${savedTracks.size} saved track${if (savedTracks.size == 1) "" else "s"} · %.0f m today".format(trackRecorder.todayDistanceMeters()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (currentTrack != null && isRecording) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                            Text("Recording: ${currentTrack.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("${currentTrack.points.size} points · %.0f m distance".format(currentTrack.distanceMeters), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        items(savedTracks.sortedByDescending { it.startedAt }) { track ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(track.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(track.startedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("%.0f m".format(track.distanceMeters), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.info)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${track.points.size} points · ${track.totalDurationMs / 1000}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                scope.launch {
                                    trackRecorder.exportToGpx(track.id)
                                }
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(FieldMindIcons.Export, null, size = 16.dp)
                            }
                            IconButton(onClick = { trackRecorder.deleteTrack(track.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(FieldMindIcons.Delete, null, size = 16.dp)
                            }
                        }
                    }
                }
            }
        }
        if (savedTracks.isEmpty() && !isRecording) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No tracks recorded yet. Start a track recording from the Map tab to log your survey path.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GeofencesTab(
    modifier: Modifier,
    geoFenceReminder: GeoFenceReminder,
    geofenceRegions: List<GeofenceRegion>
) {
    val colors = FieldMindTheme.colors

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(FieldMindIcons.Notifications, null, tint = colors.info, size = 20.dp)
                        Text("Geo-fence reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text("Mark sites and get notified when you arrive. Points drawn on the map are automatically registered as geo-fence zones.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${geofenceRegions.size} active region${if (geofenceRegions.size == 1) "" else "s"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (geofenceRegions.isNotEmpty()) {
                        OutlinedButton(onClick = { geoFenceReminder.clearAllRegions() }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Clear all") }
                    }
                }
            }
        }
        items(geofenceRegions) { region ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (region.isActive) colors.observation.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Notifications, null, tint = if (region.isActive) colors.observation else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(region.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("%.5f, %.5f".format(region.latitude, region.longitude), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${region.radiusMeters.toInt()}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (region.triggerOnEntry) Text("Entry", style = MaterialTheme.typography.labelSmall, color = colors.positive)
                            if (region.triggerOnExit) Text("Exit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (region.note.isNotBlank()) {
                        Text(region.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    IconButton(onClick = { geoFenceReminder.removeRegion(region.id) }, modifier = Modifier.size(36.dp)) {
                        Icon(FieldMindIcons.Delete, null, tint = MaterialTheme.colorScheme.error, size = 18.dp)
                    }
                }
            }
        }
        if (geofenceRegions.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No geo-fence regions. Draw points on the map and they'll be registered as geo-fences automatically.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
