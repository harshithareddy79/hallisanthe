package com.hallisanthi.digital.data.repository

import androidx.lifecycle.LiveData
import com.hallisanthi.digital.data.dao.ProductDao
import com.hallisanthi.digital.models.Product

class ProductRepository(private val dao: ProductDao) {
    fun getAllProducts(): LiveData<List<Product>> = dao.getAllProducts()
    suspend fun getAllProductsOnce(): List<Product> = dao.getAllProductsOnce()
    fun getByCategory(category: String): LiveData<List<Product>> = dao.getByCategory(category)
    fun search(query: String): LiveData<List<Product>> = dao.search(query)
    fun searchInCategory(query: String, category: String): LiveData<List<Product>> = dao.searchInCategory(query, category)
    fun getBySellerPhone(phone: String): LiveData<List<Product>> = dao.getBySellerPhone(phone)
    suspend fun insert(product: Product): Long = dao.insert(product)
    suspend fun update(product: Product) = dao.update(product)
    suspend fun delete(product: Product) = dao.delete(product)
    suspend fun softDelete(id: Long) = dao.softDelete(id)
    suspend fun setAvailability(id: Long, available: Boolean) = dao.setAvailability(id, available)
    suspend fun getProductById(id: Long): Product? = dao.getProductById(id)
    suspend fun updateRating(id: Long, newRating: Float, newCount: Int) = dao.updateRating(id, newRating, newCount)
}
