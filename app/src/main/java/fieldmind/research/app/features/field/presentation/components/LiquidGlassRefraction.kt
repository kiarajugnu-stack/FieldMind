package fieldmind.research.app.features.field.presentation.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.isActive

/**
 * AGSL (Android Graphics Shading Language) shader that creates a liquid glass
 * refraction + specular highlight effect.
 *
 * Features:
 * - Multi-layer sine-based UV displacement for organic liquid motion
 * - Dual specular highlight spots (top-left primary, mid-right secondary)
 * - Fresnel edge glow for depth at borders
 *
 * The shader is applied via [RenderEffect.createRuntimeShaderEffect] which
 * requires API 33+ (Android 13). On older devices this modifier is a no-op.
 */
private val LIQUID_GLASS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float time;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / size;

    // ── Multi-layer liquid displacement ──
    // Two sine-based displacement layers at different frequencies and speeds
    // create an organic, fluid-like refraction pattern.
    float2 disp = float2(
        sin(uv.y * 35.0 + time * 0.9 + uv.x * 6.0) * 0.008 +
        sin(uv.y * 18.0 + time * 0.5 + uv.x * 10.0) * 0.004,
        cos(uv.x * 28.0 + time * 0.7 + uv.y * 5.0) * 0.008 +
        cos(uv.x * 22.0 + time * 0.6 + uv.y * 8.0) * 0.004
    );

    // Sample with clamped displacement to prevent black edge artifacts
    float2 refracted = clamp(fragCoord + disp * size, 0.0, size);
    half4 color = content.eval(refracted);

    // ── Specular highlights ──
    // Primary light spot (top-left): tight, bright
    float2 lightPos1 = float2(0.25, 0.15);
    float d1 = distance(uv, lightPos1);
    float spec1 = exp(-d1 * d1 * 300.0) * 0.20;

    // Secondary light spot (mid-right): wider, subtler
    float2 lightPos2 = float2(0.72, 0.28);
    float d2 = distance(uv, lightPos2);
    float spec2 = exp(-d2 * d2 * 180.0) * 0.10;

    // ── Fresnel edge glow ──
    // Brighter near edges to simulate increased reflection at glancing angles.
    float2 edgeDist = abs(uv * 2.0 - 1.0);
    float fresnel = pow(1.0 - max(edgeDist.x, edgeDist.y), 2.5) * 0.04;

    color.rgb += spec1 + spec2 + fresnel;
    return color;
}
""".trimIndent()

/**
 * Applies a GPU-based liquid glass refraction effect to the composable.
 *
 * The effect uses [RuntimeShader] and [RenderEffect.createRuntimeShaderEffect]
 * to displace pixels with a multi-layer sine-based distortion, add specular
 * highlights, and apply Fresnel edge glow — creating the illusion of looking
 * through liquid glass.
 *
 * **Requires API 33+ (Android 13).** On older API levels this is a no-op.
 *
 * For best results, this modifier should wrap a composable that already has
 * backdrop blur (e.g. via Haze's `hazeChild`). The refraction processes the
 * blurred result, creating a premium liquid-glass aesthetic.
 */
@Composable
fun Modifier.liquidGlassRefraction(): Modifier {
    // RuntimeShader requires API 33+ (Android 13)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this

    val shader = remember {
        try {
            RuntimeShader(LIQUID_GLASS_AGSL)
        } catch (e: Exception) {
            // If AGSL compilation fails (unsupported driver, syntax error, etc.)
            // return this as a no-op rather than crashing the app.
            null
        }
    }
    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    // Update size uniform when the element is laid out
    LaunchedEffect(sizePx) {
        val s = shader ?: return@LaunchedEffect
        if (sizePx.width > 0 && sizePx.height > 0) {
            s.setFloatUniform("size", sizePx.width.toFloat(), sizePx.height.toFloat())
        }
    }

    // Animate time uniform every frame for continuous liquid motion
    LaunchedEffect(Unit) {
        val s = shader ?: return@LaunchedEffect
        while (isActive) {
            withFrameMillis { millis ->
                s.setFloatUniform("time", millis / 1000f)
            }
        }
    }

    // Create the RenderEffect once (null-safe — shader may be null if AGSL init failed)
    val renderEffect = remember {
        shader?.let { RenderEffect.createRuntimeShaderEffect(it, "content") }
    }

    return this
        .onSizeChanged { sizePx = it }
        .graphicsLayer {
            this.renderEffect = renderEffect
        }
}
