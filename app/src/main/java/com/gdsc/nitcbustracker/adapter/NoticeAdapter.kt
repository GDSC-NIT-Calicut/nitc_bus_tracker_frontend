package com.gdsc.nitcbustracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gdsc.nitcbustracker.R
import com.gdsc.nitcbustracker.data.model.Notice
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NoticeAdapter(private var notices: List<Notice>) :
    RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder>() {

    inner class NoticeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noticeTitle: TextView = itemView.findViewById(R.id.noticeTitle)
        val messageText: TextView = itemView.findViewById(R.id.noticeMessage)
        val dateText: TextView = itemView.findViewById(R.id.noticeDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notice_card, parent, false)
        return NoticeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        val notice = notices[position]
        holder.noticeTitle.text = notice.topic
        holder.messageText.text = notice.message

        // Format timestamp
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Kolkata")

            val parsedDate = inputFormat.parse(notice.timestamp)
            val formattedDate = outputFormat.format(parsedDate!!)
            holder.dateText.text = formattedDate
        } catch (e: Exception) {
            holder.dateText.text = notice.timestamp // fallback
        }
    }

    override fun getItemCount(): Int = notices.size

    fun updateNotices(newNotices: List<Notice>) {
        notices = newNotices
        notifyDataSetChanged()
    }
}
