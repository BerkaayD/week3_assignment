package com.example.week3assignment.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class InMemoryHardwareRepository(
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val _devices = MutableStateFlow(
        listOf(
            HardwareItem(1, "Köşe Lambası", DeviceCategory.LIGHT, "Salon", true, true, 15.0),
            HardwareItem(2, "Tavan Avizesi", DeviceCategory.LIGHT, "Salon", true, false, 60.0),
            HardwareItem(3, "Bahçe Kamerası", DeviceCategory.CAMERA, "Bahçe", true, true, 8.5),
            HardwareItem(4, "Giriş Kamerası", DeviceCategory.CAMERA, "Antre", true, true, 8.5),
            HardwareItem(5, "Merkezi Termostat", DeviceCategory.THERMOSTAT, "Salon", true, true, 12.0),
            HardwareItem(6, "Yatak Odası Kliması", DeviceCategory.THERMOSTAT, "Yatak Odası", true, false, 1200.0),
            HardwareItem(7, "Mutfak Spotları", DeviceCategory.LIGHT, "Mutfak", true, false, 45.0),
            HardwareItem(8, "Roborock S7", DeviceCategory.VACUUM, "Koridor", true, true, 50.0),
            HardwareItem(9, "Balkon Aydınlatması", DeviceCategory.LIGHT, "Balkon", false, false, 25.0),
            HardwareItem(10, "Çalışma Odası Lambası", DeviceCategory.LIGHT, "Ofis", true, true, 12.0),
        ),
    )
    val devices: StateFlow<List<HardwareItem>> = _devices.asStateFlow()

    suspend fun toggleDevice(id: Int) = withContext(computationDispatcher) {
        _devices.update { current ->
            current.map { item -> if (item.id == id) item.copy(isOn = !item.isOn) else item }
        }
    }
}
