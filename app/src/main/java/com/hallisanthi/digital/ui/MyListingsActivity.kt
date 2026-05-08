package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.hallisanthi.digital.R
import com.hallisanthi.digital.models.Product
import com.hallisanthi.digital.viewmodel.ProductViewModel
import java.io.File

class MyListingsActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: MyListingsAdapter

    companion object {
        const val EXTRA_PHONE = "artisan_phone"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_listings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Listings"

        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        if (phone.isBlank()) { finish(); return }

        val recyclerView = findViewById<RecyclerView>(R.id.listingsRecyclerView)
        val emptyView = findViewById<View>(R.id.emptyView)

        adapter = MyListingsAdapter(
            onEdit = { product ->
                val intent = Intent(this, ArtisanUploadActivity::class.java).apply {
                    putExtra(ArtisanUploadActivity.EXTRA_EDIT_PRODUCT_ID, product.id)
                }
                startActivity(intent)
            },
            onDelete = { product -> confirmDelete(product) },
            onToggleAvailability = { product, available ->
                viewModel.setAvailability(product.id, available)
                val msg = if (available) "Marked as Available ✅" else "Marked as Out of Stock ⚠️"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.loadArtisanListings(phone)
        viewModel.artisanListings.observe(this) { products ->
            adapter.submitList(products)
            emptyView.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.deleteResult.observe(this) { success ->
            if (success) Toast.makeText(this, "Product deleted ✅", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete \"${product.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteProduct(product) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

class MyListingsAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
    private val onToggleAvailability: (Product, Boolean) -> Unit
) : RecyclerView.Adapter<MyListingsAdapter.VH>() {

    private var items = listOf<Product>()

    fun submitList(list: List<Product>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_listing, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image = itemView.findViewById<ShapeableImageView>(R.id.listingImage)
        private val name = itemView.findViewById<TextView>(R.id.listingName)
        private val price = itemView.findViewById<TextView>(R.id.listingPrice)
        private val category = itemView.findViewById<TextView>(R.id.listingCategory)
        private val availSwitch = itemView.findViewById<SwitchMaterial>(R.id.availabilitySwitch)
        private val editBtn = itemView.findViewById<MaterialButton>(R.id.editButton)
        private val deleteBtn = itemView.findViewById<MaterialButton>(R.id.deleteButton)

        fun bind(product: Product) {
            name.text = product.name
            price.text = product.getFormattedPrice()
            category.text = "${Product.getCategoryEmoji(product.category)} ${product.category}"

            if (product.imagePath.isNotBlank()) {
                Glide.with(itemView.context)
                    .load(File(product.imagePath))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(image)
            } else {
                image.setImageResource(R.drawable.ic_image_placeholder)
            }

            availSwitch.isChecked = product.isAvailable
            availSwitch.text = if (product.isAvailable) "Available" else "Out of Stock"
            availSwitch.setOnCheckedChangeListener { _, checked ->
                availSwitch.text = if (checked) "Available" else "Out of Stock"
                onToggleAvailability(product, checked)
            }

            editBtn.setOnClickListener { onEdit(product) }
            deleteBtn.setOnClickListener { onDelete(product) }
        }
    }
}
