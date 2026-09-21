package com.strobingn.bowtune.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bowtune_prefs"
)

class AppPreferences(private val context: Context) {
    val activeSetupId: Flow<Long> =
        context.appPrefsDataStore.data.map { it[KEY_ACTIVE_SETUP] ?: 0L }

    val coachingLevel: Flow<CoachingLevel> =
        context.appPrefsDataStore.data.map { CoachingLevel.fromStorage(it[KEY_COACHING]) }

    val hapticEnabled: Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[KEY_HAPTIC] ?: true }

    val ttsEnabled: Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[KEY_TTS] ?: false }

    val largeText: Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[KEY_LARGE_TEXT] ?: false }

    val highContrast: Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[KEY_HIGH_CONTRAST] ?: false }

    val checklistTemplateId: Flow<String> =
        context.appPrefsDataStore.data.map {
            it[KEY_CHECKLIST_TEMPLATE] ?: ChecklistTemplates.FULL.id
        }

    suspend fun setActiveSetupId(id: Long) {
        context.appPrefsDataStore.edit { it[KEY_ACTIVE_SETUP] = id }
    }

    suspend fun setCoachingLevel(level: CoachingLevel) {
        context.appPrefsDataStore.edit { it[KEY_COACHING] = level.name }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[KEY_HAPTIC] = enabled }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[KEY_TTS] = enabled }
    }

    suspend fun setLargeText(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[KEY_LARGE_TEXT] = enabled }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[KEY_HIGH_CONTRAST] = enabled }
    }

    suspend fun setChecklistTemplateId(id: String) {
        context.appPrefsDataStore.edit { it[KEY_CHECKLIST_TEMPLATE] = id }
    }

    suspend fun snapshot(): Map<String, String> {
        val prefs = context.appPrefsDataStore.data.first()
        return buildMap {
            prefs[KEY_ACTIVE_SETUP]?.let { put(KEY_ACTIVE_SETUP.name, it.toString()) }
            prefs[KEY_COACHING]?.let { put(KEY_COACHING.name, it) }
            prefs[KEY_HAPTIC]?.let { put(KEY_HAPTIC.name, it.toString()) }
            prefs[KEY_TTS]?.let { put(KEY_TTS.name, it.toString()) }
            prefs[KEY_LARGE_TEXT]?.let { put(KEY_LARGE_TEXT.name, it.toString()) }
            prefs[KEY_HIGH_CONTRAST]?.let { put(KEY_HIGH_CONTRAST.name, it.toString()) }
            prefs[KEY_CHECKLIST_TEMPLATE]?.let { put(KEY_CHECKLIST_TEMPLATE.name, it) }
        }
    }

    suspend fun restore(values: Map<String, String>) {
        context.appPrefsDataStore.edit { prefs ->
            values[KEY_ACTIVE_SETUP.name]?.toLongOrNull()?.let { prefs[KEY_ACTIVE_SETUP] = it }
            values[KEY_COACHING.name]?.let { prefs[KEY_COACHING] = it }
            values[KEY_HAPTIC.name]?.toBooleanStrictOrNull()?.let { prefs[KEY_HAPTIC] = it }
            values[KEY_TTS.name]?.toBooleanStrictOrNull()?.let { prefs[KEY_TTS] = it }
            values[KEY_LARGE_TEXT.name]?.toBooleanStrictOrNull()?.let { prefs[KEY_LARGE_TEXT] = it }
            values[KEY_HIGH_CONTRAST.name]?.toBooleanStrictOrNull()?.let { prefs[KEY_HIGH_CONTRAST] = it }
            values[KEY_CHECKLIST_TEMPLATE.name]?.let { prefs[KEY_CHECKLIST_TEMPLATE] = it }
        }
    }

    companion object {
        val KEY_ACTIVE_SETUP = longPreferencesKey("active_setup_id")
        val KEY_COACHING = stringPreferencesKey("coaching_level")
        val KEY_HAPTIC = booleanPreferencesKey("haptic_enabled")
        val KEY_TTS = booleanPreferencesKey("tts_enabled")
        val KEY_LARGE_TEXT = booleanPreferencesKey("large_text")
        val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val KEY_CHECKLIST_TEMPLATE = stringPreferencesKey("checklist_template")
    }
}
