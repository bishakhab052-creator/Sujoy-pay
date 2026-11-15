package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.ComplaintEntity
import com.example.data.local.UserEntity
import com.example.security.CryptoVault
import com.example.ui.components.StatusPill
import com.example.ui.theme.ElectricCyan400
import com.example.ui.theme.LocalSujoyColors
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue600
import com.example.util.Formatters

@Composable
fun ProfileScreen(
    user: UserEntity?,
    complaints: List<ComplaintEntity>,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onUpdateBank: (bank: String, account: String, ifsc: String) -> Unit,
    onUpdateMpin: (current: String, newPin: String) -> Unit,
    onLogout: () -> Unit
) {
    val sujoy = LocalSujoyColors.current
    var showBankDialog by remember { mutableStateOf(false) }
    var showMpinDialog by remember { mutableStateOf(false) }

    val decryptedBank = remember(user?.encryptedBankInfo) {
        val raw = user?.encryptedBankInfo.orEmpty()
        if (raw.isBlank()) "No linked bank account yet" else CryptoVault.decryptSensitive(raw).ifBlank { "Encrypted Bank Record" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .widthIn(max = 600.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = Navy900,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_profile_avatar),
                        contentDescription = "User avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.fullName ?: "SUJOY User",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+91 ${user?.mobileNumber ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user?.upiId ?: "",
                            style = MonoCodeStyle,
                            color = ElectricCyan400
                        )
                    }
                    if (user != null) {
                        StatusPill(status = user.kycStatus)
                    }
                }
            }
        }

        // Encrypted Bank Payout Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, sujoy.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = RoyalBlue600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Linked Bank Payout (AES-256 Encrypted)", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(decryptedBank, style = MonoCodeStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = { showBankDialog = true },
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Update Bank Account")
                    }
                }
            }
        }

        // Security & Theme Controls
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
                    Text("Security & Preferences", style = MaterialTheme.typography.titleSmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = RoyalBlue600)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Dark Navy Theme", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            modifier = Modifier.testTag("profile_dark_mode_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = { showMpinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change 4-Digit Security MPIN")
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = RoyalBlue600, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Gateway Endpoint", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    runCatching { BuildConfig.SUJOY_GATEWAY_BASE_URL }.getOrDefault("Sandbox Local"),
                                    style = MonoCodeStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Support / Dispute Tickets
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = RoyalBlue600)
                Spacer(modifier = Modifier.width(8.dp))
                Text("My Grievance & Dispute Tickets (${complaints.size})", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (complaints.isEmpty()) {
            item {
                Text(
                    "No open disputes. Tap any transaction in History to raise a dispute.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(complaints, key = { it.id }) { ticket ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, sujoy.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ticket.category, style = MaterialTheme.typography.titleSmall)
                            StatusPill(status = ticket.status)
                        }
                        Text("UTR: ${ticket.utrNumber} • ${Formatters.formatInr(ticket.amountPaise)}", style = MonoCodeStyle)
                        Text(ticket.description, style = MaterialTheme.typography.bodySmall)
                        if (ticket.adminRemarks.isNotBlank()) {
                            Text("Admin Note: ${ticket.adminRemarks}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue600)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = sujoy.failureContainer,
                    contentColor = sujoy.failure
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("profile_logout_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Logout", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showBankDialog) {
        var bankLabel by remember { mutableStateOf("") }
        var accNo by remember { mutableStateOf("") }
        var ifsc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBankDialog = false },
            title = { Text("Update Linked Bank Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = bankLabel, onValueChange = { bankLabel = it }, label = { Text("Bank Name (e.g. HDFC Bank)") }, singleLine = true)
                    OutlinedTextField(
                        value = accNo,
                        onValueChange = { accNo = it.filter(Char::isDigit).take(18) },
                        label = { Text("Account Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(value = ifsc, onValueChange = { ifsc = it.uppercase().take(11) }, label = { Text("IFSC Code") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateBank(bankLabel, accNo, ifsc)
                    showBankDialog = false
                }) { Text("Encrypt & Save") }
            },
            dismissButton = { TextButton(onClick = { showBankDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMpinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMpinDialog = false },
            title = { Text("Change 4-Digit MPIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) currentPin = it },
                        label = { Text("Current MPIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("New 4-Digit MPIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMpin(currentPin, newPin)
                        showMpinDialog = false
                    },
                    enabled = currentPin.length == 4 && newPin.length == 4
                ) { Text("Update MPIN") }
            },
            dismissButton = { TextButton(onClick = { showMpinDialog = false }) { Text("Cancel") } }
        )
    }
}
