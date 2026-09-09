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
    primary = TrustBlue80,
    secondary = SecurityTeal80,
    tertiary = SafeGreen80,
    background = DarkBackground,
    surface = DarkSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TrustBlue40,
    secondary = SecurityTeal40,
    tertiary = SafeGreen40,
    background = LightBackground,
    surface = LightSurface
  )

private val OledColorScheme =
  darkColorScheme(
    primary = OledPrimary,
    secondary = OledSecondary,
    tertiary = SafeGreen80,
    background = OledBackground,
    surface = OledSurface,
    surfaceVariant = OledCard
  )

private val NavyColorScheme =
  darkColorScheme(
    primary = NavyPrimary,
    secondary = NavySecondary,
    tertiary = SecurityTeal80,
    background = NavyBackground,
    surface = NavySurface,
    surfaceVariant = NavyCard
  )

private val AuroraColorScheme =
  darkColorScheme(
    primary = AuroraPrimary,
    secondary = AuroraSecondary,
    tertiary = SafeGreen80,
    background = AuroraBackground,
    surface = AuroraSurface,
    surfaceVariant = AuroraCard
  )

private val SilverColorScheme =
  lightColorScheme(
    primary = SilverPrimary,
    secondary = SilverSecondary,
    tertiary = SafeGreen40,
    background = SilverBackground,
    surface = SilverSurface,
    surfaceVariant = SilverCard
  )

@Composable
fun MyApplicationTheme(
  appTheme: String = "Cyber Dark",
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = when (appTheme) {
    "OLED Pure Black" -> OledColorScheme
    "Deep Midnight Navy" -> NavyColorScheme
    "Neon Aurora" -> AuroraColorScheme
    "Clean Light Silver" -> SilverColorScheme
    "Cyber Dark" -> DarkColorScheme
    else -> if (darkTheme) DarkColorScheme else LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
