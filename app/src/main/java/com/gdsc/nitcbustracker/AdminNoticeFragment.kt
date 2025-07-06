package com.gdsc.nitcbustracker

import android.content.Context.MODE_PRIVATE
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
        return inflater.inflate(R.layout.fragment_admin_notice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.noticeRecyclerView)
        addNotice = view.findViewById(R.id.addNoticeButton)
        emptyNotice = view.findViewById(R.id.noticeEmpty)

        val sharedPref = requireActivity().getSharedPreferences("app_prefs", MODE_PRIVATE)
        val role = sharedPref.getString("role", null) ?: ""

        noticeAdapter = NoticeAdapter(emptyList(), role) { noticeToDelete ->

            // Show confirmation dialog
            val context = requireContext()
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to delete the notice titled \"${noticeToDelete.topic}\"?")
                .setPositiveButton("Delete") { dialog, _ ->
                    dialog.dismiss()
                    lifecycleScope.launch {
                        try {
                            val response = RetrofitClient.api.deleteNotice(noticeToDelete.topic)
                            if (response.isSuccessful) {
                                val updatedList = noticeAdapter.getNotices()
                                    .filter { it.topic != noticeToDelete.topic }
                                noticeAdapter.updateNotices(updatedList)
                            } else {
                                Log.e("AdminNoticeFragment", "Failed to delete: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e("AdminNoticeFragment", "Exception during delete", e)
                        }
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss() // just close dialog
                }
                .show()
        }


        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = noticeAdapter

        lifecycleScope.launch {
            while (isActive) {
                fetchNotices()
                delay(10000L) // refresh every 10s
            }
        }

        addNotice.setOnClickListener {
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.nav_admin_fragment, NewNoticeFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    private suspend fun fetchNotices() {
        try {
            val response = RetrofitClient.api.getNotices()
            val notices = response.body()?.takeLast(10)?.reversed()
            if (!notices.isNullOrEmpty()) {
                emptyNotice.text = ""
                noticeAdapter.updateNotices(notices)
            } else {
                emptyNotice.text = "No notices available."
                noticeAdapter.updateNotices(emptyList())
            }
        } catch (e: Exception) {
            Log.e("AdminNoticeFragment", "Failed to fetch notices", e)
        }
    }
}
