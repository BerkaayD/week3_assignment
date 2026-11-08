package com.example.week3assignment

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.week3assignment.data.InMemoryHardwareRepository
import com.example.week3assignment.ui.HardwareScreen
import com.example.week3assignment.ui.HardwareUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StatelessHardwareScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun eventsAreForwardedButScreenCannotMutateItsInput() {
        val toggles = mutableListOf<Int>()
        val rooms = mutableListOf<String?>()
        val state = HardwareUiState(InMemoryHardwareRepository().devices.value, 106.0, "", null, listOf("Salon"), false)
        compose.setContent {
            MaterialTheme { HardwareScreen(state, {}, rooms::add, toggles::add) }
        }
        compose.onNodeWithTag("device_list").performScrollToNode(hasTestTag("toggle_1"))
        compose.onNodeWithTag("toggle_1").performClick()
        compose.onNodeWithTag("toggle_1").assertIsOn()
        compose.onNodeWithTag("device_list").performScrollToNode(hasTestTag("rooms"))
        compose.onNodeWithTag("room_Salon").performClick().assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(listOf(1), toggles)
            assertEquals(listOf("Salon"), rooms)
        }
    }
}
