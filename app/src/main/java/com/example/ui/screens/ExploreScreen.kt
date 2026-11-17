package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CampusBlock
import com.example.model.CampusRoom
import com.example.model.RoomCategory
import com.example.ui.viewmodel.CampusNavUiState

@Composable
fun ExploreScreen(
    uiState: CampusNavUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (RoomCategory) -> Unit,
    onOpenBlock: (CampusBlock) -> Unit,
    onInspectRoom: (CampusRoom) -> Unit,
    onNavigateToRoom: (CampusRoom) -> Unit,
    onOpenIpsSheet: () -> Unit,
    onLocateNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFiltering = uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null
    val categories = listOf(
        RoomCategory.LAB,
        RoomCategory.CLASSROOM,
        RoomCategory.OFFICE,
        RoomCategory.FACILITY,
        RoomCategory.ELEVATOR,
        RoomCategory.AMENITY
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Campus Card
        item(key = "hero_banner") {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_campus_hero),
                        contentDescription = "Malnad College of Engineering Campus Blocks",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x330B1424),
                                        Color(0xD90B1424)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF0D9488).copy(alpha = 0.92f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "MCE HASSAN • INDOOR IPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clickable { onOpenIpsSheet() }
                                    .testTag("explore_ips_status_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = "Indoor Positioning Status",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = uiState.ipsState.position?.sourceMode?.label
                                            ?: if (uiState.ipsState.hardwareStatus.isScanning) "Scanning..." else "Locate Me",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Find Any Block, Room or Lab",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = uiState.ipsState.position?.let { pos ->
                                    "Detected near ${pos.nearestRoomLabel ?: "${pos.blockId} Fl ${pos.floorNumber}"} (±${"%.1f".format(pos.accuracyMeters)} m)"
                                } ?: "Wi-Fi triangulation, BLE beacons & step-by-step corridor maps",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }

        // Search Box
        item(key = "search_box") {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search MB 202, AB-204, Chemistry Lab, Principal...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )
        }

        // Category Filter Chips
        item(key = "category_chips") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick = { onCategorySelect(cat) },
                        label = { Text(cat.label) },
                        modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}")
                    )
                }
            }
        }

        if (isFiltering) {
            item(key = "filtered_header") {
                Text(
                    text = "Matching Rooms & Labs (${uiState.filteredRooms.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.animateItem()
                )
            }
            if (uiState.filteredRooms.isEmpty()) {
                item(key = "empty_search") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No matching room or lab",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Try searching \"MB 202\", \"AB-209\", \"Physics\", or \"Library\".",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.filteredRooms, key = { it.id }) { room ->
                    RoomDirectoryCard(
                        room = room,
                        onClick = { onInspectRoom(room) },
                        onNavigate = { onNavigateToRoom(room) },
                        searchQuery = uiState.searchQuery,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        } else {
            // Live IPS Quick Action Strip
            item(key = "ips_strip") {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .animateItem()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.ipsState.hardwareStatus.isScanning)
                                    "Live Indoor Positioning Active"
                                else "Determine My Building & Floor",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${uiState.ipsState.wifiSignals.size} Wi-Fi APs • ${uiState.ipsState.bleBeacons.size} BLE Beacons • ${uiState.ipsState.stepCount} Steps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        FilledTonalButton(
                            onClick = onLocateNow,
                            modifier = Modifier.testTag("explore_locate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("  Locate")
                        }
                    }
                }
            }

            // Campus Blocks Section
            item(key = "blocks_header") {
                Text(
                    text = "College Blocks",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.animateItem()
                )
            }

            items(uiState.blocks, key = { it.id }) { block ->
                BlockOverviewCard(
                    block = block,
                    onClick = { onOpenBlock(block) },
                    modifier = Modifier.animateItem()
                )
            }

            // Popular Classrooms, Labs & Offices Quick Navigate
            item(key = "popular_header") {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Popular Classrooms, Labs & Offices",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.animateItem()
                )
            }

            val featuredIds = listOf(
                "ab_2_201", "ab_2_207", "ab_2_212", "mb_2_202",
                "mb_2_203", "mb_g_phys_w", "mb_g_chem_lab", "mb_1_office"
            )
            val featuredRooms = featuredIds.mapNotNull { id ->
                uiState.filteredRooms.find { it.id == id }
            }

            items(featuredRooms, key = { "feat_${it.id}" }) { room ->
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

@Composable
private fun BlockOverviewCard(
    block: CampusBlock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalRooms = block.floors.sumOf { it.rooms.size }
    val labCount = block.floors.flatMap { it.rooms }.count { it.category == RoomCategory.LAB }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("block_card_${block.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(block.accentHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = block.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = block.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open ${block.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = block.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(icon = Icons.Default.Apartment, text = "${block.floors.size} Floors (G–2)")
                StatChip(icon = Icons.Default.Science, text = "$labCount Labs • $totalRooms Rooms")
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RoomDirectoryCard(
    room: CampusRoom,
    onClick: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    val floorLabel = when (room.floorNumber) {
        0 -> "Ground Floor"
        1 -> "1st Floor"
        2 -> "2nd Floor"
        else -> "Floor ${room.floorNumber}"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("room_card_${room.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = highlightExploreMatch(
                                room.code,
                                searchQuery,
                                MaterialTheme.colorScheme.primary
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${room.blockId} • $floorLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = highlightExploreMatch(
                        room.name,
                        searchQuery,
                        MaterialTheme.colorScheme.primary
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${room.wing} • ${room.category.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onNavigate,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("route_btn_${room.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Navigate to ${room.name}",
                    modifier = Modifier.size(16.dp)
                )
                Text("  Route")
            }
        }
    }
}

private fun highlightExploreMatch(text: String, query: String, highlightColor: Color) = buildAnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    val idx = text.indexOf(q, ignoreCase = true)
    if (idx < 0) {
        append(text)
    } else {
        append(text.substring(0, idx))
        withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
            append(text.substring(idx, idx + q.length))
        }
        append(text.substring(idx + q.length))
    }
}
