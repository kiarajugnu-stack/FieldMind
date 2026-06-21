package fieldmind.research.app.features.field.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Watermark utility for FieldMind captured images.
 *
 * Draws a semi-transparent timestamp overlay at the bottom-right of the image
 * showing the capture date and time with seconds (e.g. "2026-06-19 14:32:07").
 *
 * The watermark is burned into the image pixels so it survives sharing, export,
 * and any downstream processing.
 */
object FieldMindWatermark {

    private const val WATERMARK_ALPHA = 180       // 0-255, ~70% opaque
    private const val WATERMARK_BG_ALPHA = 100    // Background pill alpha
    private const val WATERMARK_MARGIN_DP = 8f     // Margin from edges (in px for mdpi)
    private const val WATERMARK_CORNER_RADIUS = 12f
    private const val WATERMARK_PADDING = 6f
    private const val MAX_BITMAP_DIMENSION = 4096  // Don't watermark ridiculously large images

    private val captureFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Add a timestamp watermark to the image at [sourceUri] and save the
     * watermarked version back to the same file path.
     *
     * @return true if the watermark was applied successfully, false otherwise.
     */
    fun applyWatermark(context: Context, sourceUri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return false
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return false

            // Don't watermark if image is too large (probable decode failure)
            if (original.width > MAX_BITMAP_DIMENSION || original.height > MAX_BITMAP_DIMENSION) {
                original.recycle()
                return false
            }

            // Create a mutable copy so we can draw on it
            val workingCopy = original.copy(Bitmap.Config.ARGB_8888, true)
            original.recycle()
            if (workingCopy == null) return false

            val canvas = Canvas(workingCopy)
            val density = context.resources.displayMetrics.density
            val margin = WATERMARK_MARGIN_DP * density
            val padding = WATERMARK_PADDING * density
            val cornerRadius = WATERMARK_CORNER_RADIUS * density

            val timestamp = captureFormatter.format(Date())

            // ── Text paint ──
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(WATERMARK_ALPHA, 255, 255, 255)
                textSize = 11f * density
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }

            // ── Measure text ──
            val textWidth = textPaint.measureText(timestamp)
            val textHeight = textPaint.descent() - textPaint.ascent()

            val bgWidth = textWidth + padding * 2
            val bgHeight = textHeight + padding * 2

            val bgLeft = workingCopy.width.toFloat() - bgWidth - margin
            val bgTop = workingCopy.height.toFloat() - bgHeight - margin
            val bgRight = bgLeft + bgWidth
            val bgBottom = bgTop + bgHeight

            // ── Background rounded rect ──
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(WATERMARK_BG_ALPHA, 0, 0, 0)
            }
            canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, cornerRadius, cornerRadius, bgPaint)

            // ── Timestamp text ──
            val textX = bgLeft + padding
            val textY = bgTop + padding - textPaint.ascent()
            canvas.drawText(timestamp, textX, textY, textPaint)

            // ── Save back to the same file ──
            val file = File(sourceUri.path ?: return false)
            val outputStream = FileOutputStream(file)
            workingCopy.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
            outputStream.flush()
            outputStream.close()
            workingCopy.recycle()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Apply watermark to an image at the given file path.
     * Convenience overload that wraps the file path into a Uri.
     */
    fun applyWatermark(context: Context, filePath: String): Boolean {
        return applyWatermark(context, Uri.fromFile(File(filePath)))
    }
}
