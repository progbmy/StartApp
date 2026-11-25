package ru.mobile.startapp.presentation.list

import android.content.Context
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.mobile.startapp.R
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.usecase.RatingsLoadResult
import ru.mobile.startapp.testing.UiTestApps
import ru.mobile.startapp.ui.theme.StartAppTheme

/**
 * UI-тесты экрана списка на настоящем Compose-хосте: состояния, карточки,
 * блок замера рейтингов и клики.
 *
 * Экран stateless, поэтому ViewModel и сеть не нужны — состояние передаётся
 * прямо в [AppListScreen], а тест проверяет, что именно видит пользователь.
 */
@RunWith(AndroidJUnit4::class)
class AppListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setScreen(
        uiState: AppListUiState,
        onAppClick: (App) -> Unit = {},
        onFavoriteClick: (App) -> Unit = {},
        onToggleFavorites: () -> Unit = {},
        onRerunRatings: () -> Unit = {}
    ) {
        composeRule.setContent {
            StartAppTheme {
                AppListScreen(
                    uiState = uiState,
                    onAppClick = onAppClick,
                    onFavoriteClick = onFavoriteClick,
                    onToggleFavorites = onToggleFavorites,
                    onRerunRatings = onRerunRatings
                )
            }
        }
    }

    private fun content(
        apps: List<App> = UiTestApps.catalog,
        ratings: Map<String, Float> = emptyMap(),
        sequential: RatingsLoadResult? = null,
        parallel: RatingsLoadResult? = null,
        runningMode: RatingLoadMode? = null,
        favoritesOnly: Boolean = false
    ) = AppListUiState.Content(
        apps = apps,
        ratings = ratings,
        sequential = sequential,
        parallel = parallel,
        runningMode = runningMode,
        favoritesOnly = favoritesOnly
    )

    private fun seconds(value: Float): String =
        String.format(Locale.getDefault(), "%.1f с", value)

    private fun benchmarkRow(resultSeconds: Float, requests: Int): String =
        seconds(resultSeconds) + " · " + context.getString(R.string.list_ratings_requests, requests)

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun loading_showsProgress_andNoAppCards() {
        // CircularProgressIndicator — бесконечная анимация: без остановки часов
        // Compose-тест не дождётся состояния idle.
        composeRule.mainClock.autoAdvance = false
        setScreen(AppListUiState.Loading)

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertExists()
        composeRule.onAllNodesWithText(UiTestApps.browser.name).assertCountEquals(0)
        composeRule.onNodeWithText(context.getString(R.string.tool_bar_title)).assertExists()
    }

    @Test
    fun content_showsToolbarAndBenchmarkTitle() {
        setScreen(content())

        composeRule.onNodeWithText(context.getString(R.string.tool_bar_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.list_ratings_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.list_ratings_hint)).assertIsDisplayed()
    }

    @Test
    fun content_listsEveryAppAfterScrolling() {
        setScreen(content())

        UiTestApps.catalog.forEach { app ->
            scrollTo(app.name)
            composeRule.onNodeWithText(app.name).assertExists()
        }
    }

    @Test
    fun clickingAppCard_reportsSelectedApp() {
        var clicked: App? = null
        setScreen(content(), onAppClick = { clicked = it })

        composeRule.onNodeWithText(UiTestApps.browser.name).performClick()

        assertEquals("browser", clicked?.id)
    }

    @Test
    fun rating_isShownNextToAppName_whenRatingLoaded() {
        setScreen(content(ratings = mapOf("browser" to 4.5f)))

        composeRule
            .onAllNodesWithContentDescription(context.getString(R.string.list_rating_content_desc))
            .assertCountEquals(1)
        composeRule.onNodeWithText(String.format(Locale.getDefault(), "%.1f", 4.5f)).assertExists()
    }

    @Test
    fun rating_isHidden_whenNotLoadedYet() {
        setScreen(content())

        composeRule
            .onAllNodesWithContentDescription(context.getString(R.string.list_rating_content_desc))
            .assertCountEquals(0)
    }

    @Test
    fun benchmark_showsBothTimesAndSpeedup() {
        setScreen(
            content(
                sequential = RatingsLoadResult(emptyMap(), elapsedMs = 15_100, requests = 10),
                parallel = RatingsLoadResult(emptyMap(), elapsedMs = 1_600, requests = 10)
            )
        )

        composeRule.onNodeWithText(benchmarkRow(15.1f, 10)).assertIsDisplayed()
        composeRule.onNodeWithText(benchmarkRow(1.6f, 10)).assertIsDisplayed()
        composeRule.onNode(hasText("быстрее", substring = true)).assertExists()
    }

    @Test
    fun benchmark_showsDashesBeforeFirstMeasurement() {
        setScreen(content())

        composeRule.onAllNodesWithText("—").assertCountEquals(2)
    }

    @Test
    fun benchmark_showsRunningLabelForActiveMode() {
        setScreen(content(runningMode = RatingLoadMode.Sequential))

        composeRule
            .onAllNodesWithText(context.getString(R.string.list_ratings_running))
            .assertCountEquals(1)
        composeRule.onAllNodesWithText("—").assertCountEquals(1)
    }

    @Test
    fun retryButton_reportsClick() {
        var retried = 0
        setScreen(content(), onRerunRatings = { retried++ })

        composeRule.onNodeWithText(context.getString(R.string.list_ratings_retry)).performClick()

        assertTrue("кнопка «Замерить заново» должна вызывать колбэк", retried == 1)
    }

    @Test
    fun favoriteIcon_onCard_reportsClickedApp() {
        var clicked: App? = null
        setScreen(
            content(apps = listOf(UiTestApps.browser)),
            onFavoriteClick = { clicked = it }
        )

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.favorite_add_content_desc))
            .performClick()

        assertEquals("browser", clicked?.id)
    }

    @Test
    fun favoriteIcon_isFilled_forFavoriteApp() {
        setScreen(content(apps = listOf(UiTestApps.browser.copy(isFavorite = true))))

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.favorite_remove_content_desc))
            .assertExists()
    }

    @Test
    fun toolbarFilter_togglesAndShowsEmptyFavoritesState() {
        var toggles = 0
        setScreen(content(apps = emptyList(), favoritesOnly = true), onToggleFavorites = { toggles++ })

        composeRule.onNodeWithText(context.getString(R.string.favorite_empty)).assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.favorite_filter_hide))
            .performClick()

        assertEquals(1, toggles)
    }

    @Test
    fun toolbarFilter_showsShowFilterDescription_whenDisabled() {
        setScreen(content())

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.favorite_filter_show))
            .assertExists()
    }
}
