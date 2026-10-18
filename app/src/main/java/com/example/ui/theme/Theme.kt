package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    RetroLedgerTheme(darkTheme = darkTheme, content = content)
}
