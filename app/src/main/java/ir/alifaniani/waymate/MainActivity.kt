package ir.alifaniani.waymate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import ir.alifaniani.waymate.service.LocationSharingService
import ir.alifaniani.waymate.ui.auth.LoginScreen
import ir.alifaniani.waymate.ui.auth.LoginViewModel
import ir.alifaniani.waymate.ui.home.HomeScreen
import ir.alifaniani.waymate.ui.home.HomeViewModel
import ir.alifaniani.waymate.ui.theme.WayMateTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as WayMateApp
        val loginViewModel: LoginViewModel = ViewModelProvider(
            this,
            LoginViewModel.provideFactory(app.container.authRepository)
        )[LoginViewModel::class.java]

        val homeViewModel: HomeViewModel = ViewModelProvider(
            this,
            HomeViewModel.provideFactory(
                teamRepository = app.container.teamRepository,
                messageRepository = app.container.messageRepository,
                statusRepository = app.container.statusRepository,
                authRepository = app.container.authRepository,
                adminRepository = app.container.adminRepository,
                offlineSyncManager = app.container.offlineSyncManager,
                pointRepository = app.container.pointRepository,
                observeLocationSharingStateUseCase = app.container.observeLocationSharingStateUseCase,
                stopLocationSharingUseCase = app.container.stopLocationSharingUseCase
            )
        )[HomeViewModel::class.java]

        setContent {
            WayMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WayMateAppShell(
                        loginViewModel = loginViewModel,
                        homeViewModel = homeViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Minimal application shell hosting the navigation between Login and Home destinations.
 */
@Composable
fun WayMateAppShell(
    loginViewModel: LoginViewModel,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by loginViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            val activeTeamId = homeUiState.teams.firstOrNull()?.id
            LocationSharingService.start(context, activeTeamId)
        } else {
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
            homeViewModel.onLocationPermissionDenied(isPermanentlyDenied = !shouldShowRationale)
        }
    }

    val currentUser = uiState.authenticatedUser

    LaunchedEffect(currentUser?.id) {
        if (currentUser != null) {
            homeViewModel.loadTeams()
            homeViewModel.loadManagementData(currentUser.role)
        }
    }

    if (currentUser != null) {
        HomeScreen(
            user = currentUser,
            onLogoutClicked = {
                LocationSharingService.stop(context)
                homeViewModel.stopLocationSharing()
                loginViewModel.logout()
            },
            homeUiState = homeUiState,
            onRetryTeams = homeViewModel::retry,
            onStatusSelected = homeViewModel::updateOperationalStatus,
            onAcknowledgeMessage = homeViewModel::acknowledgeMessage,
            onSendMessage = homeViewModel::sendTacticalMessage,
            onRefresh = homeViewModel::retry,
            onCreateUser = homeViewModel::createUser,
            onToggleUserStatus = homeViewModel::toggleUserStatus,
            onCreateTeam = { name, leadUserId, vehicleType, vehiclePlate, equipment ->
                homeViewModel.createTeam(
                    name = name,
                    leadUserId = leadUserId,
                    vehicleType = vehicleType,
                    vehiclePlate = vehiclePlate,
                    equipmentSummary = equipment,
                    members = emptyList(),
                    userRole = currentUser.role
                )
            },
            onToggleTeamStatus = { team ->
                homeViewModel.toggleTeamStatus(team, currentUser.role)
            },
            onDismissFeedback = homeViewModel::dismissManagementFeedback,
            onSyncQueue = homeViewModel::syncOfflineQueue,
            onDismissSyncFeedback = homeViewModel::dismissSyncFeedback,
            onSelectPoint = homeViewModel::selectPoint,
            onOpenCreatePointDialog = homeViewModel::openCreatePointDialog,
            onOpenEditPointDialog = homeViewModel::openEditPointDialog,
            onDeletePoint = homeViewModel::deletePoint,
            onConfirmCreatePoint = { title, description, category, lat, lon ->
                homeViewModel.createPoint(
                    title = title,
                    description = description,
                    category = category,
                    latitude = lat,
                    longitude = lon
                )
            },
            onConfirmUpdatePoint = { id, title, description, category, lat, lon, version ->
                homeViewModel.updatePoint(
                    id = id,
                    title = title,
                    description = description,
                    category = category,
                    latitude = lat,
                    longitude = lon,
                    version = version
                )
            },
            onDismissPointDialog = homeViewModel::dismissPointDialog,
            onDismissPointFeedback = homeViewModel::dismissPointFeedback,
            onSelectGroupMember = homeViewModel::selectGroupMember,
            onStartSharing = {
                val hasFine = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    val activeTeamId = homeUiState.teams.firstOrNull()?.id
                    LocationSharingService.start(context, activeTeamId)
                } else {
                    homeViewModel.showLocationRationale()
                }
            },
            onStopSharing = {
                LocationSharingService.stop(context)
                homeViewModel.stopLocationSharing()
            },
            onDismissLocationRationale = homeViewModel::dismissLocationRationale,
            onConfirmLocationRationale = {
                homeViewModel.dismissLocationRationale()
                val permissionsToRequest = buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                permissionLauncher.launch(permissionsToRequest)
            },
            onDismissLocationPermissionDenied = homeViewModel::dismissLocationPermissionDenied,
            onOpenAppSettings = {
                homeViewModel.dismissLocationPermissionDenied()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onRetryPermissionRequest = {
                homeViewModel.dismissLocationPermissionDenied()
                val permissionsToRequest = buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                permissionLauncher.launch(permissionsToRequest)
            },
            modifier = modifier
        )
    } else {
        LoginScreen(
            uiState = uiState,
            onUsernameChanged = loginViewModel::onUsernameChanged,
            onPasswordChanged = loginViewModel::onPasswordChanged,
            onLoginClicked = loginViewModel::login,
            modifier = modifier
        )
    }
}