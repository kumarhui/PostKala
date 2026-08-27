package cvam.dignity.postkala.features.aadhaar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import cvam.dignity.postkala.features.scanner.CameraScannerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AadhaarScan(val number: String, val timestamp: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AadhaarStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val aadhaarRegex = remember { Regex("[0-9]{12}") }

    var historyList by remember { mutableStateOf(getAadhaarHistory(context)) }
    var currentIndex by remember { mutableIntStateOf(0) }

    var manualInput by remember { mutableStateOf("") }
    var showLiveCamera by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val currentDisplayNumber = if (historyList.isNotEmpty() && currentIndex < historyList.size) {
        historyList[currentIndex].number
    } else ""

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showLiveCamera = true
        else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    // Auto-launch camera
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showLiveCamera = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Live update QR Code for the currently viewed item
    LaunchedEffect(currentDisplayNumber) {
        if (currentDisplayNumber.length == 12) {
            qrBitmap = withContext(Dispatchers.Default) {
                val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><PrintLetterBarcodeData uid=\"$currentDisplayNumber\"/>"
                generateAadhaarQrCode(xml)
            }
        } else {
            qrBitmap = null
        }
    }

    BackHandler {
        if (showLiveCamera) showLiveCamera = false else onBack()
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
                    .padding(16.dp)
            ) {

                // Fallback Scan Button
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            showLiveCamera = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("SCAN AADHAAR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(24.dp))

                // Manual Entry Component
                Text(
                    "MANUAL ENTRY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualInput,
                    onValueChange = {
                        val clean = it.filter { char -> char.isDigit() }
                        if (clean.length <= 12) manualInput = clean
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type 12-digit UID...", color = Color.LightGray) },
                    leadingIcon = {
                        Icon(Icons.Outlined.QrCode, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                AnimatedVisibility(visible = manualInput.length == 12) {
                    Button(
                        onClick = {
                            saveToHistory(context, manualInput)
                            historyList = getAadhaarHistory(context)
                            currentIndex = 0
                            manualInput = ""
                            Toast.makeText(context, "Added to batch", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("SAVE TO BATCH")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Batch Result Viewer
                if (historyList.isNotEmpty()) {
                    Text(
                        "CURRENT BATCH",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))

                    AadhaarBatchResultViewer(
                        results = historyList,
                        currentIndex = currentIndex,
                        qrBitmap = qrBitmap,
                        onNext = { if (currentIndex < historyList.size - 1) currentIndex++ },
                        onPrev = { if (currentIndex > 0) currentIndex-- },
                        onDelete = {
                            val code = historyList[currentIndex].number
                            deleteFromHistory(context, code)
                            historyList = getAadhaarHistory(context)
                            if (currentIndex >= historyList.size && historyList.isNotEmpty()) currentIndex--
                        },
                        onCodeChange = { newCode ->
                            val mutableList = historyList.toMutableList()
                            mutableList[currentIndex] = AadhaarScan(newCode, historyList[currentIndex].timestamp)
                            historyList = mutableList

                            val prefs = context.getSharedPreferences("postkala_aadhaar", Context.MODE_PRIVATE)
                            prefs.edit().putString("scans", mutableList.joinToString(";") { "${it.number}|${it.timestamp}" }).apply()
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString(currentDisplayNumber))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    EmptyScannerState("No Aadhaar Scanned")
                }

                Spacer(Modifier.height(40.dp))
            }

            if (showLiveCamera) {
                CameraScannerDialog(
                    patterns = listOf(aadhaarRegex),
                    title = "Scan Aadhaar",
                    onDetected = { codes ->
                        val detected = codes.firstOrNull()
                        if (detected != null) {
                            saveToHistory(context, detected)
                            historyList = getAadhaarHistory(context)
                            currentIndex = 0
                            showLiveCamera = false
                        }
                    },
                    onDismiss = { showLiveCamera = false }
                )
            }
        }
    }
}

@Composable
fun AadhaarBatchResultViewer(
    results: List<AadhaarScan>,
    currentIndex: Int,
    qrBitmap: Bitmap?,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onDelete: () -> Unit,
    onCodeChange: (String) -> Unit,
    onCopy: () -> Unit
) {
    val item = results[currentIndex]

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${currentIndex + 1} / ${results.size}", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 8.dp
        ) {
            Box {
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
                    Spacer(Modifier.height(24.dp))

                    PostKalaAadhaarInput(
                        value = item.number,
                        onValueChange = {
                            val clean = it.filter { char -> char.isDigit() }
                            if (clean.length <= 12) onCodeChange(clean)
                        },
                        onCopy = onCopy
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (qrBitmap != null) {
            ModernQrDisplay(qrBitmap, item.number)
        }

        Row(
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onPrev, enabled = currentIndex > 0, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ChevronLeft, null)
            }
            Button(onClick = onNext, enabled = currentIndex < results.size - 1, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ChevronRight, null)
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
            .border(
                1.dp,
                if (isFocused) Color.White else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).onFocusChanged { isFocused = it.isFocused }
            )
            if (value.length == 12) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ModernQrDisplay(bitmap: Bitmap, uid: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(uid.chunked(4).joinToString(" "), fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}

@Composable
fun EmptyScannerState(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.QrCodeScanner, null, Modifier.size(80.dp), Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text(message, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
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

private fun deleteFromHistory(context: Context, number: String) {
    val prefs = context.getSharedPreferences("postkala_aadhaar", Context.MODE_PRIVATE)
    val historyStr = prefs.getString("scans", "") ?: ""
    val list = historyStr.split(";").toMutableList()
    list.removeAll { it.startsWith(number) }
    prefs.edit().putString("scans", list.joinToString(";")).apply()
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