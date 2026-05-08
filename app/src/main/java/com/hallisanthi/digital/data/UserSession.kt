package com.hallisanthi.digital.data

import android.content.Context
import com.hallisanthi.digital.models.User

object UserSession {

    private const val PREFS         = "user_session"
    private const val KEY_USER_ID   = "user_id"
    private const val KEY_NAME      = "name"
    private const val KEY_EMAIL     = "email"
    private const val KEY_PHONE     = "phone"
    private const val KEY_ROLE      = "role"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_BIO       = "bio"
    private const val KEY_LOCATION  = "location"
    private const val KEY_IMG_PATH  = "profile_img"

    const val ROLE_BUYER   = User.ROLE_BUYER
    const val ROLE_ARTISAN = User.ROLE_ARTISAN

    fun login(context: Context, user: User) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_NAME, user.name)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone)
            .putString(KEY_ROLE, user.role)
            .putString(KEY_BIO, user.bio)
            .putString(KEY_LOCATION, user.location)
            .putString(KEY_IMG_PATH, user.profileImagePath)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOGGED_IN, false)

    fun getUserId(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, -1L)

    fun getName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "") ?: ""

    fun getEmail(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, "") ?: ""

    fun getPhone(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PHONE, "") ?: ""

    fun getRole(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, ROLE_BUYER) ?: ROLE_BUYER

    fun getBio(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BIO, "") ?: ""

    fun getLocation(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCATION, "") ?: ""

    fun getProfileImagePath(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_IMG_PATH, "") ?: ""

    fun isArtisan(context: Context) = getRole(context) == ROLE_ARTISAN
    fun isBuyer(context: Context)   = getRole(context) == ROLE_BUYER

    fun updateCachedProfile(context: Context, name: String, phone: String,
                             email: String = "", bio: String = "",
                             location: String = "", imgPath: String = "") {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
        if (email.isNotBlank()) prefs.putString(KEY_EMAIL, email)
        if (bio.isNotBlank()) prefs.putString(KEY_BIO, bio)
        if (location.isNotBlank()) prefs.putString(KEY_LOCATION, location)
        if (imgPath.isNotBlank()) prefs.putString(KEY_IMG_PATH, imgPath)
        prefs.apply()
    }
}
