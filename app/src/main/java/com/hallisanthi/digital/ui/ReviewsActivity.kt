package com.hallisanthi.digital.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.NotificationHelper
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.AppNotification
import com.hallisanthi.digital.models.Review
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReviewsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID   = "product_id"
        const val EXTRA_PRODUCT_NAME = "product_name"
        const val EXTRA_SELLER_PHONE = "seller_phone"
    }

    private var productId   = -1L
    private var productName = ""
    private var sellerPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reviews)

        productId   = intent.getLongExtra(EXTRA_PRODUCT_ID, -1L)
        productName = intent.getStringExtra(EXTRA_PRODUCT_NAME) ?: ""
        sellerPhone = intent.getStringExtra(EXTRA_SELLER_PHONE) ?: ""

        val toolbar = findViewById<MaterialToolbar>(R.id.reviewsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title    = "Reviews"
        supportActionBar?.subtitle = productName

        val recycler    = findViewById<RecyclerView>(R.id.reviewsRecycler)
        val emptyView   = findViewById<TextView>(R.id.reviewsEmpty)
        val avgStars    = findViewById<TextView>(R.id.reviewsAvgStars)
        val reviewCount = findViewById<TextView>(R.id.reviewsCount)
        val writeBtn    = findViewById<MaterialButton>(R.id.writeReviewBtn)

        recycler.layoutManager = LinearLayoutManager(this)

        val db = ProductDatabase.getDatabase(this)
        db.reviewDao().getForProductLive(productId).observe(this) { reviews ->
            if (reviews.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recycler.visibility  = View.GONE
                avgStars.text    = "—"
                reviewCount.text = "No reviews yet"
            } else {
                emptyView.visibility = View.GONE
                recycler.visibility  = View.VISIBLE
                val avg = reviews.map { it.stars }.average()
                avgStars.text    = String.format("%.1f ⭐", avg)
                reviewCount.text = "${reviews.size} review${if (reviews.size != 1) "s" else ""}"
                recycler.adapter = ReviewAdapter(reviews)
            }
        }

        val myPhone = UserSession.getPhone(this)
        if (UserSession.isBuyer(this) && myPhone != sellerPhone) {
            writeBtn.visibility = View.VISIBLE
            writeBtn.setOnClickListener { showWriteReviewDialog() }
        } else {
            writeBtn.visibility = View.GONE
        }
    }

    private fun showWriteReviewDialog() {
        val myId = UserSession.getUserId(this)
        // Load existing review on IO thread, then show dialog on UI thread
        lifecycleScope.launch {
            val existing = ProductDatabase.getDatabase(applicationContext)
                .reviewDao().getUserReviewForProduct(productId, myId)

            // Back on main thread to inflate UI
            runOnUiThread {
                val view = layoutInflater.inflate(R.layout.dialog_write_review, null)
                val rbar = view.findViewById<RatingBar>(R.id.dialogReviewRatingBar)
                val edit = view.findViewById<TextInputEditText>(R.id.dialogReviewComment)

                if (existing != null) {
                    rbar.rating = existing.stars.toFloat()
                    edit.setText(existing.comment)
                }

                AlertDialog.Builder(this@ReviewsActivity)
                    .setTitle(if (existing != null) "Edit Your Review" else "Write a Review")
                    .setView(view)
                    .setPositiveButton("Submit") { _, _ ->
                        val stars   = rbar.rating.toInt().coerceIn(1, 5)
                        val comment = edit.text.toString().trim()
                        if (comment.isBlank()) {
                            Toast.makeText(this@ReviewsActivity,
                                "Please write a comment", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        val review = Review(
                            id                 = existing?.id ?: 0,
                            productId          = productId,
                            reviewerId         = myId,
                            reviewerName       = UserSession.getName(this@ReviewsActivity),
                            stars              = stars,
                            comment            = comment,
                            isVerifiedPurchase = true,
                            createdAt          = existing?.createdAt ?: System.currentTimeMillis()
                        )
                        lifecycleScope.launch {
                            val db    = ProductDatabase.getDatabase(applicationContext)
                            db.reviewDao().insert(review)
                            val avg   = db.reviewDao().getAverageRating(productId) ?: 0f
                            val count = db.reviewDao().getReviewCount(productId)
                            db.productDao().updateRating(productId, avg, count)
                            val seller = db.userDao().getByPhone(sellerPhone)
                            if (seller != null) {
                                NotificationHelper.push(
                                    applicationContext, seller.id,
                                    AppNotification.TYPE_ORDER,
                                    "New review on $productName",
                                    "${UserSession.getName(this@ReviewsActivity)} gave $stars ⭐: $comment",
                                    productId
                                )
                            }
                            runOnUiThread {
                                Toast.makeText(this@ReviewsActivity,
                                    "Review submitted! ⭐", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    inner class ReviewAdapter(private val items: List<Review>) :
        RecyclerView.Adapter<ReviewAdapter.VH>() {

        private val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val avatar:   TextView = v.findViewById(R.id.reviewAvatar)
            val name:     TextView = v.findViewById(R.id.reviewerName)
            val stars:    TextView = v.findViewById(R.id.reviewStars)
            val comment:  TextView = v.findViewById(R.id.reviewComment)
            val date:     TextView = v.findViewById(R.id.reviewDate)
            val verified: TextView = v.findViewById(R.id.reviewVerified)
            val helpBtn:  TextView = v.findViewById(R.id.reviewHelpful)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val r = items[pos]
            h.avatar.text   = r.reviewerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            h.name.text     = r.reviewerName
            h.stars.text    = "⭐".repeat(r.stars)
            h.comment.text  = r.comment
            h.date.text     = sdf.format(Date(r.createdAt))
            h.verified.visibility = if (r.isVerifiedPurchase) View.VISIBLE else View.GONE
            h.helpBtn.text  = "👍 Helpful (${r.helpfulCount})"
            h.helpBtn.setOnClickListener {
                lifecycleScope.launch {
                    ProductDatabase.getDatabase(applicationContext).reviewDao().markHelpful(r.id)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
