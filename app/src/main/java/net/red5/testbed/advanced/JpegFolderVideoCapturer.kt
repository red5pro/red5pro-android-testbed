package net.red5.testbed.advanced

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import net.red5.android.api.Red5CustomVideoCapturer
import org.webrtc.CapturerObserver
import org.webrtc.JavaI420Buffer
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoFrame
import java.io.File
import java.io.FilenameFilter
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * A [CustomVideoCapturer] that reads JPEG files from a directory and streams them
 * as video frames in alphabetical filename order.
 *
 * Uses a producer-consumer design: a dedicated producer thread continuously decodes
 * JPEGs and pushes them into a small bounded queue. The frame-delivery thread pops
 * from that queue at the configured FPS. Only [QUEUE_CAPACITY] decoded frames are
 * held in memory at any time.
 *
 * @param folderPath  Absolute path to the directory containing `.jpg` / `.jpeg` files.
 * @param mode        [PlaybackMode.LOOP] to replay from the start when all files are
 *                    exhausted, or [PlaybackMode.HOLD_LAST] to send the last frame
 *                    indefinitely once the single pass is complete.
 */
class JpegFolderVideoCapturer(
    private var folderPath: String,
    private var mode: PlaybackMode = PlaybackMode.LOOP,
    private var forcedFps: Int = 0
) : Red5CustomVideoCapturer() {

    enum class PlaybackMode {
        /** Replay from the beginning after the last file is queued. */
        LOOP,
        /** Decode every file once, then hold the last frame indefinitely. */
        HOLD_LAST
    }

    private data class I420Frame(
        val y: ByteBuffer,
        val u: ByteBuffer,
        val v: ByteBuffer,
        val width: Int,
        val height: Int,
        val uvStride: Int
    )

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val frameQueue = LinkedBlockingQueue<I420Frame>(QUEUE_CAPACITY)

    @Volatile private var lastSentFrame: I420Frame? = null
    @Volatile private var producerRunning = false

    private var targetWidth = 0
    private var targetHeight = 0
    private var targetFps = 15

    /** Reused scratch buffer for ARGB pixels — avoids per-frame heap churn. */
    private var pixelScratch: IntArray = IntArray(0)

    private var producerExecutor: ExecutorService? = null
    private var consumerExecutor: ScheduledExecutorService? = null

    // -------------------------------------------------------------------------
    // VideoCapturer overrides
    // -------------------------------------------------------------------------

    override fun initialize(
        surfaceTextureHelper: SurfaceTextureHelper?,
        context: Context?,
        capturerObserver: CapturerObserver?
    ) {
        super.initialize(surfaceTextureHelper, context, capturerObserver)
        Log.d(TAG, "Initialized — folder=$folderPath  mode=$mode")
    }

    override fun startCapture(width: Int, height: Int, framerate: Int) {
        // Do NOT call super.startCapture() — we push raw I420 frames via writeFrame().
        targetWidth = width
        targetHeight = height
        targetFps = when {
            forcedFps in 1..30 -> forcedFps
            framerate > 0      -> framerate
            else               -> 15
        }

        // Pre-allocate scratch buffer once for this resolution.
        pixelScratch = IntArray(width * height)

        val intervalMs = 1000L / targetFps

        producerRunning = true
        frameQueue.clear()
        lastSentFrame = null

        producerExecutor = Executors.newSingleThreadExecutor()
        consumerExecutor = Executors.newSingleThreadScheduledExecutor()

        producerExecutor!!.execute(::producerLoop)
        // scheduleAtFixedRate: next tick fires from the START of the previous tick,
        // keeping the delivery rate stable even when individual ticks run slightly long.
        consumerExecutor!!.scheduleAtFixedRate(
            ::consumerTick, 0, intervalMs, TimeUnit.MILLISECONDS
        )

        Log.d(TAG, "startCapture ${width}x${height} @ $targetFps fps  mode=$mode")
    }

    @Throws(InterruptedException::class)
    override fun stopCapture() {
        producerRunning = false
        shutdownExecutors()
        frameQueue.clear()
        super.stopCapture()
    }

    /**
     * Stops the producer/consumer threads without notifying the WebRTC layer.
     * Use this between publish sessions when the SDK is preserving the video track
     * for reuse — calling stopCapture() would invalidate the capturer observer.
     */
    fun stopProducer() {
        producerRunning = false
        shutdownExecutors()
        frameQueue.clear()
    }

    /**
     * Updates the source parameters and restarts the producer/consumer threads.
     * The WebRTC capturer observer is left untouched so the existing video track
     * continues to receive frames on the next publish session.
     */
    fun restart(newPath: String, newMode: PlaybackMode, newFps: Int) {
        stopProducer()
        folderPath = newPath
        mode = newMode
        forcedFps = newFps
        if (targetWidth > 0 && targetHeight > 0) {
            startCapture(targetWidth, targetHeight, newFps)
        }
    }

    override fun dispose() {
        producerRunning = false
        shutdownExecutors()
        frameQueue.clear()
        lastSentFrame = null
        pixelScratch = IntArray(0)
        super.dispose()
    }

    // -------------------------------------------------------------------------
    // Producer
    // -------------------------------------------------------------------------

    private fun producerLoop() {
        when (mode) {
            PlaybackMode.LOOP      -> producerLoopForever()
            PlaybackMode.HOLD_LAST -> producerSinglePass()
        }
    }

    private fun producerLoopForever() {
        while (producerRunning) {
            val files = listSortedJpegs()
            if (files.isNullOrEmpty()) {
                Log.w(TAG, "No JPEG files in $folderPath — retrying in 500 ms")
                try { Thread.sleep(500) }
                catch (e: InterruptedException) { Thread.currentThread().interrupt(); return }
                continue
            }
            for (file in files) {
                if (!producerRunning) return
                enqueueFile(file)
            }
            // Loop immediately; newly added files are picked up on the next pass.
        }
    }

    private fun producerSinglePass() {
        // Wait until files appear, then decode them exactly once.
        var files: Array<File>?
        while (true) {
            files = listSortedJpegs()
            if (!files.isNullOrEmpty()) break
            Log.w(TAG, "No JPEG files in $folderPath — retrying in 500 ms")
            try { Thread.sleep(500) }
            catch (e: InterruptedException) { Thread.currentThread().interrupt(); return }
            if (!producerRunning) return
        }

        for (file in files!!) {
            if (!producerRunning) return
            enqueueFile(file)
        }
        // Producer exits; consumer will repeat lastSentFrame indefinitely.
        Log.d(TAG, "HOLD_LAST: single pass complete — holding last frame")
    }

    private fun enqueueFile(file: File) {
        val frame = try {
            decodeToI420(file)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM decoding ${file.name} — skipping", oom); null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode ${file.name}", e); null
        } ?: return

        try {
            frameQueue.put(frame) // blocks when full — natural backpressure
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    // -------------------------------------------------------------------------
    // Consumer — fires at configured FPS via scheduleAtFixedRate
    // -------------------------------------------------------------------------

    private fun consumerTick() {
        try {
            val polled = frameQueue.poll()
            val frame = if (polled != null) {
                lastSentFrame = polled
                polled
            } else {
                lastSentFrame ?: return // nothing decoded yet
            }
            sendPreloadedFrame(frame)
        } catch (e: Exception) {
            Log.e(TAG, "Error in consumer tick", e)
        }
    }

    private fun sendPreloadedFrame(f: I420Frame) {
        f.y.rewind(); f.u.rewind(); f.v.rewind()

        val i420 = JavaI420Buffer.wrap(
            f.width, f.height,
            f.y, f.width,
            f.u, f.uvStride,
            f.v, f.uvStride,
            null
        )
        val captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime())
        val frame = VideoFrame(i420, 0, captureTimeNs)
        writeFrame(frame) // writeFrame releases internally — do NOT call frame.release()
    }

    // -------------------------------------------------------------------------
    // JPEG → I420  (called only on the producer thread)
    // -------------------------------------------------------------------------

    private fun decodeToI420(file: File): I420Frame? {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        var bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: run { Log.w(TAG, "BitmapFactory returned null for ${file.name}"); return null }

        if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
            val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            bitmap.recycle()
            bitmap = scaled
        }

        val w = bitmap.width
        val h = bitmap.height

        // Reuse pre-allocated scratch buffer to avoid 3.6 MB per-frame heap allocation.
        val pixels = pixelScratch
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        val uvW = (w + 1) / 2
        val uvH = (h + 1) / 2
        val yBuf = ByteBuffer.allocateDirect(w * h)
        val uBuf = ByteBuffer.allocateDirect(uvW * uvH)
        val vBuf = ByteBuffer.allocateDirect(uvW * uvH)

        argbToI420(pixels, w, h, yBuf, uBuf, vBuf)

        yBuf.rewind(); uBuf.rewind(); vBuf.rewind()
        return I420Frame(yBuf, uBuf, vBuf, w, h, uvW)
    }

    /**
     * BT.601 studio-swing ARGB → I420.
     * Y  = clamp(((66R + 129G + 25B + 128) >> 8) + 16,  16, 235)
     * Cb = clamp(((-38R -  74G + 112B + 128) >> 8) + 128, 16, 240)
     * Cr = clamp(((112R -  94G -  18B + 128) >> 8) + 128, 16, 240)
     * U/V planes are 4:2:0 subsampled (top-left pixel of each 2×2 block).
     */
    private fun argbToI420(
        pixels: IntArray, w: Int, h: Int,
        yBuf: ByteBuffer, uBuf: ByteBuffer, vBuf: ByteBuffer
    ) {
        for (row in 0 until h) {
            for (col in 0 until w) {
                val pixel = pixels[row * w + col]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuf.put(y.coerceIn(16, 235).toByte())

                if (row and 1 == 0 && col and 1 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    uBuf.put(u.coerceIn(16, 240).toByte())
                    vBuf.put(v.coerceIn(16, 240).toByte())
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun listSortedJpegs(): Array<File>? {
        val dir = File(folderPath)
        if (!dir.exists() || !dir.isDirectory) {
            Log.w(TAG, "Folder not found: $folderPath")
            return null
        }
        val files = dir.listFiles(JPEG_FILTER) ?: return null
        files.sortWith(compareBy { it.name })
        return files
    }

    private fun shutdownExecutors() {
        consumerExecutor?.shutdown(); consumerExecutor = null
        producerExecutor?.shutdownNow(); producerExecutor = null
        // shutdownNow() interrupts the blocking put() in producerLoop
    }

    companion object {
        private const val TAG = "JpegFolderVideoCapturer"

        /**
         * Number of decoded I420 frames kept in the queue.
         * At 1280×720 each frame is ~1.38 MB, so 10 frames ≈ 14 MB.
         */
        private const val QUEUE_CAPACITY = 10

        private val JPEG_FILTER = FilenameFilter { _, name ->
            val lower = name.lowercase()
            lower.endsWith(".jpg") || lower.endsWith(".jpeg")
        }
    }
}
