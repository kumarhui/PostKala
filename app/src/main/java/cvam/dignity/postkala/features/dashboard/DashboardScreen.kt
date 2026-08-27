package cvam.dignity.postkala.features.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvam.dignity.postkala.AdBanner

private data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val route: String? = null,
    val action: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostKalaDashboard(
    onNavigate: (String) -> Unit
) {
    val showAadhaarServices = remember { androidx.compose.runtime.mutableStateOf(false) }
    val showAppInfo = remember { androidx.compose.runtime.mutableStateOf(false) }

    // Consolidated Dashboard items
    val items = listOf(
        DashboardItem(
            title = "Studio",
            subtitle = "Scanner",
            icon = Icons.Default.QrCodeScanner,
            color = Color(0xFF6366F1),
            route = "studio_scanner"
        ),
        DashboardItem(
            title = "RPLI",
            subtitle = "Calculator",
            icon = Icons.Default.Calculate,
            color = Color(0xFFF59E0B),
            route = "rpli"
        ),
        DashboardItem(
            title = "Aadhaar",
            subtitle = "Services",
            icon = Icons.Default.AccountBox,
            color = Color(0xFFEF4444),
            action = {
                showAadhaarServices.value = true
            }
        ),
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
                modifier = Modifier.padding(
                    top = 12.dp,
                    bottom = 20.dp
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {

                items(items) { item ->
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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