package com.sulo.mizan.inventory.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MizanTealPrimaryDark,
    onPrimary = MizanTealOnPrimaryDark,
    primaryContainer = MizanTealContainerDark,
    onPrimaryContainer = MizanTealOnContainerDark,
    secondary = MizanIndigoSecondaryDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF102A25),
    secondaryContainer = MizanIndigoContainerDark,
    onSecondaryContainer = MizanIndigoOnContainerDark,
    tertiary = MizanAmberTertiaryDark,
    onTertiary = androidx.compose.ui.graphics.Color(0xFF2D1B05),
    tertiaryContainer = MizanAmberContainerDark,
    onTertiaryContainer = MizanAmberOnContainerDark,
    background = MizanBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
    surface = MizanSurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    surfaceVariant = MizanSurfaceVariantDark,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF94A3B8),
    outline = MizanOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = MizanTealPrimaryLight,
    onPrimary = MizanTealOnPrimaryLight,
    primaryContainer = MizanTealContainerLight,
    onPrimaryContainer = MizanTealOnContainerLight,
    secondary = MizanIndigoSecondaryLight,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = MizanIndigoContainerLight,
    onSecondaryContainer = MizanIndigoOnContainerLight,
    tertiary = MizanAmberTertiaryLight,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = MizanAmberContainerLight,
    onTertiaryContainer = MizanAmberOnContainerLight,
    background = MizanBackgroundLight,
    onBackground = androidx.compose.ui.graphics.Color(0xFF0F172A),
    surface = MizanSurfaceLight,
    onSurface = androidx.compose.ui.graphics.Color(0xFF0F172A),
    surfaceVariant = MizanSurfaceVariantLight,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF475569),
    outline = MizanOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MizanShapes,
            content = content
        )
    }
}

private val MizanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)
