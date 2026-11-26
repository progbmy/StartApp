package ru.mobile.startapp.presentation.detail

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.mobile.startapp.R
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.testing.UiTestApps
import ru.mobile.startapp.ui.theme.StartAppTheme

/**
 * UI-тесты карточки приложения: содержимое, кнопка установки во всех трёх
 * состояниях, блок «Вам может понравиться» и кнопка «Назад».
 */
@RunWith(AndroidJUnit4::class)
class AppDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setScreen(
        uiState: AppDetailUiState,
        onInstallClick: () -> Unit = {},
        onSimilarAppClick: (App) -> Unit = {},
        onWishlistClick: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            StartAppTheme {
                AppDetailScreen(
                    uiState = uiState,
                    snackbarHostState = remember { SnackbarHostState() },
                    onInstallClick = onInstallClick,
                    onSimilarAppClick = onSimilarAppClick,
                    onWishlistClick = onWishlistClick,
                    onBack = onBack
                )
            }
        }
    }

    private fun content(
        installState: InstallState = InstallState.NotInstalled,
        isInWishlist: Boolean = false
    ) =
        AppDetailUiState.Content(
            app = UiTestApps.browser,
            similarApps = listOf(UiTestApps.mail, UiTestApps.gallery, UiTestApps.music),
            installState = installState,
            isInWishlist = isInWishlist
        )

    @Test
    fun content_showsNameCategoryAndDescription() {
        setScreen(content())

        // название есть и в тулбаре, и в шапке карточки — проверяем, что отрисовано
        composeRule.onAllNodesWithText(UiTestApps.browser.name).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText(UiTestApps.browser.category).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(UiTestApps.browser.description).assertIsDisplayed()
    }

    @Test
    fun content_showsAboutBlockAndInfoRows() {
        setScreen(content())

        composeRule.onNodeWithText(context.getString(R.string.app_details_about))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(UiTestApps.browser.fullDescription).assertExists()
        composeRule.onNodeWithText(UiTestApps.browser.developer).assertExists()
        composeRule.onNodeWithText(UiTestApps.browser.version).assertExists()
        composeRule.onNodeWithText(UiTestApps.browser.size).assertExists()
    }

    @Test
    fun installButton_reportsClick() {
        var clicks = 0
        setScreen(content(), onInstallClick = { clicks++ })

        composeRule.onNodeWithText(context.getString(R.string.app_details_download)).performClick()

        assertTrue("кнопка «Установить» должна вызывать колбэк", clicks == 1)
    }

    @Test
    fun installing_showsCancelButtonProgressAndPercent() {
        var clicks = 0
        setScreen(content(installState = InstallState.Installing(40)), onInstallClick = { clicks++ })

        composeRule.onNodeWithText(context.getString(R.string.app_details_install_cancel))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.app_details_install_progress, 40))
            .assertExists()
        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertExists()

        composeRule.onNodeWithText(context.getString(R.string.app_details_install_cancel)).performClick()
        assertTrue("«Отменить» должна вызывать колбэк установки", clicks == 1)
    }

    @Test
    fun installed_showsDisabledButton() {
        setScreen(content(installState = InstallState.Installed))

        composeRule.onNodeWithText(context.getString(R.string.app_details_installed))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun installed_hidesInstallAndCancelButtons() {
        setScreen(content(installState = InstallState.Installed))

        composeRule.onNodeWithText(context.getString(R.string.app_details_download)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.app_details_install_cancel)).assertDoesNotExist()
    }

    @Test
    fun notFound_showsMessage() {
        setScreen(AppDetailUiState.NotFound)

        composeRule.onNodeWithText(context.getString(R.string.app_details_not_found))
            .assertIsDisplayed()
    }

    @Test
    fun backButton_reportsClick() {
        var backs = 0
        setScreen(content(), onBack = { backs++ })

        composeRule.onNodeWithContentDescription(context.getString(R.string.action_back)).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun wishlistIcon_showsAddStateAndReportsClick() {
        var clicks = 0
        setScreen(content(isInWishlist = false), onWishlistClick = { clicks++ })

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.wishlist_add))
            .assertIsDisplayed()
            .performClick()

        assertEquals("клик по иконке должен уходить в ViewModel", 1, clicks)
    }

    @Test
    fun wishlistIcon_reflectsAddedState() {
        setScreen(content(isInWishlist = true))

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.wishlist_remove))
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.wishlist_add))
            .assertDoesNotExist()
    }

    @Test
    fun wishlistIcon_isHiddenWhenAppIsNotFound() {
        setScreen(AppDetailUiState.NotFound)

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.wishlist_add))
            .assertDoesNotExist()
    }

    @Test
    fun similarApps_showHeadingAndReportClicks() {
        var clicked: App? = null
        setScreen(content(), onSimilarAppClick = { clicked = it })

        composeRule.onNodeWithText(context.getString(R.string.app_details_similar))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText(UiTestApps.mail.name).performScrollTo().performClick()

        assertEquals("mail", clicked?.id)
    }

    @Test
    fun similarApps_blockIsHidden_whenListIsEmpty() {
        setScreen(
            AppDetailUiState.Content(
                app = UiTestApps.browser,
                similarApps = emptyList()
            )
        )

        composeRule.onNodeWithText(context.getString(R.string.app_details_similar))
            .assertDoesNotExist()
    }
}
