package fieldmind.research.app.features.field.data.settings

import android.content.Context
import com.google.gson.Gson
import fieldmind.research.app.features.field.data.background.FieldMindBackgroundScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ── Onboarding data models ──

enum class ZoologySubfield(val displayName: String) {
    Birds("Birds"), Mammals("Mammals"), Herps("Herps"), Insects("Insects"), Marine("Marine");
    companion object { fun all() = values().toSet() }
}

enum class BotanySubfield(val displayName: String) {
    Wildflowers("Wildflowers"), Trees("Trees"), Fungi("Fungi"), Mosses("Mosses");
    companion object { fun all() = values().toSet() }
}

/** Which scientific domains the user is interested in. Auto-configures UI. */
data class UserInterests(
    val zoology: Set<ZoologySubfield> = emptySet(),
    val botany: Set<BotanySubfield> = emptySet(),
    val ecologyEnvironment: Boolean = false,
    val astronomy: Boolean = false,
    val geology: Boolean = false,
    val customInterests: List<String> = emptyList()
) {
    val hasWildlife: Boolean get() = zoology.isNotEmpty() || botany.isNotEmpty() || ecologyEnvironment
    val hasAny: Boolean get() = hasWildlife || astronomy || geology || customInterests.isNotEmpty()

    companion object {
        private val gson = Gson()
        fun toJson(interests: UserInterests): String = gson.toJson(interests)
        fun fromJson(json: String?): UserInterests {
            if (json.isNullOrBlank()) return UserInterests()
            return try { gson.fromJson(json, UserInterests::class.java) } catch (_: Exception) { UserInterests() }
        }
    }
}

/** Which screens are visible in the navigation bar and app. Hidden screens accessible via Settings. */
data class ScreenVisibility(
    val showCapture: Boolean = true,
    val showProjects: Boolean = true,
    val showInsights: Boolean = true,
    val showLibrary: Boolean = true,
    val showMap: Boolean = true,
    val showExport: Boolean = false,
    val showWeather: Boolean = true,
    val showSpeciesBrowser: Boolean = true,
    val showFlashcards: Boolean = false,
    val showFieldMode: Boolean = false
) {
    companion object {
        private val gson = Gson()
        fun toJson(vis: ScreenVisibility): String = gson.toJson(vis)
        fun fromJson(json: String?): ScreenVisibility {
            if (json.isNullOrBlank()) return ScreenVisibility()
            return try { gson.fromJson(json, ScreenVisibility::class.java) } catch (_: Exception) { ScreenVisibility() }
        }

        /** Derive a default visibility from a user's interests. */
        fun fromInterests(interests: UserInterests): ScreenVisibility {
            val hasZoology = interests.zoology.isNotEmpty()
            val hasBotany = interests.botany.isNotEmpty()
            val hasWildlife = hasZoology || hasBotany || interests.ecologyEnvironment
            return ScreenVisibility(
                showCapture = true,
                showProjects = true,
                showInsights = true,
                showLibrary = true,
                showMap = hasWildlife || interests.geology,
                showExport = false,
                showWeather = interests.hasAny,
                showSpeciesBrowser = hasZoology || hasBotany,
                showFlashcards = hasWildlife,
                showFieldMode = hasWildlife
            )
        }
    }
}

class FieldMindSettings private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("fieldmind_settings", Context.MODE_PRIVATE)

    private val _dailyObservationGoal = MutableStateFlow(prefs.getInt(KEY_DAILY_GOAL, 1))
    val dailyObservationGoal: StateFlow<Int> = _dailyObservationGoal.asStateFlow()

    private val _defaultCategory = MutableStateFlow(prefs.getString(KEY_DEFAULT_CATEGORY, "Bird") ?: "Bird")
    val defaultCategory: StateFlow<String> = _defaultCategory.asStateFlow()

    private val _defaultConfidence = MutableStateFlow(prefs.getString(KEY_DEFAULT_CONFIDENCE, "Sure") ?: "Sure")
    val defaultConfidence: StateFlow<String> = _defaultConfidence.asStateFlow()

    private val _locationMode = MutableStateFlow(prefs.getString(KEY_LOCATION_MODE, "Manual only") ?: "Manual only")
    val locationMode: StateFlow<String> = _locationMode.asStateFlow()

    private val _mediaAttachmentsEnabled = MutableStateFlow(prefs.getBoolean(KEY_MEDIA_ATTACHMENTS, true))
    val mediaAttachmentsEnabled: StateFlow<Boolean> = _mediaAttachmentsEnabled.asStateFlow()

    private val _audioRecordingEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUDIO_RECORDING, true))
    val audioRecordingEnabled: StateFlow<Boolean> = _audioRecordingEnabled.asStateFlow()

    private val _attachmentExportMode = MutableStateFlow(prefs.getString(KEY_ATTACHMENT_EXPORT_MODE, "Reference URIs") ?: "Reference URIs")
    val attachmentExportMode: StateFlow<String> = _attachmentExportMode.asStateFlow()

    private val _geminiEnabled = MutableStateFlow(prefs.getBoolean(KEY_GEMINI_ENABLED, false))
    val geminiEnabled: StateFlow<Boolean> = _geminiEnabled.asStateFlow()

    private val _aiProvider = MutableStateFlow(prefs.getString(KEY_AI_PROVIDER, "Gemini") ?: "Gemini")
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString(KEY_GEMINI_API_KEY, "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _geminiModel = MutableStateFlow(prefs.getString(KEY_GEMINI_MODEL, "gemini-1.5-flash") ?: "gemini-1.5-flash")
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

    private val _openAiApiKey = MutableStateFlow(prefs.getString(KEY_OPENAI_API_KEY, "") ?: "")
    val openAiApiKey: StateFlow<String> = _openAiApiKey.asStateFlow()

    private val _openAiModel = MutableStateFlow(prefs.getString(KEY_OPENAI_MODEL, "gpt-4.1-mini") ?: "gpt-4.1-mini")
    val openAiModel: StateFlow<String> = _openAiModel.asStateFlow()

    private val _aiRequireConfirmBeforeSave = MutableStateFlow(prefs.getBoolean(KEY_AI_CONFIRM, true))
    val aiRequireConfirmBeforeSave: StateFlow<Boolean> = _aiRequireConfirmBeforeSave.asStateFlow()

    private val _aiSendAttachments = MutableStateFlow(prefs.getBoolean(KEY_AI_SEND_ATTACHMENTS, false))
    val aiSendAttachments: StateFlow<Boolean> = _aiSendAttachments.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDERS, false))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _streaksEnabled = MutableStateFlow(prefs.getBoolean(KEY_STREAKS, true))
    val streaksEnabled: StateFlow<Boolean> = _streaksEnabled.asStateFlow()

    private val _defaultExportFormat = MutableStateFlow(prefs.getString(KEY_EXPORT_FORMAT, "Markdown") ?: "Markdown")
    val defaultExportFormat: StateFlow<String> = _defaultExportFormat.asStateFlow()

    private val _privacyLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_PRIVACY_LOCK, false))
    val privacyLockEnabled: StateFlow<Boolean> = _privacyLockEnabled.asStateFlow()

    private val _privacyTypingEnabled = MutableStateFlow(prefs.getBoolean(KEY_PRIVACY_TYPING, false))
    val privacyTypingEnabled: StateFlow<Boolean> = _privacyTypingEnabled.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    /** When true, use Material You wallpaper colors instead of the FieldMind brand palette. */
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "Dark") ?: "Dark")
    /** System, Light, or Dark. MainActivity observes this so Settings has an immediate theme toggle. */
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _profileName = MutableStateFlow(prefs.getString(KEY_PROFILE_NAME, "") ?: "")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileRole = MutableStateFlow(prefs.getString(KEY_PROFILE_ROLE, "Field learner") ?: "Field learner")
    val profileRole: StateFlow<String> = _profileRole.asStateFlow()

    private val _profileFocus = MutableStateFlow(prefs.getString(KEY_PROFILE_FOCUS, "Wildlife & ecology") ?: "Wildlife & ecology")
    val profileFocus: StateFlow<String> = _profileFocus.asStateFlow()

    private val _localModelEnabled = MutableStateFlow(prefs.getBoolean(KEY_LOCAL_MODEL_ENABLED, false))
    val localModelEnabled: StateFlow<Boolean> = _localModelEnabled.asStateFlow()

    private val _localModelOption = MutableStateFlow(prefs.getString(KEY_LOCAL_MODEL_OPTION, "FieldLite 500 MB") ?: "FieldLite 500 MB")
    val localModelOption: StateFlow<String> = _localModelOption.asStateFlow()

    private val _localModelDownloaded = MutableStateFlow(prefs.getBoolean(KEY_LOCAL_MODEL_DOWNLOADED, false))
    val localModelDownloaded: StateFlow<Boolean> = _localModelDownloaded.asStateFlow()

    private val _localModelUseForStudy = MutableStateFlow(prefs.getBoolean(KEY_LOCAL_MODEL_USE_STUDY, true))
    val localModelUseForStudy: StateFlow<Boolean> = _localModelUseForStudy.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false))
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _autoBackupInterval = MutableStateFlow(prefs.getString(KEY_AUTO_BACKUP_INTERVAL, "Weekly") ?: "Weekly")
    val autoBackupInterval: StateFlow<String> = _autoBackupInterval.asStateFlow()

    private val _backupFolderUri = MutableStateFlow(prefs.getString(KEY_BACKUP_FOLDER_URI, "") ?: "")
    val backupFolderUri: StateFlow<String> = _backupFolderUri.asStateFlow()

    // ── Weather & GPS settings ──
    private val _autoWeatherEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_WEATHER, false))
    val autoWeatherEnabled: StateFlow<Boolean> = _autoWeatherEnabled.asStateFlow()

    private val _autoFlashcardsEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_FLASHCARDS, false))
    val autoFlashcardsEnabled: StateFlow<Boolean> = _autoFlashcardsEnabled.asStateFlow()

    private val _autoPatternDetectionEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PATTERNS, true))
    val autoPatternDetectionEnabled: StateFlow<Boolean> = _autoPatternDetectionEnabled.asStateFlow()

    private val _autoQuestionsEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_QUESTIONS, false))
    val autoQuestionsEnabled: StateFlow<Boolean> = _autoQuestionsEnabled.asStateFlow()

    private val _tempUnit = MutableStateFlow(prefs.getString(KEY_TEMP_UNIT, "Celsius") ?: "Celsius")
    val tempUnit: StateFlow<String> = _tempUnit.asStateFlow()

    private val _weatherRefreshInterval = MutableStateFlow(prefs.getString(KEY_WEATHER_REFRESH, "30 min") ?: "30 min")
    val weatherRefreshInterval: StateFlow<String> = _weatherRefreshInterval.asStateFlow()

    // ── Weather display preferences ──
    private val _weatherShowTemperature = MutableStateFlow(prefs.getBoolean(KEY_WEATHER_SHOW_TEMP, true))
    val weatherShowTemperature: StateFlow<Boolean> = _weatherShowTemperature.asStateFlow()
    private val _weatherShowCondition = MutableStateFlow(prefs.getBoolean(KEY_WEATHER_SHOW_CONDITION, true))
    val weatherShowCondition: StateFlow<Boolean> = _weatherShowCondition.asStateFlow()
    private val _weatherShowHumidity = MutableStateFlow(prefs.getBoolean(KEY_WEATHER_SHOW_HUMIDITY, true))
    val weatherShowHumidity: StateFlow<Boolean> = _weatherShowHumidity.asStateFlow()
    private val _weatherShowWind = MutableStateFlow(prefs.getBoolean(KEY_WEATHER_SHOW_WIND, true))
    val weatherShowWind: StateFlow<Boolean> = _weatherShowWind.asStateFlow()
    private val _weatherShowCloudCover = MutableStateFlow(prefs.getBoolean(KEY_WEATHER_SHOW_CLOUD, true))
    val weatherShowCloudCover: StateFlow<Boolean> = _weatherShowCloudCover.asStateFlow()
    private val _weatherShowPressure = MutableStateFlow(prefs.getBoolean(KEY_WEATHER_SHOW_PRESSURE, false))
    val weatherShowPressure: StateFlow<Boolean> = _weatherShowPressure.asStateFlow()
    private val _weatherShowCloudAnimation = MutableStateFlow(prefs.getBoolean(KEY_WEATHER_SHOW_CLOUD_ANIMATION, true))
    val weatherShowCloudAnimation: StateFlow<Boolean> = _weatherShowCloudAnimation.asStateFlow()

    // ── Weather provider selection ──
    private val _weatherProvider = MutableStateFlow(prefs.getString(KEY_WEATHER_PROVIDER, "met-norway") ?: "met-norway")
    val weatherProvider: StateFlow<String> = _weatherProvider.asStateFlow()

    private val _weatherProviders = MutableStateFlow(prefs.getString(KEY_WEATHER_PROVIDERS, _weatherProvider.value) ?: _weatherProvider.value)
    val weatherProviders: StateFlow<String> = _weatherProviders.asStateFlow()

    private val _weatherApiKey = MutableStateFlow(prefs.getString(KEY_WEATHER_API_KEY, "") ?: "")
    val weatherApiKey: StateFlow<String> = _weatherApiKey.asStateFlow()

    // Per-provider API keys (each provider that requires a key gets its own field)
    private val _openWeatherMapApiKey = MutableStateFlow(prefs.getString(KEY_OPENWEATHERMAP_API_KEY, "") ?: "")
    val openWeatherMapApiKey: StateFlow<String> = _openWeatherMapApiKey.asStateFlow()

    private val _weatherApiDotComApiKey = MutableStateFlow(prefs.getString(KEY_WEATHERAPI_API_KEY, "") ?: "")
    val weatherApiDotComApiKey: StateFlow<String> = _weatherApiDotComApiKey.asStateFlow()

    private val _imdApiKey = MutableStateFlow(prefs.getString(KEY_IMD_API_KEY, "") ?: "")
    val imdApiKey: StateFlow<String> = _imdApiKey.asStateFlow()

    // Open-Meteo custom API configuration JSON (imported by user)
    private val _openMeteoApiConfig = MutableStateFlow(prefs.getString(KEY_OPENMETEO_CONFIG, "") ?: "")
    val openMeteoApiConfig: StateFlow<String> = _openMeteoApiConfig.asStateFlow()

    private val _gpsMode = MutableStateFlow(prefs.getString(KEY_GPS_MODE, "On capture only") ?: "On capture only")
    val gpsMode: StateFlow<String> = _gpsMode.asStateFlow()

    // ── Units & format settings ──
    private val _distanceUnit = MutableStateFlow(prefs.getString(KEY_DISTANCE_UNIT, "km") ?: "km")
    val distanceUnit: StateFlow<String> = _distanceUnit.asStateFlow()
    private val _windSpeedUnit = MutableStateFlow(prefs.getString(KEY_WIND_SPEED_UNIT, "km/h") ?: "km/h")
    val windSpeedUnit: StateFlow<String> = _windSpeedUnit.asStateFlow()
    private val _timeFormat = MutableStateFlow(prefs.getString(KEY_TIME_FORMAT, "24h") ?: "24h")
    val timeFormat: StateFlow<String> = _timeFormat.asStateFlow()
    private val _dateFormat = MutableStateFlow(prefs.getString(KEY_DATE_FORMAT, "ISO") ?: "ISO")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    // ── Map settings ──
    private val _mapType = MutableStateFlow(prefs.getString(KEY_MAP_TYPE, "Standard") ?: "Standard")
    val mapType: StateFlow<String> = _mapType.asStateFlow()
    private val _mapShowLocation = MutableStateFlow(prefs.getBoolean(KEY_MAP_SHOW_LOCATION, true))
    val mapShowLocation: StateFlow<Boolean> = _mapShowLocation.asStateFlow()

    // ── Field-mode defaults ──
    private val _fieldModeDefaultSession = MutableStateFlow(prefs.getString(KEY_FIELD_MODE_DEFAULT_SESSION, "Quick capture") ?: "Quick capture")
    val fieldModeDefaultSession: StateFlow<String> = _fieldModeDefaultSession.asStateFlow()
    private val _fieldModeAutoStartTimer = MutableStateFlow(prefs.getBoolean(KEY_FIELD_MODE_AUTO_START_TIMER, false))
    val fieldModeAutoStartTimer: StateFlow<Boolean> = _fieldModeAutoStartTimer.asStateFlow()
    private val _fieldModeObservationSpacing = MutableStateFlow(prefs.getString(KEY_FIELD_MODE_OBSERVATION_SPACING, "None") ?: "None")
    val fieldModeObservationSpacing: StateFlow<String> = _fieldModeObservationSpacing.asStateFlow()

    // ── Developer settings ──
    private val _developerMode = MutableStateFlow(prefs.getBoolean(KEY_DEVELOPER_MODE, false))
    val developerMode: StateFlow<Boolean> = _developerMode.asStateFlow()
    private val _debugLogging = MutableStateFlow(prefs.getBoolean(KEY_DEBUG_LOGGING, false))
    val debugLogging: StateFlow<Boolean> = _debugLogging.asStateFlow()
    private val _dataIntegrityCheckOnLaunch = MutableStateFlow(prefs.getBoolean(KEY_DATA_INTEGRITY_CHECK, false))
    val dataIntegrityCheckOnLaunch: StateFlow<Boolean> = _dataIntegrityCheckOnLaunch.asStateFlow()

    // ── Species identification settings ──
    private val _speciesIdApiKey = MutableStateFlow(prefs.getString(KEY_SPECIES_ID_API_KEY, "") ?: "")
    val speciesIdApiKey: StateFlow<String> = _speciesIdApiKey.asStateFlow()
    private val _speciesIdOfflineFirst = MutableStateFlow(prefs.getBoolean(KEY_SPECIES_ID_OFFLINE_FIRST, true))
    val speciesIdOfflineFirst: StateFlow<Boolean> = _speciesIdOfflineFirst.asStateFlow()
    private val _speciesModelBaseUrl = MutableStateFlow(prefs.getString(KEY_SPECIES_MODEL_BASE_URL, "") ?: "")
    val speciesModelBaseUrl: StateFlow<String> = _speciesModelBaseUrl.asStateFlow()
    // ── Perenual API settings ──
    private val _perenualApiKey = MutableStateFlow(prefs.getString(KEY_PERENUAL_API_KEY, "") ?: "")
    val perenualApiKey: StateFlow<String> = _perenualApiKey.asStateFlow()


    // ── Security settings ──
    private val _lockTimeout = MutableStateFlow(prefs.getString(KEY_LOCK_TIMEOUT, "Immediate") ?: "Immediate")
    val lockTimeout: StateFlow<String> = _lockTimeout.asStateFlow()

    private val _autoLockOnBackground = MutableStateFlow(prefs.getBoolean(KEY_AUTO_LOCK_BACKGROUND, true))
    val autoLockOnBackground: StateFlow<Boolean> = _autoLockOnBackground.asStateFlow()

    // ── Privacy & screen protection settings ──
    private val _screenCaptureProtectionEnabled = MutableStateFlow(prefs.getBoolean(KEY_SCREEN_CAPTURE_PROTECTION, false))
    val screenCaptureProtectionEnabled: StateFlow<Boolean> = _screenCaptureProtectionEnabled.asStateFlow()

    private val _alwaysOnScreenEnabled = MutableStateFlow(prefs.getBoolean(KEY_ALWAYS_ON_SCREEN, false))
    val alwaysOnScreenEnabled: StateFlow<Boolean> = _alwaysOnScreenEnabled.asStateFlow()

    private val _alwaysOnScreenDuration = MutableStateFlow(prefs.getString(KEY_ALWAYS_ON_SCREEN_DURATION, "15 min") ?: "15 min")
    val alwaysOnScreenDuration: StateFlow<String> = _alwaysOnScreenDuration.asStateFlow()

    private val _clipboardAutoCleanupEnabled = MutableStateFlow(prefs.getBoolean(KEY_CLIPBOARD_CLEANUP, true))
    val clipboardAutoCleanupEnabled: StateFlow<Boolean> = _clipboardAutoCleanupEnabled.asStateFlow()

    private val _clipboardCleanupDelay = MutableStateFlow(prefs.getString(KEY_CLIPBOARD_CLEANUP_DELAY, "30 sec") ?: "30 sec")
    val clipboardCleanupDelay: StateFlow<String> = _clipboardCleanupDelay.asStateFlow()

    // ── Export privacy settings ──
    /** When true, clear the system clipboard shortly after exported text is copied. */
    private val _clearClipboardAfterExport = MutableStateFlow(prefs.getBoolean(KEY_CLEAR_CLIPBOARD_AFTER_EXPORT, true))
    val clearClipboardAfterExport: StateFlow<Boolean> = _clearClipboardAfterExport.asStateFlow()

    /**
     * Controls GPS precision in exports.
     * "Exact"       — include full coordinates as recorded
     * "Approximate" — round to 2 decimal places (~1 km radius)
     * "Remove"      — strip all location data from the export
     */
    private val _exportGpsPrivacy = MutableStateFlow(prefs.getString(KEY_EXPORT_GPS_PRIVACY, "Exact") ?: "Exact")
    val exportGpsPrivacy: StateFlow<String> = _exportGpsPrivacy.asStateFlow()

    /** When true, media file attachments are excluded from exports and backups. */
    private val _exportExcludeMedia = MutableStateFlow(prefs.getBoolean(KEY_EXPORT_EXCLUDE_MEDIA, false))
    val exportExcludeMedia: StateFlow<Boolean> = _exportExcludeMedia.asStateFlow()

    /** Call explicitly after initialization — avoids scheduling jobs on every getInstance(). */
    fun initializeBackgroundWork() {
        FieldMindBackgroundScheduler.syncAll(
            appContext,
            _autoBackupEnabled.value,
            _autoBackupInterval.value,
            _remindersEnabled.value
        )
    }

    fun setDailyObservationGoal(value: Int) = edit(KEY_DAILY_GOAL, value.coerceAtLeast(0)) { _dailyObservationGoal.value = value.coerceAtLeast(0) }
    fun setDefaultCategory(value: String) = edit(KEY_DEFAULT_CATEGORY, value) { _defaultCategory.value = value }
    fun setDefaultConfidence(value: String) = edit(KEY_DEFAULT_CONFIDENCE, value) { _defaultConfidence.value = value }
    fun setLocationMode(value: String) = edit(KEY_LOCATION_MODE, value) { _locationMode.value = value }
    fun setMediaAttachmentsEnabled(value: Boolean) = edit(KEY_MEDIA_ATTACHMENTS, value) { _mediaAttachmentsEnabled.value = value }
    fun setAudioRecordingEnabled(value: Boolean) = edit(KEY_AUDIO_RECORDING, value) { _audioRecordingEnabled.value = value }
    fun setAttachmentExportMode(value: String) = edit(KEY_ATTACHMENT_EXPORT_MODE, value) { _attachmentExportMode.value = value }
    fun setGeminiEnabled(value: Boolean) = edit(KEY_GEMINI_ENABLED, value) { _geminiEnabled.value = value }
    fun setAiProvider(value: String) = edit(KEY_AI_PROVIDER, value) { _aiProvider.value = value }
    fun setGeminiApiKey(value: String) = edit(KEY_GEMINI_API_KEY, value.trim()) { _geminiApiKey.value = value.trim() }
    fun setGeminiModel(value: String) = edit(KEY_GEMINI_MODEL, value) { _geminiModel.value = value }
    fun setOpenAiApiKey(value: String) = edit(KEY_OPENAI_API_KEY, value.trim()) { _openAiApiKey.value = value.trim() }
    fun setOpenAiModel(value: String) = edit(KEY_OPENAI_MODEL, value) { _openAiModel.value = value }
    fun setAiRequireConfirmBeforeSave(value: Boolean) = edit(KEY_AI_CONFIRM, value) { _aiRequireConfirmBeforeSave.value = value }
    fun setAiSendAttachments(value: Boolean) = edit(KEY_AI_SEND_ATTACHMENTS, value) { _aiSendAttachments.value = value }
    fun setRemindersEnabled(value: Boolean) = edit(KEY_REMINDERS, value) {
        _remindersEnabled.value = value
        FieldMindBackgroundScheduler.scheduleDailyReminder(appContext, value)
    }
    fun setStreaksEnabled(value: Boolean) = edit(KEY_STREAKS, value) { _streaksEnabled.value = value }
    fun setDefaultExportFormat(value: String) = edit(KEY_EXPORT_FORMAT, value) { _defaultExportFormat.value = value }
    fun setPrivacyLockEnabled(value: Boolean) = edit(KEY_PRIVACY_LOCK, value) { _privacyLockEnabled.value = value }
    fun setPrivacyTypingEnabled(value: Boolean) = edit(KEY_PRIVACY_TYPING, value) { _privacyTypingEnabled.value = value }
    fun setDynamicColorEnabled(value: Boolean) = edit(KEY_DYNAMIC_COLOR, value) { _dynamicColorEnabled.value = value }
    fun setThemeMode(value: String) = edit(KEY_THEME_MODE, value) { _themeMode.value = value }
    fun setProfileName(value: String) = edit(KEY_PROFILE_NAME, value.trim()) { _profileName.value = value.trim() }
    fun setProfileRole(value: String) = edit(KEY_PROFILE_ROLE, value) { _profileRole.value = value }
    fun setProfileFocus(value: String) = edit(KEY_PROFILE_FOCUS, value) { _profileFocus.value = value }
    fun setLocalModelEnabled(value: Boolean) = edit(KEY_LOCAL_MODEL_ENABLED, value) { _localModelEnabled.value = value }
    fun setLocalModelOption(value: String) = edit(KEY_LOCAL_MODEL_OPTION, value) { _localModelOption.value = value }
    fun setLocalModelDownloaded(value: Boolean) = edit(KEY_LOCAL_MODEL_DOWNLOADED, value) { _localModelDownloaded.value = value }
    fun setLocalModelUseForStudy(value: Boolean) = edit(KEY_LOCAL_MODEL_USE_STUDY, value) { _localModelUseForStudy.value = value }
    fun setAutoBackupEnabled(value: Boolean) = edit(KEY_AUTO_BACKUP_ENABLED, value) {
        _autoBackupEnabled.value = value
        FieldMindBackgroundScheduler.scheduleAutoBackup(appContext, value, _autoBackupInterval.value)
    }
    fun setAutoBackupInterval(value: String) = edit(KEY_AUTO_BACKUP_INTERVAL, value) {
        _autoBackupInterval.value = value
        FieldMindBackgroundScheduler.scheduleAutoBackup(appContext, _autoBackupEnabled.value, value)
    }
    fun setBackupFolderUri(value: String) = edit(KEY_BACKUP_FOLDER_URI, value) { _backupFolderUri.value = value }
    fun setAutoWeatherEnabled(value: Boolean) = edit(KEY_AUTO_WEATHER, value) { _autoWeatherEnabled.value = value }
    fun setAutoFlashcardsEnabled(value: Boolean) = edit(KEY_AUTO_FLASHCARDS, value) { _autoFlashcardsEnabled.value = value }
    fun setAutoPatternDetectionEnabled(value: Boolean) = edit(KEY_AUTO_PATTERNS, value) { _autoPatternDetectionEnabled.value = value }
    fun setAutoQuestionsEnabled(value: Boolean) = edit(KEY_AUTO_QUESTIONS, value) { _autoQuestionsEnabled.value = value }
    fun setTempUnit(value: String) = edit(KEY_TEMP_UNIT, value) { _tempUnit.value = value }
    fun setWeatherRefreshInterval(value: String) = edit(KEY_WEATHER_REFRESH, value) { _weatherRefreshInterval.value = value }
    fun setWeatherShowTemperature(value: Boolean) = edit(KEY_WEATHER_SHOW_TEMP, value) { _weatherShowTemperature.value = value }
    fun setWeatherShowCondition(value: Boolean) = edit(KEY_WEATHER_SHOW_CONDITION, value) { _weatherShowCondition.value = value }
    fun setWeatherShowHumidity(value: Boolean) = edit(KEY_WEATHER_SHOW_HUMIDITY, value) { _weatherShowHumidity.value = value }
    fun setWeatherShowWind(value: Boolean) = edit(KEY_WEATHER_SHOW_WIND, value) { _weatherShowWind.value = value }
    fun setWeatherShowCloudCover(value: Boolean) = edit(KEY_WEATHER_SHOW_CLOUD, value) { _weatherShowCloudCover.value = value }
    fun setWeatherShowPressure(value: Boolean) = edit(KEY_WEATHER_SHOW_PRESSURE, value) { _weatherShowPressure.value = value }
    fun setScreenCaptureProtectionEnabled(value: Boolean) = edit(KEY_SCREEN_CAPTURE_PROTECTION, value) { _screenCaptureProtectionEnabled.value = value }
    fun setAlwaysOnScreenEnabled(value: Boolean) = edit(KEY_ALWAYS_ON_SCREEN, value) { _alwaysOnScreenEnabled.value = value }
    fun setAlwaysOnScreenDuration(value: String) = edit(KEY_ALWAYS_ON_SCREEN_DURATION, value) { _alwaysOnScreenDuration.value = value }
    fun setClipboardAutoCleanupEnabled(value: Boolean) = edit(KEY_CLIPBOARD_CLEANUP, value) { _clipboardAutoCleanupEnabled.value = value }
    fun setClipboardCleanupDelay(value: String) = edit(KEY_CLIPBOARD_CLEANUP_DELAY, value) { _clipboardCleanupDelay.value = value }
    fun setClearClipboardAfterExport(value: Boolean) = edit(KEY_CLEAR_CLIPBOARD_AFTER_EXPORT, value) { _clearClipboardAfterExport.value = value }
    fun setExportGpsPrivacy(value: String) = edit(KEY_EXPORT_GPS_PRIVACY, value) { _exportGpsPrivacy.value = value }
    fun setExportExcludeMedia(value: Boolean) = edit(KEY_EXPORT_EXCLUDE_MEDIA, value) { _exportExcludeMedia.value = value }
    fun setWeatherShowCloudAnimation(value: Boolean) = edit(KEY_WEATHER_SHOW_CLOUD_ANIMATION, value) { _weatherShowCloudAnimation.value = value }
    fun setWeatherProvider(value: String) = edit(KEY_WEATHER_PROVIDER, value) { _weatherProvider.value = value }
    fun setWeatherProviders(value: String) = edit(KEY_WEATHER_PROVIDERS, value) {
        _weatherProviders.value = value
        _weatherProvider.value = value.split(",").firstOrNull { it.isNotBlank() } ?: "met-norway"
        prefs.edit().putString(KEY_WEATHER_PROVIDER, _weatherProvider.value).apply()
    }
    fun setWeatherProviderEnabled(slug: String, enabled: Boolean) {
        val current = _weatherProviders.value.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        if (enabled) current.add(slug) else current.remove(slug)
        if (current.isEmpty()) current.add("met-norway")
        setWeatherProviders(current.joinToString(","))
    }
    fun setWeatherApiKey(value: String) = edit(KEY_WEATHER_API_KEY, value.trim()) { _weatherApiKey.value = value.trim() }
    fun setOpenWeatherMapApiKey(value: String) = edit(KEY_OPENWEATHERMAP_API_KEY, value.trim()) { _openWeatherMapApiKey.value = value.trim() }
    fun setWeatherApiDotComApiKey(value: String) = edit(KEY_WEATHERAPI_API_KEY, value.trim()) { _weatherApiDotComApiKey.value = value.trim() }
    fun setImdApiKey(value: String) = edit(KEY_IMD_API_KEY, value.trim()) { _imdApiKey.value = value.trim() }
    fun setOpenMeteoApiConfig(value: String) = edit(KEY_OPENMETEO_CONFIG, value.trim()) { _openMeteoApiConfig.value = value.trim() }
    fun setGpsMode(value: String) = edit(KEY_GPS_MODE, value) { _gpsMode.value = value }
    fun setDistanceUnit(value: String) = edit(KEY_DISTANCE_UNIT, value) { _distanceUnit.value = value }
    fun setWindSpeedUnit(value: String) = edit(KEY_WIND_SPEED_UNIT, value) { _windSpeedUnit.value = value }
    fun setTimeFormat(value: String) = edit(KEY_TIME_FORMAT, value) { _timeFormat.value = value }
    fun setDateFormat(value: String) = edit(KEY_DATE_FORMAT, value) { _dateFormat.value = value }
    fun setMapType(value: String) = edit(KEY_MAP_TYPE, value) { _mapType.value = value }
    fun setMapShowLocation(value: Boolean) = edit(KEY_MAP_SHOW_LOCATION, value) { _mapShowLocation.value = value }
    fun setFieldModeDefaultSession(value: String) = edit(KEY_FIELD_MODE_DEFAULT_SESSION, value) { _fieldModeDefaultSession.value = value }
    fun setFieldModeAutoStartTimer(value: Boolean) = edit(KEY_FIELD_MODE_AUTO_START_TIMER, value) { _fieldModeAutoStartTimer.value = value }
    fun setFieldModeObservationSpacing(value: String) = edit(KEY_FIELD_MODE_OBSERVATION_SPACING, value) { _fieldModeObservationSpacing.value = value }
    fun setDeveloperMode(value: Boolean) = edit(KEY_DEVELOPER_MODE, value) { _developerMode.value = value }
    fun setDebugLogging(value: Boolean) = edit(KEY_DEBUG_LOGGING, value) { _debugLogging.value = value }
    fun setDataIntegrityCheckOnLaunch(value: Boolean) = edit(KEY_DATA_INTEGRITY_CHECK, value) { _dataIntegrityCheckOnLaunch.value = value }
    fun setLockTimeout(value: String) = edit(KEY_LOCK_TIMEOUT, value) { _lockTimeout.value = value }
    fun setAutoLockOnBackground(value: Boolean) = edit(KEY_AUTO_LOCK_BACKGROUND, value) { _autoLockOnBackground.value = value }

    // ── Onboarding / interests ──
    private val _userInterests = MutableStateFlow(UserInterests.fromJson(prefs.getString(KEY_USER_INTERESTS, null)))
    val userInterests: StateFlow<UserInterests> = _userInterests.asStateFlow()

    private val _screenVisibility = MutableStateFlow(ScreenVisibility.fromJson(prefs.getString(KEY_SCREEN_VISIBILITY, null)))
    val screenVisibility: StateFlow<ScreenVisibility> = _screenVisibility.asStateFlow()

    private val _onboardingExtendedTourCompleted = MutableStateFlow(prefs.getBoolean(KEY_EXTENDED_TOUR_DONE, false))
    val onboardingExtendedTourCompleted: StateFlow<Boolean> = _onboardingExtendedTourCompleted.asStateFlow()

    fun setUserInterests(value: UserInterests) {
        val json = UserInterests.toJson(value)
        prefs.edit().putString(KEY_USER_INTERESTS, json).apply()
        _userInterests.value = value
        // Auto-configure screen visibility from interests if user hasn't explicitly set it
        if (!prefs.contains(KEY_SCREEN_VISIBILITY)) {
            val derived = ScreenVisibility.fromInterests(value)
            val visJson = ScreenVisibility.toJson(derived)
            prefs.edit().putString(KEY_SCREEN_VISIBILITY, visJson).apply()
            _screenVisibility.value = derived
        }
    }

    fun setScreenVisibility(value: ScreenVisibility) {
        val json = ScreenVisibility.toJson(value)
        prefs.edit().putString(KEY_SCREEN_VISIBILITY, json).apply()
        _screenVisibility.value = value
    }

    // ── In-app PIN/password lock ──
    private val _appPinEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_PIN_ENABLED, false))
    val appPinEnabled: StateFlow<Boolean> = _appPinEnabled.asStateFlow()

    private val _appPinHash = MutableStateFlow(prefs.getString(KEY_APP_PIN_HASH, "") ?: "")
    val appPinHash: StateFlow<String> = _appPinHash.asStateFlow()

    fun setAppPinEnabled(value: Boolean) = edit(KEY_APP_PIN_ENABLED, value) { _appPinEnabled.value = value }

    fun setAppPinHash(value: String) = edit(KEY_APP_PIN_HASH, value) { _appPinHash.value = value }

    fun verifyAppPin(input: String): Boolean {
        val hash = _appPinHash.value
        if (hash.isBlank() || input.isBlank()) return false
        val inputHash = android.util.Base64.encodeToString(
            java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray()),
            android.util.Base64.NO_WRAP
        )
        return inputHash == hash
    }

    fun hashAppPin(input: String): String {
        return android.util.Base64.encodeToString(
            java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray()),
            android.util.Base64.NO_WRAP
        )
    }

    fun setOnboardingExtendedTourCompleted(value: Boolean) = edit(KEY_EXTENDED_TOUR_DONE, value) { _onboardingExtendedTourCompleted.value = value }

    /**
     * Clear ALL preferences and reset to defaults.
     * Called from developer settings. Resets every StateFlow to default values.
     */
    fun clearAllPreferences() {
        prefs.edit().clear().apply()
        // Reset all StateFlow backing fields to defaults
        _dailyObservationGoal.value = 1
        _defaultCategory.value = "Bird"
        _defaultConfidence.value = "Sure"
        _locationMode.value = "Manual only"
        _mediaAttachmentsEnabled.value = true
        _audioRecordingEnabled.value = true
        _attachmentExportMode.value = "Reference URIs"
        _geminiEnabled.value = false
        _aiProvider.value = "Gemini"
        _geminiApiKey.value = ""
        _geminiModel.value = "gemini-1.5-flash"
        _openAiApiKey.value = ""
        _openAiModel.value = "gpt-4.1-mini"
        _aiRequireConfirmBeforeSave.value = true
        _aiSendAttachments.value = false
        _remindersEnabled.value = false
        _streaksEnabled.value = true
        _defaultExportFormat.value = "Markdown"
        _privacyLockEnabled.value = false
        _privacyTypingEnabled.value = false
        _dynamicColorEnabled.value = false
        _themeMode.value = "Dark"
        _profileName.value = ""
        _profileRole.value = "Field learner"
        _profileFocus.value = "Wildlife & ecology"
        _localModelEnabled.value = false
        _localModelOption.value = "FieldLite 500 MB"
        _localModelDownloaded.value = false
        _localModelUseForStudy.value = true
        _autoBackupEnabled.value = false
        _autoBackupInterval.value = "Weekly"
        _backupFolderUri.value = ""
        _autoWeatherEnabled.value = false
        _autoFlashcardsEnabled.value = false
        _autoPatternDetectionEnabled.value = true
        _autoQuestionsEnabled.value = false
        _tempUnit.value = "Celsius"
        _weatherRefreshInterval.value = "30 min"
        _weatherShowTemperature.value = true
        _weatherShowCondition.value = true
        _weatherShowHumidity.value = true
        _weatherShowWind.value = true
        _weatherShowCloudCover.value = true
        _weatherShowPressure.value = false
        _weatherShowCloudAnimation.value = true
        _weatherProvider.value = "met-norway"
        _weatherProviders.value = "met-norway"
        _weatherApiKey.value = ""
        _openWeatherMapApiKey.value = ""
        _weatherApiDotComApiKey.value = ""
        _imdApiKey.value = ""
        _openMeteoApiConfig.value = ""
        _gpsMode.value = "On capture only"
        _distanceUnit.value = "km"
        _windSpeedUnit.value = "km/h"
        _timeFormat.value = "24h"
        _dateFormat.value = "ISO"
        _mapType.value = "Standard"
        _mapShowLocation.value = true
        _fieldModeDefaultSession.value = "Quick capture"
        _fieldModeAutoStartTimer.value = false
        _fieldModeObservationSpacing.value = "None"
        _developerMode.value = false
        _debugLogging.value = false
        _dataIntegrityCheckOnLaunch.value = false
        _lockTimeout.value = "Immediate"
        _autoLockOnBackground.value = true
        _screenCaptureProtectionEnabled.value = false
        _alwaysOnScreenEnabled.value = false
        _alwaysOnScreenDuration.value = "15 min"
        _clipboardAutoCleanupEnabled.value = true
        _clipboardCleanupDelay.value = "30 sec"
        _clearClipboardAfterExport.value = true
        _exportGpsPrivacy.value = "Exact"
        _exportExcludeMedia.value = false
        _speciesIdApiKey.value = ""
        _speciesIdOfflineFirst.value = true
        _speciesModelBaseUrl.value = ""
        _perenualApiKey.value = ""
        _appPinEnabled.value = false
        _appPinHash.value = ""
        _userInterests.value = UserInterests()
        _screenVisibility.value = ScreenVisibility()
        _onboardingExtendedTourCompleted.value = false
    }

    // ── Species identification setters ──
    fun setSpeciesIdApiKey(value: String) = edit(KEY_SPECIES_ID_API_KEY, value.trim()) { _speciesIdApiKey.value = value.trim() }
    fun setSpeciesIdOfflineFirst(value: Boolean) = edit(KEY_SPECIES_ID_OFFLINE_FIRST, value) { _speciesIdOfflineFirst.value = value }
    fun setSpeciesModelBaseUrl(value: String) = edit(KEY_SPECIES_MODEL_BASE_URL, value.trim()) { _speciesModelBaseUrl.value = value.trim() }
    // ── Perenual API ──
    fun setPerenualApiKey(value: String) = edit(KEY_PERENUAL_API_KEY, value.trim()) { _perenualApiKey.value = value.trim() }

    // ── Settings export/import for backup ──
    /**
     * Export all settings as a JSON string for inclusion in the archive.
     */
    fun toExportJson(): String = org.json.JSONObject().apply {
        put(KEY_DAILY_GOAL, _dailyObservationGoal.value)
        put(KEY_DEFAULT_CATEGORY, _defaultCategory.value)
        put(KEY_DEFAULT_CONFIDENCE, _defaultConfidence.value)
        put(KEY_LOCATION_MODE, _locationMode.value)
        put(KEY_MEDIA_ATTACHMENTS, _mediaAttachmentsEnabled.value)
        put(KEY_AUDIO_RECORDING, _audioRecordingEnabled.value)
        put(KEY_ATTACHMENT_EXPORT_MODE, _attachmentExportMode.value)
        put(KEY_GEMINI_ENABLED, _geminiEnabled.value)
        put(KEY_AI_PROVIDER, _aiProvider.value)
        put(KEY_GEMINI_API_KEY, _geminiApiKey.value)
        put(KEY_GEMINI_MODEL, _geminiModel.value)
        put(KEY_OPENAI_API_KEY, _openAiApiKey.value)
        put(KEY_OPENAI_MODEL, _openAiModel.value)
        put(KEY_AI_CONFIRM, _aiRequireConfirmBeforeSave.value)
        put(KEY_AI_SEND_ATTACHMENTS, _aiSendAttachments.value)
        put(KEY_REMINDERS, _remindersEnabled.value)
        put(KEY_STREAKS, _streaksEnabled.value)
        put(KEY_EXPORT_FORMAT, _defaultExportFormat.value)
        put(KEY_PRIVACY_LOCK, _privacyLockEnabled.value)
        put(KEY_PRIVACY_TYPING, _privacyTypingEnabled.value)
        put(KEY_DYNAMIC_COLOR, _dynamicColorEnabled.value)
        put(KEY_THEME_MODE, _themeMode.value)
        put(KEY_PROFILE_NAME, _profileName.value)
        put(KEY_PROFILE_ROLE, _profileRole.value)
        put(KEY_PROFILE_FOCUS, _profileFocus.value)
        put(KEY_LOCAL_MODEL_ENABLED, _localModelEnabled.value)
        put(KEY_LOCAL_MODEL_OPTION, _localModelOption.value)
        put(KEY_LOCAL_MODEL_DOWNLOADED, _localModelDownloaded.value)
        put(KEY_LOCAL_MODEL_USE_STUDY, _localModelUseForStudy.value)
        put(KEY_AUTO_BACKUP_ENABLED, _autoBackupEnabled.value)
        put(KEY_AUTO_BACKUP_INTERVAL, _autoBackupInterval.value)
        put(KEY_BACKUP_FOLDER_URI, _backupFolderUri.value)
        put(KEY_AUTO_WEATHER, _autoWeatherEnabled.value)
        put(KEY_AUTO_FLASHCARDS, _autoFlashcardsEnabled.value)
        put(KEY_AUTO_PATTERNS, _autoPatternDetectionEnabled.value)
        put(KEY_AUTO_QUESTIONS, _autoQuestionsEnabled.value)
        put(KEY_TEMP_UNIT, _tempUnit.value)
        put(KEY_WEATHER_REFRESH, _weatherRefreshInterval.value)
        put(KEY_WEATHER_SHOW_TEMP, _weatherShowTemperature.value)
        put(KEY_WEATHER_SHOW_CONDITION, _weatherShowCondition.value)
        put(KEY_WEATHER_SHOW_HUMIDITY, _weatherShowHumidity.value)
        put(KEY_WEATHER_SHOW_WIND, _weatherShowWind.value)
        put(KEY_WEATHER_SHOW_CLOUD, _weatherShowCloudCover.value)
        put(KEY_WEATHER_SHOW_PRESSURE, _weatherShowPressure.value)
        put(KEY_WEATHER_SHOW_CLOUD_ANIMATION, _weatherShowCloudAnimation.value)
        put(KEY_WEATHER_PROVIDER, _weatherProvider.value)
        put(KEY_WEATHER_PROVIDERS, _weatherProviders.value)
        put(KEY_WEATHER_API_KEY, _weatherApiKey.value)
        put(KEY_OPENWEATHERMAP_API_KEY, _openWeatherMapApiKey.value)
        put(KEY_WEATHERAPI_API_KEY, _weatherApiDotComApiKey.value)
        put(KEY_IMD_API_KEY, _imdApiKey.value)
        put(KEY_OPENMETEO_CONFIG, _openMeteoApiConfig.value)
        put(KEY_GPS_MODE, _gpsMode.value)
        put(KEY_DISTANCE_UNIT, _distanceUnit.value)
        put(KEY_WIND_SPEED_UNIT, _windSpeedUnit.value)
        put(KEY_TIME_FORMAT, _timeFormat.value)
        put(KEY_DATE_FORMAT, _dateFormat.value)
        put(KEY_MAP_TYPE, _mapType.value)
        put(KEY_MAP_SHOW_LOCATION, _mapShowLocation.value)
        put(KEY_FIELD_MODE_DEFAULT_SESSION, _fieldModeDefaultSession.value)
        put(KEY_FIELD_MODE_AUTO_START_TIMER, _fieldModeAutoStartTimer.value)
        put(KEY_FIELD_MODE_OBSERVATION_SPACING, _fieldModeObservationSpacing.value)
        put(KEY_DEVELOPER_MODE, _developerMode.value)
        put(KEY_DEBUG_LOGGING, _debugLogging.value)
        put(KEY_DATA_INTEGRITY_CHECK, _dataIntegrityCheckOnLaunch.value)
        put(KEY_LOCK_TIMEOUT, _lockTimeout.value)
        put(KEY_AUTO_LOCK_BACKGROUND, _autoLockOnBackground.value)
        put(KEY_SCREEN_CAPTURE_PROTECTION, _screenCaptureProtectionEnabled.value)
        put(KEY_ALWAYS_ON_SCREEN, _alwaysOnScreenEnabled.value)
        put(KEY_ALWAYS_ON_SCREEN_DURATION, _alwaysOnScreenDuration.value)
        put(KEY_CLIPBOARD_CLEANUP, _clipboardAutoCleanupEnabled.value)
        put(KEY_CLIPBOARD_CLEANUP_DELAY, _clipboardCleanupDelay.value)
        put(KEY_CLEAR_CLIPBOARD_AFTER_EXPORT, _clearClipboardAfterExport.value)
        put(KEY_EXPORT_GPS_PRIVACY, _exportGpsPrivacy.value)
        put(KEY_EXPORT_EXCLUDE_MEDIA, _exportExcludeMedia.value)
        put(KEY_SPECIES_ID_API_KEY, _speciesIdApiKey.value)
        put(KEY_SPECIES_ID_OFFLINE_FIRST, _speciesIdOfflineFirst.value)
        put(KEY_SPECIES_MODEL_BASE_URL, _speciesModelBaseUrl.value)
        put(KEY_PERENUAL_API_KEY, _perenualApiKey.value)
        put(KEY_APP_PIN_ENABLED, _appPinEnabled.value)
        put(KEY_APP_PIN_HASH, _appPinHash.value)
        put(KEY_USER_INTERESTS, UserInterests.toJson(_userInterests.value))
        put(KEY_SCREEN_VISIBILITY, ScreenVisibility.toJson(_screenVisibility.value))
        put(KEY_EXTENDED_TOUR_DONE, _onboardingExtendedTourCompleted.value)
    }.toString(2)

    /**
     * Restore all settings from a previously exported JSON object.
     */
    fun applyFromJson(json: org.json.JSONObject) {
        val edit = prefs.edit()
        // Helper to read keys and apply
        fun applyString(key: String) { if (json.has(key)) edit.putString(key, json.optString(key, "")) }
        fun applyBoolean(key: String, default: Boolean = false) { if (json.has(key)) edit.putBoolean(key, json.optBoolean(key, default)) }
        fun applyInt(key: String, default: Int = 0) { if (json.has(key)) edit.putInt(key, json.optInt(key, default)) }

        applyString(KEY_DEFAULT_CATEGORY)
        applyString(KEY_DEFAULT_CONFIDENCE)
        applyString(KEY_LOCATION_MODE)
        applyBoolean(KEY_MEDIA_ATTACHMENTS, true)
        applyBoolean(KEY_AUDIO_RECORDING, true)
        applyString(KEY_ATTACHMENT_EXPORT_MODE)
        applyBoolean(KEY_GEMINI_ENABLED)
        applyString(KEY_AI_PROVIDER)
        applyString(KEY_GEMINI_API_KEY)
        applyString(KEY_GEMINI_MODEL)
        applyString(KEY_OPENAI_API_KEY)
        applyString(KEY_OPENAI_MODEL)
        applyBoolean(KEY_AI_CONFIRM, true)
        applyBoolean(KEY_AI_SEND_ATTACHMENTS)
        applyBoolean(KEY_REMINDERS)
        applyBoolean(KEY_STREAKS, true)
        applyString(KEY_EXPORT_FORMAT)
        applyBoolean(KEY_PRIVACY_LOCK)
        applyBoolean(KEY_PRIVACY_TYPING)
        applyBoolean(KEY_DYNAMIC_COLOR)
        applyString(KEY_THEME_MODE)
        applyString(KEY_PROFILE_NAME)
        applyString(KEY_PROFILE_ROLE)
        applyString(KEY_PROFILE_FOCUS)
        applyBoolean(KEY_LOCAL_MODEL_ENABLED)
        applyString(KEY_LOCAL_MODEL_OPTION)
        applyBoolean(KEY_LOCAL_MODEL_DOWNLOADED)
        applyBoolean(KEY_LOCAL_MODEL_USE_STUDY, true)
        applyBoolean(KEY_AUTO_BACKUP_ENABLED)
        applyString(KEY_AUTO_BACKUP_INTERVAL)
        applyString(KEY_BACKUP_FOLDER_URI)
        applyBoolean(KEY_AUTO_WEATHER)
        applyBoolean(KEY_AUTO_FLASHCARDS)
        applyBoolean(KEY_AUTO_PATTERNS, true)
        applyBoolean(KEY_AUTO_QUESTIONS)
        applyString(KEY_TEMP_UNIT)
        applyString(KEY_WEATHER_REFRESH)
        applyBoolean(KEY_WEATHER_SHOW_TEMP, true)
        applyBoolean(KEY_WEATHER_SHOW_CONDITION, true)
        applyBoolean(KEY_WEATHER_SHOW_HUMIDITY, true)
        applyBoolean(KEY_WEATHER_SHOW_WIND, true)
        applyBoolean(KEY_WEATHER_SHOW_CLOUD, true)
        applyBoolean(KEY_WEATHER_SHOW_PRESSURE)
        applyBoolean(KEY_WEATHER_SHOW_CLOUD_ANIMATION, true)
        applyString(KEY_WEATHER_PROVIDER)
        applyString(KEY_WEATHER_PROVIDERS)
        applyString(KEY_WEATHER_API_KEY)
        applyString(KEY_OPENWEATHERMAP_API_KEY)
        applyString(KEY_WEATHERAPI_API_KEY)
        applyString(KEY_IMD_API_KEY)
        applyString(KEY_OPENMETEO_CONFIG)
        applyString(KEY_GPS_MODE)
        applyString(KEY_DISTANCE_UNIT)
        applyString(KEY_WIND_SPEED_UNIT)
        applyString(KEY_TIME_FORMAT)
        applyString(KEY_DATE_FORMAT)
        applyString(KEY_MAP_TYPE)
        applyBoolean(KEY_MAP_SHOW_LOCATION, true)
        applyString(KEY_FIELD_MODE_DEFAULT_SESSION)
        applyBoolean(KEY_FIELD_MODE_AUTO_START_TIMER)
        applyString(KEY_FIELD_MODE_OBSERVATION_SPACING)
        applyBoolean(KEY_DEVELOPER_MODE)
        applyBoolean(KEY_DEBUG_LOGGING)
        applyBoolean(KEY_DATA_INTEGRITY_CHECK)
        applyString(KEY_LOCK_TIMEOUT)
        applyBoolean(KEY_AUTO_LOCK_BACKGROUND, true)
        applyBoolean(KEY_SCREEN_CAPTURE_PROTECTION)
        applyBoolean(KEY_ALWAYS_ON_SCREEN)
        applyString(KEY_ALWAYS_ON_SCREEN_DURATION)
        applyBoolean(KEY_CLIPBOARD_CLEANUP, true)
        applyString(KEY_CLIPBOARD_CLEANUP_DELAY)
        applyBoolean(KEY_CLEAR_CLIPBOARD_AFTER_EXPORT, true)
        applyString(KEY_EXPORT_GPS_PRIVACY)
        applyBoolean(KEY_EXPORT_EXCLUDE_MEDIA)
        applyString(KEY_SPECIES_ID_API_KEY)
        applyBoolean(KEY_SPECIES_ID_OFFLINE_FIRST, true)
        applyString(KEY_SPECIES_MODEL_BASE_URL)
        applyString(KEY_PERENUAL_API_KEY)
        applyBoolean(KEY_APP_PIN_ENABLED)
        applyString(KEY_APP_PIN_HASH)
        if (json.has(KEY_USER_INTERESTS)) {
            val jsonStr = json.optString(KEY_USER_INTERESTS, "")
            val interests = UserInterests.fromJson(jsonStr)
            edit.putString(KEY_USER_INTERESTS, jsonStr)
            _userInterests.value = interests
        }
        if (json.has(KEY_SCREEN_VISIBILITY)) {
            val jsonStr = json.optString(KEY_SCREEN_VISIBILITY, "")
            val vis = ScreenVisibility.fromJson(jsonStr)
            edit.putString(KEY_SCREEN_VISIBILITY, jsonStr)
            _screenVisibility.value = vis
        }
        applyBoolean(KEY_EXTENDED_TOUR_DONE)
        applyInt(KEY_DAILY_GOAL)

        edit.apply()

        // Refresh StateFlows for all edited keys that have backing StateFlows
        _dailyObservationGoal.value = prefs.getInt(KEY_DAILY_GOAL, 1)
        _defaultCategory.value = prefs.getString(KEY_DEFAULT_CATEGORY, "Bird") ?: "Bird"
        _defaultConfidence.value = prefs.getString(KEY_DEFAULT_CONFIDENCE, "Sure") ?: "Sure"
        _locationMode.value = prefs.getString(KEY_LOCATION_MODE, "Manual only") ?: "Manual only"
        _mediaAttachmentsEnabled.value = prefs.getBoolean(KEY_MEDIA_ATTACHMENTS, true)
        _audioRecordingEnabled.value = prefs.getBoolean(KEY_AUDIO_RECORDING, true)
        _attachmentExportMode.value = prefs.getString(KEY_ATTACHMENT_EXPORT_MODE, "Reference URIs") ?: "Reference URIs"
        _geminiEnabled.value = prefs.getBoolean(KEY_GEMINI_ENABLED, false)
        _aiProvider.value = prefs.getString(KEY_AI_PROVIDER, "Gemini") ?: "Gemini"
        _geminiApiKey.value = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        _geminiModel.value = prefs.getString(KEY_GEMINI_MODEL, "gemini-1.5-flash") ?: "gemini-1.5-flash"
        _openAiApiKey.value = prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
        _openAiModel.value = prefs.getString(KEY_OPENAI_MODEL, "gpt-4.1-mini") ?: "gpt-4.1-mini"
        _aiRequireConfirmBeforeSave.value = prefs.getBoolean(KEY_AI_CONFIRM, true)
        _aiSendAttachments.value = prefs.getBoolean(KEY_AI_SEND_ATTACHMENTS, false)
        _remindersEnabled.value = prefs.getBoolean(KEY_REMINDERS, false)
        _streaksEnabled.value = prefs.getBoolean(KEY_STREAKS, true)
        _defaultExportFormat.value = prefs.getString(KEY_EXPORT_FORMAT, "Markdown") ?: "Markdown"
        _privacyLockEnabled.value = prefs.getBoolean(KEY_PRIVACY_LOCK, false)
        _privacyTypingEnabled.value = prefs.getBoolean(KEY_PRIVACY_TYPING, false)
        _dynamicColorEnabled.value = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        _themeMode.value = prefs.getString(KEY_THEME_MODE, "Dark") ?: "Dark"
        _profileName.value = prefs.getString(KEY_PROFILE_NAME, "") ?: ""
        _profileRole.value = prefs.getString(KEY_PROFILE_ROLE, "Field learner") ?: "Field learner"
        _profileFocus.value = prefs.getString(KEY_PROFILE_FOCUS, "Wildlife & ecology") ?: "Wildlife & ecology"
        _localModelEnabled.value = prefs.getBoolean(KEY_LOCAL_MODEL_ENABLED, false)
        _localModelOption.value = prefs.getString(KEY_LOCAL_MODEL_OPTION, "FieldLite 500 MB") ?: "FieldLite 500 MB"
        _localModelDownloaded.value = prefs.getBoolean(KEY_LOCAL_MODEL_DOWNLOADED, false)
        _localModelUseForStudy.value = prefs.getBoolean(KEY_LOCAL_MODEL_USE_STUDY, true)
        _autoBackupEnabled.value = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        _autoBackupInterval.value = prefs.getString(KEY_AUTO_BACKUP_INTERVAL, "Weekly") ?: "Weekly"
        _backupFolderUri.value = prefs.getString(KEY_BACKUP_FOLDER_URI, "") ?: ""
        _autoWeatherEnabled.value = prefs.getBoolean(KEY_AUTO_WEATHER, false)
        _autoFlashcardsEnabled.value = prefs.getBoolean(KEY_AUTO_FLASHCARDS, false)
        _autoPatternDetectionEnabled.value = prefs.getBoolean(KEY_AUTO_PATTERNS, true)
        _autoQuestionsEnabled.value = prefs.getBoolean(KEY_AUTO_QUESTIONS, false)
        _tempUnit.value = prefs.getString(KEY_TEMP_UNIT, "Celsius") ?: "Celsius"
        _weatherRefreshInterval.value = prefs.getString(KEY_WEATHER_REFRESH, "30 min") ?: "30 min"
        _weatherShowTemperature.value = prefs.getBoolean(KEY_WEATHER_SHOW_TEMP, true)
        _weatherShowCondition.value = prefs.getBoolean(KEY_WEATHER_SHOW_CONDITION, true)
        _weatherShowHumidity.value = prefs.getBoolean(KEY_WEATHER_SHOW_HUMIDITY, true)
        _weatherShowWind.value = prefs.getBoolean(KEY_WEATHER_SHOW_WIND, true)
        _weatherShowCloudCover.value = prefs.getBoolean(KEY_WEATHER_SHOW_CLOUD, true)
        _weatherShowPressure.value = prefs.getBoolean(KEY_WEATHER_SHOW_PRESSURE, false)
        _weatherShowCloudAnimation.value = prefs.getBoolean(KEY_WEATHER_SHOW_CLOUD_ANIMATION, true)
        _weatherProvider.value = prefs.getString(KEY_WEATHER_PROVIDER, "met-norway") ?: "met-norway"
        _weatherProviders.value = prefs.getString(KEY_WEATHER_PROVIDERS, "met-norway") ?: "met-norway"
        _weatherApiKey.value = prefs.getString(KEY_WEATHER_API_KEY, "") ?: ""
        _openWeatherMapApiKey.value = prefs.getString(KEY_OPENWEATHERMAP_API_KEY, "") ?: ""
        _weatherApiDotComApiKey.value = prefs.getString(KEY_WEATHERAPI_API_KEY, "") ?: ""
        _imdApiKey.value = prefs.getString(KEY_IMD_API_KEY, "") ?: ""
        _openMeteoApiConfig.value = prefs.getString(KEY_OPENMETEO_CONFIG, "") ?: ""
        _gpsMode.value = prefs.getString(KEY_GPS_MODE, "On capture only") ?: "On capture only"
        _distanceUnit.value = prefs.getString(KEY_DISTANCE_UNIT, "km") ?: "km"
        _windSpeedUnit.value = prefs.getString(KEY_WIND_SPEED_UNIT, "km/h") ?: "km/h"
        _timeFormat.value = prefs.getString(KEY_TIME_FORMAT, "24h") ?: "24h"
        _dateFormat.value = prefs.getString(KEY_DATE_FORMAT, "ISO") ?: "ISO"
        _mapType.value = prefs.getString(KEY_MAP_TYPE, "Standard") ?: "Standard"
        _mapShowLocation.value = prefs.getBoolean(KEY_MAP_SHOW_LOCATION, true)
        _fieldModeDefaultSession.value = prefs.getString(KEY_FIELD_MODE_DEFAULT_SESSION, "Quick capture") ?: "Quick capture"
        _fieldModeAutoStartTimer.value = prefs.getBoolean(KEY_FIELD_MODE_AUTO_START_TIMER, false)
        _fieldModeObservationSpacing.value = prefs.getString(KEY_FIELD_MODE_OBSERVATION_SPACING, "None") ?: "None"
        _developerMode.value = prefs.getBoolean(KEY_DEVELOPER_MODE, false)
        _debugLogging.value = prefs.getBoolean(KEY_DEBUG_LOGGING, false)
        _dataIntegrityCheckOnLaunch.value = prefs.getBoolean(KEY_DATA_INTEGRITY_CHECK, false)
        _lockTimeout.value = prefs.getString(KEY_LOCK_TIMEOUT, "Immediate") ?: "Immediate"
        _autoLockOnBackground.value = prefs.getBoolean(KEY_AUTO_LOCK_BACKGROUND, true)
        _screenCaptureProtectionEnabled.value = prefs.getBoolean(KEY_SCREEN_CAPTURE_PROTECTION, false)
        _alwaysOnScreenEnabled.value = prefs.getBoolean(KEY_ALWAYS_ON_SCREEN, false)
        _alwaysOnScreenDuration.value = prefs.getString(KEY_ALWAYS_ON_SCREEN_DURATION, "15 min") ?: "15 min"
        _clipboardAutoCleanupEnabled.value = prefs.getBoolean(KEY_CLIPBOARD_CLEANUP, true)
        _clipboardCleanupDelay.value = prefs.getString(KEY_CLIPBOARD_CLEANUP_DELAY, "30 sec") ?: "30 sec"
        _clearClipboardAfterExport.value = prefs.getBoolean(KEY_CLEAR_CLIPBOARD_AFTER_EXPORT, true)
        _exportGpsPrivacy.value = prefs.getString(KEY_EXPORT_GPS_PRIVACY, "Exact") ?: "Exact"
        _exportExcludeMedia.value = prefs.getBoolean(KEY_EXPORT_EXCLUDE_MEDIA, false)
        _speciesIdApiKey.value = prefs.getString(KEY_SPECIES_ID_API_KEY, "") ?: ""
        _speciesIdOfflineFirst.value = prefs.getBoolean(KEY_SPECIES_ID_OFFLINE_FIRST, true)
        _speciesModelBaseUrl.value = prefs.getString(KEY_SPECIES_MODEL_BASE_URL, "") ?: ""
        _perenualApiKey.value = prefs.getString(KEY_PERENUAL_API_KEY, "") ?: ""
        _appPinEnabled.value = prefs.getBoolean(KEY_APP_PIN_ENABLED, false)
        _appPinHash.value = prefs.getString(KEY_APP_PIN_HASH, "") ?: ""
    }

    private inline fun edit(key: String, value: String, after: () -> Unit) { prefs.edit().putString(key, value).apply(); after() }
    private inline fun edit(key: String, value: Boolean, after: () -> Unit) { prefs.edit().putBoolean(key, value).apply(); after() }
    private inline fun edit(key: String, value: Int, after: () -> Unit) { prefs.edit().putInt(key, value).apply(); after() }

    companion object {
        @Volatile private var INSTANCE: FieldMindSettings? = null
        fun getInstance(context: Context): FieldMindSettings = INSTANCE ?: synchronized(this) {
            INSTANCE ?: FieldMindSettings(context).also { INSTANCE = it }
        }

        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_DEFAULT_CATEGORY = "default_category"
        private const val KEY_DEFAULT_CONFIDENCE = "default_confidence"
        private const val KEY_LOCATION_MODE = "location_mode"
        private const val KEY_MEDIA_ATTACHMENTS = "media_attachments"
        private const val KEY_AUDIO_RECORDING = "audio_recording"
        private const val KEY_ATTACHMENT_EXPORT_MODE = "attachment_export_mode"
        private const val KEY_GEMINI_ENABLED = "gemini_enabled"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val KEY_AI_CONFIRM = "ai_confirm"
        private const val KEY_AI_SEND_ATTACHMENTS = "ai_send_attachments"
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_STREAKS = "streaks"
        private const val KEY_EXPORT_FORMAT = "export_format"
        private const val KEY_PRIVACY_LOCK = "privacy_lock"
        private const val KEY_PRIVACY_TYPING = "privacy_typing"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_PROFILE_ROLE = "profile_role"
        private const val KEY_PROFILE_FOCUS = "profile_focus"
        private const val KEY_LOCAL_MODEL_ENABLED = "local_model_enabled"
        private const val KEY_LOCAL_MODEL_OPTION = "local_model_option"
        private const val KEY_LOCAL_MODEL_DOWNLOADED = "local_model_downloaded"
        private const val KEY_LOCAL_MODEL_USE_STUDY = "local_model_use_study"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_AUTO_BACKUP_INTERVAL = "auto_backup_interval"
        private const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"
        private const val KEY_AUTO_WEATHER = "auto_weather"
        private const val KEY_AUTO_FLASHCARDS = "auto_flashcards"
        private const val KEY_AUTO_PATTERNS = "auto_patterns"
        private const val KEY_AUTO_QUESTIONS = "auto_questions"
        private const val KEY_TEMP_UNIT = "temp_unit"
        private const val KEY_WEATHER_REFRESH = "weather_refresh"
        private const val KEY_WEATHER_SHOW_TEMP = "weather_show_temp"
        private const val KEY_WEATHER_SHOW_CONDITION = "weather_show_condition"
        private const val KEY_WEATHER_SHOW_HUMIDITY = "weather_show_humidity"
        private const val KEY_WEATHER_SHOW_WIND = "weather_show_wind"
        private const val KEY_WEATHER_SHOW_CLOUD = "weather_show_cloud"
        private const val KEY_WEATHER_SHOW_PRESSURE = "weather_show_pressure"
        private const val KEY_WEATHER_SHOW_CLOUD_ANIMATION = "weather_show_cloud_animation"
        private const val KEY_WEATHER_PROVIDER = "weather_provider"
        private const val KEY_WEATHER_PROVIDERS = "weather_providers"
        private const val KEY_WEATHER_API_KEY = "weather_api_key"
        private const val KEY_OPENWEATHERMAP_API_KEY = "openweathermap_api_key"
        private const val KEY_WEATHERAPI_API_KEY = "weatherapi_api_key"
        private const val KEY_IMD_API_KEY = "imd_api_key"
        private const val KEY_OPENMETEO_CONFIG = "openmeteo_api_config"
        private const val KEY_GPS_MODE = "gps_mode"
        private const val KEY_DISTANCE_UNIT = "distance_unit"
        private const val KEY_WIND_SPEED_UNIT = "wind_speed_unit"
        private const val KEY_TIME_FORMAT = "time_format"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_MAP_TYPE = "map_type"
        private const val KEY_MAP_SHOW_LOCATION = "map_show_location"
        private const val KEY_FIELD_MODE_DEFAULT_SESSION = "field_mode_default_session"
        private const val KEY_FIELD_MODE_AUTO_START_TIMER = "field_mode_auto_start_timer"
        private const val KEY_FIELD_MODE_OBSERVATION_SPACING = "field_mode_observation_spacing"
        private const val KEY_DEVELOPER_MODE = "developer_mode"
        private const val KEY_DEBUG_LOGGING = "debug_logging"
        private const val KEY_DATA_INTEGRITY_CHECK = "data_integrity_check"
        private const val KEY_LOCK_TIMEOUT = "lock_timeout"
        private const val KEY_AUTO_LOCK_BACKGROUND = "auto_lock_background"
        // ── Species identification keys ──
        private const val KEY_SPECIES_ID_API_KEY = "species_id_api_key"
        private const val KEY_SPECIES_ID_OFFLINE_FIRST = "species_id_offline_first"
        private const val KEY_SPECIES_MODEL_BASE_URL = "species_model_base_url"
        // ── Perenual API keys ──
        private const val KEY_PERENUAL_API_KEY = "perenual_api_key"
        // ── Onboarding keys ──
        private const val KEY_APP_PIN_ENABLED = "app_pin_enabled"
        private const val KEY_APP_PIN_HASH = "app_pin_hash"
        private const val KEY_USER_INTERESTS = "user_interests"
        private const val KEY_SCREEN_VISIBILITY = "screen_visibility"
        private const val KEY_EXTENDED_TOUR_DONE = "onboarding_extended_tour_done"
        // ── Privacy & screen protection ──
        private const val KEY_SCREEN_CAPTURE_PROTECTION = "screen_capture_protection"
        private const val KEY_ALWAYS_ON_SCREEN = "always_on_screen"
        private const val KEY_ALWAYS_ON_SCREEN_DURATION = "always_on_screen_duration"
        private const val KEY_CLIPBOARD_CLEANUP = "clipboard_cleanup"
        private const val KEY_CLIPBOARD_CLEANUP_DELAY = "clipboard_cleanup_delay"
        // ── Export privacy ──
        private const val KEY_CLEAR_CLIPBOARD_AFTER_EXPORT = "clear_clipboard_after_export"
        private const val KEY_EXPORT_GPS_PRIVACY = "export_gps_privacy"
        private const val KEY_EXPORT_EXCLUDE_MEDIA = "export_exclude_media"
    }
}
