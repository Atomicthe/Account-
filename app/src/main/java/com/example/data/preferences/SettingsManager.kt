package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsManager {
    val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    val DARK_MODE_ENABLED_KEY = booleanPreferencesKey("dark_mode_enabled")
    val FONT_SIZE_OFFSET_KEY = intPreferencesKey("font_size_offset") // 0, 2, 4, 6, 8
    val PASSWORD_KEY = stringPreferencesKey("password")
    val LEDGER_TITLE_KEY = stringPreferencesKey("ledger_title")
    val CUSTOM_CATEGORIES_KEY = stringPreferencesKey("custom_categories")
    val IS_LOCAL_ONLY_MODE_KEY = booleanPreferencesKey("is_local_only_mode")
    val LAST_BACKUP_TIME_KEY = stringPreferencesKey("last_backup_time")
    val TITLE_EDIT_HINT_DISMISSED_KEY = booleanPreferencesKey("title_edit_hint_dismissed")

    fun isTitleEditHintDismissed(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[TITLE_EDIT_HINT_DISMISSED_KEY] ?: false
        }
    }

    suspend fun setTitleEditHintDismissed(context: Context, dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TITLE_EDIT_HINT_DISMISSED_KEY] = dismissed
        }
    }

    fun isLocalOnlyMode(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_LOCAL_ONLY_MODE_KEY] ?: true // Default to Local-Only mode (本機紀錄無雲端模式)
        }
    }

    suspend fun setLocalOnlyMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOCAL_ONLY_MODE_KEY] = enabled
        }
    }

    fun getLastBackupTime(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[LAST_BACKUP_TIME_KEY]
        }
    }

    suspend fun setLastBackupTime(context: Context, timeStr: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_BACKUP_TIME_KEY] = timeStr
        }
    }

    fun isBiometricEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: false
        }
    }

    suspend fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    fun isDarkModeEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE_ENABLED_KEY] ?: false
        }
    }

    suspend fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED_KEY] = enabled
        }
    }

    fun getFontSizeOffset(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[FONT_SIZE_OFFSET_KEY] ?: 0
        }
    }

    suspend fun setFontSizeOffset(context: Context, offset: Int) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_OFFSET_KEY] = offset.coerceIn(0, 8)
        }
    }

    fun getPassword(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[PASSWORD_KEY]
        }
    }

    suspend fun setPassword(context: Context, password: String) {
        context.dataStore.edit { preferences ->
            preferences[PASSWORD_KEY] = password
        }
    }

    fun getLedgerTitle(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[LEDGER_TITLE_KEY] ?: "記帳"
        }
    }

    suspend fun setLedgerTitle(context: Context, title: String) {
        context.dataStore.edit { preferences ->
            preferences[LEDGER_TITLE_KEY] = title
        }
    }

    fun getCustomCategories(context: Context): Flow<List<String>> {
        return context.dataStore.data.map { preferences ->
            val str = preferences[CUSTOM_CATEGORIES_KEY] ?: ""
            if (str.isBlank()) emptyList() else str.split(",")
        }
    }

    suspend fun addCustomCategory(context: Context, categoryName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_CATEGORIES_KEY] ?: ""
            val list = if (current.isBlank()) listOf(categoryName) else current.split(",") + categoryName
            preferences[CUSTOM_CATEGORIES_KEY] = list.distinct().joinToString(",")
        }
    }

    suspend fun deleteCustomCategory(context: Context, categoryName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_CATEGORIES_KEY] ?: ""
            if (current.isNotBlank()) {
                val list = current.split(",").filter { it != categoryName }
                preferences[CUSTOM_CATEGORIES_KEY] = list.joinToString(",")
            }
        }
    }
}
