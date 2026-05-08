package com.hallisanthi.digital.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.ui.adapter.ProductAdapter
import com.hallisanthi.digital.viewmodel.ProductViewModel

/**
 * Feature 12: Artisan Profile Page
 * Shows artisan name, phone, product count, and all their listings.
 */
class ArtisanProfileActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()

    companion object {
        const val EXTRA_ARTISAN_PHONE = "artisan_phone"
        const val EXTRA_ARTISAN_NAME  = "artisan_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artisan_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val phone = intent.getStringExtra(EXTRA_ARTISAN_PHONE) ?: run { finish(); return }
        val name  = intent.getStringExtra(EXTRA_ARTISAN_NAME)  ?: phone

        supportActionBar?.title = name

        val nameView     = findViewById<TextView>(R.id.artisanName)
        val phoneView    = findViewById<TextView>(R.id.artisanPhone)
        val productCount = findViewById<TextView>(R.id.productCount)
        val emptyView    = findViewById<View>(R.id.emptyView)
        val recycler     = findViewById<RecyclerView>(R.id.artisanProductsRecycler)

        nameView.text  = "🧑‍🎨 $name"
        phoneView.text = "📞 $phone"

        val adapter = ProductAdapter(
            onProductClick = { product ->
                startActivity(
                    Intent(this, ProductDetailActivity::class.java)
                        .putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
                )
            }
        )
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = adapter

        viewModel.loadArtisanListings(phone)
        viewModel.artisanListings.observe(this) { products ->
            adapter.submitList(products)
            productCount.text = "${products.size} product${if (products.size != 1) "s" else ""}"
            emptyView.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        }

        // WhatsApp button
        findViewById<View>(R.id.contactArtisanButton).setOnClickListener {
            try {
                val url = "https://wa.me/${phone.replace(Regex("[^0-9+]"), "")}?text=${
                    Uri.encode("Namaskara! 🙏 I found your shop on Halli-Santhe Digital.")}"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
