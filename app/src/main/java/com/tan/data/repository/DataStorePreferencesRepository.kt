package com.tan.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tan.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore tiene que declararse una sola vez por archivo, a nivel de archivo.
private val Context.tanDataStore by preferencesDataStore(name = "tan_prefs")

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreferencesRepository {

    override val activeModelId: Flow<String?> =
        context.tanDataStore.data.map { it[ACTIVE_MODEL] }

    override suspend fun setActiveModelId(id: String?) {
        context.tanDataStore.edit { prefs ->
            if (id != null) {
                prefs[ACTIVE_MODEL] = id
            } else {
                prefs.remove(ACTIVE_MODEL)
            }
        }
    }

    override val welcomeSeen: Flow<Boolean> =
        context.tanDataStore.data.map { it[WELCOME_SEEN] ?: false }

    override suspend fun markWelcomeSeen() {
        context.tanDataStore.edit { it[WELCOME_SEEN] = true }
    }

    private companion object {
        val ACTIVE_MODEL = stringPreferencesKey("active_model")
        val WELCOME_SEEN = booleanPreferencesKey("welcome_seen")
    }
}
