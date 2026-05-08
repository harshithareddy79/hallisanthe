package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.NotificationHelper
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.AppNotification
import com.hallisanthi.digital.models.Order
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var chipGroup: ChipGroup
    private var allOrders = listOf<Order>()
    private var currentFilter = "ALL"
    private val isBuyer get() = UserSession.isBuyer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)

        val toolbar = findViewById<MaterialToolbar>(R.id.orderToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isBuyer) "My Orders" else "Sales History"

        recycler  = findViewById(R.id.orderRecycler)
        emptyView = findViewById(R.id.orderEmpty)
        chipGroup = findViewById(R.id.orderFilterChips)

        recycler.layoutManager = LinearLayoutManager(this)

        setupFilterChips()
        observeOrders()
    }

    private fun setupFilterChips() {
        val filters = listOf("ALL", Order.STATUS_CONTACTED, Order.STATUS_ORDERED,
                             Order.STATUS_DELIVERED, Order.STATUS_CANCELLED)
        filters.forEach { status ->
            val chip = Chip(this).apply {
                text = if (status == "ALL") "All" else "${Order.statusEmoji(status)} ${Order.statusLabel(status)}"
                isCheckable = true
                isChecked = status == "ALL"
                setOnClickListener { currentFilter = status; applyFilter() }
            }
            chipGroup.addView(chip)
        }
    }

    private fun observeOrders() {
        val uid = UserSession.getUserId(this)
        val db  = ProductDatabase.getDatabase(this)
        val liveData = if (isBuyer) db.orderDao().getBuyerOrdersLive(uid)
                       else db.orderDao().getSellerOrdersLive(uid)
        liveData.observe(this) { orders ->
            allOrders = orders
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == "ALL") allOrders
                       else allOrders.filter { it.status == currentFilter }
        if (filtered.isEmpty()) {
            emptyView.visibility  = View.VISIBLE
            recycler.visibility   = View.GONE
        } else {
            emptyView.visibility  = View.GONE
            recycler.visibility   = View.VISIBLE
            recycler.adapter = OrderAdapter(filtered)
        }
    }

    // ── Adapter ─────────────────────────────────────────────────────────────
    inner class OrderAdapter(private val items: List<Order>) :
        RecyclerView.Adapter<OrderAdapter.VH>() {

        private val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val emoji:    TextView = v.findViewById(R.id.orderStatusEmoji)
            val name:     TextView = v.findViewById(R.id.orderProductName)
            val artisan:  TextView = v.findViewById(R.id.orderArtisan)
            val price:    TextView = v.findViewById(R.id.orderPrice)
            val status:   TextView = v.findViewById(R.id.orderStatus)
            val date:     TextView = v.findViewById(R.id.orderDate)
            val updateBtn:View     = v.findViewById(R.id.orderUpdateBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val o = items[pos]
            h.emoji.text   = Order.statusEmoji(o.status)
            h.name.text    = o.productName
            h.artisan.text = if (isBuyer) "From: ${o.artisanName}" else "Qty: ${o.quantity}"
            h.price.text   = o.getFormattedPrice()
            h.status.text  = Order.statusLabel(o.status)
            h.date.text    = sdf.format(Date(o.createdAt))

            // Status color
            val color = when (o.status) {
                Order.STATUS_DELIVERED  -> getColor(R.color.success_green)
                Order.STATUS_CANCELLED  -> getColor(R.color.error_red)
                Order.STATUS_ORDERED    -> getColor(R.color.info_blue)
                else                   -> getColor(R.color.warning_yellow)
            }
            h.status.setTextColor(color)

            // Sellers can update status; buyers see it read-only
            if (!isBuyer && o.status != Order.STATUS_DELIVERED && o.status != Order.STATUS_CANCELLED) {
                h.updateBtn.visibility = View.VISIBLE
                h.updateBtn.setOnClickListener { showStatusDialog(o) }
            } else {
                h.updateBtn.visibility = View.GONE
            }
        }
    }

    private fun showStatusDialog(order: Order) {
        val options = arrayOf(
            "📞 Contacted", "📦 Mark as Ordered", "✅ Mark as Delivered", "❌ Cancel Order"
        )
        val statuses = arrayOf(Order.STATUS_CONTACTED, Order.STATUS_ORDERED,
                               Order.STATUS_DELIVERED, Order.STATUS_CANCELLED)
        AlertDialog.Builder(this)
            .setTitle("Update Order Status")
            .setItems(options) { _, which ->
                val newStatus = statuses[which]
                lifecycleScope.launch {
                    val db = ProductDatabase.getDatabase(applicationContext)
                    db.orderDao().update(order.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
                    // Notify buyer
                    NotificationHelper.push(applicationContext, order.buyerId,
                        AppNotification.TYPE_ORDER,
                        "Order update: ${order.productName}",
                        "Status changed to ${Order.statusLabel(newStatus)} ${Order.statusEmoji(newStatus)}",
                        order.id)
                }
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
