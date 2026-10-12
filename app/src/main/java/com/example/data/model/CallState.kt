package com.example.data.model

enum class CallStatus {
    IDLE,
    DIALING,
    RINGING,
    CONNECTED,
    ENDED
}

data class CallState(
    val callId: String = "",
    val contactId: String = "",
    val contactName: String = "",
    val contactHandle: String = "",
    val contactAvatar: String = "",
    val status: CallStatus = CallStatus.IDLE,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val durationSeconds: Int = 0,
    val isEncrypted: Boolean = true,
    val audioAmplitudes: List<Float> = emptyList()
) {
    val durationFormatted: String
        get() {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}
