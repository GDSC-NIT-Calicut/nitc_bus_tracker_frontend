package com.gdsc.nitcbustracker.adapter

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
        val eta: TextView = itemView.findViewById(R.id.etaText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bus_row, parent, false)
        return BusViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
        val bus = buses[position]
        holder.busId.text = "Bus ID: ${bus.bus_id}"
        holder.currentStop.text = bus.current_stop_name
        holder.eta.text = bus.eta.toString()
    }

    override fun getItemCount() = buses.size
}
