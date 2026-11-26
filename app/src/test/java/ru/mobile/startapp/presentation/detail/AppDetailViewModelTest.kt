package ru.mobile.startapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.mobile.startapp.domain.repository.AppRepository
import ru.mobile.startapp.testing.FakeAppRepository

/**
 * ViewModel карточки приложения: поиск приложения по id, установка с прогрессом
 * и отмена установки (ДЗ про корутины).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        appId: String? = "browser",
        repository: FakeAppRepository = FakeAppRepository()
    ): AppDetailViewModel {
        val savedState = SavedStateHandle(
            appId?.let { mapOf(AppDetailViewModel.ARG_APP_ID to it) } ?: emptyMap()
        )
        return AppDetailViewModel(repository, savedState)
    }

    private fun TestScope.subscribe(viewModel: AppDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    private fun TestScope.collectEvents(viewModel: AppDetailViewModel): List<AppDetailEvent> {
        val events = mutableListOf<AppDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        return events
    }

    private fun AppDetailViewModel.content(): AppDetailUiState.Content =
        uiState.value as AppDetailUiState.Content

    @Test
    fun uiState_contentForKnownApp() = runTest(dispatcher) {
        val viewModel = viewModel(appId = "browser")
        subscribe(viewModel)
        advanceUntilIdle()

        val content = viewModel.content()

        assertEquals("browser", content.app.id)
        assertEquals(InstallState.NotInstalled, content.installState)
        assertEquals(AppRepository.SIMILAR_APPS_LIMIT, content.similarApps.size)
        assertTrue(content.similarApps.none { it.id == "browser" })
    }

    @Test
    fun uiState_notFoundForUnknownId() = runTest(dispatcher) {
        val viewModel = viewModel(appId = "unknown")
        subscribe(viewModel)
        advanceUntilIdle()

        assertEquals(AppDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun uiState_notFoundWithoutAppId() = runTest(dispatcher) {
        val viewModel = viewModel(appId = null)
        subscribe(viewModel)
        advanceUntilIdle()

        assertEquals(AppDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun install_runsToCompletion_andEmitsEvents() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.onInstallClick()
        advanceUntilIdle()

        assertEquals(InstallState.Installed, viewModel.content().installState)
        assertEquals(
            listOf(AppDetailEvent.InstallStarted, AppDetailEvent.InstallFinished),
            events
        )
    }

    @Test
    fun install_progressesGradually() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onInstallClick()
        advanceTimeBy(AppDetailViewModel.PROGRESS_DELAY_MS * 5)
        runCurrent()

        val installing = viewModel.content().installState
        assertTrue("ожидали Installing, получили $installing", installing is InstallState.Installing)
        val progress = (installing as InstallState.Installing).progress
        assertTrue("прогресс $progress должен быть больше нуля и меньше 100", progress in 1..99)
    }

    @Test
    fun cancel_returnsToNotInstalled_andEmitsCancelled() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.onInstallClick()
        advanceTimeBy(AppDetailViewModel.PROGRESS_DELAY_MS * 2)
        runCurrent()
        assertTrue(viewModel.content().installState is InstallState.Installing)

        viewModel.onInstallClick() // «Отменить»
        // состояние доезжает до uiState через combine — прокручиваем очередь
        advanceUntilIdle()

        assertEquals(InstallState.NotInstalled, viewModel.content().installState)

        advanceUntilIdle() // время идёт дальше, но корутина установки уже отменена
        assertEquals(InstallState.NotInstalled, viewModel.content().installState)
        assertEquals(
            listOf(AppDetailEvent.InstallStarted, AppDetailEvent.InstallCancelled),
            events
        )
    }

    @Test
    fun afterCancel_installCanBeStartedAgain() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onInstallClick()
        advanceTimeBy(AppDetailViewModel.PROGRESS_DELAY_MS * 2)
        runCurrent()
        viewModel.onInstallClick() // отмена
        advanceUntilIdle()
        assertEquals(InstallState.NotInstalled, viewModel.content().installState)

        viewModel.onInstallClick() // новая установка после отмены
        advanceUntilIdle()

        assertEquals(InstallState.Installed, viewModel.content().installState)
    }

    @Test
    fun installProgress_reachesHundredOnLastStep() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onInstallClick()
        // 20-я итерация цикла: 20 шагов по PROGRESS_DELAY_MS дают 95%
        advanceTimeBy(AppDetailViewModel.PROGRESS_DELAY_MS * 20)
        runCurrent()

        assertEquals(InstallState.Installing(95), viewModel.content().installState)
    }

    @Test
    fun install_doesNotAffectSimilarApps() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()
        val similarBefore = viewModel.content().similarApps

        viewModel.onInstallClick()
        advanceUntilIdle()

        assertEquals(similarBefore, viewModel.content().similarApps)
        assertEquals("browser", viewModel.content().app.id)
    }

    @Test
    fun install_switchesToInstallingImmediately() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onInstallClick()
        runCurrent()

        assertEquals(InstallState.Installing(0), viewModel.content().installState)
    }

    @Test
    fun doubleTap_secondTapCancelsInsteadOfStartingSecondInstall() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.onInstallClick() // установка
        viewModel.onInstallClick() // второй клик ещё до диспатча корутины — «Отменить»

        advanceUntilIdle()

        assertEquals(InstallState.NotInstalled, viewModel.content().installState)
        assertEquals(
            listOf(
                AppDetailEvent.InstallStarted,
                AppDetailEvent.InstallCancelled
            ),
            events
        )
    }

    /**
     * Регрессия: `finally` отменённой установки не должен обнулять ссылку
     * на уже запущенную следующую — иначе «Отменить» для неё перестаёт работать
     * и отменённая установка «доезжает» до [InstallState.Installed].
     */
    @Test
    fun cancelThenRestart_secondCancelStillCancelsInstall() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onInstallClick() // установка №1
        advanceTimeBy(AppDetailViewModel.PROGRESS_DELAY_MS)
        runCurrent()

        viewModel.onInstallClick() // отмена №1: её finally ещё в очереди
        viewModel.onInstallClick() // установка №2 запускается сразу же
        runCurrent() // здесь отменённая №1 доезжает до finally

        viewModel.onInstallClick() // отмена №2 — должна отменить установку №2
        advanceUntilIdle()

        assertEquals(InstallState.NotInstalled, viewModel.content().installState)
    }

    @Test
    fun afterCancelledRestart_installCompletesWhenNotCancelled() = runTest(dispatcher) {
        val viewModel = viewModel()
        subscribe(viewModel)
        advanceUntilIdle()

        viewModel.onInstallClick()
        advanceTimeBy(AppDetailViewModel.PROGRESS_DELAY_MS)
        runCurrent()
        viewModel.onInstallClick() // отмена
        viewModel.onInstallClick() // новая установка
        advanceUntilIdle()

        assertEquals(InstallState.Installed, viewModel.content().installState)
    }
}
