package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.AppNotification
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationCenterActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val toolbar = findViewById<MaterialToolbar>(R.id.notifToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications"

        recycler  = findViewById(R.id.notifRecycler)
        emptyView = findViewById(R.id.notifEmpty)
        recycler.layoutManager = LinearLayoutManager(this)

        val uid = UserSession.getUserId(this)
        val db  = ProductDatabase.getDatabase(this)

        db.notificationDao().getForUserLive(uid).observe(this) { list ->
            if (list.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recycler.visibility  = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recycler.visibility  = View.VISIBLE
                recycler.adapter = NotifAdapter(list)
            }
        }

        // Mark all read when screen opens
        lifecycleScope.launch { db.notificationDao().markAllRead(uid) }
    }

    inner class NotifAdapter(private val items: List<AppNotification>) :
        RecyclerView.Adapter<NotifAdapter.VH>() {

        private val sdf = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon:  TextView = v.findViewById(R.id.notifIcon)
            val title: TextView = v.findViewById(R.id.notifTitle)
            val body:  TextView = v.findViewById(R.id.notifBody)
            val time:  TextView = v.findViewById(R.id.notifTime)
            val dot:   View     = v.findViewById(R.id.notifUnreadDot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val n = items[pos]
            h.icon.text  = AppNotification.typeEmoji(n.type)
            h.title.text = n.title
            h.body.text  = n.body
            h.time.text  = sdf.format(Date(n.timestamp))
            h.dot.visibility = if (!n.isRead) View.VISIBLE else View.GONE

            h.itemView.setOnClickListener {
                lifecycleScope.launch {
                    ProductDatabase.getDatabase(applicationContext).notificationDao().markRead(n.id)
                }
                when (n.type) {
                    AppNotification.TYPE_CHAT  -> startActivity(Intent(this@NotificationCenterActivity, ChatListActivity::class.java))
                    AppNotification.TYPE_ORDER -> startActivity(Intent(this@NotificationCenterActivity, OrderHistoryActivity::class.java))
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
