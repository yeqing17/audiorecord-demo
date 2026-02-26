package com.example.audiorecord

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingItem(
    val file: File,
    val fileName: String,
    val date: Date,
    val duration: String
)

class RecordingAdapter(
    private val context: Context,
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

        tvFileName.text = item.fileName
        tvDuration.text = item.duration
        tvDate.text = dateFormat.format(item.date)

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
}