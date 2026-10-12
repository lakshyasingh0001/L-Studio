package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class User(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "", // e.g. "@lakshya"
    val displayName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val statusMessage: String = "Available on L Chat",
    val isOnline: Boolean = true,
    val lastSeenText: String = "Online",
    val publicKeyFingerprint: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
