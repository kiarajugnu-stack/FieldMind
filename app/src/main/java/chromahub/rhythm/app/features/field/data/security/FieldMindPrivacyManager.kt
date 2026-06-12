package chromahub.rhythm.app.features.field.data.security

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executors

/**
 * Privacy lock manager supporting BiometricPrompt (fingerprint, face, iris)
 * with PIN/pattern/password fallback via KeyguardManager.
 */
class FieldMindPrivacyManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fieldmind_privacy", Context.MODE_PRIVATE)

    /** Whether biometric auth is available on this device. */
    val isBiometricAvailable: Boolean
        get() {
            val mgr = BiometricManager.from(context)
            return mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                    BiometricManager.BIOMETRIC_SUCCESS
        }

    /** Whether device credential (PIN/pattern/password) is available. */
    val isDeviceCredentialAvailable: Boolean
        get() {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            return km.isDeviceSecure
        }

    /** The best available auth type. */
    val availableAuthType: String
        get() = when {
            isBiometricAvailable -> "biometric"
            isDeviceCredentialAvailable -> "pin"
            else -> "none"
        }

    /** Whether a privacy lock method is configured. */
    val isConfigured: Boolean get() = prefs.getBoolean(KEY_CONFIGURED, false)

    /** Mark privacy as configured. */
    fun markConfigured() {
        prefs.edit().putBoolean(KEY_CONFIGURED, true).apply()
    }

    /** Clear privacy configuration (for reset). */
    fun clearConfiguration() {
        prefs.edit().clear().apply()
    }

    /**
     * Show biometric prompt. Calls onSuccess if auth passes.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricAvailable) {
            onError("No biometric hardware available")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onError("Authentication failed")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    /**
     * Show device credential screen (PIN/pattern/password).
     */
    fun showDeviceCredential(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isDeviceCredentialAvailable) {
            onError("No device credential set up")
            return
        }

        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intent = km.createConfirmDeviceCredentialIntent(
                "FieldMind Privacy Lock",
                "Authenticate to access your FieldMind data"
            )
            if (intent != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_CONFIRM_CREDENTIAL)
                onSuccess()
            } else {
                onError("Could not launch credential screen")
            }
        } else {
            onError("Device credential not supported on this Android version")
        }
    }

    companion object {
        private const val KEY_CONFIGURED = "privacy_configured"
        const val REQUEST_CODE_CONFIRM_CREDENTIAL = 901

        @Volatile private var INSTANCE: FieldMindPrivacyManager? = null
        fun getInstance(context: Context): FieldMindPrivacyManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FieldMindPrivacyManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
