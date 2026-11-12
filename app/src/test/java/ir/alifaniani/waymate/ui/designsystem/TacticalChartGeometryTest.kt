package ir.alifaniani.waymate.ui.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TacticalChartGeometryTest {

    private val elevationLabels = listOf(
        "پایگاه",
        "دره نخل",
        "چشمه",
        "جان‌پناه",
        "خط‌الراس",
        "قله البرز"
    )

    @Test
    fun rtlMapping_ordersLocationsRightToLeftAndMapsEveryTapToExactDisplayedLabel() {
        val count = elevationLabels.size
        val width = 1000f
        val padH = 50f

        // In RTL, index 0 ("پایگاه") is at the rightmost X coordinate,
        // and index 5 ("قله البرز") is at the leftmost X coordinate.
        val leftmostX = TacticalChartGeometry.pointX(
            index = 5,
            count = count,
            width = width,
            horizontalPadding = padH,
            isRtl = true
        )
        val rightmostX = TacticalChartGeometry.pointX(
            index = 0,
            count = count,
            width = width,
            horizontalPadding = padH,
            isRtl = true
        )

        assertEquals(padH, leftmostX, 0.01f)
        assertEquals(width - padH, rightmostX, 0.01f)

        // Verify tapping the leftmost point selects "قله البرز" (index 5)
        val leftmostTapIndex = TacticalChartGeometry.nearestIndex(
            tapX = leftmostX,
            count = count,
            width = width,
            horizontalPadding = padH,
            isRtl = true
        )
        assertEquals(5, leftmostTapIndex)
        assertEquals("قله البرز", elevationLabels[leftmostTapIndex])

        // Verify tapping the rightmost point selects "پایگاه" (index 0)
        val rightmostTapIndex = TacticalChartGeometry.nearestIndex(
            tapX = rightmostX,
            count = count,
            width = width,
            horizontalPadding = padH,
            isRtl = true
        )
        assertEquals(0, rightmostTapIndex)
        assertEquals("پایگاه", elevationLabels[rightmostTapIndex])

        // Verify all 6 points individually map to their exact location index when tapped
        for (index in 0 until count) {
            val visualX = TacticalChartGeometry.pointX(
                index = index,
                count = count,
                width = width,
                horizontalPadding = padH,
                isRtl = true
            )
            val resolvedIndex = TacticalChartGeometry.nearestIndex(
                tapX = visualX,
                count = count,
                width = width,
                horizontalPadding = padH,
                isRtl = true
            )
            assertEquals("Point at index $index must resolve to itself", index, resolvedIndex)

            // Also verify slight horizontal jitter (+/- 15px) still resolves to the same point
            val jitterLeft = TacticalChartGeometry.nearestIndex(
                tapX = visualX - 15f,
                count = count,
                width = width,
                horizontalPadding = padH,
                isRtl = true
            )
            val jitterRight = TacticalChartGeometry.nearestIndex(
                tapX = visualX + 15f,
                count = count,
                width = width,
                horizontalPadding = padH,
                isRtl = true
            )
            assertEquals(index, jitterLeft)
            assertEquals(index, jitterRight)
        }

        // Verify monotonic visual progression from left ("قله البرز", index 5) to right ("پایگاه", index 0)
        val visualOrderLeftToRight = (count - 1 downTo 0).map { idx ->
            TacticalChartGeometry.pointX(idx, count, width, padH, isRtl = true)
        }
        for (i in 0 until visualOrderLeftToRight.size - 1) {
            assertTrue(visualOrderLeftToRight[i] < visualOrderLeftToRight[i + 1])
        }
    }

    @Test
    fun ltrMapping_ordersLocationsLeftToRightAndMapsEveryTapToExactIndex() {
        val count = elevationLabels.size
        val width = 1000f
        val padH = 50f

        for (index in 0 until count) {
            val visualX = TacticalChartGeometry.pointX(
                index = index,
                count = count,
                width = width,
                horizontalPadding = padH,
                isRtl = false
            )
            val resolvedIndex = TacticalChartGeometry.nearestIndex(
                tapX = visualX,
                count = count,
                width = width,
                horizontalPadding = padH,
                isRtl = false
            )
            assertEquals(index, resolvedIndex)
        }
    }
}

