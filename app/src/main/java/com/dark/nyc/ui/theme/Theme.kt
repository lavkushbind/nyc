package com.dark.nyc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun NYCDatingTheme(
    // ⚠️ FORCE LIGHT MODE: darkTheme = false
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // System default ko ignore karke Light Mode force karo
    val colorScheme = lightColorScheme(
        // ===== BRAND REDS =====
        primary = NYC_Red,                 // Main Buttons, Tabs, Highlights
        onPrimary = TextInverse,           // White text on red buttons
        primaryContainer = NYC_RedSurface, // Light red background for chips
        onPrimaryContainer = NYC_RedDark,  // Dark red text on light red bg

        // ===== BACKGROUNDS =====
        background = PureWhite,            // Full screen BG
        onBackground = TextPrimary,        // Text on background

        // ===== SURFACES (Cards, Dialogs) =====
        surface = OffWhite,                // Card backgrounds (soft white)
        onSurface = TextPrimary,           // Text on cards
        surfaceVariant = SoftGray,         // Input fields BG
        onSurfaceVariant = TextHint,       // Hint text

        // ===== SECONDARY / ACCENT =====
        secondary = NYC_RedDark,           // For pressed states / secondary actions
        onSecondary = TextInverse,
        secondaryContainer = NYC_RedLight,
        onSecondaryContainer = NYC_Red,

        // ===== ERROR / SUCCESS =====
        error = StatusError,
        onError = TextInverse,

        // ===== BORDERS =====
        outline = BorderLight,             // Strokes for inputs
        outlineVariant = BorderRed,        // Red stroke for important fields

        // ===== SCROLLBAR / ETC =====
        surfaceTint = NYC_Red.copy(alpha = 0.1f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NYCDatingTypography,
        shapes = NYCDatingShapes,
        content = content
    )
}