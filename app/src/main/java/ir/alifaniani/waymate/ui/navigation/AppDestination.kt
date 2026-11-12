package ir.alifaniani.waymate.ui.navigation

import ir.alifaniani.waymate.domain.model.User

/**
 * Type-safe navigation destinations for the minimal application flow.
 */
sealed interface AppDestination {
    data object Onboarding : AppDestination
    data object Login : AppDestination
    data class Home(val user: User) : AppDestination
}
