package fieldmind.research.app.features.field.data.location

import android.graphics.Color
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
 * These are rendered as osmdroid Marker, Polyline, and Polygon overlays
 * in the OsmMapView composable.
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
 * Handles serialization/deserialization of MapOverlay objects for persistence.
 * Also provides color helper and track overlay creation.
 */
object MapOverlayUtils {

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

    /**
     * Convert a Long ARGB color to android.graphics.Color int.
     */
    fun colorToInt(colorLong: Long): Int = colorLong.toInt()

    /**
     * Create a track overlay from recording points for rendering on the map.
     */
    fun trackOverlayFromPoints(points: List<Pair<Double, Double>>): MapOverlay.LineOverlay {
        return MapOverlay.LineOverlay(
            points = points,
            label = "Current track",
            color = 0xFFFF5252
        )
    }

    /** Android Color int for the observation marker. */
    val observationMarkerColor: Int = Color.parseColor("#4CAF50")
}
