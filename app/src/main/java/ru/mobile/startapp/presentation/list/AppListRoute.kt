package ru.mobile.startapp.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mobile.startapp.data.App
import ru.mobile.startapp.data.AppRepository

/**
 * Связка экрана списка с ViewModel: собирает состояние,
 * сам экран остаётся stateless и легко проверяется превью.
 */
@Composable
fun AppListRoute(
    repository: AppRepository,
    onAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AppListViewModel = viewModel(factory = AppListViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppListScreen(
        uiState = uiState,
        onAppClick = onAppClick,
        modifier = modifier
    )
}
