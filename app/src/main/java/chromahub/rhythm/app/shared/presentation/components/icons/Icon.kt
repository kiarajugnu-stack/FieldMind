package fieldmind.research.app.shared.presentation.components.icons

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import fieldmind.research.app.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Scale factor applied to Material Symbols glyph text size relative to the icon container.
 * The variable font's optical-size axis (opsz) produces the cleanest strokes at the specified
 * point size. 0.875× of the container dp gives a small inset that avoids ascender clipping.
 */
private const val MaterialSymbolGlyphScale = 0.875f

/**
 * Thread-safe cache and fallback provider for Material Symbols variable-font [FontFamily] instances.
 *
 * Caches [FontFamily] objects keyed by the four variable-font axes so the TTF is not re-parsed
 * on every recomposition. Falls back to a statically loaded [Typeface] on older SDK versions
 * or on devices where `FontVariation` is known to be unreliable (e.g. Xiaomi/Redmi/Poco).
 */
private object FieldMindIconFontCache {
    private val cache = ConcurrentHashMap<String, FontFamily>()
    private var fallbackFontFamily: FontFamily? = null
    private val lock = Any()

    fun getFallbackFontFamily(context: Context): FontFamily {
        fallbackFontFamily?.let { return it }
        synchronized(lock) {
            fallbackFontFamily?.let { return it }
            val tf = try {
                ResourcesCompat.getFont(context, R.font.material_symbols_outlined)
            } catch (_: Throwable) {
                null
            }
            val ff = if (tf != null) {
                FontFamily(tf)
            } else {
                FontFamily(Font(R.font.material_symbols_outlined))
            }
            fallbackFontFamily = ff
            return ff
        }
    }

    fun getOrCreate(
        context: Context,
        filled: Boolean,
        weight: Int,
        grade: Float,
        opticalSize: Float
    ): FontFamily {
        val manufacturer = Build.MANUFACTURER.lowercase(java.util.Locale.US)
        val isXiaomiDevice = manufacturer.contains("xiaomi") ||
            manufacturer.contains("redmi") ||
            manufacturer.contains("poco")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isXiaomiDevice) {
            return getFallbackFontFamily(context)
        }

        val key = "$filled-$weight-$grade-$opticalSize"
        return cache.getOrPut(key) {
            try {
                FontFamily(
                    Font(
                        resId = R.font.material_symbols_outlined,
                        variationSettings = FontVariation.Settings(
                            FontVariation.weight(weight),
                            FontVariation.Setting("FILL", if (filled) 1f else 0f),
                            FontVariation.Setting("GRAD", grade),
                            FontVariation.Setting("opsz", opticalSize)
                        )
                    )
                )
            } catch (_: Throwable) {
                getFallbackFontFamily(context)
            }
        }
    }
}

/**
 * Representation of a Material Symbols icon.
 *
 * Material Symbols use variable-font technology supporting four dynamic axes:
 * - **FILL**: 0 (outlined) to 1 (filled)
 * - **wght** (Weight): 100 to 700
 * - **GRAD** (Grade): -25 to 200
 * - **opsz** (Optical Size): 20 to 48
 *
 * Icons are rendered using the bundled `material_symbols_outlined.ttf` variable font
 * via OpenType ligatures — the [name] field is the ligature name (e.g. `"play_arrow"`).
 */
data class MaterialSymbolIcon(
    /** The ligature name of the icon (e.g. `"play_arrow"`, `"settings"`). */
    val name: String,
    /** Whether the icon renders in filled style (FILL axis = 1). */
    val filled: Boolean = false,
    /** Default font weight for this icon (100–700, overridable at render time). */
    val defaultWeight: Int = 400
) {
    override fun toString(): String = name

    /** Returns a copy of this icon with `filled = true`. */
    fun filled(): MaterialSymbolIcon = copy(filled = true)

    /** Returns a copy of this icon with `filled = false`. */
    fun outlined(): MaterialSymbolIcon = copy(filled = false)
}

/**
 * Creates a cached [FontFamily] for Material Symbols with the given variable-axis settings.
 *
 * Results are cached so the TTF is re-parsed only when one of the axis values changes.
 */
@Composable
fun rememberSymbolsFontFamily(
    filled: Boolean = false,
    weight: Int = 400,
    grade: Float = 0f,
    opticalSize: Float = 24f
): FontFamily {
    val context = LocalContext.current
    return remember(filled, weight, grade, opticalSize) {
        FieldMindIconFontCache.getOrCreate(context, filled, weight, grade, opticalSize)
    }
}

/**
 * Renders a [MaterialSymbolIcon] using the Material Symbols variable font.
 *
 * This composable replaces `androidx.compose.material3.Icon` for Material Symbols,
 * rendering icon names as text glyphs via OpenType ligatures with full variable-axis
 * control (FILL, weight, grade, optical size).
 *
 * @param icon The [MaterialSymbolIcon] to render.
 * @param contentDescription Accessibility description for the icon.
 * @param modifier Standard [Modifier] applied to the icon container.
 * @param tint Color applied to the icon glyph.
 * @param size The display size of the icon (also sets the optical-size axis).
 * @param weight Font weight for the icon (100–700). Defaults to [MaterialSymbolIcon.defaultWeight].
 * @param grade Font grade for fine-tuning stroke weight (-25 to 200).
 */
@Composable
fun Icon(
    icon: MaterialSymbolIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    weight: Int = icon.defaultWeight,
    grade: Float = 0f
) {
    val fontFamily = rememberSymbolsFontFamily(
        filled = icon.filled,
        weight = weight,
        grade = grade,
        opticalSize = size.value * MaterialSymbolGlyphScale
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                this.role = Role.Image
            },
        contentAlignment = Alignment.Center
    ) {
        val glyphSize = size * MaterialSymbolGlyphScale
        androidx.compose.material3.Text(
            text = icon.name,
            fontFamily = fontFamily,
            fontSize = glyphSize.value.sp,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(
                lineHeight = glyphSize.value.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

// ──────────────────────────────────────────────────────
// Compatibility overloads
// ──────────────────────────────────────────────────────
// These allow call sites using standard Compose icon signatures to coexist with
// our custom MaterialSymbolIcon-based icon composable.

/**
 * Overload accepting [MaterialSymbolIcon] via the `imageVector` parameter name.
 * Preserves compatibility with existing code like:
 * ```kotlin
 * Icon(imageVector = FieldMindIcons.Play, contentDescription = "Play")
 * ```
 */
@Composable
fun Icon(
    imageVector: MaterialSymbolIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        icon = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/** Delegates standard [ImageVector]-based icons to Material 3. */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/** Delegates [Painter]-based icons to Material 3. */
@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    androidx.compose.material3.Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/** Delegates [ImageBitmap]-based icons to Material 3. */
@Composable
fun Icon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    androidx.compose.material3.Icon(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
