package com.tan.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tan.presentation.components.PlaceholderScreen
import com.tan.presentation.history.HistoryScreen
import com.tan.presentation.home.HomeScreen
import com.tan.presentation.loadfile.LoadFileScreen
import com.tan.presentation.models.ModelsScreen
import com.tan.presentation.record.RecordScreen
import com.tan.presentation.transcribing.TranscribingScreen

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
        composable(Routes.RECORD) {
            RecordScreen(
                onBack = { navController.popBackStack() },
                onTranscribe = { navController.navigate(Routes.TRANSCRIBING) },
            )
        }
        composable(Routes.IMPORT) {
            LoadFileScreen(
                onBack = back,
                onContinue = { navController.navigate(Routes.TRANSCRIBING) },
            )
        }
        composable(Routes.TRANSCRIBING) {
            TranscribingScreen(
                onCancel = back,
                onErrorAccepted = back,
            )
        }
        composable(Routes.HISTORY) { HistoryScreen(onBack = back) }
        composable(Routes.DETAIL) { PlaceholderScreen("Detalle", back) }
        composable(Routes.SETTINGS) { PlaceholderScreen("Configuración", back) }
    }
}