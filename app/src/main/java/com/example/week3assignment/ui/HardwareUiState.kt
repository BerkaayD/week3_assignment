package com.example.week3assignment.ui

import com.example.week3assignment.data.HardwareItem

data class HardwareUiState(
    val visibleDevices: List<HardwareItem>,
    val totalPowerWatts: Double,
    val searchQuery: String,
    val selectedRoom: String?,
    val roomOptions: List<String>,
    val isEmptyResult: Boolean,
)
