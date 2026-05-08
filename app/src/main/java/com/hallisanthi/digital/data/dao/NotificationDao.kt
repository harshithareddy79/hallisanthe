package com.hallisanthi.digital.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hallisanthi.digital.models.AppNotification

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(n: AppNotification): Long

    @Query("SELECT * FROM notifications WHERE userId = :uid ORDER BY timestamp DESC LIMIT 50")
    fun getForUserLive(uid: Long): LiveData<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :uid AND isRead = 0")
    fun getUnreadCountLive(uid: Long): LiveData<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :uid")
    suspend fun markAllRead(uid: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("DELETE FROM notifications WHERE userId = :uid AND timestamp < :before")
    suspend fun deleteOld(uid: Long, before: Long)
}
