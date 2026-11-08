package com.example.week3assignment

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Source checks are guardrails; behavioral tests and manual review remain necessary. */
class ArchitectureRulesTest {
    private val root = File(requireNotNull(System.getProperty("projectSourceDir")), "src/main/java/com/example/week3assignment")
    private fun source(path: String): String = File(root, path).readText()
        .replace(Regex("/\\*[\\s\\S]*?\\*/|//[^\\n]*"), "")

    @Test fun screenContainsNoBusinessTransformsStateOrRepository() {
        val screen = source("ui/HardwareScreen.kt")
        assertFalse(Regex("\\.\\s*(filter|filterNot|sumOf|sum|fold|reduce|map|count|forEach)\\s*(\\(|\\{)").containsMatchIn(screen))
        assertFalse(Regex("\\b(for|while)\\s*\\(").containsMatchIn(screen))
        listOf("Repository", "ViewModel", "mutableStateOf", "MutableStateFlow", "remember", "collectAsState").forEach {
            assertFalse("Screen must not contain $it", screen.contains(it))
        }
        assertTrue(Regex("items\\(uiState\\.visibleDevices,\\s*key\\s*=\\s*\\{\\s*it.id\\s*}").containsMatchIn(screen))
    }

    @Test fun viewModelAndDataAreComposeAndAndroidUiIndependent() {
        listOf("ui/HardwareViewModel.kt", "ui/HardwareUiState.kt", "data/HardwareItem.kt", "data/InMemoryHardwareRepository.kt").forEach { path ->
            val source = source(path)
            assertFalse(path, Regex("import\\s+(androidx\\.compose|android\\.)").containsMatchIn(source))
        }
        assertFalse(source("data/HardwareItem.kt").contains("var "))
    }

    @Test fun derivedFlowRunsBeforeStateInOnInjectedDefaultDispatcher() {
        val source = source("ui/HardwareViewModel.kt")
        assertTrue(source.contains("CoroutineDispatcher = Dispatchers.Default"))
        assertTrue(source.indexOf(".flowOn(computationDispatcher)") in 0 until source.indexOf(".stateIn("))
        assertTrue(source.contains("viewModelScope.launch"))
        assertFalse(source.contains("GlobalScope"))
        assertFalse(source.contains("runBlocking"))
        assertFalse(source.contains("Thread.sleep"))
        assertFalse(source.contains("Dispatchers.IO"))
    }

    @Test fun repositoryHasEncapsulatedAtomicBackgroundMutation() {
        val source = source("data/InMemoryHardwareRepository.kt")
        assertTrue(source.contains("private val _devices"))
        assertTrue(source.contains("_devices.asStateFlow()"))
        assertTrue(source.contains("_devices.update"))
        assertTrue(source.contains("withContext(computationDispatcher)"))
        assertTrue(source.contains("CoroutineDispatcher = Dispatchers.Default"))
    }

    @Test fun activityUsesLifecycleOwnedViewModelAndLifecycleCollection() {
        val source = source("MainActivity.kt")
        assertTrue(source.contains("by viewModels"))
        assertTrue(source.contains("collectAsStateWithLifecycle()"))
        assertFalse(source.contains("InMemoryHardwareRepository"))
        assertFalse(source.contains("HardwareViewModel("))
    }
}
