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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudioDialogMode {
    ACTION_SELECTION,
    RESULTS_VIEW,
    MANUAL_ENTRY
}

@Composable
fun StudioScannerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val aadhaarRegex = remember { Regex("""[0-9]{12}""") }
    val articleRegex = remember { Regex("""[A-Z]{2}[0-9]{9}[A-Z]{2}""") }
    val boxRegex = remember { Regex("""BOX[0-9]{10}""") }

    val scanHistory = remember { mutableStateListOf<UnifiedScanResult>() }
    val codeImageCache = remember { mutableStateMapOf<String, Bitmap>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var currentMode by remember { mutableStateOf(StudioDialogMode.ACTION_SELECTION) }
    var showCameraDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraDialog = true
        } else {
            Toast.makeText(context, "Camera permission required to scan", Toast.LENGTH_SHORT).show()
        }
    }

    val processAndAddCodes: (List<String>) -> Unit = { rawCodes ->
        scope.launch {
            rawCodes.forEach { raw ->
                val clean = raw.trim().uppercase()
                if (clean.isNotEmpty()) {
                    val detectedType = if (clean.length == 12 && clean.all { it.isDigit() }) {
                        ScannedCodeType.AADHAAR_UID
                    } else {
                        ScannedCodeType.ARTICLE_BARCODE
                    }

                    val emptyIndex = scanHistory.indexOfFirst { it.code.isEmpty() }
                    if (emptyIndex != -1) {
                        scanHistory[emptyIndex] = UnifiedScanResult(code = clean, type = detectedType)
                    } else if (scanHistory.none { it.code == clean }) {
                        scanHistory.add(UnifiedScanResult(code = clean, type = detectedType))
                    }

                    withContext(Dispatchers.Default) {
                        val bmp = generateCodeImageInternal(clean, detectedType)
                        if (bmp != null) codeImageCache[clean] = bmp
                    }
                }
            }
            if (scanHistory.any { it.code.isNotEmpty() }) {
                currentIndex = 0
                currentMode = StudioDialogMode.RESULTS_VIEW
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "STUDIO TOOL",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = when (currentMode) {
                                    StudioDialogMode.ACTION_SELECTION -> "Choose scan or manual entry"
                                    StudioDialogMode.RESULTS_VIEW -> "Scanned codes & barcodes"
                                    StudioDialogMode.MANUAL_ENTRY -> "Manual code creation"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(18.dp))

                when (currentMode) {
                    StudioDialogMode.ACTION_SELECTION -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            showCameraDialog = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("SCAN WITH CAMERA", fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = {
                                        if (scanHistory.isEmpty()) {
                                            scanHistory.add(UnifiedScanResult(code = "", type = ScannedCodeType.ARTICLE_BARCODE))
                                        }
                                        currentIndex = 0
                                        currentMode = StudioDialogMode.MANUAL_ENTRY
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("MANUAL CODE ENTRY", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    StudioDialogMode.RESULTS_VIEW -> {
                        if (scanHistory.isNotEmpty() && currentIndex in scanHistory.indices) {
                            val currentItem = scanHistory[currentIndex]

                            StudioCodeCardInternal(
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
                                            val bmp = generateCodeImageInternal(clean, newType)
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
                                        scanHistory.clear()
                                        codeImageCache.clear()
                                        currentMode = StudioDialogMode.ACTION_SELECTION
                                    }
                                }
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { if (currentIndex > 0) currentIndex-- },
                                    enabled = currentIndex > 0,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scanHistory.clear()
                                        codeImageCache.clear()
                                        currentMode = StudioDialogMode.ACTION_SELECTION
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color(0xFFEF4444))
                                }

                                OutlinedButton(
                                    onClick = { if (currentIndex < scanHistory.size - 1) currentIndex++ },
                                    enabled = currentIndex < scanHistory.size - 1,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        showCameraDialog = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("SCAN MORE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    StudioDialogMode.MANUAL_ENTRY -> {
                        if (scanHistory.isNotEmpty()) {
                            val currentItem = scanHistory[0]

                            StudioCodeCardInternal(
                                item = currentItem,
                                currentIndex = 0,
                                totalItems = 1,
                                cachedBitmap = codeImageCache[currentItem.code],
                                onCodeChange = { newCode ->
                                    val clean = newCode.uppercase().trim()
                                    val newType = if (clean.length == 12 && clean.all { it.isDigit() }) {
                                        ScannedCodeType.AADHAAR_UID
                                    } else {
                                        ScannedCodeType.ARTICLE_BARCODE
                                    }

                                    val oldCode = currentItem.code
                                    scanHistory[0] = currentItem.copy(code = clean, type = newType)

                                    scope.launch(Dispatchers.Default) {
                                        if (clean.isNotEmpty()) {
                                            val bmp = generateCodeImageInternal(clean, newType)
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
                                    val targetCode = scanHistory[0].code
                                    scanHistory[0] = UnifiedScanResult(code = "", type = ScannedCodeType.ARTICLE_BARCODE)
                                    codeImageCache.remove(targetCode)
                                }
                            )

                            Spacer(Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = {
                                    scanHistory.clear()
                                    codeImageCache.clear()
                                    currentMode = StudioDialogMode.ACTION_SELECTION
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("BACK TO MODES", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCameraDialog) {
        ContinuousStreamScannerDialog(
            patterns = listOf(aadhaarRegex, articleRegex, boxRegex),
            existingCodes = scanHistory.map { it.code },
            title = "Continuous Scanner",
            onDetected = { codes: List<String> ->
                processAndAddCodes(codes)
            },
            onDismiss = {
                showCameraDialog = false
            }
        )
    }
}

@Composable
private fun StudioCodeCardInternal(
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
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4338CA),
                            Color(0xFF6D28D9)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DIGITAL CODE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currentIndex + 1} / $totalItems",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            StudioInputFieldInternal(
                value = item.code,
                onValueChange = onCodeChange,
                onCopy = {
                    if (item.code.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(item.code))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            val badgeText = if (item.type == ScannedCodeType.AADHAAR_UID) {
                "12-DIGIT UID"
            } else {
                "ARTICLE BARCODE"
            }

            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = CircleShape
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (cachedBitmap != null && item.code.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.type == ScannedCodeType.AADHAAR_UID) {
                                Image(
                                    bitmap = cachedBitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(135.dp),
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center
                                )
                            } else {
                                Image(
                                    bitmap = cachedBitmap.asImageBitmap(),
                                    contentDescription = "Barcode",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(85.dp)
                                        .padding(horizontal = 4.dp),
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val formattedDisplay = if (item.type == ScannedCodeType.AADHAAR_UID) {
                            item.code.chunked(4).joinToString(" ")
                        } else {
                            item.code
                        }

                        Text(
                            text = formattedDisplay,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.LightGray
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Scan or type to preview",
                            fontSize = 12.sp,
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
private fun StudioInputFieldInternal(
    value: String,
    onValueChange: (String) -> Unit,
    onCopy: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.12f))
            .border(
                width = 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
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
                    fontSize = 18.sp,
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
                                text = "Enter UID or Article Code...",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 12.sp,
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
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun generateCodeImageInternal(text: String, type: ScannedCodeType): Bitmap? {
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