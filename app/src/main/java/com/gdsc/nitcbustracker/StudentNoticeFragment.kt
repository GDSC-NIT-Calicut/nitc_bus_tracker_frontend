package com.gdsc.nitcbustracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gdsc.nitcbustracker.adapter.NoticeAdapter
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StudentNoticeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noticeAdapter: NoticeAdapter
    private lateinit var emptyNotice: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_student_notice, container, false)
    }

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
                    val studentNotices = notices?.filter { it.to_whom == "Students" || it.to_whom == "Both" }
                    if (studentNotices?.isNotEmpty() == true) {
                        val latestStudentNotices = studentNotices.takeLast(10).reversed()
                        Log.d("GetNoticeFragment", "Fetched notices: $latestStudentNotices")
                        emptyNotice.text = ""
                        noticeAdapter.updateNotices(latestStudentNotices)
                    }
                } catch (e: Exception) {
                    Log.e("GetNoticeFragment", "Failed to fetch notices", e)
                }
                delay(10000L)
            }
        }

    }
}
