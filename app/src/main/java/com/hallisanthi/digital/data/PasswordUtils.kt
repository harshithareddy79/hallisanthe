package com.hallisanthi.digital.data

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {

    /** Hash a plaintext password with a random salt. Returns "salt:hash" */
    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hash = sha256("$saltHex:$password")
        return "$saltHex:$hash"
    }

    /** Verify a plaintext password against stored "salt:hash" */
    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val saltHex = parts[0]
        val expectedHash = parts[1]
        val actualHash = sha256("$saltHex:$password")
        return actualHash == expectedHash
    }

    /** Validate password strength */
    fun isStrong(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.WEAK
        val hasUpper  = password.any { it.isUpperCase() }
        val hasLower  = password.any { it.isLowerCase() }
        val hasDigit  = password.any { it.isDigit() }
        val hasSpecial = password.any { "!@#$%^&*()_+-=[]{}|;':\",./<>?".contains(it) }
        val score = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
        return when {
            score >= 4 -> PasswordStrength.STRONG
            score >= 3 -> PasswordStrength.MEDIUM
            else       -> PasswordStrength.WEAK
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /** Generate a 6-digit OTP-style reset token */
    fun generateResetToken(): String {
        return String.format("%06d", SecureRandom().nextInt(1_000_000))
    }

    enum class PasswordStrength { WEAK, MEDIUM, STRONG }
}
