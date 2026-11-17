package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CobaltPrimaryDark,
    onPrimary = Color(0xFF0A224D),
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = TealRouteBright,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = AmberWayLight,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberContainer,
    background = SlateDarkBackground,
    onBackground = ChalkInk,
    surface = SlateDarkSurface,
    onSurface = ChalkInk,
    surfaceVariant = SlateDarkVariant,
    onSurfaceVariant = ChalkInkSecondary,
    outline = OutlineBlueprintDark,
    error = CoralDestLight
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = Color(0xFF0B2554),
    secondary = TealRoute,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Color(0xFF0F3F3C),
    tertiary = AmberWay,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = Color(0xFF5A2B04),
    background = ChalkBackground,
    onBackground = InkPrimary,
    surface = PureSurface,
    onSurface = InkPrimary,
    surfaceVariant = BlueprintSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = OutlineBlueprint,
    error = CoralDest
)

val BlockWayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BlockWayTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BlockWayTypography,
        shapes = BlockWayShapes,
        content = content
    )
}
