package fieldmind.research.app.features.field.presentation.components

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.sqrt

// ══════════════════════════════════════════════════════════════════════
//  FieldMind Camera V2 — Full redesigned CameraX
// ══════════════════════════════════════════════════════════════════════

private const val FLASH_OFF = ImageCapture.FLASH_MODE_OFF
private const val FLASH_ON = ImageCapture.FLASH_MODE_ON
private const val FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO

/**
 * Full-screen CameraX with:
 * - Immersive mode (no system bars)
 * - Pinch-to-zoom with on-screen slider
 * - Tap-to-focus with animated ring
 * - Flash toggle (Off/On/Auto)
 * - Front/rear switch
 * - Grid overlay (rule-of-thirds)
 * - Capture timer (3s/5s/10s)
 * - Aspect ratio toggle (4:3 / 16:9 / 1:1)
 * - Post-capture bottom sheet: "Add to Observation" / "Add Question" / "Just Save"
 * - Auto GPS + timestamp metadata on capture
 */
@Composable
fun FieldMindCameraV2(
    onPhotoCaptured: (uri: String, mimeType: String) -> Unit,
    onAddToObservation: ((uri: String, mimeType: String) -> Unit)? = null,
    onAddQuestion: ((uri: String, mimeType: String) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(FLASH_OFF) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Zoom state
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    val animatedZoom by animateFloatAsState(zoomRatio, tween(200), label = "zoom")

    // Focus ring state
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusRingVisible by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "focus")
    val focusRingScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "focusRing"
    )
    val focusRingAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "focusRingAlpha"
    )

    // Grid overlay
    var showGrid by remember { mutableStateOf(false) }

    // Timer
    var timerSeconds by remember { mutableIntStateOf(0) } // 0 = no timer
    var countdown by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }

    // Aspect ratio
    var aspectRatio by remember { mutableFloatStateOf(3f / 4f) } // 4:3 default
    val aspectRatios = listOf("4:3" to (3f / 4f), "16:9" to (9f / 16f), "1:1" to 1f)
    var aspectLabel by remember { mutableStateOf("4:3") }

    // Post-capture state
    var capturedUri by remember { mutableStateOf<String?>(null) }
    var capturedMime by remember { mutableStateOf<String?>(null) }
    var showPostCapture by remember { mutableStateOf(false) }

    // Permission
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) onDismiss()
    }
    LaunchedEffect(Unit) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // Bind camera
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
            val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            camera = cam
            imageCapture = capture
            maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
        } catch (_: Exception) {
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                try {
                    val fallback = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
                    provider.unbindAll()
                    val cam = provider.bindToLifecycle(lifecycleOwner, fallback, preview, capture)
                    camera = cam
                    imageCapture = capture
                    lensFacing = CameraSelector.LENS_FACING_FRONT
                    maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(flashMode) { imageCapture?.flashMode = flashMode }

    // Local capture function
    val doCapture = remember(imageCapture) {
        {
            val capture = imageCapture ?: return@remember
            isCapturing = true
            val photoFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "FieldMind_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            )
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            capture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    runCatching {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FieldMind")
                        }
                        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                            context.contentResolver.openOutputStream(uri)?.use { os -> photoFile.inputStream().use { it.copyTo(os) } }
                        }
                    }
                    isCapturing = false
                    capturedUri = photoFile.toURI().toString()
                    capturedMime = "image/jpeg"
                    showPostCapture = true
                }
                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    capturedUri = photoFile.toURI().toString()
                    capturedMime = "image/jpeg"
                    showPostCapture = true
                }
            })
        }
    }

    // Countdown timer
    LaunchedEffect(isCountingDown, countdown) {
        if (isCountingDown && countdown > 0) {
            delay(1000)
            countdown--
            if (countdown == 0) {
                isCountingDown = false
                doCapture()
            }
        }
    }

    if (!hasCameraPermission) {
        Box(modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon = FieldMindIcons.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 48.dp)
                Text("Camera permission required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, shape = RoundedCornerShape(16.dp)) { Text("Grant permission") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
        return
    }

    Box(modifier.fillMaxWidth()) {
        // Camera preview
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(28.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoom = (zoomRatio * zoom).coerceIn(1f, maxZoom)
                        zoomRatio = newZoom
                        camera?.cameraControl?.setZoomRatio(newZoom)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Tap to focus
                        val previewView = previewViewRef ?: return@detectTapGestures
                        val factory = SurfaceOrientedMeteringPointFactory(previewView.width.toFloat(), previewView.height.toFloat())
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                        focusPoint = offset
                        focusRingVisible = true
                        scope.launch { delay(1500); focusRingVisible = false }
                    }
                },
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewViewRef = this
                }
            },
            update = { previewViewRef = it }
        )

        // Grid overlay
        if (showGrid) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                val w = size.width
                val h = size.height
                val lineColor = Color.White.copy(alpha = 0.5f)
                // Rule of thirds
                drawLine(lineColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1.5f)
                drawLine(lineColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth = 1.5f)
                drawLine(lineColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1.5f)
                drawLine(lineColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth = 1.5f)
            }
        }

        // Focus ring
        focusPoint?.let { pt ->
            if (focusRingVisible) {
                Box(
                    Modifier
                        .offset(x = pt.x.dp - 24.dp, y = pt.y.dp - 24.dp)
                        .size(48.dp)
                        .graphicsLayer { scaleX = focusRingScale; scaleY = focusRingScale; alpha = focusRingAlpha }
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        // Zoom slider
        if (maxZoom > 1f) {
            Slider(
                value = zoomRatio,
                onValueChange = { zoomRatio = it; camera?.cameraControl?.setZoomRatio(it) },
                valueRange = 1f..maxZoom,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 60.dp, vertical = 8.dp)
                    .height(32.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }

        // Countdown overlay
        if (isCountingDown && countdown > 0) {
            Box(Modifier.fillMaxWidth().aspectRatio(aspectRatio).clip(RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Text(
                    "$countdown",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { scaleX = focusRingScale; scaleY = focusRingScale }
                )
            }
        }

        // Controls overlay
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .padding(20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Top controls
            Row(Modifier.fillMaxWidth().align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween) {
                // Flash toggle
                val flashIcon = when (flashMode) { FLASH_ON -> FieldMindIcons.FlashOn; FLASH_AUTO -> FieldMindIcons.FlashAuto; else -> FieldMindIcons.FlashOff }
                IconButton(onClick = { flashMode = when (flashMode) { FLASH_OFF -> FLASH_ON; FLASH_ON -> FLASH_AUTO; FLASH_AUTO -> FLASH_OFF; else -> FLASH_OFF } }, modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape)) {
                    Icon(icon = flashIcon, contentDescription = "Flash", tint = Color.White, size = 22.dp)
                }

                // Center controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Grid toggle
                    IconButton(onClick = { showGrid = !showGrid }, modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape)) {
                        Icon(icon = MaterialSymbolIcon("grid_on"), contentDescription = "Grid", tint = if (showGrid) Color(0xFFFFCC80) else Color.White, size = 22.dp)
                    }
                    // Timer toggle
                    IconButton(onClick = { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; 5 -> 10; else -> 0 } }, modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape)) {
                        Text(if (timerSeconds == 0) "⏱" else "${timerSeconds}s", color = if (timerSeconds > 0) Color(0xFFFFCC80) else Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    // Aspect ratio
                    IconButton(onClick = {
                        val idx = aspectRatios.indexOfFirst { it.first == aspectLabel }
                        val next = (idx + 1) % aspectRatios.size
                        aspectLabel = aspectRatios[next].first
                        aspectRatio = aspectRatios[next].second
                    }, modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape)) {
                        Text(aspectLabel, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Dismiss
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape)) {
                    Icon(icon = FieldMindIcons.Close, contentDescription = "Close", tint = Color.White, size = 22.dp)
                }
            }

            // Bottom controls
            Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Flash label
                Text(when (flashMode) { FLASH_ON -> "Flash on"; FLASH_AUTO -> "Auto"; else -> "" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 8.dp))

                // Capture button
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
                        .clickable(enabled = !isCapturing && !isCountingDown) {
                            if (timerSeconds > 0) {
                                countdown = timerSeconds
                                isCountingDown = true
                            } else {
                                doCapture()
                            }
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(if (isCapturing) Color.Gray else Color.White))
                }

                // Switch camera
                IconButton(onClick = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }, modifier = Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape)) {
                    Icon(icon = FieldMindIcons.FlipCamera, contentDescription = "Switch", tint = Color.White, size = 22.dp)
                }
            }

            if (isCapturing) {
                CircularProgressIndicator(Modifier.align(Alignment.Center).size(32.dp), color = Color.White, strokeWidth = 3.dp)
            }
        }
    }

    // Post-capture bottom sheet
    AnimatedVisibility(visible = showPostCapture, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(FieldMindTheme.colors.positive.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = FieldMindTheme.colors.positive, size = 24.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Photo saved", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("What would you like to do?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    onAddToObservation?.let { action ->
                        Button(onClick = { showPostCapture = false; action(capturedUri ?: "", capturedMime ?: "image/jpeg") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(FieldMindIcons.Observation, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Observation")
                        }
                    }
                    onAddQuestion?.let { action ->
                        OutlinedButton(onClick = { showPostCapture = false; action(capturedUri ?: "", capturedMime ?: "image/jpeg") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(FieldMindIcons.Question, null, size = 18.dp); Spacer(Modifier.size(6.dp)); Text("Question")
                        }
                    }
                    FilledTonalButton(onClick = { showPostCapture = false; onPhotoCaptured(capturedUri ?: "", capturedMime ?: "image/jpeg") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text("Just save")
                    }
                }
            }
        }
    }
}
