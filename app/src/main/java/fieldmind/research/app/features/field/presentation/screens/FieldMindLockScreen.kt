package fieldmind.research.app.features.field.presentation.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import fieldmind.research.app.features.field.data.settings.FieldMindSettings
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.LocalPrivacyTypingEnabled
import fieldmind.research.app.features.field.presentation.components.PrivacyTypingIndicator
import fieldmind.research.app.features.field.presentation.components.withPrivacyTyping
import fieldmind.research.app.shared.presentation.components.icons.Icon
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
    val appPinEnabled by settings.appPinEnabled.collectAsState()
    val appPinHash by settings.appPinHash.collectAsState()
    val hasPin = appPinEnabled && appPinHash.isNotBlank()
    if ((!privacyEnabled && !hasPin) || isUnlocked) {
        content()
        return
    }

    val context = LocalContext.current
    val keyguard = remember(context) { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val hasBiometric = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    val hasDeviceCredential = keyguard.isDeviceSecure
    var usePinLock by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pinAttempts by remember { mutableIntStateOf(0) }
    var pinLockedUntil by remember { mutableLongStateOf(0L) }
    val isPinLocked = pinLockedUntil > System.currentTimeMillis()
    var authAttempted by remember { mutableStateOf(false) }

    val unlockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onUnlock()
        } else {
            // Device auth failed, fall back to PIN if available
            if (hasPin) usePinLock = true
        }
    }

    fun startBiometricAuth() {
        authAttempted = true
        usePinLock = false
        val activity = context as? FragmentActivity
        if (hasBiometric && activity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlock()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (hasPin) usePinLock = true
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
        } else if (hasDeviceCredential) {
            val intent = keyguard.createConfirmDeviceCredentialIntent(
                "FieldMind Privacy Lock",
                "Authenticate to access your research data"
            )
            if (intent != null) {
                unlockLauncher.launch(intent)
            } else if (hasPin) {
                usePinLock = true
            }
        } else if (hasPin) {
            usePinLock = true
        }
    }

    // Try biometric/device auth first, then fall back to PIN.
    LaunchedEffect(privacyEnabled) {
        if (privacyEnabled && !isUnlocked && !authAttempted) {
            startBiometricAuth()
        }
    }

    Box(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
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
                                if (it.length >= 4 && settings.verifyAppPin(it)) {
                                    pinAttempts = 0
                                    onUnlock()
                                } else if (it.length >= 4) {
                                    pinAttempts++
                                    pinError = true
                                    pin = ""
                                    if (pinAttempts >= 3) {
                                        pinLockedUntil = System.currentTimeMillis() + 30_000
                                        pinAttempts = 0
                                    }
                                }
                            }
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("PIN") },
                        singleLine = true,
                        isError = pinError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done).withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                        supportingText = if (pinError) {{ Text("Incorrect PIN. Try again.") }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp),
                        trailingIcon = {
                            if (LocalPrivacyTypingEnabled.current) {
                                PrivacyTypingIndicator()
                            }
                        }
                    )
                }

                if (usePinLock && isPinLocked) {
                    Text(
                        "Too many attempts. Try again in 30 seconds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasPin) {
                        OutlinedButton(
                            onClick = { usePinLock = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (usePinLock) "Using PIN" else "Use PIN")
                        }
                    }
                    if (hasBiometric || hasDeviceCredential) {
                        Button(
                            onClick = { startBiometricAuth() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (hasBiometric) "Retry biometric" else "Retry device lock")
                        }
                    }
                }
            }
        }
    }
}
