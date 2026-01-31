package com.lsj.notes.ui.theme

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
import android.graphics.drawable.ColorDrawable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val OledColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color.White,
    tertiary = Color.White,
    background = Black,
    surface = Black,
    surfaceVariant = Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = Black,
    secondaryContainer = Color.White,
    onSecondaryContainer = Black,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    outline = Color.White,
    outlineVariant = Color.White,
    surfaceTint = Black,
    surfaceContainerLowest = Black,
    surfaceContainerLow = Black,
    surfaceContainer = Black,
    surfaceContainerHigh = Black,
    surfaceContainerHighest = Black,
    surfaceDim = Black,
    inverseSurface = Color.White,
    inverseOnSurface = Black
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun NotesTheme(
    themePreference: String = "Default",
    // darkTheme fallback if "Default" is selected
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isOled = themePreference == "OLED"
    val actualDarkTheme = when (themePreference) {
        "Light" -> false
        "OLED" -> true
        "Default" -> darkTheme
        else -> darkTheme
    }

    val colorScheme = when {
        isOled -> OledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (actualDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        actualDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !actualDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !actualDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}