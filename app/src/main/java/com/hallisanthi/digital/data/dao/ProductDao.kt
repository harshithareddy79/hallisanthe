package com.hallisanthi.digital.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.hallisanthi.digital.models.Product

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getAllProductsOnce(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id AND isActive = 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE category = :category AND isActive = 1 ORDER BY createdAt DESC")
    fun getByCategory(category: String): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun search(query: String): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND category = :category AND (name LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchInCategory(query: String, category: String): LiveData<List<Product>>

    @Query("UPDATE products SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE products SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailability(id: Long, available: Boolean)

    @Query("SELECT * FROM products WHERE sellerPhone = :phone AND isActive = 1 ORDER BY createdAt DESC")
    fun getBySellerPhone(phone: String): LiveData<List<Product>>

    @Query("UPDATE products SET rating = :newRating, ratingCount = :newCount WHERE id = :id")
    suspend fun updateRating(id: Long, newRating: Float, newCount: Int)

    @Query("SELECT * FROM products WHERE sellerPhone = :phone AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getProductsBySellerPhone(phone: String): List<Product>

    @Query("UPDATE products SET wishlistCount = wishlistCount + 1 WHERE id = :id")
    suspend fun incrementWishlist(id: Long)
}
