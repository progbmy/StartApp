package ru.mobile.startapp

import android.app.Application
import ru.mobile.startapp.data.AppRepository
import ru.mobile.startapp.data.DemoAppRepository

/**
 * Ручной DI-контейнер вместо Hilt: репозиторий создаётся один раз на процесс
 * и передаётся во ViewModel через фабрики.
 */
class StartAppApp : Application() {

    val appRepository: AppRepository by lazy { DemoAppRepository() }
}
