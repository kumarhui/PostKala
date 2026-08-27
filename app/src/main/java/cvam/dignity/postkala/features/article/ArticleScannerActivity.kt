package cvam.dignity.postkala.features.article

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import cvam.dignity.postkala.features.scanner.CameraScannerDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Launch camera automatically when opened
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scanningEnabled = true
            showCamera = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val addResults: (List<String>) -> Unit = { codes ->
        if (scanningEnabled) {
            scope.launch {
                codes.forEach { code ->
                    val upperCode = code.uppercase()
                    if (upperCode != lastScannedCode && scanHistory.none { it.code == upperCode }) {
                        scanHistory.add(ScanResult(code = upperCode))
                        lastScannedCode = upperCode
                        withContext(Dispatchers.Default) {
                            val bmp = generateArticleBarcode(upperCode, 600, 240)
                            if (bmp != null) barcodeCache[upperCode] = bmp
                        }
                    }
                }
                if (scanHistory.isNotEmpty()) currentIndex = scanHistory.size - 1
            }
        }
    }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                isProcessing = true
                performOcrOnGallery(context, it, articleRegex) { results ->
                    addResults(results)
                    isProcessing = false
                }
            }
        }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
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

            // Screen renders without animations
            Column(
                Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScannerActionCard(
                        Modifier.weight(1f),
                        "Live Scan",
                        Icons.Default.PhotoCamera,
                        MaterialTheme.colorScheme.primary
                    ) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            scanningEnabled = true
                            showCamera = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    ScannerActionCard(
                        Modifier.weight(1f),
                        "Gallery",
                        Icons.Default.Collections,
                        Color(0xFF00C853)
                    ) {
                        galleryLauncher.launch("image/*")
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "MANUAL ENTRY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { if (it.length <= 13) manualInput = it.uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type 13-digit code...", color = Color.LightGray) },
                    leadingIcon = {
                        Icon(Icons.Outlined.QrCode, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )

                if (manualInput.length == 13) {
                    ManualPreviewCard(manualInput) {
                        addResults(listOf(manualInput))
                        manualInput = ""
                        Toast.makeText(context, "Added to list", Toast.LENGTH_SHORT).show()
                    }
                }

                if (isProcessing) LinearProgressIndicator(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )

                Spacer(Modifier.height(24.dp))

                if (scanHistory.isNotEmpty()) {
                    Text(
                        "CURRENT BATCH",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray
                    )
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
                        },
                        onCodeChange = { newCode ->
                            val oldCode = scanHistory[currentIndex].code
                            scanHistory[currentIndex] = scanHistory[currentIndex].copy(code = newCode)
                            scope.launch(Dispatchers.Default) {
                                val bmp = generateArticleBarcode(newCode, 600, 240)
                                if (bmp != null) {
                                    barcodeCache[newCode] = bmp
                                    if (oldCode != newCode) {
                                        barcodeCache.remove(oldCode)
                                    }
                                }
                            }
                        }
                    )
                } else if (!isProcessing && manualInput.length != 13) {
                    EmptyScannerState()
                }
                Spacer(Modifier.height(40.dp))
            }

            if (showCamera) {
                CameraScannerDialog(
                    patterns = listOf(articleRegex),
                    title = "Scan Article Number",
                    onDetected = { codes: List<String> ->
                        addResults(codes)
                    },
                    onDismiss = {
                        scanningEnabled = false
                        showCamera = false
                    }
                )
            }
        }
    }
}

@Composable
fun BatchResultViewer(
    results: List<ScanResult>,
    currentIndex: Int,
    cache: Map<String, Bitmap>,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onDelete: () -> Unit,
    onCodeChange: (String) -> Unit
) {
    val item = results[currentIndex]

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${currentIndex + 1} / ${results.size}", fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f))
                }
                Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    cache[item.code]?.let {
                        Image(
                            it.asImageBitmap(),
                            null,
                            Modifier.height(110.dp).fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    BasicTextField(
                        value = item.code,
                        onValueChange = { newValue ->
                            val upper = newValue.uppercase()
                            if (upper.length <= 13) {
                                onCodeChange(upper)
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(0.04f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    innerTextField()
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Code",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPrev,
                enabled = currentIndex > 0,
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.ChevronLeft, null) }
            Button(
                onClick = onNext,
                enabled = currentIndex < results.size - 1,
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.ChevronRight, null) }
        }
    }
}

@Composable
fun ScannerActionCard(modifier: Modifier, title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(24.dp),
        color = color.copy(0.08f),
        border = BorderStroke(1.dp, color.copy(0.2f))
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(32.dp), color)
            Text(title, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 14.sp)
        }
    }
}

@Composable
fun ManualPreviewCard(code: String, onAdd: () -> Unit) {
    var previewBitmap by remember(code) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(code) {
        withContext(Dispatchers.Default) {
            previewBitmap = generateArticleBarcode(code, 600, 240)
        }
    }
    Card(
        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
        )
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            previewBitmap?.let {
                Image(
                    it.asImageBitmap(),
                    null,
                    Modifier.height(80.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White).padding(8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                code,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("SAVE TO BATCH") }
        }
    }
}

@Composable
fun EmptyScannerState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

@SuppressLint("UnsafeOptInUsageError")
private fun generateArticleBarcode(value: String, width: Int = 600, height: Int = 240): Bitmap? {
    if (value.isEmpty()) return null
    return try {
        val matrix = MultiFormatWriter().encode(value, BarcodeFormat.CODE_128, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}