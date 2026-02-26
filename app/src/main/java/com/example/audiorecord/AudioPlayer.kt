package com.example.audiorecord

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audio player using AudioTrack API.
 * Plays WAV audio files.
 */
class AudioPlayer {

    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null
    @Volatile
    private var isPlaying = false
    @Volatile
    private var isPaused = false

    private var onCompletionListener: (() -> Unit)? = null
    private var onProgressListener: ((Long, Long) -> Unit)? = null // current position, total duration

    /**
     * Play a WAV file.
     * @param file The WAV file to play
     * @return true if playback started successfully
     */
    fun play(file: File): Boolean {
        if (isPlaying) {
            if (isPaused) {
                resume()
                return true
            }
            return false
        }

        val wavInfo = readWavHeader(file) ?: return false

        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                wavInfo.sampleRate,
                if (wavInfo.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(wavInfo.sampleRate)
                        .setChannelMask(
                            if (wavInfo.channels == 1) AudioFormat.CHANNEL_OUT_MONO
                            else AudioFormat.CHANNEL_OUT_STEREO
                        )
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            isPlaying = true
            isPaused = false

            playThread = Thread {
                playAudioData(file, wavInfo.dataOffset, wavInfo.dataSize, bufferSize)
            }.apply {
                start()
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Pause playback.
     */
    fun pause() {
        if (isPlaying && !isPaused) {
            isPaused = true
            audioTrack?.pause()
        }
    }

    /**
     * Resume playback.
     */
    fun resume() {
        if (isPlaying && isPaused) {
            isPaused = false
            audioTrack?.play()
        }
    }

    /**
     * Stop playback.
     */
    fun stop() {
        isPlaying = false
        isPaused = false

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null

        playThread?.join(1000)
        playThread = null
    }

    /**
     * Check if currently playing.
     */
    fun isPlaying(): Boolean = isPlaying && !isPaused

    /**
     * Check if paused.
     */
    fun isPaused(): Boolean = isPaused

    /**
     * Set callback for playback completion.
     */
    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }

    /**
     * Set callback for playback progress.
     */
    fun setOnProgressListener(listener: (currentMs: Long, totalMs: Long) -> Unit) {
        onProgressListener = listener
    }

    /**
     * Release resources.
     */
    fun release() {
        stop()
    }

    private fun playAudioData(file: File, dataOffset: Int, dataSize: Long, bufferSize: Int) {
        try {
            FileInputStream(file).use { fis ->
                // Skip to data
                fis.skip(dataOffset.toLong())

                audioTrack?.play()

                val buffer = ByteArray(bufferSize)
                var totalBytesRead = 0L

                while (isPlaying && totalBytesRead < dataSize) {
                    if (isPaused) {
                        Thread.sleep(50)
                        continue
                    }

                    val bytesToRead = minOf(buffer.size.toLong(), dataSize - totalBytesRead).toInt()
                    val bytesRead = fis.read(buffer, 0, bytesToRead)

                    if (bytesRead == -1) break

                    val result = audioTrack?.write(buffer, 0, bytesRead)
                    if (result != null && result > 0) {
                        totalBytesRead += result

                        // Calculate progress
                        // Assuming 16-bit samples
                        val sampleRate = audioTrack?.sampleRate ?: 44100
                        val channelCount = if (audioTrack?.channelCount == 1) 1 else 2
                        val bytesPerMs = (sampleRate * channelCount * 2L) / 1000

                        val currentMs = totalBytesRead / bytesPerMs
                        val totalMs = dataSize / bytesPerMs

                        onProgressListener?.invoke(currentMs, totalMs)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            isPlaying = false
            isPaused = false
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            onCompletionListener?.invoke()
        }
    }

    private data class WavInfo(
        val sampleRate: Int,
        val channels: Int,
        val dataOffset: Int,
        val dataSize: Long
    )

    private fun readWavHeader(file: File): WavInfo? {
        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                val bytesRead = fis.read(header)
                if (bytesRead < 44) return null

                // Check RIFF header
                if (header[0] != 'R'.code.toByte() || header[1] != 'I'.code.toByte() ||
                    header[2] != 'F'.code.toByte() || header[3] != 'F'.code.toByte()
                ) {
                    return null
                }

                // Check WAVE format
                if (header[8] != 'W'.code.toByte() || header[9] != 'A'.code.toByte() ||
                    header[10] != 'V'.code.toByte() || header[11] != 'E'.code.toByte()
                ) {
                    return null
                }

                // Read format chunk
                val sampleRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val numChannels = ByteBuffer.wrap(header, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                val bitsPerSample = ByteBuffer.wrap(header, 34, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

                // Read data chunk size
                val dataSize = ByteBuffer.wrap(header, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong()

                return WavInfo(
                    sampleRate = sampleRate,
                    channels = numChannels,
                    dataOffset = 44,
                    dataSize = dataSize
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}