package com.pro.logcatreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pro.logcatreader.R
import com.pro.logcatreader.model.LogLevel

// Palet teal + bentuk 4/8/12 dp meniru LogcatReader; mode gelap memakai netral Darcula khas LogLynx.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF005B52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF008377),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF3E655F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC3EDE5),
    onSecondaryContainer = Color(0xFF28504A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FAF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF5FAF8),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFD8E5E2),
    onSurfaceVariant = Color(0xFF3D4947),
    outline = Color(0xFF6D7A77),
    outlineVariant = Color(0xFFBCC9C6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F2),
    surfaceContainer = Color(0xFFEAEFED),
    surfaceContainerHigh = Color(0xFFE4E9E7),
    surfaceContainerHighest = Color(0xFFDEE4E1)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF67D9C9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF008377),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFA5CFC7),
    onSecondary = Color(0xFF0C3731),
    secondaryContainer = Color(0xFF1E4640),
    onSecondaryContainer = Color(0xFFB3DDD5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3D4947),
    onSurfaceVariant = Color(0xFFBCC9C6),
    outline = Color(0xFF869390),
    outlineVariant = Color(0xFF3D4947),
    surfaceContainerLowest = Color(0xFF181818),
    surfaceContainerLow = Color(0xFF242424),
    surfaceContainer = Color(0xFF2B2B2B),
    surfaceContainerHigh = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF3C3C3C)
)

/** Font log: Roboto Mono (sama dengan LogcatReader), 3 bobot yang dipakai UI: Regular/Medium/Bold. */
val RobotoMonoFontFamily = FontFamily(
    Font(R.font.roboto_mono_regular),
    Font(R.font.roboto_mono_medium, FontWeight.Medium),
    Font(R.font.roboto_mono_bold, FontWeight.Bold)
)

private val LogLynxShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

@Composable
fun LogLynxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        shapes = LogLynxShapes,
        content = content
    )
}

/** Warna badge prioritas (sama dengan LogcatReader). */
fun LogLevel.badgeColor(): Color = when (this) {
    LogLevel.VERBOSE -> Color(0xFF546E7A)
    LogLevel.DEBUG -> Color(0xFF1976D2)
    LogLevel.INFO -> Color(0xFF388E3C)
    LogLevel.WARN -> Color(0xFFE65100)
    LogLevel.ERROR -> Color(0xFFD32F2F)
    LogLevel.FATAL -> Color(0xFFC2185B)
}

/** Warna teks sekunder baris log (tanggal/jam/PID/TID). */
@Composable
fun logSecondaryColor(): Color =
    if (isSystemInDarkTheme()) Color(0xB2FFFFFF) else Color(0x8A000000)

/** Warna sorot kecocokan pencarian. */
@Composable
fun searchHitColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF796A39) else Color(0xFFE5D754)
