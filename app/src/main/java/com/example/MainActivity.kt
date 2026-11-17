package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.IpsStatusSheet
import com.example.ui.components.RoomDetailSheet
import com.example.ui.components.RoomPickerSheet
import com.example.ui.screens.BlockDirectoryScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.NavigateScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.theme.BlockWayTheme
import com.example.ui.viewmodel.CampusNavViewModel

object NavRoutes {
    const val EXPLORE = "explore"
    const val NAVIGATE = "navigate"
    const val SAVED = "saved"
    const val BLOCK_DETAIL = "block/{blockId}"
    fun blockDetail(blockId: String): String = "block/$blockId"
}

private data class TopTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockWayTheme {
                BlockWayApp()
            }
        }
    }
}

@Composable
fun BlockWayApp(
    viewModel: CampusNavViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsResult()
    }

    val ipsPos = uiState.ipsState.position
    LaunchedEffect(ipsPos?.x, ipsPos?.y, ipsPos?.floorNumber, ipsPos?.blockId) {
        if (ipsPos != null && !uiState.ipsState.isPreviewWalkActive) {
            viewModel.checkAutoAdvanceOnRealIps(
                posX = ipsPos.x,
                posY = ipsPos.y,
                blockId = ipsPos.blockId,
                floorNumber = ipsPos.floorNumber
            )
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissBanner()
    }

    val tabs = remember {
        listOf(
            TopTab(NavRoutes.EXPLORE, "Blocks", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_explore"),
            TopTab(NavRoutes.NAVIGATE, "Navigate", Icons.Filled.Map, Icons.Outlined.Map, "tab_navigate"),
            TopTab(NavRoutes.SAVED, "Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "tab_saved")
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevelTab = currentDestination?.route in tabs.map { it.route }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun requestLocate() {
        val hw = uiState.ipsState.hardwareStatus
        if (hw.hasLocationPermission) {
            viewModel.startIpsScanningIfPermitted()
            viewModel.setIpsDiagnosticsVisible(true)
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail && isTopLevelTab) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && isTopLevelTab) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.EXPLORE,
                        enterTransition = { fadeIn(tween(220)) },
                        exitTransition = { fadeOut(tween(180)) },
                        popEnterTransition = { fadeIn(tween(220)) },
                        popExitTransition = { fadeOut(tween(180)) }
                    ) {
                        composable(NavRoutes.EXPLORE) {
                            ExploreScreen(
                                uiState = uiState,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onCategorySelect = viewModel::selectCategoryFilter,
                                onOpenBlock = { block ->
                                    navController.navigate(NavRoutes.blockDetail(block.id))
                                },
                                onInspectRoom = viewModel::inspectRoom,
                                onNavigateToRoom = { room ->
                                    viewModel.navigateToRoom(room)
                                    navigateToTab(NavRoutes.NAVIGATE)
                                },
                                onOpenIpsSheet = { viewModel.setIpsDiagnosticsVisible(true) },
                                onLocateNow = { requestLocate() }
                            )
                        }

                        composable(
                            route = NavRoutes.BLOCK_DETAIL,
                            arguments = listOf(navArgument("blockId") { type = NavType.StringType }),
                            enterTransition = { slideInHorizontally(tween(240)) { it / 4 } + fadeIn(tween(220)) },
                            exitTransition = { fadeOut(tween(160)) },
                            popEnterTransition = { fadeIn(tween(200)) },
                            popExitTransition = { slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)) }
                        ) { entry ->
                            val blockId = entry.arguments?.getString("blockId") ?: "MB"
                            BlockDirectoryScreen(
                                blockId = blockId,
                                onBack = { navController.popBackStack() },
                                onInspectRoom = viewModel::inspectRoom,
                                onNavigateToRoom = { room ->
                                    viewModel.navigateToRoom(room)
                                    navigateToTab(NavRoutes.NAVIGATE)
                                },
                                onViewFloorOnMap = { blk, fl ->
                                    viewModel.setViewingFloor(blk, fl)
                                    navigateToTab(NavRoutes.NAVIGATE)
                                }
                            )
                        }

                        composable(NavRoutes.NAVIGATE) {
                            NavigateScreen(
                                uiState = uiState,
                                onMapSearchQueryChange = viewModel::updateMapSearchQuery,
                                onFocusRoomOnMap = viewModel::focusRoomOnMap,
                                onNavigateToSearchedRoom = viewModel::navigateToRoom,
                                onSelectStartRoom = { viewModel.openRoomPicker(forStart = true) },
                                onSelectEndRoom = { viewModel.openRoomPicker(forStart = false) },
                                onSwapEndpoints = viewModel::swapEndpoints,
                                onToggleAccessible = viewModel::toggleAccessibleOnly,
                                onSelectFloor = viewModel::setViewingFloor,
                                onRoomTapped = viewModel::inspectRoom,
                                onNextStep = viewModel::nextStep,
                                onPrevStep = viewModel::previousStep,
                                onSelectStep = viewModel::selectStep,
                                onTogglePreviewWalk = viewModel::togglePreviewWalk,
                                onOpenIpsSheet = { viewModel.setIpsDiagnosticsVisible(true) },
                                onSnapToIps = viewModel::snapMapToIpsPosition
                            )
                        }

                        composable(NavRoutes.SAVED) {
                            SavedScreen(
                                uiState = uiState,
                                onInspectRoom = viewModel::inspectRoom,
                                onNavigateToRoom = { room ->
                                    viewModel.navigateToRoom(room)
                                    navigateToTab(NavRoutes.NAVIGATE)
                                },
                                onToggleAccessible = viewModel::toggleAccessibleOnly,
                                onOpenIpsSheet = { viewModel.setIpsDiagnosticsVisible(true) }
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.inspectedRoom?.let { room ->
        RoomDetailSheet(
            room = room,
            isFavorite = uiState.favoriteRoomIds.contains(room.id),
            hasLiveSignals = uiState.ipsState.wifiSignals.isNotEmpty() || uiState.ipsState.bleBeacons.isNotEmpty(),
            onDismiss = { viewModel.inspectRoom(null) },
            onNavigateHere = { target ->
                viewModel.navigateToRoom(target)
                navigateToTab(NavRoutes.NAVIGATE)
            },
            onSetAsStart = { start ->
                viewModel.setStartRoom(start)
                navigateToTab(NavRoutes.NAVIGATE)
            },
            onToggleFavorite = viewModel::toggleFavorite,
            onCalibrateHere = { target ->
                viewModel.calibrateCurrentLocationToRoom(target)
                viewModel.inspectRoom(null)
                navigateToTab(NavRoutes.NAVIGATE)
            }
        )
    }

    uiState.showRoomPickerForStart?.let { pickingStart ->
        RoomPickerSheet(
            isPickingStart = pickingStart,
            ipsPosition = uiState.ipsState.position,
            onDismiss = { viewModel.openRoomPicker(null) },
            onRoomSelected = { chosen ->
                if (pickingStart) viewModel.setStartRoom(chosen)
                else viewModel.setEndRoom(chosen)
                viewModel.openRoomPicker(null)
            }
        )
    }

    if (uiState.showIpsDiagnosticsSheet) {
        IpsStatusSheet(
            ipsState = uiState.ipsState,
            onDismiss = { viewModel.setIpsDiagnosticsVisible(false) },
            onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
            onToggleScan = {
                if (!uiState.ipsState.hardwareStatus.hasLocationPermission) {
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    viewModel.toggleIpsScanning()
                }
            },
            onClearCalibrations = viewModel::clearCalibrations
        )
    }
}
