package ru.mobile.startapp.presentation.list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.mobile.startapp.data.EmulatedRatingRepository
import ru.mobile.startapp.domain.usecase.LoadRatingsUseCase
import ru.mobile.startapp.testing.FakeAppRepository
import ru.mobile.startapp.testing.FakeRatingApi
import ru.mobile.startapp.testing.TestData

/**
 * ViewModel списка: состояния загрузки, замер рейтингов и его перезапуск.
 *
 * Задержки «сети» — виртуальное время тестового планировщика, поэтому тест
 * не ждёт реальные 15 секунд последовательного замера.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun useCase(api: FakeRatingApi) = LoadRatingsUseCase(EmulatedRatingRepository(api))

    /** `stateIn(WhileSubscribed)` не запускается без подписчика — подписываемся. */
    private fun TestScope.subscribe(viewModel: AppListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    private fun AppListViewModel.content(): AppListUiState.Content =
        uiState.value as AppListUiState.Content

    @Test
    fun uiState_isLoadingUntilCatalogAppears() = runTest(dispatcher) {
        val repository = FakeAppRepository(initial = emptyList())
        val viewModel = AppListViewModel(repository, useCase(FakeRatingApi(delayMs = 10)))
        subscribe(viewModel)
        advanceUntilIdle()

        assertEquals(AppListUiState.Loading, viewModel.uiState.value)

        repository.emit(TestData.catalog)
        advanceUntilIdle()

        assertEquals(TestData.catalog, viewModel.content().apps)
    }

    @Test
    fun benchmark_fillsRatingsForWholeCatalog() = runTest(dispatcher) {
        val api = FakeRatingApi(delayMs = 100, ratings = { 4.2f })
        val viewModel = AppListViewModel(FakeAppRepository(TestData.catalog), useCase(api))
        subscribe(viewModel)
        advanceUntilIdle()

        val content = viewModel.content()

        assertEquals(TestData.catalog.map { it.id }.toSet(), content.ratings.keys)
        assertTrue(content.ratings.values.all { it == 4.2f })
        assertNotNull(content.sequential)
        assertNotNull(content.parallel)
        assertNull(content.runningMode)
    }

    @Test
    fun benchmark_measuresOnlyFirstTenApps_andLoadsRestForUi() = runTest(dispatcher) {
        val catalog = TestData.catalog(size = 15)
        val viewModel = AppListViewModel(FakeAppRepository(catalog), useCase(FakeRatingApi(delayMs = 10)))
        subscribe(viewModel)
        advanceUntilIdle()

        val content = viewModel.content()

        assertEquals(AppListViewModel.RATINGS_SAMPLE_SIZE, content.sequential!!.requests)
        assertEquals(AppListViewModel.RATINGS_SAMPLE_SIZE, content.parallel!!.requests)
        // остальные приложения каталога тоже получают рейтинг — для карточек
        assertEquals(catalog.map { it.id }.toSet(), content.ratings.keys)
    }

    @Test
    fun runningMode_showsSequentialThenParallel() = runTest(dispatcher) {
        val delayMs = 100L
        val repository = FakeAppRepository(TestData.catalog.take(3))
        val viewModel = AppListViewModel(repository, useCase(FakeRatingApi(delayMs = delayMs)))
        subscribe(viewModel)

        runCurrent() // каталог получен, замер запущен, первый запрос ждёт «сеть»
        assertEquals(RatingLoadMode.Sequential, viewModel.content().runningMode)
        assertNull(viewModel.content().sequential)

        advanceTimeBy(delayMs) // один из трёх запросов завершился
        runCurrent()
        assertEquals(RatingLoadMode.Sequential, viewModel.content().runningMode)

        advanceTimeBy(delayMs * 2) // последовательный замер закончен, идёт параллельный
        runCurrent()
        assertEquals(RatingLoadMode.Parallel, viewModel.content().runningMode)
        assertNotNull(viewModel.content().sequential)

        advanceTimeBy(delayMs)
        runCurrent()
        val content = viewModel.content()
        assertNull(content.runningMode)
        assertNotNull(content.parallel)
    }

    @Test
    fun onRerunRatings_repeatsBothBenchmarks() = runTest(dispatcher) {
        val catalog = TestData.catalog(size = 15)
        val api = FakeRatingApi(delayMs = 10)
        val viewModel = AppListViewModel(FakeAppRepository(catalog), useCase(api))
        subscribe(viewModel)
        advanceUntilIdle()

        val callsAfterFirstRun = api.requestedIds.size
        val sequentialBefore = viewModel.content().sequential

        viewModel.onRerunRatings()
        advanceUntilIdle()

        val content = viewModel.content()
        // оба замера повторяются: выборка + догрузка остальных карточек
        assertEquals(callsAfterFirstRun * 2, api.requestedIds.size)
        assertEquals(AppListViewModel.RATINGS_SAMPLE_SIZE, content.sequential!!.requests)
        assertNotNull(content.parallel)
        assertNull(content.runningMode)
        // новые объекты замера, а не результат прошлого прогона (data class == по полям)
        assertNotSame(sequentialBefore, content.sequential)
    }

    @Test
    fun onRerunRatings_cancelsPreviousBenchmark() = runTest(dispatcher) {
        val repository = FakeAppRepository(TestData.catalog)
        val viewModel = AppListViewModel(repository, useCase(FakeRatingApi(delayMs = 1_000)))
        subscribe(viewModel)

        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        viewModel.onRerunRatings()
        advanceUntilIdle()

        val content = viewModel.content()
        assertNull("после перезапуска индикатор загрузки должен погаснуть", content.runningMode)
        assertNotNull(content.parallel)
    }

    @Test
    fun onRerunRatings_beforeCatalog_doesNothing() = runTest(dispatcher) {
        val api = FakeRatingApi(delayMs = 10)
        val viewModel = AppListViewModel(FakeAppRepository(initial = emptyList()), useCase(api))
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onRerunRatings()
        advanceUntilIdle()

        assertEquals(AppListUiState.Loading, viewModel.uiState.value)
        assertTrue(api.requestedIds.isEmpty())
    }

    @Test
    fun onFavoriteClick_updatesAppInList() = runTest(dispatcher) {
        val viewModel = AppListViewModel(FakeAppRepository(TestData.catalog), useCase(FakeRatingApi()))
        subscribe(viewModel)
        advanceUntilIdle()

        val browser = viewModel.content().apps.first { it.id == "browser" }
        viewModel.onFavoriteClick(browser)
        advanceUntilIdle()

        assertTrue(viewModel.content().apps.first { it.id == "browser" }.isFavorite)
        assertTrue(viewModel.content().apps.none { it.id == "mail" && it.isFavorite })
    }

    @Test
    fun onToggleFavorites_showsOnlyFavoriteApps() = runTest(dispatcher) {
        val viewModel = AppListViewModel(FakeAppRepository(TestData.catalog), useCase(FakeRatingApi()))
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onFavoriteClick(viewModel.content().apps.first { it.id == "music" })
        advanceUntilIdle()

        viewModel.onToggleFavorites()
        advanceUntilIdle()

        val filtered = viewModel.content()
        assertTrue(filtered.favoritesOnly)
        assertEquals(listOf("music"), filtered.apps.map { it.id })

        viewModel.onToggleFavorites()
        advanceUntilIdle()

        assertFalse(viewModel.content().favoritesOnly)
        assertEquals(TestData.catalog.size, viewModel.content().apps.size)
    }

    @Test
    fun favoritesFilter_withNoFavorites_givesEmptyList() = runTest(dispatcher) {
        val viewModel = AppListViewModel(FakeAppRepository(TestData.catalog), useCase(FakeRatingApi()))
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onToggleFavorites()
        advanceUntilIdle()

        val content = viewModel.content()
        assertTrue(content.favoritesOnly)
        assertTrue(content.apps.isEmpty())
    }
}
