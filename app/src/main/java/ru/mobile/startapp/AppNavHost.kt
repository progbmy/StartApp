package ru.mobile.startapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Маршруты навигации.
 *
 * Список приложений — стартовый экран, карточка открывается по id приложения,
 * поэтому каждый экран адресуем и переживает пересоздание процесса.
 */
object AppRoutes {
    const val LIST = "apps"
    const val DETAIL_ARG = "appId"
    const val DETAIL_PATTERN = "apps/{appId}"

    fun detail(appId: String): String = "apps/$appId"
}

/**
 * Навигация приложения: список приложений и карточка приложения.
 * Системная кнопка «назад» и кнопка в тулбаре работают через back stack NavHost.
 */
@Composable
fun StartAppNavHost(apps: List<App>, modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LIST,
        modifier = modifier
    ) {
        composable(route = AppRoutes.LIST) {
            AppListScreen(
                apps = apps,
                onAppClick = { app -> navController.navigate(AppRoutes.detail(app.id)) }
            )
        }

        composable(
            route = AppRoutes.DETAIL_PATTERN,
            arguments = listOf(navArgument(AppRoutes.DETAIL_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString(AppRoutes.DETAIL_ARG)
            val app = apps.firstOrNull { it.id == appId }

            if (app == null) {
                // неизвестный id (например, из сохранённого состояния) — просто уходим назад
                LaunchedEffect(appId) { navController.popBackStack() }
                Box(modifier = Modifier.fillMaxSize())
            } else {
                AppDetailScreen(
                    app = app,
                    similarApps = similarApps(apps = apps, current = app),
                    onSimilarAppClick = { similar ->
                        navController.navigate(AppRoutes.detail(similar.id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * «Вам может понравиться»: сначала приложения той же категории, затем остальные.
 */
private fun similarApps(apps: List<App>, current: App): List<App> {
    val sameCategory = apps.filter { it.id != current.id && it.category == current.category }
    val others = apps.filter { it.id != current.id && it.category != current.category }
    return (sameCategory + others).take(4)
}

