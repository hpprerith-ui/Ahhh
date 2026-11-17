package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MceCampusData
import com.example.model.CampusRoom
import com.example.model.FloorPlan
import com.example.model.IpsPositionEstimate
import com.example.model.NavRoute
import com.example.model.RoomCategory
import com.example.ui.theme.AmberWay
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralDest
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TealRoute
import com.example.ui.theme.TealRouteBright
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun FloorPlanCanvas(
    floorPlan: FloorPlan,
    activeRoute: NavRoute?,
    activeStepIndex: Int,
    ipsPosition: IpsPositionEstimate?,
    isIpsScanning: Boolean,
    highlightedRoomId: String?,
    focusedRoom: CampusRoom?,
    focusRequestNonce: Int,
    onRoomTapped: (CampusRoom) -> Unit,
    onCenterOnIps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()

    val zoomAnim = remember { Animatable(1f) }
    val panXAnim = remember { Animatable(0f) }
    val panYAnim = remember { Animatable(0f) }

    val cameraSpring = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    // Real-time smoothed user position & heading
    val smoothIpsX by animateFloatAsState(
        targetValue = ipsPosition?.x ?: 500f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ips_x"
    )
    val smoothIpsY by animateFloatAsState(
        targetValue = ipsPosition?.y ?: 360f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ips_y"
    )
    val smoothHeading by animateFloatAsState(
        targetValue = ipsPosition?.headingDegrees ?: 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ips_heading"
    )

    // Continuous 60fps real-time animations
    val infiniteTransition = rememberInfiniteTransition(label = "realtime_map_anim")
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_phase"
    )
    val routeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "route_traveler_progress"
    )
    val radarPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_phase"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val gridColor = if (isDark) Color(0xFF1F2D42) else Color(0xFFE2E8F0)
    val canvasBg = if (isDark) Color(0xFF0D1522) else Color(0xFFF1F5FA)
    val corridorColor = if (isDark) Color(0xFF1C283B) else Color(0xFFFFFFFF)
    val corridorBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val courtyardFill = if (isDark) Color(0xFF102421) else Color(0xFFE6F4EE)
    val courtyardBorder = if (isDark) Color(0xFF1E4A3E) else Color(0xFFA7D7C5)
    val wallStroke = if (isDark) Color(0xFF475569) else Color(0xFF64748B)
    val labelInk = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)

    val activeStepNodeIds = remember(activeRoute, activeStepIndex) {
        activeRoute?.steps?.getOrNull(activeStepIndex)?.nodeIds?.toSet().orEmpty()
    }

    val floorApAnchors = remember(floorPlan.blockId, floorPlan.floorNumber) {
        MceCampusData.signalAnchors.filter {
            it.blockId == floorPlan.blockId && it.floorNumber == floorPlan.floorNumber
        }
    }

    val pathMeasure = remember { PathMeasure() }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(canvasBg)
            .testTag("floor_plan_canvas")
    ) {
        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val baseScale = min(boxW / floorPlan.width, boxH / floorPlan.height) * 0.94f
        val baseOffsetX = (boxW - floorPlan.width * baseScale) / 2f
        val baseOffsetY = (boxH - floorPlan.height * baseScale) / 2f

        fun animateCameraTo(targetZoom: Float, targetPanX: Float, targetPanY: Float) {
            val maxPanX = boxW * 0.65f * targetZoom
            val maxPanY = boxH * 0.65f * targetZoom
            scope.launch {
                launch { zoomAnim.animateTo(targetZoom, cameraSpring) }
                launch { panXAnim.animateTo(targetPanX.coerceIn(-maxPanX, maxPanX), cameraSpring) }
                launch { panYAnim.animateTo(targetPanY.coerceIn(-maxPanY, maxPanY), cameraSpring) }
            }
        }

        // Smooth fly-to animation when searching or focusing a room on this floor
        LaunchedEffect(focusRequestNonce, floorPlan.blockId, floorPlan.floorNumber) {
            if (focusRequestNonce > 0 &&
                focusedRoom != null &&
                focusedRoom.blockId == floorPlan.blockId &&
                focusedRoom.floorNumber == floorPlan.floorNumber
            ) {
                val targetZoom = 1.65f
                val bx = baseOffsetX + focusedRoom.rect.centerX * baseScale
                val by = baseOffsetY + focusedRoom.rect.centerY * baseScale
                val targetPanX = (boxW / 2f - bx) * targetZoom
                val targetPanY = (boxH / 2f - by) * targetZoom
                animateCameraTo(targetZoom, targetPanX, targetPanY)
            } else {
                animateCameraTo(1f, 0f, 0f)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(floorPlan.blockId, floorPlan.floorNumber) {
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        val oldZoom = zoomAnim.value
                        val newZoom = (oldZoom * zoomChange).coerceIn(0.85f, 3.2f)
                        val pivot = centroid - Offset(boxW / 2f, boxH / 2f)
                        val oldPan = Offset(panXAnim.value, panYAnim.value)
                        val newPan = (oldPan - pivot) * (newZoom / oldZoom) + pivot + panChange
                        val maxPanX = boxW * 0.65f * newZoom
                        val maxPanY = boxH * 0.65f * newZoom
                        scope.launch {
                            zoomAnim.snapTo(newZoom)
                            panXAnim.snapTo(newPan.x.coerceIn(-maxPanX, maxPanX))
                            panYAnim.snapTo(newPan.y.coerceIn(-maxPanY, maxPanY))
                        }
                    }
                }
                .pointerInput(floorPlan.blockId, floorPlan.floorNumber) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val cx = boxW / 2f
                            val cy = boxH / 2f
                            val zoom = zoomAnim.value
                            val pan = Offset(panXAnim.value, panYAnim.value)
                            val unpanned = (tapOffset - pan - Offset(cx, cy)) / zoom + Offset(cx, cy)
                            val mapX = (unpanned.x - baseOffsetX) / baseScale
                            val mapY = (unpanned.y - baseOffsetY) / baseScale
                            val tappedRoom = floorPlan.rooms.firstOrNull { it.rect.contains(mapX, mapY) }
                            if (tappedRoom != null) {
                                onRoomTapped(tappedRoom)
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            val currentZoom = zoomAnim.value
                            if (currentZoom > 1.35f) {
                                animateCameraTo(1f, 0f, 0f)
                            } else {
                                val targetZoom = 1.9f
                                val cx = boxW / 2f
                                val cy = boxH / 2f
                                val pivot = tapOffset - Offset(cx, cy)
                                val currentPan = Offset(panXAnim.value, panYAnim.value)
                                val targetPan = (currentPan - pivot) * (targetZoom / currentZoom) + pivot
                                animateCameraTo(targetZoom, targetPan.x, targetPan.y)
                            }
                        }
                    )
                }
        ) {
            val zoom = zoomAnim.value
            val pan = Offset(panXAnim.value, panYAnim.value)
            val cx = size.width / 2f
            val cy = size.height / 2f

            fun toScreen(mx: Float, my: Float): Offset {
                val bx = baseOffsetX + mx * baseScale
                val by = baseOffsetY + my * baseScale
                return Offset(
                    cx + (bx - cx) * zoom + pan.x,
                    cy + (by - cy) * zoom + pan.y
                )
            }

            fun scaleDim(mSize: Float): Float = mSize * baseScale * zoom

            // 1. Blueprint grid background
            val gridStep = 50f
            var gx = 0f
            while (gx <= floorPlan.width) {
                val p1 = toScreen(gx, 0f)
                val p2 = toScreen(gx, floorPlan.height)
                drawLine(gridColor, p1, p2, strokeWidth = 1f)
                gx += gridStep
            }
            var gy = 0f
            while (gy <= floorPlan.height) {
                val p1 = toScreen(0f, gy)
                val p2 = toScreen(floorPlan.width, gy)
                drawLine(gridColor, p1, p2, strokeWidth = 1f)
                gy += gridStep
            }

            // 2. Courtyard
            floorPlan.courtyardRect?.let { cr ->
                val tl = toScreen(cr.x, cr.y)
                val sz = Size(scaleDim(cr.width), scaleDim(cr.height))
                drawRoundRect(
                    color = courtyardFill,
                    topLeft = tl,
                    size = sz,
                    cornerRadius = CornerRadius(scaleDim(16f), scaleDim(16f))
                )
                drawRoundRect(
                    color = courtyardBorder,
                    topLeft = tl,
                    size = sz,
                    cornerRadius = CornerRadius(scaleDim(16f), scaleDim(16f)),
                    style = Stroke(width = 2f)
                )
                drawCenteredLabel(
                    textMeasurer = textMeasurer,
                    text = "OPEN QUADRANGLE\nCOURTYARD",
                    center = toScreen(cr.centerX, cr.centerY),
                    maxWidthPx = sz.width * 0.8f,
                    color = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E),
                    fontSizeSp = (11f * zoom.coerceIn(0.85f, 1.3f))
                )
            }

            // 3. Corridors
            floorPlan.corridors.forEach { cor ->
                val tl = toScreen(cor.x, cor.y)
                val sz = Size(scaleDim(cor.width), scaleDim(cor.height))
                drawRect(color = corridorColor, topLeft = tl, size = sz)
                drawRect(color = corridorBorder, topLeft = tl, size = sz, style = Stroke(width = 1.5f))
            }

            // 4. Rooms & Labs
            floorPlan.rooms.forEach { room ->
                val isStart = activeRoute?.startRoom?.id == room.id
                val isEnd = activeRoute?.endRoom?.id == room.id
                val isFocused = focusedRoom?.id == room.id
                val isInspected = highlightedRoomId == room.id || isFocused
                val fillColor = roomFillColor(room.category, isDark, isStart, isEnd, isInspected)
                val borderColor = when {
                    isEnd -> CoralDest
                    isFocused -> TealRoute
                    isStart -> CobaltPrimary
                    isInspected -> AmberWay
                    else -> wallStroke
                }
                val borderWidth = if (isStart || isEnd || isInspected) 3.6f else 1.8f

                val tl = toScreen(room.rect.x, room.rect.y)
                val sz = Size(scaleDim(room.rect.width), scaleDim(room.rect.height))
                drawRoundRect(
                    color = fillColor,
                    topLeft = tl,
                    size = sz,
                    cornerRadius = CornerRadius(scaleDim(8f), scaleDim(8f))
                )
                drawRoundRect(
                    color = borderColor,
                    topLeft = tl,
                    size = sz,
                    cornerRadius = CornerRadius(scaleDim(8f), scaleDim(8f)),
                    style = Stroke(width = borderWidth)
                )

                MceCampusData.getNode(room.doorNodeId)?.let { doorNode ->
                    val roomEdgeCenter = toScreen(room.rect.centerX, room.rect.centerY)
                    val doorPt = toScreen(doorNode.x, doorNode.y)
                    val mid = Offset(
                        (roomEdgeCenter.x + doorPt.x) / 2f,
                        (roomEdgeCenter.y + doorPt.y) / 2f
                    )
                    drawCircle(
                        color = borderColor.copy(alpha = 0.55f),
                        radius = (3f * zoom).coerceIn(2.5f, 5f),
                        center = mid
                    )
                }

                if (sz.width > 30f && sz.height > 24f) {
                    val textColor = when {
                        isEnd -> CoralDest
                        isFocused -> if (isDark) TealRouteBright else TealRoute
                        isStart -> if (isDark) Color(0xFF93C5FD) else CobaltPrimary
                        else -> labelInk
                    }
                    drawCenteredLabel(
                        textMeasurer = textMeasurer,
                        text = room.code,
                        center = toScreen(room.rect.centerX, room.rect.centerY),
                        maxWidthPx = sz.width - 8f,
                        color = textColor,
                        fontSizeSp = (9.5f * zoom.coerceIn(0.85f, 1.35f)),
                        bold = true
                    )
                }
            }

            // Real-time sonar target ripple around searched/focused room
            if (focusedRoom != null &&
                focusedRoom.blockId == floorPlan.blockId &&
                focusedRoom.floorNumber == floorPlan.floorNumber
            ) {
                val focusCenter = toScreen(focusedRoom.rect.centerX, focusedRoom.rect.centerY)
                drawSonarRipple(
                    center = focusCenter,
                    phase = radarPhase,
                    maxRadius = (58f * zoom).coerceIn(40f, 95f),
                    color = TealRouteBright
                )
            }

            // 5. Mapped Wi-Fi AP & BLE Anchor Micro-Markers + Real-Time Scan Waves
            floorApAnchors.forEach { anchor ->
                val pt = toScreen(anchor.x, anchor.y)
                val r = (5.5f * zoom).coerceIn(4f, 9f)
                if (isIpsScanning) {
                    drawSonarRipple(
                        center = pt,
                        phase = radarPhase,
                        maxRadius = (36f * zoom).coerceIn(24f, 56f),
                        color = if (isDark) Color(0xFF60A5FA) else CobaltPrimary
                    )
                } else {
                    drawCircle(
                        color = CobaltPrimary.copy(alpha = 0.22f),
                        radius = r * 2f,
                        center = pt
                    )
                }
                drawCircle(
                    color = if (isDark) Color(0xFF60A5FA) else CobaltPrimary,
                    radius = r * 0.7f,
                    center = pt
                )
            }

            // 6. Active Route Polyline + Real-Time Traveling Route Pulse Particles
            if (activeRoute != null) {
                val floorSegments = mutableListOf<List<Offset>>()
                var currentSeg = mutableListOf<Offset>()

                activeRoute.nodePath.forEach { node ->
                    if (node.blockId == floorPlan.blockId && node.floorNumber == floorPlan.floorNumber) {
                        currentSeg.add(toScreen(node.x, node.y))
                    } else {
                        if (currentSeg.size >= 2) floorSegments.add(currentSeg)
                        currentSeg = mutableListOf()
                    }
                }
                if (currentSeg.size >= 2) floorSegments.add(currentSeg)

                val routeMainColor = if (isDark) TealRouteBright else TealRoute
                floorSegments.forEach { pts ->
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) {
                            lineTo(pts[i].x, pts[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = routeMainColor.copy(alpha = 0.24f),
                        style = Stroke(width = 14f * zoom.coerceIn(0.8f, 1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = routeMainColor,
                        style = Stroke(
                            width = 5.5f * zoom.coerceIn(0.85f, 1.4f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), dashPhase)
                        )
                    )

                    // Real-time traveling glowing orbs flowing Start -> Destination
                    pathMeasure.setPath(path, false)
                    val totalLen = pathMeasure.length
                    if (totalLen > 12f) {
                        val pulseOffsets = listOf(0f, 0.33f, 0.66f)
                        pulseOffsets.forEach { offset ->
                            val frac = (routeProgress + offset) % 1f
                            val pos = pathMeasure.getPosition(totalLen * frac)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = (6.5f * zoom).coerceIn(5f, 9f),
                                center = pos
                            )
                            drawCircle(
                                color = routeMainColor,
                                radius = (4.2f * zoom).coerceIn(3.5f, 6.5f),
                                center = pos
                            )
                        }
                    }
                }

                val activeNodesOnFloor = activeRoute.nodePath.filter {
                    it.blockId == floorPlan.blockId &&
                        it.floorNumber == floorPlan.floorNumber &&
                        activeStepNodeIds.contains(it.id)
                }
                if (activeNodesOnFloor.size >= 2) {
                    val activePath = Path().apply {
                        val first = toScreen(activeNodesOnFloor.first().x, activeNodesOnFloor.first().y)
                        moveTo(first.x, first.y)
                        for (i in 1 until activeNodesOnFloor.size) {
                            val pt = toScreen(activeNodesOnFloor[i].x, activeNodesOnFloor[i].y)
                            lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = activePath,
                        color = AmberWay,
                        style = Stroke(width = 7f * zoom.coerceIn(0.85f, 1.4f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                val startNode = activeRoute.nodePath.firstOrNull()
                if (startNode != null && startNode.blockId == floorPlan.blockId && startNode.floorNumber == floorPlan.floorNumber) {
                    val sp = toScreen(startNode.x, startNode.y)
                    drawCircle(Color.White, radius = 9f, center = sp)
                    drawCircle(CobaltPrimary, radius = 6.5f, center = sp)
                }
                val endNode = activeRoute.nodePath.lastOrNull()
                if (endNode != null && endNode.blockId == floorPlan.blockId && endNode.floorNumber == floorPlan.floorNumber) {
                    val ep = toScreen(endNode.x, endNode.y)
                    drawSonarRipple(
                        center = ep,
                        phase = radarPhase,
                        maxRadius = (38f * zoom).coerceIn(26f, 60f),
                        color = CoralDest
                    )
                    drawCircle(Color.White, radius = 10f, center = ep)
                    drawCircle(CoralDest, radius = 7.5f, center = ep)
                }
            }

            // 7. Real-Time Smoothed IPS User Location Dot + Accuracy Halo + Compass Heading Cone
            if (ipsPosition != null &&
                ipsPosition.blockId == floorPlan.blockId &&
                ipsPosition.floorNumber == floorPlan.floorNumber
            ) {
                val userPt = toScreen(smoothIpsX, smoothIpsY)
                val accuracyPx = scaleDim(ipsPosition.accuracyMeters / floorPlan.metersPerUnit)
                    .coerceIn(18f, 120f)

                drawCircle(
                    color = CobaltPrimary.copy(alpha = 0.14f),
                    radius = accuracyPx,
                    center = userPt
                )
                drawCircle(
                    color = CobaltPrimary.copy(alpha = 0.45f),
                    radius = accuracyPx,
                    center = userPt,
                    style = Stroke(width = 1.8f)
                )

                val coneRadius = (46f * zoom).coerceIn(34f, 74f)
                val startAngleDeg = smoothHeading - 90f - 28f
                drawArc(
                    color = TealRoute.copy(alpha = 0.36f),
                    startAngle = startAngleDeg,
                    sweepAngle = 56f,
                    useCenter = true,
                    topLeft = Offset(userPt.x - coneRadius, userPt.y - coneRadius),
                    size = Size(coneRadius * 2f, coneRadius * 2f)
                )

                drawCircle(
                    color = CobaltPrimary.copy(alpha = 0.3f),
                    radius = 15f * pulseScale,
                    center = userPt
                )
                drawCircle(color = Color.White, radius = 10f, center = userPt)
                drawCircle(color = Color(0xFF2563EB), radius = 7f, center = userPt)
            }
        }

        // Top-left floor title badge
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = "${floorPlan.blockId} • Floor ${floorPlan.floorLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Top-right Zoom Reset & Center IPS buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            if (ipsPosition != null) {
                FilledTonalIconButton(
                    onClick = {
                        onCenterOnIps()
                        val targetZoom = 1.45f
                        val bx = baseOffsetX + ipsPosition.x * baseScale
                        val by = baseOffsetY + ipsPosition.y * baseScale
                        val targetPanX = (boxW / 2f - bx) * targetZoom
                        val targetPanY = (boxH / 2f - by) * targetZoom
                        animateCameraTo(targetZoom, targetPanX, targetPanY)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("center_ips_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on my indoor location",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            FilledTonalIconButton(
                onClick = { animateCameraTo(1f, 0f, 0f) },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("reset_map_zoom_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Reset floor plan zoom",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom-left compact map legend
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                LegendDot(color = TealRoute, label = "Route")
                LegendDot(color = Color(0xFF2563EB), label = "You (IPS)")
                LegendDot(color = CoralDest, label = "Dest")
            }
        }
    }
}

private fun DrawScope.drawSonarRipple(
    center: Offset,
    phase: Float,
    maxRadius: Float,
    color: Color
) {
    val p1 = phase
    val p2 = (phase + 0.5f) % 1f
    listOf(p1, p2).forEach { p ->
        val radius = 10f + (maxRadius - 10f) * p
        val alpha = (1f - p).coerceIn(0f, 1f) * 0.55f
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun roomFillColor(
    category: RoomCategory,
    isDark: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isInspected: Boolean
): Color {
    if (isEnd) return if (isDark) Color(0xFF4C1D24) else Color(0xFFFEE2E2)
    if (isStart) return if (isDark) Color(0xFF1E3A6E) else Color(0xFFDBEAFE)
    if (isInspected) return if (isDark) Color(0xFF4A3212) else Color(0xFFFEF3C7)
    return when (category) {
        RoomCategory.LAB -> if (isDark) Color(0xFF172D4A) else Color(0xFFE0ECFF)
        RoomCategory.CLASSROOM -> if (isDark) Color(0xFF1B2536) else Color(0xFFF1F5F9)
        RoomCategory.OFFICE -> if (isDark) Color(0xFF2A241B) else Color(0xFFFEF7E6)
        RoomCategory.FACILITY -> if (isDark) Color(0xFF173131) else Color(0xFFE6F7F5)
        RoomCategory.ENTRANCE -> if (isDark) Color(0xFF123832) else Color(0xFFCCFBF1)
        RoomCategory.ELEVATOR -> if (isDark) Color(0xFF2E2244) else Color(0xFFF3E8FF)
        RoomCategory.STAIRS -> if (isDark) Color(0xFF33261A) else Color(0xFFFFEDD5)
        RoomCategory.AMENITY -> if (isDark) Color(0xFF232934) else Color(0xFFE2E8F0)
    }
}

private fun DrawScope.drawCenteredLabel(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    maxWidthPx: Float,
    color: Color,
    fontSizeSp: Float,
    bold: Boolean = false
) {
    val maxW = maxWidthPx.toInt().coerceAtLeast(12)
    val result = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = fontSizeSp.sp,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        constraints = Constraints(maxWidth = maxW)
    )
    drawText(
        textLayoutResult = result,
        topLeft = Offset(
            center.x - result.size.width / 2f,
            center.y - result.size.height / 2f
        )
    )
}
