package com.gdsc.nitcbustracker.data.model

data class Bus(
    val bus_id: String,
    val license_number: String,
    val capacity: Int,
    val assigned_route_id: Int
)
