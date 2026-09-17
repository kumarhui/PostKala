package cvam.dignity.postkala.features.rpli

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DobTripleInputField(
    day: String,
    onDayChange: (String) -> Unit,
    month: String,
    onMonthChange: (String) -> Unit,
    year: String,
    onYearChange: (String) -> Unit
) {
    val focusDay = remember { FocusRequester() }
    val focusMonth = remember { FocusRequester() }
    val focusYear = remember { FocusRequester() }

    var dayValue by remember {
        mutableStateOf(TextFieldValue(text = day, selection = TextRange(day.length)))
    }
    var monthValue by remember {
        mutableStateOf(TextFieldValue(text = month, selection = TextRange(month.length)))
    }
    var yearValue by remember {
        mutableStateOf(TextFieldValue(text = year, selection = TextRange(year.length)))
    }

    // Sync state when modified externally
    LaunchedEffect(day) {
        if (day != dayValue.text) {
            dayValue = TextFieldValue(text = day, selection = TextRange(day.length))
        }
    }
    LaunchedEffect(month) {
        if (month != monthValue.text) {
            monthValue = TextFieldValue(text = month, selection = TextRange(month.length))
        }
    }
    LaunchedEffect(year) {
        if (year != yearValue.text) {
            yearValue = TextFieldValue(text = year, selection = TextRange(year.length))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- DAY BOX ---
        DobBoxField(
            value = dayValue,
            placeholder = "DD",
            maxLength = 2,
            modifier = Modifier.weight(1f),
            focusRequester = focusDay,
            onValueChange = { newVal ->
                val clean = newVal.text.filter { it.isDigit() }
                if (clean.length <= 2) {
                    dayValue = newVal.copy(text = clean)
                    onDayChange(clean)
                    if (clean.length == 2) {
                        // Place cursor at the end of the existing month value
                        monthValue = monthValue.copy(selection = TextRange(monthValue.text.length))
                        focusMonth.requestFocus()
                    }
                }
            },
            onBackspaceOnEmpty = {
                // First box: no action needed on empty backspace
            }
        )

        // --- MONTH BOX ---
        DobBoxField(
            value = monthValue,
            placeholder = "MM",
            maxLength = 2,
            modifier = Modifier.weight(1f),
            focusRequester = focusMonth,
            onValueChange = { newVal ->
                val clean = newVal.text.filter { it.isDigit() }
                if (clean.length <= 2) {
                    monthValue = newVal.copy(text = clean)
                    onMonthChange(clean)
                    if (clean.length == 2) {
                        // Place cursor at the end of the existing year value
                        yearValue = yearValue.copy(selection = TextRange(yearValue.text.length))
                        focusYear.requestFocus()
                    }
                }
            },
            onBackspaceOnEmpty = {
                // When already empty, backspace moves cursor to the end of the Day box
                dayValue = dayValue.copy(selection = TextRange(dayValue.text.length))
                focusDay.requestFocus()
            }
        )

        // --- YEAR BOX ---
        DobBoxField(
            value = yearValue,
            placeholder = "YYYY",
            maxLength = 4,
            modifier = Modifier.weight(1.35f),
            focusRequester = focusYear,
            onValueChange = { newVal ->
                val clean = newVal.text.filter { it.isDigit() }
                if (clean.length <= 4) {
                    yearValue = newVal.copy(text = clean)
                    onYearChange(clean)
                }
            },
            onBackspaceOnEmpty = {
                // When already empty, backspace moves cursor to the end of the Month box
                monthValue = monthValue.copy(selection = TextRange(monthValue.text.length))
                focusMonth.requestFocus()
            }
        )
    }
}

@Composable
private fun DobBoxField(
    value: TextFieldValue,
    placeholder: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    onValueChange: (TextFieldValue) -> Unit,
    onBackspaceOnEmpty: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
            .border(
                width = 1.5.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                    if (value.text.isEmpty() || value.selection.start == 0 && value.selection.end == 0) {
                        onBackspaceOnEmpty()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            decorationBox = { innerTextField ->
                if (value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun RpliResultCard(
    result: CalculationResult,
    expanded: Boolean,
    onToggle: () -> Unit,
    onShare: () -> Unit
) {
    val cardBackground = Color(0xFF1E293B)
    val accentColor = Color(0xFF818CF8)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        color = cardBackground
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ESTIMATED PREMIUM",
                        color = Color.White.copy(0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = RpliCalculatorLogic.formatInr(result.finalPremium),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.background(Color.White.copy(0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(18.dp))

            Surface(
                color = Color.White.copy(0.06f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Maturity Amount",
                            color = Color.White.copy(0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = RpliCalculatorLogic.formatInr(result.finalMaturityAmount),
                            color = accentColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(color = accentColor.copy(0.2f), shape = CircleShape) {
                        Text(
                            "${"%.1f".format(result.roi)}% ROI",
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            TextButton(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                Spacer(Modifier.width(6.dp))
                Text(if (expanded) "HIDE BREAKDOWN" else "VIEW BREAKDOWN", fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(400)) + fadeIn()
            ) {
                Column(
                    Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(0.1f))
                    DetailRow("Entry / Maturity Age", "${result.entryAge} / ${result.maturityAge} Yrs")
                    DetailRow("Base Premium", RpliCalculatorLogic.formatInr(result.basePremiumPerPeriod))
                    DetailRow("Rebate", "- ${RpliCalculatorLogic.formatInr(result.rebatePerPeriod)}")
                    DetailRow("Payable (${result.frequency})", RpliCalculatorLogic.formatInr(result.finalPremium))
                    HorizontalDivider(color = Color.White.copy(0.06f))
                    DetailRow("Total Invested", RpliCalculatorLogic.formatInr(result.totalPremiumOverTerm))
                    DetailRow("Bonus Accrued", RpliCalculatorLogic.formatInr(result.totalBonus))
                    DetailRow("Net Gain", RpliCalculatorLogic.formatInr(result.netGain))
                }
            }
        }
    }
}

@Composable
fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLength: Int = 10,
    prefix: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                .border(
                    1.5.dp,
                    if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                prefix?.let {
                    Text(
                        it,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.onFocusChanged { isFocused = it.isFocused }
                )
            }
        }
    }
}

@Composable
fun FrequencyToggle(selected: String, onSelect: (String) -> Unit, options: Map<String, String>) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
            .padding(4.dp)
    ) {
        options.forEach { (key, label) ->
            val isSel = key == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun InputGroup(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelLarge
            )
        }
        content()
    }
}

@Composable
fun DetailRow(l: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(l, color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
        Text(v, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}