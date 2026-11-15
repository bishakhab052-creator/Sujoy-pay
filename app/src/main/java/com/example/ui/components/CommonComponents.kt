package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TransactionEntity
import com.example.ui.theme.ElectricCyan400
import com.example.ui.theme.LocalSujoyColors
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue600
import com.example.util.Formatters

@Composable
fun SujoyPayLogo(
    modifier: Modifier = Modifier,
    lightTextOnDark: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_sujoy_emblem),
            contentDescription = "SUJOY PAY emblem",
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, ElectricCyan400.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SUJOY ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (lightTextOnDark) PureWhite else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "PAY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (lightTextOnDark) ElectricCyan400 else RoyalBlue600
                )
            }
            Text(
                text = "UPI & DIGITAL WALLET",
                style = MaterialTheme.typography.labelSmall,
                color = if (lightTextOnDark) PureWhite.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SandboxBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Navy800,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan400.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Sandbox mode active",
                tint = ElectricCyan400,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "SANDBOX",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusPill(status: String, modifier: Modifier = Modifier) {
    val sujoy = LocalSujoyColors.current
    val (bg, fg, label) = when (status.uppercase()) {
        "SUCCESS", "VERIFIED", "RESOLVED", "PROCESSED" -> Triple(sujoy.successContainer, sujoy.success, status.uppercase())
        "PENDING", "MIN_KYC", "IN_REVIEW", "OPEN", "PENDING_REVIEW" -> Triple(sujoy.pendingContainer, sujoy.pending, status.replace("_", " "))
        else -> Triple(sujoy.failureContainer, sujoy.failure, status.uppercase())
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun iconForTransactionType(type: String): ImageVector = when (type) {
    "ADD_MONEY" -> Icons.Default.AddCard
    "WITHDRAW" -> Icons.Default.AccountBalance
    "SEND_MONEY" -> Icons.AutoMirrored.Filled.CallMade
    "RECEIVE_MONEY" -> Icons.AutoMirrored.Filled.CallReceived
    "MOBILE_RECHARGE" -> Icons.Default.PhoneAndroid
    "DTH_RECHARGE" -> Icons.Default.Tv
    "ELECTRICITY_BILL" -> Icons.Default.Bolt
    "WATER_BILL" -> Icons.Default.WaterDrop
    "REFUND" -> Icons.Default.Replay
    "CASHBACK" -> Icons.Default.CardGiftcard
    else -> Icons.Default.VerifiedUser
}

@Composable
fun TransactionRowCard(
    txn: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sujoy = LocalSujoyColors.current
    val isCredit = txn.direction == "CREDIT"
    val amountColor = when {
        txn.status == "FAILED" -> MaterialTheme.colorScheme.onSurfaceVariant
        isCredit -> sujoy.success
        else -> MaterialTheme.colorScheme.onSurface
    }
    val prefix = if (isCredit) "+" else "-"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("txn_row_${txn.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, sujoy.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCredit) sujoy.successContainer.copy(alpha = 0.65f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForTransactionType(txn.type),
                    contentDescription = txn.type,
                    tint = if (isCredit) sujoy.success else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.counterpartyName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${Formatters.formatShortDate(txn.createdAt)} • ${txn.gatewayMethod.replace("_", " ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${Formatters.formatInr(txn.amountPaise)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(status = txn.status)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionReceiptSheet(
    txn: TransactionEntity,
    onDismiss: () -> Unit,
    onSubmitDispute: ((category: String, description: String, requestRefund: Boolean) -> Unit)? = null
) {
    val sujoy = LocalSujoyColors.current
    var showDisputeDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                    val (icon, tint) = when (txn.status) {
                        "SUCCESS" -> Icons.Default.CheckCircle to sujoy.success
                        "PENDING" -> Icons.Default.Schedule to sujoy.pending
                        else -> Icons.Default.Error to sujoy.failure
                    }
                    Icon(imageVector = icon, contentDescription = txn.status, tint = tint, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Payment Receipt", style = MaterialTheme.typography.titleMedium)
                        Text(txn.type.replace("_", " "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.Close, contentDescription = "Close receipt")
                }
            }

            Surface(
                color = Navy900,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (txn.direction == "CREDIT") "CREDITED AMOUNT" else "PAID AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan400
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.formatInr(txn.amountPaise),
                        style = MaterialTheme.typography.displaySmall,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusPill(status = txn.status)
                }
            }

            ReceiptDetailRow("Counterparty", txn.counterpartyName)
            ReceiptDetailRow("Handle / Ref", txn.counterpartyHandle)
            ReceiptDetailRow("UTR / Bank Ref", txn.utrNumber, isMono = true)
            ReceiptDetailRow("Gateway Order ID", txn.orderId, isMono = true)
            ReceiptDetailRow("Channel", txn.gatewayMethod.replace("_", " "))
            ReceiptDetailRow("Timestamp", Formatters.formatDateTime(txn.createdAt))
            if (txn.gatewaySignature.isNotBlank()) {
                ReceiptDetailRow("HMAC Proof", txn.gatewaySignature.take(20) + "…", isMono = true)
            }
            if (txn.note.isNotBlank()) {
                ReceiptDetailRow("Remarks", txn.note)
            }

            val isEligiblePayment = txn.status == "SUCCESS" && txn.type in listOf(
                "SEND_MONEY", "MOBILE_RECHARGE", "DTH_RECHARGE", "ELECTRICITY_BILL", "WATER_BILL"
            )
            if (isEligiblePayment) {
                val cbAmount = (txn.amountPaise * 25L) / 100L
                Surface(
                    color = sujoy.successContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = sujoy.success, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "25% Cashback Reward: +${Formatters.formatInr(cbAmount)} credited to your wallet!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = sujoy.success
                        )
                    }
                }
            }

            if (onSubmitDispute != null) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = { showDisputeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("raise_dispute_button")
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Raise Complaint / Request Refund")
                }
            }
        }
    }

    if (showDisputeDialog && onSubmitDispute != null) {
        DisputeDialog(
            utr = txn.utrNumber,
            onDismiss = { showDisputeDialog = false },
            onSubmit = { cat, desc, refund ->
                showDisputeDialog = false
                onSubmitDispute(cat, desc, refund)
                onDismiss()
            }
        )
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = if (isMono) MonoCodeStyle else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DisputeDialog(
    utr: String,
    onDismiss: () -> Unit,
    onSubmit: (category: String, description: String, requestRefund: Boolean) -> Unit
) {
    var category by remember { mutableStateOf("Transaction Dispute") }
    var description by remember { mutableStateOf("") }
    var requestRefund by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raise Grievance / Dispute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("UTR Reference: $utr", style = MonoCodeStyle, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Issue Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the issue") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { requestRefund = !requestRefund }
                ) {
                    Checkbox(checked = requestRefund, onCheckedChange = { requestRefund = it })
                    Text("Request wallet refund review", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(category, description, requestRefund) },
                enabled = description.trim().length >= 8
            ) {
                Text("Submit Ticket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MpinConfirmationDialog(
    title: String,
    subtitle: String,
    amountFormatted: String,
    onDismiss: () -> Unit,
    onConfirmMpin: (String) -> Unit
) {
    var mpin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Lock, contentDescription = "MPIN Security", tint = RoyalBlue600)
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = amountFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                OutlinedTextField(
                    value = mpin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) mpin = it },
                    label = { Text("Enter 4-Digit Security MPIN") },
                    placeholder = { Text("Default demo MPIN: 1234") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mpin_dialog_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmMpin(mpin) },
                enabled = mpin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                modifier = Modifier.testTag("mpin_dialog_confirm")
            ) {
                Text("Authorize & Pay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
