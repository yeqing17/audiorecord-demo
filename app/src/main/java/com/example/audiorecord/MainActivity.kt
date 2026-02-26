package com.example.audiorecord

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 1001
    }

    private lateinit var tvStatus: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvFileName: TextView
    private lateinit var btnRecord: Button
    private lateinit var btnPlay: Button
    private lateinit var btnStop: Button

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioPlayer: AudioPlayer

    private var currentRecordingFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initAudioComponents()
        checkPermissions()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvDuration = findViewById(R.id.tvDuration)
        tvFileName = findViewById(R.id.tvFileName)
        btnRecord = findViewById(R.id.btnRecord)
        btnPlay = findViewById(R.id.btnPlay)
        btnStop = findViewById(R.id.btnStop)

        btnRecord.setOnClickListener {
            if (audioRecorder.isRecording()) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        btnPlay.setOnClickListener {
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause()
                btnPlay.text = getString(R.string.btn_play)
                tvStatus.text = getString(R.string.status_paused)
            } else if (audioPlayer.isPaused()) {
                audioPlayer.resume()
                btnPlay.text = getString(R.string.btn_pause)
                tvStatus.text = getString(R.string.status_playing)
            } else {
                startPlayback()
            }
        }

        btnStop.setOnClickListener {
            stopPlayback()
        }
    }

    private fun initAudioComponents() {
        val outputDir = getOutputDirectory()
        audioRecorder = AudioRecorder(outputDir)
        audioPlayer = AudioPlayer()

        audioRecorder.setOnDurationUpdateListener { durationMs ->
            runOnUiThread {
                tvDuration.text = formatDuration(durationMs)
            }
        }

        audioPlayer.setOnCompletionListener {
            runOnUiThread {
                btnPlay.text = getString(R.string.btn_play)
                btnPlay.isEnabled = true
                btnStop.isEnabled = false
                tvStatus.text = getString(R.string.status_idle)
            }
        }

        audioPlayer.setOnProgressListener { currentMs, totalMs ->
            runOnUiThread {
                tvDuration.text = formatDuration(currentMs)
            }
        }
    }

    private fun getOutputDirectory(): File {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    R.string.permission_required,
                    Toast.LENGTH_LONG
                ).show()
                btnRecord.isEnabled = false
            }
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
            return
        }

        // Stop any ongoing playback
        stopPlayback()

        val filePath = audioRecorder.startRecording()
        if (filePath != null) {
            currentRecordingFile = File(filePath)
            btnRecord.text = getString(R.string.btn_stop_recording)
            btnRecord.setBackgroundColor(getColor(R.color.record_button_active))
            btnPlay.isEnabled = false
            btnStop.isEnabled = false
            tvStatus.text = getString(R.string.status_recording)
            tvFileName.text = currentRecordingFile?.name
            Log.d(TAG, "Started recording to: $filePath")
        } else {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        val filePath = audioRecorder.stopRecording()
        if (filePath != null) {
            Log.d(TAG, "Recording saved to: $filePath")
            Toast.makeText(
                this,
                getString(R.string.recording_saved, File(filePath).name),
                Toast.LENGTH_SHORT
            ).show()

            btnRecord.text = getString(R.string.btn_start_recording)
            btnRecord.setBackgroundColor(getColor(R.color.record_button))
            btnPlay.isEnabled = true
            btnStop.isEnabled = false
            tvStatus.text = getString(R.string.status_idle)
        }
    }

    private fun startPlayback() {
        val fileToPlay = currentRecordingFile ?: getLatestRecording()

        if (fileToPlay == null || !fileToPlay.exists()) {
            Toast.makeText(this, R.string.no_recording, Toast.LENGTH_SHORT).show()
            return
        }

        if (audioPlayer.play(fileToPlay)) {
            btnPlay.text = getString(R.string.btn_pause)
            btnStop.isEnabled = true
            tvStatus.text = getString(R.string.status_playing)
            tvFileName.text = fileToPlay.name
            Log.d(TAG, "Started playing: ${fileToPlay.absolutePath}")
        } else {
            Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlayback() {
        audioPlayer.stop()
        btnPlay.text = getString(R.string.btn_play)
        btnStop.isEnabled = false
        tvStatus.text = getString(R.string.status_idle)
    }

    private fun getLatestRecording(): File? {
        val outputDir = getOutputDirectory()
        val files = outputDir.listFiles { _, name -> name.endsWith(".wav") }
        return files?.maxByOrNull { it.lastModified() }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.release()
        audioPlayer.release()
    }
}