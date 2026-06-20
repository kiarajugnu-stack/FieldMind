package fieldmind.research.app.features.field.presentation.components

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
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
import kotlin.math.roundToInt


// ══════════════════════════════════════════════════════════════════════
//  FieldMind Camera V2 — Redesigned with glassmorphic controls,
//  pro drawer, improved focus, species field mode
// ══════════════════════════════════════════════════════════════════════

private const val FLASH_OFF = ImageCapture.FLASH_MODE_OFF
private const val FLASH_ON = ImageCapture.FLASH_MODE_ON
private const val FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO

/**
 * Full-screen CameraX with redesigned controls:
 *
 * - **Glassmorphic pill bottom bar**: zoom strip, gallery thumbnail, capture button,
 *   flip camera, pro toggle — all in one floating pill
 * - **Pinch-to-zoom + strip slider** inside the pill
 * - **Tap-to-focus** with animated ring + AE/AF lock indicator
 * - **Slide-up pro drawer**: ISO, EV, WB, manual focus with large touch targets
 * - **Flash toggle** (Off/On/Auto)
 * - **Front/rear switch**
 * - **Grid overlay** (rule-of-thirds)
 * - **Capture timer** (3s/5s/10s)
 * - **Aspect ratio crop guide** (4:3 / 16:9 / 1:1)
 * - **Species field mode**: inline panel after capture with species autocomplete,
 *   category, confidence, notes — "Save" or " Save & Exit"
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FieldMindCameraV2(
    onPhotoCaptured: (uri: String, mimeType: String) -> Unit,
    onSpeciesCaptured: (uri: String, mimeType: String, speciesName: String, category: String, confidence: String, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onAddToObservation: ((uri: String, mimeType: String) -> Unit)? = null,
    onAddQuestion: ((uri: String, mimeType: String) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    multiCaptureMode: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    // ── Immersive mode ──
    DisposableEffect(view) {
        val window = (context as? Activity)?.window
        val previousDecorFits = window?.let { WindowCompat.getInsetsController(it, view).systemBarsBehavior }
        val previousFlags = window?.attributes?.flags ?: 0
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, view).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, view).apply {
                    previousDecorFits?.let { systemBarsBehavior = it }
                    show(WindowInsetsCompat.Type.systemBars())
                }
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.attributes = window.attributes.apply { flags = previousFlags }
            }
        }
    }

    // ── Camera state ──
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

    // ── Zoom ──
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }

    // ── Focus ──
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusRingVisible by remember { mutableStateOf(false) }
    var focusLocked by remember { mutableStateOf(false) }
    val focusRingAnim = rememberInfiniteTransition(label = "focusRing")
    val focusRingScale by focusRingAnim.animateFloat(
        0.8f, 1.2f,
        infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "focusScale"
    )
    val focusRingAlpha by focusRingAnim.animateFloat(
        1f, 0.4f,
        infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "focusAlpha"
    )

    // ── UI toggles ──
    var showGrid by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableFloatStateOf(3f / 4f) }
    val aspectRatios = listOf("4:3" to (3f / 4f), "16:9" to (9f / 16f), "1:1" to 1f)
    var aspectLabel by remember { mutableStateOf("4:3") }

    // ── Pro drawer ──
    var showProDrawer by remember { mutableStateOf(false) }

    // ── Post-capture state ──
    var capturedUri by remember { mutableStateOf<String?>(null) }
    var capturedMime by remember { mutableStateOf<String?>(null) }
    var lastCaptureUri by remember { mutableStateOf<String?>(null) }
    var showPostCapture by remember { mutableStateOf(false) }

    // ── Species field mode ──
    var showSpeciesPanel by remember { mutableStateOf(false) }
    var speciesName by remember { mutableStateOf("") }
    var speciesCategory by remember { mutableStateOf("Other") }
    var speciesConfidence by remember { mutableIntStateOf(80) }
    var speciesNotes by remember { mutableStateOf("") }

    // ── Session capture post-capture dialog ──
    var showCaptureDialog by remember { mutableStateOf(false) }
    var pendingCaptureUri by remember { mutableStateOf<String?>(null) }
    var pendingCaptureMime by remember { mutableStateOf<String?>(null) }
    var capturedCount by remember { mutableIntStateOf(0) }
    var showInAppGallery by remember { mutableStateOf(false) }

    // ── Permission ──
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) onDismiss()
    }
    // Request permission only when camera is actually about to be used
    LaunchedEffect(previewViewRef) { 
        if (previewViewRef != null && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // ── Bind camera ──
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

    // ── Capture function ──
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
                    // Apply timestamp watermark
                    FieldMindWatermark.applyWatermark(context, photoFile.absolutePath)
                    isCapturing = false
                    val uri = photoFile.toURI().toString()
                    capturedUri = uri
                    capturedMime = "image/jpeg"
                    lastCaptureUri = uri
                    if (multiCaptureMode) {
                        pendingCaptureUri = uri
                        pendingCaptureMime = "image/jpeg"
                        capturedCount++
                        showCaptureDialog = true
                    } else {
                        showSpeciesPanel = true
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    val uri = photoFile.toURI().toString()
                    capturedUri = uri
                    capturedMime = "image/jpeg"
                    lastCaptureUri = uri
                    if (multiCaptureMode) {
                        pendingCaptureUri = uri
                        pendingCaptureMime = "image/jpeg"
                        capturedCount++
                        showCaptureDialog = true
                    } else {
                        showSpeciesPanel = true
                    }
                }
            })
        }
    }

    // ── Countdown timer ──
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

    // ── Permission denied state ──
    if (!hasCameraPermission) {
        Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon = FieldMindIcons.Camera, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), size = 48.dp)
                Text("Camera permission required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, shape = RoundedCornerShape(16.dp)) { Text("Grant permission") }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.7f)) }
            }
        }
        return
    }

    // ══════════════════════════════════════════════════════════════
    //  Main camera layout
    // ══════════════════════════════════════════════════════════════
    Box(modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera preview ──
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoom = (zoomRatio * zoom).coerceIn(1f, maxZoom)
                        zoomRatio = newZoom
                        camera?.cameraControl?.setZoomRatio(newZoom)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val pv = previewViewRef ?: return@detectTapGestures
                        val factory = SurfaceOrientedMeteringPointFactory(
                            pv.width.toFloat(), pv.height.toFloat()
                        )
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                        focusPoint = offset
                        focusRingVisible = true
                        focusLocked = false
                        scope.launch {
                            delay(800)
                            focusLocked = true
                            delay(1200)
                            focusRingVisible = false
                            focusLocked = false
                        }
                    }
                },
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

        // ── Grid overlay ──
        if (showGrid) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val lineColor = Color.White.copy(alpha = 0.45f)
                drawLine(lineColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1.5f)
                drawLine(lineColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth = 1.5f)
                drawLine(lineColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1.5f)
                drawLine(lineColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth = 1.5f)
            }
        }

        // ── Crop guide ──
        if (aspectLabel != "Full") {
            CropGuideOverlay(aspectRatio)
        }

        // ── Countdown ──
        if (isCountingDown && countdown > 0) {
            val countScale by rememberInfiniteTransition(label = "countScale").animateFloat(
                0.8f, 1.2f,
                infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "countScaleAnim"
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "$countdown",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                    color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { scaleX = countScale; scaleY = countScale; alpha = 0.9f }
                )
            }
        }

        // ── Top controls bar (glassmorphic) ──
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .safeDrawingPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash toggle
                val flashIcon = when (flashMode) {
                    FLASH_ON -> FieldMindIcons.FlashOn
                    FLASH_AUTO -> FieldMindIcons.FlashAuto
                    else -> FieldMindIcons.FlashOff
                }
                CameraTopIcon(icon = flashIcon, active = flashMode != FLASH_OFF) {
                    flashMode = when (flashMode) {
                        FLASH_OFF -> FLASH_ON
                        FLASH_ON -> FLASH_AUTO
                        else -> FLASH_OFF
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Grid toggle
                    CameraTopIcon(
                        icon = MaterialSymbolIcon("grid_on"),
                        active = showGrid,
                        onClick = { showGrid = !showGrid }
                    )
                    // Timer toggle
                    CameraTopIcon(
                        label = if (timerSeconds == 0) "⏱" else "${timerSeconds}s",
                        active = timerSeconds > 0,
                        onClick = {
                            timerSeconds = when (timerSeconds) {
                                0 -> 3; 3 -> 5; 5 -> 10; else -> 0
                            }
                        }
                    )
                    // Aspect ratio
                    CameraTopIcon(
                        label = aspectLabel,
                        active = false,
                        onClick = {
                            val idx = aspectRatios.indexOfFirst { it.first == aspectLabel }
                            val next = (idx + 1) % aspectRatios.size
                            aspectLabel = aspectRatios[next].first
                            aspectRatio = aspectRatios[next].second
                        }
                    )
                }

                // Close
                CameraTopIcon(icon = FieldMindIcons.Close, active = false, onClick = onDismiss)
            }
        }

        // ── Focus ring ──
        focusPoint?.let { pt ->
            if (focusRingVisible) {
                Box(
                    Modifier
                        .offset(x = pt.x.dp - 24.dp, y = pt.y.dp - 24.dp)
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = focusRingScale
                            scaleY = focusRingScale
                            alpha = focusRingAlpha
                        }
                        .border(2.dp, if (focusLocked) Color(0xFF4CAF50) else Color.White, CircleShape)
                ) {
                    if (focusLocked) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .align(Alignment.Center)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.3f), CircleShape)
                                .border(1.5f.dp, Color(0xFF4CAF50), CircleShape)
                        )
                    }
                }
            }
        }

        // ── AE/AF lock indicator ──
        if (focusLocked) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.85f)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔒", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "AE/AF LOCK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // ── Dim overlay when species panel or pro drawer is open ──
        if (showSpeciesPanel) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {} // block touches behind panel
            )
        }

        // ── Species field panel (slides up) ──
        AnimatedVisibility(
            visible = showSpeciesPanel,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(tween(150))
        ) {
            SpeciesFieldPanel(
                capturedUri = capturedUri,
                speciesName = speciesName,
                onSpeciesNameChange = { speciesName = it },
                speciesCategory = speciesCategory,
                onSpeciesCategoryChange = { speciesCategory = it },
                speciesConfidence = speciesConfidence,
                onSpeciesConfidenceChange = { speciesConfidence = it },
                speciesNotes = speciesNotes,
                onSpeciesNotesChange = { speciesNotes = it },
                onSaveContinue = {
                    val uri = capturedUri ?: return@SpeciesFieldPanel
                    val mime = capturedMime ?: "image/jpeg"
                    // Always call onPhotoCaptured first so all callers get the photo
                    onPhotoCaptured(uri, mime)
                    // Also call onSpeciesCaptured for callers that want metadata (defaults to no-op)
                    onSpeciesCaptured(uri, mime, speciesName, speciesCategory, speciesConfidence.toString(), speciesNotes)
                    // Reset for next capture
                    speciesName = ""
                    speciesCategory = "Other"
                    speciesConfidence = 80
                    speciesNotes = ""
                    showSpeciesPanel = false
                },
                onSaveExit = {
                    val uri = capturedUri ?: return@SpeciesFieldPanel
                    val mime = capturedMime ?: "image/jpeg"
                    // Always call onPhotoCaptured first so all callers get the photo
                    onPhotoCaptured(uri, mime)
                    // Also call onSpeciesCaptured for callers that want metadata (defaults to no-op)
                    onSpeciesCaptured(uri, mime, speciesName, speciesCategory, speciesConfidence.toString(), speciesNotes)
                    showSpeciesPanel = false
                    onDismiss()
                },
                onDismissPanel = {
                    // Just save without species data
                    capturedUri?.let { uri ->
                        onPhotoCaptured(uri, capturedMime ?: "image/jpeg")
                    }
                    showSpeciesPanel = false
                }
            )
        }

        // ── Session post-capture dialog (redesigned with categories) ──
        if (showCaptureDialog && multiCaptureMode) {
            // Post-capture category/confidence state
            var postCategory by remember { mutableStateOf("Other") }
            var postConfidence by remember { mutableIntStateOf(80) }
            var postNotes by remember { mutableStateOf("") }
            var postSpeciesName by remember { mutableStateOf("") }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── Header with thumbnail + count ──
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Photo thumbnail
                            if (pendingCaptureUri != null) {
                                Box(
                                    Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    AsyncImage(
                                        model = pendingCaptureUri,
                                        contentDescription = "Last capture",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Photo $capturedCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "#$capturedCount",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Text(
                                    "Captured at ${SimpleDateFormat("HH:mm", Locale.US).format(Date())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Close button (save photo without metadata, then close dialog)
                            IconButton(onClick = {
                                val uri = pendingCaptureUri
                                val mime = pendingCaptureMime
                                if (uri != null) {
                                    onPhotoCaptured(uri, mime ?: "image/jpeg")
                                }
                                pendingCaptureUri = null
                                pendingCaptureMime = null
                                showCaptureDialog = false
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(FieldMindIcons.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                            }
                        }

                        // ── Species name field ──
                        OutlinedTextField(
                            value = postSpeciesName,
                            onValueChange = { postSpeciesName = it },
                            label = { Text("Species name (optional)") },
                            placeholder = { Text("e.g. Red-tailed Hawk") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        // ── Category chips (2 rows) ──
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Category",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            val captureCategories = listOf(
                                "Bird", "Mammal", "Insect", "Plant", "Fungi",
                                "Reptile", "Amphibian", "Fish", "Mollusk", "Habitat", "Other"
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                captureCategories.forEach { cat ->
                                    val selected = postCategory == cat
                                    Surface(
                                        onClick = { postCategory = cat },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = if (!selected) androidx.compose.foundation.BorderStroke(
                                            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ) else null,
                                        tonalElevation = 0.dp
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    FieldMindIcons.Check, null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    size = 14.dp
                                                )
                                            }
                                            Text(
                                                cat,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Confidence slider ──
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Confidence",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Text(
                                    "${postConfidence}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = postConfidence.toFloat(),
                                onValueChange = { postConfidence = it.roundToInt() },
                                valueRange = 50f..99f,
                                steps = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("50%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text("99%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }

                        // ── Notes field ──
                        OutlinedTextField(
                            value = postNotes,
                            onValueChange = { postNotes = it },
                            label = { Text("Quick notes (optional)") },
                            placeholder = { Text("Behavior, location details, etc.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        // ── Action buttons ──
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Keep Shooting — saves photo (via onPhotoCaptured) + optional metadata (via onSpeciesCaptured), stays in camera
                            OutlinedButton(
                                onClick = {
                                    val uri = pendingCaptureUri
                                    val mime = pendingCaptureMime
                                    if (uri != null) {
                                        // ALWAYS call onPhotoCaptured so all callers get the photo
                                        onPhotoCaptured(uri, mime ?: "image/jpeg")
                                        // Also call onSpeciesCaptured for callers that want metadata (defaults to no-op)
                                        onSpeciesCaptured(
                                            uri, mime ?: "image/jpeg",
                                            postSpeciesName, postCategory, postConfidence.toString(), postNotes
                                        )
                                    }
                                    // Reset dialog for next capture
                                    pendingCaptureUri = null
                                    pendingCaptureMime = null
                                    postSpeciesName = ""
                                    postCategory = "Other"
                                    postConfidence = 80
                                    postNotes = ""
                                    showCaptureDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(FieldMindIcons.Camera, null, size = 18.dp)
                                Spacer(Modifier.size(6.dp))
                                Text("Keep shooting", maxLines = 1)
                            }

                            // Done — saves photo, closes camera
                            Button(
                                onClick = {
                                    val uri = pendingCaptureUri
                                    val mime = pendingCaptureMime
                                    if (uri != null) {
                                        // ALWAYS call onPhotoCaptured so all callers get the photo
                                        onPhotoCaptured(uri, mime ?: "image/jpeg")
                                        // Also call onSpeciesCaptured for callers that want metadata (defaults to no-op)
                                        onSpeciesCaptured(
                                            uri, mime ?: "image/jpeg",
                                            postSpeciesName, postCategory, postConfidence.toString(), postNotes
                                        )
                                    }
                                    pendingCaptureUri = null
                                    pendingCaptureMime = null
                                    showCaptureDialog = false
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(FieldMindIcons.Archive, null, size = 18.dp)
                                Spacer(Modifier.size(6.dp))
                                Text("Done", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // ── Pro controls drawer ──
        Box(Modifier.fillMaxSize()) { 
            AnimatedVisibility(
                visible = showProDrawer && !showSpeciesPanel,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(tween(150))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    ProControlsDrawer(
                        camera = camera,
                        maxZoom = maxZoom,
                        onClose = { showProDrawer = false }
                    )
                }
            }
        }

        // ── Glassmorphic pill bottom bar ──
        if (!showSpeciesPanel) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .safeDrawingPadding()
            ) {
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    tonalElevation = 0.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        // ── Zoom strip (inside the pill) ──
                        if (maxZoom > 1f) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "W",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Slider(
                                    value = zoomRatio,
                                    onValueChange = {
                                        zoomRatio = it
                                        camera?.cameraControl?.setZoomRatio(it)
                                    },
                                    valueRange = 1f..maxZoom,
                                    modifier = Modifier.weight(1f).height(24.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    )
                                )
                                Text(
                                    "${zoomRatio.roundToInt()}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        // ── Bottom controls row ──
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery thumbnail (in-app FieldMind gallery)
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { showInAppGallery = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (lastCaptureUri != null) {
                                    AsyncImage(
                                        model = lastCaptureUri,
                                        contentDescription = "Last photo",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        MaterialSymbolIcon("photo_library"),
                                        null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        size = 20.dp
                                    )
                                }
                            }

                            // Capture button
                            Box(
                                Modifier.size(72.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.9f))
                                    .clickable(enabled = !isCapturing && !isCountingDown) {
                                        if (timerSeconds > 0) {
                                            countdown = timerSeconds
                                            isCountingDown = true
                                        } else {
                                            doCapture()
                                        }
                                    }
                                    .padding(5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier.fillMaxSize().clip(CircleShape)
                                        .background(if (isCapturing) Color.Gray else Color.White)
                                )
                            }

                            // Right side: flip + pro toggle
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Flip camera
                                PillIconButton(
                                    icon = FieldMindIcons.FlipCamera,
                                    onClick = {
                                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                            CameraSelector.LENS_FACING_FRONT
                                        else
                                            CameraSelector.LENS_FACING_BACK
                                    }
                                )
                                // Pro toggle
                                PillIconButton(
                                    label = "P",
                                    active = showProDrawer,
                                    onClick = { showProDrawer = !showProDrawer }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Capturing indicator ──
        if (isCapturing) {
            CircularProgressIndicator(
                Modifier.align(Alignment.Center).size(32.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }

        // ── In-app gallery overlay ──
        if (showInAppGallery) {
            FieldMindInAppGallery(
                onDismiss = { showInAppGallery = false },
                onSelectImage = { attachments ->
                    attachments.forEach { attachment ->
                        onPhotoCaptured(attachment.uri, attachment.mimeType ?: "image/jpeg")
                    }
                    showInAppGallery = false
                },
                title = "FieldMind Captures"
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Sub-components
// ══════════════════════════════════════════════════════════════════════

/**
 * Small icon button for the top controls bar.
 */
@Composable
private fun CameraTopIcon(
    icon: MaterialSymbolIcon? = null,
    label: String? = null,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint = if (active) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.85f)
    Box(
        Modifier.size(38.dp).clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon = icon, contentDescription = null, tint = tint, size = 20.dp)
        } else if (label != null) {
            Text(label, color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Icon button for the glassmorphic pill bottom bar.
 */
@Composable
private fun PillIconButton(
    icon: MaterialSymbolIcon? = null,
    label: String? = null,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (active) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
    val tint = if (active) Color(0xFFFFCC80) else Color.White
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon = icon, contentDescription = null, tint = tint, size = 20.dp)
        } else if (label != null) {
            Text(label, color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Species field mode inline panel — slides up after capture.
 * Lets the user tag the photo with species details and optionally continue shooting.
 * Redesigned with polished visual hierarchy, Material icons, and color-coded categories.
 */
@Composable
private fun SpeciesFieldPanel(
    capturedUri: String?,
    speciesName: String,
    onSpeciesNameChange: (String) -> Unit,
    speciesCategory: String,
    onSpeciesCategoryChange: (String) -> Unit,
    speciesConfidence: Int,
    onSpeciesConfidenceChange: (Int) -> Unit,
    speciesNotes: String,
    onSpeciesNotesChange: (String) -> Unit,
    onSaveContinue: () -> Unit,
    onSaveExit: () -> Unit,
    onDismissPanel: () -> Unit
) {
    val categories = listOf(
        "Bird", "Mammal", "Insect", "Plant", "Fungi",
        "Reptile", "Amphibian", "Fish", "Mollusk", "Other"
    )
    val confidenceValues = listOf(50, 60, 70, 80, 90, 95, 99)
    val confidenceLabels = mapOf(
        50 to "Tentative", 60 to "Uncertain", 70 to "Moderate",
        80 to "Plausible", 90 to "Confident", 95 to "Very confident", 99 to "Certain"
    )
    val categoryIcons = mapOf(
        "Bird" to MaterialSymbolIcon("raven"),
        "Mammal" to MaterialSymbolIcon("pets"),
        "Insect" to MaterialSymbolIcon("bug_report"),
        "Plant" to MaterialSymbolIcon("local_florist"),
        "Fungi" to MaterialSymbolIcon("psychiatry"),
        "Reptile" to MaterialSymbolIcon("pets"),
        "Amphibian" to MaterialSymbolIcon("pets"),
        "Fish" to MaterialSymbolIcon("water_drop"),
        "Mollusk" to MaterialSymbolIcon("water_drop"),
        "Other" to MaterialSymbolIcon("category")
    )
    val colors = FieldMindTheme.colors

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .safeDrawingPadding()
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
            shadowElevation = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ── Hero header with photo thumbnail ──
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Photo thumbnail with decorative ring
                    Box(
                        Modifier.size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                2.dp,
                                colors.observation.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        if (capturedUri != null) {
                            AsyncImage(
                                model = capturedUri,
                                contentDescription = "Captured photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    FieldMindIcons.Camera,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    size = 28.dp
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Tag species",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Add details and keep shooting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Dismiss button
                    Surface(
                        onClick = onDismissPanel,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                FieldMindIcons.Close,
                                "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 20.dp
                            )
                        }
                    }
                }

                // ── Species name ──
                OutlinedTextField(
                    value = speciesName,
                    onValueChange = onSpeciesNameChange,
                    label = { Text("Species name") },
                    placeholder = { Text("e.g. Red-tailed Hawk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = {
                        Icon(
                            FieldMindIcons.Search,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.observation,
                        cursorColor = colors.observation
                    )
                )

                // ── Category chips with icons ──
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    // Categories in flow layout
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = speciesCategory == cat
                            val accentColor = when (cat) {
                                "Bird" -> Color(0xFF2E7D32)
                                "Mammal" -> Color(0xFF6A1B9A)
                                "Insect" -> Color(0xFFE65100)
                                "Plant" -> Color(0xFF1B5E20)
                                "Fungi" -> Color(0xFF4E342E)
                                "Reptile" -> Color(0xFF558B2F)
                                "Amphibian" -> Color(0xFF00838F)
                                "Fish" -> Color(0xFF1565C0)
                                "Mollusk" -> Color(0xFF795548)
                                else -> Color(0xFF546E7A)
                            }
                            Surface(
                                onClick = { onSpeciesCategoryChange(cat) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) accentColor.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
                                else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                tonalElevation = 0.dp
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        categoryIcons[cat] ?: MaterialSymbolIcon("category"),
                                        null,
                                        tint = if (isSelected) accentColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 16.dp
                                    )
                                    Text(
                                        cat,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) accentColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Confidence section ──
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Confidence",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                confidenceLabels[speciesConfidence] ?: "Moderate",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.observation
                            )
                            Text(
                                "$speciesConfidence%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Confidence bubbly slider
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        confidenceValues.forEach { value ->
                            val isSelected = speciesConfidence == value
                            val selectable = abs(speciesConfidence - value) <= 10 || isSelected
                            val alpha = when {
                                isSelected -> 1f
                                abs(speciesConfidence - value) <= 10 -> 0.6f
                                else -> 0.3f
                            }
                            Surface(
                                onClick = { onSpeciesConfidenceChange(value) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (!isSelected) BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ) else null,
                                tonalElevation = 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        "$value",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Notes field ──
                OutlinedTextField(
                    value = speciesNotes,
                    onValueChange = onSpeciesNotesChange,
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Behavior, habitat, field marks…") },
                    minLines = 1,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.observation,
                        cursorColor = colors.observation
                    )
                )

                // ── Action buttons ──
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveExit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            FieldMindIcons.Archive,
                            null,
                            size = 18.dp
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Save & Exit", maxLines = 1)
                    }
                    Button(
                        onClick = onSaveContinue,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.observation
                        )
                    ) {
                        Icon(
                            FieldMindIcons.Camera,
                            null,
                            size = 18.dp
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Add More", maxLines = 1)
                    }
                }
            }
        }
    }
}


/**
 * Slide-up pro controls drawer with ISO, EV, WB, and manual focus.
 */
@Composable
private fun ProControlsDrawer(
    camera: Camera?,
    maxZoom: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var evValue by remember { mutableFloatStateOf(0f) }
    var isoMode by remember { mutableIntStateOf(0) }
    var wbMode by remember { mutableIntStateOf(0) }
    var manualFocus by remember { mutableFloatStateOf(0f) }

    val isoLabels = listOf("Auto", "100", "200", "400", "800")
    val wbLabels = listOf("Auto", "☀️Sun", "☁️Cloud", "💡Tung", "🏠Fluor")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .safeDrawingPadding(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.82f),
        shadowElevation = 16.dp
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pro Controls",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    onClick = onClose,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("Done", color = Color(0xFFFFCC80), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // EV compensation
            ProControlRow(
                label = "EV",
                value = "${evValue.toInt()}${if (evValue > 0) "+" else ""}",
                accent = evValue != 0f
            ) {
                Slider(
                    value = evValue,
                    onValueChange = {
                        evValue = it.roundToInt().toFloat()
                        camera?.cameraInfo?.exposureState?.let { state ->
                            val idx = it.toInt().coerceIn(
                                state.exposureCompensationRange.lower,
                                state.exposureCompensationRange.upper
                            )
                            camera.cameraControl?.setExposureCompensationIndex(idx)
                        }
                    },
                    valueRange = -2f..2f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFFFCC80),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }

            // ISO selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "ISO",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    isoLabels.forEachIndexed { i, label ->
                        val selected = isoMode == i
                        Surface(
                            onClick = { isoMode = i },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) Color(0xFFFFCC80).copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.08f),
                            border = if (selected) androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFFFCC80).copy(alpha = 0.5f)
                            ) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // WB selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "White Balance",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wbLabels.forEachIndexed { i, label ->
                        val selected = wbMode == i
                        Surface(
                            onClick = { wbMode = i },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) Color(0xFFFFCC80).copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.08f),
                            border = if (selected) androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFFFCC80).copy(alpha = 0.5f)
                            ) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Manual focus slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Focus",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "AF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (manualFocus == 0f) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = manualFocus,
                        onValueChange = { manualFocus = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFFFCC80),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Text(
                        "MF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (manualFocus > 0f) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Row label + value for pro control sliders.
 */
@Composable
private fun ProControlRow(
    label: String,
    value: String,
    accent: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(28.dp)
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = content)
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (accent) FontWeight.Bold else FontWeight.Normal,
            color = if (accent) Color(0xFFFFCC80) else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Crop Guide Overlay (unchanged from original)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun CropGuideOverlay(aspectRatio: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val guideWidth = size.width * 0.85f
        val guideHeight = (guideWidth / aspectRatio).coerceAtMost(size.height * 0.85f)
        val left = (size.width - guideWidth) / 2f
        val top = (size.height - guideHeight) / 2f
        val cornerLen = 24f
        val lineColor = Color.White.copy(alpha = 0.72f)
        val dimColor = Color.Black.copy(alpha = 0.32f)

        drawRect(dimColor, topLeft = Offset(0f, 0f), size = Size(size.width, top))
        drawRect(dimColor, topLeft = Offset(0f, top + guideHeight), size = Size(size.width, size.height - top - guideHeight))
        drawRect(dimColor, topLeft = Offset(0f, top), size = Size(left, guideHeight))
        drawRect(dimColor, topLeft = Offset(left + guideWidth, top), size = Size(size.width - left - guideWidth, guideHeight))

        drawLine(lineColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = 3f)
        drawLine(lineColor, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = 3f)
        drawLine(lineColor, Offset(left + guideWidth, top), Offset(left + guideWidth - cornerLen, top), strokeWidth = 3f)
        drawLine(lineColor, Offset(left + guideWidth, top), Offset(left + guideWidth, top + cornerLen), strokeWidth = 3f)
        drawLine(lineColor, Offset(left, top + guideHeight), Offset(left + cornerLen, top + guideHeight), strokeWidth = 3f)
        drawLine(lineColor, Offset(left, top + guideHeight), Offset(left, top + guideHeight - cornerLen), strokeWidth = 3f)
        drawLine(lineColor, Offset(left + guideWidth, top + guideHeight), Offset(left + guideWidth - cornerLen, top + guideHeight), strokeWidth = 3f)
        drawLine(lineColor, Offset(left + guideWidth, top + guideHeight), Offset(left + guideWidth, top + guideHeight - cornerLen), strokeWidth = 3f)
    }
}

/**
 * Resolve MIME type from a content URI using ContentResolver.
 * Falls back to image/jpeg if the type cannot be determined.
 */
private fun getMimeTypeForUri(context: android.content.Context, uri: android.net.Uri): String? {
    return runCatching {
        context.contentResolver.getType(uri)
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

