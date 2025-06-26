package com.gdsc.nitcbustracker

import com.gdsc.nitcbustracker.data.network.RetrofitClient.api
import android.Manifest
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationRequest
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import com.gdsc.nitcbustracker.data.model.BusLocation
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import jp.wasabeef.blurry.Blurry

class DriverTrackingFragment : Fragment(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var locationUpdatesStarted = false
    private lateinit var selectedHostel: String

    // Map-related variables
    private lateinit var mapView: MapView
    private lateinit var blurredMap: ImageView
    private var googleMap: GoogleMap? = null

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

        // Initialize MapView and blurred overlay
        mapView = view.findViewById(R.id.mapView)
        blurredMap = view.findViewById(R.id.blurredMap)

        var mapViewBundle: Bundle? = savedInstanceState?.getBundle(MAP_VIEW_BUNDLE_KEY)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val enableToggle = view.findViewById<MaterialButton>(R.id.btnEnable)
        val disableToggle = view.findViewById<MaterialButton>(R.id.btnDisable)
        val spinner = view.findViewById<Spinner>(R.id.spinnerBuses)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, v: View?, position: Int, id: Long
            ) {
                selectedHostel = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedHostel = ""
            }
        }

        enableToggle.setOnClickListener {
            if (::selectedHostel.isInitialized && selectedHostel.isNotEmpty()) {
                startLocationUpdates()
            } else {
                Toast.makeText(requireContext(), "Select a hostel before enabling tracking", Toast.LENGTH_SHORT).show()
            }
        }

        disableToggle.setOnClickListener {
            stopLocationUpdates()
        }

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val loc = BusLocation(
                    bus_id = selectedHostel,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = location.time.toString()
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
        map.setOnCameraIdleListener {
            captureAndBlurSnapshot()
            map.setOnCameraIdleListener(null)
        }
    }

    private fun captureAndBlurSnapshot() {
        googleMap?.snapshot { bitmap ->
            Blurry.with(requireContext())
                .radius(15)
                .from(bitmap)
                .into(blurredMap)
            blurredMap.visibility = View.VISIBLE
            mapView.visibility = View.GONE
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
