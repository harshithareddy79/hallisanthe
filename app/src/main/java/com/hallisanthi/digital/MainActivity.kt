package com.hallisanthi.digital

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.hallisanthi.digital.data.CartManager
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.WishlistManager
import com.hallisanthi.digital.ui.OrderHistoryActivity
import com.hallisanthi.digital.ui.NotificationCenterActivity
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.databinding.ActivityMainBinding
import com.hallisanthi.digital.models.Product
import com.hallisanthi.digital.ui.*
import com.hallisanthi.digital.ui.adapter.ProductAdapter
import com.hallisanthi.digital.viewmodel.ProductViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter
    private var notificationObserver: androidx.lifecycle.Observer<Int>? = null
    private var cartActionView: View? = null
    private var wishlistActionView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If somehow we land here without a session, redirect to login
        if (!UserSession.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Personalised greeting
        val name = UserSession.getName(this)
        if (name.isNotBlank()) {
            val greetView = binding.root.findViewById<TextView?>(R.id.greetingText)
            greetView?.text = "Namaskara, $name! ${if (UserSession.isArtisan(this)) "🧑‍🎨" else "🛒"}"
            greetView?.visibility = View.VISIBLE
        }

        // Role-based FAB
        if (UserSession.isArtisan(this)) {
            binding.fab.setOnClickListener {
                startActivity(Intent(this, ArtisanUploadActivity::class.java))
            }
        } else {
            binding.fab.setImageResource(R.drawable.ic_heart_outline)
            binding.fab.setOnClickListener {
                startActivity(Intent(this, WishlistActivity::class.java))
            }
        }

        // Role-based button bar
        binding.uploadProductButton.visibility = if (UserSession.isArtisan(this)) View.VISIBLE else View.GONE
        binding.browseButton.setOnClickListener {
            startActivity(Intent(this, BuyerBrowseActivity::class.java))
        }

        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        setupButtons()
        observeProducts()

        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.accent)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.action_my_listings)?.isVisible = UserSession.isArtisan(this)

        cartActionView = menu.findItem(R.id.action_cart)?.actionView
        cartActionView?.setOnClickListener {
            onOptionsItemSelected(menu.findItem(R.id.action_cart))
        }

        wishlistActionView = menu.findItem(R.id.action_wishlist)?.actionView
        wishlistActionView?.setOnClickListener {
            onOptionsItemSelected(menu.findItem(R.id.action_wishlist))
        }

        updateCartBadge()
        updateWishlistBadge()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        updateCartBadge()
        updateWishlistBadge()
        val uid = UserSession.getUserId(this)
        if (uid > 0) {
            // Remove old observer if exists
            notificationObserver?.let {
                try {
                    ProductDatabase.getDatabase(this).notificationDao()
                        .getUnreadCountLive(uid).removeObserver(it)
                } catch (e: Exception) {
                    // Ignore if database fails
                }
            }
            // Create new observer
            notificationObserver = androidx.lifecycle.Observer { count ->
                menu.findItem(R.id.action_notifications)?.let { bellItem ->
                    bellItem.title = if (count > 0) "🔔 ($count)" else "🔔"
                }
            }
            // Add new observer
            try {
                ProductDatabase.getDatabase(this).notificationDao()
                    .getUnreadCountLive(uid).observe(this, notificationObserver!!)
            } catch (e: Exception) {
                // Ignore if database fails
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_sample_data -> { addSampleData(); true }
            R.id.action_cart -> {
                startActivity(Intent(this, CartActivity::class.java))
                true
            }
            R.id.action_sort -> { showSortDialog(); true }
            R.id.action_my_listings -> {
                if (UserSession.isArtisan(this)) {
                    startActivity(Intent(this, MyListingsActivity::class.java)
                        .putExtra(MyListingsActivity.EXTRA_PHONE, UserSession.getPhone(this)))
                } else {
                    Toast.makeText(this, "Only sellers can view listings", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_wishlist -> { startActivity(Intent(this, WishlistActivity::class.java)); true }
            R.id.action_profile  -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
            R.id.action_notifications -> { startActivity(Intent(this, NotificationCenterActivity::class.java)); true }
            R.id.action_orders -> { startActivity(Intent(this, OrderHistoryActivity::class.java)); true }
            R.id.action_logout -> {
                AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log Out") { _, _ ->
                        UserSession.logout(this)
                        startActivity(Intent(this, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateCartBadge() {
        val count = CartManager.getItemCount(this)
        val badgeTextView = cartActionView?.findViewById<TextView>(R.id.cartBadgeText)
        badgeTextView?.let {
            it.text = if (count > 0) count.toString() else ""
            it.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun updateWishlistBadge() {
        val count = WishlistManager.getCount(this)
        val badgeTextView = wishlistActionView?.findViewById<TextView>(R.id.wishlistBadgeText)
        badgeTextView?.let {
            it.text = if (count > 0) count.toString() else ""
            it.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun showSortDialog() {
        val options = arrayOf("Newest First", "Price: Low to High", "Price: High to Low")
        val sorts   = arrayOf(Product.SORT_NEWEST, Product.SORT_PRICE_ASC, Product.SORT_PRICE_DESC)
        AlertDialog.Builder(this)
            .setTitle("Sort By")
            .setItems(options) { _, which ->
                viewModel.setSortOrder(sorts[which])
                Toast.makeText(this, "Sorted: ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun addSampleData() {
        val sampleProducts = listOf(
            Product(name = "Channapatna Wooden Toy", price = 350.0, category = Product.CATEGORY_WOOD,
                sellerPhone = "9876543210", artisanName = "Ravi Kumar",
                description = "Handcrafted lacquer toy from Channapatna, Karnataka"),
            Product(name = "Handwoven Silk Dupatta", price = 1200.0, category = Product.CATEGORY_TEXTILES,
                sellerPhone = "9876543211", artisanName = "Lakshmi Devi",
                description = "Traditional Mysore silk dupatta with gold border"),
            Product(name = "Clay Ganesha Idol", price = 550.0, category = Product.CATEGORY_POTTERY,
                sellerPhone = "9876543212", artisanName = "Venkatesh Murthi",
                description = "Eco-friendly clay idol for festivals"),
            Product(name = "Bamboo Basket", price = 280.0, category = Product.CATEGORY_BAMBOO,
                sellerPhone = "9876543213", artisanName = "Suresh Bai",
                description = "Durable hand-woven bamboo storage basket"),
            Product(name = "Silver Anklet", price = 890.0, category = Product.CATEGORY_JEWELRY,
                sellerPhone = "9876543214", artisanName = "Meena Kumari",
                description = "Traditional 92.5 silver anklet with bells"),
            Product(name = "Tanjore Painting", price = 2500.0, category = Product.CATEGORY_PAINTINGS,
                sellerPhone = "9876543215", artisanName = "Gopal Rao",
                description = "Original Tanjore painting with gold foil work")
        )
        sampleProducts.forEach { viewModel.insertProduct(it) }
        Toast.makeText(this, "Sample products added! 🎉", Toast.LENGTH_SHORT).show()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            onProductClick = { product ->
                startActivity(Intent(this, ProductDetailActivity::class.java)
                    .putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id))
            },
            onWishlistToggle = { _, added ->
                Toast.makeText(this,
                    if (added) "Added to wishlist ❤️" else "Removed from wishlist",
                    Toast.LENGTH_SHORT).show()
            }
        )
        binding.productsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.productsRecyclerView.adapter = adapter
    }

    private fun setupCategoryChips() {
        binding.categoryChipGroup.removeAllViews()
        val allCategories = listOf(Product.CATEGORY_ALL) + Product.getAllCategories()
        allCategories.forEach { category ->
            val chip = Chip(this).apply {
                text = if (category == Product.CATEGORY_ALL) "All"
                else "${Product.getCategoryEmoji(category)} $category"
                isCheckable = true
                isChecked = category == Product.CATEGORY_ALL
                setChipBackgroundColorResource(R.color.chip_selector)
                setTextColor(getColorStateList(R.color.chip_text_selector))
            }
            chip.setOnClickListener {
                viewModel.setCategory(category)
                for (i in 0 until binding.categoryChipGroup.childCount) {
                    (binding.categoryChipGroup.getChildAt(i) as? Chip)?.isChecked = false
                }
                chip.isChecked = true
            }
            binding.categoryChipGroup.addView(chip)
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query ?: ""); return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: ""); return true
            }
        })
    }

    private fun setupButtons() {
        binding.uploadProductButton.setOnClickListener {
            startActivity(Intent(this, ArtisanUploadActivity::class.java))
        }
    }

    private fun observeProducts() {
        viewModel.products.observe(this) { products ->
            adapter.submitList(products)
            val isEmpty = products.isNullOrEmpty()
            binding.productsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.emptyStateLayout.visibility     = if (isEmpty) View.VISIBLE else View.GONE
        }
    }
}
