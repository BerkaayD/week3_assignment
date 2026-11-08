package com.example.week3assignment

import com.example.week3assignment.data.InMemoryHardwareRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class HardwareRepositoryTest {
    @Test fun seedHasTenUniqueDevicesAndSixActiveOnlineDevices() {
        val devices = InMemoryHardwareRepository().devices.value
        assertEquals((1..10).toList(), devices.map { it.id })
        assertEquals(6, devices.count { it.isOnline && it.isOn })
        assertEquals(106.0, devices.filter { it.isOnline && it.isOn }.sumOf { it.powerWatts }, 0.0001)
    }

    @Test fun publicFlowCannotBeCastToMutableStateFlow() {
        assertFalse(InMemoryHardwareRepository().devices is MutableStateFlow<*>)
    }

    @Test fun togglePreservesPreviousSnapshotOrderAndOtherFields() = runTest {
        val repository = InMemoryHardwareRepository()
        val before = repository.devices.value
        repository.toggleDevice(1)
        val after = repository.devices.value
        assertTrue(before.first().isOn)
        assertEquals(before.first().copy(isOn = false), after.first())
        assertEquals(before.drop(1), after.drop(1))
        assertEquals(before.map { it.id }, after.map { it.id })
    }

    @Test fun unknownIdLeavesStateUnchanged() = runTest {
        val repository = InMemoryHardwareRepository()
        val before = repository.devices.value
        repository.toggleDevice(-1)
        assertEquals(before, repository.devices.value)
    }

    @Test fun concurrentUpdatesOnRealThreadsDoNotLoseToggles() = runTest {
        val repository = InMemoryHardwareRepository()
        val before = repository.devices.value
        coroutineScope {
            repeat(1_001) { launch(Dispatchers.Default) { repository.toggleDevice(1) } }
            repeat(1_000) { launch(Dispatchers.Default) { repository.toggleDevice(2) } }
        }
        assertEquals(before.first().copy(isOn = false), repository.devices.value.first())
        assertEquals(before.drop(1), repository.devices.value.drop(1))
    }
}
