package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.Order
import kotlinx.coroutines.launch

class SalesDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_dashboard)

        val toolbar = findViewById<MaterialToolbar>(R.id.dashToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sales Dashboard"

        loadStats()
    }

    private fun loadStats() {
        val uid = UserSession.getUserId(this)
        lifecycleScope.launch {
            val db       = ProductDatabase.getDatabase(applicationContext)
            val orders   = db.orderDao().getSellerOrders(uid)
            val products = db.productDao().getProductsBySellerPhone(UserSession.getPhone(this@SalesDashboardActivity))

            val totalSales      = orders.size
            val delivered       = orders.count { it.status == Order.STATUS_DELIVERED }
            val pending         = orders.count { it.status == Order.STATUS_CONTACTED || it.status == Order.STATUS_ORDERED }
            val cancelled       = orders.count { it.status == Order.STATUS_CANCELLED }
            val totalRevenue    = orders.filter { it.status != Order.STATUS_CANCELLED }
                                        .sumOf { it.price * it.quantity }
            val activeListings  = products.count { it.isActive }
            val wishlistTotal   = products.sumOf { it.wishlistCount }
            val avgRating       = products.filter { it.ratingCount > 0 }
                                         .map { it.rating }.average().let {
                                             if (it.isNaN()) 0.0 else it }

            // Category breakdown
            val byCategory = orders.groupBy { it.category }
                                   .mapValues { (_, v) -> v.size }
                                   .entries.sortedByDescending { it.value }

            runOnUiThread {
                bind(R.id.statTotalSales,    totalSales.toString())
                bind(R.id.statDelivered,     delivered.toString())
                bind(R.id.statPending,       pending.toString())
                bind(R.id.statCancelled,     cancelled.toString())
                bind(R.id.statRevenue,       "₹${String.format("%.0f", totalRevenue)}")
                bind(R.id.statListings,      activeListings.toString())
                bind(R.id.statWishlists,     wishlistTotal.toString())
                bind(R.id.statAvgRating,     String.format("%.1f ⭐", avgRating))

                // Top category
                val topCat = byCategory.firstOrNull()
                bind(R.id.statTopCategory, topCat?.let { "${it.key} (${it.value})" } ?: "—")

                // Category breakdown text
                val breakdown = byCategory.joinToString("\n") { (cat, cnt) ->
                    "  ${com.hallisanthi.digital.models.Product.getCategoryEmoji(cat)} $cat: $cnt order${if(cnt!=1) "s" else ""}"
                }
                bind(R.id.statCategoryBreakdown, breakdown.ifBlank { "No orders yet" })

                // Quick action: view all orders
                findViewById<android.view.View>(R.id.dashViewOrdersBtn)?.setOnClickListener {
                    startActivity(Intent(this@SalesDashboardActivity, OrderHistoryActivity::class.java))
                }
            }
        }
    }

    private fun bind(id: Int, text: String) {
        findViewById<TextView?>(id)?.text = text
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
