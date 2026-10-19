package ru.mobile.startapp.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import ru.mobile.startapp.data.App

/**
 * Иконка приложения для UI.
 *
 * Доменная модель не знает про Compose, поэтому маппинг живёт в слое представления.
 */
fun App.icon(): ImageVector = when (id) {
    "browser" -> Icons.Default.Search
    "mail" -> Icons.Default.Email
    "gallery" -> Icons.Default.Photo
    "music" -> Icons.Default.MusicNote
    "navigator" -> Icons.Default.DirectionsCar
    "maps" -> Icons.Default.Place
    "shop" -> Icons.Default.ShoppingCart
    "wallet" -> Icons.Default.AccountBalanceWallet
    else -> Icons.Default.Apps
}
