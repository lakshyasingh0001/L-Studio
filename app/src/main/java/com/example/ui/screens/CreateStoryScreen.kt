package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoryStroke
import com.example.ui.ChatViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink

val PALETTE_COLORS = listOf(
    Color.White,
    NeonCyan,
    NeonPink,
    EmeraldGreen,
    Color(0xFFFBBF24), // Yellow
    Color(0xFFEF4444)  // Red
)

val FILTER_OPTIONS = listOf("Normal", "Warm", "Cool", "Noir", "Neon")

@Composable
fun CreateStoryScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    var selectedGradientIndex by remember { mutableIntStateOf(1) }
    var selectedFilter by remember { mutableStateOf("Normal") }
    var caption by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var isDrawingMode by remember { mutableStateOf(true) }

    val strokes = remember { mutableStateListOf<StoryStroke>() }
    var currentPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    BackHandler {
        viewModel.navigateBack()
    }

    val gradient = STORY_GRADIENTS.getOrElse(selectedGradientIndex) { STORY_GRADIENTS[0] }

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradient))
        ) {
            // Interactive Drawing Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isDrawingMode, selectedColor) {
                        if (isDrawingMode) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val nx = offset.x / size.width
                                    val ny = offset.y / size.height
                                    currentPoints = listOf(nx to ny)
                                },
                                onDrag = { change, _ ->
                                    val nx = change.position.x / size.width
                                    val ny = change.position.y / size.height
                                    currentPoints = currentPoints + (nx to ny)
                                },
                                onDragEnd = {
                                    if (currentPoints.size > 1) {
                                        strokes.add(
                                            StoryStroke(
                                                points = currentPoints,
                                                colorHex = selectedColor.value.toLong(),
                                                strokeWidth = 8f
                                            )
                                        )
                                    }
                                    currentPoints = emptyList()
                                },
                                onDragCancel = {
                                    currentPoints = emptyList()
                                }
                            )
                        }
                    }
            ) {
                // Render finished strokes
                strokes.forEach { stroke ->
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

                // Render current active stroke
                if (currentPoints.size > 1) {
                    for (i in 0 until currentPoints.size - 1) {
                        val p1 = currentPoints[i]
                        val p2 = currentPoints[i + 1]
                        drawLine(
                            color = selectedColor,
                            start = Offset(p1.first * size.width, p1.second * size.height),
                            end = Offset(p2.first * size.width, p2.second * size.height),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Top Header: Close, Clear, Undo, Mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (strokes.isNotEmpty()) {
                            IconButton(
                                onClick = { strokes.removeLastOrNull() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White)
                            }

                            IconButton(
                                onClick = { strokes.clear() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = Color.White)
                            }
                        }

                        IconButton(
                            onClick = { isDrawingMode = !isDrawingMode },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isDrawingMode) NeonCyan else Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Draw",
                                tint = if (isDrawingMode) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Brush color palette
                if (isDrawingMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PALETTE_COLORS.forEach { color ->
                            val isSelected = selectedColor == color
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
            }

            // Bottom Tools: Filter selector, Backdrop selector, Caption, 24h Expiry indicator, Post button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Filter selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FILTER_OPTIONS.forEach { f ->
                        FilterChip(
                            selected = selectedFilter == f,
                            onClick = { selectedFilter = f },
                            label = { Text(f, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Backdrop Gradient selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Backdrop:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    STORY_GRADIENTS.forEachIndexed { index, grad ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(grad))
                                .border(
                                    width = if (selectedGradientIndex == index) 2.5.dp else 1.dp,
                                    color = if (selectedGradientIndex == index) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedGradientIndex = index }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Caption TextField
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Add caption or story status...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.45f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_caption_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 24h Expiry indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = "24h Timer",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ephemeral 24h Story • Disappears automatically after 24 hours",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.postStory(
                            caption = caption,
                            filterName = selectedFilter,
                            gradientIndex = selectedGradientIndex,
                            drawingStrokes = strokes.toList()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_story_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Share to 24h Story", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
