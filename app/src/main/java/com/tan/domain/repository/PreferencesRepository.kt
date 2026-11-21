package com.tan.domain.repository

import kotlinx.coroutines.flow.Flow

/** Ajustes chicos de la app que tienen que sobrevivir al cierre del proceso. */
interface PreferencesRepository {
    /** Id del modelo en uso ("tiny", "base"...), o null si todavía no se eligió ninguno. */
    val activeModelId: Flow<String?>
    suspend fun setActiveModelId(id: String?)

    /** true cuando el usuario ya pasó por la pantalla de bienvenida. */
    val welcomeSeen: Flow<Boolean>
    suspend fun markWelcomeSeen()
}
