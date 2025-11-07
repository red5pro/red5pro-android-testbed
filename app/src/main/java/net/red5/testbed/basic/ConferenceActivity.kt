package net.red5.testbed.basic

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonElement
import net.red5.android.api.IRed5WebrtcClient
import net.red5.android.api.IRed5WebrtcClient.Red5EventListener
import net.red5.android.core.Red5Renderer
import net.red5.android.core.model.Red5ConferenceParticipant
import net.red5.testbed.R
import net.red5.testbed.SettingsActivity
import org.json.JSONObject
import kotlin.math.min

class ConferenceActivity : AppCompatActivity(), Red5EventListener {

    private var red5Client: IRed5WebrtcClient? = null
    var TAG = "ConferenceActivity"
    private lateinit var joinLayout: LinearLayout
    private lateinit var roomIdInput: EditText
    private lateinit var userNameInput: EditText
    private lateinit var joinButton: Button

    private lateinit var conferenceLayout: RelativeLayout
    private lateinit var statusText: TextView
    private lateinit var localVideoRendererForPreview: Red5Renderer
    private lateinit var localVideoRenderer: Red5Renderer
    private lateinit var localUserNameText: TextView
    private lateinit var participantsGridLayout: GridLayout
    private lateinit var leaveButton: Button
    private lateinit var switchCameraButton: Button
    private lateinit var toggleMicButton: Button
    private lateinit var toggleCameraButton: Button
    private lateinit var roomIdText: TextView
    private lateinit var participantCountText: TextView
    private lateinit var roomIdParticipantContainer: LinearLayout
    private lateinit var localPreviewBlackOverlay: RelativeLayout
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var pageIndicatorText: TextView
    private lateinit var paginationContainer: LinearLayout

    private var roomId: String = ""
    private var userName: String = ""
    private var isMicEnabled = true
    private var isCameraEnabled = true

    private val participants = mutableMapOf<String, ParticipantViewHolder>()
    private val participantsList = mutableListOf<String>() // Ordered list of participant IDs
    private lateinit var conferenceListener: IRed5WebrtcClient.ConferenceListener

    // Pagination
    private var currentPage = 0
    private val participantsPerPage = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conference)

        initViews()
        setupListeners()
        setupGridLayout()

        conferenceListener = object : IRed5WebrtcClient.ConferenceListener {
            override fun onJoinRoomSuccess(
                roomId: String,
                conferenceParticipants: ArrayList<Red5ConferenceParticipant>
            ) {
                updateStatus("Connected", "#4CAF50")
                roomIdParticipantContainer.visibility = View.VISIBLE
                roomIdText.text = "Room ID: $roomId"
            }

            override fun onJoinRoomFailed(statusCode: Int, message: String?) {
                Toast.makeText(
                    this@ConferenceActivity,
                    "Join room failed: $message",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onParticipantJoined(
                uid: String?,
                metaData: String,
                videoEnabled: Boolean,
                audioEnabled: Boolean,
                renderer: Red5Renderer
            ) {
                runOnUiThread {
                    addParticipant(uid!!, metaData, videoEnabled, audioEnabled, renderer)
                    updateParticipantCount()
                    refreshCurrentPage()
                }
            }

            override fun onParticipantLeft(uid: String) {
                runOnUiThread {
                    removeParticipant(uid)
                    updateParticipantCount()
                    refreshCurrentPage()
                }
            }

            override fun onParticipantMediaUpdate(
                uid: String?,
                videoEnabled: Boolean,
                audioEnabled: Boolean,
                timestamp: Long
            ) {
                runOnUiThread {
                    val holder = participants[uid]
                    holder?.let {
                        // Update mic icon
                        if (audioEnabled) {
                            it.micIcon.setImageResource(R.drawable.mic_on_icon)
                        } else {
                            it.micIcon.setImageResource(R.drawable.mic_off_icon)
                        }

                        // Update camera visibility
                        if (videoEnabled) {
                            it.cameraOffLayout.visibility = View.GONE
                        } else {
                            it.cameraOffLayout.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        initSdk()
    }

    private fun initViews() {
        joinLayout = findViewById(R.id.joinLayout)
        roomIdInput = findViewById(R.id.roomIdInput)
        roomIdInput.setText("testroom")
        roomIdText = findViewById(R.id.roomIdText)
        participantCountText = findViewById(R.id.participantCountText)
        roomIdParticipantContainer = findViewById(R.id.roomIdParticipantContainer)
        userNameInput = findViewById(R.id.userNameInput)
        userNameInput.setText("yunus_android")
        joinButton = findViewById(R.id.joinButton)

        conferenceLayout = findViewById(R.id.conferenceLayout)
        statusText = findViewById(R.id.statusText)
        localVideoRendererForPreview = findViewById(R.id.localVideoRendererForPreview)
        localVideoRenderer = findViewById(R.id.localVideoRenderer)
        localUserNameText = findViewById(R.id.localUserNameText)
        participantsGridLayout = findViewById(R.id.participantsGridLayout)
        leaveButton = findViewById(R.id.leaveButton)
        switchCameraButton = findViewById(R.id.lobbySwitchCameraButton)
        toggleCameraButton = findViewById(R.id.lobbyToggleCameraButton)
        toggleMicButton = findViewById(R.id.lobbyToggleMicButton)
        localPreviewBlackOverlay = findViewById(R.id.lobbyLocalPreviewBlackOverlay)
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        pageIndicatorText = findViewById(R.id.pageIndicatorText)
        paginationContainer = findViewById(R.id.paginationContainer)

        setupConferenceButtonListeners()
    }

    private fun setupListeners() {
        joinButton.setOnClickListener {
            val room = roomIdInput.text.toString().trim()
            val user = userNameInput.text.toString().trim()

            if (room.isEmpty()) {
                Toast.makeText(this, "Please enter room ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user.isEmpty()) {
                Toast.makeText(this, "Please enter user name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            roomId = room
            userName = user

            joinConference()
        }

        leaveButton.setOnClickListener {
            leaveConference()
        }

        prevPageButton.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                showCurrentPage()
            }
        }

        nextPageButton.setOnClickListener {
            val totalPages = getTotalPages()
            if (currentPage < totalPages - 1) {
                currentPage++
                showCurrentPage()
            }
        }
    }

    private fun setupGridLayout() {
        participantsGridLayout.columnCount = 2
        participantsGridLayout.rowCount = 2
    }

    private fun addParticipant(
        userId: String,
        metaData: String,
        videoEnabled: Boolean,
        audioEnabled: Boolean,
        renderer: Red5Renderer
    ) {
        // Inflate the participant item layout
        val participantView = layoutInflater.inflate(
            R.layout.item_participant,
            participantsGridLayout,
            false
        )

        // Get references to views
        val userIdText: TextView = participantView.findViewById(R.id.participantUserId)
        val micIcon: ImageView = participantView.findViewById(R.id.participantMicIcon)
        val cameraOffLayout: RelativeLayout = participantView.findViewById(R.id.participantCameraOffLayout)
        val videoRendererPlaceholder: Red5Renderer = participantView.findViewById(R.id.participantVideoRenderer)

        // Set user ID
        userIdText.text = userId

        // Remove renderer from any previous parent
        (renderer.parent as? android.view.ViewGroup)?.removeView(renderer)

        // Replace placeholder with actual renderer
        val container = videoRendererPlaceholder.parent as? android.view.ViewGroup
        if (container != null) {
            val index = container.indexOfChild(videoRendererPlaceholder)
            container.removeViewAt(index)

            val layoutParams = videoRendererPlaceholder.layoutParams
            renderer.layoutParams = layoutParams
            container.addView(renderer, index)
        }

        // Set initial mic icon state
        if (audioEnabled) {
            micIcon.setImageResource(R.drawable.mic_on_icon)
        } else {
            micIcon.setImageResource(R.drawable.mic_off_icon)
        }

        // Set initial camera state
        if (videoEnabled) {
            cameraOffLayout.visibility = View.GONE
        } else {
            cameraOffLayout.visibility = View.VISIBLE
        }

        // Calculate tile size for 2 columns
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val margins = (8 * 2 * 2) * displayMetrics.density.toInt()
        val tileWidth = (screenWidth - margins) / 2
        val tileHeight = tileWidth

        // Set GridLayout params for the participant view
        val params = GridLayout.LayoutParams()
        params.width = tileWidth
        params.height = tileHeight
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
        params.setMargins(8, 8, 8, 8)
        participantView.layoutParams = params

        // Store the holder
        val holder = ParticipantViewHolder(
            participantView,
            renderer,
            userIdText,
            micIcon,
            cameraOffLayout
        )
        participants[userId] = holder
        participantsList.add(userId)

        Log.d(TAG, "Added participant: $userId")
    }

    private fun removeParticipant(userId: String) {
        val holder = participants[userId]
        if (holder != null) {
            // Remove renderer from its parent
            (holder.renderer.parent as? android.view.ViewGroup)?.removeView(holder.renderer)

            // Remove from grid if currently displayed
            participantsGridLayout.removeView(holder.itemView)

            // Remove from data structures
            participants.remove(userId)
            participantsList.remove(userId)

            // Adjust current page if needed
            val totalPages = getTotalPages()
            if (totalPages > 0 && currentPage >= totalPages) {
                currentPage = totalPages - 1
            }

            Log.d(TAG, "Removed participant: $userId")
        } else {
            Log.w(TAG, "Participant not found: $userId")
        }
    }

    private fun getTotalPages(): Int {
        val totalParticipants = participantsList.size
        return if (totalParticipants == 0) 1 else ((totalParticipants + participantsPerPage - 1) / participantsPerPage)
    }

    private fun showCurrentPage() {
        // Clear the grid
        participantsGridLayout.removeAllViews()

        // Calculate range for current page
        val start = currentPage * participantsPerPage
        val end = min(start + participantsPerPage, participantsList.size)

        // Add participants for current page
        for (i in start until end) {
            val userId = participantsList[i]
            val holder = participants[userId]
            holder?.let {
                participantsGridLayout.addView(it.itemView)
            }
        }

        // Update pagination controls
        updatePaginationControls()

        Log.d(TAG, "Showing page ${currentPage + 1} of ${getTotalPages()}")
    }

    private fun refreshCurrentPage() {
        // If we're beyond the last page, go to last page
        val totalPages = getTotalPages()
        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1
        }
        showCurrentPage()
    }

    private fun updatePaginationControls() {
        val totalPages = getTotalPages()
        val totalParticipants = participantsList.size

        // Show/hide pagination container
        if (totalParticipants > participantsPerPage) {
            paginationContainer.visibility = View.VISIBLE
        } else {
            paginationContainer.visibility = View.GONE
        }

        // Update page indicator
        pageIndicatorText.text = "Page ${currentPage + 1} of $totalPages"

        // Enable/disable buttons
        prevPageButton.isEnabled = currentPage > 0
        nextPageButton.isEnabled = currentPage < totalPages - 1

        // Update button appearance
        prevPageButton.alpha = if (prevPageButton.isEnabled) 1.0f else 0.5f
        nextPageButton.alpha = if (nextPageButton.isEnabled) 1.0f else 0.5f
    }

    private fun updateParticipantCount() {
        participantCountText.text = "Total Participants: ${participants.size + 1}"
    }

    fun initSdk() {
        red5Client = IRed5WebrtcClient.builder()
            .setActivity(this)
            .setDataChannelListener(object : IRed5WebrtcClient.DataChannelListener {
                override fun onDataChannelOpen() {}

                override fun onDataChannelClosed() {}

                override fun onDataChannelMessage(message: String?) {
                    Log.i(TAG, "Data channel message received: $message")
                }

                override fun onDataChannelMessage(data: ByteArray?) {}

                override fun onDataChannelError(error: String?) {}
            })
            .setStreamManagerHost(SettingsActivity.getStreamManagerHost(this))
            .setLicenseKey(SettingsActivity.getLicenseKey(this))
            .setServerIp(SettingsActivity.getStandaloneServerIp(this))
            .setStreamName(SettingsActivity.getStreamName(this))
            .setUserName(SettingsActivity.getUserName(this))
            .setPassword(SettingsActivity.getPassword(this))
            .setToken("")
            .setVideoEnabled(true)
            .setAudioEnabled(true)
            .setVideoWidth(1280)
            .setVideoHeight(720)
            .setVideoFps(30)
            .setVideoBitrate(1500)
            .setVideoSource(IRed5WebrtcClient.StreamSource.FRONT_CAMERA)
            .setVideoRenderer(localVideoRendererForPreview)
            .setEventListener(this)
            .setConferenceListener(conferenceListener)
            .build()

        red5Client?.startPreview()
    }

    private fun joinConference() {
        updateStatus("Joining...", "#FFA500")
        showConferenceLayout()
        red5Client?.setVideoRenderer(localVideoRenderer)

        val userId = userName + "_" + getRandomString(6)
        val metaData = JSONObject()
        metaData.put("name", userName)

        red5Client?.join(roomId, userId, "", "publisher", metaData.toString())
    }

    private fun leaveConference() {
        updateStatus("Disconnected", "#F44336")
        red5Client?.release()

        runOnUiThread {
            conferenceLayout.visibility = View.GONE
            joinLayout.visibility = View.VISIBLE

            // Clear all participant views
            participantsGridLayout.removeAllViews()
            participants.clear()
            participantsList.clear()
            currentPage = 0
        }

        finish()
        Toast.makeText(this, "Left the conference", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus(status: String, color: String) {
        runOnUiThread {
            statusText.text = "● $status"
            statusText.setTextColor(android.graphics.Color.parseColor(color))
        }
    }

    private fun showConferenceLayout() {
        runOnUiThread {
            joinLayout.visibility = View.GONE
            conferenceLayout.visibility = View.VISIBLE
            localUserNameText.text = userName

            switchCameraButton = findViewById(R.id.switchCameraButton)
            toggleCameraButton = findViewById(R.id.toggleCameraButton)
            toggleMicButton = findViewById(R.id.toggleMicButton)
            localPreviewBlackOverlay = findViewById(R.id.localPreviewBlackOverlay)
            setupConferenceButtonListeners()
        }
    }

    private fun setupConferenceButtonListeners() {
        switchCameraButton.setOnClickListener {
            red5Client?.switchCamera()
        }

        toggleMicButton.setOnClickListener {
            if (red5Client != null) {
                isMicEnabled = !isMicEnabled
                red5Client?.toggleSendAudio(isMicEnabled)

                toggleMicButton.text = if (isMicEnabled) "Mic On" else "Mic Off"
                toggleMicButton.backgroundTintList = resources.getColorStateList(
                    if (isMicEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark,
                    theme
                )
            }
        }

        toggleCameraButton.setOnClickListener {
            if (red5Client != null) {
                isCameraEnabled = !isCameraEnabled
                red5Client?.toggleSendVideo(isCameraEnabled)

                if (isCameraEnabled) {
                    localPreviewBlackOverlay.visibility = View.GONE
                } else {
                    localPreviewBlackOverlay.visibility = View.VISIBLE
                }

                toggleCameraButton.text = if (isCameraEnabled) "Cam On" else "Cam Off"
                toggleCameraButton.backgroundTintList = resources.getColorStateList(
                    if (isCameraEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark,
                    theme
                )
            }
        }
    }

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

    override fun onLicenseValidated(validated: Boolean, message: String?) {
        if (validated) {
            red5Client?.startPreview()
        }
    }

    override fun onError(error: String?) {
        Log.e("ConferenceActivity", "Error: $error")
    }

    override fun onChatConnected() {}
    override fun onChatDisconnected() {}
    override fun onChatMessageReceived(channel: String?, message: JsonElement?) {}
    override fun onChatSendSuccess(channel: String?, timetoken: Long?) {}
    override fun onChatSendError(channel: String?, errorMessage: String?) {}

    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        red5Client?.release()
        super.onBackPressed()
    }

    data class ParticipantViewHolder(
        val itemView: View,
        val renderer: Red5Renderer,
        val userIdText: TextView,
        val micIcon: ImageView,
        val cameraOffLayout: RelativeLayout
    )
}