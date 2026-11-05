package ir.alifaniani.waymate.ui.home

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.data.repository.FakePointOfInterestRepository
import ir.alifaniani.waymate.data.repository.FakeTeamRepository
import ir.alifaniani.waymate.domain.model.PointCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelPointOfInterestTest {

    private lateinit var fakeTeamRepository: FakeTeamRepository
    private lateinit var fakePointRepository: FakePointOfInterestRepository
    private lateinit var testDispatcherProvider: TestDispatcherProvider
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        fakeTeamRepository = FakeTeamRepository(simulatedDelayMs = 0L)
        fakePointRepository = FakePointOfInterestRepository()
        testDispatcherProvider = TestDispatcherProvider()

        viewModel = HomeViewModel(
            teamRepository = fakeTeamRepository,
            pointRepository = fakePointRepository,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @org.junit.After
    fun tearDown() {
        viewModel.stopPolling()
    }

    @Test
    fun init_loadsPointsAutomatically() {
        val state = viewModel.uiState.value
        assertEquals(3, state.points.size)
        assertTrue(state.points.any { it.title.contains("سنگ‌شکنان") })
    }

    @Test
    fun selectPoint_updatesSelectedPointInUiState() {
        val point = viewModel.uiState.value.points.first()
        viewModel.selectPoint(point)
        assertEquals(point.id, viewModel.uiState.value.selectedPoint?.id)

        viewModel.selectPoint(null)
        assertNull(viewModel.uiState.value.selectedPoint)
    }

    @Test
    fun dialogVisibility_openAndDismissWorkCorrectly() {
        assertFalse(viewModel.uiState.value.isPointDialogVisible)
        assertNull(viewModel.uiState.value.editingPoint)

        viewModel.openCreatePointDialog()
        assertTrue(viewModel.uiState.value.isPointDialogVisible)
        assertNull(viewModel.uiState.value.editingPoint)

        val point = viewModel.uiState.value.points.first()
        viewModel.openEditPointDialog(point)
        assertTrue(viewModel.uiState.value.isPointDialogVisible)
        assertEquals(point.id, viewModel.uiState.value.editingPoint?.id)

        viewModel.dismissPointDialog()
        assertFalse(viewModel.uiState.value.isPointDialogVisible)
        assertNull(viewModel.uiState.value.editingPoint)
    }

    @Test
    fun createPoint_addsPointAndUpdatesState() = runBlocking {
        viewModel.createPoint(
            title = "چشمه مانیان",
            description = "منبع آب طبیعی",
            category = PointCategory.WATER_SOURCE,
            latitude = 28.5250,
            longitude = 53.5900
        )

        val state = viewModel.uiState.value
        assertEquals(4, state.points.size)
        val created = state.points.find { it.title == "چشمه مانیان" }
        assertNotNull(created)
        assertEquals(PointCategory.WATER_SOURCE, created?.category)
        assertNotNull(state.pointFeedbackMessage)
    }

    @Test
    fun updatePoint_modifiesPointAndUpdatesState() = runBlocking {
        val initialPoint = viewModel.uiState.value.points.first()
        viewModel.updatePoint(
            id = initialPoint.id,
            title = "عنوان جدید نقطه",
            description = "توضیحات به‌روزرسانی‌شده",
            category = PointCategory.REST_AREA,
            latitude = 28.5400,
            longitude = 53.6100,
            version = initialPoint.version
        )

        val state = viewModel.uiState.value
        val updated = state.points.find { it.id == initialPoint.id }
        assertNotNull(updated)
        assertEquals("عنوان جدید نقطه", updated?.title)
        assertEquals(PointCategory.REST_AREA, updated?.category)
        assertEquals(2, updated?.version)
    }

    @Test
    fun deletePoint_removesPointAndResetsSelection() = runBlocking {
        val pointToDelete = viewModel.uiState.value.points.first()
        viewModel.selectPoint(pointToDelete)
        assertEquals(pointToDelete.id, viewModel.uiState.value.selectedPoint?.id)

        viewModel.deletePoint(pointToDelete.id)

        val state = viewModel.uiState.value
        assertEquals(2, state.points.size)
        assertNull(state.points.find { it.id == pointToDelete.id })
        assertNull(state.selectedPoint)
    }
}
