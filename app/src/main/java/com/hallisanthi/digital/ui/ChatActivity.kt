package com.hallisanthi.digital.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.models.ChatMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OTHER_USER_ID   = "other_user_id"
        const val EXTRA_OTHER_USER_NAME = "other_user_name"
        const val EXTRA_PRODUCT_ID      = "product_id"
        const val EXTRA_PRODUCT_NAME    = "product_name"
        const val EXTRA_CONV_ID         = "conv_id"
    }

    private lateinit var recycler: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendBtn: ImageButton
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private var myId       = -1L
    private var otherId    = -1L
    private var productId  = -1L
    private var productName = ""
    private var convId     = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        myId        = UserSession.getUserId(this)
        otherId     = intent.getLongExtra(EXTRA_OTHER_USER_ID, -1L)
        productId   = intent.getLongExtra(EXTRA_PRODUCT_ID, -1L)
        productName = intent.getStringExtra(EXTRA_PRODUCT_NAME) ?: ""
        val otherName = intent.getStringExtra(EXTRA_OTHER_USER_NAME) ?: "User"
        convId      = intent.getStringExtra(EXTRA_CONV_ID)
                      ?: buildConvId(myId, otherId, productId)

        val toolbar = findViewById<MaterialToolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = otherName
        supportActionBar?.subtitle = if (productName.isNotBlank()) "Re: $productName" else ""

        recycler     = findViewById(R.id.chatRecycler)
        messageInput = findViewById(R.id.messageInput)
        sendBtn      = findViewById(R.id.sendBtn)

        adapter = ChatAdapter(messages, myId)
        recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recycler.adapter = adapter

        loadMessages()

        sendBtn.setOnClickListener { sendMessage() }
        messageInput.setOnEditorActionListener { _, _, _ -> sendMessage(); true }
    }

    private fun loadMessages() {
        val db = ProductDatabase.getDatabase(this)
        db.chatDao().getMessagesLive(convId).observe(this) { list ->
            messages.clear()
            messages.addAll(list)
            adapter.notifyDataSetChanged()
            if (messages.isNotEmpty()) recycler.scrollToPosition(messages.size - 1)
        }
        // Mark as read
        lifecycleScope.launch { db.chatDao().markConversationRead(convId, myId) }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isBlank()) return
        messageInput.setText("")

        val msg = ChatMessage(
            conversationId = convId,
            senderId       = myId,
            receiverId     = otherId,
            productId      = productId,
            productName    = productName,
            message        = text,
            timestamp      = System.currentTimeMillis()
        )
        lifecycleScope.launch {
            ProductDatabase.getDatabase(applicationContext).chatDao().insert(msg)
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    // ── Adapter ───────────────────────────────────────────────────────────────
    inner class ChatAdapter(
        private val items: List<ChatMessage>,
        private val myId: Long
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_SENT     = 0
        private val TYPE_RECEIVED = 1
        private val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())

        override fun getItemViewType(pos: Int) =
            if (items[pos].senderId == myId) TYPE_SENT else TYPE_RECEIVED

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout = if (viewType == TYPE_SENT) R.layout.item_chat_sent else R.layout.item_chat_received
            val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return MsgVH(v)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            val msg = items[pos]
            val vh  = holder as MsgVH
            vh.text.text = msg.message
            vh.time.text = sdf.format(Date(msg.timestamp))
        }

        override fun getItemCount() = items.size

        inner class MsgVH(v: View) : RecyclerView.ViewHolder(v) {
            val text: TextView = v.findViewById(R.id.msgText)
            val time: TextView = v.findViewById(R.id.msgTime)
        }
    }
}

fun buildConvId(userId1: Long, userId2: Long, productId: Long): String {
    val sorted = listOf(userId1, userId2).sorted()
    return "${sorted[0]}_${sorted[1]}_$productId"
}
