package fieldmind.research.app.features.field.presentation.components

import android.view.Gravity
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fieldmind.research.app.features.field.data.location.DrawingMode
import fieldmind.research.app.features.field.data.location.MapOverlay
import fieldmind.research.app.features.field.data.location.MaplibreOfflineManager
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import kotlin.math.log2

/**
 * MapLibre Map composable that works both online and offline.
 *
 * - **Online**: Loads a free MapLibre demo tile style.
 * - **Offline**: Falls back to cached tiles via [MaplibreOfflineManager].
 *
 * No API key required — MapLibre is fully open source (BSD license).
 * Uses the free demo tiles from demotiles.maplibre.org for online rendering.
 */
@Composable
fun MaplibreMapView(
    points: List<Pair<Double, Double>> = emptyList(),
    savedOverlays: List<MapOverlay> = emptyList(),
    currentTrackPoints: List<Pair<Double, Double>> = emptyList(),
    tileManager: MaplibreOfflineManager? = null,
    drawingMode: DrawingMode = DrawingMode.View,
    modifier: Modifier = Modifier,
    showEmptyState: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 300.dp,
    onPointCreated: (MapOverlay.PointOverlay) -> Unit = {},
    onLineCreated: (MapOverlay.LineOverlay) -> Unit = {},
    onPolygonCreated: (MapOverlay.PolygonOverlay) -> Unit = {},
    onOverlaysChanged: (List<MapOverlay>) -> Unit = {}
) {
    val context = LocalContext.current
    val isOffline by tileManager?.isOffline.collectAsState() ?: remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    if (points.isEmpty() && savedOverlays.isEmpty() && showEmptyState) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text(
                if (isOffline) "No cached maps — download tiles in the Offline tab" else "No GPS-tagged observations yet",
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

    // Initialize MapLibre (safe to call multiple times)
    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
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
                    mv.getMapAsync { mapLibreMap ->
                        map = mapLibreMap
                        // Load free demo tile style — no API key needed
                        mapLibreMap.setStyle(
                            "https://demotiles.maplibre.org/style.json",
                            object : Style.OnStyleLoaded {
                                override fun onStyleLoaded(style: Style?) {
                                    // Move camera to the computed center
                                    mapLibreMap.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(avgLat, avgLon),
                                            zoom
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                // Update camera if points changed
                mv.getMapAsync { mapLibreMap ->
                    mapLibreMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(avgLat, avgLon),
                            zoom
                        )
                    )
                }
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
            "© MapLibre | © OpenStreetMap",
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }

    // Periodic connectivity check
    LaunchedEffect(Unit) {
        while (true) {
            tileManager?.checkAndUpdateOfflineState()
            delay(30_000L)
        }
    }

    // Lifecycle management
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onStop()
            mapView?.onDestroy()
        }
    }
}
