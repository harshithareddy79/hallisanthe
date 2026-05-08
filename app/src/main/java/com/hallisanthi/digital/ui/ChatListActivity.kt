package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.ChatMessage
import com.hallisanthi.digital.models.Conversation
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatListActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private var myId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        myId = UserSession.getUserId(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.chatListToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Chats"

        recycler  = findViewById(R.id.chatListRecycler)
        emptyView = findViewById(R.id.chatListEmpty)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        loadConversations()
    }

    override fun onResume() { super.onResume(); loadConversations() }

    private fun loadConversations() {
        lifecycleScope.launch {
            val db        = ProductDatabase.getDatabase(applicationContext)
            val lastMsgs  = db.chatDao().getLastMessagePerConversation(myId)

            val convList = lastMsgs.map { msg ->
                val isMe      = msg.senderId == myId
                val otherId   = if (isMe) msg.receiverId else msg.senderId
                val otherUser = db.userDao().getById(otherId)
                val unread    = db.chatDao().getUnreadInConversation(msg.conversationId, myId)
                Conversation(
                    conversationId  = msg.conversationId,
                    otherUserId     = otherId,
                    otherUserName   = otherUser?.name ?: "Unknown",
                    productId       = msg.productId,
                    productName     = msg.productName,
                    lastMessage     = msg.message,
                    lastMessageTime = msg.timestamp,
                    unreadCount     = unread,
                    isSentByMe      = isMe
                )
            }

            runOnUiThread {
                if (convList.isEmpty()) {
                    emptyView.visibility  = View.VISIBLE
                    recycler.visibility   = View.GONE
                } else {
                    emptyView.visibility  = View.GONE
                    recycler.visibility   = View.VISIBLE
                    recycler.adapter = ConvAdapter(convList)
                }
            }
        }
    }

    inner class ConvAdapter(private val items: List<Conversation>) :
        RecyclerView.Adapter<ConvAdapter.VH>() {

        private val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val avatar:   TextView = v.findViewById(R.id.convAvatar)
            val name:     TextView = v.findViewById(R.id.convName)
            val product:  TextView = v.findViewById(R.id.convProduct)
            val lastMsg:  TextView = v.findViewById(R.id.convLastMsg)
            val time:     TextView = v.findViewById(R.id.convTime)
            val badge:    TextView = v.findViewById(R.id.convUnreadBadge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, pos: Int) {
            val c = items[pos]
            holder.avatar.text   = c.otherUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.name.text     = c.otherUserName
            holder.product.text  = "Re: ${c.productName}"
            holder.lastMsg.text  = if (c.isSentByMe) "You: ${c.lastMessage}" else c.lastMessage
            holder.time.text     = sdf.format(Date(c.lastMessageTime))

            if (c.unreadCount > 0) {
                holder.badge.visibility = View.VISIBLE
                holder.badge.text = c.unreadCount.toString()
            } else {
                holder.badge.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                startActivity(Intent(this@ChatListActivity, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CONV_ID, c.conversationId)
                    putExtra(ChatActivity.EXTRA_OTHER_USER_ID, c.otherUserId)
                    putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, c.otherUserName)
                    putExtra(ChatActivity.EXTRA_PRODUCT_ID, c.productId)
                    putExtra(ChatActivity.EXTRA_PRODUCT_NAME, c.productName)
                })
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
