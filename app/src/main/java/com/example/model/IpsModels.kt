package com.example.model

enum class IpsSourceMode(val label: String) {
    FUSED_WIFI_BLE_PDR("Wi-Fi + BLE + PDR"),
    WIFI_TRIANGULATION("Wi-Fi Trilateration"),
    BLE_BEACON("BLE Beacons"),
    PDR_DEAD_RECKONING("Step & Compass PDR"),
    MANUAL_ANCHOR("Manual Anchor"),
    IDLE("Ready to Scan")
}

data class WifiSignalReading(
    val bssid: String,
    val ssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val estimatedDistanceMeters: Float,
    val matchedAnchorLabel: String? = null
)

data class BleBeaconReading(
    val address: String,
    val name: String,
    val rssiDbm: Int,
    val txPowerDbm: Int,
    val uuidOrService: String,
    val estimatedDistanceMeters: Float,
    val matchedAnchorLabel: String? = null
)

data class CampusSignalAnchor(
    val id: String,
    val label: String,
    val blockId: String,
    val floorNumber: Int,
    val x: Float,
    val y: Float,
    val wifiBssids: List<String> = emptyList(),
    val wifiSsidPrefixes: List<String> = emptyList(),
    val bleMacs: List<String> = emptyList(),
    val beaconUuid: String? = null,
    val referenceTxPower: Int = -55,
    val roomId: String? = null
)

data class UserCalibrationBinding(
    val roomId: String,
    val roomLabel: String,
    val blockId: String,
    val floorNumber: Int,
    val x: Float,
    val y: Float,
    val wifiSignatures: Map<String, Int>, // BSSID -> RSSI dBm
    val bleSignatures: Map<String, Int>   // BLE MAC -> RSSI dBm
)

data class IpsPositionEstimate(
    val blockId: String,
    val floorNumber: Int,
    val x: Float,
    val y: Float,
    val accuracyMeters: Float,
    val nearestRoomId: String?,
    val nearestRoomLabel: String?,
    val sourceMode: IpsSourceMode,
    val headingDegrees: Float,
    val confidence: Float,
    val timestampMs: Long
)

data class IpsHardwareStatus(
    val hasLocationPermission: Boolean = false,
    val hasWifiPermission: Boolean = false,
    val hasBluetoothScanPermission: Boolean = false,
    val hasActivityPermission: Boolean = false,
    val isWifiEnabled: Boolean = false,
    val isWifiRttSupported: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val hasRotationSensor: Boolean = false,
    val hasStepSensor: Boolean = false,
    val statusMessage: String = "Tap Locate to scan nearby Wi-Fi APs & BLE beacons"
)

data class IpsState(
    val hardwareStatus: IpsHardwareStatus = IpsHardwareStatus(),
    val position: IpsPositionEstimate? = null,
    val wifiSignals: List<WifiSignalReading> = emptyList(),
    val bleBeacons: List<BleBeaconReading> = emptyList(),
    val headingDegrees: Float = 0f,
    val stepCount: Int = 0,
    val customCalibrations: List<UserCalibrationBinding> = emptyList(),
    val isPreviewWalkActive: Boolean = false
)
