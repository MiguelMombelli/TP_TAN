package com.tan.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tan.presentation.components.PlaceholderScreen
import com.tan.presentation.history.HistoryScreen
import com.tan.presentation.home.HomeScreen
import com.tan.presentation.models.ModelsScreen

@Composable
fun TanNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.WELCOME) { PlaceholderScreen("Bienvenida", back) }
        composable(Routes.MODELS) { ModelsScreen(onBack = back) }
        composable(Routes.HOME) {
            HomeScreen(
                onRecord = { navController.navigate(Routes.RECORD) },
                onImport = { navController.navigate(Routes.IMPORT) },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.RECORD) { PlaceholderScreen("Grabar audio", back) }
        composable(Routes.IMPORT) { PlaceholderScreen("Cargar archivo", back) }
        composable(Routes.TRANSCRIBING) { PlaceholderScreen("Transcribiendo", back) }
        composable(Routes.HISTORY) { HistoryScreen(onBack = back) }
        composable(Routes.DETAIL) { PlaceholderScreen("Detalle", back) }
        composable(Routes.SETTINGS) { PlaceholderScreen("Configuración", back) }
    }
}