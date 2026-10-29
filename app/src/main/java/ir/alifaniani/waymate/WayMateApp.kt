package ir.alifaniani.waymate

import android.app.Application
import ir.alifaniani.waymate.di.AppContainer
import ir.alifaniani.waymate.di.DefaultAppContainer

/**
 * Application class for WayMate.
 * Hosts the manual [AppContainer] dependency graph.
 */
class WayMateApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
