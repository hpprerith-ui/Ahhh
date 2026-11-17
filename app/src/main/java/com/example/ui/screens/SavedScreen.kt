package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MceCampusData
import com.example.model.CampusRoom
import com.example.ui.viewmodel.CampusNavUiState

@Composable
fun SavedScreen(
    uiState: CampusNavUiState,
    onInspectRoom: (CampusRoom) -> Unit,
    onNavigateToRoom: (CampusRoom) -> Unit,
    onToggleAccessible: () -> Unit,
    onOpenIpsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteRooms = uiState.favoriteRoomIds.mapNotNull { MceCampusData.getRoom(it) }
    val recentRooms = uiState.recentRoomIds.mapNotNull { MceCampusData.getRoom(it) }
    val essentials = listOfNotNull(
        MceCampusData.getRoom("mb_g_portico"),
        MceCampusData.getRoom("mb_g_lift"),
        MceCampusData.getRoom("mb_g_nw_toilets"),
        MceCampusData.getRoom("mb_1_office"),
        MceCampusData.getRoom("mb_1_exam_sec"),
        MceCampusData.getRoom("ab_g_atrium")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "saved_header") {
            Text(
                text = "Saved & Campus Quick Access",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Settings & IPS Calibration Card
        item(key = "saved_settings") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Accessible,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Step-Free Elevator Routing",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Avoid staircases across all blocks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.accessibleOnly,
                            onCheckedChange = { onToggleAccessible() },
                            modifier = Modifier.testTag("saved_accessible_switch")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenIpsSheet() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = "Wi-Fi & BLE Signal Diagnostics",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${uiState.ipsState.customCalibrations.size} calibrated room fingerprints saved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenIpsSheet,
                            modifier = Modifier.testTag("saved_open_ips_button")
                        ) {
                            Text("Inspect")
                        }
                    }
                }
            }
        }

        // Saved Favorites
        item(key = "fav_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Bookmarked Rooms & Labs (${favoriteRooms.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (favoriteRooms.isEmpty()) {
            item(key = "fav_empty") {
                Text(
                    text = "Tap the bookmark icon on any room or lab to save it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(favoriteRooms, key = { "fav_${it.id}" }) { room ->
                RoomDirectoryCard(
                    room = room,
                    onClick = { onInspectRoom(room) },
                    onNavigate = { onNavigateToRoom(room) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // Recent Routes
        if (recentRooms.isNotEmpty()) {
            item(key = "rec_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Recent Destinations",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            items(recentRooms, key = { "rec_${it.id}" }) { room ->
                RoomDirectoryCard(
                    room = room,
                    onClick = { onInspectRoom(room) },
                    onNavigate = { onNavigateToRoom(room) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // Essential Campus Landmarks
        item(key = "ess_header") {
            Text(
                text = "Essential Campus Landmarks",
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(essentials, key = { "ess_${it.id}" }) { room ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToRoom(room) }
                    .animateItem()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${room.code} — ${room.name}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${room.blockId} Block • ${room.wing}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Route to ${room.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
