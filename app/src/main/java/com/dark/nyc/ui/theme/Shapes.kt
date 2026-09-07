package com.dark.nyc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val NYCDatingShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Small tags
    small = RoundedCornerShape(8.dp),        // Icons background
    medium = RoundedCornerShape(14.dp),      // Standard Cards / Input fields
    large = RoundedCornerShape(20.dp),       // Bottom sheets / Large cards
    extraLarge = RoundedCornerShape(30.dp)   // FAB / Pill Buttons
)

// Custom standalone shapes for reuse:
val PillShape = RoundedCornerShape(50.dp)    // Full pill buttons
val CardShape = RoundedCornerShape(16.dp)    // Profile cards