package com.tan.presentation.navigation

object Routes {
    const val WELCOME = "welcome"
    const val MODELS = "models"
    const val HOME = "home"
    const val RECORD = "record"
    const val IMPORT = "import"
    const val TRANSCRIBING = "transcribing"
    const val RESULT = "result"
    const val HISTORY = "history"
    const val ARG_ID = "id"
    const val DETAIL = "detail/{$ARG_ID}"
    fun detail(id: Long) = "detail/$id"
    const val SETTINGS = "settings"
}