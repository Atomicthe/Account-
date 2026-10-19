package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

private val RetroLightColorScheme = lightColorScheme(
    primary = WarmSepia,
    onPrimary = ParchmentSurface,
    primaryContainer = WarmSepiaLight,
    onPrimaryContainer = EspressoInk,
    secondary = VintageGold,
    onSecondary = EspressoInk,
    secondaryContainer = VintageGoldLight,
    onSecondaryContainer = EspressoInk,
    tertiary = ForestGreen,
    onTertiary = ParchmentSurface,
    tertiaryContainer = ForestGreenLight,
    onTertiaryContainer = EspressoInk,
    background = ParchmentBg,
    onBackground = EspressoInk,
    surface = ParchmentSurface,
    onSurface = EspressoInk,
    surfaceVariant = ParchmentCard,
    onSurfaceVariant = EspressoSecondary,
    outline = ParchmentBorder,
    error = SealingWaxRed,
    onError = ParchmentSurface,
    errorContainer = SealingWaxRedLight,
    onErrorContainer = SealingWaxRed
)

private val RetroDarkColorScheme = darkColorScheme(
    primary = VintageGold,
    onPrimary = DarkEspressoBg,
    primaryContainer = WarmSepia,
    onPrimaryContainer = DarkParchmentText,
    secondary = WarmSepia,
    onSecondary = DarkParchmentText,
    secondaryContainer = DarkEspressoCard,
    onSecondaryContainer = VintageGold,
    tertiary = ForestGreen,
    onTertiary = DarkParchmentText,
    background = DarkEspressoBg,
    onBackground = DarkParchmentText,
    surface = DarkEspressoSurface,
    onSurface = DarkParchmentText,
    surfaceVariant = DarkEspressoCard,
    onSurfaceVariant = EspressoMuted,
    outline = DarkEspressoBorder,
    error = SealingWaxRed,
    onError = DarkParchmentText
)

@Composable
fun RetroLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RetroDarkColorScheme else RetroLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RetroTypography,
        content = content
    )
}

val LocalFontSizeOffset = androidx.compose.runtime.compositionLocalOf { 0 }

@Composable
fun ThemedApp(
    darkTheme: Boolean = false,
    fontSizeOffset: Int = 0, // 0, 2, 4, 6, 8
    content: @Composable () -> Unit
) {
    val typography = RetroTypography.run {
        copy(
            bodyLarge = bodyLarge.copy(fontSize = (bodyLarge.fontSize.value + fontSizeOffset).sp),
            bodyMedium = bodyMedium.copy(fontSize = (bodyMedium.fontSize.value + fontSizeOffset).sp),
            bodySmall = bodySmall.copy(fontSize = (bodySmall.fontSize.value + fontSizeOffset).sp),
            titleLarge = titleLarge.copy(fontSize = (titleLarge.fontSize.value + fontSizeOffset).sp),
            titleMedium = titleMedium.copy(fontSize = (titleMedium.fontSize.value + fontSizeOffset).sp),
        )
    }
    
    val colorScheme = if (darkTheme) RetroDarkColorScheme else RetroLightColorScheme
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val fontScaleFactor = 1.0f + (fontSizeOffset * 0.08f)
    val scaledDensity = androidx.compose.ui.unit.Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScaleFactor
    )
    
    androidx.compose.runtime.CompositionLocalProvider(
        LocalFontSizeOffset provides fontSizeOffset,
        androidx.compose.ui.platform.LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    RetroLedgerTheme(darkTheme = darkTheme, content = content)
}
