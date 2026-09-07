package com.dark.nyc.ui.theme

import androidx.compose.ui.graphics.Color

// ======================== PRIMARY (RED FAMILY) ========================
// NYC Bold Red - Har jagah Primary action ke liye
val NYC_Red = Color(0xFFE63946)          // Main Brand Red (Buttons, Icons, Highlights)
val NYC_RedDark = Color(0xFFC1121F)      // Pressed state / Darker accents
val NYC_RedLight = Color(0xFFFF6B7A)     // Soft red (Badges, Unread indicators)
val NYC_RedSurface = Color(0xFFFFF0F1)   // Red-tinted background (For "Like" sections)

// ======================== NEUTRAL (WHITE & GREY) ========================
// Pure Whites
val PureWhite = Color(0xFFFFFFFF)        // Main App Background
val OffWhite = Color(0xFFF8F9FA)         // Card Backgrounds / Surface
val SoftGray = Color(0xFFF1F3F5)         // Divider / Input field backgrounds

// Text & Icons (Solid Grays)
val TextPrimary = Color(0xFF1A1A1A)      // Headlines, Names (Almost Black)
val TextSecondary = Color(0xFF495057)    // Subheadings, Bios
val TextHint = Color(0xFFADB5BD)         // Placeholders, Timestamps
val TextInverse = Color(0xFFFFFFFF)      // White text on Red background

// Strokes & Borders
val BorderLight = Color(0xFFE9ECEF)      // Light borders for input fields
val BorderRed = Color(0xFFE63946)        // Red borders for emphasis

// ======================== STATUS (SOLID) ========================
val StatusSuccess = Color(0xFF2EC4B6)    // Verified / Online (Teal Green)
val StatusWarning = Color(0xFFFF9F1C)    // Pending (Orange)
val StatusError = Color(0xFFD62828)      // Errors / Decline

// ======================== SHADOW / ELEVATION ========================
// Compose me shadow ke liye alpha values use karte hain,
// lekin solid color ke taur par ye rakh sakte hain:
val ShadowLight = Color(0x1A000000)      // 10% Black (Drop shadow for cards)