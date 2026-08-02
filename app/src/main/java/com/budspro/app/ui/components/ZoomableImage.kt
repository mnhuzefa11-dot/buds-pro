package com.budspro.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Upper bound on the decoded bitmap so a huge photo stays sharp when zoomed
 * without risking an out-of-memory crash (2560px ≈ 26 MB at ARGB_8888).
 */
private const val MAX_DECODE_PX = 2560

/**
 * Shared zoom/pan state for an image displayed with [ContentScale.Fit].
 *
 * Everything is kept in "container" coordinates: [scale] is relative to the
 * already fitted image, and [offset] is a translation in pixels applied after
 * scaling. Panning is always clamped so the image can never be dragged off
 * screen, which is what makes the gesture feel right.
 */
class ZoomState(
    val minScale: Float = 1f,
    val maxScale: Float = 6f,
    val doubleTapScale: Float = 3f
) {
    var scale by mutableFloatStateOf(1f)
        internal set
    var offsetX by mutableFloatStateOf(0f)
        internal set
    var offsetY by mutableFloatStateOf(0f)
        internal set

    /** Size of the composable that hosts the image, in pixels. */
    var containerSize: IntSize = IntSize.Zero
        internal set

    /** Intrinsic size of the bitmap, in pixels (0 until the image loads). */
    var imageSize: Size = Size.Zero
        internal set

    val isZoomed: Boolean get() = scale > minScale + 0.01f

    /** The on-screen size of the image at scale 1 (i.e. after "fit"). */
    private fun fittedSize(): Size {
        val cw = containerSize.width.toFloat()
        val ch = containerSize.height.toFloat()
        if (cw <= 0f || ch <= 0f) return Size.Zero
        val iw = imageSize.width
        val ih = imageSize.height
        if (iw <= 0f || ih <= 0f) return Size(cw, ch)
        val ratio = min(cw / iw, ch / ih)
        return Size(iw * ratio, ih * ratio)
    }

    /** Largest pan distance allowed on each axis for the current scale. */
    private fun maxOffset(): Offset {
        val fitted = fittedSize()
        if (fitted.width <= 0f) return Offset.Zero
        val x = max(0f, (fitted.width * scale - containerSize.width) / 2f)
        val y = max(0f, (fitted.height * scale - containerSize.height) / 2f)
        return Offset(x, y)
    }

    internal fun clamp() {
        val limit = maxOffset()
        offsetX = offsetX.coerceIn(-limit.x, limit.x)
        offsetY = offsetY.coerceIn(-limit.y, limit.y)
    }

    /**
     * Applies a pinch/drag gesture.
     *
     * @return true when the gesture was consumed. It returns false for a
     * horizontal drag at minimum zoom so a parent pager (if any) can take over.
     */
    fun onGesture(centroid: Offset, pan: Offset, zoom: Float): Boolean {
        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
        // Keep the point under the fingers pinned while zooming.
        val cx = containerSize.width / 2f
        val cy = containerSize.height / 2f
        val focusX = centroid.x - cx
        val focusY = centroid.y - cy
        val k = newScale / scale
        offsetX = (offsetX + focusX) * k - focusX + pan.x
        offsetY = (offsetY + focusY) * k - focusY + pan.y
        scale = newScale
        clamp()
        return isZoomed || abs(zoom - 1f) > 0.001f
    }

    /**
     * Converts a normalised point inside the image (0..1 on each axis) into a
     * pixel position inside the container, honouring the current zoom and pan.
     */
    fun imageRatioToContainer(xRatio: Float, yRatio: Float): Offset {
        val fitted = fittedSize()
        if (fitted.width <= 0f) return Offset.Zero
        val cx = containerSize.width / 2f
        val cy = containerSize.height / 2f
        val x = cx + (xRatio - 0.5f) * fitted.width * scale + offsetX
        val y = cy + (yRatio - 0.5f) * fitted.height * scale + offsetY
        return Offset(x, y)
    }

    /**
     * Inverse of [imageRatioToContainer]. Returns null when the point falls
     * outside the visible image, so callers can ignore stray taps.
     */
    fun containerToImageRatio(point: Offset): Offset? {
        val fitted = fittedSize()
        if (fitted.width <= 0f || scale <= 0f) return null
        val cx = containerSize.width / 2f
        val cy = containerSize.height / 2f
        val x = (point.x - cx - offsetX) / (fitted.width * scale) + 0.5f
        val y = (point.y - cy - offsetY) / (fitted.height * scale) + 0.5f
        if (x < 0f || x > 1f || y < 0f || y > 1f) return null
        return Offset(x, y)
    }

    fun reset() {
        scale = minScale
        offsetX = 0f
        offsetY = 0f
    }

    /** Target values for a double tap at [tap]; zooms in, or back out. */
    fun doubleTapTarget(tap: Offset): Triple<Float, Float, Float> {
        if (isZoomed) return Triple(minScale, 0f, 0f)
        val target = doubleTapScale.coerceIn(minScale, maxScale)
        val cx = containerSize.width / 2f
        val cy = containerSize.height / 2f
        val focusX = tap.x - cx
        val focusY = tap.y - cy
        val k = target / scale
        var nx = (offsetX + focusX) * k - focusX
        var ny = (offsetY + focusY) * k - focusY

        val fitted = fittedSize()
        val limitX = max(0f, (fitted.width * target - containerSize.width) / 2f)
        val limitY = max(0f, (fitted.height * target - containerSize.height) / 2f)
        nx = nx.coerceIn(-limitX, limitX)
        ny = ny.coerceIn(-limitY, limitY)
        return Triple(target, nx, ny)
    }
}

@Composable
fun rememberZoomState(
    minScale: Float = 1f,
    maxScale: Float = 6f,
    doubleTapScale: Float = 3f
): ZoomState = remember(minScale, maxScale, doubleTapScale) {
    ZoomState(minScale, maxScale, doubleTapScale)
}

/**
 * An image that can be pinch-zoomed, double-tap zoomed and panned.
 *
 * @param overlay drawn on top of the image *inside* the transformed layer is
 * intentionally not supported — overlays are placed in the untransformed box
 * so callers can position annotations themselves using [ZoomState].
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    state: ZoomState = rememberZoomState(),
    onTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .onSizeChanged { state.containerSize = it; state.clamp() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap?.invoke(it) },
                    onLongPress = { onLongPress?.invoke(it) },
                    onDoubleTap = { tap ->
                        val (targetScale, tx, ty) = state.doubleTapTarget(tap)
                        val fromScale = state.scale
                        val fromX = state.offsetX
                        val fromY = state.offsetY
                        scope.launch {
                            val anim = Animatable(0f)
                            anim.animateTo(1f, tween(220)) {
                                val t = value
                                state.scale = fromScale + (targetScale - fromScale) * t
                                state.offsetX = fromX + (tx - fromX) * t
                                state.offsetY = fromY + (ty - fromY) * t
                            }
                            state.clamp()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = true) { centroid, pan, zoom, _ ->
                    state.onGesture(centroid, pan, zoom)
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .size(CoilSize(MAX_DECODE_PX, MAX_DECODE_PX))
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            onSuccess = { result ->
                val d = result.result.drawable
                if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0) {
                    state.imageSize = Size(d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
                    state.clamp()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offsetX
                    translationY = state.offsetY
                }
        )
        overlay()
    }
}
