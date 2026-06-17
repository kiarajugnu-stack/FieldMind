package fieldmind.research.app.features.field.data.vision

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.concurrent.TimeUnit

/**
 * Species metadata record with identification details.
 */
data class SpeciesRecord(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val category: String,
    val description: String = "",
    val habitat: String = "",
    val diet: String = "",
    val conservationStatus: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    /** Tags for search and categorization (e.g. ["raptor", "migratory", "forest"]) */
    val tags: List<String> = emptyList(),
    /** Visual characteristics for identification hints */
    val keyFeatures: List<String> = emptyList(),
    /** Similar species for disambiguation */
    val similarSpecies: List<String> = emptyList()
)

/**
 * Regional species pack metadata for downloadable model expansions.
 */
data class RegionalPack(
    val regionId: String,
    val regionName: String,
    val description: String,
    val speciesCount: Int,
    val downloadSizeMb: Int,
    val isDownloaded: Boolean = false,
    val modelUrl: String = "",
    val labelsUrl: String = ""
)

/**
 * Bundled species database with search, filtering, and regional pack management.
 *
 * Provides:
 * - Built-in species catalog (~120 species across all major categories)
 * - Full-text search by common name, scientific name, category, and tags
 * - Regional pack download management for expanding species coverage
 * - Category-based browsing
 * - Feature-based identification hints
 */
class SpeciesDatabase(private val context: Context) {

    companion object {
        private const val DB_FILE = "species_catalog.json"
        private const val PACKS_FILE = "regional_packs.json"
        private const val DOWNLOAD_DIR = "species_packs"

        val REGIONAL_PACKS = listOf(
            RegionalPack(
                regionId = "na",
                regionName = "North America",
                description = "5,000+ species across USA, Canada, and Mexico",
                speciesCount = 5000,
                downloadSizeMb = 45,
                modelUrl = "https://models.fieldmind.app/na_v1.tflite",
                labelsUrl = "https://models.fieldmind.app/na_v1_labels.txt"
            ),
            RegionalPack(
                regionId = "eu",
                regionName = "Europe",
                description = "4,000+ species across European continent",
                speciesCount = 4000,
                downloadSizeMb = 38,
                modelUrl = "https://models.fieldmind.app/eu_v1.tflite",
                labelsUrl = "https://models.fieldmind.app/eu_v1_labels.txt"
            ),
            RegionalPack(
                regionId = "asia",
                regionName = "Asia",
                description = "6,000+ species across Asia and Southeast Asia",
                speciesCount = 6000,
                downloadSizeMb = 52,
                modelUrl = "https://models.fieldmind.app/asia_v1.tflite",
                labelsUrl = "https://models.fieldmind.app/asia_v1_labels.txt"
            ),
            RegionalPack(
                regionId = "tropical",
                regionName = "Tropics (Global)",
                description = "3,500+ tropical and subtropical species",
                speciesCount = 3500,
                downloadSizeMb = 32,
                modelUrl = "https://models.fieldmind.app/tropical_v1.tflite",
                labelsUrl = "https://models.fieldmind.app/tropical_v1_labels.txt"
            )
        )
    }

    private val gson = Gson()

    /** In-memory species catalog, loaded lazily from bundled JSON. */
    private var speciesCache: List<SpeciesRecord>? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // long timeout for large model files
        .followRedirects(true)
        .build()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("species_packs", Context.MODE_PRIVATE)

    /** Downloaded pack metadata, loaded from app storage. */
    private val downloadState = mutableMapOf<String, Boolean>()

    /** Progress callback for ongoing downloads. */
    private var progressListener: ((regionId: String, bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null

    /**
     * Set a progress listener to track download progress.
     */
    fun setProgressListener(listener: ((regionId: String, bytesDownloaded: Long, totalBytes: Long) -> Unit)?) {
        progressListener = listener
    }

    /**
     * Get all species in the catalog (browsable list).
     */
    suspend fun getAll(limit: Int = 50): List<SpeciesRecord> = withContext(Dispatchers.Default) {
        getCatalog().take(limit)
    }

    /**
     * Search species by query string.
     * Searches common name, scientific name, category, and tags.
     * When query is blank, returns all species (up to limit) for browsing.
     */
    suspend fun search(query: String, limit: Int = 30): List<SpeciesRecord> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext getCatalog().take(limit)
        val q = query.trim().lowercase()
        val catalog = getCatalog()
        catalog.filter { entry ->
            entry.commonName.lowercase().contains(q) ||
            entry.scientificName.lowercase().contains(q) ||
            entry.category.lowercase().contains(q) ||
            entry.tags.any { it.lowercase().contains(q) } ||
            entry.habitat.lowercase().contains(q)
        }.take(limit)
    }

    /**
     * Get species by category.
     */
    suspend fun getByCategory(category: String): List<SpeciesRecord> = withContext(Dispatchers.Default) {
        getCatalog().filter { it.category.equals(category, ignoreCase = true) }
    }

    /**
     * Get all available categories with species counts.
     */
    suspend fun getCategories(): List<Pair<String, Int>> = withContext(Dispatchers.Default) {
        getCatalog()
            .groupBy { it.category }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    /**
     * Get a single species by ID.
     */
    suspend fun getById(id: String): SpeciesRecord? = withContext(Dispatchers.Default) {
        getCatalog().firstOrNull { it.id == id }
    }

    /**
     * Get species by a list of feature tags (e.g. ["raptor", "forest"]).
     * Returns species matching ALL specified tags.
     */
    suspend fun findByFeatures(features: List<String>): List<SpeciesRecord> = withContext(Dispatchers.Default) {
        if (features.isEmpty()) return@withContext getCatalog().take(20)
        getCatalog().filter { record ->
            features.all { feature ->
                record.tags.any { it.equals(feature, ignoreCase = true) } ||
                record.keyFeatures.any { it.lowercase().contains(feature.lowercase()) } ||
                record.habitat.lowercase().contains(feature.lowercase())
            }
        }.take(20)
    }

    /**
     * Check if a regional pack is downloaded.
     */
    fun isPackDownloaded(regionId: String): Boolean {
        // Check in-memory first, then SharedPreferences
        return downloadState.getOrElse(regionId) {
            prefs.getBoolean("pack_downloaded_$regionId", false)
        }
    }

    /**
     * Mark a regional pack as downloaded (persisted to SharedPreferences).
     */
    fun markPackDownloaded(regionId: String, downloaded: Boolean) {
        downloadState[regionId] = downloaded
        prefs.edit().putBoolean("pack_downloaded_$regionId", downloaded).apply()
    }

    /**
     * Get all available regional packs with download status.
     */
    fun getRegionalPacks(): List<RegionalPack> {
        return REGIONAL_PACKS.map { pack ->
            pack.copy(isDownloaded = isPackDownloaded(pack.regionId))
        }
    }

    /**
     * Download a regional pack (model + labels) from the configured URLs.
     * Saves files to the app's internal storage and persists download state.
     *
     * @param regionId The region ID to download (e.g. "na", "eu").
     * @return true if download succeeded, false otherwise.
     */
    suspend fun downloadPack(regionId: String): Boolean = withContext(Dispatchers.IO) {
        val pack = REGIONAL_PACKS.firstOrNull { it.regionId == regionId } ?: return@withContext false
        val dir = File(context.filesDir, "$DOWNLOAD_DIR/$regionId")
        dir.mkdirs()

        try {
            // Download model file
            val modelFile = File(dir, "model.tflite")
            downloadFile(pack.modelUrl, modelFile) { downloaded, total ->
                progressListener?.invoke(regionId, downloaded, total)
            }

            // Download labels file
            val labelsFile = File(dir, "labels.txt")
            downloadFile(pack.labelsUrl, labelsFile) { downloaded, total ->
                progressListener?.invoke(regionId, downloaded, total)
            }

            markPackDownloaded(regionId, true)
            true
        } catch (e: Exception) {
            // Clean up partial downloads
            dir.deleteRecursively()
            markPackDownloaded(regionId, false)
            false
        }
    }

    /**
     * Delete a downloaded regional pack from device storage.
     */
    suspend fun deletePack(regionId: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "$DOWNLOAD_DIR/$regionId")
        val result = dir.deleteRecursively()
        markPackDownloaded(regionId, false)
        result
    }

    /**
     * Get the local file path for a downloaded pack's model file, or null if not downloaded.
     */
    fun getPackModelPath(regionId: String): String? {
        if (!isPackDownloaded(regionId)) return null
        val file = File(context.filesDir, "$DOWNLOAD_DIR/$regionId/model.tflite")
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Get the local file path for a downloaded pack's labels file, or null if not downloaded.
     */
    fun getPackLabelsPath(regionId: String): String? {
        if (!isPackDownloaded(regionId)) return null
        val file = File(context.filesDir, "$DOWNLOAD_DIR/$regionId/labels.txt")
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Download a single file from URL to destination, reporting progress.
     */
    @Throws(Exception::class)
    private suspend fun downloadFile(
        url: String,
        destination: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download failed: HTTP ${response.code}")

        val body = response.body ?: throw Exception("Empty response body")
        val totalBytes = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(destination)
        val buffer = ByteArray(8 * 1024) // 8KB buffer
        var bytesRead: Int
        var totalRead = 0L

        inputStream.use { input ->
            outputStream.use { output ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalBytes > 0) {
                        onProgress(totalRead, totalBytes)
                    }
                }
            }
        }
    }

    /**
     * Get total species count in the bundled catalog.
     */
    suspend fun getTotalSpeciesCount(): Int = withContext(Dispatchers.Default) {
        getCatalog().size
    }

    /**
     * Auto-suggest category based on species name keywords.
     */
    fun suggestCategory(speciesName: String): String {
        val n = speciesName.lowercase()
        return when {
            n.contains(Regex("[Bb]ird|[Aa]vian|[Rr]aven|[Ss]parrow|[Cc]row|[Ee]agle|[Ff]alcon|[Hh]awk|[Oo]wl|[Ww]oodpecker|[Cc]ardinal|[Bb]luebird|[Rr]obin|[Jj]ay|[Ww]arbler|[Ff]inch|[Ss]parrow|[Ss]wallow|[Hh]ummingbird|[Mm]ockingbird|[Gg]rackle|[Bb]lackbird|[Dd]ove|[Hh]eron|[Ee]gret|[Dd]uck|[Gg]oose|[Ss]wan|[Ll]oon|[Gg]ull|[Tt]ern|[Oo]sprey|[Kk]ite|[Hh]arrier|[Vv]ulture|[Pp]elican|[Cc]ormorant|[Kk]ingfisher")) -> "Bird"
            n.contains(Regex("[Mm]ammal|[Dd]eer|[Ff]ox|[Bb]ear|[Rr]accoon|[Ss]quirrel|[Mm]ouse|[Rr]abbit|[Cc]oyote|[Ww]olf|[Bb]at|[Oo]tter|[Bb]eaver|[Mm]uskrat|[Cc]hipmunk|[Mm]ole|[Vv]ole|[Ss]kunk|[Oo]possum|[Bb]obcat|[Ll]ynx|[Mm]oose|[Ee]lk|[Bb]ison|[Cc]ougar|[Pp]anther|[Ll]ion|[Tt]iger|[Ll]eopard")) -> "Mammal"
            n.contains(Regex("[Ii]nsect|[Bb]ee|[Bb]utterfly|[Aa]nt|[Bb]eetle|[Mm]oth|[Dd]ragonfly|[Gg]rasshopper|[Cc]aterpillar|[Ll]arva|[Cc]icada|[Mm]antis|[Ll]adybug|[Ff]irefly|[Cc]ricket|[Ww]asp|[Hh]ornet|[Ff]ly|[Mm]osquito|[Dd]amselfly|[Ss]tinkbug|[Ww]eevil|[Tt]ermite")) -> "Insect"
            n.contains(Regex("[Ss]pider|[Tt]ick|[Mm]ite|[Ss]corpion|[Cc]entipede|[Mm]illipede")) -> "Arachnid"
            n.contains(Regex("[Pp]lant|[Tt]ree|[Ff]lower|[Ll]eaf|[Ff]ern|[Mm]oss|[Gg]rass|[Ss]hrub|[Vv]ine|[Oo]ak|[Mm]aple|[Pp]ine|[Ss]pruce|[Ff]ir|[Cc]edar|[Hh]emlock|[Bb]irch|[Bb]eech|[Ee]lm|[Ww]illow|[Aa]sh|[Pp]oplar|[Aa]spen")) -> "Plant"
            n.contains(Regex("[Ff]ungi|[Ff]ungus|[Mm]ushroom|[Mm]orel|[Tt]oadstool|[Ss]helf|[Cc]onk|[Bb]racket|[Ll]ichen|[Mm]old|[Mm]ildew|[Yy]east")) -> "Fungi"
            n.contains(Regex("[Aa]mphibian|[Ff]rog|[Tt]oad|[Ss]alamander|[Nn]ewt|[Tt]adpole|[Ss]iren|[Cc]aecilian")) -> "Amphibian"
            n.contains(Regex("[Rr]eptile|[Ss]nake|[Ll]izard|[Tt]urtle|[Tt]ortoise|[Cc]rocodile|[Aa]lligator|[Gg]ecko|[Ss]kink|[Ii]guana|[Cc]hameleon|[Mm]onitor|[Bb]oa|[Pp]ython|[Vv]iper|[Rr]attlesnake|[Cc]opperhead|[Cc]oral|[Kk]ingsnake")) -> "Reptile"
            n.contains(Regex("[Ff]ish|[Bb]ass|[Tt]rout|[Ss]almon|[Cc]arp|[Cc]atfish|[Ss]unfish|[Bb]luegill|[Cc]rappie|[Pp]erch|[Pp]ike|[Mm]uskellunge|[Ww]alleye|[Ss]turgeon|[Pp]addlefish|[Gg]ar|[Bb]owfin|[Mm]innow|[Ss]hiner|[Dd]ace|[Cc]hub")) -> "Fish"
            n.contains(Regex("[Mm]ollusk|[Ss]nail|[Ss]lug|[Cc]lam|[Oo]yster|[Mm]ussel|[Ss]callop|[Ss]quid|[Oo]ctopus|[Cc]uttlefish|[Nn]autilus|[Cc]hiton|[Ll]impets|[Ww]helk|[Cc]onch|[Cc]owrie")) -> "Mollusk"
            n.contains(Regex("[Cc]rustacean|[Cc]rab|[Ll]obster|[Ss]hrimp|[Cc]rayfish|[Bb]arnacle|[Ii]sopod|[Aa]mphipod|[Kk]rill|[Cc]opepod")) -> "Crustacean"
            else -> "Other"
        }
    }

    private suspend fun getCatalog(): List<SpeciesRecord> = withContext(Dispatchers.Default) {
        if (speciesCache != null) return@withContext speciesCache!!

        // Load from bundled species JSON asset
        val records = try {
            val json = context.assets.open("species/species_catalog.json")
                .bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<SpeciesJsonRecord>>() {}.type
            val raw: List<SpeciesJsonRecord> = gson.fromJson(json, type)
            raw.map { it.toRecord() }
        } catch (_: Exception) {
            // Fall back to built-in records from the classifier
            SpeciesClassifier.FALLBACK_SPECIES.mapIndexed { index, entry ->
                SpeciesRecord(
                    id = "builtin_$index",
                    commonName = entry.commonName,
                    scientificName = entry.scientificName,
                    category = entry.category,
                    description = entry.description,
                    tags = listOf(entry.category.lowercase())
                )
            }
        }

        speciesCache = records
        records
    }

    /**
     * JSON model for the bundled species catalog asset.
     */
    private data class SpeciesJsonRecord(
        @SerializedName("id") val id: String = "",
        @SerializedName("common_name") val commonName: String = "",
        @SerializedName("scientific_name") val scientificName: String = "",
        @SerializedName("category") val category: String = "Other",
        @SerializedName("description") val description: String = "",
        @SerializedName("habitat") val habitat: String = "",
        @SerializedName("diet") val diet: String = "",
        @SerializedName("conservation_status") val conservationStatus: String = "",
        @SerializedName("image_url") val imageUrl: String = "",
        @SerializedName("thumbnail_url") val thumbnailUrl: String = "",
        @SerializedName("tags") val tags: List<String> = emptyList(),
        @SerializedName("key_features") val keyFeatures: List<String> = emptyList(),
        @SerializedName("similar_species") val similarSpecies: List<String> = emptyList()
    ) {
        fun toRecord() = SpeciesRecord(
            id = id.ifBlank { "species_${commonName.lowercase().replace(" ", "_")}" },
            commonName = commonName,
            scientificName = scientificName,
            category = category.ifBlank { "Other" },
            description = description,
            habitat = habitat,
            diet = diet,
            conservationStatus = conservationStatus,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            tags = tags,
            keyFeatures = keyFeatures,
            similarSpecies = similarSpecies
        )
    }
}
