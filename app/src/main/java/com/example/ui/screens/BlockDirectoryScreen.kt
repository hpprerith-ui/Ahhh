package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MceCampusData
import com.example.model.CampusRoom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockDirectoryScreen(
    blockId: String,
    onBack: () -> Unit,
    onInspectRoom: (CampusRoom) -> Unit,
    onNavigateToRoom: (CampusRoom) -> Unit,
    onViewFloorOnMap: (String, Int) -> Unit
) {
    val block = MceCampusData.getBlock(blockId) ?: MceCampusData.blocks.first()
    var selectedFloor by remember(blockId) { mutableIntStateOf(0) }
    val currentFloor = block.floors.find { it.floorNumber == selectedFloor } ?: block.floors.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(block.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            block.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("block_directory_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { onViewFloorOnMap(block.id, currentFloor.floorNumber) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("view_floor_map_button")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Text("  Map")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 840.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                block.floors.forEach { fl ->
                    FilterChip(
                        selected = fl.floorNumber == currentFloor.floorNumber,
                        onClick = { selectedFloor = fl.floorNumber },
                        label = {
                            Text(
                                when (fl.floorNumber) {
                                    0 -> "Ground (G)"
                                    1 -> "1st Floor"
                                    2 -> "2nd Floor"
                                    else -> "Floor ${fl.floorLabel}"
                                }
                            )
                        },
                        modifier = Modifier.testTag("dir_floor_${fl.floorNumber}")
                    )
                }
            }

            AnimatedContent(
                targetState = currentFloor,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "floor_directory_transition",
                modifier = Modifier.weight(1f)
            ) { floor ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "floor_title_${floor.floorNumber}") {
                        Text(
                            text = floor.floorTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(floor.rooms, key = { it.id }) { room ->
                        RoomDirectoryCard(
                            room = room,
                            onClick = { onInspectRoom(room) },
                            onNavigate = { onNavigateToRoom(room) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}
