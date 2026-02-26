package com.example.audiorecord

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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
    private lateinit var tvEmptyHint: TextView
    private lateinit var btnRecord: Button
    private lateinit var btnPlay: Button
    private lateinit var btnStop: Button
    private lateinit var listViewRecordings: ListView

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var recordingAdapter: RecordingAdapter

    private var currentRecordingFile: File? = null
    private var selectedRecordingFile: File? = null
    private val recordings = mutableListOf<RecordingItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initAudioComponents()
        checkPermissions()
        loadRecordings()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvDuration = findViewById(R.id.tvDuration)
        tvFileName = findViewById(R.id.tvFileName)
        tvEmptyHint = findViewById(R.id.tvEmptyHint)
        btnRecord = findViewById(R.id.btnRecord)
        btnPlay = findViewById(R.id.btnPlay)
        btnStop = findViewById(R.id.btnStop)
        listViewRecordings = findViewById(R.id.listViewRecordings)

        recordingAdapter = RecordingAdapter(this, recordings)
        listViewRecordings.adapter = recordingAdapter

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

        listViewRecordings.setOnItemClickListener { _, _, position, _ ->
            val item = recordingAdapter.getItem(position)
            selectedRecordingFile = item.file
            tvFileName.text = item.fileName
            btnPlay.isEnabled = true
            listViewRecordings.setItemChecked(position, true)
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
                btnPlay.isEnabled = selectedRecordingFile != null || currentRecordingFile != null
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

    private fun loadRecordings() {
        val outputDir = getOutputDirectory()
        val files = outputDir.listFiles { _, name -> name.endsWith(".wav") }

        recordings.clear()

        if (files != null && files.isNotEmpty()) {
            files.sortByDescending { it.lastModified() }

            for (file in files) {
                val duration = getWavDuration(file)
                val item = RecordingItem(
                    file = file,
                    fileName = file.name,
                    date = Date(file.lastModified()),
                    duration = formatDuration(duration)
                )
                recordings.add(item)
            }

            listViewRecordings.visibility = View.VISIBLE
            tvEmptyHint.visibility = View.GONE
        } else {
            listViewRecordings.visibility = View.GONE
            tvEmptyHint.visibility = View.VISIBLE
        }

        recordingAdapter.notifyDataSetChanged()
    }

    private fun getWavDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
            return
        }

        stopPlayback()

        val filePath = audioRecorder.startRecording()
        if (filePath != null) {
            currentRecordingFile = File(filePath)
            btnRecord.text = getString(R.string.btn_stop_recording)
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
            val file = File(filePath)
            Log.d(TAG, "Recording saved to: $filePath")
            Toast.makeText(
                this,
                getString(R.string.recording_saved, file.name),
                Toast.LENGTH_SHORT
            ).show()

            // Add new recording to the list
            val duration = getWavDuration(file)
            val item = RecordingItem(
                file = file,
                fileName = file.name,
                date = Date(file.lastModified()),
                duration = formatDuration(duration)
            )
            recordingAdapter.addRecording(item)

            listViewRecordings.visibility = View.VISIBLE
            tvEmptyHint.visibility = View.GONE

            btnRecord.text = getString(R.string.btn_start_recording)
            btnPlay.isEnabled = true
            btnStop.isEnabled = false
            tvStatus.text = getString(R.string.status_idle)

            selectedRecordingFile = file
        }
    }

    private fun startPlayback() {
        val fileToPlay = selectedRecordingFile ?: currentRecordingFile ?: getLatestRecording()

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