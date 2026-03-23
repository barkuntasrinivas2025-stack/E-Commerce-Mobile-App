package com.confidencecommerce.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Brand Palette ─────────────────────────────────────────────────────────────
val Brand90      = Color(0xFFE8F5E9)
val Brand80      = Color(0xFFC8E6C9)
val Brand40      = Color(0xFF388E3C)
val Brand30      = Color(0xFF2E7D32)
val Brand20      = Color(0xFF1B5E20)
val BrandAccent  = Color(0xFF00C853)

// Confidence tier colours
val ConfidenceHigh   = Color(0xFF2E7D32)   // Green
val ConfidenceMedium = Color(0xFFE65100)   // Deep Orange
val ConfidenceLow    = Color(0xFFC62828)   // Red

val ConfidenceHighBg   = Color(0xFFE8F5E9)
val ConfidenceMediumBg = Color(0xFFFFF3E0)
val ConfidenceLowBg    = Color(0xFFFFEBEE)

val DiscountBadge  = Color(0xFFD32F2F)
val StarYellow     = Color(0xFFFFA000)

private val LightColorScheme = lightColorScheme(
    primary          = Brand40,
    onPrimary        = Color.White,
    primaryContainer = Brand90,
    secondary        = Color(0xFF795548),
    surface          = Color(0xFFFFFBFE),
    background       = Color(0xFFF5F5F5),
    error            = Color(0xFFB00020),
    outline          = Color(0xFFCACACA)
)

private val DarkColorScheme = darkColorScheme(
    primary          = Brand80,
    onPrimary        = Brand20,
    primaryContainer = Brand30,
    surface          = Color(0xFF1C1C1E),
    background       = Color(0xFF121212)
)

@Composable
fun ConfidenceCommerceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
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
        typography  = AppTypography,
        content     = content
    )
}
