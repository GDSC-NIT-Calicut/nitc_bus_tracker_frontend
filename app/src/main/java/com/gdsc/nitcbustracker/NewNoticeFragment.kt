package com.gdsc.nitcbustracker

import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gdsc.nitcbustracker.data.model.Notice
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import com.gdsc.nitcbustracker.data.network.RetrofitClient.api
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NewNoticeFragment : Fragment() {

    private lateinit var noticeTopic: EditText
    private lateinit var toWhomSpinner: Spinner
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var pushNotificationCheckBox: CheckBox
    private lateinit var spinnerDuration: Spinner
    lateinit var adminName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_notice, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Hide bottom navigation
        (requireActivity() as? AdminActivity)?.hideBottomNav()

        // Initialize views
        noticeTopic = view.findViewById(R.id.noticeTopic)
        toWhomSpinner = view.findViewById(R.id.spinnerToWhom)
        messageEditText = view.findViewById(R.id.editTextMessage)
        sendButton = view.findViewById(R.id.buttonSend)
        pushNotificationCheckBox = view.findViewById(R.id.checkboxNotifs)
        spinnerDuration = view.findViewById(R.id.spinnerDuration)
        val durations = resources.getStringArray(R.array.duration_options)
        val toWhom = resources.getStringArray(R.array.to_whom_options)

        // Create ArrayAdapter
        val adapterDurations = ArrayAdapter(requireContext(), R.layout.spinner_item_icon, durations)
        val adapterToWhom = ArrayAdapter(requireContext(), R.layout.spinner_item_icon, toWhom)
        adapterDurations.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapterToWhom.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuration.adapter = adapterDurations
        toWhomSpinner.adapter = adapterToWhom

        val sharedPref = requireActivity().getSharedPreferences("app_prefs", MODE_PRIVATE)
        val email = sharedPref.getString("admin_email", null)

        // Fetch admin profile from backend
        lifecycleScope.launch {
            try {
                val response = api.getUserInfo(email.toString())
                if (response.isSuccessful) {
                    val adminInfo = response.body()
                    adminName = adminInfo?.name.toString()
                    // Optional: Load image from URL if adminInfo?.photo exists
                }
            } catch (e: Exception) {
                Log.d("GetUserInfo", "Error $e")
            }
        }

        sendButton.setOnClickListener {
            val topic = noticeTopic.text.toString().trim()
            val name = adminName
            val toWhom = toWhomSpinner.selectedItem.toString()
            val message = messageEditText.text.toString().trim()

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val timestamp = isoFormat.format(Date())

            if (name?.isEmpty() == true || message.isEmpty()) {
                Toast.makeText(requireActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedDuration = spinnerDuration.selectedItem.toString()
            val validTill: String? = when (selectedDuration) {
                "Permanent" -> null
                "6 hours" -> calculateFutureISO(60 * 6)
                "12 hours" -> calculateFutureISO(60 * 12)
                "1 day" -> calculateFutureISO(60 * 24)
                "7 days" -> calculateFutureISO(60 * 24 * 7)
                else -> null
            }

            val request = Notice(name.toString(), topic, toWhom, message, timestamp, validTill)

            // Push Notification
            if (pushNotificationCheckBox.isChecked) {
                val call: Call<Void>? = when (toWhom) {
                    "Students" -> RetrofitClient.api.sendNotificationStudent(request)
                    "Driver" -> RetrofitClient.api.sendNotificationDriver(request)
                    "Both" ->  RetrofitClient.api.sendNotificationBoth(request)
                    else -> null
                }

                call?.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireActivity(), "Notification sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireActivity(), "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(requireActivity(), "Error: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
            }

            // Update Notice
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.updateNotices(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireActivity(), response.body()?.message ?: "Successfully Registered", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireActivity(), response.body()?.message ?: "Server Error", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireActivity(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            Toast.makeText(requireActivity(), "Notice Sent:\nTo: $toWhom", Toast.LENGTH_LONG).show()
        }
    }

    fun calculateFutureISO(minutesFromNow: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, minutesFromNow)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        return isoFormat.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation again when leaving the fragment
        (requireActivity() as? AdminActivity)?.showBottomNav()
    }

}
