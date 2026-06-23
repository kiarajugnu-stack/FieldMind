package fieldmind.research.app.features.field.presentation.canvas

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 renderer for the infinite canvas background.
 *
 * Renders in two layers (bottom to top):
 * 1. **Dot grid** — subtle grid of dots at 40px logical spacing.
 *    Handled by the GPU so it's cheap even at extreme zoom levels.
 * 2. **Block selection highlights** — rounded rectangles behind
 *    selected blocks, rendered as colored quads.
 *
 * Designed to be lightweight — all interactive content (blocks, text, UI)
 * is rendered in the Compose overlay above this GL surface.
 */
class GpuCanvasRenderer : GLSurfaceView.Renderer {

    // ── Camera / projection ──
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Viewport dimensions
    private var viewportWidth = 0
    private var viewportHeight = 0

    // ── State shared from CanvasState (set by InfiniteCanvas each frame) ──
    // @Volatile ensures visibility across UI thread (setter) and GL thread (reader)
    @Volatile var zoom: Float = 1f
    @Volatile var panX: Float = 0f
    @Volatile var panY: Float = 0f

    // ── Grid config ──
    private val gridSpacing = 40f   // logical px between dots
    private val dotRadius = 1.8f    // dot radius in logical px (at 1x zoom)

    // ── OpenGL handles ──
    private var gridProgram = 0
    private var rectProgram = 0
    private var gridVbo = IntArray(1)
    private var rectVbo = IntArray(1)

    // ── Vertex data ──
    private var gridVertexBuffer: FloatBuffer? = null
    private var gridVertexCount = 0
    private var needsGridUpdate = true

    // ── Block selection highlights ──
    data class BlockRect(
        val x: Float, val y: Float,
        val w: Float, val h: Float,
        val r: Float = 8f               // corner radius (logical px)
    )
    @Volatile var selectedBlockRects: List<BlockRect> = emptyList()
        set(value) {
            field = value
            needsBlockUpdate = true
        }
    private var needsBlockUpdate = true
    private var blockVertexBuffer: FloatBuffer? = null
    private var blockVertexCount = 0

    // ── Shader sources ──

    /** Minimal vertex shader: applies MVP transform to 2D positions. */
    private val vertexShaderSrc = """
        uniform mat4 uMVPMatrix;
        attribute vec4 aPosition;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            gl_PointSize = 3.0;
        }
    """.trimIndent()

    /** Fragment shader for grid dots — solid color with alpha. */
    private val gridFragmentSrc = """
        precision mediump float;
        uniform vec4 uColor;
        void main() {
            gl_FragColor = uColor;
        }
    """.trimIndent()

    /** Fragment shader for filled rectangles — solid color with alpha. */
    private val rectFragmentSrc = """
        precision mediump float;
        uniform vec4 uColor;
        void main() {
            gl_FragColor = uColor;
        }
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    //  Renderer lifecycle
    // ═══════════════════════════════════════════════════════════════

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f) // transparent background
        gridProgram = createProgram(vertexShaderSrc, gridFragmentSrc)
        rectProgram = createProgram(vertexShaderSrc, rectFragmentSrc)

        // Generate VBOs
        GLES20.glGenBuffers(1, gridVbo, 0)
        GLES20.glGenBuffers(1, rectVbo, 0)

        // Enable blending so the transparent background works
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        needsGridUpdate = true
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Build projection matrix
        // Orthographic: left=0, right=width, bottom=height, top=0 (y-down)
        Matrix.orthoM(projectionMatrix, 0,
            0f, viewportWidth.toFloat(),
            viewportHeight.toFloat(), 0f,
            -1f, 1f
        )

        // Build view matrix from camera state
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.translateM(viewMatrix, 0, panX, panY, 0f)
        Matrix.scaleM(viewMatrix, 0, zoom, zoom, 1f)

        // MVP = projection * view
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Render layers bottom→top
        renderDotGrid()
        renderBlockHighlights()
    }

    // ═══════════════════════════════════════════════════════════════
    //  Dot grid rendering
    // ═══════════════════════════════════════════════════════════════

    /**
     * Rebuilds the grid vertex buffer for the visible viewport.
     * Called when zoom/pan changes to ensure only visible dots are sent to the GPU.
     */
    fun invalidateGrid() {
        needsGridUpdate = true
    }

    private fun renderDotGrid() {
        // Grid rendering disabled for cleaner infinite canvas
    }

    /**
     * Calculates which grid dots are visible in the current viewport
     * and builds a vertex buffer from them.
     */
    private fun buildGridVertices() {
        // Calculate which grid cells are visible
        val invZoom = 1f / zoom
        val visibleLeft = (-panX) * invZoom - gridSpacing
        val visibleTop = (-panY) * invZoom - gridSpacing
        val visibleRight = (viewportWidth - panX) * invZoom + gridSpacing
        val visibleBottom = (viewportHeight - panY) * invZoom + gridSpacing

        // Find grid iteration bounds
        val startCol = (visibleLeft / gridSpacing).toInt() - 1
        val endCol = (visibleRight / gridSpacing).toInt() + 1
        val startRow = (visibleTop / gridSpacing).toInt() - 1
        val endRow = (visibleBottom / gridSpacing).toInt() + 1

        val cols = endCol - startCol + 1
        val rows = endRow - startRow + 1
        val totalDots = cols * rows

        // Cap at reasonable limit (100k dots max)
        if (totalDots > 100_000 || totalDots <= 0) {
            gridVertexCount = 0
            gridVertexBuffer = null
            return
        }

        val vertices = FloatArray(totalDots * 2)
        var idx = 0
        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                vertices[idx++] = col * gridSpacing
                vertices[idx++] = row * gridSpacing
            }
        }

        gridVertexCount = totalDots
        gridVertexBuffer = ByteBuffer
            .allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        gridVertexBuffer!!.position(0)
    }

    // ═══════════════════════════════════════════════════════════════
    //  Block selection highlight rendering
    // ═══════════════════════════════════════════════════════════════

    private fun renderBlockHighlights() {
        if (needsBlockUpdate) {
            buildBlockVertices()
            needsBlockUpdate = false
        }

        val buf = blockVertexBuffer ?: return
        if (blockVertexCount == 0) return

        GLES20.glUseProgram(rectProgram)
        val mvpHandle = GLES20.glGetUniformLocation(rectProgram, "uMVPMatrix")
        val colorHandle = GLES20.glGetUniformLocation(rectProgram, "uColor")
        val posHandle = GLES20.glGetAttribLocation(rectProgram, "aPosition")

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

        // Selection highlight: primary color at low alpha
        GLES20.glUniform4f(colorHandle, 0.5f, 0.7f, 0.9f, 0.12f)

        buf.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, rectVbo[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buf.capacity() * 4, buf, GLES20.GL_DYNAMIC_DRAW)

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, blockVertexCount)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    /**
     * Builds triangle pairs (2 triangles = 6 vertices per rect)
     * for each selected block rectangle.
     */
    private fun buildBlockVertices() {
        if (selectedBlockRects.isEmpty()) {
            blockVertexCount = 0
            blockVertexBuffer = null
            return
        }

        val verts = mutableListOf<Float>()
        selectedBlockRects.forEach { rect ->
            // Two triangles forming a rectangle
            val x = rect.x
            val y = rect.y
            val x2 = rect.x + rect.w
            val y2 = rect.y + rect.h
            // Triangle 1: top-left, top-right, bottom-left
            verts.addAll(listOf(x, y, x2, y, x, y2))
            // Triangle 2: top-right, bottom-right, bottom-left
            verts.addAll(listOf(x2, y, x2, y2, x, y2))
        }

        blockVertexCount = verts.size / 2
        val arr = verts.toFloatArray()
        blockVertexBuffer = ByteBuffer
            .allocateDirect(arr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(arr)
        blockVertexBuffer!!.position(0)
    }

    // ═══════════════════════════════════════════════════════════════
    //  Shader compilation helpers
    // ═══════════════════════════════════════════════════════════════

    private fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        if (vertexShader == 0 || fragmentShader == 0) return 0

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            android.util.Log.e("GpuCanvasRenderer", "Program link failed: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val typeName = if (type == GLES20.GL_VERTEX_SHADER) "vertex" else "fragment"
            android.util.Log.e("GpuCanvasRenderer", "$typeName shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
