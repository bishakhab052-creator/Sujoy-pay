package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.AuditLogEntity
import com.example.data.local.ComplaintEntity
import com.example.data.local.PaymentSettingsEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.ui.components.StatusPill
import com.example.ui.theme.ElectricCyan400
import com.example.ui.theme.LocalSujoyColors
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue600
import com.example.ui.viewmodel.AdminUiState
import com.example.util.Formatters

@Composable
fun AdminPanelScreen(
    currentUser: UserEntity?,
    adminState: AdminUiState,
    users: List<UserEntity>,
    transactions: List<TransactionEntity>,
    flaggedTransactions: List<TransactionEntity>,
    complaints: List<ComplaintEntity>,
    settings: PaymentSettingsEntity?,
    auditLogs: List<AuditLogEntity>,
    onUnlockAdmin: (String) -> Unit,
    onLockAdmin: () -> Unit,
    onToggleFreeze: (userId: String, freeze: Boolean) -> Unit,
    onUpdateKyc: (userId: String, status: String) -> Unit,
    onUpdateSettings: (PaymentSettingsEntity) -> Unit,
    onResolveComplaint: (complaintId: String, status: String, remarks: String, refund: Boolean) -> Unit,
    onResolveFraud: (txnId: String, clearFlag: Boolean, freezeUser: Boolean) -> Unit
) {
    val sujoy = LocalSujoyColors.current
    var adminPinInput by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("REPORTS") }

    if (!adminState.isUnlocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Navy900,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Security Gate",
                        tint = ElectricCyan400,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "SUJOY PAY Admin Console",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (currentUser?.role == "ADMIN") {
                            "Enter Admin Password to unlock compliance, fraud monitoring & refund controls."
                        } else {
                            "Restricted to ADMIN accounts. Log out and sign in via 'Admin' on the Auth screen to inspect full governance tools."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.78f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = adminPinInput,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) adminPinInput = it },
                label = { Text("Admin Password (4-Digit)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_pin_input")
            )
            if (!adminState.pinError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(adminState.pinError, color = sujoy.failure, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { onUnlockAdmin(adminPinInput) },
                enabled = adminPinInput.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_unlock_btn")
            ) {
                Text("Unlock Admin Dashboard")
            }
        }
        return
    }

    val sections = listOf("REPORTS", "USERS", "FRAUD", "COMPLAINTS", "SETTINGS")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Navy900)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Admin Governance", style = MaterialTheme.typography.titleLarge, color = PureWhite, fontWeight = FontWeight.Bold)
                Text("Compliance • Fraud • Refunds • Audit", style = MaterialTheme.typography.labelSmall, color = ElectricCyan400)
            }
            OutlinedButton(
                onClick = onLockAdmin,
                border = BorderStroke(1.dp, ElectricCyan400)
            ) {
                Text("Lock", color = PureWhite)
            }
        }

        if (!adminState.statusBanner.isNullOrBlank()) {
            Surface(color = sujoy.successContainer, modifier = Modifier.fillMaxWidth()) {
                Text(adminState.statusBanner, color = sujoy.success, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sections) { sec ->
                FilterChip(
                    selected = selectedSection == sec,
                    onClick = { selectedSection = sec },
                    label = {
                        val badge = when (sec) {
                            "FRAUD" -> " (${flaggedTransactions.size})"
                            "COMPLAINTS" -> " (${complaints.count { it.status == "OPEN" }})"
                            else -> ""
                        }
                        Text("$sec$badge")
                    },
                    modifier = Modifier.testTag("admin_tab_${sec.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedSection) {
                "REPORTS" -> {
                    val totalFloat = users.sumOf { it.balancePaise }
                    val totalCashback = users.sumOf { it.totalCashbackEarnedPaise }
                    val successTxns = transactions.filter { it.status == "SUCCESS" }
                    val totalVol = successTxns.sumOf { it.amountPaise }
                    val successRate = if (transactions.isEmpty()) 100 else (successTxns.size * 100) / transactions.size

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminStatCard("Wallet Float", Formatters.formatInrCompact(totalFloat), Modifier.weight(1f))
                            AdminStatCard("Settled Vol", Formatters.formatInrCompact(totalVol), Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminStatCard("Success Rate", "$successRate%", Modifier.weight(1f))
                            AdminStatCard("25% Cashback Paid", Formatters.formatInrCompact(totalCashback), Modifier.weight(1f))
                        }
                    }
                    item {
                        PaymentCategoryReportChart(transactions = transactions)
                    }
                    item {
                        Text("Recent Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(auditLogs.take(15), key = { it.id }) { log ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, sujoy.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.action, style = MonoCodeStyle, color = RoyalBlue600)
                                    StatusPill(status = log.severity)
                                }
                                Text(log.details, style = MaterialTheme.typography.bodySmall)
                                Text("${log.actorRole} • ${Formatters.formatShortDate(log.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                "USERS" -> {
                    items(users, key = { it.id }) { u ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, sujoy.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("${u.fullName} (${u.role})", style = MaterialTheme.typography.titleSmall)
                                        Text("${u.upiId} • +91 ${u.mobileNumber}", style = MonoCodeStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(status = if (u.isFrozen) "FROZEN" else u.kycStatus)
                                }
                                Text("Balance: ${Formatters.formatInr(u.balancePaise)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onToggleFreeze(u.id, !u.isFrozen) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (u.isFrozen) "Unfreeze" else "Freeze Wallet")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val nextKyc = if (u.kycStatus == "VERIFIED") "MIN_KYC" else "VERIFIED"
                                            onUpdateKyc(u.id, nextKyc)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (u.kycStatus == "VERIFIED") "Set Min-KYC" else "Verify KYC")
                                    }
                                }
                            }
                        }
                    }
                }

                "FRAUD" -> {
                    item {
                        Text("Flagged & High-Risk Transactions", style = MaterialTheme.typography.titleMedium)
                    }
                    if (flaggedTransactions.isEmpty()) {
                        item {
                            Text("Zero active fraud alerts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(flaggedTransactions, key = { "flag_${it.id}" }) { txn ->
                            Surface(
                                color = sujoy.failureContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, sujoy.failure),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = sujoy.failure, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Risk Score: ${txn.riskScore}/100 • ${Formatters.formatInr(txn.amountPaise)}", style = MaterialTheme.typography.titleSmall, color = sujoy.failure)
                                    }
                                    Text("${txn.counterpartyName} (${txn.utrNumber})", style = MonoCodeStyle)
                                    Text(txn.note, style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onResolveFraud(txn.id, true, false) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Clear Flag") }
                                        Button(
                                            onClick = { onResolveFraud(txn.id, false, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = sujoy.failure),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Block & Freeze") }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("All Platform Transactions (${transactions.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(transactions.take(25), key = { "all_${it.id}" }) { txn ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, sujoy.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${txn.type} • ${txn.counterpartyName}", style = MaterialTheme.typography.titleSmall)
                                    Text("${txn.utrNumber} • Risk ${txn.riskScore}", style = MonoCodeStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Formatters.formatInr(txn.amountPaise), style = MaterialTheme.typography.titleSmall)
                                    StatusPill(status = txn.status)
                                }
                            }
                        }
                    }
                }

                "COMPLAINTS" -> {
                    items(complaints, key = { it.id }) { cmp ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, sujoy.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${cmp.category} • ${cmp.userName}", style = MaterialTheme.typography.titleSmall)
                                    StatusPill(status = cmp.status)
                                }
                                Text("UTR: ${cmp.utrNumber} • Amount: ${Formatters.formatInr(cmp.amountPaise)}", style = MonoCodeStyle)
                                Text(cmp.description, style = MaterialTheme.typography.bodySmall)
                                Text("Refund Status: ${cmp.refundStatus}", style = MaterialTheme.typography.labelMedium, color = RoyalBlue600)

                                if (cmp.status != "RESOLVED" && cmp.status != "REJECTED") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (cmp.refundRequested && cmp.refundStatus != "PROCESSED") {
                                            Button(
                                                onClick = {
                                                    onResolveComplaint(
                                                        cmp.id,
                                                        "RESOLVED",
                                                        "Verified & Refund credited to wallet.",
                                                        true
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = sujoy.success),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("admin_refund_btn_${cmp.id}")
                                            ) {
                                                Text("Issue Refund")
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onResolveComplaint(cmp.id, "RESOLVED", "Marked resolved by Support.", false)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Resolve")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onResolveComplaint(cmp.id, "REJECTED", "Declined after UTR audit.", false)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "SETTINGS" -> {
                    val current = settings ?: PaymentSettingsEntity()
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, sujoy.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Gateway & Compliance Controls", style = MaterialTheme.typography.titleMedium)
                                SettingsToggleRow("${current.cashbackPercent}% Instant Cashback", current.cashbackEnabled) {
                                    onUpdateSettings(current.copy(cashbackEnabled = it))
                                }
                                SettingsToggleRow("Sandbox Gateway Mode", current.sandboxModeEnabled) {
                                    onUpdateSettings(current.copy(sandboxModeEnabled = it))
                                }
                                SettingsToggleRow("Google Pay (UPI Gateway)", current.gpayUpiEnabled) {
                                    onUpdateSettings(current.copy(gpayUpiEnabled = it))
                                }
                                SettingsToggleRow("PhonePe UPI Channel", current.phonePeUpiEnabled) {
                                    onUpdateSettings(current.copy(phonePeUpiEnabled = it))
                                }
                                SettingsToggleRow("Other UPI Apps", current.otherUpiEnabled) {
                                    onUpdateSettings(current.copy(otherUpiEnabled = it))
                                }
                                SettingsToggleRow("Bank Withdrawals (IMPS)", current.withdrawalsEnabled) {
                                    onUpdateSettings(current.copy(withdrawalsEnabled = it))
                                }
                                SettingsToggleRow("Emergency Maintenance Mode", current.maintenanceMode) {
                                    onUpdateSettings(current.copy(maintenanceMode = it))
                                }
                                Text(
                                    "NPCI Per-Txn Cap: ${Formatters.formatInr(current.maxPerTxnPaise)} | Full-KYC Wallet Cap: ${Formatters.formatInr(current.walletMaxBalancePaise)}",
                                    style = MonoCodeStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Navy900,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = ElectricCyan400)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = PureWhite, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PaymentCategoryReportChart(transactions: List<TransactionEntity>) {
    val sujoy = LocalSujoyColors.current
    val buckets = remember(transactions) {
        val addSum = transactions.filter { it.type == "ADD_MONEY" && it.status == "SUCCESS" }.sumOf { it.amountPaise }
        val sendSum = transactions.filter { it.type == "SEND_MONEY" && it.status == "SUCCESS" }.sumOf { it.amountPaise }
        val billsSum = transactions.filter {
            it.type in listOf("MOBILE_RECHARGE", "DTH_RECHARGE", "ELECTRICITY_BILL", "WATER_BILL") && it.status == "SUCCESS"
        }.sumOf { it.amountPaise }
        val wdSum = transactions.filter { it.type == "WITHDRAW" && it.status == "SUCCESS" }.sumOf { it.amountPaise }
        listOf(
            "Top-Up" to addSum,
            "Transfers" to sendSum,
            "BBPS Bills" to billsSum,
            "Withdraw" to wdSum
        )
    }
    val maxVal = (buckets.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, sujoy.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Settled Volume by Category", style = MaterialTheme.typography.titleSmall)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val barCount = buckets.size
                val slotWidth = size.width / barCount
                val barWidth = slotWidth * 0.48f
                buckets.forEachIndexed { idx, (_, amount) ->
                    val ratio = (amount.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1f)
                    val barHeight = size.height * ratio
                    val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val y = size.height - barHeight
                    drawRoundRect(
                        color = if (idx % 2 == 0) RoyalBlue600 else ElectricCyan400,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                buckets.forEach { (label, amount) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                        Text(Formatters.formatInrCompact(amount), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
