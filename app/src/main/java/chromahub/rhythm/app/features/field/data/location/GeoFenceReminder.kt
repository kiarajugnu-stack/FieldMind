package fieldmind.research.app.features.field.data.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Defines a geo-fence region that triggers reminders when the user enters or exits.
 */
data class GeofenceRegion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 50f,
    val triggerOnEntry: Boolean = true,
    val triggerOnExit: Boolean = false,
    val note: String = "",
    val color: Long = 0xFF4CAF50,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * Manages geo-fence regions that trigger notifications when the user enters or exits
 * marked locations (e.g., sampling sites, transect start/end points).
 *
 * Uses Android's [GeofencingClient] for efficient, battery-friendly location monitoring
 * and delivers geofence transitions via [GeofenceBroadcastReceiver].
 */
class GeoFenceReminder(private val context: Context) {

    // Graceful fallback: GeofencingClient requires Google Play Services.
    // On devices without Play Services (e.g., F-Droid builds), geo-fencing
    // silently degrades — no notifications, no crash.
    private val isPlayServicesAvailable: Boolean
        get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS

    private val geofencingClient: GeofencingClient? = runCatching {
        if (isPlayServicesAvailable) LocationServices.getGeofencingClient(context)
        else null
    }.getOrNull()

    private val _activeRegions = MutableStateFlow<List<GeofenceRegion>>(emptyList())
    val activeRegions: StateFlow<List<GeofenceRegion>> = _activeRegions.asStateFlow()

    private val _lastTriggerEvent = MutableStateFlow<String?>(null)
    val lastTriggerEvent: StateFlow<String?> = _lastTriggerEvent.asStateFlow()

    companion object {
        const val ACTION_GEOFENCE_TRIGGER = "fieldmind.GEOFENCE_TRIGGER"
        const val NOTIFICATION_CHANNEL_ID = "geofence_reminders"
        const val NOTIFICATION_CHANNEL_NAME = "Geo-fence reminders"
        const val REQUEST_CODE = 1002
        const val LOITERING_DELAY_MS = 300000L // 5 minutes

        private var channelCreated = false

        fun ensureNotificationChannel(context: Context) {
            if (channelCreated) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Geo-fence entry/exit reminders"
                    enableVibration(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
            channelCreated = true
        }

        fun createPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            intent.action = ACTION_GEOFENCE_TRIGGER
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    /**
     * Checks whether Google Play Services is available for geo-fencing.
     */
    fun isAvailable(): Boolean = geofencingClient != null && isPlayServicesAvailable

    /**
     * Adds a new geo-fence region and starts monitoring it.
     */
    fun addRegion(region: GeofenceRegion) {
        if (!hasLocationPermission()) return
        val client = geofencingClient ?: return // Play Services unavailable — silently degrade

        // Ensure notification channel is registered
        ensureNotificationChannel(context)

        val geofence = Geofence.Builder()
            .setRequestId(region.id)
            .setCircularRegion(region.latitude, region.longitude, region.radiusMeters)
            .setTransitionTypes(
                (if (region.triggerOnEntry) Geofence.GEOFENCE_TRANSITION_ENTER else 0) or
                    (if (region.triggerOnExit) Geofence.GEOFENCE_TRANSITION_EXIT else 0)
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setLoiteringDelay(LOITERING_DELAY_MS.toInt())
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val pendingIntent = createPendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                geofencingClient?.addGeofences(request, pendingIntent)
            }
        } else {
            runCatching {
                @Suppress("DEPRECATION")
                geofencingClient?.addGeofences(request, pendingIntent)
            }
        }

        _activeRegions.value = _activeRegions.value + region
        saveRegions()
    }

    /**
     * Removes a geo-fence region and stops monitoring it.
     */
    fun removeRegion(regionId: String) {
        runCatching {
            geofencingClient?.removeGeofences(listOf(regionId))
        }
        _activeRegions.value = _activeRegions.value.filter { it.id != regionId }
        saveRegions()
    }

    /**
     * Updates an existing region's parameters.
     */
    fun updateRegion(region: GeofenceRegion) {
        // Remove old, add new with updated parameters
        removeRegion(region.id)
        addRegion(region)
    }

    /**
     * Returns whether any geo-fence is registered.
     */
    fun hasActiveRegions(): Boolean = _activeRegions.value.any { it.isActive }

    /**
     * Toggles a region's active state.
     */
    fun toggleRegion(regionId: String) {
        val index = _activeRegions.value.indexOfFirst { it.id == regionId }
        if (index < 0) return
        val region = _activeRegions.value[index]
        if (region.isActive) {
            removeRegion(regionId)
            _activeRegions.value = _activeRegions.value.toMutableList().apply {
                set(index, region.copy(isActive = false))
            }
        } else {
            val updated = region.copy(isActive = true)
            _activeRegions.value = _activeRegions.value.toMutableList().apply {
                set(index, updated)
            }
            addRegion(updated)
        }
    }

    /**
     * Removes all geo-fence regions.
     */
    fun clearAllRegions() {
        val ids = _activeRegions.value.map { it.id }
        geofencingClient?.let { client ->
            runCatching { client.removeGeofences(ids) }
        }
        _activeRegions.value = emptyList()
        clearRegionsFromStorage()
    }

    /**
     * Restores previously saved regions (called on app start).
     */
    fun restoreRegions() {
        val client = geofencingClient ?: return // Play Services not available
        val saved = loadRegionsFromStorage()
        _activeRegions.value = saved
        saved.filter { it.isActive }.forEach { region ->
            val geofence = Geofence.Builder()
                .setRequestId(region.id)
                .setCircularRegion(region.latitude, region.longitude, region.radiusMeters)
                .setTransitionTypes(
                    (if (region.triggerOnEntry) Geofence.GEOFENCE_TRANSITION_ENTER else 0) or
                        (if (region.triggerOnExit) Geofence.GEOFENCE_TRANSITION_EXIT else 0)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            runCatching {
                client.addGeofences(request, createPendingIntent(context))
            }
        }
    }

    /**
     * Returns regions close to the given location (useful for checking overlapping fences).
     */
    fun regionsNearby(latitude: Double, longitude: Double, maxDistanceMeters: Double = 100.0): List<GeofenceRegion> {
        return _activeRegions.value.filter { region ->
            val dist = haversineDistance(
                TrackPoint(latitude, longitude),
                TrackPoint(region.latitude, region.longitude)
            )
            dist <= maxDistanceMeters
        }
    }

    /**
     * Creates a geo-fence region from a map point overlay.
     */
    fun regionFromPointOverlay(overlay: MapOverlay.PointOverlay, radiusMeters: Float = 50f): GeofenceRegion {
        return GeofenceRegion(
            label = overlay.label.ifBlank { "Site ${System.currentTimeMillis() % 10000}" },
            latitude = overlay.latitude,
            longitude = overlay.longitude,
            radiusMeters = radiusMeters,
            note = overlay.label
        )
    }

    /**
     * Returns regions that were triggered on a given date (for logging).
     */
    fun triggeredRegionsToday(): List<String> {
        val todayStart = run {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        return _triggeredToday.filter { it.value >= todayStart }.keys.toList()
    }

    // ── Private helpers ──

    private val _triggeredToday = mutableMapOf<String, Long>()

    fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val regionsPrefs by lazy {
        context.getSharedPreferences("geofence_regions", Context.MODE_PRIVATE)
    }

    private fun saveRegions() {
        val data = _activeRegions.value.joinToString("|||") { region ->
            "${region.id}|${region.label}|${region.latitude}|${region.longitude}|${region.radiusMeters}|${region.triggerOnEntry}|${region.triggerOnExit}|${region.note}|${region.color}|${region.createdAt}|${region.isActive}"
        }
        regionsPrefs.edit().putString("regions", data).apply()
    }

    private fun loadRegionsFromStorage(): List<GeofenceRegion> {
        val data = regionsPrefs.getString("regions", "") ?: ""
        if (data.isBlank()) return emptyList()
        return data.split("|||").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size < 11) return@mapNotNull null
            try {
                GeofenceRegion(
                    id = parts[0],
                    label = parts[1],
                    latitude = parts[2].toDouble(),
                    longitude = parts[3].toDouble(),
                    radiusMeters = parts[4].toFloat(),
                    triggerOnEntry = parts[5].toBoolean(),
                    triggerOnExit = parts[6].toBoolean(),
                    note = parts[7],
                    color = parts[8].toLong(),
                    createdAt = parts[9].toLong(),
                    isActive = parts[10].toBoolean()
                )
            } catch (_: Exception) { null }
        }
    }

    private fun clearRegionsFromStorage() {
        regionsPrefs.edit().remove("regions").apply()
    }

    /**
     * Notifies the user when a geo-fence transition occurs.
     */
    private fun notifyTransition(regionLabel: String, transitionType: Int) {
        val title = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Arrived at $regionLabel"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Left $regionLabel"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "Still near $regionLabel"
            else -> "Region: $regionLabel"
        }
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText("Geo-fence reminder from FieldMind")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(
                regionLabel.hashCode(),
                notification
            )
        }
    }

    /**
     * Broadcast receiver that handles geo-fence transition events.
     */
    class GeofenceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent == null || geofencingEvent.hasError()) return

            val transitionType = geofencingEvent.geofenceTransition
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            // Notify for each triggered geofence
            triggeringGeofences.forEach { geofence ->
                val label = geofence.requestId
                val reminder = GeoFenceReminder(context)
                reminder.notifyTransition(label, transitionType)
            }
        }
    }
}
