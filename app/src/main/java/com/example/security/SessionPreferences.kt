package com.example.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sujoyDataStore: DataStore<Preferences> by preferencesDataStore(name = "sujoy_pay_secure_prefs")

data class SessionState(
    val activeUserId: String? = null,
    val hideBalance: Boolean = false,
    val darkModeOverride: Boolean? = null
)

class SessionPreferences(private val context: Context) {
    private object Keys {
        val ENCRYPTED_SESSION_USER_ID = stringPreferencesKey("enc_session_uid")
        val HIDE_BALANCE = booleanPreferencesKey("hide_balance")
        val DARK_MODE = stringPreferencesKey("dark_mode_pref")
    }

    val sessionFlow: Flow<SessionState> = context.sujoyDataStore.data.map { prefs ->
        val encryptedUid = prefs[Keys.ENCRYPTED_SESSION_USER_ID].orEmpty()
        val decryptedUid = if (encryptedUid.isNotBlank()) {
            CryptoVault.decryptSensitive(encryptedUid).ifBlank { null }
        } else null
        val darkPref = when (prefs[Keys.DARK_MODE]) {
            "DARK" -> true
            "LIGHT" -> false
            else -> null
        }
        SessionState(
            activeUserId = decryptedUid,
            hideBalance = prefs[Keys.HIDE_BALANCE] ?: false,
            darkModeOverride = darkPref
        )
    }

    suspend fun saveSessionUserId(userId: String) {
        val encrypted = CryptoVault.encryptSensitive(userId)
        context.sujoyDataStore.edit { prefs ->
            prefs[Keys.ENCRYPTED_SESSION_USER_ID] = encrypted
        }
    }

    suspend fun clearSession() {
        context.sujoyDataStore.edit { prefs ->
            prefs.remove(Keys.ENCRYPTED_SESSION_USER_ID)
        }
    }

    suspend fun setHideBalance(hidden: Boolean) {
        context.sujoyDataStore.edit { prefs ->
            prefs[Keys.HIDE_BALANCE] = hidden
        }
    }

    suspend fun setDarkMode(dark: Boolean?) {
        context.sujoyDataStore.edit { prefs ->
            when (dark) {
                true -> prefs[Keys.DARK_MODE] = "DARK"
                false -> prefs[Keys.DARK_MODE] = "LIGHT"
                null -> prefs.remove(Keys.DARK_MODE)
            }
        }
    }
}
