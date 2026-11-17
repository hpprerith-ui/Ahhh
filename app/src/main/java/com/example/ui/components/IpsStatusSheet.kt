package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.IpsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpsStatusSheet(
    ipsState: IpsState,
    onDismiss: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleScan: () -> Unit,
    onClearCalibrations: () -> Unit
) {
    val hw = ipsState.hardwareStatus

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Indoor Positioning System",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fuses Wi-Fi RSSI trilateration, BLE iBeacons, and step/compass dead reckoning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!hw.hasLocationPermission || !hw.hasBluetoothScanPermission) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Sensor & Signal Permissions Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                text = "Grant Location, Nearby Wi-Fi, and Bluetooth Scan permissions to read real AP signal strengths and BLE beacons inside campus blocks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Button(
                                onClick = onRequestPermissions,
                                modifier = Modifier.testTag("grant_ips_permissions_button")
                            ) {
                                Text("Grant Positioning Permissions")
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HardwareBadge(
                        icon = Icons.Default.Wifi,
                        label = if (hw.isWifiEnabled) "Wi-Fi Ready" else "Wi-Fi Off",
                        ok = hw.isWifiEnabled && hw.hasLocationPermission,
                        modifier = Modifier.weight(1f)
                    )
                    HardwareBadge(
                        icon = Icons.Default.BluetoothSearching,
                        label = if (hw.isBluetoothEnabled) "BLE Ready" else "BLE Off",
                        ok = hw.isBluetoothEnabled && hw.hasBluetoothScanPermission,
                        modifier = Modifier.weight(1f)
                    )
                    HardwareBadge(
                        icon = Icons.Default.Explore,
                        label = "${ipsState.headingDegrees.toInt()}° • ${ipsState.stepCount} st",
                        ok = hw.hasRotationSensor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleScan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_ips_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(if (hw.isScanning) "  Pause Live Scan" else "  Start Live Scan")
                    }
                    if (ipsState.customCalibrations.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearCalibrations,
                            modifier = Modifier.testTag("clear_calibrations_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear calibrations",
                                modifier = Modifier.size(18.dp)
                            )
                            Text("  Clear (${ipsState.customCalibrations.size})")
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Live Wi-Fi Access Points (${ipsState.wifiSignals.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (ipsState.wifiSignals.isEmpty()) {
                item {
                    Text(
                        text = if (hw.isScanning) {
                            "No Wi-Fi APs reported yet. On emulators or when Wi-Fi is off, tap any room on the map to set your anchor or use Calibrate."
                        } else {
                            "Tap Start Live Scan to detect nearby Wi-Fi AP BSSIDs and RSSI levels."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(ipsState.wifiSignals.take(6), key = { it.bssid }) { ap ->
                    SignalRow(
                        title = ap.ssid,
                        subtitle = "${ap.bssid} • ${ap.frequencyMhz} MHz" +
                            (ap.matchedAnchorLabel?.let { " • $it" } ?: ""),
                        rssiDbm = ap.rssiDbm,
                        distMeters = ap.estimatedDistanceMeters
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Live BLE Beacons (${ipsState.bleBeacons.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (ipsState.bleBeacons.isEmpty()) {
                item {
                    Text(
                        text = "No BLE iBeacons detected in range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(ipsState.bleBeacons.take(6), key = { it.address }) { beacon ->
                    SignalRow(
                        title = beacon.name,
                        subtitle = "${beacon.address} • ${beacon.uuidOrService.take(18)}",
                        rssiDbm = beacon.rssiDbm,
                        distMeters = beacon.estimatedDistanceMeters
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    ok: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (ok) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (ok) icon else Icons.Default.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (ok) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (ok) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SignalRow(
    title: String,
    subtitle: String,
    rssiDbm: Int,
    distMeters: Float
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$rssiDbm dBm",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "~${"%.1f".format(distMeters)} m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
