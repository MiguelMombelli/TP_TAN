package com.tan.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tan.presentation.detail.DetailScreen
import com.tan.presentation.history.HistoryScreen
import com.tan.presentation.home.HomeScreen
import com.tan.presentation.loadfile.LoadFileScreen
import com.tan.presentation.models.ModelsScreen
import com.tan.presentation.record.RecordScreen
import com.tan.presentation.result.ResultScreen
import com.tan.presentation.settings.SettingsScreen
import com.tan.presentation.start.StartViewModel
import com.tan.presentation.transcribing.TranscribingScreen
import com.tan.presentation.welcome.WelcomeScreen

@Composable
fun TanNavHost(startViewModel: StartViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }

    // Hasta saber si es la primera vez no se arma el NavHost (son unos milisegundos).
    val startRoute by startViewModel.startRoute.collectAsState()
    val start = startRoute ?: return

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onChooseModel = {
                    // Bienvenida -> Inicio -> Modelos: al volver atrás desde Modelos se cae en Inicio.
                    navController.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                    navController.navigate(Routes.MODELS)
                },
            )
        }
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
                onFinished = { id ->
                    navController.navigate(Routes.result(id)) { popUpTo(Routes.HOME) }
                },
            )
        }
        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
        ) {
            ResultScreen(
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onRecordAgain = {
                    navController.navigate(Routes.RECORD) { popUpTo(Routes.HOME) }
                },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = back,
                onItemClick = { id -> navController.navigate(Routes.detail(id)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
        ) {
            DetailScreen(onBack = back)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = back,
                onManageModels = { navController.navigate(Routes.MODELS) },
            )
        }
    }
}