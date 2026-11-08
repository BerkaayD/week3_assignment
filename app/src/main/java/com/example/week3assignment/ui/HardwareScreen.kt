package com.example.week3assignment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.week3assignment.data.DeviceCategory
import com.example.week3assignment.data.HardwareItem
import java.util.Locale

@Composable
fun HardwareScreen(
    uiState: HardwareUiState,
    onSearchQueryChanged: (String) -> Unit,
    onRoomSelected: (String?) -> Unit,
    onDeviceToggle: (Int) -> Unit,
) {
    Scaffold { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets).testTag("device_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "title") {
                Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                    Text("Akilli Ev", style = MaterialTheme.typography.headlineLarge)
                    Text("Cihazlariniz ve anlik enerji tuketimi", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item(key = "energy") { EnergyCard(uiState.totalPowerWatts) }
            item(key = "search") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("search"),
                    label = { Text("Cihaz adi ara") },
                    singleLine = true,
                )
            }
            item(key = "rooms") {
                LazyRow(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("rooms"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "all") {
                        FilterChip(
                            selected = uiState.selectedRoom == null,
                            onClick = { onRoomSelected(null) },
                            label = { Text("Tumu") },
                            modifier = Modifier.testTag("room_all"),
                        )
                    }
                    items(uiState.roomOptions, key = { it }) { room ->
                        FilterChip(
                            selected = uiState.selectedRoom == room,
                            onClick = { onRoomSelected(room) },
                            label = { Text(room) },
                            modifier = Modifier.testTag("room_$room"),
                        )
                    }
                }
            }
            if (uiState.isEmptyResult) {
                item(key = "empty") {
                    Text("Cihaz bulunamadi", Modifier.padding(24.dp).testTag("empty_result"))
                }
            }
            items(uiState.visibleDevices, key = { it.id }) { device ->
                DeviceCard(device, onDeviceToggle)
            }
        }
    }
}

@Composable
private fun EnergyCard(totalPowerWatts: Double) {
    Card(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Toplam Anlik Tuketim", style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatWatts(totalPowerWatts),
                modifier = Modifier.testTag("total_power"),
                style = MaterialTheme.typography.displaySmall,
            )
            Text("Tum ev · Online ve acık cihazlar", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DeviceCard(device: HardwareItem, onDeviceToggle: (Int) -> Unit) {
    Card(Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("device_${device.id}")) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text("${categoryLabel(device.category)} · ${device.room}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (device.isOnline) "Online" else "Offline",
                    color = if (device.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(if (device.isOn) "Acık" else "Kapali", style = MaterialTheme.typography.labelMedium)
                // Rated power is presentation of the model field, not a second energy calculation.
                Text("Cihaz gucu: ${formatWatts(device.powerWatts)}", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = device.isOn,
                onCheckedChange = { onDeviceToggle(device.id) },
                modifier = Modifier.testTag("toggle_${device.id}").semantics {
                    contentDescription = "${device.name} Ac/Kapat"
                },
            )
        }
    }
}

private fun formatWatts(value: Double): String = String.format(Locale.forLanguageTag("tr-TR"), "%.1f W", value)

private fun categoryLabel(category: DeviceCategory): String = when (category) {
    DeviceCategory.LIGHT -> "Aydınlatma"
    DeviceCategory.CAMERA -> "Kamera"
    DeviceCategory.THERMOSTAT -> "Iklimlendirme"
    DeviceCategory.VACUUM -> "Robot supurge"
}
