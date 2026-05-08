package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.hallisanthi.digital.R
import com.hallisanthi.digital.databinding.ActivityBuyerBrowseBinding
import com.hallisanthi.digital.models.Product
import com.hallisanthi.digital.ui.adapter.ProductAdapter
import com.hallisanthi.digital.viewmodel.ProductViewModel

class BuyerBrowseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyerBrowseBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuyerBrowseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.browse_products_title)

        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        observeProducts()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            onProductClick = { product ->
                startActivity(
                    Intent(this, ProductDetailActivity::class.java)
                        .putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
                )
            },
            onWishlistToggle = { _, added ->
                android.widget.Toast.makeText(
                    this,
                    if (added) "Added to wishlist ❤️" else "Removed from wishlist",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
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
        // Voice search mic
        binding.voiceMicBtn.setOnClickListener {
            com.hallisanthi.digital.data.VoiceSearchHelper.startListening(this)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    private fun observeProducts() {
        viewModel.products.observe(this) { products ->
            adapter.submitList(products)
            val empty = products.isNullOrEmpty()
            binding.emptyStateLayout.visibility = if (empty) View.VISIBLE else View.GONE
            binding.productsRecyclerView.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == com.hallisanthi.digital.data.VoiceSearchHelper.REQUEST_CODE
            && resultCode == android.app.Activity.RESULT_OK) {
            val result = com.hallisanthi.digital.data.VoiceSearchHelper.extractResult(data)
            if (!result.isNullOrBlank()) {
                binding.searchView.setQuery(result, true)
                android.widget.Toast.makeText(this, "🎙 Heard: $result", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
