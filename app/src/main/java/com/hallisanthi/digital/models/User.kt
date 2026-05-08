package com.hallisanthi.digital.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["phone"], unique = true)
    ]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val passwordHash: String = "",
    val role: String = ROLE_BUYER,
    val profileImagePath: String = "",
    val bio: String = "",
    val location: String = "",
    val isEmailVerified: Boolean = false,
    val resetToken: String = "",
    val resetTokenExpiry: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_BUYER   = "BUYER"
        const val ROLE_ARTISAN = "ARTISAN"
    }
    fun isArtisan() = role == ROLE_ARTISAN
    fun isBuyer()   = role == ROLE_BUYER
}
