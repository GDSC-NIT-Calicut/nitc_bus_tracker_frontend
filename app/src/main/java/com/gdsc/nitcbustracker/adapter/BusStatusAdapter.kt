package com.gdsc.nitcbustracker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gdsc.nitcbustracker.R
import com.gdsc.nitcbustracker.data.model.BusStatus
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class BusStatusAdapter(
    private val buses: List<BusStatus>,
    private val scope: CoroutineScope) :
    RecyclerView.Adapter<BusStatusAdapter.BusViewHolder>() {

    inner class BusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val busId: TextView = itemView.findViewById(R.id.busTitle)
        val currentStop: TextView = itemView.findViewById(R.id.currentStopText)
        val nextStop: TextView = itemView.findViewById(R.id.nextStopText)
        val eta: TextView = itemView.findViewById(R.id.etaText)
        var toggleSharing: Button = itemView.findViewById(R.id.toggleSharingButton)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
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

        holder.toggleSharing.isEnabled = false
        holder.progressBar.visibility = View.VISIBLE

        scope.launch {
            try {
                val response = RetrofitClient.api.getSharingStatus(bus.bus_id)
                if (response.isSuccessful) {
                    val isSharing = response.body()?.isSharing == true
                    withContext(Dispatchers.Main) {
                        holder.toggleSharing.text = if (isSharing) "Disable" else "Enable"
                        holder.toggleSharing.isEnabled = true
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        holder.toggleSharing.text = "Error"
                        holder.toggleSharing.isEnabled = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    holder.toggleSharing.text = "Error"
                    holder.toggleSharing.isEnabled = false
                }
            }
            holder.progressBar.visibility = View.GONE
        }

        holder.toggleSharing.setOnClickListener {
            holder.progressBar.visibility = View.VISIBLE
            scope.launch {
                val currentlyEnabled = holder.toggleSharing.text.toString().equals("Disable", ignoreCase = true)
                val newStatus = !currentlyEnabled

                val response = RetrofitClient.api.setSharingStatus(bus.bus_id, newStatus)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        holder.toggleSharing.text = if (newStatus) "Disable" else "Enable"
                        Toast.makeText(
                            holder.itemView.context,
                            if (newStatus) "Enabled sharing for ${holder.busId.text}" else "Disabled sharing for ${holder.busId.text}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            holder.itemView.context,
                            "Failed to update status: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                holder.progressBar.visibility = View.GONE
            }
        }

    }

    override fun getItemCount() = buses.size

    private fun getNextStop(busId: String, context: Context): String {
        val prefs = context.getSharedPreferences("next_stops_prefs", Context.MODE_PRIVATE)
        return prefs.getString(busId, "Unknown") ?: "Unknown"
    }
}
