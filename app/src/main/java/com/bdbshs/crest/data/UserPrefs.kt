// UserPrefs.kt
package com.bdbshs.crest.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private const val PREFS_NAME = "user_prefs"
private const val PAGE_PREFS_NAME = "research_pages"
private val Context.dataStore by preferencesDataStore(PREFS_NAME)

object UserPrefs {
    private val KEY_UID       = stringPreferencesKey("current_uid")
    private val KEY_ROLE      = stringPreferencesKey("user_role")
    private val KEY_ACCEPTED  = booleanPreferencesKey("is_accepted")
    private val KEY_ACCESS    = booleanPreferencesKey("is_access")
    private val KEY_THEME     = stringPreferencesKey("theme_mode")
    private fun keyPage(researchId: String) = androidx.datastore.preferences.core.intPreferencesKey("page_$researchId")

    data class UserData(
        val uid      : String?        = null,
        val role     : String?        = null,
        val accepted : Boolean        = false,
        val access   : Boolean        = false,
        val theme    : ThemeMode      = ThemeMode.SYSTEM
    )

    val Context.userDataFlow: Flow<UserData>
        get() = dataStore.data.map { prefs ->
            UserData(
                uid      = prefs[KEY_UID].orEmpty(),
                role     = prefs[KEY_ROLE].orEmpty(),
                accepted = prefs[KEY_ACCEPTED] ?: false,
                access   = prefs[KEY_ACCESS]   ?: false,
                theme    = try {
                    ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name)
                } catch (e: Exception) {
                    ThemeMode.SYSTEM
                }
            )
        }

    suspend fun Context.saveUserData(data: UserData) {
        dataStore.edit { prefs ->
            prefs[KEY_UID]      = data.uid ?: ""
            prefs[KEY_ROLE]     = data.role ?: ""
            prefs[KEY_ACCEPTED] = data.accepted
            prefs[KEY_ACCESS]   = data.access
            prefs[KEY_THEME]    = data.theme.name
        }
    }

    suspend fun Context.saveTheme(theme: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun Context.clear() {
        dataStore.edit { it.clear() }
        getPagePrefs(this).edit().clear().apply()
    }

    // ==================== SHARED PREFERENCES (MODERN PAGE SAVING) ====================
    
    private fun getPagePrefs(context: Context) = 
        context.getSharedPreferences(PAGE_PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLastPageSync(context: Context, researchId: String, page: Int) {
        val success = getPagePrefs(context).edit().putInt("page_$researchId", page).commit()
        android.util.Log.d("UserPrefs", "Saved page $page for research $researchId: $success")
    }

    fun getLastPageSync(context: Context, researchId: String): Int {
        val page = getPagePrefs(context).getInt("page_$researchId", 0)
        android.util.Log.d("UserPrefs", "Loaded page $page for research $researchId")
        return page
    }
}
