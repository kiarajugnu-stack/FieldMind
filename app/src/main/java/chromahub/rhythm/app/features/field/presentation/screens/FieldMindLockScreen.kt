package chromahub.rhythm.app.features.field.presentation.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import chromahub.rhythm.app.features.field.data.settings.FieldMindSettings
import chromahub.rhythm.app.features.field.presentation.components.FieldMindIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
// ══════════════════════════════════════════════════════════════════════
//  App Lock / Privacy Gate
// ══════════════════════════════════════════════════════════════════════

/**
 * Full-screen lock that guards the entire FieldMind app when privacy lock is enabled.
 * Supports:
 * - Device credential (PIN/pattern/password via system)
 * - Biometric (fingerprint, face)
 * - In-app 4-6 digit PIN as fallback when no device lock is set
 */
@Composable
fun FieldMindAppLock(
    settings: FieldMindSettings,
    isUnlocked: Boolean,
    onUnlock: () -> Unit,
    content: @Composable () -> Unit
) {
    val privacyEnabled by settings.privacyLockEnabled.collectAsState()
    if (!privacyEnabled || isUnlocked) {
        content()
        return
    }

    val context = LocalContext.current
    val keyguard = remember(context) { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val hasBiometric = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    val hasDeviceCredential = keyguard.isDeviceSecure
    var usePinLock by remember { mutableStateOf(!hasBiometric && !hasDeviceCredential) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf("") } // "", "biometric", "pin"

    val unlockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onUnlock()
        }
    }

    // If device credential or biometric is available, try biometric first
    LaunchedEffect(privacyEnabled) {
        if (privacyEnabled && !isUnlocked && (hasBiometric || hasDeviceCredential) && authMode.isEmpty()) {
            if (hasBiometric) {
                authMode = "biometric"
                val activity = context as? FragmentActivity
                if (activity != null) {
                    val executor = ContextCompat.getMainExecutor(context)
                    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onUnlock()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            authMode = "pin"
                        }
                        override fun onAuthenticationFailed() {
                            pinError = true
                        }
                    })
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("FieldMind Privacy Lock")
                        .setSubtitle("Authenticate to access your research data")
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build()
                    prompt.authenticate(promptInfo)
                }
            } else if (hasDeviceCredential) {
                authMode = "device"
                val intent = keyguard.createConfirmDeviceCredentialIntent(
                    "FieldMind Privacy Lock",
                    "Authenticate to access your research data"
                )
                if (intent != null) {
                    unlockLauncher.launch(intent)
                } else {
                    usePinLock = true
                }
            }
        }
    }

    // In-app PIN fallback when no device/biometric is set
    val savedPin = remember { context.getSharedPreferences("fieldmind_privacy", Context.MODE_PRIVATE).getString("app_pin", "") ?: "" }
    val hasPin = savedPin.isNotBlank()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(0.88f).animateContentSize(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Lock icon
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(FieldMindIcons.Lock, null, tint = MaterialTheme.colorScheme.primary, size = 32.dp)
                }

                Text("FieldMind Locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                Text(
                    "Your research data is protected by privacy lock. Authenticate to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // PIN input (for in-app PIN mode)
                if (usePinLock && hasPin) {
                    Text("Enter PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 6) {
                                pin = it
                                pinError = false
                                if (it.length >= 4 && it == savedPin) {
                                    onUnlock()
                                } else if (it.length >= 4) {
                                    pinError = true
                                    pin = ""
                                }
                            }
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("PIN") },
                        singleLine = true,
                        isError = pinError,
                        supportingText = if (pinError) {{ Text("Incorrect PIN. Try again.") }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp)
                    )
                }

                if (!usePinLock && authMode == "pin") {
                    // Fallback to PIN if available, or device credential
                    if (hasDeviceCredential) {
                        Button(onClick = {
                            val intent = keyguard.createConfirmDeviceCredentialIntent(
                                "FieldMind Privacy Lock",
                                "Authenticate to access your research data"
                            )
                            if (intent != null) unlockLauncher.launch(intent) else onUnlock()
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Icon(FieldMindIcons.Lock, null, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Use device PIN / pattern")
                        }
                    } else {
                        TextButton(onClick = { usePinLock = true }) {
                            Text(if (hasPin) "Enter PIN instead" else "Set up PIN lock in Settings")
                        }
                    }
                }

                if (!usePinLock && hasDeviceCredential && authMode.isEmpty()) {
                    Button(onClick = {
                        val intent = keyguard.createConfirmDeviceCredentialIntent(
                            "FieldMind Privacy Lock",
                            "Authenticate to access your research data"
                        )
                        if (intent != null) unlockLauncher.launch(intent) else onUnlock()
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Icon(FieldMindIcons.Lock, null, size = 18.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Unlock with device security")
                    }
                }
            }
        }
    }
}
