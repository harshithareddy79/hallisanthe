package com.hallisanthi.digital.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hallisanthi.digital.models.RecentlyViewed

@Dao
interface RecentlyViewedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecentlyViewed): Long

    @Query("SELECT * FROM recently_viewed WHERE userId = :userId ORDER BY viewedAt DESC LIMIT 15")
    fun getForUserLive(userId: Long): LiveData<List<RecentlyViewed>>

    @Query("SELECT * FROM recently_viewed WHERE userId = :userId ORDER BY viewedAt DESC LIMIT 15")
    suspend fun getForUser(userId: Long): List<RecentlyViewed>

    @Query("UPDATE recently_viewed SET viewedAt = :time WHERE userId = :userId AND productId = :productId")
    suspend fun updateTimestamp(userId: Long, productId: Long, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM recently_viewed WHERE userId = :userId AND productId = :productId")
    suspend fun exists(userId: Long, productId: Long): Int

    @Query("DELETE FROM recently_viewed WHERE userId = :userId AND id NOT IN (SELECT id FROM recently_viewed WHERE userId = :userId ORDER BY viewedAt DESC LIMIT 15)")
    suspend fun trimOld(userId: Long)
}
