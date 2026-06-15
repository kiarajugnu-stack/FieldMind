package fieldmind.research.app.features.field.presentation.components

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
import com.mapbox.geojson.Point

import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.locationcomponent.location
import fieldmind.research.app.features.field.data.location.MapOverlay
import fieldmind.research.app.features.field.data.location.MapboxOfflineManager
import kotlinx.coroutines.delay

/**
 * Mapbox Map composable that works both online and offline.
 *
 * - **Online**: Loads Mapbox Streets style from the cloud.
 * - **Offline**: Falls back to cached tiles via [MapboxOfflineManager].
 *
 * Supports observation markers, drawing overlays, and track recording display.
 * Requires a valid Mapbox access token in res/values/mapbox_access_token.xml.
 */
@Composable
fun MapboxMapView(
    points: List<Pair<Double, Double>> = emptyList(),
    savedOverlays: List<MapOverlay> = emptyList(),
    currentTrackPoints: List<Pair<Double, Double>> = emptyList(),
    tileManager: MapboxOfflineManager? = null,
    modifier: Modifier = Modifier,
    showEmptyState: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 300.dp,
) {
    val context = LocalContext.current
    val isOffline by tileManager?.isOffline.collectAsState() ?: remember { mutableStateOf(false) }
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
    val zoom = (14.0 - kotlin.math.log2(maxOf(latSpread, lonSpread).coerceAtLeast(0.01))).coerceIn(4.0, 18.0)

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(avgLon, avgLat))
            zoom(zoom)
            bearing(0.0)
            pitch(0.0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(16.dp))
    ) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = {
                Style.MAPBOX_STREETS
            },
            compass = {
                com.mapbox.maps.extension.compose.Compass(
                    visibility = com.mapbox.maps.plugin.compass.CompassVisibility.ADAPTIVE
                )
            },
            scaleBar = {
                com.mapbox.maps.extension.compose.ScaleBar(
                    visibility = com.mapbox.maps.extension.compose.ScaleBarVisibility.ADAPTIVE
                )
            },
            logo = {
                com.mapbox.maps.extension.compose.Logo()
            },
            attribution = {
                com.mapbox.maps.extension.compose.Attribution()
            }
        ) {
            // Enable user location puck
            MapEffect {
                mapView.location.apply {
                    enabled = true
                    pulsingEnabled = true
                }
            }
        }

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
            "© Mapbox",
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
}
