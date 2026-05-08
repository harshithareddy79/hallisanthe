package com.hallisanthi.digital.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String = "",   // "userId1_userId2_productId"
    val senderId: Long = 0,
    val receiverId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
