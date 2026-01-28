package com.example.englishword.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,

    // Secondary colors
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,

    // Tertiary colors
    tertiary = Purple40,
    onTertiary = Color.White,
    tertiaryContainer = Purple90,
    onTertiaryContainer = Purple10,

    // Error colors
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,

    // Background colors
    background = Grey99,
    onBackground = Grey10,

    // Surface colors
    surface = Grey99,
    onSurface = Grey10,
    surfaceVariant = BlueGrey90,
    onSurfaceVariant = BlueGrey30,

    // Outline colors
    outline = BlueGrey40,
    outlineVariant = BlueGrey80,

    // Inverse colors
    inverseSurface = Grey20,
    inverseOnSurface = Grey95,
    inversePrimary = Blue80,

    // Scrim
    scrim = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,

    // Secondary colors
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,

    // Tertiary colors
    tertiary = Purple80,
    onTertiary = Purple20,
    tertiaryContainer = Purple30,
    onTertiaryContainer = Purple90,

    // Error colors
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,

    // Background colors
    background = Grey10,
    onBackground = Grey90,

    // Surface colors
    surface = Grey10,
    onSurface = Grey90,
    surfaceVariant = BlueGrey30,
    onSurfaceVariant = BlueGrey80,

    // Outline colors
    outline = BlueGrey40,
    outlineVariant = BlueGrey30,

    // Inverse colors
    inverseSurface = Grey90,
    inverseOnSurface = Grey20,
    inversePrimary = Blue40,

    // Scrim
    scrim = Color.Black
)

@Composable
fun EnglishWordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
