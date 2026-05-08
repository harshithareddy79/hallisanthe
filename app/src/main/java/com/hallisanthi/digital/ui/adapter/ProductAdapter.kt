package com.hallisanthi.digital.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.WishlistManager
import com.hallisanthi.digital.databinding.ItemProductBinding
import com.hallisanthi.digital.models.Product
import java.io.File

class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onWishlistToggle: ((Product, Boolean) -> Unit)? = null
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setStartDelay((position * 50L).coerceAtMost(300L))
            .setDuration(300)
            .start()
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.productNameTextView.text = product.name
            binding.categoryTextView.text =
                "${Product.getCategoryEmoji(product.category)} ${product.category}"
            binding.priceTextView.text = product.getFormattedPrice()

            // Out of Stock badge (Feature 10)
            val stockBadge = binding.root.findViewById<TextView?>(R.id.outOfStockBadge)
            stockBadge?.visibility = if (!product.isAvailable) View.VISIBLE else View.GONE

            // Wishlist heart button (Feature 1)
            val wishBtn = binding.root.findViewById<ImageButton?>(R.id.wishlistButton)
            if (wishBtn != null) {
                val isWishlisted = WishlistManager.isWishlisted(binding.root.context, product.id)
                wishBtn.setImageResource(
                    if (isWishlisted) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
                wishBtn.setOnClickListener {
                    val added = WishlistManager.toggle(binding.root.context, product.id)
                    wishBtn.setImageResource(
                        if (added) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                    )
                    onWishlistToggle?.invoke(product, added)
                }
            }

            if (product.imagePath.isNotBlank()) {
                Glide.with(binding.root.context)
                    .load(File(product.imagePath))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(binding.productImageView)
            } else {
                binding.productImageView.setImageResource(R.drawable.ic_image_placeholder)
            }

            binding.root.setOnClickListener { onProductClick(product) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
        override fun areContentsTheSame(old: Product, new: Product) = old == new
    }
}
