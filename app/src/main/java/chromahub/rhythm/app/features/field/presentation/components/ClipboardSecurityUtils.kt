package chromahub.rhythm.app.features.field.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Utility for securely handling clipboard operations with auto-cleanup.
 * Copies sensitive data to clipboard and automatically clears it after a configurable delay.
 *
 * Usage:
 * ```kotlin
 * ClipboardSecurityUtils.copySensitiveData(
 *     context = context,
 *     label = "Observation ID",
 *     data = "OBS-12345",
 *     delaySeconds = 30
 * )
 * ```
 */
object ClipboardSecurityUtils {
    
    /**
     * Copy sensitive data to clipboard with auto-cleanup notification.
     * The clipboard will be cleared after the specified delay.
     *
     * @param context Android context
     * @param label Label for the clipboard entry (e.g., "Observation ID")
     * @param data The sensitive data to copy
     * @param delaySeconds How many seconds before auto-clearing (default 30)
     */
    fun copySensitiveData(
        context: Context,
        label: String,
        data: String,
        delaySeconds: Int = 30
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, data)
        clipboard?.setPrimaryClip(clip)
        
        // Show toast notification
        Toast.makeText(
            context,
            "$label copied. Will be cleared in ${delaySeconds}s",
            Toast.LENGTH_SHORT
        ).show()
        
        // Schedule auto-cleanup
        GlobalScope.launch(Dispatchers.Main) {
            delay((delaySeconds * 1000).toLong())
            clipboard?.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
    
    /**
     * Immediately clear the clipboard.
     * Call this when leaving a sensitive screen.
     *
     * @param context Android context
     */
    fun clearClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("", ""))
    }
}
