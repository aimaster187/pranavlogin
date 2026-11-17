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
import org.json.JSONObject

private val Context.pranavDataStore: DataStore<Preferences> by preferencesDataStore(name = "pranav_admin_prefs")

data class AdminSettingsState(
    val backendBaseUrl: String = "https://api.pranavsmm.shop",
    val environment: String = "production",
    val ownerApiSecret: String = "",
    val darkThemeEnabled: Boolean = false,
    val lowEndFallbackMode: Boolean = false,
    val moduleToggles: Map<String, Boolean> = emptyMap()
) {
    fun isModuleEnabled(key: String): Boolean = moduleToggles[key] ?: true
}

class PranavPreferences(private val context: Context) {
    private object Keys {
        val BACKEND_URL = stringPreferencesKey("backend_base_url")
        val ENVIRONMENT = stringPreferencesKey("environment")
        val OWNER_SECRET = stringPreferencesKey("owner_api_secret")
        val DARK_THEME = booleanPreferencesKey("dark_theme_enabled")
        val LOW_END_FALLBACK = booleanPreferencesKey("low_end_fallback_mode")
        val MODULE_TOGGLES_JSON = stringPreferencesKey("module_toggles_json")
    }

    val settingsFlow: Flow<AdminSettingsState> = context.pranavDataStore.data.map { prefs ->
        val toggles = parseToggles(prefs[Keys.MODULE_TOGGLES_JSON])
        AdminSettingsState(
            backendBaseUrl = prefs[Keys.BACKEND_URL] ?: "https://api.pranavsmm.shop",
            environment = prefs[Keys.ENVIRONMENT] ?: "production",
            ownerApiSecret = prefs[Keys.OWNER_SECRET] ?: "",
            darkThemeEnabled = prefs[Keys.DARK_THEME] ?: false,
            lowEndFallbackMode = prefs[Keys.LOW_END_FALLBACK] ?: false,
            moduleToggles = toggles
        )
    }

    suspend fun saveBackendConfig(baseUrl: String, environment: String, secret: String) {
        context.pranavDataStore.edit { prefs ->
            val sanitized = baseUrl.trim().trimEnd('/').ifBlank { "https://api.pranavsmm.shop" }
            prefs[Keys.BACKEND_URL] = sanitized
            prefs[Keys.ENVIRONMENT] = environment.trim().ifBlank { "production" }
            prefs[Keys.OWNER_SECRET] = secret.trim()
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.pranavDataStore.edit { prefs -> prefs[Keys.DARK_THEME] = enabled }
    }

    suspend fun setLowEndFallback(enabled: Boolean) {
        context.pranavDataStore.edit { prefs -> prefs[Keys.LOW_END_FALLBACK] = enabled }
    }

    suspend fun setModuleEnabled(key: String, enabled: Boolean) {
        context.pranavDataStore.edit { prefs ->
            val map = parseToggles(prefs[Keys.MODULE_TOGGLES_JSON]).toMutableMap()
            map[key] = enabled
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            prefs[Keys.MODULE_TOGGLES_JSON] = json.toString()
        }
    }

    private fun parseToggles(raw: String?): Map<String, Boolean> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, obj.optBoolean(k, true))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
