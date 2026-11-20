package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = OnIndigoLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = OnIndigoContainerLight,
    secondary = ElectricBlueSecondaryLight,
    onSecondary = OnElectricBlueLight,
    secondaryContainer = ElectricBlueContainerLight,
    onSecondaryContainer = OnElectricBlueContainerLight,
    tertiary = PurpleAccentLight,
    onTertiary = OnPurpleLight,
    tertiaryContainer = PurpleContainerLight,
    onTertiaryContainer = OnPurpleContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceSoftLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = DangerLight,
    errorContainer = DangerContainerLight,
    onError = OnIndigoLight,
    onErrorContainer = ColorTokensHelper.onDangerLight
)

private val DarkColors = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = OnIndigoDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnIndigoContainerDark,
    secondary = ElectricBlueSecondaryDark,
    onSecondary = OnElectricBlueDark,
    secondaryContainer = ElectricBlueContainerDark,
    onSecondaryContainer = OnElectricBlueContainerDark,
    tertiary = PurpleAccentDark,
    onTertiary = OnPurpleDark,
    tertiaryContainer = PurpleContainerDark,
    onTertiaryContainer = OnPurpleContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceSoftDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = DangerDark,
    errorContainer = DangerContainerDark,
    onError = OnIndigoDark,
    onErrorContainer = ColorTokensHelper.onDangerDark
)

private object ColorTokensHelper {
    val onDangerLight = androidx.compose.ui.graphics.Color(0xFF7F1D1D)
    val onDangerDark = androidx.compose.ui.graphics.Color(0xFFFECACA)
}

object BharatSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

val BharatShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object BharatTheme {
    val colors: BharatColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalBharatColors.current
}

@Composable
fun BharatToolsTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val tokens = if (darkTheme) DarkBharatTokens else LightBharatTokens

    CompositionLocalProvider(LocalBharatColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BharatTypography,
            shapes = BharatShapes,
            content = content
        )
    }
}
