package fieldmind.research.app.features.field.presentation.components

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fieldmind.research.app.features.field.data.location.DrawingMode
import fieldmind.research.app.features.field.data.location.MapOverlay
import fieldmind.research.app.features.field.data.location.OsmTileManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.log2

/**
 * osmdroid Map composable that works both online and offline.
 *
 * - **Online**: Uses free OpenStreetMap raster tiles (MAPNIK).
 * - **Offline**: Uses osmdroid's automatic tile caching.
 * - **Drawing**: Supports interactive point/line/polygon placement based on [drawingMode].
 *
 * No API key required — OpenStreetMap tiles are free and open.
 */
@Composable
fun OsmMapView(
    points: List<Pair<Double, Double>> = emptyList(),
    savedOverlays: List<MapOverlay> = emptyList(),
    currentTrackPoints: List<Pair<Double, Double>> = emptyList(),
    tileManager: OsmTileManager? = null,
    drawingMode: DrawingMode = DrawingMode.View,
    modifier: Modifier = Modifier,
    showEmptyState: Boolean = true,
    height: Dp = 300.dp,
    onPointCreated: (MapOverlay.PointOverlay) -> Unit = {},
    onLineCreated: (MapOverlay.LineOverlay) -> Unit = {},
    onPolygonCreated: (MapOverlay.PolygonOverlay) -> Unit = {},
    onOverlaysChanged: (List<MapOverlay>) -> Unit = {}
) {
    val context = LocalContext.current
    val isOffline by tileManager?.isOffline?.collectAsState() ?: remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Temporary drawing state for line/polygon modes
    val pendingPoints = remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }

    // Mutable refs so the MapEventsOverlay (created once in factory) always sees current values
    val currentDrawingMode = remember { mutableStateOf(drawingMode) }
    val currentOnPointCreated = remember { mutableStateOf(onPointCreated) }
    val currentOnLineCreated = remember { mutableStateOf(onLineCreated) }
    val currentOnPolygonCreated = remember { mutableStateOf(onPolygonCreated) }
    currentDrawingMode.value = drawingMode
    currentOnPointCreated.value = onPointCreated
    currentOnLineCreated.value = onLineCreated
    currentOnPolygonCreated.value = onPolygonCreated

    if (points.isEmpty() && savedOverlays.isEmpty() && showEmptyState) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text(
                if (isOffline) "No cached maps — browse tiles online first to cache them"
                else "No GPS-tagged observations yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Compute center from all visible points
    val allPoints = points + savedOverlays.flatMap { overlay ->
        when (overlay) {
            is MapOverlay.PointOverlay -> listOf(overlay.latitude to overlay.longitude)
            is MapOverlay.LineOverlay -> overlay.points
            is MapOverlay.PolygonOverlay -> overlay.points
        }
    }
    val avgLat = allPoints.map { it.first }.average()
    val avgLon = allPoints.map { it.second }.average()
    val latSpread = (allPoints.maxOf { it.first } - allPoints.minOf { it.first }).coerceAtLeast(0.01)
    val lonSpread = (allPoints.maxOf { it.second } - allPoints.minOf { it.second }).coerceAtLeast(0.01)
    val zoom = (14.0 - log2(maxOf(latSpread, lonSpread).coerceAtLeast(0.01))).coerceIn(4.0, 18.0)

    // Configure osmdroid once
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir.resolve("osmdroid")
            osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).also { mv ->
                    mapView = mv
                    mv.setTileSource(TileSourceFactory.MAPNIK)
                    mv.setMultiTouchControls(true)
                    mv.setTilesScaledToDpi(true)

                    // Set initial camera position
                    mv.controller.setZoom(zoom)
                    mv.controller.setCenter(GeoPoint(avgLat, avgLon))

                    // Wire up interactive drawing via MapEventsOverlay
                    // Uses current* mutable refs so the closure always sees fresh values
                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            val mode = currentDrawingMode.value
                            val pts = pendingPoints
                            if (mode == DrawingMode.PlacePoint) {
                                currentOnPointCreated.value(
                                    MapOverlay.PointOverlay(
                                        latitude = p.latitude,
                                        longitude = p.longitude,
                                        label = "Placed point"
                                    )
                                )
                                return true
                            }
                            if (mode == DrawingMode.DrawLine) {
                                val updated = pts.value + (p.latitude to p.longitude)
                                pts.value = updated
                                if (updated.size >= 2) {
                                    currentOnLineCreated.value(
                                        MapOverlay.LineOverlay(
                                            points = updated,
                                            label = "Transect"
                                        )
                                    )
                                }
                                return true
                            }
                            if (mode == DrawingMode.DrawPolygon) {
                                val updated = pts.value + (p.latitude to p.longitude)
                                pts.value = updated
                                if (updated.size >= 3) {
                                    currentOnPolygonCreated.value(
                                        MapOverlay.PolygonOverlay(
                                            points = updated,
                                            label = "Survey boundary"
                                        )
                                    )
                                }
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                    mv.overlays.add(0, eventsOverlay)

                    // Resume to start tile rendering
                    mv.onResume()

                    // Initial overlay rendering
                    renderOverlays(mv, points, savedOverlays, currentTrackPoints, pendingPoints.value, drawingMode)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.controller.setZoom(zoom)
                mv.controller.setCenter(GeoPoint(avgLat, avgLon))
                renderOverlays(mv, points, savedOverlays, currentTrackPoints, pendingPoints.value, drawingMode)
            }
        )

        // Offline indicator banner
        if (isOffline) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "Offline",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Attribution
        Text(
            "© OpenStreetMap contributors",
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }

    // Periodic connectivity check
    LaunchedEffect(Unit) {
        while (true) {
            tileManager?.checkAndUpdateOfflineState()
            kotlinx.coroutines.delay(30_000L)
        }
    }

    // Lifecycle management
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onDetach()
        }
    }

    // Reset pending points when drawing mode changes to View
    LaunchedEffect(drawingMode) {
        if (drawingMode == DrawingMode.View || drawingMode == DrawingMode.Select) {
            pendingPoints.value = emptyList()
        }
    }
}

/**
 * Renders all overlays on the osmdroid MapView.
 * Handles: observation markers, saved overlays (points/lines/polygons),
 * current track, and pending drawing previews.
 */
private fun renderOverlays(
    mapView: MapView,
    points: List<Pair<Double, Double>>,
    savedOverlays: List<MapOverlay>,
    currentTrackPoints: List<Pair<Double, Double>>,
    pendingPoints: List<Pair<Double, Double>>,
    drawingMode: DrawingMode
) {
    // Keep the MapEventsOverlay (index 0) — everything else gets rebuilt
    val eventsOverlay = mapView.overlays.getOrNull(0)
    mapView.overlays.clear()
    if (eventsOverlay != null) {
        mapView.overlays.add(eventsOverlay)
    }

    // 1. Render observation points
    points.forEach { (lat, lon) ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(lat, lon)
            title = "Observation"
            snippet = "%.5f, %.5f".format(lat, lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(marker)
    }

    // 2. Render saved overlays
    savedOverlays.forEach { overlay ->
        when (overlay) {
            is MapOverlay.PointOverlay -> {
                val marker = Marker(mapView).apply {
                    position = GeoPoint(overlay.latitude, overlay.longitude)
                    title = overlay.label.ifBlank { "Site point" }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
            }
            is MapOverlay.LineOverlay -> {
                val polyline = Polyline().apply {
                    setPoints(overlay.points.map { GeoPoint(it.first, it.second) })
                    outlinePaint.color = overlay.color.toInt()
                    outlinePaint.strokeWidth = 4f
                }
                mapView.overlays.add(polyline)
            }
            is MapOverlay.PolygonOverlay -> {
                val polygon = Polygon().apply {
                    points = overlay.points.map { GeoPoint(it.first, it.second) }
                    fillPaint.color = Color.argb(
                        60,
                        Color.red(overlay.color.toInt()),
                        Color.green(overlay.color.toInt()),
                        Color.blue(overlay.color.toInt())
                    )
                    outlinePaint.color = overlay.color.toInt()
                    outlinePaint.strokeWidth = 3f
                }
                mapView.overlays.add(polygon)
            }
        }
    }

    // 3. Render current track as Polyline
    if (currentTrackPoints.isNotEmpty()) {
        val trackLine = Polyline().apply {
            setPoints(currentTrackPoints.map { GeoPoint(it.first, it.second) })
            outlinePaint.color = Color.parseColor("#FF5252")
            outlinePaint.strokeWidth = 5f
        }
        mapView.overlays.add(trackLine)
    }

    // 4. Render pending drawing preview (lines/polygons being actively drawn)
    if (pendingPoints.isNotEmpty()) {
        if (drawingMode == DrawingMode.DrawLine) {
            val previewLine = Polyline().apply {
                setPoints(pendingPoints.map { GeoPoint(it.first, it.second) })
                outlinePaint.color = Color.parseColor("#2196F3")
                outlinePaint.strokeWidth = 4f
                outlinePaint.alpha = 180
            }
            mapView.overlays.add(previewLine)
        }
        if (drawingMode == DrawingMode.DrawPolygon) {
            val previewPoly = Polygon().apply {
                points = pendingPoints.map { GeoPoint(it.first, it.second) }
                fillPaint.color = Color.argb(40, 255, 152, 0)
                outlinePaint.color = Color.parseColor("#FF9800")
                outlinePaint.strokeWidth = 3f
                outlinePaint.alpha = 200
            }
            mapView.overlays.add(previewPoly)
        }
        // Show pending point markers
        pendingPoints.forEach { (lat, lon) ->
            val dot = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
                title = ""
            }
            mapView.overlays.add(dot)
        }
    }

    mapView.invalidate()
}
