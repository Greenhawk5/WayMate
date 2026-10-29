package ir.alifaniani.waymate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
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
                offlineSyncManager = app.container.offlineSyncManager
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
            onLogoutClicked = loginViewModel::logout,
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