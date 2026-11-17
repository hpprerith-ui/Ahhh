package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.MceCampusData
import com.example.model.CampusRoom
import com.example.model.StepDirection
import com.example.ui.components.FloorPlanCanvas
import com.example.ui.theme.CoralDest
import com.example.ui.theme.TealRoute
import com.example.ui.viewmodel.CampusNavUiState

@Composable
fun NavigateScreen(
    uiState: CampusNavUiState,
    onMapSearchQueryChange: (String) -> Unit,
    onFocusRoomOnMap: (CampusRoom) -> Unit,
    onNavigateToSearchedRoom: (CampusRoom) -> Unit,
    onSelectStartRoom: () -> Unit,
    onSelectEndRoom: () -> Unit,
    onSwapEndpoints: () -> Unit,
    onToggleAccessible: () -> Unit,
    onSelectFloor: (String, Int) -> Unit,
    onRoomTapped: (CampusRoom) -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onSelectStep: (Int) -> Unit,
    onTogglePreviewWalk: () -> Unit,
    onOpenIpsSheet: () -> Unit,
    onSnapToIps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBlock = MceCampusData.getBlock(uiState.viewingBlockId) ?: MceCampusData.blocks.first()
    val floorPlan = MceCampusData.getFloor(activeBlock.id, uiState.viewingFloorNumber)
        ?: activeBlock.floors.first()

    var stepsExpanded by remember { mutableStateOf(false) }

    val route = uiState.activeRoute
    val currentStep = route?.steps?.getOrNull(uiState.activeStepIndex)
    val ipsPos = uiState.ipsState.position
    val isLiveActive = uiState.ipsState.hardwareStatus.isScanning || uiState.ipsState.isPreviewWalkActive

    val infiniteTransition = rememberInfiniteTransition(label = "live_badge")
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    val routeFloorsOnBlock = remember(route, activeBlock.id) {
        route?.nodePath?.filter { it.blockId == activeBlock.id }?.map { it.floorNumber }?.toSet().orEmpty()
    }
    val routeBlocks = remember(route) {
        route?.nodePath?.map { it.blockId }?.toSet().orEmpty()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Real-Time Quick Search + IPS Telemetry + Route Planner Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live IPS Status + Accessible Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (ipsPos != null) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenIpsSheet() }
                            .testTag("nav_ips_status_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isLiveActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .alpha(liveDotAlpha)
                                        .clip(CircleShape)
                                        .background(TealRoute)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "IPS Sensor Diagnostics",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = when {
                                    uiState.ipsState.isPreviewWalkActive ->
                                        "Live Walk • ${ipsPos?.nearestRoomLabel ?: "${floorPlan.blockId} Fl ${floorPlan.floorLabel}"}"
                                    ipsPos != null ->
                                        "${ipsPos.sourceMode.label} • ±${"%.1f".format(ipsPos.accuracyMeters)}m (${uiState.ipsState.headingDegrees.toInt()}°)"
                                    uiState.ipsState.hardwareStatus.isScanning ->
                                        "Scanning ${uiState.ipsState.wifiSignals.size} APs, ${uiState.ipsState.bleBeacons.size} BLE • Tap for IPS"
                                    else -> "Tap to enable Wi-Fi/BLE Indoor Positioning"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(6.dp))

                    FilterChip(
                        selected = uiState.accessibleOnly,
                        onClick = onToggleAccessible,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Accessible,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text("Lift", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("accessible_route_toggle")
                    )
                }

                // Instant Quick-Search Bar right on the Map screen
                OutlinedTextField(
                    value = uiState.mapSearchQuery,
                    onValueChange = onMapSearchQueryChange,
                    placeholder = { Text("Quick search room/lab (e.g. MB 202, AB-204, Physics)...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.mapSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onMapSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear map search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_quick_search_input")
                )

                // Animated Instant Search Results Dropdown
                AnimatedVisibility(
                    visible = uiState.mapSearchQuery.isNotBlank(),
                    enter = expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeIn(),
                    exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                ) {
                    if (uiState.mapSearchResults.isEmpty()) {
                        Text(
                            text = "No rooms or labs match \"${uiState.mapSearchQuery}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.mapSearchResults, key = { it.id }) { room ->
                                val floorStr = when (room.floorNumber) {
                                    0 -> "G"
                                    else -> "${room.floorNumber}F"
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFocusRoomOnMap(room) }
                                        .animateItem()
                                        .testTag("map_search_result_${room.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = highlightMatch(
                                                    "${room.code} — ${room.name}",
                                                    uiState.mapSearchQuery,
                                                    MaterialTheme.colorScheme.primary
                                                ),
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${room.blockId} • $floorStr • ${room.wing}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        FilledTonalIconButton(
                                            onClick = { onFocusRoomOnMap(room) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CenterFocusStrong,
                                                contentDescription = "Fly to ${room.code}",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        FilledTonalIconButton(
                                            onClick = { onNavigateToSearchedRoom(room) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Navigation,
                                                contentDescription = "Route to ${room.code}",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // From -> To Endpoint Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EndpointPill(
                            prefix = "FROM",
                            dotColor = MaterialTheme.colorScheme.primary,
                            title = "${uiState.startRoom.code} • ${uiState.startRoom.name}",
                            onClick = onSelectStartRoom,
                            testTag = "start_room_selector"
                        )
                        EndpointPill(
                            prefix = "TO",
                            dotColor = CoralDest,
                            title = "${uiState.endRoom.code} • ${uiState.endRoom.name}",
                            onClick = onSelectEndRoom,
                            testTag = "end_room_selector"
                        )
                    }

                    IconButton(
                        onClick = onSwapEndpoints,
                        modifier = Modifier.testTag("swap_endpoints_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap start and destination"
                        )
                    }
                }
            }
        }

        // 2. Block & Floor Switcher Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.blocks.forEach { blk ->
                    val selected = blk.id == activeBlock.id
                    val onRoute = routeBlocks.contains(blk.id)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFloor(blk.id, uiState.viewingFloorNumber) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(blk.code)
                                if (onRoute) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(TealRoute)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("map_block_${blk.id}")
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                activeBlock.floors.forEach { fl ->
                    val selected = fl.floorNumber == floorPlan.floorNumber
                    val onRoute = routeFloorsOnBlock.contains(fl.floorNumber)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFloor(activeBlock.id, fl.floorNumber) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(fl.floorLabel)
                                if (onRoute) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(TealRoute)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("map_floor_${fl.floorNumber}")
                    )
                }
            }
        }

        // 3. Interactive Pan/Zoom Architectural Floor Plan with smooth floor transition
        AnimatedContent(
            targetState = floorPlan,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(tween(160)))
            },
            label = "floor_plan_transition",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { animatedFloor ->
            FloorPlanCanvas(
                floorPlan = animatedFloor,
                activeRoute = route,
                activeStepIndex = uiState.activeStepIndex,
                ipsPosition = ipsPos,
                isIpsScanning = uiState.ipsState.hardwareStatus.isScanning,
                highlightedRoomId = uiState.inspectedRoom?.id,
                focusedRoom = uiState.focusedRoom,
                focusRequestNonce = uiState.focusRequestNonce,
                onRoomTapped = onRoomTapped,
                onCenterOnIps = onSnapToIps,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 4. Real-time Turn-by-Turn Navigation Deck
        if (route != null && currentStep != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val minText = if (route.estimatedSeconds < 60) {
                                "${route.estimatedSeconds}s"
                            } else {
                                "${(route.estimatedSeconds + 30) / 60} min"
                            }
                            Text(
                                text = "${route.totalDistanceMeters} m  •  ~$minText walk",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Step ${uiState.activeStepIndex + 1} of ${route.steps.size}" +
                                    (if (route.accessibleOnly) " • Step-Free" else ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onTogglePreviewWalk,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = if (uiState.ipsState.isPreviewWalkActive) {
                                    ButtonDefaults.buttonColors(containerColor = CoralDest)
                                } else {
                                    ButtonDefaults.buttonColors()
                                },
                                modifier = Modifier.testTag("preview_walk_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.ipsState.isPreviewWalkActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(if (uiState.ipsState.isPreviewWalkActive) " Stop" else " Walk")
                            }

                            IconButton(
                                onClick = { stepsExpanded = !stepsExpanded },
                                modifier = Modifier.testTag("expand_steps_button")
                            ) {
                                Icon(
                                    imageVector = if (stepsExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (stepsExpanded) "Collapse directions" else "Show all steps"
                                )
                            }
                        }
                    }

                    // Active Step Card with directional slide transition
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnimatedContent(
                                targetState = currentStep,
                                transitionSpec = {
                                    if (targetState.index >= initialState.index) {
                                        (slideInHorizontally(tween(200)) { it / 3 } + fadeIn(tween(200)))
                                            .togetherWith(slideOutHorizontally(tween(150)) { -it / 3 } + fadeOut(tween(150)))
                                    } else {
                                        (slideInHorizontally(tween(200)) { -it / 3 } + fadeIn(tween(200)))
                                            .togetherWith(slideOutHorizontally(tween(150)) { it / 3 } + fadeOut(tween(150)))
                                    }
                                },
                                label = "active_step_transition",
                                modifier = Modifier.weight(1f)
                            ) { step ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = stepIcon(step.direction),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = step.instruction,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = step.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = onPrevStep,
                                    enabled = uiState.activeStepIndex > 0,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("prev_step_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous step"
                                    )
                                }
                                IconButton(
                                    onClick = onNextStep,
                                    enabled = uiState.activeStepIndex < route.steps.lastIndex,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("next_step_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next step"
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = stepsExpanded,
                        enter = expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeIn(),
                        exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(route.steps) { index, step ->
                                val isActive = index == uiState.activeStepIndex
                                Surface(
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectStep(index) }
                                        .testTag("route_step_$index")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = stepIcon(step.direction),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = step.instruction,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = step.detail,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (step.distanceMeters > 0) {
                                            Text(
                                                text = "${step.distanceMeters} m",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No accessible route found between these rooms. Try disabling Lift-only mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

private fun highlightMatch(text: String, query: String, highlightColor: Color) = buildAnnotatedString {
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

@Composable
private fun EndpointPill(
    prefix: String,
    dotColor: Color,
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = prefix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun stepIcon(direction: StepDirection): ImageVector = when (direction) {
    StepDirection.START -> Icons.Default.MyLocation
    StepDirection.STRAIGHT -> Icons.Default.ArrowUpward
    StepDirection.TURN_LEFT, StepDirection.SLIGHT_LEFT -> Icons.Default.TurnLeft
    StepDirection.TURN_RIGHT, StepDirection.SLIGHT_RIGHT -> Icons.Default.TurnRight
    StepDirection.STAIRS_UP, StepDirection.STAIRS_DOWN -> Icons.Default.Stairs
    StepDirection.ELEVATOR_UP, StepDirection.ELEVATOR_DOWN -> Icons.Default.Elevator
    StepDirection.OUTDOOR_WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
    StepDirection.ARRIVE -> Icons.Default.Flag
}
