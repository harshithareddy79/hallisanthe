package com.hallisanthi.digital.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.CartManager
import com.hallisanthi.digital.data.NotificationHelper
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.WishlistManager
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.databinding.ActivityProductDetailBinding
import com.hallisanthi.digital.models.AppNotification
import com.hallisanthi.digital.models.Order
import com.hallisanthi.digital.models.Product
import com.hallisanthi.digital.models.RecentlyViewed
import com.hallisanthi.digital.viewmodel.ProductViewModel
import kotlinx.coroutines.launch
import java.io.File

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val viewModel: ProductViewModel by viewModels()
    private var currentProduct: Product? = null
    private var isWishlisted = false

    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.product_details_title)

        val productId = intent.getLongExtra(EXTRA_PRODUCT_ID, -1L)
        if (productId == -1L) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)
        viewModel.loadProduct(productId)

        viewModel.product.observe(this) { product ->
            showLoading(false)
            if (product == null) {
                Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                currentProduct = product
                isWishlisted   = WishlistManager.isWishlisted(this, product.id)
                invalidateOptionsMenu()
                displayProduct(product)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.product_detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_wishlist)
            ?.setIcon(if (isWishlisted) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_wishlist -> {
                currentProduct?.let {
                    isWishlisted = WishlistManager.toggle(this, it.id)
                    item.setIcon(if (isWishlisted) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
                    Toast.makeText(this,
                        if (isWishlisted) "Added to wishlist ❤️" else "Removed from wishlist",
                        Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_share -> { shareProduct(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayProduct(product: Product) {
        binding.productNameTextView.text = product.name

        // ── Track recently viewed ─────────────────────────────────────
        val rvUserId = UserSession.getUserId(this)
        if (rvUserId > 0) {
            lifecycleScope.launch {
                val db  = ProductDatabase.getDatabase(applicationContext)
                val dao = db.recentlyViewedDao()
                if (dao.exists(rvUserId, product.id) > 0) {
                    dao.updateTimestamp(rvUserId, product.id)
                } else {
                    dao.insert(RecentlyViewed(
                        userId           = rvUserId,
                        productId        = product.id,
                        productName      = product.name,
                        productPrice     = product.price,
                        productCategory  = product.category,
                        productImagePath = product.imagePath,
                        viewedAt         = System.currentTimeMillis()
                    ))
                    dao.trimOld(rvUserId)
                }
            }
        }

        binding.categoryTextView.text    = "${Product.getCategoryEmoji(product.category)} ${product.category}"
        binding.priceTextView.text       = product.getFormattedPrice()
        binding.sellerPhoneTextView.text = formatDisplayPhone(product.sellerPhone)

        // Description
        val descView = binding.root.findViewById<TextView?>(R.id.descriptionText)
        if (product.description.isNotBlank()) {
            descView?.text = product.description
            descView?.visibility = View.VISIBLE
        } else {
            descView?.visibility = View.GONE
        }

        // Stock status
        val stockView = binding.root.findViewById<View?>(R.id.stockStatusView)
        val stockText = binding.root.findViewById<TextView?>(R.id.stockStatusText)
        if (!product.isAvailable) {
            stockView?.visibility = View.VISIBLE
            stockText?.text = "⚠️ Currently Out of Stock"
            binding.contactSellerButton.isEnabled = false
            binding.contactSellerButton.alpha = 0.5f
        } else {
            stockView?.visibility = View.GONE
            binding.contactSellerButton.isEnabled = true
            binding.contactSellerButton.alpha = 1f
        }

        // Rating
        val ratingBar  = binding.root.findViewById<RatingBar?>(R.id.productRatingBar)
        val ratingText = binding.root.findViewById<TextView?>(R.id.ratingText)
        if (ratingBar != null) {
            ratingBar.rating = product.rating
            ratingText?.text = if (product.ratingCount > 0)
                "${String.format("%.1f", product.rating)} (${product.ratingCount} ratings)"
            else "No ratings yet"
        }

        binding.root.findViewById<View?>(R.id.rateButton)?.setOnClickListener {
            showRatingDialog(product)
        }

        // Reviews button
        binding.root.findViewById<View?>(R.id.viewReviewsButton)?.setOnClickListener {
            startActivity(
                Intent(this, ReviewsActivity::class.java).apply {
                    putExtra(ReviewsActivity.EXTRA_PRODUCT_ID,   product.id)
                    putExtra(ReviewsActivity.EXTRA_PRODUCT_NAME, product.name)
                    putExtra(ReviewsActivity.EXTRA_SELLER_PHONE, product.sellerPhone)
                }
            )
        }

        // Artisan info
        val artisanName = if (product.artisanName.isNotBlank()) product.artisanName else "View Artisan"
        binding.root.findViewById<TextView?>(R.id.artisanNameText)?.text = "🧑‍🎨 $artisanName"
        binding.root.findViewById<View?>(R.id.viewArtisanButton)?.setOnClickListener {
            startActivity(
                Intent(this, ArtisanProfileActivity::class.java)
                    .putExtra(ArtisanProfileActivity.EXTRA_ARTISAN_PHONE, product.sellerPhone)
                    .putExtra(ArtisanProfileActivity.EXTRA_ARTISAN_NAME, product.artisanName.ifBlank { product.sellerPhone })
            )
        }

        // Image
        if (product.imagePath.isNotBlank()) {
            val file = File(product.imagePath)
            if (file.exists()) {
                Glide.with(this).load(file)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(binding.productImageView)
            } else {
                binding.productImageView.setImageResource(R.drawable.ic_image_placeholder)
            }
        } else {
            binding.productImageView.setImageResource(R.drawable.ic_image_placeholder)
        }

        // Add to Cart (buyers only)
        val cartBtn = binding.root.findViewById<View?>(R.id.addToCartButton)
        if (UserSession.isBuyer(this)) {
            cartBtn?.visibility = View.VISIBLE
            cartBtn?.setOnClickListener {
                CartManager.addToCart(this, product.id, product.name, product.price)
                val count = CartManager.getItemCount(this)
                Toast.makeText(this, "Added to cart! 🛒 ($count items)", Toast.LENGTH_SHORT).show()
            }
        } else {
            cartBtn?.visibility = View.GONE
        }

        // Contact / Order button
        binding.contactSellerButton.setOnClickListener {
            if (product.isAvailable) showOrderConfirmation(product)
            else Toast.makeText(this, "This product is out of stock", Toast.LENGTH_SHORT).show()
        }

        // In-App Chat with Seller
        val chatBtn = binding.root.findViewById<View?>(R.id.chatSellerButton)
        chatBtn?.setOnClickListener {
            val myId = UserSession.getUserId(this)
            lifecycleScope.launch {
                val db     = ProductDatabase.getDatabase(applicationContext)
                val seller = db.userDao().getByPhone(product.sellerPhone.replace(Regex("[^0-9]"), ""))
                runOnUiThread {
                    when {
                        seller == null -> Toast.makeText(this@ProductDetailActivity,
                            "Seller not registered in-app. Use WhatsApp.", Toast.LENGTH_SHORT).show()
                        seller.id == myId -> Toast.makeText(this@ProductDetailActivity,
                            "This is your own product!", Toast.LENGTH_SHORT).show()
                        else -> {
                            val convId = buildConvId(myId, seller.id, product.id)
                            startActivity(Intent(this@ProductDetailActivity, ChatActivity::class.java).apply {
                                putExtra(ChatActivity.EXTRA_CONV_ID,         convId)
                                putExtra(ChatActivity.EXTRA_OTHER_USER_ID,   seller.id)
                                putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, seller.name)
                                putExtra(ChatActivity.EXTRA_PRODUCT_ID,      product.id)
                                putExtra(ChatActivity.EXTRA_PRODUCT_NAME,    product.name)
                            })
                        }
                    }
                }
            }
        }
    }

    private fun showOrderConfirmation(product: Product) {
        val phone = product.sellerPhone.replace(Regex("[^0-9]"), "")
        if (phone.length < 10) {
            Toast.makeText(this, "Invalid seller phone number", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Order")
            .setMessage(
                "You are about to contact the seller via WhatsApp.\n\n" +
                "📦 Product : ${product.name}\n" +
                "💰 Price   : ${product.getFormattedPrice()}\n" +
                "📞 Seller  : $phone\n\nContinue?"
            )
            .setPositiveButton("Order via WhatsApp") { _, _ ->
                openWhatsApp(product)
                lifecycleScope.launch {
                    val db     = ProductDatabase.getDatabase(applicationContext)
                    val seller = db.userDao().getByPhone(phone)
                    val myId   = UserSession.getUserId(this@ProductDetailActivity)
                    val order  = Order(
                        buyerId          = myId,
                        sellerId         = seller?.id ?: 0L,
                        productId        = product.id,
                        productName      = product.name,
                        productImagePath = product.imagePath,
                        price            = product.price,
                        category         = product.category,
                        artisanName      = product.artisanName.ifBlank { seller?.name ?: "" },
                        status           = Order.STATUS_CONTACTED
                    )
                    db.orderDao().insert(order)
                    NotificationHelper.push(
                        applicationContext, myId,
                        AppNotification.TYPE_ORDER,
                        "Order placed: ${product.name}",
                        "You contacted the seller. Track it in Order History.",
                        product.id
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRatingDialog(product: Product) {
        val view      = layoutInflater.inflate(R.layout.dialog_rate_product, null)
        val ratingBar = view.findViewById<RatingBar>(R.id.dialogRatingBar)
        AlertDialog.Builder(this)
            .setTitle("Rate this product")
            .setView(view)
            .setPositiveButton("Submit") { _, _ ->
                val stars = ratingBar.rating
                if (stars > 0) {
                    viewModel.submitRating(product.id, stars)
                    Toast.makeText(this, "Thanks for rating! ⭐", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareProduct() {
        val product = currentProduct ?: return
        val text = "🛍️ Check out this product on Halli-Santhe Digital!\n\n" +
                "📦 ${product.name}\n" +
                "💰 ${product.getFormattedPrice()}\n" +
                "🏷️ ${product.category}\n\n" +
                "Download Halli-Santhe Digital to explore more local artisan products!"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share via"))
    }

    private fun openWhatsApp(product: Product) {
        val rawPhone = product.sellerPhone.replace(Regex("[^0-9]"), "")
        if (rawPhone.length < 10) {
            Toast.makeText(this, "Seller phone number is invalid", Toast.LENGTH_LONG).show()
            return
        }
        val e164 = if (rawPhone.length == 10) "91$rawPhone" else rawPhone
        try {
            val message = getString(R.string.whatsapp_message_template, product.name, product.getFormattedPrice())
            val url     = "https://wa.me/$e164?text=${Uri.encode(message)}"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$rawPhone")))
            } catch (ex: Exception) {
                Toast.makeText(this, "Could not open WhatsApp or phone dialler", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatDisplayPhone(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return if (digits.length == 10) "+91 $digits" else phone
    }

    private fun buildConvId(userId1: Long, userId2: Long, productId: Long): String {
        val sorted = listOf(userId1, userId2).sorted()
        return "${sorted[0]}_${sorted[1]}_$productId"
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility   = if (show) View.VISIBLE else View.GONE
        binding.contentLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
