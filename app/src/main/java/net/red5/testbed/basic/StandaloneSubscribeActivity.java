package net.red5.testbed.basic;


import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;



import net.red5.android.api.IRed5WebrtcClient;
import net.red5.android.core.Red5Renderer;
import net.red5.testbed.R;
import net.red5.testbed.SettingsActivity;

public class StandaloneSubscribeActivity extends AppCompatActivity implements IRed5WebrtcClient.Red5ProWebrtcEventListener {
    private static final String TAG = "StandaloneSubscribeActivity";

    private Red5Renderer surfaceView;
    private Button subscribeButton;
    private TextView statusIndicatorTextView;

    private IRed5WebrtcClient webrtcClient;
    private boolean isSubscribing = false;
    private boolean isInPictureInPictureMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_subscribe);

        initViews();
        initializeSubscribeClient();
    }

    private void initViews() {
        surfaceView = findViewById(R.id.surface_view);
        subscribeButton = findViewById(R.id.btn_subscribe);
        statusIndicatorTextView = findViewById(R.id.status_indicator_text);

        subscribeButton.setOnClickListener(v -> {
            if (isSubscribing) {
                stopSubscribe();
            } else {
                startSubscribe();
            }
        });
    }

    private void initializeSubscribeClient() {
        webrtcClient = IRed5WebrtcClient.builder()
                .setActivity(this)
                .setLicenseKey("MQZO-2CI6-XTAZ-6PLX")
                .setServerIp(SettingsActivity.getStandaloneServerIp(this))
                .setStreamName(SettingsActivity.getStreamName(this))
                .setVideoRenderer(surfaceView)
                .setEventListener(this)

                .build();

        Log.d(TAG, "Subscribe client initialized");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void enterPictureInPictureMode() {


        try {
            Rational aspectRatio = new Rational(surfaceView.getRendererWidth(), surfaceView.getRendererHeight());

            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();


            boolean result = enterPictureInPictureMode(params);
            Log.d(TAG, "PiP mode attempt result: " + result + " with aspect ratio: " + aspectRatio);

            if (!result) {
                Log.e(TAG, "Failed to enter Picture-in-Picture mode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception when entering PiP mode: " + e.getMessage());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        this.isInPictureInPictureMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            statusIndicatorTextView.setVisibility(View.GONE);
            subscribeButton.setVisibility(View.GONE);


            Log.d(TAG, "Entered Picture-in-Picture mode - UI hidden");
        } else {
            statusIndicatorTextView.setVisibility(View.VISIBLE);
            subscribeButton.setVisibility(View.VISIBLE);

            Log.d(TAG, "Exited Picture-in-Picture mode - UI restored");
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        Log.d(TAG, "onUserLeaveHint called. Subscribing: " + isSubscribing + ", SDK: " + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isSubscribing && !isInPictureInPictureMode) {
            Log.d(TAG, "Attempting to enter PiP mode automatically");
            enterPictureInPictureMode();
        } else {
            Log.d(TAG, "PiP conditions not met - SDK: " + Build.VERSION.SDK_INT +
                    ", Subscribing: " + isSubscribing + ", Already in PiP: " + isInPictureInPictureMode);
        }
    }

    private void startSubscribe() {
        if (webrtcClient != null && !isSubscribing) {
            Log.d(TAG, "Starting subscribe...");
            webrtcClient.subscribe(SettingsActivity.getStreamName(this));
            subscribeButton.setText("SUBSCRIBING...");
            subscribeButton.setEnabled(false);
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.blue));
            statusIndicatorTextView.setText("Connecting");
        }
    }

    private void stopSubscribe() {
        if (webrtcClient != null && isSubscribing) {
            Log.d(TAG, "Stopping subscribe...");
            webrtcClient.stopSubscribe();
            subscribeButton.setText("STOPPING...");
            subscribeButton.setEnabled(false);
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

        if (!isInPictureInPictureMode && webrtcClient != null && isSubscribing) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (webrtcClient != null) {

        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!isInPictureInPictureMode) {
            Log.d(TAG, "App stopped, not in PiP mode");
        }
    }

    @Override
    public void onPublishStarted() {
    }

    @Override
    public void onPublishStopped() {
    }

    @Override
    public void onPublishFailed(String error) {
    }

    @Override
    public void onSubscribeStarted() {
        Log.d(TAG, "Subscribe started successfully");
        runOnUiThread(() -> {
            isSubscribing = true;
            subscribeButton.setText("STOP SUBSCRIBE");
            subscribeButton.setEnabled(true);
            subscribeButton.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.holo_red_dark, getTheme())
            );
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
            statusIndicatorTextView.setText("Live");
            Toast.makeText(this, "Subscribing started", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onSubscribeStopped() {
        Log.d(TAG, "Subscribe stopped");
        runOnUiThread(() -> {
            isSubscribing = false;
            subscribeButton.setText("START SUBSCRIBE");
            subscribeButton.setEnabled(true);
            subscribeButton.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.holo_blue_dark, getTheme())
            );
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
            statusIndicatorTextView.setText("Disconnected");
            Toast.makeText(this, "Subscribe stopped", Toast.LENGTH_SHORT).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {

            }
        });
    }

    @Override
    public void onLicenseValidated(boolean validated, String message) {
        if(validated){
            Toast.makeText(this, "License check success",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "License check failed.",Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "License validation status: "+ validated +" message: "+ message);
    }

    @Override
    public void onSubscribeFailed(String error) {
        Log.e(TAG, "Subscribe failed: " + error);
        runOnUiThread(() -> {
            isSubscribing = false;
            subscribeButton.setText("START SUBSCRIBE");
            subscribeButton.setEnabled(true);
            subscribeButton.setBackgroundTintList(
                    getResources().getColorStateList(android.R.color.holo_blue_dark, getTheme())
            );
            statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
            statusIndicatorTextView.setText("Disconnected");
            Toast.makeText(this, "Subscribe failed: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onPreviewStarted() {
        Log.d(TAG, "Preview started");
    }

    @Override
    public void onPreviewStopped() {
        Log.d(TAG, "Preview stopped");
    }

    @Override
    public void onIceConnectionStateChanged(IRed5WebrtcClient.IceConnectionState state) {
        Log.d(TAG, "ICE connection state: " + state);

        if (state == IRed5WebrtcClient.IceConnectionState.CONNECTED) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show();
            });
        } else if (state == IRed5WebrtcClient.IceConnectionState.DISCONNECTED ||
                state == IRed5WebrtcClient.IceConnectionState.FAILED) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show();
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
                statusIndicatorTextView.setText("Disconnected");
            });
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
