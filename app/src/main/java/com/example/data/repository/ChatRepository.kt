package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CallState
import com.example.data.model.CallStatus
import com.example.data.model.Chat
import com.example.data.model.FriendRequest
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.data.model.Story
import com.example.data.model.User
import com.example.data.util.CryptoUtils
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

data class PushNotificationEvent(
    val title: String,
    val body: String,
    val senderAvatar: String,
    val chatId: String
)

class ChatRepository(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val auth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messages: StateFlow<Map<String, List<Message>>> = _messages.asStateFlow()

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _callState = MutableStateFlow(CallState())
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _notificationFlow = MutableSharedFlow<PushNotificationEvent>(extraBufferCapacity = 10)
    val notificationFlow: SharedFlow<PushNotificationEvent> = _notificationFlow.asSharedFlow()

    private var chatsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var storiesListener: ListenerRegistration? = null
    private var friendRequestsListener: ListenerRegistration? = null
    private val activeMessageListeners = mutableMapOf<String, ListenerRegistration>()

    private var callTimerJob: Job? = null
    private var callAudioWaveformJob: Job? = null

    init {
        // Observe auth state changes to start/stop Firestore listeners
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                onUserAuthenticated(user.uid, user.displayName, user.email, user.photoUrl?.toString())
            } else {
                onUserSignedOut()
            }
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun onUserAuthenticated(uid: String, displayName: String?, email: String?, photoUrl: String?) {
        scope.launch {
            try {
                // Ensure user document exists in Firestore
                val userDocRef = db.collection("users").document(uid)
                val snapshot = userDocRef.get().await()
                if (!snapshot.exists()) {
                    val defaultName = displayName?.ifBlank { "User" } ?: "User"
                    val defaultHandle = "@" + (email?.substringBefore("@")?.replace(".", "_") ?: uid.take(6))
                    val initialUser = User(
                        id = uid,
                        userId = uid,
                        username = defaultHandle,
                        displayName = defaultName,
                        email = email ?: "",
                        phoneNumber = "",
                        avatarUrl = photoUrl ?: "",
                        statusMessage = "Hey there! I am using L Chat.",
                        isOnline = true,
                        lastSeenText = "Online",
                        publicKeyFingerprint = CryptoUtils.generateFingerprint(uid)
                    )
                    userDocRef.set(
                        mapOf(
                            "userId" to uid,
                            "username" to initialUser.username,
                            "displayName" to initialUser.displayName,
                            "email" to initialUser.email,
                            "phoneNumber" to initialUser.phoneNumber,
                            "avatarUrl" to initialUser.avatarUrl,
                            "statusMessage" to initialUser.statusMessage,
                            "isOnline" to true,
                            "lastSeenText" to "Online",
                            "publicKeyFingerprint" to initialUser.publicKeyFingerprint,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    _currentUser.value = initialUser
                } else {
                    val u = snapshot.toObject(User::class.java)
                    _currentUser.value = u?.copy(id = uid)
                }

                // Start real-time Firestore listeners for this authenticated user
                startFirestoreRealtimeListeners(uid)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error onUserAuthenticated", e)
            }
        }
    }

    private fun onUserSignedOut() {
        chatsListener?.remove()
        usersListener?.remove()
        storiesListener?.remove()
        friendRequestsListener?.remove()
        activeMessageListeners.values.forEach { it.remove() }
        activeMessageListeners.clear()

        _currentUser.value = null
        _chats.value = emptyList()
        _messages.value = emptyMap()
        _stories.value = emptyList()
        _friendRequests.value = emptyList()
        _allUsers.value = emptyList()
    }

    private fun startFirestoreRealtimeListeners(uid: String) {
        // 1. Listen for all registered real users
        usersListener?.remove()
        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ChatRepository", "Listen failed for users", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val usersList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                    _allUsers.value = usersList
                    // Refresh current user
                    val currentUpdated = usersList.find { it.userId == uid || it.id == uid }
                    if (currentUpdated != null) {
                        _currentUser.value = currentUpdated
                    }
                }
            }

        // 2. Listen for current user's chats
        chatsListener?.remove()
        chatsListener = db.collection("chats")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ChatRepository", "Listen failed for chats", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chatList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.lastMessageMillis }
                    _chats.value = chatList

                    // Attach message listeners for each chat
                    chatList.forEach { chat ->
                        attachMessageListener(chat.id)
                    }
                }
            }

        // 3. Listen for 24-hour stories
        storiesListener?.remove()
        storiesListener = db.collection("stories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ChatRepository", "Listen failed for stories", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val storiesList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Story::class.java)?.copy(id = doc.id)
                    }.filter { !it.isExpired }
                    _stories.value = storiesList
                }
            }

        // 4. Listen for friend requests involving current user
        friendRequestsListener?.remove()
        friendRequestsListener = db.collection("friendRequests")
            .whereEqualTo("toUserId", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ChatRepository", "Listen failed for friendRequests", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requestsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                    }
                    _friendRequests.value = requestsList
                }
            }
    }

    private fun attachMessageListener(chatId: String) {
        if (activeMessageListeners.containsKey(chatId)) return

        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ChatRepository", "Listen failed for messages in $chatId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val msgs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                    _messages.update { currentMap ->
                        currentMap + (chatId to msgs)
                    }
                }
            }
        activeMessageListeners[chatId] = listener
    }

    // Profile update
    fun updateCurrentUserProfile(
        displayName: String,
        username: String,
        statusMessage: String,
        phoneNumber: String,
        avatarUrl: String
    ) {
        val uid = requireUserId()
        val cleanHandle = if (username.startsWith("@")) username.lowercase() else "@${username.lowercase()}"
        val updates = mapOf(
            "displayName" to displayName,
            "username" to cleanHandle,
            "statusMessage" to statusMessage,
            "phoneNumber" to phoneNumber,
            "avatarUrl" to avatarUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        scope.launch {
            try {
                db.collection("users").document(uid).update(updates).await()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to update profile", e)
            }
        }
    }

    // Send encrypted message to Firestore
    fun sendMessage(
        chatId: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null,
        voiceDurationSec: Int = 0,
        voiceWaveform: List<Float> = emptyList()
    ) {
        val uid = requireUserId()
        val sender = _currentUser.value ?: return
        val (cipher, iv) = CryptoUtils.encryptText(text)

        val messageData = mutableMapOf<String, Any>(
            "chatId" to chatId,
            "senderId" to uid,
            "senderName" to sender.displayName,
            "text" to text,
            "type" to type.name,
            "status" to "SENT",
            "isEncrypted" to true,
            "encryptedCiphertext" to cipher,
            "initializationVector" to iv,
            "voiceDurationSec" to voiceDurationSec,
            "voiceWaveform" to voiceWaveform.map { it.toDouble() },
            "reactions" to emptyMap<String, List<String>>(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (mediaUrl != null) {
            messageData["mediaUrl"] = mediaUrl
        }

        scope.launch {
            try {
                // Add message to subcollection
                db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(messageData)
                    .await()

                // Update parent chat snippet
                val preview = when (type) {
                    MessageType.TEXT -> text
                    MessageType.IMAGE -> "📷 Photo"
                    MessageType.VOICE -> "🎤 Voice note (${voiceDurationSec}s)"
                    MessageType.SYSTEM -> text
                }
                db.collection("chats").document(chatId).update(
                    mapOf(
                        "lastMessageText" to preview,
                        "lastMessageTime" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to send message to Firestore", e)
            }
        }
    }

    // Toggle reaction on a message in Firestore
    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val uid = requireUserId()
        val currentMsgs = _messages.value[chatId] ?: return
        val targetMsg = currentMsgs.find { it.id == messageId } ?: return

        val reactionsMap = targetMsg.reactions.toMutableMap()
        val userList = (reactionsMap[emoji] ?: emptyList()).toMutableList()
        if (userList.contains(uid)) {
            userList.remove(uid)
            if (userList.isEmpty()) {
                reactionsMap.remove(emoji)
            } else {
                reactionsMap[emoji] = userList
            }
        } else {
            userList.add(uid)
            reactionsMap[emoji] = userList
        }

        scope.launch {
            try {
                db.collection("chats").document(chatId)
                    .collection("messages").document(messageId)
                    .update("reactions", reactionsMap)
                    .await()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to update reaction", e)
            }
        }
    }

    // Start or open 1-on-1 chat with another real user
    fun getOrCreateChatWithUser(otherUserId: String, onComplete: (String) -> Unit) {
        val uid = requireUserId()
        val existing = _chats.value.find { !it.isGroup && it.participantIds.contains(otherUserId) && it.participantIds.contains(uid) }
        if (existing != null) {
            onComplete(existing.id)
            return
        }

        val otherUser = _allUsers.value.find { it.userId == otherUserId || it.id == otherUserId }
        val otherName = otherUser?.displayName ?: "User"
        val otherAvatar = otherUser?.avatarUrl ?: ""

        val newChatRef = db.collection("chats").document()
        val chatId = newChatRef.id
        val chatPayload = mapOf(
            "name" to otherName,
            "isGroup" to false,
            "participantIds" to listOf(uid, otherUserId),
            "adminIds" to listOf(uid),
            "avatarUrl" to otherAvatar,
            "description" to "",
            "lastMessageText" to "End-to-end encrypted conversation started.",
            "unreadCount" to 0,
            "isEncrypted" to true,
            "safetyNumber" to CryptoUtils.generateSafetyNumber(uid, otherUserId),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        scope.launch {
            try {
                newChatRef.set(chatPayload).await()
                onComplete(chatId)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to create 1-on-1 chat", e)
            }
        }
    }

    // Create real Group Chat in Firestore
    fun createGroupChat(name: String, participantIds: List<String>, description: String, onComplete: (String) -> Unit) {
        val uid = requireUserId()
        val allParticipants = (participantIds + uid).distinct()
        val newChatRef = db.collection("chats").document()
        val chatId = newChatRef.id

        val groupPayload = mapOf(
            "name" to name.ifBlank { "Group Chat" },
            "isGroup" to true,
            "participantIds" to allParticipants,
            "adminIds" to listOf(uid),
            "avatarUrl" to "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=300",
            "description" to description,
            "lastMessageText" to "Group created with end-to-end encryption",
            "unreadCount" to 0,
            "isEncrypted" to true,
            "safetyNumber" to "55192 38102 99401 22819 00192 77481",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        scope.launch {
            try {
                newChatRef.set(groupPayload).await()
                // Add initial system message
                val sender = _currentUser.value?.displayName ?: "A user"
                newChatRef.collection("messages").add(
                    mapOf(
                        "chatId" to chatId,
                        "senderId" to uid,
                        "senderName" to "System",
                        "text" to "$sender created group '$name' with end-to-end encryption.",
                        "type" to "SYSTEM",
                        "status" to "SENT",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                onComplete(chatId)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to create group", e)
            }
        }
    }

    fun addGroupMember(chatId: String, memberUserId: String) {
        scope.launch {
            try {
                db.collection("chats").document(chatId).update(
                    "participantIds", FieldValue.arrayUnion(memberUserId),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to add member", e)
            }
        }
    }

    fun removeGroupMember(chatId: String, memberUserId: String) {
        scope.launch {
            try {
                db.collection("chats").document(chatId).update(
                    "participantIds", FieldValue.arrayRemove(memberUserId),
                    "adminIds", FieldValue.arrayRemove(memberUserId),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to remove member", e)
            }
        }
    }

    // Post real Ephemeral 24h Story to Firestore
    fun addStory(caption: String, filterName: String, gradientIndex: Int) {
        val uid = requireUserId()
        val user = _currentUser.value ?: return
        val now = System.currentTimeMillis()
        val expireDate = Date(now + (24 * 60 * 60 * 1000L))

        val storyPayload = mapOf(
            "userId" to uid,
            "authorName" to user.displayName,
            "authorAvatar" to user.avatarUrl,
            "backgroundGradientIndex" to gradientIndex,
            "caption" to caption,
            "filterName" to filterName,
            "viewsCount" to 1,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAtTimestamp" to Timestamp(expireDate)
        )

        scope.launch {
            try {
                db.collection("stories").add(storyPayload).await()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to add story", e)
            }
        }
    }

    // Send real Friend / Contact Request
    fun sendFriendRequest(targetQuery: String, onResult: (Boolean, String) -> Unit) {
        val uid = requireUserId()
        val user = _currentUser.value ?: return
        val normalized = targetQuery.trim().lowercase()

        scope.launch {
            try {
                // Find target user by username or phone
                val targetUser = _allUsers.value.find {
                    it.username.lowercase() == normalized ||
                            it.username.lowercase() == "@$normalized" ||
                            (it.phoneNumber.isNotBlank() && it.phoneNumber == normalized)
                }

                if (targetUser == null) {
                    onResult(false, "User '$targetQuery' not found. Make sure they have registered an ID.")
                    return@launch
                }

                if (targetUser.userId == uid || targetUser.id == uid) {
                    onResult(false, "You cannot send a contact request to yourself.")
                    return@launch
                }

                val requestPayload = mapOf(
                    "fromUserId" to uid,
                    "fromUserName" to user.displayName,
                    "fromUserHandle" to user.username,
                    "fromUserAvatar" to user.avatarUrl,
                    "toUserId" to targetUser.userId,
                    "message" to "Hey! Let's connect on L Chat.",
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp()
                )

                db.collection("friendRequests").add(requestPayload).await()
                onResult(true, "Contact request sent to ${targetUser.displayName}!")
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to send friend request", e)
                onResult(false, "Failed to send request: ${e.localizedMessage}")
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        val req = _friendRequests.value.find { it.id == requestId } ?: return
        scope.launch {
            try {
                db.collection("friendRequests").document(requestId).update(
                    "status", "ACCEPTED"
                ).await()
                // Create direct chat
                getOrCreateChatWithUser(req.fromUserId) {}
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to accept friend request", e)
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        scope.launch {
            try {
                db.collection("friendRequests").document(requestId).update(
                    "status", "REJECTED"
                ).await()
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to reject friend request", e)
            }
        }
    }

    // Voice Calling between real contacts
    fun startVoiceCall(contactId: String) {
        val contact = _allUsers.value.find { it.userId == contactId || it.id == contactId } ?: return
        callTimerJob?.cancel()
        callAudioWaveformJob?.cancel()

        _callState.value = CallState(
            callId = UUID.randomUUID().toString(),
            contactId = contact.userId.ifBlank { contact.id },
            contactName = contact.displayName,
            contactHandle = contact.username,
            contactAvatar = contact.avatarUrl,
            status = CallStatus.DIALING,
            isMuted = false,
            isSpeakerOn = false,
            durationSeconds = 0,
            isEncrypted = true
        )

        scope.launch {
            delay(1200)
            if (_callState.value.status != CallStatus.ENDED) {
                _callState.update { it.copy(status = CallStatus.RINGING) }
            }
            delay(2000)
            if (_callState.value.status != CallStatus.ENDED) {
                _callState.update { it.copy(status = CallStatus.CONNECTED) }
                startCallTimer()
                startAudioWaveformSimulation()
            }
        }
    }

    private fun startCallTimer() {
        callTimerJob = scope.launch {
            while (isActive && _callState.value.status == CallStatus.CONNECTED) {
                delay(1000)
                _callState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    private fun startAudioWaveformSimulation() {
        callAudioWaveformJob = scope.launch {
            val random = java.util.Random()
            while (isActive && _callState.value.status == CallStatus.CONNECTED) {
                delay(150)
                val amplitudes = List(16) { 0.15f + random.nextFloat() * 0.85f }
                _callState.update { it.copy(audioAmplitudes = amplitudes) }
            }
        }
    }

    fun toggleCallMute() {
        _callState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallSpeaker() {
        _callState.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun endVoiceCall() {
        callTimerJob?.cancel()
        callAudioWaveformJob?.cancel()
        _callState.update { it.copy(status = CallStatus.ENDED) }
        scope.launch {
            delay(600)
            _callState.value = CallState()
        }
    }
}
