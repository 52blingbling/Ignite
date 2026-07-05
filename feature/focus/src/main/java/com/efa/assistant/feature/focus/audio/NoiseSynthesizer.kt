package com.efa.assistant.feature.focus.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Random

enum class NoiseType {
    NONE,
    WHITE,
    BROWN,
    RAIN
}

class NoiseSynthesizer(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null
    @Volatile
    private var isPlaying = false
    private var currentType = NoiseType.NONE

    fun start(type: NoiseType) {
        if (type == NoiseType.NONE) {
            stop()
            return
        }
        if (currentType == type && isPlaying) {
            return
        }

        stop()
        isPlaying = true
        currentType = type

        playJob = CoroutineScope(dispatcher).launch {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            // Modern AudioTrack.Builder (safe for API 23+)
            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            }

            audioTrack = track
            
            try {
                track.play()
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            }

            val bufferSize = minBufferSize / 2 // in shorts
            val buffer = ShortArray(bufferSize)
            val random = Random()

            // State variables for DSP (Digital Signal Processing)
            var lastOut = 0.0f // Low pass filter for Brown Noise

            // Raindrop state variables
            var raindropTimer = 0
            var raindropAmplitude = 0.0f

            while (isPlaying) {
                for (i in buffer.indices) {
                    val white = random.nextFloat() * 2.0f - 1.0f // White noise base (-1.0 to 1.0)

                    var sample = when (type) {
                        NoiseType.WHITE -> {
                            white * 0.08f // Soft volume
                        }
                        NoiseType.BROWN -> {
                            // Brown noise: Integrate white noise + leaky integration
                            lastOut = (lastOut + (0.02f * white)) / 1.02f
                            lastOut * 3.5f * 0.12f // Boost and adjust volume
                        }
                        NoiseType.RAIN -> {
                            // Rain noise: Leaky integrated brown noise + clicky transient impulses
                            lastOut = (lastOut + (0.02f * white)) / 1.02f
                            var rainSample = lastOut * 3.5f

                            // Impulse generation (raindrops clicking)
                            if (raindropTimer <= 0) {
                                // ~0.15% chance to start a drop at each sample
                                if (random.nextFloat() < 0.0015f) {
                                    raindropAmplitude = random.nextFloat() * 0.25f + 0.05f
                                    raindropTimer = random.nextInt(1500) + 300
                                }
                            } else {
                                raindropTimer--
                                raindropAmplitude *= 0.992f // Decay envelope
                                val crack = (random.nextFloat() * 2.0f - 1.0f) * raindropAmplitude
                                rainSample += crack
                            }

                            rainSample * 0.12f
                        }
                        else -> 0.0f
                    }

                    // Soft clipping
                    if (sample > 1.0f) sample = 1.0f
                    if (sample < -1.0f) sample = -1.0f

                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }

                if (isPlaying) {
                    track.write(buffer, 0, buffer.size)
                }
            }

            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        isPlaying = false
        currentType = NoiseType.NONE
        playJob?.cancel()
        playJob = null
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }
}
