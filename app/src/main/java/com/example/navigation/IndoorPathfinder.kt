package com.example.navigation

import com.example.data.MceCampusData
import com.example.model.CampusRoom
import com.example.model.EdgeType
import com.example.model.NavEdge
import com.example.model.NavNode
import com.example.model.NavRoute
import com.example.model.RouteStep
import com.example.model.StepDirection
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.roundToInt

object IndoorPathfinder {

    private data class QueueEntry(
        val nodeId: String,
        val distance: Float
    ) : Comparable<QueueEntry> {
        override fun compareTo(other: QueueEntry): Int = distance.compareTo(other.distance)
    }

    fun findRoute(
        startRoom: CampusRoom,
        endRoom: CampusRoom,
        accessibleOnly: Boolean
    ): NavRoute? {
        if (startRoom.id == endRoom.id) {
            val node = MceCampusData.getNode(startRoom.doorNodeId) ?: return null
            val step = RouteStep(
                index = 0,
                direction = StepDirection.ARRIVE,
                instruction = "You are at ${endRoom.code} — ${endRoom.name}",
                detail = "${endRoom.wing}, ${floorLabel(endRoom.floorNumber)}",
                distanceMeters = 0,
                blockId = endRoom.blockId,
                floorNumber = endRoom.floorNumber,
                nodeIds = listOf(node.id)
            )
            return NavRoute(
                startRoom = startRoom,
                endRoom = endRoom,
                nodePath = listOf(node),
                steps = listOf(step),
                totalDistanceMeters = 0,
                estimatedSeconds = 0,
                accessibleOnly = accessibleOnly,
                crossesFloors = false,
                crossesBlocks = false
            )
        }

        val startNodeId = startRoom.doorNodeId
        val endNodeId = endRoom.doorNodeId

        val adjacency = MceCampusData.edges
            .filter { !accessibleOnly || it.accessible }
            .groupBy { it.fromId }

        val dist = mutableMapOf<String, Float>().withDefault { Float.MAX_VALUE }
        val prevNode = mutableMapOf<String, String>()
        val prevEdge = mutableMapOf<String, NavEdge>()
        val pq = PriorityQueue<QueueEntry>()

        dist[startNodeId] = 0f
        pq.add(QueueEntry(startNodeId, 0f))

        while (pq.isNotEmpty()) {
            val current = pq.poll() ?: break
            if (current.distance > dist.getValue(current.nodeId)) continue
            if (current.nodeId == endNodeId) break

            val neighbors = adjacency[current.nodeId].orEmpty()
            for (edge in neighbors) {
                // Slight preference for elevators when accessibleOnly is enabled
                val nextDist = current.distance + edge.distanceMeters
                if (nextDist < dist.getValue(edge.toId)) {
                    dist[edge.toId] = nextDist
                    prevNode[edge.toId] = current.nodeId
                    prevEdge[edge.toId] = edge
                    pq.add(QueueEntry(edge.toId, nextDist))
                }
            }
        }

        if (!dist.containsKey(endNodeId)) return null

        val pathNodes = mutableListOf<NavNode>()
        val pathEdges = mutableListOf<NavEdge>()
        var curr: String? = endNodeId
        while (curr != null) {
            MceCampusData.getNode(curr)?.let { pathNodes.add(it) }
            val edge = prevEdge[curr]
            if (edge != null) pathEdges.add(edge)
            curr = prevNode[curr]
        }
        pathNodes.reverse()
        pathEdges.reverse()

        val steps = buildTurnByTurnSteps(startRoom, endRoom, pathNodes, pathEdges)
        val totalMeters = dist.getValue(endNodeId).roundToInt().coerceAtLeast(2)
        val floorTransitions = pathEdges.count { it.type == EdgeType.STAIRS || it.type == EdgeType.ELEVATOR }
        val estimatedSeconds = ((totalMeters / 1.25f) + floorTransitions * 18).roundToInt().coerceAtLeast(10)

        return NavRoute(
            startRoom = startRoom,
            endRoom = endRoom,
            nodePath = pathNodes,
            steps = steps,
            totalDistanceMeters = totalMeters,
            estimatedSeconds = estimatedSeconds,
            accessibleOnly = accessibleOnly,
            crossesFloors = pathNodes.map { it.blockId to it.floorNumber }.distinct().size > 1,
            crossesBlocks = pathNodes.map { it.blockId }.distinct().size > 1
        )
    }

    private fun buildTurnByTurnSteps(
        startRoom: CampusRoom,
        endRoom: CampusRoom,
        nodes: List<NavNode>,
        edges: List<NavEdge>
    ): List<RouteStep> {
        if (nodes.isEmpty()) return emptyList()
        val steps = mutableListOf<RouteStep>()

        val firstNode = nodes.first()
        steps.add(
            RouteStep(
                index = 0,
                direction = StepDirection.START,
                instruction = "Start at ${startRoom.code} (${startRoom.name})",
                detail = "${blockName(startRoom.blockId)} • ${floorLabel(startRoom.floorNumber)}, ${startRoom.wing}",
                distanceMeters = 0,
                blockId = firstNode.blockId,
                floorNumber = firstNode.floorNumber,
                nodeIds = listOf(firstNode.id)
            )
        )

        var idx = 0
        var prevHeading: Float? = null
        while (idx < edges.size) {
            val edge = edges[idx]
            val fromNode = nodes[idx]
            val toNode = nodes[idx + 1]

            when (edge.type) {
                EdgeType.STAIRS, EdgeType.ELEVATOR -> {
                    val goingUp = toNode.floorNumber > fromNode.floorNumber
                    val dir = when {
                        edge.type == EdgeType.ELEVATOR && goingUp -> StepDirection.ELEVATOR_UP
                        edge.type == EdgeType.ELEVATOR && !goingUp -> StepDirection.ELEVATOR_DOWN
                        goingUp -> StepDirection.STAIRS_UP
                        else -> StepDirection.STAIRS_DOWN
                    }
                    val modeText = if (edge.type == EdgeType.ELEVATOR) "Take the Elevator" else "Use the Stairs"
                    val actionText = if (goingUp) "up to ${floorLabel(toNode.floorNumber)}" else "down to ${floorLabel(toNode.floorNumber)}"
                    steps.add(
                        RouteStep(
                            index = steps.size,
                            direction = dir,
                            instruction = "$modeText $actionText",
                            detail = "${blockName(toNode.blockId)} • Exit lift/stairs onto ${floorLabel(toNode.floorNumber)}",
                            distanceMeters = edge.distanceMeters.roundToInt(),
                            blockId = toNode.blockId,
                            floorNumber = toNode.floorNumber,
                            nodeIds = listOf(fromNode.id, toNode.id)
                        )
                    )
                    prevHeading = null
                    idx++
                }
                EdgeType.OUTDOOR_WALKWAY -> {
                    steps.add(
                        RouteStep(
                            index = steps.size,
                            direction = StepDirection.OUTDOOR_WALK,
                            instruction = "Walk across Campus Plaza to ${blockName(toNode.blockId)}",
                            detail = "Exit via ${fromNode.label} and enter ${toNode.label}",
                            distanceMeters = edge.distanceMeters.roundToInt(),
                            blockId = toNode.blockId,
                            floorNumber = toNode.floorNumber,
                            nodeIds = listOf(fromNode.id, toNode.id)
                        )
                    )
                    prevHeading = null
                    idx++
                }
                EdgeType.CORRIDOR -> {
                    // Merge consecutive straight corridor edges on the same floor
                    val segNodes = mutableListOf(fromNode.id, toNode.id)
                    var segDist = edge.distanceMeters
                    val segHeading = bearingDegrees(fromNode, toNode)

                    val turnDir = if (prevHeading == null) {
                        StepDirection.STRAIGHT
                    } else {
                        classifyTurn(prevHeading, segHeading)
                    }

                    var endIdx = idx + 1
                    var lastHeading = segHeading
                    while (endIdx < edges.size && edges[endIdx].type == EdgeType.CORRIDOR) {
                        val nextEdge = edges[endIdx]
                        val nA = nodes[endIdx]
                        val nB = nodes[endIdx + 1]
                        val nextHeading = bearingDegrees(nA, nB)
                        val delta = normalizeAngle(nextHeading - lastHeading)
                        if (kotlin.math.abs(delta) < 25f) {
                            segDist += nextEdge.distanceMeters
                            segNodes.add(nB.id)
                            lastHeading = nextHeading
                            endIdx++
                        } else {
                            break
                        }
                    }

                    val targetNode = nodes[endIdx]
                    val distInt = segDist.roundToInt().coerceAtLeast(2)
                    val verb = when (turnDir) {
                        StepDirection.TURN_LEFT -> "Turn left"
                        StepDirection.TURN_RIGHT -> "Turn right"
                        StepDirection.SLIGHT_LEFT -> "Bear slight left"
                        StepDirection.SLIGHT_RIGHT -> "Bear slight right"
                        else -> "Walk straight"
                    }
                    val landmark = if (endIdx == edges.size) {
                        "toward ${endRoom.code}"
                    } else {
                        "toward ${targetNode.label}"
                    }

                    steps.add(
                        RouteStep(
                            index = steps.size,
                            direction = turnDir,
                            instruction = "$verb $distInt m $landmark",
                            detail = "${blockName(fromNode.blockId)} • ${floorLabel(fromNode.floorNumber)}",
                            distanceMeters = distInt,
                            blockId = fromNode.blockId,
                            floorNumber = fromNode.floorNumber,
                            nodeIds = segNodes
                        )
                    )

                    prevHeading = lastHeading
                    idx = endIdx
                }
            }
        }

        val lastNode = nodes.last()
        val sideHint = determineSideHint(prevHeading, lastNode, endRoom)
        steps.add(
            RouteStep(
                index = steps.size,
                direction = StepDirection.ARRIVE,
                instruction = "Arrive at ${endRoom.code} — ${endRoom.name}",
                detail = "$sideHint • ${endRoom.wing}, ${floorLabel(endRoom.floorNumber)}",
                distanceMeters = 0,
                blockId = endRoom.blockId,
                floorNumber = endRoom.floorNumber,
                nodeIds = listOf(lastNode.id)
            )
        )

        return steps
    }

    private fun bearingDegrees(a: NavNode, b: NavNode): Float {
        val rad = atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())
        return ((Math.toDegrees(rad).toFloat() + 360f) % 360f)
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = (angle + 180f) % 360f
        if (a < 0f) a += 360f
        return a - 180f
    }

    private fun classifyTurn(prevHeading: Float, nextHeading: Float): StepDirection {
        val diff = normalizeAngle(nextHeading - prevHeading)
        return when {
            diff in -25f..25f -> StepDirection.STRAIGHT
            diff in 25f..60f -> StepDirection.SLIGHT_RIGHT
            diff > 60f -> StepDirection.TURN_RIGHT
            diff in -60f..-25f -> StepDirection.SLIGHT_LEFT
            else -> StepDirection.TURN_LEFT
        }
    }

    private fun determineSideHint(lastHeading: Float?, doorNode: NavNode, room: CampusRoom): String {
        if (lastHeading == null) return "Destination is straight ahead"
        val toRoomAngle = Math.toDegrees(
            atan2((room.rect.centerY - doorNode.y).toDouble(), (room.rect.centerX - doorNode.x).toDouble())
        ).toFloat()
        val diff = normalizeAngle(toRoomAngle - lastHeading)
        return when {
            diff > 20f -> "Room entrance is on your right"
            diff < -20f -> "Room entrance is on your left"
            else -> "Room entrance is straight ahead"
        }
    }

    private fun floorLabel(floor: Int): String = when (floor) {
        0 -> "Ground Floor"
        1 -> "1st Floor"
        2 -> "2nd Floor"
        else -> "Floor $floor"
    }

    private fun blockName(blockId: String): String =
        MceCampusData.getBlock(blockId)?.name ?: blockId
}
