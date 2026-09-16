package cvam.dignity.postkala.features.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cvam.dignity.postkala.AdBanner
import cvam.dignity.postkala.core.AppPreferences
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val route: String? = null,
    val action: (() -> Unit)? = null
)

private fun buildWebRoute(title: String, url: String): String {
    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
    val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
    return "webview?title=$encodedTitle&url=$encodedUrl"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostKalaDashboard(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val appPrefs = remember { AppPreferences(context) }
    val showAadhaarServices = remember { mutableStateOf(false) }
    val showAppInfo = remember { mutableStateOf(false) }
    val showWhatsappDialog = remember { mutableStateOf(false) }

    val allItems = remember(appPrefs.isAdvancedFeaturesEnabled) {
        mutableListOf<DashboardItem>().apply {
            if (appPrefs.isAdvancedFeaturesEnabled) {
                add(
                    DashboardItem(
                        title = "Studio",
                        subtitle = "Scanner",
                        icon = Icons.Default.QrCodeScanner,
                        color = Color(0xFF6366F1),
                        route = "studio_scanner"
                    )
                )
            }

            add(
                DashboardItem(
                    title = "RPLI",
                    subtitle = "Calculator",
                    icon = Icons.Default.Calculate,
                    color = Color(0xFFF59E0B),
                    route = "rpli"
                )
            )
            add(
                DashboardItem(
                    title = "Self Service",
                    subtitle = "Portal",
                    icon = Icons.Default.Badge,
                    color = Color(0xFF0284C7),
                    route = buildWebRoute(
                        title = "Self Service Portal",
                        url = "https://app.indiapost.gov.in/employeeportal"
                    )
                )
            )
            add(
                DashboardItem(
                    title = "Karmayogi",
                    subtitle = "Training",
                    icon = Icons.Default.School,
                    color = Color(0xFF059669),
                    route = buildWebRoute(
                        title = "Dak Karmayogi",
                        url = "https://www.dakkarmayogi.gov.in/doptrg/index.php"
                    )
                )
            )
            add(
                DashboardItem(
                    title = "DSS App",
                    subtitle = "Download",
                    icon = Icons.Default.CloudDownload,
                    color = Color(0xFF7C3AED),
                    route = buildWebRoute(
                        title = "DSS App",
                        url = "https://drive.google.com/file/d/15TxcevWBon15SYF2ITd4kZoMPNj-XuQN/view?pli=1"
                    )
                )
            )
            add(
                DashboardItem(
                    title = "WhatsApp",
                    subtitle = "Direct Chat",
                    icon = Icons.Default.Chat,
                    color = Color(0xFF25D366),
                    action = {
                        showWhatsappDialog.value = true
                    }
                )
            )

            if (appPrefs.isAdvancedFeaturesEnabled) {
                add(
                    DashboardItem(
                        title = "Aadhaar",
                        subtitle = "Services",
                        icon = Icons.Default.AccountBox,
                        color = Color(0xFFEF4444),
                        action = {
                            showAadhaarServices.value = true
                        }
                    )
                )
            }

            add(
                DashboardItem(
                    title = "Info",
                    subtitle = "About App",
                    icon = Icons.Default.Info,
                    color = Color(0xFF64748B),
                    action = {
                        showAppInfo.value = true
                    }
                )
            )
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "POSTKALA",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Utility Hub",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            AdBanner()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Services",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(allItems) { item ->
                    CircularDashboardItem(
                        item = item,
                        onClick = {
                            item.route?.let { onNavigate(it) }
                            item.action?.invoke()
                        }
                    )
                }
            }
        }
    }

    if (showWhatsappDialog.value) {
        WhatsappDirectDialog(
            onDismiss = { showWhatsappDialog.value = false }
        )
    }

    if (showAadhaarServices.value) {
        AadhaarServicesDialog(
            onDismiss = { showAadhaarServices.value = false }
        )
    }

    if (showAppInfo.value) {
        AppInfoDialog(
            onDismiss = { showAppInfo.value = false }
        )
    }
}

@Composable
private fun CircularDashboardItem(
    item: DashboardItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = item.color.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = item.color,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = item.subtitle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WhatsappDirectDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Direct WhatsApp",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )

                Text(
                    text = "Start chat without saving contact",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                ModernDialogPhoneField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        if (input.length <= 10) {
                            phoneNumber = input.filter { it.isDigit() }
                        }
                    },
                    focusRequester = focusRequester
                )

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        openWhatsAppWithFallback(context, phoneNumber)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = phoneNumber.length == 10,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("OPEN CHAT", fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (phoneNumber.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                phoneNumber = ""
                                scope.launch { focusRequester.requestFocus() }
                            }
                        ) {
                            Text(
                                "Clear",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(Modifier.width(10.dp))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernDialogPhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Phone Number",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${value.length}/10",
                style = MaterialTheme.typography.labelSmall,
                color = if (value.length == 10) Color(0xFF25D366) else MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(
                    width = 2.dp,
                    color = if (isFocused) Color(0xFF25D366) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+91 ",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                )
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
        }
    }
}

private fun openWhatsAppWithFallback(context: Context, phoneNumber: String) {
    if (phoneNumber.length != 10) return
    val uri = Uri.parse("https://wa.me/91$phoneNumber")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.whatsapp")
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

@Composable
private fun AadhaarServicesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val services = listOf(
        Triple("Validity", Icons.Default.CheckCircle, "https://myaadhaar.uidai.gov.in/check-aadhaar-validity/en"),
        Triple("Download", Icons.Default.AccountBox, "https://myaadhaar.uidai.gov.in/genricDownloadAadhaar"),
        Triple("Status", Icons.Default.Search, "https://myaadhaar.uidai.gov.in/CheckAadhaarStatus/en"),
        Triple("Portal", Icons.Default.Home, "https://myaadhaar.uidai.gov.in/")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Aadhaar Services",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AadhaarServiceIcon(title = services[0].first, icon = services[0].second) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(services[0].third)))
                    }
                    AadhaarServiceIcon(title = services[1].first, icon = services[1].second) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(services[1].third)))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AadhaarServiceIcon(title = services[2].first, icon = services[2].second) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(services[2].third)))
                    }
                    AadhaarServiceIcon(title = services[3].first, icon = services[3].second) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(services[3].third)))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AadhaarServiceIcon(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(85.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(58.dp).background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(27.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AppInfoDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    val shareApp: () -> Unit = {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out PostKala - Utility Hub for Postal & UIDAI services: https://play.google.com/store/apps/details?id=${context.packageName}"
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share PostKala App")
        context.startActivity(shareIntent)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "PostKala Utility Hub",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Version $versionName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Developer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "CVAM Dignity Tech",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = shareApp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share App Link", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}