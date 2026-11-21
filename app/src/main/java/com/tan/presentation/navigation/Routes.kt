package com.tan.presentation.navigation

import android.net.Uri
import com.tan.domain.model.TranscriptionSource

object Routes {
    const val WELCOME = "welcome"
    const val MODELS = "models"
    const val HOME = "home"
    const val RECORD = "record"
    const val IMPORT = "import"
    const val ARG_PATH = "path"
    const val ARG_SOURCE = "source"
    const val ARG_LANG = "lang"
    const val TRANSCRIBING = "transcribing?$ARG_PATH={$ARG_PATH}&$ARG_SOURCE={$ARG_SOURCE}&$ARG_LANG={$ARG_LANG}"

    /** [path]: WAV 16 kHz mono temporal de la app, que se borra al terminar de transcribir. */
    fun transcribing(path: String, source: TranscriptionSource, language: String = "es") =
        "transcribing?$ARG_PATH=${Uri.encode(path)}&$ARG_SOURCE=${source.name}&$ARG_LANG=$language"
    const val HISTORY = "history"
    const val ARG_ID = "id"
    const val RESULT = "result/{$ARG_ID}"
    fun result(id: Long) = "result/$id"
    const val DETAIL = "detail/{$ARG_ID}"
    fun detail(id: Long) = "detail/$id"
    const val SETTINGS = "settings"
}