package com.hallisanthi.digital.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "recently_viewed",
    indices = [androidx.room.Index(value = ["userId", "productId"], unique = true)]
)
data class RecentlyViewed(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val productPrice: Double = 0.0,
    val productCategory: String = "",
    val productImagePath: String = "",
    val viewedAt: Long = System.currentTimeMillis()
)
