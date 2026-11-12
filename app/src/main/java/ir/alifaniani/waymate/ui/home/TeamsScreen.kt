package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.ui.designsystem.TacticalScreenHeader
import ir.alifaniani.waymate.ui.designsystem.TacticalStatusPill
import ir.alifaniani.waymate.ui.designsystem.TacticalTone
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark

/**
 * Dedicated Destination C: Teams and Operational Units.
 *
 * Consolidates team and member coordination:
 * - Role-tailored view:
 *   - Commanders & Owners receive full management (create teams, users, member allocation, operational stats).
 *   - Field Operators & Team Leads receive clean, secure read-only group and member rosters.
 * - Enforces zero privilege leakage while preserving all existing management workflows.
 */
@Composable
fun TeamsScreen(
    user: User,
    homeUiState: HomeUiState,
    onRetryTeams: () -> Unit,
    onCreateUser: (username: String, password: String, role: UserRole, callsign: String) -> Unit,
    onToggleUserStatus: (ManagedUser) -> Unit,
    onCreateTeam: (name: String, leadUserId: String, vehicleType: String, vehiclePlate: String, equipment: String) -> Unit,
    onToggleTeamStatus: (Team) -> Unit,
    onAddTeamMember: (teamId: String, fullName: String, callsign: String, roleInTeam: String) -> Unit = { _, _, _, _ -> },
    onRemoveTeamMember: (teamId: String, memberId: String) -> Unit = { _, _ -> },
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCommander = user.role != UserRole.TEAM_LEAD

    Surface(
        modifier = modifier.fillMaxSize(),
        color = WayMateMidnightDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Screen Header
            TacticalScreenHeader(
                title = "تیم‌ها و گروه‌های عملیاتی",
                subtitle = "واحدهای گشت و همنوردان مستقر در حوزه ماموریت",
                trailingContent = {
                    TacticalStatusPill(
                        text = if (isCommander) "مدیریت فرماندهی" else "مشاهده اعضا",
                        tone = if (isCommander) TacticalTone.INFO else TacticalTone.NEUTRAL
                    )
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // If Commander or Owner: Show the comprehensive management controls
            if (isCommander) {
                TacticalManagementSection(
                    currentUser = user,
                    uiState = homeUiState,
                    onCreateUser = onCreateUser,
                    onToggleUserStatus = onToggleUserStatus,
                    onCreateTeam = onCreateTeam,
                    onToggleTeamStatus = onToggleTeamStatus,
                    onDismissFeedback = onDismissFeedback,
                    onAddMember = onAddTeamMember,
                    onRemoveMember = onRemoveTeamMember
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Group & Team Roster
            TacticalUserGroupSection(
                uiState = homeUiState,
                onRetry = onRetryTeams
            )

            // Safe bottom margin ensuring complete scroll clearance above circular navigation bar
            Spacer(modifier = Modifier.height(148.dp).navigationBarsPadding())
        }
    }
}

