package fieldmind.research.app.features.field.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val provider: String,
    val capturedAt: Long = System.currentTimeMillis(),
    val placeName: String? = null
) {
    /** Decimal coordinates only, e.g. "12.97160, 77.59456". */
    fun coordinateText(): String = "%.5f, %.5f".format(latitude, longitude)

    fun asDisplayText(): String = placeName?.takeIf { it.isNotBlank() } ?: coordinateText()
}

class FieldLocationProvider(private val context: Context) {
    fun hasAnyLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun lastKnownLocation(): CapturedLocation? {
        if (!hasAnyLocationPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull(Location::getTime)
            ?.toCaptured()
    }

    /**
     * Actively requests a fresh location fix and delivers it on the main thread. Falls back to the
     * most recent cached fix if no new reading arrives within [timeoutMs]. This is the reliable path
     * for capture — [lastKnownLocation] alone is frequently null on real devices.
     */
    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(timeoutMs: Long = 10_000L, onResult: (CapturedLocation?) -> Unit) {
        if (!hasAnyLocationPermission()) { onResult(null); return }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) { onResult(null); return }

        val cached = lastKnownLocation()
        val provider = when {
            runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> LocationManager.GPS_PROVIDER
            runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) { onResult(cached); return }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            val executor = ContextCompat.getMainExecutor(context)
            runCatching {
                manager.getCurrentLocation(provider, signal, executor) { loc ->
                    onResult(loc?.toCaptured() ?: cached)
                }
            }.onFailure { onResult(cached) }
            return
        }

        // API 26-29: single-shot update with a timeout fallback.
        var delivered = false
        val handler = Handler(Looper.getMainLooper())
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (delivered) return
                delivered = true
                runCatching { manager.removeUpdates(this) }
                onResult(location.toCaptured())
            }
            override fun onProviderDisabled(p: String) {}
            override fun onProviderEnabled(p: String) {}
            @Deprecated("Deprecated in Java") override fun onStatusChanged(p: String?, status: Int, extras: Bundle?) {}
        }
        runCatching { manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }
            .onFailure { onResult(cached); return }
        handler.postDelayed({
            if (!delivered) {
                delivered = true
                runCatching { manager.removeUpdates(listener) }
                onResult(cached)
            }
        }, timeoutMs)
    }

    /**
     * Checks whether the device's GPS (or any location provider) is currently enabled.
     * Returns false when GPS is turned off in system settings, even if location
     * permission has been granted.
     */
    fun isGpsEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ||
            runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
    }

    /**
     * Creates an Intent that opens the system Location Settings page so the user
     * can enable GPS / location services.
     */
    fun openLocationSettingsIntent(): android.content.Intent =
        android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    private fun Location.toCaptured(): CapturedLocation =
        CapturedLocation(latitude, longitude, accuracy.takeIf { it > 0f }, provider ?: "device")

    /**
     * Reverse-geocodes coordinates into a short, human-readable place name (e.g.
     * "Bengaluru"). Delivers on the main thread; returns null if geocoding is
     * unavailable or fails. Geocoding may require connectivity, so callers must handle null.
     */
    fun resolvePlaceName(latitude: Double, longitude: Double, onResult: (String?) -> Unit) {
        if (!android.location.Geocoder.isPresent()) { onResult(null); return }
        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                geocoder.getFromLocation(latitude, longitude, 1) { results ->
                    onResult(results.firstOrNull()?.let(::formatPlace))
                }
            }.onFailure { onResult(null) }
        } else {
            Thread {
                val name = runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.let(::formatPlace)
                }.getOrNull()
                Handler(Looper.getMainLooper()).post { onResult(name) }
            }.start()
        }
    }

    /**
     * Formats an [android.location.Address] into a concise place name using only the
     * locality/city — the most stable and recognizable part of an address, without the
     * unreliable feature/street prefix that varies by location.
     */
    private fun formatPlace(address: android.location.Address): String {
        return address.locality
            ?: address.subAdminArea  // district
            ?: address.adminArea     // state
            ?: address.getAddressLine(0)?.let { line ->
                // Strip the street/feature prefix; everything after the first comma
                val parts = line.split(",")
                if (parts.size > 1) parts.drop(1).joinToString(",").trim() else line
            }
            ?: "Unknown place"
    }
}
