package ru.mobile.startapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.mobile.startapp.data.AppRepository
import ru.mobile.startapp.presentation.detail.AppDetailRoute
import ru.mobile.startapp.presentation.detail.AppDetailViewModel
import ru.mobile.startapp.presentation.list.AppListRoute

/**
 * Навигация приложения: список приложений и карточка приложения.
 *
 * Системная кнопка «назад» и кнопка в тулбаре работают через back stack NavHost,
 * id приложения едет аргументом маршрута и попадает в SavedStateHandle ViewModel.
 */
@Composable
fun StartAppNavHost(repository: AppRepository, modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LIST,
        modifier = modifier
    ) {
        composable(route = AppRoutes.LIST) {
            AppListRoute(
                repository = repository,
                onAppClick = { app -> navController.navigate(AppRoutes.detail(app.id)) }
            )
        }

        composable(
            route = AppRoutes.DETAIL_PATTERN,
            arguments = listOf(
                navArgument(AppDetailViewModel.ARG_APP_ID) { type = NavType.StringType }
            )
        ) {
            AppDetailRoute(
                repository = repository,
                onBack = { navController.popBackStack() },
                onSimilarAppClick = { app -> navController.navigate(AppRoutes.detail(app.id)) }
            )
        }
    }
}
