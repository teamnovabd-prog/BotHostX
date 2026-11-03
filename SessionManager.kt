package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.botHostDataStore: DataStore<Preferences> by preferencesDataStore(name = "bothostx_session")

data class SessionState(
    val apiBaseUrl: String = SessionManager.DEFAULT_PRODUCTION_PLACEHOLDER_URL,
    val accessToken: String = "",
    val onboardingCompleted: Boolean = false,
    val themeMode: String = "system",
    val userEmail: String = "",
    val userName: String = "",
    val isAdmin: Boolean = false,
)

class SessionManager(private val context: Context) {

    companion object {
        const val DEFAULT_PRODUCTION_PLACEHOLDER_URL = "https://api.example.com"

        private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_IS_ADMIN = booleanPreferencesKey("is_admin")

        @Volatile
        var currentBaseUrlVolatile: String = DEFAULT_PRODUCTION_PLACEHOLDER_URL

        @Volatile
        var currentTokenVolatile: String = ""

        fun normalizeBaseUrl(rawUrl: String): String {
            var cleaned = rawUrl.trim().trimEnd('/')
            if (cleaned.isEmpty()) return DEFAULT_PRODUCTION_PLACEHOLDER_URL
            if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
                cleaned = "https://$cleaned"
            }
            val suffixesToStrip = listOf(
                "/api/auth/login",
                "/api/auth/register",
                "/api/health",
                "/health",
                "/docs",
                "/openapi.json",
                "/api",
            )
            for (suffix in suffixesToStrip) {
                if (cleaned.endsWith(suffix, ignoreCase = true)) {
                    cleaned = cleaned.dropLast(suffix.length).trimEnd('/')
                    break
                }
            }
            return cleaned.ifEmpty { DEFAULT_PRODUCTION_PLACEHOLDER_URL }
        }
    }

    val sessionFlow: Flow<SessionState> = context.botHostDataStore.data.map { prefs ->
        val url = sanitizeBaseUrl(prefs[KEY_API_BASE_URL] ?: DEFAULT_PRODUCTION_PLACEHOLDER_URL)
        val token = prefs[KEY_ACCESS_TOKEN] ?: ""
        currentBaseUrlVolatile = url
        currentTokenVolatile = token
        SessionState(
            apiBaseUrl = url,
            accessToken = token,
            onboardingCompleted = prefs[KEY_ONBOARDING_DONE] ?: false,
            themeMode = prefs[KEY_THEME_MODE] ?: "system",
            userEmail = prefs[KEY_USER_EMAIL] ?: "",
            userName = prefs[KEY_USER_NAME] ?: "",
            isAdmin = prefs[KEY_IS_ADMIN] ?: false,
        )
    }

    suspend fun setApiBaseUrl(rawUrl: String) {
        val clean = sanitizeBaseUrl(rawUrl)
        currentBaseUrlVolatile = clean
        context.botHostDataStore.edit { prefs ->
            prefs[KEY_API_BASE_URL] = clean
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.botHostDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = completed
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.botHostDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun saveAuthenticatedSession(
        token: String,
        email: String,
        fullName: String,
        isAdmin: Boolean,
    ) {
        currentTokenVolatile = token
        context.botHostDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = token
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_NAME] = fullName
            prefs[KEY_IS_ADMIN] = isAdmin
        }
    }

    suspend fun clearSession() {
        currentTokenVolatile = ""
        context.botHostDataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_IS_ADMIN)
        }
    }

    fun sanitizeBaseUrl(rawUrl: String): String = normalizeBaseUrl(rawUrl)
}
