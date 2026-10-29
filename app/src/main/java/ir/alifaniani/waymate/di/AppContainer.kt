package ir.alifaniani.waymate.di

import android.content.Context
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.local.sqlite.SqliteOfflineStorage
import ir.alifaniani.waymate.data.local.sqlite.WayMateDatabaseHelper
import ir.alifaniani.waymate.data.network.NetworkClient
import ir.alifaniani.waymate.data.repository.RealAdminRepository
import ir.alifaniani.waymate.data.repository.RealAuthRepository
import ir.alifaniani.waymate.data.repository.RealMessageRepository
import ir.alifaniani.waymate.data.repository.RealStatusRepository
import ir.alifaniani.waymate.data.repository.RealTeamRepository
import ir.alifaniani.waymate.data.session.KeystoreSessionStorage
import ir.alifaniani.waymate.data.session.SessionStorage
import ir.alifaniani.waymate.data.sync.OfflineSyncManager
import ir.alifaniani.waymate.domain.repository.AdminRepository
import ir.alifaniani.waymate.domain.repository.AuthRepository
import ir.alifaniani.waymate.domain.repository.MessageRepository
import ir.alifaniani.waymate.domain.repository.StatusRepository
import ir.alifaniani.waymate.domain.repository.TeamRepository

/**
 * Manual Dependency Injection container contract for WayMate.
 *
 * Provides explicit, testable dependencies with zero code-generation,
 * zero reflection, and zero runtime magic.
 */
interface AppContainer {
    val sessionStorage: SessionStorage
    val offlineStorage: OfflineStorage
    val offlineSyncManager: OfflineSyncManager
    val networkClient: NetworkClient
    val authRepository: AuthRepository
    val teamRepository: TeamRepository
    val messageRepository: MessageRepository
    val statusRepository: StatusRepository
    val adminRepository: AdminRepository
}

/**
 * Default container used in standard application runtime.
 * Backed by Android Keystore session persistence, SQLite offline cache,
 * and Cloudflare Workers HTTP client.
 */
class DefaultAppContainer(
    private val context: Context
) : AppContainer {

    override val sessionStorage: SessionStorage by lazy {
        KeystoreSessionStorage(context.applicationContext)
    }

    override val offlineStorage: OfflineStorage by lazy {
        val dbHelper = WayMateDatabaseHelper(context.applicationContext)
        SqliteOfflineStorage(dbHelper)
    }

    override val networkClient: NetworkClient by lazy {
        NetworkClient(sessionStorage)
    }

    override val offlineSyncManager: OfflineSyncManager by lazy {
        OfflineSyncManager(
            offlineStorage = offlineStorage,
            messageApiService = networkClient.createMessageApiService(),
            statusApiService = networkClient.createStatusApiService(),
            teamApiService = networkClient.createTeamApiService()
        )
    }

    override val authRepository: AuthRepository by lazy {
        RealAuthRepository(
            authApiService = networkClient.createAuthApiService(),
            sessionStorage = sessionStorage,
            offlineStorage = offlineStorage
        )
    }

    override val teamRepository: TeamRepository by lazy {
        RealTeamRepository(
            teamApiService = networkClient.createTeamApiService(),
            offlineStorage = offlineStorage
        )
    }

    override val messageRepository: MessageRepository by lazy {
        RealMessageRepository(
            messageApiService = networkClient.createMessageApiService(),
            offlineStorage = offlineStorage
        )
    }

    override val statusRepository: StatusRepository by lazy {
        RealStatusRepository(
            statusApiService = networkClient.createStatusApiService(),
            offlineStorage = offlineStorage
        )
    }

    override val adminRepository: AdminRepository by lazy {
        RealAdminRepository(
            userApiService = networkClient.createUserApiService(),
            adminApiService = networkClient.createAdminApiService()
        )
    }
}
