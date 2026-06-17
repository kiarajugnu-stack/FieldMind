package fieldmind.research.app.features.field.data.vision

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persistent storage for user-confirmed species identification fingerprints.
 *
 * Every time a user confirms a species ID from a photo, the perceptual hash
 * (pHash) of that photo is stored alongside the confirmed species name.
 * Future identifications can then be compared against this growing database
 * of known fingerprints, boosting the confidence of previously-confirmed
 * species when a similar image is analyzed.
 *
 * Storage format (in SharedPreferences):
 *   "phash_store" → JSON array of PhashEntry objects
 *
 * All operations are thread-safe via Dispatchers.Default.
 */
class PhashDatabase(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "fieldmind_phash_db"
        private const val STORAGE_KEY = "phash_store"
        private const val DEFAULT_HAMMING_THRESHOLD = 12  // bits — max Hamming distance for "match"
        private const val STRICT_THRESHOLD = 5             // bits — exact-looking match

        /**
         * Confidence boost when a stored pHash matches the current image.
         */
        private const val MATCH_BOOST = 0.25f
        private const val STRICT_BOOST = 0.40f
    }

    private val gson = Gson()
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * An entry in the pHash store: the 64-bit fingerprint and the confirmed species identity.
     */
    data class PhashEntry(
        val phash: Long,
        val speciesName: String,
        val category: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val imageUri: String = ""  // stored for debugging / review
    )

    /**
     * Add a confirmed species identification to the store.
     * Computes the pHash of the image if it hasn't been computed yet.
     *
     * @param imageUri The URI of the photo the user confirmed
     * @param speciesName The common name of the species the user confirmed
     * @param category The category of the species
     * @param precomputedPhash An already-computed pHash (avoids re-analyzing the image)
     */
    suspend fun addConfirmation(
        imageUri: String,
        speciesName: String,
        category: String = "",
        precomputedPhash: Long? = null
    ) = withContext(Dispatchers.Default) {
        val phash = precomputedPhash ?: computePhashFromUri(imageUri) ?: return@withContext

        val entries = getAllEntries().toMutableList()

        // Avoid duplicates — same image + same species
        val exists = entries.any { it.imageUri == imageUri && it.speciesName == speciesName }
        if (exists) return@withContext

        entries.add(
            PhashEntry(
                phash = phash,
                speciesName = speciesName,
                category = category,
                imageUri = imageUri
            )
        )

        saveEntries(entries)
    }

    /**
     * Remove a specific confirmation entry (e.g., if user undoes a misidentification).
     */
    suspend fun removeConfirmation(imageUri: String, speciesName: String) = withContext(Dispatchers.Default) {
        val entries = getAllEntries().toMutableList()
        entries.removeAll { it.imageUri == imageUri && it.speciesName == speciesName }
        saveEntries(entries)
    }

    /**
     * Find all stored species that match a given pHash within the Hamming distance threshold.
     *
     * @param queryPhash The 64-bit perceptual hash to search against
     * @param threshold Maximum Hamming distance for a match (default 12 bits)
     * @return List of matching entries with their Hamming distance, sorted closest-first
     */
    suspend fun findSimilar(
        queryPhash: Long,
        threshold: Int = DEFAULT_HAMMING_THRESHOLD
    ): List<Pair<PhashEntry, Int>> = withContext(Dispatchers.Default) {
        getAllEntries()
            .map { entry -> entry to hammingDistance(queryPhash, entry.phash) }
            .filter { (_, distance) -> distance <= threshold }
            .sortedBy { (_, distance) -> distance }
    }

    /**
     * Compute confidence boosts for a set of species based on stored hash matches.
     * Returns a map of speciesName → (boost amount, isStrictMatch).
     */
    suspend fun computeHashBoosts(queryPhash: Long): Map<String, Pair<Float, Boolean>> =
        withContext(Dispatchers.Default) {
            val matches = findSimilar(queryPhash)
            val boosts = mutableMapOf<String, Pair<Float, Boolean>>()

            for ((entry, distance) in matches) {
                val existing = boosts[entry.speciesName]
                val currentBoost = existing?.first ?: 0f
                val isStrict = existing?.second ?: false

                // Closer match = higher boost
                val boost = if (distance <= STRICT_THRESHOLD) STRICT_BOOST else MATCH_BOOST
                val newIsStrict = isStrict || (distance <= STRICT_THRESHOLD)

                // Progressive boost: each matching entry adds diminishing returns
                val progressiveBoost = currentBoost + boost * (1f - currentBoost)

                boosts[entry.speciesName] = progressiveBoost to newIsStrict
            }

            boosts
        }

    /**
     * Get total count of stored confirmations.
     */
    suspend fun getConfirmationCount(): Int = withContext(Dispatchers.Default) {
        getAllEntries().size
    }

    /**
     * Clear all stored confirmations.
     */
    suspend fun clearAll() = withContext(Dispatchers.Default) {
        prefs.edit().remove(STORAGE_KEY).apply()
    }

    /**
     * Get all stored entries.
     */
    private fun getAllEntries(): List<PhashEntry> {
        val json = prefs.getString(STORAGE_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PhashEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Save entries to SharedPreferences.
     */
    private fun saveEntries(entries: List<PhashEntry>) {
        val json = gson.toJson(entries)
        prefs.edit().putString(STORAGE_KEY, json).apply()
    }

    /**
     * Compute 64-bit pHash from an image URI using SpeciesImageAnalyzer.
     * This is used when the caller hasn't already computed the hash.
     */
    private suspend fun computePhashFromUri(imageUri: String): Long? {
        return try {
            val analyzer = SpeciesImageAnalyzer(context)
            val features = analyzer.analyzeImage(imageUri)
            features?.perceptualHash
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Compute Hamming distance between two 64-bit hashes.
     * Counts the number of differing bits.
     */
    private fun hammingDistance(hash1: Long, hash2: Long): Int {
        var xor = hash1 xor hash2
        var count = 0
        while (xor != 0L) {
            count += (xor and 1L).toInt()
            xor = xor ushr 1
        }
        return count
    }
}
