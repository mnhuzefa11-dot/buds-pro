package com.budspro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Buds Pro dark palette.
 *
 * These are plain colour constants — nothing here touches app logic,
 * the database, importing or the player. It is purely how things look.
 */
val BudsPurple = Color(0xFFA855F7)
val BudsPurpleDark = Color(0xFF7C3AED)
val BudsCyan = Color(0xFF22D3EE)
val BudsPink = Color(0xFFF472B6)
val BudsRed = Color(0xFFEF4444)
val BudsAmber = Color(0xFFF59E0B)

val BudsBackground = Color(0xFF0B0710)
val BudsSurface = Color(0xFF16121F)
val BudsSurfaceVariant = Color(0xFF221B33)
val BudsOutline = Color(0xFF3A3050)
val BudsTextPrimary = Color(0xFFF3F0FA)
val BudsTextSecondary = Color(0xFF9E95B4)

private val BudsProDarkColors = darkColorScheme(
    primary = BudsPurple,
    onPrimary = Color(0xFF14061F),
    primaryContainer = BudsPurpleDark,
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = BudsCyan,
    onSecondary = Color(0xFF04212A),
    secondaryContainer = Color(0xFF0E3D49),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = BudsPink,
    onTertiary = Color(0xFF33101F),
    background = BudsBackground,
    onBackground = BudsTextPrimary,
    surface = BudsSurface,
    onSurface = BudsTextPrimary,
    surfaceVariant = BudsSurfaceVariant,
    onSurfaceVariant = BudsTextSecondary,
    outline = BudsOutline,
    error = BudsRed,
    onError = Color(0xFF2B0505)
)

@Composable
fun BudsProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BudsProDarkColors,
        content = content
    )
}

/** Accent colour used for the little file-type badge on each card. */
fun colorForType(type: String): Color = when (type.lowercase()) {
    "html" -> BudsPurple
    "pdf" -> BudsRed
    "json" -> BudsCyan
    else -> BudsAmber
}
