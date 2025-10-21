FROM gradle:8.10.2-jdk21 AS builder

# Install dependencies (no 32-bit libs)
RUN apt-get update && apt-get install -y \
    wget unzip curl git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Set Android SDK environment variables
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH

# Download and install Android command line tools
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools && \
    mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install required SDK components
RUN yes | sdkmanager --licenses
RUN sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Set working directory and copy project
WORKDIR /workspace
COPY . /workspace

# Declare a build argument for your service URL
ARG LICENSE_KEY
ARG SM_ENDPOINT
ARG STANDALONE_ENDPOINT
ARG LIB_PATH

# Use Gradle Wrapper to build .aar
RUN export LICENSE_KEY=${LICENSE_KEY} && \
    export SM_ENDPOINT=${SM_ENDPOINT} && \
    export STANDALONE_ENDPOINT=${STANDALONE_ENDPOINT} && \
    cp ${LIB_PATH} app/libs/ && \
    ./gradlew assembleDebug && \
    mkdir -p /workspace/dist && \
    ls -lh /workspace/app/build/outputs/apk/debug && \
    cp -v /workspace/app/build/outputs/apk/debug/*.apk /workspace/dist/ || echo "⚠️ No APKs found!"

# Default command (prints location of AAR)
CMD ["bash", "-c", "echo \"APKs:\" && ls -lh /workspace/dist"]