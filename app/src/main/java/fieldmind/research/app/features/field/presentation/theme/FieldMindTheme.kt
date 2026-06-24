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
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

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
    background = Color(0xFFFAF9F7),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFAF9F7),
    onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFDEE3DB),
    onSurfaceVariant = Color(0xFF4F4846),
    outline = Color(0xFF8B7D75),
    outlineVariant = Color(0xFFD0C4BB),
    inverseSurface = Color(0xFF31302B),
    inverseOnSurface = Color(0xFFF2F0E8),
    inversePrimary = Color(0xFF8DD5A9),
    surfaceDim = Color(0xFFDCD8D0),
    surfaceBright = Color(0xFFFAF9F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F0E8),
    surfaceContainer = Color(0xFFEDE9E0),
    surfaceContainerHigh = Color(0xFFE7E3DA),
    surfaceContainerHighest = Color(0xFFE1DDD3),
    scrim = Color(0xFF000000)
)

// ── Brand palette: dark ───────────────────────────────────────────────
private val BrandDark = darkColorScheme(
    primary = Color(0xFF8DD5A9),
    onPrimary = Color(0xFF00391E),
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
    background = Color(0xFF0F1409),
    onBackground = Color(0xFFE2E7DF),
    surface = Color(0xFF0F1409),
    onSurface = Color(0xFFE2E7DF),
    surfaceVariant = Color(0xFF4A4844),
    onSurfaceVariant = Color(0xFFC7C0B8),
    outline = Color(0xFF91897F),
    outlineVariant = Color(0xFF4A4844),
    inverseSurface = Color(0xFFE2E7DF),
    inverseOnSurface = Color(0xFF2D322C),
    inversePrimary = BrandPrimaryLight,
    surfaceDim = Color(0xFF0F1409),
    surfaceBright = Color(0xFF353A33),
    surfaceContainerLowest = Color(0xFF0A0E08),
    surfaceContainerLow = Color(0xFF171C15),
    surfaceContainer = Color(0xFF1B2019),
    surfaceContainerHigh = Color(0xFF252A23),
    surfaceContainerHighest = Color(0xFF2F342D),
    scrim = Color(0xFF000000)
)

/**
 * Semantic, research-specific colors layered on top of the Material color scheme.
 * Each research entity type and confidence level has a stable accent color that drives
 * badges, chips, chart series, and knowledge-graph edges.
 */
data class FieldMindColors(
    val isDark: Boolean,
    // ── Research entity colors ──
    val observation: Color,
    val question: Color,
    val hypothesis: Color,
    val project: Color,
    val source: Color,
    val note: Color,
    val task: Color,
    val folder: Color,
    val species: Color,
    val data: Color,
    val report: Color,
    val flashcard: Color,
    // ── Semantic state colors ──
    val positive: Color,
    val warning: Color,
    val info: Color,
    // ── Confidence level colors ──
    val confidenceSure: Color,
    val confidenceGuess: Color,
    val confidenceVerify: Color,
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
        "source", "sources", "read", "reading", "library" -> source
        "note", "notes" -> note
        "task", "tasks" -> task
        "folder", "folders" -> folder
        "species" -> species
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

    // ── isDark is provided by the constructor parameter above ──

    /**
     * Apply per-category color overrides from Settings.
     * Any entity type in [overrides] replaces its default color.
     * Entity types not in [overrides] keep their current default value.
     */
    fun applyOverrides(overrides: Map<String, Long>): FieldMindColors {
        if (overrides.isEmpty()) return this
        return copy(
            observation = overrides["observation"]?.let { Color(it) } ?: observation,
            question = overrides["question"]?.let { Color(it) } ?: question,
            hypothesis = overrides["hypothesis"]?.let { Color(it) } ?: hypothesis,
            project = overrides["project"]?.let { Color(it) } ?: project,
            source = overrides["source"]?.let { Color(it) } ?: source,
            note = overrides["note"]?.let { Color(it) } ?: note,
            task = overrides["task"]?.let { Color(it) } ?: task,
            folder = overrides["folder"]?.let { Color(it) } ?: folder,
            species = overrides["species"]?.let { Color(it) } ?: species,
            data = overrides["data"]?.let { Color(it) } ?: data,
            report = overrides["report"]?.let { Color(it) } ?: report,
            flashcard = overrides["flashcard"]?.let { Color(it) } ?: flashcard,
            positive = overrides["positive"]?.let { Color(it) } ?: positive,
            warning = overrides["warning"]?.let { Color(it) } ?: warning,
            info = overrides["info"]?.let { Color(it) } ?: info,
            confidenceSure = overrides["confidenceSure"]?.let { Color(it) } ?: confidenceSure,
            confidenceGuess = overrides["confidenceGuess"]?.let { Color(it) } ?: confidenceGuess,
            confidenceVerify = overrides["confidenceVerify"]?.let { Color(it) } ?: confidenceVerify
        )
    }
}

private val LightFieldMindColors = FieldMindColors(
    isDark = false,
    // Entity colors
    observation = Color(0xFF2E7D32),
    question = Color(0xFF1565C0),
    hypothesis = Color(0xFF8B5000),
    project = Color(0xFF00695C),
    source = Color(0xFF5E35B1),
    note = Color(0xFF8E24AA),
    task = Color(0xFF00897B),
    folder = Color(0xFF6D4C41),
    species = Color(0xFF43A047),
    data = Color(0xFF006D7A),
    report = Color(0xFFA1531F),
    flashcard = Color(0xFFE91E63),
    // State colors (distinct from entity colors)
    positive = Color(0xFF00A86B),
    warning = Color(0xFFE67E22),
    info = Color(0xFF546E7A),
    // Confidence colors (distinct from entity and state)
    confidenceSure = Color(0xFF27AE60),
    confidenceGuess = Color(0xFFF39C12),
    confidenceVerify = Color(0xFFE53935),
    categorical = listOf(
        Color(0xFF2E7D32), // observation green
        Color(0xFF1565C0), // question blue
        Color(0xFF8B5000), // hypothesis amber
        Color(0xFF5E35B1), // source violet
        Color(0xFF8E24AA), // note purple
        Color(0xFF00897B), // task teal
        Color(0xFF6D4C41), // folder brown
        Color(0xFF43A047), // species green
        Color(0xFF006D7A), // data teal
        Color(0xFFE91E63), // flashcard pink
        Color(0xFF00A86B), // positive jade
        Color(0xFFE67E22), // warning orange
    )
)

private val DarkFieldMindColors = FieldMindColors(
    isDark = true,
    // Entity colors
    observation = Color(0xFFA5D6A7),
    question = Color(0xFF90CAF9),
    hypothesis = Color(0xFFFFCC80),
    project = Color(0xFF80CBC4),
    source = Color(0xFFB39DDB),
    note = Color(0xFFCE93D8),
    task = Color(0xFF4DB6AC),
    folder = Color(0xFFBCAAA4),
    species = Color(0xFF81C784),
    data = Color(0xFF80DEEA),
    report = Color(0xFFFFB74D),
    flashcard = Color(0xFFF48FB1),
    // State colors (distinct from entity colors)
    positive = Color(0xFF69F0AE),
    warning = Color(0xFFFFB74D),
    info = Color(0xFFB0BEC5),
    // Confidence colors (distinct from entity and state)
    confidenceSure = Color(0xFF81C784),
    confidenceGuess = Color(0xFFFFD54F),
    confidenceVerify = Color(0xFFEF9A9A),
    categorical = listOf(
        Color(0xFFA5D6A7), // observation green
        Color(0xFF90CAF9), // question blue
        Color(0xFFFFCC80), // hypothesis amber
        Color(0xFFB39DDB), // source violet
        Color(0xFFCE93D8), // note purple
        Color(0xFF4DB6AC), // task teal
        Color(0xFFBCAAA4), // folder brown
        Color(0xFF81C784), // species green
        Color(0xFF80DEEA), // data teal
        Color(0xFFF48FB1), // flashcard pink
        Color(0xFF69F0AE), // positive jade
        Color(0xFFFFB74D), // warning orange
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
/**
 * Default entity colors map used for the color picker UI reset functionality.
 * Matches [LightFieldMindColors] values.
 */
val DEFAULT_ENTITY_COLORS: Map<String, Long> = mapOf(
    "observation" to 0xFF2E7D32,
    "question" to 0xFF1565C0,
    "hypothesis" to 0xFF8B5000,
    "project" to 0xFF00695C,
    "source" to 0xFF5E35B1,
    "note" to 0xFF8E24AA,
    "task" to 0xFF00897B,
    "folder" to 0xFF6D4C41,
    "species" to 0xFF43A047,
    "data" to 0xFF006D7A,
    "report" to 0xFFA1531F,
    "flashcard" to 0xFFE91E63,
    "positive" to 0xFF00A86B,
    "warning" to 0xFFE67E22,
    "info" to 0xFF546E7A,
    "confidenceSure" to 0xFF27AE60,
    "confidenceGuess" to 0xFFF39C12,
    "confidenceVerify" to 0xFFE53935
)

/** Display labels for each entity color key. */
val ENTITY_COLOR_LABELS: Map<String, String> = mapOf(
    "observation" to "Observation",
    "question" to "Question",
    "hypothesis" to "Hypothesis",
    "project" to "Project",
    "source" to "Source",
    "note" to "Note",
    "task" to "Task",
    "folder" to "Folder",
    "species" to "Species",
    "data" to "Data",
    "report" to "Report",
    "flashcard" to "Flashcard",
    "positive" to "Positive (Success)",
    "warning" to "Warning",
    "info" to "Info",
    "confidenceSure" to "Confidence: Sure",
    "confidenceGuess" to "Confidence: Guess",
    "confidenceVerify" to "Confidence: Verify"
)

/** Icons for each entity color key. */
val ENTITY_COLOR_ICONS: Map<String, MaterialSymbolIcon> = mapOf(
    "observation" to MaterialSymbolIcon("visibility"),
    "question" to MaterialSymbolIcon("help"),
    "hypothesis" to MaterialSymbolIcon("lightbulb"),
    "project" to MaterialSymbolIcon("science"),
    "source" to MaterialSymbolIcon("menu_book"),
    "note" to MaterialSymbolIcon("edit_note"),
    "task" to MaterialSymbolIcon("checklist"),
    "folder" to MaterialSymbolIcon("folder"),
    "species" to MaterialSymbolIcon("pets"),
    "data" to MaterialSymbolIcon("bar_chart"),
    "report" to MaterialSymbolIcon("description"),
    "flashcard" to MaterialSymbolIcon("style"),
    "positive" to MaterialSymbolIcon("check_circle"),
    "warning" to MaterialSymbolIcon("warning"),
    "info" to MaterialSymbolIcon("info"),
    "confidenceSure" to MaterialSymbolIcon("thumb_up"),
    "confidenceGuess" to MaterialSymbolIcon("help_outline"),
    "confidenceVerify" to MaterialSymbolIcon("verified")
)

/**
 * Applies the FieldMind brand color scheme (or dynamic Material You colors) and provides
 * [FieldMindColors] to descendants. Typography and shapes are inherited from any outer
 * [MaterialTheme] so the rest of the app's font/shape choices are preserved.
 *
 * @param darkTheme follow the system/app dark setting so the theme auto-adapts.
 * @param dynamicColor when true and supported (Android 12+), use Material You wallpaper colors.
 * @param entityColorOverrides per-category color overrides from Settings (entity type → hex Long).
 */
@Composable
fun FieldMindTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    entityColorOverrides: Map<String, Long> = emptyMap(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> BrandDark
        else -> BrandLight
    }
    val semantic = (if (darkTheme) DarkFieldMindColors else LightFieldMindColors)
        .applyOverrides(entityColorOverrides)

    CompositionLocalProvider(LocalFieldMindColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}

// ── Top-level opacity helpers ──
// These were originally member extension functions inside FieldMindColors.
// Moved top-level so they can be called from any file without dispatch-receiver scope.

/** Background tint for cards, chips – auto-adapts alpha to dark mode. */
fun Color.cardBg(isDark: Boolean): Color = copy(alpha = if (isDark) 0.22f else 0.14f)
/** Subtle border for selected state. */
fun Color.cardBorder(): Color = copy(alpha = 0.40f)
/** Muted text / secondary decoration. */
fun Color.muted(): Color = copy(alpha = 0.60f)

// ── Re-export for convenient single import ──
// import fieldmind.research.app.features.field.presentation.theme.MaterialSymbolIcon
// (MaterialSymbolIcon is already in components/icons package, not re-exported here)
