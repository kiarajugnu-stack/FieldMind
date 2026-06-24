package fieldmind.research.app.activities

import android.os.Build
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import fieldmind.research.app.features.field.presentation.components.applyScreenCaptureProtection
import fieldmind.research.app.features.field.presentation.navigation.FieldMindApp
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.data.model.AppSettings
import fieldmind.research.app.shared.presentation.viewmodel.ThemeViewModel
import fieldmind.research.app.ui.theme.RhythmTheme

class MainActivity : FragmentActivity() {
    companion object {
        /**
         * Kept for legacy, currently inactive Rhythm notification/widget code that still compiles
         * while FieldMind owns the active app surface. The FieldMind shell ignores this extra.
         */
        const val EXTRA_OPEN_PLAYER = "extra_open_player"
        const val EXTRA_FIELDMIND_DESTINATION = "fieldmind_destination"

        /**
         * Legacy request code used by inactive equalizer screens that are no longer reachable
         * from the FieldMind navigation graph, but still compile until the music modules are
         * fully decommissioned.
         */
        const val DISPLAY_AUDIO_EFFECT_CONTROL_PANEL_REQUEST = 1001
    }

    private val themeViewModel: ThemeViewModel by viewModels()
    private val fieldMindViewModel: FieldMindViewModel by viewModels()
    private lateinit var appSettings: AppSettings
    private var requestedFieldMindDestination = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appSettings = AppSettings.getInstance(applicationContext)
        handleSharedSource(intent)
        requestedFieldMindDestination.value = intent?.getStringExtra(EXTRA_FIELDMIND_DESTINATION)

        setContent {
            val useSystemTheme by themeViewModel.useSystemTheme.collectAsState()
            val darkMode by themeViewModel.darkMode.collectAsState()
            val amoledTheme by appSettings.amoledTheme.collectAsState()
            val useDynamicColors by themeViewModel.useDynamicColors.collectAsState()
            val customColorScheme by appSettings.customColorScheme.collectAsState()
            val customFont by appSettings.customFont.collectAsState()
            val fontSource by appSettings.fontSource.collectAsState()
            val customFontPath by appSettings.customFontPath.collectAsState()
            val colorSource by appSettings.colorSource.collectAsState()
            val extractedAlbumColors by appSettings.extractedAlbumColors.collectAsState()
            val fieldThemeMode by fieldMindViewModel.fieldSettings.themeMode.collectAsState()
            val isDarkTheme = when (fieldThemeMode) {
                "Light" -> false
                "Dark" -> true
                else -> if (useSystemTheme) isSystemInDarkTheme() else darkMode
            }

            // FieldMind owns the active surface: apply FieldMind's brand palette directly
            // without wrapping in RhythmTheme to avoid color conflicts
            val fieldDynamicColor by fieldMindViewModel.fieldSettings.dynamicColorEnabled.collectAsState()
            val fieldEntityColors by fieldMindViewModel.fieldSettings.entityColors.collectAsState()
            val screenCaptureProtection by fieldMindViewModel.fieldSettings.screenCaptureProtectionEnabled.collectAsState()

            LaunchedEffect(screenCaptureProtection) {
                applyScreenCaptureProtection(window, screenCaptureProtection)
            }

            FieldMindTheme(darkTheme = isDarkTheme, dynamicColor = fieldDynamicColor, entityColorOverrides = fieldEntityColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FieldMindApp(appSettings = appSettings, viewModel = fieldMindViewModel, requestedDestination = requestedFieldMindDestination.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedSource(intent)
        requestedFieldMindDestination.value = intent.getStringExtra(EXTRA_FIELDMIND_DESTINATION)
    }

    private fun handleSharedSource(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val title = intent.getStringExtra(Intent.EXTRA_TITLE)
                    ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                    ?: text.lineSequence().firstOrNull()?.take(80)
                    ?: "Shared source"
                val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                stream?.let { runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
                val mime = intent.type.orEmpty()
                fieldMindViewModel.addSource(
                    type = when {
                        mime.startsWith("image/") -> "Image"
                        mime == "application/pdf" -> "PDF"
                        stream != null -> "Local document"
                        text.startsWith("http", ignoreCase = true) -> "Website"
                        else -> "Note"
                    },
                    title = title.ifBlank { "Shared source" },
                    author = "",
                    link = if (stream == null && text.startsWith("http", ignoreCase = true)) text else "",
                    summary = if (stream == null && !text.startsWith("http", ignoreCase = true)) text else "Shared into FieldMind from another app.",
                    taught = "",
                    reliability = 3,
                    fileUri = stream?.toString().orEmpty(),
                    readingStatus = "Not started"
                )
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val streams = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                streams.forEachIndexed { index, uri ->
                    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    fieldMindViewModel.addSource(
                        type = if (intent.type.orEmpty().startsWith("image/")) "Image" else "Local document",
                        title = "Shared source ${index + 1}",
                        author = "",
                        link = "",
                        summary = "Shared into FieldMind from another app.",
                        taught = "",
                        reliability = 3,
                        fileUri = uri.toString(),
                        readingStatus = "Not started"
                    )
                }
            }
        }
    }

}
