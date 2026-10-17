package com.strobingn.bowtune.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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

    /** 1 = light, 2 = medium, 3 = strong. */
    val hapticStrength: Flow<Int> =
        context.appPrefsDataStore.data.map { (it[KEY_HAPTIC_STRENGTH] ?: DEFAULT_HAPTIC_STRENGTH).coerceIn(1, 3) }

    val ttsEnabled: Flow<Boolean> =
        context.appPrefsDataStore.data.map { it[KEY_TTS] ?: false }

    /** TextToSpeech speech rate multiplier (1.0 = normal). */
    val ttsRate: Flow<Float> =
        context.appPrefsDataStore.data.map {
            (it[KEY_TTS_RATE] ?: DEFAULT_TTS_RATE).coerceIn(MIN_TTS_RATE, MAX_TTS_RATE)
        }

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

    suspend fun setHapticStrength(level: Int) {
        context.appPrefsDataStore.edit { it[KEY_HAPTIC_STRENGTH] = level.coerceIn(1, 3) }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[KEY_TTS] = enabled }
    }

    suspend fun setTtsRate(rate: Float) {
        context.appPrefsDataStore.edit { it[KEY_TTS_RATE] = rate.coerceIn(MIN_TTS_RATE, MAX_TTS_RATE) }
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
            prefs[KEY_HAPTIC_STRENGTH]?.let { put(KEY_HAPTIC_STRENGTH.name, it.toString()) }
            prefs[KEY_TTS]?.let { put(KEY_TTS.name, it.toString()) }
            prefs[KEY_TTS_RATE]?.let { put(KEY_TTS_RATE.name, it.toString()) }
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
            values[KEY_HAPTIC_STRENGTH.name]?.toIntOrNull()?.let { prefs[KEY_HAPTIC_STRENGTH] = it.coerceIn(1, 3) }
            values[KEY_TTS.name]?.toBooleanStrictOrNull()?.let { prefs[KEY_TTS] = it }
            values[KEY_TTS_RATE.name]?.toFloatOrNull()?.let {
                prefs[KEY_TTS_RATE] = it.coerceIn(MIN_TTS_RATE, MAX_TTS_RATE)
            }
            values[KEY_LARGE_TEXT.name]?.toBooleanStrictOrNull()?.let { prefs[KEY_LARGE_TEXT] = it }
            values[KEY_HIGH_CONTRAST.name]?.toBooleanStrictOrNull()?.let { prefs[KEY_HIGH_CONTRAST] = it }
            values[KEY_CHECKLIST_TEMPLATE.name]?.let { prefs[KEY_CHECKLIST_TEMPLATE] = it }
        }
    }

    companion object {
        const val DEFAULT_HAPTIC_STRENGTH = 2
        const val DEFAULT_TTS_RATE = 1.0f
        const val MIN_TTS_RATE = 0.5f
        const val MAX_TTS_RATE = 2.0f

        val KEY_ACTIVE_SETUP = longPreferencesKey("active_setup_id")
        val KEY_COACHING = stringPreferencesKey("coaching_level")
        val KEY_HAPTIC = booleanPreferencesKey("haptic_enabled")
        val KEY_HAPTIC_STRENGTH = intPreferencesKey("haptic_strength")
        val KEY_TTS = booleanPreferencesKey("tts_enabled")
        val KEY_TTS_RATE = floatPreferencesKey("tts_rate")
        val KEY_LARGE_TEXT = booleanPreferencesKey("large_text")
        val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val KEY_CHECKLIST_TEMPLATE = stringPreferencesKey("checklist_template")
    }
}
