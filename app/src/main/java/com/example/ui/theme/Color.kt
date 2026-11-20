package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Bharat Tools SaaS Design Tokens — Deep Indigo, Electric Blue, Purple Accent
val IndigoPrimaryLight = Color(0xFF1E3A8A)
val IndigoPrimaryDarkVariant = Color(0xFF152963)
val OnIndigoLight = Color(0xFFFFFFFF)
val IndigoContainerLight = Color(0xFFE0E7FF)
val OnIndigoContainerLight = Color(0xFF112156)

val ElectricBlueSecondaryLight = Color(0xFF2563EB)
val OnElectricBlueLight = Color(0xFFFFFFFF)
val ElectricBlueContainerLight = Color(0xFFDBEAFE)
val OnElectricBlueContainerLight = Color(0xFF0B2B6B)

val PurpleAccentLight = Color(0xFF7C3AED)
val OnPurpleLight = Color(0xFFFFFFFF)
val PurpleContainerLight = Color(0xFFEDE9FE)
val OnPurpleContainerLight = Color(0xFF3B1286)

val BackgroundLight = Color(0xFFF6F8FC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceSoftLight = Color(0xFFEEF2F8)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFDCE3EF)

// Dark Palette — Deep Charcoal with Controlled Indigo / Electric Blue / Purple Accents
val IndigoPrimaryDark = Color(0xFF93C5FD)
val OnIndigoDark = Color(0xFF0B1F4D)
val IndigoContainerDark = Color(0xFF1E3266)
val OnIndigoContainerDark = Color(0xFFE0E7FF)

val ElectricBlueSecondaryDark = Color(0xFF60A5FA)
val OnElectricBlueDark = Color(0xFF082449)
val ElectricBlueContainerDark = Color(0xFF173A6E)
val OnElectricBlueContainerDark = Color(0xFFDBEAFE)

val PurpleAccentDark = Color(0xFFA78BFA)
val OnPurpleDark = Color(0xFF250B57)
val PurpleContainerDark = Color(0xFF3B1D7A)
val OnPurpleContainerDark = Color(0xFFEDE9FE)

val BackgroundDark = Color(0xFF0B0F17)
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF121824)
val SurfaceSoftDark = Color(0xFF1A2233)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF273247)

// Semantic Status Tokens
val SuccessLight = Color(0xFF16A34A)
val SuccessContainerLight = Color(0xFFDCFCE7)
val WarningLight = Color(0xFFD97706)
val WarningContainerLight = Color(0xFFFEF3C7)
val DangerLight = Color(0xFFDC2626)
val DangerContainerLight = Color(0xFFFEE2E2)

val SuccessDark = Color(0xFF4ADE80)
val SuccessContainerDark = Color(0xFF123622)
val WarningDark = Color(0xFFFBBF24)
val WarningContainerDark = Color(0xFF3E2908)
val DangerDark = Color(0xFFF87171)
val DangerContainerDark = Color(0xFF451416)

// Category Accent Tokens
val CatPdfColor = Color(0xFFDC2626)
val CatImageColor = Color(0xFF2563EB)
val CatTextColor = Color(0xFF1E3A8A)
val CatQrColor = Color(0xFF7C3AED)
val CatUtilityColor = Color(0xFF0D9488)

@Immutable
data class BharatColorTokens(
    val primary: Color,
    val primaryDark: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val isDark: Boolean
)

val LightBharatTokens = BharatColorTokens(
    primary = IndigoPrimaryLight,
    primaryDark = IndigoPrimaryDarkVariant,
    secondary = ElectricBlueSecondaryLight,
    accent = PurpleAccentLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceSoft = SurfaceSoftLight,
    textPrimary = OnBackgroundLight,
    textSecondary = OnSurfaceVariantLight,
    border = OutlineLight,
    success = SuccessLight,
    successContainer = SuccessContainerLight,
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    danger = DangerLight,
    dangerContainer = DangerContainerLight,
    isDark = false
)

val DarkBharatTokens = BharatColorTokens(
    primary = IndigoPrimaryDark,
    primaryDark = IndigoContainerDark,
    secondary = ElectricBlueSecondaryDark,
    accent = PurpleAccentDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceSoft = SurfaceSoftDark,
    textPrimary = OnBackgroundDark,
    textSecondary = OnSurfaceVariantDark,
    border = OutlineDark,
    success = SuccessDark,
    successContainer = SuccessContainerDark,
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    danger = DangerDark,
    dangerContainer = DangerContainerDark,
    isDark = true
)

val LocalBharatColors = staticCompositionLocalOf { LightBharatTokens }
