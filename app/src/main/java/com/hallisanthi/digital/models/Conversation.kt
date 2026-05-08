package com.hallisanthi.digital.models

/** Aggregated view of a conversation — not a Room entity, built from queries */
data class Conversation(
    val conversationId: String,
    val otherUserId: Long,
    val otherUserName: String,
    val productId: Long,
    val productName: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isSentByMe: Boolean
)
