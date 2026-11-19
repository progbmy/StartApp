package ru.mobile.startapp.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobile.startapp.presentation.detail.AppDetailViewModel

/** Маршруты навигации: список и карточка с аргументом appId. */
class AppRoutesTest {

    @Test
    fun listRoute_isStartDestination() {
        assertEquals("apps", AppRoutes.LIST)
    }

    @Test
    fun detailPattern_declaresAppIdArgument() {
        assertEquals("apps/{${AppDetailViewModel.ARG_APP_ID}}", AppRoutes.DETAIL_PATTERN)
        assertEquals("apps/{appId}", AppRoutes.DETAIL_PATTERN)
    }

    @Test
    fun detail_buildsConcreteRoute() {
        assertEquals("apps/browser", AppRoutes.detail("browser"))
        assertEquals("apps/navigator", AppRoutes.detail("navigator"))
    }
}
