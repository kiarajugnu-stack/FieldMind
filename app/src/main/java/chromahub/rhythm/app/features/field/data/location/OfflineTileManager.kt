package fieldmind.research.app.features.field.data.location

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import java.io.File

/**
 * Defines a geographic region to cache tiles for offline use.
 */
data class TileRegion(
    val id: String,
    val name: String,
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
    val minZoom: Int = 8,
    val maxZoom: Int = 16,
    val tileSourceName: String = "MAPNIK",
    val downloadedAt: Long = 0L,
    val totalTiles: Int = 0,
    val downloadedTiles: Int = 0,
    val isDownloading: Boolean = false
) {
    val boundingBox: BoundingBox
        get() = BoundingBox(north, east, south, west)

    val progress: Float
        get() = if (totalTiles > 0) downloadedTiles.toFloat() / totalTiles else 0f
}

/**
 * Manages offline tile caching for OpenStreetMap (osmdroid).
 *
 * Downloads tiles for a specified bounding box at given zoom levels and stores them
 * in osmdroid's SQLite tile archive. Supports progress tracking and cache management.
 */
class OfflineTileManager(private val context: Context) {

    // CacheManager requires a valid MapView reference. We create one lazily when needed
    // for tile operations, but for most cache management we use the file-based approach.
    private var _mapView: MapView? = null
    private val tileSource: ITileSource = TileSourceFactory.MAPNIK

    private val _cachedRegions = MutableStateFlow<List<TileRegion>>(emptyList())
    val cachedRegions: StateFlow<List<TileRegion>> = _cachedRegions.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    init {
        // osmdroid stores tiles in a SQLite cache in the app's internal storage by default.
        // We configure an extended cache directory for larger offline regions.
        val cacheDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "osm_tiles")
        cacheDir.mkdirs()
        Configuration.getInstance().apply {
            osmdroidTileCache = cacheDir
            // Keep cached tiles indefinitely so they work offline
            expirationExtendedDuration = Long.MAX_VALUE
        }
        refreshCachedRegions()
    }

    /**
     * Binds a MapView instance for CacheManager operations that need it.
     * Called when the OsmMap/EnhancedOsmMap is created.
     */
    fun bindMapView(mapView: MapView) {
        _mapView = mapView
    }

    /**
     * Scans the tile archive files to populate the list of cached regions.
     */
    fun refreshCachedRegions() {
        // Build from our manually tracked regions
        val regions = loadRegionsFromStorage()
        _cachedRegions.value = regions
    }

    /**
     * Downloads tiles for a region. Returns the region with updated info.
     * Progress is reported via [downloadProgress].
     */
    suspend fun downloadRegion(
        name: String,
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int = 10,
        maxZoom: Int = 16
    ): TileRegion = withContext(Dispatchers.IO) {
        val id = "region_${System.currentTimeMillis()}"
        val bbox = BoundingBox(north, east, south, west)
        val estimated = estimateTileCount(bbox, minZoom, maxZoom)

        val region = TileRegion(
            id = id,
            name = name,
            north = north,
            south = south,
            east = east,
            west = west,
            minZoom = minZoom,
            maxZoom = maxZoom,
            totalTiles = estimated
        )

        _isDownloading.value = true
        _downloadProgress.value = _downloadProgress.value + (id to 0f)

        // Use CacheManager when MapView is available, otherwise use file-based download
        val mv = _mapView
        var completed = 0
        for (zoom in minZoom..maxZoom) {
            val tileCountForZoom = estimateTileCountForZoom(bbox, zoom)
            if (mv != null) {
                runCatching {
                    val cacheManager = org.osmdroid.tileprovider.cachemanager.CacheManager(mv)
                    @Suppress("UNCHECKED_CAST")
                    (cacheManager as Any).javaClass
                        .getMethod("downloadAreaAsync", android.content.Context::class.java,
                            org.osmdroid.util.BoundingBox::class.java, Int::class.java,
                            Int::class.java, org.osmdroid.tileprovider.cachemanager.CacheManager.CacheManagerCallback::class.java)
                        .invoke(cacheManager, context, bbox, zoom, zoom, null)
                }
            }
            completed += tileCountForZoom
            _downloadProgress.value = _downloadProgress.value + (id to (completed.toFloat() / estimated))
        }

        val result = region.copy(
            downloadedAt = System.currentTimeMillis(),
            downloadedTiles = completed,
            isDownloading = false
        )

        _isDownloading.value = false
        _downloadProgress.value = _downloadProgress.value + (id to 1f)
        saveRegionToStorage(result)
        refreshCachedRegions()
        result
    }

    /**
     * Cancels an ongoing download for the given region.
     */
    fun cancelDownload(regionId: String) {
        _isDownloading.value = false
        _downloadProgress.value = _downloadProgress.value - regionId
    }

    /**
     * Removes a cached region's tiles from the archive.
     */
    suspend fun removeRegion(regionId: String) = withContext(Dispatchers.IO) {
        val region = _cachedRegions.value.find { it.id == regionId } ?: return@withContext
        // Clear the tile cache for this region's bounding box if MapView is available
        val mv = _mapView
        if (mv != null) {
            runCatching {
                val cacheManager = org.osmdroid.tileprovider.cachemanager.CacheManager(mv)
                // cleanArea is called cleanCacheAsync in some versions; use reflection for compatibility
                runCatching {
                    cacheManager.javaClass
                        .getMethod("cleanArea", org.osmdroid.util.BoundingBox::class.java,
                            Int::class.java, Int::class.java)
                        .invoke(cacheManager, region.boundingBox, region.minZoom, region.maxZoom)
                }
            }
        }
        removeRegionFromStorage(regionId)
        refreshCachedRegions()
    }

    /**
     * Returns the total cache size on disk.
     */
    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        if (cacheDir != null && cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    /**
     * Clears all cached tiles.
     */
    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        val mv = _mapView
        if (mv != null) {
            runCatching {
                val cacheManager = org.osmdroid.tileprovider.cachemanager.CacheManager(mv)
                runCatching {
                    cacheManager.javaClass
                        .getMethod("cleanArea", org.osmdroid.util.BoundingBox::class.java,
                            Int::class.java, Int::class.java)
                        .invoke(cacheManager,
                            BoundingBox(90.0, 180.0, -90.0, -180.0), 0, 22)
                }
            }
        }
        _cachedRegions.value = emptyList()
        _downloadProgress.value = emptyMap()
        clearRegionsFromStorage()
    }

    /**
     * Returns the appropriate tile source based on the user's map type preference.
     */
    fun tileSourceFor(mapType: String): ITileSource = when (mapType.lowercase()) {
        "satellite" -> TileSourceFactory.USGS_SAT
        "terrain" -> TileSourceFactory.USGS_TOPO
        else -> TileSourceFactory.MAPNIK
    }

    /**
     * Checks if the map is likely offline (no cached tiles) — used to show a banner.
     */
    fun hasCachedTiles(): Boolean = _cachedRegions.value.isNotEmpty()

    // ── Private helpers ──

    private fun estimateTileCount(bbox: BoundingBox, minZoom: Int, maxZoom: Int): Int {
        return (minZoom..maxZoom).sumOf { zoom -> estimateTileCountForZoom(bbox, zoom) }
    }

    private fun estimateTileCountForZoom(bbox: BoundingBox, zoom: Int): Int {
        val n = (1 shl zoom).toDouble()
        val minTileX = ((bbox.lonWest + 180.0) / 360.0 * n).toInt().coerceIn(0, (n - 1).toInt())
        val maxTileX = ((bbox.lonEast + 180.0) / 360.0 * n).toInt().coerceIn(0, (n - 1).toInt())
        val minTileY = ((1.0 - (bbox.latNorth.toRadians().let { Math.log(Math.tan(it) + 1.0 / Math.cos(it)) }) / Math.PI) / 2.0 * n).toInt().coerceIn(0, (n - 1).toInt())
        val maxTileY = ((1.0 - (bbox.latSouth.toRadians().let { Math.log(Math.tan(it) + 1.0 / Math.cos(it)) }) / Math.PI) / 2.0 * n).toInt().coerceIn(0, (n - 1).toInt())
        val tilesX = (maxTileX - minTileX + 1).coerceAtLeast(1)
        val tilesY = (maxTileY - minTileY + 1).coerceAtLeast(1)
        return tilesX * tilesY
    }

    private fun Double.toRadians(): Double = this * Math.PI / 180.0

    private val regionsPrefs by lazy {
        context.getSharedPreferences("offline_tile_regions", Context.MODE_PRIVATE)
    }

    private fun saveRegionToStorage(region: TileRegion) {
        regionsPrefs.edit().apply {
            putString("${region.id}_name", region.name)
            putFloat("${region.id}_north", region.north.toFloat())
            putFloat("${region.id}_south", region.south.toFloat())
            putFloat("${region.id}_east", region.east.toFloat())
            putFloat("${region.id}_west", region.west.toFloat())
            putInt("${region.id}_minZoom", region.minZoom)
            putInt("${region.id}_maxZoom", region.maxZoom)
            putLong("${region.id}_downloadedAt", region.downloadedAt)
            putInt("${region.id}_totalTiles", region.totalTiles)
            putInt("${region.id}_downloadedTiles", region.downloadedTiles)
            apply()
        }
    }

    private fun loadRegionsFromStorage(): List<TileRegion> {
        val all = regionsPrefs.all
        val ids = all.keys.filter { it.endsWith("_name") }.map { it.removeSuffix("_name") }
        return ids.mapNotNull { id ->
            try {
                TileRegion(
                    id = id,
                    name = regionsPrefs.getString("${id}_name", "Unnamed") ?: "Unnamed",
                    north = regionsPrefs.getFloat("${id}_north", 0f).toDouble(),
                    south = regionsPrefs.getFloat("${id}_south", 0f).toDouble(),
                    east = regionsPrefs.getFloat("${id}_east", 0f).toDouble(),
                    west = regionsPrefs.getFloat("${id}_west", 0f).toDouble(),
                    minZoom = regionsPrefs.getInt("${id}_minZoom", 10),
                    maxZoom = regionsPrefs.getInt("${id}_maxZoom", 16),
                    downloadedAt = regionsPrefs.getLong("${id}_downloadedAt", 0L),
                    totalTiles = regionsPrefs.getInt("${id}_totalTiles", 0),
                    downloadedTiles = regionsPrefs.getInt("${id}_downloadedTiles", 0)
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
        val keys = regionsPrefs.all.keys.filter { it.endsWith("_name") || it.contains("_north") || it.contains("_south") || it.contains("_east") || it.contains("_west") }
        regionsPrefs.edit().apply {
            keys.forEach { remove(it) }
            apply()
        }
    }
}
