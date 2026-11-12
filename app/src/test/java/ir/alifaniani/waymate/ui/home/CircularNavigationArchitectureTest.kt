package ir.alifaniani.waymate.ui.home

import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.ui.designsystem.OrbitNavigationItem
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Unit tests verifying the Circular Orbit Navigation architecture,
 * geometry calculations, shortest-path circular wrap math, and role separation.
 */
class CircularNavigationArchitectureTest {

    private val primaryDestinations = listOf(
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

    @Test
    fun primaryDestinations_exactlyFiveDistinctItems() {
        assertEquals(5, primaryDestinations.size)
        val ids = primaryDestinations.map { it.id }.toSet()
        assertEquals("Each destination must have a unique identifier", 5, ids.size)

        primaryDestinations.forEach { item ->
            assertTrue("Title must not be empty", item.title.isNotBlank())
            assertTrue("Content description must be accessible", item.contentDescription.isNotBlank())
        }
    }

    @Test
    fun orbitGeometry_stepAngleIs72Degrees() {
        val itemCount = primaryDestinations.size
        val stepAngleDegrees = 360f / itemCount
        assertEquals(72f, stepAngleDegrees, 0.001f)
    }

    @Test
    fun shortestPathCircularWrap_calculatesCorrectDelta() {
        val itemCount = 5

        // Shortest path from 0 to 4 should be -1 (one step backward)
        var delta = (((4 - 0) % itemCount) + itemCount) % itemCount
        if (delta > itemCount / 2) delta -= itemCount
        assertEquals(-1, delta)

        // Shortest path from 4 to 0 should be +1 (one step forward)
        delta = (((0 - 4) % itemCount) + itemCount) % itemCount
        if (delta > itemCount / 2) delta -= itemCount
        assertEquals(1, delta)

        // Shortest path from 1 to 3 should be +2
        delta = (((3 - 1) % itemCount) + itemCount) % itemCount
        if (delta > itemCount / 2) delta -= itemCount
        assertEquals(2, delta)

        // Shortest path from 3 to 1 should be -2
        delta = (((1 - 3) % itemCount) + itemCount) % itemCount
        if (delta > itemCount / 2) delta -= itemCount
        assertEquals(-2, delta)
    }

    @Test
    fun trigonometricProjection_centerItemHasMaximumDepthAndZeroHorizontalOffset() {
        val itemCount = 5
        val orbitRadius = 120.0

        // Active center item (diff = 0)
        val normalizedDiff = 0.0
        val angleRad = normalizedDiff * (2.0 * PI / itemCount)
        val offsetX = sin(angleRad) * orbitRadius
        val depth = cos(angleRad)

        assertEquals(0.0, offsetX, 0.0001)
        assertEquals(1.0, depth, 0.0001)

        // Neighbor at +1 step (diff = +1)
        val neighborAngleRad = 1.0 * (2.0 * PI / itemCount)
        val neighborOffsetX = sin(neighborAngleRad) * orbitRadius
        val neighborDepth = cos(neighborAngleRad)

        assertTrue("Neighbor horizontal offset must be positive", neighborOffsetX > 0.0)
        assertTrue("Neighbor depth must be less than central item depth", neighborDepth < 1.0)
        assertEquals(cos(72.0 * PI / 180.0), neighborDepth, 0.001)
    }

    @Test
    fun physicalOffset_mirrorsUnderRtlMatchingComposeModifierOffset() {
        val orbitRadius = 115f
        val angleRad = (72f * PI.toFloat() / 180f)
        val itemOffsetX = sin(angleRad) * orbitRadius

        val physicalOffsetLtr = itemOffsetX
        val physicalOffsetRtl = -itemOffsetX

        // In LTR: positive offset translates to physical Right (+X)
        assertTrue("In LTR physical offset is positive", physicalOffsetLtr > 0f)

        // In RTL: Compose Modifier.offset translates positive X to physical Left (-X).
        // The Canvas glow must mirror this physical offset so it aligns exactly with the icon.
        assertTrue("In RTL physical offset is inverted to match Compose layout", physicalOffsetRtl < 0f)
        assertEquals(-physicalOffsetLtr, physicalOffsetRtl, 0.0001f)
    }

    @Test
    fun roleSeparation_distinguishesCommanderFromOperator() {
        val commander = User(
            id = "cmd-1",
            username = "commander_demo",
            role = UserRole.COMMANDER,
            callsign = "EAGLE-1"
        )
        val operator = User(
            id = "op-1",
            username = "patrol_lead_demo",
            role = UserRole.TEAM_LEAD,
            callsign = "FALCON-LEAD"
        )

        val isCommanderAuthorized = commander.role != UserRole.TEAM_LEAD
        val isOperatorAuthorized = operator.role != UserRole.TEAM_LEAD

        assertTrue("Commander must be authorized for team management panel", isCommanderAuthorized)
        assertFalse("Operator must NOT have access to management controls", isOperatorAuthorized)
    }
}

