package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Chat(
    @DocumentId val id: String = "",
    val name: String = "",
    val isGroup: Boolean = false,
    val participantIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val avatarUrl: String = "",
    val description: String = "",
    val lastMessageText: String = "",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = true,
    val safetyNumber: String = "",
    @ServerTimestamp val lastMessageTime: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val lastMessageMillis: Long
        get() = lastMessageTime?.toDate()?.time ?: createdAt?.toDate()?.time ?: System.currentTimeMillis()
}
