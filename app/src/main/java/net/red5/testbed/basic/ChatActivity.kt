package net.red5.testbed.basic

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.red5.android.api.IRed5WebrtcClient
import net.red5.android.api.IRed5WebrtcClient.Red5EventListener
import net.red5.testbed.R
import net.red5.testbed.SettingsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatActivity : AppCompatActivity(), Red5EventListener {

    private var red5Client: IRed5WebrtcClient? = null

    private lateinit var joinLayout: LinearLayout
    private lateinit var channelNameInput: EditText
    private lateinit var userNameInput: EditText
    private lateinit var joinButton: Button

    private lateinit var chatLayout: RelativeLayout
    private lateinit var statusText: TextView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button

    private var channelName: String = ""
    private var userName: String = ""
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private var chatUserId = UUID.randomUUID().toString();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initViews()
        setupListeners()
        setupRecyclerView()
    }

    private fun initViews() {
        joinLayout = findViewById(R.id.joinLayout)
        channelNameInput = findViewById(R.id.channelNameInput)
        userNameInput = findViewById(R.id.userNameInput)
        joinButton = findViewById(R.id.joinButton)

        chatLayout = findViewById(R.id.chatLayout)
        statusText = findViewById(R.id.statusText)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun setupListeners() {
        joinButton.setOnClickListener {
            val channel = channelNameInput.text.toString().trim()
            val user = userNameInput.text.toString().trim()

            if (channel.isEmpty()) {
                Toast.makeText(this, "Please enter channel name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user.isEmpty()) {
                Toast.makeText(this, "Please enter user name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            channelName = channel
            userName = user

            initializeRed5Client()
        }

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = chatAdapter
    }

    private fun initializeRed5Client() {
        try {
            updateStatus("Connecting...", "#FFA500")

            red5Client = IRed5WebrtcClient.builder()
                .setActivity(this)
                .setLicenseKey(SettingsActivity.getLicenseKey(this))
                // A unique chat user id.
                // If auth is enabled for chat, You must send this userId to your application backend for token generation.
                .setChatUserId(chatUserId)

                // If chat authentication is enabled, set the token received from your backend server.
                // Use red5 backend sdks to generate chat tokens: https://github.com/red5pro/red5-bcs-node
                // Chat tokens can be also updated after client is initialized.
                //.setChatToken("")
                .setPubnubPublishKey(SettingsActivity.getPubnubPublishKey(this))
                .setPubnubSubscribeKey(SettingsActivity.getPubnubSubscribeKey(this))
                .setEventListener(this)
                .build()

        } catch (e: Exception) {
            Log.e("ChatActivity", "Failed to initialize Red5 client: ${e.message}")
            Toast.makeText(this, "Failed to initialize chat: ${e.message}", Toast.LENGTH_LONG).show()
            updateStatus("Connection Failed", "#F44336")
        }
    }

    private fun sendMessage() {
        val messageText = messageInput.text.toString().trim()

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        if (red5Client == null) {
            Toast.makeText(this, "Chat not connected", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonMessage = JsonObject().apply {
            addProperty("text", messageText)
            addProperty("userName", userName)
            addProperty("timestamp", System.currentTimeMillis())
        }

        val metadata = mapOf(
            "sender" to userName,
            "channelName" to channelName
        )

        red5Client?.sendChatJsonMessage(channelName, jsonMessage, metadata)

        messageInput.text.clear()
    }

    private fun updateStatus(status: String, color: String) {
        runOnUiThread {
            statusText.text = "● $status"
            statusText.setTextColor(android.graphics.Color.parseColor(color))
        }
    }

    private fun showChatLayout() {
        runOnUiThread {
            joinLayout.visibility = View.GONE
            chatLayout.visibility = View.VISIBLE
        }
    }

    private fun addMessage(userName: String, message: String, timestamp: Long) {
        runOnUiThread {
            messages.add(ChatMessage(userName, message, timestamp))
            chatAdapter.notifyItemInserted(messages.size - 1)
            messagesRecyclerView.scrollToPosition(messages.size - 1)
        }
    }

    override fun onChatConnected() {
        Log.d("ChatActivity", "Chat connected")
        updateStatus("Connected", "#4CAF50")
        showChatLayout()

        runOnUiThread {
            Toast.makeText(this, "Connected to $channelName", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onChatDisconnected() {
        Log.d("ChatActivity", "Chat disconnected")
        updateStatus("Disconnected", "#F44336")

        runOnUiThread {
            Toast.makeText(this, "Disconnected from chat", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onChatMessageReceived(channel: String?, message: JsonElement?) {
        Log.d("ChatActivity", "Message received on channel: $channel")

        if (message != null && message.isJsonObject) {
            val jsonObject = message.asJsonObject

            val text = jsonObject.get("text")?.asString ?: ""
            val sender = jsonObject.get("userName")?.asString ?: "Unknown"
            val timestamp = jsonObject.get("timestamp")?.asLong ?: System.currentTimeMillis()

            Log.d("ChatActivity", "Message from $sender: $text")

            addMessage(sender, text, timestamp)
        }
    }

    override fun onChatSendSuccess(channel: String?, timetoken: Long?) {
        Log.d("ChatActivity", "Message sent successfully on channel: $channel with timetoken: $timetoken")

        runOnUiThread {
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onChatSendError(channel: String?, errorMessage: String?) {
        Log.e("ChatActivity", "Failed to send message on channel: $channel. Error: $errorMessage")

        runOnUiThread {
            Toast.makeText(this, "Failed to send message: $errorMessage", Toast.LENGTH_LONG).show()
        }
    }

    override fun onLicenseValidated(validated: Boolean, message: String?) {
        Log.d("ChatActivity", "License validated: $validated, message: $message")
        // Subscribe to the channel
        red5Client?.subscribeChatChannel(channelName)

        // If channel auth is enabled, first get token from your backend, set it and then subscribe.

    }

    override fun onError(error: String?) {
        Log.e("ChatActivity", "Error: $error")
    }


    /**
     * Sends an HTTP request to your backend server to generate a chat token.
     *
     * The token should be generated server-side using Red5 Backend SDKs
     * to ensure secure authentication with Chat.
     *
     * Backend implementation examples and SDKs:
     * - Node.js: https://github.com/red5pro/red5-bcs-node
     * - Java: https://github.com/red5pro/red5-bcs-java
     * - Go: https://github.com/red5pro/red5-bcs-go
     *
     * @param userId The unique identifier for the user requesting the token
     * @param channelId The chat channel identifier the user wants to join
     * @return The generated chat token for chat authentication
     */
    fun getChatToken(userId: String, channelId: String) {
        //  Implement HTTP request to your backend server
    }

    // You can dynamically set(update) chat token if required.
    fun setChatToken(chatToken: String){
        red5Client?.setChatToken(chatToken)
    }

    override fun onChatError(error: String?) {

        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }

    }

    // Unused WebRTC event listeners
    override fun onPublishStarted() {}
    override fun onPublishStopped() {}
    override fun onPublishFailed(error: String?) {}
    override fun onSubscribeStarted() {}
    override fun onSubscribeStopped() {}
    override fun onSubscribeFailed(error: String?) {}
    override fun onIceConnectionStateChanged(state: IRed5WebrtcClient.IceConnectionState?) {}
    override fun onConnectionStateChanged(state: IRed5WebrtcClient.PeerConnectionState?) {}
    override fun onPreviewStarted() {}
    override fun onPreviewStopped() {}


    override fun onDestroy() {
        super.onDestroy()
        red5Client?.release()
    }

    data class ChatMessage(
        val userName: String,
        val text: String,
        val timestamp: Long
    )

    inner class ChatAdapter(private val messages: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

        inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userNameTextView: TextView = itemView.findViewById(R.id.messageUserName)
            val messageTextView: TextView = itemView.findViewById(R.id.messageText)
            val timeTextView: TextView = itemView.findViewById(R.id.messageTime)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ChatViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val message = messages[position]
            holder.userNameTextView.text = message.userName
            holder.messageTextView.text = message.text
            holder.timeTextView.text = formatTimestamp(message.timestamp)
        }

        override fun getItemCount(): Int = messages.size

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}