package ru.mobile.startapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import javax.inject.Inject

/** Состояние установки приложения: кнопка на карточке работает по нему. */
sealed interface InstallState {

    /** Приложение не установлено — кнопка «Установить». */
    data object NotInstalled : InstallState

    /** Идёт эмуляция установки, [progress] — 0..100. Кнопка «Отменить». */
    data class Installing(val progress: Int) : InstallState

    /** Установка завершена. */
    data object Installed : InstallState
}

/** Состояние карточки приложения. */
sealed interface AppDetailUiState {

    data object Loading : AppDetailUiState

    data class Content(
        val app: App,
        val similarApps: List<App>,
        val installState: InstallState = InstallState.NotInstalled
    ) : AppDetailUiState

    data object NotFound : AppDetailUiState
}

/** Разовые события карточки: показываются один раз и не хранятся в состоянии. */
enum class AppDetailEvent { InstallStarted, InstallFinished, InstallCancelled }

/**
 * ViewModel карточки приложения.
 *
 * [AppRepository] подставляется через Hilt DI.
 * id приложения приходит из аргументов маршрута через [SavedStateHandle],
 * поэтому экран восстанавливается после смерти процесса и поворота.
 */
@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val repository: AppRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val appId: String = savedStateHandle.get<String>(ARG_APP_ID).orEmpty()

    private val _events = Channel<AppDetailEvent>(Channel.BUFFERED)
    val events: Flow<AppDetailEvent> = _events.receiveAsFlow()

    /**
     * Прогресс установки — отдельный поток: это состояние UI, а не данные каталога.
     * В [AppDetailUiState.Content] оно попадает через [combine].
     */
    private val _installState = MutableStateFlow<InstallState>(InstallState.NotInstalled)

    /** Текущая эмуляция установки: держим ссылку, чтобы её можно было отменить. */
    private var installJob: Job? = null

    val uiState: StateFlow<AppDetailUiState> = combine(
        repository.apps,
        _installState
    ) { apps, installState ->
        val app = apps.firstOrNull { it.id == appId }
        if (app == null) {
            AppDetailUiState.NotFound
        } else {
            AppDetailUiState.Content(
                app = app,
                similarApps = repository.similarApps(app.id),
                installState = installState
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppDetailUiState.Loading
    )

    /**
     * Нажатие кнопки установки.
     *
     * - установка уже идёт → отменяем [installJob] и возвращаем [InstallState.NotInstalled];
     * - иначе → запускаем корутину установки в [viewModelScope] и запоминаем её в [installJob].
     */
    fun onInstallClick() {
        val current = _installState.value
        if (current is InstallState.Installing) {
            installJob?.cancel()
            installJob = null
            _installState.value = InstallState.NotInstalled
            _events.trySend(AppDetailEvent.InstallCancelled)
            return
        }

        installJob = viewModelScope.launch {
            _installState.value = InstallState.Installing(0)
            try {
                emulateInstall()
                _installState.value = InstallState.Installed
                _events.send(AppDetailEvent.InstallFinished)
            } finally {
                // джоба закончилась (успешно или отменой) — ссылку больше не держим
                installJob = null
            }
        }
        _events.trySend(AppDetailEvent.InstallStarted)
    }

    /**
     * Эмуляция установки: прогресс 0..100 шагом [PROGRESS_STEP] каждые [PROGRESS_DELAY_MS].
     *
     * [delay] — точка отмены: при `installJob.cancel()` корутина выходит с
     * CancellationException, не доходя до [InstallState.Installed],
     * а состояние уже сброшено в [onInstallClick].
     */
    private suspend fun emulateInstall() {
        for (progress in 0..100 step PROGRESS_STEP) {
            delay(PROGRESS_DELAY_MS)
            _installState.update { InstallState.Installing(progress) }
        }
    }

    companion object {

        /** Ключ аргумента маршрута с id приложения. */
        const val ARG_APP_ID = "appId"

        /** Шаг прогресса установки, %. */
        const val PROGRESS_STEP = 5

        /** Пауза между шагами прогресса, мс. */
        const val PROGRESS_DELAY_MS = 150L
    }
}
