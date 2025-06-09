package com.gdsc.nitcbustracker

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gdsc.nitcbustracker.adapter.NoticeAdapter
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdminNoticeFragment : Fragment() {
    private lateinit var addNotice: ImageView

    private lateinit var recyclerView: RecyclerView
    private lateinit var noticeAdapter: NoticeAdapter
    private lateinit var emptyNotice: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_notice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.noticeRecyclerView)
        addNotice = view.findViewById(R.id.addNoticeButton)



        // Setup adapter with empty list initially
        noticeAdapter = NoticeAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = noticeAdapter

        emptyNotice = view.findViewById(R.id.noticeEmpty)

        lifecycleScope.launch {
            while (isActive) {
                try {
                    val response = RetrofitClient.api.getNotices()
                    val notices = response.body()?.takeLast(10)?.reversed() // Take last 10 records
                    Log.d("GetNoticeFragment", "Fetched notices: $notices")

                    if (!notices.isNullOrEmpty()) {
                        emptyNotice.text = ""
                        noticeAdapter.updateNotices(notices)
                    }
                } catch (e: Exception) {
                    Log.e("GetNoticeFragment", "Failed to fetch notices", e)
                }

                delay(10000L) // Refresh every 10 seconds
            }
        }


        addNotice.setOnClickListener {
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.nav_admin_fragment, NewNoticeFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }
}