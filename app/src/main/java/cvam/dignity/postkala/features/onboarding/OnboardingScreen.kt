package cvam.dignity.postkala.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvam.dignity.postkala.core.AppPreferences

private const val REQUIRED_CODE = "909090"

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val appPrefs = remember { AppPreferences(context) }

    // Step 1: Mode Selection | Step 2: PIN Verification (only if Advanced is selected)
    var step by remember { mutableIntStateOf(1) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (step == 1) {
                // Step 1: Choose Experience
                Card(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Choose Experience",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Select your setup mode. Advanced features include Studio Scanner and Aadhaar Services.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
                        )

                        Button(
                            onClick = {
                                step = 2
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("ENABLE ADVANCED", fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                appPrefs.isAdvancedFeaturesEnabled = false
                                appPrefs.isOnboardingCompleted = true
                                onFinished()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("KEEP BASIC", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Step 2: 6-Digit PIN Verification for Advanced Mode
                Card(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                enteredPin = ""
                                pinError = false
                                step = 1
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Enter Access PIN",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "Enter the 6-digit authorization code to unlock advanced features.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                        )

                        BasicTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                    enteredPin = it
                                    pinError = false
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            decorationBox = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(6) { index ->
                                        val digit = enteredPin.getOrNull(index)
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .border(
                                                    width = 2.dp,
                                                    color = if (pinError) MaterialTheme.colorScheme.error
                                                    else if (index == enteredPin.length) MaterialTheme.colorScheme.primary
                                                    else Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = digit?.toString() ?: "",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        if (pinError) {
                            Text(
                                text = "Invalid PIN. Try again.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = {
                                if (enteredPin == REQUIRED_CODE) {
                                    appPrefs.isAdvancedFeaturesEnabled = true
                                    appPrefs.isOnboardingCompleted = true
                                    onFinished()
                                } else {
                                    pinError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = enteredPin.length == 6
                        ) {
                            Text("UNLOCK & CONTINUE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}