package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.ui.AppScreen
import com.example.ui.ChatViewModel
import com.example.ui.components.EncryptionDialog
import com.example.ui.components.ReactionPickerBar
import com.example.ui.components.UserAvatar
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.IncomingBubbleDark
import com.example.ui.theme.IncomingBubbleLight
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OutgoingBubbleDark
import com.example.ui.theme.OutgoingBubbleLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val activeChatId by viewModel.activeChatId.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedMessageForReaction by viewModel.selectedMessageForReaction.collectAsState()
    val showEncryptionInfo by viewModel.showEncryptionInfo.collectAsState()

    val chat = chats.find { it.id == activeChatId } ?: return
    val messages = viewModel.getMessagesForChat(chat.id)

    var inputText by remember { mutableStateOf("") }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableStateOf(0) }
    var inspectedMessageForCrypto by remember { mutableStateOf<Message?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        viewModel.navigateBack()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Audio recording simulation timer
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingDurationSec = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingDurationSec += 1
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // Header Top Bar
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        UserAvatar(
                            avatarUrl = chat.avatarUrl,
                            name = chat.name,
                            size = 38.dp,
                            isOnline = !chat.isGroup
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = chat.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "E2EE",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = if (chat.isGroup) "${chat.participantIds.size} members • Encrypted" else "Online • E2EE Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Voice Call Button (Only for 1-on-1 or group call initiator)
                    IconButton(
                        onClick = {
                            val myId = currentUser?.userId ?: currentUser?.id ?: ""
                            val contactId = chat.participantIds.firstOrNull { it != myId }
                            if (contactId != null) {
                                viewModel.startVoiceCall(contactId)
                            }
                        },
                        modifier = Modifier.testTag("voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Encryption Shield
                    IconButton(
                        onClick = { viewModel.toggleEncryptionInfo(true) },
                        modifier = Modifier.testTag("encryption_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Encryption Verification",
                            tint = EmeraldGreen
                        )
                    }

                    if (chat.isGroup) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.GROUP_DETAILS) },
                            modifier = Modifier.testTag("group_details_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Group Details"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Reaction bar overlay if a message was long-pressed
            AnimatedVisibility(visible = selectedMessageForReaction != null) {
                ReactionPickerBar(
                    isVisible = selectedMessageForReaction != null,
                    onEmojiSelected = { emoji ->
                        viewModel.toggleReaction(emoji)
                    },
                    onDismiss = {
                        viewModel.selectMessageForReaction(null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Message stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top E2EE Security Banner
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Messages & calls in this chat are end-to-end encrypted with AES-256 GCM. Tap shield icon to verify safety numbers.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == (currentUser?.userId ?: currentUser?.id ?: "")
                    MessageBubbleItem(
                        message = msg,
                        isMe = isMe,
                        isGroup = chat.isGroup,
                        onLongClick = {
                            viewModel.selectMessageForReaction(msg)
                        },
                        onReactionClick = { emoji ->
                            viewModel.toggleReactionDirect(chat.id, msg.id, emoji)
                        },
                        onInspectCipher = {
                            inspectedMessageForCrypto = msg
                            viewModel.toggleEncryptionInfo(true)
                        }
                    )
                }
            }

            // Bottom Audio Recording Banner or Normal Input Bar
            if (isRecordingAudio) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recording audio: 00:${String.format("%02d", recordingDurationSec)}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row {
                            IconButton(onClick = { isRecordingAudio = false }) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = "Cancel", tint = Color.Gray)
                            }
                            IconButton(
                                onClick = {
                                    isRecordingAudio = false
                                    val duration = if (recordingDurationSec == 0) 4 else recordingDurationSec
                                    val waveform = List(12) { 0.2f + (it % 5) * 0.18f }
                                    viewModel.sendVoiceMessage(duration, waveform)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Audio",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                // Regular Input bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Media attachment button
                        IconButton(
                            onClick = {
                                // Simulate sending photo attachment
                                viewModel.sendImageMessage(
                                    imageUrl = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=500",
                                    caption = "Encrypted graphic mockup ✨"
                                )
                            },
                            modifier = Modifier.testTag("attach_media_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Attach Media",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Message (End-to-End Encrypted)...", fontSize = 13.sp) },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field"),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                },
                                modifier = Modifier.testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            // Mic Button for voice recording
                            IconButton(
                                onClick = {
                                    isRecordingAudio = true
                                },
                                modifier = Modifier.testTag("record_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice Note",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Encryption sheet
        if (showEncryptionInfo) {
            EncryptionDialog(
                chat = chat,
                inspectedMessage = inspectedMessageForCrypto,
                onDismiss = {
                    inspectedMessageForCrypto = null
                    viewModel.toggleEncryptionInfo(false)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    message: Message,
    isMe: Boolean,
    isGroup: Boolean,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onInspectCipher: () -> Unit
) {
    if (message.messageType == MessageType.SYSTEM) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    var isVoicePlaying by remember { mutableStateOf(false) }
    var voiceProgress by remember { mutableStateOf(0f) }

    val bubbleColor = if (isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isMe) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMe && isGroup) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = bubbleColor,
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    )
                )
                .combinedClickable(
                    onClick = { /* normal click */ },
                    onLongClick = onLongClick
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (message.messageType) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 15.sp
                        )
                    }
                    MessageType.IMAGE -> {
                        if (!message.mediaUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = "Attached photo",
                                modifier = Modifier
                                    .size(width = 240.dp, height = 180.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (message.text.isNotBlank() && message.text != "Sent a photo") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    MessageType.VOICE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isVoicePlaying = !isVoicePlaying
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMe) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play Audio",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            VoiceWaveformVisualizer(
                                amplitudes = message.voiceWaveform.map { it.toFloat() },
                                activeColor = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                                inactiveColor = if (isMe) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                playbackProgress = if (isVoicePlaying) 0.6f else 0f
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${message.voiceDurationSec}s",
                                color = textColor.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Time, lock icon & read status
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val timeFormatted = remember(message.timestampMillis) {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestampMillis))
                    }
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )

                    if (isMe) {
                        when (message.messageStatus) {
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reactions Pills below bubble
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.forEach { (emoji, userIds) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { onReactionClick(emoji) },
                                onLongClick = onLongClick
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            if (userIds.size > 1) {
                                Text(
                                    text = "${userIds.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
