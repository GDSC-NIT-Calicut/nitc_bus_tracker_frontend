package com.gdsc.nitcbustracker

import com.gdsc.nitcbustracker.data.network.RetrofitClient.api
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.Visibility
import com.gdsc.nitcbustracker.RegisterActivity
import com.gdsc.nitcbustracker.data.model.EditRequest
import com.gdsc.nitcbustracker.data.model.RegisterRequest
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import com.gdsc.nitcbustracker.data.network.RetrofitClient.api
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import com.google.android.gms.auth.api.signin.GoogleSignInClient


class StudentProfileFragment : Fragment() {

    lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_student_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val profileName = view.findViewById<EditText>(R.id.profile_student_name)
        val profileEmail = view.findViewById<EditText>(R.id.profile_student_email)
        val profilePhoneNo = view.findViewById<EditText>(R.id.profile_student_phone_no)
        val profileHostel = view.findViewById<EditText>(R.id.profile_student_hostel)
        val profilePhoto = view.findViewById<ImageView>(R.id.profile_student_image)
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(requireContext())
        val email = account?.email
        val photo_url = account?.photoUrl
        val editIcon = view.findViewById<ImageView>(R.id.edit_icon)
        val saveButton = view.findViewById<Button>(R.id.save_student_button)

        profileName.isEnabled = false
        profileHostel.isEnabled = false
        profilePhoneNo.isEnabled = false
        profileEmail.isEnabled = false

        // Initialize GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val logoutButtonStudent = view.findViewById<Button>(R.id.logout_student_button)

        logoutButtonStudent.setOnClickListener {
            //val sharedPref = requireActivity().getSharedPreferences("app_prefs", AppCompatActivity.MODE_PRIVATE)
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(Html.fromHtml("<font color='#000000'>Logout</font>"))
                .setMessage(Html.fromHtml("<font color='#000000'>Are you sure you want to logout?</font>"))
                .setPositiveButton("Yes") { _, _ ->

                    //sharedPref.edit().clear().apply()
                    googleSignInClient.signOut().addOnCompleteListener {
                        val intent = Intent(requireActivity(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }
                .setNegativeButton("No", null)
                .show()

            dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_card)

        }

        editIcon.setOnClickListener {
            Toast.makeText(requireContext(), "Switched to Editing Mode!", Toast.LENGTH_SHORT).show()
            profileName.isEnabled = true
            profileHostel.isEnabled = true
            profilePhoneNo.isEnabled = true
            logoutButtonStudent.visibility = View.GONE
            saveButton.visibility = View.VISIBLE
        }

        saveButton.setOnClickListener {
            val name = profileName.text.toString().trim()
            val hostel = profileHostel.text.toString().trim()
            val phone = profilePhoneNo.text.toString().trim()
            val email = profileEmail.text.toString().trim()

            if (name.isEmpty() || hostel.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = EditRequest(name, email, hostel, phone)

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.editProfile(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), response.body()?.message ?: "Profile Edit Successful", Toast.LENGTH_SHORT).show()
                        profileName.isEnabled = false
                        profileHostel.isEnabled = false
                        profilePhoneNo.isEnabled = false
                        logoutButtonStudent.visibility = View.VISIBLE
                        saveButton.visibility = View.GONE

                    } else {
                        Toast.makeText(requireContext(), response.body()?.message ?: "Server Error", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Load profile photo manually
        if (photo_url != null) {
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = URL(photo_url.toString()).openStream()
                        BitmapFactory.decodeStream(inputStream)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    profilePhoto.setImageBitmap(bitmap)
                }
            }
        } else {
            // No photo URL at all, set default empty profile pic
            profilePhoto.setImageResource(R.drawable.empty_profile_pic)
        }

        // Launch coroutine here using lifecycleScope
        lifecycleScope.launch {
            val response = api.getUserInfo(email.toString())
            if (response.isSuccessful) {
                val userInfo = response.body()

                profileName.setText(userInfo?.name)
                profileEmail.setText(userInfo?.email)
                profilePhoneNo.setText(userInfo?.phone)
                profileHostel.setText(userInfo?.hostel)
            } else {
                profileName.setText("Default Name")
                profileEmail.setText("Default Email")
                profilePhoneNo.setText("Default Phone Number")
                profileHostel.setText("Default Hostel")
            }
        }
    }
}
