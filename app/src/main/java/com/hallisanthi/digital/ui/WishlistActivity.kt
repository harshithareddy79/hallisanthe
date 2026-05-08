package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.CartManager
import com.hallisanthi.digital.data.WishlistManager
import com.hallisanthi.digital.ui.adapter.ProductAdapter
import com.hallisanthi.digital.viewmodel.ProductViewModel

class WishlistActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Wishlist ❤️"

        val recycler  = findViewById<RecyclerView>(R.id.wishlistRecyclerView)
        val emptyView = findViewById<View>(R.id.emptyView)

        // Declare adapter as a member so the toggle callback can reference it safely
        adapter = ProductAdapter(
            onProductClick = { product ->
                startActivity(
                    Intent(this, ProductDetailActivity::class.java)
                        .putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
                )
            },
            onWishlistToggle = { _, added ->
                if (!added) {
                    Toast.makeText(this, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                    // Re-filter the current list to remove un-wishlisted item
                    refreshWishlist()
                }
            }
        )

        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = adapter

        // Observe once - live updates handled via refreshWishlist()
        viewModel.products.observe(this) { _ -> refreshWishlist() }

        emptyView.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.wishlist_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_wishlist_to_cart -> {
                addAllWishlistToCart()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addAllWishlistToCart() {
        val wishlistIds = WishlistManager.getWishlist(this)
        if (wishlistIds.isEmpty()) {
            Toast.makeText(this, "Your wishlist is empty.", Toast.LENGTH_SHORT).show()
            return
        }

        val allProducts = viewModel.products.value ?: emptyList()
        val wishlistedProducts = allProducts.filter { it.id in wishlistIds }
        if (wishlistedProducts.isEmpty()) {
            Toast.makeText(this, "No available wishlist items to add.", Toast.LENGTH_SHORT).show()
            return
        }

        wishlistedProducts.forEach { product ->
            CartManager.addToCart(this, product.id, product.name, product.price)
        }

        Toast.makeText(this,
            "Added ${wishlistedProducts.size} wishlist item(s) to cart 🛒",
            Toast.LENGTH_SHORT).show()
    }

    private fun refreshWishlist() {
        val wishlistIds = WishlistManager.getWishlist(this)
        val emptyView   = findViewById<View>(R.id.emptyView) ?: return
        // Get the latest list from ViewModel
        val allProducts = viewModel.products.value ?: emptyList()
        val wishlisted  = allProducts.filter { it.id in wishlistIds }
        adapter.submitList(wishlisted)
        emptyView.visibility = if (wishlisted.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
