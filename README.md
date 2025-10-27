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
- Example: `userid-737-7f2a874662.cloud.red5.net`

**For Standalone Server:**
- Use `setServerIp()` with your server IP address
- Use `setServerPort()` if different from default (5080)

```java
IRed5WebrtcClient webrtcClient = IRed5WebrtcClient.builder()
    .setActivity(this)
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
    .build();
```

#### Step 4: Start Preview

When `webrtcClient` is created, it performs a license check. Implement `IRed5WebrtcClient.Red5EventListener` in your activity and override `onLicenseValidated`:

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