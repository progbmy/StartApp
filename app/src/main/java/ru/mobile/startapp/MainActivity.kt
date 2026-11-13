package ru.mobile.startapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.mobile.startapp.presentation.navigation.StartAppNavHost
import ru.mobile.startapp.ui.theme.StartAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StartAppTheme {
                StartAppNavHost()
            }
        }
    }
}
