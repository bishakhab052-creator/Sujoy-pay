package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SujoyExtendedColors(
    val navySurface: Color,
    val navySurfaceElevated: Color,
    val electricAccent: Color,
    val success: Color,
    val successContainer: Color,
    val pending: Color,
    val pendingContainer: Color,
    val failure: Color,
    val failureContainer: Color,
    val cardBorder: Color
)

val LocalSujoyColors = staticCompositionLocalOf {
    SujoyExtendedColors(
        navySurface = Navy900,
        navySurfaceElevated = Navy800,
        electricAccent = ElectricCyan500,
        success = EmeraldSuccess,
        successContainer = EmeraldSurface,
        pending = AmberPending,
        pendingContainer = AmberSurface,
        failure = RedFailure,
        failureContainer = RedSurface,
        cardBorder = Slate200
    )
}

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue600,
    onPrimary = PureWhite,
    primaryContainer = IceBlue100,
    onPrimaryContainer = Navy900,
    secondary = Navy800,
    onSecondary = PureWhite,
    secondaryContainer = IceBlue50,
    onSecondaryContainer = Navy800,
    tertiary = ElectricCyan500,
    onTertiary = PureWhite,
    tertiaryContainer = IceBlue100,
    onTertiaryContainer = Navy900,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    error = RedFailure,
    onError = PureWhite,
    errorContainer = RedSurface,
    onErrorContainer = RedFailure
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlue500,
    onPrimary = PureWhite,
    primaryContainer = Navy700,
    onPrimaryContainer = IceBlue100,
    secondary = ElectricCyan400,
    onSecondary = Navy950,
    secondaryContainer = Navy800,
    onSecondaryContainer = IceBlue100,
    tertiary = ElectricCyan500,
    onTertiary = PureWhite,
    background = Navy950,
    onBackground = PureWhite,
    surface = Navy900,
    onSurface = PureWhite,
    surfaceVariant = Navy800,
    onSurfaceVariant = IceBlue200,
    outline = Navy700,
    error = RedFailure,
    onError = PureWhite,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = RedSurface
)

val SujoyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SujoyPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        SujoyExtendedColors(
            navySurface = Navy900,
            navySurfaceElevated = Navy800,
            electricAccent = ElectricCyan400,
            success = EmeraldSuccess,
            successContainer = Color(0xFF064E3B),
            pending = AmberPending,
            pendingContainer = Color(0xFF78350F),
            failure = RedFailure,
            failureContainer = Color(0xFF7F1D1D),
            cardBorder = Navy700
        )
    } else {
        SujoyExtendedColors(
            navySurface = Navy900,
            navySurfaceElevated = Navy800,
            electricAccent = ElectricCyan500,
            success = EmeraldSuccess,
            successContainer = EmeraldSurface,
            pending = AmberPending,
            pendingContainer = AmberSurface,
            failure = RedFailure,
            failureContainer = RedSurface,
            cardBorder = Slate200
        )
    }

    CompositionLocalProvider(LocalSujoyColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SujoyTypography,
            shapes = SujoyShapes,
            content = content
        )
    }
}
