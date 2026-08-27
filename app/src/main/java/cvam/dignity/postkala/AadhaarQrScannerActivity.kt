package cvam.dignity.postkala.features

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import cvam.dignity.postkala.AppEasing

data class AadhaarScan(val number: String, val timestamp: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AadhaarStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var aadhaarNumber by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showLiveCamera by remember { mutableStateOf(false) }
    val aadhaarRegex = remember { Regex("[0-9]{12}") }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var historyList by remember { mutableStateOf(getAadhaarHistory(context)) }
    var showFullHistory by remember { mutableStateOf(false) }

    // ANIMATION: State Entry visibility trigger
    var entryVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entryVisible = true }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showLiveCamera = true
        else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    BackHandler {
        if (showLiveCamera) showLiveCamera = false else onBack()
    }

    val runRecognition = { image: InputImage ->
        isProcessing = true
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
            .addOnSuccessListener { visionText ->
                val aadhaar = findAadhaarNumber(visionText, aadhaarRegex)
                if (aadhaar != null) {
                    aadhaarNumber = aadhaar
                    saveToHistory(context, aadhaar)
                    historyList = getAadhaarHistory(context)
                } else {
                    Toast.makeText(context, "No valid Aadhaar found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnCompleteListener { isProcessing = false }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            if (mimeType?.startsWith("image/") == true) {
                runRecognition(InputImage.fromFilePath(context, it))
            } else if (mimeType == "application/pdf") {
                scope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) { isProcessing = true }
                    try {
                        val pfd = context.contentResolver.openFileDescriptor(it, "r")!!
                        val renderer = PdfRenderer(pfd)
                        val page = renderer.openPage(0)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderer.close()
                        withContext(Dispatchers.Main) { runRecognition(InputImage.fromBitmap(bitmap, 0)) }
                    } catch (e: Exception) { e.printStackTrace() } finally {
                        withContext(Dispatchers.Main) { isProcessing = false }
                    }
                }
            }
        }
    }

    LaunchedEffect(aadhaarNumber) {
        if (aadhaarNumber.length == 12) {
            qrBitmap = withContext(Dispatchers.Default) {
                val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><PrintLetterBarcodeData uid=\"$aadhaarNumber\"/>"
                generateAadhaarQrCode(xml)
            }
        } else { qrBitmap = null }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AADHAAR STUDIO", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { if (showLiveCamera) showLiveCamera = false else onBack() }) {
                        Icon(if (showLiveCamera) Icons.Default.Close else Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape))

                // ANIMATION: State Entry Growth for the Digital Identity Card
                AnimatedVisibility(
                    visible = entryVisible,
                    enter = scaleIn(initialScale = 0.85f, animationSpec = tween(500, easing = AppEasing)) + fadeIn(animationSpec = tween(500))
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .background(Brush.verticalGradient(listOf(Color(0xFF4A4AFF), Color(0xFF6C63FF))))
                                .padding(24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, null, tint = Color.White.copy(alpha = 0.8f))
                                Spacer(Modifier.width(12.dp))
                                Text("UIDAI Digital Identity", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(32.dp))
                            PostKalaAadhaarInput(value = aadhaarNumber, onValueChange = {
                                if (it.length <= 12) {
                                    aadhaarNumber = it
                                    if (it.length == 12) {
                                        saveToHistory(context, it)
                                        historyList = getAadhaarHistory(context)
                                    }
                                }
                            }, onCopy = {
                                clipboard.setText(AnnotatedString(aadhaarNumber))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                }

                // ANIMATION: Shared Axis (Staggered Entry) for Action Cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val buttonEntryAnim = remember { MutableTransitionState(false) }.apply { targetState = entryVisible }

                    AnimatedVisibility(
                        visibleState = buttonEntryAnim,
                        enter = scaleIn(initialScale = 0.7f, animationSpec = tween(600, delayMillis = 100, easing = AppEasing)) + fadeIn(),
                        modifier = Modifier.weight(1f)
                    ) {
                        AadhaarActionCard("Live Cam", Icons.Default.PhotoCamera, Color(0xFFE3F2FD), Color(0xFF0D47A1)) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showLiveCamera = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visibleState = buttonEntryAnim,
                        enter = scaleIn(initialScale = 0.7f, animationSpec = tween(600, delayMillis = 200, easing = AppEasing)) + fadeIn(),
                        modifier = Modifier.weight(1f)
                    ) {
                        AadhaarActionCard("Gallery", Icons.Default.Image, Color(0xFFFFF3E0), Color(0xFFE65100)) {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }
                    }
                }

                // ANIMATION: Parent-to-Child Shared Axis (Z-axis scale up) for QR
                AnimatedVisibility(
                    visible = qrBitmap != null,
                    enter = scaleIn(initialScale = 0.8f, animationSpec = tween(500, easing = AppEasing)) + fadeIn(),
                    exit = scaleOut(targetScale = 0.8f) + fadeOut()
                ) {
                    qrBitmap?.let { ModernQrDisplay(it, aadhaarNumber) }
                }

                if (historyList.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("RECENT SCANS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                            if (historyList.size > 4) {
                                TextButton(onClick = { showFullHistory = !showFullHistory }) {
                                    Text(if (showFullHistory) "Show Less" else "View All")
                                }
                            }
                        }

                        // ANIMATION: List Scrolling Stagger with physical grounding
                        val displayList = if (showFullHistory) historyList else historyList.take(4)
                        displayList.forEachIndexed { index, scan ->
                            key(scan.timestamp) {
                                val itemTransition = remember { MutableTransitionState(false) }.apply { targetState = entryVisible }
                                AnimatedVisibility(
                                    visibleState = itemTransition,
                                    enter = slideInHorizontally(tween(500, delayMillis = 300 + (index * 50), easing = AppEasing)) { it / 4 } + fadeIn(),
                                ) {
                                    AadhaarHistoryItem(scan) { aadhaarNumber = scan.number }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }

            if (showLiveCamera) {
                AadhaarCameraOverlay(
                    onDetected = { detected ->
                        aadhaarNumber = detected
                        saveToHistory(context, detected)
                        historyList = getAadhaarHistory(context)
                        showLiveCamera = false
                    },
                    onClose = { showLiveCamera = false }
                )
            }
        }
    }
}

@Composable
fun PostKalaAadhaarInput(value: String, onValueChange: (String) -> Unit, onCopy: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.1f))
            .border(1.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).onFocusChanged { isFocused = it.isFocused }
            )
            if (value.length == 12) {
                IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, null, tint = Color.White) }
            }
        }
    }
}

@Composable
fun AadhaarCameraOverlay(onDetected: (String) -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AadhaarCameraProcessor(onDetected)
        Box(Modifier.fillMaxSize().border(2.dp, Color.White.copy(0.5f)), contentAlignment = Alignment.Center) {
            Text("Align Aadhaar UID Here", color = Color.White, modifier = Modifier.background(Color.Black.copy(0.5f)).padding(8.dp))
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun AadhaarCameraProcessor(onDetected: (String) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val regex = remember { Regex("[0-9]{12}") }

    AndroidView(factory = { ctx ->
        val view = PreviewView(ctx)
        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().apply { setSurfaceProvider(view.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                proxy.image?.let { mediaImage ->
                    val img = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    recognizer.process(img).addOnSuccessListener { vt ->
                        vt.textBlocks.forEach { block ->
                            val cleanText = block.text.replace("\\s".toRegex(), "")
                            val match = regex.find(cleanText)
                            if (match != null) onDetected(match.value)
                        }
                    }.addOnCompleteListener { proxy.close() }
                } ?: proxy.close()
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) { e.printStackTrace() }
        }, ContextCompat.getMainExecutor(ctx))
        view
    }, Modifier.fillMaxSize())
}

private fun findAadhaarNumber(visionText: Text, regex: Regex): String? {
    for (block in visionText.textBlocks) {
        val clean = block.text.replace("\\s".toRegex(), "")
        if (regex.containsMatchIn(clean)) return regex.find(clean)?.value
    }
    return null
}

private fun getAadhaarHistory(context: Context): List<AadhaarScan> {
    val prefs = context.getSharedPreferences("postkala_aadhaar", Context.MODE_PRIVATE)
    val historyStr = prefs.getString("scans", "") ?: ""
    if (historyStr.isEmpty()) return emptyList()
    return historyStr.split(";").mapNotNull { entry ->
        val parts = entry.split("|")
        if (parts.size == 2) AadhaarScan(parts[0], parts[1].toLongOrNull() ?: 0L) else null
    }
}

private fun saveToHistory(context: Context, number: String) {
    val prefs = context.getSharedPreferences("postkala_aadhaar", Context.MODE_PRIVATE)
    val historyStr = prefs.getString("scans", "") ?: ""
    val currentTime = System.currentTimeMillis()
    val list = if (historyStr.isEmpty()) mutableListOf() else historyStr.split(";").toMutableList()
    list.removeAll { it.startsWith(number) }
    list.add(0, "$number|$currentTime")
    prefs.edit().putString("scans", list.take(100).joinToString(";")).apply()
}

private fun generateAadhaarQrCode(text: String): Bitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) for (y in 0 until 512) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
        bmp
    } catch (e: Exception) { null }
}

@Composable
fun AadhaarActionCard(title: String, icon: ImageVector, bgColor: Color, tint: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier.height(110.dp).clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), color = bgColor) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
fun AadhaarHistoryItem(scan: AadhaarScan, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(scan.number.chunked(4).joinToString(" "), fontWeight = FontWeight.Bold)
                Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(scan.timestamp)), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ModernQrDisplay(bitmap: Bitmap, uid: String) {
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(200.dp))
            Spacer(Modifier.height(16.dp))
            Text(uid.chunked(4).joinToString(" "), fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}