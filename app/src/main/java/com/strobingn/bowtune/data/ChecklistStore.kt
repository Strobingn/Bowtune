package com.strobingn.bowtune.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.checklistDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tune_checklist"
)

class ChecklistStore(private val context: Context) {
    fun checkedIds(templateId: String): Flow<Set<String>> =
        context.checklistDataStore.data.map { prefs ->
            val template = ChecklistTemplates.byId(templateId)
            template.items
                .map { it.id }
                .filter { id -> prefs[key(templateId, id)] == true }
                .toSet()
        }

    suspend fun setChecked(templateId: String, id: String, checked: Boolean) {
        context.checklistDataStore.edit { prefs ->
            prefs[key(templateId, id)] = checked
        }
    }

    suspend fun clearTemplate(templateId: String) {
        context.checklistDataStore.edit { prefs ->
            ChecklistTemplates.byId(templateId).items.forEach { item ->
                prefs.remove(key(templateId, item.id))
            }
        }
    }

    suspend fun snapshot(): Map<String, Boolean> {
        val prefs = context.checklistDataStore.data.first()
        return prefs.asMap().mapNotNull { (k, v) ->
            val value = v as? Boolean ?: return@mapNotNull null
            k.name to value
        }.toMap()
    }

    suspend fun restore(values: Map<String, Boolean>) {
        context.checklistDataStore.edit { prefs ->
            prefs.clear()
            values.forEach { (name, checked) ->
                prefs[booleanPreferencesKey(name)] = checked
            }
        }
    }

    private fun key(templateId: String, id: String) =
        booleanPreferencesKey("$templateId::$id")
}
