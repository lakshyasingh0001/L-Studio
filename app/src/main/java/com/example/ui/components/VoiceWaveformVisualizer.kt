package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VoiceWaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.5.dp,
    maxHeight: Dp = 32.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
    playbackProgress: Float = 0f
) {
    val items = if (amplitudes.isEmpty()) {
        listOf(0.2f, 0.4f, 0.7f, 0.3f, 0.9f, 0.6f, 0.4f, 0.8f, 0.3f, 0.6f, 0.4f, 0.2f)
    } else {
        amplitudes
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, amp ->
            val ratio = index.toFloat() / items.size.toFloat()
            val color = if (ratio <= playbackProgress) activeColor else inactiveColor
            val height = (maxHeight.value * amp.coerceIn(0.15f, 1f)).dp

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun PulsingCallWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 20,
    activeColor: Color = MaterialTheme.colorScheme.secondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseHeight"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = kotlin.math.sin((i.toDouble() / barCount) * Math.PI).toFloat()
            val height = (8 + (factor * 36 * pulse)).dp

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor)
            )
        }
    }
}
