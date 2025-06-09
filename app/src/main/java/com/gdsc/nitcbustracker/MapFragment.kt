package com.gdsc.nitcbustracker

import com.gdsc.nitcbustracker.data.network.RetrofitClient
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.gdsc.nitcbustracker.data.model.Bus
import com.gdsc.nitcbustracker.data.model.BusLocation
import com.gdsc.nitcbustracker.data.model.BusStatus
import com.gdsc.nitcbustracker.data.model.RouteStop
import com.gdsc.nitcbustracker.data.model.Stop
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.collections.get
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapFragment : Fragment(), OnMapReadyCallback {

    data class StopPair(val currentStop: Stop?, val nextStop: Stop?)
    private val busDirectionMap = mutableMapOf<String, Boolean>() // true = forward, false = reverse
    private val busLastSeenMap = mutableMapOf<String, Pair<LatLng, Long>>() // bus_id -> (LatLng, timestamp)



    private var mapFragment: SupportMapFragment? = null
    private lateinit var map: GoogleMap
    private val markerMap = mutableMapOf<String, Marker>()
    private val updateInterval = 4000L
    private var updateJob: Job? = null
    private var lastBusDetails: Map<String, Bus> = emptyMap()
    private var lastLocations: List<BusLocation> = emptyList()
    private var hasZoomedToMarkers = false
    private var stops: List<Stop> = emptyList()
    private val routeStopsCache = mutableMapOf<Int, List<RouteStop>>()

    private var busIdText: TextView? = null
    private var statusText: TextView? = null
    private var stopText: TextView? = null
    private var nextStopText: TextView? = null
    private var etaText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_student_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        busIdText = view.findViewById(R.id.title)
        statusText = view.findViewById(R.id.runningStatus)
        stopText = view.findViewById(R.id.current_stop_text)
        nextStopText = view.findViewById(R.id.next_stop_text)
        etaText = view.findViewById(R.id.eta_text_view)

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.map, mapFragment!!)
                .commit()
        }
        mapFragment?.getMapAsync(this)
        startRepeatingUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(11.320436, 75.934947), 16f))

        map.setOnMarkerClickListener { marker ->
            val busId = marker.title?.substringAfter("Bus ID: ")?.trim()
            val bus = lastBusDetails[busId]
            val location = lastLocations.find { it.bus_id == busId }
            if (bus != null && location != null) {
                // Launch coroutine to call suspend function and update UI
                viewLifecycleOwner.lifecycleScope.launch {
                    val routeStops = getRouteStopsForBus(bus)
                    val (currentStop, nextStop) = findCurrentAndNextStop(location, routeStops)
                    updateInfoCard(location, bus, nextStop)
                }
            }
            false
        }

        resetInfoCard()
        map.setOnMapClickListener {
            resetInfoCard()
        }
    }

    private fun startRepeatingUpdates() {
        updateJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                stops = RetrofitClient.api.getStops()
            } catch (e: Exception) {
                Log.e("MapFragment", "Error fetching stops", e)
            }
            while (isActive) {
                try {
                    val locationsResponse = RetrofitClient.api.getLocations()
                    if (locationsResponse.isSuccessful) {
                        val rawLocations = locationsResponse.body() ?: emptyList()
                        lastLocations = rawLocations.distinctBy { it.bus_id }

                        val busDetailsList = lastLocations.map { location ->
                            async {
                                try {
                                    val response = RetrofitClient.api.getBusDetails(location.bus_id)
                                    if (response.isSuccessful) response.body() else null
                                } catch (e: Exception) {
                                    Log.e("MapFragment", "Error fetching bus ${location.bus_id}", e)
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()

                        lastBusDetails = busDetailsList.associateBy { it.bus_id }

                        updateBusMarkers(lastLocations)

                        lastLocations.forEach { location ->
                            val bus = lastBusDetails[location.bus_id] ?: return@forEach

                            launch {
                                val routeStops = getRouteStopsForBus(bus)
                                val (currentStop, nextStop) = findCurrentAndNextStop(location, routeStops)

                                Log.d("BusDebug", "Bus ${bus.bus_id} Current Stop: ${currentStop?.name ?: "Unknown"}")
                                Log.d("BusDebug", "Bus ${bus.bus_id} Next Stop: ${nextStop?.name ?: "Unknown"}")

                                val speed = calculateSpeed(location.bus_id, location.latitude, location.longitude)
                                val eta = calculateEtaWithSpeed(location.latitude, location.longitude, nextStop, speed)


                                val isMoving = hasLocationChanged(location.bus_id, location.latitude, location.longitude)
                                val statusText = if (isMoving) "Running" else "Not Running"

                                val busStatus = BusStatus(
                                    bus_id = bus.bus_id,
                                    current_stop_name = currentStop?.name ?: "Unknown",
                                    eta = eta,
                                    isRunning = statusText
                                )


                                try {
                                    val response = RetrofitClient.api.postBusStatuses(busStatus)
                                    if (response.isSuccessful) {
                                        Log.d("BusDebug", "Posted BusStatus: $busStatus")
                                    } else {
                                        Log.e("BusDebug", "Failed to post BusStatus: ${response.errorBody()?.string()}")
                                    }
                                } catch (e: Exception) {
                                    Log.e("BusDebug", "Error posting BusStatus", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MapFragment", "Failed to fetch bus locations", e)
                }
                delay(updateInterval)
            }
        }
    }

    private fun updateBusMarkers(locations: List<BusLocation>) {
        // Remove markers for buses not present anymore
        val currentBusIds = locations.map { it.bus_id }.toSet()
        markerMap.keys.filterNot { currentBusIds.contains(it) }.forEach { removedId ->
            markerMap[removedId]?.remove()
            markerMap.remove(removedId)
        }

        // Add/update markers for current buses
        locations.forEach { location ->
            val position = LatLng(location.latitude, location.longitude)
            val marker = markerMap[location.bus_id]
            if (marker == null) {
                map.addMarker(
                    MarkerOptions().position(position).title("Bus ID: ${location.bus_id}")
                )?.let { markerMap[location.bus_id] = it
                    Log.d("BusDebug", "Bus ${location.bus_id}: Lat=${location.latitude}, Lng=${location.longitude}" )
                }
            } else {
                marker.position = position
            }
        }

        // --- Auto-zoom to fit all markers only ONCE ---
        if (!hasZoomedToMarkers && markerMap.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            markerMap.values.forEach { marker ->
                builder.include(marker.position)
            }
            val bounds = builder.build()
            val padding = 100 // pixels, adjust as needed
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            hasZoomedToMarkers = true
        }
    }

    private fun updateInfoCard(location: BusLocation, bus: Bus, nextStop: Stop?) {
        val speed = calculateSpeed(location.bus_id, location.latitude, location.longitude)
        val eta = calculateEtaWithSpeed(location.latitude, location.longitude, nextStop, speed)

        val nearestStop = findNearestStop(location.latitude, location.longitude)

        val isMoving = hasLocationChanged(bus.bus_id, location.latitude, location.longitude)
        statusText?.text = if (isMoving) "Running" else "Not Running"


        // Log for debug
        if (nearestStop != null) {
            val dist = distanceBetween(
                location.latitude, location.longitude,
                nearestStop.latitude.toDouble(), nearestStop.longitude.toDouble()
            )
            Log.d(
                "BusDebug",
                "Bus ${bus.bus_id}: Lat=${location.latitude}, Lng=${location.longitude} | Nearest stop: ${nearestStop.name} (${nearestStop.latitude}, ${nearestStop.longitude}) at ${"%.2f".format(dist)} km"
            )
        } else {
            Log.d(
                "BusDebug",
                "Bus ${bus.bus_id}: Lat=${location.latitude}, Lng=${location.longitude} | Nearest stop: NONE"
            )
        }

        // Update UI
        busIdText?.text = "Bus ${bus.bus_id}"
        stopText?.text = "Current Stop: ${nearestStop?.name ?: "Unknown"}"
        nextStopText?.text = "Next Stop: ${nextStop?.name ?: "Unknown"}"
        etaText?.text = "$eta mins"
    }

    private fun resetInfoCard() {
        busIdText?.text = "Tracking"
        etaText?.text = "--"
        stopText?.text = "Current Stop: --"
        nextStopText?.text = "Next Stop: --"
        statusText?.text = "--"
    }

    private fun findNearestStop(lat: Double, lng: Double): Stop? {
        if (stops.isEmpty()) {
            Log.d("BusDebug", "No stops loaded")
            return null
        }
        stops.forEach { stop ->
            val dist = distanceBetween(lat, lng, stop.latitude.toDouble(), stop.longitude.toDouble())
            Log.d("BusDebug", "Dist to ${stop.name}: $dist km")
        }
        return stops.minByOrNull { stop ->
            distanceBetween(lat, lng, stop.latitude.toDouble(), stop.longitude.toDouble())
        }
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun findCurrentAndNextStop(
        location: BusLocation,
        routeStops: List<RouteStop>
    ): StopPair {
        if (routeStops.isEmpty()) return StopPair(null, null)

        val currentRouteStop = routeStops.minByOrNull {
            val stop = stops.find { s -> s.stop_id == it.stop_id }
            if (stop != null) {
                distanceBetween(location.latitude, location.longitude, stop.latitude.toDouble(), stop.longitude.toDouble())
            } else {
                Double.MAX_VALUE
            }
        } ?: return StopPair(null, null)

        val currentOrder = currentRouteStop.stop_order

        // Default direction is forward
        val isForward = busDirectionMap.getOrPut(location.bus_id) { true }

        // Determine the max order
        val maxOrder = routeStops.maxOfOrNull { it.stop_order ?: 0 } ?: currentOrder
        val minOrder = routeStops.minOfOrNull { it.stop_order ?: 0 } ?: currentOrder

        // Update direction at ends
        if (currentOrder >= maxOrder) {
            busDirectionMap[location.bus_id] = false
        } else if (currentOrder <= minOrder) {
            busDirectionMap[location.bus_id] = true
        }

        val nextOrder = if (busDirectionMap[location.bus_id] == true) currentOrder + 1 else currentOrder - 1

        val nextRouteStop = routeStops.find { it.stop_order == nextOrder }

        val current = stops.find { it.stop_id == currentRouteStop.stop_id }
        val next = stops.find { it.stop_id == nextRouteStop?.stop_id }

        return StopPair(current, next)
    }


    private suspend fun getRouteStopsForBus(bus: Bus): List<RouteStop> {
        val routeId = bus.assigned_route_id

        routeStopsCache[routeId]?.let { return it }

        return try {
            val response = RetrofitClient.api.getRouteStops(routeId)
            if (response.isSuccessful) {
                val stops = response.body() ?: emptyList()
                routeStopsCache[routeId] = stops
                stops
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Failed to fetch route stops for route $routeId", e)
            emptyList()
        }
    }

    private fun hasLocationChanged(busId: String, newLat: Double, newLng: Double): Boolean {
        val newLocation = LatLng(newLat, newLng)
        val currentTime = System.currentTimeMillis()

        val (lastLocation, lastTimestamp) = busLastSeenMap[busId] ?: Pair(newLocation, currentTime)
        val distance = distanceBetween(
            newLat, newLng,
            lastLocation.latitude, lastLocation.longitude
        )

        return if (distance > 0.05) { // change threshold ~50 meters
            busLastSeenMap[busId] = Pair(newLocation, currentTime)
            true
        } else {
            val tenMinutesAgo = currentTime - (10 * 60 * 1000)
            busLastSeenMap[busId] = Pair(lastLocation, lastTimestamp)
            lastTimestamp > tenMinutesAgo // true if movement within 10 mins
        }
    }


    private fun calculateEtaWithSpeed(
        currentLat: Double,
        currentLng: Double,
        nextStop: Stop?,
        speedKmPerHour: Double
    ): Int {
        if (nextStop == null || speedKmPerHour <= 0.1) return (5..10).random() // fallback/mock

        val distance = distanceBetween(
            currentLat, currentLng,
            nextStop.latitude.toDouble(),
            nextStop.longitude.toDouble()
        ) // in km

        val etaHours = distance / speedKmPerHour
        val etaMinutes = (etaHours * 60).toInt()
        return etaMinutes.coerceIn(1, 60)
    }


    private fun calculateSpeed(busId: String, newLat: Double, newLng: Double): Double {
        val newTime = System.currentTimeMillis()
        val newLocation = LatLng(newLat, newLng)

        val (lastLocation, lastTime) = busLastSeenMap[busId] ?: return 0.0

        val distance = distanceBetween(newLat, newLng, lastLocation.latitude, lastLocation.longitude) // in km
        val timeDiffHours = (newTime - lastTime).toDouble() / (1000 * 60 * 60) // in hours

        return if (timeDiffHours > 0) distance / timeDiffHours else 0.0 // km/h
    }


    override fun onDestroyView() {
        super.onDestroyView()
        updateJob?.cancel()
    }
}
