package net.red5.testbed.utility

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.sqrt

class SoundMeter(
    updateAudioLevelFrequencyMs: Long,
    private val activity: Activity,
    private val audioLevelListener: LocalAudioLevelListener
) {
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE =
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    private var audioRecord: AudioRecord? = null
    private var executorService: ScheduledExecutorService? = null
    private var updateAudioLevelFrequencyMs = 250L


    init {
        this.updateAudioLevelFrequencyMs = updateAudioLevelFrequencyMs
        init()
    }

    private fun init() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("RECORD_AUDIO permission not granted.")
        }
        setAudioRecord(
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )
        )
    }

    fun start() {
        if (audioRecord!!.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord!!.startRecording()

            executorService = Executors.newScheduledThreadPool(1)
            executorService!!.scheduleAtFixedRate(Runnable {
                val buffer = ShortArray(BUFFER_SIZE / 2)
                val numSamples = audioRecord!!.read(buffer, 0, buffer.size)
                if (numSamples > 0) {
                    val rms = calculateRMS(buffer, numSamples)
                    val db = 20 * log10(rms)
                    audioLevelListener.onAudioLevelUpdated(db)
                }
            }, 0, updateAudioLevelFrequencyMs, TimeUnit.MILLISECONDS)
        }
    }

    fun stop() {
        if (audioRecord != null) {
            if (audioRecord!!.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord!!.stop()
            }
            audioRecord!!.release()
            audioRecord = null
        }
        if (executorService != null) {
            executorService!!.shutdown()
        }
    }

    fun calculateRMS(audioData: ShortArray, numSamples: Int): Double {
        var sum = 0.0
        for (i in 0..<numSamples) {
            sum += (audioData[i] * audioData[i]).toDouble()
        }
        val mean = sum / numSamples
        return sqrt(mean)
    }

    fun setAudioRecord(audioRecord: AudioRecord?) {
        this.audioRecord = audioRecord
    }
}
