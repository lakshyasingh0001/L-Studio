package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallStatus
import com.example.ui.ChatViewModel
import com.example.ui.components.PulsingCallWaveform
import com.example.ui.components.UserAvatar
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan

@Composable
fun VoiceCallScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val callState by viewModel.callState.collectAsState()

    BackHandler {
        viewModel.endVoiceCall()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ripple"
    )

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: E2EE badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "End-to-End Encrypted Voice Call",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callState.contactName.ifBlank { "Contact" },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = callState.contactHandle,
                    color = NeonCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                val statusText = when (callState.status) {
                    CallStatus.DIALING -> "Calling..."
                    CallStatus.RINGING -> "Ringing..."
                    CallStatus.CONNECTED -> "Connected • ${callState.durationFormatted}"
                    CallStatus.ENDED -> "Call Ended"
                    CallStatus.IDLE -> ""
                }

                Text(
                    text = statusText,
                    color = if (callState.status == CallStatus.CONNECTED) EmeraldGreen else Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Center: Avatar with pulsing rings and audio visualizer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    if (callState.status == CallStatus.CONNECTED || callState.status == CallStatus.RINGING) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .scale(rippleScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            NeonCyan.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape)
                        )
                    }

                    UserAvatar(
                        avatarUrl = callState.contactAvatar,
                        name = callState.contactName,
                        size = 110.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (callState.status == CallStatus.CONNECTED) {
                    PulsingCallWaveform(
                        barCount = 18,
                        activeColor = NeonCyan
                    )
                }
            }

            // Bottom Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute button
                    IconButton(
                        onClick = { viewModel.toggleCallMute() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (callState.isMuted) Color.White else Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (callState.isMuted) Color.Black else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Speaker toggle
                    IconButton(
                        onClick = { viewModel.toggleCallSpeaker() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (callState.isSpeakerOn) NeonCyan else Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = if (callState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = "Speaker",
                            tint = if (callState.isSpeakerOn) Color.Black else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // End call button
                FloatingActionButton(
                    onClick = { viewModel.endVoiceCall() },
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
