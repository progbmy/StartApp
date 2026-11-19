package ru.mobile.startapp

import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Дымовой тест запуска: MainActivity поднимается через Hilt (DI не сломан),
 * не падает и устанавливает Compose-контент.
 *
 * Каталог при этом грузится из сети, поэтому проверяем только каркас:
 * тексты экранов проверяют тесты самих экранов и сквозной NavigationFlowTest.
 *
 * `@HiltAndroidTest` нужен потому, что весь тестовый APK работает на
 * `HiltTestApplication`: без правила Hilt-компонент приложения не создаётся.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityLaunchTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun injectTestBindings() {
        hiltRule.inject()
    }

    @Test
    fun launch_createsComposition_withoutCrash() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                assertFalse("активити закрылась сразу после запуска", activity.isFinishing)

                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                assertNotNull("в активити нет content-вью", content)
                assertTrue("Compose-контент не установлен", content.childCount > 0)
                assertTrue(
                    "ожидали ComposeView в иерархии, получили ${content.getChildAt(0)}",
                    content.getChildAt(0).javaClass.name.contains("ComposeView")
                )
            }
        } finally {
            scenario.close()
        }
    }
}
