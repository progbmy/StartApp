package ru.mobile.startapp.presentation.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mobile.startapp.R
import ru.mobile.startapp.data.App
import ru.mobile.startapp.data.AppRepository

/**
 * Связка экрана карточки с ViewModel.
 * Здесь же живут UI-эффекты: снекбар по разовому событию из ViewModel.
 */
@Composable
fun AppDetailRoute(
    repository: AppRepository,
    onBack: () -> Unit,
    onSimilarAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AppDetailViewModel = viewModel(factory = AppDetailViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentState by rememberUpdatedState(uiState)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AppDetailEvent.InstallStarted -> {
                    val appName = (currentState as? AppDetailUiState.Content)?.app?.name.orEmpty()
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.app_details_download_started, appName)
                    )
                }
            }
        }
    }

    AppDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onInstallClick = viewModel::onInstallClick,
        onSimilarAppClick = onSimilarAppClick,
        onBack = onBack,
        modifier = modifier
    )
}
