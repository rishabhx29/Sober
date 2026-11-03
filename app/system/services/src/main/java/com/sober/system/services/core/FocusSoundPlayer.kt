package com.sober.system.services.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Brown noise generated on the device while a focus session runs.
 *
 * No audio file is shipped and nothing is streamed: the noise is written
 * straight into an [AudioTrack] in streaming mode. That keeps the APK small,
 * matches the app's no-network promise, and needs no media permission — only
 * the user's own media volume applies.
 *
 * The engine owns the lifetime ([setPlaying] follows the active session and
 * the preference), so callers never have to remember to stop it.
 */
@Singleton
class FocusSoundPlayer @Inject constructor() {

    private val lock = Any()
    private var track: AudioTrack? = null
    private var thread: Thread? = null

    @Volatile
    private var running = false

    fun setPlaying(playing: Boolean) {
        synchronized(lock) {
            if (playing) startLocked() else stopLocked()
        }
    }

    private fun startLocked() {
        if (track != null) return

        val created = runCatching { buildTrack() }.getOrNull()
        if (created == null) {
            // Some emulators and unusual audio HALs refuse a custom stream.
            // Failing silently is right here: a focus session must never fail
            // because its optional sound could not start.
            Log.w(TAG, "Focus sound unavailable on this device")
            return
        }

        track = created
        running = true
        created.play()
        thread = Thread(::writeLoop, "sober-focus-sound").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Brown noise is white noise fed through a leaky integrator. The result is
     * the low, even rumble that makes noise easy to ignore, rather than the
     * sharp hiss of raw white noise.
     */
    private fun writeLoop() {
        val buffer = ShortArray(BUFFER_SAMPLES)
        var brown = 0f
        while (running) {
            val sink = track ?: break
            for (index in buffer.indices) {
                val white = Random.nextFloat() * 2f - 1f
                brown = (brown + white * INTEGRATION).coerceIn(-1f, 1f)
                buffer[index] = (brown * Short.MAX_VALUE * VOLUME).toInt().toShort()
            }
            val written = runCatching { sink.write(buffer, 0, buffer.size) }.getOrElse { break }
            if (written <= 0) break
        }
    }

    private fun stopLocked() {
        running = false
        val sink = track
        track = null
        thread = null
        if (sink != null) {
            runCatching {
                sink.pause()
                sink.flush()
                sink.release()
            }
        }
    }

    private fun buildTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, BUFFER_SAMPLES * 2)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private companion object {
        const val TAG = "FocusSoundPlayer"

        // 16 kHz is plenty for low-frequency noise and halves the data the
        // device has to generate compared with a 44.1 kHz stream.
        const val SAMPLE_RATE = 16_000
        const val BUFFER_SAMPLES = 4_096

        /** How strongly white noise is folded into the running brown value. */
        const val INTEGRATION = 0.02f

        /** Quiet by design: this is a background, not a track. */
        const val VOLUME = 0.22f
    }
}
