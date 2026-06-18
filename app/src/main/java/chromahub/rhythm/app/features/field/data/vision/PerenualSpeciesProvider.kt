package fieldmind.research.app.features.field.data.vision

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Provider for the Perenual API (v2) — plant & botany species reference data.
 *
 * Base URL: https://perenual.com/api/v2/
 * Auth: API key passed as query parameter `key`
 * Free tier: 100 requests/day, 3,000+ species
 *
 * Docs: https://perenual.com/docs/api
 *
 * This provider enables:
 * - Searching species by common/scientific name
 * - Fetching detailed species info (description, taxonomy, care guides, images)
 * - Enriching on-device species identification results with cloud data
 */
class PerenualSpeciesProvider(
    private val apiKey: String
) {
    companion object {
        private const val TAG = "PerenualProvider"
        private const val BASE_URL = "https://perenual.com/api/v2"
        private const val MAX_RESULTS = 10
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Whether this provider has a valid API key configured.
     */
    val isAvailable: Boolean get() = apiKey.isNotBlank()

    /**
     * Search species by name query.
     *
     * @param query Search term (common name or scientific name).
     * @param page Page number for pagination (default 1).
     * @return List of species summaries, or empty list on failure.
     */
    suspend fun searchSpecies(
        query: String,
        page: Int = 1
    ): List<PerenualSpeciesSummary> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !isAvailable) return@withContext emptyList()

        try {
            val url = "$BASE_URL/species-list?key=$apiKey" +
                "&q=${java.net.URLEncoder.encode(query.trim(), "UTF-8")}" +
                "&page=$page&order=asc"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FieldMind/1.0 (field-research-app; perenual-provider)")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "searchSpecies HTTP ${response.code} for query=$query")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val parsed = gson.fromJson(body, PerenualSpeciesListResponse::class.java)
            parsed.data.take(MAX_RESULTS)
        } catch (e: Exception) {
            Log.e(TAG, "searchSpecies failed for query=$query", e)
            emptyList()
        }
    }

    /**
     * Fetch detailed species information by Perenual species ID.
     *
     * @param speciesId The numeric species ID from Perenual.
     * @return Detailed species info, or null on failure.
     */
    suspend fun getSpeciesDetail(speciesId: Int): PerenualSpeciesDetail? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null

        try {
            val url = "$BASE_URL/species/details/$speciesId?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FieldMind/1.0 (field-research-app; perenual-provider)")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "getSpeciesDetail HTTP ${response.code} for id=$speciesId")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            gson.fromJson(body, PerenualSpeciesDetail::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "getSpeciesDetail failed for id=$speciesId", e)
            null
        }
    }

    /**
     * Search Perenual and convert results to SpeciesRecord format.
     * Useful for enriching the species database with cloud data.
     */
    suspend fun searchAsRecords(query: String): List<SpeciesRecord> = withContext(Dispatchers.IO) {
        val results = searchSpecies(query)
        results.mapNotNull { summary ->
            val sciName = summary.scientificName?.firstOrNull() ?: ""
            val commonName = summary.commonName ?: summary.speciesEpithet ?: return@mapNotNull null

            SpeciesRecord(
                id = "perenual_${summary.id}",
                commonName = commonName,
                scientificName = sciName,
                category = suggestCategoryFromPerenual(summary),
                description = "",
                imageUrl = summary.defaultImage?.regularUrl ?: "",
                thumbnailUrl = summary.defaultImage?.thumbnail ?: "",
                family = summary.family ?: "",
                genus = summary.genus ?: "",
                // Perenual is plant-focused, so default to Plant category
                kingdom = "Plantae",
                continents = emptyList()
            )
        }
    }

    /**
     * Enrich a species match with Perenual detail data.
     * Returns a copy of the match with extended description if available.
     */
    suspend fun enrichMatch(match: SpeciesMatch): SpeciesMatch? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null

        // Search for the species by common name first
        val results = searchSpecies(match.commonName.take(40), page = 1)
        if (results.isEmpty()) return@withContext null

        val bestMatch = results.firstOrNull { summary ->
            val sciNames = summary.scientificName?.map { it.lowercase() } ?: emptyList()
            val common = summary.commonName?.lowercase() ?: ""
            common == match.commonName.lowercase() ||
                sciNames.any { it == match.scientificName.lowercase() }
        } ?: results.first()

        // Fetch full details
        val detail = getSpeciesDetail(bestMatch.id) ?: return@withContext null

        // Build an enriched description
        val enrichedDesc = buildString {
            append(match.description.ifBlank { detail.description ?: "" })
            if (detail.family != null) append("\nFamily: ${detail.family}")
            if (detail.origin?.isNotEmpty() == true) append("\nOrigin: ${detail.origin.joinToString(", ")}")
            if (detail.careLevel != null) append("\nCare level: ${detail.careLevel}")
            if (detail.cycle != null) append("\nCycle: ${detail.cycle}")
            if (detail.watering != null) append("\nWatering: ${detail.watering}")
            if (detail.sunlight?.isNotEmpty() == true) append("\nSunlight: ${detail.sunlight.joinToString(", ")}")
        }.trim()

        match.copy(
            description = enrichedDesc,
            imageUrl = detail.defaultImage?.regularUrl ?: match.imageUrl
        )
    }

    private fun suggestCategoryFromPerenual(summary: PerenualSpeciesSummary): String {
        val commonName = summary.commonName?.lowercase() ?: ""
        // Perenual is plant/botany focused; we can try to infer if it's a tree, flower, etc.
        return "Plant"
    }
}
