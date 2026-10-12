package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallState
import com.example.data.model.Chat
import com.example.data.model.FriendRequest
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.data.model.Story
import com.example.data.model.StoryStroke
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import com.example.data.repository.PushNotificationEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    CHAT_DETAIL,
    VOICE_CALL,
    STORY_VIEWER,
    CREATE_STORY,
    CREATE_GROUP,
    GROUP_DETAILS,
    SEARCH_CONTACTS,
    PROFILE_SETTINGS
}

data class NotificationBannerUi(
    val title: String,
    val body: String,
    val avatarUrl: String,
    val chatId: String,
    val isVisible: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)

    val currentUser: StateFlow<User?> = repository.currentUser
    val allUsers: StateFlow<List<User>> = repository.allUsers
    val chats: StateFlow<List<Chat>> = repository.chats
    val stories: StateFlow<List<Story>> = repository.stories
    val friendRequests: StateFlow<List<FriendRequest>> = repository.friendRequests
    val callState: StateFlow<CallState> = repository.callState

    // Screen stack for back-navigation
    private val _screenStack = MutableStateFlow(listOf(AppScreen.HOME))
    val currentScreen: StateFlow<AppScreen> = MutableStateFlow(AppScreen.HOME).apply {
        viewModelScope.launch {
            _screenStack.collect { stack ->
                this@apply.value = stack.lastOrNull() ?: AppScreen.HOME
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppScreen.HOME)

    // Active selected chat ID
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    // Active story viewing index
    private val _activeStoryIndex = MutableStateFlow(0)
    val activeStoryIndex: StateFlow<Int> = _activeStoryIndex.asStateFlow()

    // Active selected message for reactions
    private val _selectedMessageForReaction = MutableStateFlow<Message?>(null)
    val selectedMessageForReaction: StateFlow<Message?> = _selectedMessageForReaction.asStateFlow()

    // Encryption info dialog
    private val _showEncryptionInfo = MutableStateFlow(false)
    val showEncryptionInfo: StateFlow<Boolean> = _showEncryptionInfo.asStateFlow()

    // Push notification banner
    private val _notificationBanner = MutableStateFlow(NotificationBannerUi("", "", "", "", false))
    val notificationBanner: StateFlow<NotificationBannerUi> = _notificationBanner.asStateFlow()

    init {
        viewModelScope.launch {
            repository.notificationFlow.collect { event ->
                if (_activeChatId.value != event.chatId || _screenStack.value.lastOrNull() != AppScreen.CHAT_DETAIL) {
                    _notificationBanner.value = NotificationBannerUi(
                        title = event.title,
                        body = event.body,
                        avatarUrl = event.senderAvatar,
                        chatId = event.chatId,
                        isVisible = true
                    )
                }
            }
        }
    }

    fun dismissNotificationBanner() {
        _notificationBanner.update { it.copy(isVisible = false) }
    }

    // Navigation
    fun navigateTo(screen: AppScreen) {
        _screenStack.update { it + screen }
    }

    fun navigateBack(): Boolean {
        if (_screenStack.value.size > 1) {
            _screenStack.update { it.dropLast(1) }
            if (_screenStack.value.lastOrNull() != AppScreen.CHAT_DETAIL) {
                _activeChatId.value = null
            }
            return true
        }
        return false
    }

    fun openChat(chatId: String) {
        _activeChatId.value = chatId
        navigateTo(AppScreen.CHAT_DETAIL)
    }

    fun openStoryViewer(index: Int) {
        _activeStoryIndex.value = index
        navigateTo(AppScreen.STORY_VIEWER)
    }

    fun nextStory() {
        if (_activeStoryIndex.value < stories.value.size - 1) {
            _activeStoryIndex.value += 1
        } else {
            navigateBack()
        }
    }

    fun previousStory() {
        if (_activeStoryIndex.value > 0) {
            _activeStoryIndex.value -= 1
        }
    }

    // Messaging
    fun getMessagesForChat(chatId: String): List<Message> {
        return repository.messages.value[chatId] ?: emptyList()
    }

    fun sendMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        if (text.isNotBlank()) {
            repository.sendMessage(chatId, text.trim(), MessageType.TEXT)
        }
    }

    fun sendVoiceMessage(durationSec: Int, waveform: List<Float>) {
        val chatId = _activeChatId.value ?: return
        repository.sendMessage(
            chatId = chatId,
            text = "Voice message ($durationSec s)",
            type = MessageType.VOICE,
            voiceDurationSec = durationSec,
            voiceWaveform = waveform
        )
    }

    fun sendImageMessage(imageUrl: String, caption: String = "") {
        val chatId = _activeChatId.value ?: return
        repository.sendMessage(
            chatId = chatId,
            text = caption.ifBlank { "Sent a photo" },
            type = MessageType.IMAGE,
            mediaUrl = imageUrl
        )
    }

    fun selectMessageForReaction(message: Message?) {
        _selectedMessageForReaction.value = message
    }

    fun toggleReaction(emoji: String) {
        val msg = _selectedMessageForReaction.value ?: return
        val chatId = _activeChatId.value ?: return
        repository.toggleReaction(chatId, msg.id, emoji)
        _selectedMessageForReaction.value = null
    }

    fun toggleReactionDirect(chatId: String, messageId: String, emoji: String) {
        repository.toggleReaction(chatId, messageId, emoji)
    }

    fun toggleEncryptionInfo(show: Boolean) {
        _showEncryptionInfo.value = show
    }

    // Voice Calling
    fun startVoiceCall(contactId: String) {
        repository.startVoiceCall(contactId)
        navigateTo(AppScreen.VOICE_CALL)
    }

    fun toggleCallMute() {
        repository.toggleCallMute()
    }

    fun toggleCallSpeaker() {
        repository.toggleCallSpeaker()
    }

    fun endVoiceCall() {
        repository.endVoiceCall()
        if (currentScreen.value == AppScreen.VOICE_CALL) {
            navigateBack()
        }
    }

    // Group Chat
    fun createGroup(name: String, participantIds: List<String>, description: String) {
        repository.createGroupChat(name, participantIds, description) { newGroupId ->
            navigateBack()
            openChat(newGroupId)
        }
    }

    fun addMemberToGroup(chatId: String, userId: String) {
        repository.addGroupMember(chatId, userId)
    }

    fun removeMemberFromGroup(chatId: String, userId: String) {
        repository.removeGroupMember(chatId, userId)
    }

    // Story Creation
    fun postStory(caption: String, filterName: String, gradientIndex: Int, drawingStrokes: List<StoryStroke> = emptyList()) {
        repository.addStory(caption, filterName, gradientIndex)
        navigateBack()
    }

    // Contacts & Friend Requests
    fun searchAndAddContact(query: String, onResult: (Boolean, String) -> Unit) {
        repository.sendFriendRequest(query, onResult)
    }

    fun acceptFriendRequest(requestId: String) {
        repository.acceptFriendRequest(requestId)
    }

    fun rejectFriendRequest(requestId: String) {
        repository.rejectFriendRequest(requestId)
    }

    fun startChatWithUser(userId: String) {
        repository.getOrCreateChatWithUser(userId) { chatId ->
            openChat(chatId)
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        statusMessage: String,
        phoneNumber: String,
        avatarUrl: String
    ) {
        repository.updateCurrentUserProfile(displayName, username, statusMessage, phoneNumber, avatarUrl)
    }

    fun signOut() {
        Firebase.auth.signOut()
        _screenStack.value = listOf(AppScreen.HOME)
        _activeChatId.value = null
    }
}
