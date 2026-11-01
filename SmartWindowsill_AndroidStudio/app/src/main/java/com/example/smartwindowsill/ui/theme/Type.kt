package com.example.smartwindowsill.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// One family, weights 400 and 500, sizes 12 / 14 / 16 / 20 / 24.
private val Family = FontFamily.Default

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    headlineMedium = style(24, 32, FontWeight.Medium),
    titleLarge = style(20, 28, FontWeight.Medium),
    titleMedium = style(16, 24, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(16, 24),
    bodyMedium = style(14, 20),
    bodySmall = style(12, 16),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(12, 16, FontWeight.Medium)
)

/** Sensor figures and percentages: same width for every digit so columns align. */
val NumberStyle = style(16, 24, FontWeight.Medium).copy(fontFeatureSettings = "tnum")
val NumberSmallStyle = style(14, 20, FontWeight.Medium).copy(fontFeatureSettings = "tnum")
