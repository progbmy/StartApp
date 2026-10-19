package ru.mobile.startapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.mobile.startapp.data.App
import ru.mobile.startapp.data.AppRepository

/** Состояние карточки приложения. */
sealed interface AppDetailUiState {

    data object Loading : AppDetailUiState

    data class Content(val app: App, val similarApps: List<App>) : AppDetailUiState

    data object NotFound : AppDetailUiState
}

/** Разовые события карточки: показываются один раз и не хранятся в состоянии. */
enum class AppDetailEvent { InstallStarted }

/**
 * ViewModel карточки приложения.
 *
 * id приложения приходит из аргументов маршрута через [SavedStateHandle],
 * поэтому экран восстанавливается после смерти процесса и поворота.
 */
class AppDetailViewModel(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val appId: String = savedStateHandle.get<String>(ARG_APP_ID).orEmpty()

    private val _events = Channel<AppDetailEvent>(Channel.BUFFERED)
    val events: Flow<AppDetailEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<AppDetailUiState> = repository.apps
        .map { apps ->
            val app = apps.firstOrNull { it.id == appId }
            if (app == null) {
                AppDetailUiState.NotFound
            } else {
                AppDetailUiState.Content(
                    app = app,
                    similarApps = repository.similarApps(app.id)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppDetailUiState.Loading
        )

    /** Нажатие «Установить»: пока только сообщаем UI, что загрузка началась. */
    fun onInstallClick() {
        viewModelScope.launch { _events.send(AppDetailEvent.InstallStarted) }
    }

    companion object {

        /** Ключ аргумента маршрута с id приложения. */
        const val ARG_APP_ID = "appId"

        fun factory(repository: AppRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppDetailViewModel(repository, createSavedStateHandle()) }
        }
    }
}
