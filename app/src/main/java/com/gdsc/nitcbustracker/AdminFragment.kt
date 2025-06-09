package com.gdsc.nitcbustracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gdsc.nitcbustracker.adapter.BusStatusAdapter
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BusStatusAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.busStatusRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        fetchBusStatuses()


    }

    private fun fetchBusStatuses() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getBusStatuses()
                }

                if (response.isSuccessful) {
                    val busList = response.body() ?: emptyList()
                    adapter = BusStatusAdapter(busList)
                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(requireActivity(), "Failed to fetch bus data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireActivity(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}