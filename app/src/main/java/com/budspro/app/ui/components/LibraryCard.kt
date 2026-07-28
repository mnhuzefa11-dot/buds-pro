package com.budspro.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.budspro.app.data.GameItem
import com.budspro.app.data.effectiveCover
import com.budspro.app.ui.theme.badgeLabelForType
import com.budspro.app.ui.theme.colorForType
import java.io.File
import java.util.Locale

/**
 * The redesigned library card.
 *
 *  - Cover fills the FULL card width and 70% of the card height, cropped.
 *  - Title sits on a dark gradient overlay at the bottom of the cover.
 *  - File-type badge (HTML / PDF / IMG / JSON) top-right.
 *  - Favourite heart top-left, only when the item is favourited.
 *  - Progress bar pinned to the very bottom of the card.
 *  - 16dp corners, subtle shadow, press-scale animation.
 *  - Long press fires haptic feedback and opens the context sheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryGameCard(
    item: GameItem,
    /** Receives the card's on-screen bounds so the caller can run a hero transition. */
    onOpen: (Rect) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true,
    cardHeight: androidx.compose.ui.unit.Dp = 230.dp
) {
    val accent = colorForType(item.type)
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.65f),
        label = "cardScale"
    )
    var bounds by remember { mutableStateOf(Rect.Zero) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .scale(scale)
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onOpen(bounds) },
                onLongClick = {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ---- Cover area: full width, 70% of the height ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.70f)
            ) {
                CoverArt(item = item, accent = accent, modifier = Modifier.fillMaxSize())

                // Bottom gradient scrim so the title is always legible.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.62f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.82f)
                                )
                            )
                        )
                )

                // Title overlay.
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
                )

                // Type badge, top-right.
                TypeBadgePill(
                    label = badgeLabelForType(item.type),
                    accent = accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )

                // Favourite heart, top-left, only when favourited.
                if (item.isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // ---- Meta strip (remaining 30%) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.30f)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatBytes(item.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (item.progress > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${item.progress}% complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            maxLines = 1
                        )
                    }
                }
            }

            // ---- Progress bar at the very bottom of the card ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (item.progress > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress.coerceIn(0, 100) / 100f)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accent.copy(alpha = 0.75f), accent)
                                )
                            )
                    )
                }
            }
        }
    }
}

/** Cover image, or a generated placeholder when the item has no cover yet. */
@Composable
fun CoverArt(item: GameItem, accent: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cover = item.effectiveCover
    val fallbackSource: Any? = when {
        cover != null && File(cover).exists() -> File(cover)
        item.type.equals("image", ignoreCase = true) ->
            File(File(context.filesDir, "games"), item.fileName)
        else -> null
    }

    if (fallbackSource != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(fallbackSource)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.42f), accent.copy(alpha = 0.12f))
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForType(item.type),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
fun TypeBadgePill(label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

/**
 * The list-mode row, matching the card visual language.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryListRow(
    item: GameItem,
    onOpen: (Rect) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    val accent = colorForType(item.type)
    val haptics = LocalHapticFeedback.current
    var bounds by remember { mutableStateOf(Rect.Zero) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onOpen(bounds) },
                onLongClick = {
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    CoverArt(item = item, accent = accent, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = badgeLabelForType(item.type),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            text = "  ·  ${formatBytes(item.fileSize)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (item.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (item.progress > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress.coerceIn(0, 100) / 100f)
                            .fillMaxHeight()
                            .background(accent)
                    )
                }
            }
        }
    }
}

fun iconForType(type: String): ImageVector = when (type.lowercase()) {
    "html", "htm" -> Icons.Filled.SportsEsports
    "pdf" -> Icons.Filled.PictureAsPdf
    "json" -> Icons.Filled.DataObject
    "image" -> Icons.Filled.Image
    else -> Icons.Filled.Description
}

fun formatBytes(bytes: Long): String = when {
    bytes <= 0L -> "0 KB"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    bytes < 1024L * 1024L * 1024L ->
        String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
