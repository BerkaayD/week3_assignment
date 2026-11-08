package com.example.week3assignment.data

enum class DeviceCategory { LIGHT, CAMERA, THERMOSTAT, VACUUM }

data class HardwareItem(
    val id: Int,
    val name: String,
    val category: DeviceCategory,
    val room: String,
    val isOnline: Boolean,
    val isOn: Boolean,
    val powerWatts: Double,
)
