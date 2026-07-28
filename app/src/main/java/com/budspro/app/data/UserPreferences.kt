package com.budspro.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** The four selectable app themes. */
enum class AppTheme(val key: String, val label: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    AMOLED("amoled", "AMOLED Black"),
    PURPLE("purple", "Purple");

    companion object {
        fun fromKey(key: String?): AppTheme = values().firstOrNull { it.key == key } ?: DARK
    }
}

/** Default library layout. */
enum class DefaultView(val key: String, val label: String) {
    GRID("grid", "Grid"),
    LIST("list", "List");

    companion object {
        fun fromKey(key: String?): DefaultView = values().firstOrNull { it.key == key } ?: GRID
    }
}

data class BudsPreferences(
    val theme: AppTheme = AppTheme.DARK,
    val defaultView: DefaultView = DefaultView.GRID,
    val hapticsEnabled: Boolean = true
)

private val Context.budsDataStore: DataStore<Preferences> by preferencesDataStore(name = "buds_settings")

/**
 * Thin DataStore wrapper for user preferences. Completely separate from the
 * Room database and from anything that existed before, so no existing feature
 * can be affected by it.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_VIEW = stringPreferencesKey("default_view")
        val HAPTICS = booleanPreferencesKey("haptics")
    }

    val preferences: Flow<BudsPreferences> = context.budsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            BudsPreferences(
                theme = AppTheme.fromKey(prefs[Keys.THEME]),
                defaultView = DefaultView.fromKey(prefs[Keys.DEFAULT_VIEW]),
                hapticsEnabled = prefs[Keys.HAPTICS] ?: true
            )
        }

    suspend fun setTheme(theme: AppTheme) {
        context.budsDataStore.edit { it[Keys.THEME] = theme.key }
    }

    suspend fun setDefaultView(view: DefaultView) {
        context.budsDataStore.edit { it[Keys.DEFAULT_VIEW] = view.key }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.budsDataStore.edit { it[Keys.HAPTICS] = enabled }
    }
}
