package com.example.audiorecord

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
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

data class RecordingItem(
    val file: File,
    val fileName: String,
    val date: Date,
    val duration: String,
    var isPlaying: Boolean = false,
    var isSelected: Boolean = false
)

class RecordingAdapter(
    private val context: MainActivity,
    private var recordings: MutableList<RecordingItem>
) : BaseAdapter() {

    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    override fun getCount(): Int = recordings.size

    override fun getItem(position: Int): RecordingItem = recordings[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_recording, parent, false)

        val item = recordings[position]

        val tvFileName = view.findViewById<TextView>(R.id.tvItemFileName)
        val tvDuration = view.findViewById<TextView>(R.id.tvItemDuration)
        val tvDate = view.findViewById<TextView>(R.id.tvItemDate)
        val ivPlaying = view.findViewById<ImageView>(R.id.ivPlaying)

        tvFileName.text = item.fileName
        tvDuration.text = item.duration
        tvDate.text = dateFormat.format(item.date)

        // Show playing indicator
        ivPlaying.visibility = if (item.isPlaying) View.VISIBLE else View.GONE

        // Set activated state for selected item
        view.isActivated = item.isSelected || item.isPlaying

        return view
    }

    fun updateRecordings(newRecordings: List<RecordingItem>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }

    fun addRecording(recording: RecordingItem) {
        recordings.add(0, recording)
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        recordings.forEachIndexed { index, item ->
            item.isSelected = (index == position)
        }
        notifyDataSetChanged()
    }

    fun setPlayingPosition(position: Int) {
        recordings.forEachIndexed { index, item ->
            item.isPlaying = (index == position)
        }
        notifyDataSetChanged()
    }

    fun clearPlaying() {
        recordings.forEach { it.isPlaying = false }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        recordings.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }
}

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

    private val recordings = mutableListOf<RecordingItem>()
    private var currentRecordingFile: File? = null
    private var focusedPosition = -1

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

        // Touch click - directly play
        listViewRecordings.setOnItemClickListener { _, _, position, _ ->
            playRecordingAtPosition(position)
        }

        // Handle keyboard/D-pad events for selection
        listViewRecordings.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {
                        // Play the currently focused item
                        val position = listViewRecordings.selectedItemPosition
                        if (position != ListView.INVALID_POSITION && position < recordings.size) {
                            playRecordingAtPosition(position)
                        }
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }

        // Update selection visual when focus changes
        listViewRecordings.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                recordingAdapter.clearSelection()
            }
        }

        // Track selection changes
        listViewRecordings.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                focusedPosition = position
                recordingAdapter.setSelectedPosition(position)
                if (position >= 0 && position < recordings.size) {
                    tvFileName.text = recordings[position].fileName
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                focusedPosition = -1
                recordingAdapter.clearSelection()
            }
        })
    }

    private fun playRecordingAtPosition(position: Int) {
        if (position < 0 || position >= recordings.size) return

        // Stop current playback
        audioPlayer.stop()

        val item = recordings[position]
        val file = item.file

        if (!file.exists()) {
            Toast.makeText(this, R.string.no_recording, Toast.LENGTH_SHORT).show()
            return
        }

        if (audioPlayer.play(file)) {
            btnPlay.text = getString(R.string.btn_pause)
            btnStop.isEnabled = true
            tvStatus.text = getString(R.string.status_playing)
            tvFileName.text = file.name

            // Update playing indicator
            recordingAdapter.setPlayingPosition(position)

            Toast.makeText(this, "Playing: ${item.fileName}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show()
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
                btnPlay.isEnabled = recordings.isNotEmpty()
                btnStop.isEnabled = false
                tvStatus.text = getString(R.string.status_idle)
                recordingAdapter.clearPlaying()
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
            btnPlay.isEnabled = true
        } else {
            listViewRecordings.visibility = View.GONE
            tvEmptyHint.visibility = View.VISIBLE
            btnPlay.isEnabled = false
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

            // Auto-select the new recording
            focusedPosition = 0
            tvFileName.text = file.name
        }
    }

    private fun startPlayback() {
        // If focused on a list item, play that
        if (focusedPosition >= 0 && focusedPosition < recordings.size) {
            playRecordingAtPosition(focusedPosition)
            return
        }

        // Otherwise, try latest recording
        val latestFile = getLatestRecording()
        if (latestFile != null && latestFile.exists()) {
            if (audioPlayer.play(latestFile)) {
                btnPlay.text = getString(R.string.btn_pause)
                btnStop.isEnabled = true
                tvStatus.text = getString(R.string.status_playing)
                tvFileName.text = latestFile.name
            } else {
                Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, R.string.no_recording, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlayback() {
        audioPlayer.stop()
        btnPlay.text = getString(R.string.btn_play)
        btnStop.isEnabled = false
        tvStatus.text = getString(R.string.status_idle)
        recordingAdapter.clearPlaying()
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