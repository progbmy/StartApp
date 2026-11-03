package ru.mobile.startapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Приложение с Hilt DI.
 *
 * @HiltAndroidApp запускает процесс компиляции Hilt — генерирует
 * HiltApplication и компонент SingletonComponent.
 */
@HiltAndroidApp
class StartAppApp : Application()
