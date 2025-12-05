package net.red5.testbed.basic

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.JsonElement
import net.red5.android.api.IRed5WebrtcClient
import net.red5.android.api.IRed5WebrtcClient.Red5EventListener
import net.red5.android.core.Red5Renderer
import net.red5.testbed.R
import net.red5.testbed.SettingsActivity


/**
 * Simple example showing publishing to Red5 standalone
 */
class StandalonePublishActivity : AppCompatActivity(), Red5EventListener {
    private var surfaceView: Red5Renderer? = null
    private var publishButton: Button? = null
    private var switchCameraButton: Button? = null
    private var toggleMicButton: Button? = null
    private var toggleCameraButton: Button? = null

    private var blackOverlay: FrameLayout? = null

    private var statusIndicatorTextView: TextView? = null
    private var controlsLayout: LinearLayout? = null

    private var webrtcClient: IRed5WebrtcClient? = null
    private var isPublishing = false
    private var isMicEnabled = true
    private var isCameraEnabled = true
    private var isInPictureInPictureMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_publish)

        initViews()
        checkPermissions()
    }

    override fun onLicenseValidated(validated: Boolean, message: String?) {
        if (validated) {
            webrtcClient!!.startPreview()
            Log.d(TAG, "WHIP client initialized")

            Toast.makeText(this, "License check success", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "License check failed.", Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "License validation status: " + validated + " message: " + message)
    }

    override fun onChatMessageReceived(
        channel: String?,
        message: JsonElement?
    ) {

    }

    override fun onChatConnected() {
    }

    override fun onChatDisconnected() {
    }

    override fun onChatSendError(channel: String?, errorMessage: String?) {
    }

    override fun onChatSendSuccess(channel: String?, timetoken: Long?) {
    }


    private fun initViews() {
        surfaceView = findViewById<Red5Renderer>(R.id.surface_view)
        publishButton = findViewById<Button>(R.id.btn_publish)
        switchCameraButton = findViewById<Button>(R.id.btn_switch_camera)
        toggleMicButton = findViewById<Button>(R.id.btn_toggle_mic)
        toggleCameraButton = findViewById<Button>(R.id.btn_toggle_camera)
        controlsLayout = findViewById<LinearLayout>(R.id.controls_layout)
        statusIndicatorTextView = findViewById<TextView>(R.id.status_indicator_text)
        blackOverlay = findViewById<FrameLayout>(R.id.blackOverlay)
        // Main publish button
        publishButton!!.setOnClickListener { v: View? ->
            if (isPublishing) {
                stopPublish()
            } else {
                startPublish()
            }
        }

        // Switch camera button
        switchCameraButton!!.setOnClickListener { v: View? ->
            if (webrtcClient != null) {
                webrtcClient!!.switchCamera()
                Toast.makeText(this, "Camera switched", Toast.LENGTH_SHORT).show()
            }
        }

        // Toggle microphone button
        toggleMicButton!!.setOnClickListener { v: View? ->
            if (webrtcClient != null) {
                isMicEnabled = !isMicEnabled
                webrtcClient!!.toggleSendAudio(isMicEnabled)

                // Update button text and color
                toggleMicButton!!.setText(if (isMicEnabled) "MIC ON" else "MIC OFF")
                toggleMicButton!!.setBackgroundTintList(
                    getResources().getColorStateList(
                        if (isMicEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark,
                        getTheme()
                    )
                )

                Toast.makeText(
                    this, if (isMicEnabled) "Microphone enabled" else "Microphone disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        toggleCameraButton!!.setOnClickListener { v: View? ->
            if (webrtcClient != null) {
                isCameraEnabled = !isCameraEnabled
                webrtcClient!!.toggleSendVideo(isCameraEnabled)
                if (isCameraEnabled) {
                    blackOverlay!!.setVisibility(View.GONE)
                } else {
                    blackOverlay!!.setVisibility(View.VISIBLE)
                }


                toggleCameraButton!!.setText(if (isCameraEnabled) "CAM ON" else "CAM OFF")
                toggleCameraButton!!.setBackgroundTintList(
                    getResources().getColorStateList(
                        if (isCameraEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark,
                        getTheme()
                    )
                )
            }
        }

        switchCameraButton!!.setEnabled(false)
        toggleMicButton!!.setEnabled(false)
        toggleCameraButton!!.setEnabled(false)
    }



    private fun checkPermissions() {
        if (hasAllPermissions()) {
            initializeWebrtcClient()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
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

    private fun initializeWebrtcClient() {
        webrtcClient = IRed5WebrtcClient.builder()
            .setActivity(this)
            .setLicenseKey(SettingsActivity.getLicenseKey(this))
            .setServerIp(SettingsActivity.getStandaloneServerIp(this))
            .setStreamName(SettingsActivity.getStreamName(this))
            .setUserName(SettingsActivity.getUserName(this))
            .setPassword(SettingsActivity.getPassword(this))
            .setAuthToken("")
            .setVideoEnabled(true)
            .setAudioEnabled(true)
            .setVideoWidth(1280)
            .setVideoHeight(720)
            .setVideoFps(30)
            .setVideoBitrate(1500)
            .setVideoSource(IRed5WebrtcClient.StreamSource.FRONT_CAMERA)
            .setVideoRenderer(surfaceView)
            .setEventListener(this)
            .build()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun enterPictureInPictureMode() {
        val aspectRatio = Rational(surfaceView!!.getWidth(), surfaceView!!.getHeight())

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()

        val result = enterPictureInPictureMode(params)
        if (!result) {
            Toast.makeText(this, "Could not enter Picture-in-Picture mode", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        this.isInPictureInPictureMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            // Hide UI controls in PiP mode
            controlsLayout!!.setVisibility(View.GONE)
            publishButton!!.setVisibility(View.GONE)
            statusIndicatorTextView!!.setVisibility(View.GONE)
            Log.d(TAG, "Entered Picture-in-Picture mode")
        } else {
            // Show UI controls when returning from PiP mode
            controlsLayout!!.setVisibility(View.VISIBLE)
            publishButton!!.setVisibility(View.VISIBLE)

            statusIndicatorTextView!!.setVisibility(View.VISIBLE)
            Log.d(TAG, "Exited Picture-in-Picture mode")
        }
    }

    private fun startPublish() {
        if (webrtcClient != null && !isPublishing) {
            Log.d(TAG, "Starting publish...")
            webrtcClient!!.publish(SettingsActivity.getStreamName(this))
            publishButton!!.setText("PUBLISHING...")
            publishButton!!.setEnabled(false)
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.blue))
            statusIndicatorTextView!!.setText("Connecting")
        }
    }

    private fun stopPublish() {
        if (webrtcClient != null && isPublishing) {
            Log.d(TAG, "Stopping publish...")
            webrtcClient!!.stopPublish()
            publishButton!!.setText("STOPPING...")
            publishButton!!.setEnabled(false)
        }
    }

    private fun enableControlButtons(enable: Boolean) {
        runOnUiThread(Runnable {
            switchCameraButton!!.setEnabled(enable)
            toggleMicButton!!.setEnabled(enable)
            toggleCameraButton!!.setEnabled(enable)
            if (enable) {
                toggleMicButton!!.setText(if (isMicEnabled) "MIC ON" else "MIC OFF")
                toggleMicButton!!.setBackgroundTintList(
                    getResources().getColorStateList(
                        if (isMicEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark,
                        getTheme()
                    )
                )

                toggleCameraButton!!.setText(if (isCameraEnabled) "CAM ON" else "CAM OFF")
                toggleCameraButton!!.setBackgroundTintList(
                    getResources().getColorStateList(
                        if (isCameraEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark,
                        getTheme()
                    )
                )
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions()) {
                initializeWebrtcClient()
            } else {
                Toast.makeText(this, "Permissions required for WebRTC", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        if (webrtcClient != null) {
            webrtcClient!!.release()
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        // Auto-enter PiP mode when user navigates away (if publishing and supported)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPublishing && !isInPictureInPictureMode) {
            enterPictureInPictureMode()
        }
    }

    override fun onPublishStarted() {
        Log.d(TAG, "Publish started successfully")
        runOnUiThread(Runnable {
            isPublishing = true
            publishButton!!.setText("STOP PUBLISH")
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.green))
            statusIndicatorTextView!!.setText("Live")

            publishButton!!.setEnabled(true)
            publishButton!!.setBackgroundTintList(
                getResources().getColorStateList(android.R.color.holo_red_dark, getTheme())
            )
            enableControlButtons(true)
        })
    }

    override fun onPublishStopped() {
        Log.d(TAG, "Publish stopped")
        runOnUiThread(Runnable {
            isPublishing = false
            publishButton!!.setText("START PUBLISH")
            publishButton!!.setEnabled(true)
            publishButton!!.setBackgroundTintList(
                getResources().getColorStateList(android.R.color.holo_blue_dark, getTheme())
            )
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.red))
            statusIndicatorTextView!!.setText("Disconnected")


            // Exit PiP mode when publishing stops
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
                // The system will automatically exit PiP when the activity finishes
                // or you can call moveTaskToBack(false) to minimize instead
            }
        })
    }

    override fun onPublishFailed(error: String?) {
        Log.e(TAG, "Publish failed: " + error)
        runOnUiThread(Runnable {
            isPublishing = false
            publishButton!!.setText("START PUBLISH")
            publishButton!!.setEnabled(true)
            publishButton!!.setBackgroundTintList(
                getResources().getColorStateList(android.R.color.holo_blue_dark, getTheme())
            )
            Toast.makeText(this, "Publish failed: " + error, Toast.LENGTH_LONG).show()
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.red))
            statusIndicatorTextView!!.setText("Disconnected")
        })
    }

    override fun onSubscribeStarted() {
        Log.d(TAG, "Subscribe started")
    }

    override fun onSubscribeStopped() {
        Log.d(TAG, "Subscribe stopped")
    }

    override fun onSubscribeFailed(error: String?) {
        Log.e(TAG, "Subscribe failed: " + error)
    }

    override fun onPreviewStarted() {
        Log.d(TAG, "Preview started")
        runOnUiThread(Runnable {
            enableControlButtons(true)
        })
    }

    override fun onPreviewStopped() {
        Log.d(TAG, "Preview stopped")
        runOnUiThread(Runnable {
            enableControlButtons(false)
        })
    }

    override fun onIceConnectionStateChanged(state: IRed5WebrtcClient.IceConnectionState?) {
        Log.d(TAG, "ICE connection state: " + state)

        // Handle connection state changes
        if (state == IRed5WebrtcClient.IceConnectionState.CONNECTED) {
            runOnUiThread(Runnable {
                Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show()
            })
        } else if (state == IRed5WebrtcClient.IceConnectionState.DISCONNECTED ||
            state == IRed5WebrtcClient.IceConnectionState.FAILED
        ) {
            runOnUiThread(Runnable {
                Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show()
            })
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.red))
            statusIndicatorTextView!!.setText("Disconnected")
        }
    }

    override fun onConnectionStateChanged(state: IRed5WebrtcClient.PeerConnectionState?) {
        Log.d(TAG, "Peer connection state: " + state)
    }


    override fun onError(error: String?) {
        Log.e(TAG, "General error: " + error)
        runOnUiThread {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onChatError(error: String?) {

    }

    companion object {
        private const val TAG = "StandalonePublishActivity"
        private const val PERMISSION_REQUEST_CODE = 1001

        private val REQUIRED_PERMISSIONS = arrayOf<String>(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}