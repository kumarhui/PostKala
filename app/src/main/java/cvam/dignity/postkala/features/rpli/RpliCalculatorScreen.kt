package cvam.dignity.postkala.features.rpli

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import cvam.dignity.postkala.AppEasing
import cvam.dignity.postkala.ui.theme.PostKalaTheme
import org.json.JSONArray
import java.time.LocalDate
import java.time.Period

// Configuration for Ads
object AdsConfig {
    const val interstitialId = "ca-app-pub-3940256099942544/1033173712"
}

class RpliCalculatorActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PostKalaTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("RPLI PREMIUM", fontWeight = FontWeight.Black) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    RpliCalculatorScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RpliCalculatorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var day by remember { mutableStateOf("01") }
    var month by remember { mutableStateOf("01") }
    var year by remember { mutableStateOf("1995") }
    var policyTerm by remember { mutableFloatStateOf(10f) }
    var sumAssured by remember { mutableStateOf("100000") }
    var frequency by remember { mutableStateOf("monthly") }

    var calculationResult by remember { mutableStateOf<CalculationResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFullDetails by remember { mutableStateOf(false) }

    var entryVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entryVisible = true }

    var mInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, AdsConfig.interstitialId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { mInterstitialAd = null }
            override fun onAdLoaded(interstitialAd: InterstitialAd) { mInterstitialAd = interstitialAd }
        })
    }

    LaunchedEffect(Unit) { loadInterstitial() }

    val entryAge = try {
        val dob = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        Period.between(dob, LocalDate.now()).years + 1
    } catch (_: Exception) { 0 }

    val maxTerm = (60 - entryAge).coerceAtLeast(5).toFloat()
    val premiumTables = remember { mutableStateOf<Map<String, JSONArray>>(emptyMap()) }

    LaunchedEffect(Unit) {
        premiumTables.value = RpliCalculatorLogic.loadRpliTables(context)
    }

    LaunchedEffect(day, month, year, policyTerm, sumAssured, frequency, premiumTables.value) {
        if (premiumTables.value.isNotEmpty() && sumAssured.isNotEmpty()) {
            try {
                errorMessage = null
                calculationResult = RpliCalculatorLogic.performRpliCalc(
                    day, month, year, policyTerm.toInt(), sumAssured.toDouble(), frequency, premiumTables.value
                )
            } catch (e: Exception) {
                calculationResult = null
                errorMessage = if (sumAssured.isEmpty()) null else e.message
            }
        }
    }

    val inputAlpha by animateFloatAsState(
        targetValue = if (calculationResult != null) 0.7f else 1f,
        label = "input_fade"
    )

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AnimatedVisibility(
            visible = entryVisible,
            enter = scaleIn(initialScale = 0.9f, animationSpec = tween(500, easing = AppEasing)) + fadeIn()
        ) {
            Column(
                Modifier.graphicsLayer { alpha = inputAlpha },
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                InputGroup(title = "Date of Birth", icon = Icons.Default.DateRange) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { NumericField(day, { day = it }, "DD", maxLength = 2) }
                        Box(Modifier.weight(1f)) { NumericField(month, { month = it }, "MM", maxLength = 2) }
                        Box(Modifier.weight(1.5f)) { NumericField(year, { year = it }, "YYYY", maxLength = 4) }
                    }
                }

                InputGroup(title = "Policy Term", icon = Icons.Default.TrackChanges) {
                    Text("${policyTerm.toInt()} Years", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Slider(value = policyTerm, onValueChange = { policyTerm = it }, valueRange = 5f..maxTerm)
                }

                InputGroup(title = "Sum Assured", icon = Icons.Default.AccountBalanceWallet) {
                    NumericField(value = sumAssured, onValueChange = { sumAssured = it }, label = "Coverage Amount", maxLength = 7, prefix = "₹")
                }

                FrequencyToggle(frequency, { frequency = it }, mapOf("monthly" to "M", "quarterly" to "Q", "halfyearly" to "H", "yearly" to "Y"))
            }
        }

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }

        AnimatedVisibility(
            visible = calculationResult != null,
            enter = scaleIn(initialScale = 0.8f, animationSpec = tween(600, easing = AppEasing)) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut()
        ) {
            calculationResult?.let { result ->
                RpliResultCard(
                    result = result,
                    expanded = showFullDetails,
                    onToggle = { showFullDetails = !showFullDetails },
                    onShare = {
                        mInterstitialAd?.let { ad ->
                            ad.show(context as Activity)
                            loadInterstitial()
                        }
                        RpliCalculatorLogic.shareRpliQuote(context, result)
                    }
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}