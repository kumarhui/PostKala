package cvam.dignity.postkala.features.scanner

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@Composable
fun ContinuousStreamScannerDialog(
    patterns: List<Regex>,
    existingCodes: List<String>,
    title: String = "Continuous Scanner",
    onDetected: (codes: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Tracks all codes already present in current batch + newly detected in this session
    val sessionCaptured = remember {
        mutableStateListOf<String>().apply {
            addAll(existingCodes.filter { it.isNotBlank() })
        }
    }

    var lastDetectedCode by remember { mutableStateOf<String?>(null) }
    var detectedCount by remember { mutableIntStateOf(sessionCaptured.size) }

    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Map to count consecutive hits for candidate codes (anti-flicker stability filter)
    val hitCounts = remember { mutableMapOf<String, Int>() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            recognizer.close()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Stream active • Auto-deduplication ON",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Continuous Camera Viewport
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.90f)
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val providerFuture = ProcessCameraProvider.getInstance(ctx)

                                providerFuture.addListener({
                                    val provider = providerFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    analysis.setAnalyzer(executor) { imageProxy ->
                                        analyzeStreamFrame(
                                            imageProxy = imageProxy,
                                            recognizer = recognizer,
                                            patterns = patterns
                                        ) { frameCandidates ->
                                            val newUniqueCandidates = mutableListOf<String>()

                                            frameCandidates.forEach { candidate ->
                                                // If already saved in this session or history, reject immediately
                                                if (candidate !in sessionCaptured) {
                                                    val currentHits = (hitCounts[candidate] ?: 0) + 1
                                                    hitCounts[candidate] = currentHits

                                                    // Must be confirmed over at least 2 distinct frames to avoid misreads
                                                    if (currentHits >= 2) {
                                                        sessionCaptured.add(candidate)
                                                        newUniqueCandidates.add(candidate)
                                                        hitCounts.remove(candidate)
                                                    }
                                                }
                                            }

                                            if (newUniqueCandidates.isNotEmpty()) {
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    lastDetectedCode = newUniqueCandidates.last()
                                                    detectedCount = sessionCaptured.size
                                                    onDetected(newUniqueCandidates)
                                                }
                                            }
                                        }
                                    }

                                    try {
                                        provider.unbindAll()
                                        provider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis
                                        )
                                    } catch (_: Exception) {}
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Scan Bounds Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )

                        // Unique Count Badge
                        Surface(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.TopEnd),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$detectedCount Unique",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Real-time status text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                if (lastDetectedCode != null) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lastDetectedCode != null) {
                            "Captured: $lastDetectedCode"
                        } else {
                            "Point at barcodes or UID numbers..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lastDetectedCode != null) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Duplicates are filtered automatically. Tap DONE when finished.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeStreamFrame(
    imageProxy: ImageProxy,
    recognizer: TextRecognizer,
    patterns: List<Regex>,
    onCandidatesFound: (List<String>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        try { imageProxy.close() } catch (_: Exception) {}
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val normalized = visionText.text.uppercase().replace(Regex("[\\s.\\-:]"), "")
            val foundSet = mutableSetOf<String>()
            patterns.forEach { pattern ->
                pattern.findAll(normalized).forEach { match ->
                    foundSet.add(match.value)
                }
            }
            onCandidatesFound(foundSet.toList())
        }
        .addOnCompleteListener {
            try { imageProxy.close() } catch (_: Exception) {}
        }
}