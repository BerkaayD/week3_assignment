package com.example.week3assignment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.week3assignment.data.HardwareItem
import com.example.week3assignment.data.InMemoryHardwareRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HardwareViewModel(
    private val repository: InMemoryHardwareRepository,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedRoom = MutableStateFlow<String?>(null)

    private val initialState = HardwareUiState(
        visibleDevices = repository.devices.value,
        totalPowerWatts = 106.0,
        searchQuery = "",
        selectedRoom = null,
        roomOptions = listOf("Salon", "Bahçe", "Antre", "Yatak Odası", "Mutfak", "Koridor", "Balkon", "Ofis"),
        isEmptyResult = false,
    )
    private val publishedState = MutableStateFlow(initialState)
    val uiState: StateFlow<HardwareUiState> = publishedState.asStateFlow()

    private val derivedState = combine(
        repository.devices, searchQuery, selectedRoom,
    ) { devices, query, room -> deriveState(devices, query, room) }
        .flowOn(computationDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),

            initialValue = initialState,
        )

    init {
        viewModelScope.launch {
            publishedState.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { observed ->
                if (observed) derivedState.collect { derived ->
                    publishedState.update { current ->
                        if (derived.searchQuery == current.searchQuery && derived.selectedRoom == current.selectedRoom) {
                            derived
                        } else current
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(text: String) {
        publishedState.update { it.copy(searchQuery = text) }
        searchQuery.value = text
    }

    fun onRoomSelected(room: String?) {
        publishedState.update { it.copy(selectedRoom = room) }
        selectedRoom.value = room
    }

    fun onDeviceToggle(id: Int) {
        viewModelScope.launch { repository.toggleDevice(id) }
    }

    private fun deriveState(devices: List<HardwareItem>, query: String, room: String?): HardwareUiState {
        val trimmedQuery = query.trim()
        val visible = devices.filter {
            it.name.contains(trimmedQuery, ignoreCase = true) && (room == null || it.room == room)
        }
        return HardwareUiState(
            visibleDevices = visible,
            totalPowerWatts = devices.filter { it.isOnline && it.isOn }.sumOf { it.powerWatts },
            searchQuery = query,
            selectedRoom = room,
            roomOptions = devices.map { it.room }.distinct(),
            isEmptyResult = visible.isEmpty(),
        )
    }
}
