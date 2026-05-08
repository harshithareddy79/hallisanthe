package com.hallisanthi.digital.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hallisanthi.digital.models.Review

@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: Review): Long

    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY createdAt DESC")
    fun getForProductLive(productId: Long): LiveData<List<Review>>

    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY createdAt DESC")
    suspend fun getForProduct(productId: Long): List<Review>

    @Query("SELECT * FROM reviews WHERE reviewerId = :userId ORDER BY createdAt DESC")
    suspend fun getByUser(userId: Long): List<Review>

    @Query("SELECT * FROM reviews WHERE productId = :productId AND reviewerId = :userId LIMIT 1")
    suspend fun getUserReviewForProduct(productId: Long, userId: Long): Review?

    @Query("SELECT AVG(stars) FROM reviews WHERE productId = :productId")
    suspend fun getAverageRating(productId: Long): Float?

    @Query("SELECT COUNT(*) FROM reviews WHERE productId = :productId")
    suspend fun getReviewCount(productId: Long): Int

    @Query("UPDATE reviews SET helpfulCount = helpfulCount + 1 WHERE id = :id")
    suspend fun markHelpful(id: Long)
}
