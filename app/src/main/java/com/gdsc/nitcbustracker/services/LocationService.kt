package com.gdsc.nitcbustracker.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.gdsc.nitcbustracker.DriverActivity
import com.gdsc.nitcbustracker.R
import com.gdsc.nitcbustracker.data.model.BusLocation
import com.gdsc.nitcbustracker.data.network.RetrofitClient.api
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.content.edit

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var adminCheckJob: Job? = null

    private var busId: String = ""

    companion object {
        const val CHANNEL_ID = "bus_tracking_channel"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        busId = intent?.getStringExtra("bus_id") ?: ""

        if (busId.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(1, createNotification())

        startLocationUpdates()
        startAdminCheck()

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, DriverActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bus Tracker")
            .setContentText("Sharing location for bus $busId")
            .setSmallIcon(R.drawable.ic_location_marker)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Bus Tracking Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location: Location = result.lastLocation ?: return
                sendLocationToServer(location)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun sendLocationToServer(location: Location) {
        val busLocation = BusLocation(
            bus_id = busId,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.time.toString(),
            isSharing = true
        )

        api.sendLocation(busLocation).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d("Send Location", "Location sent successfully")
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d("Send Location", "Failed to send location")
            }
        })
    }

    private fun startAdminCheck() {
        adminCheckJob?.cancel()
        adminCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(5000)
                try {
                    val response = api.getSharingStatus(busId)
                    if (response.isSuccessful) {
                        val isSharingAllowed = response.body()?.isSharing ?: false
                        if (!isSharingAllowed) {
                            Log.d("Admin Check", "Admin disabled location sharing.")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@LocationService,
                                    "Admin disabled your location sharing",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            // update shared prefs to reflect disabled state
                            val prefs = getSharedPreferences("bus_tracker_prefs", 0)
                            prefs.edit {
                                putBoolean("is_location_sharing_enabled", false)
                            }

                            stopSelf()
                            return@launch
                        }
                    } else {
                        Log.d("Admin Check", "Error response from server")
                    }
                } catch (e: Exception) {
                    Log.e("Admin Check", "Exception during admin check", e)
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        adminCheckJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
