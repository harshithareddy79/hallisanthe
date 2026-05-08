package com.hallisanthi.digital.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "reviews",
    indices = [androidx.room.Index(value = ["productId", "reviewerId"], unique = true)]
)
data class Review(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long = 0,
    val reviewerId: Long = 0,
    val reviewerName: String = "",
    val stars: Int = 5,           // 1-5
    val comment: String = "",
    val isVerifiedPurchase: Boolean = false,
    val helpfulCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
