package chromahub.rhythm.app.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import chromahub.rhythm.app.features.field.presentation.navigation.FieldMindApp
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.viewmodel.ThemeViewModel
import chromahub.rhythm.app.ui.theme.RhythmTheme

class MainActivity : FragmentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()
    private val fieldMindViewModel: FieldMindViewModel by viewModels()
    private lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appSettings = AppSettings.getInstance(applicationContext)

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
            val isDarkTheme = if (useSystemTheme) isSystemInDarkTheme() else darkMode

            RhythmTheme(
                darkTheme = isDarkTheme,
                amoledTheme = amoledTheme && isDarkTheme,
                dynamicColor = useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                customColorScheme = customColorScheme,
                customFont = customFont,
                fontSource = fontSource,
                customFontPath = customFontPath,
                colorSource = colorSource,
                extractedAlbumColorsJson = extractedAlbumColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FieldMindApp(appSettings = appSettings, viewModel = fieldMindViewModel)
                }
            }
        }
    }
}
