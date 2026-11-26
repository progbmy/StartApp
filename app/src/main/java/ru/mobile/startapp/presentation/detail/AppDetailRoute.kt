package ru.mobile.startapp.presentation.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.mobile.startapp.R
import ru.mobile.startapp.domain.App

/**
 * Связка экрана карточки с ViewModel.
 *
 * ViewModel создаётся через Hilt — [AppRepository] подставляется автоматически.
 * Здесь же живут UI-эффекты: снекбар по разовому событию из ViewModel.
 */
@Composable
fun AppDetailRoute(
    onBack: () -> Unit,
    onSimilarAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AppDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentState by rememberUpdatedState(uiState)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val appName = (currentState as? AppDetailUiState.Content)?.app?.name.orEmpty()
            when (event) {
                AppDetailEvent.InstallStarted -> snackbarHostState.showSnackbar(
                    context.getString(R.string.app_details_download_started, appName)
                )

                AppDetailEvent.InstallFinished -> snackbarHostState.showSnackbar(
                    context.getString(R.string.app_details_install_finished, appName)
                )

                AppDetailEvent.InstallCancelled -> snackbarHostState.showSnackbar(
                    context.getString(R.string.app_details_install_cancelled, appName)
                )
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
