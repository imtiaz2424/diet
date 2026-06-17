package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PolishDarkPrimary,
    primaryContainer = PolishDarkPrimaryContainer,
    onPrimaryContainer = PolishDarkOnPrimaryContainer,
    secondary = PolishDarkSecondary,
    secondaryContainer = PolishDarkSecondaryContainer,
    background = PolishDarkBackground,
    surface = PolishDarkSurface,
    onSurface = PolishDarkOnSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PolishPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondary = PolishSecondary,
    secondaryContainer = PolishSecondaryContainer,
    background = PolishBackground,
    surface = PolishSurface,
    onSurface = PolishOnSurface,
    outline = PolishOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Forcing our precise custom Brand colors for visual polish
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
