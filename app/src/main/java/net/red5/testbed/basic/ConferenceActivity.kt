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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import net.red5.android.api.IRed5WebrtcClient
import net.red5.android.api.IRed5WebrtcClient.Red5EventListener
import net.red5.android.core.Red5Renderer
import net.red5.testbed.R
import net.red5.testbed.SettingsActivity
import org.json.JSONObject
import org.webrtc.VideoTrack

class ConferenceActivity : AppCompatActivity(), Red5EventListener {

    private var red5Client: IRed5WebrtcClient? = null
    public var TAG = "ConferenceActivity"
    private lateinit var joinLayout: LinearLayout
    private lateinit var roomIdInput: EditText
    private lateinit var userNameInput: EditText
    private lateinit var joinButton: Button

    private lateinit var conferenceLayout: RelativeLayout
    private lateinit var statusText: TextView
    private lateinit var localVideoRenderer: Red5Renderer
    private lateinit var localUserNameText: TextView
    private lateinit var participantsRecyclerView: RecyclerView
    private lateinit var leaveButton: Button

    private var roomId: String = ""
    private var userName: String = ""
    private val participants = mutableListOf<Participant>()
    private lateinit var participantsAdapter: ParticipantsAdapter
    private lateinit var conferenceListener: IRed5WebrtcClient.ConferenceListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conference)

        initViews()
        setupListeners()
        setupRecyclerView()

        conferenceListener = object : IRed5WebrtcClient.ConferenceListener{
            override fun onJoinRoomSuccess() {

                updateStatus("Connected", "#4CAF50")

            }

            override fun onJoinRoomFailed(message: String?) {

            }

            override fun onParticipantJoined(
                uid: String?,
                videoEnabled: Boolean,
                audioEnabled: Boolean,
                renderer: Red5Renderer
            ) {
                runOnUiThread {
                        val participant = Participant(uid!!, renderer)
                        participants.add(participant)
                        participantsAdapter.notifyItemInserted(participants.size - 1)

                }
            }

            override fun onParticipantLeft(uid:String) {
                runOnUiThread {
                    val index = participants.indexOfFirst { it.userId == uid }
                    if (index != -1) {
                        val participant = participants[index]

                        (participant.renderer.parent as? android.view.ViewGroup)?.removeView(participant.renderer)

                        participants.removeAt(index)

                        participantsAdapter.notifyItemRemoved(index)

                        Log.d(TAG, "Removed participant: $uid from RecyclerView")
                    } else {
                        Log.w(TAG, "Participant not found in list: $uid")
                    }
                }

            }
        }
    }

    private fun initViews() {
        joinLayout = findViewById(R.id.joinLayout)
        roomIdInput = findViewById(R.id.roomIdInput)
        roomIdInput.setText("testroom")

        userNameInput = findViewById(R.id.userNameInput)
        userNameInput.setText("yunus_android")
        joinButton = findViewById(R.id.joinButton)

        conferenceLayout = findViewById(R.id.conferenceLayout)
        statusText = findViewById(R.id.statusText)
        localVideoRenderer = findViewById(R.id.localVideoRenderer)
        localUserNameText = findViewById(R.id.localUserNameText)
        participantsRecyclerView = findViewById(R.id.participantsRecyclerView)
        leaveButton = findViewById(R.id.leaveButton)
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
    }

    private fun setupRecyclerView() {
        participantsAdapter = ParticipantsAdapter(participants)
        participantsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        participantsRecyclerView.adapter = participantsAdapter


        participantsRecyclerView.setItemViewCacheSize(0)
        participantsRecyclerView.recycledViewPool.setMaxRecycledViews(0, 0)

    }

    private fun joinConference() {
        updateStatus("Joining...", "#FFA500")

        showConferenceLayout()



        red5Client = IRed5WebrtcClient.builder()
            .setActivity(this)
            .setDataChannelListener(object : IRed5WebrtcClient.DataChannelListener{
                override fun onDataChannelOpen() {

                }

                override fun onDataChannelClosed() {

                }

                override fun onDataChannelMessage(message: String?) {
                Log.i(TAG, "Data channel message received: $message")
                }

                override fun onDataChannelMessage(data: ByteArray?) {

                }

                override fun onDataChannelError(error: String?) {

                }
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
            .setVideoRenderer(localVideoRenderer)
            .setEventListener(this)
            .setConferenceListener(conferenceListener)
            .build()
        
        

       // Toast.makeText(this, "Joined room: $roomId", Toast.LENGTH_SHORT).show()

    }

    private fun leaveConference() {
        updateStatus("Disconnected", "#F44336")
        red5Client?.release()

        runOnUiThread {
            conferenceLayout.visibility = View.GONE
            joinLayout.visibility = View.VISIBLE
            participants.clear()
            participantsAdapter.notifyDataSetChanged()
        }

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
        }
    }



    private fun removeParticipant(userId: String) {
        runOnUiThread {
            val index = participants.indexOfFirst { it.userId == userId }
            if (index != -1) {
                participants.removeAt(index)
                participantsAdapter.notifyItemRemoved(index)
            }
        }
    }

    // Unused event listeners for now
    override fun onPublishStarted() {}
    override fun onPublishStopped() {}
    override fun onPublishFailed(error: String?) {}
    override fun onSubscribeStarted() {}
    override fun onSubscribeStopped() {}
    override fun onSubscribeFailed(error: String?) {}
    override fun onIceConnectionStateChanged(state: IRed5WebrtcClient.IceConnectionState?) {}
    override fun onConnectionStateChanged(state: IRed5WebrtcClient.PeerConnectionState?) {}
    override fun onPreviewStarted() {
        var userId = userName + "_"+getRandomString(6)
        var metaData = JSONObject()
        metaData.put("name", userName)


        red5Client?.join(roomId, userId, "", "publisher", metaData.toString())


    }
    override fun onPreviewStopped() {}
    override fun onLicenseValidated(validated: Boolean, message: String?) {
        if(validated){

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


    fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun onDestroy() {
        super.onDestroy()
        red5Client?.release()
    }

    data class Participant(
        val userId: String,
        val renderer: Red5Renderer
    )

    inner class ParticipantsAdapter(private val participants: List<Participant>) :
        RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder>() {

        inner class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val videoRenderer: Red5Renderer = itemView.findViewById(R.id.participantVideoRenderer)
            val userIdText: TextView = itemView.findViewById(R.id.participantUserId)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ParticipantViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_participant, parent, false)
            return ParticipantViewHolder(view)
        }

        override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
            val participant = participants[position]
            holder.userIdText.text = participant.userId

            // Remove the participant's renderer from its current parent (if any)
            (participant.renderer.parent as? android.view.ViewGroup)?.removeView(participant.renderer)
            // Get the container that holds the placeholder renderer
            val container = holder.videoRenderer.parent as? android.view.ViewGroup

            if (container != null) {
                // Find the index of the placeholder renderer
                val index = container.indexOfChild(holder.videoRenderer)

                // Remove the placeholder renderer
                container.removeViewAt(index)

                // Add the actual initialized renderer at the same position
                val layoutParams = holder.videoRenderer.layoutParams
                participant.renderer.layoutParams = layoutParams
                container.addView(participant.renderer, index)
            }
        }

        override fun onViewRecycled(holder: ParticipantViewHolder) {
            super.onViewRecycled(holder)
            // Remove sink when view is recycled to prevent memory leaks
            holder.videoRenderer.clearImage()
        }

        override fun getItemCount(): Int = participants.size
    }
}