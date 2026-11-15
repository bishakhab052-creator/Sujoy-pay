package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.ui.components.SandboxBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.SujoyPayLogo
import com.example.ui.components.TransactionReceiptSheet
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.ElectricCyan400
import com.example.ui.theme.LocalSujoyColors
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue600
import com.example.ui.theme.RoyalBlue700
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    user: UserEntity?,
    transactions: List<TransactionEntity>,
    hideBalance: Boolean,
    onToggleHideBalance: () -> Unit,
    onNavigateAddMoney: () -> Unit,
    onNavigateSendMoney: () -> Unit,
    onNavigateWithdraw: () -> Unit,
    onNavigateScanAndPay: () -> Unit,
    onNavigateReceiveQr: () -> Unit,
    onNavigateBillCategory: (String) -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateProfile: () -> Unit,
    onSubmitDispute: (TransactionEntity, String, String, Boolean) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copiedFeedback by remember { mutableStateOf(false) }
    var selectedReceiptTxn by remember { mutableStateOf<TransactionEntity?>(null) }
    val sujoy = LocalSujoyColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Bar: Logo + Sandbox Badge + Profile Avatar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SujoyPayLogo()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SandboxBadge()
                    Image(
                        painter = painterResource(id = R.drawable.img_profile_avatar),
                        contentDescription = "Open Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onNavigateProfile)
                            .testTag("dashboard_profile_avatar")
                    )
                }
            }
        }

        // 2. Welcome & KYC Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Namaste, ${user?.fullName?.substringBefore(" ") ?: "Guest"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (user?.isFrozen == true) "Account Frozen • Contact Support" else "Verified Indian Digital Wallet",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user?.isFrozen == true) sujoy.failure else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (user != null) {
                    StatusPill(status = user.kycStatus)
                }
            }
        }

        // 3. Premium Dark Navy & Royal Blue Wallet Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Navy950, Navy900, RoyalBlue700)
                            )
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_fintech_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.18f)
                    )
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Verified Escrow",
                                    tint = ElectricCyan400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AVAILABLE WALLET BALANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = onToggleHideBalance,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (hideBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (hideBalance) "Show balance" else "Hide balance",
                                    tint = PureWhite
                                )
                            }
                        }

                        Text(
                            text = if (hideBalance) "₹ ••••••" else Formatters.formatInr(user?.balancePaise ?: 0L),
                            style = MaterialTheme.typography.displayLarge,
                            color = PureWhite,
                            modifier = Modifier.testTag("dashboard_wallet_balance")
                        )

                        // UPI ID Copy Chip
                        Surface(
                            color = PureWhite.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable {
                                val upi = user?.upiId.orEmpty()
                                if (upi.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(upi))
                                    copiedFeedback = true
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UPI: ${user?.upiId ?: "—"}",
                                    style = MonoCodeStyle,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy UPI ID",
                                    tint = ElectricCyan400,
                                    modifier = Modifier.size(14.dp)
                                )
                                AnimatedVisibility(visible = copiedFeedback) {
                                    Text(
                                        text = "  Copied",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan400
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateAddMoney,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite,
                                    contentColor = Navy900
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_add_money_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Money", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onNavigateWithdraw,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite.copy(alpha = 0.16f),
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_withdraw_btn")
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Withdraw", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 3b. 25% Instant Cashback Reward Banner
        item {
            Surface(
                color = sujoy.successContainer,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, sujoy.success.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_cashback_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "25% Cashback",
                            tint = sujoy.success,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "25% Instant Cashback Active",
                                style = MaterialTheme.typography.titleSmall,
                                color = sujoy.success,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Earned on every UPI transfer & bill payment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EARNED", style = MaterialTheme.typography.labelSmall, color = sujoy.success)
                        Text(
                            text = Formatters.formatInrCompact(user?.totalCashbackEarnedPaise ?: 0L),
                            style = MaterialTheme.typography.titleMedium,
                            color = sujoy.success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Primary Payment Quick Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Payments", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        label = "Send\nMoney",
                        icon = Icons.AutoMirrored.Filled.Send,
                        containerColor = RoyalBlue600,
                        iconTint = PureWhite,
                        onClick = onNavigateSendMoney,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_send_money")
                    )
                    QuickActionTile(
                        label = "Scan &\nPay QR",
                        icon = Icons.Default.QrCodeScanner,
                        containerColor = Navy800,
                        iconTint = ElectricCyan400,
                        onClick = onNavigateScanAndPay,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_scan_pay")
                    )
                    QuickActionTile(
                        label = "Receive\nMy QR",
                        icon = Icons.Default.QrCode,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = RoyalBlue600,
                        onClick = onNavigateReceiveQr,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_receive_qr")
                    )
                    QuickActionTile(
                        label = "Add\nMoney",
                        icon = Icons.Default.Add,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = RoyalBlue600,
                        onClick = onNavigateAddMoney,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_money")
                    )
                }
            }
        }

        // 5. Recharge & Utility Bills (BBPS)
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, sujoy.cardBorder),
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
                        Text("Recharge & Bills", style = MaterialTheme.typography.titleMedium)
                        Text("BBPS Instant", style = MaterialTheme.typography.labelSmall, color = RoyalBlue600)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BillTile(
                            label = "Mobile",
                            icon = Icons.Default.PhoneAndroid,
                            onClick = { onNavigateBillCategory("MOBILE_RECHARGE") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_mobile")
                        )
                        BillTile(
                            label = "DTH",
                            icon = Icons.Default.Tv,
                            onClick = { onNavigateBillCategory("DTH_RECHARGE") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_dth")
                        )
                        BillTile(
                            label = "Electricity",
                            icon = Icons.Default.Bolt,
                            onClick = { onNavigateBillCategory("ELECTRICITY_BILL") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_electricity")
                        )
                        BillTile(
                            label = "Water",
                            icon = Icons.Default.WaterDrop,
                            onClick = { onNavigateBillCategory("WATER_BILL") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_water")
                        )
                    }
                }
            }
        }

        // 6. Recent Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNavigateHistory) {
                    Text("View All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = RoyalBlue600,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No transactions yet", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Add money via verified UPI gateway to start transacting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(transactions.take(5), key = { it.id }) { txn ->
                TransactionRowCard(
                    txn = txn,
                    onClick = { selectedReceiptTxn = txn }
                )
            }
        }
    }

    selectedReceiptTxn?.let { txn ->
        TransactionReceiptSheet(
            txn = txn,
            onDismiss = { selectedReceiptTxn = null },
            onSubmitDispute = { cat, desc, refund ->
                onSubmitDispute(txn, cat, desc, refund)
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BillTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
