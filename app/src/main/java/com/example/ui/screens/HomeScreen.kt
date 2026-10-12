package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chat
import com.example.ui.AppScreen
import com.example.ui.ChatViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StoryGradientEnd
import com.example.ui.theme.StoryGradientMid
import com.example.ui.theme.StoryGradientStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser
        .collectAsState()
    val chats by viewModel.chats.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Chats", "Groups", "Stories", "Contacts")

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, NeonCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("L", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "L Chat",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "E2EE",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "E2EE Secured",
                                        color = EmeraldGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Personalized Encrypted Messaging",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SEARCH_CONTACTS) },
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (friendRequests.isNotEmpty()) {
                                    Badge { Text("${friendRequests.size}") }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search Users")
                        }
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.PROFILE_SETTINGS) },
                        modifier = Modifier.testTag("home_profile_button")
                    ) {
                        UserAvatar(
                            avatarUrl = currentUser?.avatarUrl,
                            name = currentUser?.displayName ?: "User",
                            size = 32.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Ephemeral 24h Stories Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Add to Story" item
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.navigateTo(AppScreen.CREATE_STORY) }
                                .testTag("create_story_card")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                UserAvatar(
                                    avatarUrl = currentUser?.avatarUrl,
                                    name = currentUser?.displayName ?: "User",
                                    size = 56.dp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Post Story",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your Story",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Active Ephemeral Stories (Vanishes in 24h)
                    itemsIndexed(stories) { index, story ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                viewModel.openStoryViewer(index)
                            }
                        ) {
                            UserAvatar(
                                avatarUrl = story.authorAvatar,
                                name = story.authorName,
                                size = 56.dp,
                                showStoryRing = true,
                                hasUnseenStory = !story.isViewedByCurrentUser
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = story.authorName.split(" ").firstOrNull() ?: story.authorName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = story.remainingTimeText,
                                fontSize = 9.sp,
                                color = NeonCyan,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Navigation Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTabIndex) {
                    0 -> { // 1-on-1 Chats
                        val directChats = chats.filter { !it.isGroup }
                        ChatListView(
                            chats = directChats,
                            onChatClick = { viewModel.openChat(it.id) }
                        )
                    }
                    1 -> { // Group Chats
                        val groupChats = chats.filter { it.isGroup }
                        GroupChatsTab(
                            groups = groupChats,
                            onChatClick = { viewModel.openChat(it.id) },
                            onCreateGroupClick = { viewModel.navigateTo(AppScreen.CREATE_GROUP) }
                        )
                    }
                    2 -> { // Stories feed
                        StoriesFeedTab(
                            stories = stories,
                            onStoryClick = { viewModel.openStoryViewer(it) },
                            onCreateStoryClick = { viewModel.navigateTo(AppScreen.CREATE_STORY) }
                        )
                    }
                    3 -> { // Contacts & Requests
                        ContactsTab(
                            allUsers = allUsers.filter { it.userId != (currentUser?.userId ?: "") && it.id != (currentUser?.id ?: "") },
                            friendRequests = friendRequests,
                            onStartChat = { viewModel.startChatWithUser(it) },
                            onStartCall = { viewModel.startVoiceCall(it) },
                            onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                            onRejectRequest = { viewModel.rejectFriendRequest(it) },
                            onSearchClick = { viewModel.navigateTo(AppScreen.SEARCH_CONTACTS) }
                        )
                    }
                }

                // Floating Action Button
                FloatingActionButton(
                    onClick = {
                        when (selectedTabIndex) {
                            1 -> viewModel.navigateTo(AppScreen.CREATE_GROUP)
                            2 -> viewModel.navigateTo(AppScreen.CREATE_STORY)
                            else -> viewModel.navigateTo(AppScreen.SEARCH_CONTACTS)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("home_fab_button")
                ) {
                    Icon(
                        imageVector = when (selectedTabIndex) {
                            1 -> Icons.Default.GroupAdd
                            2 -> Icons.Default.CameraAlt
                            else -> Icons.AutoMirrored.Filled.Chat
                        },
                        contentDescription = "Action"
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListView(
    chats: List<Chat>,
    onChatClick: (Chat) -> Unit
) {
    if (chats.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active conversations yet.\nTap + or Search ID to start chatting!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(chats, key = { it.id }) { chat ->
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChatClick(chat) }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUrl = chat.avatarUrl,
                            name = chat.name,
                            size = 52.dp,
                            isOnline = !chat.isGroup
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = chat.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Encrypted",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                val timeText = remember(chat.lastMessageMillis) {
                                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(chat.lastMessageMillis))
                                }
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chat.lastMessageText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (chat.unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${chat.unreadCount}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupChatsTab(
    groups: List<Chat>,
    onChatClick: (Chat) -> Unit,
    onCreateGroupClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onCreateGroupClick() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = "New Group",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Create New Encrypted Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Add contacts, assign admins, and share group keys", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        ChatListView(chats = groups, onChatClick = onChatClick)
    }
}

@Composable
fun StoriesFeedTab(
    stories: List<com.example.data.model.Story>,
    onStoryClick: (Int) -> Unit,
    onCreateStoryClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateStoryClick() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Post a Vanishing Story", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Share photos with filters & canvas doodles that vanish after 24h", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        itemsIndexed(stories) { index, story ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStoryClick(index) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        avatarUrl = story.authorAvatar,
                        name = story.authorName,
                        size = 50.dp,
                        showStoryRing = true,
                        hasUnseenStory = !story.isViewedByCurrentUser
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = story.authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = story.caption.ifBlank { "Vanishing 24h Status" }, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = story.remainingTimeText, fontSize = 11.sp, color = NeonCyan)
                    }
                    Text("View", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ContactsTab(
    allUsers: List<com.example.data.model.User>,
    friendRequests: List<com.example.data.model.FriendRequest>,
    onStartChat: (String) -> Unit,
    onStartCall: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchClick() }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PersonSearch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Search Users by ID or Phone Number", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        if (friendRequests.isNotEmpty()) {
            item {
                Text("Pending Requests (${friendRequests.size})", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 14.sp)
            }
            items(friendRequests) { req ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(avatarUrl = req.fromUserAvatar, name = req.fromUserName, size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.fromUserName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(req.fromUserHandle, fontSize = 12.sp, color = NeonCyan)
                        }
                        IconButton(onClick = { onAcceptRequest(req.id) }) {
                            Icon(Icons.Default.Add, contentDescription = "Accept", tint = EmeraldGreen)
                        }
                    }
                }
            }
        }

        item {
            Text("Direct Contacts (${allUsers.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(allUsers) { user ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(avatarUrl = user.avatarUrl, name = user.displayName, size = 44.dp, isOnline = user.isOnline)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(user.username, fontSize = 12.sp, color = NeonCyan)
                        Text(user.statusMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = { onStartCall(user.id) }) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onStartChat(user.id) }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
