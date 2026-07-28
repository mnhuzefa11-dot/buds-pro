package com.budspro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.budspro.app.data.AppTheme

/**
 * Buds Pro palette.
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

/** The original (default) dark scheme — unchanged. */
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

/** True black for OLED panels. */
private val BudsProAmoledColors = darkColorScheme(
    primary = BudsPurple,
    onPrimary = Color(0xFF14061F),
    primaryContainer = Color(0xFF2A1440),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = BudsCyan,
    onSecondary = Color(0xFF04212A),
    secondaryContainer = Color(0xFF07222A),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = BudsPink,
    onTertiary = Color(0xFF33101F),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF090909),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFF9C9CA6),
    outline = Color(0xFF2A2A2A),
    error = BudsRed,
    onError = Color(0xFF2B0505)
)

/** Deeply saturated purple variant. */
private val BudsProPurpleColors = darkColorScheme(
    primary = Color(0xFFC084FC),
    onPrimary = Color(0xFF1D0730),
    primaryContainer = Color(0xFF6D28D9),
    onPrimaryContainer = Color(0xFFF6EBFF),
    secondary = Color(0xFFF0ABFC),
    onSecondary = Color(0xFF2A0731),
    secondaryContainer = Color(0xFF5B2168),
    onSecondaryContainer = Color(0xFFFCE7FF),
    tertiary = BudsPink,
    onTertiary = Color(0xFF33101F),
    background = Color(0xFF14082A),
    onBackground = Color(0xFFF4ECFF),
    surface = Color(0xFF1E0F3C),
    onSurface = Color(0xFFF4ECFF),
    surfaceVariant = Color(0xFF2C1653),
    onSurfaceVariant = Color(0xFFC2B0E0),
    outline = Color(0xFF4B2C7A),
    error = BudsRed,
    onError = Color(0xFF2B0505)
)

/** Clean light scheme with the same purple accent. */
private val BudsProLightColors = lightColorScheme(
    primary = BudsPurpleDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE0FF),
    onPrimaryContainer = Color(0xFF2C0F55),
    secondary = Color(0xFF0E7490),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF06333F),
    tertiary = Color(0xFFBE185D),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF7FF),
    onBackground = Color(0xFF14101C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14101C),
    surfaceVariant = Color(0xFFF0EAF8),
    onSurfaceVariant = Color(0xFF5C5470),
    outline = Color(0xFFD8CEE8),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

/**
 * Root theme.
 *
 * The `theme` parameter is defaulted, so every existing call site —
 * `BudsProTheme { ... }` — keeps compiling and keeps rendering the original
 * dark palette exactly as before.
 */
@Composable
fun BudsProTheme(
    theme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colors = when (theme) {
        AppTheme.DARK -> BudsProDarkColors
        AppTheme.LIGHT -> BudsProLightColors
        AppTheme.AMOLED -> BudsProAmoledColors
        AppTheme.PURPLE -> BudsProPurpleColors
    }
    MaterialTheme(
        colorScheme = colors,
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

/** Short badge label shown in the corner of a card. */
fun badgeLabelForType(type: String): String = when (type.lowercase()) {
    "html", "htm" -> "HTML"
    "pdf" -> "PDF"
    "json" -> "JSON"
    "image", "img", "jpg", "jpeg", "png", "webp" -> "IMG"
    else -> type.uppercase()
}
