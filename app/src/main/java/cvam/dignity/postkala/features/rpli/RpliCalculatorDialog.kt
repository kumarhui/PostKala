package cvam.dignity.postkala.features.rpli

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import cvam.dignity.postkala.AdsConfig
import org.json.JSONArray
import java.time.LocalDate
import java.time.Period

@Composable
fun RpliCalculatorDialog(
    onDismiss: () -> Unit
) {
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

    var mInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        val interstitialId = try {
            AdsConfig.interstitialId
        } catch (_: Exception) {
            "ca-app-pub-3940256099942544/1033173712"
        }
        InterstitialAd.load(context, interstitialId, adRequest, object : InterstitialAdLoadCallback() {
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
        if (premiumTables.value.isNotEmpty() && sumAssured.isNotEmpty() && day.isNotEmpty() && month.isNotEmpty() && year.length == 4) {
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
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Calculate, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("RPLI PREMIUM", fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text("Gram Santosh Calculator", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    InputGroup(title = "Date of Birth", icon = Icons.Default.DateRange) {
                        DobTripleInputField(
                            day = day,
                            onDayChange = { day = it },
                            month = month,
                            onMonthChange = { month = it },
                            year = year,
                            onYearChange = { year = it }
                        )
                    }

                    InputGroup(title = "Policy Term", icon = Icons.Default.TrackChanges) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${policyTerm.toInt()} Years", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = policyTerm,
                            onValueChange = { policyTerm = it },
                            valueRange = 5f..maxTerm
                        )
                    }

                    InputGroup(title = "Sum Assured", icon = Icons.Default.AccountBalanceWallet) {
                        NumericField(
                            value = sumAssured,
                            onValueChange = { sumAssured = it },
                            label = "Coverage Amount",
                            maxLength = 7,
                            prefix = "₹"
                        )
                    }

                    FrequencyToggle(
                        selected = frequency,
                        onSelect = { frequency = it },
                        options = mapOf("monthly" to "M", "quarterly" to "Q", "halfyearly" to "H", "yearly" to "Y")
                    )

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    calculationResult?.let { result ->
                        RpliResultCard(
                            result = result,
                            expanded = showFullDetails,
                            onToggle = { showFullDetails = !showFullDetails },
                            onShare = {
                                (context as? Activity)?.let { act ->
                                    mInterstitialAd?.show(act)
                                    loadInterstitial()
                                }
                                RpliCalculatorLogic.shareRpliQuote(context, result)
                            }
                        )
                    }
                }
            }
        }
    }
}