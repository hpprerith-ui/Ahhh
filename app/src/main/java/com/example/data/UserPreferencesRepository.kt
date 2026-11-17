package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserCalibrationBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blockway_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITE_ROOMS = stringSetPreferencesKey("favorite_rooms")
        val RECENT_ROOMS = stringPreferencesKey("recent_rooms_csv")
        val ACCESSIBLE_ONLY = booleanPreferencesKey("accessible_only")
        val CALIBRATIONS = stringPreferencesKey("ips_calibrations_v1")
    }

    val favoriteRoomIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_ROOMS] ?: setOf("mb_2_202", "ab_2_204", "mb_g_phys_w", "mb_1_office")
    }

    val recentRoomIds: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENT_ROOMS].orEmpty()
        if (raw.isBlank()) listOf("mb_2_202", "ab_2_205", "mb_1_cad_lab")
        else raw.split(",").filter { it.isNotBlank() }
    }

    val accessibleOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCESSIBLE_ONLY] ?: false
    }

    val customCalibrations: Flow<List<UserCalibrationBinding>> = context.dataStore.data.map { prefs ->
        decodeCalibrations(prefs[Keys.CALIBRATIONS].orEmpty())
    }

    suspend fun toggleFavorite(roomId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_ROOMS]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(roomId)) current.remove(roomId)
            prefs[Keys.FAVORITE_ROOMS] = current
        }
    }

    suspend fun recordRecent(roomId: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.RECENT_ROOMS]
                ?.split(",")
                ?.filter { it.isNotBlank() && it != roomId }
                .orEmpty()
            val updated = (listOf(roomId) + existing).take(8)
            prefs[Keys.RECENT_ROOMS] = updated.joinToString(",")
        }
    }

    suspend fun setAccessibleOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESSIBLE_ONLY] = enabled
        }
    }

    suspend fun saveCalibration(binding: UserCalibrationBinding) {
        context.dataStore.edit { prefs ->
            val existing = decodeCalibrations(prefs[Keys.CALIBRATIONS].orEmpty())
                .filterNot { it.roomId == binding.roomId }
            prefs[Keys.CALIBRATIONS] = encodeCalibrations(existing + binding)
        }
    }

    suspend fun clearCalibrations() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.CALIBRATIONS)
        }
    }

    private fun encodeCalibrations(list: List<UserCalibrationBinding>): String {
        return list.joinToString("\n") { item ->
            val wifiPart = item.wifiSignatures.entries.joinToString(";") { "${it.key}=${it.value}" }
            val blePart = item.bleSignatures.entries.joinToString(";") { "${it.key}=${it.value}" }
            listOf(
                item.roomId,
                item.roomLabel.replace("|", " "),
                item.blockId,
                item.floorNumber.toString(),
                item.x.toString(),
                item.y.toString(),
                wifiPart,
                blePart
            ).joinToString("|")
        }
    }

    private fun decodeCalibrations(raw: String): List<UserCalibrationBinding> {
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 8) return@mapNotNull null
            val wifiMap = parts[6].split(";").mapNotNull { kv ->
                val pair = kv.split("=")
                if (pair.size == 2) pair[0] to (pair[1].toIntOrNull() ?: return@mapNotNull null) else null
            }.toMap()
            val bleMap = parts[7].split(";").mapNotNull { kv ->
                val pair = kv.split("=")
                if (pair.size == 2) pair[0] to (pair[1].toIntOrNull() ?: return@mapNotNull null) else null
            }.toMap()
            UserCalibrationBinding(
                roomId = parts[0],
                roomLabel = parts[1],
                blockId = parts[2],
                floorNumber = parts[3].toIntOrNull() ?: 0,
                x = parts[4].toFloatOrNull() ?: 0f,
                y = parts[5].toFloatOrNull() ?: 0f,
                wifiSignatures = wifiMap,
                bleSignatures = bleMap
            )
        }
    }
}
