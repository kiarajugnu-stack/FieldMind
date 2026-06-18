package fieldmind.research.app.shared.data.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import fieldmind.research.app.BuildConfig

/**
 * Data class to represent a single crash log entry
 */
data class CrashLogEntry(
    val timestamp: Long,
    val log: String
)

/**
 * Application settings singleton.
 *
 * Holds only FieldMind-essential preferences plus shared app-shell settings
 * (theme, festive overlay, and legacy NetworkClient API toggles).
 * All Rhythm music-player settings have been removed.
 */
class AppSettings private constructor(context: Context) {
    companion object {
        private const val PREFS_NAME = "rhythm_preferences"

        // ── Onboarding ──
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        // ── Beta Program ──
        private const val KEY_HAS_SHOWN_BETA_POPUP = "has_shown_beta_popup"

        // ── Crash Reporting ──
        private const val KEY_LAST_CRASH_LOG = "last_crash_log"
        private const val KEY_CRASH_LOG_HISTORY = "crash_log_history"

        // ── Theme ──
        private const val KEY_USE_SYSTEM_THEME = "use_system_theme"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AMOLED_THEME = "amoled_theme"
        private const val KEY_USE_DYNAMIC_COLORS = "use_dynamic_colors"
        private const val KEY_CUSTOM_COLOR_SCHEME = "custom_color_scheme"
        private const val KEY_CUSTOM_FONT = "custom_font"
        private const val KEY_FONT_SOURCE = "font_source"
        private const val KEY_CUSTOM_FONT_PATH = "custom_font_path"
        private const val KEY_CUSTOM_FONT_FAMILY = "custom_font_family"
        private const val KEY_COLOR_SOURCE = "color_source"
        private const val KEY_EXTRACTED_ALBUM_COLORS = "extracted_album_colors"

        // ── Festive Overlay (used by FestiveOverlay) ──
        private const val KEY_FESTIVE_THEME_ENABLED = "festive_theme_enabled"
        private const val KEY_FESTIVE_THEME_TYPE = "festive_theme_type"
        private const val KEY_FESTIVE_THEME_INTENSITY = "festive_theme_intensity"

        // ── Network API toggles (used by legacy NetworkClient) ──
        private const val KEY_DEEZER_API_ENABLED = "deezer_api_enabled"
        private const val KEY_LRCLIB_API_ENABLED = "lrclib_api_enabled"
        private const val KEY_YTMUSIC_API_ENABLED = "ytmusic_api_enabled"
        private const val KEY_SPOTIFY_API_ENABLED = "spotify_api_enabled"
        private const val KEY_APPLEMUSIC_API_ENABLED = "applemusic_api_enabled"
        private const val KEY_SPOTIFY_CLIENT_ID = "spotify_client_id"
        private const val KEY_SPOTIFY_CLIENT_SECRET = "spotify_client_secret"

        @Volatile
        private var INSTANCE: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettings(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════
    //  Onboarding
    // ═══════════════════════════════════════

    private val _onboardingCompleted =
        MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    // ═══════════════════════════════════════
    //  Beta Popup
    // ═══════════════════════════════════════

    private val _hasShownBetaPopup =
        MutableStateFlow(prefs.getBoolean(KEY_HAS_SHOWN_BETA_POPUP, false))
    val hasShownBetaPopup: StateFlow<Boolean> = _hasShownBetaPopup.asStateFlow()

    fun setHasShownBetaPopup(shown: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SHOWN_BETA_POPUP, shown).apply()
        _hasShownBetaPopup.value = shown
    }

    // ═══════════════════════════════════════
    //  Crash Reporting
    // ═══════════════════════════════════════

    private val _lastCrashLog =
        MutableStateFlow<String?>(prefs.getString(KEY_LAST_CRASH_LOG, null))
    val lastCrashLog: StateFlow<String?> = _lastCrashLog.asStateFlow()

    private val _crashLogHistory = MutableStateFlow<List<CrashLogEntry>>(
        try {
            val json = prefs.getString(KEY_CRASH_LOG_HISTORY, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<List<CrashLogEntry>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )
    val crashLogHistory: StateFlow<List<CrashLogEntry>> = _crashLogHistory.asStateFlow()

    fun setLastCrashLog(log: String) {
        _lastCrashLog.value = log
        prefs.edit().putString(KEY_LAST_CRASH_LOG, log).apply()
    }

    fun addCrashLogEntry(log: String) {
        val currentHistory = _crashLogHistory.value.toMutableList()
        currentHistory.add(CrashLogEntry(timestamp = System.currentTimeMillis(), log = log))
        val limitedHistory = currentHistory.takeLast(50)
        _crashLogHistory.value = limitedHistory
        prefs.edit().putString(KEY_CRASH_LOG_HISTORY, Gson().toJson(limitedHistory)).apply()
        setLastCrashLog(log)
    }

    // ═══════════════════════════════════════
    //  Theme
    // ═══════════════════════════════════════

    private val _useSystemTheme =
        MutableStateFlow(prefs.getBoolean(KEY_USE_SYSTEM_THEME, true))
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()

    private val _darkMode =
        MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _amoledTheme =
        MutableStateFlow(prefs.getBoolean(KEY_AMOLED_THEME, false))
    val amoledTheme: StateFlow<Boolean> = _amoledTheme.asStateFlow()

    private val _useDynamicColors =
        MutableStateFlow(prefs.getBoolean(KEY_USE_DYNAMIC_COLORS, true))
    val useDynamicColors: StateFlow<Boolean> = _useDynamicColors.asStateFlow()

    private val _customColorScheme =
        MutableStateFlow(prefs.getString(KEY_CUSTOM_COLOR_SCHEME, "Default") ?: "Default")
    val customColorScheme: StateFlow<String> = _customColorScheme.asStateFlow()

    private val _customFont =
        MutableStateFlow(prefs.getString(KEY_CUSTOM_FONT, "Geom") ?: "Geom")
    val customFont: StateFlow<String> = _customFont.asStateFlow()

    private val _fontSource =
        MutableStateFlow(prefs.getString(KEY_FONT_SOURCE, "SYSTEM") ?: "SYSTEM")
    val fontSource: StateFlow<String> = _fontSource.asStateFlow()

    private val _customFontPath =
        MutableStateFlow<String?>(prefs.getString(KEY_CUSTOM_FONT_PATH, null))
    val customFontPath: StateFlow<String?> = _customFontPath.asStateFlow()

    private val _customFontFamily =
        MutableStateFlow(prefs.getString(KEY_CUSTOM_FONT_FAMILY, "System") ?: "System")
    val customFontFamily: StateFlow<String> = _customFontFamily.asStateFlow()

    private val _colorSource =
        MutableStateFlow(prefs.getString(KEY_COLOR_SOURCE, "MONET") ?: "MONET")
    val colorSource: StateFlow<String> = _colorSource.asStateFlow()

    private val _extractedAlbumColors =
        MutableStateFlow<String?>(prefs.getString(KEY_EXTRACTED_ALBUM_COLORS, null))
    val extractedAlbumColors: StateFlow<String?> = _extractedAlbumColors.asStateFlow()

    fun setUseSystemTheme(use: Boolean) {
        prefs.edit().putBoolean(KEY_USE_SYSTEM_THEME, use).apply()
        _useSystemTheme.value = use
    }

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
        _darkMode.value = dark
    }

    fun setAmoledTheme(amoled: Boolean) {
        prefs.edit().putBoolean(KEY_AMOLED_THEME, amoled).apply()
        _amoledTheme.value = amoled
    }

    fun setUseDynamicColors(use: Boolean) {
        prefs.edit().putBoolean(KEY_USE_DYNAMIC_COLORS, use).apply()
        _useDynamicColors.value = use
    }

    fun setCustomColorScheme(scheme: String) {
        prefs.edit().putString(KEY_CUSTOM_COLOR_SCHEME, scheme).apply()
        _customColorScheme.value = scheme
    }

    fun setCustomFont(font: String) {
        prefs.edit().putString(KEY_CUSTOM_FONT, font).apply()
        _customFont.value = font
    }

    fun setColorSource(source: String) {
        prefs.edit().putString(KEY_COLOR_SOURCE, source).apply()
        _colorSource.value = source
    }

    fun setExtractedAlbumColors(colorsJson: String?) {
        prefs.edit().putString(KEY_EXTRACTED_ALBUM_COLORS, colorsJson).apply()
        _extractedAlbumColors.value = colorsJson
    }

    fun setFontSource(source: String) {
        prefs.edit().putString(KEY_FONT_SOURCE, source).apply()
        _fontSource.value = source
    }

    fun setCustomFontPath(path: String?) {
        prefs.edit().putString(KEY_CUSTOM_FONT_PATH, path).apply()
        _customFontPath.value = path
    }

    fun setCustomFontFamily(family: String) {
        prefs.edit().putString(KEY_CUSTOM_FONT_FAMILY, family).apply()
        _customFontFamily.value = family
    }

    // ═══════════════════════════════════════
    //  Festive Overlay
    // ═══════════════════════════════════════

    private val _festiveThemeEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_FESTIVE_THEME_ENABLED, true))
    val festiveThemeEnabled: StateFlow<Boolean> = _festiveThemeEnabled.asStateFlow()

    private val _festiveThemeType =
        MutableStateFlow(prefs.getString(KEY_FESTIVE_THEME_TYPE, "CHRISTMAS") ?: "CHRISTMAS")
    val festiveThemeType: StateFlow<String> = _festiveThemeType.asStateFlow()

    private val _festiveThemeIntensity =
        MutableStateFlow(prefs.getFloat(KEY_FESTIVE_THEME_INTENSITY, 0.5f))
    val festiveThemeIntensity: StateFlow<Float> = _festiveThemeIntensity.asStateFlow()

    fun setFestiveThemeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FESTIVE_THEME_ENABLED, enabled).apply()
        _festiveThemeEnabled.value = enabled
    }

    fun setFestiveThemeType(type: String) {
        prefs.edit().putString(KEY_FESTIVE_THEME_TYPE, type).apply()
        _festiveThemeType.value = type
    }

    fun setFestiveThemeIntensity(intensity: Float) {
        prefs.edit().putFloat(KEY_FESTIVE_THEME_INTENSITY, intensity).apply()
        _festiveThemeIntensity.value = intensity
    }

    // ═══════════════════════════════════════
    //  Network API toggles (legacy NetworkClient)
    // ═══════════════════════════════════════

    private val _deezerApiEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DEEZER_API_ENABLED, BuildConfig.FLAVOR != "fdroid")
    )
    val deezerApiEnabled: StateFlow<Boolean> = _deezerApiEnabled.asStateFlow()

    private val _lrclibApiEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_LRCLIB_API_ENABLED, BuildConfig.FLAVOR != "fdroid")
    )
    val lrclibApiEnabled: StateFlow<Boolean> = _lrclibApiEnabled.asStateFlow()

    private val _ytMusicApiEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_YTMUSIC_API_ENABLED, BuildConfig.FLAVOR != "fdroid")
    )
    val ytMusicApiEnabled: StateFlow<Boolean> = _ytMusicApiEnabled.asStateFlow()

    private val _spotifyApiEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SPOTIFY_API_ENABLED, BuildConfig.FLAVOR != "fdroid")
    )
    val spotifyApiEnabled: StateFlow<Boolean> = _spotifyApiEnabled.asStateFlow()

    private val _appleMusicApiEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_APPLEMUSIC_API_ENABLED, BuildConfig.FLAVOR != "fdroid")
    )
    val appleMusicApiEnabled: StateFlow<Boolean> = _appleMusicApiEnabled.asStateFlow()

    private val _spotifyClientId =
        MutableStateFlow(prefs.getString(KEY_SPOTIFY_CLIENT_ID, "") ?: "")
    val spotifyClientId: StateFlow<String> = _spotifyClientId.asStateFlow()

    private val _spotifyClientSecret =
        MutableStateFlow(prefs.getString(KEY_SPOTIFY_CLIENT_SECRET, "") ?: "")
    val spotifyClientSecret: StateFlow<String> = _spotifyClientSecret.asStateFlow()
}
