package fieldmind.research.app.features.field.data.weather

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Open-Meteo provider (free, no API key required, 10,000 requests/day).
 * Uses the Open-Meteo API to fetch current weather and daily forecasts.
 * Supports user-imported custom API configuration JSON for custom endpoints/parameters.
 */
class OpenMeteoProvider : WeatherProvider {
    override val slug: String = "open-meteo"
    override val displayName: String = "Open-Meteo"
    override val requiresApiKey: Boolean = false
    override val apiKeyLabel: String = "N/A"
    override val apiKeyPlaceholder: String = "Open-Meteo is free and requires no API key"

    /**
     * Optional user-provided custom API configuration.
     * Set via [configureWithJson] before calling fetchWeather.
     */
    private var customConfig: OpenMeteoConfig? = null

    /**
     * Configure this provider with a user-imported JSON config.
     * The JSON should match the [OpenMeteoConfig] data class structure.
     */
    fun configureWithJson(jsonString: String) {
        customConfig = try {
            Gson().fromJson(jsonString, OpenMeteoConfig::class.java)
        } catch (e: Exception) {
            Log.w("OpenMeteo", "Failed to parse custom API config JSON", e)
            null
        }
    }

    /** Clear any user-imported config and revert to defaults. */
    fun clearCustomConfig() {
        customConfig = null
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Detect the response format from the config.
     * Priority: responseFormat override > extraParams format= > json default.
     */
    private fun detectFormat(config: OpenMeteoConfig?): String {
        // Check explicit responseFormat field first (set via settings UI)
        config?.responseFormat?.let {
            if (it in listOf("csv", "xlsx", "xls")) return it
        }
        // Fall back to extraParams
        val extra = config?.extraParams?.lowercase() ?: return "json"
        return when {
            "format=csv" in extra -> "csv"
            "format=xlsx" in extra || "format=xls" in extra -> "xlsx"
            else -> "json"
        }
    }

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        apiKey: String?
    ): WeatherSnapshot? = withContext(Dispatchers.IO) {
        try {
            // Use custom config if available, otherwise use defaults
            val config = customConfig
            val baseUrl = config?.baseUrl ?: "https://api.open-meteo.com/v1/forecast"
            val currentParams = config?.currentParams ?: "temperature_2m,relative_humidity_2m,weather_code,cloud_cover,surface_pressure,wind_speed_10m,wind_direction_10m"
            val dailyParams = config?.dailyParams ?: "sunrise,sunset,temperature_2m_max,temperature_2m_min,weather_code,precipitation_sum,wind_speed_10m_max,apparent_temperature_max,apparent_temperature_min,time"
            val timezone = config?.timezone ?: "auto"

            val url = "$baseUrl" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=$currentParams" +
                "&daily=$dailyParams" +
                "&timezone=$timezone" +
                (config?.extraParams?.let { "&$it" } ?: "") +
                // Append format= from responseFormat field (set via settings UI)
                (config?.responseFormat?.let { if (it != "json") "&format=$it" else "" } ?: "")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FieldMind/1.0 (field-research-app; open-meteo-provider)")
                .get()
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w("OpenMeteo", "HTTP ${response.code} for $latitude,$longitude — body: ${response.body?.string()?.take(200)}")
                return@withContext null
            }

            // Detect response format from config's extraParams
            val format = detectFormat(config)

            // Parse response body according to format (each branch consumes the body once)
            var jsonBody: String? = null
            val parsed: OpenMeteoFullResponse? = when (format) {
                "csv" -> {
                    val body = response.body?.string() ?: return@withContext null
                    parseCsvResponse(body)
                }
                "xlsx" -> {
                    val bytes = response.body?.bytes() ?: return@withContext null
                    parseXlsxResponse(bytes)
                }
                else -> {
                    response.body?.string()?.let {
                        jsonBody = it
                        gson.fromJson(it, OpenMeteoFullResponse::class.java)
                    }
                }
            }

            val curr = parsed?.current
            val daily = parsed?.daily

            // Legacy JSON fallback: only for JSON format, and only if parsing returned nothing
            if (curr == null && parsed == null && format == "json" && jsonBody != null) {
                Log.w("OpenMeteo", "No 'current' object in API response for $latitude,$longitude")
                val legacy = gson.fromJson(jsonBody, OpenMeteoLegacyResponse::class.java)
                val lw = legacy.currentWeather ?: return@withContext null
                return@withContext WeatherSnapshot(
                    temperature = lw.temperature,
                    weatherCode = lw.weatherCode ?: 0,
                    weatherDescription = WeatherSnapshot.descriptionForCode(lw.weatherCode ?: 0),
                    windSpeed = lw.windSpeed,
                    windDirection = lw.windDirection
                )
            }

            val temp = curr?.temperature
            val code = curr?.weatherCode ?: 0
            val humidity = curr?.humidity
            val windSpeed = curr?.windSpeed
            val windDir = curr?.windDirection
            val cloudCover = curr?.cloudCover
            val pressure = curr?.pressure
            val sunrise = daily?.sunrise?.firstOrNull { it.isNotBlank() }
            val sunset = daily?.sunset?.firstOrNull { it.isNotBlank() }

            // Parse daily forecasts
            val forecasts = if (daily?.time != null && daily.tempMax != null && daily.tempMin != null) {
                daily.time.zip(daily.tempMax.zip(daily.tempMin)).mapIndexed { index, (date, temps) ->
                    val (tMax, tMin) = temps
                    val wCode = daily.weatherCode?.getOrNull(index) ?: 0
                    val precip = daily.precipitationSum?.getOrNull(index)
                    val windMax = daily.windSpeedMax?.getOrNull(index)
                    val apparentMax = daily.apparentTempMax?.getOrNull(index)
                    val apparentMin = daily.apparentTempMin?.getOrNull(index)
                    val apparentAvg = if (apparentMax != null && apparentMin != null) (apparentMax + apparentMin) / 2.0 else null
                    DailyForecast(
                        date = date,
                        temperatureMax = tMax,
                        temperatureMin = tMin,
                        weatherCode = wCode,
                        weatherDescription = WeatherSnapshot.descriptionForCode(wCode),
                        precipitationSum = precip,
                        windSpeedMax = windMax,
                        humidityMax = null,
                        apparentTemperature = apparentAvg
                    )
                }
            } else emptyList()

            WeatherSnapshot(
                temperature = temp,
                weatherCode = code,
                weatherDescription = WeatherSnapshot.descriptionForCode(code),
                humidity = humidity,
                windSpeed = windSpeed,
                windDirection = windDir,
                cloudCover = cloudCover,
                pressure = pressure,
                sunrise = sunrise,
                sunset = sunset,
                dailyForecasts = forecasts
            )
        } catch (e: Exception) {
            Log.e("OpenMeteo", "fetchWeather failed for $latitude,$longitude", e)
            null
        }
    }

    // ── CSV Parser ──

    /** Parse a single CSV line, handling quoted fields and escaped quotes. */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    // Check for escaped quote ("")
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // skip the second quote
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    /**
     * Parse an Open-Meteo CSV response body into [OpenMeteoFullResponse].
     * The CSV format uses the same column names as the JSON response keys.
     *
     * Handles duplicate column headers (e.g. "weather_code" appears in both
     * current and daily parameter groups) by appending "_2", "_3" suffixes
     * so each column key is unique and accessible via fallback lookup.
     */
    private fun parseCsvResponse(body: String): OpenMeteoFullResponse? {
        val lines = body.trim().lines().filter { it.isNotBlank() }
        if (lines.size < 2) return null

        val rawHeaders = parseCsvLine(lines[0])
        // Make duplicate column headers unique by appending "_2", "_3" suffixes
        val seen = mutableSetOf<String>()
        val headers = rawHeaders.map { header ->
            var unique = header
            var counter = 1
            while (unique in seen) {
                counter++
                unique = "${header}_$counter"
            }
            seen.add(unique)
            unique
        }
        val rows = lines.drop(1).map { line ->
            val values = parseCsvLine(line)
            headers.zip(values).toMap()
        }
        if (rows.isEmpty()) return null

        return parseCsvRows(rows)
    }

    // ── XLSX Parser ──

    /**
     * Parse an Open-Meteo XLSX (binary) response into [OpenMeteoFullResponse].
     * XLSX files are ZIP archives containing XML. We extract and parse
     * the sheet data and shared strings using Android's built-in XmlPullParser.
     */
    private fun parseXlsxResponse(bytes: ByteArray): OpenMeteoFullResponse? {
        try {
            // Read shared strings and sheet XML from the ZIP
            var sharedStrings = listOf<String>()
            var sheetXml: String? = null

            val zis = ZipInputStream(bytes.inputStream())
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> {
                        sharedStrings = parseXlsxSharedStrings(zis.readBytes().toString(Charsets.UTF_8))
                    }
                    "xl/worksheets/sheet1.xml" -> {
                        sheetXml = zis.readBytes().toString(Charsets.UTF_8)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()

            val xml = sheetXml ?: return null

            // Parse the sheet XML to extract rows and cells
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            // Map column ref (A, B, C...) to index
            fun colIndex(ref: String): Int {
                val letters = ref.takeWhile { it.isLetter() }
                var idx = 0
                for (ch in letters) {
                    idx = idx * 26 + (ch.code - 'A'.code + 1)
                }
                return idx - 1
            }

            // Extract numeric value from cell ref (e.g., "A1" -> 1)
            fun rowNum(ref: String): Int {
                return ref.dropWhile { it.isLetter() }.toIntOrNull() ?: 0
            }

            // Parse rows
            val rows = mutableListOf<MutableMap<Int, String>>()
            var currentRow: MutableMap<Int, String>? = null
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableMapOf()
                        }
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            val type = parser.getAttributeValue(null, "t") ?: ""
                            val colIdx = colIndex(ref)

                            // Read the cell value
                            var value: String? = null
                            var depth = 1
                            while (depth > 0) {
                                eventType = parser.next()
                                when (eventType) {
                                    XmlPullParser.START_TAG -> {
                                        if (parser.name == "v") {
                                            value = parser.nextText()
                                        }
                                        depth++
                                    }
                                    XmlPullParser.END_TAG -> depth--
                                }
                            }

                            if (value != null) {
                                // If type is "s" (shared string), look up the string table
                                val resolved = if (type == "s") {
                                    sharedStrings.getOrElse(value.toIntOrNull() ?: -1) { value }
                                } else value
                                currentRow?.put(colIdx, resolved)
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && parser.name == "row") {
                    currentRow?.let { rows.add(it) }
                    currentRow = null
                }
                eventType = parser.next()
            }

            // Convert parsed rows to the CSV-style table
            if (rows.isEmpty()) return null

            // Build headers from the first row, handling duplicate names
            val headerMap = mutableMapOf<Int, String>()
            val seenHeaderNames = mutableSetOf<String>()
            // Expected column order from Open-Meteo
            val expectedCurrentVars = listOf(
                "temperature_2m", "relative_humidity_2m", "weather_code",
                "cloud_cover", "surface_pressure", "wind_speed_10m", "wind_direction_10m"
            )
            val expectedDailyVars = listOf(
                "time", "temperature_2m_max", "temperature_2m_min", "weather_code",
                "precipitation_sum", "wind_speed_10m_max",
                "apparent_temperature_max", "apparent_temperature_min",
                "sunrise", "sunset"
            )

            // Try to determine headers: check if first row cells look like column names
            val firstRowData = rows.first()
            // Check if first row contains known variable names
            val isHeaderRow = firstRowData.values.any { it in expectedCurrentVars || it in expectedDailyVars }

            val dataRows = if (isHeaderRow) {
                // First row is headers, subsequent rows are data
                // Rename duplicate headers with _2, _3 suffixes (same as CSV parser)
                for ((col, name) in firstRowData) {
                    var uniqueName = name
                    var counter = 1
                    while (uniqueName in seenHeaderNames) {
                        counter++
                        uniqueName = "${name}_$counter"
                    }
                    seenHeaderNames.add(uniqueName)
                    headerMap[col] = uniqueName
                }
                rows.drop(1)
            } else {
                // No header row - use default position-based mapping
                // Column order: time, temperature_2m, humidity, weather_code, cloud_cover, pressure, wind_speed_10m, wind_direction_10m, ...
                rows
            }

            // Build CSV-like rows as maps
            val csvRows = dataRows.map { row ->
                val map = mutableMapOf<String, String>()
                for ((colIdx, value) in row) {
                    val name = headerMap[colIdx] ?: "col_$colIdx"
                    map[name] = value
                }
                map
            }

            // Parse using the same logic as CSV
            return parseCsvRows(csvRows)
        } catch (e: Exception) {
            Log.e("OpenMeteo", "Failed to parse XLSX response", e)
            return null
        }
    }

    /** Parse shared strings XML from XLSX. */
    private fun parseXlsxSharedStrings(xml: String): List<String> {
        val strings = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                    strings.add(parser.nextText())
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w("OpenMeteo", "Failed to parse XLSX shared strings", e)
        }
        return strings
    }

    /** Shared CSV/XLSX row-to-response logic. */
    private fun parseCsvRows(rows: List<Map<String, String>>): OpenMeteoFullResponse {
        /**
         * Get a value from the row map, falling back to suffixed keys for duplicate
         * column headers (e.g. "weather_code_2" if "weather_code" is blank).
         */
        fun <T> getValue(row: Map<String, String>, key: String, parse: (String) -> T): T? {
            val raw = row[key]?.takeIf { it.isNotBlank() && it != "null" }
                ?: row["${key}_2"]?.takeIf { it.isNotBlank() && it != "null" }
                ?: row["${key}_3"]?.takeIf { it.isNotBlank() && it != "null" }
            return try { raw?.let(parse) } catch (_: Exception) { null }
        }
        fun getDouble(row: Map<String, String>, key: String) = getValue(row, key) { it.toDouble() }
        fun getInt(row: Map<String, String>, key: String) = getValue(row, key) { it.toInt() }
        fun getString(row: Map<String, String>, key: String) = 
            row[key]?.takeIf { it.isNotBlank() && it != "null" }
                ?: row["${key}_2"]?.takeIf { it.isNotBlank() && it != "null" }
                ?: row["${key}_3"]?.takeIf { it.isNotBlank() && it != "null" }

        // Extract daily data
        val dailyTimes = mutableListOf<String>()
        val dailyTempMax = mutableListOf<Double>()
        val dailyTempMin = mutableListOf<Double>()
        val dailyWeatherCode = mutableListOf<Int>()
        val dailyPrecip = mutableListOf<Double>()
        val dailyWindMax = mutableListOf<Double>()
        val dailyApparentMax = mutableListOf<Double>()
        val dailyApparentMin = mutableListOf<Double>()
        val dailySunrise = mutableListOf<String>()
        val dailySunset = mutableListOf<String>()

        var currentData: OpenMeteoCurrent? = null

        for (row in rows) {
            val time = getString(row, "time") ?: continue
            val isDaily = time.length <= 10 && "-" in time

            if (isDaily) {
                dailyTimes.add(time)
                dailyTempMax.add(getDouble(row, "temperature_2m_max") ?: 0.0)
                dailyTempMin.add(getDouble(row, "temperature_2m_min") ?: 0.0)
                dailyWeatherCode.add(getInt(row, "weather_code") ?: 0)
                dailyPrecip.add(getDouble(row, "precipitation_sum") ?: 0.0)
                dailyWindMax.add(getDouble(row, "wind_speed_10m_max") ?: 0.0)
                dailyApparentMax.add(getDouble(row, "apparent_temperature_max") ?: 0.0)
                dailyApparentMin.add(getDouble(row, "apparent_temperature_min") ?: 0.0)
                dailySunrise.add(getString(row, "sunrise") ?: "")
                dailySunset.add(getString(row, "sunset") ?: "")
            } else if (currentData == null) {
                // First non-daily row = current conditions
                currentData = OpenMeteoCurrent(
                    temperature = getDouble(row, "temperature_2m"),
                    humidity = getInt(row, "relative_humidity_2m"),
                    weatherCode = getInt(row, "weather_code"),
                    cloudCover = getInt(row, "cloud_cover"),
                    pressure = getDouble(row, "surface_pressure"),
                    windSpeed = getDouble(row, "wind_speed_10m"),
                    windDirection = getInt(row, "wind_direction_10m")
                )
            }
        }

        // If we didn't find current data from a non-daily row, use the first row
        if (currentData == null && rows.isNotEmpty()) {
            val first = rows.first()
            currentData = OpenMeteoCurrent(
                temperature = getDouble(first, "temperature_2m"),
                humidity = getInt(first, "relative_humidity_2m"),
                weatherCode = getInt(first, "weather_code"),
                cloudCover = getInt(first, "cloud_cover"),
                pressure = getDouble(first, "surface_pressure"),
                windSpeed = getDouble(first, "wind_speed_10m"),
                windDirection = getInt(first, "wind_direction_10m")
            )
        }

        val daily = if (dailyTimes.isNotEmpty()) {
            OpenMeteoDaily(
                time = dailyTimes,
                tempMax = dailyTempMax,
                tempMin = dailyTempMin,
                weatherCode = dailyWeatherCode,
                precipitationSum = dailyPrecip,
                windSpeedMax = dailyWindMax,
                apparentTempMax = dailyApparentMax,
                apparentTempMin = dailyApparentMin,
                sunrise = dailySunrise,
                sunset = dailySunset
            )
        } else null

        return OpenMeteoFullResponse(current = currentData, daily = daily)
    }
}

/**
 * User-imported custom API configuration for Open-Meteo.
 * Users can import a JSON file matching this structure to customize
 * the API endpoint, parameters, and timezone.
 */
data class OpenMeteoConfig(
    val baseUrl: String? = null,
    val currentParams: String? = null,
    val dailyParams: String? = null,
    val timezone: String? = null,
    val extraParams: String? = null,
    /**
     * Response format override: "json", "csv", or "xlsx".
     * When set, this takes precedence over any "format=" in [extraParams].
     * Set via the weather settings UI format selector.
     */
    val responseFormat: String? = null
)

// ── Response models ──

/** New API format: current + daily at top level. */
internal data class OpenMeteoFullResponse(
    @SerializedName("current") val current: OpenMeteoCurrent? = null,
    @SerializedName("daily") val daily: OpenMeteoDaily? = null
)

/** Legacy format: current_weather flat object. */
internal data class OpenMeteoLegacyResponse(
    @SerializedName("current_weather") val currentWeather: OpenMeteoLegacyCurrent? = null
)

internal data class OpenMeteoCurrent(
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("cloud_cover") val cloudCover: Int? = null,
    @SerializedName("surface_pressure") val pressure: Double? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("wind_direction_10m") val windDirection: Int? = null
)

internal data class OpenMeteoLegacyCurrent(
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("weathercode") val weatherCode: Int? = null,
    @SerializedName("windspeed") val windSpeed: Double? = null,
    @SerializedName("winddirection") val windDirection: Int? = null
)

internal data class OpenMeteoDaily(
    @SerializedName("time") val time: List<String>? = null,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>? = null,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int>? = null,
    @SerializedName("precipitation_sum") val precipitationSum: List<Double>? = null,
    @SerializedName("wind_speed_10m_max") val windSpeedMax: List<Double>? = null,
    @SerializedName("apparent_temperature_max") val apparentTempMax: List<Double>? = null,
    @SerializedName("apparent_temperature_min") val apparentTempMin: List<Double>? = null,
    @SerializedName("sunrise") val sunrise: List<String>? = null,
    @SerializedName("sunset") val sunset: List<String>? = null
)
