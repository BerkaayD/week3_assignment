package com.example.week3assignment

import androidx.lifecycle.ViewModelStore
import com.example.week3assignment.data.InMemoryHardwareRepository
import com.example.week3assignment.ui.HardwareViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HardwareViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()
    private lateinit var repository: InMemoryHardwareRepository
    private lateinit var viewModel: HardwareViewModel

    @Before fun setUp() {
        repository = InMemoryHardwareRepository(main.dispatcher)
        viewModel = HardwareViewModel(repository, main.dispatcher)
        store.put("hardware", viewModel)
    }
    @After fun tearDown() { store.clear() }

    private fun TestScope.subscribe() = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect()
    }.also { runCurrent() }

    private fun assertPower(expected: Double) = assertEquals(expected, viewModel.uiState.value.totalPowerWatts, 0.0001)
    private fun ids() = viewModel.uiState.value.visibleDevices.map { it.id }

    @Test fun f01_initialSnapshotMatchesReactiveDerivation() = runTest {
        val initial = viewModel.uiState.value
        assertEquals(10, initial.visibleDevices.size)
        assertPower(106.0)
        assertEquals("", initial.searchQuery)
        assertNull(initial.selectedRoom)
        assertFalse(initial.isEmptyResult)
        assertEquals(listOf("Salon", "Bahçe", "Antre", "Yatak Odası", "Mutfak", "Koridor", "Balkon", "Ofis"), initial.roomOptions)
        subscribe()
        assertEquals(initial, viewModel.uiState.value)
    }

    @Test fun textEditsArePublishedBeforeBackgroundWorkRuns() = runTest {
        subscribe()
        listOf("k", "ka", "kam", "kame", "kamer", "kamera", "kamer", "", "kamera").forEach { text ->
            viewModel.onSearchQueryChanged(text)
            assertEquals(text, viewModel.uiState.value.searchQuery)
        }
        runCurrent()
        assertEquals("kamera", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(3, 4), ids())
        assertPower(106.0)
    }

    @Test fun newInputSurvivesResubscriptionWithAnOlderCachedResult() = runTest {
        val collector = subscribe()
        viewModel.onSearchQueryChanged("lamba")
        runCurrent()
        collector.cancel()
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        viewModel.onSearchQueryChanged("kamera")
        viewModel.onRoomSelected("Bahçe")
        assertEquals("kamera", viewModel.uiState.value.searchQuery)
        val observedQueries = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { observedQueries += it.searchQuery }
        }
        runCurrent()
        assertFalse("Cached text must not be republished", observedQueries.contains("lamba"))
        assertEquals("kamera", viewModel.uiState.value.searchQuery)
        assertEquals("Bahçe", viewModel.uiState.value.selectedRoom)
        assertEquals(listOf(3), ids())
    }

    @Test fun f02_searchCamera() = runTest {
        subscribe()
        viewModel.onSearchQueryChanged("kamera")
        runCurrent()
        assertEquals(listOf(3, 4), ids())
    }

    @Test fun f03_filterSalon() = runTest {
        subscribe()
        viewModel.onRoomSelected("Salon")
        runCurrent()
        assertEquals(listOf(1, 2, 5), ids())
    }

    @Test fun f04_searchAndRoomUseAnd() = runTest {
        subscribe()
        viewModel.onSearchQueryChanged("kamera")
        viewModel.onRoomSelected("Bahçe")
        runCurrent()
        assertEquals(listOf(3), ids())
    }

    @Test fun f05_emptyResultIsReadyForUi() = runTest {
        subscribe()
        viewModel.onSearchQueryChanged("kamera")
        viewModel.onRoomSelected("Salon")
        runCurrent()
        assertTrue(ids().isEmpty())
        assertTrue(viewModel.uiState.value.isEmptyResult)
    }

    @Test fun f06_filtersNeverChangeWholeHomePowerOrRoomOptions() = runTest {
        subscribe()
        val rooms = viewModel.uiState.value.roomOptions
        viewModel.onSearchQueryChanged("no match")
        runCurrent()
        assertPower(106.0)
        viewModel.onRoomSelected("Mutfak")
        runCurrent()
        assertPower(106.0)
        assertEquals(rooms, viewModel.uiState.value.roomOptions)
        viewModel.onSearchQueryChanged("")
        viewModel.onRoomSelected(null)
        runCurrent()
        assertEquals(10, ids().size)
        assertFalse(viewModel.uiState.value.isEmptyResult)
    }

    @Test fun f07_turnOffCornerLamp() = runTest {
        subscribe()
        viewModel.onDeviceToggle(1)
        runCurrent()
        assertPower(91.0)
        assertFalse(viewModel.uiState.value.visibleDevices.first().isOn)
    }

    @Test fun f08_turnOnChandelier() = runTest {
        subscribe()
        viewModel.onDeviceToggle(2)
        runCurrent()
        assertPower(166.0)
    }

    @Test fun f09_turnOnAirConditioner() = runTest {
        subscribe()
        viewModel.onDeviceToggle(6)
        runCurrent()
        assertPower(1306.0)
    }

    @Test fun f10_offlineToggleDoesNotConsumePower() = runTest {
        subscribe()
        viewModel.onDeviceToggle(9)
        runCurrent()
        assertTrue(viewModel.uiState.value.visibleDevices.single { it.id == 9 }.isOn)
        assertPower(106.0)
    }

    @Test fun f11_kitchenToggleStillUsesWholeHome() = runTest {
        subscribe()
        viewModel.onRoomSelected("Mutfak")
        viewModel.onDeviceToggle(7)
        runCurrent()
        assertEquals(listOf(7), ids())
        assertPower(151.0)
    }

    @Test fun f12_twoRapidTogglesRestoreOriginalState() = runTest {
        subscribe()
        val before = viewModel.uiState.value
        viewModel.onDeviceToggle(1)
        viewModel.onDeviceToggle(1)
        runCurrent()
        assertEquals(before, viewModel.uiState.value)
    }

    @Test fun f13_searchTrimsAndIgnoresCase() = runTest {
        subscribe()
        viewModel.onSearchQueryChanged("  LAMBA  ")
        runCurrent()
        assertEquals(listOf(1, 10), ids())
        assertEquals("  LAMBA  ", viewModel.uiState.value.searchQuery)
    }

    @Test fun repositoryChangesReachUiWithoutManualRefresh() = runTest {
        subscribe()
        viewModel.onRoomSelected("Bahçe")
        runCurrent()
        repository.toggleDevice(2)
        runCurrent()
        assertEquals(listOf(3), ids())
        assertPower(166.0)
    }

    @Test fun stoppedSubscriberCatchesUpOnReturn() = runTest {
        val collector = subscribe()
        collector.cancel()
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        repository.toggleDevice(1)
        assertPower(106.0)
        subscribe()
        assertPower(91.0)
    }
}
