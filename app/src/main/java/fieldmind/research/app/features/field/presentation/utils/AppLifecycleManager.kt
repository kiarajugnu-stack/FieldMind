package fieldmind.research.app.features.field.presentation.utils

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages app lifecycle events, inactivity tracking, and auto-lock timeout.
 * Tracks when the app goes to background and triggers lock when timeout expires.
 *
 * Usage:
 * ```kotlin
 * AppLifecycleManager.initialize(activity, context, settings)
 * AppLifecycleManager.onActivityPaused()  // Called from MainActivity.onPause()
 * AppLifecycleManager.onActivityResumed()  // Called from MainActivity.onResume()
 * ```
 */
object AppLifecycleManager {
    private var currentActivity: Activity? = null
    private var handler: Handler? = null
    private var lockTimeoutMs: Long = 0  // 0 = immediate, -1 = disabled
    private var lastInteractionTime: Long = System.currentTimeMillis()
    private var isAppInBackground = false
    
    private val _shouldShowLock = MutableStateFlow(false)
    val shouldShowLock: StateFlow<Boolean> = _shouldShowLock.asStateFlow()
    
    private val lockTimeoutRunnable = Runnable {
        triggerLock()
    }
    
    /**
     * Initialize the lifecycle manager. Call this once from MainActivity.onCreate().
     */
    fun initialize(
        activity: Activity,
        lockTimeoutSeconds: Int = 0,  // 0 = immediate, negative = disabled
        onLockTriggered: (() -> Unit)? = null
    ) {
        currentActivity = activity
        handler = Handler(Looper.getMainLooper())
        parseLockTimeout(lockTimeoutSeconds)
        onLockTriggered?.invoke()
    }
    
    /**
     * Parse timeout string from settings (e.g., "Immediate", "5 min", "30 sec")
     */
    private fun parseLockTimeout(seconds: Int) {
        lockTimeoutMs = when {
            seconds < 0 -> -1  // Disabled
            seconds == 0 -> 0   // Immediate
            else -> (seconds * 1000).toLong()
        }
    }
    
    /**
     * Call from MainActivity.onPause() to track app backgrounding.
     */
    fun onActivityPaused() {
        isAppInBackground = true
        lastInteractionTime = System.currentTimeMillis()
        scheduleAutoLock()
    }
    
    /**
     * Call from MainActivity.onResume() to check if lock should be shown.
     */
    fun onActivityResumed() {
        isAppInBackground = false
        cancelScheduledLock()
        
        // Check if timeout has passed
        if (shouldTriggerLock()) {
            triggerLock()
        }
    }
    
    /**
     * Update user interaction time - call this on any user input.
     */
    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }
    
    /**
     * Update lock timeout setting from settings change.
     */
    fun setLockTimeout(timeoutSeconds: Int) {
        parseLockTimeout(timeoutSeconds)
        // Reschedule lock if app is in background
        if (isAppInBackground) {
            scheduleAutoLock()
        }
    }
    
    /**
     * Enable/disable screen keep-awake with time limit.
     */
    fun setScreenKeepAwake(enabled: Boolean, durationMinutes: Int = 15) {
        val activity = currentActivity ?: return
        val window = activity.window ?: return
        
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Schedule timeout to disable keep-awake
            handler?.postDelayed(
                { setScreenKeepAwake(false) },
                (durationMinutes * 60 * 1000).toLong()
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    private fun scheduleAutoLock() {
        if (lockTimeoutMs < 0) return  // Disabled
        
        cancelScheduledLock()
        handler?.postDelayed(lockTimeoutRunnable, lockTimeoutMs)
    }
    
    private fun cancelScheduledLock() {
        handler?.removeCallbacks(lockTimeoutRunnable)
    }
    
    private fun shouldTriggerLock(): Boolean {
        if (lockTimeoutMs < 0) return false  // Disabled
        if (lockTimeoutMs == 0L) return true  // Immediate lock on background
        
        val timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime
        return timeSinceLastInteraction >= lockTimeoutMs
    }
    
    private fun triggerLock() {
        _shouldShowLock.value = true
    }
    
    fun dismissLock() {
        _shouldShowLock.value = false
        lastInteractionTime = System.currentTimeMillis()
    }
    
    fun reset() {
        currentActivity = null
        handler?.removeCallbacks(lockTimeoutRunnable)
        handler = null
        _shouldShowLock.value = false
    }
}
