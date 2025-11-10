package net.red5.testbed.basic

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonElement
import net.red5.android.api.IRed5WebrtcClient
import net.red5.android.api.IRed5WebrtcClient.Red5EventListener
import net.red5.android.core.Red5Renderer
import net.red5.testbed.R
import net.red5.testbed.SettingsActivity


class StandaloneSubscribeActivity : AppCompatActivity(), Red5EventListener {
    private var surfaceView: Red5Renderer? = null
    private var subscribeButton: Button? = null
    private var statusIndicatorTextView: TextView? = null

    private var webrtcClient: IRed5WebrtcClient? = null
    private var isSubscribing = false
    private var isInPictureInPictureMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_subscribe)

        initViews()
        initializeSubscribeClient()
    }

    private fun initViews() {
        surfaceView = findViewById<Red5Renderer>(R.id.surface_view)
        subscribeButton = findViewById<Button>(R.id.btn_subscribe)
        statusIndicatorTextView = findViewById<TextView>(R.id.status_indicator_text)

        subscribeButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            if (isSubscribing) {
                stopSubscribe()
            } else {
                startSubscribe()
            }
        })
    }

    private fun initializeSubscribeClient() {
        webrtcClient = IRed5WebrtcClient.builder()
            .setActivity(this)
            .setDtlsSetup(SettingsActivity.getDtlsSetup(this))
            .setLicenseKey(SettingsActivity.getLicenseKey(this))
            .setServerIp(SettingsActivity.getStandaloneServerIp(this))
            .setStreamName(SettingsActivity.getStreamName(this))
            .setVideoRenderer(surfaceView)
            .setEventListener(this)

            .build()

        Log.d(TAG, "Subscribe client initialized")
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun enterPictureInPictureMode() {
        try {
            val aspectRatio =
                Rational(surfaceView!!.getRendererWidth(), surfaceView!!.getRendererHeight())

            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()


            val result = enterPictureInPictureMode(params)
            Log.d(TAG, "PiP mode attempt result: " + result + " with aspect ratio: " + aspectRatio)

            if (!result) {
                Log.e(TAG, "Failed to enter Picture-in-Picture mode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when entering PiP mode: " + e.message)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        this.isInPictureInPictureMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            statusIndicatorTextView!!.setVisibility(View.GONE)
            subscribeButton!!.setVisibility(View.GONE)


            Log.d(TAG, "Entered Picture-in-Picture mode - UI hidden")
        } else {
            statusIndicatorTextView!!.setVisibility(View.VISIBLE)
            subscribeButton!!.setVisibility(View.VISIBLE)

            Log.d(TAG, "Exited Picture-in-Picture mode - UI restored")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        Log.d(
            TAG,
            "onUserLeaveHint called. Subscribing: " + isSubscribing + ", SDK: " + Build.VERSION.SDK_INT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isSubscribing && !isInPictureInPictureMode) {
            Log.d(TAG, "Attempting to enter PiP mode automatically")
            enterPictureInPictureMode()
        } else {
            Log.d(
                TAG, "PiP conditions not met - SDK: " + Build.VERSION.SDK_INT +
                        ", Subscribing: " + isSubscribing + ", Already in PiP: " + isInPictureInPictureMode
            )
        }
    }

    private fun startSubscribe() {
        if (webrtcClient != null && !isSubscribing) {
            Log.d(TAG, "Starting subscribe...")
            webrtcClient!!.subscribe(SettingsActivity.getStreamName(this))
            subscribeButton!!.setText("SUBSCRIBING...")
            subscribeButton!!.setEnabled(false)
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.blue))
            statusIndicatorTextView!!.setText("Connecting")
        }
    }

    private fun stopSubscribe() {
        if (webrtcClient != null && isSubscribing) {
            Log.d(TAG, "Stopping subscribe...")
            webrtcClient!!.stopSubscribe()
            subscribeButton!!.setText("STOPPING...")
            subscribeButton!!.setEnabled(false)
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

        if (!isInPictureInPictureMode && webrtcClient != null && isSubscribing) {
        }
    }

    override fun onResume() {
        super.onResume()

        if (webrtcClient != null) {
        }
    }

    override fun onStop() {
        super.onStop()

        if (!isInPictureInPictureMode) {
            Log.d(TAG, "App stopped, not in PiP mode")
        }
    }

    override fun onPublishStarted() {
    }

    override fun onPublishStopped() {
    }

    override fun onPublishFailed(error: String?) {
    }

    override fun onSubscribeStarted() {
        Log.d(TAG, "Subscribe started successfully")
        runOnUiThread(Runnable {
            isSubscribing = true
            subscribeButton!!.setText("STOP SUBSCRIBE")
            subscribeButton!!.setEnabled(true)
            subscribeButton!!.setBackgroundTintList(
                getResources().getColorStateList(android.R.color.holo_red_dark, getTheme())
            )
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.green))
            statusIndicatorTextView!!.setText("Live")
            Toast.makeText(this, "Subscribing started", Toast.LENGTH_SHORT).show()
        })
    }

    override fun onSubscribeStopped() {
        Log.d(TAG, "Subscribe stopped")
        runOnUiThread(Runnable {
            isSubscribing = false
            subscribeButton!!.setText("START SUBSCRIBE")
            subscribeButton!!.setEnabled(true)
            subscribeButton!!.setBackgroundTintList(
                getResources().getColorStateList(android.R.color.holo_blue_dark, getTheme())
            )
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.red))
            statusIndicatorTextView!!.setText("Disconnected")
            Toast.makeText(this, "Subscribe stopped", Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            }
        })
    }

    override fun onLicenseValidated(validated: Boolean, message: String?) {
        if (validated) {
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
    override fun onChatError(error: String?) {
    }


    override fun onSubscribeFailed(error: String?) {
        Log.e(TAG, "Subscribe failed: " + error)
        runOnUiThread(Runnable {
            isSubscribing = false
            subscribeButton!!.setText("START SUBSCRIBE")
            subscribeButton!!.setEnabled(true)
            subscribeButton!!.setBackgroundTintList(
                getResources().getColorStateList(android.R.color.holo_blue_dark, getTheme())
            )
            statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.red))
            statusIndicatorTextView!!.setText("Disconnected")
            Toast.makeText(this, "Subscribe failed: " + error, Toast.LENGTH_SHORT).show()
        })
    }

    override fun onPreviewStarted() {
        Log.d(TAG, "Preview started")
    }

    override fun onPreviewStopped() {
        Log.d(TAG, "Preview stopped")
    }

    override fun onIceConnectionStateChanged(state: IRed5WebrtcClient.IceConnectionState?) {
        Log.d(TAG, "ICE connection state: " + state)

        if (state == IRed5WebrtcClient.IceConnectionState.CONNECTED) {
            runOnUiThread(Runnable {
                Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show()
            })
        } else if (state == IRed5WebrtcClient.IceConnectionState.DISCONNECTED ||
            state == IRed5WebrtcClient.IceConnectionState.FAILED
        ) {
            runOnUiThread(Runnable {
                Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show()
                statusIndicatorTextView!!.setTextColor(getResources().getColor(R.color.red))
                statusIndicatorTextView!!.setText("Disconnected")
            })
        }
    }

    override fun onConnectionStateChanged(state: IRed5WebrtcClient.PeerConnectionState?) {
        Log.d(TAG, "Peer connection state: " + state)
    }


    override fun onError(error: String?) {
        Log.e(TAG, "General error: " + error)
        runOnUiThread(Runnable {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show()
        })
    }

    companion object {
        private const val TAG = "StandaloneSubscribeActivity"
    }
}
