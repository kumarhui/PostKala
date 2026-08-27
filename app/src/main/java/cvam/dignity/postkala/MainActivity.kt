package cvam.dignity.postkala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.*
import cvam.dignity.postkala.features.AadhaarStudioScreen
import cvam.dignity.postkala.features.ArticleScannerScreen
import cvam.dignity.postkala.features.rpli.RpliCalculatorScreen
import cvam.dignity.postkala.ui.theme.PostKalaTheme
import kotlinx.coroutines.delay

/**
 * GLOBAL EASING: Public top-level property used by feature files
 * for shared axis and state entry animations.
 */
val AppEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install System Splash Screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}
        enableEdgeToEdge()

        setContent {
            PostKalaTheme {
                // 2. Control when the system splash screen disappears
                var isAppReady by remember { mutableStateOf(false) }

                // Keep the system splash icon on screen until initialization is done
                splashScreen.setKeepOnScreenCondition { !isAppReady }

                LaunchedEffect(Unit) {
                    delay(1000) // Keep the app icon splash for 1 second
                    isAppReady = true
                }

                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // MAIN APP CONTENT
                    // Shows only after isAppReady becomes true
                    AnimatedVisibility(
                        visible = isAppReady,
                        enter = scaleIn(initialScale = 0.95f, animationSpec = tween(600, easing = AppEasing)) + fadeIn(animationSpec = tween(600)),
                        exit = fadeOut()
                    ) {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "dashboard") {
                            composable("dashboard") {
                                PostKalaDashboard(onNavigate = { route -> navController.navigate(route) })
                            }
                            composable("aadhaar") {
                                AadhaarStudioScreen(onBack = { navController.popBackStack() })
                            }
                            composable("article") {
                                ArticleScannerScreen(onBack = { navController.popBackStack() })
                            }
                            composable("rpli") {
                                Scaffold(
                                    topBar = {
                                        CenterAlignedTopAppBar(
                                            title = { Text("RPLI PREMIUM", fontWeight = FontWeight.Black) },
                                            navigationIcon = {
                                                IconButton(onClick = { navController.popBackStack() }) {
                                                    Icon(Icons.Default.ArrowBack, null)
                                                }
                                            }
                                        )
                                    },
                                    bottomBar = { AdBanner() }
                                ) { padding ->
                                    RpliCalculatorScreen(Modifier.padding(padding))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val adUnitId = try { AdsConfig.bannerId } catch (e: Exception) { "ca-app-pub-3940256099942544/6300978111" }
    Box(modifier.fillMaxWidth().height(60.dp), Alignment.Center) {
        AndroidView(factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostKalaDashboard(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("POSTKALA", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Utility Hub", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = { AdBanner() }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                text = "Services",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { DashboardCard("Aadhaar Studio", "UID Scanner", Icons.Default.Fingerprint, Color(0xFF6366F1)) { onNavigate("aadhaar") } }
                item { DashboardCard("Article Scan", "Postal Tracker", Icons.Default.QrCodeScanner, Color(0xFF10B981)) { onNavigate("article") } }
                item { DashboardCard("RPLI Calc", "Premium Calculator", Icons.Default.Calculate, Color(0xFFF59E0B)) { onNavigate("rpli") } }
                item { DashboardCard("Info", "App Details", Icons.Default.Info, Color(0xFF64748B)) { } }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "card_scale")

    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp).fillMaxSize(), Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color), Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                Text(subtitle, fontSize = 11.sp, color = color.copy(alpha = 0.7f))
            }
        }
    }
}