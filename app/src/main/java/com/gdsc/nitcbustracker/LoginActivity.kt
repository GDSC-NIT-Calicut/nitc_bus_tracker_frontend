package com.gdsc.nitcbustracker

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.*
import java.io.IOException
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.gdsc.nitcbustracker.data.model.LoginRequest
import com.gdsc.nitcbustracker.data.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rememberMeCheckbox: MaterialCheckBox

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "LoginPrefs"

    private val RC_SIGN_IN = 100
    private val PERMISSION_REQUEST_CODE = 100

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.INTERNET
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Views
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        val btnGoogleSignIn = findViewById<MaterialButton>(R.id.btnGoogleSignIn)

        rememberMeCheckbox = findViewById(R.id.cbRememberMe)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Load saved credentials
        val savedUsername = sharedPreferences.getString("email", null)
        val savedPassword = sharedPreferences.getString("password", null)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        
        if (savedUsername != null && savedPassword != null) {
            // Autofill the fields
            etUsername.setText(savedUsername)
            etPassword.setText(savedPassword)
            rememberMeCheckbox.isChecked = true
        }

        if (!hasAllPermissions()) {
            requestPermissions()
        }

        // Google Sign-In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            val email = account.email
            val name = account.displayName

            if (email != null) {
                lifecycleScope.launch {
                    try {
                        val userInfoResponse = withContext(Dispatchers.IO) {
                            RetrofitClient.api.getUserInfo(email)
                        }
                        if (userInfoResponse.isSuccessful) {
                            val userInfo = userInfoResponse.body()
                            if (userInfo?.needsMoreInfo == true) {
                                // Registration incomplete: force to RegisterActivity
                                val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                                intent.putExtra("prefill_name", name)
                                intent.putExtra("prefill_email", email)
                                startActivity(intent)
                                finish()
                            } else {
                                // Registration complete: proceed to StudentActivity
                                val intent = Intent(this@LoginActivity, StudentActivity::class.java)
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Welcome back, $name",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("logged_in", false)
        val role = prefs.getString("role", "")

        Log.d("LoginDebug", "onCreate - logged_in: $isLoggedIn, role: $role")

        if (isLoggedIn && role != null) {
            val intent = when (role) {
                "admin" -> Intent(this, AdminActivity::class.java)
                "driver" -> Intent(this, DriverActivity::class.java)
                else -> null
            }
            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(it)
                finish()
            }
        }

        FirebaseMessaging.getInstance().subscribeToTopic("notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to notifications")
                }
            }

        btnGoogleSignIn.setOnClickListener {
            googleSignInClient.signOut()
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        btnLogin.setOnClickListener {
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (rememberMeCheckbox.isChecked) {
                // Save credentials
                sharedPreferences.edit {
                    putString("email", email)
                        .putString("password", password)
                }
            } else {
                // Clear credentials if unchecked
                sharedPreferences.edit {
                    remove("email")
                        .remove("password")
                }
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.login(LoginRequest(email, password))
                }
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true) {
                        prefs.edit { putBoolean("logged_in", true) }
                        when (loginResponse.role) {
                            "admin" -> {
                                prefs.edit {
                                    putString("role", "admin")
                                    putString("admin_email", email)
                                }
                                startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
                                finish()
                            }
                            "driver" -> {
                                prefs.edit {
                                    putString("role", "driver")
                                    putString("driver_email", email)
                                }
                                val intent = Intent(this@LoginActivity, DriverActivity::class.java)
                                intent.putExtra("busId", loginResponse.busId)
                                startActivity(intent)
                                finish()
                            }
                            "student" -> {
                                prefs.edit { putString("role", "student") }
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Student login is only available via Google Sign-In",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            "default" -> {
                                prefs.edit { putString("role", "default") }
                                val intent = Intent(this@LoginActivity, StudentActivity::class.java)
                                startActivity(intent)
                                finish()

                            }
                            else -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Unknown role",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val email = account.email
                val name = account.displayName

                if (email != null) {
                    // Check with server
                    lifecycleScope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.api.checkUserExists(email)
                            }

                            if (response.isSuccessful) {
                                val exists = response.body()?.get("exists") == true
                                if (exists) {
                                    // User exists → go to StudentActivity
                                    startActivity(Intent(this@LoginActivity, StudentActivity::class.java))
                                    Toast.makeText(this@LoginActivity, "Welcome back, ${account.displayName}", Toast.LENGTH_SHORT).show()
                                } else if (email.endsWith("@nitc.ac.in")) {
                                    // Auto-add user and go to RegisterActivity
                                    try {
                                        val createResponse = withContext(Dispatchers.IO) {
                                            RetrofitClient.api.partialRegistration(mapOf("email" to email))
                                        }

                                        if (createResponse.isSuccessful) {
                                            Toast.makeText(this@LoginActivity, "Welcome back, ${account.displayName}", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                                            intent.putExtra("prefill_name", name)
                                            intent.putExtra("prefill_email", email)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(this@LoginActivity, "Couldn't auto-register", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@LoginActivity, "Error registering: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this@LoginActivity, "Only NITC emails are allowed", Toast.LENGTH_SHORT).show()
                                }
                            }


                        } catch (e: Exception) {
                            Toast.makeText(this@LoginActivity, "Server error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode}")
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE)
    }

}
