package ru.mobile.startapp.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.mobile.startapp.domain.App

/**
 * Связка экрана списка с ViewModel.
 *
 * ViewModel создаётся через Hilt — [ru.mobile.startapp.domain.repository.AppRepository]
 * и [ru.mobile.startapp.domain.usecase.LoadRatingsUseCase] подставляются автоматически.
 */
@Composable
fun AppListRoute(
    onAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AppListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppListScreen(
        uiState = uiState,
        onAppClick = onAppClick,
        onRerunRatings = viewModel::onRerunRatings,
        modifier = modifier
    )
}
