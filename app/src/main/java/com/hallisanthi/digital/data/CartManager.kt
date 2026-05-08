package com.hallisanthi.digital.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Feature 2: Cart — persisted in SharedPreferences as JSON
 * Stores: productId → quantity
 */
object CartManager {

    private const val PREFS = "cart_prefs"
    private const val KEY_CART = "cart_items"

    data class CartItem(val productId: Long, val productName: String, val price: Double, var quantity: Int)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getCart(ctx: Context): MutableList<CartItem> {
        val json = prefs(ctx).getString(KEY_CART, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CartItem(obj.getLong("id"), obj.getString("name"), obj.getDouble("price"), obj.getInt("qty"))
            }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    fun addToCart(ctx: Context, productId: Long, productName: String, price: Double) {
        val cart = getCart(ctx)
        val existing = cart.find { it.productId == productId }
        if (existing != null) existing.quantity++
        else cart.add(CartItem(productId, productName, price, 1))
        saveCart(ctx, cart)
    }

    fun removeFromCart(ctx: Context, productId: Long) {
        val cart = getCart(ctx).filter { it.productId != productId }.toMutableList()
        saveCart(ctx, cart)
    }

    fun updateQuantity(ctx: Context, productId: Long, qty: Int) {
        val cart = getCart(ctx)
        if (qty <= 0) { removeFromCart(ctx, productId); return }
        cart.find { it.productId == productId }?.quantity = qty
        saveCart(ctx, cart)
    }

    fun clearCart(ctx: Context) = prefs(ctx).edit().remove(KEY_CART).apply()

    fun getTotal(ctx: Context): Double = getCart(ctx).sumOf { it.price * it.quantity }

    fun getItemCount(ctx: Context): Int = getCart(ctx).sumOf { it.quantity }

    fun isInCart(ctx: Context, productId: Long): Boolean =
        getCart(ctx).any { it.productId == productId }

    private fun saveCart(ctx: Context, cart: List<CartItem>) {
        val arr = JSONArray()
        cart.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.productId)
                put("name", item.productName)
                put("price", item.price)
                put("qty", item.quantity)
            })
        }
        prefs(ctx).edit().putString(KEY_CART, arr.toString()).apply()
    }
}
