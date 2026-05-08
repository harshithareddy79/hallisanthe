package com.hallisanthi.digital.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hallisanthi.digital.models.ChatMessage

@Dao
interface ChatDao {

    @Insert
    suspend fun insert(message: ChatMessage): Long

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesLive(convId: String): LiveData<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    suspend fun getMessages(convId: String): List<ChatMessage>

    /** All conversations involving this user — latest message per conversation */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE conversationId IN (
            SELECT conversationId FROM chat_messages 
            WHERE senderId = :userId OR receiverId = :userId
        )
        AND id IN (
            SELECT MAX(id) FROM chat_messages GROUP BY conversationId
        )
        ORDER BY timestamp DESC
    """)
    suspend fun getLastMessagePerConversation(userId: Long): List<ChatMessage>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadCountLive(userId: Long): LiveData<Int>

    @Query("UPDATE chat_messages SET isRead = 1 WHERE conversationId = :convId AND receiverId = :userId")
    suspend fun markConversationRead(convId: String, userId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :convId AND receiverId = :userId AND isRead = 0")
    suspend fun getUnreadInConversation(convId: String, userId: Long): Int
}
