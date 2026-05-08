package com.hallisanthi.digital.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hallisanthi.digital.models.Order

@Dao
interface OrderDao {

    @Insert
    suspend fun insert(order: Order): Long

    @Update
    suspend fun update(order: Order)

    @Query("SELECT * FROM orders WHERE buyerId = :uid ORDER BY createdAt DESC")
    fun getBuyerOrdersLive(uid: Long): LiveData<List<Order>>

    @Query("SELECT * FROM orders WHERE sellerId = :uid ORDER BY createdAt DESC")
    fun getSellerOrdersLive(uid: Long): LiveData<List<Order>>

    @Query("SELECT * FROM orders WHERE buyerId = :uid ORDER BY createdAt DESC")
    suspend fun getBuyerOrders(uid: Long): List<Order>

    @Query("SELECT * FROM orders WHERE sellerId = :uid ORDER BY createdAt DESC")
    suspend fun getSellerOrders(uid: Long): List<Order>

    @Query("SELECT COUNT(*) FROM orders WHERE sellerId = :uid")
    suspend fun getTotalSales(uid: Long): Int

    @Query("SELECT SUM(price * quantity) FROM orders WHERE sellerId = :uid AND status != 'CANCELLED'")
    suspend fun getTotalRevenue(uid: Long): Double?

    @Query("SELECT COUNT(*) FROM orders WHERE sellerId = :uid AND status = :status")
    suspend fun countByStatus(uid: Long, status: String): Int

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Order?

    @Query("SELECT COUNT(*) FROM orders WHERE buyerId = :uid AND status = 'CONTACTED'")
    fun getPendingCountLive(uid: Long): LiveData<Int>
}
