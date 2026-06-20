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

    fun setOnboardingExtendedTourCompleted(value: Boolean) = edit(KEY_EXTENDED_TOUR_DONE, value) { _onboardingExtendedTourCompleted.value = value }

    // ── Species identification setters ──
    fun setSpeciesIdApiKey(value: String) = edit(KEY_SPECIES_ID_API_KEY, value.trim()) { _speciesIdApiKey.value = value.trim() }
    fun setSpeciesIdOfflineFirst(value: Boolean) = edit(KEY_SPECIES_ID_OFFLINE_FIRST, value) { _speciesIdOfflineFirst.value = value }
    fun setSpeciesModelBaseUrl(value: String) = edit(KEY_SPECIES_MODEL_BASE_URL, value.trim()) { _speciesModelBaseUrl.value = value.trim() }
    // ── Perenual API ──
    fun setPerenualApiKey(value: String) = edit(KEY_PERENUAL_API_KEY, value.trim()) { _perenualApiKey.value = value.trim() }

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
        private const val KEY_USER_INTERESTS = "user_interests"
        private const val KEY_SCREEN_VISIBILITY = "screen_visibility"
        private const val KEY_EXTENDED_TOUR_DONE = "onboarding_extended_tour_done"
    }
}
