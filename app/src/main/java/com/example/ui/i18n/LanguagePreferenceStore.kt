package com.example.ui.i18n

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "real_reward_prefs")

class LanguagePreferenceStore(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("app_language")
    private val keyNotificationSound = booleanPreferencesKey("notification_sound_enabled")

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[keyLanguage]) {
            AppLanguage.ENGLISH.code -> AppLanguage.ENGLISH
            else -> AppLanguage.BANGLA // Default language is Bangla
        }
    }

    val notificationSoundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[keyNotificationSound] ?: true // Default: Notification sound ON
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[keyLanguage] = language.code
        }
    }

    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyNotificationSound] = enabled
        }
    }
}
