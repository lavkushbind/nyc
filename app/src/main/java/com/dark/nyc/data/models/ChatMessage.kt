package com.dark.nyc.data.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "senderId" to senderId,
        "receiverId" to receiverId,
        "text" to text,
        "imageUrl" to imageUrl,
        "replyToText" to replyToText,
        "replyToSenderName" to replyToSenderName,
        "timestamp" to timestamp,
        "isRead" to isRead
    )
}