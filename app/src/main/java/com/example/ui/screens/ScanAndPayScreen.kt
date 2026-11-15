package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ElectricCyan400
import com.example.ui.theme.LocalSujoyColors
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue600
import com.example.util.ParsedUpiPayload
import com.example.util.QrCodeUtils
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanAndPayScreen(
    onBack: () -> Unit,
    onUpiPayloadScanned: (ParsedUpiPayload) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sujoy = LocalSujoyColors.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var manualInput by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var lastDecoded by remember { mutableStateOf<ParsedUpiPayload?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Play-policy compliant Android Photo Picker (zero storage permissions!)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val decoded = QrCodeUtils.decodeFromBitmap(bitmap)
                val parsed = decoded?.let { QrCodeUtils.parseUpiPayload(it) }
                if (parsed != null) {
                    scanError = null
                    lastDecoded = parsed
                } else {
                    scanError = "No valid UPI QR code found in the selected image."
                }
            } catch (e: Throwable) {
                scanError = "Unable to decode image: ${e.localizedMessage}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Scan & Pay UPI QR") },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (hasCameraPermission) {
                    IconButton(
                        onClick = {
                            val next = !torchEnabled
                            torchEnabled = next
                            cameraRef?.cameraControl?.enableTorch(next)
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                            contentDescription = "Toggle Flashlight"
                        )
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live CameraX Preview / Permission Request Card
            item {
                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    if (hasCameraPermission) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
                            DisposableEffect(Unit) {
                                onDispose { analysisExecutor.shutdown() }
                            }
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        try {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.surfaceProvider = previewView.surfaceProvider
                                            }
                                            val imageAnalysis = ImageAnalysis.Builder()
                                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .build()
                                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                                try {
                                                    val buffer = imageProxy.planes[0].buffer
                                                    val bytes = ByteArray(buffer.remaining())
                                                    buffer.get(bytes)
                                                    val rawText = QrCodeUtils.decodeFromYuv(
                                                        bytes,
                                                        imageProxy.width,
                                                        imageProxy.height
                                                    )
                                                    if (rawText != null) {
                                                        val parsed = QrCodeUtils.parseUpiPayload(rawText)
                                                        if (parsed != null) {
                                                            lastDecoded = parsed
                                                            scanError = null
                                                        }
                                                    }
                                                } catch (_: Throwable) {
                                                } finally {
                                                    imageProxy.close()
                                                }
                                            }
                                            cameraProvider.unbindAll()
                                            cameraRef = cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (_: Throwable) {
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Viewfinder frame overlay
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .align(Alignment.Center)
                                    .border(2.5.dp, ElectricCyan400, RoundedCornerShape(20.dp))
                            )
                            Text(
                                text = "Align NPCI / UPI QR inside frame",
                                style = MaterialTheme.typography.labelMedium,
                                color = PureWhite,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 14.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Navy900.copy(alpha = 0.78f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera permission required",
                                tint = ElectricCyan400,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Enable Camera to Scan Live QR Codes",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Or pick a saved QR image from your gallery or enter a UPI URI/ID below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                                modifier = Modifier.testTag("grant_camera_btn")
                            ) {
                                Text("Allow Camera Access")
                            }
                        }
                    }
                }
            }

            // Decoded QR confirmation card
            lastDecoded?.let { decoded ->
                item {
                    Surface(
                        color = sujoy.successContainer,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, sujoy.success),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("UPI QR Verified", style = MaterialTheme.typography.titleMedium, color = sujoy.success, fontWeight = FontWeight.Bold)
                            Text("Payee: ${decoded.payeeName}", style = MaterialTheme.typography.bodyLarge)
                            Text("UPI ID: ${decoded.payeeAddress}", style = MaterialTheme.typography.bodyMedium)
                            if (!decoded.amountRupees.isNullOrBlank()) {
                                Text("Amount: ₹${decoded.amountRupees}", style = MaterialTheme.typography.titleSmall)
                            }
                            Button(
                                onClick = { onUpiPayloadScanned(decoded) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("proceed_scanned_upi_btn")
                            ) {
                                Text("Proceed to Pay")
                            }
                        }
                    }
                }
            }

            // Pick from Gallery (Zero-permission Photo Picker)
            item {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pick_qr_gallery_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan QR from Gallery (Photo Picker)")
                }
            }

            // Emulator / Manual UPI QR Payload input
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, sujoy.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = RoyalBlue600, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Or Enter / Paste UPI QR Payload", style = MaterialTheme.typography.titleSmall)
                        }
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it; scanError = null },
                            label = { Text("UPI ID or upi://pay?... URI") },
                            placeholder = { Text("e.g. 9811223344@sujoypay") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_qr_input")
                        )
                        if (!scanError.isNullOrBlank()) {
                            Text(scanError!!, style = MaterialTheme.typography.bodySmall, color = sujoy.failure)
                        }
                        Button(
                            onClick = {
                                val parsed = QrCodeUtils.parseUpiPayload(manualInput)
                                if (parsed != null) {
                                    scanError = null
                                    onUpiPayloadScanned(parsed)
                                } else {
                                    scanError = "Invalid UPI ID or upi://pay QR URI format."
                                }
                            },
                            enabled = manualInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue600),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("parse_manual_qr_btn")
                        ) {
                            Text("Verify & Continue")
                        }
                    }
                }
            }
        }
    }
}
