# Red5 Android WebRTC SDK Testbed(Example App)

## Introduction

Build low-latency live streaming apps with the Red5 Android WebRTC SDK. Stream video using WebRTC, and subscribe (play) streams via WebRTC. Compatible with both Red5Pro Cloud (Stream Manager) and standalone Red5Pro servers.

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Configuration Notes](#configuration-notes)
4. [Requirements](#requirements)
5. [Quick Start](#quick-start)
6. [Usage](#usage)
   - 6.1 [Publishing to Red5 Cloud and Standalone with WebRTC](#publishing-to-red5-cloud-and-standalone-with-webrtc)
      - 6.1.1 [Step 1: Create an activity with Red5Renderer](#step-1-create-an-activity-with-red5renderer)
      - 6.1.2 [Step 2: Request Publish Permissions](#step-2-request-publish-permissions)
      - 6.1.3 [Step 3: Create Red5WebrtcClient object with IRed5WebrtcClient.builder()](#step-3-create-red5webrtcclient-object-with-ired5webrtcclientbuilder)
      - 6.1.4 [Step 4: Start Preview](#step-4-start-preview)
      - 6.1.5 [Step 5: Start Publishing](#step-5-start-publishing)
   - 6.2 [Subscribing to Red5 Cloud and Standalone Streams with WebRTC](#subscribing-to-red5-cloud-and-standalone-streams-with-webrtc)
      - 6.2.1 [Step 1: Create an activity with Red5Renderer](#step-1-create-an-activity-with-red5renderer-1)
      - 6.2.2 [Step 2: Create Red5WebrtcClient object with IRed5WebrtcClient.builder()](#step-2-create-red5webrtcclient-object-with-ired5webrtcclientbuilder)
      - 6.2.3 [Step 3: Start Subscribing](#step-3-start-subscribing)
7. [Listening For Events](#listening-for-events)
   - 7.1 [Event Types](#event-types)
   - 7.2 [Connection State Handling](#connection-state-handling)
   - 7.3 [Full Working Example](#full-working-example)
8. [Advanced Usage](#advanced-usage)
   - 8.1 [Turn Off/On Camera](#turn-offon-camera)
   - 8.2 [Switch Camera](#switch-camera)
   - 8.3 [Mute/Unmute Microphone](#muteunmute-microphone)
   - 8.4 [Picture in Picture (PiP) Mode](#picture-in-picture-pip-mode)
9. [Chat Integration](#chat-integration)
   - 9.1 [Chat Overview](#chat-overview)
   - 9.2 [Chat Setup](#chat-setup)
   - 9.3 [Chat Operations](#chat-operations)
   - 9.4 [Listening for Chat Events](#listening-for-chat-events)
   - 9.5 [Complete Example](#complete-example)
10. [Conferencing](#conferencing)
- 10.1 [Joining a Conference Room](#joining-a-conference-room)
- 10.2 [Leaving a Conference Room](#leaving-a-conference-room)
- 10.3 [Listening for Conference Events](#listening-for-conference-events)
- 10.4 [Complete Example](#complete-example-1)
11. [Stats Collector](#stats-collector)
- 11.1 [Overview](#overview)
- 11.2 [Enabling Stats Collection](#enabling-stats-collection)
- 11.3 [Receiving Stats](#receiving-stats)
- 11.4 [Available Statistics](#available-statistics)
- 11.5 [Audio Levels](#audio-levels)
- 11.6 [Conference Stats](#conference-stats)
- 11.7 [Complete Example](#complete-example-2)

## Installation

Place .aar SDK file to app/libs folder.
Add dependencies to your apps build.gradle as follows:
```xml
// Red5 WebRTC SDK Dependencies
implementation fileTree(include: ['*.aar'], dir: 'libs')
implementation 'androidx.annotation:annotation:1.9.1'
implementation 'com.google.code.gson:gson:2.13.2'
implementation 'com.squareup.okhttp3:okhttp:5.1.0'
implementation 'io.github.webrtc-sdk:android:137.7151.03'
implementation 'com.pubnub:pubnub-gson:11.0.0'
```

## Configuration Notes

Ensure you have the necessary permissions in your `AndroidManifest.xml` for publishing:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

```

## Requirements

- Red5Pro SDK license key
- Camera and microphone permissions for publishing
- Internet permission for both publishing and subscribing

## Quick Start

```java
// Publishing/subscribing to cloud. For standalone, use setServerIp() and setServerPort() instead of setStreamManagerHost()
IRed5WebrtcClient webrtcClient = IRed5WebrtcClient.builder()
    .setActivity(this)
    .setLicenseKey(YOUR_SDK_LICENSE_KEY)
    .setStreamManagerHost(YOUR_STREAM_MANAGER_HOST_ADDRESS) //e.g. "userid-737-7f2a874662.cloud.red5.net"
    .setUserName(USERNAME_IF_USERNAME_PASS_AUTH_ENABLED)
    .setPassword(PASSWORD_IF_USERNAME_PASS_AUTH_ENABLED)
    .setToken(AUTH_TOKEN_IF_ENABLED)
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

// Publish a stream
webrtcClient.publish("myStream");

// Subscribe to a stream
webrtcClient.subscribe("myStream");
```

## Usage

The SDK supports both publishing to Red5 Cloud (Stream Manager deployments) and standalone Red5Pro servers, as well as subscription (playback) with WebRTC for all streams on both cloud and standalone deployments.

**To publish:** Request camera/microphone permissions, create a `Red5WebrtcClient` using `IRed5WebrtcClient.builder()`, and call `webrtcClient.publish(YOUR_STREAM_NAME)`.

**To subscribe:** Create a `Red5WebrtcClient` using `IRed5WebrtcClient.builder()` and call `webrtcClient.subscribe(YOUR_STREAM_NAME)`.

### Publishing to Red5 Cloud and Standalone with WebRTC

#### Step 1: Create an activity with Red5Renderer

Set up your activity and add a `Red5Renderer` to your layout for preview display. This is an extension of WebRTC's `SurfaceViewRenderer`.

**Java:**
```java
private Red5Renderer surfaceView;
```

**XML Layout:**
```xml
<net.red5.android.core.Red5Renderer
    android:id="@+id/surface_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_centerInParent="true" />
```

#### Step 2: Request Publish Permissions

These permissions are required for publishing:

```java
private static final String[] REQUIRED_PERMISSIONS = {
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
};

private void checkPermissions() {
    if (hasAllPermissions()) {
        createWebrtcClient();
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
```

#### Step 3: Create Red5WebrtcClient object with IRed5WebrtcClient.builder()

Create the WebRTC client when publish permissions are granted. This single object handles all streaming configuration.

**For Red5 Cloud (Stream Manager):**
- Use `setStreamManagerHost()` with your stream manager host address
- Example: `userid-000-xxxxxxxxxx.cloud.red5.net`

**For Standalone Server:**
- Use `setServerIp()` with your server IP address
- Use `setServerPort()` if different from default (5080)

Kotlin:
```kotlin
val webrtcClient: IRed5WebrtcClient? = IRed5WebrtcClient.builder()
    .setActivity(this.requireActivity())
    .setLicenseKey(YOUR_SDK_LICENSE_KEY)
    .setStreamManagerHost("userid-737-7f2a874662.cloud.red5.net") // For cloud
    // .setServerIp("192.168.1.100") // For standalone
    // .setServerPort(5080) // For standalone (optional, default is 5080)
    .setUserName(USERNAME_IF_USERNAME_PASS_AUTH_ENABLED)
    .setPassword(PASSWORD_IF_USERNAME_PASS_AUTH_ENABLED)
    .setToken(AUTH_TOKEN_IF_ENABLED)
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
```

Java:
```java
IRed5WebrtcClient webrtcClient = IRed5WebrtcClient.builder()
    .setActivity(this)
    .setLicenseKey(YOUR_SDK_LICENSE_KEY)
    .setStreamManagerHost("userid-000-xxxxxxxxxx.cloud.red5.net") // For cloud
    // .setServerIp("192.168.1.100") // For standalone
    // .setServerPort(5080) // For standalone (optional, default is 5080)
    .setUserName(USERNAME_IF_USERNAME_PASS_AUTH_ENABLED)
    .setPassword(PASSWORD_IF_USERNAME_PASS_AUTH_ENABLED)
    .setToken(AUTH_TOKEN_IF_ENABLED)
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
```

#### Step 4: Start Preview

When `webrtcClient` is created, it performs a license check. Implement `IRed5WebrtcClient.Red5EventListener` in your activity and override `onLicenseValidated`:

Kotlin:
```kotlin
override fun onLicenseValidated(validated: Boolean, message: String?) {
    if (validated) {
        renderer.startPreview()
        Toast.makeText(this.requireContext(), "License check success", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(this.requireContext(), "License check failed: " + message, Toast.LENGTH_SHORT).show()
    }
}
```

Java:

```java
@Override
public void onLicenseValidated(boolean validated, String message) {
    if (validated) {
        webrtcClient.startPreview();
        Toast.makeText(this, "License check success", Toast.LENGTH_SHORT).show();
    } else {
        Toast.makeText(this, "License check failed: " + message, Toast.LENGTH_SHORT).show();
    }
}
```

After successful validation, call `webrtcClient.startPreview()` to see the camera preview rendering on the surface view.

#### Step 5: Start Publishing

Call `webrtcClient.publish(YOUR_STREAM_NAME)` to start publishing:

```java
webrtcClient.publish("myStreamName");
```

### Subscribing to Red5 Cloud and Standalone Streams with WebRTC

#### Step 1: Create an activity with Red5Renderer

Set up your activity and add a `Red5Renderer` to your layout to play the stream:

**Java:**
```java
private Red5Renderer surfaceView;
```

**XML Layout:**
```xml
<net.red5.android.core.Red5Renderer
    android:id="@+id/surface_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_centerInParent="true" />
```

#### Step 2: Create Red5WebrtcClient object with IRed5WebrtcClient.builder()

Configure the client for subscription. Use the same host configuration as publishing:

```java
IRed5WebrtcClient webrtcClient = IRed5WebrtcClient.builder()
    .setActivity(this)
    .setLicenseKey(YOUR_SDK_LICENSE_KEY)
    .setStreamManagerHost("userid-737-7f2a874662.cloud.red5.net") // For cloud
    // .setServerIp("192.168.1.100") // For standalone
    // .setServerPort(5080) // For standalone (optional)
    .setUserName(USERNAME_IF_USERNAME_PASS_AUTH_ENABLED)
    .setPassword(PASSWORD_IF_USERNAME_PASS_AUTH_ENABLED)
    .setToken(AUTH_TOKEN_IF_ENABLED)
    .setVideoRenderer(surfaceView)
    .setEventListener(this)
    .build();
```

#### Step 3: Start Subscribing

Call `webrtcClient.subscribe(YOUR_STREAM_NAME)` to start subscribing:

```java
webrtcClient.subscribe("myStreamName");
```

## Listening For Events

Implement `IRed5WebrtcClient.Red5EventListener` in your activity to handle SDK events.

### Event Types

The SDK emits the following events:

```java
void onPublishStarted();
void onPublishStopped();
void onPublishFailed(String error);
void onSubscribeStarted();
void onSubscribeStopped();
void onSubscribeFailed(String error);
void onIceConnectionStateChanged(IceConnectionState state);
void onConnectionStateChanged(PeerConnectionState state);
void onError(String error);
void onPreviewStarted();
void onPreviewStopped();
void onLicenseValidated(boolean validated, String message);
```

### Connection State Handling

Handle connection state changes using `IRed5WebrtcClient.IceConnectionState`:

```java
@Override
public void onIceConnectionStateChanged(IRed5WebrtcClient.IceConnectionState state) {
    switch (state) {
        case CONNECTED:
            runOnUiThread(() -> {
                Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show();
            });
            break;
        case DISCONNECTED:
        case FAILED:
            runOnUiThread(() -> {
                Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show();
            });
            break;
        default:
            break;
    }
}
```

### Full Working Example

*// Link to testbed example to be added*

## Advanced Usage

### Turn Off/On Camera

Toggle camera on/off during streaming:

```java
private boolean isCameraEnabled = true;

private void toggleCamera() {
    isCameraEnabled = !isCameraEnabled;
    webrtcClient.toggleSendVideo(isCameraEnabled);
}
```

### Switch Camera

Switch between front and back cameras:

```java
private void switchCamera() {
    webrtcClient.switchCamera();
}
```

### Mute/Unmute Microphone

Toggle microphone on/off during streaming:

```java
private boolean isMicEnabled = true;

private void toggleMic() {
    isMicEnabled = !isMicEnabled;
    webrtcClient.toggleSendAudio(isMicEnabled);
}
```

### Picture in Picture (PiP) Mode

Red5 SDK fully supports PiP mode for both publishing and subscribing, providing a better streaming experience when your application is backgrounded.

**Auto-enter PiP mode when user navigates away:**

```java
@Override
protected void onUserLeaveHint() {
    super.onUserLeaveHint();
    
    // Auto-enter PiP mode when user navigates away (if publishing and supported)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && 
        isPublishing && 
        !isInPictureInPictureMode()) {
        enterPictureInPictureMode();
    }
}
```

**Manually enter PiP mode:**

```java
@TargetApi(Build.VERSION_CODES.O)
public void enterPictureInPictureMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Rational aspectRatio = new Rational(surfaceView.getWidth(), surfaceView.getHeight());
        
        PictureInPictureParams params = new PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build();
        
        boolean result = enterPictureInPictureMode(params);
        if (!result) {
            Toast.makeText(this, "Could not enter Picture-in-Picture mode", Toast.LENGTH_SHORT).show();
        }
    }
}
```

## Chat Integration

The Red5 Android WebRTC SDK includes built-in chat functionality, allowing you to build infinitely scalable chat applications alongside your live streaming features.

### Chat Overview

The chat system uses a channel-based architecture where users can:
- Subscribe to multiple chat channels simultaneously
- Send and receive text or JSON messages in real-time
- Share metadata with messages for additional context
- Scale to handle unlimited concurrent users and messages

### Chat Setup

#### Configure Chat Credentials

When building your `Red5WebrtcClient`, include your PubNub publish and subscribe keys. Get those from your red5 cloud panel:
```java
IRed5WebrtcClient webrtcClient = IRed5WebrtcClient.builder()
    .setActivity(this)
    .setLicenseKey(YOUR_SDK_LICENSE_KEY)
    // A unique chat user id.
    // If auth is enabled for chat, You must send this userId to your application backend for token generation.
    .setChatUserId(USER_ID)

    // If chat authentication is enabled, set the token received from your backend server.
    // Use red5 backend sdks to generate chat tokens: https://github.com/red5pro/red5-bcs-node
    // Chat tokens can be also updated after client is initialized.
    //.setChatToken("")
    .setPubnubPublishKey(YOUR_PUBNUB_PUBLISH_KEY)
    .setPubnubSubscribeKey(YOUR_PUBNUB_SUBSCRIBE_KEY)
    .setEventListener(this)
    .build();
```

#### Subscribe to a Channel

Subscribe to a chat channel after license validation. The connection is established automatically upon subscription:
```java
@Override
public void onLicenseValidated(boolean validated, String message) {
    if (validated) {
        String channelName = "my-chat-room";
        webrtcClient.subscribeChatChannel(channelName);
    }
}
```

### Chat Operations

#### Send Text Messages

Send plain text messages to a channel:
```java
String channelName = "my-chat-room";
String message = "Hello, everyone!";
Object metadata = null; // Optional metadata

webrtcClient.sendChatTextMessage(channelName, message, metadata);
```

#### Send JSON Messages

Send structured JSON messages for more complex data:
```java
JsonObject jsonMessage = new JsonObject();
jsonMessage.addProperty("text", "Hello!");
jsonMessage.addProperty("userName", "John");
jsonMessage.addProperty("timestamp", System.currentTimeMillis());

Object metadata = Map.of("sender", "John", "type", "greeting");

webrtcClient.sendChatJsonMessage(channelName, jsonMessage, metadata);
```

#### Unsubscribe from a Channel

Stop receiving messages from a channel:
```java
webrtcClient.unsubscribeChatChannel(channelName);
```

#### Get Subscribed Channels

Retrieve a list of all currently subscribed channels:
```java
List<String> channels = webrtcClient.getSubscribedChatChannels();
```

#### Disconnect Chat

Disconnect from all chat channels:
```java
webrtcClient.disconnectChat();
```

#### Destroy Chat

Completely destroy the chat client and release resources:
```java
webrtcClient.destroyChat();
```
Using .release() would also destroy chat.

### Listening for Chat Events

Implement the chat-related callbacks in `IRed5WebrtcClient.Red5EventListener`:
```java
@Override
public void onChatConnected() {
    // Called when successfully connected to PubNub
    Toast.makeText(this, "Chat connected", Toast.LENGTH_SHORT).show();
}

@Override
public void onChatDisconnected() {
    // Called when disconnected from PubNub
    Toast.makeText(this, "Chat disconnected", Toast.LENGTH_SHORT).show();
}

@Override
public void onChatMessageReceived(String channel, JsonElement message) {
    // Called when a message is received on a subscribed channel
    if (message != null && message.isJsonObject()) {
        JsonObject jsonObject = message.asJsonObject();
        String text = jsonObject.get("text").getAsString();
        String userName = jsonObject.get("userName").getAsString();
        
        Log.d("Chat", "Message from " + userName + ": " + text);
        // Update your UI with the new message
    }
}

@Override
public void onChatSendSuccess(String channel, Long timetoken) {
    // Called when a message is successfully sent
    Log.d("Chat", "Message sent successfully with timetoken: " + timetoken);
}

@Override
public void onChatSendError(String channel, String errorMessage) {
    // Called when message sending fails
    Toast.makeText(this, "Failed to send message: " + errorMessage, Toast.LENGTH_SHORT).show();
}
```

### Complete Example

For a complete working implementation of chat functionality, refer to the `ChatActivity` class in the testbed source code.

## Conferencing

The Red5 Android WebRTC SDK provides infinitely scalable real-time conferencing capabilities, allowing you to build applications similar to Google Meet or Zoom. Conference rooms support multiple participants with different roles (publishers and subscribers), real-time media management, and automatic participant handling.

**Note:** Conferencing requires Red5 Cloud (Stream Manager) and does not work with standalone servers.

### Joining a Conference Room

#### Step 1: Initialize Red5WebrtcClient with Conference Listener

Create your WebRTC client and set a conference listener to handle conference events:
```java
private IRed5WebrtcClient red5Client;
private IRed5WebrtcClient.ConferenceListener conferenceListener;

private void initSdk() {
    conferenceListener = new IRed5WebrtcClient.ConferenceListener() {
        @Override
        public void onJoinRoomSuccess(String roomId, ArrayList participants) {
            // Successfully joined the conference room
            Log.d(TAG, "Joined room: " + roomId + " with " + participants.size() + " participants");
        }

        @Override
        public void onJoinRoomFailed(int statusCode, String message) {
            // Failed to join conference room
            Log.e(TAG, "Join failed: " + message);
        }

        @Override
        public void onParticipantJoined(String uid, String role, String metaData, 
                                       boolean videoEnabled, boolean audioEnabled, 
                                       Red5Renderer renderer) {
            // A new participant joined the room
            Log.d(TAG, "Participant joined: " + uid + " (role: " + role + ")");
        }

        @Override
        public void onParticipantLeft(String uid) {
            // A participant left the room
            Log.d(TAG, "Participant left: " + uid);
        }

        @Override
        public void onParticipantMediaUpdate(String uid, boolean videoEnabled, 
                                            boolean audioEnabled, long timestamp) {
            // Participant toggled their camera or microphone
            Log.d(TAG, "Participant " + uid + " - video: " + videoEnabled + ", audio: " + audioEnabled);
        }
    };

    red5Client = IRed5WebrtcClient.builder()
        .setActivity(this)
        .setLicenseKey(YOUR_SDK_LICENSE_KEY)
        .setStreamManagerHost(YOUR_STREAM_MANAGER_HOST) // e.g. "userid-737-7f2a874662.cloud.red5.net"
        .setVideoEnabled(true)
        .setAudioEnabled(true)
        .setVideoWidth(1280)
        .setVideoHeight(720)
        .setVideoFps(30)
        .setVideoBitrate(1500)
        .setVideoSource(IRed5WebrtcClient.StreamSource.FRONT_CAMERA)
        .setVideoRenderer(localVideoRenderer)
        .setEventListener(this)
        .setConferenceListener(conferenceListener)  // Set conference listener
        .build();
}
```

#### Step 2: Join the Room

Call `join()` with your conference parameters:
```java
String roomId = "my-conference-room";
String userId = "john_" + System.currentTimeMillis();
String token = ""; // Optional authentication token
String role = "publisher"; // "publisher" or "subscriber"
String metaData = "{\"name\":\"John Doe\"}"; // Optional JSON metadata

red5Client.join(roomId, userId, token, role, metaData);
```

**Roles:**
- **Publisher**: Can send audio/video and receive from others. Full participation in the conference.
- **Subscriber**: Receive-only mode. Can see/hear other participants but doesn't send media.

### Leaving a Conference Room

Simply call `release()` to leave the conference and clean up resources:
```java
red5Client.release();
```

### Listening for Conference Events

The `ConferenceListener` interface provides callbacks for all conference-related events:

#### onJoinRoomSuccess
Called when you successfully join a conference room.
```java
@Override
public void onJoinRoomSuccess(String roomId, ArrayList participants) {
    runOnUiThread(() -> {
        Toast.makeText(this, "Joined room: " + roomId, Toast.LENGTH_SHORT).show();
        
        // Update UI with initial participant count
        updateParticipantCount(participants.size());
    });
}
```

#### onJoinRoomFailed
Called when joining a conference room fails.
```java
@Override
public void onJoinRoomFailed(int statusCode, String message) {
    runOnUiThread(() -> {
        Toast.makeText(this, "Failed to join: " + message, Toast.LENGTH_LONG).show();
    });
}
```

#### onParticipantJoined
Called when a new participant joins the room. This is where you receive their video renderer.
```java
@Override
public void onParticipantJoined(String uid, String role, String metaData,
                               boolean videoEnabled, boolean audioEnabled,
                               Red5Renderer renderer) {
    runOnUiThread(() -> {
        // Add participant to your UI
        if (renderer != null) {
            // Add renderer to your layout to display participant's video
            participantContainer.addView(renderer);
        }
        
        // Update UI with participant info
        Log.d(TAG, "New participant: " + uid);
        updateParticipantList();
    });
}
```

**Parameters:**
- `uid`: Unique identifier for the participant
- `role`: Participant's role ("publisher" or "subscriber")
- `metaData`: Custom JSON metadata provided when joining
- `videoEnabled`: Whether participant's camera is on
- `audioEnabled`: Whether participant's microphone is on
- `renderer`: Red5Renderer to display participant's video (null for subscribers)

#### onParticipantLeft
Called when a participant leaves the room.
```java
@Override
public void onParticipantLeft(String uid) {
    runOnUiThread(() -> {
        // Remove participant from your UI
        removeParticipantFromUI(uid);
        
        // Update participant count
        updateParticipantList();
    });
}
```

#### onParticipantMediaUpdate
Called when a participant toggles their camera or microphone.
```java
@Override
public void onParticipantMediaUpdate(String uid, boolean videoEnabled,
                                    boolean audioEnabled, long timestamp) {
    runOnUiThread(() -> {
        // Update UI to show participant's media state
        updateParticipantMediaState(uid, videoEnabled, audioEnabled);
        
        // For example, show/hide camera icon or mute indicator
        if (!videoEnabled) {
            showCameraOffIndicator(uid);
        }
        if (!audioEnabled) {
            showMutedIndicator(uid);
        }
    });
}
```

### Complete Example

For a comprehensive implementation of conferencing with pagination, role management, and Picture-in-Picture support, see the `ConferenceActivity` class in the testbed example project. This demonstrates:

- Publisher and subscriber role handling
- Dynamic participant grid with pagination
- Real-time media state updates
- Conference UI management
- Picture-in-Picture mode for conferences

The complete source code is available in the example application.

## Stats Collector

The Red5 Android WebRTC SDK includes a comprehensive stats collection system that provides real-time metrics about your WebRTC connections. This is essential for monitoring stream quality, diagnosing network issues, and building quality indicators in your UI.

### Overview

The Stats Collector automatically gathers WebRTC statistics every 2 seconds (configurable) and provides:

- **Network metrics**: Bitrates, packet loss, RTT (round-trip time), jitter
- **Media metrics**: Frame rates, resolution, bytes transferred
- **Quality metrics**: Frames dropped, freeze counts, pause durations
- **Audio levels**: Real-time microphone and participant volume levels
- **Per-participant stats**: Individual metrics for each conference participant
- **System metrics**: Memory usage, CPU usage

### Enabling Stats Collection

Enable stats collection when building your `Red5WebrtcClient`:

```java
IRed5WebrtcClient webrtcClient = IRed5WebrtcClient.builder()
    .setActivity(this)
    .setLicenseKey(YOUR_SDK_LICENSE_KEY)
    .setStreamManagerHost(YOUR_STREAM_MANAGER_HOST)
    .setVideoEnabled(true)
    .setAudioEnabled(true)
    .setVideoRenderer(surfaceView)
    .setEventListener(this)
    // Enable stats collection by default its enabled
    .setStatsCollectorEnabled(true)
    // Optional: Set polling interval (default is 2000ms)
    .setStatsPollingIntervalMs(2000)
    .build();
```

### Receiving Stats

Implement the `onRtcStats()` callback in your `Red5EventListener`:

```java
@Override
public void onRtcStats(RTCStats stats) {
    // Called every 2 seconds (or your configured interval) with updated stats
    Log.d(TAG, "TX Bitrate: " + stats.txKBitRate + " kbps");
    Log.d(TAG, "RX Bitrate: " + stats.rxKBitRate + " kbps");
    Log.d(TAG, "Packet Loss: " + stats.rxPacketLossRate + "%");
}
```

### Available Statistics

The `RTCStats` object provides comprehensive metrics:

#### Connection Duration
```java
int totalDuration = stats.totalDuration; // Total call duration in seconds
int users = stats.users; // Number of users in call/conference
```

#### Bitrate Metrics (in kbps)
```java
// Transmit (upload) bitrates
int txKBitRate = stats.txKBitRate;       // Total TX bitrate
int txAudioKBitRate = stats.txAudioKBitRate; // Audio TX bitrate
int txVideoKBitRate = stats.txVideoKBitRate; // Video TX bitrate

// Receive (download) bitrates
int rxKBitRate = stats.rxKBitRate;       // Total RX bitrate
int rxAudioKBitRate = stats.rxAudioKBitRate; // Audio RX bitrate
int rxVideoKBitRate = stats.rxVideoKBitRate; // Video RX bitrate
```

#### Bytes Transferred
```java
// Transmit (upload) bytes - cumulative
int txBytes = stats.txBytes;             // Total TX bytes
int txAudioBytes = stats.txAudioBytes;   // Audio TX bytes
int txVideoBytes = stats.txVideoBytes;   // Video TX bytes

// Receive (download) bytes - cumulative
int rxBytes = stats.rxBytes;             // Total RX bytes
int rxAudioBytes = stats.rxAudioBytes;   // Audio RX bytes
int rxVideoBytes = stats.rxVideoBytes;   // Video RX bytes
```

#### Network Quality Metrics
```java
int lastmileDelay = stats.lastmileDelay;     // RTT to server in ms
int gatewayRtt = stats.gatewayRtt;           // Gateway RTT in ms
int txPacketLossRate = stats.txPacketLossRate; // TX packet loss % (0-100)
int rxPacketLossRate = stats.rxPacketLossRate; // RX packet loss % (0-100)
```

#### System Metrics
```java
double cpuTotalUsage = stats.cpuTotalUsage;           // System CPU usage %
double cpuAppUsage = stats.cpuAppUsage;               // App CPU usage %
double memoryAppUsageRatio = stats.memoryAppUsageRatio;     // App memory %
double memoryTotalUsageRatio = stats.memoryTotalUsageRatio; // System memory %
int memoryAppUsageInKbytes = stats.memoryAppUsageInKbytes;  // App memory in KB
```

### Audio Levels

The Stats Collector provides real-time audio level monitoring, perfect for building "who is talking" indicators in your UI.

#### Local Audio Level

Monitor your own microphone input level:

```java
@Override
public void onRtcStats(RTCStats stats) {
    // Local microphone level (0.0 = silence, 1.0 = maximum)
    double micLevel = stats.localAudioLevel;

    // Show visual indicator when speaking
    if (micLevel > 0.05) {
        // User is speaking - show microphone activity indicator
        showMicrophoneActivity();
    } else {
        hideMicrophoneActivity();
    }
}
```

**Audio Level Ranges:**
- `0.0`: Complete silence
- `0.01 - 0.05`: Very quiet / background noise
- `0.05 - 0.15`: Normal conversation (most common)
- `0.15 - 0.3`: Louder talking
- `0.3 - 1.0`: Very loud / shouting

**Recommended threshold for voice activity detection:** `0.02 - 0.05`
Ranges may vary, so test before deciding.

#### Remote Participant Audio Levels

In conference mode, monitor each participant's audio level:

```java
@Override
public void onRtcStats(RTCStats stats) {
    // Iterate through all remote participants
    for (Map.Entry<String, RemoteParticipantStats> entry : stats.participantStats.entrySet()) {
        String participantId = entry.getKey();
        RemoteParticipantStats pStats = entry.getValue();

        // Get participant's audio level (0.0 - 1.0)
        double audioLevel = pStats.audioLevel;

        // Update UI to show who is speaking
        if (audioLevel > 0.05) {
            highlightSpeakingParticipant(participantId);
        } else {
            removeSpeakingHighlight(participantId);
        }
    }
}
```

### Conference Stats

When in conference mode, the Stats Collector automatically provides per-participant statistics, allowing you to monitor the quality of each individual participant's stream.

#### Accessing Per-Participant Stats

```java
@Override
public void onRtcStats(RTCStats stats) {
    
    // Iterate through each participant
    for (Map.Entry<String, RemoteParticipantStats> entry : stats.participantStats.entrySet()) {
        String participantUid = entry.getKey();
        RemoteParticipantStats pStats = entry.getValue();

        Log.d(TAG, "=== Participant: " + participantUid + " ===");
        Log.d(TAG, "Audio Level: " + pStats.audioLevel);
        Log.d(TAG, "RX Bitrate: " + pStats.rxKBitRate + " kbps");
        Log.d(TAG, "Packet Loss: " + pStats.packetLossRate + "%");
        Log.d(TAG, "RTT: " + pStats.rtt + " ms");
    }
}
```

#### RemoteParticipantStats Fields

Each participant has the following statistics:

**Participant Identification:**
```java
String participantId = pStats.participantId; // Participant's unique ID
```

**Audio Metrics:**
```java
double audioLevel = pStats.audioLevel;              // Volume level (0.0 - 1.0)
long rxAudioBytes = pStats.rxAudioBytes;            // Audio bytes received
int rxAudioKBitRate = pStats.rxAudioKBitRate;       // Audio bitrate in kbps
long concealmentEvents = pStats.concealmentEvents;  // Audio packet loss concealment
long concealedSamples = pStats.concealedSamples;    // Concealed audio samples
```

**Video Metrics:**
```java
long rxVideoBytes = pStats.rxVideoBytes;            // Video bytes received
int rxVideoKBitRate = pStats.rxVideoKBitRate;       // Video bitrate in kbps
long framesReceived = pStats.framesReceived;        // Total frames received
long framesDropped = pStats.framesDropped;          // Frames dropped
long framesDecoded = pStats.framesDecoded;          // Frames decoded
```

**Network Quality:**
```java
int rxKBitRate = pStats.rxKBitRate;                 // Total bitrate in kbps
long rxBytes = pStats.rxBytes;                      // Total bytes received
int packetLossRate = pStats.packetLossRate;         // Packet loss % (0-100)
int rtt = pStats.rtt;                               // Round-trip time in ms
double jitter = pStats.jitter;                      // Jitter in seconds
```

**Video Quality Indicators:**
```java
long freezeCount = pStats.freezeCount;              // Number of video freezes
double totalFreezesDuration = pStats.totalFreezesDuration; // Total freeze time (seconds)
long pauseCount = pStats.pauseCount;                // Number of pauses
double totalPausesDuration = pStats.totalPausesDuration;   // Total pause time (seconds)
String decoderImplementation = pStats.decoderImplementation; // Video decoder name
```


