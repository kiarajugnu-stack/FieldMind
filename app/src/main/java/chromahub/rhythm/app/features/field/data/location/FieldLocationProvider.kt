package chromahub.rhythm.app.features.field.data.location

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
    val capturedAt: Long = System.currentTimeMillis()
) {
    fun asDisplayText(): String = buildString {
        append("GPS: %.5f, %.5f".format(latitude, longitude))
        accuracyMeters?.let { append(" • ±${it.toInt()}m") }
        append(" • $provider")
    }
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

    private fun Location.toCaptured(): CapturedLocation =
        CapturedLocation(latitude, longitude, accuracy.takeIf { it > 0f }, provider ?: "device")
}
