package fieldmind.research.app.features.field.data.vision

import com.google.gson.annotations.SerializedName

/**
 * Perenual API v2 response models.
 * Docs: https://perenual.com/docs/api
 */

// ── Species List Response ──

data class PerenualSpeciesListResponse(
    val data: List<PerenualSpeciesSummary> = emptyList(),
    val to: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    @SerializedName("current_page") val currentPage: Int? = null,
    val from: Int? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    val total: Int? = null
)

data class PerenualSpeciesSummary(
    val id: Int = 0,
    @SerializedName("common_name") val commonName: String? = null,
    @SerializedName("scientific_name") val scientificName: List<String>? = null,
    @SerializedName("other_name") val otherName: List<String>? = null,
    val family: String? = null,
    val genus: String? = null,
    @SerializedName("species_epithet") val speciesEpithet: String? = null,
    @SerializedName("default_image") val defaultImage: PerenualImage? = null
)

// ── Species Details Response ──

data class PerenualSpeciesDetail(
    val id: Int = 0,
    @SerializedName("common_name") val commonName: String? = null,
    @SerializedName("scientific_name") val scientificName: List<String>? = null,
    @SerializedName("other_name") val otherName: List<String>? = null,
    val family: String? = null,
    val genus: String? = null,
    @SerializedName("species_epithet") val speciesEpithet: String? = null,
    val origin: List<String>? = null,
    val type: String? = null,
    val cycle: String? = null,
    val watering: String? = null,
    val sunlight: List<String>? = null,
    val description: String? = null,
    @SerializedName("care_level") val careLevel: String? = null,
    @SerializedName("growth_rate") val growthRate: String? = null,
    val maintenance: String? = null,
    val medicinal: Boolean? = null,
    @SerializedName("poisonous_to_humans") val poisonousToHumans: Boolean? = null,
    @SerializedName("poisonous_to_pets") val poisonousToPets: Boolean? = null,
    val indoor: Boolean? = null,
    val flowers: Boolean? = null,
    val fruits: Boolean? = null,
    @SerializedName("edible_fruit") val edibleFruit: Boolean? = null,
    @SerializedName("flowering_season") val floweringSeason: String? = null,
    @SerializedName("fruiting_season") val fruitingSeason: String? = null,
    val attracts: List<String>? = null,
    val propagation: List<String>? = null,
    val soil: List<String>? = null,
    @SerializedName("pest_susceptibility") val pestSusceptibility: List<String>? = null,
    @SerializedName("default_image") val defaultImage: PerenualImage? = null,
    @SerializedName("other_images") val otherImages: List<PerenualImage>? = null,
    val dimensions: PerenualDimensions? = null,
    val hardiness: PerenualHardiness? = null
)

data class PerenualImage(
    @SerializedName("image_id") val imageId: Int? = null,
    val license: Int? = null,
    @SerializedName("license_name") val licenseName: String? = null,
    @SerializedName("license_url") val licenseUrl: String? = null,
    @SerializedName("original_url") val originalUrl: String? = null,
    @SerializedName("regular_url") val regularUrl: String? = null,
    @SerializedName("medium_url") val mediumUrl: String? = null,
    @SerializedName("small_url") val smallUrl: String? = null,
    val thumbnail: String? = null
)

data class PerenualDimensions(
    val type: String? = null,
    @SerializedName("min_value") val minValue: Double? = null,
    @SerializedName("max_value") val maxValue: Double? = null,
    val unit: String? = null
)

data class PerenualHardiness(
    val min: String? = null,
    val max: String? = null
)
