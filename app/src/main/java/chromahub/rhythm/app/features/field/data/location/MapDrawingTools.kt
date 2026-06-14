package fieldmind.research.app.features.field.data.location

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.util.UUID

/**
 * Enum defining the current drawing mode for the map.
 */
enum class DrawingMode {
    /** Just viewing — no drawing interaction. */
    View,
    /** Placing markers at tapped locations. */
    PlacePoint,
    /** Drawing a line (transect) by tapping points. */
    DrawLine,
    /** Drawing a polygon (survey boundary) by tapping points. */
    DrawPolygon,
    /** Selecting and manipulating existing drawings. */
    Select
}

/**
 * A persisted geometry overlay on the map.
 */
sealed class MapOverlay(
    open val id: String = UUID.randomUUID().toString(),
    open val label: String = "",
    open val color: Long = 0xFF4CAF50,
    open val createdAt: Long = System.currentTimeMillis()
) {
    data class PointOverlay(
        val latitude: Double,
        val longitude: Double,
        override val id: String = UUID.randomUUID().toString(),
        override val label: String = "",
        override val color: Long = 0xFF4CAF50,
        override val createdAt: Long = System.currentTimeMillis()
    ) : MapOverlay(id, label, color, createdAt)

    data class LineOverlay(
        val points: List<Pair<Double, Double>>,
        override val id: String = UUID.randomUUID().toString(),
        override val label: String = "",
        override val color: Long = 0xFF2196F3,
        override val createdAt: Long = System.currentTimeMillis()
    ) : MapOverlay(id, label, color, createdAt)

    data class PolygonOverlay(
        val points: List<Pair<Double, Double>>,
        override val id: String = UUID.randomUUID().toString(),
        override val label: String = "",
        override val color: Long = 0xFFFF9800,
        override val createdAt: Long = System.currentTimeMillis()
    ) : MapOverlay(id, label, color, createdAt)
}

/**
 * Renders [MapOverlay] objects onto an osmdroid [MapView] using osmdroid's built-in
 * [Marker], [Polyline], and [Polygon] overlay types.
 */
object MapOverlayRenderer {

    /**
     * Apply a list of [MapOverlay]s to the given [MapView].
     * Removes any previously-applied drawing overlays first, then adds fresh ones.
     */
    fun applyOverlays(mapView: MapView, overlays: List<MapOverlay>) {
        // Remove our custom drawing overlays (keep osmdroid built-ins like my-location)
        mapView.overlays.removeAll { it is DrawingOverlayTag }
        mapView.overlays.removeAll { it is Marker && (it.relatedObject as? String)?.startsWith("drawing_") == true }

        overlays.forEach { overlay ->
            when (overlay) {
                is MapOverlay.PointOverlay -> addPointMarker(mapView, overlay)
                is MapOverlay.LineOverlay -> addPolyline(mapView, overlay)
                is MapOverlay.PolygonOverlay -> addPolygon(mapView, overlay)
            }
        }
        mapView.invalidate()
    }

    /**
     * Converts a [MapOverlay] list to a JSON-like string for persistence.
     */
    fun serializeOverlays(overlays: List<MapOverlay>): String {
        return overlays.joinToString("|||") { overlay ->
            val base = "${overlay::class.simpleName}|${overlay.id}|${overlay.label}|${overlay.color}|${overlay.createdAt}"
            when (overlay) {
                is MapOverlay.PointOverlay -> "$base|${overlay.latitude}|${overlay.longitude}"
                is MapOverlay.LineOverlay -> "$base|${overlay.points.joinToString(";") { "${it.first},${it.second}" }}"
                is MapOverlay.PolygonOverlay -> "$base|${overlay.points.joinToString(";") { "${it.first},${it.second}" }}"
            }
        }
    }

    /**
     * Parses a serialized overlay string back into a list of [MapOverlay].
     */
    fun deserializeOverlays(data: String): List<MapOverlay> {
        if (data.isBlank()) return emptyList()
        return data.split("|||").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size < 5) return@mapNotNull null
            val type = parts[0]
            val id = parts[1]
            val label = parts[2]
            val color = parts[3].toLongOrNull() ?: 0xFF4CAF50
            val createdAt = parts[4].toLongOrNull() ?: System.currentTimeMillis()
            when (type) {
                "PointOverlay" -> if (parts.size >= 7) {
                    val lat = parts[5].toDoubleOrNull() ?: return@mapNotNull null
                    val lon = parts[6].toDoubleOrNull() ?: return@mapNotNull null
                    MapOverlay.PointOverlay(lat, lon, id, label, color, createdAt)
                } else null
                "LineOverlay", "PolygonOverlay" -> if (parts.size >= 6) {
                    val points = parsePoints(parts[5])
                    if (type == "LineOverlay") MapOverlay.LineOverlay(points, id, label, color, createdAt)
                    else MapOverlay.PolygonOverlay(points, id, label, color, createdAt)
                } else null
                else -> null
            }
        }
    }

    private fun parsePoints(data: String): List<Pair<Double, Double>> {
        return data.split(";").mapNotNull { segment ->
            val coords = segment.split(",")
            if (coords.size >= 2) {
                val lat = coords[0].toDoubleOrNull()
                val lon = coords[1].toDoubleOrNull()
                if (lat != null && lon != null) lat to lon else null
            } else null
        }
    }

    private fun addPointMarker(mapView: MapView, overlay: MapOverlay.PointOverlay) {
        val marker = Marker(mapView).apply {
            position = GeoPoint(overlay.latitude, overlay.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = overlay.label.ifBlank { "Site point" }
            snippet = "%.5f, %.5f".format(overlay.latitude, overlay.longitude)
            icon = mapView.context.getDrawable(android.R.drawable.ic_menu_mylocation)?.let { drawable ->
                // Tint with overlay color
                val tinted = drawable.mutate()
                tinted.setTint(overlay.color.toInt())
                tinted
            }
            relatedObject = "drawing_${overlay.id}"
        }
        mapView.overlays.add(marker)
    }

    private fun addPolyline(mapView: MapView, overlay: MapOverlay.LineOverlay) {
        if (overlay.points.size < 2) return
        val polyline = Polyline().apply {
            setPoints(overlay.points.map { GeoPoint(it.first, it.second) })
            outlinePaint.apply {
                color = overlay.color.toInt()
                strokeWidth = 6f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            title = overlay.label.ifBlank { "Transect line" }
            relatedObject = DrawingOverlayTag("line_${overlay.id}")
            geodesic = true
        }
        mapView.overlays.add(polyline)
    }

    private fun addPolygon(mapView: MapView, overlay: MapOverlay.PolygonOverlay) {
        if (overlay.points.size < 3) return
        val polygon = Polygon().apply {
            setPoints(overlay.points.map { GeoPoint(it.first, it.second) })
            fillPaint.apply {
                color = (overlay.color.toInt() and 0x00FFFFFF) or (0x33000000) // 20% alpha
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            outlinePaint.apply {
                color = overlay.color.toInt()
                strokeWidth = 4f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            title = overlay.label.ifBlank { "Survey boundary" }
            relatedObject = DrawingOverlayTag("polygon_${overlay.id}")
        }
        mapView.overlays.add(polygon)
    }
}

/**
 * Tag interface to identify our custom drawing overlays in osmdroid's overlay list.
 */
class DrawingOverlayTag(val drawingId: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DrawingOverlayTag && drawingId == other.drawingId
    }

    override fun hashCode(): Int = drawingId.hashCode()
}

/**
 * Handles the interactive drawing mode — processes taps on the map to build
 * points/lines/polygons and manages the drawing state machine.
 */
class DrawingInputHandler(
    private val mapView: MapView,
    private val mode: () -> DrawingMode,
    private val onPointCreated: (MapOverlay.PointOverlay) -> Unit = {},
    private val onLineCreated: (MapOverlay.LineOverlay) -> Unit = {},
    private val onPolygonCreated: (MapOverlay.PolygonOverlay) -> Unit = {},
    private val onDrawingComplete: () -> Unit = {}
) : Overlay() {

    private val currentPoints = mutableListOf<GeoPoint>()
    private val tempMarkerDrawer = TempMarkerDrawer()

    data class TempMarkerDrawer(
        var markers: List<Marker> = emptyList()
    )

    private var originPoint: Point = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        // Nothing to draw at the overlay level — markers/polylines handle their own rendering
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val currentMode = mode()
        if (currentMode == DrawingMode.View || currentMode == DrawingMode.Select) return false

        // Convert screen pixel coordinates to GeoPoint
        val projection = mapView.projection
        val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

        when (currentMode) {
            DrawingMode.PlacePoint -> {
                onPointCreated(
                    MapOverlay.PointOverlay(
                        latitude = geoPoint.latitude,
                        longitude = geoPoint.longitude,
                        label = "Site point"
                    )
                )
                onDrawingComplete()
            }
            DrawingMode.DrawLine -> {
                currentPoints.add(geoPoint)
                addTempMarker(geoPoint)
                if (currentPoints.size >= 2) {
                    val overlay = createLineFromTemp()
                    if (overlay != null) onLineCreated(overlay)
                    clearTemp()
                    onDrawingComplete()
                }
            }
            DrawingMode.DrawPolygon -> {
                currentPoints.add(geoPoint)
                addTempMarker(geoPoint)
                if (currentPoints.size >= 3 && isNearFirstPoint(geoPoint)) {
                    val overlay = createPolygonFromTemp()
                    if (overlay != null) onPolygonCreated(overlay)
                    clearTemp()
                    onDrawingComplete()
                }
            }
            else -> {}
        }
        return true
    }

    private fun addTempMarker(geoPoint: GeoPoint) {
        val marker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = mapView.context.getDrawable(android.R.drawable.ic_menu_mylocation)
            alpha = 0.6f
        }
        mapView.overlays.add(marker)
        tempMarkerDrawer = tempMarkerDrawer.copy(
            markers = tempMarkerDrawer.markers + marker
        )
    }

    private fun isNearFirstPoint(geoPoint: GeoPoint): Boolean {
        if (currentPoints.isEmpty()) return false
        val first = currentPoints.first()
        val distance = geoPoint.distanceToAsDouble(first)
        // If within ~50 meters of the first point, close the polygon
        return distance < 50.0
    }

    private fun createLineFromTemp(): MapOverlay.LineOverlay? {
        if (currentPoints.size < 2) return null
        return MapOverlay.LineOverlay(
            points = currentPoints.map { it.latitude to it.longitude },
            label = "Transect ${System.currentTimeMillis() % 1000}"
        )
    }

    private fun createPolygonFromTemp(): MapOverlay.PolygonOverlay? {
        if (currentPoints.size < 3) return null
        return MapOverlay.PolygonOverlay(
            points = currentPoints.map { it.latitude to it.longitude },
            label = "Boundary ${System.currentTimeMillis() % 1000}"
        )
    }

    private fun clearTemp() {
        mapView.overlays.removeAll(tempMarkerDrawer.markers)
        tempMarkerDrawer = TempMarkerDrawer()
        currentPoints.clear()
        mapView.invalidate()
    }
}
