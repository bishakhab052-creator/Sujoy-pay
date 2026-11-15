package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.PaymentSettingsEntity
import com.example.data.local.UserEntity
import com.example.data.repository.InitiatedAddMoneyOrder
import com.example.ui.components.SandboxBadge
import com.example.ui.theme.ElectricCyan400
import com.example.ui.theme.LocalSujoyColors
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue600
import com.example.ui.viewmodel.WalletUiState
import com.example.util.Formatters
import com.example.util.UpiAppChannel
import com.example.util.UpiIntentHelper
import com.example.util.UpiLaunchOutcome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneyScreen(
    user: UserEntity?,
    settings: PaymentSettingsEntity?,
    uiState: WalletUiState,
    onBack: () -> Unit,
    onInitiateAddMoney: (amountText: String, channel: UpiAppChannel, onReady: (InitiatedAddMoneyOrder) -> Unit) -> Unit,
    onGatewayLaunchNote: (String) -> Unit,
    onCompleteVerification: (orderId: String, authorized: Boolean) -> Unit,
    onDismissOrder: () -> Unit
) {
    val context = LocalContext.current
    val sujoy = LocalSujoyColors.current
    var amountText by remember { mutableStateOf("1000") }
    var selectedChannel by remember { mutableStateOf(UpiAppChannel.GOOGLE_PAY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Add Money to Wallet") },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                SandboxBadge(modifier = Modifier.padding(end = 12.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CURRENT SUJOY PAY BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan400
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                Formatters.formatInr(user?.balancePaise ?: 0L),
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureWhite
                            )
                        }
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = ElectricCyan400,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, sujoy.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Enter Amount (INR)", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() || it == '.' }
                                amountText = filtered.take(9)
                            },
                            prefix = { Text("₹ ", style = MaterialTheme.typography.titleLarge) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_money_amount_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("500", "1000", "2000", "5000").forEach { preset ->
                                FilterChip(
                                    selected = amountText == preset,
                                    onClick = { amountText = preset },
                                    label = { Text("₹$preset") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Authorized UPI Payment Gateway", style = MaterialTheme.typography.titleMedium)

                    val gpayEnabled = settings?.gpayUpiEnabled ?: true
                    val phonePeEnabled = settings?.phonePeUpiEnabled ?: true
                    val otherEnabled = settings?.otherUpiEnabled ?: true

                    PaymentChannelCard(
                        channel = UpiAppChannel.GOOGLE_PAY,
                        selected = selectedChannel == UpiAppChannel.GOOGLE_PAY,
                        enabled = gpayEnabled,
                        onClick = { selectedChannel = UpiAppChannel.GOOGLE_PAY },
                        modifier = Modifier.testTag("method_gpay_upi")
                    )
                    PaymentChannelCard(
                        channel = UpiAppChannel.PHONEPE,
                        selected = selectedChannel == UpiAppChannel.PHONEPE,
                        enabled = phonePeEnabled,
                        onClick = { selectedChannel = UpiAppChannel.PHONEPE },
                        modifier = Modifier.testTag("method_phonepe_upi")
                    )
                    PaymentChannelCard(
                        channel = UpiAppChannel.OTHER_UPI,
                        selected = selectedChannel == UpiAppChannel.OTHER_UPI,
                        enabled = otherEnabled,
                        onClick = { selectedChannel = UpiAppChannel.OTHER_UPI },
                        modifier = Modifier.testTag("method_other_upi")
                    )
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = RoyalBlue600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "NPCI & Gateway Verification Policy: Google Pay and PhonePe are invoked strictly via authorized UPI payment gateway intents. SUJOY PAY never reads external Google Pay balances and credits your wallet only after server-side HMAC signature verification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
                        Text(
                            text = uiState.errorMessage,
                            color = sujoy.failure,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onInitiateAddMoney(amountText, selectedChannel) { initiated ->
                            val outcome = UpiIntentHelper.launchUpiPaymentIntent(
                                context = context,
                                upiUri = initiated.order.upiIntentUri,
                                channel = initiated.channel
                            )
                            when (outcome) {
                                is UpiLaunchOutcome.LaunchedExternalApp -> {
                                    onGatewayLaunchNote("External UPI app (${outcome.appName}) opened. Complete authorization and verify callback below.")
                                }
                                is UpiLaunchOutcome.NoUpiAppInstalledFallbackToSandbox -> {
                                    onGatewayLaunchNote(outcome.reason)
                                }
                            }
                        }
                    },
                    enabled = !uiState.isBusy && (Formatters.parseRupeesToPaise(amountText) ?: 0L) >= 1000L,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("proceed_add_money_btn")
                ) {
                    if (uiState.isBusy) {
                        CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Proceed to Gateway Checkout", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }

    // Server-Verified Gateway Checkout & Callback Sheet
    uiState.activeGatewayOrder?.let { activeOrder ->
        ModalBottomSheet(
            onDismissRequest = onDismissOrder,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RoyalBlue600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gateway Payment Verification", style = MaterialTheme.typography.titleLarge)
                    }
                    SandboxBadge()
                }

                if (!uiState.gatewayLaunchNote.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.gatewayLaunchNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("ORDER AMOUNT", style = MaterialTheme.typography.labelSmall, color = ElectricCyan400)
                        Text(
                            Formatters.formatInr(activeOrder.order.amountPaise),
                            style = MaterialTheme.typography.displaySmall,
                            color = PureWhite
                        )
                        Text(
                            "Order ID: ${activeOrder.order.orderId}",
                            style = MonoCodeStyle,
                            color = PureWhite.copy(alpha = 0.85f)
                        )
                        Text(
                            "Merchant VPA: ${activeOrder.merchantVpa}",
                            style = MonoCodeStyle,
                            color = PureWhite.copy(alpha = 0.75f)
                        )
                        Text(
                            "Challenge HMAC: ${activeOrder.order.hmacSignature.take(24)}…",
                            style = MonoCodeStyle,
                            color = ElectricCyan400
                        )
                    }
                }

                Text(
                    "Wallet balance remains unchanged until the gateway callback signature is verified server-side.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { onCompleteVerification(activeOrder.order.orderId, true) },
                    enabled = !uiState.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = sujoy.success),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("gateway_verify_success_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authorize & Verify Callback (Sandbox)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onCompleteVerification(activeOrder.order.orderId, false) },
                    enabled = !uiState.isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gateway_verify_fail_btn")
                ) {
                    Text("Cancel / Simulate Payment Decline")
                }
            }
        }
    }
}

@Composable
private fun PaymentChannelCard(
    channel: UpiAppChannel,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sujoy = LocalSujoyColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) RoyalBlue600 else sujoy.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (selected) RoyalBlue600 else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = if (selected) PureWhite else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (enabled) channel.subtitle else "Temporarily disabled by Admin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (selected) "Selected" else "Not selected",
                tint = if (selected) RoyalBlue600 else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
