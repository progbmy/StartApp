package ru.mobile.startapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.AndroidEntryPoint
import ru.mobile.startapp.presentation.list.AppListViewModel
import ru.mobile.startapp.presentation.navigation.StartAppNavHost
import ru.mobile.startapp.ui.theme.StartAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appListViewModel: AppListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StartAppTheme {
                StartAppNavHost(viewModel = appListViewModel)
            }
        }
    }
}
