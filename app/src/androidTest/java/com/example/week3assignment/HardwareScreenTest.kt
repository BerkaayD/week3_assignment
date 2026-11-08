package com.example.week3assignment

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HardwareScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun show(tag: String) {
        compose.onNodeWithTag("device_list").performScrollToNode(hasTestTag(tag))
    }

    private fun awaitPower(text: String) {
        show("total_power")
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("total_power").fetchSemanticsNode().config[SemanticsProperties.Text]
                .any { it.text == text }
        }
    }

    private fun search(text: String) {
        show("search")
        compose.onNodeWithTag("search").performTextReplacement(text)
        compose.waitForIdle()
    }

    private fun room(name: String) {
        show("rooms")
        compose.onNodeWithTag("rooms").performScrollToNode(hasTestTag("room_$name"))
        compose.onNodeWithTag("room_$name").performClick()
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        compose.onNodeWithTag("device_list").performScrollToIndex(0)
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir("screenshots"))
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun searchAndRoomFilterTogetherAndDisplayEmptyState() {
        awaitPower("106,0 W")
        screenshot("01-initial")
        search("kamera")
        room("Bahçe")
        show("device_3")
        compose.onNodeWithText("Bahçe Kamerası").assertIsDisplayed()
        compose.onNodeWithTag("device_4").assertDoesNotExist()
        screenshot("02-camera-garden")
        room("Salon")
        show("empty_result")
        compose.onNodeWithText("Cihaz bulunamadi").assertIsDisplayed()
        awaitPower("106,0 W")
        screenshot("03-empty-result")
    }

    @Test fun toggleUpdatesSwitchAndPowerReactively() {
        show("toggle_1")
        compose.onNodeWithTag("toggle_1").assertIsOn().performClick()
        awaitPower("91,0 W")
        show("toggle_1")
        compose.onNodeWithTag("toggle_1").assertIsOff().performClick()
        awaitPower("106,0 W")
    }

    @Test fun offlineDeviceCanToggleWithoutPowerContribution() {
        room("Balkon")
        show("toggle_9")
        compose.onNodeWithTag("toggle_9").assertIsOff().performClick()
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("toggle_9").fetchSemanticsNode()
                .config[SemanticsProperties.ToggleableState] == androidx.compose.ui.state.ToggleableState.On
        }
        awaitPower("106,0 W")
        show("toggle_9")
        compose.onNodeWithTag("toggle_9").assertIsOn()
    }

    @Test fun kitchenFilterDoesNotLimitEnergyScope() {
        room("Mutfak")
        show("toggle_7")
        compose.onNodeWithTag("toggle_7").performClick()
        awaitPower("151,0 W")
        show("device_7")
        compose.onNodeWithTag("device_7").assertIsDisplayed()
        compose.onNodeWithTag("device_1").assertDoesNotExist()
        screenshot("04-kitchen-151w")
    }

    @Test fun f14_activityRecreationPreservesFiltersToggleAndPower() {
        search("lamba")
        room("Salon")
        show("toggle_1")
        compose.onNodeWithTag("toggle_1").performClick()
        awaitPower("91,0 W")
        // Exercises the configuration-recreation lifecycle and retained ViewModelStore.
        compose.activityRule.scenario.recreate()
        awaitPower("91,0 W")
        show("search")
        compose.onNodeWithTag("search").assertTextContains("lamba")
        show("rooms")
        compose.onNodeWithTag("room_Salon").assertIsSelected()
        show("toggle_1")
        compose.onNodeWithTag("toggle_1").assertIsOff()
        compose.onNodeWithTag("device_2").assertDoesNotExist()
    }

    @Test fun allTenDevicesRemainReachableWhileScrolling() {
        for (id in 1..10) {
            show("device_$id")
            compose.onNodeWithTag("device_$id").assertIsDisplayed()
        }
        awaitPower("106,0 W")
    }

    @Test fun physicalOrientationChangePreservesStateAndControlsRemainReachable() {
        search("lamba")
        room("Salon")
        show("toggle_1")
        compose.onNodeWithTag("toggle_1").performClick()
        awaitPower("91,0 W")
        try {
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            compose.waitUntil(5_000) {
                compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            awaitPower("91,0 W")
            show("search")
            compose.onNodeWithTag("search").assertTextContains("lamba")
            show("rooms")
            compose.onNodeWithTag("room_Salon").assertIsSelected()
            show("toggle_1")
            compose.onNodeWithTag("toggle_1").assertIsOff()
            screenshot("05-landscape")
        } finally {
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}
