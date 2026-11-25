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

private val LightColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = NavySurface,
    onPrimaryContainer = Color.White,
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    secondaryContainer = EmeraldSoft,
    onSecondaryContainer = EmeraldDarkContainer,
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldSoft,
    onTertiaryContainer = GoldDarkContainer,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorSoft,
    onErrorContainer = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = NavyDeep,
    primaryContainer = NavySurface,
    onPrimaryContainer = Color.White,
    secondary = EmeraldBright,
    onSecondary = NavyDeep,
    secondaryContainer = EmeraldDarkContainer,
    onSecondaryContainer = EmeraldSoft,
    tertiary = GoldBright,
    onTertiary = NavyDeep,
    tertiaryContainer = GoldDarkContainer,
    onTertiaryContainer = GoldSoft,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = Color(0xFFF87171),
    onError = NavyDeep,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val RealRewardShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RealRewardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val activeColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = activeColorScheme,
        typography = RealRewardTypography,
        shapes = RealRewardShapes,
        content = content
    )
}
