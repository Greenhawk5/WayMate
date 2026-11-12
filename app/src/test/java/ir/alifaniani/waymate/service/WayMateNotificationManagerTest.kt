package ir.alifaniani.waymate.service

import ir.alifaniani.waymate.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the 15-icon tactical notification mapping and channel configuration.
 */
class WayMateNotificationManagerTest {

    @Test
    fun tacticalNotificationType_hasExactly15Classifications() {
        assertEquals(15, TacticalNotificationType.entries.size)
    }

    @Test
    fun tacticalNotificationType_allEntriesHaveValidResourcesAndChannels() {
        val seenIds = mutableSetOf<Int>()

        TacticalNotificationType.entries.forEach { type ->
            assertTrue("Notification ID must be unique: ${type.notificationId}", seenIds.add(type.notificationId))
            assertTrue("Channel ID must not be blank for ${type.name}", type.channelId.isNotBlank())
            assertNotEquals("Small icon resource must be defined for ${type.name}", 0, type.smallIconRes)
            assertNotEquals("Large icon resource must be defined for ${type.name}", 0, type.largeIconRes)
        }
    }

    @Test
    fun tacticalNotificationType_correctMappingsForSpecificTacticalEvents() {
        assertEquals(R.drawable.ic_stat_notify_team, TacticalNotificationType.TEAM_UPDATE.smallIconRes)
        assertEquals(R.drawable.ic_large_notify_team, TacticalNotificationType.TEAM_UPDATE.largeIconRes)

        assertEquals(R.drawable.ic_stat_notify_command, TacticalNotificationType.COMMAND_ORDER.smallIconRes)
        assertEquals(R.drawable.ic_large_notify_command, TacticalNotificationType.COMMAND_ORDER.largeIconRes)

        assertEquals(R.drawable.ic_stat_notify_location, TacticalNotificationType.LOCATION_SHARING.smallIconRes)
        assertEquals(R.drawable.ic_large_notify_location, TacticalNotificationType.LOCATION_SHARING.largeIconRes)

        assertEquals(R.drawable.ic_stat_notify_sos, TacticalNotificationType.EMERGENCY_SOS.smallIconRes)
        assertEquals(R.drawable.ic_large_notify_sos, TacticalNotificationType.EMERGENCY_SOS.largeIconRes)

        assertEquals(R.drawable.ic_stat_notify_sync, TacticalNotificationType.SYNC_QUEUE.smallIconRes)
        assertEquals(R.drawable.ic_large_notify_sync, TacticalNotificationType.SYNC_QUEUE.largeIconRes)
    }

    @Test
    fun notificationChannels_areProperlyDefined() {
        assertEquals("waymate_location_sharing_channel", WayMateNotificationManager.CHANNEL_LOCATION_SHARING)
        assertEquals("waymate_tactical_orders_channel", WayMateNotificationManager.CHANNEL_TACTICAL_ORDERS)
        assertEquals("waymate_emergency_channel", WayMateNotificationManager.CHANNEL_EMERGENCY)
        assertEquals("waymate_sync_channel", WayMateNotificationManager.CHANNEL_SYNC)
        assertEquals("waymate_squad_channel", WayMateNotificationManager.CHANNEL_SQUAD)
        assertEquals("waymate_navigation_channel", WayMateNotificationManager.CHANNEL_NAVIGATION)
    }
}

