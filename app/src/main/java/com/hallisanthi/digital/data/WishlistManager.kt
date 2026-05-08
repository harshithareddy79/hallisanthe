package com.hallisanthi.digital.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Feature 1: Wishlist/Favorites — persisted in SharedPreferences as a Set of product ID strings.
 */
object WishlistManager {

    private const val PREFS_NAME = "wishlist_prefs"
    private const val KEY_WISHLIST = "wishlist_ids"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getWishlist(context: Context): Set<Long> {
        return prefs(context).getStringSet(KEY_WISHLIST, emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    fun isWishlisted(context: Context, productId: Long): Boolean =
        getWishlist(context).contains(productId)

    fun getCount(context: Context): Int =
        getWishlist(context).size

    fun toggle(context: Context, productId: Long): Boolean {
        val current = getWishlist(context).toMutableSet()
        val added = if (current.contains(productId)) {
            current.remove(productId)
            false
        } else {
            current.add(productId)
            true
        }
        prefs(context).edit()
            .putStringSet(KEY_WISHLIST, current.map { it.toString() }.toSet())
            .apply()
        return added
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_WISHLIST).apply()
    }
}
