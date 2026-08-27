package cvam.dignity.postkala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.*
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import cvam.dignity.postkala.features.dashboard.PostKalaDashboard
import cvam.dignity.postkala.features.rpli.RpliCalculatorScreen
import cvam.dignity.postkala.features.scanner.StudioScannerScreen
import cvam.dignity.postkala.ui.theme.PostKalaTheme
import kotlinx.coroutines.delay

val AppEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

class MainActivity : ComponentActivity() {

    private val UPDATE_REQUEST_CODE = 1001

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        checkForInAppUpdates()
        MobileAds.initialize(this) {}
        enableEdgeToEdge()

        setContent {
            PostKalaTheme {
                var isAppReady by remember { mutableStateOf(false) }

                splashScreen.setKeepOnScreenCondition { !isAppReady }

                LaunchedEffect(Unit) {
                    delay(1000)
                    isAppReady = true
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AnimatedVisibility(
                        visible = isAppReady,
                        enter = scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(600, easing = AppEasing)
                        ) + fadeIn(animationSpec = tween(600)),
                        exit = fadeOut()
                    ) {
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = "dashboard"
                        ) {
                            composable("dashboard") {
                                PostKalaDashboard(onNavigate = { route -> navController.navigate(route) })
                            }

                            // Single unified scanner route
                            composable("studio_scanner") {
                                StudioScannerScreen(onBack = { navController.popBackStack() })
                            }

                            composable("rpli") {
                                Scaffold(
                                    topBar = {
                                        CenterAlignedTopAppBar(
                                            title = { Text("RPLI PREMIUM", fontWeight = FontWeight.Black) },
                                            navigationIcon = {
                                                IconButton(onClick = { navController.popBackStack() }) {
                                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

    private fun checkForInAppUpdates() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val adUnitId = try {
        AdsConfig.bannerId
    } catch (e: Exception) {
        "ca-app-pub-3940256099942544/6300978111"
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}