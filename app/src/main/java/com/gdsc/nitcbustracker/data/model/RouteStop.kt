package com.gdsc.nitcbustracker.data.model

data class RouteStop(
    val route_id: Int,
    val stop_id: Int,
    val stop_name: String,
    val stop_order: Int,
    val eta_offset_minutes: Int
)
