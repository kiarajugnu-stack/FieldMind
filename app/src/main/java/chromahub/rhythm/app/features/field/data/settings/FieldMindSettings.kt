package chromahub.rhythm.app.features.field.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FieldMindSettings private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("fieldmind_settings", Context.MODE_PRIVATE)

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

    private val _geminiApiKey = MutableStateFlow(prefs.getString(KEY_GEMINI_API_KEY, "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _geminiModel = MutableStateFlow(prefs.getString(KEY_GEMINI_MODEL, "gemini-1.5-flash") ?: "gemini-1.5-flash")
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

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

    private val _dynamicColorEnabled = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    /** When true, use Material You wallpaper colors instead of the FieldMind brand palette. */
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setDailyObservationGoal(value: Int) = edit(KEY_DAILY_GOAL, value.coerceAtLeast(0)) { _dailyObservationGoal.value = value.coerceAtLeast(0) }
    fun setDefaultCategory(value: String) = edit(KEY_DEFAULT_CATEGORY, value) { _defaultCategory.value = value }
    fun setDefaultConfidence(value: String) = edit(KEY_DEFAULT_CONFIDENCE, value) { _defaultConfidence.value = value }
    fun setLocationMode(value: String) = edit(KEY_LOCATION_MODE, value) { _locationMode.value = value }
    fun setMediaAttachmentsEnabled(value: Boolean) = edit(KEY_MEDIA_ATTACHMENTS, value) { _mediaAttachmentsEnabled.value = value }
    fun setAudioRecordingEnabled(value: Boolean) = edit(KEY_AUDIO_RECORDING, value) { _audioRecordingEnabled.value = value }
    fun setAttachmentExportMode(value: String) = edit(KEY_ATTACHMENT_EXPORT_MODE, value) { _attachmentExportMode.value = value }
    fun setGeminiEnabled(value: Boolean) = edit(KEY_GEMINI_ENABLED, value) { _geminiEnabled.value = value }
    fun setGeminiApiKey(value: String) = edit(KEY_GEMINI_API_KEY, value.trim()) { _geminiApiKey.value = value.trim() }
    fun setGeminiModel(value: String) = edit(KEY_GEMINI_MODEL, value) { _geminiModel.value = value }
    fun setAiRequireConfirmBeforeSave(value: Boolean) = edit(KEY_AI_CONFIRM, value) { _aiRequireConfirmBeforeSave.value = value }
    fun setAiSendAttachments(value: Boolean) = edit(KEY_AI_SEND_ATTACHMENTS, value) { _aiSendAttachments.value = value }
    fun setRemindersEnabled(value: Boolean) = edit(KEY_REMINDERS, value) { _remindersEnabled.value = value }
    fun setStreaksEnabled(value: Boolean) = edit(KEY_STREAKS, value) { _streaksEnabled.value = value }
    fun setDefaultExportFormat(value: String) = edit(KEY_EXPORT_FORMAT, value) { _defaultExportFormat.value = value }
    fun setPrivacyLockEnabled(value: Boolean) = edit(KEY_PRIVACY_LOCK, value) { _privacyLockEnabled.value = value }
    fun setDynamicColorEnabled(value: Boolean) = edit(KEY_DYNAMIC_COLOR, value) { _dynamicColorEnabled.value = value }

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
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_AI_CONFIRM = "ai_confirm"
        private const val KEY_AI_SEND_ATTACHMENTS = "ai_send_attachments"
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_STREAKS = "streaks"
        private const val KEY_EXPORT_FORMAT = "export_format"
        private const val KEY_PRIVACY_LOCK = "privacy_lock"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}
