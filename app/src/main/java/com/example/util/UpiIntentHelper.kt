package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

enum class UpiAppChannel(
    val displayName: String,
    val packageName: String?,
    val subtitle: String
) {
    GOOGLE_PAY(
        displayName = "Google Pay (UPI Gateway)",
        packageName = "com.google.android.apps.nbu.paisa.user",
        subtitle = "Authorized NPCI UPI intent via gateway"
    ),
    PHONEPE(
        displayName = "PhonePe (UPI)",
        packageName = "com.phonepe.app",
        subtitle = "Instant UPI collect / intent flow"
    ),
    OTHER_UPI(
        displayName = "Other Supported UPI Apps",
        packageName = null,
        subtitle = "BHIM, Paytm, Cred, Amazon Pay & Bank UPI"
    )
}

sealed interface UpiLaunchOutcome {
    data class LaunchedExternalApp(val appName: String) : UpiLaunchOutcome
    data class NoUpiAppInstalledFallbackToSandbox(val reason: String) : UpiLaunchOutcome
}

object UpiIntentHelper {
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun launchUpiPaymentIntent(
        context: Context,
        upiUri: String,
        channel: UpiAppChannel
    ): UpiLaunchOutcome {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)).apply {
            if (!channel.packageName.isNullOrBlank()) {
                setPackage(channel.packageName)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pm = context.packageManager
        val resolved = intent.resolveActivity(pm)
        return if (resolved != null) {
            try {
                if (channel.packageName == null) {
                    val chooser = Intent.createChooser(intent, "Pay with UPI App").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } else {
                    context.startActivity(intent)
                }
                UpiLaunchOutcome.LaunchedExternalApp(channel.displayName)
            } catch (e: Throwable) {
                UpiLaunchOutcome.NoUpiAppInstalledFallbackToSandbox(
                    e.localizedMessage ?: "Unable to open external UPI app"
                )
            }
        } else {
            UpiLaunchOutcome.NoUpiAppInstalledFallbackToSandbox(
                "${channel.displayName} is not installed on this device/emulator. Completing verification via Authorized Sandbox UPI Checkout."
            )
        }
    }
}
