package com.gdsc.nitcbustracker.data.model

data class Notice(
    val name: String,
    val topic: String,
    val to_whom: String,
    val message: String,
    val timestamp: String
)