package com.gdsc.nitcbustracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.gdsc.nitcbustracker.data.model.RegisterRequest
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnRegister: Button
    private lateinit var spinnerRole: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        etName = findViewById(R.id.etName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etPhone = findViewById(R.id.etPhone)
        btnRegister = findViewById(R.id.btnRegister)
        spinnerRole = findViewById(R.id.spinnerRole)
        
        val rootView = findViewById<View>(R.id.register_layout)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBarHeight)
            insets
        }

        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val prefillEmail = intent.getStringExtra("prefill_email")
        val prefillName = intent.getStringExtra("prefill_name")
        if (!prefillEmail.isNullOrEmpty() && !prefillName.isNullOrEmpty()) {
            etEmail.setText(prefillEmail)
            etName.setText(prefillName)
            etName.isEnabled = false
        }
        val role = "student"
        val hostels = listOf("MBH1", "MBH2", "MLH")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hostels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val hostel = spinnerRole.selectedItem.toString().uppercase()
            val phone = etPhone.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.endsWith("@nitc.ac.in")) {
                Toast.makeText(this, "Only NITC email allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(name, username, password, role, hostel, email, phone)

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.completeRegistration(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@RegisterActivity, response.body()?.message ?: "Successfully Registered", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, StudentActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, response.body()?.message ?: "Username or email already exists", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
