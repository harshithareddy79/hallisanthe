package com.hallisanthi.digital.data.dao

import androidx.room.*
import com.hallisanthi.digital.models.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): User?

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun emailExists(email: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE phone = :phone)")
    suspend fun phoneExists(phone: String): Boolean

    @Query("SELECT * FROM users WHERE resetToken = :token AND resetTokenExpiry > :now LIMIT 1")
    suspend fun getByResetToken(token: String, now: Long = System.currentTimeMillis()): User?

    @Query("UPDATE users SET resetToken = :token, resetTokenExpiry = :expiry WHERE id = :id")
    suspend fun setResetToken(id: Long, token: String, expiry: Long)

    @Query("UPDATE users SET passwordHash = :hash, resetToken = '', resetTokenExpiry = 0 WHERE id = :id")
    suspend fun updatePassword(id: Long, hash: String)

    @Query("UPDATE users SET lastLoginAt = :time WHERE id = :id")
    suspend fun updateLastLogin(id: Long, time: Long = System.currentTimeMillis())
}
