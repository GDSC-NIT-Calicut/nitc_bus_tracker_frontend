package com.gdsc.nitcbustracker

import com.gdsc.nitcbustracker.data.network.RetrofitClient.api
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.gdsc.nitcbustracker.data.model.BusLocation
import com.gdsc.nitcbustracker.data.model.SharingStatus
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DriverTrackingFragment : Fragment(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var locationUpdatesStarted = false
    private lateinit var selectedBus: String

    private lateinit var mapView: MapView
    private lateinit var blurredMap: ImageView
    private var googleMap: GoogleMap? = null

    private lateinit var enableToggle: TextView
    private lateinit var disableToggle: TextView
    var isSharingEnabled: Boolean = true

    companion object {
        private const val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_driver_tracking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        blurredMap = view.findViewById(R.id.blurredMap)

        enableToggle = view.findViewById(R.id.btnEnable)
        disableToggle = view.findViewById(R.id.btnDisable)

        var mapViewBundle: Bundle? = savedInstanceState?.getBundle(MAP_VIEW_BUNDLE_KEY)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

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

        // Initialize toggle state: Disabled selected by default
        selectDisable()

        enableToggle.setOnClickListener {
            if (::selectedBus.isInitialized && selectedBus.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = api.getSharingStatus(selectedBus)
                        if (response.isSuccessful) {
                            val isSharing = response.body()?.isSharing == true
                            if (isSharing) {
                                startLocationUpdates()
                                selectEnable()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Admin disabled your location sharing",
                                    Toast.LENGTH_LONG
                                ).show()
                                selectDisable()
                            }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failed: ${response.code()} ${response.message()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Error){
                        Toast.makeText(
                            requireContext(),
                            "Catch",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Select a Bus before enabling tracking",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        disableToggle.setOnClickListener {
            stopLocationUpdates()
            selectDisable()
        }

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val loc = BusLocation(
                    bus_id = selectedBus,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = location.time.toString(),
                    isSharing = isSharingEnabled
                )
                api.sendLocation(loc).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Toast.makeText(requireContext(), "Location sent", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(requireContext(), "Failed to send location", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val nitc = LatLng(11.3186, 75.9344)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(nitc, 16f))

        map.setOnMapLoadedCallback {
            // Start with blurredMap invisible
            blurredMap.alpha = 0f
            blurredMap.visibility = View.VISIBLE

            googleMap?.snapshot { bitmap ->
                if (bitmap != null) {
                    Blurry.with(requireContext())
                        .radius(15)
                        .from(bitmap)
                        .into(blurredMap)

                    // Animate fade-in of blurred overlay
                    blurredMap.animate()
                        .alpha(1f)
                        .setDuration(1000) // 1 second fade-in
                        .start()
                } else {
                    Toast.makeText(requireContext(), "Map snapshot failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
        if (!locationUpdatesStarted) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireActivity().mainLooper)
            locationUpdatesStarted = true
        }
    }

    private fun stopLocationUpdates() {
        if (locationUpdatesStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationUpdatesStarted = false
        }
    }

    private fun selectEnable() {
        enableToggle.apply {
            setBackgroundResource(R.drawable.toggle_right_selected) // Make sure you have this drawable
            setTextColor(Color.WHITE)
        }
        disableToggle.apply {
            setBackgroundResource(R.drawable.toggle_left_unselected) // Make sure you have this drawable
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

    // Lifecycle methods below...

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }
}
