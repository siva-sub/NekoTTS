package com.nekotts.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val NekoDarkColorScheme = darkColorScheme(
    primary = NekoDarkPrimary,
    onPrimary = NekoDarkOnPrimary,
    primaryContainer = NekoDarkPrimaryContainer,
    onPrimaryContainer = NekoDarkOnPrimaryContainer,
    secondary = NekoDarkSecondary,
    onSecondary = NekoDarkOnSecondary,
    secondaryContainer = NekoDarkSecondaryContainer,
    onSecondaryContainer = NekoDarkOnSecondaryContainer,
    tertiary = NekoDarkTertiary,
    onTertiary = NekoDarkOnTertiary,
    tertiaryContainer = NekoDarkTertiaryContainer,
    onTertiaryContainer = NekoDarkOnTertiaryContainer,
    error = NekoDarkError,
    errorContainer = NekoDarkErrorContainer,
    onError = NekoDarkOnError,
    onErrorContainer = NekoDarkOnErrorContainer,
    background = NekoDarkBackground,
    onBackground = NekoDarkOnBackground,
    surface = NekoDarkSurface,
    onSurface = NekoDarkOnSurface,
    surfaceVariant = NekoDarkSurfaceVariant,
    onSurfaceVariant = NekoDarkOnSurfaceVariant,
    outline = NekoDarkOutline,
    inverseOnSurface = NekoDarkInverseOnSurface,
    inverseSurface = NekoDarkInverseSurface,
    inversePrimary = NekoDarkInversePrimary,
)

private val NekoLightColorScheme = lightColorScheme(
    primary = NekoLightPrimary,
    onPrimary = NekoLightOnPrimary,
    primaryContainer = NekoLightPrimaryContainer,
    onPrimaryContainer = NekoLightOnPrimaryContainer,
    secondary = NekoLightSecondary,
    onSecondary = NekoLightOnSecondary,
    secondaryContainer = NekoLightSecondaryContainer,
    onSecondaryContainer = NekoLightOnSecondaryContainer,
    tertiary = NekoLightTertiary,
    onTertiary = NekoLightOnTertiary,
    tertiaryContainer = NekoLightTertiaryContainer,
    onTertiaryContainer = NekoLightOnTertiaryContainer,
    error = NekoLightError,
    errorContainer = NekoLightErrorContainer,
    onError = NekoLightOnError,
    onErrorContainer = NekoLightOnErrorContainer,
    background = NekoLightBackground,
    onBackground = NekoLightOnBackground,
    surface = NekoLightSurface,
    onSurface = NekoLightOnSurface,
    surfaceVariant = NekoLightSurfaceVariant,
    onSurfaceVariant = NekoLightOnSurfaceVariant,
    outline = NekoLightOutline,
    inverseOnSurface = NekoLightInverseOnSurface,
    inverseSurface = NekoLightInverseSurface,
    inversePrimary = NekoLightInversePrimary,
)

@Composable
fun NekoTTSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default to maintain Neko branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> NekoDarkColorScheme
        else -> NekoLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Cat-themed Material Design 3 theme extensions
 */
object NekoTheme {
    /**
     * Additional colors for cat-themed UI elements
     */
    object Colors {
        val pawPrint = NekoPurple
        val whiskers = NekoLightPink
        val furPrimary = NekoPink
        val furSecondary = NekoLightPurple
        val eyes = NekoAccent
        val nose = NekoWarning
        val success = NekoSuccess
        val warning = NekoWarning
        val info = NekoInfo
        val accent = NekoAccent
    }
    
    /**
     * Gradient definitions for cat-themed elements
     */
    object Gradients {
        val pawGradient = NekoPawGradient
        val whiskerGradient = NekoWhiskerGradient
        val backgroundGradient = listOf(NekoLightBackground, NekoLightSurface)
    }
    
    /**
     * Common spacing values for consistent UI
     */
    object Spacing {
        val tiny = 4.dp
        val small = 8.dp
        val medium = 16.dp
        val large = 24.dp
        val extraLarge = 32.dp
        val huge = 48.dp
    }
    
    /**
     * Common corner radius values
     */
    object CornerRadius {
        val small = 8.dp
        val medium = 12.dp
        val large = 16.dp
        val extraLarge = 24.dp
        val round = 50.dp
    }
    
    /**
     * Elevation values for consistent shadows
     */
    object Elevation {
        val none = 0.dp
        val small = 2.dp
        val medium = 4.dp
        val large = 8.dp
        val extraLarge = 16.dp
    }
}

