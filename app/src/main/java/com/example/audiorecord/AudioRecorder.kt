package com.example.audiorecord

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Audio recorder using AudioRecord API.
 * Records audio in PCM format and saves as WAV file.
 */
class AudioRecorder(
    private val outputDir: File
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BITS_PER_SAMPLE = 16
        private const val NUM_CHANNELS = 1
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private var outputFile: File? = null
    private var onDurationUpdate: ((Long) -> Unit)? = null

    var currentOutputFile: File? = null
        private set

    /**
     * Start recording audio.
     * @return The output file path, or null if failed to start
     */
    fun startRecording(): String? {
        if (isRecording.get()) {
            return null
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            return null
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return null
            }

            // Create output file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(outputDir, "recording_$timestamp.wav")
            currentOutputFile = outputFile

            // Start recording
            audioRecord?.startRecording()
            isRecording.set(true)

            recordingThread = Thread {
                writeAudioDataToFile(bufferSize)
            }.apply {
                start()
            }

            return outputFile?.absolutePath
        } catch (e: SecurityException) {
            audioRecord?.release()
            audioRecord = null
            return null
        } catch (e: IOException) {
            audioRecord?.release()
            audioRecord = null
            return null
        }
    }

    /**
     * Stop recording audio.
     * @return The output file path, or null if no recording was active
     */
    fun stopRecording(): String? {
        if (!isRecording.get()) {
            return null
        }

        isRecording.set(false)

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            // Ignore
        }

        recordingThread?.join(1000)
        recordingThread = null

        return outputFile?.absolutePath
    }

    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean = isRecording.get()

    /**
     * Set callback for duration updates.
     */
    fun setOnDurationUpdateListener(listener: (Long) -> Unit) {
        onDurationUpdate = listener
    }

    private fun writeAudioDataToFile(bufferSize: Int) {
        val tempPcmFile = File(outputDir, "temp_${System.currentTimeMillis()}.pcm")

        try {
            val buffer = ByteArray(bufferSize)
            var totalBytesRead = 0L
            val startTime = System.currentTimeMillis()

            FileOutputStream(tempPcmFile).use { fos ->
                while (isRecording.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (bytesRead > 0) {
                        fos.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Calculate duration in milliseconds
                        val durationMs = (totalBytesRead * 1000L) / (SAMPLE_RATE * NUM_CHANNELS * (BITS_PER_SAMPLE / 8))
                        onDurationUpdate?.invoke(durationMs)
                    }
                }
            }

            // Convert PCM to WAV
            if (outputFile != null) {
                convertPcmToWav(tempPcmFile, outputFile!!, totalBytesRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            tempPcmFile.delete()
        }
    }

    private fun convertPcmToWav(pcmFile: File, wavFile: File, totalDataSize: Long) {
        try {
            FileOutputStream(wavFile).use { fos ->
                // Write WAV header
                val header = ByteArray(44)
                val totalSize = totalDataSize + 36
                val byteRate = SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8

                // RIFF header
                header[0] = 'R'.code.toByte()
                header[1] = 'I'.code.toByte()
                header[2] = 'F'.code.toByte()
                header[3] = 'F'.code.toByte()
                writeInt(header, 4, totalSize.toInt())
                header[8] = 'W'.code.toByte()
                header[9] = 'A'.code.toByte()
                header[10] = 'V'.code.toByte()
                header[11] = 'E'.code.toByte()

                // fmt chunk
                header[12] = 'f'.code.toByte()
                header[13] = 'm'.code.toByte()
                header[14] = 't'.code.toByte()
                header[15] = ' '.code.toByte()
                writeInt(header, 16, 16) // chunk size
                writeShort(header, 20, 1.toShort()) // audio format (PCM)
                writeShort(header, 22, NUM_CHANNELS.toShort())
                writeInt(header, 24, SAMPLE_RATE)
                writeInt(header, 28, byteRate)
                writeShort(header, 32, (NUM_CHANNELS * BITS_PER_SAMPLE / 8).toShort()) // block align
                writeShort(header, 34, BITS_PER_SAMPLE.toShort())

                // data chunk
                header[36] = 'd'.code.toByte()
                header[37] = 'a'.code.toByte()
                header[38] = 't'.code.toByte()
                header[39] = 'a'.code.toByte()
                writeInt(header, 40, totalDataSize.toInt())

                fos.write(header)

                // Write PCM data
                pcmFile.inputStream().use { input ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = (value shr 8 and 0xFF).toByte()
        buffer[offset + 2] = (value shr 16 and 0xFF).toByte()
        buffer[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun writeShort(buffer: ByteArray, offset: Int, value: Short) {
        buffer[offset] = (value.toInt() and 0xFF).toByte()
        buffer[offset + 1] = (value.toInt() shr 8 and 0xFF).toByte()
    }

    /**
     * Release resources.
     */
    fun release() {
        stopRecording()
    }
}