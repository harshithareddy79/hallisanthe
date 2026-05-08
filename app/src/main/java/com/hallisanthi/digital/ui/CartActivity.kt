package com.hallisanthi.digital.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.CartManager

class CartActivity : AppCompatActivity() {

    private lateinit var adapter: CartAdapter
    private lateinit var totalText: TextView
    private lateinit var checkoutButton: MaterialButton
    private lateinit var emptyView: View
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Cart 🛒"

        totalText     = findViewById(R.id.totalText)
        checkoutButton = findViewById(R.id.checkoutButton)
        emptyView     = findViewById(R.id.emptyView)
        recycler      = findViewById(R.id.cartRecyclerView)

        adapter = CartAdapter(
            onRemove = { item ->
                CartManager.removeFromCart(this, item.productId)
                refreshCart()
            },
            onQuantityChange = { item, qty ->
                CartManager.updateQuantity(this, item.productId, qty)
                refreshCart()
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        refreshCart()

        checkoutButton.setOnClickListener { showCheckoutDialog() }
    }

    private fun refreshCart() {
        val cart = CartManager.getCart(this)
        adapter.submitList(cart)
        val isEmpty = cart.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recycler.visibility  = if (isEmpty) View.GONE else View.VISIBLE
        val total = CartManager.getTotal(this)
        totalText.text = "Total: ₹${String.format("%.0f", total)}"
        checkoutButton.isEnabled = !isEmpty
    }

    private fun showCheckoutDialog() {
        val cart  = CartManager.getCart(this)
        val total = CartManager.getTotal(this)
        val summary = cart.joinToString("\n") { "• ${it.productName} x${it.quantity} = ₹${String.format("%.0f", it.price * it.quantity)}" }
        AlertDialog.Builder(this)
            .setTitle("Order Summary 📋")
            .setMessage("$summary\n\n──────────\nTotal: ₹${String.format("%.0f", total)}\n\nContact each seller on WhatsApp to confirm your order.")
            .setPositiveButton("Place Order") { _, _ ->
                CartManager.clearCart(this)
                Toast.makeText(this, "Order placed! Contact sellers on WhatsApp 🎉", Toast.LENGTH_LONG).show()
                finish()
            }
            .setNegativeButton("Continue Shopping", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}

class CartAdapter(
    private val onRemove: (CartManager.CartItem) -> Unit,
    private val onQuantityChange: (CartManager.CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    private var items = listOf<CartManager.CartItem>()
    fun submitList(list: List<CartManager.CartItem>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
    override fun getItemCount() = items.size

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name     = v.findViewById<TextView>(R.id.cartItemName)
        val price    = v.findViewById<TextView>(R.id.cartItemPrice)
        val qty      = v.findViewById<TextView>(R.id.cartItemQty)
        val minusBtn = v.findViewById<ImageButton>(R.id.minusButton)
        val plusBtn  = v.findViewById<ImageButton>(R.id.plusButton)
        val removeBtn= v.findViewById<ImageButton>(R.id.removeButton)

        fun bind(item: CartManager.CartItem) {
            name.text  = item.productName
            price.text = "₹${String.format("%.0f", item.price * item.quantity)}"
            qty.text   = item.quantity.toString()
            minusBtn.setOnClickListener { onQuantityChange(item, item.quantity - 1) }
            plusBtn.setOnClickListener  { onQuantityChange(item, item.quantity + 1) }
            removeBtn.setOnClickListener { onRemove(item) }
        }
    }
}
