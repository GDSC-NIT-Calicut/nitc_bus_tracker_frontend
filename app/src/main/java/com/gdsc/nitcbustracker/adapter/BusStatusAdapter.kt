package com.gdsc.nitcbustracker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gdsc.nitcbustracker.R
import com.gdsc.nitcbustracker.data.model.BusStatus

class BusStatusAdapter(private val buses: List<BusStatus>) :
    RecyclerView.Adapter<BusStatusAdapter.BusViewHolder>() {

    inner class BusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val busId: TextView = itemView.findViewById(R.id.busTitle)
        val currentStop: TextView = itemView.findViewById(R.id.currentStopText)
        val nextStop: TextView = itemView.findViewById(R.id.nextStopText)
        val eta: TextView = itemView.findViewById(R.id.etaText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bus_row, parent, false)
        return BusViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
        val bus = buses[position]
        holder.busId.text = "${bus.bus_id}"
        holder.currentStop.text = "${bus.current_stop_name}"
        holder.nextStop.text = "${getNextStop(bus.bus_id, holder.itemView.context)}"
        holder.eta.text = "${bus.eta} mins"
    }

    override fun getItemCount() = buses.size

    private fun getNextStop(busId: String, context: Context): String {
        val prefs = context.getSharedPreferences("next_stops_prefs", Context.MODE_PRIVATE)
        return prefs.getString(busId, "Unknown") ?: "Unknown"
    }
}
