package com.hallisanthi.digital.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val buyerId: Long = 0,
    val sellerId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val productImagePath: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val artisanName: String = "",
    val quantity: Int = 1,
    val status: String = STATUS_CONTACTED,   // CONTACTED | ORDERED | DELIVERED | CANCELLED
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_CONTACTED  = "CONTACTED"
        const val STATUS_ORDERED    = "ORDERED"
        const val STATUS_DELIVERED  = "DELIVERED"
        const val STATUS_CANCELLED  = "CANCELLED"

        fun statusEmoji(status: String) = when (status) {
            STATUS_CONTACTED -> "📞"
            STATUS_ORDERED   -> "📦"
            STATUS_DELIVERED -> "✅"
            STATUS_CANCELLED -> "❌"
            else             -> "❓"
        }
        fun statusLabel(status: String) = when (status) {
            STATUS_CONTACTED -> "Contacted"
            STATUS_ORDERED   -> "Ordered"
            STATUS_DELIVERED -> "Delivered"
            STATUS_CANCELLED -> "Cancelled"
            else             -> status
        }
    }
    fun getFormattedPrice() = "₹${String.format("%.0f", price)}"
}
