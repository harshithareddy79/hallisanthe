package com.hallisanthi.digital.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val sellerPhone: String = "",
    val imagePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isAvailable: Boolean = true,
    val wishlistCount: Int = 0,
    val description: String = "",
    val artisanName: String = "",
    val rating: Float = 0f,
    val ratingCount: Int = 0
) {
    fun getFormattedPrice(): String = "₹${String.format("%.0f", price)}"

    fun isValid(): Boolean =
        name.isNotBlank() && price > 0 && category.isNotBlank() && sellerPhone.isNotBlank()

    companion object {
        const val CATEGORY_ALL       = "All"
        const val CATEGORY_POTTERY   = "Pottery"
        const val CATEGORY_TEXTILES  = "Textiles"
        const val CATEGORY_BAMBOO    = "Bamboo Crafts"
        const val CATEGORY_METAL     = "Metal Work"
        const val CATEGORY_PAINTINGS = "Paintings"
        const val CATEGORY_JEWELRY   = "Jewelry"
        const val CATEGORY_WOOD      = "Wood Crafts"
        const val CATEGORY_OTHER     = "Other"

        fun getAllCategories(): List<String> = listOf(
            CATEGORY_POTTERY, CATEGORY_TEXTILES, CATEGORY_BAMBOO,
            CATEGORY_METAL, CATEGORY_PAINTINGS, CATEGORY_JEWELRY,
            CATEGORY_WOOD, CATEGORY_OTHER
        )

        const val SORT_NEWEST = "newest"
        const val SORT_PRICE_ASC = "price_asc"
        const val SORT_PRICE_DESC = "price_desc"

        fun getCategoryEmoji(category: String): String = when (category) {
            CATEGORY_POTTERY   -> "🏺"
            CATEGORY_TEXTILES  -> "🧵"
            CATEGORY_BAMBOO    -> "🎋"
            CATEGORY_METAL     -> "⚒️"
            CATEGORY_PAINTINGS -> "🎨"
            CATEGORY_JEWELRY   -> "💍"
            CATEGORY_WOOD      -> "🪵"
            else               -> "🛍️"
        }
    }
}
