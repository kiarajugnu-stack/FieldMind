package fieldmind.research.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import android.util.Log
import fieldmind.research.app.shared.data.model.AppSettings
import fieldmind.research.app.util.ANRWatchdog
import fieldmind.research.app.util.CrashReporter

/**
 * Custom Application class for FieldMind.
 * Handles initialization of:
 * - AppSettings
 * - CrashReporter
 * - NetworkClient
 * - LeakCanary (debug builds)
 * - ANR Watchdog (debug builds)
 */
class FieldMindApplication : Application() {
    
    companion object {
        private const val TAG = "FieldMindApplication"
        private const val TRIM_MEMORY_RUNNING_MODERATE_LEVEL = 5
        private const val TRIM_MEMORY_RUNNING_LOW_LEVEL = 10
        private const val TRIM_MEMORY_RUNNING_CRITICAL_LEVEL = 15
        private const val TRIM_MEMORY_MODERATE_LEVEL = 60
        private const val TRIM_MEMORY_COMPLETE_LEVEL = 80
        
        lateinit var instance: FieldMindApplication
            private set
    }
    
    private var anrWatchdog: ANRWatchdog? = null
    
    override fun onCreate() {
        super.onCreate()
        
        instance = this
        
        Log.d(TAG, "═══════════════════════════════════════════════════")
        Log.d(TAG, "FieldMindApplication onCreate")
        Log.d(TAG, "Build Type: ${BuildConfig.BUILD_TYPE}")
        Log.d(TAG, "Version: ${BuildConfig.VERSION_NAME}")
        Log.d(TAG, "═══════════════════════════════════════════════════")
        
        AppSettings.getInstance(applicationContext)
        Log.d(TAG, "✓ AppSettings initialized")
        
        CrashReporter.init(this)
        Log.d(TAG, "✓ CrashReporter initialized")
        
        fieldmind.research.app.network.NetworkClient.initialize(
            AppSettings.getInstance(applicationContext)
        )
        Log.d(TAG, "✓ NetworkClient initialized")
        
        if (BuildConfig.DEBUG) {
            configureLeakCanary()
            startANRWatchdog()
        }
        
        Log.d(TAG, "FieldMindApplication initialization complete")
    }
    
    private fun configureLeakCanary() {
        try {
            val debugConfigClass = Class.forName("fieldmind.research.app.debug.LeakCanaryDebugConfig")
            val applyMethod = debugConfigClass.getDeclaredMethod("applyKnownReferenceMatchers")
            applyMethod.invoke(null)
            Log.d(TAG, "✓ LeakCanary configured (auto-init + debug matcher tuning)")
        } catch (_: ClassNotFoundException) {
            Log.d(TAG, "✓ LeakCanary configured (auto-init)")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring LeakCanary", e)
        }
    }
    
    private fun startANRWatchdog() {
        try {
            anrWatchdog = ANRWatchdog(timeoutMs = 5000).apply {
                start()
            }
            Log.d(TAG, "✓ ANR Watchdog started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ANR Watchdog", e)
        }
    }
    
    override fun onTerminate() {
        Log.d(TAG, "FieldMindApplication onTerminate")
        anrWatchdog?.stopWatching()
        anrWatchdog = null
        super.onTerminate()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "═══════════════════════════════════════════════════")
        Log.w(TAG, "LOW MEMORY WARNING!")
        Log.w(TAG, "═══════════════════════════════════════════════════")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        val levelName = when (level) {
            TRIM_MEMORY_RUNNING_MODERATE_LEVEL -> "RUNNING_MODERATE"
            TRIM_MEMORY_RUNNING_LOW_LEVEL -> "RUNNING_LOW"
            TRIM_MEMORY_RUNNING_CRITICAL_LEVEL -> "RUNNING_CRITICAL"
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            TRIM_MEMORY_MODERATE_LEVEL -> "MODERATE"
            TRIM_MEMORY_COMPLETE_LEVEL -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }
        
        Log.w(TAG, "onTrimMemory: $levelName")
        
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL_LEVEL,
            TRIM_MEMORY_COMPLETE_LEVEL -> {
                Log.w(TAG, "Critical memory pressure - performing aggressive cleanup")
            }
            TRIM_MEMORY_RUNNING_LOW_LEVEL,
            TRIM_MEMORY_MODERATE_LEVEL -> {
                Log.w(TAG, "Moderate memory pressure - performing standard cleanup")
            }
        }
    }
}
