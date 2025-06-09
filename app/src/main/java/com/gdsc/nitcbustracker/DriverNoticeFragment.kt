package com.gdsc.nitcbustracker

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gdsc.nitcbustracker.adapter.NoticeAdapter
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DriverNoticeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notice, container, false)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var noticeAdapter: NoticeAdapter
    private lateinit var emptyNotice: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.noticeRecyclerView)
        emptyNotice = view.findViewById(R.id.noticeEmpty)

        // Setup adapter with empty list initially
        noticeAdapter = NoticeAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = noticeAdapter

        lifecycleScope.launch {
            while (isActive) {
                try {
                    val response = RetrofitClient.api.getNotices()
                    val notices = response.body()
                    if (!notices.isNullOrEmpty()) {
                        val driverNotices = notices.filter { it.to_whom == "Driver" || it.to_whom == "Both" }
                        val latestDriverNotices = driverNotices.takeLast(10).reversed()
                        Log.d("GetNoticeFragment", "Fetched notices: $latestDriverNotices")
                        emptyNotice.text = ""
                        noticeAdapter.updateNotices(latestDriverNotices)
                    }
                } catch (e: Exception) {
                    Log.e("GetNoticeFragment", "Failed to fetch notices", e)
                }
                delay(10000L)
            }
        }
    }
}