package fieldmind.research.app.features.field.presentation.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * FieldMind brand theme.
 *
 * Provides a dedicated "field notebook" palette (forest green + warm ochre) as the
 * default brand, with an opt-in Material You / dynamic-color path that auto-adapts to the
 * system wallpaper and light/dark setting. Layered on top is [FieldMindColors], a set of
 * semantic colors for research entity types and confidence levels used consistently across
 * cards, chips, charts, and the knowledge graph.
 */

// ── Brand palette: light ──────────────────────────────────────────────
private val BrandPrimaryLight = Color(0xFF1F6B4C)
private val BrandLight = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F2C8),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4F6353),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8D4),
    onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF2C1700),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FBF3),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF6FBF3),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707974),
    outlineVariant = Color(0xFFC0C9C0),
    inverseSurface = Color(0xFF2D322C),
    inverseOnSurface = Color(0xFFEEF2EA),
    inversePrimary = Color(0xFF8DD5A9),
    surfaceDim = Color(0xFFD7DBD3),
    surfaceBright = Color(0xFFF6FBF3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5ED),
    surfaceContainer = Color(0xFFEAF0E7),
    surfaceContainerHigh = Color(0xFFE4EAE1),
    surfaceContainerHighest = Color(0xFFDEE4DB)
)

// ── Brand palette: dark ───────────────────────────────────────────────
private val BrandDark = darkColorScheme(
    primary = Color(0xFF8DD5A9),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005230),
    onPrimaryContainer = Color(0xFFA8F2C8),
    secondary = Color(0xFFB6CCB9),
    onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD2E8D4),
    tertiary = Color(0xFFFFB951),
    onTertiary = Color(0xFF492C00),
    tertiaryContainer = Color(0xFF694200),
    onTertiaryContainer = Color(0xFFFFDDB0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10140F),
    onBackground = Color(0xFFE0E4DC),
    surface = Color(0xFF10140F),
    onSurface = Color(0xFFE0E4DC),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C0),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF404943),
    inverseSurface = Color(0xFFE0E4DC),
    inverseOnSurface = Color(0xFF2D322C),
    inversePrimary = BrandPrimaryLight,
    surfaceDim = Color(0xFF10140F),
    surfaceBright = Color(0xFF363A34),
    surfaceContainerLowest = Color(0xFF0B0F0A),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1C211B),
    surfaceContainerHigh = Color(0xFF262B25),
    surfaceContainerHighest = Color(0xFF313630)
)

/**
 * Semantic, research-specific colors layered on top of the Material color scheme.
 * Each research entity type and confidence level has a stable accent color that drives
 * badges, chips, chart series, and knowledge-graph edges.
 */
data class FieldMindColors(
    val isDark: Boolean,
    val observation: Color,
    val question: Color,
    val hypothesis: Color,
    val project: Color,
    val source: Color,
    val data: Color,
    val report: Color,
    val flashcard: Color,
    val confidenceSure: Color,
    val confidenceGuess: Color,
    val confidenceVerify: Color,
    val positive: Color,
    val warning: Color,
    val info: Color,
    /** Distinct, harmonious series colors for charts and per-category accents. */
    val categorical: List<Color>
) {
    /** Stable distinct accent for an arbitrary label (e.g. an observation category). */
    fun categoryColor(label: String): Color {
        if (label.isBlank()) return info
        val idx = (Math.floorMod(label.trim().lowercase().hashCode(), categorical.size))
        return categorical[idx]
    }

    /** Accent color for an entity kind keyword (case-insensitive). */
    fun accentFor(kind: String): Color = when (kind.trim().lowercase()) {
        "observation", "observations", "observe" -> observation
        "question", "questions" -> question
        "hypothesis", "hypotheses" -> hypothesis
        "project", "projects" -> project
        "source", "sources", "read", "reading", "library", "note", "notes" -> source
        "data", "data record", "datarecord" -> data
        "report", "reports" -> report
        "flashcard", "flashcards", "card", "cards" -> flashcard
        else -> info
    }

    /** Accent color for a confidence label. */
    fun confidenceColor(level: String): Color = when (level.trim().lowercase()) {
        "sure", "high", "confirmed" -> confidenceSure
        "guess", "low", "maybe" -> confidenceGuess
        else -> confidenceVerify
    }
}

private val LightFieldMindColors = FieldMindColors(
    isDark = false,
    observation = Color(0xFF2E7D32),
    question = Color(0xFF1565C0),
    hypothesis = Color(0xFF8B5000),
    project = Color(0xFF00695C),
    source = Color(0xFF5E35B1),
    data = Color(0xFF006D7A),
    report = Color(0xFFA1531F),
    flashcard = Color(0xFFAD1457),
    confidenceSure = Color(0xFF2E7D32),
    confidenceGuess = Color(0xFF8B5000),
    confidenceVerify = Color(0xFFC62828),
    positive = Color(0xFF2E7D32),
    warning = Color(0xFF8B5000),
    info = Color(0xFF455A64),
    categorical = listOf(
        Color(0xFF2E7D32), // green
        Color(0xFF1565C0), // blue
        Color(0xFF8B5000), // amber
        Color(0xFF5E35B1), // violet
        Color(0xFF006D7A), // teal
        Color(0xFFAD1457), // magenta
        Color(0xFF00695C), // deep teal
        Color(0xFFD84315), // burnt orange
        Color(0xFF455A64), // slate
        Color(0xFF6D4C41)  // brown
    )
)

private val DarkFieldMindColors = FieldMindColors(
    isDark = true,
    observation = Color(0xFFA5D6A7),
    question = Color(0xFF90CAF9),
    hypothesis = Color(0xFFFFCC80),
    project = Color(0xFF80CBC4),
    source = Color(0xFFB39DDB),
    data = Color(0xFF80DEEA),
    report = Color(0xFFFFB74D),
    flashcard = Color(0xFFF48FB1),
    confidenceSure = Color(0xFFA5D6A7),
    confidenceGuess = Color(0xFFFFCC80),
    confidenceVerify = Color(0xFFEF9A9A),
    positive = Color(0xFFA5D6A7),
    warning = Color(0xFFFFCC80),
    info = Color(0xFFB0BEC5),
    categorical = listOf(
        Color(0xFFA5D6A7), // green
        Color(0xFF90CAF9), // blue
        Color(0xFFFFCC80), // amber
        Color(0xFFB39DDB), // violet
        Color(0xFF80DEEA), // teal
        Color(0xFFF48FB1), // magenta
        Color(0xFF80CBC4), // deep teal
        Color(0xFFFFAB91), // burnt orange
        Color(0xFFB0BEC5), // slate
        Color(0xFFBCAAA4)  // brown
    )
)

val LocalFieldMindColors = staticCompositionLocalOf { LightFieldMindColors }

/**
 * Accessor object so callers can read semantic colors via `FieldMindTheme.colors`.
 */
object FieldMindTheme {
    val colors: FieldMindColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFieldMindColors.current
}

/**
 * Applies the FieldMind brand color scheme (or dynamic Material You colors) and provides
 * [FieldMindColors] to descendants. Typography and shapes are inherited from any outer
 * [MaterialTheme] so the rest of the app's font/shape choices are preserved.
 *
 * @param darkTheme follow the system/app dark setting so the theme auto-adapts.
 * @param dynamicColor when true and supported (Android 12+), use Material You wallpaper colors.
 */
@Composable
fun FieldMindTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> BrandDark
        else -> BrandLight
    }
    val semantic = if (darkTheme) DarkFieldMindColors else LightFieldMindColors

    CompositionLocalProvider(LocalFieldMindColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}
