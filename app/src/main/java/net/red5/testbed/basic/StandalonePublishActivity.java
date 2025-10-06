package net.red5.testbed.basic;


import android.Manifest;
import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.red5.android.api.IRed5WebrtcClient;
import net.red5.android.core.Red5Renderer;
import net.red5.testbed.R;
import net.red5.testbed.SettingsActivity;

/**
 * Simple example showing publishing to Red5 standalone
 */
public class StandalonePublishActivity extends AppCompatActivity implements IRed5WebrtcClient.Red5ProWebrtcEventListener {
    private static final String TAG = "StandalonePublishActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private Red5Renderer surfaceView;
    private Button publishButton;
    private Button switchCameraButton;
    private Button toggleMicButton;
    private Button toggleCameraButton;

    private FrameLayout blackOverlay;

    private TextView statusIndicatorTextView;
    private LinearLayout controlsLayout;

    private IRed5WebrtcClient webrtcClient;
    private boolean isPublishing = false;
    private boolean isMicEnabled = true;
    private boolean isCameraEnabled = true;
    private boolean isInPictureInPictureMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_publish);

        initViews();
        checkPermissions();
    }

    @Override
    public void onLicenseValidated(boolean validated, String message) {
        if(validated){

            webrtcClient.startPreview();
            Log.d(TAG, "WHIP client initialized");

            Toast.makeText(this, "License check success",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "License check failed.",Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "License validation status: "+ validated +" message: "+ message);
    }

    private void initViews() {
        surfaceView = findViewById(R.id.surface_view);
        publishButton = findViewById(R.id.btn_publish);
        switchCameraButton = findViewById(R.id.btn_switch_camera);
        toggleMicButton = findViewById(R.id.btn_toggle_mic);
        toggleCameraButton = findViewById(R.id.btn_toggle_camera);
        controlsLayout = findViewById(R.id.controls_layout);
        statusIndicatorTextView = findViewById(R.id.status_indicator_text);
        blackOverlay = findViewById(R.id.blackOverlay);
        // Main publish button
        publishButton.setOnClickListener(v -> {
            if (isPublishing) {
                stopPublish();
            } else {
                startPublish();
            }
        });

        // Switch camera button
        switchCameraButton.setOnClickListener(v -> {
            if (webrtcClient != null) {
                webrtcClient.switchCamera();
                Toast.makeText(this, "Camera switched", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle microphone button
        toggleMicButton.setOnClickListener(v -> {
            if (webrtcClient != null) {
                isMicEnabled = !isMicEnabled;
                webrtcClient.toggleSendAudio(isMicEnabled);

                // Update button text and color
                toggleMicButton.setText(isMicEnabled ? "MIC ON" : "MIC OFF");
                toggleMicButton.setBackgroundTintList(
                        getResources().getColorStateList(
                                isMicEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark,
                                getTheme()
                        )
                );

                Toast.makeText(this, isMicEnabled ? "Microphone enabled" : "Microphone disabled",
                        Toast.LENGTH_SHORT).show();
            }
        });

        toggleCameraButton.setOnClickListener(v -> {
            if (webrtcClient != null) {
                isCameraEnabled = !isCameraEnabled;
                webrtcClient.toggleSendVideo(isCameraEnabled);
                if(isCameraEnabled){
                    blackOverlay.setVisibility(View.GONE);
                }else{
                    blackOverlay.setVisibility(View.VISIBLE);
                }


                toggleCameraButton.setText(isCameraEnabled ? "CAM ON" : "CAM OFF");
                toggleCameraButton.setBackgroundTintList(
                        getResources().getColorStateList(
                                isCameraEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark,
                                getTheme()
                        )
                );


            }
        });

        switchCameraButton.setEnabled(false);
        toggleMicButton.setEnabled(false);
        toggleCameraButton.setEnabled(false);
    }

    private void checkPermissions() {
        if (hasAllPermissions()) {
            initializeWhipClient();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initializeWhipClient() {
        webrtcClient = IRed5WebrtcClient.builder()
                .setActivity(this)
                .setLicenseKey("MQZO-2CI6-XTAZ-6PLX")
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
                .setVideoRenderer(surfaceView)
                .setEventListener(this)
                .build();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void enterPictureInPictureMode() {

        Rational aspectRatio = new Rational(surfaceView.getWidth(), surfaceView.getHeight());

        PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build();

        boolean result = enterPictureInPictureMode(params);
        if (!result) {
            Toast.makeText(this, "Could not enter Picture-in-Picture mode", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        this.isInPictureInPictureMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            // Hide UI controls in PiP mode
            controlsLayout.setVisibility(View.GONE);
            publishButton.setVisibility(View.GONE);
            statusIndicatorTextView.setVisibility(View.GONE);
            Log.d(TAG, "Entered Picture-in-Picture mode");
        } else {
            // Show UI controls when returning from PiP mode
            controlsLayout.setVisibility(View.VISIBLE);
            publishButton.setVisibility(View.VISIBLE);

            statusIndicatorTextView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Exited Picture-in-Picture mode");
        }
    }

    private void startPublish() {
        if (webrtcClient != null && !isPublishing) {
            Log.d(TAG, "Starting publish...");
            webrtcClient.publish(SettingsActivity.getStreamName(this));
            publishButton.setText("PUBLISHING...");
            publishButton.setEnabled(false);
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.blue));
            statusIndicatorTextView.setText("Connecting");
        }
    }

    private void stopPublish() {
        if (webrtcClient != null && isPublishing) {
            Log.d(TAG, "Stopping publish...");
            webrtcClient.stopPublish();
            publishButton.setText("STOPPING...");
            publishButton.setEnabled(false);
        }
    }

    private void enableControlButtons(boolean enable) {
        runOnUiThread(() -> {
            switchCameraButton.setEnabled(enable);
            toggleMicButton.setEnabled(enable);
            toggleCameraButton.setEnabled(enable);

            if (enable) {
                toggleMicButton.setText(isMicEnabled ? "MIC ON" : "MIC OFF");
                toggleMicButton.setBackgroundTintList(
                        getResources().getColorStateList(
                                isMicEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark,
                                getTheme()
                        )
                );

                toggleCameraButton.setText(isCameraEnabled ? "CAM ON" : "CAM OFF");
                toggleCameraButton.setBackgroundTintList(
                        getResources().getColorStateList(
                                isCameraEnabled ? android.R.color.holo_green_dark : android.R.color.holo_red_dark,
                                getTheme()
                        )
                );
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions()) {
                initializeWhipClient();
            } else {
                Toast.makeText(this, "Permissions required for WebRTC", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (webrtcClient != null) {
            webrtcClient.release();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        // Auto-enter PiP mode when user navigates away (if publishing and supported)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPublishing && !isInPictureInPictureMode) {
            enterPictureInPictureMode();
        }
    }

    @Override
    public void onPublishStarted() {
        Log.d(TAG, "Publish started successfully");
        runOnUiThread(() -> {
            isPublishing = true;
            publishButton.setText("STOP PUBLISH");
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
            statusIndicatorTextView.setText("Live");

            publishButton.setEnabled(true);
            publishButton.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.holo_red_dark, getTheme())
            );
            enableControlButtons(true);

        });
    }

    @Override
    public void onPublishStopped() {
        Log.d(TAG, "Publish stopped");
        runOnUiThread(() -> {
            isPublishing = false;
            publishButton.setText("START PUBLISH");
            publishButton.setEnabled(true);
            publishButton.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.holo_blue_dark, getTheme())
            );
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
            statusIndicatorTextView.setText("Disconnected");


            // Exit PiP mode when publishing stops
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
                // The system will automatically exit PiP when the activity finishes
                // or you can call moveTaskToBack(false) to minimize instead
            }
        });
    }

    @Override
    public void onPublishFailed(String error) {
        Log.e(TAG, "Publish failed: " + error);
        runOnUiThread(() -> {
            isPublishing = false;
            publishButton.setText("START PUBLISH");
            publishButton.setEnabled(true);
            publishButton.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.holo_blue_dark, getTheme())
            );
            Toast.makeText(this, "Publish failed: " + error, Toast.LENGTH_LONG).show();
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
            statusIndicatorTextView.setText("Disconnected");
        });
    }

    @Override
    public void onSubscribeStarted() {
        Log.d(TAG, "Subscribe started");
    }

    @Override
    public void onSubscribeStopped() {
        Log.d(TAG, "Subscribe stopped");
    }

    @Override
    public void onSubscribeFailed(String error) {
        Log.e(TAG, "Subscribe failed: " + error);
    }

    @Override
    public void onPreviewStarted() {
        Log.d(TAG, "Preview started");
        runOnUiThread(() -> {
            enableControlButtons(true);
        });
    }

    @Override
    public void onPreviewStopped() {
        Log.d(TAG, "Preview stopped");
        runOnUiThread(() -> {
            enableControlButtons(false);
        });
    }

    @Override
    public void onIceConnectionStateChanged(IRed5WebrtcClient.IceConnectionState state) {
        Log.d(TAG, "ICE connection state: " + state);

        // Handle connection state changes
        if (state == IRed5WebrtcClient.IceConnectionState.CONNECTED) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show();
            });
        } else if (state == IRed5WebrtcClient.IceConnectionState.DISCONNECTED ||
                state == IRed5WebrtcClient.IceConnectionState.FAILED) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show();
            });
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
            statusIndicatorTextView.setText("Disconnected");
        }
    }

    @Override
    public void onConnectionStateChanged(IRed5WebrtcClient.PeerConnectionState state) {
        Log.d(TAG, "Peer connection state: " + state);
    }



    @Override
    public void onError(String error) {
        Log.e(TAG, "General error: " + error);
        runOnUiThread(() -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
        });
    }
}