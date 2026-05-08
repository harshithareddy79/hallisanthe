package com.hallisanthi.digital.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,
    val type: String = TYPE_CHAT,     // CHAT | ORDER | WISHLIST | SYSTEM
    val title: String = "",
    val body: String = "",
    val referenceId: Long = 0,        // productId / orderId / conversationId
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CHAT     = "CHAT"
        const val TYPE_ORDER    = "ORDER"
        const val TYPE_WISHLIST = "WISHLIST"
        const val TYPE_SYSTEM   = "SYSTEM"

        fun typeEmoji(type: String) = when (type) {
            TYPE_CHAT     -> "💬"
            TYPE_ORDER    -> "📦"
            TYPE_WISHLIST -> "❤️"
            TYPE_SYSTEM   -> "📣"
            else          -> "🔔"
        }
    }
}
