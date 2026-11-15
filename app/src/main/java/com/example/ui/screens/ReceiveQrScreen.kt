package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.UserEntity
import com.example.ui.components.SujoyPayLogo
import com.example.ui.theme.ElectricCyan400
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue600
import com.example.util.Formatters
import com.example.util.QrCodeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveQrScreen(
    user: UserEntity?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var requestAmountText by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    val vpa = user?.upiId ?: "user@sujoypay"
    val name = user?.fullName ?: "SUJOY PAY User"
    val amountPaise = Formatters.parseRupeesToPaise(requestAmountText)
    val upiUri = remember(vpa, name, amountPaise) {
        QrCodeUtils.buildUpiUri(
            vpa = vpa,
            payeeName = name,
            amountPaise = amountPaise,
            note = "Pay $name via SUJOY PAY"
        )
    }
    val qrBitmap = remember(upiUri) {
        QrCodeUtils.generateQrBitmap(upiUri, sizePx = 640)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Receive Money • My QR") },
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
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SujoyPayLogo(lightTextOnDark = true)

                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "SUJOY PAY NPCI UPI QR Code for $vpa",
                                    modifier = Modifier
                                        .size(230.dp)
                                        .testTag("generated_upi_qr_image")
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vpa,
                                style = MonoCodeStyle,
                                color = ElectricCyan400
                            )
                            if (amountPaise != null && amountPaise > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Requested Amount: ${Formatters.formatInr(amountPaise)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                            }
                        }

                        Text(
                            text = "Scan with any UPI app (Google Pay, PhonePe, Paytm, BHIM) or SUJOY PAY",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = requestAmountText,
                    onValueChange = { raw -> requestAmountText = raw.filter { it.isDigit() || it == '.' }.take(9) },
                    label = { Text("Set Specific Request Amount (Optional)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receive_qr_amount_input")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(vpa))
                            copied = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("copy_upi_id_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copied) "Copied!" else "Copy UPI ID")
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Pay $name via SUJOY PAY")
                                putExtra(Intent.EXTRA_TEXT, "Pay $name on SUJOY PAY UPI ID: $vpa\n$upiUri")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Payment Link"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("share_upi_link_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Link")
                    }
                }
            }
        }
    }
}
