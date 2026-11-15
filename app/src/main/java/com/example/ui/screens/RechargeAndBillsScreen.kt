package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.PaymentSettingsEntity
import com.example.data.local.UserEntity
import com.example.ui.components.MpinConfirmationDialog
import com.example.ui.theme.LocalSujoyColors
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue600
import com.example.ui.viewmodel.WalletUiState
import com.example.util.Formatters

private data class BillPlanPreset(
    val amountRupees: String,
    val title: String,
    val subtitle: String
)

private enum class BillCategory(
    val typeCode: String,
    val title: String,
    val idLabel: String,
    val providers: List<String>
) {
    MOBILE(
        typeCode = "MOBILE_RECHARGE",
        title = "Mobile",
        idLabel = "10-Digit Mobile Number",
        providers = listOf("Jio Prepaid", "Airtel Prepaid", "Vi Prepaid", "BSNL")
    ),
    DTH(
        typeCode = "DTH_RECHARGE",
        title = "DTH",
        idLabel = "Subscriber / Smart Card ID",
        providers = listOf("Tata Play", "Airtel Digital TV", "Dish TV", "Sun Direct", "d2h")
    ),
    ELECTRICITY(
        typeCode = "ELECTRICITY_BILL",
        title = "Electricity",
        idLabel = "Consumer / CA Number",
        providers = listOf("WBSEDCL", "Tata Power", "BESCOM", "MSEDCL", "Adani Electricity")
    ),
    WATER(
        typeCode = "WATER_BILL",
        title = "Water",
        idLabel = "Water Connection / K No.",
        providers = listOf("Delhi Jal Board", "BWSSB Bengaluru", "KMC Water", "BMC Mumbai")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeAndBillsScreen(
    initialCategoryCode: String,
    user: UserEntity?,
    uiState: WalletUiState,
    onBack: () -> Unit,
    onPayBill: (type: String, provider: String, consumerId: String, amount: String, note: String, mpin: String) -> Unit,
    settings: PaymentSettingsEntity? = null
) {
    val sujoy = LocalSujoyColors.current
    var selectedCategory by remember {
        mutableStateOf(
            BillCategory.entries.firstOrNull { it.typeCode == initialCategoryCode } ?: BillCategory.MOBILE
        )
    }
    var selectedProvider by remember(selectedCategory) {
        mutableStateOf(selectedCategory.providers.first())
    }
    var consumerId by remember(selectedCategory) {
        mutableStateOf(if (selectedCategory == BillCategory.MOBILE) user?.mobileNumber.orEmpty() else "")
    }
    var amountText by remember(selectedCategory) { mutableStateOf("") }
    var planNote by remember(selectedCategory) { mutableStateOf("") }
    var fetchedBillBanner by remember(selectedCategory) { mutableStateOf<String?>(null) }
    var showMpinDialog by remember { mutableStateOf(false) }

    val plans = remember(selectedCategory) {
        when (selectedCategory) {
            BillCategory.MOBILE -> listOf(
                BillPlanPreset("299", "₹299 • 28 Days", "2GB/day + Unlimited 5G"),
                BillPlanPreset("666", "₹666 • 84 Days", "1.5GB/day + Calls"),
                BillPlanPreset("2999", "₹2,999 • 365 Days", "Annual 2.5GB/day Pack")
            )
            BillCategory.DTH -> listOf(
                BillPlanPreset("350", "₹350 • Monthly HD", "Hindi + Sports + Kids Pack"),
                BillPlanPreset("550", "₹550 • Family Mega", "All Regional + 4K Channels"),
                BillPlanPreset("1800", "₹1,800 • 6 Months", "Semi-Annual Value Saver")
            )
            else -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Recharge & BBPS Bills") },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BillCategory.entries) { cat ->
                        val icon = when (cat) {
                            BillCategory.MOBILE -> Icons.Default.PhoneAndroid
                            BillCategory.DTH -> Icons.Default.Tv
                            BillCategory.ELECTRICITY -> Icons.Default.Bolt
                            BillCategory.WATER -> Icons.Default.WaterDrop
                        }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.title) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("bill_cat_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            item {
                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wallet Balance", style = MaterialTheme.typography.bodyMedium, color = PureWhite.copy(alpha = 0.78f))
                        Text(
                            Formatters.formatInr(user?.balancePaise ?: 0L),
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Operator / Biller", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedCategory.providers) { provider ->
                            FilterChip(
                                selected = selectedProvider == provider,
                                onClick = { selectedProvider = provider },
                                label = { Text(provider) }
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, sujoy.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = consumerId,
                            onValueChange = { consumerId = it.take(24) },
                            label = { Text(selectedCategory.idLabel) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bill_consumer_input")
                        )

                        if (selectedCategory == BillCategory.ELECTRICITY || selectedCategory == BillCategory.WATER) {
                            OutlinedButton(
                                onClick = {
                                    val fetchedPaise = if (selectedCategory == BillCategory.ELECTRICITY) "1420" else "480"
                                    amountText = fetchedPaise
                                    planNote = "$selectedProvider BBPS Bill"
                                    fetchedBillBanner = "BBPS Sandbox Bill Fetched • Consumer: ${user?.fullName ?: "Verified"} • Due: 25 Nov 2026 • ₹$fetchedPaise"
                                },
                                enabled = consumerId.trim().length >= 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("fetch_bill_btn")
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetch BBPS Bill Amount")
                            }
                        }

                        fetchedBillBanner?.let { banner ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = banner,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { raw -> amountText = raw.filter { it.isDigit() || it == '.' }.take(8) },
                            label = { Text("Amount (INR)") },
                            prefix = { Text("₹ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bill_amount_input")
                        )
                    }
                }
            }

            val cashbackEnabled = settings?.cashbackEnabled ?: true
            val cashbackPercent = settings?.cashbackPercent ?: 25
            if (cashbackEnabled && cashbackPercent > 0) {
                item {
                    val billPaise = Formatters.parseRupeesToPaise(amountText) ?: 0L
                    val previewCashback = if (billPaise >= 10_00L) (billPaise * cashbackPercent) / 100L else 0L
                    Surface(
                        color = sujoy.successContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, sujoy.success.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bill_cashback_preview")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Instant Cashback",
                                tint = sujoy.success,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (previewCashback > 0L) {
                                    "Instant $cashbackPercent% Cashback: +${Formatters.formatInr(previewCashback)} credited after payment"
                                } else {
                                    "Get $cashbackPercent% instant cashback on every recharge & BBPS bill"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = sujoy.success
                            )
                        }
                    }
                }
            }

            if (plans.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Popular Plans", style = MaterialTheme.typography.titleSmall)
                        plans.forEach { plan ->
                            Surface(
                                color = if (amountText == plan.amountRupees) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (amountText == plan.amountRupees) RoyalBlue600 else sujoy.cardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        amountText = plan.amountRupees
                                        planNote = "${plan.title} (${plan.subtitle})"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(plan.title, style = MaterialTheme.typography.titleSmall)
                                        Text(plan.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("Select", color = RoyalBlue600, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = sujoy.failureContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(uiState.errorMessage, color = sujoy.failure, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            item {
                val amountPaise = Formatters.parseRupeesToPaise(amountText) ?: 0L
                Button(
                    onClick = { showMpinDialog = true },
                    enabled = !uiState.isBusy && consumerId.trim().length >= 5 && amountPaise >= 10_00L,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("bill_pay_submit_btn")
                ) {
                    Text("Verify MPIN & Pay Bill", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    if (showMpinDialog) {
        val paise = Formatters.parseRupeesToPaise(amountText) ?: 0L
        MpinConfirmationDialog(
            title = "Authorize ${selectedCategory.title} Payment",
            subtitle = "$selectedProvider • $consumerId",
            amountFormatted = Formatters.formatInr(paise),
            onDismiss = { showMpinDialog = false },
            onConfirmMpin = { pin ->
                showMpinDialog = false
                onPayBill(
                    selectedCategory.typeCode,
                    selectedProvider,
                    consumerId,
                    amountText,
                    planNote.ifBlank { "$selectedProvider ${selectedCategory.title}" },
                    pin
                )
            }
        )
    }
}
