package com.strobingn.bowtune.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.checklistDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tune_checklist"
)

class ChecklistStore(private val context: Context) {
    fun checkedIds(): Flow<Set<String>> =
        context.checklistDataStore.data.map { prefs ->
            TuneChecklistCatalog.items
                .map { it.id }
                .filter { id -> prefs[booleanPreferencesKey(id)] == true }
                .toSet()
        }

    suspend fun setChecked(id: String, checked: Boolean) {
        context.checklistDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(id)] = checked
        }
    }

    suspend fun clearAll() {
        context.checklistDataStore.edit { prefs ->
            TuneChecklistCatalog.items.forEach { item ->
                prefs.remove(booleanPreferencesKey(item.id))
            }
        }
    }
}
