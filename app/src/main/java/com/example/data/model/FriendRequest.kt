package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserHandle: String = "",
    val fromUserAvatar: String = "",
    val toUserId: String = "",
    val message: String = "Hey! Let's connect on L Chat.",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null
)
