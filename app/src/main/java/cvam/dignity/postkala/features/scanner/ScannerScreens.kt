package cvam.dignity.postkala.features.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

private data class OcrBarcodeItem(
    val number: String,
    val barcode: Bitmap
)

private val ocrArticlePatterns = listOf(
    Regex("""[A-Z]{2}[0-9]{9}[A-Z]{2}"""),
    Regex("""BOX[0-9]{10}""")
)

@Composable
fun OcrBarcodeScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val results = remember { mutableStateListOf<OcrBarcodeItem>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    var showScanner by remember { mutableStateOf(false) }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraPermissionGranted = granted
            if (granted) showScanner = true
        }

    // Launch camera automatically when opened
    LaunchedEffect(Unit) {
        if (cameraPermissionGranted) {
            showScanner = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun openScanner() {
        if (cameraPermissionGranted) {
            showScanner = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "OCR + BARCODE",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Postal number scanner",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(onClick = { openScanner() }) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Scan"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No numbers scanned yet",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Scan a postal article number",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { openScanner() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("START SCANNING")
                }
            } else {
                Text(
                    "${results.size} ITEM${if (results.size == 1) "" else "S"} IN BATCH",
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                val item = results[currentIndex]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${currentIndex + 1} / ${results.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(16.dp))

                        BasicTextField(
                            value = item.number,
                            onValueChange = { newValue ->
                                val upper = newValue.uppercase()
                                if (upper.length <= 13) {
                                    val newBmp = generateBarcode(upper)
                                    if (newBmp != null) {
                                        results[currentIndex] = item.copy(number = upper, barcode = newBmp)
                                    } else {
                                        results[currentIndex] = item.copy(number = upper)
                                    }
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "BARCODE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(10.dp))

                        Image(
                            bitmap = item.barcode.asImageBitmap(),
                            contentDescription = "Barcode",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = currentIndex > 0,
                        onClick = { currentIndex-- }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous"
                        )
                    }

                    IconButton(
                        onClick = {
                            results.removeAt(currentIndex)
                            if (results.isEmpty()) {
                                currentIndex = 0
                            } else if (currentIndex >= results.size) {
                                currentIndex = results.lastIndex
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }

                    IconButton(
                        enabled = currentIndex < results.lastIndex,
                        onClick = { currentIndex++ }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next"
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { openScanner() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("SCAN MORE")
                }
            }
        }
    }

    if (showScanner) {
        CameraScannerDialog(
            patterns = ocrArticlePatterns,
            title = "Scan Article Number",
            onDetected = { codes: List<String> ->
                codes.forEach { number: String ->
                    if (results.none { it.number == number }) {
                        generateBarcode(number)?.let { barcode ->
                            results.add(
                                OcrBarcodeItem(
                                    number = number,
                                    barcode = barcode
                                )
                            )
                        }
                    }
                }

                if (results.isNotEmpty()) {
                    currentIndex = results.lastIndex
                }
            },
            onDismiss = {
                showScanner = false
            }
        )
    }
}

private fun generateBarcode(value: String): Bitmap? {
    if (value.isEmpty()) return null
    return try {
        val matrix = MultiFormatWriter().encode(
            value,
            BarcodeFormat.CODE_128,
            700,
            220
        )

        val bitmap = Bitmap.createBitmap(
            700,
            220,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until 700) {
            for (y in 0 until 220) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix.get(x, y)) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                )
            }
        }

        bitmap
    } catch (_: Exception) {
        null
    }
}