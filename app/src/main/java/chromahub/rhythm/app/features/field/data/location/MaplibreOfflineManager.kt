package fieldmind.research.app.features.field.data.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionDefinition
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import org.maplibre.android.geometry.LatLngBounds
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Defines a geographic region with cached tiles for offline use via MapLibre OfflineManager.
 */
data class MaplibreTileRegion(
    val id: String,
    val name: String,
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
    val minZoom: Int = 10,
    val maxZoom: Int = 16,
    val downloadedAt: Long = 0L,
    val tileCount: Long = 0,
    val progress: Float = 0f,
    val isDownloading: Boolean = false
)

/**
 * Manages offline tile caching for MapLibre Maps SDK.
 *
 * Uses [OfflineManager] for downloading and managing tile regions.
 * No API key required — MapLibre is fully open source.
 *
 * Usage:
 *   val manager = MaplibreOfflineManager(context)
 *   manager.downloadRegion("Study Area", north, south, east, west, 10, 16)
 */
class MaplibreOfflineManager(private val context: Context) {

    private val offlineManager: OfflineManager by lazy {
        OfflineManager.getInstance(context)
    }

    private val _cachedRegions = MutableStateFlow<List<MaplibreTileRegion>>(emptyList())
    val cachedRegions: StateFlow<List<MaplibreTileRegion>> = _cachedRegions.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        checkConnectivity()
        refreshCachedRegions()
    }

    /** Check if the device has internet connectivity. */
    fun checkConnectivity() {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val activeNetwork = connectivity?.activeNetworkInfo
        _isOffline.value = activeNetwork == null || !activeNetwork.isConnectedOrConnecting
    }

    /** Returns true if we have cached tiles (offline-capable). */
    fun hasCachedTiles(): Boolean = _cachedRegions.value.isNotEmpty()

    /**
     * Download a tile region for offline use using MapLibre OfflineManager.
     */
    suspend fun downloadRegion(
        name: String,
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int = 10,
        maxZoom: Int = 16
    ): MaplibreTileRegion = withContext(Dispatchers.IO) {
        val id = "region_${System.currentTimeMillis()}"
        val region = MaplibreTileRegion(
            id = id,
            name = name,
            north = north,
            south = south,
            east = east,
            west = west,
            minZoom = minZoom,
            maxZoom = maxZoom,
            isDownloading = true
        )

        _isDownloading.value = true
        _downloadProgress.value = _downloadProgress.value + (id to 0f)

        try {
            // Define the offline region bounds
            val bounds = LatLngBounds.Builder()
                .include(org.maplibre.android.geometry.LatLng(north, west))
                .include(org.maplibre.android.geometry.LatLng(south, east))
                .build()

            val definition: OfflineRegionDefinition = OfflineTilePyramidRegionDefinition(
                "https://demotiles.maplibre.org/style.json",
                bounds,
                minZoom.toDouble(),
                maxZoom.toDouble(),
                context.resources.displayMetrics.density.toFloat()
            )

            // Suspend-coroutine wrapper around the callback-based createOfflineRegion API
            suspendCancellableCoroutine<OfflineRegion> { continuation ->
                offlineManager.createOfflineRegion(
                    definition,
                    byteArrayOf(), // metadata — empty for now
                    object : OfflineManager.CreateOfflineRegionCallback {
                        override fun onCreate(offlineRegion: OfflineRegion) {
                            // Start downloading the region
                            offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                            offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                                override fun onStatusChanged(status: OfflineRegionStatus) {
                                    // Calculate progress from completed/total resources
                                    val pct = if (status.requiredResourceCount > 0)
                                        status.completedResourceCount.toFloat() / status.requiredResourceCount.toFloat()
                                    else 0f
                                    _downloadProgress.value = _downloadProgress.value + (id to pct)
                                    if (status.isComplete) {
                                        continuation.resume(offlineRegion)
                                    }
                                }

                                override fun onError(error: OfflineRegionError) {
                                    continuation.resumeWithException(
                                        RuntimeException("Offline region error: ${error.message}")
                                    )
                                }

                                override fun mapboxTileCountLimitExceeded(limit: Long) {
                                    // Continue anyway with what we have
                                }
                            })
                        }

                        override fun onError(error: String) {
                            continuation.resumeWithException(
                                RuntimeException("Failed to create offline region: $error")
                            )
                        }
                    }
                )
            }

            val completed = region.copy(
                downloadedAt = System.currentTimeMillis(),
                progress = 1f,
                isDownloading = false
            )

            _downloadProgress.value = _downloadProgress.value + (id to 1f)
            saveRegionToStorage(completed)
            refreshCachedRegions()
            completed
        } catch (e: Exception) {
            _isDownloading.value = false
            _downloadProgress.value = _downloadProgress.value - id
            throw e
        } finally {
            _isDownloading.value = false
        }
    }

    /**
     * Remove a cached tile region from the metadata list.
     *
     * MapLibre's [OfflineRegion] uses internal database IDs not exposed in the public API,
     * so selective tile deletion is not possible via the SDK. This method removes the region
     * metadata from storage so it no longer shows in the UI. The actual tile data on disk
     * will be cleaned up when [clearAllCaches] is called explicitly.
     */
    suspend fun removeRegion(regionId: String) = withContext(Dispatchers.IO) {
        val kept = _cachedRegions.value.filter { it.id != regionId }
        clearRegionsFromStorage()
        _cachedRegions.value = emptyList()
        kept.forEach { saveRegionToStorage(it) }
        _cachedRegions.value = kept
        refreshCachedRegions()
    }

    /**
     * Get total cache size on disk by walking the MapLibre cache directory.
     */
    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val dir = context.filesDir.resolve("maplibre_offline")
        if (dir.exists()) {
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            // Fall back to app-level cache if MapLibre hasn't created its dir yet
            val cacheDir = context.cacheDir.resolve("maplibre")
            if (cacheDir.exists()) cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            else 0L
        }
    }

    /**
     * Clear all cached tiles and regions.
     */
    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Unit> { continuation ->
            offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(regions: Array<OfflineRegion>?) {
                    if (regions.isNullOrEmpty()) {
                        continuation.resume(Unit)
                        return
                    }
                    var deleted = 0
                    regions.forEach { region ->
                        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                            override fun onDelete() {
                                deleted++
                                if (deleted >= regions.size) {
                                    continuation.resume(Unit)
                                }
                            }

                            override fun onError(error: String) {
                                deleted++
                                if (deleted >= regions.size) {
                                    continuation.resume(Unit)
                                }
                            }
                        })
                    }
                }

                override fun onError(error: String) {
                    // Fallback: delete the cache directory
                    runCatching {
                        val dir = context.filesDir.resolve("maplibre_offline")
                        dir.deleteRecursively()
                    }
                    continuation.resume(Unit)
                }
            })
        }
        _cachedRegions.value = emptyList()
        _downloadProgress.value = emptyMap()
        clearRegionsFromStorage()
    }

    /**
     * Refresh the cached regions list from storage.
     */
    fun refreshCachedRegions() {
        val saved = loadRegionsFromStorage()
        _cachedRegions.value = saved
    }

    /**
     * Check if we have connectivity and update the offline state.
     */
    fun checkAndUpdateOfflineState() {
        checkConnectivity()
    }

    // ── Storage helpers ──

    private val regionsPrefs = context.getSharedPreferences("maplibre_offline_regions", Context.MODE_PRIVATE)

    private fun saveRegionToStorage(region: MaplibreTileRegion) {
        regionsPrefs.edit().apply {
            putString("${region.id}_name", region.name)
            putFloat("${region.id}_north", region.north.toFloat())
            putFloat("${region.id}_south", region.south.toFloat())
            putFloat("${region.id}_east", region.east.toFloat())
            putFloat("${region.id}_west", region.west.toFloat())
            putInt("${region.id}_minZoom", region.minZoom)
            putInt("${region.id}_maxZoom", region.maxZoom)
            putLong("${region.id}_downloadedAt", region.downloadedAt)
            putLong("${region.id}_tileCount", region.tileCount)
            putFloat("${region.id}_progress", region.progress)
            apply()
        }
    }

    private fun loadRegionsFromStorage(): List<MaplibreTileRegion> {
        val all = regionsPrefs.all
        val ids = all.keys.filter { it.endsWith("_name") }.map { it.removeSuffix("_name") }
        return ids.mapNotNull { id ->
            try {
                MaplibreTileRegion(
                    id = id,
                    name = regionsPrefs.getString("${id}_name", "Unnamed") ?: "Unnamed",
                    north = regionsPrefs.getFloat("${id}_north", 0f).toDouble(),
                    south = regionsPrefs.getFloat("${id}_south", 0f).toDouble(),
                    east = regionsPrefs.getFloat("${id}_east", 0f).toDouble(),
                    west = regionsPrefs.getFloat("${id}_west", 0f).toDouble(),
                    minZoom = regionsPrefs.getInt("${id}_minZoom", 10),
                    maxZoom = regionsPrefs.getInt("${id}_maxZoom", 16),
                    downloadedAt = regionsPrefs.getLong("${id}_downloadedAt", 0L),
                    tileCount = regionsPrefs.getLong("${id}_tileCount", 0),
                    progress = regionsPrefs.getFloat("${id}_progress", 0f)
                )
            } catch (_: Exception) { null }
        }
    }

    private fun removeRegionFromStorage(regionId: String) {
        val keys = regionsPrefs.all.keys.filter { it.startsWith("${regionId}_") }
        regionsPrefs.edit().apply {
            keys.forEach { remove(it) }
            apply()
        }
    }

    private fun clearRegionsFromStorage() {
        val keys = regionsPrefs.all.keys.filter {
            it.endsWith("_name") || it.endsWith("_north") ||
            it.endsWith("_south") || it.endsWith("_east") ||
            it.endsWith("_west") || it.endsWith("_progress")
        }
        regionsPrefs.edit().apply {
            keys.forEach { remove(it) }
            apply()
        }
    }
}
