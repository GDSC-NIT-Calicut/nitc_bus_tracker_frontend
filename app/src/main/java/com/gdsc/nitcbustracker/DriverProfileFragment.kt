package com.gdsc.nitcbustracker

import com.gdsc.nitcbustracker.data.network.RetrofitClient.api
import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DriverProfileFragment : Fragment() {

    private lateinit var profileName: EditText
    private lateinit var profileEmail: EditText
    private lateinit var profilePhoneNo: EditText
    private lateinit var profileRole: EditText
    private lateinit var profilePhoto: ImageView
    private lateinit var logoutButtonDriver: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        profilePhoneNo = view.findViewById(R.id.profile_phone_no)
        profileRole = view.findViewById(R.id.profile_role)
        profilePhoto = view.findViewById(R.id.profile_image)
        logoutButtonDriver = view.findViewById(R.id.logout_button)

        val sharedPref = requireActivity().getSharedPreferences("app_prefs", MODE_PRIVATE)
        val email = sharedPref.getString("driver_email", null)
        val role = sharedPref.getString("role", null)

        // Fetch admin profile from backend
        lifecycleScope.launch {
            try {
                val response = api.getUserInfo(email.toString())
                if (response.isSuccessful) {
                    val driverInfo = response.body()
                    profileName.setText(driverInfo?.name)
                    profileEmail.setText(driverInfo?.email)
                    profilePhoneNo.setText(driverInfo?.phone)
                    profileRole.setText(role)
                    // Optional: Load image from URL if adminInfo?.photo exists
                } else {
                    showErrorDefaults()
                }
            } catch (e: Exception) {
                showErrorDefaults()
            }
        }

        logoutButtonDriver.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("app_prefs", MODE_PRIVATE)

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(Html.fromHtml("<font color='#000000'>Logout</font>"))
                .setMessage(Html.fromHtml("<font color='#000000'>Are you sure you want to logout?</font>"))
                .setPositiveButton("Yes") { dialog, _ ->
                    sharedPref.edit { clear() }
                    goToLogin()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .show()

            dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_card)
        }

    }

    private fun showErrorDefaults() {
        profileName.setText("Default Name")
        profileEmail.setText("Default Email")
        profilePhoneNo.setText("Default Phone")
        profileRole.setText("Default Hostel")
    }

    private fun goToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
