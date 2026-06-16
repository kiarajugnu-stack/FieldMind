package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for [DevWeatherTestPanel] composable and
 * [rememberAssetImage] helper.
 */
class WeatherWidgetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── DevWeatherTestPanel ──────────────────────────────────────

    @Test
    fun devPanel_showsHeaderAndWeatherChips() {
        composeTestRule.setContent {
            FieldMindTheme(darkTheme = false, dynamicColor = false) {
                DevWeatherTestPanel(
                    testCode = null,
                    testNight = false,
                    onCodeChange = {},
                    onNightChange = {}
                )
            }
        }

        // Header text
        composeTestRule.onNodeWithText("Dev: Test conditions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Using live data").assertIsDisplayed()

        // Weather code chips — check some labels are present
        composeTestRule.onNodeWithText("Clear").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rain").assertIsDisplayed()
        composeTestRule.onNodeWithText("Snow").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thunderstorm").assertIsDisplayed()
    }

    @Test
    fun devPanel_selectingChip_showsOverrideLabel() {
        val testCode = mutableStateOf<Int?>(null)

        composeTestRule.setContent {
            FieldMindTheme(darkTheme = false, dynamicColor = false) {
                DevWeatherTestPanel(
                    testCode = testCode.value,
                    testNight = false,
                    onCodeChange = { testCode.value = it },
                    onNightChange = {}
                )
            }
        }

        // Tap the "Fog" chip (code 45)
        composeTestRule.onNodeWithText("Fog").performClick()

        // Should show override label
        composeTestRule.onNodeWithText("Override: Fog").assertIsDisplayed()
    }

    @Test
    fun devPanel_selectingChipTwice_resetsToLive() {
        val testCode = mutableStateOf<Int?>(null)

        composeTestRule.setContent {
            FieldMindTheme(darkTheme = false, dynamicColor = false) {
                DevWeatherTestPanel(
                    testCode = testCode.value,
                    testNight = false,
                    onCodeChange = { testCode.value = it },
                    onNightChange = {}
                )
            }
        }

        // Tap "Rain" chip (code 61) — select it
        composeTestRule.onNodeWithText("Rain").performClick()
        composeTestRule.onNodeWithText("Override: Rain").assertIsDisplayed()

        // Tap "Rain" again — deselect it
        composeTestRule.onNodeWithText("Rain").performClick()
        composeTestRule.onNodeWithText("Using live data").assertIsDisplayed()
    }

    @Test
    fun devPanel_nightModeToggle_showsNightOverride() {
        val testCode = mutableStateOf<Int?>(null)
        val testNight = mutableStateOf(false)

        composeTestRule.setContent {
            FieldMindTheme(darkTheme = false, dynamicColor = false) {
                DevWeatherTestPanel(
                    testCode = testCode.value,
                    testNight = testNight.value,
                    onCodeChange = { testCode.value = it },
                    onNightChange = { testNight.value = it }
                )
            }
        }

        // Tap "Night mode" filter chip
        composeTestRule.onNodeWithText("Night mode").performClick()
        composeTestRule.onNodeWithText("Override: Night Clear").assertIsDisplayed()
    }

    @Test
    fun devPanel_resetButton_clearsSelection() {
        val testCode = mutableStateOf<Int?>(61)
        val testNight = mutableStateOf(true)

        composeTestRule.setContent {
            FieldMindTheme(darkTheme = false, dynamicColor = false) {
                DevWeatherTestPanel(
                    testCode = testCode.value,
                    testNight = testNight.value,
                    onCodeChange = { testCode.value = it },
                    onNightChange = { testNight.value = it }
                )
            }
        }

        // Tap "Reset" button
        composeTestRule.onNodeWithText("Reset").performClick()
        composeTestRule.onNodeWithText("Using live data").assertIsDisplayed()
    }

    @Test
    fun devPanel_selectingNightThenRain_showsNightRain() {
        val testCode = mutableStateOf<Int?>(null)
        val testNight = mutableStateOf(false)

        composeTestRule.setContent {
            FieldMindTheme(darkTheme = false, dynamicColor = false) {
                DevWeatherTestPanel(
                    testCode = testCode.value,
                    testNight = testNight.value,
                    onCodeChange = { testCode.value = it },
                    onNightChange = { testNight.value = it }
                )
            }
        }

        // Enable night mode first
        composeTestRule.onNodeWithText("Night mode").performClick()
        composeTestRule.onNodeWithText("Override: Night Clear").assertIsDisplayed()

        // Then tap a different weather code
        composeTestRule.onNodeWithText("Thunderstorm").performClick()
        composeTestRule.onNodeWithText("Night Storm").assertIsDisplayed()
    }

    // ── rememberAssetImage ─────────────────────────────────

    @Test
    fun rememberAssetImage_withNullPath_returnsNull() {
        var bitmap: Any? = null
        composeTestRule.setContent {
            FieldMindTheme(darkTheme = false, dynamicColor = false) {
                bitmap = rememberAssetImage(null)
            }
        }
        assertNull(bitmap)
    }

    @Test
    fun rememberAssetImage_withInvalidPath_returnsNull() {
        var bitmap: Any? = null
        composeTestRule.setContent {
            FieldMindTheme(darkTheme = false, dynamicColor = false) {
                bitmap = rememberAssetImage("nonexistent/file.png")
            }
        }
        assertNull(bitmap)
    }
}
