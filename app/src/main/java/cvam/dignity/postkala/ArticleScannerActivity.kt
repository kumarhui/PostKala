package cvam.dignity.postkala.features

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

data class ScanResult(val code: String, val timestamp: Long = System.currentTimeMillis())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val articleRegex = remember { Regex("[A-Z]{2}[0-9]{9}[A-Z]{2}") }

    val scanHistory = remember { mutableStateListOf<ScanResult>() }
    val barcodeCache = remember { mutableStateMapOf<String, Bitmap>() }

    var lastScannedCode by remember { mutableStateOf("") }
    var currentIndex by remember { mutableIntStateOf(0) }
    var showCamera by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }
    var scanningEnabled by remember { mutableStateOf(true) }

    var entryVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entryVisible = true }

    // PERMISSION HANDLER: Added to ensure camera works on first install
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scanningEnabled = true
            showCamera = true
        } else {
            Toast.makeText(context, "Camera permission is required for scanning", Toast.LENGTH_SHORT).show()
        }
    }

    val addResults: (List<String>) -> Unit = { codes ->
        if (scanningEnabled) {
            scope.launch {
                codes.forEach { code ->
                    val upperCode = code.uppercase()
                    if (upperCode != lastScannedCode && scanHistory.none { it.code == upperCode }) {
                        scanHistory.add(ScanResult(upperCode))
                        lastScannedCode = upperCode
                        withContext(Dispatchers.Default) {
                            val bmp = generateArticleBarcode(upperCode)
                            if (bmp != null) barcodeCache[upperCode] = bmp
                        }
                    }
                }
                if (scanHistory.isNotEmpty()) currentIndex = scanHistory.size - 1
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isProcessing = true
            performOcrOnGallery(context, it, articleRegex) { results ->
                addResults(results)
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("STUDIO SCANNER", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { if (showCamera) showCamera = false else onBack() }) {
                        Icon(if (showCamera) Icons.Default.Close else Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AnimatedVisibility(
                visible = entryVisible,
                enter = scaleIn(initialScale = 0.9f, animationSpec = tween(500, easing = DecelerateEasing)) + fadeIn()
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ScannerActionCard(Modifier.weight(1f), "Live Scan", Icons.Default.PhotoCamera, MaterialTheme.colorScheme.primary) {
                            // Check permission before launching camera
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                scanningEnabled = true
                                showCamera = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        ScannerActionCard(Modifier.weight(1f), "Gallery", Icons.Default.Collections, Color(0xFF00C853)) {
                            galleryLauncher.launch("image/*")
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("MANUAL ENTRY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { if (it.length <= 13) manualInput = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type 13-digit code...", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Outlined.QrCode, null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )

                    AnimatedVisibility(visible = manualInput.length == 13) {
                        ManualPreviewCard(manualInput) {
                            addResults(listOf(manualInput))
                            manualInput = ""
                            Toast.makeText(context, "Added to list", Toast.LENGTH_SHORT).show()
                        }
                    }

                    if (isProcessing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 16.dp))

                    Spacer(Modifier.height(24.dp))

                    if (scanHistory.isNotEmpty()) {
                        Text("CURRENT BATCH", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        BatchResultViewer(
                            results = scanHistory,
                            currentIndex = currentIndex,
                            cache = barcodeCache,
                            onNext = { if (currentIndex < scanHistory.size - 1) currentIndex++ },
                            onPrev = { if (currentIndex > 0) currentIndex-- },
                            onDelete = {
                                val code = scanHistory[currentIndex].code
                                scanHistory.removeAt(currentIndex)
                                barcodeCache.remove(code)
                                if (currentIndex >= scanHistory.size && scanHistory.isNotEmpty()) currentIndex--
                            }
                        )
                    } else if (!isProcessing && manualInput.length != 13) {
                        EmptyScannerState()
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }

            if (showCamera) {
                ScannerCameraOverlay(articleRegex, { addResults(listOf(it)) }, { scanningEnabled = false; showCamera = false }, scanHistory.size, scanningEnabled)
            }
        }
    }
}

@Composable
fun BatchResultViewer(results: List<ScanResult>, currentIndex: Int, cache: Map<String, Bitmap>, onNext: () -> Unit, onPrev: () -> Unit, onDelete: () -> Unit) {
    val item = results[currentIndex]
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${currentIndex + 1} / ${results.size}", fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.padding(top = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Box {
                IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f))
                }
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    cache[item.code]?.let { Image(it.asImageBitmap(), null, Modifier.height(110.dp).fillMaxWidth()) }
                    Spacer(Modifier.height(24.dp))
                    Text(item.code, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Row(modifier = Modifier.padding(top = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPrev, enabled = currentIndex > 0, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ChevronLeft, null) }
            Button(onClick = onNext, enabled = currentIndex < results.size - 1, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ChevronRight, null) }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun ArticleCameraProcessor(regex: Regex, onDetected: (String) -> Unit, scanningEnabled: Boolean) {
    val owner = LocalLifecycleOwner.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var lastAnalyzed by remember { mutableLongStateOf(0L) }
    val currentScanningEnabled by rememberUpdatedState(scanningEnabled)

    AndroidView(factory = { ctx ->
        val view = PreviewView(ctx)
        ProcessCameraProvider.getInstance(ctx).addListener({
            val provider = ProcessCameraProvider.getInstance(ctx).get()
            val preview = Preview.Builder().build().apply { setSurfaceProvider(view.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                if (!currentScanningEnabled) { proxy.close(); return@setAnalyzer }
                val now = System.currentTimeMillis()
                if (now - lastAnalyzed >= 300) {
                    proxy.image?.let { img ->
                        recognizer.process(InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)).addOnSuccessListener { vt ->
                            if (currentScanningEnabled) {
                                vt.textBlocks.forEach { block ->
                                    val clean = block.text.replace(Regex("[\\s\\.\\-]"), "").uppercase()
                                    regex.find(clean)?.let {
                                        lastAnalyzed = now
                                        onDetected(it.value)
                                    }
                                }
                            }
                        }.addOnCompleteListener { proxy.close() }
                    } ?: proxy.close()
                } else proxy.close()
            }
            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(ctx))
        view
    }, Modifier.fillMaxSize())
}

@Composable
fun ScannerCameraOverlay(regex: Regex, onDetected: (String) -> Unit, onClose: () -> Unit, count: Int, scanningEnabled: Boolean) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ArticleCameraProcessor(regex, onDetected, scanningEnabled)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(300.dp, 180.dp).border(2.dp, Color.White.copy(0.4f), RoundedCornerShape(24.dp)))
        }
        Surface(Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(28.dp)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$count ITEMS IN BATCH", fontWeight = FontWeight.Black)
                Button(onClick = onClose) { Text("FINISH") }
            }
        }
    }
}

fun generateArticleBarcode(text: String): Bitmap? {
    return try {
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.CODE_128, 600, 240)
        val bitmap = Bitmap.createBitmap(600, 240, Bitmap.Config.ARGB_8888)
        for (x in 0 until 600) for (y in 0 until 240) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
        bitmap
    } catch (e: Exception) { null }
}

@Composable
fun ScannerActionCard(modifier: Modifier, title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(100.dp), shape = RoundedCornerShape(24.dp), color = color.copy(0.08f), border = BorderStroke(1.dp, color.copy(0.2f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(32.dp), color)
            Text(title, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 14.sp)
        }
    }
}

@Composable
fun ManualPreviewCard(code: String, onAdd: () -> Unit) {
    var previewBitmap by remember(code) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(code) { withContext(Dispatchers.Default) { previewBitmap = generateArticleBarcode(code) } }
    Card(modifier = Modifier.padding(top = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f))) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            previewBitmap?.let { Image(it.asImageBitmap(), null, Modifier.height(80.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(8.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(code, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("SAVE TO BATCH") }
        }
    }
}

@Composable
fun EmptyScannerState() {
    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.QrCodeScanner, null, Modifier.size(80.dp), Color.LightGray)
        Text("No Articles Scanned", fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}

private fun performOcrOnGallery(context: Context, uri: Uri, regex: Regex, onComplete: (List<String>) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ -> d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
            } else MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { visionText ->
                val detected = mutableSetOf<String>()
                visionText.textBlocks.forEach { block ->
                    val clean = block.text.replace(Regex("[\\s\\.\\-]"), "").uppercase()
                    regex.findAll(clean).forEach { detected.add(it.value) }
                }
                onComplete(detected.toList())
            }
        } catch (e: Exception) { onComplete(emptyList()) }
    }
}