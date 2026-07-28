package com.budspro.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.budspro.app.data.GameItem
import com.budspro.app.ui.theme.colorForType

/**
 * A lightweight "shared element" hero transition.
 *
 * When a card is tapped we render its cover once more in a full-screen
 * overlay, starting exactly at the card's on-screen bounds, and animate it out
 * to fill the screen. When the animation finishes [onFinished] launches the
 * viewer/player activity, so the hand-off feels continuous.
 *
 * It is purely visual: the callback always fires when the animation ends (or
 * immediately if the bounds are unusable), so opening an item can never hang.
 */
@Composable
fun HeroOpenOverlay(
    item: GameItem,
    startBounds: Rect,
    onFinished: () -> Unit
) {
    val density = LocalDensity.current
    var container by remember { mutableStateOf(Rect.Zero) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(item.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { container = it.boundsInWindow() }
    ) {
        val ready = container.width > 0f && startBounds.width > 0f
        if (ready) {
            val t = progress.value
            val left = startBounds.left + (container.left - startBounds.left) * t
            val top = startBounds.top + (container.top - startBounds.top) * t
            val width = startBounds.width + (container.width - startBounds.width) * t
            val height = startBounds.height + (container.height - startBounds.height) * t

            val widthDp = with(density) { width.toDp() }
            val heightDp = with(density) { height.toDp() }
            val cornerDp = (16f * (1f - t)).dp

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = left - container.left
                        translationY = top - container.top
                        alpha = 1f - (t * 0.2f)
                    }
                    .size(width = widthDp, height = heightDp)
                    .clip(RoundedCornerShape(cornerDp))
            ) {
                CoverArt(
                    item = item,
                    accent = colorForType(item.type),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
