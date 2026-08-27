package cvam.dignity.postkala.features.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScannedCodeType {
    AADHAAR_UID,
    ARTICLE_BARCODE
}

data class UnifiedScanResult(
    val code: String,
    val type: ScannedCodeType,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Regex patterns for automatic code detection
    val aadhaarRegex = remember { Regex("""[0-9]{12}""") }
    val articleRegex = remember { Regex("""[A-Z]{2}[0-9]{9}[A-Z]{2}""") }
    val boxRegex = remember { Regex("""BOX[0-9]{10}""") }

    // Scan history batch & cache
    val scanHistory = remember { mutableStateListOf<UnifiedScanResult>() }
    val codeImageCache = remember { mutableStateMapOf<String, Bitmap>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var showCamera by remember { mutableStateOf(false) }
    var scanningEnabled by remember { mutableStateOf(true) }

    // Ensure at least one blank entry exists on start for direct typing
    LaunchedEffect(Unit) {
        if (scanHistory.isEmpty()) {
            scanHistory.add(UnifiedScanResult(code = "", type = ScannedCodeType.ARTICLE_BARCODE))
        }
    }

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

    // Function to handle newly scanned or typed codes
    val processAndAddCodes: (List<String>) -> Unit = { rawCodes ->
        if (scanningEnabled) {
            scope.launch {
                rawCodes.forEach { raw ->
                    val clean = raw.trim().uppercase()
                    if (clean.isNotEmpty()) {
                        val detectedType = if (clean.length == 12 && clean.all { it.isDigit() }) {
                            ScannedCodeType.AADHAAR_UID
                        } else {
                            ScannedCodeType.ARTICLE_BARCODE
                        }

                        // Replace existing blank item if present, else append new
                        val emptyIndex = scanHistory.indexOfFirst { it.code.isEmpty() }
                        if (emptyIndex != -1) {
                            scanHistory[emptyIndex] = UnifiedScanResult(code = clean, type = detectedType)
                            currentIndex = emptyIndex
                        } else if (scanHistory.none { it.code == clean }) {
                            scanHistory.add(UnifiedScanResult(code = clean, type = detectedType))
                            currentIndex = scanHistory.size - 1
                        }

                        withContext(Dispatchers.Default) {
                            val bmp = generateCodeImage(clean, detectedType)
                            if (bmp != null) codeImageCache[clean] = bmp
                        }
                    }
                }
            }
        }
    }

    // Auto-launch camera when screen opens
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scanningEnabled = true
            showCamera = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("STUDIO SCANNER", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = { if (showCamera) showCamera = false else onBack() }) {
                        Icon(if (showCamera) Icons.Default.Close else Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Action Button
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            scanningEnabled = true
                            showCamera = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("SCAN CODE / UID", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(20.dp))

                // Unified Interactive Card Component
                if (scanHistory.isNotEmpty() && currentIndex in scanHistory.indices) {
                    val currentItem = scanHistory[currentIndex]

                    UnifiedCodeCard(
                        item = currentItem,
                        currentIndex = currentIndex,
                        totalItems = scanHistory.size,
                        cachedBitmap = codeImageCache[currentItem.code],
                        onCodeChange = { newCode ->
                            val clean = newCode.uppercase().trim()
                            val newType = if (clean.length == 12 && clean.all { it.isDigit() }) {
                                ScannedCodeType.AADHAAR_UID
                            } else {
                                ScannedCodeType.ARTICLE_BARCODE
                            }

                            val oldCode = currentItem.code
                            scanHistory[currentIndex] = currentItem.copy(code = clean, type = newType)

                            scope.launch(Dispatchers.Default) {
                                if (clean.isNotEmpty()) {
                                    val bmp = generateCodeImage(clean, newType)
                                    if (bmp != null) {
                                        codeImageCache[clean] = bmp
                                        if (oldCode != clean) codeImageCache.remove(oldCode)
                                    }
                                } else {
                                    codeImageCache.remove(oldCode)
                                }
                            }
                        },
                        onDelete = {
                            if (scanHistory.size > 1) {
                                val targetCode = scanHistory[currentIndex].code
                                scanHistory.removeAt(currentIndex)
                                codeImageCache.remove(targetCode)
                                if (currentIndex >= scanHistory.size) {
                                    currentIndex = scanHistory.size - 1
                                }
                            } else {
                                // Clear sole item
                                val targetCode = scanHistory[0].code
                                scanHistory[0] = UnifiedScanResult(code = "", type = ScannedCodeType.ARTICLE_BARCODE)
                                codeImageCache.remove(targetCode)
                            }
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Batch Navigation Controls (Icon-only PREV/NEXT)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentIndex > 0) currentIndex-- },
                            enabled = currentIndex > 0,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                        }

                        OutlinedButton(
                            onClick = {
                                if (scanHistory.isEmpty() || scanHistory.last().code.isNotEmpty()) {
                                    scanHistory.add(UnifiedScanResult(code = "", type = ScannedCodeType.ARTICLE_BARCODE))
                                }
                                currentIndex = scanHistory.size - 1
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add New")
                        }

                        OutlinedButton(
                            onClick = { if (currentIndex < scanHistory.size - 1) currentIndex++ },
                            enabled = currentIndex < scanHistory.size - 1,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }
                }

                Spacer(Modifier.height(30.dp))
            }

            // Shared Camera Scanner Modal
            if (showCamera) {
                CameraScannerDialog(
                    patterns = listOf(aadhaarRegex, articleRegex, boxRegex),
                    title = "Scan Aadhaar or Article",
                    onDetected = { codes: List<String> ->
                        processAndAddCodes(codes)
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
fun UnifiedCodeCard(
    item: UnifiedScanResult,
    currentIndex: Int,
    totalItems: Int,
    cachedBitmap: Bitmap?,
    onCodeChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4F46E5), // Indigo
                            Color(0xFF7C3AED)  // Purple
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "POSTKALA DIGITAL CODE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currentIndex + 1} / $totalItems",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Interactive Editable Code Field inside the Card
            EditableCodeInputField(
                value = item.code,
                onValueChange = onCodeChange,
                onCopy = {
                    if (item.code.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(item.code))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            // Code Format Pill Indicator
            val badgeText = if (item.type == ScannedCodeType.AADHAAR_UID) {
                "12-DIGIT UID (QR CODE)"
            } else {
                "ARTICLE CODE (BARCODE)"
            }

            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = CircleShape
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // Perfect Centered Visual Preview (QR Code or Barcode)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (cachedBitmap != null && item.code.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (item.type == ScannedCodeType.AADHAAR_UID) {
                            // Centered QR Display
                            Image(
                                bitmap = cachedBitmap.asImageBitmap(),
                                contentDescription = "Aadhaar QR Code",
                                modifier = Modifier.size(190.dp),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center
                            )
                        } else {
                            // Centered Barcode Display
                            Image(
                                bitmap = cachedBitmap.asImageBitmap(),
                                contentDescription = "Article Barcode",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .padding(horizontal = 8.dp),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        val formattedDisplay = if (item.type == ScannedCodeType.AADHAAR_UID) {
                            item.code.chunked(4).joinToString(" ")
                        } else {
                            item.code
                        }

                        Text(
                            text = formattedDisplay,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Empty state inside the preview card
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = Color.LightGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Type or scan to generate code",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditableCodeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onCopy: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.12f))
            .border(
                width = 1.5.dp,
                color = if (isFocused) Color.White else Color.White.copy(0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    val clean = newValue.uppercase().filter { it.isLetterOrDigit() }
                    if (clean.length <= 13) {
                        onValueChange(clean)
                    }
                },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = "ENTER 12-DIGIT UID OR 13-CHAR CODE...",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun generateCodeImage(text: String, type: ScannedCodeType): Bitmap? {
    if (text.isEmpty()) return null
    return try {
        if (type == ScannedCodeType.AADHAAR_UID) {
            val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><PrintLetterBarcodeData uid=\"$text\"/>"
            val bitMatrix = QRCodeWriter().encode(xml, BarcodeFormat.QR_CODE, 512, 512)
            val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp
        } else {
            val matrix = MultiFormatWriter().encode(text, BarcodeFormat.CODE_128, 600, 240)
            val bitmap = Bitmap.createBitmap(600, 240, Bitmap.Config.ARGB_8888)
            for (x in 0 until 600) {
                for (y in 0 until 240) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        }
    } catch (_: Exception) {
        null
    }
}