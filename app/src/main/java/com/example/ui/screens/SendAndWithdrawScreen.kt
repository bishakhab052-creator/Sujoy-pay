package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendAndWithdrawScreen(
    initialTabIsWithdraw: Boolean,
    user: UserEntity?,
    uiState: WalletUiState,
    onBack: () -> Unit,
    onOpenScanner: () -> Unit,
    onSendMoney: (handle: String, name: String, amount: String, note: String, mpin: String) -> Unit,
    onWithdrawMoney: (account: String, ifsc: String, holder: String, amount: String, mpin: String) -> Unit,
    settings: PaymentSettingsEntity? = null
) {
    val sujoy = LocalSujoyColors.current
    var isWithdrawTab by remember { mutableStateOf(initialTabIsWithdraw) }

    // Send money state (auto-populated if coming from QR scanner)
    var recipientHandle by remember { mutableStateOf(uiState.scannedUpiHandle) }
    var recipientName by remember { mutableStateOf(uiState.scannedPayeeName) }
    var sendAmount by remember { mutableStateOf(uiState.scannedAmountRupees) }
    var sendNote by remember { mutableStateOf(uiState.scannedNote) }

    LaunchedEffect(uiState.scannedUpiHandle) {
        if (uiState.scannedUpiHandle.isNotBlank()) {
            recipientHandle = uiState.scannedUpiHandle
            recipientName = uiState.scannedPayeeName
            if (uiState.scannedAmountRupees.isNotBlank()) sendAmount = uiState.scannedAmountRupees
            if (uiState.scannedNote.isNotBlank()) sendNote = uiState.scannedNote
            isWithdrawTab = false
        }
    }

    // Withdraw state
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf(user?.fullName.orEmpty()) }
    var withdrawAmount by remember { mutableStateOf("") }

    var showMpinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(if (isWithdrawTab) "Withdraw to Bank" else "Send Money") },
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = !isWithdrawTab,
                        onClick = { isWithdrawTab = false },
                        label = { Text("Send via UPI / Mobile") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_send_money")
                    )
                    FilterChip(
                        selected = isWithdrawTab,
                        onClick = { isWithdrawTab = true },
                        label = { Text("Withdraw IMPS") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_withdraw_money")
                    )
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
                        Text("Available Balance", style = MaterialTheme.typography.bodyMedium, color = PureWhite.copy(alpha = 0.75f))
                        Text(
                            Formatters.formatInr(user?.balancePaise ?: 0L),
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isWithdrawTab) {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recipient Details", style = MaterialTheme.typography.titleMedium)
                                OutlinedButton(
                                    onClick = onOpenScanner,
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan QR")
                                }
                            }

                            OutlinedTextField(
                                value = recipientHandle,
                                onValueChange = { recipientHandle = it },
                                label = { Text("UPI ID or 10-Digit Mobile") },
                                placeholder = { Text("e.g. 9811223344@sujoypay or name@okaxis") },
                                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_recipient_input")
                            )

                            OutlinedTextField(
                                value = recipientName,
                                onValueChange = { recipientName = it },
                                label = { Text("Payee Name (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = sendAmount,
                                onValueChange = { raw -> sendAmount = raw.filter { it.isDigit() || it == '.' }.take(9) },
                                label = { Text("Amount (INR)") },
                                prefix = { Text("₹ ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_amount_input")
                            )

                            OutlinedTextField(
                                value = sendNote,
                                onValueChange = { sendNote = it.take(60) },
                                label = { Text("Payment Note (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                val cashbackEnabled = settings?.cashbackEnabled ?: true
                val cashbackPercent = settings?.cashbackPercent ?: 25
                if (cashbackEnabled && cashbackPercent > 0) {
                    item {
                        val sendPaise = Formatters.parseRupeesToPaise(sendAmount) ?: 0L
                        val previewCashback = if (sendPaise >= 100L) (sendPaise * cashbackPercent) / 100L else 0L
                        Surface(
                            color = sujoy.successContainer,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, sujoy.success.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_cashback_preview")
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
                                        "Instant $cashbackPercent% Cashback: +${Formatters.formatInr(previewCashback)} credited on payment"
                                    } else {
                                        "Earn $cashbackPercent% instant cashback on every UPI transfer"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = sujoy.success
                                )
                            }
                        }
                    }
                }
            } else {
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
                            Text("Bank Account Payout (RBI Full-KYC Only)", style = MaterialTheme.typography.titleMedium)
                            if (user?.kycStatus != "VERIFIED") {
                                Surface(
                                    color = sujoy.pendingContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Your KYC tier is ${user?.kycStatus ?: "MIN_KYC"}. RBI PPI regulations permit bank withdrawals only for Full-KYC verified wallets.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = sujoy.pending,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = holderName,
                                onValueChange = { holderName = it },
                                label = { Text("Account Holder Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { raw -> accountNumber = raw.filter { it.isDigit() }.take(18) },
                                label = { Text("Bank Account Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("withdraw_account_input")
                            )
                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it.uppercase().take(11) },
                                label = { Text("IFSC Code (e.g. HDFC0001024)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("withdraw_ifsc_input")
                            )
                            OutlinedTextField(
                                value = withdrawAmount,
                                onValueChange = { raw -> withdrawAmount = raw.filter { it.isDigit() || it == '.' }.take(9) },
                                label = { Text("Withdrawal Amount (Min ₹100)") },
                                prefix = { Text("₹ ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("withdraw_amount_input")
                            )
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
                val canProceed = if (!isWithdrawTab) {
                    recipientHandle.isNotBlank() && (Formatters.parseRupeesToPaise(sendAmount) ?: 0L) >= 100L
                } else {
                    accountNumber.length >= 9 && ifscCode.length == 11 &&
                        (Formatters.parseRupeesToPaise(withdrawAmount) ?: 0L) >= 100_00L
                }
                Button(
                    onClick = { showMpinDialog = true },
                    enabled = !uiState.isBusy && canProceed,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("send_withdraw_submit_btn")
                ) {
                    Text(
                        if (isWithdrawTab) "Verify MPIN & Withdraw" else "Verify MPIN & Send",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }

    if (showMpinDialog) {
        val paise = Formatters.parseRupeesToPaise(if (isWithdrawTab) withdrawAmount else sendAmount) ?: 0L
        MpinConfirmationDialog(
            title = if (isWithdrawTab) "Authorize Bank Payout" else "Authorize UPI Transfer",
            subtitle = if (isWithdrawTab) "IMPS to A/C •••• ${accountNumber.takeLast(4)}" else "Payee: $recipientHandle",
            amountFormatted = Formatters.formatInr(paise),
            onDismiss = { showMpinDialog = false },
            onConfirmMpin = { pin ->
                showMpinDialog = false
                if (isWithdrawTab) {
                    onWithdrawMoney(accountNumber, ifscCode, holderName, withdrawAmount, pin)
                } else {
                    onSendMoney(recipientHandle, recipientName, sendAmount, sendNote, pin)
                }
            }
        )
    }
}
