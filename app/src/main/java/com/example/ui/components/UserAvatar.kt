package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.StoryGradientEnd
import com.example.ui.theme.StoryGradientMid
import com.example.ui.theme.StoryGradientStart

@Composable
fun UserAvatar(
    avatarUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    showStoryRing: Boolean = false,
    hasUnseenStory: Boolean = false
) {
    val storyBrush = Brush.sweepGradient(
        listOf(StoryGradientStart, StoryGradientMid, StoryGradientEnd, StoryGradientStart)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        val ringModifier = if (showStoryRing) {
            Modifier
                .size(size + 6.dp)
                .clip(CircleShape)
                .background(if (hasUnseenStory) storyBrush else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)))
                .padding(2.5.dp)
        } else {
            Modifier
        }

        Box(
            modifier = ringModifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "$name's avatar",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.45f).sp
                    )
                }
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.3f).coerceIn(12f, 18f).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(EmeraldGreen)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
