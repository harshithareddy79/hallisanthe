package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.RecentlyViewed
import kotlinx.coroutines.launch
import java.io.File

class RecentlyViewedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recently_viewed)

        val toolbar   = findViewById<MaterialToolbar>(R.id.rvToolbar)
        val recycler  = findViewById<RecyclerView>(R.id.rvRecycler)
        val emptyView = findViewById<TextView>(R.id.rvEmpty)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recently Viewed"

        recycler.layoutManager = GridLayoutManager(this, 2)

        val uid = UserSession.getUserId(this)
        ProductDatabase.getDatabase(this).recentlyViewedDao()
            .getForUserLive(uid).observe(this) { items ->
                if (items.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    recycler.visibility  = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    recycler.visibility  = View.VISIBLE
                    recycler.adapter = RVAdapter(items)
                }
            }
    }

    inner class RVAdapter(private val items: List<RecentlyViewed>) :
        RecyclerView.Adapter<RVAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val image: ShapeableImageView = v.findViewById(R.id.rvItemImage)
            val name:  TextView           = v.findViewById(R.id.rvItemName)
            val price: TextView           = v.findViewById(R.id.rvItemPrice)
            val cat:   TextView           = v.findViewById(R.id.rvItemCategory)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_recently_viewed, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]
            h.name.text  = item.productName
            h.price.text = "₹${String.format("%.0f", item.productPrice)}"
            h.cat.text   = item.productCategory

            if (item.productImagePath.isNotBlank() && File(item.productImagePath).exists()) {
                Glide.with(this@RecentlyViewedActivity)
                    .load(File(item.productImagePath))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(h.image)
            } else {
                h.image.setImageResource(R.drawable.ic_image_placeholder)
            }

            h.itemView.setOnClickListener {
                lifecycleScope.launch {
                    val product = ProductDatabase.getDatabase(applicationContext)
                        .productDao().getProductById(item.productId)
                    if (product != null) {
                        runOnUiThread {
                            startActivity(
                                Intent(this@RecentlyViewedActivity, ProductDetailActivity::class.java)
                                    .putExtra("product_id", product.id)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
