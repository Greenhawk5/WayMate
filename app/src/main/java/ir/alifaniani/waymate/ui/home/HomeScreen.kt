package ir.alifaniani.waymate.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ir.alifaniani.waymate.domain.model.GroupMemberLocation
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.ui.designsystem.CircularOrbitNavigationBar
import ir.alifaniani.waymate.ui.designsystem.OrbitNavigationItem
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.home.map.PointOfInterestDialog
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import kotlinx.coroutines.launch

/**
 * Secondary independent destinations reachable from Profile.
 */
private enum class SecondaryDestination {
    SETTINGS,
    ABOUT
}

/**
 * Redesigned WayMate Home Hub.
 *
 * Orchestrates 5 dedicated primary destinations via fluid HorizontalPager and custom
 * Circular Orbit Navigation system, plus 2 secondary overlay destinations (Settings and About).
 *
 * Key Interactions:
 * 1. Horizontal swipe navigation between pages with Persian RTL support.
 * 2. Strict gesture isolation: Map panning/zooming on Tab 0 (Live Map) is preserved without pager conflicts.
 * 3. 100% synchronization across swipe, tap, orbit drag, and system back button.
 * 4. Preserves 100% of underlying business logic, state holders, and operational workflows.
 */
@Composable
fun HomeScreen(
    user: User,
    onLogoutClicked: () -> Unit,
    homeUiState: HomeUiState = HomeUiState(),
    onRetryTeams: () -> Unit = {},
    onStatusSelected: (OperationalStatus) -> Unit = {},
    onAcknowledgeMessage: (String) -> Unit = {},
    onSendMessage: (body: String, isUrgent: Boolean, targetTeamId: String?) -> Unit = { _, _, _ -> },
    onRefresh: () -> Unit = {},
    onCreateUser: (username: String, password: String, role: UserRole, callsign: String) -> Unit = { _, _, _, _ -> },
    onToggleUserStatus: (ManagedUser) -> Unit = {},
    onCreateTeam: (name: String, leadUserId: String, vehicleType: String, vehiclePlate: String, equipment: String) -> Unit = { _, _, _, _, _ -> },
    onToggleTeamStatus: (Team) -> Unit = {},
    onAddTeamMember: (teamId: String, fullName: String, callsign: String, roleInTeam: String) -> Unit = { _, _, _, _ -> },
    onRemoveTeamMember: (teamId: String, memberId: String) -> Unit = { _, _ -> },
    onDismissFeedback: () -> Unit = {},
    onSyncQueue: () -> Unit = {},
    onDismissSyncFeedback: () -> Unit = {},
    onSelectPoint: (PointOfInterest?) -> Unit = {},
    onOpenCreatePointDialog: () -> Unit = {},
    onOpenEditPointDialog: (PointOfInterest) -> Unit = {},
    onDeletePoint: (String) -> Unit = {},
    onConfirmCreatePoint: (title: String, description: String, category: PointCategory, latitude: Double, longitude: Double) -> Unit = { _, _, _, _, _ -> },
    onConfirmUpdatePoint: (id: String, title: String, description: String, category: PointCategory, latitude: Double, longitude: Double, version: Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onDismissPointDialog: () -> Unit = {},
    onDismissPointFeedback: () -> Unit = {},
    onSelectGroupMember: (GroupMemberLocation?) -> Unit = {},
    onStartSharing: () -> Unit = {},
    onStopSharing: () -> Unit = {},
    onDismissLocationRationale: () -> Unit = {},
    onConfirmLocationRationale: () -> Unit = {},
    onDismissLocationPermissionDenied: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onRetryPermissionRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 5 Primary Orbit Destinations
    val navItems = remember {
        listOf(
            OrbitNavigationItem(
                id = "map",
                title = "نقشه زنده",
                icon = TacticalIcons.TacticalMap,
                contentDescription = "نقشه عملیاتی و مکانی زنده"
            ),
            OrbitNavigationItem(
                id = "overview",
                title = "نمای کلی",
                icon = TacticalIcons.Overview,
                contentDescription = "نمای کلی وضعیت عملیات"
            ),
            OrbitNavigationItem(
                id = "teams",
                title = "تیم‌ها",
                icon = TacticalIcons.Teams,
                contentDescription = "مدیریت گروه‌ها و اعضا"
            ),
            OrbitNavigationItem(
                id = "operations",
                title = "عملیات",
                icon = TacticalIcons.Operations,
                contentDescription = "ارتباطات و پیام‌های تاکتیکی"
            ),
            OrbitNavigationItem(
                id = "profile",
                title = "پروفایل",
                icon = TacticalIcons.Profile,
                contentDescription = "مشخصات و تنظیمات کاربری"
            )
        )
    }

    val coroutineScope = rememberCoroutineScope()
    // Default to Overview (tab index 1)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { navItems.size })
    var activeSecondaryScreen by remember { mutableStateOf<SecondaryDestination?>(null) }

    // System Back Navigation Integration:
    // 1. If currently inside a secondary destination (Settings/About), Back returns to Profile.
    BackHandler(enabled = activeSecondaryScreen != null) {
        activeSecondaryScreen = null
    }

    // 2. If on a primary destination other than Overview (index 1), Back returns to Overview.
    BackHandler(enabled = activeSecondaryScreen == null && pagerState.currentPage != 1) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WayMateMidnightDark)
    ) {
        // Primary Destination Pager with horizontal swipe and gesture isolation
        HorizontalPager(
            state = pagerState,
            // Disable pager scrolling on Tab 0 (LiveMapScreen) to prevent stealing map gestures
            userScrollEnabled = activeSecondaryScreen == null && pagerState.currentPage != 0,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            when (pageIndex) {
                0 -> {
                    LiveMapScreen(
                        points = homeUiState.points,
                        selectedPoint = homeUiState.selectedPoint,
                        onSelectPoint = onSelectPoint,
                        groupMembers = homeUiState.locationSharingState.activeSharers,
                        selectedGroupMember = homeUiState.selectedGroupMember,
                        onSelectGroupMember = onSelectGroupMember,
                        onOpenCreatePointDialog = onOpenCreatePointDialog,
                        onOpenEditPointDialog = onOpenEditPointDialog,
                        onDeletePoint = onDeletePoint
                    )
                }
                1 -> {
                    OverviewScreen(
                        user = user,
                        homeUiState = homeUiState,
                        onStatusSelected = onStatusSelected,
                        onStartSharing = onStartSharing,
                        onStopSharing = onStopSharing,
                        onSyncQueue = onSyncQueue,
                        onAcknowledgeMessage = onAcknowledgeMessage,
                        onNavigateToOperations = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        }
                    )
                }
                2 -> {
                    TeamsScreen(
                        user = user,
                        homeUiState = homeUiState,
                        onRetryTeams = onRetryTeams,
                        onCreateUser = onCreateUser,
                        onToggleUserStatus = onToggleUserStatus,
                        onCreateTeam = onCreateTeam,
                        onToggleTeamStatus = onToggleTeamStatus,
                        onAddTeamMember = onAddTeamMember,
                        onRemoveTeamMember = onRemoveTeamMember,
                        onDismissFeedback = onDismissFeedback
                    )
                }
                3 -> {
                    OperationsScreen(
                        user = user,
                        homeUiState = homeUiState,
                        onStatusSelected = onStatusSelected,
                        onAcknowledgeMessage = onAcknowledgeMessage,
                        onSendMessage = onSendMessage,
                        onRefresh = onRefresh,
                        onSyncQueue = onSyncQueue,
                        onDismissSyncFeedback = onDismissSyncFeedback
                    )
                }
                4 -> {
                    ProfileScreen(
                        user = user,
                        onLogoutClicked = onLogoutClicked,
                        onNavigateToSettings = { activeSecondaryScreen = SecondaryDestination.SETTINGS },
                        onNavigateToAbout = { activeSecondaryScreen = SecondaryDestination.ABOUT }
                    )
                }
            }
        }

        // Fixed Circular Orbit Navigation Bar (Visible when in Primary Destinations)
        if (activeSecondaryScreen == null) {
            CircularOrbitNavigationBar(
                items = navItems,
                selectedItemIndex = pagerState.currentPage,
                onItemSelected = { targetIndex ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetIndex)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Secondary Destinations (Settings & About) overlaid cleanly
        AnimatedVisibility(
            visible = activeSecondaryScreen != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (activeSecondaryScreen) {
                SecondaryDestination.SETTINGS -> {
                    SettingsScreen(
                        onNavigateBack = { activeSecondaryScreen = null },
                        onSyncQueue = onSyncQueue,
                        offlineQueueCount = homeUiState.offlineQueueCount
                    )
                }
                SecondaryDestination.ABOUT -> {
                    AboutScreen(
                        onNavigateBack = { activeSecondaryScreen = null }
                    )
                }
                null -> Unit
            }
        }

        // Point of Interest Create / Edit Dialog
        if (homeUiState.isPointDialogVisible) {
            PointOfInterestDialog(
                initialPoint = homeUiState.editingPoint,
                onConfirmCreate = onConfirmCreatePoint,
                onConfirmUpdate = onConfirmUpdatePoint,
                onDismiss = onDismissPointDialog
            )
        }

        // Location Permission Rationale Dialog (Pre-permission explanation)
        if (homeUiState.isLocationPermissionRationaleVisible) {
            LocationPermissionRationaleDialog(
                onConfirm = onConfirmLocationRationale,
                onDismiss = onDismissLocationRationale
            )
        }

        // Location Permission Denied Dialog
        if (homeUiState.isLocationPermissionDenied) {
            LocationPermissionDeniedDialog(
                isPermanentlyDenied = homeUiState.isLocationPermissionPermanentlyDenied,
                onOpenSettings = onOpenAppSettings,
                onRetry = onRetryPermissionRequest,
                onDismiss = onDismissLocationPermissionDenied
            )
        }
    }
}
