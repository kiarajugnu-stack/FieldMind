package fieldmind.research.app.features.field.data.location

import android.content.Context
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Polygon
import com.mapbox.geojson.Position
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.Style
import com.mapbox.maps.extension.offline.OfflineManager
import com.mapbox.maps.extension.offline.model.*
import com.mapbox.maps.extension.offline.observers.monitor
import com.mapbox.maps.extension.offline.TileStore
import com.mapbox.maps.extension.offline.TileStoreOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Defines a geographic region to cache tiles for offline use via Mapbox TileStore.
 */
data class MapboxTileRegion(
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
) {
    /** GeoJSON polygon geometry for Mapbox TileRegionLoadOptions. */
    val geometryPolygon: Polygon by lazy {
        Polygon.fromLngLats(
            listOf(
                listOf(
                    Position.fromLngLat(east, north),
                    Position.fromLngLat(east, south),
                    Position.fromLngLat(west, south),
                    Position.fromLngLat(west, north),
                    Position.fromLngLat(east, north)
                )
            )
        )
    }
}

/**
 * Manages offline tile caching for Mapbox Maps SDK.
 *
 * Uses [TileStore] for downloading and managing tile regions, and [OfflineManager]
 * for style pack downloads. Requires a valid Mapbox access token to be set as a
 * string resource `mapbox_access_token` (see app/src/main/res/values/mapbox_access_token.xml).
 *
 * Usage:
 *   val manager = MapboxOfflineManager(context)
 *   manager.downloadRegion("Study Area", north, south, east, west, 10, 16)
 */
class MapboxOfflineManager(private val context: Context) {

    private val tileStore: TileStore by lazy {
        TileStore.create(context.filesDir.resolve("mapbox_offline").also { it.mkdirs() })
    }

    private val offlineManager: OfflineManager by lazy {
        OfflineManager(context)
    }

    private val _cachedRegions = MutableStateFlow<List<MapboxTileRegion>>(emptyList())
    val cachedRegions: StateFlow<List<MapboxTileRegion>> = _cachedRegions.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        // Set a 500 MB disk quota for the tile store
        tileStore.setOption(TileStoreOptions.DISK_QUOTA, 500L * 1024 * 1024)
        // Check connectivity
        checkConnectivity()
        // Restore saved regions metadata
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
     * Download a tile region for offline use.
     * Uses Mapbox TileStore.loadTileRegion() with a GeoJSON geometry boundary.
     */
    suspend fun downloadRegion(
        name: String,
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int = 10,
        maxZoom: Int = 16
    ): MapboxTileRegion = withContext(Dispatchers.IO) {
        val id = "region_${System.currentTimeMillis()}"
        val region = MapboxTileRegion(
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
            // Build tile region load options
            val geometry = region.geometryPolygon
            val descriptorOptions = TilesetDescriptorOptions.Builder()
                .styleURI(Style.MAPBOX_STREETS)
                .minZoom(minZoom.toDouble())
                .maxZoom(maxZoom.toDouble())
                .build()

            val tilesetDescriptor = offlineManager.createTilesetDescriptor(descriptorOptions)

            val loadOptions = TileRegionLoadOptions.Builder()
                .geometry(geometry)
                .descriptors(listOf(tilesetDescriptor))
                .acceptExpired(true)
                .metadata(mapOf("name" to name).toMap().mapValues { it.value.toString() })
                .build()

            // Suspend-coroutine wrapper around the callback-based TileRegion API
            val resultTileRegion = suspendCancellableCoroutine<TileRegion> { continuation ->
                tileStore.loadTileRegion(
                    id = id,
                    options = loadOptions,
                    onProgress = { progress ->
                        val pct = (progress?.percentage ?: 0).toFloat() / 100f
                        _downloadProgress.value = _downloadProgress.value + (id to pct)
                    },
                    onFinished = { result ->
                        if (result.isValue) {
                            continuation.resume(result.value!!)
                        } else {
                            continuation.resumeWithException(
                                RuntimeException("Tile region download failed: ${result.error?.message}")
                            )
                        }
                    }
                )
            }

            val completed = region.copy(
                downloadedAt = System.currentTimeMillis(),
                tileCount = resultTileRegion.estimatedSize ?: 0L,
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
     * Remove a cached tile region.
     */
    suspend fun removeRegion(regionId: String) = withContext(Dispatchers.IO) {
        runCatching {
            tileStore.removeTileRegion(regionId)
        }
        removeRegionFromStorage(regionId)
        refreshCachedRegions()
    }

    /**
     * Get total cache size on disk.
     */
    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val dir = context.filesDir.resolve("mapbox_offline")
        if (dir.exists()) {
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    /**
     * Clear all cached tiles and regions.
     */
    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        // Remove all regions
        val allIds = _cachedRegions.value.map { it.id }
        allIds.forEach { id ->
            runCatching { tileStore.removeTileRegion(id) }
        }
        _cachedRegions.value = emptyList()
        _downloadProgress.value = emptyMap()
        // Also remove the tile store directory
        val dir = context.filesDir.resolve("mapbox_offline")
        runCatching { dir.deleteRecursively() }
        clearRegionsFromStorage()
        // Recreate empty tile store
        dir.mkdirs()
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

    private val regionsPrefs by lazy {
        context.getSharedPreferences("mapbox_offline_regions", Context.MODE_PRIVATE)
    }

    private fun saveRegionToStorage(region: MapboxTileRegion) {
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

    private fun loadRegionsFromStorage(): List<MapboxTileRegion> {
        val all = regionsPrefs.all
        val ids = all.keys.filter { it.endsWith("_name") }.map { it.removeSuffix("_name") }
        return ids.mapNotNull { id ->
            try {
                MapboxTileRegion(
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
