package com.budspro.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A small, self-contained pull-to-refresh container.
 *
 * Written against stable `foundation` + `ui` APIs (a NestedScrollConnection and
 * an Animatable) rather than the experimental Material3 pull-to-refresh
 * package, whose signatures changed between 1.2 and 1.3. This keeps the build
 * green regardless of which Compose BOM the project resolves to.
 *
 * Behaviour: dragging down at the top of the scrollable content reveals a
 * spinner; releasing past the threshold calls [onRefresh].
 */
@Composable
fun BudsPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 76.dp.toPx() }
    val maxPx = with(density) { 130.dp.toPx() }
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }

    // Snap the indicator to the resting/refreshing position when the caller's
    // refreshing flag changes.
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) offset.animateTo(thresholdPx) else offset.animateTo(0f)
    }

    val connection = remember(isRefreshing, thresholdPx, maxPx) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Consume upward drags first so the indicator retracts before
                // the list starts scrolling again.
                if (isRefreshing) return Offset.Zero
                if (available.y < 0f && offset.value > 0f) {
                    val consumed = (-available.y).coerceAtMost(offset.value)
                    scope.launch { offset.snapTo(offset.value - consumed) }
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isRefreshing) return Offset.Zero
                if (available.y > 0f) {
                    // Rubber-band resistance.
                    val next = (offset.value + available.y * 0.5f).coerceAtMost(maxPx)
                    scope.launch { offset.snapTo(next) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isRefreshing) return Velocity.Zero
                if (offset.value > 0f) {
                    val triggered = offset.value >= thresholdPx
                    if (triggered) {
                        offset.animateTo(thresholdPx)
                        onRefresh()
                    } else {
                        offset.animateTo(0f)
                    }
                    return if (triggered) available else Velocity.Zero
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(connection)
    ) {
        content()

        if (offset.value > 0.5f) {
            val fraction = (offset.value / thresholdPx).coerceIn(0f, 1f)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = offset.value - thresholdPx * 0.35f
                        alpha = fraction
                        scaleX = 0.6f + 0.4f * fraction
                        scaleY = 0.6f + 0.4f * fraction
                    }
                    .size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Always the indeterminate spinner: the pull progress is
                    // already communicated through the scale/alpha above, and
                    // the determinate overload's signature has churned across
                    // Material3 releases.
                    CircularProgressIndicator(
                        modifier = Modifier.padding(9.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
