package fieldmind.research.app.features.field.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.io.FileWriter

/**
 * A single recorded GPS point in a track.
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "gps"
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
}

/**
 * A complete track recording session.
 */
data class TrackRecording(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Track ${System.currentTimeMillis() % 10000}",
    val sessionId: Long? = null,
    val points: List<TrackPoint> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val distanceMeters: Double = 0.0,
    val totalDurationMs: Long = 0L,
    val isPaused: Boolean = false
) {
    val isActive: Boolean get() = endedAt == null

    /** Computes total distance in meters using the Haversine formula. */
    fun computeDistance(): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            total += haversineDistance(points[i - 1], points[i])
        }
        return total
    }

    val polylineOverlay: Polyline
        get() = Polyline().apply {
            setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
        }
}

/**
 * Manages recording GPS tracks during field research sessions.
 *
 * Records location points at configurable intervals and can save/export tracks.
 * Integrates with ResearchSessionEntity for session-level track associating.
 */
class TrackRecorder(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _currentRecording = MutableStateFlow<TrackRecording?>(null)
    val currentRecording: StateFlow<TrackRecording?> = _currentRecording.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _savedTracks = MutableStateFlow<List<TrackRecording>>(emptyList())
    val savedTracks: StateFlow<List<TrackRecording>> = _savedTracks.asStateFlow()

    private var recordingHandler: Handler? = null
    private var locationListener: LocationListener? = null

    companion object {
        private const val MIN_TIME_MS = 2000L       // 2 seconds between updates
        private const val MIN_DISTANCE_M = 2f        // 2 meters between updates
        private const val DEFAULT_MIN_ACCURACY = 30f // Ignore points worse than 30m
        const val NOTIFICATION_CHANNEL_ID = "track_recording"
        const val NOTIFICATION_CHANNEL_NAME = "Track Recording"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_TRACKING = "fieldmind.STOP_TRACK_RECORDING"

        private var channelCreated = false

        fun ensureNotificationChannel(context: Context) {
            if (channelCreated) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows ongoing track recording status"
                    setShowBadge(false)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
            channelCreated = true
        }
    }

    /**
     * Starts a new track recording session.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(name: String? = null, sessionId: Long? = null) {
        if (!hasLocationPermission()) return
        if (_isRecording.value) return

        // Ensure notification channel is registered before showing notifications
        ensureNotificationChannel(context)

        val recording = TrackRecording(
            name = name ?: "Track ${System.currentTimeMillis() % 10000}",
            sessionId = sessionId
        )
        _currentRecording.value = recording
        _isRecording.value = true

        showRecordingNotification(recording)

        locationListener = createLocationListener()
        val provider = bestProvider()
        if (provider != null) {
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener!!,
                    Looper.getMainLooper()
                )
            }
        }
    }

    /**
     * Stops the current recording and saves it.
     */
    fun stopRecording() {
        val recording = _currentRecording.value ?: return
        removeLocationUpdates()

        val completed = recording.copy(
            endedAt = System.currentTimeMillis(),
            totalDurationMs = System.currentTimeMillis() - recording.startedAt,
            distanceMeters = recording.computeDistance()
        )
        _currentRecording.value = completed
        _isRecording.value = false
        _savedTracks.value = _savedTracks.value + completed

        cancelNotification()
    }

    /**
     * Pauses/resumes the current recording.
     */
    fun togglePause() {
        val recording = _currentRecording.value ?: return
        if (recording.isPaused) {
            removeLocationUpdates()
            val provider = bestProvider()
            if (provider != null) {
                runCatching {
                    locationManager.requestLocationUpdates(
                        provider,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        locationListener!!,
                        Looper.getMainLooper()
                    )
                }
            }
            _currentRecording.value = recording.copy(isPaused = false)
        } else {
            removeLocationUpdates()
            _currentRecording.value = recording.copy(isPaused = true)
        }
    }

    /**
     * Deletes a saved track by ID.
     */
    fun deleteTrack(trackId: String) {
        _savedTracks.value = _savedTracks.value.filter { it.id != trackId }
    }

    /**
     * Exports a track to a GPX file for use in other mapping apps.
     */
    suspend fun exportToGpx(trackId: String, outputFile: File? = null): File? = withContext(Dispatchers.IO) {
        val track = _savedTracks.value.find { it.id == trackId } ?: return@withContext null
        val file = outputFile ?: File(
            context.getExternalFilesDir(null),
            "tracks/${track.name}.gpx"
        )
        file.parentFile?.mkdirs()

        FileWriter(file).use { writer ->
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            writer.write("<gpx version=\"1.1\" creator=\"FieldMind\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
            writer.write("  <trk>\n")
            writer.write("    <name>${track.name}</name>\n")
            writer.write("    <trkseg>\n")
            track.points.forEach { pt ->
                writer.write("      <trkpt lat=\"${pt.latitude}\" lon=\"${pt.longitude}\">\n")
                pt.altitude?.let { writer.write("        <ele>$it</ele>\n") }
                writer.write("        <time>${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date(pt.timestamp))}</time>\n")
                writer.write("      </trkpt>\n")
            }
            writer.write("    </trkseg>\n")
            writer.write("  </trk>\n")
            writer.write("</gpx>\n")
        }
        file
    }

    /**
     * Gets the total distance recorded across all sessions today.
     */
    fun todayDistanceMeters(): Double {
        val todayStart = run {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        return _savedTracks.value
            .filter { it.startedAt >= todayStart }
            .sumOf { it.distanceMeters }
    }

    // ── Private helpers ──

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun bestProvider(): String? {
        return when {
            runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> LocationManager.GPS_PROVIDER
            runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    private fun createLocationListener(): LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.accuracy > DEFAULT_MIN_ACCURACY) return
            val recording = _currentRecording.value ?: return
            if (recording.isPaused) return

            val point = TrackPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude.takeIf { it != 0.0 },
                accuracy = location.accuracy,
                speed = location.speed.takeIf { it >= 0f },
                bearing = location.bearing.takeIf { it >= 0f },
                provider = location.provider ?: "gps"
            )
            _currentRecording.value = recording.copy(
                points = recording.points + point,
                distanceMeters = recording.computeDistance()
            )
        }

        override fun onProviderDisabled(p: String) {}
        override fun onProviderEnabled(p: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(p: String?, status: Int, extras: Bundle?) {}
    }

    private fun removeLocationUpdates() {
        locationListener?.let { runCatching { locationManager.removeUpdates(it) } }
    }

    private fun showRecordingNotification(recording: TrackRecording) {
        val stopIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_STOP_TRACKING),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking: ${recording.name}")
            .setContentText("Recording GPS track…")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun cancelNotification() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}

/**
 * Calculates the Haversine distance between two track points in meters.
 */
internal fun haversineDistance(p1: TrackPoint, p2: TrackPoint): Double {
    val R = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(p2.latitude - p1.latitude)
    val dLon = Math.toRadians(p2.longitude - p1.longitude)
    val a = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
        Math.sin(dLon / 2).let { it * it }
    return R * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
}
