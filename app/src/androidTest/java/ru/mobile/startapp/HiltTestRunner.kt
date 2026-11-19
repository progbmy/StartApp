package ru.mobile.startapp

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Runner для инструментальных тестов с Hilt.
 *
 * Подменяет `Application` на [HiltTestApplication] — без этого не работают
 * `@HiltAndroidTest`, `@UninstallModules` и подмена биндингов в тестовых модулях.
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?
    ): Application = super.newApplication(
        classLoader,
        HiltTestApplication::class.java.name,
        context
    )
}
