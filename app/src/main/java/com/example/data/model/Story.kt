package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class StoryStroke(
    val points: List<Pair<Float, Float>> = emptyList(),
    val colorHex: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 8f
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val mediaUrl: String? = null,
    val backgroundGradientIndex: Int = 0,
    val caption: String = "",
    val filterName: String = "Normal",
    val drawingStrokes: List<StoryStroke> = emptyList(),
    val isViewedByCurrentUser: Boolean = false,
    val viewsCount: Int = 1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAtTimestamp: Timestamp? = null
) {
    val isExpired: Boolean
        get() {
            val exp = expiresAtTimestamp?.toDate()?.time ?: (createdAt?.toDate()?.time?.plus(24 * 60 * 60 * 1000L) ?: Long.MAX_VALUE)
            return System.currentTimeMillis() >= exp
        }

    val remainingTimeText: String
        get() {
            val exp = expiresAtTimestamp?.toDate()?.time ?: (createdAt?.toDate()?.time?.plus(24 * 60 * 60 * 1000L) ?: Long.MAX_VALUE)
            val remainingMillis = exp - System.currentTimeMillis()
            if (remainingMillis <= 0) return "Expired"
            val hours = remainingMillis / (1000 * 60 * 60)
            val minutes = (remainingMillis % (1000 * 60 * 60)) / (1000 * 60)
            return if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
        }
}
