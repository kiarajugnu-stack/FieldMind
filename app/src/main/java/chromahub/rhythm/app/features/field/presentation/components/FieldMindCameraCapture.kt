package chromahub.rhythm.app.features.field.presentation.components

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ══════════════════════════════════════════════════════════════════════
//  In-app Camera Capture (CameraX)
// ══════════════════════════════════════════════════════════════════════

private const val FLASH_OFF = ImageCapture.FLASH_MODE_OFF
private const val FLASH_ON = ImageCapture.FLASH_MODE_ON
private const val FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO

/**
 * Full in-app camera capture UI using CameraX.
 *
 * Features a live preview, capture button, flash toggle, and front/back camera switch.
 * Captured photos are saved to app-specific storage and returned via [onPhotoCaptured].
 */
@Composable
fun FieldMindCameraCapture(
    onPhotoCaptured: (uri: String, mimeType: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(FLASH_OFF) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // Request camera permission if needed
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) onDismiss()
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Cleanup executor on dispose
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    // Bind camera when preview view is available or lens facing changes
    LaunchedEffect(previewViewRef, lensFacing) {
        val previewView = previewViewRef ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        val provider = runCatching { ProcessCameraProvider.getInstance(context).get() }.getOrNull() ?: return@LaunchedEffect

        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            imageCapture = capture
        } catch (_: Exception) {
            // Camera binding failed—likely no back camera on device; try front as fallback
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                try {
                    val fallbackSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, fallbackSelector, preview, capture)
                    imageCapture = capture
                    lensFacing = CameraSelector.LENS_FACING_FRONT
                } catch (_: Exception) {
                    // No camera available at all
                }
            }
        }
    }

    // Sync flash mode on the ImageCapture (handled automatically for the next capture)
    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    fun capturePhoto() {
        val capture = imageCapture ?: return
        isCapturing = true

        val photoFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "FieldMind_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Save to MediaStore for gallery visibility
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FieldMind")
                    }
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            photoFile.inputStream().use { it.copyTo(outputStream) }
                        }
                    }
                }
                isCapturing = false
                onPhotoCaptured(photoFile.toURI().toString(), "image/jpeg")
            }

            override fun onError(exception: ImageCaptureException) {
                isCapturing = false
                // Fallback: return URI even if MediaStore save failed
                onPhotoCaptured(photoFile.toURI().toString(), "image/jpeg")
            }
        })
    }

    if (!hasCameraPermission) {
        // Permission not yet granted — show placeholder
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon = FieldMindIcons.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 48.dp)
                Text("Camera permission required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, shape = RoundedCornerShape(16.dp)) {
                    Text("Grant permission")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Camera preview
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(28.dp)),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewViewRef = this
                }
            },
            update = { previewViewRef = it }
        )

        // Camera controls overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .padding(20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Top controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Flash toggle
                val flashIcon = when (flashMode) {
                    FLASH_ON -> FieldMindIcons.FlashOn
                    FLASH_AUTO -> FieldMindIcons.FlashAuto
                    else -> FieldMindIcons.FlashOff
                }
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            FLASH_OFF -> FLASH_ON
                            FLASH_ON -> FLASH_AUTO
                            FLASH_AUTO -> FLASH_OFF
                            else -> FLASH_OFF
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(icon = flashIcon, contentDescription = "Flash", tint = Color.White, size = 22.dp)
                }

                // Dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(icon = FieldMindIcons.Close, contentDescription = "Close camera", tint = Color.White, size = 22.dp)
                }
            }

            // Bottom controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash label (shows current flash state)
                Text(
                    when (flashMode) {
                        FLASH_ON -> "Flash on"
                        FLASH_AUTO -> "Auto"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp)
                )

                // Capture button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(enabled = !isCapturing) { capturePhoto() }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (isCapturing) Color.Gray else Color.White)
                    )
                }

                // Switch camera button
                IconButton(
                    onClick = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(icon = FieldMindIcons.FlipCamera, contentDescription = "Switch camera", tint = Color.White, size = 22.dp)
                }
            }

            // Capturing indicator
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
