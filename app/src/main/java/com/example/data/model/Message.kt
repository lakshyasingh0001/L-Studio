package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    SYSTEM
}

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ
}

@IgnoreExtraProperties
data class Message(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val status: String = "SENT", // MessageStatus string representation for Firestore
    val type: String = "TEXT", // MessageType string representation for Firestore
    val mediaUrl: String? = null,
    val voiceDurationSec: Int = 0,
    val voiceWaveform: List<Double> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(), // emoji -> list of userIds
    val isEncrypted: Boolean = true,
    val encryptedCiphertext: String = "",
    val initializationVector: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val messageType: MessageType
        get() = try { MessageType.valueOf(type) } catch (e: Exception) { MessageType.TEXT }

    val messageStatus: MessageStatus
        get() = try { MessageStatus.valueOf(status) } catch (e: Exception) { MessageStatus.SENT }

    val timestampMillis: Long
        get() = createdAt?.toDate()?.time ?: System.currentTimeMillis()
}
