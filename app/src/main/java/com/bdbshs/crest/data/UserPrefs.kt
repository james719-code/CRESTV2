// UserPrefs.kt
package com.bdbshs.crest.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFS_NAME = "user_prefs"
private val Context.dataStore by preferencesDataStore(PREFS_NAME)

object UserPrefs {
    private val KEY_UID       = stringPreferencesKey("current_uid")
    private val KEY_ROLE      = stringPreferencesKey("user_role")     // "STUDENT" or "TEACHER"
    private val KEY_ACCEPTED  = booleanPreferencesKey("is_accepted")  // for students
    private val KEY_ACCESS    = booleanPreferencesKey("is_access")    // for teachers

    data class UserData(
        val uid      : String?        = null,
        val role     : String?        = null,
        val accepted : Boolean        = false,
        val access   : Boolean        = false
    )

    /** 1) A Flow you can collect from to get the latest cached data */
    val Context.userDataFlow: Flow<UserData>
        get() = dataStore.data.map { prefs ->
            UserData(
                uid      = prefs[KEY_UID].orEmpty(),
                role     = prefs[KEY_ROLE].orEmpty(),
                accepted = prefs[KEY_ACCEPTED] ?: false,
                access   = prefs[KEY_ACCESS]   ?: false
            )
        }


    /** 2) Write the current user’s data all at once */
    suspend fun Context.saveUserData(data: UserData) {
        dataStore.edit { prefs ->
            prefs[KEY_UID]      = data.uid ?: ""
            prefs[KEY_ROLE]     = data.role ?: ""
            prefs[KEY_ACCEPTED] = data.accepted
            prefs[KEY_ACCESS]   = data.access
        }
    }

    /** 3) Clear all prefs on sign‐out */
    suspend fun Context.clear() {
        dataStore.edit { it.clear() }
    }
}
