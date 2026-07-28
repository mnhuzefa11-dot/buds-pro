package com.budspro.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A friendly illustrated empty state: a soft glowing orb with floating
 * "cards" drawn behind the icon, then a title, a helpful message and an
 * optional call to action.
 *
 * Drawn entirely with Compose Canvas so no drawable assets are needed.
 */
@Composable
fun IllustratedEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val transition = rememberInfiniteTransition(label = "emptyGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val w = size.width
                    val h = size.height

                    // Soft radial glow behind everything.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.28f), primary.copy(alpha = 0f)),
                            center = Offset(w / 2f, h / 2f),
                            radius = (w / 2f) * pulse
                        ),
                        radius = (w / 2f) * pulse,
                        center = Offset(w / 2f, h / 2f)
                    )

                    // Three tilted "cards" fanned out behind the icon.
                    val cardW = w * 0.34f
                    val cardH = h * 0.44f
                    val cy = h * 0.52f

                    rotate(degrees = -16f, pivot = Offset(w * 0.30f, cy)) {
                        drawRoundRectCompat(
                            color = surfaceVariant,
                            topLeft = Offset(w * 0.30f - cardW / 2f, cy - cardH / 2f),
                            size = Size(cardW, cardH),
                            alpha = 0.9f
                        )
                    }
                    rotate(degrees = 16f, pivot = Offset(w * 0.70f, cy)) {
                        drawRoundRectCompat(
                            color = surfaceVariant,
                            topLeft = Offset(w * 0.70f - cardW / 2f, cy - cardH / 2f),
                            size = Size(cardW, cardH),
                            alpha = 0.9f
                        )
                    }
                    drawRoundRectCompat(
                        color = secondary,
                        topLeft = Offset(w * 0.5f - cardW / 2f, cy - cardH / 2f),
                        size = Size(cardW, cardH),
                        alpha = 0.22f
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Box(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun DrawScope.drawRoundRectCompat(
    color: androidx.compose.ui.graphics.Color,
    topLeft: Offset,
    size: Size,
    alpha: Float
) {
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
        alpha = alpha
    )
}
