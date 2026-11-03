package ru.mobile.startapp.presentation.navigation

import ru.mobile.startapp.presentation.detail.AppDetailViewModel

/**
 * Маршруты навигации.
 *
 * Список приложений — стартовый экран, карточка открывается по id приложения,
 * поэтому каждый экран адресуем и переживает пересоздание процесса.
 */
object AppRoutes {

    const val LIST = "apps"

    val DETAIL_PATTERN = "apps/{${AppDetailViewModel.ARG_APP_ID}}"

    fun detail(appId: String): String = "apps/$appId"
}
