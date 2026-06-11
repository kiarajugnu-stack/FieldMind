package chromahub.rhythm.app.features.field.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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
            ?.let { CapturedLocation(it.latitude, it.longitude, it.accuracy.takeIf { accuracy -> accuracy > 0f }, it.provider ?: "device") }
    }
}
