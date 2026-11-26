package ru.mobile.startapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.mobile.startapp.di.AppModule
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import ru.mobile.startapp.domain.repository.RatingRepository
import ru.mobile.startapp.testing.InstantRatingRepository
import ru.mobile.startapp.testing.UiTestAppRepository
import ru.mobile.startapp.testing.UiTestApps

/**
 * Сквозной сценарий на настоящем приложении: MainActivity → Hilt-граф → NavHost →
 * ViewModel → экраны.
 *
 * Продовый `AppModule` (сеть) подменён тестовым модулем с каталогом в памяти
 * и мгновенными рейтингами, поэтому сценарий детерминированный: список → карточка →
 * похожее приложение → назад, установка и её отмена.
 */
@UninstallModules(AppModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun injectTestBindings() {
        hiltRule.inject()
    }

    private fun text(resId: Int): String = composeRule.activity.getString(resId)

    private fun hasNode(text: String): Boolean =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun awaitNode(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) { hasNode(text) }
    }

    /** Прокручивает список до карточки приложения и открывает её. */
    private fun openCard(app: App) {
        awaitNode(app.name)
        composeRule
            .onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText(app.name))
        composeRule.onNodeWithText(app.name).performClick()
        awaitNode(text(R.string.app_details_about))
    }

    @Test
    fun listScreen_showsCatalogFromInjectedRepository() {
        awaitNode(UiTestApps.browser.name)
        composeRule.onNodeWithText(text(R.string.list_ratings_title)).assertIsDisplayed()

        UiTestApps.catalog.forEach { app ->
            composeRule
                .onNode(hasScrollToIndexAction())
                .performScrollToNode(hasText(app.name))
            composeRule.onNodeWithText(app.name).assertExists()
        }
    }

    @Test
    fun clickApp_opensDetailOfThatApp() {
        openCard(UiTestApps.browser)

        composeRule.onNodeWithText(UiTestApps.browser.version).assertExists()
        composeRule.onNodeWithText(UiTestApps.browser.fullDescription).assertExists()
        composeRule.onNodeWithText(text(R.string.app_details_download)).assertIsDisplayed()
    }

    @Test
    fun toolbarBack_returnsToCatalog() {
        openCard(UiTestApps.mail)

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()

        awaitNode(text(R.string.list_ratings_title))
        composeRule.onNodeWithText(text(R.string.list_ratings_title)).assertIsDisplayed()
    }

    @Test
    fun systemBack_returnsToCatalog() {
        openCard(UiTestApps.mail)

        Espresso.pressBack()

        awaitNode(text(R.string.list_ratings_title))
    }

    @Test
    fun similarApp_opensItsOwnCard_andBackReturnsToPrevious() {
        openCard(UiTestApps.browser)

        composeRule.onNodeWithText(text(R.string.app_details_similar)).performScrollTo()
        composeRule.onNodeWithText(UiTestApps.mail.name).performScrollTo().performClick()
        awaitNode(UiTestApps.mail.version)

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        awaitNode(UiTestApps.browser.version)
    }

    @Test
    fun wishlist_togglesIcon_andSurvivesCardReopen() {
        openCard(UiTestApps.browser)

        composeRule.onNodeWithContentDescription(text(R.string.wishlist_add)).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription(text(R.string.wishlist_remove))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        awaitNode(text(R.string.list_ratings_title))

        openCard(UiTestApps.browser)

        composeRule
            .onNodeWithContentDescription(text(R.string.wishlist_remove))
            .assertIsDisplayed()
    }

    @Test
    fun favorites_togglesButton_andSurvivesCardReopen() {
        openCard(UiTestApps.browser)

        composeRule.onNodeWithText(text(R.string.favorite_add)).performScrollTo().performClick()
        awaitNode(text(R.string.favorite_remove))

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        awaitNode(text(R.string.list_ratings_title))

        openCard(UiTestApps.browser)

        composeRule.onNodeWithText(text(R.string.favorite_remove)).assertIsDisplayed()
    }

    @Test
    fun install_runsFromStartToInstalled() {
        openCard(UiTestApps.browser)

        composeRule.onNodeWithText(text(R.string.app_details_download)).performClick()
        awaitNode(text(R.string.app_details_install_cancel))

        awaitNode(text(R.string.app_details_installed), timeoutMillis = 10_000)
        composeRule.onNodeWithText(text(R.string.app_details_installed)).assertIsNotEnabled()
    }

    @Test
    fun cancelInstall_leavesAppNotInstalled() {
        openCard(UiTestApps.browser)

        composeRule.onNodeWithText(text(R.string.app_details_download)).performClick()
        awaitNode(text(R.string.app_details_install_cancel))
        composeRule.onNodeWithText(text(R.string.app_details_install_cancel)).performClick()
        awaitNode(text(R.string.app_details_download))

        // полная эмуляция установки — 21 шаг по 150 мс (≈3,2 с)
        Thread.sleep(4_000)

        composeRule.onNodeWithText(text(R.string.app_details_download)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.app_details_installed)).assertDoesNotExist()
    }

    /**
     * Тестовые биндинги только для этого класса: сеть заменена каталогом в памяти.
     * Модуль вложен в тест намеренно — глобальный androidTest-модуль конфликтовал бы
     * с продовым [AppModule] в компоненте [MainActivityLaunchTest].
     */
    @Module
    @InstallIn(SingletonComponent::class)
    object TestBindings {

        @Provides
        @Singleton
        fun provideAppRepository(): AppRepository = UiTestAppRepository()

        @Provides
        @Singleton
        fun provideRatingRepository(): RatingRepository = InstantRatingRepository()
    }
}
