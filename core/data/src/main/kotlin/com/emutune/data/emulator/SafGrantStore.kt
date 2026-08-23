package com.emutune.data.emulator

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.safDataStore by preferencesDataStore(name = "saf_grants")

/**
 * Persists the user's SAF grants so a granted emulator config folder survives app
 * restart. The system-level permission is already taken via
 * `takePersistableUriPermission`; this store remembers the URI string so the reader can
 * find it again. Only the URI is stored — never the config contents, which stay in the
 * emulator's own storage.
 */
@Singleton
class SafGrantStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dolphinUriKey = stringPreferencesKey("dolphin_tree_uri")

    val dolphinUri: Flow<String?> = context.safDataStore.data.map { it[dolphinUriKey] }

    suspend fun setDolphinUri(uri: String) {
        context.safDataStore.edit { it[dolphinUriKey] = uri }
    }

    suspend fun clearDolphinUri() {
        context.safDataStore.edit { it.remove(dolphinUriKey) }
    }
}
