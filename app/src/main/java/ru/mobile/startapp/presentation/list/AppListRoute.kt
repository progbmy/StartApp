package ru.mobile.startapp.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Связка экрана списка с ViewModel.
 *
 * ViewModel создаётся через Hilt — [AppRepository] подставляется автоматически.
 */
@Composable
fun AppListRoute(
    onAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AppListViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppListScreen(
        uiState = uiState,
        onAppClick = onAppClick,
        modifier = modifier
    )
}
