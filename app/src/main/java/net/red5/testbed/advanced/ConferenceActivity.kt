package net.red5.testbed.advanced

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.JsonElement
import net.red5.android.api.IRed5WebrtcClient
import net.red5.android.core.Red5Renderer
import net.red5.android.core.model.RTCStats
import net.red5.android.core.model.Red5ConferenceParticipant
import net.red5.testbed.R
import net.red5.testbed.SettingsActivity
import net.red5.testbed.utility.ConnectionForegroundService
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.get
import kotlin.math.min

class ConferenceActivity : AppCompatActivity(), IRed5WebrtcClient.Red5EventListener {

    private var red5Client: IRed5WebrtcClient? = null
    var TAG = "ConferenceActivity"
    private  val PERMISSION_REQUEST_CODE = 1001

    private val REQUIRED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )


    private lateinit var joinLayout: LinearLayout
    private lateinit var roomIdInput: EditText
    private lateinit var userNameInput: EditText
    private lateinit var joinButton: Button
    private lateinit var roleRadioGroup: RadioGroup
    private lateinit var publisherRadioButton: RadioButton
    private lateinit var subscriberRadioButton: RadioButton
    private lateinit var lobbyPreviewContainer: RelativeLayout
    private lateinit var lobbyButtonContainer: LinearLayout
    private lateinit var subscriberLeaveButton: Button
    private lateinit var participantsContainer: LinearLayout
    private lateinit var subscriberLocalContainer: RelativeLayout
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
    private lateinit var localVideoSection: RelativeLayout
    private lateinit var conferenceButtonContainer: LinearLayout
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var pageIndicatorText: TextView
    private lateinit var paginationContainer: LinearLayout
    private lateinit var subscriberLocalParticipantUserId : TextView

    private var roomId: String = ""
    private var userName: String = ""
    private var userRole: String = "publisher"
    private var isMicEnabled = true
    private var isCameraEnabled = true
    private var isConnected = false;

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
                ConnectionForegroundService.startConference(this@ConferenceActivity)
                updateStatus("Connected", "#4CAF50")
                updateParticipantCount()
                isConnected = true
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
                uid: String,
                role: String,
                metaData: String,
                videoEnabled: Boolean,
                audioEnabled: Boolean,
                renderer: Red5Renderer?
            ) {
                runOnUiThread {
                    addParticipant(uid, role, metaData, videoEnabled, audioEnabled, renderer)
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
                            it.micIcon?.setImageResource(R.drawable.mic_on_icon)
                        } else {
                            it.micIcon?.setImageResource(R.drawable.mic_off_icon)
                        }

                        // Update camera visibility
                        if (videoEnabled) {
                            it.cameraOffLayout?.visibility = View.GONE
                        } else {
                            it.cameraOffLayout?.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        checkPermissions()
    }


    private fun checkPermissions() {
        if (hasAllPermissions()) {
            initSdk()
        } else {
            ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasAllPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun initViews() {
        joinLayout = findViewById(R.id.joinLayout)
        roomIdInput = findViewById(R.id.roomIdInput)
        roomIdText = findViewById(R.id.roomIdText)
        participantCountText = findViewById(R.id.participantCountText)
        roomIdParticipantContainer = findViewById(R.id.roomIdParticipantContainer)
        userNameInput = findViewById(R.id.userNameInput)
        joinButton = findViewById(R.id.joinButton)
        subscriberLeaveButton = findViewById(R.id.subscriberLeaveButton)
        subscriberLocalContainer = findViewById(R.id.subscriberLocalContainer)
        subscriberLocalParticipantUserId = findViewById(R.id.subscriberLocalParticipantUserId)

        participantsContainer = findViewById(R.id.participantsContainer)
        // Role selection views
        roleRadioGroup = findViewById(R.id.roleRadioGroup)
        publisherRadioButton = findViewById(R.id.publisherRadioButton)
        subscriberRadioButton = findViewById(R.id.subscriberRadioButton)
        lobbyPreviewContainer = findViewById(R.id.lobbyPreviewContainer)
        lobbyButtonContainer = findViewById(R.id.lobbyButtonContainer)

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
        localVideoSection = findViewById(R.id.localVideoSection)
        conferenceButtonContainer = findViewById(R.id.conferenceButtonContainer)
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        pageIndicatorText = findViewById(R.id.pageIndicatorText)
        paginationContainer = findViewById(R.id.paginationContainer)

        setupConferenceButtonListeners()
    }

    private fun setupListeners() {
        // Role selection listener
        roleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.publisherRadioButton -> {
                    userRole = "publisher"
                    showPublisherControls()
                }
                R.id.subscriberRadioButton -> {
                    userRole = "subscriber"
                    hidePublisherControls()
                }
            }
        }

        subscriberLeaveButton.setOnClickListener {
            leaveConference()
        }

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

    private fun showPublisherControls() {
        lobbyPreviewContainer.visibility = View.VISIBLE
        lobbyButtonContainer.visibility = View.VISIBLE

        // Start preview if SDK is initialized
        red5Client?.startPreview()
    }

    private fun hidePublisherControls() {
        lobbyPreviewContainer.visibility = View.GONE
        lobbyButtonContainer.visibility = View.GONE
        red5Client?.stopPreview()
    }

    private fun setupGridLayout() {
        participantsGridLayout.columnCount = 2
        participantsGridLayout.rowCount = 2
    }

    private fun addParticipant(
        userId: String,
        role: String,
        metaData: String,
        videoEnabled: Boolean,
        audioEnabled: Boolean,
        renderer: Red5Renderer?
    ) {
        val participantView: View
        val holder: ParticipantViewHolder

        if (role == "subscriber") {
            // Use simple subscriber layout (no video renderer)
            participantView = layoutInflater.inflate(
                R.layout.item_subscriber_participant,
                participantsGridLayout,
                false
            )

            val userIdText: TextView = participantView.findViewById(R.id.subscriberParticipantUserId)
            userIdText.text = userId

            // Calculate tile size for 2 columns
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val margins = (8 * 2 * 2) * displayMetrics.density.toInt()
            val baseTileWidth = (screenWidth - margins) / 2
            val tileWidth = (baseTileWidth * 0.8).toInt()
            val tileHeight = tileWidth

            // Set GridLayout params
            val params = GridLayout.LayoutParams()
            params.width = tileWidth
            params.height = tileHeight
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
            params.setMargins(8, 8, 8, 8)
            participantView.layoutParams = params

            // Store the holder (no renderer for subscribers)
            holder = ParticipantViewHolder(
                participantView,
                null, // No renderer for subscribers
                userIdText,
                null, // No mic icon
                null  // No camera layout
            )

        } else {
            // Use regular participant layout with video renderer
            participantView = layoutInflater.inflate(
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
            (renderer?.parent as? ViewGroup)?.removeView(renderer)

            // Replace placeholder with actual renderer
            val container = videoRendererPlaceholder.parent as? ViewGroup
            if (container != null) {
                val index = container.indexOfChild(videoRendererPlaceholder)
                container.removeViewAt(index)

                val layoutParams = videoRendererPlaceholder.layoutParams
                renderer?.layoutParams = layoutParams
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
            val baseTileWidth = (screenWidth - margins) / 2
            val tileWidth = (baseTileWidth * 0.8).toInt()
            val tileHeight = tileWidth

            // Set GridLayout params for the participant view
            val params = GridLayout.LayoutParams()
            params.width = tileWidth
            params.height = tileHeight
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
            params.setMargins(8, 8, 8, 8)
            participantView.layoutParams = params

            holder = ParticipantViewHolder(
                participantView,
                renderer,
                userIdText,
                micIcon,
                cameraOffLayout
            )
        }

        // Common for both roles
        participants[userId] = holder
        participantsList.add(userId)

        // Set initial visibility to GONE (showCurrentPage will make it visible if needed)
        participantView.visibility = View.GONE

        // Add to grid once
        participantsGridLayout.addView(participantView)

        Log.d(TAG, "Added participant: $userId (role: $role)")
    }

    private fun removeParticipant(userId: String) {
        val holder = participants[userId]
        if (holder != null) {
            // Remove from grid
            participantsGridLayout.removeView(holder.itemView)

            // Remove renderer from its parent
            (holder.renderer?.parent as? ViewGroup)?.removeView(holder.renderer)

            // Remove from data structures
            participants.remove(userId)
            participantsList.remove(userId)

            // Adjust current page if needed
            val totalPages = getTotalPages()
            if (totalPages > 0 && currentPage >= totalPages) {
                currentPage = totalPages - 1
            }

            Log.d(TAG, "Removed participant: $userId")
        }
    }

    private fun getTotalPages(): Int {
        val totalParticipants = participantsList.size
        return if (totalParticipants == 0) 1 else ((totalParticipants + participantsPerPage - 1) / participantsPerPage)
    }

    private fun showCurrentPage() {
        // Calculate range for current page
        val start = currentPage * participantsPerPage
        val end = min(start + participantsPerPage, participantsList.size)

        // Update visibility of all participants
        for (i in participantsList.indices) {
            val userId = participantsList[i]
            val holder = participants[userId]
            holder?.let {
                if (i in start until end) {
                    // Show this participant
                    it.itemView.visibility = View.VISIBLE

                    // Ensure it's in the grid (only add once)
                    if (it.itemView.parent == null) {
                        participantsGridLayout.addView(it.itemView)
                    }
                } else {
                    // Hide this participant
                    it.itemView.visibility = View.GONE
                }
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
        // Include local user in count only if publisher
        val localUserCount = 1
        participantCountText.text = "Total Participants: ${participants.size + localUserCount}"
    }

    fun initSdk() {
        val builder = IRed5WebrtcClient.builder()
            .setActivity(this)
            .setDataChannelListener(object : IRed5WebrtcClient.DataChannelListener {
                override fun onDataChannelOpen() {
                    Log.i(TAG, "Data Channel Open")

                }

                override fun onDataChannelClosed() {}

                override fun onDataChannelMessage(message: String?) {
                    Log.i(TAG, "Data channel message received: $message")
                }

                override fun onDataChannelMessage(data: ByteArray?) {}

                override fun onDataChannelError(error: String?) {}
            })
            .setStreamManagerHost(SettingsActivity.getStreamManagerHost(this))
            .setLicenseKey(SettingsActivity.getLicenseKey(this))
            .setStreamName(SettingsActivity.getStreamName(this))
            .setUserName(SettingsActivity.getUserName(this))
            .setPassword(SettingsActivity.getPassword(this))
            .setNodeGroup(SettingsActivity.getNodeGroup(this))
            .setAuthToken("")
            .setStatsPollingIntervalMs(500)
            .setVideoWidth(1280)
            .setVideoHeight(720)
            .setVideoFps(30)
            .setVideoBitrate(1500)
            .setVideoSource(IRed5WebrtcClient.StreamSource.FRONT_CAMERA)
            .setEventListener(this)
            .setConferenceListener(conferenceListener)

        // Only enable video/audio for publishers
        if (userRole == "publisher") {
            builder.setVideoEnabled(true)
                .setAudioEnabled(true)
                .setVideoRenderer(localVideoRendererForPreview)
        } else {
            builder.setVideoEnabled(false)
                .setAudioEnabled(false)
        }

        red5Client = builder.build()

    }

    private fun joinConference() {
        updateStatus("Joining...", "#FFA500")
        showConferenceLayout()

        if (userRole == "publisher") {
            red5Client?.setVideoRenderer(localVideoRenderer)
        }

        val userId = userName + "_" + getRandomString(6)
        val metaData = JSONObject()
        metaData.put("name", userName)

        fetchTokenAndJoin(userId, metaData.toString())
    }

    private fun fetchTokenAndJoin(userId: String, metaData: String) {
        val host = SettingsActivity.getStreamManagerHost(this)
        val cleanHost = host.removePrefix("https://").removePrefix("http://")
        val urlString = "https://$cleanHost/config-meetings/api/generate-token"

        Thread {
            var fetchedToken = ""
            try {
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("userId", userId)
                    put("roomId", roomId)
                    put("role", userRole)
                    put("expirationMinutes", 300)
                }
                connection.outputStream.use { it.write(body.toString().toByteArray()) }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    fetchedToken = try {
                        JSONObject(response).optString("token", "")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse token: ${e.message}")
                        ""
                    }
                } else {
                    Log.e(TAG, "Token fetch failed with code: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token fetch error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Token fetch error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return@Thread
            }

            runOnUiThread {
                red5Client?.join(roomId, userId, fetchedToken, userRole, metaData)
            }
        }.start()
    }

    private fun leaveConference() {
        ConnectionForegroundService.stop(this)
        updateStatus("Disconnected", "#F44336")
        isConnected = false
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
            statusText.setTextColor(Color.parseColor(color))
        }
    }

    private fun showConferenceLayout() {
        runOnUiThread {
            joinLayout.visibility = View.GONE
            conferenceLayout.visibility = View.VISIBLE
            localUserNameText.text = userName

            // Get layout params for participants container
            val participantsParams = participantsContainer.layoutParams as RelativeLayout.LayoutParams
            val paginationParams = paginationContainer.layoutParams as RelativeLayout.LayoutParams

            if (userRole == "subscriber") {
                // Subscriber mode - hide publisher UI, show subscriber leave button
                localVideoSection.visibility = View.GONE
                conferenceButtonContainer.visibility = View.GONE
                subscriberLeaveButton.visibility = View.VISIBLE
                subscriberLocalContainer.visibility = View.VISIBLE
                subscriberLocalParticipantUserId.text = userName

                participantsParams.removeRule(RelativeLayout.BELOW)
                participantsParams.addRule(RelativeLayout.BELOW, R.id.statusInfoContainer)
                participantsParams.topMargin = 16

                // Pagination above subscriber leave button
                paginationParams.removeRule(RelativeLayout.ABOVE)
                paginationParams.addRule(RelativeLayout.ABOVE, R.id.subscriberLocalContainer)
                paginationParams.bottomMargin = 8
            } else {
                // Publisher mode - show publisher UI, hide subscriber button
                localVideoSection.visibility = View.VISIBLE
                conferenceButtonContainer.visibility = View.VISIBLE
                subscriberLeaveButton.visibility = View.GONE
                subscriberLocalContainer.visibility = View.GONE

                // Participants container below local video
                participantsParams.removeRule(RelativeLayout.BELOW)
                participantsParams.addRule(RelativeLayout.BELOW, R.id.localVideoSection)
                participantsParams.topMargin = 8

                // Pagination above publisher buttons
                paginationParams.removeRule(RelativeLayout.ABOVE)
                paginationParams.addRule(RelativeLayout.ABOVE, R.id.conferenceButtonContainer)
                paginationParams.bottomMargin = 8

                switchCameraButton = findViewById(R.id.switchCameraButton)
                toggleCameraButton = findViewById(R.id.toggleCameraButton)
                toggleMicButton = findViewById(R.id.toggleMicButton)
                localPreviewBlackOverlay = findViewById(R.id.localPreviewBlackOverlay)
                setupConferenceButtonListeners()
            }

            // Apply the updated params
            participantsContainer.layoutParams = participantsParams
            paginationContainer.layoutParams = paginationParams
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
        if (validated && userRole == "publisher") {
            red5Client?.startPreview()
        }
    }

    override fun onError(error: String?) {
        Log.e("ConferenceActivity", "Error: $error")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions()) {
                initSdk()
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        // Auto-enter PiP mode when user navigates away (if publishing and supported)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isConnected && !isInPictureInPictureMode) {
            enterPictureInPictureMode()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun enterPictureInPictureMode() {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(9, 16))
            .build()

        val result = enterPictureInPictureMode(params)
        if (!result) {
            Toast.makeText(this, "Could not enter Picture-in-Picture mode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)


        if (isInPictureInPictureMode) {
            // Hide all UI controls in PiP mode
            statusText.visibility = View.GONE
            roomIdParticipantContainer.visibility = View.GONE
            conferenceButtonContainer.visibility = View.GONE
            paginationContainer.visibility = View.GONE
            subscriberLeaveButton.visibility = View.GONE
            subscriberLocalContainer.visibility = View.GONE

            // Check if there are remote participants
            if (participantsList.isEmpty()) {
                // No remote participants - show only local video
                localVideoSection.visibility = View.VISIBLE
                participantsContainer.visibility = View.GONE
                Log.d(TAG, "PiP mode: No remote participants, showing local video")
            } else {
                // Show all remote participants (remove pagination in PiP)
                localVideoSection.visibility = View.GONE
                participantsContainer.visibility = View.VISIBLE

                for (holder in participants.values) {
                    holder.itemView.visibility = View.VISIBLE
                }
                Log.d(TAG, "PiP mode: Showing ${participantsList.size} remote participants")
            }

            Log.d(TAG, "Entered Picture-in-Picture mode")
        } else {
            // Restore UI controls when returning from PiP mode
            statusText.visibility = View.VISIBLE
            participantsContainer.visibility = View.VISIBLE

            if (isConnected) {
                roomIdParticipantContainer.visibility = View.VISIBLE
            }

            if (userRole == "subscriber") {
                subscriberLeaveButton.visibility = View.VISIBLE
                subscriberLocalContainer.visibility = View.VISIBLE
                localVideoSection.visibility = View.GONE
                conferenceButtonContainer.visibility = View.GONE
            } else {
                localVideoSection.visibility = View.VISIBLE
                conferenceButtonContainer.visibility = View.VISIBLE
                subscriberLeaveButton.visibility = View.GONE
                subscriberLocalContainer.visibility = View.GONE
            }

            // Restore pagination and current page view
            refreshCurrentPage()

            Log.d(TAG, "Exited Picture-in-Picture mode")
        }
    }


    override fun onChatConnected() {}
    override fun onChatDisconnected() {}
    override fun onChatMessageReceived(channel: String?, message: JsonElement?) {}
    override fun onChatSendSuccess(channel: String?, timetoken: Long?) {}
    override fun onChatSendError(channel: String?, errorMessage: String?) {}
    override fun onChatError(error: String?) {}

    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
    override fun onRtcStats(stats: RTCStats?) {

        stats?.let {
            // Print local audio level
            Log.d(TAG, "=== Local Audio Level ===")
            Log.d(TAG, "Local microphone level: ${it.localAudioLevel}")

            // Print per-participant stats
            if (it.participantStats.isNotEmpty()) {
                Log.d(TAG, "=== Remote Participants (${it.participantStats.size}) ===")

                it.participantStats.forEach { (participantUid, participantStats) ->
                    Log.d(TAG, "Participant: $participantUid")
                    Log.d(TAG, "  Audio Level: ${participantStats.audioLevel}")
                    Log.d(TAG, "  RX Video: ${participantStats.rxVideoBytes} bytes")
                    Log.d(TAG, "  RX Audio: ${participantStats.rxAudioBytes} bytes")
                    Log.d(TAG, "  Bitrate: ${participantStats.rxKBitRate} kbps")
                    Log.d(TAG, "  Packet Loss: ${participantStats.packetLossRate}%")
                    Log.d(TAG, "  RTT: ${participantStats.rtt} ms")
                    Log.d(TAG, "  Jitter: ${participantStats.jitter} sec")
                    Log.d(TAG, "  Frames Dropped: ${participantStats.framesDropped}")
                    Log.d(TAG, "  Freezes: ${participantStats.freezeCount}")
                    Log.d(TAG, "  ---")
                }
            } else {
                Log.d(TAG, "No remote participants")
            }

            Log.d(TAG, "=== Aggregated Stats ===")
            Log.d(TAG, "Total Duration: ${it.totalDuration} sec")
            Log.d(TAG, "Total Users: ${it.users}")
            Log.d(TAG, "TX Bitrate: ${it.txKBitRate} kbps (Audio: ${it.txAudioKBitRate}, Video: ${it.txVideoKBitRate})")
            Log.d(TAG, "RX Bitrate: ${it.rxKBitRate} kbps (Audio: ${it.rxAudioKBitRate}, Video: ${it.rxVideoKBitRate})")
            Log.d(TAG, "================================")
        }
    }



    override fun onResume() {
        super.onResume()
        red5Client?.onActivityResume()
    }

    override fun onDestroy() {
        ConnectionForegroundService.stop(this)
        super.onDestroy()
    }

    override fun onBackPressed() {
        red5Client?.release()
        super.onBackPressed()
    }

    data class ParticipantViewHolder(
        val itemView: View,
        val renderer: Red5Renderer?,
        val userIdText: TextView,
        val micIcon: ImageView?,
        val cameraOffLayout: RelativeLayout?
    )
}