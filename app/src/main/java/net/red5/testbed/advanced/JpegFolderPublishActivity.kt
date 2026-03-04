package net.red5.testbed.advanced

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.JsonElement
import net.red5.android.api.IRed5WebrtcClient
import net.red5.android.api.IRed5WebrtcClient.Red5EventListener
import net.red5.android.core.model.RTCStats
import net.red5.testbed.R
import net.red5.testbed.SettingsActivity
import androidx.core.graphics.toColorInt

/**
 * Demonstrates publishing a stream where video frames are read from a folder of JPEG images.
 *
 * The SDK client is built once at startup (so the licence check runs immediately).
 * The publish button is enabled after licence validation. Before each publish,
 * a fresh [JpegFolderVideoCapturer] is created with the selected folder path and
 * playback mode, then handed to the client via [IRed5WebrtcClient.setVideoCapturer].
 */
class JpegFolderPublishActivity : AppCompatActivity(), Red5EventListener {

    private var etFolderPath: EditText? = null
    private var etFps: EditText? = null
    private var btnBrowse: Button? = null
    private var btnPublish: Button? = null
    private var statusTextView: TextView? = null
    private var rgPlaybackMode: RadioGroup? = null
    private var rgConnectionMode: RadioGroup? = null

    private var red5Client: IRed5WebrtcClient? = null
    private var isPublishing = false

    // -------------------------------------------------------------------------
    // Folder picker
    // -------------------------------------------------------------------------

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val path = uriToFilePath(uri)
        if (path != null) {
            etFolderPath?.setText(path)
        } else {
            Toast.makeText(
                this,
                "Could not resolve path automatically. Please type the folder path manually.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jpeg_folder_publish)
        initViews()
        checkPermissions()
    }

    override fun onDestroy() {
        red5Client?.release()
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // UI setup
    // -------------------------------------------------------------------------

    private fun initViews() {
        etFolderPath = findViewById(R.id.et_folder_path)
        etFps = findViewById(R.id.et_fps)
        btnBrowse = findViewById(R.id.btn_browse)
        btnPublish = findViewById(R.id.btn_publish)
        statusTextView = findViewById(R.id.status_indicator_text)
        rgPlaybackMode = findViewById(R.id.rg_playback_mode)
        rgConnectionMode = findViewById(R.id.rg_connection_mode)

        // Publish button starts disabled until licence is validated
        btnPublish?.isEnabled = false

        btnBrowse?.setOnClickListener { folderPickerLauncher.launch(null) }
        btnPublish?.setOnClickListener { if (isPublishing) stopPublish() else startPublish() }
    }

    // -------------------------------------------------------------------------
    // Permissions
    // -------------------------------------------------------------------------

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun checkPermissions() {
        if (hasAllPermissions()) {
            initializeWebrtcClient()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions()) {
                initializeWebrtcClient()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to read JPEG files.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // -------------------------------------------------------------------------
    // SDK initialisation
    // -------------------------------------------------------------------------

    private fun initializeWebrtcClient() {
        red5Client = IRed5WebrtcClient.builder()
            .setActivity(this)
            .setLicenseKey(SettingsActivity.getLicenseKey(this))
            .setServerIp(SettingsActivity.getStandaloneServerIp(this))
            .setStreamName(SettingsActivity.getStreamName(this))
            .setVideoSource(IRed5WebrtcClient.StreamSource.CUSTOM)
            .setVideoEnabled(true)
            .setAudioEnabled(true)
            .setVideoWidth(1280)
            .setVideoHeight(720)
            .setVideoFps(30)
            .setEventListener(this)
            .build()

        updateStatus("Validating license...", "#FFA500")
    }

    // -------------------------------------------------------------------------
    // Publish control
    // -------------------------------------------------------------------------

    private fun startPublish() {
        val folderPath = etFolderPath?.text?.toString()?.trim() ?: ""
        if (folderPath.isEmpty()) {
            Toast.makeText(this, "Please enter or browse to a JPEG folder path.", Toast.LENGTH_SHORT).show()
            return
        }

        val mode = when (rgPlaybackMode?.checkedRadioButtonId) {
            R.id.rb_hold_last -> JpegFolderVideoCapturer.PlaybackMode.HOLD_LAST
            else              -> JpegFolderVideoCapturer.PlaybackMode.LOOP
        }

        val fps = etFps?.text?.toString()?.toIntOrNull()?.coerceIn(1, 30) ?: 20

        if (rgConnectionMode?.checkedRadioButtonId == R.id.rb_stream_manager) {
            red5Client?.config?.serverIp = SettingsActivity.getStreamManagerHost(this)
        } else {
            red5Client?.config?.serverIp = SettingsActivity.getStandaloneServerIp(this)
        }

        // Create a fresh capturer for this publish session and hand it to the client.
        val capturer = JpegFolderVideoCapturer(folderPath, mode, fps)
        red5Client?.setVideoCapturer(capturer)

        updateStatus("Connecting...", "#FFA500")
        btnPublish?.text = "CONNECTING..."
        btnPublish?.isEnabled = false
        etFolderPath?.isEnabled = false
        etFps?.isEnabled = false
        btnBrowse?.isEnabled = false
        rgPlaybackMode?.isEnabled = false
        rgPlaybackMode?.alpha = 0.5f
        rgConnectionMode?.isEnabled = false
        rgConnectionMode?.alpha = 0.5f

        red5Client?.publish(SettingsActivity.getStreamName(this))
    }

    private fun stopPublish() {
        red5Client?.stopPublish()
        btnPublish?.text = "STOPPING..."
        btnPublish?.isEnabled = false
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun updateStatus(text: String, colorHex: String) {
        runOnUiThread {
            statusTextView?.text = text
            statusTextView?.setTextColor(colorHex.toColorInt())
        }
    }

    /**
     * Attempts to convert a content:// tree URI from OpenDocumentTree into an absolute path.
     * Works for primary internal storage ("primary:…"). Returns null for SD-card / other
     * providers where the path cannot be determined reliably.
     */
    private fun uriToFilePath(uri: Uri): String? {
        return try {
            val treeDocId = DocumentsContract.getTreeDocumentId(uri)
            when {
                treeDocId.startsWith("primary:") ->
                    "${Environment.getExternalStorageDirectory().absolutePath}/${treeDocId.removePrefix("primary:")}"
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not convert URI to path: $uri", e)
            null
        }
    }

    // -------------------------------------------------------------------------
    // Red5EventListener
    // -------------------------------------------------------------------------

    override fun onLicenseValidated(validated: Boolean, message: String?) {
        Log.d(TAG, "License validated: $validated – $message")
        runOnUiThread {
            if (validated) {
                updateStatus("Ready", "#00CC44")
                btnPublish?.isEnabled = true
                Toast.makeText(this, "License validated", Toast.LENGTH_SHORT).show()
            } else {
                updateStatus("License failed", "#FF3333")
                Toast.makeText(this, "License validation failed: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPublishStarted() {
        Log.d(TAG, "Publish started")
        runOnUiThread {
            isPublishing = true
            updateStatus("Live", "#00CC44")
            btnPublish?.text = "STOP PUBLISH"
            btnPublish?.isEnabled = true
            btnPublish?.backgroundTintList =
                resources.getColorStateList(android.R.color.holo_red_dark, theme)
            Toast.makeText(this, "Streaming JPEG frames", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPublishStopped() {
        Log.d(TAG, "Publish stopped")
        runOnUiThread {
            isPublishing = false
            updateStatus("Ready", "#00CC44")
            resetPublishButton()
            etFolderPath?.isEnabled = true
            etFps?.isEnabled = true
            btnBrowse?.isEnabled = true
            rgPlaybackMode?.isEnabled = true
            rgPlaybackMode?.alpha = 1f
            rgConnectionMode?.isEnabled = true
            rgConnectionMode?.alpha = 1f
            Toast.makeText(this, "Publish stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPublishFailed(error: String?) {
        Log.e(TAG, "Publish failed: $error")
        runOnUiThread {
            isPublishing = false
            updateStatus("Failed", "#FF3333")
            resetPublishButton()
            etFolderPath?.isEnabled = true
            etFps?.isEnabled = true
            btnBrowse?.isEnabled = true
            rgPlaybackMode?.isEnabled = true
            rgPlaybackMode?.alpha = 1f
            rgConnectionMode?.isEnabled = true
            rgConnectionMode?.alpha = 1f
            Toast.makeText(this, "Publish failed: $error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onIceConnectionStateChanged(state: IRed5WebrtcClient.IceConnectionState?) {
        Log.d(TAG, "ICE state: $state")
        if (state == IRed5WebrtcClient.IceConnectionState.DISCONNECTED ||
            state == IRed5WebrtcClient.IceConnectionState.FAILED
        ) {
            updateStatus("Disconnected", "#FF3333")
        }
    }

    override fun onConnectionStateChanged(state: IRed5WebrtcClient.PeerConnectionState?) {
        Log.d(TAG, "PC state: $state")
    }

    override fun onSubscribeStarted() {}
    override fun onSubscribeStopped() {}
    override fun onSubscribeFailed(error: String?) {}
    override fun onPreviewStarted() {}
    override fun onPreviewStopped() {}

    override fun onError(error: String?) {
        Log.e(TAG, "Error: $error")
        runOnUiThread { Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show() }
    }

    override fun onRtcStats(stats: RTCStats?) {}
    override fun onChatConnected() {}
    override fun onChatDisconnected() {}
    override fun onChatMessageReceived(channel: String?, message: JsonElement?) {}
    override fun onChatSendSuccess(channel: String?, timetoken: Long?) {}
    override fun onChatSendError(channel: String?, errorMessage: String?) {}
    override fun onChatError(error: String?) {}

    // -------------------------------------------------------------------------

    private fun resetPublishButton() {
        btnPublish?.text = "START PUBLISH"
        btnPublish?.isEnabled = true
        btnPublish?.backgroundTintList =
            resources.getColorStateList(android.R.color.holo_blue_dark, theme)
    }

    companion object {
        private const val TAG = "JpegFolderPublishActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
