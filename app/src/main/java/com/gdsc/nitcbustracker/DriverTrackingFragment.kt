package com.gdsc.nitcbustracker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.gdsc.nitcbustracker.data.network.RetrofitClient.api
import com.gdsc.nitcbustracker.services.LocationService
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import androidx.core.content.edit

class DriverTrackingFragment : Fragment(), OnMapReadyCallback {

    private lateinit var selectedBus: String

    private lateinit var mapView: MapView
    private lateinit var blurredMap: ImageView
    private var googleMap: GoogleMap? = null

    private lateinit var enableToggle: TextView
    private lateinit var disableToggle: TextView

    private var adminCheckerJob: Job? = null

    companion object {
        private const val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_driver_tracking, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("bus_tracker_prefs", 0)
        val savedBus = prefs.getString("selected_bus", "") ?: ""
        val isEnabled = prefs.getBoolean("is_location_sharing_enabled", false)

        mapView = view.findViewById(R.id.mapView)
        blurredMap = view.findViewById(R.id.blurredMap)

        enableToggle = view.findViewById(R.id.btnEnable)
        disableToggle = view.findViewById(R.id.btnDisable)

        val mapViewBundle: Bundle? = savedInstanceState?.getBundle(MAP_VIEW_BUNDLE_KEY)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        val spinner = view.findViewById<Spinner>(R.id.busList)

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.bus_list,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, v: View?, position: Int, id: Long
            ) {
                selectedBus = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedBus = ""
            }
        }

        if (savedBus.isNotEmpty()) {
            selectedBus = savedBus

            if (isEnabled) {
                selectEnable()
                startAdminChecker()
            } else {
                stopLocationService()
                selectDisable()
                stopAdminChecker()
            }
        } else {
            stopLocationService()
            selectDisable()
        }


        enableToggle.setOnClickListener {
                saveSharingState(true)
                if (::selectedBus.isInitialized && selectedBus.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val response = api.getSharingStatus(selectedBus)
                            if (response.isSuccessful) {
                                val isSharing = response.body()?.isSharing == true
                                if (isSharing) {
                                    startLocationService()
                                    selectEnable()
                                } else {
                                    stopLocationService()
                                    selectDisable()
                                }
                            } else {
                                stopLocationService()
                                selectDisable()
                            }
                        } catch (e: Exception) {
                            stopLocationService()
                            selectDisable()
                        }
                    }
                } else {
                    // No bus selected yet, disable location sharing by default
                    stopLocationService()
                    selectDisable()
                }
            }

        disableToggle.setOnClickListener {
            saveSharingState(false)
            stopLocationService()
            stopAdminChecker()
            selectDisable()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationService() {
        val intent = Intent(requireContext(), LocationService::class.java)
        intent.putExtra("bus_id", selectedBus)
        requireContext().startForegroundService(intent)
    }

    private fun stopLocationService() {
        val intent = Intent(requireContext(), LocationService::class.java).apply {
            action = "STOP"
        }
        requireContext().startService(intent)
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val nitc = LatLng(11.3186, 75.9344)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(nitc, 16f))

        map.setOnMapLoadedCallback {
            blurredMap.alpha = 0f
            blurredMap.visibility = View.VISIBLE

            googleMap?.snapshot { bitmap ->
                if (bitmap != null) {
                    Blurry.with(requireContext())
                        .radius(15)
                        .from(bitmap)
                        .into(blurredMap)

                    blurredMap.animate()
                        .alpha(1f)
                        .setDuration(1000)
                        .start()
                } else {
                    Toast.makeText(requireContext(), "Map snapshot failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun selectEnable() {
        enableToggle.apply {
            setBackgroundResource(R.drawable.toggle_right_selected)
            setTextColor(Color.WHITE)
        }
        disableToggle.apply {
            setBackgroundResource(R.drawable.toggle_left_unselected)
            setTextColor(Color.BLACK)
        }
    }

    private fun selectDisable() {
        disableToggle.apply {
            setBackgroundResource(R.drawable.toggle_left_selected)
            setTextColor(Color.WHITE)
        }
        enableToggle.apply {
            setBackgroundResource(R.drawable.toggle_right_unselected)
            setTextColor(Color.BLACK)
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }

    private fun startAdminChecker() {
        adminCheckerJob?.cancel()
        adminCheckerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(5000)
                try {
                    val response = api.getSharingStatus(selectedBus)
                    if (response.isSuccessful) {
                        val isSharingAllowed = response.body()?.isSharing == true
                        if (!isSharingAllowed) {
                            Toast.makeText(requireContext(), "Admin has disabled sharing", Toast.LENGTH_LONG).show()
                            stopLocationService()
                            stopAdminChecker()
                            selectDisable()
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopAdminChecker() {
        adminCheckerJob?.cancel()
        adminCheckerJob = null
    }

    private fun saveSharingState(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("bus_tracker_prefs", 0)
        prefs.edit {
            putBoolean("is_location_sharing_enabled", enabled)
                .putString("selected_bus", selectedBus)
        }
    }

    private val adminDisabledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(requireContext(), "Admin disabled your location sharing", Toast.LENGTH_LONG).show()
            selectDisable()
            stopLocationService()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(
            adminDisabledReceiver,
            IntentFilter("com.gdsc.nitcbustracker.ADMIN_DISABLED"),
            Context.RECEIVER_NOT_EXPORTED
            )
    }


    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(adminDisabledReceiver)
    }
}
