package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MceCampusData
import com.example.data.UserPreferencesRepository
import com.example.ips.IndoorPositioningEngine
import com.example.model.CampusBlock
import com.example.model.CampusRoom
import com.example.model.IpsState
import com.example.model.NavRoute
import com.example.model.RoomCategory
import com.example.model.UserCalibrationBinding
import com.example.navigation.IndoorPathfinder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot

data class CampusNavUiState(
    val blocks: List<CampusBlock> = MceCampusData.blocks,
    val searchQuery: String = "",
    val mapSearchQuery: String = "",
    val selectedCategory: RoomCategory? = null,
    val filteredRooms: List<CampusRoom> = MceCampusData.allRooms,
    val mapSearchResults: List<CampusRoom> = emptyList(),
    val startRoom: CampusRoom = MceCampusData.getRoom("mb_g_portico") ?: MceCampusData.allRooms.first(),
    val endRoom: CampusRoom = MceCampusData.getRoom("mb_2_202") ?: MceCampusData.allRooms.last(),
    val activeRoute: NavRoute? = null,
    val activeStepIndex: Int = 0,
    val viewingBlockId: String = "MB",
    val viewingFloorNumber: Int = 0,
    val focusedRoom: CampusRoom? = null,
    val focusRequestNonce: Int = 0,
    val accessibleOnly: Boolean = false,
    val favoriteRoomIds: Set<String> = emptySet(),
    val recentRoomIds: List<String> = emptyList(),
    val inspectedRoom: CampusRoom? = null,
    val showIpsDiagnosticsSheet: Boolean = false,
    val showRoomPickerForStart: Boolean? = null,
    val ipsState: IpsState = IpsState(),
    val statusBannerMessage: String? = null
)

class CampusNavViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = UserPreferencesRepository(application)
    private val ipsEngine = IndoorPositioningEngine(application)

    private val _localState = MutableStateFlow(CampusNavUiState())
    private var previewWalkJob: Job? = null

    val uiState: StateFlow<CampusNavUiState> = combine(
        _localState,
        prefsRepo.favoriteRoomIds,
        prefsRepo.recentRoomIds,
        prefsRepo.accessibleOnly,
        prefsRepo.customCalibrations,
        ipsEngine.state
    ) { values ->
        val local = values[0] as CampusNavUiState
        @Suppress("UNCHECKED_CAST")
        val favorites = values[1] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val recents = values[2] as List<String>
        val accessible = values[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val calibrations = values[4] as List<UserCalibrationBinding>
        val ips = values[5] as IpsState

        ipsEngine.updateCustomCalibrations(calibrations)

        local.copy(
            favoriteRoomIds = favorites,
            recentRoomIds = recents,
            accessibleOnly = accessible,
            ipsState = ips
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CampusNavUiState()
    )

    init {
        recomputeRoute()
        ipsEngine.refreshHardwareStatus()
    }

    fun onPermissionsResult() {
        ipsEngine.refreshHardwareStatus()
        if (uiState.value.ipsState.hardwareStatus.hasLocationPermission) {
            ipsEngine.startScanning()
        }
    }

    fun toggleIpsScanning() {
        if (uiState.value.ipsState.hardwareStatus.isScanning) {
            ipsEngine.stopScanning()
        } else {
            ipsEngine.startScanning()
        }
    }

    fun startIpsScanningIfPermitted() {
        ipsEngine.refreshHardwareStatus()
        if (uiState.value.ipsState.hardwareStatus.hasLocationPermission) {
            ipsEngine.startScanning()
        }
    }

    fun updateSearchQuery(query: String) {
        _localState.update { state ->
            state.copy(
                searchQuery = query,
                filteredRooms = filterRooms(query, state.selectedCategory)
            )
        }
    }

    fun updateMapSearchQuery(query: String) {
        _localState.update { state ->
            val trimmed = query.trim()
            state.copy(
                mapSearchQuery = query,
                mapSearchResults = if (trimmed.isEmpty()) emptyList() else filterRooms(trimmed, null).take(6)
            )
        }
    }

    fun focusRoomOnMap(room: CampusRoom) {
        _localState.update { state ->
            state.copy(
                viewingBlockId = room.blockId,
                viewingFloorNumber = room.floorNumber,
                focusedRoom = room,
                focusRequestNonce = state.focusRequestNonce + 1,
                mapSearchQuery = "",
                mapSearchResults = emptyList()
            )
        }
    }

    fun selectCategoryFilter(category: RoomCategory?) {
        _localState.update { state ->
            val nextCat = if (state.selectedCategory == category) null else category
            state.copy(
                selectedCategory = nextCat,
                filteredRooms = filterRooms(state.searchQuery, nextCat)
            )
        }
    }

    private fun filterRooms(query: String, category: RoomCategory?): List<CampusRoom> =
        MceCampusData.searchRooms(query, category)

    fun setViewingFloor(blockId: String, floorNumber: Int) {
        _localState.update {
            it.copy(viewingBlockId = blockId, viewingFloorNumber = floorNumber)
        }
    }

    fun navigateToRoom(destination: CampusRoom) {
        stopPreviewWalk()
        viewModelScope.launch { prefsRepo.recordRecent(destination.id) }
        val ipsPos = uiState.value.ipsState.position
        val derivedStart = ipsPos?.nearestRoomId?.let { MceCampusData.getRoom(it) }
            ?: _localState.value.startRoom

        _localState.update {
            it.copy(
                startRoom = derivedStart,
                endRoom = destination,
                viewingBlockId = destination.blockId,
                viewingFloorNumber = destination.floorNumber,
                focusedRoom = destination,
                focusRequestNonce = it.focusRequestNonce + 1,
                activeStepIndex = 0,
                inspectedRoom = null,
                mapSearchQuery = "",
                mapSearchResults = emptyList()
            )
        }
        recomputeRoute()
    }

    fun setStartRoom(start: CampusRoom) {
        stopPreviewWalk()
        _localState.update {
            it.copy(
                startRoom = start,
                viewingBlockId = start.blockId,
                viewingFloorNumber = start.floorNumber,
                focusedRoom = start,
                focusRequestNonce = it.focusRequestNonce + 1,
                activeStepIndex = 0,
                inspectedRoom = null
            )
        }
        ipsEngine.setManualAnchor(start)
        recomputeRoute()
    }

    fun setEndRoom(end: CampusRoom) {
        stopPreviewWalk()
        viewModelScope.launch { prefsRepo.recordRecent(end.id) }
        _localState.update {
            it.copy(
                endRoom = end,
                viewingBlockId = end.blockId,
                viewingFloorNumber = end.floorNumber,
                focusedRoom = end,
                focusRequestNonce = it.focusRequestNonce + 1,
                activeStepIndex = 0,
                inspectedRoom = null
            )
        }
        recomputeRoute()
    }

    fun swapEndpoints() {
        stopPreviewWalk()
        _localState.update {
            it.copy(
                startRoom = it.endRoom,
                endRoom = it.startRoom,
                viewingBlockId = it.endRoom.blockId,
                viewingFloorNumber = it.endRoom.floorNumber,
                activeStepIndex = 0
            )
        }
        recomputeRoute()
    }

    fun toggleAccessibleOnly() {
        val next = !uiState.value.accessibleOnly
        viewModelScope.launch {
            prefsRepo.setAccessibleOnly(next)
            _localState.update { it.copy(accessibleOnly = next, activeStepIndex = 0) }
            recomputeRoute(overrideAccessible = next)
        }
    }

    fun recomputeRoute(overrideAccessible: Boolean? = null) {
        val local = _localState.value
        val accessible = overrideAccessible ?: uiState.value.accessibleOnly
        val route = IndoorPathfinder.findRoute(local.startRoom, local.endRoom, accessible)
        _localState.update {
            it.copy(
                activeRoute = route,
                activeStepIndex = 0
            )
        }
    }

    fun selectStep(stepIndex: Int) {
        val route = _localState.value.activeRoute ?: return
        val clamped = stepIndex.coerceIn(0, (route.steps.size - 1).coerceAtLeast(0))
        val step = route.steps.getOrNull(clamped)
        _localState.update {
            it.copy(
                activeStepIndex = clamped,
                viewingBlockId = step?.blockId ?: it.viewingBlockId,
                viewingFloorNumber = step?.floorNumber ?: it.viewingFloorNumber
            )
        }
    }

    fun nextStep() {
        selectStep(_localState.value.activeStepIndex + 1)
    }

    fun previousStep() {
        selectStep(_localState.value.activeStepIndex - 1)
    }

    fun inspectRoom(room: CampusRoom?) {
        _localState.update { it.copy(inspectedRoom = room) }
    }

    fun openRoomPicker(forStart: Boolean?) {
        _localState.update { it.copy(showRoomPickerForStart = forStart) }
    }

    fun setIpsDiagnosticsVisible(visible: Boolean) {
        if (visible) ipsEngine.refreshHardwareStatus()
        _localState.update { it.copy(showIpsDiagnosticsSheet = visible) }
    }

    fun toggleFavorite(roomId: String) {
        viewModelScope.launch {
            prefsRepo.toggleFavorite(roomId)
        }
    }

    fun calibrateCurrentLocationToRoom(room: CampusRoom) {
        val binding = ipsEngine.buildCalibrationForRoom(room)
        viewModelScope.launch {
            prefsRepo.saveCalibration(binding)
            ipsEngine.setManualAnchor(room)
            _localState.update {
                it.copy(
                    startRoom = room,
                    viewingBlockId = room.blockId,
                    viewingFloorNumber = room.floorNumber,
                    focusedRoom = room,
                    focusRequestNonce = it.focusRequestNonce + 1,
                    statusBannerMessage = "Calibrated ${room.code} with ${binding.wifiSignatures.size} Wi-Fi & ${binding.bleSignatures.size} BLE signals"
                )
            }
            recomputeRoute()
        }
    }

    fun clearCalibrations() {
        viewModelScope.launch {
            prefsRepo.clearCalibrations()
            _localState.update {
                it.copy(statusBannerMessage = "Cleared saved Wi-Fi/BLE room calibrations")
            }
        }
    }

    fun snapMapToIpsPosition() {
        val pos = uiState.value.ipsState.position ?: return
        _localState.update {
            it.copy(
                viewingBlockId = pos.blockId,
                viewingFloorNumber = pos.floorNumber
            )
        }
        pos.nearestRoomId?.let { roomId ->
            MceCampusData.getRoom(roomId)?.let { room ->
                _localState.update { it.copy(startRoom = room) }
                recomputeRoute()
            }
        }
    }

    fun togglePreviewWalk() {
        if (uiState.value.ipsState.isPreviewWalkActive) {
            stopPreviewWalk()
            return
        }
        val route = _localState.value.activeRoute ?: return
        if (route.nodePath.size < 2) return

        previewWalkJob?.cancel()
        previewWalkJob = viewModelScope.launch {
            val nodes = route.nodePath
            for (i in 0 until nodes.size - 1) {
                val a = nodes[i]
                val b = nodes[i + 1]
                val heading = ((Math.toDegrees(
                    atan2((b.x - a.x).toDouble(), -(b.y - a.y).toDouble())
                ).toFloat() + 360f) % 360f)

                val matchingStepIdx = route.steps.indexOfFirst { step ->
                    step.nodeIds.contains(a.id) && (step.nodeIds.contains(b.id) || step.index > 0)
                }.takeIf { it >= 0 } ?: _localState.value.activeStepIndex

                _localState.update {
                    it.copy(
                        viewingBlockId = a.blockId,
                        viewingFloorNumber = a.floorNumber,
                        activeStepIndex = matchingStepIdx
                    )
                }

                val subSteps = if (a.blockId != b.blockId || a.floorNumber != b.floorNumber) 3 else 8
                for (s in 0..subSteps) {
                    if (!isActive) return@launch
                    val t = s / subSteps.toFloat()
                    val px = a.x + (b.x - a.x) * t
                    val py = a.y + (b.y - a.y) * t
                    val curBlock = if (t < 0.5f) a.blockId else b.blockId
                    val curFloor = if (t < 0.5f) a.floorNumber else b.floorNumber

                    if (curBlock != _localState.value.viewingBlockId || curFloor != _localState.value.viewingFloorNumber) {
                        _localState.update {
                            it.copy(viewingBlockId = curBlock, viewingFloorNumber = curFloor)
                        }
                    }

                    ipsEngine.setPreviewWalkPosition(
                        blockId = curBlock,
                        floorNumber = curFloor,
                        x = px,
                        y = py,
                        headingDeg = heading,
                        active = true
                    )
                    delay(260L)
                }
            }
            val last = nodes.last()
            _localState.update {
                it.copy(
                    activeStepIndex = (route.steps.size - 1).coerceAtLeast(0),
                    viewingBlockId = last.blockId,
                    viewingFloorNumber = last.floorNumber
                )
            }
            ipsEngine.setPreviewWalkPosition(
                blockId = last.blockId,
                floorNumber = last.floorNumber,
                x = last.x,
                y = last.y,
                headingDeg = uiState.value.ipsState.headingDegrees,
                active = false
            )
        }
    }

    fun stopPreviewWalk() {
        previewWalkJob?.cancel()
        previewWalkJob = null
        val pos = uiState.value.ipsState.position
        if (uiState.value.ipsState.isPreviewWalkActive && pos != null) {
            ipsEngine.setPreviewWalkPosition(
                blockId = pos.blockId,
                floorNumber = pos.floorNumber,
                x = pos.x,
                y = pos.y,
                headingDeg = pos.headingDegrees,
                active = false
            )
        }
    }

    fun dismissBanner() {
        _localState.update { it.copy(statusBannerMessage = null) }
    }

    fun checkAutoAdvanceOnRealIps(posX: Float, posY: Float, blockId: String, floorNumber: Int) {
        val route = _localState.value.activeRoute ?: return
        val nextIdx = (_localState.value.activeStepIndex + 1).coerceAtMost(route.steps.lastIndex)
        val nextStep = route.steps.getOrNull(nextIdx) ?: return
        if (nextStep.blockId == blockId && nextStep.floorNumber == floorNumber) {
            val targetNodeId = nextStep.nodeIds.firstOrNull() ?: return
            val node = MceCampusData.getNode(targetNodeId) ?: return
            val distMeters = hypot(node.x - posX, node.y - posY) * 0.08f
            if (distMeters < 3.2f) {
                selectStep(nextIdx)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPreviewWalk()
        ipsEngine.stopScanning()
    }
}
