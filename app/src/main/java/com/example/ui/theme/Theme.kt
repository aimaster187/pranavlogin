package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PranavLightColorScheme = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = LuminousCyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF164E63),
    background = IceWhite,
    onBackground = InkDark,
    surface = IceSurface,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFEEF2FF),
    onSurfaceVariant = IceSubtleText,
    outline = Color(0xFFCBD5E1),
    error = CrimsonError,
    onError = Color.White
)

private val PranavDarkColorScheme = darkColorScheme(
    primary = VividViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0764),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = LuminousCyan,
    onTertiary = Color(0xFF042F2E),
    background = MidnightBg,
    onBackground = IceTextLight,
    surface = MidnightSurface,
    onSurface = IceTextLight,
    surfaceVariant = Color(0xFF1E2548),
    onSurfaceVariant = MidnightSubtleText,
    outline = Color(0xFF334155),
    error = CrimsonError,
    onError = Color.White
)

val PranavShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PranavAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) PranavDarkColorScheme else PranavLightColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = PranavTypography,
        shapes = PranavShapes,
        content = content
    )
}
