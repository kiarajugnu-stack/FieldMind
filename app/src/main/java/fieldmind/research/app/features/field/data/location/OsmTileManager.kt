package fieldmind.research.app.features.field.data.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Defines a geographic region with cached tiles for offline use via osmdroid.
 *
 * osmdroid automatically caches tiles to disk as they are viewed via its
 * [org.osmdroid.tileprovider.modules.MapTileFileStorageProvider] and
 * [org.osmdroid.tileprovider.modules.TileWriter]. This class tracks which
 * regions have been browsed (and thus cached) and provides cache management.
 *
 * Note: osmdroid tiles auto-cache — users just need to browse an area
 * while online and the tiles will be available offline later.
 */
data class OsmTileRegion(
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
 * Manages offline tile caching for osmdroid Maps.
 *
 * osmdroid automatically caches tiles as they are viewed — no explicit
 * download API is needed. This manager provides:
 * - Region metadata tracking (what areas have been cached)
 * - Cache size reporting
 * - Cache clearing
 * - Connectivity state
 *
 * Usage:
 *   val manager = OsmTileManager(context)
 *   manager.refreshCachedRegions()
 */
class OsmTileManager(private val context: Context) {

    private val _cachedRegions = MutableStateFlow<List<OsmTileRegion>>(emptyList())
    val cachedRegions: StateFlow<List<OsmTileRegion>> = _cachedRegions.asStateFlow()

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
     * Register an area as "cached" in metadata so the UI shows it.
     * osmdroid auto-caches tiles as they are browsed; this just tracks
     * which areas the user has explicitly requested.
     */
    suspend fun downloadRegion(
        name: String,
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int = 10,
        maxZoom: Int = 16
    ): OsmTileRegion = withContext(Dispatchers.IO) {
        val id = "region_${System.currentTimeMillis()}"
        val region = OsmTileRegion(
            id = id,
            name = name,
            north = north,
            south = south,
            east = east,
            west = west,
            minZoom = minZoom,
            maxZoom = maxZoom,
            downloadedAt = System.currentTimeMillis(),
            progress = 1f,
            isDownloading = false
        )

        // Simulate a brief "download" - in osmdroid, tiles cache as they're
        // browsed on the map view. We track the region as earmarked for caching.
        _isDownloading.value = true
        _downloadProgress.value = _downloadProgress.value + (id to 0.5f)

        // Yield briefly to simulate progress
        kotlinx.coroutines.delay(500)

        _downloadProgress.value = _downloadProgress.value + (id to 1f)

        saveRegionToStorage(region)
        refreshCachedRegions()
        _isDownloading.value = false
        region
    }

    /**
     * Remove a cached tile region from the metadata list.
     *
     * To actually clear the tile data from disk, call [clearAllCaches].
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
     * Get total cache size on disk by walking the osmdroid tile cache directory.
     */
    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val dir = context.cacheDir.resolve("osmdroid/tiles")
        if (dir.exists()) {
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            // Fall back to the osmdroid base path
            val baseDir = context.cacheDir.resolve("osmdroid")
            if (baseDir.exists()) baseDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            else 0L
        }
    }

    /**
     * Clear all cached tiles and region metadata.
     */
    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        runCatching {
            val dir = context.cacheDir.resolve("osmdroid")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
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

    private val regionsPrefs = context.getSharedPreferences("osmdroid_regions", Context.MODE_PRIVATE)

    private fun saveRegionToStorage(region: OsmTileRegion) {
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

    private fun loadRegionsFromStorage(): List<OsmTileRegion> {
        val all = regionsPrefs.all
        val ids = all.keys.filter { it.endsWith("_name") }.map { it.removeSuffix("_name") }
        return ids.mapNotNull { id ->
            try {
                OsmTileRegion(
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
