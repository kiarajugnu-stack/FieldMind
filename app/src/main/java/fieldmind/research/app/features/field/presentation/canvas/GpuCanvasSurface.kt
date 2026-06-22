package fieldmind.research.app.features.field.presentation.canvas

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Compose wrapper around a [GLSurfaceView] that renders the infinite canvas
 * dot-grid background and block selection highlights via [GpuCanvasRenderer].
 *
 * Usage:
 * ```kotlin
 * GpuCanvasSurface(
 *     canvasState = canvasState,
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 *
 * The surface is transparent (clear color = (0,0,0,0)) so Compose UI
 * drawn on top of it shows through.
 *
 * @param canvasState shared state driving zoom/pan
 * @param selectedBlockRects block rectangles to highlight on the GPU
 * @param modifier standard Compose modifier
 */
@Composable
fun GpuCanvasSurface(
    canvasState: CanvasState,
    selectedBlockRects: List<GpuCanvasRenderer.BlockRect>,
    modifier: Modifier = Modifier
) {
    val renderer = remember { GpuCanvasRenderer() }

    AndroidView(
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2) // OpenGL ES 2.0

                // Transparent surface so Compose draws through
                setEGLConfigChooser(8, 8, 8, 8, 0, 0) // RGBA8888, no depth/stencil
                holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)

                setRenderer(renderer)

                // Only re-draw when explicitly requested (not continuously)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
        },
        modifier = modifier,
        update = { glSurfaceView ->
            // Push latest state to renderer each composition
            renderer.zoom = canvasState.zoom
            renderer.panX = canvasState.panX
            renderer.panY = canvasState.panY
            renderer.selectedBlockRects = selectedBlockRects

            // Request a re-draw
            glSurfaceView.requestRender()
        }
    )
}
