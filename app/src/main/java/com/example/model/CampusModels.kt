package com.example.model

enum class RoomCategory(val label: String) {
    LAB("Lab"),
    CLASSROOM("Lecture Hall"),
    OFFICE("Office / Admin"),
    FACILITY("Hall / Library"),
    AMENITY("Amenity"),
    STAIRS("Stairs"),
    ELEVATOR("Elevator"),
    ENTRANCE("Entrance")
}

enum class EdgeType {
    CORRIDOR,
    STAIRS,
    ELEVATOR,
    OUTDOOR_WALKWAY
}

enum class StepDirection {
    START,
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    STAIRS_UP,
    STAIRS_DOWN,
    ELEVATOR_UP,
    ELEVATOR_DOWN,
    OUTDOOR_WALK,
    ARRIVE
}

data class CampusBlock(
    val id: String,
    val code: String,
    val name: String,
    val subtitle: String,
    val accentHex: Long,
    val floors: List<FloorPlan>,
    val description: String
)

data class FloorPlan(
    val blockId: String,
    val floorNumber: Int,
    val floorLabel: String, // "G", "1", "2"
    val floorTitle: String,
    val width: Float = 1000f,
    val height: Float = 720f,
    val metersPerUnit: Float = 0.08f,
    val hasCourtyard: Boolean = false,
    val courtyardRect: RectArea? = null,
    val corridors: List<RectArea>,
    val rooms: List<CampusRoom>
)

data class RectArea(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    fun contains(px: Float, py: Float): Boolean =
        px >= x && px <= x + width && py >= y && py <= y + height
}

data class CampusRoom(
    val id: String,
    val blockId: String,
    val floorNumber: Int,
    val code: String,
    val name: String,
    val category: RoomCategory,
    val wing: String,
    val rect: RectArea,
    val doorNodeId: String,
    val description: String,
    val keywords: List<String> = emptyList()
)

data class NavNode(
    val id: String,
    val blockId: String,
    val floorNumber: Int,
    val x: Float,
    val y: Float,
    val label: String = "",
    val isStairs: Boolean = false,
    val isElevator: Boolean = false,
    val isEntrance: Boolean = false
)

data class NavEdge(
    val fromId: String,
    val toId: String,
    val distanceMeters: Float,
    val type: EdgeType = EdgeType.CORRIDOR,
    val accessible: Boolean = type != EdgeType.STAIRS
)

data class RouteStep(
    val index: Int,
    val direction: StepDirection,
    val instruction: String,
    val detail: String,
    val distanceMeters: Int,
    val blockId: String,
    val floorNumber: Int,
    val nodeIds: List<String>
)

data class NavRoute(
    val startRoom: CampusRoom,
    val endRoom: CampusRoom,
    val nodePath: List<NavNode>,
    val steps: List<RouteStep>,
    val totalDistanceMeters: Int,
    val estimatedSeconds: Int,
    val accessibleOnly: Boolean,
    val crossesFloors: Boolean,
    val crossesBlocks: Boolean
)
