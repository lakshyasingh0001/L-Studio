package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink

val STORY_GRADIENTS = listOf(
    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81)), // Deep Night
    listOf(Color(0xFF831843), Color(0xFFBE185D), Color(0xFFF43F5E)), // Neon Pink
    listOf(Color(0xFFB45309), Color(0xFFD97706), Color(0xFFF59E0B)), // Warm Sunset
    listOf(Color(0xFF065F46), Color(0xFF059669), Color(0xFF10B981)), // Emerald Aurora
    listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))  // Noir
)

@Composable
fun StoryViewerScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.stories.collectAsState()
    val activeIndex by viewModel.activeStoryIndex.collectAsState()
    val story = stories.getOrNull(activeIndex)

    BackHandler {
        viewModel.navigateBack()
    }

    if (story == null) {
        LaunchedEffect(Unit) {
            viewModel.navigateBack()
        }
        return
    }

    val progress = remember(story.id) { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    // Auto-advance progress timer (5 seconds per story)
    LaunchedEffect(story.id, isPaused) {
        if (!isPaused) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ((1f - progress.value) * 5000).toInt(),
                    easing = LinearEasing
                )
            )
            viewModel.nextStory()
        }
    }

    val gradientColors = STORY_GRADIENTS.getOrElse(story.backgroundGradientIndex) { STORY_GRADIENTS[0] }

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
        ) {
            // Render any canvas drawing strokes
            if (story.drawingStrokes.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    story.drawingStrokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            for (i in 0 until stroke.points.size - 1) {
                                val p1 = stroke.points[i]
                                val p2 = stroke.points[i + 1]
                                drawLine(
                                    color = Color(stroke.colorHex),
                                    start = Offset(p1.first * size.width, p1.second * size.height),
                                    end = Offset(p2.first * size.width, p2.second * size.height),
                                    strokeWidth = stroke.strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }

            // Left / Right tap zones for navigation
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.previousStory()
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(2f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.nextStory()
                        }
                )
            }

            // Top Header: Progress Segments, Author, 24h Countdown Badge, and Close Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Segmented Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val segmentProgress = when {
                            idx < activeIndex -> 1f
                            idx == activeIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Author Info & Countdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            avatarUrl = story.authorAvatar,
                            name = story.authorName,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = story.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = "Vanishing Timer",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = story.remainingTimeText,
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (story.filterName != "Normal") {
                                    Text(
                                        text = " • ${story.filterName}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Views",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${story.viewsCount}",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.navigateBack() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Caption Text Centered or Bottom
            if (story.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = story.caption,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Bottom Reactions & Reply
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Quick emoji reaction buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("❤️", "🔥", "😂", "😮", "👏", "🎉").forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable {
                                    // Send direct message with story reaction
                                    viewModel.startChatWithUser(story.userId)
                                    viewModel.sendMessage("Reacted $emoji to your story: \"${story.caption}\"")
                                    viewModel.navigateBack()
                                }
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Reply to story...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (replyText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                viewModel.startChatWithUser(story.userId)
                                viewModel.sendMessage("Reply to story: $replyText")
                                replyText = ""
                                viewModel.navigateBack()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Send", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
