package com.example.ips

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.MceCampusData
import com.example.model.BleBeaconReading
import com.example.model.CampusRoom
import com.example.model.CampusSignalAnchor
import com.example.model.IpsHardwareStatus
import com.example.model.IpsPositionEstimate
import com.example.model.IpsSourceMode
import com.example.model.IpsState
import com.example.model.UserCalibrationBinding
import com.example.model.WifiSignalReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class IndoorPositioningEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val _state = MutableStateFlow(IpsState())
    val state: StateFlow<IpsState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var isWifiRegistered = false
    private var isBleScanning = false
    private var isSensorsRegistered = false

    private val smoothedWifiRssi = mutableMapOf<String, Float>()
    private val smoothedBleRssi = mutableMapOf<String, Float>()
    private val liveBleMap = mutableMapOf<String, BleBeaconReading>()

    // Sensor fusion state
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var smoothedHeadingDeg = 0f
    private var lastAccelPeakMs = 0L
    private var hasHardwareStepSensor = false

    private val wifiScanCallback: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        object : WifiManager.ScanResultsCallback() {
            override fun onScanResultsAvailable() {
                processWifiScanResults()
            }
        }
    } else null

    private val wifiBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                processWifiScanResults()
            }
        }
    }

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val mac = device.address ?: return
            val prev = smoothedBleRssi[mac]
            val smoothed = if (prev == null) result.rssi.toFloat() else (0.35f * result.rssi + 0.65f * prev)
            smoothedBleRssi[mac] = smoothed

            val scanRecord = result.scanRecord
            val txPower = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && result.txPower != ScanResult.TX_POWER_NOT_PRESENT) {
                result.txPower
            } else {
                -59
            }
            val parsedUuid = parseIBeaconUuid(scanRecord?.bytes)
                ?: scanRecord?.serviceUuids?.firstOrNull()?.uuid?.toString()
                ?: "BLE-ADV"

            val dist = rssiToMeters(smoothed.toInt(), txPower, pathLossExp = 2.4f)
            val matchedAnchor = findMatchingAnchorForBle(mac, parsedUuid)

            val reading = BleBeaconReading(
                address = mac,
                name = result.scanRecord?.deviceName ?: device.name ?: "BLE Beacon",
                rssiDbm = smoothed.toInt(),
                txPowerDbm = txPower,
                uuidOrService = parsedUuid,
                estimatedDistanceMeters = dist,
                matchedAnchorLabel = matchedAnchor?.label
            )

            synchronized(liveBleMap) {
                liveBleMap[mac] = reading
            }
            _state.update { current ->
                current.copy(
                    bleBeacons = liveBleMap.values.sortedByDescending { it.rssiDbm }.take(15)
                )
            }
            recomputePositionFromSignals()
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuth = ((Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f)
                    smoothedHeadingDeg = smoothAngle(smoothedHeadingDeg, azimuth, 0.25f)
                    _state.update { st ->
                        st.copy(
                            headingDegrees = smoothedHeadingDeg,
                            position = st.position?.copy(headingDegrees = smoothedHeadingDeg)
                        )
                    }
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    onStepDetected()
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    if (!hasHardwareStepSensor) {
                        val mag = sqrt(
                            event.values[0] * event.values[0] +
                                event.values[1] * event.values[1] +
                                event.values[2] * event.values[2]
                        )
                        val now = System.currentTimeMillis()
                        if (mag > 12.1f && now - lastAccelPeakMs > 420L) {
                            lastAccelPeakMs = now
                            onStepDetected()
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun updateCustomCalibrations(calibrations: List<UserCalibrationBinding>) {
        _state.update { it.copy(customCalibrations = calibrations) }
        recomputePositionFromSignals()
    }

    fun refreshHardwareStatus() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasWifiPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else hasFine
        val hasBlePerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else hasFine
        val hasActivity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true

        val isWifiRtt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)
        } else false

        val hasRot = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        val hasStep = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        hasHardwareStepSensor = hasStep

        val wifiEnabled = wifiManager?.isWifiEnabled == true
        val btEnabled = bluetoothManager?.adapter?.isEnabled == true

        val statusMsg = when {
            !hasFine -> "Location & Nearby permissions needed for Wi-Fi/BLE positioning"
            _state.value.hardwareStatus.isScanning && _state.value.position != null ->
                "Active (${_state.value.position?.sourceMode?.label}) • ±${"%.1f".format(_state.value.position?.accuracyMeters ?: 3f)} m"
            _state.value.hardwareStatus.isScanning ->
                "Scanning ${_state.value.wifiSignals.size} Wi-Fi APs & ${_state.value.bleBeacons.size} BLE beacons"
            else -> "Ready to scan Wi-Fi, BLE & PDR sensors"
        }

        _state.update {
            it.copy(
                hardwareStatus = IpsHardwareStatus(
                    hasLocationPermission = hasFine,
                    hasWifiPermission = hasWifiPerm,
                    hasBluetoothScanPermission = hasBlePerm,
                    hasActivityPermission = hasActivity,
                    isWifiEnabled = wifiEnabled,
                    isWifiRttSupported = isWifiRtt,
                    isBluetoothEnabled = btEnabled,
                    isScanning = it.hardwareStatus.isScanning,
                    hasRotationSensor = hasRot,
                    hasStepSensor = hasStep,
                    statusMessage = statusMsg
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        refreshHardwareStatus()
        registerSensors()
        startWifiScanning()
        startBleScanning()

        _state.update {
            it.copy(hardwareStatus = it.hardwareStatus.copy(isScanning = true))
        }
        refreshHardwareStatus()

        scanJob?.cancel()
        scanJob = scope.launch {
            while (isActive) {
                triggerWifiScan()
                delay(4500L)
            }
        }
    }

    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        unregisterWifi()
        stopBleScanning()
        unregisterSensors()
        _state.update {
            it.copy(hardwareStatus = it.hardwareStatus.copy(isScanning = false))
        }
        refreshHardwareStatus()
    }

    fun setManualAnchor(room: CampusRoom) {
        val node = MceCampusData.getNode(room.doorNodeId)
        val x = node?.x ?: room.rect.centerX
        val y = node?.y ?: room.rect.centerY
        _state.update {
            it.copy(
                isPreviewWalkActive = false,
                position = IpsPositionEstimate(
                    blockId = room.blockId,
                    floorNumber = room.floorNumber,
                    x = x,
                    y = y,
                    accuracyMeters = 1.5f,
                    nearestRoomId = room.id,
                    nearestRoomLabel = "${room.code} • ${room.name}",
                    sourceMode = IpsSourceMode.MANUAL_ANCHOR,
                    headingDegrees = it.headingDegrees,
                    confidence = 1.0f,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
        refreshHardwareStatus()
    }

    fun buildCalibrationForRoom(room: CampusRoom): UserCalibrationBinding {
        val node = MceCampusData.getNode(room.doorNodeId)
        val wifiSigs = _state.value.wifiSignals.take(6).associate { it.bssid to it.rssiDbm }
        val bleSigs = _state.value.bleBeacons.take(6).associate { it.address to it.rssiDbm }
        return UserCalibrationBinding(
            roomId = room.id,
            roomLabel = "${room.code} ${room.name}",
            blockId = room.blockId,
            floorNumber = room.floorNumber,
            x = node?.x ?: room.rect.centerX,
            y = node?.y ?: room.rect.centerY,
            wifiSignatures = wifiSigs,
            bleSignatures = bleSigs
        )
    }

    fun setPreviewWalkPosition(
        blockId: String,
        floorNumber: Int,
        x: Float,
        y: Float,
        headingDeg: Float,
        active: Boolean
    ) {
        val nearest = findNearestRoom(blockId, floorNumber, x, y)
        _state.update {
            it.copy(
                isPreviewWalkActive = active,
                headingDegrees = headingDeg,
                position = IpsPositionEstimate(
                    blockId = blockId,
                    floorNumber = floorNumber,
                    x = x,
                    y = y,
                    accuracyMeters = 1.8f,
                    nearestRoomId = nearest?.id,
                    nearestRoomLabel = nearest?.let { r -> "${r.code} • ${r.name}" },
                    sourceMode = IpsSourceMode.PDR_DEAD_RECKONING,
                    headingDegrees = headingDeg,
                    confidence = 0.92f,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }

    private fun registerSensors() {
        if (isSensorsRegistered) return
        val sm = sensorManager ?: return
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sm.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        val stepDet = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepDet != null) {
            hasHardwareStepSensor = true
            sm.registerListener(sensorListener, stepDet, SensorManager.SENSOR_DELAY_UI)
        } else {
            hasHardwareStepSensor = false
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sm.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        isSensorsRegistered = true
    }

    private fun unregisterSensors() {
        if (!isSensorsRegistered) return
        sensorManager?.unregisterListener(sensorListener)
        isSensorsRegistered = false
    }

    private fun startWifiScanning() {
        val wm = wifiManager ?: return
        if (!_state.value.hardwareStatus.hasLocationPermission) return
        if (!isWifiRegistered) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiScanCallback is WifiManager.ScanResultsCallback) {
                    wm.registerScanResultsCallback(ContextCompat.getMainExecutor(context), wifiScanCallback)
                } else {
                    ContextCompat.registerReceiver(
                        context,
                        wifiBroadcastReceiver,
                        IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                }
                isWifiRegistered = true
            } catch (_: SecurityException) {
            }
        }
        processWifiScanResults()
        triggerWifiScan()
    }

    private fun unregisterWifi() {
        if (!isWifiRegistered) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiScanCallback is WifiManager.ScanResultsCallback) {
                wifiManager?.unregisterScanResultsCallback(wifiScanCallback)
            } else {
                context.unregisterReceiver(wifiBroadcastReceiver)
            }
        } catch (_: Exception) {
        }
        isWifiRegistered = false
    }

    @Suppress("DEPRECATION")
    private fun triggerWifiScan() {
        if (!_state.value.hardwareStatus.hasLocationPermission) return
        try {
            wifiManager?.startScan()
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun processWifiScanResults() {
        val wm = wifiManager ?: return
        if (!_state.value.hardwareStatus.hasLocationPermission) return
        val rawResults = try {
            wm.scanResults.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }

        val readings = rawResults.mapNotNull { sr ->
            val bssid = sr.BSSID ?: return@mapNotNull null
            val prev = smoothedWifiRssi[bssid]
            val smoothed = if (prev == null) sr.level.toFloat() else (0.4f * sr.level + 0.6f * prev)
            smoothedWifiRssi[bssid] = smoothed

            val txAt1m = if (sr.frequency > 4000) -48 else -44
            val dist = rssiToMeters(smoothed.toInt(), txAt1m, pathLossExp = 2.6f)
            val anchor = findMatchingAnchorForWifi(bssid, sr.SSID.orEmpty())

            WifiSignalReading(
                bssid = bssid,
                ssid = sr.SSID.orEmpty().ifBlank { "<Hidden SSID>" },
                rssiDbm = smoothed.toInt(),
                frequencyMhz = sr.frequency,
                estimatedDistanceMeters = dist,
                matchedAnchorLabel = anchor?.label
            )
        }.sortedByDescending { it.rssiDbm }.take(18)

        _state.update { it.copy(wifiSignals = readings) }
        recomputePositionFromSignals()
    }

    @SuppressLint("MissingPermission")
    private fun startBleScanning() {
        if (isBleScanning) return
        if (!_state.value.hardwareStatus.hasBluetoothScanPermission) return
        val adapter = bluetoothManager?.adapter ?: return
        if (!adapter.isEnabled) return
        bleScanner = adapter.bluetoothLeScanner ?: return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            bleScanner?.startScan(null, settings, bleScanCallback)
            isBleScanning = true
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScanning() {
        if (!isBleScanning) return
        try {
            bleScanner?.stopScan(bleScanCallback)
        } catch (_: SecurityException) {
        }
        isBleScanning = false
    }

    private fun onStepDetected() {
        _state.update { current ->
            val nextSteps = current.stepCount + 1
            val pos = current.position
            if (pos == null || current.isPreviewWalkActive) {
                current.copy(stepCount = nextSteps)
            } else {
                // Advance 0.72m per step converted into floor units
                val stepUnits = 0.72f / 0.08f
                val rad = Math.toRadians(current.headingDegrees.toDouble())
                val newX = (pos.x + (sin(rad) * stepUnits).toFloat()).coerceIn(40f, 960f)
                val newY = (pos.y - (cos(rad) * stepUnits).toFloat()).coerceIn(40f, 680f)
                val nearest = findNearestRoom(pos.blockId, pos.floorNumber, newX, newY)
                current.copy(
                    stepCount = nextSteps,
                    position = pos.copy(
                        x = newX,
                        y = newY,
                        accuracyMeters = (pos.accuracyMeters + 0.12f).coerceAtMost(6.5f),
                        nearestRoomId = nearest?.id ?: pos.nearestRoomId,
                        nearestRoomLabel = nearest?.let { "${it.code} • ${it.name}" } ?: pos.nearestRoomLabel,
                        sourceMode = if (pos.sourceMode == IpsSourceMode.MANUAL_ANCHOR) {
                            IpsSourceMode.PDR_DEAD_RECKONING
                        } else {
                            IpsSourceMode.FUSED_WIFI_BLE_PDR
                        },
                        timestampMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun recomputePositionFromSignals() {
        val currentState = _state.value
        if (currentState.isPreviewWalkActive) return

        // 1. Check user-calibrated room RSSI fingerprints (KNN match)
        val calMatch = matchUserCalibration(
            wifi = currentState.wifiSignals,
            ble = currentState.bleBeacons,
            calibrations = currentState.customCalibrations
        )
        if (calMatch != null) {
            val nearest = findNearestRoom(calMatch.blockId, calMatch.floorNumber, calMatch.x, calMatch.y)
            _state.update {
                it.copy(
                    position = IpsPositionEstimate(
                        blockId = calMatch.blockId,
                        floorNumber = calMatch.floorNumber,
                        x = calMatch.x,
                        y = calMatch.y,
                        accuracyMeters = 2.1f,
                        nearestRoomId = nearest?.id ?: calMatch.roomId,
                        nearestRoomLabel = nearest?.let { r -> "${r.code} • ${r.name}" } ?: calMatch.roomLabel,
                        sourceMode = IpsSourceMode.FUSED_WIFI_BLE_PDR,
                        headingDegrees = currentState.headingDegrees,
                        confidence = 0.90f,
                        timestampMs = System.currentTimeMillis()
                    )
                )
            }
            return
        }

        // 2. Trilaterate using matched MCE campus Wi-Fi APs & BLE Beacons
        data class WeightedAnchor(
            val anchor: CampusSignalAnchor,
            val distanceMeters: Float,
            val isBle: Boolean
        )

        val matched = mutableListOf<WeightedAnchor>()
        currentState.wifiSignals.forEach { reading ->
            findMatchingAnchorForWifi(reading.bssid, reading.ssid)?.let { anchor ->
                matched.add(WeightedAnchor(anchor, reading.estimatedDistanceMeters, isBle = false))
            }
        }
        currentState.bleBeacons.forEach { beacon ->
            findMatchingAnchorForBle(beacon.address, beacon.uuidOrService)?.let { anchor ->
                matched.add(WeightedAnchor(anchor, beacon.estimatedDistanceMeters, isBle = true))
            }
        }

        if (matched.isEmpty()) return

        // Select dominant (blockId, floorNumber) cluster
        val bestCluster = matched
            .groupBy { it.anchor.blockId to it.anchor.floorNumber }
            .maxByOrNull { (_, list) -> list.sumOf { (1.0 / it.distanceMeters.coerceAtLeast(1f)) } }
            ?: return

        val (blockId, floorNum) = bestCluster.key
        val clusterAnchors = bestCluster.value

        var sumW = 0f
        var sumX = 0f
        var sumY = 0f
        var minD = Float.MAX_VALUE
        clusterAnchors.forEach { item ->
            val d = item.distanceMeters.coerceAtLeast(0.8f)
            if (d < minD) minD = d
            val w = 1f / (d * d)
            sumW += w
            sumX += w * item.anchor.x
            sumY += w * item.anchor.y
        }

        if (sumW <= 0f) return
        val estX = (sumX / sumW).coerceIn(40f, 960f)
        val estY = (sumY / sumW).coerceIn(40f, 680f)
        val hasWifi = clusterAnchors.any { !it.isBle }
        val hasBle = clusterAnchors.any { it.isBle }

        val mode = when {
            hasWifi && hasBle -> IpsSourceMode.FUSED_WIFI_BLE_PDR
            hasBle -> IpsSourceMode.BLE_BEACON
            else -> IpsSourceMode.WIFI_TRIANGULATION
        }
        val accuracy = (minD * 0.45f + 1.2f).coerceIn(1.5f, 8.5f)
        val nearest = findNearestRoom(blockId, floorNum, estX, estY)

        _state.update {
            it.copy(
                position = IpsPositionEstimate(
                    blockId = blockId,
                    floorNumber = floorNum,
                    x = estX,
                    y = estY,
                    accuracyMeters = accuracy,
                    nearestRoomId = nearest?.id,
                    nearestRoomLabel = nearest?.let { r -> "${r.code} • ${r.name}" },
                    sourceMode = mode,
                    headingDegrees = currentState.headingDegrees,
                    confidence = (1f / (1f + accuracy / 6f)).coerceIn(0.45f, 0.96f),
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
        refreshHardwareStatus()
    }

    private fun matchUserCalibration(
        wifi: List<WifiSignalReading>,
        ble: List<BleBeaconReading>,
        calibrations: List<UserCalibrationBinding>
    ): UserCalibrationBinding? {
        if (calibrations.isEmpty()) return null
        val liveWifiMap = wifi.associate { it.bssid to it.rssiDbm }
        val liveBleMap = ble.associate { it.address to it.rssiDbm }

        var bestBinding: UserCalibrationBinding? = null
        var bestScore = Float.MAX_VALUE

        for (cal in calibrations) {
            var overlap = 0
            var diffSum = 0f
            cal.wifiSignatures.forEach { (bssid, refRssi) ->
                liveWifiMap[bssid]?.let { liveRssi ->
                    overlap++
                    val diff = (liveRssi - refRssi).toFloat()
                    diffSum += diff * diff
                }
            }
            cal.bleSignatures.forEach { (mac, refRssi) ->
                liveBleMap[mac]?.let { liveRssi ->
                    overlap++
                    val diff = (liveRssi - refRssi).toFloat()
                    diffSum += diff * diff
                }
            }
            if (overlap >= 1) {
                val rmse = sqrt(diffSum / overlap)
                // Penalize fewer overlapping signals slightly
                val score = rmse - overlap * 1.5f
                if (rmse < 16f && score < bestScore) {
                    bestScore = score
                    bestBinding = cal
                }
            }
        }
        return bestBinding
    }

    private fun findMatchingAnchorForWifi(bssid: String, ssid: String): CampusSignalAnchor? {
        val lowerBssid = bssid.lowercase()
        return MceCampusData.signalAnchors.find { anchor ->
            anchor.wifiBssids.any { it.equals(lowerBssid, ignoreCase = true) } ||
                (ssid.isNotBlank() && anchor.wifiSsidPrefixes.any { ssid.startsWith(it, ignoreCase = true) })
        }
    }

    private fun findMatchingAnchorForBle(mac: String, uuid: String): CampusSignalAnchor? {
        return MceCampusData.signalAnchors.find { anchor ->
            anchor.bleMacs.any { it.equals(mac, ignoreCase = true) } ||
                (anchor.beaconUuid != null && uuid.equals(anchor.beaconUuid, ignoreCase = true))
        }
    }

    private fun findNearestRoom(blockId: String, floorNumber: Int, x: Float, y: Float): CampusRoom? {
        val floor = MceCampusData.getFloor(blockId, floorNumber) ?: return null
        return floor.rooms.minByOrNull { room ->
            hypot(room.rect.centerX - x, room.rect.centerY - y)
        }
    }

    private fun rssiToMeters(rssi: Int, txPower: Int, pathLossExp: Float): Float {
        val ratio = (txPower - rssi) / (10f * pathLossExp)
        return 10f.pow(ratio).coerceIn(0.6f, 45f)
    }

    private fun smoothAngle(current: Float, target: Float, alpha: Float): Float {
        var diff = (target - current + 540f) % 360f - 180f
        return ((current + alpha * diff) + 360f) % 360f
    }

    private fun parseIBeaconUuid(bytes: ByteArray?): String? {
        if (bytes == null || bytes.size < 25) return null
        for (i in 0..(bytes.size - 23)) {
            if ((bytes[i].toInt() and 0xFF) == 0x4C &&
                (bytes[i + 1].toInt() and 0xFF) == 0x00 &&
                (bytes[i + 2].toInt() and 0xFF) == 0x02 &&
                (bytes[i + 3].toInt() and 0xFF) == 0x15
            ) {
                val hex = (0 until 16).joinToString("") { idx ->
                    "%02x".format(bytes[i + 4 + idx].toInt() and 0xFF)
                }
                return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
            }
        }
        return null
    }
}
